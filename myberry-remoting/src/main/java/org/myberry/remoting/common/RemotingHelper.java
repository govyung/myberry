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
* RemotingHelper is written by Apache RocketMQ.
* The author has modified it and hereby declares the copyright of this source code.
*/
package org.myberry.remoting.common;

import io.netty.channel.Channel;

import java.net.*;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotingHelper {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.REMOTING_LOGGER_NAME);

  public static String exceptionSimpleDesc(final Throwable e) {
    StringBuffer sb = new StringBuffer();
    if (e != null) {
      sb.append(e.toString());

      StackTraceElement[] stackTrace = e.getStackTrace();
      if (stackTrace != null && stackTrace.length > 0) {
        StackTraceElement elment = stackTrace[0];
        sb.append(", ");
        sb.append(elment.toString());
      }
    }

    return sb.toString();
  }

  public static String getPhyLocalAddress() {
    try {
      Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
      InetAddress ip = null;
      while (allNetInterfaces.hasMoreElements()) {
        NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
        if (netInterface.isLoopback() || netInterface.isVirtual() || !netInterface.isUp()) {
          continue;
        } else {
          Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
          while (addresses.hasMoreElements()) {
            ip = addresses.nextElement();
            if (ip != null && ip instanceof Inet4Address) {
              return ip.getHostAddress();
            }

            /*
             * if (ip != null && ip instanceof Inet6Address) { return ip.getHostAddress(); }
             */
          }
        }
      }
    } catch (Exception e) {
      log.error("get physical address exception: ", e);
    }
    return "";
  }

  public static SocketAddress string2SocketAddress(final String addr) {
    String[] s = addr.split(":");
    InetSocketAddress isa = new InetSocketAddress(s[0], Integer.parseInt(s[1]));
    return isa;
  }

  public static String makeStringAddress(final String ip, final int port) {
    StringBuilder addr = new StringBuilder();
    addr.append(ip);
    addr.append(":");
    addr.append(port);
    return addr.toString();
  }

  public static String getAddressIP(final String addr) {
    if (addr == null || "".equals(addr)) {
      return "";
    }
    return addr.split(":")[0];
  }

  public static int getAddressPort(final String addr) {
    if (addr == null || "".equals(addr)) {
      return -1;
    }
    return Integer.parseInt(addr.split(":")[1]);
  }

  public static String parseChannelRemoteAddr(final Channel channel) {
    if (null == channel) {
      return "";
    }
    SocketAddress remote = channel.remoteAddress();
    final String addr = remote != null ? remote.toString() : "";

    if (addr.length() > 0) {
      int index = addr.lastIndexOf("/");
      if (index >= 0) {
        return addr.substring(index + 1);
      }

      return addr;
    }

    return "";
  }

  public static String parseSocketAddressAddr(SocketAddress socketAddress) {
    if (socketAddress != null) {
      final String addr = socketAddress.toString();

      if (addr.length() > 0) {
        return addr.substring(1);
      }
    }
    return "";
  }
}
