/*
* Copyright 2018 gaoyang.  All rights reserved.
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
package org.myberry.store.converter.impl;

import org.myberry.store.converter.DynamicConverter;
import org.myberry.store.converter.IncrConverter;
import org.myberry.store.converter.LetterConverter;
import org.myberry.store.converter.NumberConverter;
import org.myberry.store.converter.RandomConverter;
import org.myberry.store.converter.SidConverter;
import org.myberry.store.converter.TimeConverter;

public class ConverterManager {

  private final ExpressionConverterFactory expressionConverterFactory;

  private static class SingletonHolder {
    private static final ConverterManager INSTANCE = new ConverterManager();
  }

  private ConverterManager() {
    this.expressionConverterFactory = new ExpressionConverterFactory();
  }

  public static ConverterManager getInstance() {
    return SingletonHolder.INSTANCE;
  }

  public void registerDefaultConverter() {
    expressionConverterFactory
        .addConverter(new LetterConverter()) //
        .addConverter(new NumberConverter()) //
        .addConverter(new RandomConverter()) //
        .addConverter(new TimeConverter()) //
        .addConverter(new IncrConverter()) //
        .addConverter(new SidConverter()) //
        .addConverter(new DynamicConverter());
  }

  public void unRegisterDefaultConverter() {
    expressionConverterFactory.removeAllConverter();
  }

  public ExpressionConverterFactory getExpressionConverterFactory() {
    return expressionConverterFactory;
  }
}
