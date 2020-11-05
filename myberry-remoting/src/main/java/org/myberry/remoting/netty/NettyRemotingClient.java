/*
* MIT License
*
* Copyright (c) 2020 gaoyang
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
*
* NettyRemotingClient is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.net.SocketAddress;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.myberry.remoting.InvokeCallback;
import org.myberry.remoting.RemotingClient;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.remoting.common.RemotingUtil;
import org.myberry.remoting.exception.RemotingConnectException;
import org.myberry.remoting.exception.RemotingSendRequestException;
import org.myberry.remoting.exception.RemotingTimeoutException;
import org.myberry.remoting.exception.RemotingTooMuchRequestException;
import org.myberry.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {

  private static final Logger log = LoggerFactory.getLogger(NettyRemotingClient.class);

  private static final long LOCK_TIMEOUT_MILLIS = 3000;

  private final NettyClientConfig nettyClientConfig;
  private final Bootstrap bootstrap = new Bootstrap();
  private final EventLoopGroup eventLoopGroupWorker;
  private final Lock lockChannelTables = new ReentrantLock();
  private final ConcurrentMap<String /* addr */, ChannelWrapper> channelTables =
      new ConcurrentHashMap<String, ChannelWrapper>();
  private final Timer timer = new Timer("ClientHouseKeepingService", true);

  private final ExecutorService publicExecutor;

  /** Invoke the callback methods in this executor when process response. */
  private ExecutorService callbackExecutor;

  private DefaultEventExecutorGroup defaultEventExecutorGroup;

  public NettyRemotingClient(final NettyClientConfig nettyClientConfig) {
    super(
        nettyClientConfig.getClientOnewaySemaphoreValue(),
        nettyClientConfig.getClientAsyncSemaphoreValue());
    this.nettyClientConfig = nettyClientConfig;

    int publicThreadNums = nettyClientConfig.getClientCallbackExecutorThreads();
    if (publicThreadNums <= 0) {
      publicThreadNums = 4;
    }

    this.publicExecutor =
        Executors.newFixedThreadPool(
            publicThreadNums,
            new ThreadFactory() {
              private AtomicInteger threadIndex = new AtomicInteger(0);

              @Override
              public Thread newThread(Runnable r) {
                return new Thread(
                    r, "NettyClientPublicExecutor_" + this.threadIndex.incrementAndGet());
              }
            });

    this.eventLoopGroupWorker =
        new NioEventLoopGroup(
            1,
            new ThreadFactory() {
              private AtomicInteger threadIndex = new AtomicInteger(0);

              @Override
              public Thread newThread(Runnable r) {
                return new Thread(
                    r, String.format("NettyClientSelector_%d", this.threadIndex.incrementAndGet()));
              }
            });
  }

  @Override
  public void start() {
    this.defaultEventExecutorGroup =
        new DefaultEventExecutorGroup( //
            nettyClientConfig.getClientWorkerThreads(), //
            new ThreadFactory() {

              private AtomicInteger threadIndex = new AtomicInteger(0);

              @Override
              public Thread newThread(Runnable r) {
                return new Thread(
                    r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
              }
            });

    Bootstrap handler =
        this.bootstrap
            .group(this.eventLoopGroupWorker)
            .channel(NioSocketChannel.class) //
            .option(ChannelOption.TCP_NODELAY, true) //
            .option(ChannelOption.SO_KEEPALIVE, false) //
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                nettyClientConfig.getConnectTimeoutMillis()) //
            .option(ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize()) //
            .option(ChannelOption.SO_RCVBUF, nettyClientConfig.getClientSocketRcvBufSize()) //
            .handler(
                new ChannelInitializer<SocketChannel>() {
                  @Override
                  public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast( //
                            defaultEventExecutorGroup, //
                            new NettyDecoder(), //
                            new NettyEncoder(), //
                            new IdleStateHandler(
                                0, 0, nettyClientConfig.getClientChannelMaxIdleTimeSeconds()), //
                            new NettyConnectManageHandler(), //
                            new NettyClientHandler()); //
                  }
                });

    this.timer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            try {
              NettyRemotingClient.this.scanResponseTable();
            } catch (Exception e) {
              log.error("scanResponseTable exception", e);
            }
          }
        },
        1000 * 3,
        1000);
  }

  @Override
  public void shutdown() {
    try {
      this.timer.cancel();

      for (ChannelWrapper cw : this.channelTables.values()) {
        this.closeChannel(null, cw.getChannel());
      }

      this.channelTables.clear();

      this.eventLoopGroupWorker.shutdownGracefully();

      if (this.defaultEventExecutorGroup != null) {
        this.defaultEventExecutorGroup.shutdownGracefully();
      }
    } catch (Exception e) {
      log.error("NettyRemotingClient shutdown exception, ", e);
    }

    if (this.publicExecutor != null) {
      try {
        this.publicExecutor.shutdown();
      } catch (Exception e) {
        log.error("NettyRemotingServer shutdown exception, ", e);
      }
    }
  }

  public void closeChannel(final String addr, final Channel channel) {
    if (null == channel) return;

    final String addrRemote = null == addr ? RemotingHelper.parseChannelRemoteAddr(channel) : addr;

    try {
      if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
        try {
          boolean removeItemFromTable = true;
          final ChannelWrapper prevCW = this.channelTables.get(addrRemote);

          log.info(
              "closeChannel: begin close the channel[{}] Found: {}", addrRemote, prevCW != null);

          if (null == prevCW) {
            log.info(
                "closeChannel: the channel[{}] has been removed from the channel table before",
                addrRemote);
            removeItemFromTable = false;
          } else if (prevCW.getChannel() != channel) {
            log.info(
                "closeChannel: the channel[{}] has been closed before, and has been created again, nothing to do.",
                addrRemote);
            removeItemFromTable = false;
          }

          if (removeItemFromTable) {
            this.channelTables.remove(addrRemote);
            log.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
          }

          RemotingUtil.closeChannel(channel);
        } catch (Exception e) {
          log.error("closeChannel: close the channel exception", e);
        } finally {
          this.lockChannelTables.unlock();
        }
      } else {
        log.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
      }
    } catch (InterruptedException e) {
      log.error("closeChannel exception", e);
    }
  }

  public void closeChannel(final Channel channel) {
    if (null == channel) return;

    try {
      if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
        try {
          boolean removeItemFromTable = true;
          ChannelWrapper prevCW = null;
          String addrRemote = null;
          for (Map.Entry<String, ChannelWrapper> entry : channelTables.entrySet()) {
            String key = entry.getKey();
            ChannelWrapper prev = entry.getValue();
            if (prev.getChannel() != null) {
              if (prev.getChannel() == channel) {
                prevCW = prev;
                addrRemote = key;
                break;
              }
            }
          }

          if (null == prevCW) {
            log.info(
                "eventCloseChannel: the channel[{}] has been removed from the channel table before",
                addrRemote);
            removeItemFromTable = false;
          }

          if (removeItemFromTable) {
            this.channelTables.remove(addrRemote);
            log.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
            RemotingUtil.closeChannel(channel);
          }
        } catch (Exception e) {
          log.error("closeChannel: close the channel exception", e);
        } finally {
          this.lockChannelTables.unlock();
        }
      } else {
        log.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
      }
    } catch (InterruptedException e) {
      log.error("closeChannel exception", e);
    }
  }

  private Channel getAndCreateChannel(final String addr)
      throws RemotingConnectException, InterruptedException {
    if (null == addr) {
      throw new RemotingConnectException("addr is null");
    }

    ChannelWrapper cw = this.channelTables.get(addr);
    if (cw != null && cw.isOK()) {
      return cw.getChannel();
    }

    return this.createChannel(addr);
  }

  private Channel createChannel(final String addr) throws InterruptedException {
    ChannelWrapper cw = this.channelTables.get(addr);
    if (cw != null && cw.isOK()) {
      return cw.getChannel();
    }

    if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
      try {
        boolean createNewConnection = false;
        cw = this.channelTables.get(addr);
        if (cw != null) {

          if (cw.isOK()) {
            return cw.getChannel();
          } else if (!cw.getChannelFuture().isDone()) {
            createNewConnection = false;
          } else {
            this.channelTables.remove(addr);
            createNewConnection = true;
          }
        } else {
          createNewConnection = true;
        }

        if (createNewConnection) {
          ChannelFuture channelFuture =
              this.bootstrap.connect(RemotingHelper.string2SocketAddress(addr));
          log.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
          cw = new ChannelWrapper(channelFuture);
          this.channelTables.put(addr, cw);
        }
      } catch (Exception e) {
        log.error("createChannel: create channel exception", e);
      } finally {
        this.lockChannelTables.unlock();
      }
    } else {
      log.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
    }

    if (cw != null) {
      ChannelFuture channelFuture = cw.getChannelFuture();
      if (channelFuture.awaitUninterruptibly(this.nettyClientConfig.getConnectTimeoutMillis())) {
        if (cw.isOK()) {
          log.info(
              "createChannel: connect remote host[{}] success, {}", addr, channelFuture.toString());
          return cw.getChannel();
        } else {
          log.warn(
              "createChannel: connect remote host["
                  + addr
                  + "] failed, "
                  + channelFuture.toString(),
              channelFuture.cause());
        }
      } else {
        log.warn(
            "createChannel: connect remote host[{}] timeout {}ms, {}",
            addr,
            this.nettyClientConfig.getConnectTimeoutMillis(),
            channelFuture.toString());
      }
    }

    return null;
  }

  @Override
  public RemotingCommand invokeSync(String addr, RemotingCommand request, long timeoutMillis)
      throws InterruptedException, RemotingConnectException, RemotingSendRequestException,
          RemotingTimeoutException {
    long beginStartTime = System.currentTimeMillis();
    final Channel channel = this.getAndCreateChannel(addr);
    if (channel != null && channel.isActive()) {
      try {
        long costTime = System.currentTimeMillis() - beginStartTime;
        if (timeoutMillis < costTime) {
          throw new RemotingTimeoutException("invokeSync call timeout");
        }
        RemotingCommand response = this.invokeSyncImpl(channel, request, timeoutMillis - costTime);
        return response;
      } catch (RemotingSendRequestException e) {
        log.warn("invokeSync: send request exception, so close the channel[{}]", addr);
        this.closeChannel(addr, channel);
        throw e;
      } catch (RemotingTimeoutException e) {
        if (nettyClientConfig.isClientCloseSocketIfTimeout()) {
          this.closeChannel(addr, channel);
          log.warn("invokeSync: close socket because of timeout, {}ms, {}", timeoutMillis, addr);
        }
        log.warn("invokeSync: wait response timeout exception, the channel[{}]", addr);
        throw e;
      }
    } else {
      this.closeChannel(addr, channel);
      throw new RemotingConnectException(addr);
    }
  }

  @Override
  public void invokeAsync(
      String addr, RemotingCommand request, long timeoutMillis, InvokeCallback invokeCallback)
      throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
          RemotingTimeoutException, RemotingSendRequestException {
    long beginStartTime = System.currentTimeMillis();
    final Channel channel = this.getAndCreateChannel(addr);
    if (channel != null && channel.isActive()) {
      try {
        long costTime = System.currentTimeMillis() - beginStartTime;
        if (timeoutMillis < costTime) {
          throw new RemotingTimeoutException("invokeSync call timeout");
        }
        this.invokeAsyncImpl(channel, request, timeoutMillis - costTime, invokeCallback);
      } catch (RemotingSendRequestException e) {
        log.warn("invokeAsync: send request exception, so close the channel[{}]", addr);
        this.closeChannel(addr, channel);
        throw e;
      }
    } else {
      this.closeChannel(addr, channel);
      throw new RemotingConnectException(addr);
    }
  }

  @Override
  public void invokeOneway(String addr, RemotingCommand request, long timeoutMillis)
      throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
          RemotingTimeoutException, RemotingSendRequestException {
    final Channel channel = this.getAndCreateChannel(addr);
    if (channel != null && channel.isActive()) {
      try {
        this.invokeOnewayImpl(channel, request, timeoutMillis);
      } catch (RemotingSendRequestException e) {
        log.warn("invokeOneway: send request exception, so close the channel[{}]", addr);
        this.closeChannel(addr, channel);
        throw e;
      }
    } else {
      this.closeChannel(addr, channel);
      throw new RemotingConnectException(addr);
    }
  }

  class NettyClientHandler extends SimpleChannelInboundHandler<RemotingCommand> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingCommand msg) throws Exception {
      processMessageReceived(ctx, msg);
    }
  }

  static class ChannelWrapper {
    private final ChannelFuture channelFuture;

    public ChannelWrapper(ChannelFuture channelFuture) {
      this.channelFuture = channelFuture;
    }

    public boolean isOK() {
      return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
    }

    public boolean isWriteable() {
      return this.channelFuture.channel().isWritable();
    }

    private Channel getChannel() {
      return this.channelFuture.channel();
    }

    public ChannelFuture getChannelFuture() {
      return channelFuture;
    }
  }

  class NettyConnectManageHandler extends ChannelDuplexHandler {
    @Override
    public void connect(
        ChannelHandlerContext ctx,
        SocketAddress remoteAddress,
        SocketAddress localAddress,
        ChannelPromise promise)
        throws Exception {
      final String local =
          localAddress == null ? "UNKNOWN" : RemotingHelper.parseSocketAddressAddr(localAddress);
      final String remote =
          remoteAddress == null ? "UNKNOWN" : RemotingHelper.parseSocketAddressAddr(remoteAddress);
      log.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);

      super.connect(ctx, remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
      log.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
      closeChannel(ctx.channel());
      super.disconnect(ctx, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
      log.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
      closeChannel(ctx.channel());
      super.close(ctx, promise);
      failFast(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof IdleStateEvent) {
        IdleStateEvent event = (IdleStateEvent) evt;
        if (event.state().equals(IdleState.ALL_IDLE)) {
          final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
          log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
          closeChannel(ctx.channel());
        }
      }

      ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      final String remoteAddress = RemotingHelper.parseChannelRemoteAddr(ctx.channel());
      log.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
      log.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
      closeChannel(ctx.channel());
    }
  }

  @Override
  public ExecutorService getCallbackExecutor() {
    return callbackExecutor != null ? callbackExecutor : publicExecutor;
  }

  @Override
  public void setCallbackExecutor(ExecutorService callbackExecutor) {
    this.callbackExecutor = callbackExecutor;
  }

  @Override
  public boolean isChannelWriteable(String addr) {
    ChannelWrapper cw = this.channelTables.get(addr);
    if (cw != null && cw.isOK()) {
      return cw.isWriteable();
    }
    return true;
  }
}
