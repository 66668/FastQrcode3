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

package com.google.myzxing3.qrcode.encoder;

import com.google.myzxing3.qrcode.decoder.ErrorCorrectionLevel_S;
import com.google.myzxing3.qrcode.decoder.Mode_S;
import com.google.myzxing3.qrcode.decoder.Version_S;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class QRCode_S {

  public static final int NUM_MASK_PATTERNS = 8;

  private Mode_S mode;
  private ErrorCorrectionLevel_S ecLevel;
  private Version_S version;
  private int maskPattern;
  private ByteMatrix_S matrix;

  public QRCode_S() {
    maskPattern = -1;
  }

  public Mode_S getMode() {
    return mode;
  }

  public ErrorCorrectionLevel_S getECLevel() {
    return ecLevel;
  }

  public Version_S getVersion() {
    return version;
  }

  public int getMaskPattern() {
    return maskPattern;
  }

  public ByteMatrix_S getMatrix() {
    return matrix;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(200);
    result.append("<<\n");
    result.append(" mode: ");
    result.append(mode);
    result.append("\n ecLevel: ");
    result.append(ecLevel);
    result.append("\n version: ");
    result.append(version);
    result.append("\n maskPattern: ");
    result.append(maskPattern);
    if (matrix == null) {
      result.append("\n matrix: null\n");
    } else {
      result.append("\n matrix:\n");
      result.append(matrix);
    }
    result.append(">>\n");
    return result.toString();
  }

  public void setMode(Mode_S value) {
    mode = value;
  }

  public void setECLevel(ErrorCorrectionLevel_S value) {
    ecLevel = value;
  }

  public void setVersion(Version_S version) {
    this.version = version;
  }

  public void setMaskPattern(int value) {
    maskPattern = value;
  }

  public void setMatrix(ByteMatrix_S value) {
    matrix = value;
  }

  // Check if "mask_pattern" is valid.
  public static boolean isValidMaskPattern(int maskPattern) {
    return maskPattern >= 0 && maskPattern < NUM_MASK_PATTERNS;
  }

}
