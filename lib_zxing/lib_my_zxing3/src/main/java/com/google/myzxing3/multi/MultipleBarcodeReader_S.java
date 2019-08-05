/*
 * Copyright 2009 ZXing authors
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

package com.google.myzxing3.multi;

import com.google.myzxing3.BinaryBitmap_S;
import com.google.myzxing3.DecodeHintType_S;
import com.google.myzxing3.NotFoundException_S;
import com.google.myzxing3.Reader_S;
import com.google.myzxing3.Result_S;

import java.util.Map;

/**
 * Implementation of this interface attempt to read several barcodes from one image.
 *
 * @see Reader_S
 * @author Sean Owen
 */
public interface MultipleBarcodeReader_S {

  Result_S[] decodeMultiple(BinaryBitmap_S image) throws NotFoundException_S;

  Result_S[] decodeMultiple(BinaryBitmap_S image,
                            Map<DecodeHintType_S, ?> hints) throws NotFoundException_S;

}
