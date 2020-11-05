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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.myberry.common.codec.annotation.SerialField;
import org.myberry.common.codec.exception.UnsupportedCodecTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightCodec {

  private static final Logger log = LoggerFactory.getLogger(LightCodec.class);

  private static final int DEFAULT_MAX_CAPACITY = 16;

  public static int MAX_CAPACITY = 0;

  public static byte[] toBytes(MessageLite messageLite) {
    if (null == messageLite) {
      return null;
    }

    InOutStream inOutStream = null;
    try {
      int objectLength = getObjectLength(messageLite);
      inOutStream = new InOutStream(objectLength);
      writeFields(messageLite, inOutStream);
    } catch (UnsupportedCodecTypeException | IllegalAccessException e) {
      log.error(e.getMessage());
    }

    return inOutStream.getByteBuffer().array();
  }

  public static <T> T toObj(byte[] src, Class<T> clz) {
    if (null == src || src.length == 0) {
      return null;
    }

    LightDecoder.setMaxCapacity(MAX_CAPACITY == 0 ? DEFAULT_MAX_CAPACITY : MAX_CAPACITY);
    Object obj = null;
    try {
      obj = readFields(ByteBuffer.wrap(src), clz);
    } catch (UnsupportedCodecTypeException | IllegalAccessException e) {
      log.error(e.getMessage());
    }
    return (T) obj;
  }

  private static int getObjectLength(MessageLite messageLite)
      throws UnsupportedCodecTypeException, IllegalAccessException {
    Field[] fields = messageLite.getClass().getDeclaredFields();

    int size = 0;
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())
          && !Modifier.isFinal(field.getModifiers())
          && !Modifier.isTransient(field.getModifiers())
          && Modifier.isPrivate(field.getModifiers())) {

        SerialField annotation = field.getAnnotation(SerialField.class);
        if (null == annotation) {
          continue;
        }

        field.setAccessible(true);

        if (FieldType.INT.isValidForField(field)) {
          size += SerializedObjectFormat.getTagLength() + SerializedObjectFormat.getIntLength();
        } else if (FieldType.LONG.isValidForField(field)) {
          size += SerializedObjectFormat.getTagLength() + SerializedObjectFormat.getLongLength();
        } else if (FieldType.FLOAT.isValidForField(field)) {
          size += SerializedObjectFormat.getTagLength() + SerializedObjectFormat.getFloatLength();
        } else if (FieldType.DOUBLE.isValidForField(field)) {
          size += SerializedObjectFormat.getTagLength() + SerializedObjectFormat.getDoubleLength();
        } else if (FieldType.BOOLEAN.isValidForField(field)) {
          size += SerializedObjectFormat.getTagLength() + SerializedObjectFormat.getBooleanLength();
        } else if (FieldType.STRING.isValidForField(field)) {
          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getStringLength(
                      NullObjects.getDefaultStringIfAbsent((String) field.get(messageLite)));
        } else if (FieldType.MESSAGE.isValidForField(field)) {
          MessageLite ml = (MessageLite) field.get(messageLite);

          size +=
              SerializedObjectFormat.getTagLength() + SerializedObjectFormat.getMessageCodeLength();
          if (NullObjects.NOT_NULL_MESSAGE == NullObjects.getDefaultMessageLengthCodeIfAbsent(ml)) {
            size += getObjectLength(ml);
          }
        } else if (FieldType.INT_LIST.isValidForField(field)) {
          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getIntListLength(
                      (NullObjects.getDefaultListIfAbsent((List<Integer>) field.get(messageLite))
                          .size()));
        } else if (FieldType.LONG_LIST.isValidForField(field)) {
          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getLongListLength(
                      NullObjects.getDefaultListIfAbsent((List<Long>) field.get(messageLite))
                          .size());
        } else if (FieldType.FLOAT_LIST.isValidForField(field)) {
          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getFloatListLength(
                      NullObjects.getDefaultListIfAbsent((List<Float>) field.get(messageLite))
                          .size());
        } else if (FieldType.DOUBLE_LIST.isValidForField(field)) {
          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getDoubleListLength(
                      NullObjects.getDefaultListIfAbsent((List<Double>) field.get(messageLite))
                          .size());
        } else if (FieldType.BOOLEAN_LIST.isValidForField(field)) {
          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getBooleanListLength(
                      NullObjects.getDefaultListIfAbsent((List<Boolean>) field.get(messageLite))
                          .size());
        } else if (FieldType.STRING_LIST.isValidForField(field)) {
          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getStringListLength(
                      NullObjects.getDefaultListIfAbsent((List<String>) field.get(messageLite)));
        } else if (FieldType.MESSAGE_LIST.isValidForField(field)) {
          List<MessageLite> lists =
              NullObjects.getDefaultListIfAbsent((List<MessageLite>) field.get(messageLite));
          int listLength = 0;
          for (MessageLite ml : lists) {
            listLength += getObjectLength(ml);
          }

          size +=
              SerializedObjectFormat.getTagLength()
                  + SerializedObjectFormat.getMessageListLength(listLength);
        } else {
          throw new UnsupportedCodecTypeException("Unsupported field type: " + field.getType());
        }
      }
    }
    return size;
  }

  private static void writeFields(MessageLite messageLite, InOutStream inOutStream)
      throws UnsupportedCodecTypeException, IllegalAccessException {

    Field[] fields = messageLite.getClass().getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())
          && !Modifier.isFinal(field.getModifiers())
          && !Modifier.isTransient(field.getModifiers())
          && Modifier.isPrivate(field.getModifiers())) {

        SerialField annotation = field.getAnnotation(SerialField.class);
        if (null == annotation) {
          continue;
        }

        field.setAccessible(true);

        if (FieldType.INT.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.INT.id())
              .putInt(NullObjects.getDefaultIntIfAbsent((Integer) field.get(messageLite)));
        } else if (FieldType.LONG.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.LONG.id())
              .putLong(NullObjects.getDefaultLongIfAbsent((Long) field.get(messageLite)));
        } else if (FieldType.FLOAT.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.FLOAT.id())
              .putFloat(NullObjects.getDefaultFloatIfAbsent((Float) field.get(messageLite)));
        } else if (FieldType.DOUBLE.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.DOUBLE.id())
              .putDouble(NullObjects.getDefaultDoubleIfAbsent((Double) field.get(messageLite)));
        } else if (FieldType.BOOLEAN.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.BOOLEAN.id())
              .putBoolean(NullObjects.getDefaultBooleanIfAbsent((Boolean) field.get(messageLite)));
        } else if (FieldType.STRING.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.STRING.id())
              .putString(NullObjects.getDefaultStringIfAbsent((String) field.get(messageLite)));
        } else if (FieldType.MESSAGE.isValidForField(field)) {
          MessageLite ml = (MessageLite) field.get(messageLite);
          int messageLengthCode = NullObjects.getDefaultMessageLengthCodeIfAbsent(ml);
          inOutStream
              .putTag(annotation.ordinal(), FieldType.MESSAGE.id())
              .putMessageLengthCode(messageLengthCode);

          if (NullObjects.NOT_NULL_MESSAGE == messageLengthCode) {
            writeFields(ml, inOutStream);
          }
        } else if (FieldType.INT_LIST.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.INT_LIST.id())
              .putIntList(
                  NullObjects.getDefaultListIfAbsent((List<Integer>) field.get(messageLite)));
        } else if (FieldType.LONG_LIST.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.LONG_LIST.id())
              .putLongList(NullObjects.getDefaultListIfAbsent((List<Long>) field.get(messageLite)));
        } else if (FieldType.FLOAT_LIST.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.FLOAT_LIST.id())
              .putFloatList(
                  NullObjects.getDefaultListIfAbsent((List<Float>) field.get(messageLite)));
        } else if (FieldType.DOUBLE_LIST.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.DOUBLE_LIST.id())
              .putDoubleList(
                  NullObjects.getDefaultListIfAbsent((List<Double>) field.get(messageLite)));
        } else if (FieldType.BOOLEAN_LIST.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.BOOLEAN_LIST.id())
              .putBooleanList(
                  NullObjects.getDefaultListIfAbsent((List<Boolean>) field.get(messageLite)));
        } else if (FieldType.STRING_LIST.isValidForField(field)) {
          inOutStream
              .putTag(annotation.ordinal(), FieldType.STRING_LIST.id())
              .putStringList(
                  NullObjects.getDefaultListIfAbsent((List<String>) field.get(messageLite)));
        } else if (FieldType.MESSAGE_LIST.isValidForField(field)) {
          List<MessageLite> lists =
              NullObjects.getDefaultListIfAbsent((List<MessageLite>) field.get(messageLite));

          inOutStream
              .putTag(annotation.ordinal(), FieldType.MESSAGE_LIST.id())
              .putListSize(lists.size());
          for (MessageLite ml : lists) {
            writeFields(ml, inOutStream);
          }
        } else {
          throw new UnsupportedCodecTypeException("Unsupported field type: " + field.getType());
        }
      }
    }
  }

  private static Object readFields(ByteBuffer byteBuffer, Class<?> clz)
      throws UnsupportedCodecTypeException, IllegalAccessException {
    Object obj = newInstance(clz);

    int fieldsCount = 0;
    while (byteBuffer.position() < byteBuffer.limit()) {

      int serialNo = InOutStream.getSerialNo(byteBuffer);
      Field field = LightDecoder.getField(clz, serialNo);
      if (null != field) {
        field.setAccessible(true);
        fieldsCount++;
      }

      switch (FieldType.forId(InOutStream.getFieldId(byteBuffer))) {
        case INT:
          int inputInt = InOutStream.getInt(byteBuffer);
          if (null != field) {
            field.set(obj, inputInt);
          }

          break;
        case LONG:
          long inputLong = InOutStream.getLong(byteBuffer);
          if (null != field) {
            field.set(obj, inputLong);
          }

          break;
        case FLOAT:
          float inputFloat = InOutStream.getFloat(byteBuffer);
          if (null != field) {
            field.set(obj, inputFloat);
          }

          break;
        case DOUBLE:
          double inputDouble = InOutStream.getDouble(byteBuffer);
          if (null != field) {
            field.set(obj, inputDouble);
          }

          break;
        case BOOLEAN:
          boolean inputBoolean = InOutStream.getBoolean(byteBuffer);
          if (null != field) {
            field.set(obj, inputBoolean);
          }

          break;
        case STRING:
          String inputString = InOutStream.getString(byteBuffer);
          if (null != field) {
            field.set(obj, inputString);
          }

          break;
        case MESSAGE:
          if (NullObjects.NOT_NULL_MESSAGE == InOutStream.getMessageLengthCode(byteBuffer)) {
            if (null != field) {
              Object object = readFields(byteBuffer, field.getType());
              field.set(obj, object);
            }
          }
          break;
        case INT_LIST:
          List<Integer> inputIntList = InOutStream.getIntList(byteBuffer);
          if (null != field) {
            field.set(obj, inputIntList);
          }

          break;
        case LONG_LIST:
          List<Long> inputLongList = InOutStream.getLongList(byteBuffer);
          if (null != field) {
            field.set(obj, inputLongList);
          }

          break;
        case FLOAT_LIST:
          List<Float> inputFloatList = InOutStream.getFloatList(byteBuffer);
          if (null != field) {
            field.set(obj, inputFloatList);
          }

          break;
        case DOUBLE_LIST:
          List<Double> inputDoubleList = InOutStream.getDoubleList(byteBuffer);
          if (null != field) {
            field.set(obj, inputDoubleList);
          }

          break;
        case BOOLEAN_LIST:
          List<Boolean> inputBooleanList = InOutStream.getBooleanList(byteBuffer);
          if (null != field) {
            field.set(obj, inputBooleanList);
          }

          break;
        case STRING_LIST:
          List<String> inputStringList = InOutStream.getStringList(byteBuffer);
          if (null != field) {
            field.set(obj, inputStringList);
          }

          break;
        case MESSAGE_LIST:
          int fixedCapacity = InOutStream.getListSize(byteBuffer);
          List<Object> list = new ArrayList<>(fixedCapacity);
          int i = 0;
          while (i < fixedCapacity) {
            Type[] realTypes =
                ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
            list.add(readFields(byteBuffer, ((Class<?>) realTypes[0])));
            i++;
          }

          if (null != field) {
            field.set(obj, list);
          }

          break;
        default:
          throw new UnsupportedCodecTypeException(
              "Unsupported remote field name: " + field.getName());
      }

      if (fieldsCount >= LightDecoder.getRegistryTableSize(clz)) {
        break;
      }
    }
    return obj;
  }

  private static Object newInstance(Class<?> clz) {
    try {
      return clz.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      return null;
    }
  }

  static class LightDecoder {

    private static final RegistryTable<
            String /*ClassName*/, HashMap<Integer /*FieldOrdinal*/, Field /*Field*/>>
        registryTable = new RegistryTable<>();

    public static Field getField(Class<?> clz, int ordinal) {

      HashMap<Integer, Field> fieldHashMap = registryTable.get(clz.getName());
      if (null == fieldHashMap) {
        fieldHashMap = registryFields(clz);
        registryTable.put(clz.getName(), fieldHashMap); // unsafe thread, but no effect
      }
      return fieldHashMap.get(ordinal);
    }

    public static void setMaxCapacity(int maxCapacity) {
      registryTable.setMaxCapacity(maxCapacity);
    }

    private static HashMap<Integer, Field> registryFields(Class<?> clz) {
      HashMap<Integer, Field> fieldHashMap = new HashMap<>();

      Field[] fields = clz.getDeclaredFields();
      for (Field field : fields) {
        if (!Modifier.isStatic(field.getModifiers())
            && !Modifier.isFinal(field.getModifiers())
            && !Modifier.isTransient(field.getModifiers())
            && Modifier.isPrivate(field.getModifiers())) {

          SerialField annotation = field.getAnnotation(SerialField.class);
          if (null == annotation) {
            continue;
          }

          fieldHashMap.put(annotation.ordinal(), field);
        }
      }

      return fieldHashMap;
    }

    public static int getRegistryTableSize(Class<?> clz) {
      HashMap<Integer, Field> fieldHashMap = registryTable.get(clz.getName());
      if (null != fieldHashMap) {
        return fieldHashMap.size();
      } else {
        return 0;
      }
    }
  }
}
