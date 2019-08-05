/*
 * Copyright 2006-2007 Jeremias Maerki.
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

package com.google.myzxing2.datamatrix.encoder;

final class ASCIIEncoder_S implements Encoder_S {

  @Override
  public int getEncodingMode() {
    return com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.ASCII_ENCODATION;
  }

  @Override
  public void encode(com.google.myzxing2.datamatrix.encoder.EncoderContext_S context) {
    //step B
    int n = com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.determineConsecutiveDigitCount(context.getMessage(), context.pos);
    if (n >= 2) {
      context.writeCodeword(encodeASCIIDigits(context.getMessage().charAt(context.pos),
                                              context.getMessage().charAt(context.pos + 1)));
      context.pos += 2;
    } else {
      char c = context.getCurrentChar();
      int newMode = com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.lookAheadTest(context.getMessage(), context.pos, getEncodingMode());
      if (newMode != getEncodingMode()) {
        switch (newMode) {
          case com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.BASE256_ENCODATION:
            context.writeCodeword(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.LATCH_TO_BASE256);
            context.signalEncoderChange(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.BASE256_ENCODATION);
            return;
          case com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.C40_ENCODATION:
            context.writeCodeword(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.LATCH_TO_C40);
            context.signalEncoderChange(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.C40_ENCODATION);
            return;
          case com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.X12_ENCODATION:
            context.writeCodeword(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.LATCH_TO_ANSIX12);
            context.signalEncoderChange(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.X12_ENCODATION);
            break;
          case com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.TEXT_ENCODATION:
            context.writeCodeword(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.LATCH_TO_TEXT);
            context.signalEncoderChange(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.TEXT_ENCODATION);
            break;
          case com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.EDIFACT_ENCODATION:
            context.writeCodeword(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.LATCH_TO_EDIFACT);
            context.signalEncoderChange(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.EDIFACT_ENCODATION);
            break;
          default:
            throw new IllegalStateException("Illegal mode: " + newMode);
        }
      } else if (com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.isExtendedASCII(c)) {
        context.writeCodeword(com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.UPPER_SHIFT);
        context.writeCodeword((char) (c - 128 + 1));
        context.pos++;
      } else {
        context.writeCodeword((char) (c + 1));
        context.pos++;
      }

    }
  }

  private static char encodeASCIIDigits(char digit1, char digit2) {
    if (com.google.myzxing2.datamatrix.encoder.HighLevelEncoder_S.isDigit(digit1) && HighLevelEncoder_S.isDigit(digit2)) {
      int num = (digit1 - 48) * 10 + (digit2 - 48);
      return (char) (num + 130);
    }
    throw new IllegalArgumentException("not digits: " + digit1 + digit2);
  }

}
