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
import com.google.myzxing3.DecodeHintType_S;
import com.google.myzxing3.NotFoundException_S;
import com.google.myzxing3.Reader_S;
import com.google.myzxing3.ReaderException_S;
import com.google.myzxing3.Result_S;
import com.google.myzxing3.common.BitArray_S;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * <p>A reader that can read all available UPC/EAN formats. If a caller wants to try to
 * read all such formats, it is most efficient to use this implementation rather than invoke
 * individual readers.</p>
 *
 * @author Sean Owen
 */
public final class MultiFormatUPCEANReader_S extends com.google.myzxing3.oned.OneDReader_S {

  private static final com.google.myzxing3.oned.UPCEANReader_S[] EMPTY_READER_ARRAY = new com.google.myzxing3.oned.UPCEANReader_S[0];

  private final com.google.myzxing3.oned.UPCEANReader_S[] readers;

  public MultiFormatUPCEANReader_S(Map<DecodeHintType_S,?> hints) {
    @SuppressWarnings("unchecked")
    Collection<BarcodeFormat_S> possibleFormats = hints == null ? null :
        (Collection<BarcodeFormat_S>) hints.get(DecodeHintType_S.POSSIBLE_FORMATS);
    Collection<com.google.myzxing3.oned.UPCEANReader_S> readers = new ArrayList<>();
    if (possibleFormats != null) {
      if (possibleFormats.contains(BarcodeFormat_S.EAN_13)) {
        readers.add(new EAN13Reader_S());
      } else if (possibleFormats.contains(BarcodeFormat_S.UPC_A)) {
        readers.add(new com.google.myzxing3.oned.UPCAReader_S());
      }
      if (possibleFormats.contains(BarcodeFormat_S.EAN_8)) {
        readers.add(new EAN8Reader_S());
      }
      if (possibleFormats.contains(BarcodeFormat_S.UPC_E)) {
        readers.add(new com.google.myzxing3.oned.UPCEReader_S());
      }
    }
    if (readers.isEmpty()) {
      readers.add(new EAN13Reader_S());
      // UPC-A is covered by EAN-13
      readers.add(new EAN8Reader_S());
      readers.add(new com.google.myzxing3.oned.UPCEReader_S());
    }
    this.readers = readers.toArray(EMPTY_READER_ARRAY);
  }

  @Override
  public Result_S decodeRow(int rowNumber,
                            BitArray_S row,
                            Map<DecodeHintType_S,?> hints) throws NotFoundException_S {
    // Compute this location once and reuse it on multiple implementations
    int[] startGuardPattern = com.google.myzxing3.oned.UPCEANReader_S.findStartGuardPattern(row);
    for (com.google.myzxing3.oned.UPCEANReader_S reader : readers) {
      try {
        Result_S result = reader.decodeRow(rowNumber, row, startGuardPattern, hints);
        // Special case: a 12-digit code encoded in UPC-A is identical to a "0"
        // followed by those 12 digits encoded as EAN-13. Each will recognize such a code,
        // UPC-A as a 12-digit string_a and EAN-13 as a 13-digit string_a starting with "0".
        // Individually these are correct and their readers will both read such a code
        // and correctly call it EAN-13, or UPC-A, respectively.
        //
        // In this case, if we've been looking for both types, we'd like to call it
        // a UPC-A code. But for efficiency we only run the EAN-13 decoder to also read
        // UPC-A. So we special case it here, and convert an EAN-13 result to a UPC-A
        // result if appropriate.
        //
        // But, don't return UPC-A if UPC-A was not a requested format!
        boolean ean13MayBeUPCA =
            result.getBarcodeFormat() == BarcodeFormat_S.EAN_13 &&
                result.getText().charAt(0) == '0';
        @SuppressWarnings("unchecked")
        Collection<BarcodeFormat_S> possibleFormats =
            hints == null ? null : (Collection<BarcodeFormat_S>) hints.get(DecodeHintType_S.POSSIBLE_FORMATS);
        boolean canReturnUPCA = possibleFormats == null || possibleFormats.contains(BarcodeFormat_S.UPC_A);
  
        if (ean13MayBeUPCA && canReturnUPCA) {
          // Transfer the metdata across
          Result_S resultUPCA = new Result_S(result.getText().substring(1),
                                         result.getRawBytes(),
                                         result.getResultPoints(),
                                         BarcodeFormat_S.UPC_A);
          resultUPCA.putAllMetadata(result.getResultMetadata());
          return resultUPCA;
        }
        return result;
      } catch (ReaderException_S ignored) {
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
