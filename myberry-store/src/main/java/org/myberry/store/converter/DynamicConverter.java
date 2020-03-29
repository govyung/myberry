/*
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
package org.myberry.store.converter;

import org.myberry.common.expression.parser.DynamicParser;
import org.myberry.store.Converter;
import org.myberry.store.PlaceholderHandler;
import org.myberry.store.converter.handler.DynamicHandler;
import org.myberry.store.converter.impl.PlaceholderObject;

public class DynamicConverter implements Converter {

  private final DynamicParser dynamicParser;

  public DynamicConverter() {
    this.dynamicParser = new DynamicParser();
  }

  @Override
  public PlaceholderObject doConvert(String placeholder) {
    if (dynamicParser.doParse(placeholder)) {
      PlaceholderHandler placeholderHandler = new DynamicHandler(dynamicParser);
      placeholderHandler.handle(placeholder);

      return new PlaceholderObject(placeholderHandler);
    }
    return null;
  }
}
