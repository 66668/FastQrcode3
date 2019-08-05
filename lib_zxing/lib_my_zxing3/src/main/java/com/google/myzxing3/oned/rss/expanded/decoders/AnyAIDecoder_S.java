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

package com.google.myzxing3.oned.rss.expanded.decoders;

import com.google.myzxing3.FormatException_S;
import com.google.myzxing3.NotFoundException_S;
import com.google.myzxing3.common.BitArray_S;

/**
 * @author Pablo Orduña, University of Deusto (pablo.orduna@deusto.es)
 * @author Eduardo Castillejo, University of Deusto (eduardo.castillejo@deusto.es)
 */
final class AnyAIDecoder_S extends AbstractExpandedDecoder_S {

  private static final int HEADER_SIZE = 2 + 1 + 2;

  AnyAIDecoder_S(BitArray_S information) {
    super(information);
  }

  @Override
  public String parseInformation() throws NotFoundException_S, FormatException_S {
    StringBuilder buf = new StringBuilder();
    return this.getGeneralDecoder().decodeAllCodes(buf, HEADER_SIZE);
  }
}
