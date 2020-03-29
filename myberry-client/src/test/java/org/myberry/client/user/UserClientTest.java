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
package org.myberry.client.user;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class UserClientTest {

  private static DefaultUserClient defaultUserClient;

  @BeforeClass
  public static void setup() throws Exception {
    defaultUserClient = new DefaultUserClient();
    defaultUserClient.setServerAddr("192.168.1.3:8085,192.168.1.3:8086,192.168.1.3:8087");
    defaultUserClient.start();
  }

  @Test
  public void getSync() throws Exception {
    Map<String, String> map = new HashMap<>();
    map.put("hello", "AAA");
    map.put("world", "BBB");
    PullResult pull = defaultUserClient.pull("key1", map);
    System.out.println("Sync: " + pull);
    PullResult pull1 = defaultUserClient.pull("key1", map);
    System.out.println("Sync: " + pull1);
    PullResult pull2 = defaultUserClient.pull("key1", map);
    System.out.println("Sync: " + pull2);
    PullResult pull3 = defaultUserClient.pull("key1", map);
    System.out.println("Sync: " + pull3);
    PullResult pull4 = defaultUserClient.pull("key1", map);
    System.out.println("Sync: " + pull4);
    assertNotNull(pull.getNewId());
  }

  @Test
  public void getSyncTimesRetry() throws Exception {
    PullResult pull = defaultUserClient.pull("key1", 3000, 2);
    System.out.println("Sync: " + pull);
    assertNotNull(pull.getNewId());
  }

  @Test
  public void beComplicatedBy() throws Exception {
    long start = System.currentTimeMillis();

    for (int i = 0; i < 10000; i++) {
      Thread.sleep(50);
      PullResult pull = defaultUserClient.pull("key1");
    }
    System.out.println(System.currentTimeMillis() - start);
  }

  @Test
  public void getAsync() throws Exception {
    for (int i = 0; i < 3; i++) {
      defaultUserClient.pull(
          "key2",
          new PullCallback() {

            @Override
            public void onSuccess(PullResult pullResult) {
              System.out.println("Async: " + pullResult);
              assertNotNull(pullResult.getNewId());
            }

            @Override
            public void onException(Throwable e) {
              e.printStackTrace();
            }
          });
    }
    Thread.sleep(3000);
  }

  @Test
  public void getAsyncTimesRetry() throws Exception {
    defaultUserClient.pull(
        "key1",
        new PullCallback() {

          @Override
          public void onSuccess(PullResult pullResult) {
            System.out.println("Async: " + pullResult);
            assertNotNull(pullResult.getNewId());
          }

          @Override
          public void onException(Throwable e) {
            e.printStackTrace();
          }
        },
        99999,
        2);

    Thread.sleep(3000);
  }

  @AfterClass
  public static void close() {
    defaultUserClient.shutdown();
  }
}
