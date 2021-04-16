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
package org.myberry.server.impl;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.myberry.common.ProduceMode;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.body.admin.NSComponentData;
import org.myberry.common.strategy.StrategyDate;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.remoting.protocol.RemotingSysResponseCode;
import org.myberry.server.util.DateUtils;
import org.myberry.store.MyberryStore;
import org.myberry.store.NSComponent;
import org.myberry.store.common.LoggerName;
import org.myberry.store.config.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NSService extends MyberryService {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  private final Lock lock = new ReentrantLock();

  public NSService(final MyberryStore myberryStore) {
    super(myberryStore);
  }

  @Override
  public PullIdResult getNewId(String key, Map<String, String> attachments) {
    if (!myberryStore.isExistKey(key)) {
      log.warn("invalid key: {}", key);
      return new PullIdResult(ResponseCode.KEY_NOT_EXISTED, emptyString());
    }

    NSComponent nsc = (NSComponent) myberryStore.getComponentMap().get(key);
    nsc.getLock().lock();
    try {
      long beginLockTimestamp = this.getSystemClock().now();
      myberryStore.setBeginTimeInLock(beginLockTimestamp);

      if (isReset(nsc)) {
        nsc.resetCurrentValue();
      }

      int start = nsc.getCurrentValue();

      nsc.setUpdateTime(beginLockTimestamp);
      nsc.incrementMbid();
      nsc.setCurrentValue(nsc.getCurrentValue() + nsc.getStepSize());

      myberryStore.updateBufferLong(
          (int) nsc.getPhyOffset() + NSComponent.updateTimeHeader, nsc.getUpdateTime());
      myberryStore.updateBufferLong(
          (int) nsc.getPhyOffset() + NSComponent.mbidHeader, nsc.getMbid());
      myberryStore.updateBufferInt(
          (int) nsc.getPhyOffset() + NSComponent.currentValueHeader, nsc.getCurrentValue());
      myberryStore.incrMbid();
      if (this.isAlwaysFlush()) {
        myberryStore.save();
      }

      myberryStore.setBeginTimeInLock(0);
      return new PullIdResult(
          ResponseCode.SUCCESS,
          ProduceMode.NS.getProduceCode(),
          start,
          nsc.getCurrentValue() - 1,
          myberryStore.getMySidFromDisk());
    } catch (Exception e) {
      myberryStore.setBeginTimeInLock(0);
      log.error("getNewId() error: ", e.getMessage());
      return new PullIdResult(
          RemotingSysResponseCode.SYSTEM_ERROR, RemotingHelper.exceptionSimpleDesc(e));
    } finally {
      nsc.getLock().unlock();
    }
  }

  @Override
  public AdminManageResult addComponent(Object... obj) {
    lock.lock();
    try {
      long currentTime = new Date().getTime();

      NSComponent nsc = new NSComponent();
      nsc.setCreateTime(currentTime);
      nsc.setUpdateTime(currentTime);
      nsc.setPhyOffset(myberryStore.getLastOffset());
      nsc.setStatus(ComponentStatus.OPEN.getStatus());

      for (int i = 0; i < obj.length; i++) {
        if (i == 0) {
          String str = (String) obj[i];
          int length = str.getBytes(StoreConfig.MYBERRY_STORE_DEFAULT_CHARSET).length;
          nsc.setKeyLength(length);
          nsc.setKey(str);
        } else if (i == 1) {
          Integer integer = (Integer) obj[i];
          nsc.setInitValue(integer.intValue());
          nsc.setCurrentValue(integer.intValue());
        } else if (i == 2) {
          Integer integer = (Integer) obj[i];
          nsc.setStepSize(integer.intValue());
        } else if (i == 3) {
          Integer integer = (Integer) obj[i];
          nsc.setResetType(integer);
        }
      }

      if (myberryStore.isWriteFull(NSComponent.FIXED_FIELD_SIZE + nsc.getKeyLength())) {
        return new AdminManageResult(ResponseCode.DISK_FULL);
      } else if (myberryStore.isExistKey(nsc.getKey())) {
        return new AdminManageResult(ResponseCode.KEY_EXISTED);
      }
      myberryStore.addComponent(nsc);

      log.info(
          "{} ++> add key: {}, value: {}, stepSize: {} success.",
          this.getServiceName(),
          nsc.getKey(),
          nsc.getInitValue(),
          nsc.getStepSize());
      return new AdminManageResult(ResponseCode.SUCCESS);
    } catch (Exception e) {
      log.error("error request: ", e);
      return new AdminManageResult(ResponseCode.SYSTEM_ERROR);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public AdminManageResult queryComponentByKey(String key) {
    if (!myberryStore.isExistKey(key)) {
      log.warn("invalid key: {}", key);
      return new AdminManageResult(ResponseCode.KEY_NOT_EXISTED);
    }

    NSComponent nsc = (NSComponent) myberryStore.getComponentMap().get(key);

    NSComponentData nscd = new NSComponentData();
    nscd.setKey(key);
    nscd.setValue(nsc.getInitValue());
    nscd.setStepSize(nsc.getStepSize());
    nscd.setResetType(nsc.getResetType());
    nscd.setCode(NSComponentData.CODE);
    return new AdminManageResult(ResponseCode.SUCCESS, nscd);
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void shutdown() {
    super.shutdown();
  }

  public boolean isReset(NSComponent nsComponent) {
    long currentTimeMillis = System.currentTimeMillis();

    if (StrategyDate.NON_TIME == nsComponent.getResetType()) {
      return false;
    } else if (StrategyDate.TIME_DAY == nsComponent.getResetType()) {
      return compare(currentTimeMillis, nsComponent.getUpdateTime(), StrategyDate.TIME_DAY);
    } else if (StrategyDate.TIME_MONTH == nsComponent.getResetType()) {
      return compare(currentTimeMillis, nsComponent.getUpdateTime(), StrategyDate.TIME_MONTH);
    } else if (StrategyDate.TIME_YEAR == nsComponent.getResetType()) {
      return compare(currentTimeMillis, nsComponent.getUpdateTime(), StrategyDate.TIME_YEAR);
    } else {
      return false;
    }
  }

  private boolean compare(long currentTime, long updateTime, int type) {
    return currentTime > updateTime
        && !DateUtils.convertTime(type, currentTime)
            .equals(DateUtils.convertTime(type, updateTime));
  }

  @Override
  public String getServiceName() {
    return NSService.class.getSimpleName();
  }
}
