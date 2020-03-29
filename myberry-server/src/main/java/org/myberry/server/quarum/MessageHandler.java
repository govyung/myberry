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
package org.myberry.server.quarum;

import java.util.Map;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.server.common.LoggerName;
import org.myberry.server.quarum.message.Ack;
import org.myberry.server.quarum.message.Inform;
import org.myberry.server.quarum.message.Notification;
import org.myberry.server.quarum.message.Proposal;
import org.myberry.server.quarum.message.Refuse;
import org.myberry.server.quarum.message.Sync;
import org.myberry.server.quarum.message.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageHandler {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.MESSAGE_HANDLER_NAME);

  private final QuorumPeer self;
  private final QuorumCnxManager manager;
  private final FastElection electionAlg;

  public MessageHandler(
      final QuorumPeer self, final QuorumCnxManager manager, final FastElection electionAlg) {
    this.self = self;
    this.manager = manager;
    this.electionAlg = electionAlg;
  }

  public void sendInitVote(
      int connId, int sid, long electionEpoch, int leader, int offset, long peerEpoch) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Vote.create(sid, electionEpoch, leader, offset, peerEpoch));
    manager.sendMessage(connId, byteBuf, false, false);
    electionAlg
        .getLastMessageSent()
        .put(connId, new LastMessage(byteBuf, self.getMyberryStore().now()));
    log.info("send init vote to sid: {}, {}", connId, byteBuf);
  }

  public void resendVote(
      int connId, int sid, long electionEpoch, int leader, int offset, long peerEpoch) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Vote.create(sid, electionEpoch, leader, offset, peerEpoch));
    manager.sendMessage(connId, byteBuf, true, true);
    electionAlg
        .getLastMessageSent()
        .put(connId, new LastMessage(byteBuf, self.getMyberryStore().now()));
    log.info("resend vote to sid: {}, {}", connId, byteBuf);
  }

  public void sendInform(int connId, int leader, long peerEpoch) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Inform.create(self.getMySid(), leader, peerEpoch));
    manager.sendMessage(connId, byteBuf, true, true);
    electionAlg
        .getLastMessageSent()
        .put(connId, new LastMessage(byteBuf, self.getMyberryStore().now()));
    log.info("notify inform to sid {}, {}", connId, byteBuf);
  }

  public void sendAck(int connId, int leader, long peerEpoch, String electionAddr) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Ack.create(
                self.getMySid(),
                self.getServerConfig().getWeight(),
                self.getNettyServerConfig().getListenPort(),
                electionAddr,
                self.getMyberryStore().now(),
                leader,
                peerEpoch));
    manager.sendMessage(connId, byteBuf, true, true);
    electionAlg
        .getLastMessageSent()
        .put(connId, new LastMessage(byteBuf, self.getMyberryStore().now()));
    log.info("send ack to sid: {}, {}", connId, byteBuf);
  }

  public void sendNotification(int connId) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Notification.create(
                self.getMySid(),
                self.getMySid(),
                self.getLogicalclock(),
                self.getMyberryStore().now(),
                self.getMaxSid()));
    manager.sendMessage(connId, byteBuf, true, true);
    electionAlg
        .getLastMessageSent()
        .put(connId, new LastMessage(byteBuf, self.getMyberryStore().now()));
    log.info("send notification to sid {}, {}", connId, byteBuf);
  }

  public void sendProposal(int connId) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Proposal.create(self.getMySid(), self.getMyberryStore().getLastOffset()));
    manager.sendMessage(connId, byteBuf, false, true);
    log.info("send proposal to sid: {}, {}", connId, byteBuf);
  }

  public void sendSync(
      int connId,
      String leaderInfo,
      Map<Integer, Invoker> invokers,
      Map<Integer, String> views,
      int offset,
      byte[] data) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Sync.create(
                self.getLeader(),
                self.getLeaderEpoch(),
                self.getMyberryStore().getComponentCountFromDisk(),
                self.getMaxSid(),
                leaderInfo,
                invokers,
                views,
                offset,
                data));
    manager.sendMessage(connId, byteBuf, false, true);
    log.info("send sync to sid: {}, {}", connId, byteBuf);
  }

  public void sendRefuse(int connId, String msg) {
    ByteBuf byteBuf =
        ByteBuf.create(
            QuorumPeer.VERSION,
            self.getClusterName(),
            self.getMyberryStore().getRunningModeFromDisk(),
            self.getStoreConfig().getFileSize(),
            self.getMySid(),
            Refuse.create(self.getMySid(), msg));
    manager.sendMessage(connId, byteBuf, false, true);
    manager.addDelayCloseQuorumConnection(connId, 10 * 1000);
    log.info("send refuse to sid: {}, {}", connId, byteBuf);
  }

  public void addQueueSendMap(int connId) {
    manager.addQueueSendMap(connId);
  }

  public void sendLatest(int connId, ByteBuf byteBuf) {
    manager.sendMessage(connId, byteBuf, false, true);
    log.info("send latest to sid: {}, {}", connId, byteBuf);
  }

  public void recvSync(int sid, Sync sync) {
    electionAlg
        .getLearner()
        .setRecvLeaderLastMessage(
            new LastMessage(
                ByteBuf.create(
                    QuorumPeer.VERSION,
                    self.getClusterName(),
                    self.getMyberryStore().getRunningModeFromDisk(),
                    self.getStoreConfig().getFileSize(),
                    sid,
                    sync),
                self.getMyberryStore().now()));
  }

  public void recvProposal(int sid, Proposal proposal) {
    electionAlg
        .getLeader()
        .getRecvLearnerLastMessage()
        .put(
            sid,
            new LastMessage(
                ByteBuf.create(
                    QuorumPeer.VERSION,
                    self.getClusterName(),
                    self.getMyberryStore().getRunningModeFromDisk(),
                    self.getStoreConfig().getFileSize(),
                    sid,
                    proposal),
                self.getMyberryStore().now()));
  }
}
