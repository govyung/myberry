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
package org.myberry.server.quarum;

import org.junit.Assert;
import org.junit.Test;
import org.myberry.server.quarum.message.Proposal;
import org.myberry.server.quarum.message.Refuse;
import org.myberry.server.quarum.message.Vote;

public class MessageTest {

  @Test
  public void encodeAndDecodeTest() throws Exception {
    Vote vote = new Vote();
    vote.setSid(1);
    vote.setElectionEpoch(5L);
    vote.setLeader(7);
    vote.setPeerEpoch(3L);

    Vote v = (Vote) Message.decode(vote.encode(), Vote.class);
    Assert.assertNotNull(v);
    Assert.assertEquals(1, v.getSid());
    Assert.assertEquals(5, v.getElectionEpoch());
    Assert.assertEquals(7, v.getLeader());
    Assert.assertEquals(3, v.getPeerEpoch());

    Refuse refuse = new Refuse();
    refuse.setSid(10);
    refuse.setMsg("测试");

    Refuse r = (Refuse) Message.decode(refuse.encode(), Refuse.class);
    Assert.assertNotNull(r);
    Assert.assertEquals(10, r.getSid());
    Assert.assertEquals("测试", r.getMsg());
  }

  @Test
  public void encodeAndDecodeTest2() throws Exception {
    Proposal proposal = new Proposal();
    proposal.setSid(11);
    proposal.setOffset(123);

    Proposal p = (Proposal) Message.decode(proposal.encode(), Proposal.class);
    Assert.assertNotNull(p);
    Assert.assertEquals(11, p.getSid());
    Assert.assertEquals(123, p.getOffset());
  }
}
