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

package com.google.myzxing;

import com.google.myzxing.common.BitMatrix_S;

import java.util.Map;

/**
 * The base class for all objects which encode/generate a barcode image.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public interface Writer_S {

  /**
   * Encode a barcode using the default settings.
   *
   * @param contents The contents to encode in the barcode
   * @param format The barcode format to generate
   * @param width The preferred width in pixels
   * @param height The preferred height in pixels
   * @return {@link BitMatrix_S} representing encoded barcode image
   * @throws WriterException_S if contents cannot be encoded legally in a format
   */
  BitMatrix_S encode(String contents, BarcodeFormat_S format, int width, int height)
      throws WriterException_S;

  /**
   * @param contents The contents to encode in the barcode
   * @param format The barcode format to generate
   * @param width The preferred width in pixels
   * @param height The preferred height in pixels
   * @param hints Additional parameters to supply to the encoder
   * @return {@link BitMatrix_S} representing encoded barcode image
   * @throws WriterException_S if contents cannot be encoded legally in a format
   */
  BitMatrix_S encode(String contents,
                     BarcodeFormat_S format,
                     int width,
                     int height,
                     Map<EncodeHintType_S, ?> hints)
      throws WriterException_S;

}
