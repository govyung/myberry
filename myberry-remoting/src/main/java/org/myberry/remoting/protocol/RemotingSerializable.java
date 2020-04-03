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
* RemotingSerializable is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.protocol;

import java.nio.charset.Charset;

import com.alibaba.fastjson.JSON;

public abstract class RemotingSerializable {
	private final static Charset CHARSET_UTF8 = Charset.forName("UTF-8");

	public static byte[] encode(final Object obj) {
		final String json = toJson(obj, false);
		if (json != null) {
			return json.getBytes(CHARSET_UTF8);
		}
		return null;
	}

	public static String toJson(final Object obj, boolean prettyFormat) {
		return JSON.toJSONString(obj, prettyFormat);
	}

	public static <T> T decode(final byte[] data, Class<T> classOfT) {
		final String json = new String(data, CHARSET_UTF8);
		return fromJson(json, classOfT);
	}

	public static <T> T fromJson(String json, Class<T> classOfT) {
		return JSON.parseObject(json, classOfT);
	}

	public byte[] encode() {
		final String json = this.toJson();
		if (json != null) {
			return json.getBytes(CHARSET_UTF8);
		}
		return null;
	}

	public String toJson() {
		return toJson(false);
	}

	public String toJson(final boolean prettyFormat) {
		return toJson(this, prettyFormat);
	}
}
