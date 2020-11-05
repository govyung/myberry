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

import java.util.List;

class NullObjects {

  static final int NULL_INT_DEFAULT = 0;
  static final long NULL_LONG_DEFAULT = 0L;
  static final float NULL_FLOAT_DEFAULT = 0F;
  static final double NULL_DOUBLE_DEFAULT = 0D;
  static final boolean NULL_BOOLEAN_DEFAULT = false;
  static final String NULL_STRING_DEFAULT = "";

  static final int NULL_MESSAGE = 0;
  static final int NOT_NULL_MESSAGE = 1;

  public static int getDefaultIntIfAbsent(Integer current) {
    return current == null ? NULL_INT_DEFAULT : current;
  }

  public static long getDefaultLongIfAbsent(Long current) {
    return current == null ? NULL_LONG_DEFAULT : current;
  }

  public static float getDefaultFloatIfAbsent(Float current) {
    return current == null ? NULL_FLOAT_DEFAULT : current;
  }

  public static double getDefaultDoubleIfAbsent(Double current) {
    return current == null ? NULL_DOUBLE_DEFAULT : current;
  }

  public static boolean getDefaultBooleanIfAbsent(Boolean current) {
    return current == null ? NULL_BOOLEAN_DEFAULT : current;
  }

  public static String getDefaultStringIfAbsent(String current) {
    return current == null ? NULL_STRING_DEFAULT : current;
  }

  public static int getDefaultMessageLengthCodeIfAbsent(MessageLite current) {
    return current == null ? NULL_MESSAGE : NOT_NULL_MESSAGE;
  }

  public static <T> List<T> getDefaultListIfAbsent(List<T> current) {
    return current == null ? Lists.newArrayList() : current;
  }
}
