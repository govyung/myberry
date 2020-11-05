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
package org.myberry.server.impl;

public class PullIdResult {

  private int respCode;
  private int produceCode;
  private String remark;

  private String newId;

  private int start;
  private int end;
  private int synergyId;

  public PullIdResult() {}

  public PullIdResult(int respCode, String remark) {
    this.respCode = respCode;
    this.remark = remark;
  }

  public PullIdResult(int respCode, int produceCode, String newId) {
    this.respCode = respCode;
    this.produceCode = produceCode;
    this.newId = newId;
  }

  public PullIdResult(int respCode, int produceCode, int start, int end, int synergyId) {
    this.respCode = respCode;
    this.produceCode = produceCode;
    this.start = start;
    this.end = end;
    this.synergyId = synergyId;
  }

  public int getRespCode() {
    return respCode;
  }

  public void setRespCode(int respCode) {
    this.respCode = respCode;
  }

  public int getProduceCode() {
    return produceCode;
  }

  public void setProduceCode(int produceCode) {
    this.produceCode = produceCode;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
  }

  public String getNewId() {
    return newId;
  }

  public void setNewId(String newId) {
    this.newId = newId;
  }

  public int getStart() {
    return start;
  }

  public void setStart(int start) {
    this.start = start;
  }

  public int getEnd() {
    return end;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public int getSynergyId() {
    return synergyId;
  }

  public void setSynergyId(int synergyId) {
    this.synergyId = synergyId;
  }
}
