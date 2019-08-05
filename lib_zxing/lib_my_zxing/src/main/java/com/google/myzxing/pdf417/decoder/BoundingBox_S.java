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

package com.google.myzxing.pdf417.decoder;

import com.google.myzxing.NotFoundException_S;
import com.google.myzxing.ResultPoint_S;
import com.google.myzxing.common.BitMatrix_S;

/**
 * @author Guenther Grau
 */
final class BoundingBox_S {

  private final BitMatrix_S image;
  private final ResultPoint_S topLeft;
  private final ResultPoint_S bottomLeft;
  private final ResultPoint_S topRight;
  private final ResultPoint_S bottomRight;
  private final int minX;
  private final int maxX;
  private final int minY;
  private final int maxY;

  BoundingBox_S(BitMatrix_S image,
                ResultPoint_S topLeft,
                ResultPoint_S bottomLeft,
                ResultPoint_S topRight,
                ResultPoint_S bottomRight) throws NotFoundException_S {
    boolean leftUnspecified = topLeft == null || bottomLeft == null;
    boolean rightUnspecified = topRight == null || bottomRight == null;
    if (leftUnspecified && rightUnspecified) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    if (leftUnspecified) {
      topLeft = new ResultPoint_S(0, topRight.getY());
      bottomLeft = new ResultPoint_S(0, bottomRight.getY());
    } else if (rightUnspecified) {
      topRight = new ResultPoint_S(image.getWidth() - 1, topLeft.getY());
      bottomRight = new ResultPoint_S(image.getWidth() - 1, bottomLeft.getY());
    }
    this.image = image;
    this.topLeft = topLeft;
    this.bottomLeft = bottomLeft;
    this.topRight = topRight;
    this.bottomRight = bottomRight;
    this.minX = (int) Math.min(topLeft.getX(), bottomLeft.getX());
    this.maxX = (int) Math.max(topRight.getX(), bottomRight.getX());
    this.minY = (int) Math.min(topLeft.getY(), topRight.getY());
    this.maxY = (int) Math.max(bottomLeft.getY(), bottomRight.getY());
  }

  BoundingBox_S(BoundingBox_S boundingBox) {
    this.image = boundingBox.image;
    this.topLeft = boundingBox.getTopLeft();
    this.bottomLeft = boundingBox.getBottomLeft();
    this.topRight = boundingBox.getTopRight();
    this.bottomRight = boundingBox.getBottomRight();
    this.minX = boundingBox.getMinX();
    this.maxX = boundingBox.getMaxX();
    this.minY = boundingBox.getMinY();
    this.maxY = boundingBox.getMaxY();
  }

  static BoundingBox_S merge(BoundingBox_S leftBox, BoundingBox_S rightBox) throws NotFoundException_S {
    if (leftBox == null) {
      return rightBox;
    }
    if (rightBox == null) {
      return leftBox;
    }
    return new BoundingBox_S(leftBox.image, leftBox.topLeft, leftBox.bottomLeft, rightBox.topRight, rightBox.bottomRight);
  }

  BoundingBox_S addMissingRows(int missingStartRows, int missingEndRows, boolean isLeft) throws NotFoundException_S {
    ResultPoint_S newTopLeft = topLeft;
    ResultPoint_S newBottomLeft = bottomLeft;
    ResultPoint_S newTopRight = topRight;
    ResultPoint_S newBottomRight = bottomRight;

    if (missingStartRows > 0) {
      ResultPoint_S top = isLeft ? topLeft : topRight;
      int newMinY = (int) top.getY() - missingStartRows;
      if (newMinY < 0) {
        newMinY = 0;
      }
      ResultPoint_S newTop = new ResultPoint_S(top.getX(), newMinY);
      if (isLeft) {
        newTopLeft = newTop;
      } else {
        newTopRight = newTop;
      }
    }

    if (missingEndRows > 0) {
      ResultPoint_S bottom = isLeft ? bottomLeft : bottomRight;
      int newMaxY = (int) bottom.getY() + missingEndRows;
      if (newMaxY >= image.getHeight()) {
        newMaxY = image.getHeight() - 1;
      }
      ResultPoint_S newBottom = new ResultPoint_S(bottom.getX(), newMaxY);
      if (isLeft) {
        newBottomLeft = newBottom;
      } else {
        newBottomRight = newBottom;
      }
    }

    return new BoundingBox_S(image, newTopLeft, newBottomLeft, newTopRight, newBottomRight);
  }

  int getMinX() {
    return minX;
  }

  int getMaxX() {
    return maxX;
  }

  int getMinY() {
    return minY;
  }

  int getMaxY() {
    return maxY;
  }

  ResultPoint_S getTopLeft() {
    return topLeft;
  }

  ResultPoint_S getTopRight() {
    return topRight;
  }

  ResultPoint_S getBottomLeft() {
    return bottomLeft;
  }

  ResultPoint_S getBottomRight() {
    return bottomRight;
  }

}
