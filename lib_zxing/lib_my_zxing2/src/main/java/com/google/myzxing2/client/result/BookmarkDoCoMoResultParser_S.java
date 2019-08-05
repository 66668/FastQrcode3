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

package com.google.myzxing2.client.result;


import com.google.myzxing2.Result_S;

/**
 * @author Sean Owen
 */
public final class BookmarkDoCoMoResultParser_S extends AbstractDoCoMoResultParser_S {

  @Override
  public com.google.myzxing2.client.result.URIParsedResult_S parse(Result_S result) {
    String rawText = result.getText();
    if (!rawText.startsWith("MEBKM:")) {
      return null;
    }
    String title = matchSingleDoCoMoPrefixedField("TITLE:", rawText, true);
    String[] rawUri = matchDoCoMoPrefixedField("URL:", rawText, true);
    if (rawUri == null) {
      return null;
    }
    String uri = rawUri[0];
    return com.google.myzxing2.client.result.URIResultParser_S.isBasicallyValidURI(uri) ? new com.google.myzxing2.client.result.URIParsedResult_S(uri, title) : null;
  }

}