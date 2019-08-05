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

import com.google.myzxing3.ChecksumException_S;
import com.google.myzxing3.common.reedsolomon.ReedSolomonDecoder_S;

/**
 * <p>PDF417 error correction implementation.</p>
 *
 * <p>This <a href="http://en.wikipedia.org/wiki/Reed%E2%80%93Solomon_error_correction#Example">example</a>
 * is quite useful in understanding the algorithm.</p>
 *
 * @author Sean Owen
 * @see ReedSolomonDecoder_S
 */
public final class ErrorCorrection_S {

  private final com.google.myzxing3.pdf417.decoder.ec.ModulusGF_S field;

  public ErrorCorrection_S() {
    this.field = com.google.myzxing3.pdf417.decoder.ec.ModulusGF_S.PDF417_GF;
  }

  /**
   * @param received received codewords
   * @param numECCodewords number of those codewords used for EC
   * @param erasures location of erasures
   * @return number of errors
   * @throws ChecksumException_S if errors cannot be corrected, maybe because of too many errors
   */
  public int decode(int[] received,
                    int numECCodewords,
                    int[] erasures) throws ChecksumException_S {

    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S poly = new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S(field, received);
    int[] S = new int[numECCodewords];
    boolean error = false;
    for (int i = numECCodewords; i > 0; i--) {
      int eval = poly.evaluateAt(field.exp(i));
      S[numECCodewords - i] = eval;
      if (eval != 0) {
        error = true;
      }
    }

    if (!error) {
      return 0;
    }

    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S knownErrors = field.getOne();
    if (erasures != null) {
      for (int erasure : erasures) {
        int b = field.exp(received.length - 1 - erasure);
        // Add (1 - bx) term:
        com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S term = new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S(field, new int[]{field.subtract(0, b), 1});
        knownErrors = knownErrors.multiply(term);
      }
    }

    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S syndrome = new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S(field, S);
    //syndrome = syndrome.multiply(knownErrors);

    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S[] sigmaOmega =
        runEuclideanAlgorithm(field.buildMonomial(numECCodewords, 1), syndrome, numECCodewords);
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S sigma = sigmaOmega[0];
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S omega = sigmaOmega[1];

    //sigma = sigma.multiply(knownErrors);

    int[] errorLocations = findErrorLocations(sigma);
    int[] errorMagnitudes = findErrorMagnitudes(omega, sigma, errorLocations);

    for (int i = 0; i < errorLocations.length; i++) {
      int position = received.length - 1 - field.log(errorLocations[i]);
      if (position < 0) {
        throw ChecksumException_S.getChecksumInstance();
      }
      received[position] = field.subtract(received[position], errorMagnitudes[i]);
    }
    return errorLocations.length;
  }

  private com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S[] runEuclideanAlgorithm(com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S a, com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S b, int R)
      throws ChecksumException_S {
    // Assume a's degree is >= b's
    if (a.getDegree() < b.getDegree()) {
      com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S temp = a;
      a = b;
      b = temp;
    }

    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S rLast = a;
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S r = b;
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S tLast = field.getZero();
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S t = field.getOne();

    // Run Euclidean algorithm until r's degree is less than R/2
    while (r.getDegree() >= R / 2) {
      com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S rLastLast = rLast;
      com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S tLastLast = tLast;
      rLast = r;
      tLast = t;

      // Divide rLastLast by rLast, with quotient in q and remainder in r
      if (rLast.isZero()) {
        // Oops, Euclidean algorithm already terminated?
        throw ChecksumException_S.getChecksumInstance();
      }
      r = rLastLast;
      com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S q = field.getZero();
      int denominatorLeadingTerm = rLast.getCoefficient(rLast.getDegree());
      int dltInverse = field.inverse(denominatorLeadingTerm);
      while (r.getDegree() >= rLast.getDegree() && !r.isZero()) {
        int degreeDiff = r.getDegree() - rLast.getDegree();
        int scale = field.multiply(r.getCoefficient(r.getDegree()), dltInverse);
        q = q.add(field.buildMonomial(degreeDiff, scale));
        r = r.subtract(rLast.multiplyByMonomial(degreeDiff, scale));
      }

      t = q.multiply(tLast).subtract(tLastLast).negative();
    }

    int sigmaTildeAtZero = t.getCoefficient(0);
    if (sigmaTildeAtZero == 0) {
      throw ChecksumException_S.getChecksumInstance();
    }

    int inverse = field.inverse(sigmaTildeAtZero);
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S sigma = t.multiply(inverse);
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S omega = r.multiply(inverse);
    return new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S[]{sigma, omega};
  }

  private int[] findErrorLocations(com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S errorLocator) throws ChecksumException_S {
    // This is a direct application of Chien's search
    int numErrors = errorLocator.getDegree();
    int[] result = new int[numErrors];
    int e = 0;
    for (int i = 1; i < field.getSize() && e < numErrors; i++) {
      if (errorLocator.evaluateAt(i) == 0) {
        result[e] = field.inverse(i);
        e++;
      }
    }
    if (e != numErrors) {
      throw ChecksumException_S.getChecksumInstance();
    }
    return result;
  }

  private int[] findErrorMagnitudes(com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S errorEvaluator,
                                    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S errorLocator,
                                    int[] errorLocations) {
    int errorLocatorDegree = errorLocator.getDegree();
    int[] formalDerivativeCoefficients = new int[errorLocatorDegree];
    for (int i = 1; i <= errorLocatorDegree; i++) {
      formalDerivativeCoefficients[errorLocatorDegree - i] =
          field.multiply(i, errorLocator.getCoefficient(i));
    }
    com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S formalDerivative = new com.google.myzxing3.pdf417.decoder.ec.ModulusPoly_S(field, formalDerivativeCoefficients);

    // This is directly applying Forney's Formula
    int s = errorLocations.length;
    int[] result = new int[s];
    for (int i = 0; i < s; i++) {
      int xiInverse = field.inverse(errorLocations[i]);
      int numerator = field.subtract(0, errorEvaluator.evaluateAt(xiInverse));
      int denominator = field.inverse(formalDerivative.evaluateAt(xiInverse));
      result[i] = field.multiply(numerator, denominator);
    }
    return result;
  }
}
