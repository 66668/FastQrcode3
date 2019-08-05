/*
 * Copyright 2011 ZXing authors
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

package com.google.myzxing3.oned;

import com.google.myzxing3.BarcodeFormat_S;
import com.google.myzxing3.EncodeHintType_S;
import com.google.myzxing3.Writer_S;
import com.google.myzxing3.WriterException_S;
import com.google.myzxing3.common.BitMatrix_S;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>Encapsulates functionality and implementation that is common to one-dimensional barcodes.</p>
 *
 * @author dsbnatut@gmail.com (Kazuki Nishiura)
 */
public abstract class OneDimensionalCodeWriter_S implements Writer_S {
  private static final Pattern NUMERIC = Pattern.compile("[0-9]+");

  @Override
  public final BitMatrix_S encode(String contents, BarcodeFormat_S format, int width, int height)
      throws WriterException_S {
    return encode(contents, format, width, height, null);
  }

  /**
   * Encode the contents following specified format.
   * {@code width} and {@code height} are required size. This method may return bigger size
   * {@code BitMatrix} when specified size is too small. The user can set both {@code width} and
   * {@code height} to zero to get minimum size barcode. If negative value is set to {@code width}
   * or {@code height}, {@code IllegalArgumentException} is thrown.
   */
  @Override
  public BitMatrix_S encode(String contents,
                            BarcodeFormat_S format,
                            int width,
                            int height,
                            Map<EncodeHintType_S,?> hints) throws WriterException_S {
    if (contents.isEmpty()) {
      throw new IllegalArgumentException("Found empty contents");
    }

    if (width < 0 || height < 0) {
      throw new IllegalArgumentException("Negative size is not allowed. Input: "
                                             + width + 'x' + height);
    }

    int sidesMargin = getDefaultMargin();
    if (hints != null && hints.containsKey(EncodeHintType_S.MARGIN)) {
      sidesMargin = Integer.parseInt(hints.get(EncodeHintType_S.MARGIN).toString());
    }

    boolean[] code = encode(contents);
    return renderResult(code, width, height, sidesMargin);
  }

  /**
   * @return a byte array of horizontal pixels (0 = white, 1 = black)
   */
  private static BitMatrix_S renderResult(boolean[] code, int width, int height, int sidesMargin) {
    int inputWidth = code.length;
    // Add quiet zone on both sides.
    int fullWidth = inputWidth + sidesMargin;
    int outputWidth = Math.max(width, fullWidth);
    int outputHeight = Math.max(1, height);

    int multiple = outputWidth / fullWidth;
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;

    BitMatrix_S output = new BitMatrix_S(outputWidth, outputHeight);
    for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
      if (code[inputX]) {
        output.setRegion(outputX, 0, multiple, outputHeight);
      }
    }
    return output;
  }

  /**
   * @param contents string_a to check for numeric characters
   * @throws IllegalArgumentException if input contains characters other than digits 0-9.
   */
  protected static void checkNumeric(String contents) {
    if (!NUMERIC.matcher(contents).matches()) {
      throw new IllegalArgumentException("Input should only contain digits 0-9");
    }
  }

  /**
   * @param target encode black/white pattern into this array
   * @param pos position to start encoding at in {@code target}
   * @param pattern lengths of black/white runs to encode
   * @param startColor starting color - false for white, true for black
   * @return the number of elements added to target.
   */
  protected static int appendPattern(boolean[] target, int pos, int[] pattern, boolean startColor) {
    boolean color = startColor;
    int numAdded = 0;
    for (int len : pattern) {
      for (int j = 0; j < len; j++) {
        target[pos++] = color;
      }
      numAdded += len;
      color = !color; // flip color after each segment
    }
    return numAdded;
  }

  public int getDefaultMargin() {
    // CodaBar spec requires a side margin to be more than ten times wider than narrow space.
    // This seems like a decent idea for a default for all formats.
    return 10;
  }

  /**
   * Encode the contents to boolean array expression of one-dimensional barcode.
   * Start code and end code should be included in result, and side margins should not be included.
   *
   * @param contents barcode contents to encode
   * @return a {@code boolean[]} of horizontal pixels (false = white, true = black)
   */
  public abstract boolean[] encode(String contents);
}

