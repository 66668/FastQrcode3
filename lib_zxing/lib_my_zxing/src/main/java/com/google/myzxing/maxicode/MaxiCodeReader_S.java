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

package com.google.myzxing.maxicode;

import com.google.myzxing.BarcodeFormat_S;
import com.google.myzxing.BinaryBitmap_S;
import com.google.myzxing.ChecksumException_S;
import com.google.myzxing.DecodeHintType_S;
import com.google.myzxing.FormatException_S;
import com.google.myzxing.NotFoundException_S;
import com.google.myzxing.Reader_S;
import com.google.myzxing.Result_S;
import com.google.myzxing.ResultMetadataType_S;
import com.google.myzxing.ResultPoint_S;
import com.google.myzxing.common.BitMatrix_S;
import com.google.myzxing.common.DecoderResult_S;
import com.google.myzxing.maxicode.decoder.Decoder_S;

import java.util.Map;

/**
 * This implementation can detect and decode a MaxiCode in an image.
 */
public final class MaxiCodeReader_S implements Reader_S {

  private static final ResultPoint_S[] NO_POINTS = new ResultPoint_S[0];
  private static final int MATRIX_WIDTH = 30;
  private static final int MATRIX_HEIGHT = 33;

  private final Decoder_S decoder = new Decoder_S();

  /**
   * Locates and decodes a MaxiCode in an image.
   *
   * @return a String representing the content encoded by the MaxiCode
   * @throws NotFoundException_S if a MaxiCode cannot be found
   * @throws FormatException_S if a MaxiCode cannot be decoded
   * @throws ChecksumException_S if error correction fails
   */
  @Override
  public Result_S decode(BinaryBitmap_S image) throws NotFoundException_S, ChecksumException_S, FormatException_S {
    return decode(image, null);
  }

  @Override
  public Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, ChecksumException_S, FormatException_S {
    // Note that MaxiCode reader effectively always assumes PURE_BARCODE mode
    // and can't detect it in an image
    BitMatrix_S bits = extractPureBits(image.getBlackMatrix());
    DecoderResult_S decoderResult = decoder.decode(bits, hints);
    Result_S result = new Result_S(decoderResult.getText(), decoderResult.getRawBytes(), NO_POINTS, BarcodeFormat_S.MAXICODE);

    String ecLevel = decoderResult.getECLevel();
    if (ecLevel != null) {
      result.putMetadata(ResultMetadataType_S.ERROR_CORRECTION_LEVEL, ecLevel);
    }
    return result;
  }

  @Override
  public void reset() {
    // do nothing
  }

  /**
   * This method detects a code in a "pure" image -- that is, pure monochrome image
   * which contains only an unrotated, unskewed, image of a code, with some white border
   * around it. This is a specialized method that works exceptionally fast in this special
   * case.
   *
   * @see com.google.zxing.datamatrix.DataMatrixReader#extractPureBits(BitMatrix_S)
   * @see com.google.zxing.qrcode.QRCodeReader#extractPureBits(BitMatrix_S)
   */
  private static BitMatrix_S extractPureBits(BitMatrix_S image) throws NotFoundException_S {

    int[] enclosingRectangle = image.getEnclosingRectangle();
    if (enclosingRectangle == null) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    int left = enclosingRectangle[0];
    int top = enclosingRectangle[1];
    int width = enclosingRectangle[2];
    int height = enclosingRectangle[3];

    // Now just read off the bits
    BitMatrix_S bits = new BitMatrix_S(MATRIX_WIDTH, MATRIX_HEIGHT);
    for (int y = 0; y < MATRIX_HEIGHT; y++) {
      int iy = top + (y * height + height / 2) / MATRIX_HEIGHT;
      for (int x = 0; x < MATRIX_WIDTH; x++) {
        int ix = left + (x * width + width / 2 + (y & 0x01) *  width / 2) / MATRIX_WIDTH;
        if (image.get(ix, iy)) {
          bits.set(x, y);
        }
      }
    }
    return bits;
  }

}
