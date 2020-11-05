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
*
* RemotingSerializable is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.protocol;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RemotingSerializable {
  private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

  public static byte[] encode(RemotingCommand cmd) {
    // String remark
    byte[] remarkBytes = null;
    int remarkLen = 0;
    if (cmd.getRemark() != null && cmd.getRemark().length() > 0) {
      remarkBytes = cmd.getRemark().getBytes(CHARSET_UTF8);
      remarkLen = remarkBytes.length;
    }

    // HashMap<String, String> extFields
    byte[] extFieldsBytes = null;
    int extLen = 0;
    if (cmd.getExtFields() != null && !cmd.getExtFields().isEmpty()) {
      extFieldsBytes = mapSerialize(cmd.getExtFields());
      extLen = extFieldsBytes.length;
    }

    int totalLen = calTotalLen(remarkLen, extLen);

    ByteBuffer headerBuffer = ByteBuffer.allocate(totalLen);
    // int code(~32767)
    headerBuffer.putShort((short) cmd.getCode());
    // int version(~32767)
    headerBuffer.putShort((short) cmd.getVersion());
    // int opaque
    headerBuffer.putInt(cmd.getOpaque());
    // int flag
    headerBuffer.putInt(cmd.getFlag());
    // String remark
    if (remarkBytes != null) {
      headerBuffer.putInt(remarkBytes.length);
      headerBuffer.put(remarkBytes);
    } else {
      headerBuffer.putInt(0);
    }
    // HashMap<String, String> extFields;
    if (extFieldsBytes != null) {
      headerBuffer.putInt(extFieldsBytes.length);
      headerBuffer.put(extFieldsBytes);
    } else {
      headerBuffer.putInt(0);
    }

    return headerBuffer.array();
  }

  public static byte[] mapSerialize(HashMap<String, String> map) {
    // keySize+key+valSize+val
    if (null == map || map.isEmpty()) return null;

    int totalLength = 0;
    int kvLength;
    Iterator<Entry<String, String>> it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> entry = it.next();
      if (entry.getKey() != null && entry.getValue() != null) {
        kvLength =
            // keySize + Key
            2
                + entry.getKey().getBytes(CHARSET_UTF8).length
                // valSize + val
                + 4
                + entry.getValue().getBytes(CHARSET_UTF8).length;
        totalLength += kvLength;
      }
    }

    ByteBuffer content = ByteBuffer.allocate(totalLength);
    byte[] key;
    byte[] val;
    it = map.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> entry = it.next();
      if (entry.getKey() != null && entry.getValue() != null) {
        key = entry.getKey().getBytes(CHARSET_UTF8);
        val = entry.getValue().getBytes(CHARSET_UTF8);

        content.putShort((short) key.length);
        content.put(key);

        content.putInt(val.length);
        content.put(val);
      }
    }

    return content.array();
  }

  private static int calTotalLen(int remark, int ext) {
    // int code(~32767)
    int length =
        2
            // int version(~32767)
            + 2
            // int opaque
            + 4
            // int flag
            + 4
            // String remark
            + 4 + remark
            // HashMap<String, String> extFields
            + 4 + ext;

    return length;
  }

  public static RemotingCommand decode(final byte[] headerArray) {
    RemotingCommand cmd = new RemotingCommand();
    ByteBuffer headerBuffer = ByteBuffer.wrap(headerArray);
    // int code(~32767)
    cmd.setCode(headerBuffer.getShort());
    // int version(~32767)
    cmd.setVersion(headerBuffer.getShort());
    // int opaque
    cmd.setOpaque(headerBuffer.getInt());
    // int flag
    cmd.setFlag(headerBuffer.getInt());
    // String remark
    int remarkLength = headerBuffer.getInt();
    if (remarkLength > 0) {
      byte[] remarkContent = new byte[remarkLength];
      headerBuffer.get(remarkContent);
      cmd.setRemark(new String(remarkContent, CHARSET_UTF8));
    }

    // HashMap<String, String> extFields
    int extFieldsLength = headerBuffer.getInt();
    if (extFieldsLength > 0) {
      byte[] extFieldsBytes = new byte[extFieldsLength];
      headerBuffer.get(extFieldsBytes);
      cmd.setExtFields(mapDeserialize(extFieldsBytes));
    }
    return cmd;
  }

  public static HashMap<String, String> mapDeserialize(byte[] bytes) {
    if (bytes == null || bytes.length <= 0) return null;

    HashMap<String, String> map = new HashMap<String, String>();
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

    short keySize;
    byte[] keyContent;
    int valSize;
    byte[] valContent;
    while (byteBuffer.hasRemaining()) {
      keySize = byteBuffer.getShort();
      keyContent = new byte[keySize];
      byteBuffer.get(keyContent);

      valSize = byteBuffer.getInt();
      valContent = new byte[valSize];
      byteBuffer.get(valContent);

      map.put(new String(keyContent, CHARSET_UTF8), new String(valContent, CHARSET_UTF8));
    }
    return map;
  }
}
