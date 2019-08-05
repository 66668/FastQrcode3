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

package com.google.myzxing2.common;


import com.google.myzxing2.ResultPoint_S;

/**
 * <p>Encapsulates the result of detecting a barcode in an image. This includes the raw
 * matrix of black/white pixels corresponding to the barcode, and possibly points of interest
 * in the image, like the location of finder patterns or corners of the barcode in the image.</p>
 *
 * @author Sean Owen
 */
public class DetectorResult_S {

  private final BitMatrix_S bits;
  private final ResultPoint_S[] points;

  public DetectorResult_S(BitMatrix_S bits, ResultPoint_S[] points) {
    this.bits = bits;
    this.points = points;
  }

  public final BitMatrix_S getBits() {
    return bits;
  }

  public final ResultPoint_S[] getPoints() {
    return points;
  }

}