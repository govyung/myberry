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
package org.myberry.server.routeinfo;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myberry.common.ServerConfig;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.server.quarum.message.Sync;

public class RouteInfoManagerTest {

  private static RouteInfoManager routeInfoManager;
  private static ServerConfig serverConfig;

  @BeforeClass
  public static void init() {
    routeInfoManager = new RouteInfoManager();
    serverConfig = new ServerConfig();
    serverConfig.setClusterName("testCluster");
  }

  @Test
  public void test1RegisterLearner() {
    routeInfoManager.registerLearner(serverConfig.getClusterName(), 1, "127.0.0.1:5551", 1);
    routeInfoManager.registerLearner(serverConfig.getClusterName(), 1, "127.0.0.1:5551", 1);
    routeInfoManager.registerLearner(serverConfig.getClusterName(), 2, "127.0.0.1:5552", 1);
    Map<Integer, Invoker> learnerTable = routeInfoManager.getLearnerTable();
    Assert.assertEquals(2, learnerTable.size());
    Assert.assertEquals("127.0.0.1:5551", learnerTable.get(1).getAddr());
    Assert.assertEquals("127.0.0.1:5552", learnerTable.get(2).getAddr());
  }

  @Test
  public void test2UpdateLearner() {
    Map<Integer, Invoker> map = new HashMap<>();
    map.put(1, new Invoker("127.0.0.1:5551", 3));
    map.put(3, new Invoker("127.0.0.1:5553", 1));
    Sync sync = Sync.create(0, 0, 0, 0, "", map, null, 0, null);

    Map<Integer, Invoker> syncInvokerTable = sync.getInvokers();
    boolean changed =
        routeInfoManager.updateLearner(serverConfig.getClusterName(), syncInvokerTable);
    Assert.assertEquals(true, changed);
  }

  @Test
  public void test3UnregisterLearner() {
    Map<Integer, Invoker> learnerTable = routeInfoManager.getLearnerTable();
    routeInfoManager.unregisterLearner(serverConfig.getClusterName(), 1);
    Assert.assertEquals(1, learnerTable.size());
    Assert.assertEquals("127.0.0.1:5553", learnerTable.get(3).getAddr());
  }
}
