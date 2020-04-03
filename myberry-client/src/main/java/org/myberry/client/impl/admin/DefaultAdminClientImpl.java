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
package org.myberry.client.impl.admin;

import org.myberry.client.admin.SendCallback;
import org.myberry.client.admin.SendResult;
import org.myberry.client.admin.DefaultAdminClient;
import org.myberry.client.exception.MyberryServerException;
import org.myberry.client.impl.AbstractClientImpl;
import org.myberry.client.impl.CommunicationMode;
import org.myberry.common.protocol.RequestCode;
import org.myberry.common.protocol.header.admin.ManageComponentRequestHeader;
import org.myberry.remoting.exception.RemotingException;

public class DefaultAdminClientImpl extends AbstractClientImpl {

  private final DefaultAdminClient defaultAdminClient;

  public DefaultAdminClientImpl(DefaultAdminClient defaultAdminClient) {
    super(defaultAdminClient);
    this.defaultAdminClient = defaultAdminClient;
  }

  public SendResult createComponent(String password, String runningMode, byte[] componentData)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.createComponent(
        password, runningMode, componentData, defaultAdminClient.getSendMsgTimeout());
  }

  public SendResult createComponent(
      String password, String runningMode, byte[] componentData, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.createComponentImpl(
        password, runningMode, componentData, CommunicationMode.SYNC, null, timeout);
  }

  public void createComponent(
      String password, String runningMode, byte[] componentData, SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.createComponent(
        password, runningMode, componentData, defaultAdminClient.getSendMsgTimeout(), sendCallback);
  }

  public void createComponent(
      String password,
      String runningMode,
      byte[] componentData,
      long timeout,
      SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.createComponentImpl(
        password, runningMode, componentData, CommunicationMode.ASYNC, sendCallback, timeout);
  }

  private SendResult createComponentImpl( //
      String password, //
      String runningMode, //
      byte[] componentData, //
      final CommunicationMode communicationMode, //
      final SendCallback sendCallback, //
      final long timeout //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    ManageComponentRequestHeader manageComponentRequestHeader =
        this.createAdminRequestHeader(password, runningMode);

    SendResult pullResult = null;
    switch (communicationMode) {
      case ASYNC:
        pullResult =
            this.getMyberryClientFactory()
                .getMyberryClientAPIImpl()
                .createComponent(
                    RequestCode.CREATE_COMPONENT,
                    defaultAdminClient.getDefaultRouter().getMaintainerAddr(),
                    componentData,
                    manageComponentRequestHeader,
                    timeout,
                    communicationMode,
                    sendCallback);
        break;
      case ONEWAY:
      case SYNC:
        pullResult =
            this.getMyberryClientFactory()
                .getMyberryClientAPIImpl()
                .createComponent(
                    RequestCode.CREATE_COMPONENT,
                    defaultAdminClient.getDefaultRouter().getMaintainerAddr(),
                    componentData,
                    manageComponentRequestHeader,
                    timeout,
                    communicationMode);
        break;
      default:
        assert false;
        break;
    }
    return pullResult;
  }

  public SendResult queryAllComponent(String password)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryAllComponent(password, defaultAdminClient.getSendMsgTimeout());
  }

  public SendResult queryAllComponent(String password, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryAllComponentImpl(password, CommunicationMode.SYNC, null, timeout);
  }

  public void queryAllComponent(String password, SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.queryAllComponent(password, defaultAdminClient.getSendMsgTimeout(), sendCallback);
  }

  public void queryAllComponent(String password, long timeout, SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.queryAllComponentImpl(password, CommunicationMode.ASYNC, sendCallback, timeout);
  }

  private SendResult queryAllComponentImpl( //
      String password, //
      final CommunicationMode communicationMode, //
      final SendCallback sendCallback, //
      final long timeout //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    ManageComponentRequestHeader manageComponentRequestHeader =
        this.createAdminRequestHeader(password, "");

    SendResult sendResult = null;
    switch (communicationMode) {
      case ASYNC:
        sendResult =
            this.getMyberryClientFactory()
                .getMyberryClientAPIImpl()
                .queryAllComponent(
                    RequestCode.QUERY_ALL_COMPONENT,
                    defaultAdminClient.getDefaultRouter().getMaintainerAddr(),
                    manageComponentRequestHeader,
                    timeout,
                    communicationMode,
                    sendCallback);
        break;
      case ONEWAY:
      case SYNC:
        sendResult =
            this.getMyberryClientFactory()
                .getMyberryClientAPIImpl()
                .queryAllComponent(
                    RequestCode.QUERY_ALL_COMPONENT,
                    defaultAdminClient.getDefaultRouter().getMaintainerAddr(),
                    manageComponentRequestHeader,
                    timeout,
                    communicationMode);
        break;
      default:
        assert false;
        break;
    }
    return sendResult;
  }

  private ManageComponentRequestHeader createAdminRequestHeader(
      String password, String runningMode) {
    ManageComponentRequestHeader manageComponentRequestHeader = new ManageComponentRequestHeader();
    manageComponentRequestHeader.setPassword(password);
    manageComponentRequestHeader.setRunningMode(runningMode);
    return manageComponentRequestHeader;
  }
}
