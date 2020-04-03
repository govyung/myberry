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
* RemotingServer is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting;

import io.netty.channel.Channel;
import java.util.concurrent.ExecutorService;
import org.myberry.remoting.common.Pair;
import org.myberry.remoting.exception.RemotingSendRequestException;
import org.myberry.remoting.exception.RemotingTimeoutException;
import org.myberry.remoting.exception.RemotingTooMuchRequestException;
import org.myberry.remoting.netty.NettyRequestProcessor;
import org.myberry.remoting.protocol.RemotingCommand;

public interface RemotingServer extends RemotingService {

  void registerProcessor(
      final int requestCode, final NettyRequestProcessor processor, final ExecutorService executor);

  void registerDefaultProcessor(
      final NettyRequestProcessor processor, final ExecutorService executor);

  int localListenPort();

  Pair<NettyRequestProcessor, ExecutorService> getProcessorPair(final int requestCode);

  RemotingCommand invokeSync(
      final Channel channel, final RemotingCommand request, final long timeoutMillis)
      throws InterruptedException, RemotingSendRequestException, RemotingTimeoutException;

  void invokeAsync(
      final Channel channel,
      final RemotingCommand request,
      final long timeoutMillis,
      final InvokeCallback invokeCallback)
      throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException,
          RemotingSendRequestException;

  void invokeOneway(final Channel channel, final RemotingCommand request, final long timeoutMillis)
      throws InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException,
          RemotingSendRequestException;
}
