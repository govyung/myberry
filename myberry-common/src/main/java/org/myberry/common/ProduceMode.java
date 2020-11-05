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
package org.myberry.common;

import java.util.HashMap;
import java.util.Map;

public enum ProduceMode {
  CR(1, "cr"),
  NS(2, "ns");

  private int produceCode;
  private String produceName;

  private static Map<String, Integer> map = new HashMap<>();

  static {
    ProduceMode[] values = ProduceMode.values();
    for (ProduceMode produceMode : values) {
      map.put(produceMode.produceName, produceMode.produceCode);
    }
  }

  ProduceMode(int produceCode, String produceName) {
    this.produceCode = produceCode;
    this.produceName = produceName;
  }

  public static Integer getCode(String produceName) {
    return map.get(produceName);
  }

  public int getProduceCode() {
    return produceCode;
  }

  public String getProduceName() {
    return produceName;
  }
}
