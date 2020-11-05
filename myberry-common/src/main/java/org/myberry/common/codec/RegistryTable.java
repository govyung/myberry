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
import java.util.LinkedHashMap;
import sun.misc.Unsafe;

class RegistryTable<K, V> extends LinkedHashMap<K, V> {

  private static final sun.misc.Unsafe UNSAFE;
  private static final long valueOffset;
  transient volatile int maxCapacity = 0;

  static {
    try {
      Field field = Unsafe.class.getDeclaredField("theUnsafe");
      field.setAccessible(true);
      UNSAFE = (Unsafe) field.get(null);

      valueOffset = UNSAFE.objectFieldOffset(RegistryTable.class.getDeclaredField("maxCapacity"));
    } catch (Exception ex) {
      throw new Error(ex);
    }
  }

  public RegistryTable() {
    super(16, 0.75f, true);
  }

  @Override
  protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
    return size() > maxCapacity;
  }

  public boolean setMaxCapacity(int maxCapacity) {
    if (UNSAFE.compareAndSwapInt(this, valueOffset, 0, maxCapacity)) {
      return true;
    } else {
      return false;
    }
  }
}
