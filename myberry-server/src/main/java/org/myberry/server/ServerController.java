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
*/
package org.myberry.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.myberry.common.ProduceMode;
import org.myberry.common.ServerConfig;
import org.myberry.common.ThreadFactoryImpl;
import org.myberry.common.expression.ParserPlugins;
import org.myberry.common.expression.impl.ParserManager;
import org.myberry.common.protocol.RequestCode;
import org.myberry.remoting.RemotingServer;
import org.myberry.remoting.netty.NettyRemotingServer;
import org.myberry.remoting.netty.NettyServerConfig;
import org.myberry.server.impl.CRService;
import org.myberry.server.impl.MyberryService;
import org.myberry.server.impl.NSService;
import org.myberry.server.processor.AdminRequestProcessor;
import org.myberry.server.processor.ClientManageProcessor;
import org.myberry.server.processor.UserRequestProcessor;
import org.myberry.server.quarum.Quorum;
import org.myberry.server.quarum.QuorumPeer;
import org.myberry.server.routeinfo.RouteInfoManager;
import org.myberry.store.DefaultMyberryStore;
import org.myberry.store.MyberryStore;
import org.myberry.store.config.StoreConfig;

public class ServerController {

  private final ServerConfig serverConfig;
  private final NettyServerConfig nettyServerConfig;
  private final StoreConfig storeConfig;
  private final BlockingQueue<Runnable> userManagerThreadPoolQueue;
  private final BlockingQueue<Runnable> clientManagerThreadPoolQueue;
  private final BlockingQueue<Runnable> adminManagerThreadPoolQueue;
  private MyberryStore myberryStore;
  private ParserPlugins parserPlugins;
  private MyberryService myberryService;
  private RouteInfoManager routeInfoManager;
  private Quorum quorum;
  private RemotingServer remotingServer;
  private ExecutorService userManageExecutor;
  private ExecutorService clientManageExecutor;
  private ExecutorService adminManageExecutor;

  public ServerController(
      final ServerConfig serverConfig,
      final NettyServerConfig nettyServerConfig,
      final StoreConfig storeConfig) {
    this.serverConfig = serverConfig;
    this.nettyServerConfig = nettyServerConfig;
    this.storeConfig = storeConfig;

    this.userManagerThreadPoolQueue =
        new LinkedBlockingQueue<Runnable>(
            this.serverConfig.getUserManagerThreadPoolQueueCapacity());
    this.clientManagerThreadPoolQueue =
        new LinkedBlockingQueue<Runnable>(
            this.serverConfig.getClientManagerThreadPoolQueueCapacity());
    this.adminManagerThreadPoolQueue =
        new LinkedBlockingQueue<Runnable>(
            this.serverConfig.getAdminManagerThreadPoolQueueCapacity());
  }

  public boolean initialize() {
    boolean result = true;
    try {
      this.myberryStore = new DefaultMyberryStore(storeConfig);
      initPlugins(storeConfig.getProduceMode());

      if (ProduceMode.CR.getProduceName().equals(storeConfig.getProduceMode())) {
        this.myberryService = new CRService(myberryStore);
      } else if (ProduceMode.NS.getProduceName().equals(storeConfig.getProduceMode())) {
        this.myberryService = new NSService(myberryStore);
      } else {
        this.myberryService = new CRService(myberryStore);
      }

      if (serverConfig.getClusterName() != null) {
        this.routeInfoManager = new RouteInfoManager();
        this.quorum =
            new QuorumPeer(serverConfig, nettyServerConfig, myberryStore, routeInfoManager);
      }
    } catch (Exception e) {
      result = false;
      e.printStackTrace();
    }

    if (result) {
      this.remotingServer = new NettyRemotingServer(nettyServerConfig);
      this.userManageExecutor =
          new ThreadPoolExecutor( //
              this.serverConfig.getUserManageThreadPoolNums(), //
              this.serverConfig.getUserManageThreadPoolNums(), //
              1000 * 60, //
              TimeUnit.MILLISECONDS, //
              userManagerThreadPoolQueue, //
              new ThreadFactoryImpl("UserManageThread_") //
              );

      this.clientManageExecutor =
          new ThreadPoolExecutor( //
              this.serverConfig.getClientManageThreadPoolNums(), //
              this.serverConfig.getClientManageThreadPoolNums(), //
              1000 * 60, //
              TimeUnit.MILLISECONDS, //
              this.clientManagerThreadPoolQueue, //
              new ThreadFactoryImpl("ClientManageThread_") //
              );

      this.adminManageExecutor =
          new ThreadPoolExecutor( //
              this.serverConfig.getAdminManageThreadPoolNums(), //
              this.serverConfig.getAdminManageThreadPoolNums(), //
              0L, //
              TimeUnit.MILLISECONDS, //
              this.adminManagerThreadPoolQueue, //
              new ThreadFactoryImpl("AdminManageThread_") //
              );
      this.registerProcessor();
    }

    return result;
  }

  public void registerProcessor() {
    /** UserRequestProcessor */
    UserRequestProcessor userRequestProcessor = new UserRequestProcessor(this);
    this.remotingServer.registerProcessor( //
        RequestCode.PULL_ID, //
        userRequestProcessor, //
        userManageExecutor //
        );

    /** ClientManageProcessor */
    ClientManageProcessor clientManageProcessor = new ClientManageProcessor(this);
    this.remotingServer.registerProcessor( //
        RequestCode.HEART_BEAT, //
        clientManageProcessor, //
        clientManageExecutor //
        );

    /** AdminRequestProcessor */
    AdminRequestProcessor adminRequestProcessor = new AdminRequestProcessor(this);
    /** Default */
    this.remotingServer.registerDefaultProcessor(adminRequestProcessor, adminManageExecutor);
  }

  public void start() throws Exception {
    if (this.myberryStore != null) {
      this.myberryStore.start();
    }

    // parserPlugins
    if (this.parserPlugins != null) {
      this.parserPlugins.registerDefaultPlugins();
    }

    if (this.myberryService != null) {
      this.myberryService.start();
    }

    if (this.quorum != null) {
      this.quorum.start();
    }

    if (this.remotingServer != null) {
      this.remotingServer.start();
    }
  }

  public void shutdown() {
    if (this.remotingServer != null) {
      this.remotingServer.shutdown();
    }

    if (this.quorum != null) {
      this.quorum.shutdown();
    }

    if (this.myberryService != null) {
      this.myberryService.shutdown();
    }

    // parserPlugins
    if (this.parserPlugins != null) {
      this.parserPlugins.unRegisterDefaultPlugins();
    }

    if (this.myberryStore != null) {
      this.myberryStore.shutdown();
    }

    if (this.userManageExecutor != null) {
      this.userManageExecutor.shutdown();
    }

    if (this.clientManageExecutor != null) {
      this.clientManageExecutor.shutdown();
    }

    if (this.adminManageExecutor != null) {
      this.adminManageExecutor.shutdown();
    }
  }

  private void initPlugins(String produceMode) {
    if (ProduceMode.CR.getProduceName().equals(produceMode)) {
      this.parserPlugins = ParserManager.getInstance();
    }
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  public NettyServerConfig getNettyServerConfig() {
    return nettyServerConfig;
  }

  public StoreConfig getStoreConfig() {
    return storeConfig;
  }

  public MyberryStore getMyberryStore() {
    return myberryStore;
  }

  public MyberryService getMyberryService() {
    return myberryService;
  }

  public RouteInfoManager getRouteInfoManager() {
    return routeInfoManager;
  }

  public ParserPlugins getParserPlugins() {
    return parserPlugins;
  }
}
