/*
 * Copyright 2008 ZXing authors
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

package com.google.myzxing2.oned;

import com.google.myzxing2.BarcodeFormat_S;
import com.google.myzxing2.NotFoundException_S;
import com.google.myzxing2.common.BitArray_S;

/**
 * <p>Implements decoding of the EAN-8 format.</p>
 *
 * @author Sean Owen
 */
public final class EAN8Reader_S extends com.google.myzxing2.oned.UPCEANReader_S {

  private final int[] decodeMiddleCounters;

  public EAN8Reader_S() {
    decodeMiddleCounters = new int[4];
  }

  @Override
  protected int decodeMiddle(BitArray_S row,
                             int[] startRange,
                             StringBuilder result) throws NotFoundException_S {
    int[] counters = decodeMiddleCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;
    int end = row.getSize();
    int rowOffset = startRange[1];

    for (int x = 0; x < 4 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
      result.append((char) ('0' + bestMatch));
      for (int counter : counters) {
        rowOffset += counter;
      }
    }

    int[] middleRange = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
    rowOffset = middleRange[1];

    for (int x = 0; x < 4 && rowOffset < end; x++) {
      int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
      result.append((char) ('0' + bestMatch));
      for (int counter : counters) {
        rowOffset += counter;
      }
    }

    return rowOffset;
  }

  @Override
  BarcodeFormat_S getBarcodeFormat() {
    return BarcodeFormat_S.EAN_8;
  }

}
