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

package com.google.myzxing.datamatrix.encoder;


import com.google.myzxing.Dimension_S;

import java.nio.charset.StandardCharsets;

final class EncoderContext_S {

  private final String msg;
  private SymbolShapeHint_S shape;
  private Dimension_S minSize;
  private Dimension_S maxSize;
  private final StringBuilder codewords;
  int pos;
  private int newEncoding;
  private SymbolInfo_S symbolInfo;
  private int skipAtEnd;

  EncoderContext_S(String msg) {
    //From this point on Strings are not Unicode anymore!
    byte[] msgBinary = msg.getBytes(StandardCharsets.ISO_8859_1);
    StringBuilder sb = new StringBuilder(msgBinary.length);
    for (int i = 0, c = msgBinary.length; i < c; i++) {
      char ch = (char) (msgBinary[i] & 0xff);
      if (ch == '?' && msg.charAt(i) != '?') {
        throw new IllegalArgumentException("Message contains characters outside ISO-8859-1 encoding.");
      }
      sb.append(ch);
    }
    this.msg = sb.toString(); //Not Unicode here!
    shape = SymbolShapeHint_S.FORCE_NONE;
    this.codewords = new StringBuilder(msg.length());
    newEncoding = -1;
  }

  public void setSymbolShape(SymbolShapeHint_S shape) {
    this.shape = shape;
  }

  public void setSizeConstraints(Dimension_S minSize, Dimension_S maxSize) {
    this.minSize = minSize;
    this.maxSize = maxSize;
  }

  public String getMessage() {
    return this.msg;
  }

  public void setSkipAtEnd(int count) {
    this.skipAtEnd = count;
  }

  public char getCurrentChar() {
    return msg.charAt(pos);
  }

  public char getCurrent() {
    return msg.charAt(pos);
  }

  public StringBuilder getCodewords() {
    return codewords;
  }

  public void writeCodewords(String codewords) {
    this.codewords.append(codewords);
  }

  public void writeCodeword(char codeword) {
    this.codewords.append(codeword);
  }

  public int getCodewordCount() {
    return this.codewords.length();
  }

  public int getNewEncoding() {
    return newEncoding;
  }

  public void signalEncoderChange(int encoding) {
    this.newEncoding = encoding;
  }

  public void resetEncoderSignal() {
    this.newEncoding = -1;
  }

  public boolean hasMoreCharacters() {
    return pos < getTotalMessageCharCount();
  }

  private int getTotalMessageCharCount() {
    return msg.length() - skipAtEnd;
  }

  public int getRemainingCharacters() {
    return getTotalMessageCharCount() - pos;
  }

  public SymbolInfo_S getSymbolInfo() {
    return symbolInfo;
  }

  public void updateSymbolInfo() {
    updateSymbolInfo(getCodewordCount());
  }

  public void updateSymbolInfo(int len) {
    if (this.symbolInfo == null || len > this.symbolInfo.getDataCapacity()) {
      this.symbolInfo = SymbolInfo_S.lookup(len, shape, minSize, maxSize, true);
    }
  }

  public void resetSymbolInfo() {
    this.symbolInfo = null;
  }
}
