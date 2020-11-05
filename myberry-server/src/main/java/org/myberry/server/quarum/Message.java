/*
 * MIT License
 *
 * Copyright (c) 2020 gaoyang
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
package org.myberry.server.quarum;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.lang.reflect.Field;
import org.myberry.server.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Message {

  private static final Logger log = LoggerFactory.getLogger(LoggerName.QUORUM_LOGGER_NAME);

  public byte[] encode() {
    return JSON.toJSONBytes(this, SerializerFeature.BeanToArray);
  }

  public static Message decode(byte[] data, Class<? extends Message> clz) {
    return JSON.parseObject(data, clz, Feature.SupportArrayToBean);
  }

  protected abstract int type();

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append(this.getClass().getSimpleName());
    stringBuilder.append(" [");
    Field[] fields = this.getClass().getDeclaredFields();
    for (int i = 0; i < fields.length; i++) {
      Field field = fields[i];
      field.setAccessible(true);
      stringBuilder.append(field.getName());
      stringBuilder.append("=");
      try {
        stringBuilder.append(field.get(this));
      } catch (IllegalArgumentException | IllegalAccessException e) {
        log.warn("{} toString() error: ", this.getClass().getSimpleName(), e);
      }
      if (i != fields.length - 1) {
        stringBuilder.append(", ");
      }
    }
    stringBuilder.append("]");

    return stringBuilder.toString();
  }
}
