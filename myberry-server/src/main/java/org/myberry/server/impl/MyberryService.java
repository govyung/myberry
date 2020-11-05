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
package org.myberry.server.impl;

import java.util.Map;
import org.myberry.common.SystemClock;
import org.myberry.store.MyberryStore;

public abstract class MyberryService {

  public abstract AdminManageResult addComponent(Object... obj);

  public abstract Object queryComponentList(int pageNo, int pageSize);

  public abstract PullIdResult getNewId(String key, Map<String, String> attachments);

  public abstract String getServiceName();

  private final SystemClock systemClock = SystemClock.getInstance();
  protected final MyberryStore myberryStore;

  public MyberryService(final MyberryStore myberryStore) {
    this.myberryStore = myberryStore;
  }

  public void start() {}

  public void shutdown() {}

  public boolean isAlwaysFlush() {
    return myberryStore.getStoreConfig().isAlwaysFlush();
  }

  public SystemClock getSystemClock() {
    return systemClock;
  }

  public String emptyString() {
    return "";
  }
}
