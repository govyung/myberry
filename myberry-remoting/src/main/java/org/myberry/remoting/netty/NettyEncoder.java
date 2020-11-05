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
* NettyEncoder is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.netty;

import java.nio.ByteBuffer;

import org.myberry.remoting.common.LoggerName;
import org.myberry.remoting.common.RemotingHelper;
import org.myberry.remoting.common.RemotingUtil;
import org.myberry.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class NettyEncoder extends MessageToByteEncoder<RemotingCommand> {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.REMOTING_LOGGER_NAME);

  @Override
  protected void encode(ChannelHandlerContext ctx, RemotingCommand remotingCommand, ByteBuf out)
      throws Exception {
    try {
      ByteBuffer header = remotingCommand.encodeHeader();
      out.writeBytes(header);
      byte[] body = remotingCommand.getBody();
      if (body != null) {
        out.writeBytes(body);
      }
    } catch (Exception e) {
      log.error("encode exception, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()), e);
      if (remotingCommand != null) {
        log.error(remotingCommand.toString());
      }
      RemotingUtil.closeChannel(ctx.channel());
    }
  }
}
