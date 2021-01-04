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
package org.myberry.client.impl;

import java.util.HashMap;
import org.myberry.client.admin.SendCallback;
import org.myberry.client.admin.SendResult;
import org.myberry.client.admin.SendStatus;
import org.myberry.client.exception.MyberryServerException;
import org.myberry.client.monitor.BreakdownInfo;
import org.myberry.client.router.RouterInfo;
import org.myberry.client.user.PullCallback;
import org.myberry.client.user.PullResult;
import org.myberry.client.user.PullStatus;
import org.myberry.common.ProduceMode;
import org.myberry.common.codec.LightCodec;
import org.myberry.common.codec.util.Maps;
import org.myberry.common.monitor.MonitorCode;
import org.myberry.common.protocol.RequestCode;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.body.HeartbeatData;
import org.myberry.common.protocol.body.admin.AllCRComponentData;
import org.myberry.common.protocol.body.admin.AllNSComponentData;
import org.myberry.common.protocol.body.user.CRPullResultData;
import org.myberry.common.protocol.body.user.NSPullResultData;
import org.myberry.common.protocol.header.admin.ManageComponentResponseHeader;
import org.myberry.common.protocol.header.user.PullIdBackRequestHeader;
import org.myberry.remoting.CommandCustomHeader;
import org.myberry.remoting.InvokeCallback;
import org.myberry.remoting.RemotingClient;
import org.myberry.remoting.exception.RemotingCommandException;
import org.myberry.remoting.exception.RemotingConnectException;
import org.myberry.remoting.exception.RemotingException;
import org.myberry.remoting.exception.RemotingSendRequestException;
import org.myberry.remoting.exception.RemotingTimeoutException;
import org.myberry.remoting.netty.NettyClientConfig;
import org.myberry.remoting.netty.NettyRemotingClient;
import org.myberry.remoting.netty.ResponseFuture;
import org.myberry.remoting.protocol.RemotingCommand;

public class MyberryClientAPIImpl {

  private final RemotingClient remotingClient;

  public MyberryClientAPIImpl(final NettyClientConfig nettyClientConfig) {
    this.remotingClient = new NettyRemotingClient(nettyClientConfig);
  }

  public void start() {
    this.remotingClient.start();
  }

  public void shutdown() {
    this.remotingClient.shutdown();
  }

  public RemotingClient getRemotingClient() {
    return remotingClient;
  }

  public PullResult pull( //
      final String addr, //
      final CommandCustomHeader requstHeader, //
      final HashMap<String, String> attachments, //
      final long timeoutMillis, //
      final CommunicationMode communicationMode //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    return pull(addr, requstHeader, attachments, timeoutMillis, communicationMode, null);
  }

  public PullResult pull( //
      final String addr, //
      final CommandCustomHeader requstHeader, //
      final HashMap<String, String> attachments, //
      final long timeoutMillis, //
      final CommunicationMode communicationMode, //
      final PullCallback pullCallback //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    RemotingCommand request =
        RemotingCommand.createRequestCommand(RequestCode.PULL_ID, requstHeader);
    request.setBody(Maps.serialize(attachments));
    switch (communicationMode) {
      case ONEWAY:
        this.remotingClient.invokeOneway(addr, request, timeoutMillis);
        return null;
      case ASYNC:
        this.pullAsync(addr, request, timeoutMillis, pullCallback);
        return null;
      case SYNC:
        return this.pullSync(addr, request, timeoutMillis);
      default:
        assert false;
        break;
    }
    return null;
  }

  private PullResult pullSync( //
      final String addr, //
      final RemotingCommand request, //
      final long timeoutMillis //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
    assert response != null;
    return this.processPullResponse(response);
  }

  private void pullAsync( //
      final String addr, //
      final RemotingCommand request, //
      final long timeoutMillis, //
      final PullCallback pullCallback //
      ) throws RemotingException, InterruptedException {
    this.remotingClient.invokeAsync(
        addr,
        request,
        timeoutMillis,
        new InvokeCallback() {

          @Override
          public void operationComplete(ResponseFuture responseFuture) {
            PullResult pullResult = null;
            try {
              pullResult =
                  MyberryClientAPIImpl.this.processPullResponse(
                      responseFuture.getResponseCommand());
            } catch (Throwable e) {
              pullCallback.onException(e);
            }
            pullCallback.onSuccess(pullResult);
          }
        });
  }

  private PullResult processPullResponse(final RemotingCommand response)
      throws MyberryServerException, RemotingCommandException {
    PullStatus pullStatus = PullStatus.KEY_NOT_EXISTED;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        pullStatus = PullStatus.PULL_OK;
        break;
      case ResponseCode.KEY_NOT_EXISTED:
        pullStatus = PullStatus.KEY_NOT_EXISTED;
        break;
      default:
        throw new MyberryServerException(response.getCode(), response.getRemark());
    }

    PullIdBackRequestHeader responseHeader =
        (PullIdBackRequestHeader) response.decodeCommandCustomHeader(PullIdBackRequestHeader.class);

    switch (pullStatus) {
      case PULL_OK:
        if (ProduceMode.CR.getProduceCode() == responseHeader.getProduceCode()) {
          CRPullResultData crPullResultData =
              LightCodec.toObj(response.getBody(), CRPullResultData.class);
          return new PullResult(pullStatus, responseHeader.getKey(), crPullResultData.getNewId());
        } else if (ProduceMode.NS.getProduceCode() == responseHeader.getProduceCode()) {
          NSPullResultData nsPullResultData =
              LightCodec.toObj(response.getBody(), NSPullResultData.class);
          return new PullResult(
              pullStatus,
              responseHeader.getKey(),
              nsPullResultData.getStart(),
              nsPullResultData.getEnd(),
              nsPullResultData.getSynergyId());
        }
      default:
        return new PullResult(pullStatus, responseHeader.getKey());
    }
  }

  public SendResult createComponent( //
      final int code, //
      final String addr, //
      byte[] componentData, //
      final CommandCustomHeader requstHeader, //
      final long timeoutMillis, //
      final CommunicationMode communicationMode //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    return this.createComponent(
        code, addr, componentData, requstHeader, timeoutMillis, communicationMode, null);
  }

  public SendResult createComponent( //
      final int code, //
      final String addr, //
      byte[] componentData, //
      final CommandCustomHeader requstHeader, //
      final long timeoutMillis, //
      final CommunicationMode communicationMode, //
      final SendCallback sendCallback //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    RemotingCommand request = RemotingCommand.createRequestCommand(code, requstHeader);
    request.setBody(componentData);
    return this.sendKernelImpl(code, addr, request, timeoutMillis, communicationMode, sendCallback);
  }

  public SendResult queryAllComponent( //
      final int code, //
      final String addr, //
      byte[] pageData, //
      final CommandCustomHeader requstHeader, //
      final long timeoutMillis, //
      final CommunicationMode communicationMode //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    return this.queryAllComponent(
        code, addr, pageData, requstHeader, timeoutMillis, communicationMode, null);
  }

  public SendResult queryAllComponent( //
      final int code, //
      final String addr, //
      byte[] pageData, //
      final CommandCustomHeader requstHeader, //
      final long timeoutMillis, //
      final CommunicationMode communicationMode, //
      final SendCallback sendCallback //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    RemotingCommand request = RemotingCommand.createRequestCommand(code, requstHeader);
    request.setBody(pageData);
    return this.sendKernelImpl(code, addr, request, timeoutMillis, communicationMode, sendCallback);
  }

  private SendResult sendKernelImpl( //
      final int code, //
      final String addr, //
      final RemotingCommand request, //
      final long timeoutMillis, //
      final CommunicationMode communicationMode, //
      final SendCallback sendCallback //
      ) throws RemotingException, InterruptedException, MyberryServerException {
    switch (communicationMode) {
      case ONEWAY:
        this.remotingClient.invokeOneway(addr, request, timeoutMillis);
        return null;
      case ASYNC:
        this.sendKernelAsync(code, addr, request, timeoutMillis, sendCallback);
        return null;
      case SYNC:
        return this.sendKernelSync(code, addr, request, timeoutMillis);
      default:
        assert false;
        break;
    }
    return null;
  }

  private SendResult sendKernelSync( //
      final int code, //
      final String addr, //
      final RemotingCommand request, //
      final long timeoutMillis //
      ) throws RemotingException, MyberryServerException, InterruptedException {
    RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
    assert response != null;
    return this.processSendResponse(code, response);
  }

  private void sendKernelAsync( //
      final int code, //
      final String addr, //
      final RemotingCommand request, //
      final long timeoutMillis, //
      final SendCallback sendCallback //
      ) throws RemotingException, InterruptedException {
    this.remotingClient.invokeAsync(
        addr,
        request,
        timeoutMillis,
        new InvokeCallback() {

          @Override
          public void operationComplete(ResponseFuture responseFuture) {
            SendResult pullResult = null;
            try {
              pullResult =
                  MyberryClientAPIImpl.this.processSendResponse(
                      code, responseFuture.getResponseCommand());
            } catch (Throwable e) {
              sendCallback.onException(e);
            }
            sendCallback.onSuccess(pullResult);
          }
        });
  }

  private SendResult processSendResponse(final int code, final RemotingCommand response)
      throws RemotingCommandException, MyberryServerException {
    switch (response.getCode()) {
      case ResponseCode.KEY_EXISTED:
      case ResponseCode.DISK_FULL:
      case ResponseCode.PASSWORD_ERROR:
      case ResponseCode.DIFF_PRODUCE_MODE:
        {
        }
      case ResponseCode.SUCCESS:
        {
          SendStatus sendStatus = SendStatus.SEND_OK;
          switch (response.getCode()) {
            case ResponseCode.KEY_EXISTED:
              sendStatus = SendStatus.KEY_EXISTED;
              break;
            case ResponseCode.DISK_FULL:
              sendStatus = SendStatus.DISK_FULL;
              break;
            case ResponseCode.PASSWORD_ERROR:
              sendStatus = SendStatus.PASSWORD_ERROR;
              break;
            case ResponseCode.DIFF_PRODUCE_MODE:
              sendStatus = SendStatus.DIFF_PRODUCE_MODE;
              break;
            case ResponseCode.SUCCESS:
              sendStatus = SendStatus.SEND_OK;
              break;
            default:
              assert false;
              break;
          }

          byte[] body = response.getBody();
          if (body == null) {
            body = new byte[0];
          }
          switch (code) {
            case RequestCode.CREATE_COMPONENT:
              ManageComponentResponseHeader createResponseHeader =
                  (ManageComponentResponseHeader)
                      response.decodeCommandCustomHeader(ManageComponentResponseHeader.class);
              return new SendResult(sendStatus, createResponseHeader.getKey());
            case RequestCode.QUERY_ALL_COMPONENT:
              ManageComponentResponseHeader queryResponseHeader =
                  (ManageComponentResponseHeader)
                      response.decodeCommandCustomHeader(ManageComponentResponseHeader.class);
              SendResult sendResult = new SendResult(sendStatus);
              if (ProduceMode.CR.getProduceName().equals(queryResponseHeader.getProduceMode())) {
                sendResult.setComponentsOfCR(
                    LightCodec.toObj(body, AllCRComponentData.class).getComponents());
                return sendResult;
              } else if (ProduceMode.NS
                  .getProduceName()
                  .equals(queryResponseHeader.getProduceMode())) {
                sendResult.setComponentsOfNS(
                    LightCodec.toObj(body, AllNSComponentData.class).getComponents());
                return sendResult;
              } else {
                return new SendResult(sendStatus);
              }

            default:
              assert false;
              break;
          }
        }
      default:
        break;
    }

    throw new MyberryServerException(response.getCode(), response.getRemark());
  }

  public HeartbeatResult sendHearbeat( //
      final String addr, //
      final CommandCustomHeader requstHeader, //
      final long timeoutMillis //
      )
      throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
          InterruptedException, MyberryServerException {
    RemotingCommand request =
        RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, requstHeader);

    RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
    assert response != null;
    switch (response.getCode()) {
      case ResponseCode.SUCCESS:
        {
          HeartbeatData heartbeatData = LightCodec.toObj(response.getBody(), HeartbeatData.class);
          RouterInfo routerInfo = new RouterInfo();
          routerInfo.setLoadBalanceName(heartbeatData.getLoadBalanceName());
          routerInfo.setMaintainer(heartbeatData.getMaintainer());
          routerInfo.setInvokers(heartbeatData.getInvokers());

          BreakdownInfo breakdownInfo = null;
          if (heartbeatData.getMonitorCode() > MonitorCode.NORMAL) {
            breakdownInfo = new BreakdownInfo();
            breakdownInfo.setBreakdownCode(heartbeatData.getMonitorCode());
            breakdownInfo.setClusterName(heartbeatData.getClusterName());
            breakdownInfo.setInfo(heartbeatData.getInfo());
          }

          return new HeartbeatResult(routerInfo, breakdownInfo);
        }
      default:
        break;
    }
    throw new MyberryServerException(response.getCode(), response.getRemark());
  }
}
