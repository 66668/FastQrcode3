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

package com.google.myzxing3;

import com.google.myzxing3.aztec.AztecReader_S;
import com.google.myzxing3.datamatrix.DataMatrixReader_S;
import com.google.myzxing3.maxicode.MaxiCodeReader_S;
import com.google.myzxing3.oned.MultiFormatOneDReader_S;
import com.google.myzxing3.pdf417.PDF417Reader_S;
import com.google.myzxing3.qrcode.QRCodeReader_S;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * MultiFormatReader is a convenience class and the main entry point into the library for most uses.
 * By default it attempts to decode all barcode formats that the library supports. Optionally, you
 * can provide a hints object to request different behavior, for example only decoding QR codes.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class MultiFormatReader_S implements Reader_S {

  private static final Reader_S[] EMPTY_READER_ARRAY = new Reader_S[0];

  private Map<DecodeHintType_S,?> hints;
  private Reader_S[] readers;

  /**
   * This version of decode honors the intent of Reader.decode(BinaryBitmap) in that it
   * passes null as a hint to the decoders. However, that makes it inefficient to call repeatedly.
   * Use setHints() followed by decodeWithState() for continuous scan applications.
   *
   * @param image The pixel data to decode
   * @return The contents of the image
   * @throws NotFoundException_S Any errors which occurred
   */
  @Override
  public Result_S decode(BinaryBitmap_S image) throws NotFoundException_S {
    setHints(null);
    return decodeInternal(image);
  }

  /**
   * Decode an image using the hints provided. Does not honor existing state.
   *
   * @param image The pixel data to decode
   * @param hints The hints to use, clearing the previous state.
   * @return The contents of the image
   * @throws NotFoundException_S Any errors which occurred
   */
  @Override
  public Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints) throws NotFoundException_S {
    setHints(hints);
    return decodeInternal(image);
  }

  /**
   * Decode an image using the state set up by calling setHints() previously. Continuous scan
   * clients will get a <b>large</b> speed increase by using this instead of decode().
   *
   * @param image The pixel data to decode
   * @return The contents of the image
   * @throws NotFoundException_S Any errors which occurred
   */
  public Result_S decodeWithState(BinaryBitmap_S image) throws NotFoundException_S {
    // Make sure to set up the default state so we don't crash
    if (readers == null) {
      setHints(null);
    }
    return decodeInternal(image);
  }

  /**
   * This method adds state to the MultiFormatReader. By setting the hints once, subsequent calls
   * to decodeWithState(image) can reuse the same set of readers without reallocating memory. This
   * is important for performance in continuous scan clients.
   *
   * @param hints The set of hints to use for subsequent calls to decode(image)
   */
  public void setHints(Map<DecodeHintType_S,?> hints) {
    this.hints = hints;

    boolean tryHarder = hints != null && hints.containsKey(DecodeHintType_S.TRY_HARDER);
    @SuppressWarnings("unchecked")
    Collection<BarcodeFormat_S> formats =
        hints == null ? null : (Collection<BarcodeFormat_S>) hints.get(DecodeHintType_S.POSSIBLE_FORMATS);
    Collection<Reader_S> readers = new ArrayList<>();
    if (formats != null) {
      boolean addOneDReader =
          formats.contains(BarcodeFormat_S.UPC_A) ||
          formats.contains(BarcodeFormat_S.UPC_E) ||
          formats.contains(BarcodeFormat_S.EAN_13) ||
          formats.contains(BarcodeFormat_S.EAN_8) ||
          formats.contains(BarcodeFormat_S.CODABAR) ||
          formats.contains(BarcodeFormat_S.CODE_39) ||
          formats.contains(BarcodeFormat_S.CODE_93) ||
          formats.contains(BarcodeFormat_S.CODE_128) ||
          formats.contains(BarcodeFormat_S.ITF) ||
          formats.contains(BarcodeFormat_S.RSS_14) ||
          formats.contains(BarcodeFormat_S.RSS_EXPANDED);
      // Put 1D readers upfront in "normal" mode
      if (addOneDReader && !tryHarder) {
        readers.add(new MultiFormatOneDReader_S(hints));
      }
      if (formats.contains(BarcodeFormat_S.QR_CODE)) {
        readers.add(new QRCodeReader_S());
      }
      if (formats.contains(BarcodeFormat_S.DATA_MATRIX)) {
        readers.add(new DataMatrixReader_S());
      }
      if (formats.contains(BarcodeFormat_S.AZTEC)) {
        readers.add(new AztecReader_S());
      }
      if (formats.contains(BarcodeFormat_S.PDF_417)) {
         readers.add(new PDF417Reader_S());
      }
      if (formats.contains(BarcodeFormat_S.MAXICODE)) {
         readers.add(new MaxiCodeReader_S());
      }
      // At end in "try harder" mode
      if (addOneDReader && tryHarder) {
        readers.add(new MultiFormatOneDReader_S(hints));
      }
    }
    if (readers.isEmpty()) {
      if (!tryHarder) {
        readers.add(new MultiFormatOneDReader_S(hints));
      }

      readers.add(new QRCodeReader_S());
      readers.add(new DataMatrixReader_S());
      readers.add(new AztecReader_S());
      readers.add(new PDF417Reader_S());
      readers.add(new MaxiCodeReader_S());

      if (tryHarder) {
        readers.add(new MultiFormatOneDReader_S(hints));
      }
    }
    this.readers = readers.toArray(EMPTY_READER_ARRAY);
  }

  @Override
  public void reset() {
    if (readers != null) {
      for (Reader_S reader : readers) {
        reader.reset();
      }
    }
  }

  private Result_S decodeInternal(BinaryBitmap_S image) throws NotFoundException_S {
    if (readers != null) {
      for (Reader_S reader : readers) {
        try {
          return reader.decode(image, hints);
        } catch (ReaderException_S re) {
          // continue
        }
      }
    }
    throw NotFoundException_S.getNotFoundInstance();
  }

}
