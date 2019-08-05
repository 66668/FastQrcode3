/*
 * Copyright 2011 ZXing authors
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

package com.google.myzxing2.maxicode.decoder;

import com.google.myzxing2.ChecksumException_S;
import com.google.myzxing2.DecodeHintType_S;
import com.google.myzxing2.FormatException_S;
import com.google.myzxing2.common.BitMatrix_S;
import com.google.myzxing2.common.DecoderResult_S;
import com.google.myzxing2.common.reedsolomon.GenericGF_S;
import com.google.myzxing2.common.reedsolomon.ReedSolomonDecoder_S;
import com.google.myzxing2.common.reedsolomon.ReedSolomonException_S;

import java.util.Map;

/**
 * <p>The main class which implements MaxiCode decoding -- as opposed to locating and extracting
 * the MaxiCode from an image.</p>
 *
 * @author Manuel Kasten
 */
public final class Decoder_S {

  private static final int ALL = 0;
  private static final int EVEN = 1;
  private static final int ODD = 2;

  private final ReedSolomonDecoder_S rsDecoder;

  public Decoder_S() {
    rsDecoder = new ReedSolomonDecoder_S(GenericGF_S.MAXICODE_FIELD_64);
  }

  public DecoderResult_S decode(BitMatrix_S bits) throws ChecksumException_S, FormatException_S {
    return decode(bits, null);
  }

  public DecoderResult_S decode(BitMatrix_S bits,
                                Map<DecodeHintType_S,?> hints) throws FormatException_S, ChecksumException_S {
    BitMatrixParser_S parser = new BitMatrixParser_S(bits);
    byte[] codewords = parser.readCodewords();

    correctErrors(codewords, 0, 10, 10, ALL);
    int mode = codewords[0] & 0x0F;
    byte[] datawords;
    switch (mode) {
      case 2:
      case 3:
      case 4:
        correctErrors(codewords, 20, 84, 40, EVEN);
        correctErrors(codewords, 20, 84, 40, ODD);
        datawords = new byte[94];
        break;
      case 5:
        correctErrors(codewords, 20, 68, 56, EVEN);
        correctErrors(codewords, 20, 68, 56, ODD);
        datawords = new byte[78];
        break;
      default:
        throw FormatException_S.getFormatInstance();
    }

    System.arraycopy(codewords, 0, datawords, 0, 10);
    System.arraycopy(codewords, 20, datawords, 10, datawords.length - 10);

    return DecodedBitStreamParser_S.decode(datawords, mode);
  }

  private void correctErrors(byte[] codewordBytes,
                             int start,
                             int dataCodewords,
                             int ecCodewords,
                             int mode) throws ChecksumException_S {
    int codewords = dataCodewords + ecCodewords;

    // in EVEN or ODD mode only half the codewords
    int divisor = mode == ALL ? 1 : 2;

    // First read into an array of ints
    int[] codewordsInts = new int[codewords / divisor];
    for (int i = 0; i < codewords; i++) {
      if ((mode == ALL) || (i % 2 == (mode - 1))) {
        codewordsInts[i / divisor] = codewordBytes[i + start] & 0xFF;
      }
    }
    try {
      rsDecoder.decode(codewordsInts, ecCodewords / divisor);
    } catch (ReedSolomonException_S ignored) {
      throw ChecksumException_S.getChecksumInstance();
    }
    // Copy back into array of bytes -- only need to worry about the bytes that were data
    // We don't care about errors in the error-correction codewords
    for (int i = 0; i < dataCodewords; i++) {
      if ((mode == ALL) || (i % 2 == (mode - 1))) {
        codewordBytes[i + start] = (byte) codewordsInts[i / divisor];
      }
    }
  }

}
