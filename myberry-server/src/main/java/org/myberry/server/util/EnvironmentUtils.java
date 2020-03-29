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
package org.myberry.server.util;

import java.lang.reflect.Field;

import org.myberry.common.annotation.ImportantField;

public class EnvironmentUtils {

	public static void printConfig(boolean isImportant, Object... object) {
		for (Object obj : object) {
			Class<? extends Object> cls = obj.getClass();
			Field[] fields = cls.getDeclaredFields();
			Field.setAccessible(fields, true);
			for (Field field : fields) {
				if (isImportant) {
					boolean fieldHasAnno = field.isAnnotationPresent(ImportantField.class);
					if (fieldHasAnno) {
						try {
							System.out.println(field.getName() + ":" + field.get(obj));
						} catch (Exception e) {
						}
					}
				} else {
					try {
						System.out.println(field.getName() + ":" + field.get(obj));
					} catch (Exception e) {
					}
				}

			}
		}
	}
}
