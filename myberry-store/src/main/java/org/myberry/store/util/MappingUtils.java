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
package org.myberry.store.util;

import org.myberry.common.ProduceMode;
import org.myberry.store.AbstractComponent;
import org.myberry.store.CRComponent;
import org.myberry.store.NSComponent;

public class MappingUtils {

  public static int getProduceModeMapping(String produceName) {
    Integer code = ProduceMode.getCode(produceName);
    if (code == null) {
      return ProduceMode.getCode(ProduceMode.CR.getProduceName());
    } else {
      return code.intValue();
    }
  }

  public static AbstractComponent getComponentMapping(int produceMode) {
    if (ProduceMode.CR.getProduceCode() == produceMode) {
      return new CRComponent();
    } else if (ProduceMode.NS.getProduceCode() == produceMode) {
      return new NSComponent();
    } else {
      return new CRComponent();
    }
  }
}
