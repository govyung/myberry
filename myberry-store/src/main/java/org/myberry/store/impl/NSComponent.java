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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NSComponent extends AbstractComponent {

  // FIXED_FIELD_SIZE = createTimeHeader + updateTimeHeader + phyOffsetHeader +
  // mbidHeader + statusHeader + initValueHeader +
  // currentValueHeader + stepSizeHeader + resetTypeHeader + keyLengthHeader
  public static final int FIXED_FIELD_SIZE = 56;

  public static int createTimeHeader = 0;
  public static int updateTimeHeader = 8;
  public static int phyOffsetHeader = 16;
  public static int mbidHeader = 24;
  public static int statusHeader = 32;
  public static int initValueHeader = 36;
  public static int currentValueHeader = 40;
  public static int stepSizeHeader = 44;
  public static int resetTypeHeader = 48;
  public static int keyLengthHeader = 52;

  private long createTime;
  private long updateTime;
  private long phyOffset;
  private AtomicLong mbid = new AtomicLong();
  private int status;
  private int initValue;
  private AtomicInteger currentValue = new AtomicInteger(0);
  private int stepSize;
  private int resetType;
  private int keyLength;
  private String key;

  private final Lock lock = new ReentrantLock();

  public long getCreateTime() {
    return createTime;
  }

  public void setCreateTime(long createTime) {
    this.createTime = createTime;
  }

  public long getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(long updateTime) {
    this.updateTime = updateTime;
  }

  public long getPhyOffset() {
    return phyOffset;
  }

  public void setPhyOffset(long phyOffset) {
    this.phyOffset = phyOffset;
  }

  public long getMbid() {
    return mbid.get();
  }

  public void setMbid(long mbid) {
    this.mbid.set(mbid);
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public int getInitValue() {
    return initValue;
  }

  public void setInitValue(int initValue) {
    this.initValue = initValue;
  }

  public int getCurrentValue() {
    return currentValue.get();
  }

  public void setCurrentValue(int currentValue) {
    this.currentValue.set(currentValue);
  }

  public int getStepSize() {
    return stepSize;
  }

  public void setStepSize(int stepSize) {
    this.stepSize = stepSize;
  }

  public int getResetType() {
    return resetType;
  }

  public void setResetType(int resetType) {
    this.resetType = resetType;
  }

  public int getKeyLength() {
    return keyLength;
  }

  public void setKeyLength(int keyLength) {
    this.keyLength = keyLength;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public void resetCurrentValue() {
    currentValue.set(initValue);
  }

  public long incrementMbid() {
    return mbid.getAndIncrement();
  }

  public Lock getLock() {
    return lock;
  }

  @Override
  public String toString() {
    return new StringBuilder() //
        .append("NSComponent [") //
        .append("createTime=") //
        .append(createTime) //
        .append(", updateTime=") //
        .append(updateTime) //
        .append(", phyOffset=") //
        .append(phyOffset) //
        .append(", mbid=") //
        .append(mbid) //
        .append(", status=") //
        .append(status) //
        .append(", initValue=") //
        .append(initValue) //
        .append(", currentValue=") //
        .append(currentValue) //
        .append(", stepSize=") //
        .append(stepSize) //
        .append(", resetType=") //
        .append(resetType) //
        .append(", keyLength=") //
        .append(keyLength) //
        .append(", key='") //
        .append(key) //
        .append('\'') //
        .append(']') //
        .toString();
  }
}
