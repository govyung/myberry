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
package org.myberry.store.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import org.myberry.store.MappedFile;
import org.myberry.store.common.LoggerName;
import org.myberry.store.config.StoreConfig;
import org.myberry.store.config.StorePathConfigHelper;
import org.myberry.store.util.MappingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhysicalFile {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  private final StoreConfig storeConfig;
  private final MappedFile mappedFile;
  private final MappedByteBuffer mappedByteBuffer;
  private final StoreHeader storeHeader;
  private final StoreComponent storeComponent;

  public PhysicalFile(final StoreConfig storeConfig) throws IOException {
    String storeFileName =
        StorePathConfigHelper.getStoreFilePath(storeConfig.getStoreRootDir())
            + File.separator
            + StoreConfig.MYBERRY_STORE_FILE_NAME;

    this.storeConfig = storeConfig;
    this.mappedFile = new MappedFile(storeFileName, storeConfig.getFileSize());
    this.mappedByteBuffer = this.mappedFile.getMappedByteBuffer();

    ByteBuffer byteBuffer = mappedByteBuffer.slice();
    this.storeHeader = new StoreHeader(byteBuffer);
    this.storeComponent = new StoreComponent(mappedByteBuffer);
  }

  public void addComponent(final AbstractComponent abstractComponent) {
    abstractComponent.setPhyOffset(mappedByteBuffer.position());
    storeComponent.write(abstractComponent);
    storeComponent.addMap(abstractComponent);

    if (storeHeader.getBeginPhyoffset() == 0) {
      storeHeader.setBeginPhyoffset(StoreHeader.STORE_HEADER_SIZE);
    }
    storeHeader.setEndPhyoffset(mappedByteBuffer.position());
    storeHeader.incrComponentCount();

    mappedFile.flush();
  }

  public void loadHeader() {
    storeHeader.load();

    long currentTimeMillis = System.currentTimeMillis();
    storeHeader.setBeginTimestamp(currentTimeMillis);
    storeHeader.setEndTimestamp(currentTimeMillis);

    if (storeHeader.getRunningMode() == 0) {
      int runningModeMapping = MappingUtils.getRunningModeMapping(storeConfig.getRunningMode());
      storeHeader.setRunningMode(runningModeMapping);
    }

    mappedByteBuffer.position(StoreHeader.STORE_HEADER_SIZE);
  }

  public void loadComponent() {
    if (storeHeader.getBeginPhyoffset() == 0) {
      return;
    }

    int position = this.mappedByteBuffer.position();
    if (position < this.storeHeader.getBeginPhyoffset()) {
      createNewComponent();
      loadComponent();
    } else if (position == this.storeHeader.getBeginPhyoffset()) {
      createNewComponent();
    } else {
      log.warn(
          "myberry store IndexOutOfBoundsException: mappedByteBuffer position={}, lastOffset={}",
          position,
          this.storeHeader.getEndPhyoffset());
      System.exit(1);
    }
  }

  private void createNewComponent() {
    AbstractComponent abstractComponent =
        MappingUtils.getComponentMapping(storeHeader.getRunningMode());

    try {
      storeComponent.load(abstractComponent);
      storeComponent.addMap(abstractComponent);
      log.info("add cache success: key = {}", abstractComponent.getKey());
    } catch (Exception e) {
      log.error("createNewComponent error: ", e.getMessage());
    }
    log.debug("load: {}", abstractComponent);
  }

  public void save() {
    mappedFile.flush();
  }

  public void unload() {
    storeHeader.setEndTimestamp(System.currentTimeMillis());
    mappedFile.destroy();
  }

  public boolean isWriteFull(int size) {
    if (mappedByteBuffer.capacity() - mappedByteBuffer.position() >= size) {
      return false;
    } else {
      return true;
    }
  }

  public void updateBufferLong(int index, long value) {
    mappedByteBuffer.putLong(index, value);
  }

  public long getMbid() {
    return storeHeader.getMbid();
  }

  public long getEpoch() {
    return storeHeader.getEpoch();
  }

  public int getMaxSid() {
    return storeHeader.getMaxSid();
  }

  public int getMySid() {
    return storeHeader.getMySid();
  }

  public int getComponentCount() {
    return storeHeader.getComponentCount();
  }

  public int getRunningMode() {
    return storeHeader.getRunningMode();
  }

  public boolean isExistKey(String key) {
    return storeComponent.isExistKey(key);
  }

  public void incrMbid() {
    storeHeader.incrMbid();
  }

  public int getLastOffset() {
    return mappedByteBuffer.position();
  }

  public AbstractComponent getComponent(String key) {
    return storeComponent.getComponent(key);
  }

  public ConcurrentMap<String, AbstractComponent> getComponentMap() {
    return storeComponent.getComponentMap();
  }

  public byte[] getSyncByteBuffer(List<AbstractComponent> abstractComponents, int bufferSize) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
    for (AbstractComponent abstractComponent : abstractComponents) {
      storeComponent.write(abstractComponent, byteBuffer);
    }
    return byteBuffer.array();
  }

  public byte[] getComponentByteArray(int offset) {
    return StoreComponent.get(
        this.storeHeader.getByteBuffer(), offset, new byte[mappedByteBuffer.position() - offset]);
  }

  public List<AbstractComponent> getComponentByteBuffer(byte[] src) {
    try {
      List<AbstractComponent> list = new ArrayList<>();
      ByteBuffer byteBuffer = ByteBuffer.wrap(src);

      do {
        AbstractComponent abstractComponent =
            MappingUtils.getComponentMapping(storeHeader.getRunningMode());
        storeComponent.load(abstractComponent, byteBuffer);
        list.add(abstractComponent);
      } while (byteBuffer.position() != byteBuffer.limit());

      return list;
    } catch (Exception e) {
      log.error("getComponentByteBuffer error: ", e.getMessage());
    }

    return null;
  }

  public void setComponentByteBuffer(byte[] src) {
    this.storeHeader.setBeginPhyoffset(mappedByteBuffer.position());
    this.storeHeader.getByteBuffer().position(mappedByteBuffer.position());
    StoreComponent.put(this.storeHeader.getByteBuffer(), this.getLastOffset(), src);
    mappedFile.flush();
    this.loadComponent();
  }

  public void setMySid(int mySid) {
    this.storeHeader.setMySid(mySid);
    mappedFile.flush();
  }

  public void setMaxSid(int maxSid) {
    this.storeHeader.setMaxSid(maxSid);
    mappedFile.flush();
  }

  public void setEpoch(long epoch) {
    this.storeHeader.setEpoch(epoch);
    mappedFile.flush();
  }

  public void setComponentCount(int componentCount) {
    this.storeHeader.setComponentCount(componentCount);
    mappedFile.flush();
  }
}
