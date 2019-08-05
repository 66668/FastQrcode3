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

package com.google.myzxing2.multi;

import com.google.myzxing2.BinaryBitmap_S;
import com.google.myzxing2.ChecksumException_S;
import com.google.myzxing2.DecodeHintType_S;
import com.google.myzxing2.FormatException_S;
import com.google.myzxing2.NotFoundException_S;
import com.google.myzxing2.Reader_S;
import com.google.myzxing2.ResultPoint_S;
import com.google.myzxing2.Result_S;

import java.util.Map;

/**
 * This class attempts to decode a barcode from an image, not by scanning the whole image,
 * but by scanning subsets of the image. This is important when there may be multiple barcodes in
 * an image, and detecting a barcode may find parts of multiple barcode and fail to decode
 * (e.g. QR Codes). Instead this scans the four quadrants of the image -- and also the center
 * 'quadrant' to cover the case where a barcode is found in the center.
 *
 * @see GenericMultipleBarcodeReader_S
 */
public final class ByQuadrantReader_S implements Reader_S {

  private final Reader_S delegate;

  public ByQuadrantReader_S(Reader_S delegate) {
    this.delegate = delegate;
  }

  @Override
  public Result_S decode(BinaryBitmap_S image)
      throws NotFoundException_S, ChecksumException_S, FormatException_S {
    return decode(image, null);
  }

  @Override
  public Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, ChecksumException_S, FormatException_S {

    int width = image.getWidth();
    int height = image.getHeight();
    int halfWidth = width / 2;
    int halfHeight = height / 2;

    try {
      // No need to call makeAbsolute as results will be relative to original top left here
      return delegate.decode(image.crop(0, 0, halfWidth, halfHeight), hints);
    } catch (NotFoundException_S re) {
      // continue
    }

    try {
      Result_S result = delegate.decode(image.crop(halfWidth, 0, halfWidth, halfHeight), hints);
      makeAbsolute(result.getResultPoints(), halfWidth, 0);
      return result;
    } catch (NotFoundException_S re) {
      // continue
    }

    try {
      Result_S result = delegate.decode(image.crop(0, halfHeight, halfWidth, halfHeight), hints);
      makeAbsolute(result.getResultPoints(), 0, halfHeight);
      return result;
    } catch (NotFoundException_S re) {
      // continue
    }

    try {
      Result_S result = delegate.decode(image.crop(halfWidth, halfHeight, halfWidth, halfHeight), hints);
      makeAbsolute(result.getResultPoints(), halfWidth, halfHeight);
      return result;
    } catch (NotFoundException_S re) {
      // continue
    }

    int quarterWidth = halfWidth / 2;
    int quarterHeight = halfHeight / 2;
    BinaryBitmap_S center = image.crop(quarterWidth, quarterHeight, halfWidth, halfHeight);
    Result_S result = delegate.decode(center, hints);
    makeAbsolute(result.getResultPoints(), quarterWidth, quarterHeight);
    return result;
  }

  @Override
  public void reset() {
    delegate.reset();
  }

  private static void makeAbsolute(ResultPoint_S[] points, int leftOffset, int topOffset) {
    if (points != null) {
      for (int i = 0; i < points.length; i++) {
        ResultPoint_S relative = points[i];
        if (relative != null) {
          points[i] = new ResultPoint_S(relative.getX() + leftOffset, relative.getY() + topOffset);
        }    
      }
    }
  }

}
