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
*
* RemotingCommandTest is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.protocol;

import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.myberry.remoting.SampleCommandCustomHeader;

public class RemotingSerializableTest {

  @Test
  public void testEncode2Decode() throws Exception {
    // case
    int code = 111;

    int id = 15;
    String name = "John";
    boolean flag = true;
    SampleCommandCustomHeader header = new SampleCommandCustomHeader();
    header.setId(id);
    header.setName(name);
    header.setFlag(flag);

    RemotingCommand requestCommand = RemotingCommand.createRequestCommand(code, header);
    ByteBuffer byteBuffer = requestCommand.encode();

    // offset 4 due to LengthFieldBasedFrameDecoder
    byte[] bytes = new byte[byteBuffer.capacity() - 4];
    for (int i = 0; i < byteBuffer.capacity() - 4; i++) {
      bytes[i] = byteBuffer.get(4 + i);
    }

    RemotingCommand command = RemotingCommand.decode(ByteBuffer.wrap(bytes));
    Assert.assertEquals(code, command.getCode());

    SampleCommandCustomHeader commandHeader =
        (SampleCommandCustomHeader)
            command.decodeCommandCustomHeader(SampleCommandCustomHeader.class);
    Assert.assertEquals(id, commandHeader.getId());
    Assert.assertEquals(name, commandHeader.getName());
    Assert.assertEquals(flag, commandHeader.isFlag());
  }
}
