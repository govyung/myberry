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

import java.util.List;
import org.myberry.common.codec.MessageLite;
import org.myberry.common.codec.asm.serializer.MessageLiteSerializer;
import org.myberry.common.codec.formatter.InOutStream;

public class ASMSerializerAdpter {

  private static final ASMSerializerAdpter asmSerializerAdpter = new ASMSerializerAdpter();
  private final ASMSerializerFactory asmSerializerFactory;

  private ASMSerializerAdpter() {
    this.asmSerializerFactory = new ASMSerializerFactory(this);
  }

  public static ASMSerializerAdpter getInstance() {
    return asmSerializerAdpter;
  }

  public void writeTo(MessageLite messageLite, InOutStream inOutStream) throws Exception {
    if (null != inOutStream) {
      inOutStream.markMessageLiteLength();
      if (null != messageLite) {
        MessageLiteSerializer messageLiteSerializer =
            asmSerializerFactory.getSerializer(messageLite.getClass());
        messageLiteSerializer.writeMessageLite(messageLite, inOutStream);
      }
      inOutStream.putMessageLiteLength();
    }
  }

  public void writeTo(List<? extends MessageLite> messageLites, InOutStream inOutStream)
      throws Exception {
    if (null != messageLites && messageLites.size() != 0) {
      for (MessageLite messageLite : messageLites) {
        writeTo(messageLite, inOutStream);
      }
    }
  }
}
