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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.myberry.common.MixAll;
import org.myberry.common.ServerConfig;
import org.myberry.common.constant.LoggerName;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.remoting.common.RemotingUtil;
import org.myberry.remoting.netty.NettyServerConfig;
import org.myberry.remoting.netty.NettySystemConfig;
import org.myberry.remoting.protocol.RemotingCommand;
import org.myberry.server.util.EnvironmentUtils;
import org.myberry.server.util.ServerUtils;
import org.myberry.store.config.StoreConfig;
import org.myberry.common.MyberryVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

public class ServerStartup {

  public static Properties properties = null;
  public static CommandLine commandLine = null;
  public static Logger log;

  public static void main(String[] args) {
    start(createBrokerController(args));
  }

  public static ServerController start(ServerController controller) {
    try {
      controller.start();
      String tip =
          "The server["
              + RemotingHelper.getPhyLocalAddress()
              + ":"
              + controller.getNettyServerConfig().getListenPort()
              + "] boot success.";

      if (null != controller.getServerConfig().getHaServerAddr()) {
        tip += " and ha server is " + controller.getServerConfig().getHaServerAddr();
      }

      log.info(tip);
      return controller;
    } catch (Throwable e) {
      e.printStackTrace();
      System.exit(-1);
    }

    return null;
  }

  public static ServerController createBrokerController(String[] args) {
    System.setProperty(
        RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MyberryVersion.CURRENT_VERSION));

    if (null == System.getProperty(NettySystemConfig.MYBERRY_REMOTING_SOCKET_SNDBUF_SIZE)) {
      NettySystemConfig.socketSndbufSize = 131072;
    }

    if (null == System.getProperty(NettySystemConfig.MYBERRY_REMOTING_SOCKET_RCVBUF_SIZE)) {
      NettySystemConfig.socketRcvbufSize = 131072;
    }

    try {
      Options options = ServerUtils.buildCommandlineOptions(new Options());
      commandLine =
          ServerUtils.parseCmdLine(
              "myberryServer", args, buildCommandlineOptions(options), new DefaultParser());
      if (null == commandLine) {
        System.exit(-1);
      }

      final ServerConfig serverConfig = new ServerConfig();
      final NettyServerConfig nettyServerConfig = new NettyServerConfig();
      final StoreConfig storeConfig = new StoreConfig();

      if (null == serverConfig.getMyberryHome()) {
        System.out.printf(
            "Please set the "
                + MixAll.MYBERRY_HOME_ENV
                + " variable in your environment to match the location of the Myberry installation");
        System.exit(-2);
      }

      if (commandLine.hasOption('p')) {
        EnvironmentUtils.printConfig(false, serverConfig, storeConfig);
      } else if (commandLine.hasOption('m')) {
        EnvironmentUtils.printConfig(true, serverConfig, storeConfig);
      }

      readMyberryProperties(serverConfig, nettyServerConfig, storeConfig);

      String haServerAddr = serverConfig.getHaServerAddr();
      if (null != haServerAddr) {
        try {
          String[] addrArray = haServerAddr.split(",");
          for (String addr : addrArray) {
            RemotingUtil.string2SocketAddress(addr);
          }
        } catch (Exception e) {
          System.out.printf(
              "The HA Server Address[%s] illegal, please set it as follows, \"1@192.168.1.7:10737,2@192.168.1.8:10747,3@192.168.1.9:10757\"%n",
              haServerAddr);
          System.exit(-3);
        }
      }

      initLogback(serverConfig);

      final ServerController controller =
          new ServerController( //
              serverConfig, //
              nettyServerConfig, //
              storeConfig //
              );

      boolean initResult = controller.initialize();
      if (!initResult) {
        controller.shutdown();
        System.exit(-3);
      }

      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  new Runnable() {
                    private volatile boolean hasShutdown = false;
                    private AtomicInteger shutdownTimes = new AtomicInteger(0);

                    @Override
                    public void run() {
                      synchronized (this) {
                        log.info(
                            "Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
                        if (!this.hasShutdown) {
                          this.hasShutdown = true;
                          long beginTime = System.currentTimeMillis();
                          controller.shutdown();
                          long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                          log.info(
                              "Shutdown hook over, consuming total time(ms): {}",
                              consumingTimeTotal);
                        }
                      }
                    }
                  },
                  "ShutdownHook"));
      return controller;
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(-1);
    }

    return null;
  }

  private static void readMyberryProperties(
      final ServerConfig serverConfig,
      final NettyServerConfig nettyServerConfig,
      final StoreConfig storeConfig)
      throws Exception {
    String file = serverConfig.getMyberryHome() + "/conf/myberry.properties";
    InputStream in = new BufferedInputStream(new FileInputStream(file));
    properties = new Properties();
    properties.load(in);

    MixAll.properties2Object(properties, serverConfig);
    MixAll.properties2Object(properties, nettyServerConfig);
    MixAll.properties2Object(properties, storeConfig);

    in.close();
  }

  private static void initLogback(final ServerConfig serverConfig) throws Exception {
    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(lc);
    lc.reset();
    configurator.doConfigure(serverConfig.getMyberryHome() + "/conf/logback.xml");

    log = LoggerFactory.getLogger(LoggerName.SERVER_LOGGER_NAME);
  }

  public static Options buildCommandlineOptions(final Options options) {
    Option opt = new Option("p", "printConfigItem", false, "Print all config item");
    opt.setRequired(false);
    options.addOption(opt);

    opt = new Option("m", "printImportantConfig", false, "Print important config item");
    opt.setRequired(false);
    options.addOption(opt);

    return options;
  }
}
