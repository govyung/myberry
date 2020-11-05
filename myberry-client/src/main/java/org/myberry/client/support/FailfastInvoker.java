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
package org.myberry.client.support;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.myberry.client.exception.MyberryClientException;
import org.myberry.client.exception.MyberryServerException;
import org.myberry.client.impl.CommunicationMode;
import org.myberry.client.impl.user.DefaultUserClientImpl;
import org.myberry.client.router.DefaultRouter;
import org.myberry.client.router.loadbalance.LoadBalance;
import org.myberry.client.user.PullCallback;
import org.myberry.client.user.PullResult;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.common.protocol.header.user.PullIdBackRequestHeader;
import org.myberry.remoting.CommandCustomHeader;
import org.myberry.remoting.exception.RemotingException;
import org.myberry.remoting.exception.RemotingTooMuchRequestException;

/**
 * Execute exactly once, which means this policy will throw an exception immediately in case of an
 * invocation error. Usually used for non-idempotent write operations
 *
 * <p><a href="http://en.wikipedia.org/wiki/Fail-fast">Fail-fast</a>
 */
public class FailfastInvoker extends AbstractInvoker {

  public static final String NAME = "failfast";

  public FailfastInvoker(final ExecutorService asyncSenderExecutor) {
    super(asyncSenderExecutor);
  }

  @Override
  public PullResult doInvoke(
      final DefaultUserClientImpl defaultUserClientImpl, //
      final CommandCustomHeader requstHeader, //
      final HashMap<String, String> attachments,
      final long timeoutMillis, //
      final int timesRetry, //
      final CommunicationMode communicationMode //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    DefaultRouter defaultRouter = defaultUserClientImpl.getDefaultUserClient().getDefaultRouter();
    LoadBalance loadBalance = defaultRouter.getLoadBalance();
    List<Invoker> invokers = defaultRouter.getInvokers();
    String addr =
        defaultRouter
            .getInvoker(loadBalance, invokers, ((PullIdBackRequestHeader) requstHeader).getKey())
            .getAddr();
    try {
      return defaultUserClientImpl
          .getMyberryClientFactory()
          .getMyberryClientAPIImpl()
          .pull(addr, requstHeader, attachments, timeoutMillis, communicationMode);
    } catch (RemotingException e) {
      throw new RemotingException(
          String.format(
              "failfast invoke server %s %s select from all invokers %s for request key %s on communicationMode %s, and last error is: %s",
              addr,
              (loadBalance == null ? null : loadBalance.getClass().getSimpleName()),
              invokers,
              ((PullIdBackRequestHeader) requstHeader).getKey(),
              communicationMode,
              e.getMessage()));
    } catch (InterruptedException | MyberryServerException e) {
      throw e;
    }
  }

  @Override
  public void doInvoke(
      final DefaultUserClientImpl defaultUserClientImpl, //
      final CommandCustomHeader requstHeader, //
      final HashMap<String, String> attachments,
      final long timeoutMillis, //
      final int timesRetry, //
      final CommunicationMode communicationMode, //
      final PullCallback pullCallback //
      ) throws MyberryClientException {
    final long beginStartTime = System.currentTimeMillis();

    DefaultRouter defaultRouter = defaultUserClientImpl.getDefaultUserClient().getDefaultRouter();
    final LoadBalance loadBalance = defaultRouter.getLoadBalance();
    final List<Invoker> invokers = defaultRouter.getInvokers();
    final String addr =
        defaultRouter
            .getInvoker(loadBalance, invokers, ((PullIdBackRequestHeader) requstHeader).getKey())
            .getAddr();
    try {
      this.asyncSenderExecutor.submit(
          new Runnable() {
            @Override
            public void run() {
              long costTime = System.currentTimeMillis() - beginStartTime;
              if (timeoutMillis > costTime) {
                try {
                  defaultUserClientImpl
                      .getMyberryClientFactory()
                      .getMyberryClientAPIImpl()
                      .pull(
                          addr,
                          requstHeader,
                          attachments,
                          timeoutMillis - costTime,
                          communicationMode,
                          pullCallback);
                } catch (Exception e) {
                  pullCallback.onException(
                      new RemotingException(
                          String.format(
                              "failfast invoke server %s %s select from all invokers %s for request key %s on communicationMode %s, and last error is: %s",
                              addr,
                              (loadBalance == null ? null : loadBalance.getClass().getSimpleName()),
                              invokers,
                              ((PullIdBackRequestHeader) requstHeader).getKey(),
                              communicationMode,
                              e.getMessage())));
                }
              } else {
                pullCallback.onException(
                    new RemotingTooMuchRequestException("DEFAULT ASYNC pull call timeout"));
              }
            }
          });
    } catch (RejectedExecutionException e) {
      throw new MyberryClientException("executor rejected ", e);
    }
  }
}
