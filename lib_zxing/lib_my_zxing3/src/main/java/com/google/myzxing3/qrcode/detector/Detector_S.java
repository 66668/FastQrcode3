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

package com.google.myzxing3.qrcode.detector;

import com.google.myzxing3.DecodeHintType_S;
import com.google.myzxing3.FormatException_S;
import com.google.myzxing3.NotFoundException_S;
import com.google.myzxing3.ResultPoint_S;
import com.google.myzxing3.ResultPointCallback_S;
import com.google.myzxing3.common.BitMatrix_S;
import com.google.myzxing3.common.DetectorResult_S;
import com.google.myzxing3.common.GridSampler_S;
import com.google.myzxing3.common.PerspectiveTransform_S;
import com.google.myzxing3.common.detector.MathUtils_S;
import com.google.myzxing3.qrcode.decoder.Version_S;

import java.util.Map;

/**
 * <p>Encapsulates logic that can detect a QR Code in an image, even if the QR Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author Sean Owen
 */
public class Detector_S {

  private final BitMatrix_S image;
  private ResultPointCallback_S resultPointCallback;

  public Detector_S(BitMatrix_S image) {
    this.image = image;
  }

  protected final BitMatrix_S getImage() {
    return image;
  }

  protected final ResultPointCallback_S getResultPointCallback() {
    return resultPointCallback;
  }

  /**
   * <p>Detects a QR Code in an image.</p>
   *
   * @return {@link DetectorResult_S} encapsulating results of detecting a QR Code
   * @throws NotFoundException_S if QR Code cannot be found
   * @throws FormatException_S if a QR Code cannot be decoded
   */
  public DetectorResult_S detect() throws NotFoundException_S, FormatException_S {
    return detect(null);
  }

  /**
   * <p>Detects a QR Code in an image.</p>
   *
   * @param hints optional hints to detector
   * @return {@link DetectorResult_S} encapsulating results of detecting a QR Code
   * @throws NotFoundException_S if QR Code cannot be found
   * @throws FormatException_S if a QR Code cannot be decoded
   */
  public final DetectorResult_S detect(Map<DecodeHintType_S,?> hints) throws NotFoundException_S, FormatException_S {

    resultPointCallback = hints == null ? null :
        (ResultPointCallback_S) hints.get(DecodeHintType_S.NEED_RESULT_POINT_CALLBACK);

    com.google.myzxing3.qrcode.detector.FinderPatternFinder_S finder = new com.google.myzxing3.qrcode.detector.FinderPatternFinder_S(image, resultPointCallback);
    com.google.myzxing3.qrcode.detector.FinderPatternInfo_S info = finder.find(hints);

    return processFinderPatternInfo(info);
  }

  protected final DetectorResult_S processFinderPatternInfo(com.google.myzxing3.qrcode.detector.FinderPatternInfo_S info)
      throws NotFoundException_S, FormatException_S {

    com.google.myzxing3.qrcode.detector.FinderPattern_S topLeft = info.getTopLeft();
    com.google.myzxing3.qrcode.detector.FinderPattern_S topRight = info.getTopRight();
    com.google.myzxing3.qrcode.detector.FinderPattern_S bottomLeft = info.getBottomLeft();

    float moduleSize = calculateModuleSize(topLeft, topRight, bottomLeft);
    if (moduleSize < 1.0f) {
      throw NotFoundException_S.getNotFoundInstance();
    }
    int dimension = computeDimension(topLeft, topRight, bottomLeft, moduleSize);
    Version_S provisionalVersion = Version_S.getProvisionalVersionForDimension(dimension);
    int modulesBetweenFPCenters = provisionalVersion.getDimensionForVersion() - 7;

    AlignmentPattern_S alignmentPattern = null;
    // Anything above version 1 has an alignment pattern
    if (provisionalVersion.getAlignmentPatternCenters().length > 0) {

      // Guess where a "bottom right" finder pattern would have been
      float bottomRightX = topRight.getX() - topLeft.getX() + bottomLeft.getX();
      float bottomRightY = topRight.getY() - topLeft.getY() + bottomLeft.getY();

      // Estimate that alignment pattern is closer by 3 modules
      // from "bottom right" to known top left location
      float correctionToTopLeft = 1.0f - 3.0f / modulesBetweenFPCenters;
      int estAlignmentX = (int) (topLeft.getX() + correctionToTopLeft * (bottomRightX - topLeft.getX()));
      int estAlignmentY = (int) (topLeft.getY() + correctionToTopLeft * (bottomRightY - topLeft.getY()));

      // Kind of arbitrary -- expand search radius before giving up
      for (int i = 4; i <= 16; i <<= 1) {
        try {
          alignmentPattern = findAlignmentInRegion(moduleSize,
              estAlignmentX,
              estAlignmentY,
              i);
          break;
        } catch (NotFoundException_S re) {
          // try next round
        }
      }
      // If we didn't find alignment pattern... well try anyway without it
    }

    PerspectiveTransform_S transform =
        createTransform(topLeft, topRight, bottomLeft, alignmentPattern, dimension);

    BitMatrix_S bits = sampleGrid(image, transform, dimension);

    ResultPoint_S[] points;
    if (alignmentPattern == null) {
      points = new ResultPoint_S[]{bottomLeft, topLeft, topRight};
    } else {
      points = new ResultPoint_S[]{bottomLeft, topLeft, topRight, alignmentPattern};
    }
    return new DetectorResult_S(bits, points);
  }

  private static PerspectiveTransform_S createTransform(ResultPoint_S topLeft,
                                                        ResultPoint_S topRight,
                                                        ResultPoint_S bottomLeft,
                                                        ResultPoint_S alignmentPattern,
                                                        int dimension) {
    float dimMinusThree = dimension - 3.5f;
    float bottomRightX;
    float bottomRightY;
    float sourceBottomRightX;
    float sourceBottomRightY;
    if (alignmentPattern != null) {
      bottomRightX = alignmentPattern.getX();
      bottomRightY = alignmentPattern.getY();
      sourceBottomRightX = dimMinusThree - 3.0f;
      sourceBottomRightY = sourceBottomRightX;
    } else {
      // Don't have an alignment pattern, just make up the bottom-right point
      bottomRightX = (topRight.getX() - topLeft.getX()) + bottomLeft.getX();
      bottomRightY = (topRight.getY() - topLeft.getY()) + bottomLeft.getY();
      sourceBottomRightX = dimMinusThree;
      sourceBottomRightY = dimMinusThree;
    }

    return PerspectiveTransform_S.quadrilateralToQuadrilateral(
        3.5f,
        3.5f,
        dimMinusThree,
        3.5f,
        sourceBottomRightX,
        sourceBottomRightY,
        3.5f,
        dimMinusThree,
        topLeft.getX(),
        topLeft.getY(),
        topRight.getX(),
        topRight.getY(),
        bottomRightX,
        bottomRightY,
        bottomLeft.getX(),
        bottomLeft.getY());
  }

  private static BitMatrix_S sampleGrid(BitMatrix_S image,
                                        PerspectiveTransform_S transform,
                                        int dimension) throws NotFoundException_S {

    GridSampler_S sampler = GridSampler_S.getInstance();
    return sampler.sampleGrid(image, dimension, dimension, transform);
  }

  /**
   * <p>Computes the dimension (number of modules on a size) of the QR Code based on the position
   * of the finder patterns and estimated module size.</p>
   */
  private static int computeDimension(ResultPoint_S topLeft,
                                      ResultPoint_S topRight,
                                      ResultPoint_S bottomLeft,
                                      float moduleSize) throws NotFoundException_S {
    int tltrCentersDimension = MathUtils_S.round(ResultPoint_S.distance(topLeft, topRight) / moduleSize);
    int tlblCentersDimension = MathUtils_S.round(ResultPoint_S.distance(topLeft, bottomLeft) / moduleSize);
    int dimension = ((tltrCentersDimension + tlblCentersDimension) / 2) + 7;
    switch (dimension & 0x03) { // mod 4
      case 0:
        dimension++;
        break;
        // 1? do nothing
      case 2:
        dimension--;
        break;
      case 3:
        throw NotFoundException_S.getNotFoundInstance();
    }
    return dimension;
  }

  /**
   * <p>Computes an average estimated module size based on estimated derived from the positions
   * of the three finder patterns.</p>
   *
   * @param topLeft detected top-left finder pattern center
   * @param topRight detected top-right finder pattern center
   * @param bottomLeft detected bottom-left finder pattern center
   * @return estimated module size
   */
  protected final float calculateModuleSize(ResultPoint_S topLeft,
                                            ResultPoint_S topRight,
                                            ResultPoint_S bottomLeft) {
    // Take the average
    return (calculateModuleSizeOneWay(topLeft, topRight) +
        calculateModuleSizeOneWay(topLeft, bottomLeft)) / 2.0f;
  }

  /**
   * <p>Estimates module size based on two finder patterns -- it uses
   * {@link #sizeOfBlackWhiteBlackRunBothWays(int, int, int, int)} to figure the
   * width of each, measuring along the axis between their centers.</p>
   */
  private float calculateModuleSizeOneWay(ResultPoint_S pattern, ResultPoint_S otherPattern) {
    float moduleSizeEst1 = sizeOfBlackWhiteBlackRunBothWays((int) pattern.getX(),
        (int) pattern.getY(),
        (int) otherPattern.getX(),
        (int) otherPattern.getY());
    float moduleSizeEst2 = sizeOfBlackWhiteBlackRunBothWays((int) otherPattern.getX(),
        (int) otherPattern.getY(),
        (int) pattern.getX(),
        (int) pattern.getY());
    if (Float.isNaN(moduleSizeEst1)) {
      return moduleSizeEst2 / 7.0f;
    }
    if (Float.isNaN(moduleSizeEst2)) {
      return moduleSizeEst1 / 7.0f;
    }
    // Average them, and divide by 7 since we've counted the width of 3 black modules,
    // and 1 white and 1 black module on either side. Ergo, divide sum by 14.
    return (moduleSizeEst1 + moduleSizeEst2) / 14.0f;
  }

  /**
   * See {@link #sizeOfBlackWhiteBlackRun(int, int, int, int)}; computes the total width of
   * a finder pattern by looking for a black-white-black run from the center in the direction
   * of another point (another finder pattern center), and in the opposite direction too.
   */
  private float sizeOfBlackWhiteBlackRunBothWays(int fromX, int fromY, int toX, int toY) {

    float result = sizeOfBlackWhiteBlackRun(fromX, fromY, toX, toY);

    // Now count other way -- don't run off image though of course
    float scale = 1.0f;
    int otherToX = fromX - (toX - fromX);
    if (otherToX < 0) {
      scale = fromX / (float) (fromX - otherToX);
      otherToX = 0;
    } else if (otherToX >= image.getWidth()) {
      scale = (image.getWidth() - 1 - fromX) / (float) (otherToX - fromX);
      otherToX = image.getWidth() - 1;
    }
    int otherToY = (int) (fromY - (toY - fromY) * scale);

    scale = 1.0f;
    if (otherToY < 0) {
      scale = fromY / (float) (fromY - otherToY);
      otherToY = 0;
    } else if (otherToY >= image.getHeight()) {
      scale = (image.getHeight() - 1 - fromY) / (float) (otherToY - fromY);
      otherToY = image.getHeight() - 1;
    }
    otherToX = (int) (fromX + (otherToX - fromX) * scale);

    result += sizeOfBlackWhiteBlackRun(fromX, fromY, otherToX, otherToY);

    // Middle pixel is double-counted this way; subtract 1
    return result - 1.0f;
  }

  /**
   * <p>This method traces a line from a point in the image, in the direction towards another point.
   * It begins in a black region, and keeps going until it finds white, then black, then white again.
   * It reports the distance from the start to this point.</p>
   *
   * <p>This is used when figuring out how wide a finder pattern is, when the finder pattern
   * may be skewed or rotated.</p>
   */
  private float sizeOfBlackWhiteBlackRun(int fromX, int fromY, int toX, int toY) {
    // Mild variant of Bresenham's algorithm;
    // see http://en.wikipedia.org/wiki/Bresenham's_line_algorithm
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
    int xstep = fromX < toX ? 1 : -1;
    int ystep = fromY < toY ? 1 : -1;

    // In black pixels, looking for white, first or second time.
    int state = 0;
    // Loop up until x == toX, but not beyond
    int xLimit = toX + xstep;
    for (int x = fromX, y = fromY; x != xLimit; x += xstep) {
      int realX = steep ? y : x;
      int realY = steep ? x : y;

      // Does current pixel mean we have moved white to black or vice versa?
      // Scanning black in state 0,2 and white in state 1, so if we find the wrong
      // color, advance to next state or end if we are in state 2 already
      if ((state == 1) == image.get(realX, realY)) {
        if (state == 2) {
          return MathUtils_S.distance(x, y, fromX, fromY);
        }
        state++;
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
    // Found black-white-black; give the benefit of the doubt that the next pixel outside the image
    // is "white" so this last point at (toX+xStep,toY) is the right ending. This is really a
    // small approximation; (toX+xStep,toY+yStep) might be really correct. Ignore this.
    if (state == 2) {
      return MathUtils_S.distance(toX + xstep, toY, fromX, fromY);
    }
    // else we didn't find even black-white-black; no estimate is really possible
    return Float.NaN;
  }

  /**
   * <p>Attempts to locate an alignment pattern in a limited region of the image, which is
   * guessed to contain it. This method uses {@link AlignmentPattern_S}.</p>
   *
   * @param overallEstModuleSize estimated module size so far
   * @param estAlignmentX x coordinate of center of area probably containing alignment pattern
   * @param estAlignmentY y coordinate of above
   * @param allowanceFactor number of pixels in all directions to search from the center
   * @return {@link AlignmentPattern_S} if found, or null otherwise
   * @throws NotFoundException_S if an unexpected error occurs during detection
   */
  protected final AlignmentPattern_S findAlignmentInRegion(float overallEstModuleSize,
                                                           int estAlignmentX,
                                                           int estAlignmentY,
                                                           float allowanceFactor)
      throws NotFoundException_S {
    // Look for an alignment pattern (3 modules in size) around where it
    // should be
    int allowance = (int) (allowanceFactor * overallEstModuleSize);
    int alignmentAreaLeftX = Math.max(0, estAlignmentX - allowance);
    int alignmentAreaRightX = Math.min(image.getWidth() - 1, estAlignmentX + allowance);
    if (alignmentAreaRightX - alignmentAreaLeftX < overallEstModuleSize * 3) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    int alignmentAreaTopY = Math.max(0, estAlignmentY - allowance);
    int alignmentAreaBottomY = Math.min(image.getHeight() - 1, estAlignmentY + allowance);
    if (alignmentAreaBottomY - alignmentAreaTopY < overallEstModuleSize * 3) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    AlignmentPatternFinder_S alignmentFinder =
        new AlignmentPatternFinder_S(
            image,
            alignmentAreaLeftX,
            alignmentAreaTopY,
            alignmentAreaRightX - alignmentAreaLeftX,
            alignmentAreaBottomY - alignmentAreaTopY,
            overallEstModuleSize,
            resultPointCallback);
    return alignmentFinder.find();
  }

}
