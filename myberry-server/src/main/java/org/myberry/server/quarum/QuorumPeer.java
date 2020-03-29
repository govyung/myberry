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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.myberry.common.CountDownLatch2;
import org.myberry.common.ServerConfig;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.remoting.netty.NettyServerConfig;
import org.myberry.server.common.LoggerName;
import org.myberry.server.routeinfo.RouteInfoManager;
import org.myberry.store.MyberryStore;
import org.myberry.store.Quorum;
import org.myberry.store.config.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuorumPeer implements Quorum {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.QUORUM_LOGGER_NAME);

  /** algorithm version */
  public static final int VERSION = 1;

  private final CountDownLatch2 waitElect = new CountDownLatch2(1);
  private final AtomicReference<QuorumVerifier> quorumVerifier = new AtomicReference<>();

  private final MyberryStore myberryStore;
  private final StoreConfig storeConfig;
  private final NettyServerConfig nettyServerConfig;
  private final ServerConfig serverConfig;
  private final int mySid;
  private final RouteInfoManager routeInfoManager;

  private Map<Integer, String> members;

  private QuorumCnxManager quorumCnxManager;

  private FastElection electionAlg;
  private volatile boolean looking = false;

  private long logicalclock;

  private int leader = -1;
  private long leaderEpoch = -1L;

  private int maxSid;

  public QuorumPeer(
      final ServerConfig serverConfig,
      final NettyServerConfig nettyServerConfig,
      final MyberryStore myberryStore,
      final RouteInfoManager routeInfoManager)
      throws IllegalArgumentException {
    this.myberryStore = myberryStore;
    this.storeConfig = this.myberryStore.getStoreConfig();
    this.nettyServerConfig = nettyServerConfig;
    this.serverConfig = serverConfig;
    this.mySid = this.storeConfig.getMySid();
    this.routeInfoManager = routeInfoManager;
    this.initialize(myberryStore);
  }

  private void initialize(MyberryStore myberryStore) throws IllegalArgumentException {
    int mySidFromSettings = myberryStore.getStoreConfig().getMySid();
    int mySidFromDisk = myberryStore.getMySidFromDisk();
    if (mySidFromDisk == 0 && mySidFromSettings > 0) {
      this.myberryStore.setMySid(mySidFromSettings);
      log.info(
          "*****  mysid first initialized, mysid: {}  *****", this.myberryStore.getMySidFromDisk());
    } else if (mySidFromSettings > 0 && mySidFromSettings == mySidFromDisk) {
      log.info("*****  mysid: {}  *****", mySidFromSettings);
    } else {
      throw new IllegalArgumentException(
          "mySid is illegal, StoreConfig mySid: "
              + mySidFromSettings
              + ", Disk mySid: "
              + mySidFromDisk);
    }
  }

  @Override
  public void start() throws Exception {
    this.newElection();

    waitElect.await();
  }

  public void newElection() throws Exception {
    log.info("=============================================================================");
    log.info(
        "mbid: {}, offset: {}, epoch: {}, maxSid: {}, mySid: {}, componentCount: {}, runningMode: {}",
        myberryStore.getMbidFromDisk(),
        myberryStore.getLastOffset(),
        myberryStore.getEpochFromDisk(),
        myberryStore.getMaxSidFromDisk(),
        myberryStore.getMySidFromDisk(),
        myberryStore.getComponentCountFromDisk(),
        myberryStore.getRunningModeFromDisk());
    log.info("=============================================================================");

    this.setLooking(true);

    if (null == members) {
      this.members = parsePeer(serverConfig.getHaServerAddr());
    }

    quorumVerifier.set(new QuorumVerifier(members));

    if (null == quorumCnxManager) {
      quorumCnxManager =
          new QuorumCnxManager(this, RemotingHelper.getAddressPort(members.get(mySid)));
      quorumCnxManager.start();
    }

    quorumCnxManager.clearAllReadyQueueSend();

    if (null != electionAlg) {
      electionAlg.shutdown();
    }
    electionAlg = new FastElection(this, quorumCnxManager);
    electionAlg.initElection(myberryStore);
    electionAlg.sendInitVote();
    electionAlg.start();
  }

  private Map<Integer, String> parsePeer(String activeAddress) {
    String[] peers = activeAddress.trim().split(",");
    Map<Integer, String> map = new HashMap<>(peers.length);
    for (String peer : peers) {
      String[] element = peer.split("@");
      map.put(Integer.parseInt(element[0]), element[1]);
    }
    return map;
  }

  @Override
  public boolean isLooking() {
    return looking;
  }

  @Override
  public void shutdown() {
    electionAlg.shutdown();
    quorumCnxManager.finish();
  }

  public void addMember(Integer sid, String addr) {
    this.members.put(sid, addr);
  }

  public void removeMember(Integer sid) {
    this.members.remove(sid);
  }

  public void notifyElectionFinish() {
    waitElect.countDown();
  }

  public void setLooking(boolean status) {
    this.looking = status;
  }

  public int getMySid() {
    return mySid;
  }

  public String getClusterName() {
    return serverConfig.getClusterName();
  }

  public QuorumVerifier getQuorumVerifier() {
    return quorumVerifier.get();
  }

  public MyberryStore getMyberryStore() {
    return myberryStore;
  }

  public StoreConfig getStoreConfig() {
    return storeConfig;
  }

  public NettyServerConfig getNettyServerConfig() {
    return nettyServerConfig;
  }

  public ServerConfig getServerConfig() {
    return serverConfig;
  }

  public boolean amILeader(int leader) {
    return mySid == leader;
  }

  public int getMaxSidFromDisk() {
    return myberryStore.getMaxSidFromDisk();
  }

  public long getEpochFromDisk() {
    return myberryStore.getEpochFromDisk();
  }

  public int getLeader() {
    return leader;
  }

  public void setLeader(int leader) {
    this.leader = leader;
  }

  public long getLogicalclock() {
    return logicalclock;
  }

  public void setLogicalclock(long logicalclock) {
    this.logicalclock = logicalclock;
  }

  public long getLeaderEpoch() {
    return leaderEpoch;
  }

  public void setLeaderEpoch(long leaderEpoch) {
    this.leaderEpoch = leaderEpoch;
  }

  public Map<Integer, String> getMembers() {
    return members;
  }

  public int getMaxSid() {
    return maxSid;
  }

  public void setMaxSid(int maxSid) {
    this.maxSid = maxSid;
  }

  public RouteInfoManager getRouteInfoManager() {
    return routeInfoManager;
  }
}
