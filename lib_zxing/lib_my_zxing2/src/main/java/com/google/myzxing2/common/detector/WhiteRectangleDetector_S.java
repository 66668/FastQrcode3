/*
 * Copyright 2010 ZXing authors
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

package com.google.myzxing2.common.detector;


import com.google.myzxing2.NotFoundException_S;
import com.google.myzxing2.ResultPoint_S;
import com.google.myzxing2.common.BitMatrix_S;

/**
 * <p>
 * Detects a candidate barcode-like rectangular region within an image. It
 * starts around the center of the image, increases the size of the candidate
 * region until it finds a white rectangular region. By keeping track of the
 * last black points it encountered, it determines the corners of the barcode.
 * </p>
 *
 * @author David Olivier
 */
public final class WhiteRectangleDetector_S {

  private static final int INIT_SIZE = 10;
  private static final int CORR = 1;

  private final BitMatrix_S image;
  private final int height;
  private final int width;
  private final int leftInit;
  private final int rightInit;
  private final int downInit;
  private final int upInit;

  public WhiteRectangleDetector_S(BitMatrix_S image) throws NotFoundException_S {
    this(image, INIT_SIZE, image.getWidth() / 2, image.getHeight() / 2);
  }

  /**
   * @param image barcode image to find a rectangle in
   * @param initSize initial size of search area around center
   * @param x x position of search center
   * @param y y position of search center
   * @throws NotFoundException_S if image is too small to accommodate {@code initSize}
   */
  public WhiteRectangleDetector_S(BitMatrix_S image, int initSize, int x, int y) throws NotFoundException_S {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    int halfsize = initSize / 2;
    leftInit = x - halfsize;
    rightInit = x + halfsize;
    upInit = y - halfsize;
    downInit = y + halfsize;
    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width) {
      throw NotFoundException_S.getNotFoundInstance();
    }
  }

  /**
   * <p>
   * Detects a candidate barcode-like rectangular region within an image. It
   * starts around the center of the image, increases the size of the candidate
   * region until it finds a white rectangular region.
   * </p>
   *
   * @return {@link ResultPoint_S}[] describing the corners of the rectangular
   *         region. The first and last points are opposed on the diagonal, as
   *         are the second and third. The first point will be the topmost
   *         point and the last, the bottommost. The second point will be
   *         leftmost and the third, the rightmost
   * @throws NotFoundException_S if no Data Matrix Code can be found
   */
  public ResultPoint_S[] detect() throws NotFoundException_S {

    int left = leftInit;
    int right = rightInit;
    int up = upInit;
    int down = downInit;
    boolean sizeExceeded = false;
    boolean aBlackPointFoundOnBorder = true;

    boolean atLeastOneBlackPointFoundOnRight = false;
    boolean atLeastOneBlackPointFoundOnBottom = false;
    boolean atLeastOneBlackPointFoundOnLeft = false;
    boolean atLeastOneBlackPointFoundOnTop = false;

    while (aBlackPointFoundOnBorder) {

      aBlackPointFoundOnBorder = false;

      // .....
      // .   |
      // .....
      boolean rightBorderNotWhite = true;
      while ((rightBorderNotWhite || !atLeastOneBlackPointFoundOnRight) && right < width) {
        rightBorderNotWhite = containsBlackPoint(up, down, right, false);
        if (rightBorderNotWhite) {
          right++;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnRight = true;
        } else if (!atLeastOneBlackPointFoundOnRight) {
          right++;
        }
      }

      if (right >= width) {
        sizeExceeded = true;
        break;
      }

      // .....
      // .   .
      // .___.
      boolean bottomBorderNotWhite = true;
      while ((bottomBorderNotWhite || !atLeastOneBlackPointFoundOnBottom) && down < height) {
        bottomBorderNotWhite = containsBlackPoint(left, right, down, true);
        if (bottomBorderNotWhite) {
          down++;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnBottom = true;
        } else if (!atLeastOneBlackPointFoundOnBottom) {
          down++;
        }
      }

      if (down >= height) {
        sizeExceeded = true;
        break;
      }

      // .....
      // |   .
      // .....
      boolean leftBorderNotWhite = true;
      while ((leftBorderNotWhite || !atLeastOneBlackPointFoundOnLeft) && left >= 0) {
        leftBorderNotWhite = containsBlackPoint(up, down, left, false);
        if (leftBorderNotWhite) {
          left--;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnLeft = true;
        } else if (!atLeastOneBlackPointFoundOnLeft) {
          left--;
        }
      }

      if (left < 0) {
        sizeExceeded = true;
        break;
      }

      // .___.
      // .   .
      // .....
      boolean topBorderNotWhite = true;
      while ((topBorderNotWhite || !atLeastOneBlackPointFoundOnTop) && up >= 0) {
        topBorderNotWhite = containsBlackPoint(left, right, up, true);
        if (topBorderNotWhite) {
          up--;
          aBlackPointFoundOnBorder = true;
          atLeastOneBlackPointFoundOnTop = true;
        } else if (!atLeastOneBlackPointFoundOnTop) {
          up--;
        }
      }

      if (up < 0) {
        sizeExceeded = true;
        break;
      }

    }

    if (!sizeExceeded) {

      int maxSize = right - left;

      ResultPoint_S z = null;
      for (int i = 1; z == null && i < maxSize; i++) {
        z = getBlackPointOnSegment(left, down - i, left + i, down);
      }

      if (z == null) {
        throw NotFoundException_S.getNotFoundInstance();
      }

      ResultPoint_S t = null;
      //go down right
      for (int i = 1; t == null && i < maxSize; i++) {
        t = getBlackPointOnSegment(left, up + i, left + i, up);
      }

      if (t == null) {
        throw NotFoundException_S.getNotFoundInstance();
      }

      ResultPoint_S x = null;
      //go down left
      for (int i = 1; x == null && i < maxSize; i++) {
        x = getBlackPointOnSegment(right, up + i, right - i, up);
      }

      if (x == null) {
        throw NotFoundException_S.getNotFoundInstance();
      }

      ResultPoint_S y = null;
      //go up left
      for (int i = 1; y == null && i < maxSize; i++) {
        y = getBlackPointOnSegment(right, down - i, right - i, down);
      }

      if (y == null) {
        throw NotFoundException_S.getNotFoundInstance();
      }

      return centerEdges(y, z, x, t);

    } else {
      throw NotFoundException_S.getNotFoundInstance();
    }
  }

  private ResultPoint_S getBlackPointOnSegment(float aX, float aY, float bX, float bY) {
    int dist = MathUtils_S.round(MathUtils_S.distance(aX, aY, bX, bY));
    float xStep = (bX - aX) / dist;
    float yStep = (bY - aY) / dist;

    for (int i = 0; i < dist; i++) {
      int x = MathUtils_S.round(aX + i * xStep);
      int y = MathUtils_S.round(aY + i * yStep);
      if (image.get(x, y)) {
        return new ResultPoint_S(x, y);
      }
    }
    return null;
  }

  /**
   * recenters the points of a constant distance towards the center
   *
   * @param y bottom most point
   * @param z left most point
   * @param x right most point
   * @param t top most point
   * @return {@link ResultPoint_S}[] describing the corners of the rectangular
   *         region. The first and last points are opposed on the diagonal, as
   *         are the second and third. The first point will be the topmost
   *         point and the last, the bottommost. The second point will be
   *         leftmost and the third, the rightmost
   */
  private ResultPoint_S[] centerEdges(ResultPoint_S y, ResultPoint_S z,
                                      ResultPoint_S x, ResultPoint_S t) {

    //
    //       t            t
    //  z                      x
    //        x    OR    z
    //   y                    y
    //

    float yi = y.getX();
    float yj = y.getY();
    float zi = z.getX();
    float zj = z.getY();
    float xi = x.getX();
    float xj = x.getY();
    float ti = t.getX();
    float tj = t.getY();

    if (yi < width / 2.0f) {
      return new ResultPoint_S[]{
          new ResultPoint_S(ti - CORR, tj + CORR),
          new ResultPoint_S(zi + CORR, zj + CORR),
          new ResultPoint_S(xi - CORR, xj - CORR),
          new ResultPoint_S(yi + CORR, yj - CORR)};
    } else {
      return new ResultPoint_S[]{
          new ResultPoint_S(ti + CORR, tj + CORR),
          new ResultPoint_S(zi + CORR, zj - CORR),
          new ResultPoint_S(xi - CORR, xj + CORR),
          new ResultPoint_S(yi - CORR, yj - CORR)};
    }
  }

  /**
   * Determines whether a segment contains a black point
   *
   * @param a          min value of the scanned coordinate
   * @param b          max value of the scanned coordinate
   * @param fixed      value of fixed coordinate
   * @param horizontal set to true if scan must be horizontal, false if vertical
   * @return true if a black point has been found, else false.
   */
  private boolean containsBlackPoint(int a, int b, int fixed, boolean horizontal) {

    if (horizontal) {
      for (int x = a; x <= b; x++) {
        if (image.get(x, fixed)) {
          return true;
        }
      }
    } else {
      for (int y = a; y <= b; y++) {
        if (image.get(fixed, y)) {
          return true;
        }
      }
    }

    return false;
  }

}
