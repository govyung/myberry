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
package org.myberry.client.router.loadbalance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.myberry.common.loadbalance.Invoker;

public class RandomLoadBalance implements LoadBalance {

  @Override
  public Invoker doSelect(List<Invoker> invokers, String key) {
    // Number of invokers
    int length = invokers.size();
    // Every invoker has the same weight?
    boolean sameWeight = true;
    // the weight of every invokers
    int[] weights = new int[length];
    // the first invoker's weight
    int firstWeight = invokers.get(0).getWeight();
    weights[0] = firstWeight;
    // The sum of weights
    int totalWeight = firstWeight;
    for (int i = 1; i < length; i++) {
      int weight = invokers.get(i).getWeight();
      // save for later use
      weights[i] = weight;
      // Sum
      totalWeight += weight;
      if (sameWeight && weight != firstWeight) {
        sameWeight = false;
      }
    }
    if (totalWeight > 0 && !sameWeight) {
      // If (not every invoker has the same weight & at least one invoker's weight>0), select
      // randomly based on totalWeight.
      int offset = ThreadLocalRandom.current().nextInt(totalWeight);
      // Return a invoker based on the random value.
      for (int i = 0; i < length; i++) {
        offset -= weights[i];
        if (offset < 0) {
          return invokers.get(i);
        }
      }
    }
    // If all invokers have the same weight value or totalWeight=0, return evenly.
    return invokers.get(ThreadLocalRandom.current().nextInt(length));
  }
}
