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

package com.google.myzxing.client.result;


import com.google.myzxing.BarcodeFormat_S;
import com.google.myzxing.Result_S;
import com.google.myzxing.oned.UPCEReader_S;

/**
 * Parses strings of digits that represent a UPC code.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ProductResultParser_S extends ResultParser_S {

  // Treat all UPC and EAN variants as UPCs, in the sense that they are all product barcodes.
  @Override
  public ProductParsedResult_S parse(Result_S result) {
    BarcodeFormat_S format = result.getBarcodeFormat();
    if (!(format == BarcodeFormat_S.UPC_A || format == BarcodeFormat_S.UPC_E ||
          format == BarcodeFormat_S.EAN_8 || format == BarcodeFormat_S.EAN_13)) {
      return null;
    }
    String rawText = getMassagedText(result);
    if (!isStringOfDigits(rawText, rawText.length())) {
      return null;
    }
    // Not actually checking the checksum again here    

    String normalizedProductID;
    // Expand UPC-E for purposes of searching
    if (format == BarcodeFormat_S.UPC_E && rawText.length() == 8) {
      normalizedProductID = UPCEReader_S.convertUPCEtoUPCA(rawText);
    } else {
      normalizedProductID = rawText;
    }

    return new ProductParsedResult_S(rawText, normalizedProductID);
  }

}