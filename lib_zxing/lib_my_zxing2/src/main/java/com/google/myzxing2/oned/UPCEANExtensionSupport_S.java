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

package com.google.myzxing2.oned;

import com.google.myzxing2.NotFoundException_S;
import com.google.myzxing2.ReaderException_S;
import com.google.myzxing2.Result_S;
import com.google.myzxing2.common.BitArray_S;

final class UPCEANExtensionSupport_S {

  private static final int[] EXTENSION_START_PATTERN = {1,1,2};

  private final UPCEANExtension2Support_S twoSupport = new UPCEANExtension2Support_S();
  private final UPCEANExtension5Support_S fiveSupport = new UPCEANExtension5Support_S();

  Result_S decodeRow(int rowNumber, BitArray_S row, int rowOffset) throws NotFoundException_S {
    int[] extensionStartRange = com.google.myzxing2.oned.UPCEANReader_S.findGuardPattern(row, rowOffset, false, EXTENSION_START_PATTERN);
    try {
      return fiveSupport.decodeRow(rowNumber, row, extensionStartRange);
    } catch (ReaderException_S ignored) {
      return twoSupport.decodeRow(rowNumber, row, extensionStartRange);
    }
  }

}
