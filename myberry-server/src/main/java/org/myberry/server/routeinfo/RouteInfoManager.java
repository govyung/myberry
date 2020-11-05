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
package org.myberry.server.routeinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.myberry.common.constant.LoggerName;
import org.myberry.common.loadbalance.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteInfoManager {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.SERVER_LOGGER_NAME);

  private static final long TIMEOUT = 120 * 1000;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final HashMap<String /* clusterName */, Set<LearnerInfo>> clusterTable;
  private final HashMap<Integer /* learnerSid */, Invoker> learnerTable;

  private final BlockingDeque<String> lostLearnerQueue;

  private String leaderInfo;

  public RouteInfoManager() {
    this.clusterTable = new HashMap<>(32);
    this.learnerTable = new HashMap<>(64);
    this.lostLearnerQueue = new LinkedBlockingDeque<>(128);
  }

  public void registerLearner(
      final String clusterName, final int sid, final String learnerAddr, final int weight) {
    try {
      try {
        this.lock.writeLock().lockInterruptibly();

        Set<LearnerInfo> learnerAddrs = this.clusterTable.get(clusterName);
        if (null == learnerAddrs) {
          learnerAddrs = new HashSet<>();
          this.clusterTable.put(clusterName, learnerAddrs);
        }
        learnerAddrs.add(new LearnerInfo(sid, learnerAddr));

        Invoker invoker = this.learnerTable.get(sid);
        if (null == invoker) {
          invoker = new Invoker(learnerAddr, weight);
          this.learnerTable.put(sid, invoker);
        }

      } finally {
        this.lock.writeLock().unlock();
      }
    } catch (Exception e) {
      log.error("registerLearner Exception", e);
    }
  }

  public Invoker unregisterLearner(final String clusterName, final int sid) {
    try {
      try {
        this.lock.writeLock().lockInterruptibly();

        Set<LearnerInfo> learnerAddrs = this.clusterTable.get(clusterName);
        if (null != learnerAddrs) {
          Iterator<LearnerInfo> it = learnerAddrs.iterator();
          while (it.hasNext()) {
            LearnerInfo baseInfo = it.next();
            if (baseInfo.getSid() == sid) {
              it.remove();
              break;
            }
          }
        }

        return this.learnerTable.remove(sid);
      } finally {
        this.lock.writeLock().unlock();
      }
    } catch (Exception e) {
      log.error("unregisterLearner Exception", e);
    }
    return null;
  }

  public boolean updateLearner(final String clusterName, Map<Integer, Invoker> syncInvokerTable) {
    boolean changed = false;
    try {
      try {
        this.lock.writeLock().lockInterruptibly();

        Set<LearnerInfo> learnerAddrs = this.clusterTable.get(clusterName);
        if (null == learnerAddrs) {
          learnerAddrs = new HashSet<>();
          this.clusterTable.put(clusterName, learnerAddrs);
        }

        Iterator<Entry<Integer, Invoker>> syncInvokerIt = syncInvokerTable.entrySet().iterator();
        while (syncInvokerIt.hasNext()) {
          Entry<Integer, Invoker> entry = syncInvokerIt.next();
          if (!learnerTable.containsKey(entry.getKey())) {
            learnerAddrs.add(new LearnerInfo(entry.getKey(), entry.getValue().getAddr()));
            learnerTable.put(entry.getKey(), entry.getValue());
            changed = true;
          } else {
            if (!learnerTable.get(entry.getKey()).equals(entry.getValue())) {
              learnerAddrs.add(new LearnerInfo(entry.getKey(), entry.getValue().getAddr()));
              learnerTable.put(entry.getKey(), entry.getValue());
              changed = true;
            }
          }
        }

        Iterator<Entry<Integer, Invoker>> sidInvokerIt = this.learnerTable.entrySet().iterator();
        while (sidInvokerIt.hasNext()) {
          Entry<Integer, Invoker> entry = sidInvokerIt.next();
          if (!syncInvokerTable.containsKey(entry.getKey())) {
            learnerAddrs.remove(new LearnerInfo(entry.getKey(), entry.getValue().getAddr()));
            sidInvokerIt.remove();
            changed = true;
          }
        }

      } finally {
        this.lock.writeLock().unlock();
      }
    } catch (Exception e) {
      log.error("updateLearner Exception", e);
    }

    return changed;
  }

  public void printAllTrigger() {
    try {
      try {
        this.lock.readLock().lockInterruptibly();
        log.info("--------------------------------------------------------");
        {
          log.info("clusterTable SIZE: {}", this.clusterTable.size());
          Iterator<Entry<String, Set<LearnerInfo>>> it = this.clusterTable.entrySet().iterator();
          while (it.hasNext()) {
            Entry<String, Set<LearnerInfo>> entry = it.next();
            log.info("clusterTable clusterName: {} {}", entry.getKey(), entry.getValue());
          }
        }

        {
          log.info("learnerTable SIZE: {}", this.learnerTable.size());
          Iterator<Entry<Integer, Invoker>> it = this.learnerTable.entrySet().iterator();
          while (it.hasNext()) {
            Entry<Integer, Invoker> entry = it.next();
            log.info("learnerTable learnerSid: {} {}", entry.getKey(), entry.getValue());
          }
        }
        log.info("--------------------------------------------------------");
      } finally {
        this.lock.readLock().unlock();
      }
    } catch (Exception e) {
      log.error("printAllTrigger Exception", e);
    }
  }

  public List<Invoker> getLearnerInfo() {
    try {
      try {
        this.lock.readLock().lockInterruptibly();
        return new ArrayList<>(this.learnerTable.values());
      } finally {
        this.lock.readLock().unlock();
      }
    } catch (Exception e) {
      log.error("getLearnerInfo Exception", e);
    }
    return new ArrayList<>();
  }

  public Map<Integer, Invoker> getLearnerTable() {
    return learnerTable;
  }

  public void registerLeader(final String leaderAddr) {
    try {
      this.leaderInfo = leaderAddr;
      log.info("leaderInfo leaderAddr: {}", leaderAddr);
    } catch (Exception e) {
      log.error("registerLeader Exception", e);
    }
  }

  public void putLostLearnerQueue(String learnerAddr) throws InterruptedException {
    lostLearnerQueue.put(learnerAddr);
  }

  public String pollLostLearnerQueue() {
    return lostLearnerQueue.poll();
  }

  public void unregisterLeader() {
    try {
      this.leaderInfo = null;
    } catch (Exception e) {
      log.error("unregisterLeader Exception", e);
    }
  }

  public String getLeaderInfo() {
    return leaderInfo;
  }

  public void changeLearnerWeight(final int sid, final int weight) {
    try {
      Invoker invoker = this.learnerTable.get(sid);
      invoker.setWeight(weight);

      log.info("server changeLearnerWeight, {} ", invoker.getAddr());
    } catch (Exception e) {
      log.error("changeLearnerWeight Exception", e);
    }
  }
}

class LearnerInfo {

  private int sid;
  private String learnerAddr;
  private long lastUpdateTimestamp;

  public LearnerInfo(int sid, String learnerAddr) {
    this.sid = sid;
    this.learnerAddr = learnerAddr;
    this.lastUpdateTimestamp = System.currentTimeMillis();
  }

  public int getSid() {
    return sid;
  }

  public void setSid(int sid) {
    this.sid = sid;
  }

  public String getLearnerAddr() {
    return learnerAddr;
  }

  public void setLearnerAddr(String learnerAddr) {
    this.learnerAddr = learnerAddr;
  }

  public long getLastUpdateTimestamp() {
    return lastUpdateTimestamp;
  }

  public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
    this.lastUpdateTimestamp = lastUpdateTimestamp;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((learnerAddr == null) ? 0 : learnerAddr.hashCode());
    result = prime * result + sid;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    LearnerInfo other = (LearnerInfo) obj;
    if (learnerAddr == null) {
      if (other.learnerAddr != null) return false;
    } else if (!learnerAddr.equals(other.learnerAddr)) return false;
    if (sid != other.sid) return false;
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder() //
        .append("LearnerInfo [sid=") //
        .append(sid) //
        .append(", learnerAddr=") //
        .append(learnerAddr) //
        .append(", lastUpdateTimestamp=") //
        .append(lastUpdateTimestamp) //
        .append("]") //
        .toString();
  }
}
