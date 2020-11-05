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
* NettyDecoder is written by Apache RocketMQ.
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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class NettyDecoder extends LengthFieldBasedFrameDecoder {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.REMOTING_LOGGER_NAME);
  private static final int FRAME_MAX_LENGTH = //
      Integer.parseInt(System.getProperty("myberry.remoting.frameMaxLength", "16777216"));

  public NettyDecoder() {
    super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
  }

  @Override
  public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
    ByteBuf frame = null;
    try {
      frame = (ByteBuf) super.decode(ctx, in);
      if (null == frame) {
        return null;
      }

      ByteBuffer byteBuffer = frame.nioBuffer();

      return RemotingCommand.decode(byteBuffer);
    } catch (Exception e) {
      log.error("decode exception, " + RemotingHelper.parseChannelRemoteAddr(ctx.channel()), e);
      RemotingUtil.closeChannel(ctx.channel());
    } finally {
      if (null != frame) {
        frame.release();
      }
    }

    return null;
  }
}
