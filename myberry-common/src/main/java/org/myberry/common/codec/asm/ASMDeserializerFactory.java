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
package org.myberry.common.codec.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import org.myberry.common.codec.MessageLite;
import org.myberry.common.codec.annotation.SerialField;
import org.myberry.common.codec.asm.deserializer.MessageLiteDeserializer;
import org.myberry.common.codec.exception.UnsupportedCodecTypeException;
import org.myberry.common.codec.support.FieldType;

public class ASMDeserializerFactory implements Opcodes {

  private static final ASMClassLoader amsClassLoader = new ASMClassLoader();

  private static final ConcurrentMap<String /*ClassName*/, MessageLiteDeserializer>
      messageLiteDeserializerMap = new ConcurrentHashMap<>();

  private static final String ASM_CLASS_DESERIALIZER_PRE = "ASMDeserializer";
  private static final String ASM_DEFAULT_CONSTRUCTOR_NAME = "<init>";
  private static final String ASM_FINAL_FIELD_NAME_DESERIALIZER_ADPTER = "asmDeserializerAdpter";
  private static final String CLASS_MESSAGELITEDESERIALIZER_METHOD_NAME_CREATEINSTANCE =
      "createInstance";
  private static final String CLASS_MESSAGELITEDESERIALIZER_METHOD_NAME_READMESSAGELITE =
      "readMessageLite";

  private static final Map<FieldType, String /*typeName*/> typeNameMap = new HashMap<>(5);

  static {
    typeNameMap.put(FieldType.INT, "intValue");
    typeNameMap.put(FieldType.LONG, "longValue");
    typeNameMap.put(FieldType.FLOAT, "floatValue");
    typeNameMap.put(FieldType.DOUBLE, "doubleValue");
    typeNameMap.put(FieldType.BOOLEAN, "booleanValue");
  }

  private final ConcurrentMap<String /*ClassName*/, Map<Integer /*serialNo*/, Class>> serialNoMap;

  public ASMDeserializerFactory(final ConcurrentMap<String, Map<Integer, Class>> serialNoMap) {
    this.serialNoMap = serialNoMap;
  }

  public MessageLiteDeserializer getDeserializer(Class<?> clazz) throws Exception {
    MessageLiteDeserializer messageLiteDeserializer =
        messageLiteDeserializerMap.get(clazz.getName());
    if (null != messageLiteDeserializer) {
      return messageLiteDeserializer;
    }

    if (!isMessageLite(clazz)) {
      throw new RuntimeException(clazz.getName() + " did not directly implement MessageLite");
    }
    messageLiteDeserializer = factoryASMDeserializer(clazz);
    messageLiteDeserializerMap.putIfAbsent(clazz.getName(), messageLiteDeserializer);
    return messageLiteDeserializer;
  }

  private MessageLiteDeserializer factoryASMDeserializer(Class<?> clazz) throws Exception {
    // define AMS class
    ClassWriter cw = new ClassWriter(0);

    ASMType asmClazzType =
        ASMUtils.generateASMClassTypeName(
            MessageLiteDeserializer.class.getPackage().getName(),
            ASM_CLASS_DESERIALIZER_PRE + clazz.getSimpleName());
    cw.visit(
        V1_5,
        ACC_PUBLIC + ACC_SUPER,
        asmClazzType.getNameType(),
        null,
        Type.getType(Object.class).getInternalName(),
        new String[] {Type.getType(MessageLiteDeserializer.class).getInternalName()});

    // define default constructor
    MethodVisitor mv =
        cw.visitMethod(
            ACC_PUBLIC, ASM_DEFAULT_CONSTRUCTOR_NAME, ASMUtils.getMethodDescriptor(), null, null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(
        INVOKESPECIAL,
        Type.getType(Object.class).getInternalName(),
        ASM_DEFAULT_CONSTRUCTOR_NAME,
        ASMUtils.getMethodDescriptor(),
        false);
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

    // define
    // org.myberry.common.codec.asm.deserializer.MessageLiteDeserializer.createInstance()
    mv =
        cw.visitMethod(
            ACC_PUBLIC,
            CLASS_MESSAGELITEDESERIALIZER_METHOD_NAME_CREATEINSTANCE,
            ASMUtils.getMethodDescriptor(Type.getType(Object.class)),
            null,
            null);
    mv.visitCode();
    mv.visitTypeInsn(NEW, Type.getType(clazz).getInternalName());
    mv.visitInsn(DUP);
    mv.visitMethodInsn(
        INVOKESPECIAL,
        Type.getType(clazz).getInternalName(),
        ASM_DEFAULT_CONSTRUCTOR_NAME,
        ASMUtils.getMethodDescriptor(),
        false);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(2, 1);
    mv.visitEnd();

    // define
    // org.myberry.common.codec.asm.deserializer.MessageLiteDeserializer.readMessageLite(Object,
    // int, Object)
    mv =
        cw.visitMethod(
            ACC_PUBLIC,
            CLASS_MESSAGELITEDESERIALIZER_METHOD_NAME_READMESSAGELITE,
            ASMUtils.getMethodDescriptor(
                Type.getType(Object.class),
                Type.getType(Object.class).getDescriptor(),
                Type.getType(int.class).getDescriptor(),
                Type.getType(Object.class).getDescriptor()),
            null,
            new String[] {Type.getType(RuntimeException.class).getInternalName()});
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(CHECKCAST, Type.getType(clazz).getInternalName());
    mv.visitVarInsn(ASTORE, 4);

    Map<Integer, Class> serialNoFieldMap = new HashMap<>();
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers())
          && !Modifier.isFinal(field.getModifiers())
          && !Modifier.isTransient(field.getModifiers())
          && Modifier.isPrivate(field.getModifiers())) {

        SerialField annotation = field.getAnnotation(SerialField.class);
        if (null == annotation) {
          continue;
        } else if (annotation.ordinal() < 0) {
          throw new IllegalAccessException(
              "SerialField ordinal cannot be less than or equal to zero");
        }

        if (FieldType.INT.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.INT);
        } else if (FieldType.LONG.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.LONG);
        } else if (FieldType.FLOAT.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.FLOAT);
        } else if (FieldType.DOUBLE.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.DOUBLE);
        } else if (FieldType.BOOLEAN.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.BOOLEAN);
        } else if (FieldType.STRING.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.STRING);
        } else if (FieldType.MESSAGELITE.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.MESSAGELITE);
          serialNoFieldMap.put(annotation.ordinal(), field.getType());
        } else if (FieldType.INT_LIST.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.INT_LIST);
        } else if (FieldType.LONG_LIST.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.LONG_LIST);
        } else if (FieldType.FLOAT_LIST.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.FLOAT_LIST);
        } else if (FieldType.DOUBLE_LIST.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.DOUBLE_LIST);
        } else if (FieldType.BOOLEAN_LIST.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.BOOLEAN_LIST);
        } else if (FieldType.STRING_LIST.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.STRING_LIST);
        } else if (FieldType.MESSAGELITE_LIST.isValidForField(field)) {
          readTypeFormatField(mv, clazz, field, FieldType.MESSAGELITE_LIST);
          serialNoFieldMap.put(annotation.ordinal(), getFieldActualType(field));
        } else {
          throw new UnsupportedCodecTypeException("Unsupported field type: " + field.getType());
        }
      }
    }
    mv.visitVarInsn(ALOAD, 4);
    serialNoMap.putIfAbsent(clazz.getName(), serialNoFieldMap);

    // end
    mv.visitInsn(ARETURN);
    mv.visitMaxs(3, 5);
    mv.visitEnd();

    byte[] code = cw.toByteArray();

    Class<?> serializerClass =
        amsClassLoader.defineClassByASM(asmClazzType.getName(), code, 0, code.length);
    Constructor<?> constructor = serializerClass.getConstructor();
    Object instance = constructor.newInstance();

    return (MessageLiteDeserializer) instance;
  }

  private void readTypeFormatField(
      MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    Label ifLable = new Label();
    mv.visitVarInsn(ILOAD, 2);
    mv.visitLdcInsn(field.getAnnotation(SerialField.class).ordinal());
    mv.visitJumpInsn(IF_ICMPNE, ifLable);

    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitTypeInsn(CHECKCAST, Type.getType(checkCastType(field, fieldType)).getInternalName());
    if (!fieldType.isList()
        && fieldType.isDifferentBetweenTypeAndBoxedType()
        && field.getType() == fieldType.getType()) {
      mv.visitMethodInsn(
          INVOKEVIRTUAL,
          Type.getType(fieldType.getBoxedType()).getInternalName(),
          typeNameMap.get(fieldType),
          ASMUtils.getMethodDescriptor(Type.getType(fieldType.getType()), null),
          false);
    }
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(clazz).getInternalName(),
        ASMUtils.getFieldSetterMethodName(field),
        ASMUtils.getMethodDescriptor(
            Type.getType(getSetterParameterType(field, fieldType)).getDescriptor()),
        false);

    mv.visitLabel(ifLable);
  }

  private Class checkCastType(Field field, FieldType fieldType) {
    if (fieldType.isList()) {
      return List.class;
    } else if (FieldType.MESSAGELITE == fieldType) {
      return field.getType();
    } else {
      return fieldType.getBoxedType();
    }
  }

  private Class getSetterParameterType(Field field, FieldType fieldType) {
    if (fieldType.isList()) {
      return List.class;
    } else {
      return field.getType();
    }
  }

  private boolean isMessageLite(Class<?> clazz) {
    Class<?>[] interfaces = clazz.getInterfaces();

    for (Class clz : interfaces) {
      if (clz == MessageLite.class) {
        return true;
      }
    }
    return false;
  }

  private Class getFieldActualType(Field field) {
    java.lang.reflect.Type[] realTypes =
        ((ParameterizedType) field.getGenericType()).getActualTypeArguments();
    return (Class<?>) realTypes[0];
  }
}
