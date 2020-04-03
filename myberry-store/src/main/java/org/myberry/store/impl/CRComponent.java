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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CRComponent extends AbstractComponent {

  // FIXED_FIELD_SIZE = createTimeHeader + updateTimeHeader + phyOffsetHeader +
  // incrNumberHeader + statusHeader +
  // keyLengthHeader + expressionLengthHeader
  public static final int FIXED_FIELD_SIZE = 44;

  public static int createTimeHeader = 0;
  public static int updateTimeHeader = 8;
  public static int phyOffsetHeader = 16;
  public static int incrNumberHeader = 24;
  public static int statusHeader = 32;

  private long createTime;
  private long updateTime;
  private long phyOffset;
  private AtomicLong incrNumber = new AtomicLong();
  private int status;
  private int keyLength;
  private String key;
  private int expressionLength;
  private String expression;

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

  public long getIncrNumber() {
    return incrNumber.get();
  }

  public void setIncrNumber(long incrNumber) {
    this.incrNumber.set(incrNumber);
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
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

  public int getExpressionLength() {
    return expressionLength;
  }

  public void setExpressionLength(int expressionLength) {
    this.expressionLength = expressionLength;
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public Lock getLock() {
    return lock;
  }

  public long incrementAndGet() {
    return incrNumber.incrementAndGet();
  }

  public void resetIncrNumber() {
    incrNumber.set(0);
  }

  @Override
  public String toString() {
    return new StringBuilder() //
        .append("CRComponent [") //
        .append("createTime=") //
        .append(createTime) //
        .append(", updateTime=") //
        .append(updateTime) //
        .append(", phyOffset=") //
        .append(phyOffset) //
        .append(", incrNumber=") //
        .append(incrNumber) //
        .append(", status=") //
        .append(status) //
        .append(", keyLength=") //
        .append(keyLength) //
        .append(", key='") //
        .append(key) //
        .append('\'') //
        .append(", expressionLength=") //
        .append(expressionLength) //
        .append(", expression='") //
        .append(expression) //
        .append('\'') //
        .append(']') //
        .toString();
  }
}
