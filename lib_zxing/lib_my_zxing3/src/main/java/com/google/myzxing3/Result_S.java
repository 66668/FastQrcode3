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

package com.google.myzxing3;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>Encapsulates the result of decoding a barcode within an image.</p>
 *
 * @author Sean Owen
 */
public final class Result_S {

  private final String text;
  private final byte[] rawBytes;
  private final int numBits;
  private ResultPoint_S[] resultPoints;
  private final BarcodeFormat_S format;
  private Map<ResultMetadataType_S,Object> resultMetadata;
  private final long timestamp;

  public Result_S(String text,
                  byte[] rawBytes,
                  ResultPoint_S[] resultPoints,
                  BarcodeFormat_S format) {
    this(text, rawBytes, resultPoints, format, System.currentTimeMillis());
  }

  public Result_S(String text,
                  byte[] rawBytes,
                  ResultPoint_S[] resultPoints,
                  BarcodeFormat_S format,
                  long timestamp) {
    this(text, rawBytes, rawBytes == null ? 0 : 8 * rawBytes.length,
         resultPoints, format, timestamp);
  }

  public Result_S(String text,
                  byte[] rawBytes,
                  int numBits,
                  ResultPoint_S[] resultPoints,
                  BarcodeFormat_S format,
                  long timestamp) {
    this.text = text;
    this.rawBytes = rawBytes;
    this.numBits = numBits;
    this.resultPoints = resultPoints;
    this.format = format;
    this.resultMetadata = null;
    this.timestamp = timestamp;
  }

  /**
   * @return raw text encoded by the barcode
   */
  public String getText() {
    return text;
  }

  /**
   * @return raw bytes encoded by the barcode, if applicable, otherwise {@code null}
   */
  public byte[] getRawBytes() {
    return rawBytes;
  }

  /**
   * @return how many bits of {@link #getRawBytes()} are valid; typically 8 times its length
   * @since 3.3.0
   */
  public int getNumBits() {
    return numBits;
  }

  /**
   * @return points related to the barcode in the image. These are typically points
   *         identifying finder patterns or the corners of the barcode. The exact meaning is
   *         specific to the type of barcode that was decoded.
   */
  public ResultPoint_S[] getResultPoints() {
    return resultPoints;
  }

  /**
   * @return {@link BarcodeFormat_S} representing the format of the barcode that was decoded
   */
  public BarcodeFormat_S getBarcodeFormat() {
    return format;
  }

  /**
   * @return {@link Map} mapping {@link ResultMetadataType_S} keys to values. May be
   *   {@code null}. This contains optional metadata about what was detected about the barcode,
   *   like orientation.
   */
  public Map<ResultMetadataType_S,Object> getResultMetadata() {
    return resultMetadata;
  }

  public void putMetadata(ResultMetadataType_S type, Object value) {
    if (resultMetadata == null) {
      resultMetadata = new EnumMap<>(ResultMetadataType_S.class);
    }
    resultMetadata.put(type, value);
  }

  public void putAllMetadata(Map<ResultMetadataType_S,Object> metadata) {
    if (metadata != null) {
      if (resultMetadata == null) {
        resultMetadata = metadata;
      } else {
        resultMetadata.putAll(metadata);
      }
    }
  }

  public void addResultPoints(ResultPoint_S[] newPoints) {
    ResultPoint_S[] oldPoints = resultPoints;
    if (oldPoints == null) {
      resultPoints = newPoints;
    } else if (newPoints != null && newPoints.length > 0) {
      ResultPoint_S[] allPoints = new ResultPoint_S[oldPoints.length + newPoints.length];
      System.arraycopy(oldPoints, 0, allPoints, 0, oldPoints.length);
      System.arraycopy(newPoints, 0, allPoints, oldPoints.length, newPoints.length);
      resultPoints = allPoints;
    }
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return text;
  }

}
