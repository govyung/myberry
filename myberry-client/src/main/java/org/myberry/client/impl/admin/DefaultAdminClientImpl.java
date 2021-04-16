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

import org.myberry.client.admin.DefaultAdminClient;
import org.myberry.client.admin.SendCallback;
import org.myberry.client.admin.SendResult;
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

  public SendResult createComponent(String password, String produceMode, byte[] componentData)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.createComponent(
        password, produceMode, componentData, defaultAdminClient.getSendMsgTimeout());
  }

  public SendResult createComponent(
      String password, String produceMode, byte[] componentData, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.createComponentImpl(
        password, produceMode, componentData, CommunicationMode.SYNC, null, timeout);
  }

  private SendResult createComponentImpl( //
      String password, //
      String produceMode, //
      byte[] componentData, //
      final CommunicationMode communicationMode, //
      final SendCallback sendCallback, //
      final long timeout //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    ManageComponentRequestHeader manageComponentRequestHeader =
        this.createAdminRequestHeader(password, produceMode);

    SendResult sendResult = null;
    switch (communicationMode) {
      case ASYNC:
        sendResult =
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
        sendResult =
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
    return sendResult;
  }

  public SendResult queryComponentSize(String password)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryComponentSize(password, defaultAdminClient.getSendMsgTimeout());
  }

  public SendResult queryComponentSize(String password, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryComponentSizeImpl(password, CommunicationMode.SYNC, null, timeout);
  }

  private SendResult queryComponentSizeImpl(
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
                .queryComponentSize(
                    RequestCode.QUERY_COMPONENT_SIZE,
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
                .queryComponentSize(
                    RequestCode.QUERY_COMPONENT_SIZE,
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

  public SendResult queryComponentByKey(byte[] key, String password)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryComponentByKey(key, password, defaultAdminClient.getSendMsgTimeout());
  }

  public SendResult queryComponentByKey(byte[] key, String password, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryComponentByKeyImpl(key, password, CommunicationMode.SYNC, null, timeout);
  }

  public SendResult queryComponentByKeyImpl(
      byte[] key, //
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
                .queryComponentByKey(
                    RequestCode.QUERY_COMPONENT_BY_KEY,
                    defaultAdminClient.getDefaultRouter().getMaintainerAddr(),
                    key,
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
                .queryComponentByKey(
                    RequestCode.QUERY_COMPONENT_BY_KEY,
                    defaultAdminClient.getDefaultRouter().getMaintainerAddr(),
                    key,
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

  public SendResult queryClusterList(String password)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryClusterList(password, defaultAdminClient.getSendMsgTimeout());
  }

  public SendResult queryClusterList(String password, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryClusterListImpl(password, CommunicationMode.SYNC, null, timeout);
  }

  private SendResult queryClusterListImpl(
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
                .queryClusterList(
                    RequestCode.QUERY_CLUSTER_LIST,
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
                .queryClusterList(
                    RequestCode.QUERY_CLUSTER_LIST,
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
      String password, String produceMode) {
    ManageComponentRequestHeader manageComponentRequestHeader = new ManageComponentRequestHeader();
    manageComponentRequestHeader.setPassword(password);
    manageComponentRequestHeader.setProduceMode(produceMode);
    return manageComponentRequestHeader;
  }
}
