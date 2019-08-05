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

package com.google.myzxing3;

/**
 * A wrapper implementation of {@link LuminanceSource_S} which inverts the luminances it returns -- black becomes
 * white and vice versa, and each value becomes (255-value).
 *
 * @author Sean Owen
 */
public final class InvertedLuminanceSource_S extends LuminanceSource_S {

  private final LuminanceSource_S delegate;

  public InvertedLuminanceSource_S(LuminanceSource_S delegate) {
    super(delegate.getWidth(), delegate.getHeight());
    this.delegate = delegate;
  }

  @Override
  public byte[] getRow(int y, byte[] row) {
    row = delegate.getRow(y, row);
    int width = getWidth();
    for (int i = 0; i < width; i++) {
      row[i] = (byte) (255 - (row[i] & 0xFF));
    }
    return row;
  }

  @Override
  public byte[] getMatrix() {
    byte[] matrix = delegate.getMatrix();
    int length = getWidth() * getHeight();
    byte[] invertedMatrix = new byte[length];
    for (int i = 0; i < length; i++) {
      invertedMatrix[i] = (byte) (255 - (matrix[i] & 0xFF));
    }
    return invertedMatrix;
  }

  @Override
  public boolean isCropSupported() {
    return delegate.isCropSupported();
  }

  @Override
  public LuminanceSource_S crop(int left, int top, int width, int height) {
    return new InvertedLuminanceSource_S(delegate.crop(left, top, width, height));
  }

  @Override
  public boolean isRotateSupported() {
    return delegate.isRotateSupported();
  }

  /**
   * @return original delegate {@link LuminanceSource_S} since invert undoes itself
   */
  @Override
  public LuminanceSource_S invert() {
    return delegate;
  }

  @Override
  public LuminanceSource_S rotateCounterClockwise() {
    return new InvertedLuminanceSource_S(delegate.rotateCounterClockwise());
  }

  @Override
  public LuminanceSource_S rotateCounterClockwise45() {
    return new InvertedLuminanceSource_S(delegate.rotateCounterClockwise45());
  }

}
