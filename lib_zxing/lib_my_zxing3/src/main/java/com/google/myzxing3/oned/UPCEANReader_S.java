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

package com.google.myzxing3.oned;

import com.google.myzxing3.BarcodeFormat_S;
import com.google.myzxing3.ChecksumException_S;
import com.google.myzxing3.DecodeHintType_S;
import com.google.myzxing3.FormatException_S;
import com.google.myzxing3.NotFoundException_S;
import com.google.myzxing3.ReaderException_S;
import com.google.myzxing3.Result_S;
import com.google.myzxing3.ResultMetadataType_S;
import com.google.myzxing3.ResultPoint_S;
import com.google.myzxing3.ResultPointCallback_S;
import com.google.myzxing3.common.BitArray_S;

import java.util.Arrays;
import java.util.Map;

/**
 * <p>Encapsulates functionality and implementation that is common to UPC and EAN families
 * of one-dimensional barcodes.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 * @author alasdair@google.com (Alasdair Mackintosh)
 */
public abstract class UPCEANReader_S extends OneDReader_S {

  // These two values are critical for determining how permissive the decoding will be.
  // We've arrived at these values through a lot of trial and error. Setting them any higher
  // lets false positives creep in quickly.
  private static final float MAX_AVG_VARIANCE = 0.48f;
  private static final float MAX_INDIVIDUAL_VARIANCE = 0.7f;

  /**
   * Start/end guard pattern.
   */
  static final int[] START_END_PATTERN = {1, 1, 1,};

  /**
   * Pattern marking the middle of a UPC/EAN pattern, separating the two halves.
   */
  static final int[] MIDDLE_PATTERN = {1, 1, 1, 1, 1};
  /**
   * end guard pattern.
   */
  static final int[] END_PATTERN = {1, 1, 1, 1, 1, 1};
  /**
   * "Odd", or "L" patterns used to encode UPC/EAN digits.
   */
  static final int[][] L_PATTERNS = {
      {3, 2, 1, 1}, // 0
      {2, 2, 2, 1}, // 1
      {2, 1, 2, 2}, // 2
      {1, 4, 1, 1}, // 3
      {1, 1, 3, 2}, // 4
      {1, 2, 3, 1}, // 5
      {1, 1, 1, 4}, // 6
      {1, 3, 1, 2}, // 7
      {1, 2, 1, 3}, // 8
      {3, 1, 1, 2}  // 9
  };

  /**
   * As above but also including the "even", or "G" patterns used to encode UPC/EAN digits.
   */
  static final int[][] L_AND_G_PATTERNS;

  static {
    L_AND_G_PATTERNS = new int[20][];
    System.arraycopy(L_PATTERNS, 0, L_AND_G_PATTERNS, 0, 10);
    for (int i = 10; i < 20; i++) {
      int[] widths = L_PATTERNS[i - 10];
      int[] reversedWidths = new int[widths.length];
      for (int j = 0; j < widths.length; j++) {
        reversedWidths[j] = widths[widths.length - j - 1];
      }
      L_AND_G_PATTERNS[i] = reversedWidths;
    }
  }

  private final StringBuilder decodeRowStringBuffer;
  private final UPCEANExtensionSupport_S extensionReader;
  private final EANManufacturerOrgSupport_S eanManSupport;

  protected UPCEANReader_S() {
    decodeRowStringBuffer = new StringBuilder(20);
    extensionReader = new UPCEANExtensionSupport_S();
    eanManSupport = new EANManufacturerOrgSupport_S();
  }

  static int[] findStartGuardPattern(BitArray_S row) throws NotFoundException_S {
    boolean foundStart = false;
    int[] startRange = null;
    int nextStart = 0;
    int[] counters = new int[START_END_PATTERN.length];
    while (!foundStart) {
      Arrays.fill(counters, 0, START_END_PATTERN.length, 0);
      startRange = findGuardPattern(row, nextStart, false, START_END_PATTERN, counters);
      int start = startRange[0];
      nextStart = startRange[1];
      // Make sure there is a quiet zone at least as big as the start pattern before the barcode.
      // If this check would run off the left edge of the image, do not accept this barcode,
      // as it is very likely to be a false positive.
      int quietStart = start - (nextStart - start);
      if (quietStart >= 0) {
        foundStart = row.isRange(quietStart, start, false);
      }
    }
    return startRange;
  }

  @Override
  public Result_S decodeRow(int rowNumber, BitArray_S row, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, ChecksumException_S, FormatException_S {
    return decodeRow(rowNumber, row, findStartGuardPattern(row), hints);
  }

  /**
   * <p>Like {@link #decodeRow(int, BitArray_S, Map)}, but
   * allows caller to inform method about where the UPC/EAN start pattern is
   * found. This allows this to be computed once and reused across many implementations.</p>
   *
   * @param rowNumber row index into the image
   * @param row encoding of the row of the barcode image
   * @param startGuardRange start/end column where the opening start pattern was found
   * @param hints optional hints that influence decoding
   * @return {@link Result_S} encapsulating the result of decoding a barcode in the row
   * @throws NotFoundException_S if no potential barcode is found
   * @throws ChecksumException_S if a potential barcode is found but does not pass its checksum
   * @throws FormatException_S if a potential barcode is found but format is invalid
   */
  public Result_S decodeRow(int rowNumber,
                            BitArray_S row,
                            int[] startGuardRange,
                            Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, ChecksumException_S, FormatException_S {

    ResultPointCallback_S resultPointCallback = hints == null ? null :
        (ResultPointCallback_S) hints.get(DecodeHintType_S.NEED_RESULT_POINT_CALLBACK);

    if (resultPointCallback != null) {
      resultPointCallback.foundPossibleResultPoint(new ResultPoint_S(
          (startGuardRange[0] + startGuardRange[1]) / 2.0f, rowNumber
      ));
    }

    StringBuilder result = decodeRowStringBuffer;
    result.setLength(0);
    int endStart = decodeMiddle(row, startGuardRange, result);

    if (resultPointCallback != null) {
      resultPointCallback.foundPossibleResultPoint(new ResultPoint_S(
          endStart, rowNumber
      ));
    }

    int[] endRange = decodeEnd(row, endStart);

    if (resultPointCallback != null) {
      resultPointCallback.foundPossibleResultPoint(new ResultPoint_S(
          (endRange[0] + endRange[1]) / 2.0f, rowNumber
      ));
    }


    // Make sure there is a quiet zone at least as big as the end pattern after the barcode. The
    // spec might want more whitespace, but in practice this is the maximum we can count on.
    int end = endRange[1];
    int quietEnd = end + (end - endRange[0]);
    if (quietEnd >= row.getSize() || !row.isRange(end, quietEnd, false)) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    String resultString = result.toString();
    // UPC/EAN should never be less than 8 chars anyway
    if (resultString.length() < 8) {
      throw FormatException_S.getFormatInstance();
    }
    if (!checkChecksum(resultString)) {
      throw ChecksumException_S.getChecksumInstance();
    }

    float left = (startGuardRange[1] + startGuardRange[0]) / 2.0f;
    float right = (endRange[1] + endRange[0]) / 2.0f;
    BarcodeFormat_S format = getBarcodeFormat();
    Result_S decodeResult = new Result_S(resultString,
        null, // no natural byte representation for these barcodes
        new ResultPoint_S[]{
            new ResultPoint_S(left, rowNumber),
            new ResultPoint_S(right, rowNumber)},
        format);

    int extensionLength = 0;

    try {
      Result_S extensionResult = extensionReader.decodeRow(rowNumber, row, endRange[1]);
      decodeResult.putMetadata(ResultMetadataType_S.UPC_EAN_EXTENSION, extensionResult.getText());
      decodeResult.putAllMetadata(extensionResult.getResultMetadata());
      decodeResult.addResultPoints(extensionResult.getResultPoints());
      extensionLength = extensionResult.getText().length();
    } catch (ReaderException_S re) {
      // continue
    }

    int[] allowedExtensions =
        hints == null ? null : (int[]) hints.get(DecodeHintType_S.ALLOWED_EAN_EXTENSIONS);
    if (allowedExtensions != null) {
      boolean valid = false;
      for (int length : allowedExtensions) {
        if (extensionLength == length) {
          valid = true;
          break;
        }
      }
      if (!valid) {
        throw NotFoundException_S.getNotFoundInstance();
      }
    }

    if (format == BarcodeFormat_S.EAN_13 || format == BarcodeFormat_S.UPC_A) {
      String countryID = eanManSupport.lookupCountryIdentifier(resultString);
      if (countryID != null) {
        decodeResult.putMetadata(ResultMetadataType_S.POSSIBLE_COUNTRY, countryID);
      }
    }

    return decodeResult;
  }

  /**
   * @param s string_a of digits to check
   * @return {@link #checkStandardUPCEANChecksum(CharSequence)}
   * @throws FormatException_S if the string_a does not contain only digits
   */
  boolean checkChecksum(String s) throws FormatException_S {
    return checkStandardUPCEANChecksum(s);
  }

  /**
   * Computes the UPC/EAN checksum on a string_a of digits, and reports
   * whether the checksum is correct or not.
   *
   * @param s string_a of digits to check
   * @return true iff string_a of digits passes the UPC/EAN checksum algorithm
   * @throws FormatException_S if the string_a does not contain only digits
   */
  static boolean checkStandardUPCEANChecksum(CharSequence s) throws FormatException_S {
    int length = s.length();
    if (length == 0) {
      return false;
    }
    int check = Character.digit(s.charAt(length - 1), 10);
    return getStandardUPCEANChecksum(s.subSequence(0, length - 1)) == check;
  }

  static int getStandardUPCEANChecksum(CharSequence s) throws FormatException_S {
    int length = s.length();
    int sum = 0;
    for (int i = length - 1; i >= 0; i -= 2) {
      int digit = s.charAt(i) - '0';
      if (digit < 0 || digit > 9) {
        throw FormatException_S.getFormatInstance();
      }
      sum += digit;
    }
    sum *= 3;
    for (int i = length - 2; i >= 0; i -= 2) {
      int digit = s.charAt(i) - '0';
      if (digit < 0 || digit > 9) {
        throw FormatException_S.getFormatInstance();
      }
      sum += digit;
    }
    return (1000 - sum) % 10;
  }

  int[] decodeEnd(BitArray_S row, int endStart) throws NotFoundException_S {
    return findGuardPattern(row, endStart, false, START_END_PATTERN);
  }

  static int[] findGuardPattern(BitArray_S row,
                                int rowOffset,
                                boolean whiteFirst,
                                int[] pattern) throws NotFoundException_S {
    return findGuardPattern(row, rowOffset, whiteFirst, pattern, new int[pattern.length]);
  }

  /**
   * @param row row of black/white values to search
   * @param rowOffset position to start search
   * @param whiteFirst if true, indicates that the pattern specifies white/black/white/...
   * pixel counts, otherwise, it is interpreted as black/white/black/...
   * @param pattern pattern of counts of number of black and white pixels that are being
   * searched for as a pattern
   * @param counters array of counters, as long as pattern, to re-use
   * @return start/end horizontal offset of guard pattern, as an array of two ints
   * @throws NotFoundException_S if pattern is not found
   */
  private static int[] findGuardPattern(BitArray_S row,
                                        int rowOffset,
                                        boolean whiteFirst,
                                        int[] pattern,
                                        int[] counters) throws NotFoundException_S {
    int width = row.getSize();
    rowOffset = whiteFirst ? row.getNextUnset(rowOffset) : row.getNextSet(rowOffset);
    int counterPosition = 0;
    int patternStart = rowOffset;
    int patternLength = pattern.length;
    boolean isWhite = whiteFirst;
    for (int x = rowOffset; x < width; x++) {
      if (row.get(x) != isWhite) {
        counters[counterPosition]++;
      } else {
        if (counterPosition == patternLength - 1) {
          if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
            return new int[]{patternStart, x};
          }
          patternStart += counters[0] + counters[1];
          System.arraycopy(counters, 2, counters, 0, counterPosition - 1);
          counters[counterPosition - 1] = 0;
          counters[counterPosition] = 0;
          counterPosition--;
        } else {
          counterPosition++;
        }
        counters[counterPosition] = 1;
        isWhite = !isWhite;
      }
    }
    throw NotFoundException_S.getNotFoundInstance();
  }

  /**
   * Attempts to decode a single UPC/EAN-encoded digit.
   *
   * @param row row of black/white values to decode
   * @param counters the counts of runs of observed black/white/black/... values
   * @param rowOffset horizontal offset to start decoding from
   * @param patterns the set of patterns to use to decode -- sometimes different encodings
   * for the digits 0-9 are used, and this indicates the encodings for 0 to 9 that should
   * be used
   * @return horizontal offset of first pixel beyond the decoded digit
   * @throws NotFoundException_S if digit cannot be decoded
   */
  static int decodeDigit(BitArray_S row, int[] counters, int rowOffset, int[][] patterns)
      throws NotFoundException_S {
    recordPattern(row, rowOffset, counters);
    float bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
    int bestMatch = -1;
    int max = patterns.length;
    for (int i = 0; i < max; i++) {
      int[] pattern = patterns[i];
      float variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
      if (variance < bestVariance) {
        bestVariance = variance;
        bestMatch = i;
      }
    }
    if (bestMatch >= 0) {
      return bestMatch;
    } else {
      throw NotFoundException_S.getNotFoundInstance();
    }
  }

  /**
   * Get the format of this decoder.
   *
   * @return The 1D format.
   */
  abstract BarcodeFormat_S getBarcodeFormat();

  /**
   * Subclasses override this to decode the portion of a barcode between the start
   * and end guard patterns.
   *
   * @param row row of black/white values to search
   * @param startRange start/end offset of start guard pattern
   * @param resultString {@link StringBuilder} to append decoded chars to
   * @return horizontal offset of first pixel after the "middle" that was decoded
   * @throws NotFoundException_S if decoding could not complete successfully
   */
  protected abstract int decodeMiddle(BitArray_S row,
                                      int[] startRange,
                                      StringBuilder resultString) throws NotFoundException_S;

}
