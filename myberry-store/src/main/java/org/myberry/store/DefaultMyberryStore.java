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
package org.myberry.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Map;
import org.myberry.common.Component;
import org.myberry.common.RunningMode;
import org.myberry.common.SystemClock;
import org.myberry.store.common.LoggerName;
import org.myberry.store.config.StoreConfig;
import org.myberry.store.config.StorePathConfigHelper;
import org.myberry.store.impl.AbstractStoreService;
import org.myberry.store.impl.CRStoreService;
import org.myberry.store.impl.NSStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMyberryStore implements MyberryStore {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  private volatile boolean shutdown = false;

  private final AbstractStoreService abstractStoreService;
  private final StoreConfig storeConfig;
  private RandomAccessFile lockFile;

  private FileLock lock;

  private final SystemClock systemClock = SystemClock.getInstance();

  public DefaultMyberryStore(final StoreConfig storeConfig) throws IOException {
    this.storeConfig = storeConfig;

    if (RunningMode.CR.getRunningName().equals(storeConfig.getRunningMode())) {
      this.abstractStoreService = new CRStoreService(storeConfig);
    } else if (RunningMode.NS.getRunningName().equals(storeConfig.getRunningMode())) {
      this.abstractStoreService = new NSStoreService(storeConfig);
    } else {
      this.abstractStoreService = new CRStoreService(storeConfig);
    }
    this.initProcessLock();
  }

  @Override
  public AdminManageResult addComponent(Object... obj) {
    return abstractStoreService.addComponent(obj);
  }

  @Override
  public List<Component> queryAllComponent() {
    return abstractStoreService.queryAllComponent();
  }

  @Override
  public long getMbidFromDisk() {
    return abstractStoreService.getMbid();
  }

  @Override
  public long getEpochFromDisk() {
    return abstractStoreService.getEpoch();
  }

  @Override
  public int getMaxSidFromDisk() {
    return abstractStoreService.getMaxSid();
  }

  @Override
  public int getMySidFromDisk() {
    return abstractStoreService.getMySid();
  }

  @Override
  public int getComponentCountFromDisk() {
    return abstractStoreService.getComponentCount();
  }

  @Override
  public int getRunningModeFromDisk() {
    return abstractStoreService.getRunningMode();
  }

  @Override
  public PullIdResult getNewId(String key, Map<String, String> attachments) {
    return abstractStoreService.getNewId(key, attachments);
  }

  @Override
  public StoreConfig getStoreConfig() {
    return storeConfig;
  }

  @Override
  public long now() {
    return this.systemClock.now();
  }

  @Override
  public int getLastOffset() {
    return abstractStoreService.getLastOffset();
  }

  @Override
  public byte[] getSyncByteBuffer(int offset) {
    return abstractStoreService.getSyncByteBuffer(offset);
  }

  @Override
  public void setSyncByteBuffer(byte[] src) {
    this.abstractStoreService.setSyncByteBuffer(src);
  }

  @Override
  public void setMySid(int mySid) {
    this.abstractStoreService.setMySid(mySid);
  }

  @Override
  public void setMaxSid(int maxSid) {
    this.abstractStoreService.setMaxSid(maxSid);
  }

  @Override
  public void setEpoch(long epoch) {
    this.abstractStoreService.setEpoch(epoch);
  }

  @Override
  public void setComponentCount(int componentCount) {
    this.abstractStoreService.setComponentCount(componentCount);
  }

  @Override
  public void start() throws Exception {
    this.startProcessLock();
    this.abstractStoreService.start();
  }

  @Override
  public void shutdown() {
    if (!this.shutdown) {
      this.shutdown = true;
      this.abstractStoreService.shutdown();
    }

    this.closeProcessLock();
  }

  @Override
  public boolean isOSPageCacheBusy() {
    long begin = abstractStoreService.getBeginTimeInLock();
    long diff = this.systemClock.now() - begin;

    return diff < 10000000 && diff > this.storeConfig.getOsPageCacheBusyTimeOutMills();
  }

  private void initProcessLock() throws IOException {
    File file = new File(StorePathConfigHelper.getLockFile(storeConfig.getStoreRootDir()));
    MappedFile.ensureDirOK(file.getParent());
    lockFile = new RandomAccessFile(file, "rw");
  }

  private void startProcessLock() throws IOException {
    lock = lockFile.getChannel().tryLock(0, 1, false);

    if (lock == null || lock.isShared() || !lock.isValid()) {
      throw new RuntimeException("Lock failed, Myberry already started");
    }

    lockFile.getChannel().write(ByteBuffer.wrap("lock".getBytes()));
    lockFile.getChannel().force(true);
  }

  private void closeProcessLock() {
    if (lockFile != null && lock != null) {
      try {
        lock.release();
        lockFile.close();
      } catch (IOException e) {
        log.error("Myberry file close fail: ", e.getMessage());
      }
    }
  }

  @Override
  public boolean hasShutdown() {
    return shutdown;
  }

  public AbstractStoreService getAbstractStoreService() {
    return abstractStoreService;
  }
}
