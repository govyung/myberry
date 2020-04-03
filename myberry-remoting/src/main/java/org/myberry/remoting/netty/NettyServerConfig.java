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
* NettyServerConfig is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.netty;

public class NettyServerConfig {

	private int listenPort = 8085;
	private int serverWorkerThreads = 8;
	private int serverCallbackExecutorThreads = 0;
	private int serverSelectorThreads = 3;
	private int serverOnewaySemaphoreValue = 256;
	private int serverAsyncSemaphoreValue = 64;
	private int serverChannelMaxIdleTimeSeconds = 120;

	private int serverSocketSndBufSize = NettySystemConfig.socketSndbufSize;
	private int serverSocketRcvBufSize = NettySystemConfig.socketRcvbufSize;
	private boolean serverPooledByteBufAllocatorEnable = true;

	/**
	 * make make install
	 *
	 *
	 * ../glibc-2.10.1/configure \ --prefix=/usr \ --with-headers=/usr/include \
	 * --host=x86_64-linux-gnu \ --build=x86_64-pc-linux-gnu \ --without-gd
	 */
	private boolean useEpollNativeSelector = false;

	public int getListenPort() {
		return listenPort;
	}

	public void setListenPort(int listenPort) {
		this.listenPort = listenPort;
	}

	public int getServerWorkerThreads() {
		return serverWorkerThreads;
	}

	public void setServerWorkerThreads(int serverWorkerThreads) {
		this.serverWorkerThreads = serverWorkerThreads;
	}

	public int getServerCallbackExecutorThreads() {
		return serverCallbackExecutorThreads;
	}

	public void setServerCallbackExecutorThreads(int serverCallbackExecutorThreads) {
		this.serverCallbackExecutorThreads = serverCallbackExecutorThreads;
	}

	public int getServerSelectorThreads() {
		return serverSelectorThreads;
	}

	public void setServerSelectorThreads(int serverSelectorThreads) {
		this.serverSelectorThreads = serverSelectorThreads;
	}

	public int getServerOnewaySemaphoreValue() {
		return serverOnewaySemaphoreValue;
	}

	public void setServerOnewaySemaphoreValue(int serverOnewaySemaphoreValue) {
		this.serverOnewaySemaphoreValue = serverOnewaySemaphoreValue;
	}

	public int getServerAsyncSemaphoreValue() {
		return serverAsyncSemaphoreValue;
	}

	public void setServerAsyncSemaphoreValue(int serverAsyncSemaphoreValue) {
		this.serverAsyncSemaphoreValue = serverAsyncSemaphoreValue;
	}
	
	public int getServerChannelMaxIdleTimeSeconds() {
		return serverChannelMaxIdleTimeSeconds;
	}

	public void setServerChannelMaxIdleTimeSeconds(int serverChannelMaxIdleTimeSeconds) {
		this.serverChannelMaxIdleTimeSeconds = serverChannelMaxIdleTimeSeconds;
	}

	public int getServerSocketSndBufSize() {
		return serverSocketSndBufSize;
	}

	public void setServerSocketSndBufSize(int serverSocketSndBufSize) {
		this.serverSocketSndBufSize = serverSocketSndBufSize;
	}

	public int getServerSocketRcvBufSize() {
		return serverSocketRcvBufSize;
	}

	public void setServerSocketRcvBufSize(int serverSocketRcvBufSize) {
		this.serverSocketRcvBufSize = serverSocketRcvBufSize;
	}

	public boolean isServerPooledByteBufAllocatorEnable() {
		return serverPooledByteBufAllocatorEnable;
	}

	public void setServerPooledByteBufAllocatorEnable(boolean serverPooledByteBufAllocatorEnable) {
		this.serverPooledByteBufAllocatorEnable = serverPooledByteBufAllocatorEnable;
	}

	public boolean isUseEpollNativeSelector() {
		return useEpollNativeSelector;
	}

	public void setUseEpollNativeSelector(boolean useEpollNativeSelector) {
		this.useEpollNativeSelector = useEpollNativeSelector;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return (NettyServerConfig) super.clone();
	}

}
