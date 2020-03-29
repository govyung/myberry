/*
* Copyright 2018 gaoyang.  All rights reserved.
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import org.myberry.store.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappedFile {

  protected static final Logger log = LoggerFactory.getLogger(LoggerName.STORE_LOGGER_NAME);

  protected RandomAccessFile raf;

  protected FileChannel fileChannel;

  private MappedByteBuffer mappedByteBuffer;

  public MappedFile() {}

  public MappedFile(final String fileName, final int fileSize) throws IOException {
    init(fileName, fileSize);
  }

  private void init(final String fileName, final int fileSize) throws IOException {
    File file = new File(fileName);
    ensureDirOK(file.getParent());
    isCreatedFile(file);

    boolean ok = false;
    try {

      this.raf = new RandomAccessFile(file, "rw");
      this.fileChannel = this.raf.getChannel();
      this.mappedByteBuffer = this.fileChannel.map(MapMode.READ_WRITE, 0, fileSize);
      ok = true;
    } catch (FileNotFoundException e) {
      log.error("create file channel [{}] Failed. ", file.getAbsoluteFile(), e);
      throw e;
    } catch (IOException e) {
      log.error("map file [{}] Failed. ", file.getAbsoluteFile(), e);
      throw e;
    } finally {
      if (!ok && this.fileChannel != null) {
        this.fileChannel.close();
      }
    }
  }

  public void flush() {
    this.mappedByteBuffer.force();
  }

  public static void ensureDirOK(final String dirName) {
    if (dirName != null) {
      File f = new File(dirName);
      if (!f.exists()) {
        boolean result = f.mkdirs();
        log.info("{} mkdir {}", dirName, (result ? "OK" : "Failed"));
      }
    }
  }

  public static void isCreatedFile(final File file) throws IOException {
    if (file.exists()) {
      return;
    } else {
      file.createNewFile();
    }
  }

  public void destroy() {
    if (this.fileChannel != null) {
      try {
        this.fileChannel.close();
      } catch (IOException e) {
        log.error("close fileChannel Exception: ", e.getMessage());
      }
    }
    if (this.raf != null) {
      try {
        this.raf.close();
      } catch (IOException e) {
        log.error("close randomAccessFile Exception: ", e.getMessage());
      }
    }
  }

  public FileChannel getFileChannel() {
    return fileChannel;
  }

  public MappedByteBuffer getMappedByteBuffer() {
    return mappedByteBuffer;
  }
}
