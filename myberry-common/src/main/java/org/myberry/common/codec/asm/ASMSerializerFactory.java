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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.FieldVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import org.myberry.common.codec.MessageLite;
import org.myberry.common.codec.annotation.SerialField;
import org.myberry.common.codec.asm.serializer.MessageLiteSerializer;
import org.myberry.common.codec.exception.UnsupportedCodecTypeException;
import org.myberry.common.codec.formatter.InOutStream;
import org.myberry.common.codec.support.FieldType;
import org.myberry.common.codec.util.NullObjects;

public class ASMSerializerFactory implements Opcodes {

  private static final ASMClassLoader amsClassLoader = new ASMClassLoader();

  private static final ConcurrentMap<String /*ClassName*/, MessageLiteSerializer>
      messageLiteSerializerMap = new ConcurrentHashMap<>();

  private static final String ASM_CLASS_SERIALIZER_PRE = "ASMSerializer";
  private static final String ASM_DEFAULT_CONSTRUCTOR_NAME = "<init>";
  private static final String ASM_FINAL_FIELD_NAME_SERIALIZER_ADPTER = "asmSerializerAdpter";
  private static final String CLASS_MESSAGELITEERIALIZER_METHOD_NAME_WRITEMESSAGELITE =
      "writeMessageLite";
  private static final String CLASS_ASMSERIALIZERADPTER_METHOD_NAME_WRITETO = "writeTo";
  private static final String CLASS_FIELDTYPE_METHOD_NAME_ID = "id";
  private static final String CLASS_INOUTSTREAM_METHOD_NAME_PUTTAG = "putTag";
  private static final String CLASS_BOXEDTYPE_METHOD_NAME_VALUEOF = "valueOf";
  private static final String CLASS_LIST_METHOD_NAME_SIZE = "size";

  private static final Map<FieldType, TypeMethodName /*typeMethodName*/> typeMethodNameMap =
      new HashMap<>(14);

  static {
    typeMethodNameMap.put(FieldType.INT, new TypeMethodName("putInt", "getDefaultIntIfAbsent"));
    typeMethodNameMap.put(FieldType.LONG, new TypeMethodName("putLong", "getDefaultLongIfAbsent"));
    typeMethodNameMap.put(
        FieldType.FLOAT, new TypeMethodName("putFloat", "getDefaultFloatIfAbsent"));
    typeMethodNameMap.put(
        FieldType.DOUBLE, new TypeMethodName("putDouble", "getDefaultDoubleIfAbsent"));
    typeMethodNameMap.put(
        FieldType.BOOLEAN, new TypeMethodName("putBoolean", "getDefaultBooleanIfAbsent"));
    typeMethodNameMap.put(
        FieldType.STRING, new TypeMethodName("putString", "getDefaultStringIfAbsent"));
    typeMethodNameMap.put(FieldType.MESSAGELITE, new TypeMethodName("putMessageLiteLength", ""));
    typeMethodNameMap.put(
        FieldType.INT_LIST, new TypeMethodName("putIntList", "getDefaultListIfAbsent"));
    typeMethodNameMap.put(
        FieldType.LONG_LIST, new TypeMethodName("putLongList", "getDefaultListIfAbsent"));
    typeMethodNameMap.put(
        FieldType.FLOAT_LIST, new TypeMethodName("putFloatList", "getDefaultListIfAbsent"));
    typeMethodNameMap.put(
        FieldType.DOUBLE_LIST, new TypeMethodName("putDoubleList", "getDefaultListIfAbsent"));
    typeMethodNameMap.put(
        FieldType.BOOLEAN_LIST, new TypeMethodName("putBooleanList", "getDefaultListIfAbsent"));
    typeMethodNameMap.put(
        FieldType.STRING_LIST, new TypeMethodName("putStringList", "getDefaultListIfAbsent"));
    typeMethodNameMap.put(
        FieldType.MESSAGELITE_LIST, new TypeMethodName("putListSize", "getDefaultListIfAbsent"));
  }

  private final ASMSerializerAdpter asmSerializerAdpter;

  public ASMSerializerFactory(final ASMSerializerAdpter asmSerializerAdpter) {
    this.asmSerializerAdpter = asmSerializerAdpter;
  }

  public MessageLiteSerializer getSerializer(Class<? extends MessageLite> clazz) throws Exception {

    MessageLiteSerializer messageLiteSerializer = messageLiteSerializerMap.get(clazz.getName());
    if (messageLiteSerializer != null) {
      return messageLiteSerializer;
    }

    messageLiteSerializer = factoryASMSerializer(clazz);
    messageLiteSerializerMap.putIfAbsent(clazz.getName(), messageLiteSerializer);
    return messageLiteSerializer;
  }

  private MessageLiteSerializer factoryASMSerializer(Class<?> clazz) throws Exception {
    // define AMS class
    ClassWriter cw = new ClassWriter(0);

    ASMType asmClazzType =
        ASMUtils.generateASMClassTypeName(
            MessageLiteSerializer.class.getPackage().getName(),
            ASM_CLASS_SERIALIZER_PRE + clazz.getSimpleName());
    cw.visit(
        V1_5,
        ACC_PUBLIC + ACC_SUPER,
        asmClazzType.getNameType(),
        null,
        Type.getType(Object.class).getInternalName(),
        new String[] {Type.getType(MessageLiteSerializer.class).getInternalName()});

    // define final field
    FieldVisitor fieldVisitor =
        cw.visitField(
            ACC_PRIVATE + ACC_FINAL,
            ASM_FINAL_FIELD_NAME_SERIALIZER_ADPTER,
            Type.getType(ASMSerializerAdpter.class).getDescriptor(),
            null,
            null);
    fieldVisitor.visitEnd();

    // define default constructor
    MethodVisitor mv =
        cw.visitMethod(
            ACC_PUBLIC,
            ASM_DEFAULT_CONSTRUCTOR_NAME,
            ASMUtils.getMethodDescriptor(Type.getType(ASMSerializerAdpter.class).getDescriptor()),
            null,
            null);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(
        INVOKESPECIAL,
        Type.getType(Object.class).getInternalName(),
        ASM_DEFAULT_CONSTRUCTOR_NAME,
        ASMUtils.getMethodDescriptor(),
        false);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitFieldInsn(
        PUTFIELD,
        asmClazzType.getNameType(),
        ASM_FINAL_FIELD_NAME_SERIALIZER_ADPTER,
        Type.getType(ASMSerializerAdpter.class).getDescriptor());
    mv.visitInsn(RETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();

    // define
    // org.myberry.common.codec.asm.serializer.MessageLiteSerializer.writeMessageLite(MessageLite,
    // InOutStream)
    mv =
        cw.visitMethod(
            ACC_PUBLIC,
            CLASS_MESSAGELITEERIALIZER_METHOD_NAME_WRITEMESSAGELITE,
            ASMUtils.getMethodDescriptor(
                Type.VOID_TYPE,
                Type.getType(MessageLite.class).getDescriptor(),
                Type.getType(InOutStream.class).getDescriptor()),
            null,
            new String[] {Type.getType(Exception.class).getInternalName()});
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitTypeInsn(CHECKCAST, Type.getType(clazz).getInternalName());
    mv.visitVarInsn(ASTORE, 3);

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
          writeBaseTypeFormatField(mv, clazz, field, FieldType.INT);
        } else if (FieldType.LONG.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.LONG);
        } else if (FieldType.FLOAT.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.FLOAT);
        } else if (FieldType.DOUBLE.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.DOUBLE);
        } else if (FieldType.BOOLEAN.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.BOOLEAN);
        } else if (FieldType.STRING.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.STRING);
        } else if (FieldType.MESSAGELITE.isValidForField(field)) {
          writeMessageLiteFormatField(
              asmClazzType.getNameType(), mv, clazz, field, FieldType.MESSAGELITE);
        } else if (FieldType.INT_LIST.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.INT_LIST);
        } else if (FieldType.LONG_LIST.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.LONG_LIST);
        } else if (FieldType.FLOAT_LIST.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.FLOAT_LIST);
        } else if (FieldType.DOUBLE_LIST.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.DOUBLE_LIST);
        } else if (FieldType.BOOLEAN_LIST.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.BOOLEAN_LIST);
        } else if (FieldType.STRING_LIST.isValidForField(field)) {
          writeBaseTypeFormatField(mv, clazz, field, FieldType.STRING_LIST);
        } else if (FieldType.MESSAGELITE_LIST.isValidForField(field)) {
          writeMessageLiteListFormatField(
              asmClazzType.getNameType(), mv, clazz, field, FieldType.MESSAGELITE_LIST);
        } else {
          throw new UnsupportedCodecTypeException("Unsupported field type: " + field.getType());
        }
      }
    }

    // end
    mv.visitInsn(RETURN);
    mv.visitMaxs(3, 4);
    mv.visitEnd();

    byte[] code = cw.toByteArray();

    Class<?> serializerClass =
        amsClassLoader.defineClassByASM(asmClazzType.getName(), code, 0, code.length);
    Constructor<?> constructor = serializerClass.getConstructor(ASMSerializerAdpter.class);
    Object instance = constructor.newInstance(asmSerializerAdpter);

    return (MessageLiteSerializer) instance;
  }

  private void writeBaseTypeFormatField(
      MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    mv.visitVarInsn(ALOAD, 2);
    writeTag(mv, field.getAnnotation(SerialField.class).ordinal(), fieldType.toString());
    mv.visitVarInsn(ALOAD, 3);
    writeValueOrValueList(mv, clazz, field, fieldType);
    mv.visitInsn(POP);
  }

  private void writeMessageLiteFormatField(
      String nameType, MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    mv.visitVarInsn(ALOAD, 2);
    writeTag(mv, field.getAnnotation(SerialField.class).ordinal(), fieldType.toString());
    mv.visitInsn(POP);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(
        GETFIELD,
        nameType,
        ASM_FINAL_FIELD_NAME_SERIALIZER_ADPTER,
        Type.getType(ASMSerializerAdpter.class).getDescriptor());
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(clazz).getInternalName(),
        ASMUtils.getFieldGetterMethodName(field),
        ASMUtils.getMethodDescriptor(Type.getType(field.getType()), null),
        false);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(ASMSerializerAdpter.class).getInternalName(),
        CLASS_ASMSERIALIZERADPTER_METHOD_NAME_WRITETO,
        ASMUtils.getMethodDescriptor(
            Type.getType(MessageLite.class).getDescriptor(),
            Type.getType(InOutStream.class).getDescriptor()),
        false);
  }

  private void writeMessageLiteListFormatField(
      String nameType, MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    mv.visitVarInsn(ALOAD, 2);
    writeTag(mv, field.getAnnotation(SerialField.class).ordinal(), fieldType.toString());
    mv.visitVarInsn(ALOAD, 3);
    writeMessageLiteListSize(mv, clazz, field, fieldType);
    mv.visitInsn(POP);

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(
        GETFIELD,
        nameType,
        ASM_FINAL_FIELD_NAME_SERIALIZER_ADPTER,
        Type.getType(ASMSerializerAdpter.class).getDescriptor());
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(clazz).getInternalName(),
        ASMUtils.getFieldGetterMethodName(field),
        ASMUtils.getMethodDescriptor(Type.getType(List.class), null),
        false);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(ASMSerializerAdpter.class).getInternalName(),
        CLASS_ASMSERIALIZERADPTER_METHOD_NAME_WRITETO,
        ASMUtils.getMethodDescriptor(
            Type.getType(List.class).getDescriptor(),
            Type.getType(InOutStream.class).getDescriptor()),
        false);
  }

  private void writeTag(MethodVisitor mv, int ordinal, String fieldName) {
    mv.visitLdcInsn(ordinal);
    mv.visitFieldInsn(
        GETSTATIC,
        Type.getType(FieldType.class).getInternalName(),
        fieldName,
        Type.getType(FieldType.class).getDescriptor());
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(FieldType.class).getInternalName(),
        CLASS_FIELDTYPE_METHOD_NAME_ID,
        ASMUtils.getMethodDescriptor(Type.getType(int.class), null),
        false);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(InOutStream.class).getInternalName(),
        CLASS_INOUTSTREAM_METHOD_NAME_PUTTAG,
        ASMUtils.getMethodDescriptor(
            Type.getType(InOutStream.class),
            Type.getType(int.class).getDescriptor(),
            Type.getType(int.class).getDescriptor()),
        false);
  }

  private void writeValueOrValueList(
      MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    if (!fieldType.isList()) {
      writeValue(mv, clazz, field, fieldType);
    } else {
      writeValueList(mv, clazz, field, fieldType);
    }
  }

  private void writeValue(MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(clazz).getInternalName(),
        ASMUtils.getFieldGetterMethodName(field),
        ASMUtils.getMethodDescriptor(
            Type.getType(
                field.getType() != fieldType.getBoxedType()
                    ? fieldType.getType()
                    : fieldType.getBoxedType()),
            null),
        false);
    if (field.getType() != fieldType.getBoxedType()
        && fieldType.isDifferentBetweenTypeAndBoxedType()) {
      mv.visitMethodInsn(
          INVOKESTATIC,
          Type.getType(fieldType.getBoxedType()).getInternalName(),
          CLASS_BOXEDTYPE_METHOD_NAME_VALUEOF,
          ASMUtils.getMethodDescriptor(
              Type.getType(fieldType.getBoxedType()),
              Type.getType(fieldType.getType()).getDescriptor()),
          false);
    }
    mv.visitMethodInsn(
        INVOKESTATIC,
        Type.getType(NullObjects.class).getInternalName(),
        typeMethodNameMap.get(fieldType).getDefaultValueName(),
        ASMUtils.getMethodDescriptor(
            Type.getType(fieldType.getType()),
            Type.getType(fieldType.getBoxedType()).getDescriptor()),
        false);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(InOutStream.class).getInternalName(),
        typeMethodNameMap.get(fieldType).getPutName(),
        ASMUtils.getMethodDescriptor(
            Type.getType(InOutStream.class), Type.getType(fieldType.getType()).getDescriptor()),
        false);
  }

  private void writeValueList(MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(clazz).getInternalName(),
        ASMUtils.getFieldGetterMethodName(field),
        ASMUtils.getMethodDescriptor(Type.getType(List.class), null),
        false);
    mv.visitMethodInsn(
        INVOKESTATIC,
        Type.getType(NullObjects.class).getInternalName(),
        typeMethodNameMap.get(fieldType).getDefaultValueName(),
        ASMUtils.getMethodDescriptor(
            Type.getType(List.class), Type.getType(List.class).getDescriptor()),
        false);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(InOutStream.class).getInternalName(),
        typeMethodNameMap.get(fieldType).getPutName(),
        ASMUtils.getMethodDescriptor(
            Type.getType(InOutStream.class), Type.getType(List.class).getDescriptor()),
        false);
  }

  private void writeMessageLiteListSize(
      MethodVisitor mv, Class<?> clazz, Field field, FieldType fieldType) {
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(clazz).getInternalName(),
        ASMUtils.getFieldGetterMethodName(field),
        ASMUtils.getMethodDescriptor(Type.getType(List.class)),
        false);
    mv.visitMethodInsn(
        INVOKESTATIC,
        Type.getType(NullObjects.class).getInternalName(),
        typeMethodNameMap.get(fieldType).getDefaultValueName(),
        ASMUtils.getMethodDescriptor(
            Type.getType(List.class), Type.getType(List.class).getDescriptor()),
        false);
    mv.visitMethodInsn(
        INVOKEINTERFACE,
        Type.getType(List.class).getInternalName(),
        CLASS_LIST_METHOD_NAME_SIZE,
        ASMUtils.getMethodDescriptor(Type.getType(int.class), null),
        true);
    mv.visitMethodInsn(
        INVOKEVIRTUAL,
        Type.getType(InOutStream.class).getInternalName(),
        typeMethodNameMap.get(fieldType).getPutName(),
        ASMUtils.getMethodDescriptor(
            Type.getType(InOutStream.class), Type.getType(int.class).getDescriptor()),
        false);
  }

  private static class TypeMethodName {
    private final String putName;
    private final String defaultValueName;

    public TypeMethodName(String putName, String defaultValueName) {
      this.putName = putName;
      this.defaultValueName = defaultValueName;
    }

    public String getPutName() {
      return putName;
    }

    public String getDefaultValueName() {
      return defaultValueName;
    }
  }
}
