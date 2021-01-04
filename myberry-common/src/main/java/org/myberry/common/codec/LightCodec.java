/*
 * MIT License
 *
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
package org.myberry.common.codec;

import java.util.ArrayList;
import java.util.List;
import org.myberry.common.codec.asm.ASMDeserializerAdpter;
import org.myberry.common.codec.asm.ASMSerializerAdpter;
import org.myberry.common.codec.asm.deserializer.MessageLiteDeserializer;
import org.myberry.common.codec.exception.UnsupportedCodecTypeException;
import org.myberry.common.codec.formatter.InOutStream;
import org.myberry.common.codec.support.FieldType;
import org.myberry.common.codec.util.ElasticBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightCodec {

  private static final Logger log = LoggerFactory.getLogger(LightCodec.class);

  private static final int MAGIC = 0x5d7657dd;

  private static final int ROOT_MESSAGELITE_SERIALNO = 0;

  private static final ASMSerializerAdpter asmSerializerAdpter = ASMSerializerAdpter.getInstance();

  private static final ASMDeserializerAdpter asmDeserializerAdpter =
      ASMDeserializerAdpter.getInstance();

  static {
    char[] signature = new char[] {'M', 'Y', 'B', 'E', 'R', 'R', 'Y'};
    int m = 0;
    for (int i = signature.length - 1; i >= 0; i--) {
      m |= signature[i] << i * 4;
    }
    // m = 0x5d7657dd, MAGIC algorithm
  }

  public static byte[] toBytes(MessageLite messageLite) {
    if (null == messageLite) {
      return null;
    }

    InOutStream inOutStream = null;
    try {
      inOutStream = new InOutStream();
      putMagic(inOutStream);

      putRootMessageLiteTag(inOutStream);
      asmSerializerAdpter.writeTo(messageLite, inOutStream);
    } catch (Exception e) {
      log.error(e.getMessage());
    }

    return inOutStream.getWrittenBuffer();
  }

  public static <T> T toObj(byte[] src, Class<T> clz) {
    if (null == src || src.length == 0 || null == clz) {
      return null;
    }

    ElasticBuffer elasticBuffer = ElasticBuffer.wrap(src);

    Object obj = null;
    try {
      int magic = getMagic(elasticBuffer);
      short rootSerialNo = getRootSerialNo(elasticBuffer);
      short rootFieldId = getRootFieldId(elasticBuffer);
      int rootLength = getRootMessageLiteLength(elasticBuffer);

      if (isLegal(magic, rootSerialNo, rootFieldId)) {
        obj = BufferReader.readTo(elasticBuffer, elasticBuffer.position() + rootLength, clz);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error(e.getMessage());
    }
    return (T) obj;
  }

  private static void putMagic(InOutStream inOutStream) {
    inOutStream.putInt(MAGIC);
  }

  private static int getMagic(ElasticBuffer elasticBuffer) {
    return InOutStream.getInt(elasticBuffer);
  }

  private static void putRootMessageLiteTag(InOutStream inOutStream) {
    inOutStream.putTag(ROOT_MESSAGELITE_SERIALNO, FieldType.MESSAGELITE.id());
  }

  private static short getRootSerialNo(ElasticBuffer elasticBuffer) {
    return InOutStream.getSerialNo(elasticBuffer);
  }

  private static short getRootFieldId(ElasticBuffer elasticBuffer) {
    return InOutStream.getFieldId(elasticBuffer);
  }

  private static int getRootMessageLiteLength(ElasticBuffer elasticBuffer) {
    return InOutStream.getMessageLiteLength(elasticBuffer);
  }

  private static boolean isLegal(int magic, int rootSerialNo, int rootFieldId) {
    if (magic != MAGIC) {
      throw new RuntimeException("LightCodec signature error");
    }

    if (rootSerialNo != ROOT_MESSAGELITE_SERIALNO) {
      throw new RuntimeException(
          "The root node's SerialNo must be 0, and the current one is " + rootSerialNo);
    }

    if (rootFieldId != FieldType.MESSAGELITE.id()) {
      throw new RuntimeException(
          "The root node's FieldType must be "
              + FieldType.MESSAGELITE.id()
              + ", and the current one is "
              + rootFieldId);
    }
    return true;
  }

  private static class BufferReader {

    static Object readTo(ElasticBuffer elasticBuffer, int length, Class clazz) throws Exception {

      MessageLiteDeserializer messageLiteDeserializer = asmDeserializerAdpter.readTo(clazz);
      Object instance = messageLiteDeserializer.createInstance();
      while (elasticBuffer.position() < length) {

        int serialNo = InOutStream.getSerialNo(elasticBuffer);
        switch (FieldType.forId(InOutStream.getFieldId(elasticBuffer))) {
          case INT:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getInt(elasticBuffer));
            break;
          case LONG:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getLong(elasticBuffer));
            break;
          case FLOAT:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getFloat(elasticBuffer));
            break;
          case DOUBLE:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getDouble(elasticBuffer));
            break;
          case BOOLEAN:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getBoolean(elasticBuffer));
            break;
          case STRING:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getString(elasticBuffer));
            break;
          case MESSAGELITE:
            int messageLiteLength = InOutStream.getMessageLiteLength(elasticBuffer);
            if (messageLiteLength != InOutStream.DEFAULT_MESSAGELITE_LENGTH) {
              Class innerClass =
                  asmDeserializerAdpter.getClassByFieldSerialNo(clazz.getName(), serialNo);
              messageLiteDeserializer.readMessageLite(
                  instance,
                  serialNo,
                  readTo(elasticBuffer, elasticBuffer.position() + messageLiteLength, innerClass));
            }
            break;
          case INT_LIST:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getIntList(elasticBuffer));
            break;
          case LONG_LIST:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getLongList(elasticBuffer));
            break;
          case FLOAT_LIST:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getFloatList(elasticBuffer));
            break;
          case DOUBLE_LIST:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getDoubleList(elasticBuffer));
            break;
          case BOOLEAN_LIST:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getBooleanList(elasticBuffer));
            break;
          case STRING_LIST:
            messageLiteDeserializer.readMessageLite(
                instance, serialNo, InOutStream.getStringList(elasticBuffer));
            break;
          case MESSAGELITE_LIST:
            Class signatureClass =
                asmDeserializerAdpter.getClassByFieldSerialNo(clazz.getName(), serialNo);
            int fixedCapacity = InOutStream.getListSize(elasticBuffer);
            List<Object> list = new ArrayList<>(fixedCapacity);
            int i = 0;
            while (i < fixedCapacity) {
              int messageLiteLengthInArray = InOutStream.getMessageLiteLength(elasticBuffer);
              list.add(
                  readTo(
                      elasticBuffer,
                      elasticBuffer.position() + messageLiteLengthInArray,
                      signatureClass));
              i++;
            }
            messageLiteDeserializer.readMessageLite(instance, serialNo, list);
            break;
          default:
            throw new UnsupportedCodecTypeException(
                "Unsupported remote field type when serialNo = " + serialNo);
        }
      }
      return instance;
    }
  }
}
