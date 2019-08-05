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

package com.google.myzxing2;

/**
 * Thrown when a barcode was successfully detected and decoded, but
 * was not returned because its checksum feature failed.
 *
 * @author Sean Owen
 */
public final class ChecksumException_S extends com.google.myzxing2.ReaderException_S {

  private static final ChecksumException_S INSTANCE = new ChecksumException_S();
  static {
    INSTANCE.setStackTrace(NO_TRACE); // since it's meaningless
  }

  private ChecksumException_S() {
    // do nothing
  }

  private ChecksumException_S(Throwable cause) {
    super(cause);
  }

  public static ChecksumException_S getChecksumInstance() {
    return isStackTrace ? new ChecksumException_S() : INSTANCE;
  }

  public static ChecksumException_S getChecksumInstance(Throwable cause) {
    return isStackTrace ? new ChecksumException_S(cause) : INSTANCE;
  }
}