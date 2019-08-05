/*
 * Copyright 2012 ZXing authors
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

package com.google.myzxing3.pdf417.decoder.ec;

import com.google.myzxing3.common.reedsolomon.GenericGF_S;
import com.google.myzxing3.pdf417.PDF417Common_S;

/**
 * <p>A field based on powers of a generator integer, modulo some modulus.</p>
 *
 * @author Sean Owen
 * @see GenericGF_S
 */
public final class ModulusGF_S {

  public static final ModulusGF_S PDF417_GF = new ModulusGF_S(PDF417Common_S.NUMBER_OF_CODEWORDS, 3);

  private final int[] expTable;
  private final int[] logTable;
  private final com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S zero;
  private final com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S one;
  private final int modulus;

  private ModulusGF_S(int modulus, int generator) {
    this.modulus = modulus;
    expTable = new int[modulus];
    logTable = new int[modulus];
    int x = 1;
    for (int i = 0; i < modulus; i++) {
      expTable[i] = x;
      x = (x * generator) % modulus;
    }
    for (int i = 0; i < modulus - 1; i++) {
      logTable[expTable[i]] = i;
    }
    // logTable[0] == 0 but this should never be used
    zero = new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S(this, new int[]{0});
    one = new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S(this, new int[]{1});
  }


  com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S getZero() {
    return zero;
  }

  com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S getOne() {
    return one;
  }

  com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S buildMonomial(int degree, int coefficient) {
    if (degree < 0) {
      throw new IllegalArgumentException();
    }
    if (coefficient == 0) {
      return zero;
    }
    int[] coefficients = new int[degree + 1];
    coefficients[0] = coefficient;
    return new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S(this, coefficients);
  }

  int add(int a, int b) {
    return (a + b) % modulus;
  }

  int subtract(int a, int b) {
    return (modulus + a - b) % modulus;
  }

  int exp(int a) {
    return expTable[a];
  }

  int log(int a) {
    if (a == 0) {
      throw new IllegalArgumentException();
    }
    return logTable[a];
  }

  int inverse(int a) {
    if (a == 0) {
      throw new ArithmeticException();
    }
    return expTable[modulus - logTable[a] - 1];
  }

  int multiply(int a, int b) {
    if (a == 0 || b == 0) {
      return 0;
    }
    return expTable[(logTable[a] + logTable[b]) % (modulus - 1)];
  }

  int getSize() {
    return modulus;
  }

}
