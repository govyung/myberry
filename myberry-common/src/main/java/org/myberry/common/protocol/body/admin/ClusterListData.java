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
import java.util.List;
import org.myberry.common.codec.MessageLite;
import org.myberry.common.codec.annotation.SerialField;

public class ClusterListData implements MessageLite {

  @SerialField(ordinal = 0)
  private List<ClusterNode> clusters;

  public List<ClusterNode> getClusters() {
    return clusters;
  }

  public void setClusters(List<ClusterNode> clusters) {
    this.clusters = clusters;
  }

  public static class ClusterNode implements MessageLite {

    @SerialField(ordinal = 0)
    private int sid;

    @SerialField(ordinal = 1)
    private String type;

    @SerialField(ordinal = 2)
    private String ip;

    @SerialField(ordinal = 3)
    private int servicePort;

    public int getSid() {
      return sid;
    }

    public void setSid(int sid) {
      this.sid = sid;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getIp() {
      return ip;
    }

    public void setIp(String ip) {
      this.ip = ip;
    }

    public int getServicePort() {
      return servicePort;
    }

    public void setServicePort(int servicePort) {
      this.servicePort = servicePort;
    }

    @Override
    public String toString() {
      Class<?> clz = this.getClass();
      Field[] fields = clz.getDeclaredFields();
      StringBuilder builder = new StringBuilder();
      builder.append("ClusterNode [");
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

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ClusterListData{");
    builder.append("clusters=");
    builder.append(clusters);
    builder.append('}');
    return builder.toString();
  }
}
