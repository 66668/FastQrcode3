/*
 * Copyright (C) 2010 ZXing authors
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

/*
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", led by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 */

package com.google.myzxing2.oned.rss.expanded.decoders;

import com.google.myzxing2.NotFoundException_S;
import com.google.myzxing2.common.BitArray_S;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class AI013x0x1xDecoder_S extends AI01weightDecoder_S {

  private static final int HEADER_SIZE = 7 + 1;
  private static final int WEIGHT_SIZE = 20;
  private static final int DATE_SIZE = 16;

  private final String dateCode;
  private final String firstAIdigits;

  AI013x0x1xDecoder_S(BitArray_S information, String firstAIdigits, String dateCode) {
    super(information);
    this.dateCode = dateCode;
    this.firstAIdigits = firstAIdigits;
  }

  @Override
  public String parseInformation() throws NotFoundException_S {
    if (this.getInformation().getSize() != HEADER_SIZE + GTIN_SIZE + WEIGHT_SIZE + DATE_SIZE) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    StringBuilder buf = new StringBuilder();

    encodeCompressedGtin(buf, HEADER_SIZE);
    encodeCompressedWeight(buf, HEADER_SIZE + GTIN_SIZE, WEIGHT_SIZE);
    encodeCompressedDate(buf, HEADER_SIZE + GTIN_SIZE + WEIGHT_SIZE);

    return buf.toString();
  }

  private void encodeCompressedDate(StringBuilder buf, int currentPos) {
    int numericDate = this.getGeneralDecoder().extractNumericValueFromBitArray(currentPos, DATE_SIZE);
    if (numericDate == 38400) {
      return;
    }

    buf.append('(');
    buf.append(this.dateCode);
    buf.append(')');

    int day   = numericDate % 32;
    numericDate /= 32;
    int month = numericDate % 12 + 1;
    numericDate /= 12;
    int year  = numericDate;

    if (year / 10 == 0) {
      buf.append('0');
    }
    buf.append(year);
    if (month / 10 == 0) {
      buf.append('0');
    }
    buf.append(month);
    if (day / 10 == 0) {
      buf.append('0');
    }
    buf.append(day);
  }

  @Override
  protected void addWeightCode(StringBuilder buf, int weight) {
    buf.append('(');
    buf.append(this.firstAIdigits);
    buf.append(weight / 100000);
    buf.append(')');
  }

  @Override
  protected int checkWeight(int weight) {
    return weight % 100000;
  }
}
