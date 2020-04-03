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
* RemotingClient is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting;

import java.util.concurrent.ExecutorService;
import org.myberry.remoting.exception.RemotingConnectException;
import org.myberry.remoting.exception.RemotingSendRequestException;
import org.myberry.remoting.exception.RemotingTimeoutException;
import org.myberry.remoting.exception.RemotingTooMuchRequestException;
import org.myberry.remoting.protocol.RemotingCommand;

public interface RemotingClient extends RemotingService {

  RemotingCommand invokeSync(
      final String addr, final RemotingCommand request, final long timeoutMillis)
      throws InterruptedException, RemotingConnectException, RemotingSendRequestException,
          RemotingTimeoutException;

  void invokeAsync(
      final String addr,
      final RemotingCommand request,
      final long timeoutMillis,
      final InvokeCallback invokeCallback)
      throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
          RemotingTimeoutException, RemotingSendRequestException;

  void invokeOneway(final String addr, final RemotingCommand request, final long timeoutMillis)
      throws InterruptedException, RemotingConnectException, RemotingTooMuchRequestException,
          RemotingTimeoutException, RemotingSendRequestException;

  void setCallbackExecutor(final ExecutorService callbackExecutor);

  ExecutorService getCallbackExecutor();

  boolean isChannelWriteable(final String addr);
}
