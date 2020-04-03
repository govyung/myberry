/*
* MIT License
*
* Copyright (c) 2020 gaoyang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.myberry.server.quarum.message;

import java.util.Map;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.server.quarum.ByteBuf;
import org.myberry.server.quarum.Message;

public class Sync extends Message {

  private int leader;
  private long leaderEpoch;
  private int componentCount;
  private int maxSid;
  private String leaderInfo;
  private Map<Integer, Invoker> invokers;
  private Map<Integer, String> views;
  private int offset;
  private byte[] data;

  public static Sync create(
      int leader,
      long leaderEpoch,
      int componentCount,
      int maxSid,
      String leaderInfo,
      Map<Integer, Invoker> invokers,
      Map<Integer, String> views,
      int offset,
      byte[] data) {
    Sync sync = new Sync();
    sync.setLeader(leader);
    sync.setLeaderEpoch(leaderEpoch);
    sync.setComponentCount(componentCount);
    sync.setMaxSid(maxSid);
    sync.setLeaderInfo(leaderInfo);
    sync.setInvokers(invokers);
    sync.setViews(views);
    sync.setOffset(offset);
    sync.setData(data);
    return sync;
  }

  public int getLeader() {
    return leader;
  }

  public void setLeader(int leader) {
    this.leader = leader;
  }

  public long getLeaderEpoch() {
    return leaderEpoch;
  }

  public void setLeaderEpoch(long leaderEpoch) {
    this.leaderEpoch = leaderEpoch;
  }

  public int getComponentCount() {
    return componentCount;
  }

  public void setComponentCount(int componentCount) {
    this.componentCount = componentCount;
  }

  public int getMaxSid() {
    return maxSid;
  }

  public void setMaxSid(int maxSid) {
    this.maxSid = maxSid;
  }

  public String getLeaderInfo() {
    return leaderInfo;
  }

  public void setLeaderInfo(String leaderInfo) {
    this.leaderInfo = leaderInfo;
  }

  public Map<Integer, Invoker> getInvokers() {
    return invokers;
  }

  public void setInvokers(Map<Integer, Invoker> invokers) {
    this.invokers = invokers;
  }

  public Map<Integer, String> getViews() {
    return views;
  }

  public void setViews(Map<Integer, String> views) {
    this.views = views;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  @Override
  public int type() {
    return ByteBuf.SYNC;
  }
}
