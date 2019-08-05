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

package com.google.myzxing.aztec;


import com.google.myzxing.BarcodeFormat_S;
import com.google.myzxing.EncodeHintType_S;
import com.google.myzxing.Writer_S;
import com.google.myzxing.aztec.encoder.AztecCode;
import com.google.myzxing.aztec.encoder.Encoder;
import com.google.myzxing.common.BitMatrix_S;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Renders an Aztec code as a {@link BitMatrix_S}.
 */
public final class AztecWriter_S implements Writer_S {

  @Override
  public BitMatrix_S encode(String contents, BarcodeFormat_S format, int width, int height) {
    return encode(contents, format, width, height, null);
  }

  @Override
  public BitMatrix_S encode(String contents, BarcodeFormat_S format, int width, int height, Map<EncodeHintType_S,?> hints) {
    Charset charset = StandardCharsets.ISO_8859_1;
    int eccPercent = Encoder.DEFAULT_EC_PERCENT;
    int layers = Encoder.DEFAULT_AZTEC_LAYERS;
    if (hints != null) {
      if (hints.containsKey(EncodeHintType_S.CHARACTER_SET)) {
        charset = Charset.forName(hints.get(EncodeHintType_S.CHARACTER_SET).toString());
      }
      if (hints.containsKey(EncodeHintType_S.ERROR_CORRECTION)) {
        eccPercent = Integer.parseInt(hints.get(EncodeHintType_S.ERROR_CORRECTION).toString());
      }
      if (hints.containsKey(EncodeHintType_S.AZTEC_LAYERS)) {
        layers = Integer.parseInt(hints.get(EncodeHintType_S.AZTEC_LAYERS).toString());
      }
    }
    return encode(contents, format, width, height, charset, eccPercent, layers);
  }

  private static BitMatrix_S encode(String contents, BarcodeFormat_S format,
                                    int width, int height,
                                    Charset charset, int eccPercent, int layers) {
    if (format != BarcodeFormat_S.AZTEC) {
      throw new IllegalArgumentException("Can only encode AZTEC, but got " + format);
    }
    AztecCode aztec = Encoder.encode(contents.getBytes(charset), eccPercent, layers);
    return renderResult(aztec, width, height);
  }

  private static BitMatrix_S renderResult(AztecCode code, int width, int height) {
    BitMatrix_S input = code.getMatrix();
    if (input == null) {
      throw new IllegalStateException();
    }
    int inputWidth = input.getWidth();
    int inputHeight = input.getHeight();
    int outputWidth = Math.max(width, inputWidth);
    int outputHeight = Math.max(height, inputHeight);

    int multiple = Math.min(outputWidth / inputWidth, outputHeight / inputHeight);
    int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
    int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

    BitMatrix_S output = new BitMatrix_S(outputWidth, outputHeight);

    for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
      // Write the contents of this row of the barcode
      for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
        if (input.get(inputX, inputY)) {
          output.setRegion(outputX, outputY, multiple, multiple);
        }
      }
    }
    return output;
  }
}
