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
package org.myberry.store.util;

import org.myberry.common.RunningMode;
import org.myberry.store.impl.AbstractComponent;
import org.myberry.store.impl.CRComponent;
import org.myberry.store.impl.NSComponent;

public class MappingUtils {

  public static int getRunningModeMapping(String runningMode) {
    Integer code = RunningMode.getCode(runningMode);
    if (code == null) {
      return RunningMode.getCode(RunningMode.CR.getRunningName());
    } else {
      return code.intValue();
    }
  }

  public static AbstractComponent getComponentMapping(int runningMode) {
    if (RunningMode.CR.getRunningCode() == runningMode) {
      return new CRComponent();
    } else if (RunningMode.NS.getRunningCode() == runningMode) {
      return new NSComponent();
    } else {
      return new CRComponent();
    }
  }
}
