/*
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import org.myberry.common.ServiceThread;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.server.common.LoggerName;
import org.myberry.server.quarum.message.Ack;
import org.myberry.server.quarum.message.Inform;
import org.myberry.server.quarum.message.Notification;
import org.myberry.server.quarum.message.Proposal;
import org.myberry.server.quarum.message.Refuse;
import org.myberry.server.quarum.message.Sync;
import org.myberry.server.quarum.message.Vote;
import org.myberry.store.MyberryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastElection {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.FAST_ELECTION_NAME);

  private final QuorumPeer self;
  private final QuorumCnxManager manager;
  private final Messenger messenger;
  private final MessageHandler messageHandler;

  private ConcurrentMap<Integer, LastMessage> lastMessageSent = new ConcurrentHashMap<>();
  private ConcurrentMap<
          Integer
          /** sid */
          ,
          Vote
      /** vote */
      >
      voteSet = new ConcurrentHashMap<>();

  private int proposedLeader;
  private int proposedOffset;
  private long proposedEpoch;

  private Leader leader;
  private Learner learner;

  public FastElection(final QuorumPeer self, final QuorumCnxManager manager) {
    this.self = self;
    this.manager = manager;
    this.messenger = new Messenger(manager);
    this.messageHandler = new MessageHandler(self, manager, this);
  }

  class Messenger {

    private static final int TIME_OUT = 120 * 1000;

    private final Receiver receiver;
    private final Sender sender;

    public Messenger(final QuorumCnxManager manager) {
      this.sender = new Sender(manager);
      this.receiver = new Receiver(manager);
    }

    class Receiver extends ServiceThread {

      private final QuorumCnxManager manager;

      Receiver(final QuorumCnxManager manager) {
        this.manager = manager;
      }

      @Override
      public String getServiceName() {
        return Receiver.class.getSimpleName();
      }

      @Override
      public void run() {
        log.info(this.getServiceName() + " service started");
        while (!this.isStopped()) {
          try {
            ByteBuf byteBuf = manager.getRecvQueue().poll(10, TimeUnit.MILLISECONDS);
            if (byteBuf == null) {
              continue;
            }

            if (!self.getMyberryStore().hasShutdown()) {
              if (QuorumPeer.VERSION == byteBuf.getVersion()
                  && self.getServerConfig().getClusterName() != null
                  && self.getServerConfig().getClusterName().equals(byteBuf.getClusterName())
                  && self.getMyberryStore().getRunningModeFromDisk() == byteBuf.getRunningMode()
                  && self.getStoreConfig().getFileSize() == byteBuf.getFileSize()) {
                dispatcher(byteBuf);
              } else {
                manager.disconnect(byteBuf.getConnId());
              }
            }

          } catch (Exception e) {
            log.error("Receiver exception: ", e);
          }
        }
        log.info(this.getServiceName() + " service end");
      }
    }

    class Sender extends ServiceThread {

      private final QuorumCnxManager manager;

      Sender(final QuorumCnxManager manager) {
        this.manager = manager;
      }

      @Override
      public String getServiceName() {
        return Sender.class.getSimpleName();
      }

      @Override
      public void run() {
        log.info(this.getServiceName() + " service started");
        while (!this.isStopped()) {
          try {
            if (self.isLooking()) {
              if (leader != null) {
                Iterator<Entry<Integer, Ack>> it = leader.getAckSet().entrySet().iterator();
                while (it.hasNext()) {
                  Entry<Integer, Ack> entry = it.next();
                  if (self.getMyberryStore().now() - entry.getValue().getAckTime() > TIME_OUT) {
                    self.newElection();
                    break;
                  } else if (self.getMyberryStore().now()
                          - lastMessageSent.get(entry.getKey()).getActionTime()
                      > 10 * 1000) {
                    messageHandler.sendLatest(
                        entry.getKey(), lastMessageSent.get(entry.getKey()).getByteBuf());
                  }
                }
              } else if (learner != null) {
                if (learner.getNotification() != null
                    && (self.getMyberryStore().now()
                            - learner.getNotification().getNotificationTime()
                        > TIME_OUT)) {
                  self.newElection();
                } else {
                  Iterator<Entry<Integer, LastMessage>> it = lastMessageSent.entrySet().iterator();
                  while (it.hasNext()) {
                    Entry<Integer, LastMessage> entry = it.next();
                    if (self.getMyberryStore().now() - entry.getValue().getActionTime()
                        > 10 * 1000) {
                      messageHandler.sendLatest(entry.getKey(), entry.getValue().getByteBuf());
                    }
                  }
                }
              } else {
                Set<Integer> sids = self.getQuorumVerifier().getAllPeers().keySet();
                for (Integer sid : sids) {
                  if (sid.intValue() != self.getMySid()) {
                    messageHandler.sendLatest(sid, lastMessageSent.get(sid).getByteBuf());
                  }
                }
              }
              this.waitForRunning(5 * 1000);
            } else {
              if (self.amILeader(self.getLeader())) {
                ConcurrentMap<Integer, LastMessage> recvLearnerLastMessage =
                    leader.getRecvLearnerLastMessage();
                Iterator<Entry<Integer, LastMessage>> it =
                    recvLearnerLastMessage.entrySet().iterator();
                while (it.hasNext()) {
                  Entry<Integer, LastMessage> entry = it.next();
                  if (self.getMyberryStore().now() - entry.getValue().getActionTime() > TIME_OUT) {
                    if (manager.getConnectionPoolSize() > 0) {
                      it.remove();
                      self.removeMember(entry.getKey());
                      Invoker invoker =
                          self.getRouteInfoManager()
                              .unregisterLearner(self.getClusterName(), entry.getKey());
                      self.getRouteInfoManager().putLostLearnerQueue(invoker.getAddr());
                      manager.disconnect(entry.getKey());

                      self.getRouteInfoManager().printAllTrigger();
                    } else {
                      self.newElection();
                    }
                  } else if (self.getMyberryStore().now() - entry.getValue().getActionTime()
                      > 10 * 1000) {
                    if (!manager.existConnection(entry.getKey())
                        && self.getMySid() > entry.getKey().intValue()) {
                      manager.reconnect(entry.getKey());
                      messageHandler.sendInform(
                          entry.getKey(), self.getMySid(), self.getLogicalclock());
                    }
                  }
                }
              } else {
                if (learner.getRecvLeaderLastMessage() != null
                    && self.getMyberryStore().now()
                            - learner.getRecvLeaderLastMessage().getActionTime()
                        > TIME_OUT) {
                  self.newElection();
                } else {
                  messageHandler.sendProposal(self.getLeader());
                }
              }
              this.waitForRunning(5 * 1000);
            }
          } catch (Exception e) {
            log.error("Sender exception: ", e);
          }
        }
        log.info(this.getServiceName() + " service end");
      }
    }

    public void start() {
      this.sender.start();
      this.receiver.start();
    }

    public void shutdown() {
      this.sender.shutdown(true);
      this.receiver.shutdown(true);
    }
  }

  public void sendInitVote() {
    Set<Integer> sids = self.getQuorumVerifier().getAllPeers().keySet();
    for (Integer sid : sids) {
      if (sid.intValue() != self.getMySid()) {
        messageHandler.sendInitVote(
            sid,
            self.getMySid(),
            self.getLogicalclock(),
            proposedLeader,
            proposedOffset,
            proposedEpoch);
      }
    }
  }

  public void initElection(MyberryStore myberryStore) {
    log.info("===========================================================================");
    log.info("*****  election start  *****");
    log.info("set election view: {}", self.getQuorumVerifier().getAllPeers().toString());

    long newEpoch = myberryStore.getEpochFromDisk() + 1;

    self.setLeader(-1);
    self.setLeaderEpoch(-1L);
    self.setLogicalclock(newEpoch);

    updateProposal(self.getMySid(), self.getMyberryStore().getLastOffset(), newEpoch);
  }

  public void start() {
    this.messenger.start();
  }

  public void shutdown() {
    this.messenger.shutdown();
  }

  private void dispatcher(ByteBuf byteBuf) {
    if (ByteBuf.VOTE == byteBuf.getData().type()) {
      log.info("receive vote: {}", byteBuf);
      processVote((Vote) byteBuf.getData());
    } else if (ByteBuf.INFORM == byteBuf.getData().type()) {
      log.info("receive inform: {}", byteBuf);
      if (self.getMySid() != ((Inform) byteBuf.getData()).getLeader()) {
        if (null == learner) {
          learner = new Learner(self, this, messageHandler);
          learner.setFromFastElection(false);
        }
        learner.processInform((Inform) byteBuf.getData());
      }
    } else if (ByteBuf.ACK == byteBuf.getData().type()) {
      log.info("receive ack: {}", byteBuf);
      if (null != leader) {
        leader.processAck((Ack) byteBuf.getData());
      }
    } else if (ByteBuf.NOTIFICATION == byteBuf.getData().type()) {
      log.info("receive notification: {}", byteBuf);
      if (null != learner) {
        learner.processNotification((Notification) byteBuf.getData());
      }
    } else if (ByteBuf.PROPOSAL == byteBuf.getData().type()) {
      log.info("receive proposal: {}", byteBuf);
      if (null != leader) {
        leader.processProposal((Proposal) byteBuf.getData());
      }
    } else if (ByteBuf.SYNC == byteBuf.getData().type()) {
      log.info("receive sync: {}", byteBuf);
      if (null != learner) {
        learner.processSync((Sync) byteBuf.getData());
      }
    } else if (ByteBuf.REFUSE == byteBuf.getData().type()) {
      Refuse crRefuse = (Refuse) byteBuf.getData();
      log.warn("receive refuse: sid={}, feedback={}", crRefuse.getSid(), crRefuse.getMsg());
    } else {
      log.info("receive unknown: {}", byteBuf);
    }
  }

  private void processVote(Vote vote) {
    if (self.isLooking()) {
      if (self.getQuorumVerifier().getAllPeers().containsKey(vote.getSid())) {
        lookForLeader(vote);
      } else {
        messageHandler.sendRefuse(vote.getSid(), "not in view");
      }
    } else {
      if (self.getMembers().containsKey(vote.getSid())) {
        messageHandler.sendInform(vote.getSid(), self.getLeader(), self.getLeaderEpoch());
      } else {
        if (vote.getSid() > self.getMaxSidFromDisk()) {
          messageHandler.sendInform(vote.getSid(), self.getLeader(), self.getLeaderEpoch());
        } else {
          messageHandler.sendRefuse(vote.getSid(), "current maxId is " + self.getMaxSidFromDisk());
        }
      }
    }
  }

  /**
   * Check if a pair (server id, zxid) succeeds our current vote
   *
   * @param newSid
   * @param newOffset
   * @param newEpoch
   * @param currSid
   * @param currEpoch
   * @return
   */
  private boolean totalOrderPredicate(
      int newSid, int newOffset, long newEpoch, int currSid, int currOffset, long currEpoch) {
    /*
     * We return true if one of the following three cases hold:
     * 1- New epoch is higher
     * 2- New epoch is the same as current epoch, but new offset is higher
     * 3- New epoch is the same as current epoch, new offset is the same
     *  as current offset, but server id is higher.
     */

    return ((newEpoch > currEpoch)
        || ((newEpoch == currEpoch)
            && ((newOffset > currOffset) || ((newOffset == currOffset) && (newSid > currSid)))));
  }

  public Map<Integer, Set<Integer>> sum() {
    Map<Integer, Set<Integer>> votes = new HashMap<>();
    for (Map.Entry<Integer, Vote> entry : voteSet.entrySet()) {
      Set<Integer> set = votes.putIfAbsent(entry.getValue().getLeader(), new HashSet<>());
      if (set == null) {
        set = votes.get(entry.getValue().getLeader());
      }
      set.add(entry.getKey());
    }
    log.debug("sum: {}", votes);
    return votes;
  }

  public HighestVote getHighestVotes(Map<Integer, Set<Integer>> votes) {
    Entry<Integer, Set<Integer>> highest = null;

    Iterator<Entry<Integer, Set<Integer>>> it = votes.entrySet().iterator();
    while (it.hasNext()) {
      Entry<Integer, Set<Integer>> entry = it.next();
      if (null == highest) {
        highest = entry;
      } else {
        if (entry.getValue().size() > highest.getValue().size()) {
          highest = entry;
        }
      }
    }

    HighestVote highestVote = new HighestVote();
    highestVote.setSid(highest.getKey());
    highestVote.setVotes(highest.getValue().size());
    return highestVote;
  }

  static class HighestVote {

    private int sid;
    private int votes;

    public int getSid() {
      return sid;
    }

    public void setSid(int sid) {
      this.sid = sid;
    }

    public int getVotes() {
      return votes;
    }

    public void setVotes(int votes) {
      this.votes = votes;
    }
  }

  private void updateProposal(int leader, int offset, long epoch) {
    this.proposedLeader = leader;
    this.proposedOffset = offset;
    this.proposedEpoch = epoch;
  }

  private void resendVote(int leader, int offset, long peerEpoch) {
    Set<Integer> sids = self.getQuorumVerifier().getAllPeers().keySet();
    for (Integer sid : sids) {
      if (sid.intValue() != self.getMySid()) {
        messageHandler.resendVote(
            sid, self.getMySid(), self.getLogicalclock(), leader, offset, peerEpoch);
      }
    }
  }

  private void notifyPeers() {
    Set<Integer> sids = voteSet.keySet();
    for (Integer sid : sids) {
      if (self.getMySid() == sid.intValue()) {
        leader
            .getAckSet()
            .put(
                sid,
                Ack.create(
                    self.getMySid(),
                    self.getServerConfig().getWeight(),
                    self.getNettyServerConfig().getListenPort(),
                    self.getQuorumVerifier().getAllPeers().get(self.getMySid()),
                    self.getMyberryStore().now(),
                    proposedLeader,
                    proposedEpoch));
      } else {
        messageHandler.sendInform(sid, self.getMySid(), self.getLogicalclock());
      }
    }
  }

  private boolean consistencyVerify(Set<Integer> peers) {
    if (peers.size() == 0) {
      return false;
    }
    boolean verifierResult = true;
    Vote vote = null;
    for (Integer peer : peers) {
      if (vote == null) {
        vote = voteSet.get(peer);
      } else {
        verifierResult &=
            (vote.getElectionEpoch() == voteSet.get(peer).getElectionEpoch())
                & (vote.getLeader() == voteSet.get(peer).getLeader())
                & (vote.getPeerEpoch() == voteSet.get(peer).getPeerEpoch());
        if (!verifierResult) {
          break;
        }
      }
    }

    return verifierResult;
  }

  private void archive(Vote vote) {
    voteSet.put(vote.getSid(), vote);

    /*
     * Update proposal
     */
    Vote p =
        Vote.create(
            self.getMySid(), self.getLogicalclock(), proposedLeader, proposedOffset, proposedEpoch);
    voteSet.put(p.getSid(), p);

    log.info("archive: {}", voteSet);

    Map<Integer, Set<Integer>> collections = sum();
    HighestVote highestVotes = getHighestVotes(collections);
    if (self.getQuorumVerifier().containsQuorum(highestVotes.getVotes())
        && consistencyVerify(collections.get(highestVotes.getSid()))) {
      if (self.amILeader(highestVotes.getSid())) {
        learner = null;
        if (leader == null) {
          leader = new Leader(self, this, messageHandler);
        }
        notifyPeers();
      } else {
        leader = null;
        if (learner == null) {
          learner = new Learner(self, this, messageHandler);
        }
      }
    }
  }

  public void lookForLeader(Vote vote) {
    if (vote.getElectionEpoch() > self.getLogicalclock()) {
      log.info(
          "vote epoch: {} > current epoch: {}, clear vote set",
          vote.getElectionEpoch(),
          self.getLogicalclock());
      self.setLogicalclock(vote.getElectionEpoch());
      voteSet.clear();
      if (totalOrderPredicate(
          vote.getLeader(),
          vote.getOffset(),
          vote.getPeerEpoch(),
          self.getMySid(),
          self.getMyberryStore().getLastOffset(),
          self.getEpochFromDisk())) {
        log.info("then greater than and change", vote.getElectionEpoch(), self.getLogicalclock());
        updateProposal(vote.getLeader(), vote.getOffset(), vote.getPeerEpoch());
      } else {
        log.info(
            "then greater than and no change", vote.getElectionEpoch(), self.getLogicalclock());
        updateProposal(
            self.getMySid(), self.getMyberryStore().getLastOffset(), self.getEpochFromDisk());
      }
      this.resendVote(proposedLeader, proposedOffset, proposedEpoch);
      this.archive(vote);
    } else if (vote.getElectionEpoch() < self.getLogicalclock()) {
      log.info(
          "vote epoch: {} < current epoch: {}", vote.getElectionEpoch(), self.getLogicalclock());
    } else if (totalOrderPredicate(
        vote.getLeader(),
        vote.getOffset(),
        vote.getPeerEpoch(),
        proposedLeader,
        proposedOffset,
        proposedEpoch)) {
      log.info(
          "vote epoch: {} = current epoch: {}, equal and change",
          vote.getElectionEpoch(),
          self.getLogicalclock());
      updateProposal(vote.getLeader(), vote.getOffset(), vote.getPeerEpoch());
      this.resendVote(proposedLeader, proposedOffset, proposedEpoch);
      this.archive(vote);
    } else {
      log.info(
          "vote epoch: {} = current epoch: {}, equal and no change",
          vote.getElectionEpoch(),
          self.getLogicalclock());
      this.archive(vote);
    }
  }

  public Leader getLeader() {
    return leader;
  }

  public void setLeader(Leader leader) {
    this.leader = leader;
  }

  public Learner getLearner() {
    return learner;
  }

  public void setLearner(Learner learner) {
    this.learner = learner;
  }

  public ConcurrentMap<Integer, Vote> getVoteSet() {
    return voteSet;
  }

  public int getProposedLeader() {
    return proposedLeader;
  }

  public long getProposedEpoch() {
    return proposedEpoch;
  }

  public ConcurrentMap<Integer, LastMessage> getLastMessageSent() {
    return lastMessageSent;
  }

  public QuorumCnxManager getManager() {
    return manager;
  }
}
