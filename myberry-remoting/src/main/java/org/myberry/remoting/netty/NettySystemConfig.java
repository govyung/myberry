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
* NettySystemConfig is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.netty;

public class NettySystemConfig {

	public static final String MYBERRY_REMOTING_NETTY_POOLED_BYTE_BUF_ALLOCATOR_ENABLE =
	        "myberry.remoting.nettyPooledByteBufAllocatorEnable";
	public static final String MYBERRY_REMOTING_SOCKET_SNDBUF_SIZE = //
	        "myberry.remoting.socket.sndbuf.size";
	public static final String MYBERRY_REMOTING_SOCKET_RCVBUF_SIZE = //
	        "myberry.remoting.socket.rcvbuf.size";
	public static final String MYBERRY_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE = //
			"myberry.remoting.clientAsyncSemaphoreValue";
	public static final String MYBERRY_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE = //
			"myberry.remoting.clientOnewaySemaphoreValue";

	public static final int CLIENT_ASYNC_SEMAPHORE_VALUE = //
			Integer.parseInt(System.getProperty(MYBERRY_REMOTING_CLIENT_ASYNC_SEMAPHORE_VALUE, "65535"));
	public static final int CLIENT_ONEWAY_SEMAPHORE_VALUE = //
			Integer.parseInt(System.getProperty(MYBERRY_REMOTING_CLIENT_ONEWAY_SEMAPHORE_VALUE, "65535"));
	public static int socketSndbufSize = //
			Integer.parseInt(System.getProperty(MYBERRY_REMOTING_SOCKET_SNDBUF_SIZE, "65535"));
	public static int socketRcvbufSize = //
			Integer.parseInt(System.getProperty(MYBERRY_REMOTING_SOCKET_RCVBUF_SIZE, "65535"));
}
