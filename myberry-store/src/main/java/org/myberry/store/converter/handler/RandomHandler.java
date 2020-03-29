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
package org.myberry.store.converter.handler;

import java.util.concurrent.ThreadLocalRandom;
import org.myberry.store.PlaceholderHandler;

public class RandomHandler implements PlaceholderHandler {

  private int randMax;
  private int length;

  @Override
  public void handle(String placeholder) {
    this.length = Integer.parseInt(placeholder);
    this.randMax = Double.valueOf(Math.pow(10, length)).intValue();
  }

  @Override
  public String get(String value) {
    int nextInt = ThreadLocalRandom.current().nextInt(randMax);
    String rand = String.valueOf(nextInt);
    if (length == rand.length()) {
      return rand;
    } else {
      int diff = length - rand.length();

      StringBuilder stringBuilder = new StringBuilder(diff);
      for (int i = 0; i < diff; i++) {
        stringBuilder.append("0");
      }
      stringBuilder.append(rand);

      return stringBuilder.toString();
    }
  }
}
