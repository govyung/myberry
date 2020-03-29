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
package org.myberry.common.expression.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.myberry.common.expression.Parser;

public class RandomParser implements Parser {

	private String prefix = "#rand(";
	private String suffix = ")";
	private final int minLength = 1;
	private final int maxLength = 2 << 3;

	private final Pattern pattern = Pattern.compile("^[\\d]*$");

	@Override
	public boolean doParse(String placeholder) {
		if (placeholder.startsWith(prefix) && placeholder.endsWith(suffix)) {
			String slice = placeholder.substring(prefix.length(), placeholder.length() - 1);
			Matcher matcher = pattern.matcher(slice);
			if (matcher.matches()) {
				int parseInt = Integer.parseInt(slice);
				if (parseInt >= minLength && parseInt <= maxLength) {
					return true;
				}
			}
		}
		return false;
	}

	public String getSeedLength(String placeholder) {
		return placeholder.substring(prefix.length(), placeholder.length() - 1);
	}

}
