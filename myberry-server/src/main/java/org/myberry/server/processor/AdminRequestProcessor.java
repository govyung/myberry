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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.myberry.common.ProduceMode;
import org.myberry.common.codec.LightCodec;
import org.myberry.common.constant.LoggerName;
import org.myberry.common.loadbalance.Invoker;
import org.myberry.common.protocol.RequestCode;
import org.myberry.common.protocol.ResponseCode;
import org.myberry.common.protocol.body.admin.CRComponentData;
import org.myberry.common.protocol.body.admin.ClusterListData;
import org.myberry.common.protocol.body.admin.ClusterListData.ClusterNode;
import org.myberry.common.protocol.body.admin.ComponentKeyData;
import org.myberry.common.protocol.body.admin.ComponentSizeData;
import org.myberry.common.protocol.body.admin.NSComponentData;
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
      case RequestCode.QUERY_COMPONENT_SIZE:
        response = queryComponentSize();
        break;
      case RequestCode.QUERY_COMPONENT_BY_KEY:
        ComponentKeyData componentKeyData =
            LightCodec.toObj(request.getBody(), ComponentKeyData.class);
        response = queryComponentByKey(componentKeyData.getKey());
        break;
      case RequestCode.QUERY_CLUSTER_LIST:
        response = queryClusterList();
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
    return createResponseCommand(result.getRespCode(), nsComponentData.getKey());
  }

  private RemotingCommand createResponseCommand(int code, String key) {
    ManageComponentResponseHeader responseHeader = new ManageComponentResponseHeader();
    responseHeader.setKey(key);

    RemotingCommand response = RemotingCommand.createResponseCommand(code, null);
    response.writeCustomHeader(responseHeader);
    return response;
  }

  private RemotingCommand queryComponentSize() {
    int size = serverController.getMyberryService().queryComponentSize();
    ComponentSizeData componentSizeData = new ComponentSizeData();
    componentSizeData.setSize(size);

    RemotingCommand response = RemotingCommand.createResponseCommand(null);
    response.setBody(LightCodec.toBytes(componentSizeData));
    response.setCode(ResponseCode.SUCCESS);
    response.setRemark(null);
    return response;
  }

  private RemotingCommand queryComponentByKey(String key) {
    ManageComponentResponseHeader responseHeader = new ManageComponentResponseHeader();
    responseHeader.setKey(key);
    responseHeader.setProduceMode(serverController.getStoreConfig().getProduceMode());

    AdminManageResult adminManageResult =
        serverController.getMyberryService().queryComponentByKey(key);

    RemotingCommand response = RemotingCommand.createResponseCommand(null);
    if (adminManageResult.getComponent() instanceof CRComponentData) {
      CRComponentData crcd = (CRComponentData) adminManageResult.getComponent();
      response.setBody(LightCodec.toBytes(crcd));
    } else if (adminManageResult.getComponent() instanceof NSComponentData) {
      NSComponentData nscd = (NSComponentData) adminManageResult.getComponent();
      response.setBody(LightCodec.toBytes(nscd));
    }

    response.writeCustomHeader(responseHeader);
    response.setCode(adminManageResult.getRespCode());
    response.setRemark(null);

    return response;
  }

  private RemotingCommand queryClusterList() {
    ClusterListData clusterListData = new ClusterListData();

    ArrayList<ClusterNode> clusters = new ArrayList<>();

    if (serverController.getServerConfig().getClusterName() != null) {
      String leaderInfo = serverController.getRouteInfoManager().getLeaderInfo();
      String[] leader = leaderInfo.split(":");
      ClusterNode clusterNode = new ClusterNode();
      clusterNode.setSid(serverController.getMyberryStore().getMySidFromDisk());
      clusterNode.setType("Leader");
      clusterNode.setIp(leader[0]);
      clusterNode.setServicePort(Integer.parseInt(leader[1]));
      clusters.add(clusterNode);

      Map<Integer, Invoker> learnerTable = serverController.getRouteInfoManager().getLearnerTable();
      Iterator<Entry<Integer, Invoker>> it = learnerTable.entrySet().iterator();

      while (it.hasNext()) {
        Entry<Integer, Invoker> entry = it.next();
        String[] learner = entry.getValue().getAddr().split(":");
        clusterNode = new ClusterNode();
        clusterNode.setSid(entry.getKey());
        clusterNode.setType("Learner");
        clusterNode.setIp(learner[0]);
        clusterNode.setServicePort(Integer.parseInt(learner[1]));
        clusters.add(clusterNode);
      }
    }

    clusterListData.setClusters(clusters);

    RemotingCommand response = RemotingCommand.createResponseCommand(null);
    response.setBody(LightCodec.toBytes(clusterListData));
    response.setCode(ResponseCode.SUCCESS);
    response.setRemark(null);
    return response;
  }

  public String getProcessorName() {
    return AdminRequestProcessor.class.getSimpleName();
  }
}
