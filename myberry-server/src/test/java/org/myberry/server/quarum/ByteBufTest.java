/*
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
package org.myberry.server.quarum;

import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import org.myberry.server.quarum.message.Proposal;
import org.myberry.server.quarum.message.Vote;

public class ByteBufTest {

  @Test
  public void encodeAndDecodeTest() {
    Vote vote = new Vote();
    vote.setSid(1);
    vote.setElectionEpoch(5L);
    vote.setLeader(7);
    vote.setPeerEpoch(3L);
    ByteBuf byteBuf1 = new ByteBuf();
    byteBuf1.setVersion(30);
    byteBuf1.setClusterName("测试");
    byteBuf1.setRunningMode(55);
    byteBuf1.setFileSize(1000);
    byteBuf1.setConnId(101);
    byteBuf1.setData(vote);
    ByteBuffer byteBuffer1 = byteBuf1.encode();

    int pos1 = 0;
    ByteBuf decode1 = ByteBuf.decode(byteBuffer1, pos1);
    Assert.assertEquals(30, decode1.getVersion());
    Assert.assertEquals("测试", decode1.getClusterName());
    Assert.assertEquals(55, decode1.getRunningMode());
    Assert.assertEquals(1000, decode1.getFileSize());
    Assert.assertEquals(101, decode1.getConnId());
    Assert.assertNotNull(decode1.getData());
    Vote v = (Vote) decode1.getData();
    Assert.assertEquals(1, v.getSid());
    Assert.assertEquals(5L, v.getElectionEpoch());
    Assert.assertEquals(7, v.getLeader());
    Assert.assertEquals(3L, v.getPeerEpoch());

    Proposal proposal = new Proposal();
    proposal.setSid(11);
    proposal.setOffset(123);
    ByteBuf byteBuf2 = new ByteBuf();
    byteBuf2.setVersion(30);
    byteBuf2.setClusterName("测试");
    byteBuf2.setRunningMode(55);
    byteBuf2.setFileSize(1000);
    byteBuf2.setConnId(101);
    byteBuf2.setData(proposal);
    ByteBuffer byteBuffer2 = byteBuf2.encode();

    int pos2 = 0;
    ByteBuf decode2 = ByteBuf.decode(byteBuffer2, pos2);
    Assert.assertEquals(30, decode2.getVersion());
    Assert.assertEquals("测试", decode2.getClusterName());
    Assert.assertEquals(55, decode2.getRunningMode());
    Assert.assertEquals(1000, decode2.getFileSize());
    Assert.assertEquals(101, decode2.getConnId());
    Assert.assertNotNull(decode2.getData());
    Proposal p = (Proposal) decode2.getData();
    Assert.assertEquals(11, p.getSid());
    Assert.assertEquals(123, p.getOffset());
  }
}
