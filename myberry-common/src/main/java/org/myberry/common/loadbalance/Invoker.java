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
package org.myberry.common.loadbalance;

public class Invoker {

  private String addr;
  private int weight;

  public Invoker() {}

  public Invoker(String addr, int weight) {
    this.addr = addr;
    this.weight = weight;
  }

  public String getAddr() {
    return addr;
  }

  public void setAddr(String addr) {
    this.addr = addr;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  public static Invoker getInvoker(String invoker) {
    String[] split = invoker.split("@");
    Invoker ink = new Invoker();
    ink.setAddr(split[0]);
    ink.setWeight(Integer.parseInt(split[1]));
    return ink;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((addr == null) ? 0 : addr.hashCode());
    result = prime * result + weight;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Invoker other = (Invoker) obj;
    if (addr == null) {
      if (other.addr != null)
        return false;
    } else if (!addr.equals(other.addr))
      return false;
    if (weight != other.weight)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(addr).append("#").append(weight).toString();
  }
}
