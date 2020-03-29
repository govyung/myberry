/*
 * Copyright 2018 gaoyang. All rights reserved.
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

import org.myberry.server.common.LoggerName;
import org.myberry.server.quarum.FastElection.HighestVote;
import org.myberry.server.quarum.message.Inform;
import org.myberry.server.quarum.message.Notification;
import org.myberry.server.quarum.message.Sync;
import org.myberry.server.quarum.message.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Learner {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.LEARNER_NAME);

  private final QuorumPeer self;
  private final FastElection electionAlg;
  private final MessageHandler messageHandler;

  private boolean fromFastElection = true;

  private LastMessage recvLeaderLastMessage;
  private Notification notification;

  public Learner(
      final QuorumPeer self, final FastElection electionAlg, final MessageHandler messageHandler) {
    this.self = self;
    this.electionAlg = electionAlg;
    this.electionAlg.setLeader(null);
    this.messageHandler = messageHandler;
  }

  public void processInform(Inform inform) {
    if (!self.isLooking()) {
      return;
    }

    if (fromFastElection) {
      if (self.getQuorumVerifier().getAllPeers().containsKey(inform.getSid())) {
        HighestVote highestVotes = electionAlg.getHighestVotes(electionAlg.sum());
        if (self.getQuorumVerifier().containsQuorum(highestVotes.getVotes())) {
          Vote vote = electionAlg.getVoteSet().get(highestVotes.getSid());
          if (vote.getSid() == inform.getLeader()
              && vote.getElectionEpoch() == inform.getPeerEpoch()) {
            messageHandler.sendAck(
                inform.getLeader(),
                inform.getLeader(),
                inform.getPeerEpoch(),
                self.getQuorumVerifier().getAllPeers().get(self.getMySid()));
          }
        }
      } else {
        messageHandler.sendRefuse(inform.getSid(), "not in view");
      }
    } else {
      messageHandler.sendAck(
          inform.getLeader(),
          inform.getLeader(),
          inform.getPeerEpoch(),
          self.getQuorumVerifier().getAllPeers().get(self.getMySid()));
    }
  }

  public void processNotification(Notification notification) {
    this.notification = notification;
    if (!self.isLooking()) {
      return;
    }

    if (fromFastElection) {
      HighestVote highestVotes = electionAlg.getHighestVotes(electionAlg.sum());
      if (self.getQuorumVerifier().containsQuorum(highestVotes.getVotes())) {
        Vote vote = electionAlg.getVoteSet().get(highestVotes.getSid());
        if (vote.getSid() == notification.getLeader()
            && vote.getElectionEpoch() == notification.getPeerEpoch()) {
          this.setLeader(notification);
        }
      }
    } else {
      this.setLeader(notification);
    }
  }

  private void setLeader(Notification notification) {
    self.setLogicalclock(notification.getPeerEpoch());
    self.setLeader(notification.getLeader());
    self.setLeaderEpoch(notification.getPeerEpoch());
    self.getMyberryStore().setEpoch(notification.getPeerEpoch());
    self.setMaxSid(notification.getMaxSid());
    self.getMyberryStore().setMaxSid(notification.getMaxSid());

    self.setLooking(false);

    self.notifyElectionFinish();

    log.info("====>");
    log.info(
        "MySid={}, logicalclock={}, I became learner!", self.getMySid(), self.getLogicalclock());
    log.info("<====");

    log.info("*****  election end  *****");
    log.info("===========================================================================");
  }

  public void processSync(Sync sync) {
    if (self.getMembers().containsKey(sync.getLeader())) {
      messageHandler.recvSync(sync.getLeader(), sync);

      if (self.getLeader() != sync.getLeader()) {
        self.setLeader(sync.getLeader());
      }
      if (self.getLeaderEpoch() != sync.getLeaderEpoch()) {
        self.setLeaderEpoch(sync.getLeaderEpoch());
      }
      if (self.getMyberryStore().getComponentCountFromDisk() != sync.getComponentCount()) {
        self.getMyberryStore().setComponentCount(sync.getComponentCount());
      }
      if (self.getMaxSid() != sync.getMaxSid()) {
        self.setMaxSid(sync.getMaxSid());
      }
      if (self.getRouteInfoManager().getLeaderInfo() == null
          || !self.getRouteInfoManager().getLeaderInfo().equals(sync.getLeaderInfo())) {
        self.getRouteInfoManager().registerLeader(sync.getLeaderInfo());
      }

      boolean routeChange =
          self.getRouteInfoManager()
              .updateLearner(self.getServerConfig().getClusterName(), sync.getInvokers());
      if (routeChange) {
        self.getRouteInfoManager().printAllTrigger();
      }

      boolean viewChange = electionAlg.getManager().updateViewAndConnectionWorker(sync.getViews());
      if (viewChange) {
        electionAlg.getManager().printAllTrigger();
      }

      if (sync.getData() != null
          && self.getMyberryStore().getLastOffset() + sync.getData().length == sync.getOffset()) {
        log.info(
            "update offset from {} to {}",
            self.getMyberryStore().getLastOffset(),
            sync.getOffset());
        self.getMyberryStore().setSyncByteBuffer(sync.getData());
      }

      /*Set<Integer> sids = self.getRouteInfoManager().getLearnerTable().keySet();
      for (Integer connId : sids) {
        if (self.getMySid() != connId.intValue() && self.getLeader() != connId.intValue()) {
          electionAlg.getManager().disconnect(connId);
        }
      }*/

    } else {
      messageHandler.sendRefuse(sync.getLeader(), "notification object cannot be Learner");
    }
  }

  public LastMessage getRecvLeaderLastMessage() {
    return recvLeaderLastMessage;
  }

  public void setRecvLeaderLastMessage(LastMessage recvLeaderLastMessage) {
    this.recvLeaderLastMessage = recvLeaderLastMessage;
  }

  public void setFromFastElection(boolean fromFastElection) {
    this.fromFastElection = fromFastElection;
  }

  public Notification getNotification() {
    return notification;
  }
}
