/*
* Copyright 2018 gaoyang.  All rights reserved.
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
package org.myberry.client.user;

public class PullResult {

  private PullStatus pullStatus;
  private String key;
  private String newId;
  private String remark;

  public PullResult(PullStatus pullStatus, String key, String newId) {
    this.pullStatus = pullStatus;
    this.key = key;
    this.newId = newId;
  }

  public PullStatus getPullStatus() {
    return pullStatus;
  }

  public void setPullStatus(PullStatus pullStatus) {
    this.pullStatus = pullStatus;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getNewId() {
    return newId;
  }

  public void setNewId(String newId) {
    this.newId = newId;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  @Override
  public String toString() {
    return new StringBuilder() //
        .append("PullResult [pullStatus=") //
        .append(pullStatus) //
        .append(", key=") //
        .append(key) //
        .append(", newId=") //
        .append(newId) //
        .append(", remark=") //
        .append(remark) //
        .append("]") //
        .toString();
  }
}
