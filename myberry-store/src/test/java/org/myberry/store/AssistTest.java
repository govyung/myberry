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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Date;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myberry.store.config.StoreConfig;

// @Ignore
public class AssistTest {

  private static int FILE_SLICE_SIZE = 1024 * 1024 * 8;
  private static String testPath = new File("").getAbsolutePath();
  private static String base_dir = testPath + File.separator + ".myberry";
  private static String storePathRootDir =
      base_dir + File.separator + "store" + File.separator + "myberry";
  private static RandomAccessFile randomAccessFile;
  private static FileChannel channel;
  private static MappedByteBuffer map;

  private static String key = "key1";
  private static String value = "[#time(day) 2 3 #sid(0) #sid(1) m z #incr(0)]";

  private static byte[] keyBytes;
  private static byte[] valueBytes;

  @BeforeClass
  public static void init() throws IOException {
    File file = new File(storePathRootDir);
    File fileTemp = new File(file.getParent());
    if (!fileTemp.exists()) {
      fileTemp.mkdirs();
    }
    if (!file.exists()) {
      file.createNewFile();
    }
    randomAccessFile = new RandomAccessFile(file, "rw");
    channel = randomAccessFile.getChannel();
    map = channel.map(MapMode.READ_WRITE, 0, FILE_SLICE_SIZE);

    keyBytes = key.getBytes(StoreConfig.MYBERRY_STORE_DEFAULT_CHARSET);
    valueBytes = value.getBytes(StoreConfig.MYBERRY_STORE_DEFAULT_CHARSET);
  }

  /**
   * write
   *
   * @throws Exception
   */
  @Test
  public void test_a() throws Exception {
    long headerTime = new Date().getTime();
    map.putLong(headerTime); // beginTimestamp
    map.putLong(headerTime); // endTimestamp
    map.putLong(64); // beginPhyoffset
    map.putLong(64); // endPhyoffset
    map.putLong(0); // mbid
    map.putLong(3); // epoch
    map.putInt(2); // maxSid
    map.putInt(5); // mySid
    map.putInt(6); // componentCount
    map.putInt(1); // mode

    // cr
    long contentTime = new Date().getTime();
    map.putLong(contentTime); // createTime
    map.putLong(contentTime); // updateTime
    map.putLong(64); // phyOffset
    map.putLong(1); // incrNumber
    map.putInt(1); // status

    map.putInt(keyBytes.length);
    for (int i = 0; i < keyBytes.length; i++) {
      map.put(keyBytes[i]);
    }

    map.putInt(valueBytes.length);
    for (int i = 0; i < valueBytes.length; i++) {
      map.put(valueBytes[i]);
    }

    map.force();
  }

  /**
   * read
   *
   * @throws UnsupportedEncodingException
   */
  @Test
  public void test_b() throws UnsupportedEncodingException {
    map.position(0);
    // store header
    long beginTimestamp = map.getLong();
    Assert.assertNotEquals(0, beginTimestamp);
    long endTimestamp = map.getLong();
    Assert.assertNotEquals(0, endTimestamp);
    long beginPhyoffset = map.getLong();
    Assert.assertEquals(64, beginPhyoffset);
    long endPhyoffset = map.getLong();
    Assert.assertEquals(64, endPhyoffset);
    long mbid = map.getLong();
    Assert.assertEquals(0, mbid);
    long epoch = map.getLong();
    Assert.assertEquals(3, epoch);
    int maxSid = map.getInt();
    Assert.assertEquals(2, maxSid);
    int mySid = map.getInt();
    Assert.assertEquals(5, mySid);
    int componentCount = map.getInt();
    Assert.assertEquals(6, componentCount);
    int mode = map.getInt();
    Assert.assertEquals(1, mode);
    // store component
    long createTime = map.getLong();
    Assert.assertNotEquals(0, createTime);
    long updateTime = map.getLong();
    Assert.assertNotEquals(0, updateTime);
    long phyOffset = map.getLong();
    Assert.assertEquals(64, phyOffset);
    long incrNumber = map.getLong();
    Assert.assertEquals(1, incrNumber);
    int status = map.getInt();
    Assert.assertEquals(1, status);

    int keyLenght = map.getInt();
    byte keys[] = new byte[keyLenght];
    for (int i = 0; i < keyLenght; i++) {
      keys[i] = map.get();
    }
    Assert.assertEquals(key, new String(keys, StoreConfig.MYBERRY_STORE_DEFAULT_CHARSET));

    int valueLenght = map.getInt();
    byte values[] = new byte[valueLenght];
    for (int i = 0; i < valueLenght; i++) {
      values[i] = map.get();
    }
    Assert.assertEquals(value, new String(values, StoreConfig.MYBERRY_STORE_DEFAULT_CHARSET));
  }

  @AfterClass
  public static void close() throws IOException {
    if (channel != null) {
      channel.close();
    }
    if (randomAccessFile != null) {
      randomAccessFile.close();
    }
  }
}
