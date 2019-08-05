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

/*
 * These authors would like to acknowledge the Spanish Ministry of Industry,
 * Tourism and Trade, for the support in the project TSI020301-2008-2
 * "PIRAmIDE: Personalizable Interactions with Resources on AmI-enabled
 * Mobile Dynamic Environments", led by Treelogic
 * ( http://www.treelogic.com/ ):
 *
 *   http://www.piramidepse.com/
 */

package com.google.myzxing2.oned.rss.expanded.decoders;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class BlockParsedResult_S {

  private final com.google.myzxing2.oned.rss.expanded.decoders.DecodedInformation_S decodedInformation;
  private final boolean finished;

  BlockParsedResult_S(boolean finished) {
    this(null, finished);
  }

  BlockParsedResult_S(com.google.myzxing2.oned.rss.expanded.decoders.DecodedInformation_S information, boolean finished) {
    this.finished = finished;
    this.decodedInformation = information;
  }

  com.google.myzxing2.oned.rss.expanded.decoders.DecodedInformation_S getDecodedInformation() {
    return this.decodedInformation;
  }

  boolean isFinished() {
    return this.finished;
  }
}
