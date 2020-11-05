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
package org.myberry.client.admin;

import java.lang.reflect.Field;
import java.util.List;
import org.myberry.common.protocol.body.admin.CRComponentData;
import org.myberry.common.protocol.body.admin.NSComponentData;

public class SendResult {

  private SendStatus sendStatus;
  private String remark;

  private String key;
  private List<CRComponentData> componentsOfCR;

  private List<NSComponentData> componentsOfNS;

  public SendResult(SendStatus sendStatus) {
    this.sendStatus = sendStatus;
  }

  public SendResult(SendStatus sendStatus, String key) {
    this.sendStatus = sendStatus;
    this.key = key;
  }

  public SendStatus getSendStatus() {
    return sendStatus;
  }

  public void setSendStatus(SendStatus sendStatus) {
    this.sendStatus = sendStatus;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<CRComponentData> getComponentsOfCR() {
    return componentsOfCR;
  }

  public void setComponentsOfCR(List<CRComponentData> componentsOfCR) {
    this.componentsOfCR = componentsOfCR;
  }

  public List<NSComponentData> getComponentsOfNS() {
    return componentsOfNS;
  }

  public void setComponentsOfNS(List<NSComponentData> componentsOfNS) {
    this.componentsOfNS = componentsOfNS;
  }

  @Override
  public String toString() {
    Class<?> clz = this.getClass();
    Field[] fields = clz.getDeclaredFields();
    StringBuilder builder = new StringBuilder();
    builder.append("SendResult [");
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
