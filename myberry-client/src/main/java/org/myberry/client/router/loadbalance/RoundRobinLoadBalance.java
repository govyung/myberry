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
package org.myberry.client.router.loadbalance;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.myberry.common.constant.LoggerName;
import org.myberry.common.loadbalance.Invoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoundRobinLoadBalance {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.CLIENT_LOGGER_NAME);

  private static final int RECYCLE_PERIOD = 60000;

  private Map<
          String
          /** addr */
          ,
          WeightedRoundRobin>
      addrWeightMap = new HashMap<>();

  private final Lock lock = new ReentrantLock();

  private static class WeightedRoundRobin {

    private int weight;
    private AtomicLong current = new AtomicLong(0);
    private long lastUpdate;

    WeightedRoundRobin(final int weight) {
      this.weight = weight;
    }

    public void setWeight(int weight) {
      this.weight = weight;
    }

    public int getWeight() {
      return weight;
    }

    public long increaseCurrent() {
      return current.addAndGet(weight);
    }

    public void sel(int total) {
      current.addAndGet(-1 * total);
    }

    public long getLastUpdate() {
      return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
      this.lastUpdate = lastUpdate;
    }
  }

  public Invoker doSelect(List<Invoker> invokers) {
    int totalWeight = 0;
    long maxCurrent = Long.MIN_VALUE;
    long now = System.currentTimeMillis();
    Invoker selectedInvoker = null;
    WeightedRoundRobin selectedWRR = null;
    for (Invoker invoker : invokers) {
      WeightedRoundRobin weightedRoundRobin = addrWeightMap.get(invoker.getAddr());

      if (weightedRoundRobin == null) {
        weightedRoundRobin = new WeightedRoundRobin(invoker.getWeight());
        addrWeightMap.put(invoker.getAddr(), weightedRoundRobin);
      }

      if (invoker.getWeight() != weightedRoundRobin.getWeight()) {
        // weight changed
        weightedRoundRobin.setWeight(invoker.getWeight());
      }

      long cur = weightedRoundRobin.increaseCurrent();
      weightedRoundRobin.setLastUpdate(now);
      if (cur > maxCurrent) {
        maxCurrent = cur;
        selectedInvoker = invoker;
        selectedWRR = weightedRoundRobin;
      }
      totalWeight += invoker.getWeight();
    }
    try {
      if (lock.tryLock(2L, TimeUnit.MILLISECONDS) && invokers.size() != addrWeightMap.size()) {
        try {
          // copy -> modify -> update reference
          ConcurrentMap<String, WeightedRoundRobin> newMap = new ConcurrentHashMap<>(addrWeightMap);
          Iterator<Entry<String, WeightedRoundRobin>> it = newMap.entrySet().iterator();
          while (it.hasNext()) {
            Entry<String, WeightedRoundRobin> item = it.next();
            if (now - item.getValue().getLastUpdate() > RECYCLE_PERIOD) {
              it.remove();
            }
          }
          this.addrWeightMap = newMap;
        } catch (Exception e) {
          log.error("RoundRobinLoadBalance Exception: ", e);
        } finally {
          lock.unlock();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    if (selectedInvoker != null) {
      selectedWRR.sel(totalWeight);
      return selectedInvoker;
    }
    /** Acquisition timeout during competition, conservative random acquisition. */
    return invokers.get(ThreadLocalRandom.current().nextInt(invokers.size()));
  }
}
