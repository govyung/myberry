/*
* Copyright 2018 gaoyang.  All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package org.myberry.server.quarum;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.myberry.common.ServiceThread;
import org.myberry.remoting.common.RemotingUtil;
import org.myberry.server.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuorumCnxManager {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.QUORUM_CNX_MANAGER_NAME);

  static final int RECV_CAPACITY = 100;
  static final int SEND_CAPACITY = 1;

  private final QuorumPeer self;
  private final ConcurrentMap<Integer, ArrayBlockingQueue<ByteBuf>> queueSendMap;
  private final ArrayBlockingQueue<ByteBuf> recvQueue;
  private final BiMap<Integer, QuorumConnection> quorumConnections;
  private final AcceptSocketService acceptSocketService;
  private final ConcurrentMap<Integer, ConnectionWorker> connectionWorkerMap;
  private final DelayCloseQuorumConnectionManager delayCloseQuorumConnectionManager;

  public QuorumCnxManager(final QuorumPeer self, final int port) {
    this.self = self;
    this.acceptSocketService = new AcceptSocketService(port);
    this.queueSendMap = new ConcurrentHashMap<>(17);
    this.recvQueue = new ArrayBlockingQueue<>(RECV_CAPACITY);
    this.connectionWorkerMap = new ConcurrentHashMap<>();
    this.quorumConnections = HashBiMap.create();
    this.delayCloseQuorumConnectionManager = new DelayCloseQuorumConnectionManager();
  }

  public void start() throws Exception {
    this.delayCloseQuorumConnectionManager.start();
    this.acceptSocketService.beginAccept();
    this.acceptSocketService.start();
    this.connectionWorkerInit();
  }

  public void finish() {
    this.destroyConnections();
    this.connectionWorkerStop();
    this.acceptSocketService.shutdown();
    this.delayCloseQuorumConnectionManager.shutdown();
  }

  public void connectionWorkerInit() {
    Iterator<Integer> queueSendIterator =
        self.getQuorumVerifier().getAllPeers().keySet().iterator();
    while (queueSendIterator.hasNext()) {
      Integer connId = queueSendIterator.next();
      if (self.getMySid() != connId.intValue()) {
        addQueueSendMap(connId);
      }
    }

    Iterator<Entry<Integer, String>> connectionWorkerIterator =
        ImmutableSortedMap.copyOf(self.getQuorumVerifier().getAllPeers())
            .subMap(0, self.getMySid())
            .entrySet()
            .iterator();
    while (connectionWorkerIterator.hasNext()) {
      Entry<Integer, String> entry = connectionWorkerIterator.next();
      addConnectionWorkerMap(entry.getKey(), entry.getValue());
    }
  }

  public void connectionWorkerStop() {
    Iterator<Entry<Integer, ConnectionWorker>> it = connectionWorkerMap.entrySet().iterator();
    while (it.hasNext()) {
      Entry<Integer, ConnectionWorker> entry = it.next();
      entry.getValue().shutdown(true);
      it.remove();
    }
  }

  public void destroyConnections() {
    synchronized (this.quorumConnections) {
      Iterator<Entry<Integer, QuorumConnection>> it = quorumConnections.entrySet().iterator();
      while (it.hasNext()) {
        Entry<Integer, QuorumConnection> entry = it.next();
        entry.getValue().shutdown();
      }
      quorumConnections.clear();
    }
  }

  /**
   * Every time send message, check to see if it stays connected. If it is not connected, try to
   * reconnect now.
   *
   * @param connId Sent Object
   * @param byteBuf Sent message
   * @param force Forced replacement of old messages
   * @param check check connected
   */
  public void sendMessage(int connId, ByteBuf byteBuf, boolean force, boolean check) {
    ArrayBlockingQueue<ByteBuf> byteBufs = queueSendMap.get(connId);
    if (null == byteBufs) {
      return;
    }
    synchronized (byteBufs) {
      if (force) {
        ByteBuf oldByteBuf = byteBufs.poll();
        if (oldByteBuf != null) {
          log.debug("queueSendMap force: ", oldByteBuf);
        }
      }

      boolean offer = false;
      try {
        offer = byteBufs.offer(byteBuf, 3 * 1000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.info("interrupt sendMessage: {}", byteBuf);
      }

      if (!offer) {
        log.warn("offer timeout sendMessage: {}", byteBuf);
      }
    }

    if (check
        && !existConnection(connId)
        && self.getQuorumVerifier().getAllPeers().get(connId) != null) {
      reconnect(connId);
    }
  }

  /**
   * Get the connId corresponding to the connection.
   *
   * @param quorumConnection
   * @return
   */
  public int getConnId(final QuorumConnection quorumConnection) {
    Integer connId = quorumConnections.inverse().get(quorumConnection);
    if (connId == null) {
      return -1;
    } else {
      return connId;
    }
  }

  /**
   * Add one to queueSendMap.
   *
   * @param connId
   */
  public void addQueueSendMap(int connId) {
    queueSendMap.putIfAbsent(connId, new ArrayBlockingQueue<ByteBuf>(SEND_CAPACITY));
  }

  /**
   * Remove one to queueSendMap.
   *
   * @param connId
   */
  public void removeQueueSendMap(int connId) {
    queueSendMap.remove(connId);
  }

  /**
   * Add one to connectionWorkerMap.
   *
   * @param connId
   * @param remoteAddr
   */
  public void addConnectionWorkerMap(int connId, String remoteAddr) {
    ConnectionWorker connectionWorker = new ConnectionWorker(connId, remoteAddr);
    connectionWorker.start();
    ConnectionWorker oldSendWorker = connectionWorkerMap.put(connId, connectionWorker);
    if (oldSendWorker != null) {
      oldSendWorker.shutdown(true);
      oldSendWorker.close();
    }
  }

  /**
   * Remove one to connectionWorkerMap.
   *
   * @param connId
   */
  public void removeConnectionWorkerMap(int connId) {
    ConnectionWorker oldSendWorker = connectionWorkerMap.remove(connId);
    if (oldSendWorker != null) {
      oldSendWorker.shutdown(true);
      oldSendWorker.close();
    }
  }

  /**
   * Update view and connectionWorker
   *
   * @param syncView
   * @return
   */
  public boolean updateViewAndConnectionWorker(Map<Integer, String> syncView) {
    boolean changed = false;
    Iterator<Entry<Integer, String>> syncViewIt = syncView.entrySet().iterator();
    while (syncViewIt.hasNext()) {
      Entry<Integer, String> entry = syncViewIt.next();
      if (!queueSendMap.containsKey(entry.getKey())
          && self.getMySid() != entry.getKey().intValue()) {
        if (entry.getKey().intValue() < self.getMySid()) {
          this.addQueueSendMap(entry.getKey());
          this.addConnectionWorkerMap(entry.getKey(), entry.getValue());
        }
        self.addMember(entry.getKey(), entry.getValue());
        changed = true;
      }
    }

    Iterator<Integer> sendViewIt = queueSendMap.keySet().iterator();
    while (sendViewIt.hasNext()) {
      Integer connId = sendViewIt.next();
      if (!syncView.containsKey(connId.intValue()) && self.getMySid() != connId.intValue()) {
        self.removeMember(connId);
        this.removeConnectionWorkerMap(connId);
        this.removeQueueSendMap(connId);
        changed = true;
      }
    }
    return changed;
  }

  public void printAllTrigger() {
    try {
      log.info("--------------------------------------------------------");
      {
        log.info("members SIZE: {}", self.getMembers().size());
        Iterator<Entry<Integer, String>> it = self.getMembers().entrySet().iterator();
        while (it.hasNext()) {
          Entry<Integer, String> entry = it.next();
          log.info("members: {} {}", entry.getKey(), entry.getValue());
        }
      }

      {
        log.info("queueSendMap SIZE: {}", this.queueSendMap.size());
        Iterator<Integer> it = this.queueSendMap.keySet().iterator();
        while (it.hasNext()) {
          Integer connId = it.next();
          log.info("queueSendMap connId: {}", connId);
        }
      }

      {
        log.info("connectionWorkerMap SIZE: {}", this.connectionWorkerMap.size());
        Iterator<Integer> it = this.connectionWorkerMap.keySet().iterator();
        while (it.hasNext()) {
          Integer connId = it.next();
          log.info("connectionWorkerMap connId: {}", connId);
        }
      }

      {
        log.info("quorumConnections SIZE: {}", this.quorumConnections.size());
        Iterator<Integer> it = this.quorumConnections.keySet().iterator();
        while (it.hasNext()) {
          Integer connId = it.next();
          log.info("quorumConnections connId: {}", connId);
        }
      }
      log.info("--------------------------------------------------------");
    } catch (Exception e) {
      log.error("printAllTrigger Exception", e);
    }
  }

  /**
   * Whether the connection corresponding to connId exists.
   *
   * @param connId
   * @return
   */
  public boolean existConnection(int connId) {
    return quorumConnections.get(connId) == null ? false : true;
  }

  /**
   * Get connection pool size.
   *
   * @return
   */
  public int getConnectionPoolSize() {
    return quorumConnections.size();
  }

  /**
   * Add a connection to the connection pool.
   *
   * @param connId
   * @param quorumConnection
   */
  public void addConnection(final int connId, final QuorumConnection quorumConnection) {
    synchronized (this.quorumConnections) {
      QuorumConnection oldQuorumConnection = quorumConnections.put(connId, quorumConnection);
      log.info(
          "quorumConnections added from {} connId: {}",
          quorumConnection.isFromConnectionWorker() == true
              ? connectionWorkerMap.get(connId).getServiceName()
              : acceptSocketService.getServiceName(),
          connId);
      if (oldQuorumConnection != null) {
        oldQuorumConnection.shutdown();
      }
    }
  }

  /**
   * Remove the corresponding connection from the connection pool.
   *
   * @param quorumConnection
   */
  public void removeConnection(final QuorumConnection quorumConnection) {
    synchronized (this.quorumConnections) {
      Integer connId = quorumConnections.inverse().get(quorumConnection);
      if (connId != null) {
        this.quorumConnections.remove(connId);
        log.info("quorumConnections removed connId: {}", connId);
      }
    }
  }

  /**
   * Disconnect the connection corresponding to connId.
   *
   * @param connId
   */
  public void disconnect(int connId) {
    synchronized (this.quorumConnections) {
      QuorumConnection quorumConnection = quorumConnections.remove(connId);
      if (quorumConnection != null) {
        quorumConnection.shutdown();
        log.info("quorumConnections disconnected connId: {}", connId);
      }
    }
  }

  /**
   * Reconnect the server corresponding to connId.
   *
   * @param connId
   */
  public void reconnect(int connId) {
    ConnectionWorker connectionWorker = connectionWorkerMap.get(connId);
    if (connectionWorker != null) {
      connectionWorker.wakeup();
    }
  }

  /** Clear all send queues. */
  public void clearAllReadyQueueSend() {
    Collection<ArrayBlockingQueue<ByteBuf>> queues = queueSendMap.values();
    for (ArrayBlockingQueue<ByteBuf> queue : queues) {
      queue.clear();
    }
  }

  /**
   * Delay closing the connection corresponding to connId.
   *
   * @param connId
   * @param delayMills
   */
  public void addDelayCloseQuorumConnection(int connId, long delayMills) {
    delayCloseQuorumConnectionManager.addDelayCloseQuorumConnection(
        new DelayConnId(connId, delayMills));
  }

  public ConcurrentMap<Integer, ArrayBlockingQueue<ByteBuf>> getQueueSendMap() {
    return queueSendMap;
  }

  public ArrayBlockingQueue<ByteBuf> getRecvQueue() {
    return recvQueue;
  }

  private static class DelayConnId implements Delayed {

    private int connId;
    private long excuteTime;

    public DelayConnId(int connId, long delayTime) {
      this.connId = connId;
      this.excuteTime = delayTime;
    }

    public int getConnId() {
      return connId;
    }

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(this.excuteTime - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed delayed) {
      DelayConnId delayConnId = (DelayConnId) delayed;
      return this.connId > delayConnId.connId ? 1 : (this.connId < delayConnId.connId ? -1 : 0);
    }
  }

  class DelayCloseQuorumConnectionManager extends ServiceThread {

    private DelayQueue<DelayConnId> queue = new DelayQueue<>();

    @Override
    public void run() {
      log.info("{} service started", this.getServiceName());

      while (!this.isStopped()) {
        try {
          DelayConnId delayConnId = queue.poll(100, TimeUnit.MILLISECONDS);
          if (delayConnId != null) {
            disconnect(delayConnId.getConnId());
          }
        } catch (InterruptedException e) {
          log.warn("{} service has exception.", this.getServiceName(), e);
        }
      }

      log.info("{} service end", this.getServiceName());
    }

    public void addDelayCloseQuorumConnection(DelayConnId delayConnId) {
      queue.offer(delayConnId);
    }

    @Override
    public String getServiceName() {
      return DelayCloseQuorumConnectionManager.class.getSimpleName();
    }
  }

  class AcceptSocketService extends ServiceThread {

    private final SocketAddress socketAddressListen;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public AcceptSocketService(final int port) {
      this.socketAddressListen = new InetSocketAddress(port);
    }

    public void beginAccept() throws Exception {
      this.serverSocketChannel = ServerSocketChannel.open();
      this.selector = RemotingUtil.openSelector();
      this.serverSocketChannel.socket().setReuseAddress(true);
      this.serverSocketChannel.socket().bind(this.socketAddressListen);
      this.serverSocketChannel.configureBlocking(false);
      this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void shutdown(final boolean interrupt) {
      super.shutdown(interrupt);
      try {
        this.serverSocketChannel.close();
        this.selector.close();
      } catch (IOException e) {
        log.error("AcceptSocketService shutdown exception", e);
      }
    }

    @Override
    public void run() {
      log.info("{} service started", this.getServiceName());

      while (!this.isStopped()) {
        try {
          this.selector.select(1000);
          Set<SelectionKey> selected = this.selector.selectedKeys();

          if (selected != null) {
            for (SelectionKey k : selected) {
              if ((k.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
                SocketChannel sc = ((ServerSocketChannel) k.channel()).accept();

                if (sc != null) {
                  QuorumCnxManager.log.info(
                      "{} create new QuorumConnection, {}",
                      this.getServiceName(),
                      sc.socket().getRemoteSocketAddress());

                  try {
                    QuorumConnection qc = new QuorumConnection(QuorumCnxManager.this, sc);
                    qc.start();
                  } catch (Exception e) {
                    log.error("new QuorumConnection exception", e);
                    sc.close();
                  }
                }
              } else {
                log.warn("Unexpected ops in select {}", k.readyOps());
              }
            }

            selected.clear();
          }
        } catch (Exception e) {
          log.error("{} service has exception.", this.getServiceName(), e);
        }
      }

      log.info("{} service end", this.getServiceName());
    }

    @Override
    public String getServiceName() {
      return AcceptSocketService.class.getSimpleName();
    }
  }

  class ConnectionWorker extends ServiceThread {

    private final int connId;
    private final String remoteAddr;
    private final SocketAddress socketAddress;

    private SocketChannel socketChannel;

    public ConnectionWorker(final int connId, final String remoteAddr) {
      this.connId = connId;
      this.remoteAddr = remoteAddr;
      this.socketAddress = RemotingUtil.string2SocketAddress(remoteAddr);
    }

    private boolean connectOne(final SocketAddress socketAddress) {
      if (socketAddress != null) {
        this.socketChannel = RemotingUtil.connect(socketAddress);
        if (socketChannel != null) {
          return true;
        }
      }
      return false;
    }

    @Override
    public void run() {
      log.info("{} connId: [{}] service started", this.getServiceName(), connId);

      while (!this.isStopped()) {
        try {
          if (!existConnection(connId) && this.connectOne(socketAddress)) {
            QuorumConnection qc = new QuorumConnection(QuorumCnxManager.this, socketChannel);
            qc.setFromConnectionWorker(true);

            log.info("{} create new QuorumConnection, {}", this.getServiceName(), remoteAddr);
            QuorumCnxManager.this.addConnection(connId, qc);

            qc.start();

            this.waitForRunning(Long.MAX_VALUE);
          } else {
            this.waitForRunning(1 * 1000);
          }
        } catch (Exception e) {
          log.warn("{} has exception. ", this.getServiceName(), e);
          this.waitForRunning(5 * 1000);
        }
      }

      log.info("{} connId: [{}] service end", this.getServiceName(), connId);
    }

    public void close() {
      if (null != this.socketChannel) {
        try {
          this.socketChannel.close();
          this.socketChannel = null;
        } catch (IOException e) {
          log.warn("close socketChannel exception. ", e);
        }
      }
    }

    @Override
    public String getServiceName() {
      return ConnectionWorker.class.getSimpleName();
    }
  }
}
