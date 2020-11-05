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
package org.myberry.common.codec;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

final class InOutStream {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private final ByteBuffer byteBuffer;

  public InOutStream(int bufferSize) {
    this.byteBuffer = ByteBuffer.allocate(bufferSize);
  }

  public static short getSerialNo(ByteBuffer byteBuffer) {
    return byteBuffer.getShort();
  }

  public static short getFieldId(ByteBuffer byteBuffer) {
    return byteBuffer.getShort();
  }
  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getTagLength()}
   *
   * @param serialNo
   * @param fieldId
   * @return
   */
  public InOutStream putTag(int serialNo, int fieldId) {
    byteBuffer.putShort((short) serialNo);
    byteBuffer.putShort((short) fieldId);
    return this;
  }

  public static int getMessageLengthCode(ByteBuffer byteBuffer) {
    return byteBuffer.getInt();
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getMessageCodeLength()}
   *
   * @param messageLengthCode
   * @return
   */
  public InOutStream putMessageLengthCode(int messageLengthCode) {
    byteBuffer.putInt(messageLengthCode);
    return this;
  }

  public static int getInt(ByteBuffer byteBuffer) {
    return byteBuffer.getInt();
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getIntLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putInt(Integer output) {
    byteBuffer.putInt(output);
    return this;
  }

  public static long getLong(ByteBuffer byteBuffer) {
    return byteBuffer.getLong();
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getLongLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putLong(Long output) {
    byteBuffer.putLong(output);
    return this;
  }

  public static float getFloat(ByteBuffer byteBuffer) {
    return byteBuffer.getFloat();
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getFloatLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putFloat(Float output) {
    byteBuffer.putFloat(output);
    return this;
  }

  public static double getDouble(ByteBuffer byteBuffer) {
    return byteBuffer.getDouble();
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getDoubleLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putDouble(Double output) {
    byteBuffer.putDouble(output);
    return this;
  }

  public static boolean getBoolean(ByteBuffer byteBuffer) {
    return transformBoolean(byteBuffer.get());
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getBooleanLength()}
   *
   * @param output
   * @return
   */
  public InOutStream putBoolean(Boolean output) {
    byteBuffer.put(transformBoolean(output));
    return this;
  }

  public static String getString(ByteBuffer byteBuffer) {
    int fieldLength = byteBuffer.getInt();
    byte[] stringByte = getStringByte(byteBuffer, fieldLength);

    return read(stringByte);
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getStringLength(String)}
   *
   * @param output
   * @return
   */
  public InOutStream putString(String output) {
    byte[] stringByte = InOutStream.write(output);
    byteBuffer.putInt(stringByte.length);
    byteBuffer.put(stringByte);
    return this;
  }

  public static int getListSize(ByteBuffer byteBuffer) {
    return byteBuffer.getInt();
  }

  public InOutStream putListSize(int size) {
    byteBuffer.putInt(size);
    return this;
  }

  public static List<Integer> getIntList(ByteBuffer byteBuffer) {
    int fixedCapacity = getListSize(byteBuffer);
    List<Integer> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getInt(byteBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getIntListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putIntList(List<Integer> outputs) {
    putListSize(outputs.size());
    for (Integer output : outputs) {
      byteBuffer.putInt(output);
    }
    return this;
  }

  public static List<Long> getLongList(ByteBuffer byteBuffer) {
    int fixedCapacity = getListSize(byteBuffer);
    List<Long> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getLong(byteBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getLongListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putLongList(List<Long> outputs) {
    putListSize(outputs.size());
    for (Long output : outputs) {
      byteBuffer.putLong(output);
    }
    return this;
  }

  public static List<Float> getFloatList(ByteBuffer byteBuffer) {
    int fixedCapacity = getListSize(byteBuffer);
    List<Float> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getFloat(byteBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getFloatListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putFloatList(List<Float> outputs) {
    putListSize(outputs.size());
    for (Float output : outputs) {
      byteBuffer.putFloat(output);
    }
    return this;
  }

  public static List<Double> getDoubleList(ByteBuffer byteBuffer) {
    int fixedCapacity = getListSize(byteBuffer);
    List<Double> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getDouble(byteBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getDoubleListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putDoubleList(List<Double> outputs) {
    putListSize(outputs.size());
    for (Double output : outputs) {
      byteBuffer.putDouble(output);
    }
    return this;
  }

  public static List<Boolean> getBooleanList(ByteBuffer byteBuffer) {
    int fixedCapacity = getListSize(byteBuffer);
    List<Boolean> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getBoolean(byteBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getBooleanListLength(int)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putBooleanList(List<Boolean> outputs) {
    putListSize(outputs.size());
    for (Boolean output : outputs) {
      byteBuffer.put(transformBoolean(output));
    }
    return this;
  }

  public static List<String> getStringList(ByteBuffer byteBuffer) {
    int fixedCapacity = getListSize(byteBuffer);
    List<String> list = Lists.newArrayList(fixedCapacity);
    int i = 0;
    while (i < fixedCapacity) {
      list.add(getString(byteBuffer));
      i++;
    }
    return list;
  }

  /**
   * {@link org.myberry.common.codec.SerializedObjectFormat#getStringListLength(List)}
   *
   * @param outputs
   * @return
   */
  public InOutStream putStringList(List<String> outputs) {
    putListSize(outputs.size());
    for (String output : outputs) {
      byte[] stringByte = InOutStream.write(output);
      byteBuffer.putInt(stringByte.length);
      byteBuffer.put(stringByte);
    }
    return this;
  }

  public void flip() {
    byteBuffer.flip();
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

  private static byte[] getStringByte(ByteBuffer byteBuffer, int stringLength) {
    byte[] bytes = new byte[stringLength];
    System.arraycopy(byteBuffer.array(), byteBuffer.position(), bytes, 0, stringLength);
    byteBuffer.position(byteBuffer.position() + stringLength);
    return bytes;
  }

  public ByteBuffer getByteBuffer() {
    return byteBuffer;
  }
}
