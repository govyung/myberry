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
package org.myberry.common.codec;

import java.util.List;
import org.myberry.common.codec.annotation.SerialField;

public class TestObject implements MessageLite {

  @SerialField(ordinal = 0)
  private int a;

  @SerialField(ordinal = 1)
  private Integer b;

  @SerialField(ordinal = 2)
  private long c;

  @SerialField(ordinal = 3)
  private Long d;

  @SerialField(ordinal = 4)
  private float e;

  @SerialField(ordinal = 5)
  private Float f;

  @SerialField(ordinal = 6)
  private double g;

  @SerialField(ordinal = 7)
  private Double h;

  @SerialField(ordinal = 8)
  private boolean i;

  @SerialField(ordinal = 9)
  private Boolean j;

  @SerialField(ordinal = 10)
  private String k;

  @SerialField(ordinal = 11)
  private List<Integer> l;

  @SerialField(ordinal = 12)
  private List<Long> m;

  @SerialField(ordinal = 13)
  private List<Float> n;

  @SerialField(ordinal = 14)
  private List<Double> o;

  @SerialField(ordinal = 15)
  private List<Boolean> p;

  @SerialField(ordinal = 16)
  private List<String> q;

  @SerialField(ordinal = 17)
  private InnnerObj innnerObj;

  @SerialField(ordinal = 18)
  private List<InnnerObj> innnerObjs;

  public int getA() {
    return a;
  }

  public void setA(int a) {
    this.a = a;
  }

  public Integer getB() {
    return b;
  }

  public void setB(Integer b) {
    this.b = b;
  }

  public long getC() {
    return c;
  }

  public void setC(long c) {
    this.c = c;
  }

  public Long getD() {
    return d;
  }

  public void setD(Long d) {
    this.d = d;
  }

  public float getE() {
    return e;
  }

  public void setE(float e) {
    this.e = e;
  }

  public Float getF() {
    return f;
  }

  public void setF(Float f) {
    this.f = f;
  }

  public double getG() {
    return g;
  }

  public void setG(double g) {
    this.g = g;
  }

  public Double getH() {
    return h;
  }

  public void setH(Double h) {
    this.h = h;
  }

  public boolean isI() {
    return i;
  }

  public void setI(boolean i) {
    this.i = i;
  }

  public Boolean getJ() {
    return j;
  }

  public void setJ(Boolean j) {
    this.j = j;
  }

  public String getK() {
    return k;
  }

  public void setK(String k) {
    this.k = k;
  }

  public List<Integer> getL() {
    return l;
  }

  public void setL(List<Integer> l) {
    this.l = l;
  }

  public List<Long> getM() {
    return m;
  }

  public void setM(List<Long> m) {
    this.m = m;
  }

  public List<Float> getN() {
    return n;
  }

  public void setN(List<Float> n) {
    this.n = n;
  }

  public List<Double> getO() {
    return o;
  }

  public void setO(List<Double> o) {
    this.o = o;
  }

  public List<Boolean> getP() {
    return p;
  }

  public void setP(List<Boolean> p) {
    this.p = p;
  }

  public List<String> getQ() {
    return q;
  }

  public void setQ(List<String> q) {
    this.q = q;
  }

  public InnnerObj getInnnerObj() {
    return innnerObj;
  }

  public void setInnnerObj(InnnerObj innnerObj) {
    this.innnerObj = innnerObj;
  }

  public List<InnnerObj> getInnnerObjs() {
    return innnerObjs;
  }

  public void setInnnerObjs(List<InnnerObj> innnerObjs) {
    this.innnerObjs = innnerObjs;
  }
}
