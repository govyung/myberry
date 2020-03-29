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
package org.myberry.store;

import java.io.File;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.myberry.store.config.StoreConfig;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DefaultMyberryStoreTest {

  private static String testPath = new File("").getAbsolutePath();
  private static DefaultMyberryStore defaultMyberryStore;
  private static StoreConfig storeConfig;

  public static void main(String[] args) {
    storeConfig = new StoreConfig();
    storeConfig.setStorePath(testPath);
    deleteDir(new File(storeConfig.getStoreRootDir()));
  }

  @BeforeClass
  public static void init() throws Exception {
    storeConfig = new StoreConfig();
    storeConfig.setStorePath(testPath);
    defaultMyberryStore = new DefaultMyberryStore(storeConfig);
    defaultMyberryStore.start();
  }

  @Test
  public void test1() {
    defaultMyberryStore
        .getAbstractStoreService()
        .addComponent("key2", "[#time(day) 2 3 #sid(0) #sid(1) m z #incr(0)]");
  }

  @Test
  public void test2() throws InterruptedException {
    PullIdResult result = defaultMyberryStore.getAbstractStoreService().getNewId("key2", null);
    Assert.assertNotNull(result);
  }

  @AfterClass
  public static void destory() {
    defaultMyberryStore.shutdown();
  }

  private static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      String[] children = dir.list();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
          return false;
        }
      }
    }
    return dir.delete();
  }
}
