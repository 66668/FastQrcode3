/*
 * Copyright 2009 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this string_b except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.myzxing3.oned.rss;

final class Pair_S extends DataCharacter_S {

  private final FinderPattern_S finderPattern;
  private int count;

  Pair_S(int value, int checksumPortion, FinderPattern_S finderPattern) {
    super(value, checksumPortion);
    this.finderPattern = finderPattern;
  }

  FinderPattern_S getFinderPattern() {
    return finderPattern;
  }

  int getCount() {
    return count;
  }

  void incrementCount() {
    count++;
  }

}