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
package org.myberry.store.config;

import java.io.File;
import org.myberry.common.annotation.ImportantField;

public class StoreConfig {

  public static final String MYBERRY_STORE_DEFAULT_CHARSET = "UTF-8";
  public static final String MYBERRY_STORE_FILE_NAME = "myberry";

  @ImportantField private int mySid;

  @ImportantField private String produceMode;

  private boolean alwaysFlush;
  private long osPageCacheBusyTimeOutMills = 1000;

  private int fileSize = 1024 * 1024 * 8;

  @ImportantField private String storePath = System.getProperty("user.home");

  public String getProduceMode() {
    return produceMode;
  }

  public void setProduceMode(String produceMode) {
    this.produceMode = produceMode;
  }

  public int getMySid() {
    return mySid;
  }

  public void setMySid(int mySid) {
    this.mySid = mySid;
  }

  public boolean isAlwaysFlush() {
    return alwaysFlush;
  }

  public void setAlwaysFlush(boolean alwaysFlush) {
    this.alwaysFlush = alwaysFlush;
  }

  public long getOsPageCacheBusyTimeOutMills() {
    return osPageCacheBusyTimeOutMills;
  }

  public void setOsPageCacheBusyTimeOutMills(final long osPageCacheBusyTimeOutMills) {
    this.osPageCacheBusyTimeOutMills = osPageCacheBusyTimeOutMills;
  }

  public String getStorePath() {
    return storePath;
  }

  public int getFileSize() {
    return fileSize;
  }

  public void setFileSize(int fileSize) {
    this.fileSize = fileSize;
  }

  public void setStorePath(String storePath) {
    this.storePath = storePath;
  }

  public String getStoreRootDir() {
    return storePath + File.separator + ".myberry";
  }
}
