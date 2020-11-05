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
package org.myberry.common.codec;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LightCodecTest {

  private static TestObject testObject = new TestObject();

  private static TestNullObject testNullObject = new TestNullObject();

  @BeforeClass
  public static void before() {
    testObject.setA(1);
    testObject.setB(2);
    testObject.setC(3L);
    testObject.setD(4L);
    testObject.setE(5F);
    testObject.setF(6F);
    testObject.setG(7D);
    testObject.setH(8D);
    testObject.setI(false);
    testObject.setJ(true);
    testObject.setK("中文 English");

    List<Integer> l1 = new ArrayList<>();
    l1.add(11);
    l1.add(12);
    testObject.setL(l1);

    ArrayList<Long> l2 = new ArrayList<>();
    l2.add(21L);
    l2.add(22L);
    testObject.setM(l2);

    ArrayList<Float> l3 = new ArrayList<>();
    l3.add(31F);
    l3.add(32F);
    testObject.setN(l3);

    List<Double> l4 = new ArrayList<>();
    l4.add(41D);
    l4.add(42D);
    testObject.setO(l4);

    List<Boolean> l5 = new ArrayList<>();
    l5.add(false);
    l5.add(true);
    testObject.setP(l5);

    List<String> l6 = new ArrayList<>();
    l6.add("中文");
    l6.add("English");
    testObject.setQ(l6);

    InnnerObj innnerObj = new InnnerObj();
    innnerObj.setName("测试");

    List<Integer> orders = new ArrayList<>();
    orders.add(111);
    orders.add(222);
    innnerObj.setOrder(orders);

    testObject.setInnnerObj(innnerObj);

    InnnerObj innnerObj1 = new InnnerObj();
    innnerObj1.setName("测试1");

    List<Integer> orders1 = new ArrayList<>();
    orders1.add(1111);
    orders1.add(2222);
    innnerObj1.setOrder(orders1);

    InnnerObj innnerObj2 = new InnnerObj();
    innnerObj2.setName("测试2");

    List<Integer> orders2 = new ArrayList<>();
    orders2.add(3333);
    orders2.add(4444);
    innnerObj2.setOrder(orders2);

    List<InnnerObj> innnerObjs = new ArrayList<>();
    innnerObjs.add(innnerObj1);
    innnerObjs.add(innnerObj2);

    testObject.setInnnerObjs(innnerObjs);
  }

  @Test
  public void testEncodeDecode() {
    byte[] bytes = LightCodec.toBytes(testObject);
    TestObject obj = LightCodec.toObj(bytes, TestObject.class);

    Assert.assertEquals(1, obj.getA());
    Assert.assertEquals(2, obj.getB().intValue());
    Assert.assertEquals(3L, obj.getC());
    Assert.assertEquals(4L, obj.getD().longValue());
    Assert.assertEquals(5F, obj.getE(), .0);
    Assert.assertEquals(6F, obj.getF().floatValue(), .0);
    Assert.assertEquals(7D, obj.getG(), .0);
    Assert.assertEquals(8D, obj.getH().doubleValue(), .0);
    Assert.assertEquals(false, obj.isI());
    Assert.assertEquals(true, obj.getJ().booleanValue());
    Assert.assertEquals("中文 English", obj.getK());
    Assert.assertEquals(LightCodecTest.testObject.getL(), obj.getL());
    Assert.assertEquals(LightCodecTest.testObject.getM(), obj.getM());
    Assert.assertEquals(LightCodecTest.testObject.getN(), obj.getN());
    Assert.assertEquals(LightCodecTest.testObject.getO(), obj.getO());
    Assert.assertEquals(LightCodecTest.testObject.getP(), obj.getP());
    Assert.assertEquals(LightCodecTest.testObject.getQ(), obj.getQ());
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObj().getName(), obj.getInnnerObj().getName());
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObj().getOrder(), obj.getInnnerObj().getOrder());
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObjs().get(0).getName(),
        obj.getInnnerObjs().get(0).getName());
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObjs().get(0).getOrder().get(0),
        obj.getInnnerObjs().get(0).getOrder().get(0));
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObjs().get(0).getOrder().get(1),
        obj.getInnnerObjs().get(0).getOrder().get(1));
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObjs().get(1).getName(),
        obj.getInnnerObjs().get(1).getName());
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObjs().get(1).getOrder().get(0),
        obj.getInnnerObjs().get(1).getOrder().get(0));
    Assert.assertEquals(
        LightCodecTest.testObject.getInnnerObjs().get(1).getOrder().get(1),
        obj.getInnnerObjs().get(1).getOrder().get(1));
  }

  @Test
  public void testNullDefault() {
    byte[] bytes = LightCodec.toBytes(testNullObject);
    TestNullObject testNullObject = LightCodec.toObj(bytes, TestNullObject.class);

    Assert.assertEquals(5, testNullObject.getAa());
    Assert.assertEquals(NullObjects.NULL_INT_DEFAULT, testNullObject.getBb().intValue());
    Assert.assertEquals(NullObjects.NULL_LONG_DEFAULT, testNullObject.getCc().longValue());
    Assert.assertEquals(NullObjects.NULL_FLOAT_DEFAULT, testNullObject.getDd().floatValue(), .0);
    Assert.assertEquals(NullObjects.NULL_DOUBLE_DEFAULT, testNullObject.getEe().doubleValue(), .0);
    Assert.assertEquals(NullObjects.NULL_BOOLEAN_DEFAULT, testNullObject.getFf());
    Assert.assertEquals(NullObjects.NULL_STRING_DEFAULT, testNullObject.getGg());
    Assert.assertEquals(null, testNullObject.getHh());
    Assert.assertEquals(new ArrayList<>(0), testNullObject.getIi());
    Assert.assertEquals(new ArrayList<>(0), testNullObject.getJj());
    Assert.assertEquals(new ArrayList<>(0), testNullObject.getKk());
    Assert.assertEquals(new ArrayList<>(0), testNullObject.getLl());
    Assert.assertEquals(new ArrayList<>(0), testNullObject.getMm());
    Assert.assertEquals(new ArrayList<>(0), testNullObject.getNn());
    Assert.assertEquals(new ArrayList<>(0), testNullObject.getOo());
  }

  /*@Test
  public void testPerformance() {
    long startTimestamp = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      byte[] bytes = LightCodec.toBytes(testObject);
      LightCodec.toObj(bytes, TestObject.class);
    }
    long endTimestamp = System.currentTimeMillis();

    System.out.print("执行耗时:" + (endTimestamp - startTimestamp));
  }

  @Test
  public void testFastjsonPerformance() throws UnsupportedEncodingException {
    long startTimestamp = System.currentTimeMillis();
    for (int i = 0; i < 10000; i++) {
      byte[] bytes = com.alibaba.fastjson.JSON.toJSONBytes(testObject);

      String json = new String(bytes, "UTF-8");
      com.alibaba.fastjson.JSON.parseObject(json, TestObject.class);
    }
    long endTimestamp = System.currentTimeMillis();

    System.out.print("执行耗时:" + (endTimestamp - startTimestamp));
  }*/

}
