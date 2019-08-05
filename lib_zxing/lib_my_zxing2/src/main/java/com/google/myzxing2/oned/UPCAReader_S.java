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

package com.google.myzxing2.oned;

import com.google.myzxing2.BarcodeFormat_S;
import com.google.myzxing2.BinaryBitmap_S;
import com.google.myzxing2.ChecksumException_S;
import com.google.myzxing2.DecodeHintType_S;
import com.google.myzxing2.FormatException_S;
import com.google.myzxing2.NotFoundException_S;
import com.google.myzxing2.Result_S;
import com.google.myzxing2.common.BitArray_S;

import java.util.Map;

/**
 * <p>Implements decoding of the UPC-A format.</p>
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class UPCAReader_S extends com.google.myzxing2.oned.UPCEANReader_S {

  private final com.google.myzxing2.oned.UPCEANReader_S ean13Reader = new EAN13Reader_S();

  @Override
  public Result_S decodeRow(int rowNumber,
                            BitArray_S row,
                            int[] startGuardRange,
                            Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, FormatException_S, ChecksumException_S {
    return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, startGuardRange, hints));
  }

  @Override
  public Result_S decodeRow(int rowNumber, BitArray_S row, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, FormatException_S, ChecksumException_S {
    return maybeReturnResult(ean13Reader.decodeRow(rowNumber, row, hints));
  }

  @Override
  public Result_S decode(BinaryBitmap_S image) throws NotFoundException_S, FormatException_S {
    return maybeReturnResult(ean13Reader.decode(image));
  }

  @Override
  public Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints)
      throws NotFoundException_S, FormatException_S {
    return maybeReturnResult(ean13Reader.decode(image, hints));
  }

  @Override
  BarcodeFormat_S getBarcodeFormat() {
    return BarcodeFormat_S.UPC_A;
  }

  @Override
  protected int decodeMiddle(BitArray_S row, int[] startRange, StringBuilder resultString)
      throws NotFoundException_S {
    return ean13Reader.decodeMiddle(row, startRange, resultString);
  }

  private static Result_S maybeReturnResult(Result_S result) throws FormatException_S {
    String text = result.getText();
    if (text.charAt(0) == '0') {
      Result_S upcaResult = new Result_S(text.substring(1), null, result.getResultPoints(), BarcodeFormat_S.UPC_A);
      if (result.getResultMetadata() != null) {
        upcaResult.putAllMetadata(result.getResultMetadata());
      }
      return upcaResult;
    } else {
      throw FormatException_S.getFormatInstance();
    }
  }

}
