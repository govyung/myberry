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
package org.myberry.client.admin;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.myberry.client.exception.MyberryClientException;
import org.myberry.client.exception.MyberryServerException;
import org.myberry.common.protocol.body.CRComponentData;
import org.myberry.common.protocol.body.NSComponentData;
import org.myberry.common.strategy.StrategyDate;
import org.myberry.remoting.exception.RemotingException;

@Ignore
public class AdminClientTest {

  private static DefaultAdminClient defaultAdminClient;

  @BeforeClass
  public static void setup() throws MyberryClientException {
    defaultAdminClient = new DefaultAdminClient();
    defaultAdminClient.setPassword("foobared");
    defaultAdminClient.setServerAddr("192.168.1.3:8085,192.168.1.3:8086,192.168.1.3:8087");
    defaultAdminClient.start();
  }

  @Test
  public void createComponent1()
      throws RemotingException, InterruptedException, MyberryServerException {
    CRComponentData cr = new CRComponentData();
    cr.setKey("key1");
    cr.setExpression(
        "[#time(day) 9 #sid(2) #sid(1) #sid(0) m #incr(4) #incr(3) #incr(2) #incr(1) #incr(0) $dynamic(hello) #rand(3)]");

    SendResult sendResult = defaultAdminClient.createComponent(cr);
    System.out.println(sendResult);
    assertEquals(SendStatus.SEND_OK, sendResult.getSendStatus());
  }

  @Test
  public void createComponent2()
      throws RemotingException, InterruptedException, MyberryServerException {
    NSComponentData ns = new NSComponentData();
    ns.setKey("key2");
    ns.setValue(100);
    ns.setStepSize(5);
    ns.setResetType(StrategyDate.TIME_DAY);

    SendResult sendResult = defaultAdminClient.createComponent(ns);
    System.out.println(sendResult);
  }

  @Test
  public void queryAllComponent()
      throws RemotingException, InterruptedException, MyberryServerException {
    SendResult sendResult = defaultAdminClient.queryAllComponent();
    System.out.println(sendResult);
  }

  @AfterClass
  public static void close() {
    defaultAdminClient.shutdown();
  }
}
