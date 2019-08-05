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

package com.google.myzxing3.datamatrix;

import com.google.myzxing3.BarcodeFormat_S;
import com.google.myzxing3.Dimension_S;
import com.google.myzxing3.EncodeHintType_S;
import com.google.myzxing3.Writer_S;
import com.google.myzxing3.common.BitMatrix_S;
import com.google.myzxing3.datamatrix.encoder.DefaultPlacement_S;
import com.google.myzxing3.datamatrix.encoder.ErrorCorrection_S;
import com.google.myzxing3.datamatrix.encoder.HighLevelEncoder_S;
import com.google.myzxing3.datamatrix.encoder.SymbolInfo_S;
import com.google.myzxing3.datamatrix.encoder.SymbolShapeHint_S;
import com.google.myzxing3.qrcode.encoder.ByteMatrix_S;

import java.util.Map;

/**
 * This object renders a Data Matrix code as a BitMatrix 2D array of greyscale values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Guillaume Le Biller Added to zxing lib.
 */
public final class DataMatrixWriter_S implements Writer_S {

  @Override
  public BitMatrix_S encode(String contents, BarcodeFormat_S format, int width, int height) {
    return encode(contents, format, width, height, null);
  }

  @Override
  public BitMatrix_S encode(String contents, BarcodeFormat_S format, int width, int height, Map<EncodeHintType_S,?> hints) {

    if (contents.isEmpty()) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (format != BarcodeFormat_S.DATA_MATRIX) {
      throw new IllegalArgumentException("Can only encode DATA_MATRIX, but got " + format);
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Requested dimensions can't be negative: " + width + 'x' + height);
    }

    // Try to get force shape & min / max size
    SymbolShapeHint_S shape = SymbolShapeHint_S.FORCE_NONE;
    Dimension_S minSize = null;
    Dimension_S maxSize = null;
    if (hints != null) {
      SymbolShapeHint_S requestedShape = (SymbolShapeHint_S) hints.get(EncodeHintType_S.DATA_MATRIX_SHAPE);
      if (requestedShape != null) {
        shape = requestedShape;
      }
      @SuppressWarnings("deprecation")
      Dimension_S requestedMinSize = (Dimension_S) hints.get(EncodeHintType_S.MIN_SIZE);
      if (requestedMinSize != null) {
        minSize = requestedMinSize;
      }
      @SuppressWarnings("deprecation")
      Dimension_S requestedMaxSize = (Dimension_S) hints.get(EncodeHintType_S.MAX_SIZE);
      if (requestedMaxSize != null) {
        maxSize = requestedMaxSize;
      }
    }


    //1. step: Data encodation
    String encoded = HighLevelEncoder_S.encodeHighLevel(contents, shape, minSize, maxSize);

    SymbolInfo_S symbolInfo = SymbolInfo_S.lookup(encoded.length(), shape, minSize, maxSize, true);

    //2. step: ECC generation
    String codewords = ErrorCorrection_S.encodeECC200(encoded, symbolInfo);

    //3. step: Module placement in Matrix
    DefaultPlacement_S placement = new DefaultPlacement_S(codewords, symbolInfo.getSymbolDataWidth(), symbolInfo.getSymbolDataHeight());
    placement.place();

    //4. step: low-level encoding
    return encodeLowLevel(placement, symbolInfo, width, height);
  }

  /**
   * Encode the given symbol info to a bit matrix.
   *
   * @param placement  The DataMatrix placement.
   * @param symbolInfo The symbol info to encode.
   * @return The bit matrix generated.
   */
  private static BitMatrix_S encodeLowLevel(DefaultPlacement_S placement, SymbolInfo_S symbolInfo, int width, int height) {
    int symbolWidth = symbolInfo.getSymbolDataWidth();
    int symbolHeight = symbolInfo.getSymbolDataHeight();

    ByteMatrix_S matrix = new ByteMatrix_S(symbolInfo.getSymbolWidth(), symbolInfo.getSymbolHeight());

    int matrixY = 0;

    for (int y = 0; y < symbolHeight; y++) {
      // Fill the top edge with alternate 0 / 1
      int matrixX;
      if ((y % symbolInfo.matrixHeight) == 0) {
        matrixX = 0;
        for (int x = 0; x < symbolInfo.getSymbolWidth(); x++) {
          matrix.set(matrixX, matrixY, (x % 2) == 0);
          matrixX++;
        }
        matrixY++;
      }
      matrixX = 0;
      for (int x = 0; x < symbolWidth; x++) {
        // Fill the right edge with full 1
        if ((x % symbolInfo.matrixWidth) == 0) {
          matrix.set(matrixX, matrixY, true);
          matrixX++;
        }
        matrix.set(matrixX, matrixY, placement.getBit(x, y));
        matrixX++;
        // Fill the right edge with alternate 0 / 1
        if ((x % symbolInfo.matrixWidth) == symbolInfo.matrixWidth - 1) {
          matrix.set(matrixX, matrixY, (y % 2) == 0);
          matrixX++;
        }
      }
      matrixY++;
      // Fill the bottom edge with full 1
      if ((y % symbolInfo.matrixHeight) == symbolInfo.matrixHeight - 1) {
        matrixX = 0;
        for (int x = 0; x < symbolInfo.getSymbolWidth(); x++) {
          matrix.set(matrixX, matrixY, true);
          matrixX++;
        }
        matrixY++;
      }
    }

    return convertByteMatrixToBitMatrix(matrix, width, height);
  }

  /**
   * Convert the ByteMatrix to BitMatrix.
   *
   * @param reqHeight The requested height of the image (in pixels) with the Datamatrix code
   * @param reqWidth The requested width of the image (in pixels) with the Datamatrix code
   * @param matrix The input matrix.
   * @return The output matrix.
   */
  private static BitMatrix_S convertByteMatrixToBitMatrix(ByteMatrix_S matrix, int reqWidth, int reqHeight) {
    int matrixWidth = matrix.getWidth();
    int matrixHeight = matrix.getHeight();
    int outputWidth = Math.max(reqWidth, matrixWidth);
    int outputHeight = Math.max(reqHeight, matrixHeight);

    int multiple = Math.min(outputWidth / matrixWidth, outputHeight / matrixHeight);

    int leftPadding = (outputWidth - (matrixWidth * multiple)) / 2 ;
    int topPadding = (outputHeight - (matrixHeight * multiple)) / 2 ;

    BitMatrix_S output;

    // remove padding if requested width and height are too small
    if (reqHeight < matrixHeight || reqWidth < matrixWidth) {
      leftPadding = 0;
      topPadding = 0;
      output = new BitMatrix_S(matrixWidth, matrixHeight);
    } else {
      output = new BitMatrix_S(reqWidth, reqHeight);
    }

    output.clear();
    for (int inputY = 0, outputY = topPadding; inputY < matrixHeight; inputY++, outputY += multiple) {
      // Write the contents of this row of the bytematrix
      for (int inputX = 0, outputX = leftPadding; inputX < matrixWidth; inputX++, outputX += multiple) {
        if (matrix.get(inputX, inputY) == 1) {
          output.setRegion(outputX, outputY, multiple, multiple);
        }
      }
    }

    return output;
  }

}
