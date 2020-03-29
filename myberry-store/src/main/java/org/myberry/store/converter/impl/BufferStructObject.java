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
package org.myberry.store.converter.impl;

import java.util.Map;
import org.myberry.store.converter.handler.DynamicHandler;
import org.myberry.store.converter.handler.IncrHandler;
import org.myberry.store.converter.handler.SidHandler;
import org.myberry.store.impl.CRComponent;

public class BufferStructObject {

  private final PlaceholderObject[] placeholderObjects;
  private int sid;

  public BufferStructObject(int length) {
    this.placeholderObjects = new PlaceholderObject[length];
  }

  public PlaceholderObject[] getPlaceholderObjects() {
    return placeholderObjects;
  }

  public void setSid(int sid) {
    this.sid = sid;
  }

  public String getResult(CRComponent crComponent, Map<String, String> attachments) {
    StringBuilder result = new StringBuilder(placeholderObjects.length);
    for (PlaceholderObject placeholderObject : placeholderObjects) {
      if (placeholderObject.getPlaceholderHandler() instanceof IncrHandler) {
        result.append(
            placeholderObject
                .getPlaceholderHandler()
                .get(String.valueOf(crComponent.getIncrNumber())));
      } else if (placeholderObject.getPlaceholderHandler() instanceof SidHandler) {
        result.append(placeholderObject.getPlaceholderHandler().get(String.valueOf(sid)));
      } else if (placeholderObject.getPlaceholderHandler() instanceof DynamicHandler) {
        if (attachments != null
            && attachments.get(placeholderObject.getPlaceholderHandler().get(null)) != null) {
          result.append(attachments.get(placeholderObject.getPlaceholderHandler().get(null)));
        }
      } else {
        result.append(placeholderObject.getPlaceholderHandler().get(null));
      }
    }
    return result.toString();
  }
}
