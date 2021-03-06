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
package org.myberry.store;

import java.util.concurrent.ConcurrentMap;
import org.myberry.store.config.StoreConfig;

/**
 * This class defines contracting interfaces to implement, allowing third-party vendor to use
 * customized component store.
 */
public interface MyberryStore {

  /**
   * Launch this component store.
   *
   * @throws Exception if there is any error.
   */
  void start() throws Exception;

  /** Shutdown this component store. */
  void shutdown();

  /** Add a component into store. */
  void addComponent(AbstractComponent abstractComponent);

  /** Remove a component into store. */
  void removeComponent(String key);

  /** Got persistent mbid. */
  long getMbidFromDisk();

  /** Got persistent epoch. */
  long getEpochFromDisk();

  /** Got persistent max sid. */
  int getMaxSidFromDisk();

  /** Got persistent sid. */
  int getMySidFromDisk();

  /** Got persistent componentCount. */
  int getComponentCountFromDisk();

  /** Got persistent produceMode. */
  int getProduceModeFromDisk();

  /** Get Store Config. */
  StoreConfig getStoreConfig();

  /**
   * Get component map.
   *
   * @return
   */
  ConcurrentMap<String, AbstractComponent> getComponentMap();

  /**
   * Return the current timestamp of the store.
   *
   * @return current time in milliseconds since 1970-01-01.
   */
  long now();

  /** * Get PhysicalFile last offset. */
  int getLastOffset();

  /** Data file write status. */
  boolean isWriteFull(int size);

  /** Index key existence status. */
  boolean isExistKey(String key);

  /**
   * Get byteBuffer for ha send data.
   *
   * @param offset
   * @return
   */
  byte[] getSyncByteBuffer(int offset);

  /**
   * * Set byteBuffer for ha receive data.
   *
   * @param src
   */
  void setSyncByteBuffer(byte[] src);

  /** Increase mbid. */
  void incrMbid();

  /** Set persistent my sid. */
  void setMySid(int mySid);

  /** Set persistent max sid. */
  void setMaxSid(int maxSid);

  /** Set current epoch. */
  void setEpoch(long epoch);

  /** Set component count. */
  void setComponentCount(int componentCount);

  /** Flush disk. */
  void save();

  /**
   * Update long buffer
   *
   * @param index
   * @param value
   */
  void updateBufferLong(int index, long value);

  /**
   * Update int buffer
   *
   * @param index
   * @param value
   */
  void updateBufferInt(int index, int value);

  /**
   * Check if the operation system page cache is busy or not.
   *
   * @return true if the OS page cache is busy; false otherwise.
   */
  boolean isOSPageCacheBusy();

  /**
   * Instance has shutdown.
   *
   * @return
   */
  boolean hasShutdown();

  /**
   * Set flush disk begin time.
   *
   * @param beginTimeInLock
   */
  void setBeginTimeInLock(long beginTimeInLock);
}
