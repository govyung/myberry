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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StoreHeader {

  public static final int STORE_HEADER_SIZE = 64;
  private static int beginTimestampHeader = 0;
  private static int endTimestampHeader = 8;
  private static int beginPhyoffsetHeader = 16;
  private static int endPhyoffsetHeader = 24;
  private static int mbidHeader = 32;
  private static int epochHeader = 40;
  private static int maxSidHeader = 48;
  private static int mySidHeader = 52;
  private static int componentCountHeader = 56;
  private static int produceModeHeader = 60;

  private final ByteBuffer byteBuffer;

  private AtomicLong beginTimestamp = new AtomicLong(0);
  private AtomicLong endTimestamp = new AtomicLong(0);
  private AtomicLong beginPhyoffset = new AtomicLong(0);
  private AtomicLong endPhyoffset = new AtomicLong(0);
  private AtomicLong mbid = new AtomicLong(0);
  private AtomicLong epoch = new AtomicLong(0);

  private AtomicInteger maxSid = new AtomicInteger();
  private AtomicInteger mySid = new AtomicInteger();
  private AtomicInteger componentCount = new AtomicInteger();
  private AtomicInteger produceMode = new AtomicInteger();

  public StoreHeader(final ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public void load() {
    this.setBeginTimestamp(byteBuffer.getLong(beginTimestampHeader));
    this.setEndTimestamp(byteBuffer.getLong(endTimestampHeader));
    this.setBeginPhyoffset(byteBuffer.getLong(beginPhyoffsetHeader));
    this.setEndPhyoffset(byteBuffer.getLong(endPhyoffsetHeader));
    this.setMbid(byteBuffer.getLong(mbidHeader));
    this.setEpoch(byteBuffer.getLong(epochHeader));
    this.setMaxSid(byteBuffer.getInt(maxSidHeader));
    this.setMySid(byteBuffer.getInt(mySidHeader));
    this.setComponentCount(byteBuffer.getInt(componentCountHeader));
    this.setProduceMode(byteBuffer.getInt(produceModeHeader));
  }

  public long getBeginTimestamp() {
    return beginTimestamp.get();
  }

  public void setBeginTimestamp(long beginTimestamp) {
    this.beginTimestamp.set(beginTimestamp);
    this.byteBuffer.putLong(beginTimestampHeader, beginTimestamp);
  }

  public long getEndTimestamp() {
    return endTimestamp.get();
  }

  public void setEndTimestamp(long endTimestamp) {
    this.endTimestamp.set(endTimestamp);
    this.byteBuffer.putLong(endTimestampHeader, endTimestamp);
  }

  public long getBeginPhyoffset() {
    return beginPhyoffset.get();
  }

  public void setBeginPhyoffset(long beginPhyoffset) {
    this.beginPhyoffset.set(beginPhyoffset);
    this.byteBuffer.putLong(beginPhyoffsetHeader, beginPhyoffset);
  }

  public long getEndPhyoffset() {
    return endPhyoffset.get();
  }

  public void setEndPhyoffset(long endPhyoffset) {
    this.endPhyoffset.set(endPhyoffset);
    this.byteBuffer.putLong(endPhyoffsetHeader, endPhyoffset);
  }

  public long getMbid() {
    return mbid.get();
  }

  public void setMbid(long mbid) {
    this.mbid.set(mbid);
    this.byteBuffer.putLong(mbidHeader, mbid);
  }

  public long getEpoch() {
    return epoch.get();
  }

  public void setEpoch(long epoch) {
    this.epoch.set(epoch);
    this.byteBuffer.putLong(epochHeader, epoch);
  }

  public int getMaxSid() {
    return maxSid.get();
  }

  public void setMaxSid(int maxSid) {
    this.maxSid.set(maxSid);
    this.byteBuffer.putInt(maxSidHeader, maxSid);
  }

  public int getMySid() {
    return mySid.get();
  }

  public void setMySid(int mySid) {
    this.mySid.set(mySid);
    this.byteBuffer.putInt(mySidHeader, mySid);
  }

  public int getComponentCount() {
    return componentCount.get();
  }

  public void setComponentCount(int componentCount) {
    this.componentCount.set(componentCount);
    this.byteBuffer.putInt(componentCountHeader, componentCount);
  }

  public int getProduceMode() {
    return produceMode.get();
  }

  public void setProduceMode(int produceMode) {
    this.produceMode.set(produceMode);
    this.byteBuffer.putInt(produceModeHeader, produceMode);
  }

  public ByteBuffer getByteBuffer() {
    return byteBuffer;
  }

  public void incrMbid() {
    this.byteBuffer.putLong(mbidHeader, mbid.incrementAndGet());
  }

  public void incrComponentCount() {
    this.byteBuffer.putInt(componentCountHeader, componentCount.incrementAndGet());
  }
}
