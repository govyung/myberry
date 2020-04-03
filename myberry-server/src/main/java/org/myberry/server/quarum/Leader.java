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
package org.myberry.server.quarum;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.server.common.LoggerName;
import org.myberry.server.quarum.message.Ack;
import org.myberry.server.quarum.message.Proposal;
import org.myberry.server.quarum.message.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Leader {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.LEADER_NAME);

  private final QuorumPeer self;
  private final FastElection electionAlg;
  private final MessageHandler messageHandler;

  private ConcurrentMap<Integer, LastMessage> recvLearnerLastMessage = new ConcurrentHashMap<>();
  private ConcurrentMap<
          Integer
          /** sid */
          ,
          Ack
      /** ack */
      >
      ackSet = new ConcurrentHashMap<>();

  public Leader(
      final QuorumPeer self, final FastElection electionAlg, final MessageHandler messageHandler) {
    this.self = self;
    this.electionAlg = electionAlg;
    this.electionAlg.setLearner(null);
    this.messageHandler = messageHandler;
  }

  public void processAck(Ack ack) {
    if (electionAlg.getProposedLeader() != ack.getLeader()
        || electionAlg.getProposedEpoch() != ack.getPeerEpoch()
        || ack.getLeader() != self.getMySid()) {
      return;
    }

    ackSet.put(ack.getSid(), ack);

    self.addMember(ack.getSid(), ack.getElectionAddr());
    messageHandler.addQueueSendMap(ack.getSid());

    self.getRouteInfoManager()
        .registerLeader(
            RemotingHelper.makeStringAddress(
                RemotingHelper.getAddressIP(
                    self.getQuorumVerifier().getAllPeers().get(self.getMySid())),
                self.getNettyServerConfig().getListenPort()));
    self.getRouteInfoManager()
        .registerLearner(
            self.getServerConfig().getClusterName(),
            ack.getSid(),
            RemotingHelper.makeStringAddress(
                RemotingHelper.getAddressIP(ack.getElectionAddr()), ack.getListenPort()),
            ack.getWeight());
    self.getRouteInfoManager()
        .unregisterLearner(self.getServerConfig().getClusterName(), self.getMySid());

    if (self.isLooking()) {
      if (checkRecvAck(ack)) {
        self.setLeader(ack.getLeader());
        self.setLeaderEpoch(self.getLogicalclock());
        self.getMyberryStore().setEpoch(self.getLogicalclock());
        log.info(
            "verify pass, set leader = {}, leaderEpoch = {}",
            self.getLeader(),
            self.getLeaderEpoch());

        int maxElector = getMaxElector(ackSet.keySet());
        if (self.getMyberryStore().getMaxSidFromDisk() > maxElector) {
          self.setMaxSid(self.getMyberryStore().getMaxSidFromDisk());
        } else {
          self.setMaxSid(maxElector);
          self.getMyberryStore().setMaxSid(maxElector);
        }

        sendNotifications();

        self.setLooking(false);

        self.notifyElectionFinish();

        log.info("====>");
        log.info(
            "MySid={}, logicalclock={}, I became leader!", self.getMySid(), self.getLogicalclock());
        log.info("<====");

        log.info("*****  election end  *****");
        log.info("===========================================================================");
      }
    } else {
      messageHandler.sendNotification(ack.getSid());
    }

    self.getRouteInfoManager().printAllTrigger();
  }

  private boolean checkRecvAck(Ack ack) {
    int pass = 0;
    Iterator<Entry<Integer, Vote>> it = electionAlg.getVoteSet().entrySet().iterator();
    while (it.hasNext()) {
      Entry<Integer, Vote> entry = it.next();
      if (entry.getValue().getLeader() == ack.getLeader()
          && entry.getValue().getPeerEpoch() == ack.getPeerEpoch()) {
        pass += 1;
      }
    }

    return self.getQuorumVerifier().containsQuorum(pass);
  }

  private int getMaxElector(Set<Integer> electors) {
    int maxElector = 0;
    for (Integer elector : electors) {
      if (elector > maxElector) {
        maxElector = elector;
      }
    }
    return maxElector > self.getMySid() ? maxElector : self.getMySid();
  }

  private void sendNotifications() {
    Set<Integer> sids = ackSet.keySet();
    for (Integer sid : sids) {
      if (self.getMySid() != sid.intValue()) {
        messageHandler.sendNotification(sid);
      }
    }
  }

  public void processProposal(Proposal proposal) {
    if (self.getMembers().containsKey(proposal.getSid())) {
      messageHandler.recvProposal(proposal.getSid(), proposal);

      if (proposal.getOffset() < self.getMyberryStore().getLastOffset()) {
        messageHandler.sendSync(
            proposal.getSid(),
            self.getRouteInfoManager().getLeaderInfo(),
            self.getRouteInfoManager().getLearnerTable(),
            self.getMembers(),
            self.getMyberryStore().getLastOffset(),
            self.getMyberryStore().getSyncByteBuffer(proposal.getOffset()));
      } else {
        messageHandler.sendSync(
            proposal.getSid(),
            self.getRouteInfoManager().getLeaderInfo(),
            self.getRouteInfoManager().getLearnerTable(),
            self.getMembers(),
            self.getMyberryStore().getLastOffset(),
            null);
      }

      // TO DO Monitor

    } else {
      messageHandler.sendRefuse(proposal.getSid(), "notification object cannot be Leader");
    }
  }

  public ConcurrentMap<Integer, Ack> getAckSet() {
    return ackSet;
  }

  public ConcurrentMap<Integer, LastMessage> getRecvLearnerLastMessage() {
    return recvLearnerLastMessage;
  }
}
