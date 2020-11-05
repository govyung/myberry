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

import org.myberry.server.quarum.ByteBuf;
import org.myberry.server.quarum.Message;

public class Ack extends Message {

  private int sid;
  private int weight;
  private int listenPort;
  private String electionAddr;
  private long ackTime;
  private int leader;
  private long peerEpoch;

  public static Ack create(
      int sid,
      int weight,
      int listenPort,
      String electionAddr,
      long ackTime,
      int leader,
      long peerEpoch) {
    Ack ack = new Ack();
    ack.setSid(sid);
    ack.setWeight(weight);
    ack.setListenPort(listenPort);
    ack.setElectionAddr(electionAddr);
    ack.setAckTime(ackTime);
    ack.setLeader(leader);
    ack.setPeerEpoch(peerEpoch);
    return ack;
  }

  public int getSid() {
    return sid;
  }

  public void setSid(int sid) {
    this.sid = sid;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  public int getListenPort() {
    return listenPort;
  }

  public void setListenPort(int listenPort) {
    this.listenPort = listenPort;
  }

  public String getElectionAddr() {
    return electionAddr;
  }

  public void setElectionAddr(String electionAddr) {
    this.electionAddr = electionAddr;
  }

  public long getAckTime() {
    return ackTime;
  }

  public void setAckTime(long ackTime) {
    this.ackTime = ackTime;
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

  @Override
  public int type() {
    return ByteBuf.ACK;
  }
}
