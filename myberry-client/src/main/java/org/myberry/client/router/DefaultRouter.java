/*
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
package org.myberry.client.router;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.myberry.client.ClientConfig;
import org.myberry.client.exception.MyberryClientException;
import org.myberry.client.router.loadbalance.ConsistentHashLoadBalance;
import org.myberry.client.router.loadbalance.LoadBalance;
import org.myberry.client.router.loadbalance.RandomLoadBalance;
import org.myberry.client.router.loadbalance.RoundRobinLoadBalance;
import org.myberry.common.MixAll;
import org.myberry.common.constant.LoggerName;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.common.loadbalance.LoadBalanceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultRouter {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.CLIENT_LOGGER_NAME);

  private static final Map<String, Class<?>> loadBalanceRegistry = new HashMap<>();

  static {
    loadBalanceRegistry.put(LoadBalanceName.RANDOM_LOADBALANCE, RandomLoadBalance.class);
    loadBalanceRegistry.put(LoadBalanceName.ROUNDROBIN_LOADBALANCE, RoundRobinLoadBalance.class);
    loadBalanceRegistry.put(
        LoadBalanceName.CONSISTENTHASH_LOADBALANCE, ConsistentHashLoadBalance.class);
  }

  private final ClientConfig clientConfig;

  private final AtomicReference<LoadBalance> lb = new AtomicReference<>();
  private final AtomicReference<String> maintainer = new AtomicReference<>();
  private final AtomicReference<List<Invoker>> invokers = new AtomicReference<>();

  private volatile boolean retryInvoker = false;

  private List<String> serverAddrs;
  private int invokersRetryIndex;
  private int serverAddrRetryIndex;

  public DefaultRouter(ClientConfig clientConfig) {
    this.clientConfig = clientConfig;
  }

  /**
   * Try all nodes to get routing information for init
   *
   * @return
   */
  public String initAddrs(int timesRetry) throws MyberryClientException {
    this.initServerAddrs();
    if (timesRetry <= serverAddrs.size()) {
      return getAddrFromServerAddr();
    } else {
      throw new MyberryClientException("cannot connect to server: " + clientConfig.getServerAddr());
    }
  }

  public boolean isRetryInvoker() {
    return retryInvoker;
  }

  public void setRetryInvoker(boolean retryInvoker) {
    this.retryInvoker = retryInvoker;
  }

  /** for heartbeat */
  public String fetchServerAddr() throws MyberryClientException {
    String addr = this.getAddrFromMaintainer();
    if (addr == null || retryInvoker) {
      addr = this.getAddrFromInvoker();
      if (addr == null) {
        addr = this.getAddrFromServerAddr();
      }
    } else {
      invokersRetryIndex = 0;
      serverAddrRetryIndex = 0;
    }
    return addr;
  }

  /** for heartbeat */
  public void setRouterInfo(RouterInfo routerInfo) {
    LoadBalance loadBalance = lb.get();
    if (loadBalance == null
        || loadBalanceRegistry.get(routerInfo.getLoadBalanceName()) == null
        || loadBalanceRegistry.get(routerInfo.getLoadBalanceName()) != loadBalance.getClass()) {

      maintainer.set(
          routerInfo.getMaintainer() == null
              ? clientConfig.getServerAddr()
              : routerInfo.getMaintainer());

      List<Invoker> invokerList = new ArrayList<>();
      invokerList.add(new Invoker(clientConfig.getServerAddr(), 1));
      invokers.set(routerInfo.getInvokers() == null ? invokerList : routerInfo.getInvokers());

      loadBalance =
          LoadBalanceFactory.factoryLoadBalance(
              routerInfo.getLoadBalanceName() == null
                  ? LoadBalanceName.ROUNDROBIN_LOADBALANCE
                  : routerInfo.getLoadBalanceName());
      lb.set(loadBalance);
      log.info("client load balance changed. NEW : {}", routerInfo.getLoadBalanceName());
    } else {
      if (routerInfo.getMaintainer() != null
          && !routerInfo.getMaintainer().equals(maintainer.get())) {
        log.info(
            "remoting maintainer address updated. NEW : {} , OLD: {}",
            routerInfo.getMaintainer(),
            maintainer.get());
        maintainer.set(routerInfo.getMaintainer());
      }

      if (routerInfo.getInvokers() != null) {
        if (routerInfo.getInvokers().size() != invokers.get().size()) {
          log.info(
              "remoting invokers address updated. NEW : {} , OLD: {}",
              routerInfo.getInvokers(),
              invokers.get());
          invokers.set(routerInfo.getInvokers());
          return;
        }

        boolean nodeChange = false;
        for (Invoker invokerRemote : routerInfo.getInvokers()) {
          for (Invoker invokerLocal : invokers.get()) {
            if (invokerRemote.equals(invokerLocal)) {
              nodeChange = false;
              break;
            } else {
              nodeChange = true;
            }
          }
          if (nodeChange) {
            break;
          }
        }

        if (nodeChange) {
          log.info(
              "remoting invokers address updated. NEW : {} , OLD: {}",
              routerInfo.getInvokers(),
              invokers.get());
          invokers.set(routerInfo.getInvokers());
        }
      }
    }
  }

  /**
   * for admin
   *
   * @return addr
   */
  public String getMaintainerAddr() {
    return this.getAddrFromMaintainer();
  }

  /**
   * for user
   *
   * @return
   */
  public LoadBalance getLoadBalance() {
    return lb.get();
  }

  /**
   * for user
   *
   * @return
   */
  public Invoker getInvoker(LoadBalance loadbalance, List<Invoker> invokers, String key) {
    if (loadbalance == null || invokers == null) {
      return null;
    }

    return loadbalance.doSelect(invokers, key);
  }

  public List<Invoker> getInvokers() {
    return invokers.get();
  }

  private String getAddrFromMaintainer() {
    return maintainer.get();
  }

  private String getAddrFromInvoker() {
    String addr = null;

    List<Invoker> invokers = this.invokers.get();
    if (invokers != null) {
      if (invokersRetryIndex < invokers.size()) {
        addr = invokers.get(invokersRetryIndex).getAddr();
        invokersRetryIndex++;
      } else {
        invokersRetryIndex = 0;
        addr = invokers.get(invokersRetryIndex).getAddr();
      }
    }

    return addr;
  }

  private String getAddrFromServerAddr() throws MyberryClientException {
    this.checkServerAddrs();

    String addr = null;
    this.initServerAddrs();
    if (serverAddrRetryIndex < this.serverAddrs.size()) {
      addr = this.serverAddrs.get(serverAddrRetryIndex);
      serverAddrRetryIndex++;
    } else {
      serverAddrRetryIndex = 0;
      addr = this.serverAddrs.get(serverAddrRetryIndex);
    }

    return addr;
  }

  public void checkServerAddrs() throws MyberryClientException {
    if (MixAll.isBlank(this.clientConfig.getServerAddr())) {
      throw new MyberryClientException("serverAddr is null");
    }
  }

  public boolean isSingleInstance() throws MyberryClientException {
    this.checkServerAddrs();
    this.initServerAddrs();
    return serverAddrs.size() == 1;
  }

  private void initServerAddrs() {
    if (this.serverAddrs == null) {
      this.serverAddrs = Arrays.asList(this.clientConfig.getServerAddr().split(","));
    }
  }
}
