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
* RequestTask is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.netty;

import org.myberry.remoting.protocol.RemotingCommand;

import io.netty.channel.Channel;

public class RequestTask implements Runnable {
  private final Runnable runnable;
  private final long createTimestamp = System.currentTimeMillis();
  private final Channel channel;
  private final RemotingCommand request;
  private boolean stopRun = false;

  public RequestTask(
      final Runnable runnable, final Channel channel, final RemotingCommand request) {
    this.runnable = runnable;
    this.channel = channel;
    this.request = request;
  }

  @Override
  public int hashCode() {
    int result = runnable != null ? runnable.hashCode() : 0;
    result = 31 * result + (int) (getCreateTimestamp() ^ (getCreateTimestamp() >>> 32));
    result = 31 * result + (channel != null ? channel.hashCode() : 0);
    result = 31 * result + (request != null ? request.hashCode() : 0);
    result = 31 * result + (isStopRun() ? 1 : 0);
    return result;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof RequestTask)) return false;

    final RequestTask that = (RequestTask) o;

    if (getCreateTimestamp() != that.getCreateTimestamp()) return false;
    if (isStopRun() != that.isStopRun()) return false;
    if (channel != null ? !channel.equals(that.channel) : that.channel != null) return false;
    return request != null ? request.getOpaque() == that.request.getOpaque() : that.request == null;
  }

  public long getCreateTimestamp() {
    return createTimestamp;
  }

  public boolean isStopRun() {
    return stopRun;
  }

  public void setStopRun(final boolean stopRun) {
    this.stopRun = stopRun;
  }

  @Override
  public void run() {
    if (!this.stopRun) this.runnable.run();
  }

  public void returnResponse(int code, String remark) {
    final RemotingCommand response = RemotingCommand.createResponseCommand(code, remark);
    response.setOpaque(request.getOpaque());
    this.channel.writeAndFlush(response);
  }
}
