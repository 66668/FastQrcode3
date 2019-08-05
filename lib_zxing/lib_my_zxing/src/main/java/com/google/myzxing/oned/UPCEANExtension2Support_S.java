/*
 * Copyright (C) 2012 ZXing authors
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

package com.google.myzxing.oned;

import com.google.myzxing.BarcodeFormat_S;
import com.google.myzxing.NotFoundException_S;
import com.google.myzxing.Result_S;
import com.google.myzxing.ResultMetadataType_S;
import com.google.myzxing.ResultPoint_S;
import com.google.myzxing.common.BitArray_S;

import java.util.EnumMap;
import java.util.Map;

/**
 * @see UPCEANExtension5Support_S
 */
final class UPCEANExtension2Support_S {

  private final int[] decodeMiddleCounters = new int[4];
  private final StringBuilder decodeRowStringBuffer = new StringBuilder();

  Result_S decodeRow(int rowNumber, BitArray_S row, int[] extensionStartRange) throws NotFoundException_S {

    StringBuilder result = decodeRowStringBuffer;
    result.setLength(0);
    int end = decodeMiddle(row, extensionStartRange, result);

    String resultString = result.toString();
    Map<ResultMetadataType_S,Object> extensionData = parseExtensionString(resultString);

    Result_S extensionResult =
        new Result_S(resultString,
                   null,
                   new ResultPoint_S[] {
                       new ResultPoint_S((extensionStartRange[0] + extensionStartRange[1]) / 2.0f, rowNumber),
                       new ResultPoint_S(end, rowNumber),
                   },
                   BarcodeFormat_S.UPC_EAN_EXTENSION);
    if (extensionData != null) {
      extensionResult.putAllMetadata(extensionData);
    }
    return extensionResult;
  }

  private int decodeMiddle(BitArray_S row, int[] startRange, StringBuilder resultString) throws NotFoundException_S {
    int[] counters = decodeMiddleCounters;
    counters[0] = 0;
    counters[1] = 0;
    counters[2] = 0;
    counters[3] = 0;
    int end = row.getSize();
    int rowOffset = startRange[1];

    int checkParity = 0;

    for (int x = 0; x < 2 && rowOffset < end; x++) {
      int bestMatch = UPCEANReader_S.decodeDigit(row, counters, rowOffset, UPCEANReader_S.L_AND_G_PATTERNS);
      resultString.append((char) ('0' + bestMatch % 10));
      for (int counter : counters) {
        rowOffset += counter;
      }
      if (bestMatch >= 10) {
        checkParity |= 1 << (1 - x);
      }
      if (x != 1) {
        // Read off separator if not last
        rowOffset = row.getNextSet(rowOffset);
        rowOffset = row.getNextUnset(rowOffset);
      }
    }

    if (resultString.length() != 2) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    if (Integer.parseInt(resultString.toString()) % 4 != checkParity) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    return rowOffset;
  }

  /**
   * @param raw raw content of extension
   * @return formatted interpretation of raw content as a {@link Map} mapping
   *  one {@link ResultMetadataType_S} to appropriate value, or {@code null} if not known
   */
  private static Map<ResultMetadataType_S,Object> parseExtensionString(String raw) {
    if (raw.length() != 2) {
      return null;
    }
    Map<ResultMetadataType_S,Object> result = new EnumMap<>(ResultMetadataType_S.class);
    result.put(ResultMetadataType_S.ISSUE_NUMBER, Integer.valueOf(raw));
    return result;
  }

}
