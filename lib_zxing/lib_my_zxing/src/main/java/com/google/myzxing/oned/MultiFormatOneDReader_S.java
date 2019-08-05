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

package com.google.myzxing.oned;

import com.google.myzxing.BarcodeFormat_S;
import com.google.myzxing.DecodeHintType_S;
import com.google.myzxing.NotFoundException_S;
import com.google.myzxing.Reader_S;
import com.google.myzxing.ReaderException_S;
import com.google.myzxing.Result_S;
import com.google.myzxing.common.BitArray_S;
import com.google.myzxing.oned.rss.RSS14Reader_S;
import com.google.myzxing.oned.rss.expanded.RSSExpandedReader_S;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class MultiFormatOneDReader_S extends OneDReader_S {

  private static final OneDReader_S[] EMPTY_ONED_ARRAY = new OneDReader_S[0];

  private final OneDReader_S[] readers;

  public MultiFormatOneDReader_S(Map<DecodeHintType_S,?> hints) {
    @SuppressWarnings("unchecked")
    Collection<BarcodeFormat_S> possibleFormats = hints == null ? null :
        (Collection<BarcodeFormat_S>) hints.get(DecodeHintType_S.POSSIBLE_FORMATS);
    boolean useCode39CheckDigit = hints != null &&
        hints.get(DecodeHintType_S.ASSUME_CODE_39_CHECK_DIGIT) != null;
    Collection<OneDReader_S> readers = new ArrayList<>();
    if (possibleFormats != null) {
      if (possibleFormats.contains(BarcodeFormat_S.EAN_13) ||
          possibleFormats.contains(BarcodeFormat_S.UPC_A) ||
          possibleFormats.contains(BarcodeFormat_S.EAN_8) ||
          possibleFormats.contains(BarcodeFormat_S.UPC_E)) {
        readers.add(new MultiFormatUPCEANReader_S(hints));
      }
      if (possibleFormats.contains(BarcodeFormat_S.CODE_39)) {
        readers.add(new Code39Reader_S(useCode39CheckDigit));
      }
      if (possibleFormats.contains(BarcodeFormat_S.CODE_93)) {
        readers.add(new Code93Reader_S());
      }
      if (possibleFormats.contains(BarcodeFormat_S.CODE_128)) {
        readers.add(new Code128Reader_S());
      }
      if (possibleFormats.contains(BarcodeFormat_S.ITF)) {
         readers.add(new ITFReader_S());
      }
      if (possibleFormats.contains(BarcodeFormat_S.CODABAR)) {
         readers.add(new CodaBarReader_S());
      }
      if (possibleFormats.contains(BarcodeFormat_S.RSS_14)) {
         readers.add(new RSS14Reader_S());
      }
      if (possibleFormats.contains(BarcodeFormat_S.RSS_EXPANDED)) {
        readers.add(new RSSExpandedReader_S());
      }
    }
    if (readers.isEmpty()) {
      readers.add(new MultiFormatUPCEANReader_S(hints));
      readers.add(new Code39Reader_S());
      readers.add(new CodaBarReader_S());
      readers.add(new Code93Reader_S());
      readers.add(new Code128Reader_S());
      readers.add(new ITFReader_S());
      readers.add(new RSS14Reader_S());
      readers.add(new RSSExpandedReader_S());
    }
    this.readers = readers.toArray(EMPTY_ONED_ARRAY);
  }

  @Override
  public Result_S decodeRow(int rowNumber,
                            BitArray_S row,
                            Map<DecodeHintType_S,?> hints) throws NotFoundException_S {
    for (OneDReader_S reader : readers) {
      try {
        return reader.decodeRow(rowNumber, row, hints);
      } catch (ReaderException_S re) {
        // continue
      }
    }

    throw NotFoundException_S.getNotFoundInstance();
  }

  @Override
  public void reset() {
    for (Reader_S reader : readers) {
      reader.reset();
    }
  }

}
