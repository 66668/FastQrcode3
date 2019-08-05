/*
 * Copyright 2010 ZXing authors
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

package com.google.myzxing2.aztec;


import com.google.myzxing2.BarcodeFormat_S;
import com.google.myzxing2.BinaryBitmap_S;
import com.google.myzxing2.DecodeHintType_S;
import com.google.myzxing2.FormatException_S;
import com.google.myzxing2.NotFoundException_S;
import com.google.myzxing2.Reader_S;
import com.google.myzxing2.ResultMetadataType_S;
import com.google.myzxing2.ResultPointCallback_S;
import com.google.myzxing2.ResultPoint_S;
import com.google.myzxing2.Result_S;
import com.google.myzxing2.aztec.decoder.Decoder_S;
import com.google.myzxing2.aztec.detector.Detector_S;
import com.google.myzxing2.common.DecoderResult_S;

import java.util.List;
import java.util.Map;

/**
 * This implementation can detect and decode Aztec codes in an image.
 *
 * @author David Olivier
 */
public final class AztecReader_S implements Reader_S {

    /**
     * Locates and decodes a Data Matrix code in an image.
     *
     * @return a String representing the content encoded by the Data Matrix code
     * @throws NotFoundException_S if a Data Matrix code cannot be found
     * @throws FormatException_S   if a Data Matrix code cannot be decoded
     */
    @Override
    public Result_S decode(BinaryBitmap_S image) throws NotFoundException_S, FormatException_S {
        return decode(image, null);
    }

    @Override
    public Result_S decode(BinaryBitmap_S image, Map<DecodeHintType_S, ?> hints)
            throws NotFoundException_S, FormatException_S {

        NotFoundException_S notFoundException = null;
        FormatException_S formatException = null;
        Detector_S detector = new Detector_S(image.getBlackMatrix());
        ResultPoint_S[] points = null;
        DecoderResult_S decoderResult = null;
        try {
            AztecDetectorResult_S detectorResult = detector.detect(false);
            points = detectorResult.getPoints();
            decoderResult = new Decoder_S().decode(detectorResult);
        } catch (NotFoundException_S e) {
            notFoundException = e;
        } catch (FormatException_S e) {
            formatException = e;
        }
        if (decoderResult == null) {
            try {
                AztecDetectorResult_S detectorResult = detector.detect(true);
                points = detectorResult.getPoints();
                decoderResult = new Decoder_S().decode(detectorResult);
            } catch (NotFoundException_S | FormatException_S e) {
                if (notFoundException != null) {
                    throw notFoundException;
                }
                if (formatException != null) {
                    throw formatException;
                }
                throw e;
            }
        }

        if (hints != null) {
            ResultPointCallback_S rpcb = (ResultPointCallback_S) hints.get(DecodeHintType_S.NEED_RESULT_POINT_CALLBACK);
            if (rpcb != null) {
                for (ResultPoint_S point : points) {
                    rpcb.foundPossibleResultPoint(point);
                }
            }
        }

        Result_S result = new Result_S(decoderResult.getText(),
                decoderResult.getRawBytes(),
                decoderResult.getNumBits(),
                points,
                BarcodeFormat_S.AZTEC,
                System.currentTimeMillis());

        List<byte[]> byteSegments = decoderResult.getByteSegments();
        if (byteSegments != null) {
            result.putMetadata(ResultMetadataType_S.BYTE_SEGMENTS, byteSegments);
        }
        String ecLevel = decoderResult.getECLevel();
        if (ecLevel != null) {
            result.putMetadata(ResultMetadataType_S.ERROR_CORRECTION_LEVEL, ecLevel);
        }

        return result;
    }

    @Override
    public void reset() {
        // do nothing
    }

}
