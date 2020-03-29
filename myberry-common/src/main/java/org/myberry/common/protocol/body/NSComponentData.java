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
package org.myberry.common.protocol.body;

import org.myberry.common.Component;
import org.myberry.remoting.protocol.RemotingSerializable;

public class NSComponentData extends RemotingSerializable implements Component {

  public static final int CODE = 2;

  private String key;
  private int value;
  private int stepSize;
  private int resetType;
  private int code;

  public static NSComponentData decode(final byte[] data, boolean nullCheck) {
    NSComponentData nsComponentData = decode(data, NSComponentData.class);
    if (!nullCheck) {
      return nsComponentData;
    }
    if (nsComponentData.isEmpty(nsComponentData.getKey())
        || nsComponentData.isEmpty(String.valueOf(nsComponentData.getValue()))
        || nsComponentData.isEmpty(String.valueOf(nsComponentData.getStepSize()))) {
      return null;
    } else {
      return nsComponentData;
    }
  }

  @Override
  public byte[] encode(boolean nullCheck) {
    if (!nullCheck) {
      return this.encode();
    }
    if (this.isEmpty(key)
        || this.isEmpty(String.valueOf(value))
        || this.isEmpty(String.valueOf(stepSize))) {
      return null;
    } else {
      return this.encode();
    }
  }

  private boolean isEmpty(String str) {
    if (str == null || str.equals("")) {
      return true;
    } else {
      return false;
    }
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
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

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("NSComponentData [key=")
        .append(key)
        .append(", value=")
        .append(value)
        .append(", stepSize=")
        .append(stepSize)
        .append(", resetType=")
        .append(resetType)
        .append("]")
        .toString();
  }
}
