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

package com.google.myzxing2.qrcode.decoder;

import com.google.myzxing2.FormatException_S;
import com.google.myzxing2.common.BitMatrix_S;

/**
 * @author Sean Owen
 */
final class BitMatrixParser_S {

  private final BitMatrix_S bitMatrix;
  private Version_S parsedVersion;
  private com.google.myzxing2.qrcode.decoder.FormatInformation_S parsedFormatInfo;
  private boolean mirror;

  /**
   * @param bitMatrix {@link BitMatrix_S} to parse
   * @throws FormatException_S if dimension is not >= 21 and 1 mod 4
   */
  BitMatrixParser_S(BitMatrix_S bitMatrix) throws FormatException_S {
    int dimension = bitMatrix.getHeight();
    if (dimension < 21 || (dimension & 0x03) != 1) {
      throw FormatException_S.getFormatInstance();
    }
    this.bitMatrix = bitMatrix;
  }

  /**
   * <p>Reads format information from one of its two locations within the QR Code.</p>
   *
   * @return {@link FormatInformation_S} encapsulating the QR Code's format info
   * @throws FormatException_S if both format information locations cannot be parsed as
   * the valid encoding of format information
   */
  com.google.myzxing2.qrcode.decoder.FormatInformation_S readFormatInformation() throws FormatException_S {

    if (parsedFormatInfo != null) {
      return parsedFormatInfo;
    }

    // Read top-left format info bits
    int formatInfoBits1 = 0;
    for (int i = 0; i < 6; i++) {
      formatInfoBits1 = copyBit(i, 8, formatInfoBits1);
    }
    // .. and skip a bit in the timing pattern ...
    formatInfoBits1 = copyBit(7, 8, formatInfoBits1);
    formatInfoBits1 = copyBit(8, 8, formatInfoBits1);
    formatInfoBits1 = copyBit(8, 7, formatInfoBits1);
    // .. and skip a bit in the timing pattern ...
    for (int j = 5; j >= 0; j--) {
      formatInfoBits1 = copyBit(8, j, formatInfoBits1);
    }

    // Read the top-right/bottom-left pattern too
    int dimension = bitMatrix.getHeight();
    int formatInfoBits2 = 0;
    int jMin = dimension - 7;
    for (int j = dimension - 1; j >= jMin; j--) {
      formatInfoBits2 = copyBit(8, j, formatInfoBits2);
    }
    for (int i = dimension - 8; i < dimension; i++) {
      formatInfoBits2 = copyBit(i, 8, formatInfoBits2);
    }

    parsedFormatInfo = com.google.myzxing2.qrcode.decoder.FormatInformation_S.decodeFormatInformation(formatInfoBits1, formatInfoBits2);
    if (parsedFormatInfo != null) {
      return parsedFormatInfo;
    }
    throw FormatException_S.getFormatInstance();
  }

  /**
   * <p>Reads version information from one of its two locations within the QR Code.</p>
   *
   * @return {@link Version_S} encapsulating the QR Code's version
   * @throws FormatException_S if both version information locations cannot be parsed as
   * the valid encoding of version information
   */
  Version_S readVersion() throws FormatException_S {

    if (parsedVersion != null) {
      return parsedVersion;
    }

    int dimension = bitMatrix.getHeight();

    int provisionalVersion = (dimension - 17) / 4;
    if (provisionalVersion <= 6) {
      return com.google.myzxing2.qrcode.decoder.Version_S.getVersionForNumber(provisionalVersion);
    }

    // Read top-right version info: 3 wide by 6 tall
    int versionBits = 0;
    int ijMin = dimension - 11;
    for (int j = 5; j >= 0; j--) {
      for (int i = dimension - 9; i >= ijMin; i--) {
        versionBits = copyBit(i, j, versionBits);
      }
    }

    com.google.myzxing2.qrcode.decoder.Version_S theParsedVersion = com.google.myzxing2.qrcode.decoder.Version_S.decodeVersionInformation(versionBits);
    if (theParsedVersion != null && theParsedVersion.getDimensionForVersion() == dimension) {
      parsedVersion = theParsedVersion;
      return theParsedVersion;
    }

    // Hmm, failed. Try bottom left: 6 wide by 3 tall
    versionBits = 0;
    for (int i = 5; i >= 0; i--) {
      for (int j = dimension - 9; j >= ijMin; j--) {
        versionBits = copyBit(i, j, versionBits);
      }
    }

    theParsedVersion = com.google.myzxing2.qrcode.decoder.Version_S.decodeVersionInformation(versionBits);
    if (theParsedVersion != null && theParsedVersion.getDimensionForVersion() == dimension) {
      parsedVersion = theParsedVersion;
      return theParsedVersion;
    }
    throw FormatException_S.getFormatInstance();
  }

  private int copyBit(int i, int j, int versionBits) {
    boolean bit = mirror ? bitMatrix.get(j, i) : bitMatrix.get(i, j);
    return bit ? (versionBits << 1) | 0x1 : versionBits << 1;
  }

  /**
   * <p>Reads the bits in the {@link BitMatrix_S} representing the finder pattern in the
   * correct order in order to reconstruct the codewords bytes contained within the
   * QR Code.</p>
   *
   * @return bytes encoded within the QR Code
   * @throws FormatException_S if the exact number of bytes expected is not read
   */
  byte[] readCodewords() throws FormatException_S {

    com.google.myzxing2.qrcode.decoder.FormatInformation_S formatInfo = readFormatInformation();
    Version_S version = readVersion();

    // Get the data mask for the format used in this QR Code. This will exclude
    // some bits from reading as we wind through the bit matrix.
    com.google.myzxing2.qrcode.decoder.DataMask_S dataMask = com.google.myzxing2.qrcode.decoder.DataMask_S.values()[formatInfo.getDataMask()];
    int dimension = bitMatrix.getHeight();
    dataMask.unmaskBitMatrix(bitMatrix, dimension);

    BitMatrix_S functionPattern = version.buildFunctionPattern();

    boolean readingUp = true;
    byte[] result = new byte[version.getTotalCodewords()];
    int resultOffset = 0;
    int currentByte = 0;
    int bitsRead = 0;
    // Read columns in pairs, from right to left
    for (int j = dimension - 1; j > 0; j -= 2) {
      if (j == 6) {
        // Skip whole column with vertical alignment pattern;
        // saves time and makes the other code proceed more cleanly
        j--;
      }
      // Read alternatingly from bottom to top then top to bottom
      for (int count = 0; count < dimension; count++) {
        int i = readingUp ? dimension - 1 - count : count;
        for (int col = 0; col < 2; col++) {
          // Ignore bits covered by the function pattern
          if (!functionPattern.get(j - col, i)) {
            // Read a bit
            bitsRead++;
            currentByte <<= 1;
            if (bitMatrix.get(j - col, i)) {
              currentByte |= 1;
            }
            // If we've made a whole byte, save it off
            if (bitsRead == 8) {
              result[resultOffset++] = (byte) currentByte;
              bitsRead = 0;
              currentByte = 0;
            }
          }
        }
      }
      readingUp ^= true; // readingUp = !readingUp; // switch directions
    }
    if (resultOffset != version.getTotalCodewords()) {
      throw FormatException_S.getFormatInstance();
    }
    return result;
  }

  /**
   * Revert the mask removal done while reading the code words. The bit matrix should revert to its original state.
   */
  void remask() {
    if (parsedFormatInfo == null) {
      return; // We have no format information, and have no data mask
    }
    com.google.myzxing2.qrcode.decoder.DataMask_S dataMask = com.google.myzxing2.qrcode.decoder.DataMask_S.values()[parsedFormatInfo.getDataMask()];
    int dimension = bitMatrix.getHeight();
    dataMask.unmaskBitMatrix(bitMatrix, dimension);
  }

  /**
   * Prepare the parser for a mirrored operation.
   * This flag has effect only on the {@link #readFormatInformation()} and the
   * {@link #readVersion()}. Before proceeding with {@link #readCodewords()} the
   * {@link #mirror()} method should be called.
   *
   * @param mirror Whether to read version and format information mirrored.
   */
  void setMirror(boolean mirror) {
    parsedVersion = null;
    parsedFormatInfo = null;
    this.mirror = mirror;
  }

  /** Mirror the bit matrix in order to attempt a second reading. */
  void mirror() {
    for (int x = 0; x < bitMatrix.getWidth(); x++) {
      for (int y = x + 1; y < bitMatrix.getHeight(); y++) {
        if (bitMatrix.get(x, y) != bitMatrix.get(y, x)) {
          bitMatrix.flip(y, x);
          bitMatrix.flip(x, y);
        }
      }
    }
  }

}
