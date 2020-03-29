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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import org.myberry.common.ServiceThread;
import org.myberry.remoting.common.RemotingUtil;
import org.myberry.server.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuorumConnection {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.QUORUM_CONNECTION_NAME);

  private boolean fromConnectionWorker;

  private final QuorumCnxManager quorumCnxManager;
  private final SocketChannel socketChannel;
  private final String clientAddr;
  private WriteSocketService writeSocketService;
  private ReadSocketService readSocketService;

  public QuorumConnection(
      final QuorumCnxManager quorumCnxManager, final SocketChannel socketChannel)
      throws IOException {
    this.quorumCnxManager = quorumCnxManager;
    this.socketChannel = socketChannel;
    this.clientAddr = this.socketChannel.socket().getRemoteSocketAddress().toString();
    this.socketChannel.configureBlocking(false);
    this.socketChannel.socket().setSoLinger(false, -1);
    this.socketChannel.socket().setTcpNoDelay(true);
    this.socketChannel.socket().setReceiveBufferSize(1024 * 64);
    this.socketChannel.socket().setSendBufferSize(1024 * 64);
    this.writeSocketService = new WriteSocketService(this.socketChannel);
    this.readSocketService = new ReadSocketService(this.socketChannel);
  }

  public void start() {
    this.readSocketService.start();
    this.writeSocketService.start();
  }

  public void shutdown() {
    this.writeSocketService.shutdown(true);
    this.readSocketService.shutdown(true);
    this.close();
  }

  public void close() {
    if (this.socketChannel != null) {
      try {
        this.socketChannel.close();
      } catch (IOException e) {
        log.error("close socketChannel error: ", e);
      }
    }
  }

  public void setFromConnectionWorker(boolean fromConnectionWorker) {
    this.fromConnectionWorker = fromConnectionWorker;
  }

  public boolean isFromConnectionWorker() {
    return fromConnectionWorker;
  }

  class ReadSocketService extends ServiceThread {

    private static final int MAX_BUFFER_SIZE = 1024 * 1024;
    private ByteBuffer byteBufferRead = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    private ByteBuffer byteBufferBackup = ByteBuffer.allocate(MAX_BUFFER_SIZE);
    private int processPostion = 0;

    private final Selector selector;
    private final SocketChannel socketChannel;

    public ReadSocketService(final SocketChannel socketChannel) throws IOException {
      this.selector = RemotingUtil.openSelector();
      this.socketChannel = socketChannel;
      this.socketChannel.register(this.selector, SelectionKey.OP_READ);
      this.setDaemon(true);
    }

    @Override
    public void run() {
      QuorumConnection.log.debug("{} service started", this.getServiceName());

      while (!this.isStopped()) {
        try {
          this.selector.select(10);

          boolean ok = this.processReadEvent();
          if (!ok) {
            QuorumConnection.log.error("processReadEvent error");
            break;
          }
        } catch (Exception e) {
          QuorumConnection.log.error("{} service has exception.", this.getServiceName(), e);
          break;
        }
      }

      this.makeStop();

      writeSocketService.makeStop();

      quorumCnxManager.removeConnection(QuorumConnection.this);

      SelectionKey sk = this.socketChannel.keyFor(this.selector);
      if (sk != null) {
        sk.cancel();
      }

      try {
        this.selector.close();
        this.socketChannel.close();
      } catch (IOException e) {
        QuorumConnection.log.error("", e);
      }

      QuorumConnection.log.debug("{} service end", this.getServiceName());
    }

    private boolean processReadEvent() throws InterruptedException {
      int readSizeZeroTimes = 0;

      if (!this.byteBufferRead.hasRemaining()) {
        this.reallocateByteBuffer();
      }

      while (this.byteBufferRead.hasRemaining()) {
        try {
          int readSize = this.socketChannel.read(this.byteBufferRead);
          if (readSize > 0) {
            readSizeZeroTimes = 0;

            int msgLen = ByteBuf.headerDecode(this.byteBufferRead, this.processPostion);
            if (readFull(msgLen)) {
              ByteBuf byteBuf = ByteBuf.decode(this.byteBufferRead, this.processPostion);
              if (!fromConnectionWorker && !quorumCnxManager.existConnection(byteBuf.getConnId())) {
                quorumCnxManager.addQueueSendMap(byteBuf.getConnId());
                quorumCnxManager.addConnection(byteBuf.getConnId(), QuorumConnection.this);
              }
              this.processPostion += msgLen;
              quorumCnxManager.getRecvQueue().put(byteBuf);
            }
          } else if (readSize == 0) {
            if (++readSizeZeroTimes >= 3) {
              break;
            }
          } else {
            log.error("read socket[{}] < 0", QuorumConnection.this.clientAddr);
            return false;
          }
        } catch (IOException e) {
          log.error("processReadEvent exception", e);
          return false;
        }
      }

      return true;
    }

    private boolean readFull(int msgLen) {
      if (byteBufferRead.position() - processPostion >= msgLen) {
        return true;
      } else {
        return false;
      }
    }

    private void reallocateByteBuffer() {
      int remain = MAX_BUFFER_SIZE - this.processPostion;
      if (remain > 0) {
        this.byteBufferRead.position(this.processPostion);

        this.byteBufferBackup.position(0);
        this.byteBufferBackup.limit(MAX_BUFFER_SIZE);
        this.byteBufferBackup.put(this.byteBufferRead);
      }

      this.swapByteBuffer();

      this.byteBufferRead.position(remain);
      this.byteBufferRead.limit(MAX_BUFFER_SIZE);
      this.processPostion = 0;
    }

    private void swapByteBuffer() {
      ByteBuffer tmp = this.byteBufferRead;
      this.byteBufferRead = this.byteBufferBackup;
      this.byteBufferBackup = tmp;
    }

    @Override
    public String getServiceName() {
      return ReadSocketService.class.getSimpleName();
    }
  }

  class WriteSocketService extends ServiceThread {

    private final Selector selector;
    private final SocketChannel socketChannel;

    public WriteSocketService(final SocketChannel socketChannel) throws IOException {
      this.selector = RemotingUtil.openSelector();
      this.socketChannel = socketChannel;
      this.socketChannel.register(this.selector, SelectionKey.OP_WRITE);
      this.setDaemon(true);
    }

    @Override
    public void run() {
      QuorumConnection.log.debug("{} service started", this.getServiceName());

      while (!this.isStopped()) {
        try {
          this.selector.select(10);

          ArrayBlockingQueue<ByteBuf> byteBufQueue =
              quorumCnxManager
                  .getQueueSendMap()
                  .get(quorumCnxManager.getConnId(QuorumConnection.this));
          if (byteBufQueue != null) {
            ByteBuf byteBuf = byteBufQueue.poll();
            if (byteBuf != null) {
              this.transferData(byteBuf.encode());
            } else {
              this.waitForRunning(1 * 1000);
            }
          } else {
            this.waitForRunning(1 * 1000);
          }
        } catch (Exception e) {
          QuorumConnection.log.error("{} service has exception.", this.getServiceName(), e);
          break;
        }
      }

      this.makeStop();

      readSocketService.makeStop();

      quorumCnxManager.removeConnection(QuorumConnection.this);

      SelectionKey sk = this.socketChannel.keyFor(this.selector);
      if (sk != null) {
        sk.cancel();
      }

      try {
        this.selector.close();
        this.socketChannel.close();
      } catch (IOException e) {
        QuorumConnection.log.error("", e);
      }

      QuorumConnection.log.debug("{} service end", this.getServiceName());
    }

    private boolean transferData(ByteBuffer byteBuffer) throws Exception {
      int writeSizeZeroTimes = 0;
      // Write Header
      while (byteBuffer.hasRemaining()) {
        int writeSize = this.socketChannel.write(byteBuffer);
        if (writeSize > 0) {
          writeSizeZeroTimes = 0;
        } else if (writeSize == 0) {
          if (++writeSizeZeroTimes >= 3) {
            break;
          }
        } else {
          throw new Exception("write error < 0");
        }
      }

      return !byteBuffer.hasRemaining();
    }

    @Override
    public String getServiceName() {
      return WriteSocketService.class.getSimpleName();
    }
  }
}
