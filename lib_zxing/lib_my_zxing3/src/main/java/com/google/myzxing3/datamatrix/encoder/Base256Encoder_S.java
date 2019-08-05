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

package com.google.myzxing3.datamatrix.encoder;

final class Base256Encoder_S implements Encoder_S {

  @Override
  public int getEncodingMode() {
    return com.google.myzxing3.datamatrix.encoder.HighLevelEncoder_S.BASE256_ENCODATION;
  }

  @Override
  public void encode(com.google.myzxing3.datamatrix.encoder.EncoderContext_S context) {
    StringBuilder buffer = new StringBuilder();
    buffer.append('\0'); //Initialize length field
    while (context.hasMoreCharacters()) {
      char c = context.getCurrentChar();
      buffer.append(c);

      context.pos++;

      int newMode = com.google.myzxing3.datamatrix.encoder.HighLevelEncoder_S.lookAheadTest(context.getMessage(), context.pos, getEncodingMode());
      if (newMode != getEncodingMode()) {
        // Return to ASCII encodation, which will actually handle latch to new mode
        context.signalEncoderChange(com.google.myzxing3.datamatrix.encoder.HighLevelEncoder_S.ASCII_ENCODATION);
        break;
      }
    }
    int dataCount = buffer.length() - 1;
    int lengthFieldSize = 1;
    int currentSize = context.getCodewordCount() + dataCount + lengthFieldSize;
    context.updateSymbolInfo(currentSize);
    boolean mustPad = (context.getSymbolInfo().getDataCapacity() - currentSize) > 0;
    if (context.hasMoreCharacters() || mustPad) {
      if (dataCount <= 249) {
        buffer.setCharAt(0, (char) dataCount);
      } else if (dataCount <= 1555) {
        buffer.setCharAt(0, (char) ((dataCount / 250) + 249));
        buffer.insert(1, (char) (dataCount % 250));
      } else {
        throw new IllegalStateException(
            "Message length not in valid ranges: " + dataCount);
      }
    }
    for (int i = 0, c = buffer.length(); i < c; i++) {
      context.writeCodeword(randomize255State(
          buffer.charAt(i), context.getCodewordCount() + 1));
    }
  }

  private static char randomize255State(char ch, int codewordPosition) {
    int pseudoRandom = ((149 * codewordPosition) % 255) + 1;
    int tempVariable = ch + pseudoRandom;
    if (tempVariable <= 255) {
      return (char) tempVariable;
    } else {
      return (char) (tempVariable - 256);
    }
  }

}
