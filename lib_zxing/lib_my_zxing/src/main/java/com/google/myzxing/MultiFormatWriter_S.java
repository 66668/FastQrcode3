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

package com.google.myzxing;

import com.google.myzxing.aztec.AztecWriter_S;
import com.google.myzxing.common.BitMatrix_S;
import com.google.myzxing.datamatrix.DataMatrixWriter_S;
import com.google.myzxing.oned.CodaBarWriter_S;
import com.google.myzxing.oned.Code128Writer_S;
import com.google.myzxing.oned.Code39Writer_S;
import com.google.myzxing.oned.Code93Writer_S;
import com.google.myzxing.oned.EAN13Writer_S;
import com.google.myzxing.oned.EAN8Writer_S;
import com.google.myzxing.oned.ITFWriter_S;
import com.google.myzxing.oned.UPCAWriter_S;
import com.google.myzxing.oned.UPCEWriter_S;
import com.google.myzxing.pdf417.PDF417Writer_S;
import com.google.myzxing.qrcode.QRCodeWriter_S;

import java.util.Map;

/**
 * This is a factory class which finds the appropriate Writer subclass for the BarcodeFormat
 * requested and encodes the barcode with the supplied contents.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class MultiFormatWriter_S implements Writer_S {

  @Override
  public BitMatrix_S encode(String contents,
                            BarcodeFormat_S format,
                            int width,
                            int height) throws WriterException_S {
    return encode(contents, format, width, height, null);
  }

  @Override
  public BitMatrix_S encode(String contents,
                            BarcodeFormat_S format,
                            int width, int height,
                            Map<EncodeHintType_S,?> hints) throws WriterException_S {

    Writer_S writer;
    switch (format) {
      case EAN_8:
        writer = new EAN8Writer_S();
        break;
      case UPC_E:
        writer = new UPCEWriter_S();
        break;
      case EAN_13:
        writer = new EAN13Writer_S();
        break;
      case UPC_A:
        writer = new UPCAWriter_S();
        break;
      case QR_CODE:
        writer = new QRCodeWriter_S();
        break;
      case CODE_39:
        writer = new Code39Writer_S();
        break;
      case CODE_93:
        writer = new Code93Writer_S();
        break;
      case CODE_128:
        writer = new Code128Writer_S();
        break;
      case ITF:
        writer = new ITFWriter_S();
        break;
      case PDF_417:
        writer = new PDF417Writer_S();
        break;
      case CODABAR:
        writer = new CodaBarWriter_S();
        break;
      case DATA_MATRIX:
        writer = new DataMatrixWriter_S();
        break;
      case AZTEC:
        writer = new AztecWriter_S();
        break;
      default:
        throw new IllegalArgumentException("No encoder available for format " + format);
    }
    return writer.encode(contents, format, width, height, hints);
  }

}
