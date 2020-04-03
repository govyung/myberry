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
package org.myberry.client.impl;

import org.myberry.client.monitor.BreakdownInfo;
import org.myberry.client.router.RouterInfo;

public class HeartbeatResult {

  private RouterInfo routerInfo;
  private BreakdownInfo breakdownInfo;

  public HeartbeatResult() {}

  public HeartbeatResult(RouterInfo routerInfo, BreakdownInfo breakdownInfo) {
    this.routerInfo = routerInfo;
    this.breakdownInfo = breakdownInfo;
  }

  public RouterInfo getRouterInfo() {
    return routerInfo;
  }

  public void setRouterInfo(RouterInfo routerInfo) {
    this.routerInfo = routerInfo;
  }

  public BreakdownInfo getBreakdownInfo() {
    return breakdownInfo;
  }

  public void setBreakdownInfo(BreakdownInfo breakdownInfo) {
    this.breakdownInfo = breakdownInfo;
  }

  @Override
  public String toString() {
    return new StringBuilder() //
        .append("HeartbeatResult{") //
        .append("routerInfo=") //
        .append(routerInfo) //
        .append(", breakdownInfo=") //
        .append(breakdownInfo) //
        .append('}') //
        .toString();
  }
}
