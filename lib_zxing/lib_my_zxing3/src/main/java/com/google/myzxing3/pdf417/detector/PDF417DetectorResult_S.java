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

package com.google.myzxing3.pdf417.detector;

import com.google.myzxing3.ResultPoint_S;
import com.google.myzxing3.common.BitMatrix_S;

import java.util.List;

/**
 * @author Guenther Grau
 */
public final class PDF417DetectorResult_S {

  private final BitMatrix_S bits;
  private final List<ResultPoint_S[]> points;

  public PDF417DetectorResult_S(BitMatrix_S bits, List<ResultPoint_S[]> points) {
    this.bits = bits;
    this.points = points;
  }

  public BitMatrix_S getBits() {
    return bits;
  }

  public List<ResultPoint_S[]> getPoints() {
    return points;
  }

}
