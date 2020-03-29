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
package org.myberry.server.processor;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import java.util.Map;
import org.myberry.common.constant.LoggerName;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.header.user.PullIdBackRequestHeader;
import org.myberry.remoting.netty.NettyRequestProcessor;
import org.myberry.remoting.protocol.RemotingCommand;
import org.myberry.server.ServerController;
import org.myberry.store.PullIdResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserRequestProcessor implements NettyRequestProcessor {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.SERVER_LOGGER_NAME);
  private final ServerController serverController;

  public UserRequestProcessor(final ServerController serverController) {
    this.serverController = serverController;
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
      throws Exception {
    RemotingCommand response = RemotingCommand.createResponseCommand(PullIdBackRequestHeader.class);
    final PullIdBackRequestHeader responseHeader =
        (PullIdBackRequestHeader) response.readCustomHeader();

    PullIdBackRequestHeader requestHeader =
        (PullIdBackRequestHeader) request.decodeCommandCustomHeader(PullIdBackRequestHeader.class);
    PullIdResult result =
        serverController
            .getMyberryStore()
            .getNewId(requestHeader.getKey(), JSON.parseObject(request.getBody(), Map.class));

    if (ResponseCode.KEY_NOT_EXISTED == result.getRespCode()) {
      response.setCode(ResponseCode.KEY_NOT_EXISTED);
      response.setRemark(null);
      responseHeader.setKey(requestHeader.getKey());
    } else if (ResponseCode.SUCCESS == result.getRespCode()) {
      response.setCode(ResponseCode.SUCCESS);
      response.setRemark(null);
      responseHeader.setKey(requestHeader.getKey());
      responseHeader.setNewId(result.getNewId());
    } else {
      response.setCode(result.getRespCode());
      response.setRemark(result.getNewId());
      responseHeader.setKey(requestHeader.getKey());
    }

    return response;
  }

  @Override
  public boolean rejectRequest() {
    return this.serverController.getMyberryStore().isOSPageCacheBusy();
  }
}
