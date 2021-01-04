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
package org.myberry.common.protocol.body.admin;

import java.lang.reflect.Field;
import org.myberry.common.Component;
import org.myberry.common.codec.LightCodec;
import org.myberry.common.codec.MessageLite;
import org.myberry.common.codec.annotation.SerialField;

public class NSComponentData implements MessageLite, Component {

  public static final int CODE = 2;

  @SerialField(ordinal = 0)
  private String key;

  @SerialField(ordinal = 1)
  private int value;

  @SerialField(ordinal = 2)
  private int stepSize;

  @SerialField(ordinal = 3)
  private int resetType;

  @SerialField(ordinal = 4)
  private int code;

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
  public byte[] encode() {
    return LightCodec.toBytes(this);
  }

  @Override
  public String toString() {
    Class<?> clz = this.getClass();
    Field[] fields = clz.getDeclaredFields();
    StringBuilder builder = new StringBuilder();
    builder.append("NSComponentData [");
    for (int i = 0; i < fields.length; i++) {
      fields[i].setAccessible(true);
      Object obj = null;
      try {
        obj = fields[i].get(this);
      } catch (IllegalAccessException e) {
      }
      if (obj == null) {
        continue;
      }

      if (i != 0) {
        builder.append(", ");
      }
      builder.append(fields[i].getName());
      builder.append("=");
      builder.append(obj);
    }
    return builder.append("]").toString();
  }
}
