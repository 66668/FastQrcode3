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

package com.google.myzxing.multi;

import com.google.myzxing.BinaryBitmap_S;
import com.google.myzxing.DecodeHintType_S;
import com.google.myzxing.NotFoundException_S;
import com.google.myzxing.Reader_S;
import com.google.myzxing.ReaderException_S;
import com.google.myzxing.Result_S;
import com.google.myzxing.ResultPoint_S;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Attempts to locate multiple barcodes in an image by repeatedly decoding portion of the image.
 * After one barcode is found, the areas left, above, right and below the barcode's
 * {@link ResultPoint_S}s are scanned, recursively.</p>
 *
 * <p>A caller may want to also employ {@link ByQuadrantReader_S} when attempting to find multiple
 * 2D barcodes, like QR Codes, in an image, where the presence of multiple barcodes might prevent
 * detecting any one of them.</p>
 *
 * <p>That is, instead of passing a {@link Reader_S} a caller might pass
 * {@code new ByQuadrantReader(reader)}.</p>
 *
 * @author Sean Owen
 */
public final class GenericMultipleBarcodeReader_S implements MultipleBarcodeReader_S {

  private static final int MIN_DIMENSION_TO_RECUR = 100;
  private static final int MAX_DEPTH = 4;

  static final Result_S[] EMPTY_RESULT_ARRAY = new Result_S[0];

  private final Reader_S delegate;

  public GenericMultipleBarcodeReader_S(Reader_S delegate) {
    this.delegate = delegate;
  }

  @Override
  public Result_S[] decodeMultiple(BinaryBitmap_S image) throws NotFoundException_S {
    return decodeMultiple(image, null);
  }

  @Override
  public Result_S[] decodeMultiple(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S {
    List<Result_S> results = new ArrayList<>();
    doDecodeMultiple(image, hints, results, 0, 0, 0);
    if (results.isEmpty()) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    return results.toArray(EMPTY_RESULT_ARRAY);
  }

  private void doDecodeMultiple(BinaryBitmap_S image,
                                Map<DecodeHintType_S,?> hints,
                                List<Result_S> results,
                                int xOffset,
                                int yOffset,
                                int currentDepth) {
    if (currentDepth > MAX_DEPTH) {
      return;
    }

    Result_S result;
    try {
      result = delegate.decode(image, hints);
    } catch (ReaderException_S ignored) {
      return;
    }
    boolean alreadyFound = false;
    for (Result_S existingResult : results) {
      if (existingResult.getText().equals(result.getText())) {
        alreadyFound = true;
        break;
      }
    }
    if (!alreadyFound) {
      results.add(translateResultPoints(result, xOffset, yOffset));
    }
    ResultPoint_S[] resultPoints = result.getResultPoints();
    if (resultPoints == null || resultPoints.length == 0) {
      return;
    }
    int width = image.getWidth();
    int height = image.getHeight();
    float minX = width;
    float minY = height;
    float maxX = 0.0f;
    float maxY = 0.0f;
    for (ResultPoint_S point : resultPoints) {
      if (point == null) {
        continue;
      }
      float x = point.getX();
      float y = point.getY();
      if (x < minX) {
        minX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y > maxY) {
        maxY = y;
      }
    }

    // Decode left of barcode
    if (minX > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, (int) minX, height),
                       hints, results,
                       xOffset, yOffset,
                       currentDepth + 1);
    }
    // Decode above barcode
    if (minY > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, width, (int) minY),
                       hints, results,
                       xOffset, yOffset,
                       currentDepth + 1);
    }
    // Decode right of barcode
    if (maxX < width - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop((int) maxX, 0, width - (int) maxX, height),
                       hints, results,
                       xOffset + (int) maxX, yOffset,
                       currentDepth + 1);
    }
    // Decode below barcode
    if (maxY < height - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, (int) maxY, width, height - (int) maxY),
                       hints, results,
                       xOffset, yOffset + (int) maxY,
                       currentDepth + 1);
    }
  }

  private static Result_S translateResultPoints(Result_S result, int xOffset, int yOffset) {
    ResultPoint_S[] oldResultPoints = result.getResultPoints();
    if (oldResultPoints == null) {
      return result;
    }
    ResultPoint_S[] newResultPoints = new ResultPoint_S[oldResultPoints.length];
    for (int i = 0; i < oldResultPoints.length; i++) {
      ResultPoint_S oldPoint = oldResultPoints[i];
      if (oldPoint != null) {
        newResultPoints[i] = new ResultPoint_S(oldPoint.getX() + xOffset, oldPoint.getY() + yOffset);
      }
    }
    Result_S newResult = new Result_S(result.getText(),
                                  result.getRawBytes(),
                                  result.getNumBits(),
                                  newResultPoints,
                                  result.getBarcodeFormat(),
                                  result.getTimestamp());
    newResult.putAllMetadata(result.getResultMetadata());
    return newResult;
  }

}
