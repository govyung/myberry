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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.myberry.store.AbstractComponent;
import org.myberry.store.DefaultMyberryStore;
import org.myberry.store.common.LoggerName;
import org.myberry.store.config.StoreConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileService {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  private final DefaultMyberryStore myberryStore;
  private ArrayList<BlockFile> blockFileList = new ArrayList<>();
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  public FileService(final DefaultMyberryStore store) {
    this.myberryStore = store;
  }

  public void addComponent(AbstractComponent abstractComponent) {
    BlockFile blockFile = blockFileList.get(blockFileList.size() - 1);
    blockFile.addComponent(abstractComponent);
  }

  public void removeComponent(String key) {}

  public Collection<AbstractComponent> queryAllComponent() {
    BlockFile blockFile = blockFileList.get(blockFileList.size() - 1);
    return blockFile.getComponentMap().values();
  }

  public BlockFile getBlockFile(int index){
    return blockFileList.get(index);
  }

  public boolean load(final StoreConfig storeConfig) {
    try {
      BlockFile f = new BlockFile(storeConfig);
      f.loadHeader();
      f.loadComponent();

      this.blockFileList.add(f);
    } catch (IOException e) {
      log.error("load file error", e);
      return false;
    } catch (NumberFormatException e) {
      log.error("load file error", e);
    }

    return true;
  }

  public void unload() {
    try {
      this.readWriteLock.writeLock().lock();
      for (BlockFile f : this.blockFileList) {
        f.unload();
      }
      this.blockFileList.clear();
    } catch (Exception e) {
      log.error("destroy exception", e);
    } finally {
      this.readWriteLock.writeLock().unlock();
    }
  }
}
