/*
 * Copyright (c) 2020 gaoyang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.myberry.server.quarum;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import org.myberry.server.common.LoggerName;
import org.myberry.server.quarum.message.Ack;
import org.myberry.server.quarum.message.Inform;
import org.myberry.server.quarum.message.Notification;
import org.myberry.server.quarum.message.Proposal;
import org.myberry.server.quarum.message.Refuse;
import org.myberry.server.quarum.message.Sync;
import org.myberry.server.quarum.message.Vote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteBuf {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.QUORUM_LOGGER_NAME);

  public static final int VOTE = 0x01;
  public static final int INFORM = 0x02;
  public static final int ACK = 0x03;
  public static final int NOTIFICATION = 0x04;
  public static final int PROPOSAL = 0x05;
  public static final int SYNC = 0x06;
  public static final int REFUSE = 0x07;

  private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

  private static final Map<Integer, Class<? extends Message>> MESSAGE_TYPE_MAP = new HashMap<>();

  static {
    MESSAGE_TYPE_MAP.put(VOTE, Vote.class);
    MESSAGE_TYPE_MAP.put(INFORM, Inform.class);
    MESSAGE_TYPE_MAP.put(ACK, Ack.class);
    MESSAGE_TYPE_MAP.put(NOTIFICATION, Notification.class);
    MESSAGE_TYPE_MAP.put(PROPOSAL, Proposal.class);
    MESSAGE_TYPE_MAP.put(SYNC, Sync.class);
    MESSAGE_TYPE_MAP.put(REFUSE, Refuse.class);
  }

  private int version = 0;
  private String clusterName;
  private int runningMode;
  private int fileSize;
  private int connId;

  private transient Message data;

  public static ByteBuf create(
      int version, String clusterName, int runningMode, int fileSize, int connId, Message data) {
    ByteBuf byteBuf = new ByteBuf();
    byteBuf.setVersion(version);
    byteBuf.setClusterName(clusterName);
    byteBuf.setRunningMode(runningMode);
    byteBuf.setFileSize(fileSize);
    byteBuf.setConnId(connId);
    byteBuf.setData(data);
    return byteBuf;
  }

  public static int headerDecode(final ByteBuffer byteBuffer, final int pos) {
    int header = byteBuffer.getInt(pos);
    return (header >>> 4) + 4;
  }

  public static ByteBuf decode(final ByteBuffer byteBuffer, final int pos) {
    int header = byteBuffer.getInt(pos);
    if (header < 0) {
      return null;
    }

    int type = header & 0x0F;

    byte[] body = new byte[header >>> 4];
    for (int i = pos + 4; i < pos + 4 + body.length; i++) {
      body[i - pos - 4] = byteBuffer.get(i);
    }

    return bodyDecode(type, body);
  }

  private static ByteBuf bodyDecode(int type, final byte[] body) {
    return bodyDeserialize(body, MESSAGE_TYPE_MAP.get(type));
  }

  private static ByteBuf bodyDeserialize(
      final byte[] bodyArray, final Class<? extends Message> clz) {
    ByteBuf byteBuf = new ByteBuf();
    ByteBuffer bodyBuffer = ByteBuffer.wrap(bodyArray);
    // int version
    byteBuf.setVersion(bodyBuffer.getInt());
    // String clusterName
    int clusterNameLen = bodyBuffer.getInt();
    if (clusterNameLen > 0) {
      byte[] clusterNameContent = new byte[clusterNameLen];
      bodyBuffer.get(clusterNameContent);
      byteBuf.setClusterName(new String(clusterNameContent, CHARSET_UTF8));
    }
    // int runningMode
    byteBuf.setRunningMode(bodyBuffer.getInt());
    // int fileSize
    byteBuf.setFileSize(bodyBuffer.getInt());
    // int connId
    byteBuf.setConnId(bodyBuffer.getInt());
    // T data
    byte[] data = new byte[bodyArray.length - bodyBuffer.position()];
    bodyBuffer.get(data);
    if (null != clz) {
      byteBuf.setData(Message.decode(data, clz));
    }
    return byteBuf;
  }

  public ByteBuffer encode() {
    // byte[] body
    byte[] body = this.bodyEncode();
    // int header
    int header = this.headerEncode(body.length, data == null ? 0 : data.type());

    ByteBuffer byteBuffer = ByteBuffer.allocate(4 + body.length);
    byteBuffer.putInt(header);
    byteBuffer.put(body);
    byteBuffer.flip();
    return byteBuffer;
  }

  private byte[] bodyEncode() {
    byte[] bodySerialize = bodySerialize();
    byte[] data = new byte[0];
    try {
      data = this.data.encode();
    } catch (Exception e) {
      log.error("ByteBuf body encode exception: ", e);
    }

    ByteBuffer byteBuffer = ByteBuffer.allocate(bodySerialize.length + data.length);
    byteBuffer.put(bodySerialize);
    byteBuffer.put(data);
    return byteBuffer.array();
  }

  private byte[] bodySerialize() {
    // String clusterName
    byte[] clusterNameBytes = null;
    int clusterNameLen = 0;
    if (getClusterName() != null && getClusterName().length() > 0) {
      clusterNameBytes = getClusterName().getBytes(CHARSET_UTF8);
      clusterNameLen = clusterNameBytes.length;
    }

    ByteBuffer bodyBuffer = ByteBuffer.allocate(4 + 4 + clusterNameLen + +4 + 4 + 4);
    // int version
    bodyBuffer.putInt(this.getVersion());
    // String clusterName
    if (getClusterName() != null) {
      bodyBuffer.putInt(clusterNameBytes.length);
      bodyBuffer.put(clusterNameBytes);
    } else {
      bodyBuffer.putInt(0);
    }
    // int runningMode
    bodyBuffer.putInt(this.getRunningMode());
    // int fileSize
    bodyBuffer.putInt(this.getFileSize());
    // int connId
    bodyBuffer.putInt(this.getConnId());
    return bodyBuffer.array();
  }

  /*
   * type max value is 7
   */
  private int headerEncode(int bodyLength, int type) {
    return (bodyLength << 4) | type;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public int getRunningMode() {
    return runningMode;
  }

  public void setRunningMode(int runningMode) {
    this.runningMode = runningMode;
  }

  public int getFileSize() {
    return fileSize;
  }

  public void setFileSize(int fileSize) {
    this.fileSize = fileSize;
  }

  public int getConnId() {
    return connId;
  }

  public void setConnId(int connId) {
    this.connId = connId;
  }

  public Message getData() {
    return data;
  }

  public void setData(Message data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("ByteBuf{")
        .append("version=")
        .append(version)
        .append(", clusterName='")
        .append(clusterName)
        .append('\'')
        .append(", runningMode=")
        .append(runningMode)
        .append(", fileSize=")
        .append(fileSize)
        .append(", connId=")
        .append(connId)
        .append(", data=")
        .append(data)
        .append('}')
        .toString();
  }
}
