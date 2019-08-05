/*
 * Copyright 2013 ZXing authors
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

package com.google.myzxing2.pdf417.decoder;

import java.util.Formatter;

/**
 * @author Guenther Grau
 */
class DetectionResultColumn_S {

  private static final int MAX_NEARBY_DISTANCE = 5;

  private final BoundingBox_S boundingBox;
  private final Codeword_S[] codewords;

  DetectionResultColumn_S(BoundingBox_S boundingBox) {
    this.boundingBox = new BoundingBox_S(boundingBox);
    codewords = new Codeword_S[boundingBox.getMaxY() - boundingBox.getMinY() + 1];
  }

  final Codeword_S getCodewordNearby(int imageRow) {
    Codeword_S codeword = getCodeword(imageRow);
    if (codeword != null) {
      return codeword;
    }
    for (int i = 1; i < MAX_NEARBY_DISTANCE; i++) {
      int nearImageRow = imageRowToCodewordIndex(imageRow) - i;
      if (nearImageRow >= 0) {
        codeword = codewords[nearImageRow];
        if (codeword != null) {
          return codeword;
        }
      }
      nearImageRow = imageRowToCodewordIndex(imageRow) + i;
      if (nearImageRow < codewords.length) {
        codeword = codewords[nearImageRow];
        if (codeword != null) {
          return codeword;
        }
      }
    }
    return null;
  }

  final int imageRowToCodewordIndex(int imageRow) {
    return imageRow - boundingBox.getMinY();
  }

  final void setCodeword(int imageRow, Codeword_S codeword) {
    codewords[imageRowToCodewordIndex(imageRow)] = codeword;
  }

  final Codeword_S getCodeword(int imageRow) {
    return codewords[imageRowToCodewordIndex(imageRow)];
  }

  final BoundingBox_S getBoundingBox() {
    return boundingBox;
  }

  final Codeword_S[] getCodewords() {
    return codewords;
  }

  @Override
  public String toString() {
    try (Formatter formatter = new Formatter()) {
      int row = 0;
      for (Codeword_S codeword : codewords) {
        if (codeword == null) {
          formatter.format("%3d:    |   %n", row++);
          continue;
        }
        formatter.format("%3d: %3d|%3d%n", row++, codeword.getRowNumber(), codeword.getValue());
      }
      return formatter.toString();
    }
  }

}
