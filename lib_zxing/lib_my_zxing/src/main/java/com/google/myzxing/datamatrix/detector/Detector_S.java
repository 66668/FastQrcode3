/*
 * Copyright 2008 ZXing authors
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

package com.google.myzxing.datamatrix.detector;


import com.google.myzxing.NotFoundException_S;
import com.google.myzxing.ResultPoint_S;
import com.google.myzxing.common.BitMatrix_S;
import com.google.myzxing.common.DetectorResult_S;
import com.google.myzxing.common.GridSampler_S;
import com.google.myzxing.common.detector.WhiteRectangleDetector_S;

/**
 * <p>Encapsulates logic that can detect a Data Matrix Code in an image, even if the Data Matrix Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author Sean Owen
 */
public final class Detector_S {

  private final BitMatrix_S image;
  private final WhiteRectangleDetector_S rectangleDetector;

  public Detector_S(BitMatrix_S image) throws NotFoundException_S {
    this.image = image;
    rectangleDetector = new WhiteRectangleDetector_S(image);
  }

  /**
   * <p>Detects a Data Matrix Code in an image.</p>
   *
   * @return {@link DetectorResult_S} encapsulating results of detecting a Data Matrix Code
   * @throws NotFoundException_S if no Data Matrix Code can be found
   */
  public DetectorResult_S detect() throws NotFoundException_S {

    ResultPoint_S[] cornerPoints = rectangleDetector.detect();

    ResultPoint_S[] points = detectSolid1(cornerPoints);
    points = detectSolid2(points);
    points[3] = correctTopRight(points);
    if (points[3] == null) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    points = shiftToModuleCenter(points);

    ResultPoint_S topLeft = points[0];
    ResultPoint_S bottomLeft = points[1];
    ResultPoint_S bottomRight = points[2];
    ResultPoint_S topRight = points[3];

    int dimensionTop = transitionsBetween(topLeft, topRight) + 1;
    int dimensionRight = transitionsBetween(bottomRight, topRight) + 1;
    if ((dimensionTop & 0x01) == 1) {
      dimensionTop += 1;
    }
    if ((dimensionRight & 0x01) == 1) {
      dimensionRight += 1;
    }

    if (4 * dimensionTop < 7 * dimensionRight && 4 * dimensionRight < 7 * dimensionTop) {
      // The matrix is square
      dimensionTop = dimensionRight = Math.max(dimensionTop, dimensionRight);
    }

    BitMatrix_S bits = sampleGrid(image,
                                topLeft,
                                bottomLeft,
                                bottomRight,
                                topRight,
                                dimensionTop,
                                dimensionRight);

    return new DetectorResult_S(bits, new ResultPoint_S[]{topLeft, bottomLeft, bottomRight, topRight});
  }

  private ResultPoint_S shiftPoint(ResultPoint_S point, ResultPoint_S to, int div) {
    float x = (to.getX() - point.getX()) / (div + 1);
    float y = (to.getY() - point.getY()) / (div + 1);
    return new ResultPoint_S(point.getX() + x, point.getY() + y);
  }

  private ResultPoint_S moveAway(ResultPoint_S point, float fromX, float fromY) {
    float x = point.getX();
    float y = point.getY();

    if (x < fromX) {
      x -= 1;
    } else {
      x += 1;
    }

    if (y < fromY) {
      y -= 1;
    } else {
      y += 1;
    }

    return new ResultPoint_S(x, y);
  }

  /**
   * Detect a solid side which has minimum transition.
   */
  private ResultPoint_S[] detectSolid1(ResultPoint_S[] cornerPoints) {
    // 0  2
    // 1  3
    ResultPoint_S pointA = cornerPoints[0];
    ResultPoint_S pointB = cornerPoints[1];
    ResultPoint_S pointC = cornerPoints[3];
    ResultPoint_S pointD = cornerPoints[2];

    int trAB = transitionsBetween(pointA, pointB);
    int trBC = transitionsBetween(pointB, pointC);
    int trCD = transitionsBetween(pointC, pointD);
    int trDA = transitionsBetween(pointD, pointA);

    // 0..3
    // :  :
    // 1--2
    int min = trAB;
    ResultPoint_S[] points = {pointD, pointA, pointB, pointC};
    if (min > trBC) {
      min = trBC;
      points[0] = pointA;
      points[1] = pointB;
      points[2] = pointC;
      points[3] = pointD;
    }
    if (min > trCD) {
      min = trCD;
      points[0] = pointB;
      points[1] = pointC;
      points[2] = pointD;
      points[3] = pointA;
    }
    if (min > trDA) {
      points[0] = pointC;
      points[1] = pointD;
      points[2] = pointA;
      points[3] = pointB;
    }

    return points;
  }

  /**
   * Detect a second solid side next to first solid side.
   */
  private ResultPoint_S[] detectSolid2(ResultPoint_S[] points) {
    // A..D
    // :  :
    // B--C
    ResultPoint_S pointA = points[0];
    ResultPoint_S pointB = points[1];
    ResultPoint_S pointC = points[2];
    ResultPoint_S pointD = points[3];

    // Transition detection on the edge is not stable.
    // To safely detect, shift the points to the module center.
    int tr = transitionsBetween(pointA, pointD);
    ResultPoint_S pointBs = shiftPoint(pointB, pointC, (tr + 1) * 4);
    ResultPoint_S pointCs = shiftPoint(pointC, pointB, (tr + 1) * 4);
    int trBA = transitionsBetween(pointBs, pointA);
    int trCD = transitionsBetween(pointCs, pointD);

    // 0..3
    // |  :
    // 1--2
    if (trBA < trCD) {
      // solid sides: A-B-C
      points[0] = pointA;
      points[1] = pointB;
      points[2] = pointC;
      points[3] = pointD;
    } else {
      // solid sides: B-C-D
      points[0] = pointB;
      points[1] = pointC;
      points[2] = pointD;
      points[3] = pointA;
    }

    return points;
  }

  /**
   * Calculates the corner position of the white top right module.
   */
  private ResultPoint_S correctTopRight(ResultPoint_S[] points) {
    // A..D
    // |  :
    // B--C
    ResultPoint_S pointA = points[0];
    ResultPoint_S pointB = points[1];
    ResultPoint_S pointC = points[2];
    ResultPoint_S pointD = points[3];

    // shift points for safe transition detection.
    int trTop = transitionsBetween(pointA, pointD);
    int trRight = transitionsBetween(pointB, pointD);
    ResultPoint_S pointAs = shiftPoint(pointA, pointB, (trRight + 1) * 4);
    ResultPoint_S pointCs = shiftPoint(pointC, pointB, (trTop + 1) * 4);

    trTop = transitionsBetween(pointAs, pointD);
    trRight = transitionsBetween(pointCs, pointD);

    ResultPoint_S candidate1 = new ResultPoint_S(
      pointD.getX() + (pointC.getX() - pointB.getX()) / (trTop + 1),
      pointD.getY() + (pointC.getY() - pointB.getY()) / (trTop + 1));
    ResultPoint_S candidate2 = new ResultPoint_S(
      pointD.getX() + (pointA.getX() - pointB.getX()) / (trRight + 1),
      pointD.getY() + (pointA.getY() - pointB.getY()) / (trRight + 1));

    if (!isValid(candidate1)) {
      if (isValid(candidate2)) {
        return candidate2;
      }
      return null;
    }
    if (!isValid(candidate2)) {
      return candidate1;
    }

    int sumc1 = transitionsBetween(pointAs, candidate1) + transitionsBetween(pointCs, candidate1);
    int sumc2 = transitionsBetween(pointAs, candidate2) + transitionsBetween(pointCs, candidate2);

    if (sumc1 > sumc2) {
      return candidate1;
    } else {
      return candidate2;
    }
  }

  /**
   * Shift the edge points to the module center.
   */
  private ResultPoint_S[] shiftToModuleCenter(ResultPoint_S[] points) {
    // A..D
    // |  :
    // B--C
    ResultPoint_S pointA = points[0];
    ResultPoint_S pointB = points[1];
    ResultPoint_S pointC = points[2];
    ResultPoint_S pointD = points[3];

    // calculate pseudo dimensions
    int dimH = transitionsBetween(pointA, pointD) + 1;
    int dimV = transitionsBetween(pointC, pointD) + 1;

    // shift points for safe dimension detection
    ResultPoint_S pointAs = shiftPoint(pointA, pointB, dimV * 4);
    ResultPoint_S pointCs = shiftPoint(pointC, pointB, dimH * 4);

    //  calculate more precise dimensions
    dimH = transitionsBetween(pointAs, pointD) + 1;
    dimV = transitionsBetween(pointCs, pointD) + 1;
    if ((dimH & 0x01) == 1) {
      dimH += 1;
    }
    if ((dimV & 0x01) == 1) {
      dimV += 1;
    }

    // WhiteRectangleDetector returns points inside of the rectangle.
    // I want points on the edges.
    float centerX = (pointA.getX() + pointB.getX() + pointC.getX() + pointD.getX()) / 4;
    float centerY = (pointA.getY() + pointB.getY() + pointC.getY() + pointD.getY()) / 4;
    pointA = moveAway(pointA, centerX, centerY);
    pointB = moveAway(pointB, centerX, centerY);
    pointC = moveAway(pointC, centerX, centerY);
    pointD = moveAway(pointD, centerX, centerY);

    ResultPoint_S pointBs;
    ResultPoint_S pointDs;

    // shift points to the center of each modules
    pointAs = shiftPoint(pointA, pointB, dimV * 4);
    pointAs = shiftPoint(pointAs, pointD, dimH * 4);
    pointBs = shiftPoint(pointB, pointA, dimV * 4);
    pointBs = shiftPoint(pointBs, pointC, dimH * 4);
    pointCs = shiftPoint(pointC, pointD, dimV * 4);
    pointCs = shiftPoint(pointCs, pointB, dimH * 4);
    pointDs = shiftPoint(pointD, pointC, dimV * 4);
    pointDs = shiftPoint(pointDs, pointA, dimH * 4);

    return new ResultPoint_S[]{pointAs, pointBs, pointCs, pointDs};
  }

  private boolean isValid(ResultPoint_S p) {
    return p.getX() >= 0 && p.getX() < image.getWidth() && p.getY() > 0 && p.getY() < image.getHeight();
  }

  private static BitMatrix_S sampleGrid(BitMatrix_S image,
                                        ResultPoint_S topLeft,
                                        ResultPoint_S bottomLeft,
                                        ResultPoint_S bottomRight,
                                        ResultPoint_S topRight,
                                        int dimensionX,
                                        int dimensionY) throws NotFoundException_S {

    GridSampler_S sampler = GridSampler_S.getInstance();

    return sampler.sampleGrid(image,
                              dimensionX,
                              dimensionY,
                              0.5f,
                              0.5f,
                              dimensionX - 0.5f,
                              0.5f,
                              dimensionX - 0.5f,
                              dimensionY - 0.5f,
                              0.5f,
                              dimensionY - 0.5f,
                              topLeft.getX(),
                              topLeft.getY(),
                              topRight.getX(),
                              topRight.getY(),
                              bottomRight.getX(),
                              bottomRight.getY(),
                              bottomLeft.getX(),
                              bottomLeft.getY());
  }

  /**
   * Counts the number of black/white transitions between two points, using something like Bresenham's algorithm.
   */
  private int transitionsBetween(ResultPoint_S from, ResultPoint_S to) {
    // See QR Code Detector, sizeOfBlackWhiteBlackRun()
    int fromX = (int) from.getX();
    int fromY = (int) from.getY();
    int toX = (int) to.getX();
    int toY = (int) to.getY();
    boolean steep = Math.abs(toY - fromY) > Math.abs(toX - fromX);
    if (steep) {
      int temp = fromX;
      fromX = fromY;
      fromY = temp;
      temp = toX;
      toX = toY;
      toY = temp;
    }

    int dx = Math.abs(toX - fromX);
    int dy = Math.abs(toY - fromY);
    int error = -dx / 2;
    int ystep = fromY < toY ? 1 : -1;
    int xstep = fromX < toX ? 1 : -1;
    int transitions = 0;
    boolean inBlack = image.get(steep ? fromY : fromX, steep ? fromX : fromY);
    for (int x = fromX, y = fromY; x != toX; x += xstep) {
      boolean isBlack = image.get(steep ? y : x, steep ? x : y);
      if (isBlack != inBlack) {
        transitions++;
        inBlack = isBlack;
      }
      error += dy;
      if (error > 0) {
        if (y == toY) {
          break;
        }
        y += ystep;
        error -= dx;
      }
    }
    return transitions;
  }

}
