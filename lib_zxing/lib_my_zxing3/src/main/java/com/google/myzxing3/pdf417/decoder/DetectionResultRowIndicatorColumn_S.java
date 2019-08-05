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

package com.google.myzxing3.pdf417.decoder;

import com.google.myzxing3.ResultPoint_S;
import com.google.myzxing3.pdf417.PDF417Common_S;

/**
 * @author Guenther Grau
 */
final class DetectionResultRowIndicatorColumn_S extends DetectionResultColumn_S {

  private final boolean isLeft;

  DetectionResultRowIndicatorColumn_S(BoundingBox_S boundingBox, boolean isLeft) {
    super(boundingBox);
    this.isLeft = isLeft;
  }

  private void setRowNumbers() {
    for (Codeword_S codeword : getCodewords()) {
      if (codeword != null) {
        codeword.setRowNumberAsRowIndicatorColumn();
      }
    }
  }

  // TODO implement properly
  // TODO maybe we should add missing codewords to store the correct row number to make
  // finding row numbers for other columns easier
  // use row height count to make detection of invalid row numbers more reliable
  void adjustCompleteIndicatorColumnRowNumbers(BarcodeMetadata_S barcodeMetadata) {
    Codeword_S[] codewords = getCodewords();
    setRowNumbers();
    removeIncorrectCodewords(codewords, barcodeMetadata);
    BoundingBox_S boundingBox = getBoundingBox();
    ResultPoint_S top = isLeft ? boundingBox.getTopLeft() : boundingBox.getTopRight();
    ResultPoint_S bottom = isLeft ? boundingBox.getBottomLeft() : boundingBox.getBottomRight();
    int firstRow = imageRowToCodewordIndex((int) top.getY());
    int lastRow = imageRowToCodewordIndex((int) bottom.getY());
    // We need to be careful using the average row height. Barcode could be skewed so that we have smaller and
    // taller rows
    //float averageRowHeight = (lastRow - firstRow) / (float) barcodeMetadata.getRowCount();
    int barcodeRow = -1;
    int maxRowHeight = 1;
    int currentRowHeight = 0;
    for (int codewordsRow = firstRow; codewordsRow < lastRow; codewordsRow++) {
      if (codewords[codewordsRow] == null) {
        continue;
      }
      Codeword_S codeword = codewords[codewordsRow];

      //      float expectedRowNumber = (codewordsRow - firstRow) / averageRowHeight;
      //      if (Math.abs(codeword.getRowNumber() - expectedRowNumber) > 2) {
      //        SimpleLog.log(LEVEL.WARNING,
      //            "Removing codeword, rowNumberSkew too high, codeword[" + codewordsRow + "]: Expected Row: " +
      //                expectedRowNumber + ", RealRow: " + codeword.getRowNumber() + ", value: " + codeword.getValue());
      //        codewords[codewordsRow] = null;
      //      }

      int rowDifference = codeword.getRowNumber() - barcodeRow;

      // TODO improve handling with case where first row indicator doesn't start with 0

      if (rowDifference == 0) {
        currentRowHeight++;
      } else if (rowDifference == 1) {
        maxRowHeight = Math.max(maxRowHeight, currentRowHeight);
        currentRowHeight = 1;
        barcodeRow = codeword.getRowNumber();
      } else if (rowDifference < 0 ||
                 codeword.getRowNumber() >= barcodeMetadata.getRowCount() ||
                 rowDifference > codewordsRow) {
        codewords[codewordsRow] = null;
      } else {
        int checkedRows;
        if (maxRowHeight > 2) {
          checkedRows = (maxRowHeight - 2) * rowDifference;
        } else {
          checkedRows = rowDifference;
        }
        boolean closePreviousCodewordFound = checkedRows >= codewordsRow;
        for (int i = 1; i <= checkedRows && !closePreviousCodewordFound; i++) {
          // there must be (height * rowDifference) number of codewords missing. For now we assume height = 1.
          // This should hopefully get rid of most problems already.
          closePreviousCodewordFound = codewords[codewordsRow - i] != null;
        }
        if (closePreviousCodewordFound) {
          codewords[codewordsRow] = null;
        } else {
          barcodeRow = codeword.getRowNumber();
          currentRowHeight = 1;
        }
      }
    }
    //return (int) (averageRowHeight + 0.5);
  }

  int[] getRowHeights() {
    BarcodeMetadata_S barcodeMetadata = getBarcodeMetadata();
    if (barcodeMetadata == null) {
      return null;
    }
    adjustIncompleteIndicatorColumnRowNumbers(barcodeMetadata);
    int[] result = new int[barcodeMetadata.getRowCount()];
    for (Codeword_S codeword : getCodewords()) {
      if (codeword != null) {
        int rowNumber = codeword.getRowNumber();
        if (rowNumber >= result.length) {
          // We have more rows than the barcode metadata allows for, ignore them.
          continue;
        }
        result[rowNumber]++;
      } // else throw exception?
    }
    return result;
  }

  // TODO maybe we should add missing codewords to store the correct row number to make
  // finding row numbers for other columns easier
  // use row height count to make detection of invalid row numbers more reliable
  private void adjustIncompleteIndicatorColumnRowNumbers(BarcodeMetadata_S barcodeMetadata) {
    BoundingBox_S boundingBox = getBoundingBox();
    ResultPoint_S top = isLeft ? boundingBox.getTopLeft() : boundingBox.getTopRight();
    ResultPoint_S bottom = isLeft ? boundingBox.getBottomLeft() : boundingBox.getBottomRight();
    int firstRow = imageRowToCodewordIndex((int) top.getY());
    int lastRow = imageRowToCodewordIndex((int) bottom.getY());
    //float averageRowHeight = (lastRow - firstRow) / (float) barcodeMetadata.getRowCount();
    Codeword_S[] codewords = getCodewords();
    int barcodeRow = -1;
    int maxRowHeight = 1;
    int currentRowHeight = 0;
    for (int codewordsRow = firstRow; codewordsRow < lastRow; codewordsRow++) {
      if (codewords[codewordsRow] == null) {
        continue;
      }
      Codeword_S codeword = codewords[codewordsRow];

      codeword.setRowNumberAsRowIndicatorColumn();

      int rowDifference = codeword.getRowNumber() - barcodeRow;

      // TODO improve handling with case where first row indicator doesn't start with 0

      if (rowDifference == 0) {
        currentRowHeight++;
      } else if (rowDifference == 1) {
        maxRowHeight = Math.max(maxRowHeight, currentRowHeight);
        currentRowHeight = 1;
        barcodeRow = codeword.getRowNumber();
      } else if (codeword.getRowNumber() >= barcodeMetadata.getRowCount()) {
        codewords[codewordsRow] = null;
      } else {
        barcodeRow = codeword.getRowNumber();
        currentRowHeight = 1;
      }
    }
    //return (int) (averageRowHeight + 0.5);
  }

  BarcodeMetadata_S getBarcodeMetadata() {
    Codeword_S[] codewords = getCodewords();
    BarcodeValue_S barcodeColumnCount = new BarcodeValue_S();
    BarcodeValue_S barcodeRowCountUpperPart = new BarcodeValue_S();
    BarcodeValue_S barcodeRowCountLowerPart = new BarcodeValue_S();
    BarcodeValue_S barcodeECLevel = new BarcodeValue_S();
    for (Codeword_S codeword : codewords) {
      if (codeword == null) {
        continue;
      }
      codeword.setRowNumberAsRowIndicatorColumn();
      int rowIndicatorValue = codeword.getValue() % 30;
      int codewordRowNumber = codeword.getRowNumber();
      if (!isLeft) {
        codewordRowNumber += 2;
      }
      switch (codewordRowNumber % 3) {
        case 0:
          barcodeRowCountUpperPart.setValue(rowIndicatorValue * 3 + 1);
          break;
        case 1:
          barcodeECLevel.setValue(rowIndicatorValue / 3);
          barcodeRowCountLowerPart.setValue(rowIndicatorValue % 3);
          break;
        case 2:
          barcodeColumnCount.setValue(rowIndicatorValue + 1);
          break;
      }
    }
    // Maybe we should check if we have ambiguous values?
    if ((barcodeColumnCount.getValue().length == 0) ||
        (barcodeRowCountUpperPart.getValue().length == 0) ||
        (barcodeRowCountLowerPart.getValue().length == 0) ||
        (barcodeECLevel.getValue().length == 0) ||
        barcodeColumnCount.getValue()[0] < 1 ||
        barcodeRowCountUpperPart.getValue()[0] + barcodeRowCountLowerPart.getValue()[0] < PDF417Common_S.MIN_ROWS_IN_BARCODE ||
        barcodeRowCountUpperPart.getValue()[0] + barcodeRowCountLowerPart.getValue()[0] > PDF417Common_S.MAX_ROWS_IN_BARCODE) {
      return null;
    }
    BarcodeMetadata_S barcodeMetadata = new BarcodeMetadata_S(barcodeColumnCount.getValue()[0],
        barcodeRowCountUpperPart.getValue()[0], barcodeRowCountLowerPart.getValue()[0], barcodeECLevel.getValue()[0]);
    removeIncorrectCodewords(codewords, barcodeMetadata);
    return barcodeMetadata;
  }

  private void removeIncorrectCodewords(Codeword_S[] codewords, BarcodeMetadata_S barcodeMetadata) {
    // Remove codewords which do not match the metadata
    // TODO Maybe we should keep the incorrect codewords for the start and end positions?
    for (int codewordRow = 0; codewordRow < codewords.length; codewordRow++) {
      Codeword_S codeword = codewords[codewordRow];
      if (codewords[codewordRow] == null) {
        continue;
      }
      int rowIndicatorValue = codeword.getValue() % 30;
      int codewordRowNumber = codeword.getRowNumber();
      if (codewordRowNumber > barcodeMetadata.getRowCount()) {
        codewords[codewordRow] = null;
        continue;
      }
      if (!isLeft) {
        codewordRowNumber += 2;
      }
      switch (codewordRowNumber % 3) {
        case 0:
          if (rowIndicatorValue * 3 + 1 != barcodeMetadata.getRowCountUpperPart()) {
            codewords[codewordRow] = null;
          }
          break;
        case 1:
          if (rowIndicatorValue / 3 != barcodeMetadata.getErrorCorrectionLevel() ||
              rowIndicatorValue % 3 != barcodeMetadata.getRowCountLowerPart()) {
            codewords[codewordRow] = null;
          }
          break;
        case 2:
          if (rowIndicatorValue + 1 != barcodeMetadata.getColumnCount()) {
            codewords[codewordRow] = null;
          }
          break;
      }
    }
  }

  boolean isLeft() {
    return isLeft;
  }

  @Override
  public String toString() {
    return "IsLeft: " + isLeft + '\n' + super.toString();
  }

}
