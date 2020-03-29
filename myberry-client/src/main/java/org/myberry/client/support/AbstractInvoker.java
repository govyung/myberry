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
package org.myberry.client.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.myberry.client.exception.MyberryClientException;
import org.myberry.client.exception.MyberryServerException;
import org.myberry.client.impl.CommunicationMode;
import org.myberry.client.impl.user.DefaultUserClientImpl;
import org.myberry.client.user.PullCallback;
import org.myberry.client.user.PullResult;
import org.myberry.common.MixAll;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.remoting.CommandCustomHeader;
import org.myberry.remoting.exception.RemotingException;

public abstract class AbstractInvoker {

  protected final ExecutorService asyncSenderExecutor;

  public AbstractInvoker(final ExecutorService asyncSenderExecutor) {
    this.asyncSenderExecutor = asyncSenderExecutor;
  }

  protected void checkInvokers(List<Invoker> invokers) throws MyberryClientException {
    if (MixAll.isEmpty(invokers)) {
      throw new MyberryClientException("no available invoker");
    }
  }

  public abstract PullResult doInvoke(
      final DefaultUserClientImpl defaultUserClientImpl, //
      final CommandCustomHeader requstHeader, //
      final Map<String, String> attachments, //
      final long timeoutMillis, //
      final int timesRetry, //
      final CommunicationMode communicationMode //
      ) throws RemotingException, InterruptedException, MyberryServerException;

  public abstract void doInvoke(
      final DefaultUserClientImpl defaultUserClientImpl, //
      final CommandCustomHeader requstHeader, //
      final Map<String, String> attachments, //
      final long timeoutMillis, //
      final int timesRetry, //
      final CommunicationMode communicationMode,
      final PullCallback pullCallback //
      ) throws MyberryClientException;
}
