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

package com.google.myzxing3.client.result;

/**
 * Represents a parsed result that encodes a product ISBN number.
 *
 * @author jbreiden@google.com (Jeff Breidenbach)
 */
public final class ISBNParsedResult_S extends com.google.myzxing3.client.result.ParsedResult_S {

  private final String isbn;

  ISBNParsedResult_S(String isbn) {
    super(com.google.myzxing3.client.result.ParsedResultType_S.ISBN);
    this.isbn = isbn;
  }

  public String getISBN() {
    return isbn;
  }

  @Override
  public String getDisplayResult() {
    return isbn;
  }

}
