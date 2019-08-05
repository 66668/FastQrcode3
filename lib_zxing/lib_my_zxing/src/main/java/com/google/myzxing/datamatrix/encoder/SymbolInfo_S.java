/*
 * Copyright 2006 Jeremias Maerki
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

package com.google.myzxing.datamatrix.encoder;


import com.google.myzxing.Dimension_S;

/**
 * Symbol info table for DataMatrix.
 *
 * @version $Id$
 */
public class SymbolInfo_S {

  static final SymbolInfo_S[] PROD_SYMBOLS = {
    new SymbolInfo_S(false, 3, 5, 8, 8, 1),
    new SymbolInfo_S(false, 5, 7, 10, 10, 1),
      /*rect*/new SymbolInfo_S(true, 5, 7, 16, 6, 1),
    new SymbolInfo_S(false, 8, 10, 12, 12, 1),
      /*rect*/new SymbolInfo_S(true, 10, 11, 14, 6, 2),
    new SymbolInfo_S(false, 12, 12, 14, 14, 1),
      /*rect*/new SymbolInfo_S(true, 16, 14, 24, 10, 1),

    new SymbolInfo_S(false, 18, 14, 16, 16, 1),
    new SymbolInfo_S(false, 22, 18, 18, 18, 1),
      /*rect*/new SymbolInfo_S(true, 22, 18, 16, 10, 2),
    new SymbolInfo_S(false, 30, 20, 20, 20, 1),
      /*rect*/new SymbolInfo_S(true, 32, 24, 16, 14, 2),
    new SymbolInfo_S(false, 36, 24, 22, 22, 1),
    new SymbolInfo_S(false, 44, 28, 24, 24, 1),
      /*rect*/new SymbolInfo_S(true, 49, 28, 22, 14, 2),

    new SymbolInfo_S(false, 62, 36, 14, 14, 4),
    new SymbolInfo_S(false, 86, 42, 16, 16, 4),
    new SymbolInfo_S(false, 114, 48, 18, 18, 4),
    new SymbolInfo_S(false, 144, 56, 20, 20, 4),
    new SymbolInfo_S(false, 174, 68, 22, 22, 4),

    new SymbolInfo_S(false, 204, 84, 24, 24, 4, 102, 42),
    new SymbolInfo_S(false, 280, 112, 14, 14, 16, 140, 56),
    new SymbolInfo_S(false, 368, 144, 16, 16, 16, 92, 36),
    new SymbolInfo_S(false, 456, 192, 18, 18, 16, 114, 48),
    new SymbolInfo_S(false, 576, 224, 20, 20, 16, 144, 56),
    new SymbolInfo_S(false, 696, 272, 22, 22, 16, 174, 68),
    new SymbolInfo_S(false, 816, 336, 24, 24, 16, 136, 56),
    new SymbolInfo_S(false, 1050, 408, 18, 18, 36, 175, 68),
    new SymbolInfo_S(false, 1304, 496, 20, 20, 36, 163, 62),
    new DataMatrixSymbolInfo144_S(),
  };

  private static SymbolInfo_S[] symbols = PROD_SYMBOLS;

  private final boolean rectangular;
  private final int dataCapacity;
  private final int errorCodewords;
  public final int matrixWidth;
  public final int matrixHeight;
  private final int dataRegions;
  private final int rsBlockData;
  private final int rsBlockError;

  /**
   * Overrides the symbol info set used by this class. Used for testing purposes.
   *
   * @param override the symbol info set to use
   */
  public static void overrideSymbolSet(SymbolInfo_S[] override) {
    symbols = override;
  }

  public SymbolInfo_S(boolean rectangular, int dataCapacity, int errorCodewords,
                      int matrixWidth, int matrixHeight, int dataRegions) {
    this(rectangular, dataCapacity, errorCodewords, matrixWidth, matrixHeight, dataRegions,
         dataCapacity, errorCodewords);
  }

  SymbolInfo_S(boolean rectangular, int dataCapacity, int errorCodewords,
               int matrixWidth, int matrixHeight, int dataRegions,
               int rsBlockData, int rsBlockError) {
    this.rectangular = rectangular;
    this.dataCapacity = dataCapacity;
    this.errorCodewords = errorCodewords;
    this.matrixWidth = matrixWidth;
    this.matrixHeight = matrixHeight;
    this.dataRegions = dataRegions;
    this.rsBlockData = rsBlockData;
    this.rsBlockError = rsBlockError;
  }

  public static SymbolInfo_S lookup(int dataCodewords) {
    return lookup(dataCodewords, SymbolShapeHint_S.FORCE_NONE, true);
  }

  public static SymbolInfo_S lookup(int dataCodewords, SymbolShapeHint_S shape) {
    return lookup(dataCodewords, shape, true);
  }

  public static SymbolInfo_S lookup(int dataCodewords, boolean allowRectangular, boolean fail) {
    SymbolShapeHint_S shape = allowRectangular
        ? SymbolShapeHint_S.FORCE_NONE : SymbolShapeHint_S.FORCE_SQUARE;
    return lookup(dataCodewords, shape, fail);
  }

  private static SymbolInfo_S lookup(int dataCodewords, SymbolShapeHint_S shape, boolean fail) {
    return lookup(dataCodewords, shape, null, null, fail);
  }

  public static SymbolInfo_S lookup(int dataCodewords,
                                    SymbolShapeHint_S shape,
                                    Dimension_S minSize,
                                    Dimension_S maxSize,
                                    boolean fail) {
    for (SymbolInfo_S symbol : symbols) {
      if (shape == SymbolShapeHint_S.FORCE_SQUARE && symbol.rectangular) {
        continue;
      }
      if (shape == SymbolShapeHint_S.FORCE_RECTANGLE && !symbol.rectangular) {
        continue;
      }
      if (minSize != null
          && (symbol.getSymbolWidth() < minSize.getWidth()
          || symbol.getSymbolHeight() < minSize.getHeight())) {
        continue;
      }
      if (maxSize != null
          && (symbol.getSymbolWidth() > maxSize.getWidth()
          || symbol.getSymbolHeight() > maxSize.getHeight())) {
        continue;
      }
      if (dataCodewords <= symbol.dataCapacity) {
        return symbol;
      }
    }
    if (fail) {
      throw new IllegalArgumentException(
          "Can't find a symbol arrangement that matches the message. Data codewords: "
              + dataCodewords);
    }
    return null;
  }

  private int getHorizontalDataRegions() {
    switch (dataRegions) {
      case 1:
        return 1;
      case 2:
      case 4:
        return 2;
      case 16:
        return 4;
      case 36:
        return 6;
      default:
        throw new IllegalStateException("Cannot handle this number of data regions");
    }
  }

  private int getVerticalDataRegions() {
    switch (dataRegions) {
      case 1:
      case 2:
        return 1;
      case 4:
        return 2;
      case 16:
        return 4;
      case 36:
        return 6;
      default:
        throw new IllegalStateException("Cannot handle this number of data regions");
    }
  }

  public final int getSymbolDataWidth() {
    return getHorizontalDataRegions() * matrixWidth;
  }

  public final int getSymbolDataHeight() {
    return getVerticalDataRegions() * matrixHeight;
  }

  public final int getSymbolWidth() {
    return getSymbolDataWidth() + (getHorizontalDataRegions() * 2);
  }

  public final int getSymbolHeight() {
    return getSymbolDataHeight() + (getVerticalDataRegions() * 2);
  }

  public int getCodewordCount() {
    return dataCapacity + errorCodewords;
  }

  public int getInterleavedBlockCount() {
    return dataCapacity / rsBlockData;
  }

  public final int getDataCapacity() {
    return dataCapacity;
  }

  public final int getErrorCodewords() {
    return errorCodewords;
  }

  public int getDataLengthForInterleavedBlock(int index) {
    return rsBlockData;
  }

  public final int getErrorLengthForInterleavedBlock(int index) {
    return rsBlockError;
  }

  @Override
  public final String toString() {
    return (rectangular ? "Rectangular Symbol:" : "Square Symbol:") +
        " data region " + matrixWidth + 'x' + matrixHeight +
        ", symbol size " + getSymbolWidth() + 'x' + getSymbolHeight() +
        ", symbol data size " + getSymbolDataWidth() + 'x' + getSymbolDataHeight() +
        ", codewords " + dataCapacity + '+' + errorCodewords;
  }

}
