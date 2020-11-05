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

public class TestNullObject implements MessageLite {

  @SerialField(ordinal = 0)
  private int aa = 5;

  @SerialField(ordinal = 1)
  private Integer bb;

  @SerialField(ordinal = 2)
  private Long cc;

  @SerialField(ordinal = 3)
  private Float dd;

  @SerialField(ordinal = 4)
  private Double ee;

  @SerialField(ordinal = 5)
  private Boolean ff;

  @SerialField(ordinal = 6)
  private String gg;

  @SerialField(ordinal = 7)
  private InnnerObj hh;

  @SerialField(ordinal = 8)
  private List<Integer> ii;

  @SerialField(ordinal = 9)
  private List<Long> jj;

  @SerialField(ordinal = 10)
  private List<Float> kk;

  @SerialField(ordinal = 11)
  private List<Double> ll;

  @SerialField(ordinal = 12)
  private List<Boolean> mm;

  @SerialField(ordinal = 13)
  private List<String> nn;

  @SerialField(ordinal = 14)
  private List<InnnerObj> oo;

  public int getAa() {
    return aa;
  }

  public void setAa(int aa) {
    this.aa = aa;
  }

  public Integer getBb() {
    return bb;
  }

  public void setBb(Integer bb) {
    this.bb = bb;
  }

  public Long getCc() {
    return cc;
  }

  public void setCc(Long cc) {
    this.cc = cc;
  }

  public Float getDd() {
    return dd;
  }

  public void setDd(Float dd) {
    this.dd = dd;
  }

  public Double getEe() {
    return ee;
  }

  public void setEe(Double ee) {
    this.ee = ee;
  }

  public Boolean getFf() {
    return ff;
  }

  public void setFf(Boolean ff) {
    this.ff = ff;
  }

  public String getGg() {
    return gg;
  }

  public void setGg(String gg) {
    this.gg = gg;
  }

  public InnnerObj getHh() {
    return hh;
  }

  public void setHh(InnnerObj hh) {
    this.hh = hh;
  }

  public List<Integer> getIi() {
    return ii;
  }

  public void setIi(List<Integer> ii) {
    this.ii = ii;
  }

  public List<Long> getJj() {
    return jj;
  }

  public void setJj(List<Long> jj) {
    this.jj = jj;
  }

  public List<Float> getKk() {
    return kk;
  }

  public void setKk(List<Float> kk) {
    this.kk = kk;
  }

  public List<Double> getLl() {
    return ll;
  }

  public void setLl(List<Double> ll) {
    this.ll = ll;
  }

  public List<Boolean> getMm() {
    return mm;
  }

  public void setMm(List<Boolean> mm) {
    this.mm = mm;
  }

  public List<String> getNn() {
    return nn;
  }

  public void setNn(List<String> nn) {
    this.nn = nn;
  }

  public List<InnnerObj> getOo() {
    return oo;
  }

  public void setOo(List<InnnerObj> oo) {
    this.oo = oo;
  }
}
