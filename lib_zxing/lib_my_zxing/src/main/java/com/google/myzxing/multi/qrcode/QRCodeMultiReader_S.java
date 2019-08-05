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

package com.google.myzxing.multi.qrcode;

import com.google.myzxing.BarcodeFormat_S;
import com.google.myzxing.BinaryBitmap_S;
import com.google.myzxing.DecodeHintType_S;
import com.google.myzxing.NotFoundException_S;
import com.google.myzxing.ReaderException_S;
import com.google.myzxing.Result_S;
import com.google.myzxing.ResultMetadataType_S;
import com.google.myzxing.ResultPoint_S;
import com.google.myzxing.common.DecoderResult_S;
import com.google.myzxing.common.DetectorResult_S;
import com.google.myzxing.multi.MultipleBarcodeReader_S;
import com.google.myzxing.multi.qrcode.detector.MultiDetector_S;
import com.google.myzxing.qrcode.QRCodeReader_S;
import com.google.myzxing.qrcode.decoder.QRCodeDecoderMetaData_S;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This implementation can detect and decode multiple QR Codes in an image.
 *
 * @author Sean Owen
 * @author Hannes Erven
 */
public final class QRCodeMultiReader_S extends QRCodeReader_S implements MultipleBarcodeReader_S {

  private static final Result_S[] EMPTY_RESULT_ARRAY = new Result_S[0];
  private static final ResultPoint_S[] NO_POINTS = new ResultPoint_S[0];

  @Override
  public Result_S[] decodeMultiple(BinaryBitmap_S image) throws NotFoundException_S {
    return decodeMultiple(image, null);
  }

  @Override
  public Result_S[] decodeMultiple(BinaryBitmap_S image, Map<DecodeHintType_S,?> hints) throws NotFoundException_S {
    List<Result_S> results = new ArrayList<>();
    DetectorResult_S[] detectorResults = new MultiDetector_S(image.getBlackMatrix()).detectMulti(hints);
    for (DetectorResult_S detectorResult : detectorResults) {
      try {
        DecoderResult_S decoderResult = getDecoder().decode(detectorResult.getBits(), hints);
        ResultPoint_S[] points = detectorResult.getPoints();
        // If the code was mirrored: swap the bottom-left and the top-right points.
        if (decoderResult.getOther() instanceof QRCodeDecoderMetaData_S) {
          ((QRCodeDecoderMetaData_S) decoderResult.getOther()).applyMirroredCorrection(points);
        }
        Result_S result = new Result_S(decoderResult.getText(), decoderResult.getRawBytes(), points,
                                   BarcodeFormat_S.QR_CODE);
        List<byte[]> byteSegments = decoderResult.getByteSegments();
        if (byteSegments != null) {
          result.putMetadata(ResultMetadataType_S.BYTE_SEGMENTS, byteSegments);
        }
        String ecLevel = decoderResult.getECLevel();
        if (ecLevel != null) {
          result.putMetadata(ResultMetadataType_S.ERROR_CORRECTION_LEVEL, ecLevel);
        }
        if (decoderResult.hasStructuredAppend()) {
          result.putMetadata(ResultMetadataType_S.STRUCTURED_APPEND_SEQUENCE,
                             decoderResult.getStructuredAppendSequenceNumber());
          result.putMetadata(ResultMetadataType_S.STRUCTURED_APPEND_PARITY,
                             decoderResult.getStructuredAppendParity());
        }
        results.add(result);
      } catch (ReaderException_S re) {
        // ignore and continue 
      }
    }
    if (results.isEmpty()) {
      return EMPTY_RESULT_ARRAY;
    } else {
      results = processStructuredAppend(results);
      return results.toArray(EMPTY_RESULT_ARRAY);
    }
  }

  private static List<Result_S> processStructuredAppend(List<Result_S> results) {
    boolean hasSA = false;

    // first, check, if there is at least on SA result in the list
    for (Result_S result : results) {
      if (result.getResultMetadata().containsKey(ResultMetadataType_S.STRUCTURED_APPEND_SEQUENCE)) {
        hasSA = true;
        break;
      }
    }
    if (!hasSA) {
      return results;
    }

    // it is, second, split the lists and built a new result list
    List<Result_S> newResults = new ArrayList<>();
    List<Result_S> saResults = new ArrayList<>();
    for (Result_S result : results) {
      newResults.add(result);
      if (result.getResultMetadata().containsKey(ResultMetadataType_S.STRUCTURED_APPEND_SEQUENCE)) {
        saResults.add(result);
      }
    }
    // sort and concatenate the SA list items
    Collections.sort(saResults, new SAComparator());
    StringBuilder concatedText = new StringBuilder();
    int rawBytesLen = 0;
    int byteSegmentLength = 0;
    for (Result_S saResult : saResults) {
      concatedText.append(saResult.getText());
      rawBytesLen += saResult.getRawBytes().length;
      if (saResult.getResultMetadata().containsKey(ResultMetadataType_S.BYTE_SEGMENTS)) {
        @SuppressWarnings("unchecked")
        Iterable<byte[]> byteSegments =
            (Iterable<byte[]>) saResult.getResultMetadata().get(ResultMetadataType_S.BYTE_SEGMENTS);
        for (byte[] segment : byteSegments) {
          byteSegmentLength += segment.length;
        }
      }
    }
    byte[] newRawBytes = new byte[rawBytesLen];
    byte[] newByteSegment = new byte[byteSegmentLength];
    int newRawBytesIndex = 0;
    int byteSegmentIndex = 0;
    for (Result_S saResult : saResults) {
      System.arraycopy(saResult.getRawBytes(), 0, newRawBytes, newRawBytesIndex, saResult.getRawBytes().length);
      newRawBytesIndex += saResult.getRawBytes().length;
      if (saResult.getResultMetadata().containsKey(ResultMetadataType_S.BYTE_SEGMENTS)) {
        @SuppressWarnings("unchecked")
        Iterable<byte[]> byteSegments =
            (Iterable<byte[]>) saResult.getResultMetadata().get(ResultMetadataType_S.BYTE_SEGMENTS);
        for (byte[] segment : byteSegments) {
          System.arraycopy(segment, 0, newByteSegment, byteSegmentIndex, segment.length);
          byteSegmentIndex += segment.length;
        }
      }
    }
    Result_S newResult = new Result_S(concatedText.toString(), newRawBytes, NO_POINTS, BarcodeFormat_S.QR_CODE);
    if (byteSegmentLength > 0) {
      Collection<byte[]> byteSegmentList = new ArrayList<>();
      byteSegmentList.add(newByteSegment);
      newResult.putMetadata(ResultMetadataType_S.BYTE_SEGMENTS, byteSegmentList);
    }
    newResults.add(newResult);
    return newResults;
  }

  private static final class SAComparator implements Comparator<Result_S>, Serializable {
    @Override
    public int compare(Result_S a, Result_S b) {
      int aNumber = (int) a.getResultMetadata().get(ResultMetadataType_S.STRUCTURED_APPEND_SEQUENCE);
      int bNumber = (int) b.getResultMetadata().get(ResultMetadataType_S.STRUCTURED_APPEND_SEQUENCE);
      return Integer.compare(aNumber, bNumber);
    }
  }

}
