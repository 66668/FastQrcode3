/*
 * Copyright 2007 ZXing authors
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

package com.google.myzxing.datamatrix.decoder;


import com.google.myzxing.ChecksumException_S;
import com.google.myzxing.FormatException_S;
import com.google.myzxing.common.BitMatrix_S;
import com.google.myzxing.common.DecoderResult_S;
import com.google.myzxing.common.reedsolomon.GenericGF_S;
import com.google.myzxing.common.reedsolomon.ReedSolomonDecoder_S;
import com.google.myzxing.common.reedsolomon.ReedSolomonException_S;

/**
 * <p>The main class which implements Data Matrix Code decoding -- as opposed to locating and extracting
 * the Data Matrix Code from an image.</p>
 *
 * @author bbrown@google.com (Brian Brown)
 */
public final class Decoder_S {

  private final ReedSolomonDecoder_S rsDecoder;

  public Decoder_S() {
    rsDecoder = new ReedSolomonDecoder_S(GenericGF_S.DATA_MATRIX_FIELD_256);
  }

  /**
   * <p>Convenience method that can decode a Data Matrix Code represented as a 2D array of booleans.
   * "true" is taken to mean a black module.</p>
   *
   * @param image booleans representing white/black Data Matrix Code modules
   * @return text and bytes encoded within the Data Matrix Code
   * @throws FormatException_S if the Data Matrix Code cannot be decoded
   * @throws ChecksumException_S if error correction fails
   */
  public DecoderResult_S decode(boolean[][] image) throws FormatException_S, ChecksumException_S {
    return decode(BitMatrix_S.parse(image));
  }

  /**
   * <p>Decodes a Data Matrix Code represented as a {@link BitMatrix_S}. A 1 or "true" is taken
   * to mean a black module.</p>
   *
   * @param bits booleans representing white/black Data Matrix Code modules
   * @return text and bytes encoded within the Data Matrix Code
   * @throws FormatException_S if the Data Matrix Code cannot be decoded
   * @throws ChecksumException_S if error correction fails
   */
  public DecoderResult_S decode(BitMatrix_S bits) throws FormatException_S, ChecksumException_S {

    // Construct a parser and read version, error-correction level
    BitMatrixParser_S parser = new BitMatrixParser_S(bits);
    Version_S version = parser.getVersion();

    // Read codewords
    byte[] codewords = parser.readCodewords();
    // Separate into data blocks
    DataBlock_S[] dataBlocks = DataBlock_S.getDataBlocks(codewords, version);

    // Count total number of data bytes
    int totalBytes = 0;
    for (DataBlock_S db : dataBlocks) {
      totalBytes += db.getNumDataCodewords();
    }
    byte[] resultBytes = new byte[totalBytes];

    int dataBlocksCount = dataBlocks.length;
    // Error-correct and copy data blocks together into a stream of bytes
    for (int j = 0; j < dataBlocksCount; j++) {
      DataBlock_S dataBlock = dataBlocks[j];
      byte[] codewordBytes = dataBlock.getCodewords();
      int numDataCodewords = dataBlock.getNumDataCodewords();
      correctErrors(codewordBytes, numDataCodewords);
      for (int i = 0; i < numDataCodewords; i++) {
        // De-interlace data blocks.
        resultBytes[i * dataBlocksCount + j] = codewordBytes[i];
      }
    }

    // Decode the contents of that stream of bytes
    return DecodedBitStreamParser_S.decode(resultBytes);
  }

  /**
   * <p>Given data and error-correction codewords received, possibly corrupted by errors, attempts to
   * correct the errors in-place using Reed-Solomon error correction.</p>
   *
   * @param codewordBytes data and error correction codewords
   * @param numDataCodewords number of codewords that are data bytes
   * @throws ChecksumException_S if error correction fails
   */
  private void correctErrors(byte[] codewordBytes, int numDataCodewords) throws ChecksumException_S {
    int numCodewords = codewordBytes.length;
    // First read into an array of ints
    int[] codewordsInts = new int[numCodewords];
    for (int i = 0; i < numCodewords; i++) {
      codewordsInts[i] = codewordBytes[i] & 0xFF;
    }
    try {
      rsDecoder.decode(codewordsInts, codewordBytes.length - numDataCodewords);
    } catch (ReedSolomonException_S ignored) {
      throw ChecksumException_S.getChecksumInstance();
    }
    // Copy back into array of bytes -- only need to worry about the bytes that were data
    // We don't care about errors in the error-correction codewords
    for (int i = 0; i < numDataCodewords; i++) {
      codewordBytes[i] = (byte) codewordsInts[i];
    }
  }

}
