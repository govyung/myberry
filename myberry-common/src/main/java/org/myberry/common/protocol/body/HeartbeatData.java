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
package org.myberry.common.protocol.body;

import java.util.List;
import org.myberry.common.codec.MessageLite;
import org.myberry.common.codec.annotation.SerialField;
import org.myberry.common.loadbalance.Invoker;

public class HeartbeatData implements MessageLite {

  @SerialField(ordinal = 0)
  private String loadBalanceName;

  @SerialField(ordinal = 1)
  private String maintainer;

  @SerialField(ordinal = 2)
  private List<Invoker> invokers;

  /** ----only for admin----> */
  @SerialField(ordinal = 3)
  private int monitorCode;

  @SerialField(ordinal = 4)
  private String clusterName;

  @SerialField(ordinal = 5)
  private String info;
  /** <----only for admin---- */
  public String getLoadBalanceName() {
    return loadBalanceName;
  }

  public void setLoadBalanceName(String loadBalanceName) {
    this.loadBalanceName = loadBalanceName;
  }

  public String getMaintainer() {
    return maintainer;
  }

  public void setMaintainer(String maintainer) {
    this.maintainer = maintainer;
  }

  public List<Invoker> getInvokers() {
    return invokers;
  }

  public void setInvokers(List<Invoker> invokers) {
    this.invokers = invokers;
  }

  public int getMonitorCode() {
    return monitorCode;
  }

  public void setMonitorCode(int monitorCode) {
    this.monitorCode = monitorCode;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getInfo() {
    return info;
  }

  public void setInfo(String info) {
    this.info = info;
  }
}
