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

import org.junit.Assert;
import org.junit.Test;
import org.myberry.common.expression.impl.ExpressionObject;
import org.myberry.common.expression.impl.ExpressionParser;
import org.myberry.common.expression.impl.ExpressionString;
import org.myberry.common.expression.impl.ParserManager;
import org.myberry.store.converter.handler.DynamicHandler;
import org.myberry.store.converter.handler.IncrHandler;
import org.myberry.store.converter.handler.SidHandler;
import org.myberry.store.converter.impl.BufferStructObject;
import org.myberry.store.converter.impl.ConverterManager;
import org.myberry.store.converter.impl.ExpressionConverterFactory;
import org.myberry.store.converter.impl.PlaceholderObject;

public class ConverterTest {

  @Test
  public void test() throws Exception {
    ParserManager pm = ParserManager.getInstance();
    pm.registerDefaultPlugins();
    ConverterManager cm = ConverterManager.getInstance();
    cm.registerDefaultConverter();

    ExpressionObject eo = new ExpressionObject(8);
    eo.addPlaceholder("#time(month)") //
        .addPlaceholder("5") //
        .addPlaceholder("#sid(1)") //
        .addPlaceholder("#sid(0)") //
        .addPlaceholder("#incr(0)") //
        .addPlaceholder("#rand(2)") //
        .addPlaceholder("m") //
        .addPlaceholder("$dynamic(Hi_)");

    Assert.assertEquals(true, pm.parseExpression(eo));
    Assert.assertEquals(
        true,
        pm.parseExpression(
            new ExpressionString("[#time(month) 5 #sid(1) #sid(0) #incr(0) #rand(2) m $dynamic(Hi_)]")));

    if (pm.parseExpression(eo)) {
      String[] split = ExpressionParser.split(eo.toString());
      ExpressionConverterFactory expressionConverterFactory = cm.getExpressionConverterFactory();

      BufferStructObject doConvert = expressionConverterFactory.doConvert(split);

      PlaceholderObject[] placeholderObjects = doConvert.getPlaceholderObjects();

      StringBuilder sb = new StringBuilder(placeholderObjects.length);
      for (PlaceholderObject placeholderObject : placeholderObjects) {
        if (placeholderObject.getPlaceholderHandler() instanceof IncrHandler) {
          sb.append(placeholderObject.getPlaceholderHandler().get("4"));
        } else if (placeholderObject.getPlaceholderHandler() instanceof SidHandler) {
          sb.append(placeholderObject.getPlaceholderHandler().get("12"));
        } else if (placeholderObject.getPlaceholderHandler() instanceof DynamicHandler) {
          sb.append(placeholderObject.getPlaceholderHandler().get(null));
        } else {
          sb.append(placeholderObject.getPlaceholderHandler().get(null));
        }
      }

      System.out.println(sb.toString());
      Assert.assertNotNull(sb.toString());
    }
  }
}
