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
package org.myberry.client.router;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.myberry.client.ClientConfig;
import org.myberry.client.router.loadbalance.LoadBalance;
import org.myberry.client.router.loadbalance.RoundRobinLoadBalance;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.common.loadbalance.LoadBalanceName;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultRouterTest {

  private static DefaultRouter defaultRouter;

  @BeforeClass
  public static void setup() {
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.setServerAddr("127.0.0.1:8085,127.0.0.1:8086,127.0.0.1:8087");
    defaultRouter = new DefaultRouter(clientConfig);
  }

  @Test
  public void a_testForUpdateAddr() throws Exception {
    String addr1 = defaultRouter.fetchServerAddr();
    Assert.assertEquals("127.0.0.1:8085", addr1);
    String addr2 = defaultRouter.fetchServerAddr();
    Assert.assertEquals("127.0.0.1:8086", addr2);
    String addr3 = defaultRouter.fetchServerAddr();
    Assert.assertEquals("127.0.0.1:8087", addr3);
    String addr4 = defaultRouter.fetchServerAddr();
    Assert.assertEquals("127.0.0.1:8085", addr4);
  }

  @Test
  public void b_testForSetRouterInfo() throws Exception {
    List<Invoker> invokers = new ArrayList<>();
    invokers.add(new Invoker("127.0.0.1:8086", 1));
    invokers.add(new Invoker("127.0.0.1:8087", 2));

    RouterInfo routerInfo = new RouterInfo();
    routerInfo.setLoadBalanceName(LoadBalanceName.RANDOM_LOADBALANCE);
    routerInfo.setMaintainer("127.0.0.1:8085");
    routerInfo.setInvokers(invokers);

    defaultRouter.setRouterInfo(routerInfo);
  }

  @Test
  public void c_testForGetMaintainerAddr() throws Exception {
    String maintainerAddr = defaultRouter.getMaintainerAddr();
    Assert.assertEquals("127.0.0.1:8085", maintainerAddr);
  }

  @Test
  public void d_testForGetInvokerAddr() throws Exception {
    LoadBalance loadBalance = defaultRouter.getLoadBalance();
    Invoker invokerAddr1 =
        defaultRouter.getInvoker(loadBalance, defaultRouter.getInvokers(), "key1");
    Assert.assertNotNull(invokerAddr1);
    Invoker invokerAddr2 =
        defaultRouter.getInvoker(loadBalance, defaultRouter.getInvokers(), "key1");
    Assert.assertNotNull(invokerAddr2);
    Invoker invokerAddr3 =
        defaultRouter.getInvoker(loadBalance, defaultRouter.getInvokers(), "key1");
    Assert.assertNotNull(invokerAddr3);
    Invoker invokerAddr4 =
        defaultRouter.getInvoker(loadBalance, defaultRouter.getInvokers(), "key1");
    Assert.assertNotNull(invokerAddr4);
    Invoker invokerAddr5 =
        defaultRouter.getInvoker(loadBalance, defaultRouter.getInvokers(), "key1");
    Assert.assertNotNull(invokerAddr5);
    Invoker invokerAddr6 =
        defaultRouter.getInvoker(loadBalance, defaultRouter.getInvokers(), "key1");
    Assert.assertNotNull(invokerAddr6);
  }

  @Test
  public void e_testForRouterChange() throws Exception {
    List<Invoker> invokers = new ArrayList<>();
    invokers.add(new Invoker("127.0.0.1:8086", 1));
    invokers.add(new Invoker("127.0.0.1:8087", 2));

    RouterInfo routerInfo = new RouterInfo();
    routerInfo.setLoadBalanceName(LoadBalanceName.CONSISTENTHASH_LOADBALANCE);
    routerInfo.setMaintainer("127.0.0.1:8085");
    routerInfo.setInvokers(invokers);

    defaultRouter.setRouterInfo(routerInfo);

    String addr1 = defaultRouter.fetchServerAddr();
    Assert.assertEquals("127.0.0.1:8085", addr1);
    String addr2 = defaultRouter.fetchServerAddr();
    Assert.assertNotEquals("127.0.0.1:8086", addr2);
  }

  @Test
  public void f_testForMaintainerChange() throws Exception {
    List<Invoker> invokers = new ArrayList<>();
    invokers.add(new Invoker("127.0.0.1:8086", 1));
    invokers.add(new Invoker("127.0.0.1:8087", 2));

    RouterInfo routerInfo = new RouterInfo();
    routerInfo.setLoadBalanceName(LoadBalanceName.CONSISTENTHASH_LOADBALANCE);
    routerInfo.setMaintainer("127.0.0.1:8089");
    routerInfo.setInvokers(invokers);

    defaultRouter.setRouterInfo(routerInfo);
    String addr1 = defaultRouter.fetchServerAddr();
    Assert.assertEquals("127.0.0.1:8089", addr1);
  }

  @Test
  public void g_testForInvokersChange() throws Exception {
    // addr change
    List<Invoker> invokers = new ArrayList<>();
    invokers.add(new Invoker("127.0.0.1:8086", 1));
    invokers.add(new Invoker("127.0.0.1:8088", 2));

    RouterInfo routerInfo = new RouterInfo();
    routerInfo.setLoadBalanceName(LoadBalanceName.CONSISTENTHASH_LOADBALANCE);
    routerInfo.setMaintainer("127.0.0.1:8089");
    routerInfo.setInvokers(invokers);
    defaultRouter.setRouterInfo(routerInfo);

    List<Invoker> invokerList = defaultRouter.getInvokers();
    boolean updated = false;
    for (Invoker ink : invokerList) {
      if ("127.0.0.1:8088".equals(ink.getAddr())) {
        updated = true;
        break;
      }
    }

    Assert.assertEquals(true, updated);

    // weight change
    List<Invoker> invokers2 = new ArrayList<>();
    invokers2.add(new Invoker("127.0.0.1:8086", 5));
    invokers2.add(new Invoker("127.0.0.1:8088", 2));

    RouterInfo routerInfo2 = new RouterInfo();
    routerInfo2.setLoadBalanceName(LoadBalanceName.CONSISTENTHASH_LOADBALANCE);
    routerInfo2.setMaintainer("127.0.0.1:8089");
    routerInfo2.setInvokers(invokers2);
    defaultRouter.setRouterInfo(routerInfo2);

    List<Invoker> invokerList2 = defaultRouter.getInvokers();
    boolean updated2 = false;
    for (Invoker ink : invokerList2) {
      if ("127.0.0.1:8086".equals(ink.getAddr()) && 5 == ink.getWeight()) {
        updated2 = true;
        break;
      }
    }

    Assert.assertEquals(true, updated2);
  }

  @Test
  public void h_testForDefaultBalanceName() throws Exception {
    List<Invoker> invokers = new ArrayList<>();
    invokers.add(new Invoker("127.0.0.1:8086", 5));
    invokers.add(new Invoker("127.0.0.1:8088", 2));

    RouterInfo routerInfo = new RouterInfo();
    routerInfo.setLoadBalanceName("unknow");
    routerInfo.setMaintainer("127.0.0.1:8085");
    routerInfo.setInvokers(invokers);

    defaultRouter.setRouterInfo(routerInfo);
    Assert.assertEquals(
        RoundRobinLoadBalance.class.getSimpleName(),
        defaultRouter.getLoadBalance().getClass().getSimpleName());
  }

}
