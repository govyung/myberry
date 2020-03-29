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
package org.myberry.client.user;

import java.util.Map;
import org.myberry.client.exception.MyberryClientException;
import org.myberry.client.exception.MyberryServerException;
import org.myberry.remoting.exception.RemotingException;

public interface UserClient {

  PullResult pull(String key)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  PullResult pull(String key, Map<String, String> attachments)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  PullResult pull(String key, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  PullResult pull(String key, Map<String, String> attachments, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  PullResult pull(String key, long timeout, int timesRetry)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  PullResult pull(String key, Map<String, String> attachments, long timeout, int timesRetry)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  void pull(String key, PullCallback pullCallback)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  void pull(String key, Map<String, String> attachments, PullCallback pullCallback)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  void pull(String key, PullCallback pullCallback, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  void pull(String key, Map<String, String> attachments, PullCallback pullCallback, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  void pull(String key, PullCallback pullCallback, long timeout, int timesRetry)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;

  void pull(
      String key,
      Map<String, String> attachments,
      PullCallback pullCallback,
      long timeout,
      int timesRetry)
      throws RemotingException, InterruptedException, MyberryServerException,
          MyberryClientException;
}
