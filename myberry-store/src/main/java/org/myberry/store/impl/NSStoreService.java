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
package org.myberry.store.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.myberry.common.Component;
import org.myberry.common.interval.IntervalEntry;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.body.NSComponentData;
import org.myberry.common.strategy.StrategyDate;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.remoting.protocol.RemotingSysResponseCode;
import org.myberry.store.AdminManageResult;
import org.myberry.store.PullIdResult;
import org.myberry.store.common.LoggerName;
import org.myberry.store.config.StoreConfig;
import org.myberry.store.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NSStoreService extends AbstractStoreService {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  private final Lock lock = new ReentrantLock();

  public NSStoreService(final StoreConfig storeConfig) throws IOException {
    super(storeConfig);
  }

  @Override
  public AdminManageResult addComponent(Object... obj) {
    lock.lock();
    try {
      long currentTime = new Date().getTime();

      NSComponent nsc = new NSComponent();
      nsc.setCreateTime(currentTime);
      nsc.setUpdateTime(currentTime);
      nsc.setPhyOffset(physicalFile.getLastOffset());
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

      if (physicalFile.isWriteFull(NSComponent.FIXED_FIELD_SIZE + nsc.getKeyLength())) {
        return new AdminManageResult(ResponseCode.DISK_FULL);
      } else if (physicalFile.isExistKey(nsc.getKey())) {
        return new AdminManageResult(ResponseCode.KEY_EXISTED);
      }
      physicalFile.addComponent(nsc);

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
  public List<Component> queryAllComponent() {
    Collection<AbstractComponent> componentMap = this.getComponentMap();
    List<Component> component = new ArrayList<Component>();

    Iterator<AbstractComponent> iterator = componentMap.iterator();
    while (iterator.hasNext()) {
      NSComponent nsc = (NSComponent) iterator.next();
      NSComponentData nscd = new NSComponentData();
      nscd.setKey(nsc.getKey());
      nscd.setValue(nsc.getInitValue());
      nscd.setStepSize(nsc.getStepSize());
      nscd.setResetType(nsc.getResetType());
      nscd.setCode(NSComponentData.CODE);
      component.add(nscd);
    }
    return component;
  }

  @Override
  public PullIdResult getNewId(String key, Map<String, String> attachments) {
    if (!physicalFile.isExistKey(key)) {
      log.warn("invalid key: {}", key);
      return new PullIdResult(ResponseCode.KEY_NOT_EXISTED, emptyString());
    }

    NSComponent nsc = (NSComponent) physicalFile.getComponent(key);
    nsc.getLock().lock();
    try {
      long beginLockTimestamp = this.getSystemClock().now();
      this.beginTimeInLock = beginLockTimestamp;

      if (isReset(nsc)) {
        nsc.resetCurrentValue();
      }

      int start = nsc.getCurrentValue();

      nsc.setUpdateTime(beginLockTimestamp);
      nsc.incrementMbid();
      nsc.setCurrentValue(nsc.getCurrentValue() + nsc.getStepSize());

      physicalFile.updateBufferLong(
          (int) nsc.getPhyOffset() + NSComponent.updateTimeHeader, nsc.getUpdateTime());
      physicalFile.updateBufferLong(
          (int) nsc.getPhyOffset() + NSComponent.mbidHeader, nsc.getMbid());
      physicalFile.updateBufferLong(
          (int) nsc.getPhyOffset() + NSComponent.currentValueHeader, nsc.getCurrentValue());
      physicalFile.incrMbid();
      if (this.isAlwaysFlush()) {
        physicalFile.save();
      }

      IntervalEntry intervalEntry = new IntervalEntry();
      intervalEntry.setStart(start);
      intervalEntry.setEnd(nsc.getCurrentValue() - 1);
      intervalEntry.setSynergyId(physicalFile.getMySid());

      beginTimeInLock = 0;
      return new PullIdResult(ResponseCode.SUCCESS, IntervalEntry.encode(intervalEntry));
    } catch (Exception e) {
      this.beginTimeInLock = 0;
      log.error("getNewId() error: ", e.getMessage());
      return new PullIdResult(
          RemotingSysResponseCode.SYSTEM_ERROR, RemotingHelper.exceptionSimpleDesc(e));
    } finally {
      nsc.getLock().unlock();
    }
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void shutdown() {
    super.shutdown();
  }

  @Override
  public byte[] getSyncByteBuffer(int offset) {
    byte[] byteArray = physicalFile.getComponentByteArray(offset);
    List<AbstractComponent> list = physicalFile.getComponentByteBuffer(byteArray);
    for (AbstractComponent abstractComponent : list) {
      NSComponent nsc = (NSComponent) abstractComponent;
      nsc.setCurrentValue(nsc.getInitValue());
    }
    return physicalFile.getSyncByteBuffer(list, byteArray.length);
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
    return NSStoreService.class.getSimpleName();
  }
}
