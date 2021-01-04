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
package org.myberry.common.codec.formatter;

import java.nio.charset.Charset;
import java.util.List;
import org.myberry.common.codec.util.ElasticBuffer;
import org.myberry.common.codec.util.LinkedListStack;
import org.myberry.common.codec.util.Lists;

public final class InOutStream {

  private static final Charset UTF_8 = Charset.forName("UTF-8");
  public static final int DEFAULT_MESSAGELITE_LENGTH = 0;

  private final ElasticBuffer elasticBuffer;

  private final LinkedListStack<Integer> stack = new LinkedListStack<>();

  public InOutStream() {
    this.elasticBuffer = ElasticBuffer.getOrCreateElasticBuffer(0);
  }

  public static short getSerialNo(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getShort();
  }

  public static short getFieldId(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getShort();
  }
  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getTagLength()}
   *
   * @param serialNo
   * @param fieldId
   * @return
   */
  public InOutStream putTag(int serialNo, int fieldId) {
    elasticBuffer.putShort((short) serialNo);
    elasticBuffer.putShort((short) fieldId);
    return this;
  }

  public void markMessageLiteLength() {
    stack.push(elasticBuffer.position());
    elasticBuffer.putInt(DEFAULT_MESSAGELITE_LENGTH);
  }

  public static int getMessageLiteLength(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getInt();
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getMessageLiteLength()}
   *
   * @return
   */
  public InOutStream putMessageLiteLength() {
    Integer markedMessageLiteLengthOffset = stack.pop();
    elasticBuffer.putInt(
        markedMessageLiteLengthOffset,
        elasticBuffer.position() - markedMessageLiteLengthOffset - 4);
    return this;
  }

  public static int getInt(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getInt();
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getIntLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putInt(int output) {
    elasticBuffer.putInt(output);
    return this;
  }

  public static long getLong(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getLong();
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getLongLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putLong(long output) {
    elasticBuffer.putLong(output);
    return this;
  }

  public static float getFloat(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getFloat();
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getFloatLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putFloat(float output) {
    elasticBuffer.putFloat(output);
    return this;
  }

  public static double getDouble(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getDouble();
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getDoubleLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putDouble(double output) {
    elasticBuffer.putDouble(output);
    return this;
  }

  public static boolean getBoolean(ElasticBuffer elasticBuffer) {
    return transformBoolean(elasticBuffer.get());
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getBooleanLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putBoolean(boolean output) {
    elasticBuffer.put(transformBoolean(output));
    return this;
  }

  public static String getString(ElasticBuffer elasticBuffer) {
    int fieldLength = elasticBuffer.getInt();
    byte[] stringByte = elasticBuffer.getArray(fieldLength);
    return read(stringByte);
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getStringLength(String)}
   *
   * @param output
   * @return
   */
  public InOutStream putString(String output) {
    byte[] stringByte = InOutStream.write(output);
    elasticBuffer.putInt(stringByte.length);
    elasticBuffer.putArray(stringByte);
    return this;
  }

  public static int getListSize(ElasticBuffer elasticBuffer) {
    return elasticBuffer.getInt();
  }

  public InOutStream putListSize(int size) {
    elasticBuffer.putInt(size);
    return this;
  }

  public static List<Integer> getIntList(ElasticBuffer elasticBuffer) {
    int fixedCapacity = getListSize(elasticBuffer);
    List<Integer> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getInt(elasticBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getIntListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putIntList(List<Integer> outputs) {
    putListSize(outputs.size());
    for (Integer output : outputs) {
      elasticBuffer.putInt(output);
    }
    return this;
  }

  public static List<Long> getLongList(ElasticBuffer elasticBuffer) {
    int fixedCapacity = getListSize(elasticBuffer);
    List<Long> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getLong(elasticBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getLongListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putLongList(List<Long> outputs) {
    putListSize(outputs.size());
    for (Long output : outputs) {
      elasticBuffer.putLong(output);
    }
    return this;
  }

  public static List<Float> getFloatList(ElasticBuffer elasticBuffer) {
    int fixedCapacity = getListSize(elasticBuffer);
    List<Float> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getFloat(elasticBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getFloatListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putFloatList(List<Float> outputs) {
    putListSize(outputs.size());
    for (Float output : outputs) {
      elasticBuffer.putFloat(output);
    }
    return this;
  }

  public static List<Double> getDoubleList(ElasticBuffer elasticBuffer) {
    int fixedCapacity = getListSize(elasticBuffer);
    List<Double> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getDouble(elasticBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getDoubleListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putDoubleList(List<Double> outputs) {
    putListSize(outputs.size());
    for (Double output : outputs) {
      elasticBuffer.putDouble(output);
    }
    return this;
  }

  public static List<Boolean> getBooleanList(ElasticBuffer elasticBuffer) {
    int fixedCapacity = getListSize(elasticBuffer);
    List<Boolean> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getBoolean(elasticBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getBooleanListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putBooleanList(List<Boolean> outputs) {
    putListSize(outputs.size());
    for (Boolean output : outputs) {
      elasticBuffer.put(transformBoolean(output));
    }
    return this;
  }

  public static List<String> getStringList(ElasticBuffer elasticBuffer) {
    int fixedCapacity = getListSize(elasticBuffer);
    List<String> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getString(elasticBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.formatter.SerializedObjectFormat#getStringListLength(List)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putStringList(List<String> outputs) {
    putListSize(outputs.size());
    for (String output : outputs) {
      byte[] stringByte = InOutStream.write(output);
      elasticBuffer.putInt(stringByte.length);
      elasticBuffer.putArray(stringByte);
    }
    return this;
  }

  public static byte[] write(String output) {
    return output.getBytes(UTF_8);
  }

  public static String read(byte[] input) {
    return new String(input, UTF_8);
  }

  private byte transformBoolean(boolean outPut) {
    if (outPut) {
      return (byte) 1;
    }
    return (byte) 0;
  }

  private static boolean transformBoolean(byte input) {
    if (input == (byte) 1) {
      return true;
    }
    return false;
  }

  public byte[] getWrittenBuffer() {
    return elasticBuffer.getArray(0, elasticBuffer.position());
  }
}
