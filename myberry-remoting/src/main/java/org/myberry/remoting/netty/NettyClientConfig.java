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
* NettyClientConfig is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.netty;


public class NettyClientConfig {

	/**
	 * Worker thread number
	 */
	private int clientWorkerThreads = 4;
	private int connectTimeoutMillis = 3000;
	private int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();
	private int clientOnewaySemaphoreValue = NettySystemConfig.CLIENT_ONEWAY_SEMAPHORE_VALUE;
    private int clientAsyncSemaphoreValue = NettySystemConfig.CLIENT_ASYNC_SEMAPHORE_VALUE;
    
    private int clientChannelMaxIdleTimeSeconds = 120;
    
	private int clientSocketSndBufSize = NettySystemConfig.socketSndbufSize;
	private int clientSocketRcvBufSize = NettySystemConfig.socketRcvbufSize;
	
	private boolean clientCloseSocketIfTimeout = false;
	
	public boolean isClientCloseSocketIfTimeout() {
        return clientCloseSocketIfTimeout;
    }

    public void setClientCloseSocketIfTimeout(final boolean clientCloseSocketIfTimeout) {
        this.clientCloseSocketIfTimeout = clientCloseSocketIfTimeout;
    }

	public int getClientWorkerThreads() {
		return clientWorkerThreads;
	}

	public void setClientWorkerThreads(int clientWorkerThreads) {
		this.clientWorkerThreads = clientWorkerThreads;
	}

	public int getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(int connectTimeoutMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
	}

	public int getClientCallbackExecutorThreads() {
		return clientCallbackExecutorThreads;
	}

	public void setClientCallbackExecutorThreads(int clientCallbackExecutorThreads) {
		this.clientCallbackExecutorThreads = clientCallbackExecutorThreads;
	}
	
	public int getClientOnewaySemaphoreValue() {
		return clientOnewaySemaphoreValue;
	}

	public void setClientOnewaySemaphoreValue(int clientOnewaySemaphoreValue) {
		this.clientOnewaySemaphoreValue = clientOnewaySemaphoreValue;
	}

	public int getClientAsyncSemaphoreValue() {
		return clientAsyncSemaphoreValue;
	}

	public void setClientAsyncSemaphoreValue(int clientAsyncSemaphoreValue) {
		this.clientAsyncSemaphoreValue = clientAsyncSemaphoreValue;
	}
	
	public int getClientChannelMaxIdleTimeSeconds() {
		return clientChannelMaxIdleTimeSeconds;
	}

	public void setClientChannelMaxIdleTimeSeconds(int clientChannelMaxIdleTimeSeconds) {
		this.clientChannelMaxIdleTimeSeconds = clientChannelMaxIdleTimeSeconds;
	}

	public int getClientSocketSndBufSize() {
		return clientSocketSndBufSize;
	}

	public void setClientSocketSndBufSize(int clientSocketSndBufSize) {
		this.clientSocketSndBufSize = clientSocketSndBufSize;
	}

	public int getClientSocketRcvBufSize() {
		return clientSocketRcvBufSize;
	}

	public void setClientSocketRcvBufSize(int clientSocketRcvBufSize) {
		this.clientSocketRcvBufSize = clientSocketRcvBufSize;
	}

}
