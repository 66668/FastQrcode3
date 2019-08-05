/*
 * Copyright 2012 ZXing authors
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

package com.google.myzxing.pdf417;

import com.google.myzxing.BarcodeFormat_S;
import com.google.myzxing.EncodeHintType_S;
import com.google.myzxing.Writer_S;
import com.google.myzxing.WriterException_S;
import com.google.myzxing.common.BitMatrix_S;
import com.google.myzxing.pdf417.encoder.Compaction_S;
import com.google.myzxing.pdf417.encoder.Dimensions_S;
import com.google.myzxing.pdf417.encoder.PDF417_S;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * @author Jacob Haynes
 * @author qwandor@google.com (Andrew Walbran)
 */
public final class PDF417Writer_S implements Writer_S {

  /**
   * default white space (margin) around the code
   */
  private static final int WHITE_SPACE = 30;

  /**
   * default error correction level
   */
  private static final int DEFAULT_ERROR_CORRECTION_LEVEL = 2;

  @Override
  public BitMatrix_S encode(String contents,
                            BarcodeFormat_S format,
                            int width,
                            int height,
                            Map<EncodeHintType_S,?> hints) throws WriterException_S {
    if (format != BarcodeFormat_S.PDF_417) {
      throw new IllegalArgumentException("Can only encode PDF_417, but got " + format);
    }

    PDF417_S encoder = new PDF417_S();
    int margin = WHITE_SPACE;
    int errorCorrectionLevel = DEFAULT_ERROR_CORRECTION_LEVEL;

    if (hints != null) {
      if (hints.containsKey(EncodeHintType_S.PDF417_COMPACT)) {
        encoder.setCompact(Boolean.valueOf(hints.get(EncodeHintType_S.PDF417_COMPACT).toString()));
      }
      if (hints.containsKey(EncodeHintType_S.PDF417_COMPACTION)) {
        encoder.setCompaction(Compaction_S.valueOf(hints.get(EncodeHintType_S.PDF417_COMPACTION).toString()));
      }
      if (hints.containsKey(EncodeHintType_S.PDF417_DIMENSIONS)) {
        Dimensions_S dimensions = (Dimensions_S) hints.get(EncodeHintType_S.PDF417_DIMENSIONS);
        encoder.setDimensions(dimensions.getMaxCols(),
                              dimensions.getMinCols(),
                              dimensions.getMaxRows(),
                              dimensions.getMinRows());
      }
      if (hints.containsKey(EncodeHintType_S.MARGIN)) {
        margin = Integer.parseInt(hints.get(EncodeHintType_S.MARGIN).toString());
      }
      if (hints.containsKey(EncodeHintType_S.ERROR_CORRECTION)) {
        errorCorrectionLevel = Integer.parseInt(hints.get(EncodeHintType_S.ERROR_CORRECTION).toString());
      }
      if (hints.containsKey(EncodeHintType_S.CHARACTER_SET)) {
        Charset encoding = Charset.forName(hints.get(EncodeHintType_S.CHARACTER_SET).toString());
        encoder.setEncoding(encoding);
      }
    }

    return bitMatrixFromEncoder(encoder, contents, errorCorrectionLevel, width, height, margin);
  }

  @Override
  public BitMatrix_S encode(String contents,
                            BarcodeFormat_S format,
                            int width,
                            int height) throws WriterException_S {
    return encode(contents, format, width, height, null);
  }

  /**
   * Takes encoder, accounts for width/height, and retrieves bit matrix
   */
  private static BitMatrix_S bitMatrixFromEncoder(PDF417_S encoder,
                                                  String contents,
                                                  int errorCorrectionLevel,
                                                  int width,
                                                  int height,
                                                  int margin) throws WriterException_S {
    encoder.generateBarcodeLogic(contents, errorCorrectionLevel);

    int aspectRatio = 4;
    byte[][] originalScale = encoder.getBarcodeMatrix().getScaledMatrix(1, aspectRatio);
    boolean rotated = false;
    if ((height > width) != (originalScale[0].length < originalScale.length)) {
      originalScale = rotateArray(originalScale);
      rotated = true;
    }

    int scaleX = width / originalScale[0].length;
    int scaleY = height / originalScale.length;

    int scale;
    if (scaleX < scaleY) {
      scale = scaleX;
    } else {
      scale = scaleY;
    }

    if (scale > 1) {
      byte[][] scaledMatrix =
          encoder.getBarcodeMatrix().getScaledMatrix(scale, scale * aspectRatio);
      if (rotated) {
        scaledMatrix = rotateArray(scaledMatrix);
      }
      return bitMatrixFromBitArray(scaledMatrix, margin);
    }
    return bitMatrixFromBitArray(originalScale, margin);
  }

  /**
   * This takes an array holding the values of the PDF 417
   *
   * @param input a byte array of information with 0 is black, and 1 is white
   * @param margin border around the barcode
   * @return BitMatrix of the input
   */
  private static BitMatrix_S bitMatrixFromBitArray(byte[][] input, int margin) {
    // Creates the bit matrix with extra space for whitespace
    BitMatrix_S output = new BitMatrix_S(input[0].length + 2 * margin, input.length + 2 * margin);
    output.clear();
    for (int y = 0, yOutput = output.getHeight() - margin - 1; y < input.length; y++, yOutput--) {
      byte[] inputY = input[y];
      for (int x = 0; x < input[0].length; x++) {
        // Zero is white in the byte matrix
        if (inputY[x] == 1) {
          output.set(x + margin, yOutput);
        }
      }
    }
    return output;
  }

  /**
   * Takes and rotates the it 90 degrees
   */
  private static byte[][] rotateArray(byte[][] bitarray) {
    byte[][] temp = new byte[bitarray[0].length][bitarray.length];
    for (int ii = 0; ii < bitarray.length; ii++) {
      // This makes the direction consistent on screen when rotating the
      // screen;
      int inverseii = bitarray.length - ii - 1;
      for (int jj = 0; jj < bitarray[0].length; jj++) {
        temp[jj][inverseii] = bitarray[ii][jj];
      }
    }
    return temp;
  }

}
