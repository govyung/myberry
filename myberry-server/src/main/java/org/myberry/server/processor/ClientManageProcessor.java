/*
 * Copyright 2018 gaoyang. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.myberry.server.processor;

import io.netty.channel.ChannelHandlerContext;
import org.myberry.common.constant.LoggerName;
import org.myberry.common.monitor.MonitorCode;
import org.myberry.common.protocol.RequestCode;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.body.HeartbeatData;
import org.myberry.common.protocol.header.HeartbeatRequestHeader;
import org.myberry.remoting.netty.NettyRequestProcessor;
import org.myberry.remoting.protocol.RemotingCommand;
import org.myberry.server.ServerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientManageProcessor implements NettyRequestProcessor {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.SERVER_LOGGER_NAME);

  private final ServerController serverController;

  public ClientManageProcessor(final ServerController serverController) {
    this.serverController = serverController;
  }

  @Override
  public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request)
      throws Exception {
    HeartbeatRequestHeader requestHeader =
        (HeartbeatRequestHeader) request.decodeCommandCustomHeader(HeartbeatRequestHeader.class);
    log.debug("receive [{}] heartbeat.", requestHeader.getClientId());

    boolean notOnlyRouterInfo = false;
    if (this.serverController.getServerConfig().getPassword().equals(requestHeader.getPassword())) {
      notOnlyRouterInfo = true;
    }

    RemotingCommand response = RemotingCommand.createResponseCommand(null);
    switch (request.getCode()) {
      case RequestCode.HEART_BEAT:
        return this.heartBeat(response, notOnlyRouterInfo);
      default:
        break;
    }
    return response;
  }

  public RemotingCommand heartBeat(RemotingCommand response, boolean notOnlyRouterInfo) {
    HeartbeatData heartbeatData = new HeartbeatData();

    if (serverController.getServerConfig().getClusterName() != null) {
      heartbeatData.setLoadBalanceName(serverController.getServerConfig().getLoadbalance());
      heartbeatData.setMaintainer(serverController.getRouteInfoManager().getLeaderInfo());
      heartbeatData.setInvokers(serverController.getRouteInfoManager().getLearnerInfo());
      if (notOnlyRouterInfo) {
        String lostLearner = serverController.getRouteInfoManager().pollLostLearnerQueue();
        if (lostLearner != null) {
          heartbeatData.setMonitorCode(MonitorCode.LOSING_CONTACT);
          heartbeatData.setClusterName(serverController.getServerConfig().getClusterName());
          heartbeatData.setInfo(
              new StringBuilder().append("lose server ").append(lostLearner).toString());
        }
      }
    }

    response.setCode(ResponseCode.SUCCESS);
    response.setBody(heartbeatData.encode());
    response.setRemark(null);
    return response;
  }

  @Override
  public boolean rejectRequest() {
    return false;
  }
}
