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
package org.myberry.client.admin;

import org.myberry.client.AbstractMyberryClient;
import org.myberry.client.exception.MyberryClientException;
import org.myberry.client.exception.MyberryServerException;
import org.myberry.client.impl.admin.DefaultAdminClientImpl;
import org.myberry.client.spi.NoticeListener;
import org.myberry.common.Component;
import org.myberry.common.ProduceMode;
import org.myberry.common.protocol.body.admin.CRComponentData;
import org.myberry.common.protocol.body.admin.NSComponentData;
import org.myberry.common.protocol.body.admin.PageData;
import org.myberry.remoting.exception.RemotingException;

/** This class is the entry point for applications intending to manage component. */
public class DefaultAdminClient extends AbstractMyberryClient implements AdminClient {

  protected final transient DefaultAdminClientImpl defaultAdminClientImpl;

  /** Administrator client authentication */
  private String password;
  /** The group has DefaultAdminClient and DefaultUserClient. */
  private String clientGroup;
  /** Timeout for sending messages. */
  private int sendMsgTimeout = 3000;

  /** Notice SPI */
  private NoticeListener noticeListener;

  /** Default constructor. */
  public DefaultAdminClient() {
    this.defaultAdminClientImpl = new DefaultAdminClientImpl(this);
  }

  /**
   * Start this AdminClient instance. <strong> Much internal initializing procedures are carried out
   * to make this instance prepared, thus, it's a must to invoke this method before sending or
   * querying information. </strong>
   *
   * @throws MyberryClientException if there is any unexpected error.
   */
  @Override
  public void start() throws MyberryClientException {
    this.defaultAdminClientImpl.start();
  }

  /** This method shuts down this AdminClient instance and releases related resources. */
  @Override
  public void shutdown() {
    defaultAdminClientImpl.shutdown();
  }

  /**
   * Create Component in synchronous mode. This method returns only when the sending procedure
   * totally completes.
   *
   * @param component component to send.
   * @return {@link SendResult} instance to inform senders details of the deliverable, say result of
   *     the component, {@link SendStatus} indicating component status, etc.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public SendResult createComponent(Component component)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.defaultAdminClientImpl.createComponent(
        password, getProduceMode(component), component.encode());
  }

  /**
   * Same to {@link #createComponent(Component)} with send timeout specified in addition.
   *
   * @param component component to send.
   * @param timeout send timeout.
   * @return {@link SendResult} instance to inform senders details of the deliverable, say result of
   *     the component, {@link SendStatus} indicating component status, etc.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public SendResult createComponent(Component component, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.defaultAdminClientImpl.createComponent(
        password, getProduceMode(component), component.encode(), timeout);
  }

  /**
   * Create component asynchronously. This method returns immediately. On sending completion, <code>
   * sendCallback</code> will be executed.
   *
   * @param component component to send.
   * @param sendCallback Callback to execute on sending completed, either successful or
   *     unsuccessful.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public void createComponent(Component component, SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.defaultAdminClientImpl.createComponent(
        password, getProduceMode(component), component.encode(), sendCallback);
  }

  /**
   * Same to {@link #createComponent(Component, SendCallback)} with send timeout specified in
   * addition.
   *
   * @param component component to send.
   * @param timeout send timeout.
   * @param sendCallback Callback to execute on sending completed, either successful or
   *     unsuccessful.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public void createComponent(Component component, long timeout, SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.defaultAdminClientImpl.createComponent(
        password, getProduceMode(component), component.encode(), timeout, sendCallback);
  }

  /**
   * Query all component in synchronous mode. This method returns only when the sending procedure
   * totally completes.
   *
   * @param pageNo
   * @param pageSize
   * @return {@link SendResult} instance to inform senders details of the deliverable, say result
   *     list of the component, {@link SendStatus} indicating component status, etc.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public SendResult queryAllComponent(int pageNo, int pageSize)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.defaultAdminClientImpl.queryAllComponent(
        new PageData(pageNo, pageSize).encode(), password);
  }

  /**
   * Same to {@link #queryAllComponent(int, int)} with send timeout specified in addition.
   *
   * @param pageNo
   * @param pageSize
   * @param timeout send timeout.
   * @return {@link SendResult} instance to inform senders details of the deliverable, say result
   *     list of the component, {@link SendStatus} indicating component status, etc.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public SendResult queryAllComponent(int pageNo, int pageSize, long timeout)
      throws RemotingException, InterruptedException, MyberryServerException {
    return this.defaultAdminClientImpl.queryAllComponent(
        new PageData(pageNo, pageSize).encode(), password, timeout);
  }

  /**
   * Query all component asynchronously. This method returns immediately. On sending completion,
   * <code>sendCallback</code> will be executed.
   *
   * @param pageNo
   * @param pageSize
   * @param sendCallback Callback to execute on sending completed, either successful or
   *     unsuccessful.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public void queryAllComponent(int pageNo, int pageSize, SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.defaultAdminClientImpl.queryAllComponent(
        new PageData(pageNo, pageSize).encode(), password, sendCallback);
  }

  /**
   * Same to {@link #queryAllComponent(int, int, SendCallback)} with send timeout specified in
   * addition.
   *
   * @param pageNo
   * @param pageSize
   * @param timeout send timeout.
   * @param sendCallback Callback to execute on sending completed, either successful or
   *     unsuccessful.
   * @throws RemotingException if there is any network-tier error.
   * @throws MyberryServerException if there is any error with broker.
   * @throws InterruptedException if the sending thread is interrupted.
   */
  @Override
  public void queryAllComponent(int pageNo, int pageSize, long timeout, SendCallback sendCallback)
      throws RemotingException, InterruptedException, MyberryServerException {
    this.defaultAdminClientImpl.queryAllComponent(
        new PageData(pageNo, pageSize).encode(), password, timeout, sendCallback);
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setClientGroup(String clientGroup) {
    this.clientGroup = clientGroup;
  }

  public int getSendMsgTimeout() {
    return sendMsgTimeout;
  }

  public void setSendMsgTimeout(int sendMsgTimeout) {
    this.sendMsgTimeout = sendMsgTimeout;
  }

  public NoticeListener getNoticeListener() {
    return noticeListener;
  }

  public void setNoticeListener(NoticeListener noticeListener) {
    this.noticeListener = noticeListener;
  }

  private String getProduceMode(Component component) {
    if (component instanceof CRComponentData) {
      return ProduceMode.CR.getProduceName();
    } else if (component instanceof NSComponentData) {
      return ProduceMode.NS.getProduceName();
    } else {
      return ProduceMode.CR.getProduceName();
    }
  }

  @Override
  public String getClientGroup() {
    return clientGroup == null ? DefaultAdminClient.class.getSimpleName() : clientGroup;
  }
}
