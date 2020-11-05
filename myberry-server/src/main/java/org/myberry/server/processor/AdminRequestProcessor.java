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
package org.myberry.server.processor;

import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import org.myberry.common.ProduceMode;
import org.myberry.common.codec.LightCodec;
import org.myberry.common.constant.LoggerName;
import org.myberry.common.protocol.RequestCode;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.body.admin.AllCRComponentData;
import org.myberry.common.protocol.body.admin.AllNSComponentData;
import org.myberry.common.protocol.body.admin.CRComponentData;
import org.myberry.common.protocol.body.admin.NSComponentData;
import org.myberry.common.protocol.body.admin.PageData;
import org.myberry.common.protocol.header.admin.ManageComponentRequestHeader;
import org.myberry.common.protocol.header.admin.ManageComponentResponseHeader;
import org.myberry.remoting.netty.NettyRequestProcessor;
import org.myberry.remoting.protocol.RemotingCommand;
import org.myberry.server.ServerController;
import org.myberry.server.impl.AdminManageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminRequestProcessor implements NettyRequestProcessor {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.SERVER_LOGGER_NAME);
  private final ServerController serverController;

  public AdminRequestProcessor(final ServerController serverController) {
    this.serverController = serverController;
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
      throws Exception {
    ManageComponentRequestHeader requestHeader = //
        (ManageComponentRequestHeader)
            request.decodeCommandCustomHeader(ManageComponentRequestHeader.class);

    if (!this.serverController
        .getServerConfig()
        .getPassword()
        .equals(requestHeader.getPassword())) {
      RemotingCommand response = RemotingCommand.createResponseCommand(null);
      response.setCode(ResponseCode.PASSWORD_ERROR);
      response.setRemark(null);
      return response;
    }

    RemotingCommand response = null;
    switch (request.getCode()) {
      case RequestCode.CREATE_COMPONENT:
        if (ProduceMode.CR.getProduceName().equals(requestHeader.getProduceMode())) {

          CRComponentData crComponentData =
              LightCodec.toObj(request.getBody(), CRComponentData.class);
          response = createCRComponent(crComponentData);
        } else if (ProduceMode.NS.getProduceName().equals(requestHeader.getProduceMode())) {

          NSComponentData nsComponentData =
              LightCodec.toObj(request.getBody(), NSComponentData.class);
          response = createNSComponent(nsComponentData);
        } else {
          response = RemotingCommand.createResponseCommand(null);
          response.setCode(ResponseCode.DIFF_PRODUCE_MODE);
          response.setRemark("Client mode and server mode are different.");
        }
        break;
      case RequestCode.QUERY_ALL_COMPONENT:
        PageData pageData = LightCodec.toObj(request.getBody(), PageData.class);
        response = queryAllComponent(pageData);
        break;
      default:
        break;
    }

    return response;
  }

  @Override
  public boolean rejectRequest() {
    return false;
  }

  private RemotingCommand createCRComponent(CRComponentData crComponentData) {

    AdminManageResult result =
        serverController
            .getMyberryService()
            .addComponent(crComponentData.getKey(), crComponentData.getExpression());

    log.info(
        "[{}] [{} : {}] process success.",
        getProcessorName(),
        crComponentData.getKey(),
        crComponentData.getExpression());

    return createResponseCommand(result.getRespCode(), crComponentData.getKey());
  }

  private RemotingCommand createNSComponent(NSComponentData nsComponentData) {

    AdminManageResult result =
        serverController
            .getMyberryService()
            .addComponent(
                nsComponentData.getKey(),
                nsComponentData.getValue(),
                nsComponentData.getStepSize(),
                nsComponentData.getResetType());

    log.info(
        "[{}] [{} : {} : {} : {}] process success.",
        getProcessorName(),
        nsComponentData.getKey(),
        nsComponentData.getValue(),
        nsComponentData.getStepSize(),
        nsComponentData.getResetType());

    return createResponseCommand(result.getRespCode(), nsComponentData.getKey());
  }

  private RemotingCommand createResponseCommand(int code, String key) {
    ManageComponentResponseHeader responseHeader = new ManageComponentResponseHeader();
    responseHeader.setKey(key);

    RemotingCommand response = RemotingCommand.createResponseCommand(code, null);
    response.writeCustomHeader(responseHeader);
    return response;
  }

  private RemotingCommand queryAllComponent(PageData pageData) {

    ManageComponentResponseHeader responseHeader = new ManageComponentResponseHeader();
    responseHeader.setKey("");
    responseHeader.setProduceMode(serverController.getStoreConfig().getProduceMode());

    RemotingCommand response = RemotingCommand.createResponseCommand(null);

    Object queryAllComponent =
        serverController
            .getMyberryService()
            .queryComponentList(pageData.getPageNo(), pageData.getPageSize());
    if (ProduceMode.CR
        .getProduceName()
        .equals(serverController.getStoreConfig().getProduceMode())) {
      AllCRComponentData allCRComponentData = new AllCRComponentData();
      allCRComponentData.setComponents((List<CRComponentData>) queryAllComponent);
      response.setBody(LightCodec.toBytes(allCRComponentData));
    } else if (ProduceMode.NS
        .getProduceName()
        .equals(serverController.getStoreConfig().getProduceMode())) {
      AllNSComponentData allNSComponentData = new AllNSComponentData();
      allNSComponentData.setComponents((List<NSComponentData>) queryAllComponent);
      response.setBody(LightCodec.toBytes(allNSComponentData));
    }

    response.writeCustomHeader(responseHeader);
    response.setCode(ResponseCode.SUCCESS);
    response.setRemark(null);

    return response;
  }

  public String getProcessorName() {
    return AdminRequestProcessor.class.getSimpleName();
  }
}
