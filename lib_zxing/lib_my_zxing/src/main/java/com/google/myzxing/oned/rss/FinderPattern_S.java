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

package com.google.myzxing.oned.rss;

import com.google.myzxing.ResultPoint_S;

/**
 * Encapsulates an RSS barcode finder pattern, including its start/end position and row.
 */
public final class FinderPattern_S {

  private final int value;
  private final int[] startEnd;
  private final ResultPoint_S[] resultPoints;

  public FinderPattern_S(int value, int[] startEnd, int start, int end, int rowNumber) {
    this.value = value;
    this.startEnd = startEnd;
    this.resultPoints = new ResultPoint_S[] {
        new ResultPoint_S(start, rowNumber),
        new ResultPoint_S(end, rowNumber),
    };
  }

  public int getValue() {
    return value;
  }

  public int[] getStartEnd() {
    return startEnd;
  }

  public ResultPoint_S[] getResultPoints() {
    return resultPoints;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof FinderPattern_S)) {
      return false;
    }
    FinderPattern_S that = (FinderPattern_S) o;
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return value;
  }

}
