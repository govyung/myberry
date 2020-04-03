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
package org.myberry.store.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.myberry.common.Component;
import org.myberry.common.SystemClock;
import org.myberry.store.AdminManageResult;
import org.myberry.store.PullIdResult;
import org.myberry.store.config.StoreConfig;

public abstract class AbstractStoreService {

  public abstract AdminManageResult addComponent(Object... obj);

  public abstract List<Component> queryAllComponent();

  public abstract PullIdResult getNewId(String key, Map<String, String> attachments);

  public abstract String getServiceName();

  private final SystemClock systemClock = SystemClock.getInstance();
  protected final StoreConfig storeConfig;
  protected final PhysicalFile physicalFile;

  protected volatile long beginTimeInLock = 0;

  public AbstractStoreService(final StoreConfig storeConfig) throws IOException {
    this.storeConfig = storeConfig;
    this.physicalFile = new PhysicalFile(storeConfig);
  }

  public void start() {
    physicalFile.loadHeader();
    physicalFile.loadComponent();
  }

  public void shutdown() {
    physicalFile.unload();
  }

  public long getMbid() {
    return physicalFile.getMbid();
  }

  public long getEpoch() {
    return physicalFile.getEpoch();
  }

  public int getMaxSid() {
    return physicalFile.getMaxSid();
  }

  public int getMySid() {
    return physicalFile.getMySid();
  }

  public int getComponentCount() {
    return physicalFile.getComponentCount();
  }

  public int getRunningMode() {
    return physicalFile.getRunningMode();
  }

  public int getLastOffset() {
    return physicalFile.getLastOffset();
  }

  protected Collection<AbstractComponent> getComponentMap() {
    return physicalFile.getComponentMap().values();
  }

  public abstract byte[] getSyncByteBuffer(int offset);

  public void setSyncByteBuffer(byte[] src) {
    physicalFile.setComponentByteBuffer(src);
  }

  public void setMySid(int mySid) {
    physicalFile.setMySid(mySid);
  }

  public void setMaxSid(int maxSid) {
    physicalFile.setMaxSid(maxSid);
  }

  public void setEpoch(long epoch) {
    physicalFile.setEpoch(epoch);
  }

  public void setComponentCount(int componentCount) {
    physicalFile.setComponentCount(componentCount);
  }

  public boolean isAlwaysFlush() {
    return storeConfig.isAlwaysFlush();
  }

  public SystemClock getSystemClock() {
    return systemClock;
  }

  public long getBeginTimeInLock() {
    return beginTimeInLock;
  }

  public String emptyString() {
    return "";
  }
}
