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

package com.google.myzxing3.oned;

import com.google.myzxing3.BarcodeFormat_S;
import com.google.myzxing3.EncodeHintType_S;
import com.google.myzxing3.FormatException_S;
import com.google.myzxing3.WriterException_S;
import com.google.myzxing3.common.BitMatrix_S;

import java.util.Map;

/**
 * This object renders an EAN8 code as a {@link BitMatrix_S}.
 *
 * @author aripollak@gmail.com (Ari Pollak)
 */
public final class EAN8Writer_S extends com.google.myzxing3.oned.UPCEANWriter_S {

  private static final int CODE_WIDTH = 3 + // start guard
      (7 * 4) + // left bars
      5 + // middle guard
      (7 * 4) + // right bars
      3; // end guard

  @Override
  public BitMatrix_S encode(String contents,
                            BarcodeFormat_S format,
                            int width,
                            int height,
                            Map<EncodeHintType_S,?> hints) throws WriterException_S {
    if (format != BarcodeFormat_S.EAN_8) {
      throw new IllegalArgumentException("Can only encode EAN_8, but got "
          + format);
    }

    return super.encode(contents, format, width, height, hints);
  }

  /**
   * @return a byte array of horizontal pixels (false = white, true = black)
   */
  @Override
  public boolean[] encode(String contents) {
    int length = contents.length();
    switch (length) {
      case 7:
        // No check digit present, calculate it and add it
        int check;
        try {
          check = com.google.myzxing3.oned.UPCEANReader_S.getStandardUPCEANChecksum(contents);
        } catch (FormatException_S fe) {
          throw new IllegalArgumentException(fe);
        }
        contents += check;
        break;
      case 8:
        try {
          if (!com.google.myzxing3.oned.UPCEANReader_S.checkStandardUPCEANChecksum(contents)) {
            throw new IllegalArgumentException("Contents do not pass checksum");
          }
        } catch (FormatException_S ignored) {
          throw new IllegalArgumentException("Illegal contents");
        }
        break;
      default:
        throw new IllegalArgumentException(
            "Requested contents should be 7 or 8 digits long, but got " + length);
    }

    checkNumeric(contents);

    boolean[] result = new boolean[CODE_WIDTH];
    int pos = 0;

    pos += appendPattern(result, pos, com.google.myzxing3.oned.UPCEANReader_S.START_END_PATTERN, true);

    for (int i = 0; i <= 3; i++) {
      int digit = Character.digit(contents.charAt(i), 10);
      pos += appendPattern(result, pos, com.google.myzxing3.oned.UPCEANReader_S.L_PATTERNS[digit], false);
    }

    pos += appendPattern(result, pos, com.google.myzxing3.oned.UPCEANReader_S.MIDDLE_PATTERN, false);

    for (int i = 4; i <= 7; i++) {
      int digit = Character.digit(contents.charAt(i), 10);
      pos += appendPattern(result, pos, com.google.myzxing3.oned.UPCEANReader_S.L_PATTERNS[digit], true);
    }
    appendPattern(result, pos, com.google.myzxing3.oned.UPCEANReader_S.START_END_PATTERN, true);

    return result;
  }

}
