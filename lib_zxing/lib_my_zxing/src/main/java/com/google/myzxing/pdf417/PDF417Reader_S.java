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

package com.google.myzxing.pdf417;

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
import com.google.myzxing.common.DecoderResult_S;
import com.google.myzxing.multi.MultipleBarcodeReader_S;
import com.google.myzxing.pdf417.decoder.PDF417ScanningDecoder_S;
import com.google.myzxing.pdf417.detector.Detector_S;
import com.google.myzxing.pdf417.detector.PDF417DetectorResult_S;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This implementation can detect and decode PDF417 codes in an image.
 *
 * @author Guenther Grau
 */
public final class PDF417Reader_S implements Reader_S, MultipleBarcodeReader_S {

  private static final Result_S[] EMPTY_RESULT_ARRAY = new Result_S[0];

  /**
   * Locates and decodes a PDF417 code in an image.
   *
   * @return a String representing the content encoded by the PDF417 code
   * @throws NotFoundException_S if a PDF417 code cannot be found,
   * @throws FormatException_S if a PDF417 cannot be decoded
   */
  @Override
  public Result_S decode(BinaryBitmap_S image) throws NotFoundException_S, FormatException_S, ChecksumException_S {
    return decode(image, null);
  }

  @Override
  public Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints) throws NotFoundException_S, FormatException_S,
          ChecksumException_S {
    Result_S[] result = decode(image, hints, false);
    if (result == null || result.length == 0 || result[0] == null) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    return result[0];
  }

  @Override
  public Result_S[] decodeMultiple(BinaryBitmap_S image) throws NotFoundException_S {
    return decodeMultiple(image, null);
  }

  @Override
  public Result_S[] decodeMultiple(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints) throws NotFoundException_S {
    try {
      return decode(image, hints, true);
    } catch (FormatException_S | ChecksumException_S ignored) {
      throw NotFoundException_S.getNotFoundInstance();
    }
  }

  private static Result_S[] decode(BinaryBitmap_S image, Map<DecodeHintType_S, ?> hints, boolean multiple)
      throws NotFoundException_S, FormatException_S, ChecksumException_S {
    List<Result_S> results = new ArrayList<>();
    PDF417DetectorResult_S detectorResult = Detector_S.detect(image, hints, multiple);
    for (ResultPoint_S[] points : detectorResult.getPoints()) {
      DecoderResult_S decoderResult = PDF417ScanningDecoder_S.decode(detectorResult.getBits(), points[4], points[5],
          points[6], points[7], getMinCodewordWidth(points), getMaxCodewordWidth(points));
      Result_S result = new Result_S(decoderResult.getText(), decoderResult.getRawBytes(), points, BarcodeFormat_S.PDF_417);
      result.putMetadata(ResultMetadataType_S.ERROR_CORRECTION_LEVEL, decoderResult.getECLevel());
      PDF417ResultMetadata_S pdf417ResultMetadata = (PDF417ResultMetadata_S) decoderResult.getOther();
      if (pdf417ResultMetadata != null) {
        result.putMetadata(ResultMetadataType_S.PDF417_EXTRA_METADATA, pdf417ResultMetadata);
      }
      results.add(result);
    }
    return results.toArray(EMPTY_RESULT_ARRAY);
  }

  private static int getMaxWidth(ResultPoint_S p1, ResultPoint_S p2) {
    if (p1 == null || p2 == null) {
      return 0;
    }
    return (int) Math.abs(p1.getX() - p2.getX());
  }

  private static int getMinWidth(ResultPoint_S p1, ResultPoint_S p2) {
    if (p1 == null || p2 == null) {
      return Integer.MAX_VALUE;
    }
    return (int) Math.abs(p1.getX() - p2.getX());
  }

  private static int getMaxCodewordWidth(ResultPoint_S[] p) {
    return Math.max(
        Math.max(getMaxWidth(p[0], p[4]), getMaxWidth(p[6], p[2]) * PDF417Common_S.MODULES_IN_CODEWORD /
            PDF417Common_S.MODULES_IN_STOP_PATTERN),
        Math.max(getMaxWidth(p[1], p[5]), getMaxWidth(p[7], p[3]) * PDF417Common_S.MODULES_IN_CODEWORD /
            PDF417Common_S.MODULES_IN_STOP_PATTERN));
  }

  private static int getMinCodewordWidth(ResultPoint_S[] p) {
    return Math.min(
        Math.min(getMinWidth(p[0], p[4]), getMinWidth(p[6], p[2]) * PDF417Common_S.MODULES_IN_CODEWORD /
            PDF417Common_S.MODULES_IN_STOP_PATTERN),
        Math.min(getMinWidth(p[1], p[5]), getMinWidth(p[7], p[3]) * PDF417Common_S.MODULES_IN_CODEWORD /
            PDF417Common_S.MODULES_IN_STOP_PATTERN));
  }

  @Override
  public void reset() {
    // nothing needs to be reset
  }

}
