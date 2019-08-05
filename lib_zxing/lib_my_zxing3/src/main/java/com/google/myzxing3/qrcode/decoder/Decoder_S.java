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

package com.google.myzxing3.qrcode.decoder;

import com.google.myzxing3.ChecksumException_S;
import com.google.myzxing3.DecodeHintType_S;
import com.google.myzxing3.FormatException_S;
import com.google.myzxing3.common.BitMatrix_S;
import com.google.myzxing3.common.DecoderResult_S;
import com.google.myzxing3.common.reedsolomon.GenericGF_S;
import com.google.myzxing3.common.reedsolomon.ReedSolomonDecoder_S;
import com.google.myzxing3.common.reedsolomon.ReedSolomonException_S;

import java.util.Map;

/**
 * <p>The main class which implements QR Code decoding -- as opposed to locating and extracting
 * the QR Code from an image.</p>
 *
 * @author Sean Owen
 */
public final class Decoder_S {

  private final ReedSolomonDecoder_S rsDecoder;

  public Decoder_S() {
    rsDecoder = new ReedSolomonDecoder_S(GenericGF_S.QR_CODE_FIELD_256);
  }

  public DecoderResult_S decode(boolean[][] image) throws ChecksumException_S, FormatException_S {
    return decode(image, null);
  }

  /**
   * <p>Convenience method that can decode a QR Code represented as a 2D array of booleans.
   * "true" is taken to mean a black module.</p>
   *
   * @param image booleans representing white/black QR Code modules
   * @param hints decoding hints that should be used to influence decoding
   * @return text and bytes encoded within the QR Code
   * @throws FormatException_S if the QR Code cannot be decoded
   * @throws ChecksumException_S if error correction fails
   */
  public DecoderResult_S decode(boolean[][] image, Map<DecodeHintType_S,?> hints)
      throws ChecksumException_S, FormatException_S {
    return decode(BitMatrix_S.parse(image), hints);
  }

  public DecoderResult_S decode(BitMatrix_S bits) throws ChecksumException_S, FormatException_S {
    return decode(bits, null);
  }

  /**
   * <p>Decodes a QR Code represented as a {@link BitMatrix_S}. A 1 or "true" is taken to mean a black module.</p>
   *
   * @param bits booleans representing white/black QR Code modules
   * @param hints decoding hints that should be used to influence decoding
   * @return text and bytes encoded within the QR Code
   * @throws FormatException_S if the QR Code cannot be decoded
   * @throws ChecksumException_S if error correction fails
   */
  public DecoderResult_S decode(BitMatrix_S bits, Map<DecodeHintType_S,?> hints)
      throws FormatException_S, ChecksumException_S {

    // Construct a parser and read version, error-correction level
    BitMatrixParser_S parser = new BitMatrixParser_S(bits);
    FormatException_S fe = null;
    ChecksumException_S ce = null;
    try {
      return decode(parser, hints);
    } catch (FormatException_S e) {
      fe = e;
    } catch (ChecksumException_S e) {
      ce = e;
    }

    try {

      // Revert the bit matrix
      parser.remask();

      // Will be attempting a mirrored reading of the version and format info.
      parser.setMirror(true);

      // Preemptively read the version.
      parser.readVersion();

      // Preemptively read the format information.
      parser.readFormatInformation();

      /*
       * Since we're here, this means we have successfully detected some kind
       * of version and format information when mirrored. This is a good sign,
       * that the QR code may be mirrored, and we should try once more with a
       * mirrored content.
       */
      // Prepare for a mirrored reading.
      parser.mirror();

      DecoderResult_S result = decode(parser, hints);

      // Success! Notify the caller that the code was mirrored.
      result.setOther(new com.google.myzxing3.qrcode.decoder.QRCodeDecoderMetaData_S(true));

      return result;

    } catch (FormatException_S | ChecksumException_S e) {
      // Throw the exception from the original reading
      if (fe != null) {
        throw fe;
      }
      throw ce; // If fe is null, this can't be
    }
  }

  private DecoderResult_S decode(BitMatrixParser_S parser, Map<DecodeHintType_S,?> hints)
      throws FormatException_S, ChecksumException_S {
    Version_S version = parser.readVersion();
    com.google.myzxing3.qrcode.decoder.ErrorCorrectionLevel_S ecLevel = parser.readFormatInformation().getErrorCorrectionLevel();

    // Read codewords
    byte[] codewords = parser.readCodewords();
    // Separate into data blocks
    DataBlock_S[] dataBlocks = DataBlock_S.getDataBlocks(codewords, version, ecLevel);

    // Count total number of data bytes
    int totalBytes = 0;
    for (DataBlock_S dataBlock : dataBlocks) {
      totalBytes += dataBlock.getNumDataCodewords();
    }
    byte[] resultBytes = new byte[totalBytes];
    int resultOffset = 0;

    // Error-correct and copy data blocks together into a stream of bytes
    for (DataBlock_S dataBlock : dataBlocks) {
      byte[] codewordBytes = dataBlock.getCodewords();
      int numDataCodewords = dataBlock.getNumDataCodewords();
      correctErrors(codewordBytes, numDataCodewords);
      for (int i = 0; i < numDataCodewords; i++) {
        resultBytes[resultOffset++] = codewordBytes[i];
      }
    }

    // Decode the contents of that stream of bytes
    return DecodedBitStreamParser_S.decode(resultBytes, version, ecLevel, hints);
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
