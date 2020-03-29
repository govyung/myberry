/*
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
package org.myberry.server.quarum.message;

import org.myberry.server.quarum.ByteBuf;
import org.myberry.server.quarum.Message;

public class Notification extends Message {

  private int sid;
  private int leader;
  private long peerEpoch;
  private long notificationTime;
  private int maxSid;

  public static Notification create(
      int sid, int leader, long peerEpoch, long notificationTime, int maxSid) {
    Notification notification = new Notification();
    notification.setSid(sid);
    notification.setLeader(leader);
    notification.setPeerEpoch(peerEpoch);
    notification.setNotificationTime(notificationTime);
    notification.setMaxSid(maxSid);
    return notification;
  }

  public int getSid() {
    return sid;
  }

  public void setSid(int sid) {
    this.sid = sid;
  }

  public int getLeader() {
    return leader;
  }

  public void setLeader(int leader) {
    this.leader = leader;
  }

  public long getPeerEpoch() {
    return peerEpoch;
  }

  public void setPeerEpoch(long peerEpoch) {
    this.peerEpoch = peerEpoch;
  }

  public long getNotificationTime() {
    return notificationTime;
  }

  public void setNotificationTime(long notificationTime) {
    this.notificationTime = notificationTime;
  }

  public int getMaxSid() {
    return maxSid;
  }

  public void setMaxSid(int maxSid) {
    this.maxSid = maxSid;
  }

  @Override
  protected int type() {
    return ByteBuf.NOTIFICATION;
  }
}
