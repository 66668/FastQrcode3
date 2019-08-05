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

package com.google.myzxing3.multi.qrcode.detector;

import com.google.myzxing3.DecodeHintType_S;
import com.google.myzxing3.NotFoundException_S;
import com.google.myzxing3.ReaderException_S;
import com.google.myzxing3.ResultPointCallback_S;
import com.google.myzxing3.common.BitMatrix_S;
import com.google.myzxing3.common.DetectorResult_S;
import com.google.myzxing3.qrcode.detector.Detector_S;
import com.google.myzxing3.qrcode.detector.FinderPatternInfo_S;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Encapsulates logic that can detect one or more QR Codes in an image, even if the QR Code
 * is rotated or skewed, or partially obscured.</p>
 *
 * @author Sean Owen
 * @author Hannes Erven
 */
public final class MultiDetector_S extends Detector_S {

  private static final DetectorResult_S[] EMPTY_DETECTOR_RESULTS = new DetectorResult_S[0];

  public MultiDetector_S(BitMatrix_S image) {
    super(image);
  }

  public DetectorResult_S[] detectMulti(Map<DecodeHintType_S,?> hints) throws NotFoundException_S {
    BitMatrix_S image = getImage();
    ResultPointCallback_S resultPointCallback =
        hints == null ? null : (ResultPointCallback_S) hints.get(DecodeHintType_S.NEED_RESULT_POINT_CALLBACK);
    com.google.myzxing3.multi.qrcode.detector.MultiFinderPatternFinder_S finder = new com.google.myzxing3.multi.qrcode.detector.MultiFinderPatternFinder_S(image, resultPointCallback);
    FinderPatternInfo_S[] infos = finder.findMulti(hints);

    if (infos.length == 0) {
      throw NotFoundException_S.getNotFoundInstance();
    }

    List<DetectorResult_S> result = new ArrayList<>();
    for (FinderPatternInfo_S info : infos) {
      try {
        result.add(processFinderPatternInfo(info));
      } catch (ReaderException_S e) {
        // ignore
      }
    }
    if (result.isEmpty()) {
      return EMPTY_DETECTOR_RESULTS;
    } else {
      return result.toArray(EMPTY_DETECTOR_RESULTS);
    }
  }

}
