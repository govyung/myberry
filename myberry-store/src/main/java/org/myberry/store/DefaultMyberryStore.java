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
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import org.myberry.common.SystemClock;
import org.myberry.store.common.LoggerName;
import org.myberry.store.config.StoreConfig;
import org.myberry.store.config.StorePathConfigHelper;
import org.myberry.store.impl.FileService;
import org.myberry.store.impl.MappedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMyberryStore implements MyberryStore {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  private volatile boolean shutdown = false;

  private final StoreConfig storeConfig;
  private final FileService fileService;
  private RandomAccessFile lockFile;

  private FileLock lock;

  private final SystemClock systemClock = SystemClock.getInstance();
  private volatile long beginTimeInLock = 0;

  public DefaultMyberryStore(final StoreConfig storeConfig) throws IOException {
    this.storeConfig = storeConfig;
    this.fileService = new FileService(this);
    this.initProcessLock();
  }

  @Override
  public void addComponent(AbstractComponent abstractComponent) {
    fileService.addComponent(abstractComponent);
  }

  @Override
  public void removeComponent(String key) {}

  @Override
  public Collection<AbstractComponent> queryAllComponent() {
    return fileService.queryAllComponent();
  }

  @Override
  public long getMbidFromDisk() {
    return fileService.getBlockFile(0).getMbid();
  }

  @Override
  public long getEpochFromDisk() {
    return fileService.getBlockFile(0).getEpoch();
  }

  @Override
  public int getMaxSidFromDisk() {
    return fileService.getBlockFile(0).getMaxSid();
  }

  @Override
  public int getMySidFromDisk() {
    return fileService.getBlockFile(0).getMySid();
  }

  @Override
  public int getComponentCountFromDisk() {
    return fileService.getBlockFile(0).getComponentCount();
  }

  @Override
  public int getProduceModeFromDisk() {
    return fileService.getBlockFile(0).getProduceMode();
  }

  @Override
  public StoreConfig getStoreConfig() {
    return storeConfig;
  }

  @Override
  public ConcurrentMap<String, AbstractComponent> getComponentMap() {
    return fileService.getBlockFile(0).getComponentMap();
  }

  @Override
  public long now() {
    return this.systemClock.now();
  }

  @Override
  public int getLastOffset() {
    return fileService.getBlockFile(0).getLastOffset();
  }

  @Override
  public boolean isWriteFull(int size) {
    return fileService.getBlockFile(0).isWriteFull(size);
  }

  @Override
  public boolean isExistKey(String key) {
    return fileService.getBlockFile(0).isExistKey(key);
  }

  @Override
  public byte[] getSyncByteBuffer(int offset) {
    return fileService.getBlockFile(0).getComponentByteArray(offset);
  }

  @Override
  public void setSyncByteBuffer(byte[] src) {
    this.fileService.getBlockFile(0).setComponentByteBuffer(src);
  }

  @Override
  public void incrMbid() {
    this.fileService.getBlockFile(0).incrMbid();
  }

  @Override
  public void setMySid(int mySid) {
    this.fileService.getBlockFile(0).setMySid(mySid);
  }

  @Override
  public void setMaxSid(int maxSid) {
    this.fileService.getBlockFile(0).setMaxSid(maxSid);
  }

  @Override
  public void setEpoch(long epoch) {
    this.fileService.getBlockFile(0).setEpoch(epoch);
  }

  @Override
  public void setComponentCount(int componentCount) {
    this.fileService.getBlockFile(0).setComponentCount(componentCount);
  }

  @Override
  public void save() {
    this.fileService.getBlockFile(0).save();
  }

  @Override
  public void updateBufferLong(int index, long value) {
    this.fileService.getBlockFile(0).updateBufferLong(index, value);
  }

  @Override
  public void start() throws Exception {
    this.startProcessLock();
    this.fileService.load(storeConfig);
  }

  @Override
  public void shutdown() {
    if (!this.shutdown) {
      this.shutdown = true;
      this.fileService.unload();
    }

    this.closeProcessLock();
  }

  @Override
  public boolean isOSPageCacheBusy() {
    long diff = this.systemClock.now() - beginTimeInLock;

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

  @Override
  public void setBeginTimeInLock(long beginTimeInLock) {
    this.beginTimeInLock = beginTimeInLock;
  }
}
