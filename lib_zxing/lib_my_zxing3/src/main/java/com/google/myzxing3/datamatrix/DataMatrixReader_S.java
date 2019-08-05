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

package com.google.myzxing3.datamatrix;

import com.google.myzxing3.Reader_S;
import com.google.myzxing3.Result_S;
import com.google.myzxing3.BarcodeFormat_S;
import com.google.myzxing3.BinaryBitmap_S;
import com.google.myzxing3.ChecksumException_S;
import com.google.myzxing3.DecodeHintType_S;
import com.google.myzxing3.FormatException_S;
import com.google.myzxing3.NotFoundException_S;
import com.google.myzxing3.ResultMetadataType_S;
import com.google.myzxing3.ResultPoint_S;
import com.google.myzxing3.common.BitMatrix_S;
import com.google.myzxing3.common.DecoderResult_S;
import com.google.myzxing3.common.DetectorResult_S;
import com.google.myzxing3.datamatrix.decoder.Decoder_S;
import com.google.myzxing3.datamatrix.detector.Detector_S;

import java.util.List;
import java.util.Map;

/**
 * This implementation can detect and decode Data Matrix codes in an image.
 *
 * @author bbrown@google.com (Brian Brown)
 */
public final class DataMatrixReader_S implements Reader_S {

  private static final ResultPoint_S[] NO_POINTS = new ResultPoint_S[0];

  private final Decoder_S decoder = new Decoder_S();

  /**
   * Locates and decodes a Data Matrix code in an image.
   *
   * @return a String representing the content encoded by the Data Matrix code
   * @throws NotFoundException_S if a Data Matrix code cannot be found
   * @throws FormatException_S if a Data Matrix code cannot be decoded
   * @throws ChecksumException_S if error correction fails
   */
  @Override
  public Result_S decode(BinaryBitmap_S image) throws NotFoundException_S, ChecksumException_S, FormatException_S {
    return decode(image, null);
  }

  @Override
  public Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, ChecksumException_S, FormatException_S {
    DecoderResult_S decoderResult;
    ResultPoint_S[] points;
    if (hints != null && hints.containsKey(DecodeHintType_S.PURE_BARCODE)) {
      BitMatrix_S bits = extractPureBits(image.getBlackMatrix());
      decoderResult = decoder.decode(bits);
      points = NO_POINTS;
    } else {
      DetectorResult_S detectorResult = new Detector_S(image.getBlackMatrix()).detect();
      decoderResult = decoder.decode(detectorResult.getBits());
      points = detectorResult.getPoints();
    }
    Result_S result = new Result_S(decoderResult.getText(), decoderResult.getRawBytes(), points,
        BarcodeFormat_S.DATA_MATRIX);
    List<byte[]> byteSegments = decoderResult.getByteSegments();
    if (byteSegments != null) {
      result.putMetadata(ResultMetadataType_S.BYTE_SEGMENTS, byteSegments);
    }
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
   * @see com.google.zxing.qrcode.QRCodeReader#extractPureBits(BitMatrix_S)
   */
  private static BitMatrix_S extractPureBits(BitMatrix_S image) throws NotFoundException_S {

    int[] leftTopBlack = image.getTopLeftOnBit();
    int[] rightBottomBlack = image.getBottomRightOnBit();
    if (leftTopBlack == null || rightBottomBlack == null) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    int moduleSize = moduleSize(leftTopBlack, image);

    int top = leftTopBlack[1];
    int bottom = rightBottomBlack[1];
    int left = leftTopBlack[0];
    int right = rightBottomBlack[0];

    int matrixWidth = (right - left + 1) / moduleSize;
    int matrixHeight = (bottom - top + 1) / moduleSize;
    if (matrixWidth <= 0 || matrixHeight <= 0) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    // Push in the "border" by half the module width so that we start
    // sampling in the middle of the module. Just in case the image is a
    // little off, this will help recover.
    int nudge = moduleSize / 2;
    top += nudge;
    left += nudge;

    // Now just read off the bits
    BitMatrix_S bits = new BitMatrix_S(matrixWidth, matrixHeight);
    for (int y = 0; y < matrixHeight; y++) {
      int iOffset = top + y * moduleSize;
      for (int x = 0; x < matrixWidth; x++) {
        if (image.get(left + x * moduleSize, iOffset)) {
          bits.set(x, y);
        }
      }
    }
    return bits;
  }

  private static int moduleSize(int[] leftTopBlack, BitMatrix_S image) throws NotFoundException_S {
    int width = image.getWidth();
    int x = leftTopBlack[0];
    int y = leftTopBlack[1];
    while (x < width && image.get(x, y)) {
      x++;
    }
    if (x == width) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    int moduleSize = x - leftTopBlack[0];
    if (moduleSize == 0) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    return moduleSize;
  }

}