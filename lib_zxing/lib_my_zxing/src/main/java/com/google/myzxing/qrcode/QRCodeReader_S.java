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

package com.google.myzxing.qrcode;

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
import com.google.myzxing.common.DetectorResult_S;
import com.google.myzxing.datamatrix.DataMatrixReader_S;
import com.google.myzxing.qrcode.decoder.Decoder_S;
import com.google.myzxing.qrcode.decoder.QRCodeDecoderMetaData_S;
import com.google.myzxing.qrcode.detector.Detector_S;

import java.util.List;
import java.util.Map;

/**
 * This implementation can detect and decode QR Codes in an image.
 *
 * @author Sean Owen
 */
public class QRCodeReader_S implements Reader_S {

  private static final ResultPoint_S[] NO_POINTS = new ResultPoint_S[0];

  private final Decoder_S decoder = new Decoder_S();

  protected final Decoder_S getDecoder() {
    return decoder;
  }

  /**
   * Locates and decodes a QR code in an image.
   *
   * @return a String representing the content encoded by the QR code
   * @throws NotFoundException_S if a QR code cannot be found
   * @throws FormatException_S if a QR code cannot be decoded
   * @throws ChecksumException_S if error correction fails
   */
  @Override
  public Result_S decode(BinaryBitmap_S image) throws NotFoundException_S, ChecksumException_S, FormatException_S {
    return decode(image, null);
  }

  @Override
  public final Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, ChecksumException_S, FormatException_S {
    DecoderResult_S decoderResult;
    ResultPoint_S[] points;
    if (hints != null && hints.containsKey(DecodeHintType_S.PURE_BARCODE)) {
      BitMatrix_S bits = extractPureBits(image.getBlackMatrix());
      decoderResult = decoder.decode(bits, hints);
      points = NO_POINTS;
    } else {
      DetectorResult_S detectorResult = new Detector_S(image.getBlackMatrix()).detect(hints);
      decoderResult = decoder.decode(detectorResult.getBits(), hints);
      points = detectorResult.getPoints();
    }

    // If the code was mirrored: swap the bottom-left and the top-right points.
    if (decoderResult.getOther() instanceof QRCodeDecoderMetaData_S) {
      ((QRCodeDecoderMetaData_S) decoderResult.getOther()).applyMirroredCorrection(points);
    }

    Result_S result = new Result_S(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat_S.QR_CODE);
    List<byte[]> byteSegments = decoderResult.getByteSegments();
    if (byteSegments != null) {
      result.putMetadata(ResultMetadataType_S.BYTE_SEGMENTS, byteSegments);
    }
    String ecLevel = decoderResult.getECLevel();
    if (ecLevel != null) {
      result.putMetadata(ResultMetadataType_S.ERROR_CORRECTION_LEVEL, ecLevel);
    }
    if (decoderResult.hasStructuredAppend()) {
      result.putMetadata(ResultMetadataType_S.STRUCTURED_APPEND_SEQUENCE,
                         decoderResult.getStructuredAppendSequenceNumber());
      result.putMetadata(ResultMetadataType_S.STRUCTURED_APPEND_PARITY,
                         decoderResult.getStructuredAppendParity());
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
   * @see DataMatrixReader_S#extractPureBits(BitMatrix_S)
   */
  private static BitMatrix_S extractPureBits(BitMatrix_S image) throws NotFoundException_S {

    int[] leftTopBlack = image.getTopLeftOnBit();
    int[] rightBottomBlack = image.getBottomRightOnBit();
    if (leftTopBlack == null || rightBottomBlack == null) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    float moduleSize = moduleSize(leftTopBlack, image);

    int top = leftTopBlack[1];
    int bottom = rightBottomBlack[1];
    int left = leftTopBlack[0];
    int right = rightBottomBlack[0];

    // Sanity check!
    if (left >= right || top >= bottom) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    if (bottom - top != right - left) {
      // Special case, where bottom-right module wasn't black so we found something else in the last row
      // Assume it's a square, so use height as the width
      right = left + (bottom - top);
      if (right >= image.getWidth()) {
        // Abort if that would not make sense -- off image
        throw NotFoundException_S.getNotFoundInstance();
      }
    }

    int matrixWidth = Math.round((right - left + 1) / moduleSize);
    int matrixHeight = Math.round((bottom - top + 1) / moduleSize);
    if (matrixWidth <= 0 || matrixHeight <= 0) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    if (matrixHeight != matrixWidth) {
      // Only possibly decode square regions
      throw NotFoundException_S.getNotFoundInstance();
    }

    // Push in the "border" by half the module width so that we start
    // sampling in the middle of the module. Just in case the image is a
    // little off, this will help recover.
    int nudge = (int) (moduleSize / 2.0f);
    top += nudge;
    left += nudge;

    // But careful that this does not sample off the edge
    // "right" is the farthest-right valid pixel location -- right+1 is not necessarily
    // This is positive by how much the inner x loop below would be too large
    int nudgedTooFarRight = left + (int) ((matrixWidth - 1) * moduleSize) - right;
    if (nudgedTooFarRight > 0) {
      if (nudgedTooFarRight > nudge) {
        // Neither way fits; abort
        throw NotFoundException_S.getNotFoundInstance();
      }
      left -= nudgedTooFarRight;
    }
    // See logic above
    int nudgedTooFarDown = top + (int) ((matrixHeight - 1) * moduleSize) - bottom;
    if (nudgedTooFarDown > 0) {
      if (nudgedTooFarDown > nudge) {
        // Neither way fits; abort
        throw NotFoundException_S.getNotFoundInstance();
      }
      top -= nudgedTooFarDown;
    }

    // Now just read off the bits
    BitMatrix_S bits = new BitMatrix_S(matrixWidth, matrixHeight);
    for (int y = 0; y < matrixHeight; y++) {
      int iOffset = top + (int) (y * moduleSize);
      for (int x = 0; x < matrixWidth; x++) {
        if (image.get(left + (int) (x * moduleSize), iOffset)) {
          bits.set(x, y);
        }
      }
    }
    return bits;
  }

  private static float moduleSize(int[] leftTopBlack, BitMatrix_S image) throws NotFoundException_S {
    int height = image.getHeight();
    int width = image.getWidth();
    int x = leftTopBlack[0];
    int y = leftTopBlack[1];
    boolean inBlack = true;
    int transitions = 0;
    while (x < width && y < height) {
      if (inBlack != image.get(x, y)) {
        if (++transitions == 5) {
          break;
        }
        inBlack = !inBlack;
      }
      x++;
      y++;
    }
    if (x == width || y == height) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    return (x - leftTopBlack[0]) / 7.0f;
  }

}
