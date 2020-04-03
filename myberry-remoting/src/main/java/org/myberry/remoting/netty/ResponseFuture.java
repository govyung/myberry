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
* ResponseFuture is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.netty;

import io.netty.channel.Channel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.myberry.remoting.InvokeCallback;
import org.myberry.remoting.common.SemaphoreReleaseOnlyOnce;
import org.myberry.remoting.protocol.RemotingCommand;

public class ResponseFuture {
  private final int opaque;
  private final Channel processChannel;
  private final long timeoutMillis;
  private final InvokeCallback invokeCallback;
  private final long beginTimestamp = System.currentTimeMillis();
  private final CountDownLatch countDownLatch = new CountDownLatch(1);

  private final SemaphoreReleaseOnlyOnce once;

  private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);
  private volatile RemotingCommand responseCommand;
  private volatile boolean sendRequestOK = true;
  private volatile Throwable cause;

  public ResponseFuture(
      int opaque,
      Channel channel,
      long timeoutMillis,
      InvokeCallback invokeCallback,
      SemaphoreReleaseOnlyOnce once) {
    this.opaque = opaque;
    this.processChannel = channel;
    this.timeoutMillis = timeoutMillis;
    this.invokeCallback = invokeCallback;
    this.once = once;
  }

  public void executeInvokeCallback() {
    if (invokeCallback != null) {
      if (this.executeCallbackOnlyOnce.compareAndSet(false, true)) {
        invokeCallback.operationComplete(this);
      }
    }
  }

  public void release() {
    if (this.once != null) {
      this.once.release();
    }
  }

  public boolean isTimeout() {
    long diff = System.currentTimeMillis() - this.beginTimestamp;
    return diff > this.timeoutMillis;
  }

  public RemotingCommand waitResponse(final long timeoutMillis) throws InterruptedException {
    this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
    return this.responseCommand;
  }

  public void putResponse(final RemotingCommand responseCommand) {
    this.responseCommand = responseCommand;
    this.countDownLatch.countDown();
  }

  public long getBeginTimestamp() {
    return beginTimestamp;
  }

  public boolean isSendRequestOK() {
    return sendRequestOK;
  }

  public void setSendRequestOK(boolean sendRequestOK) {
    this.sendRequestOK = sendRequestOK;
  }

  public long getTimeoutMillis() {
    return timeoutMillis;
  }

  public InvokeCallback getInvokeCallback() {
    return invokeCallback;
  }

  public Throwable getCause() {
    return cause;
  }

  public void setCause(Throwable cause) {
    this.cause = cause;
  }

  public RemotingCommand getResponseCommand() {
    return responseCommand;
  }

  public void setResponseCommand(RemotingCommand responseCommand) {
    this.responseCommand = responseCommand;
  }

  public int getOpaque() {
    return opaque;
  }

  public Channel getProcessChannel() {
    return processChannel;
  }

  @Override
  public String toString() {
    return new StringBuilder() //
        .append("ResponseFuture [responseCommand=") //
        .append(responseCommand) //
        .append(", sendRequestOK=") //
        .append(sendRequestOK) //
        .append(", cause=") //
        .append(cause) //
        .append(", opaque=") //
        .append(opaque) //
        .append(", processChannel=") //
        .append(processChannel) //
        .append(", timeoutMillis=") //
        .append(timeoutMillis) //
        .append(", invokeCallback=") //
        .append(invokeCallback) //
        .append(", beginTimestamp=") //
        .append(beginTimestamp) //
        .append(", countDownLatch=") //
        .append(countDownLatch) //
        .append("]") //
        .toString();
  }
}
