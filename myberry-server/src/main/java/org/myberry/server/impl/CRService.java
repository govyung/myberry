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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.myberry.common.ProduceMode;
import org.myberry.common.expression.impl.ExpressionParser;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.body.admin.CRComponentData;
import org.myberry.common.strategy.StrategyDate;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.remoting.protocol.RemotingSysResponseCode;
import org.myberry.server.expression.ConverterManager;
import org.myberry.server.expression.ExpressionConverterFactory;
import org.myberry.server.expression.impl.BufferStructObject;
import org.myberry.server.util.DateUtils;
import org.myberry.store.AbstractComponent;
import org.myberry.store.CRComponent;
import org.myberry.store.MyberryStore;
import org.myberry.store.common.LoggerName;
import org.myberry.store.config.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CRService extends MyberryService {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  private final ConcurrentMap<
          String
          /** key */
          ,
          BufferStructObject>
      buffertMap = new ConcurrentHashMap<>();
  private final ConverterManager converterManager;
  private final ExpressionConverterFactory expressionConverterFactory;

  private final Lock lock = new ReentrantLock();

  public CRService(final MyberryStore myberryStore) {
    super(myberryStore);
    this.converterManager = ConverterManager.getInstance();
    this.expressionConverterFactory = converterManager.getExpressionConverterFactory();
  }

  @Override
  public PullIdResult getNewId(String key, Map<String, String> attachments) {
    if (!myberryStore.isExistKey(key)) {
      log.warn("invalid key: {}", key);
      return new PullIdResult(ResponseCode.KEY_NOT_EXISTED, emptyString());
    }

    CRComponent crc = (CRComponent) myberryStore.getComponentMap().get(key);
    crc.getLock().lock();
    try {
      long beginLockTimestamp = this.getSystemClock().now();
      myberryStore.setBeginTimeInLock(beginLockTimestamp);

      if (isReset(crc)) {
        crc.resetIncrNumber();
      }
      crc.setUpdateTime(System.currentTimeMillis());
      crc.incrementAndGet();

      myberryStore.updateBufferLong(
          (int) crc.getPhyOffset() + CRComponent.updateTimeHeader, crc.getUpdateTime());
      myberryStore.updateBufferLong(
          (int) crc.getPhyOffset() + CRComponent.incrNumberHeader, crc.getIncrNumber());
      myberryStore.incrMbid();
      if (this.isAlwaysFlush()) {
        myberryStore.save();
      }

      BufferStructObject bufferStructObject =
          expressionConverterFactory.doConvert(ExpressionParser.split(crc.getExpression()));
      bufferStructObject.setSid(myberryStore.getMySidFromDisk());

      myberryStore.setBeginTimeInLock(0);
      return new PullIdResult(
          ResponseCode.SUCCESS,
          ProduceMode.CR.getProduceCode(),
          bufferStructObject.getResult(crc, attachments));
    } catch (Exception e) {
      myberryStore.setBeginTimeInLock(0);
      log.error("getNewId() error: ", e.getMessage());
      return new PullIdResult(
          RemotingSysResponseCode.SYSTEM_ERROR, RemotingHelper.exceptionSimpleDesc(e));
    } finally {
      crc.getLock().unlock();
    }
  }

  @Override
  public AdminManageResult addComponent(Object... obj) {
    lock.lock();
    try {
      long currentTime = new Date().getTime();

      CRComponent crc = new CRComponent();
      crc.setCreateTime(currentTime);
      crc.setUpdateTime(currentTime);
      crc.setPhyOffset(myberryStore.getLastOffset());
      crc.setStatus(ComponentStatus.OPEN.getStatus());

      for (int i = 0; i < obj.length; i++) {
        String str = (String) obj[i];
        int length = str.getBytes(StoreConfig.MYBERRY_STORE_DEFAULT_CHARSET).length;
        if (i == 0) {
          crc.setKeyLength(length);
          crc.setKey(str);
        } else if (i == 1) {
          crc.setExpressionLength(length);
          crc.setExpression(str);
        }
      }

      String[] exp = ExpressionParser.split(crc.getExpression().trim());

      if (myberryStore.isWriteFull(
          CRComponent.FIXED_FIELD_SIZE + crc.getKeyLength() + crc.getExpressionLength())) {
        return new AdminManageResult(ResponseCode.DISK_FULL);
      } else if (myberryStore.isExistKey(crc.getKey())) {
        return new AdminManageResult(ResponseCode.KEY_EXISTED);
      }
      myberryStore.addComponent(crc);

      BufferStructObject bufferStructObject = expressionConverterFactory.doConvert(exp);
      bufferStructObject.setSid(myberryStore.getMySidFromDisk());
      buffertMap.put(crc.getKey(), bufferStructObject);

      log.info(
          "{} ++> add key: {}, expression: {} success.",
          this.getServiceName(),
          crc.getKey(),
          crc.getExpression());
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

    CRComponent crc = (CRComponent) myberryStore.getComponentMap().get(key);

    CRComponentData crcd = new CRComponentData();
    crcd.setKey(key);
    crcd.setExpression(crc.getExpression());
    crcd.setCode(CRComponentData.CODE);
    return new AdminManageResult(ResponseCode.SUCCESS, crcd);
  }

  @Override
  public void start() {
    converterManager.registerDefaultConverter();
    super.start();
    loadBufferMap();
  }

  @Override
  public void shutdown() {
    super.shutdown();
    converterManager.unRegisterDefaultConverter();
  }

  private void loadBufferMap() {
    Map<String, AbstractComponent> componentMap = myberryStore.getComponentMap();
    Iterator<Entry<String, AbstractComponent>> iterator = componentMap.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, AbstractComponent> next = iterator.next();
      CRComponent crc = (CRComponent) next.getValue();
      BufferStructObject bufferStructObject =
          expressionConverterFactory.doConvert(ExpressionParser.split(crc.getExpression()));
      bufferStructObject.setSid(myberryStore.getMySidFromDisk());
      buffertMap.put(next.getKey(), bufferStructObject);
    }
  }

  public boolean isReset(CRComponent crComponent) {
    int type = DateUtils.isIncludeTime(crComponent.getExpression());
    if (type == StrategyDate.NON_TIME) {
      return false;
    }

    long currentTimeMillis = System.currentTimeMillis();
    long updateTime = crComponent.getUpdateTime();
    if (currentTimeMillis > updateTime
        && !DateUtils.convertTime(type, currentTimeMillis)
            .equals(DateUtils.convertTime(type, updateTime))) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  public String getServiceName() {
    return CRService.class.getSimpleName();
  }
}
