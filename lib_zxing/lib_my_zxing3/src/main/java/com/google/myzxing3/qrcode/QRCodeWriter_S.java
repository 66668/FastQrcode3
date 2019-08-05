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

package com.google.myzxing3.qrcode;

import com.google.myzxing3.BarcodeFormat_S;
import com.google.myzxing3.EncodeHintType_S;
import com.google.myzxing3.Writer_S;
import com.google.myzxing3.WriterException_S;
import com.google.myzxing3.common.BitMatrix_S;
import com.google.myzxing3.qrcode.decoder.ErrorCorrectionLevel_S;
import com.google.myzxing3.qrcode.encoder.ByteMatrix_S;
import com.google.myzxing3.qrcode.encoder.Encoder_S;
import com.google.myzxing3.qrcode.encoder.QRCode_S;

import java.util.Map;

/**
 * This object renders a QR Code as a BitMatrix 2D array of greyscale values.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class QRCodeWriter_S implements Writer_S {

    private static final int QUIET_ZONE_SIZE = 4;

    @Override
    public BitMatrix_S encode(String contents, BarcodeFormat_S format, int width, int height)
            throws WriterException_S {

        return encode(contents, format, width, height, null);
    }

    @Override
    public BitMatrix_S encode(String contents,
                              BarcodeFormat_S format,
                              int width,
                              int height,
                              Map<EncodeHintType_S, ?> hints) throws WriterException_S {

        if (contents.isEmpty()) {
            throw new IllegalArgumentException("Found empty contents");
        }

        if (format != BarcodeFormat_S.QR_CODE) {
            throw new IllegalArgumentException("Can only encode QR_CODE, but got " + format);
        }

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Requested dimensions are too small: " + width + 'x' +
                    height);
        }

        ErrorCorrectionLevel_S errorCorrectionLevel = ErrorCorrectionLevel_S.L;
        int quietZone = QUIET_ZONE_SIZE;
        if (hints != null) {
            if (hints.containsKey(EncodeHintType_S.ERROR_CORRECTION)) {
                errorCorrectionLevel = ErrorCorrectionLevel_S.valueOf(hints.get(EncodeHintType_S.ERROR_CORRECTION).toString());
            }
            if (hints.containsKey(EncodeHintType_S.MARGIN)) {
                quietZone = Integer.parseInt(hints.get(EncodeHintType_S.MARGIN).toString());
            }
        }

        QRCode_S code = Encoder_S.encode(contents, errorCorrectionLevel, hints);
        return renderResult(code, width, height, quietZone);
    }

    //TODO 修改源代码 去除白边 /已修改 sjy
    // Note that the input matrix uses 0 == white, 1 == black, while the output matrix uses
    // 0 == black, 255 == white (i.e. an 8 bit greyscale bitmap).
    private static BitMatrix_S renderResult(QRCode_S code, int width, int height, int quietZone) {
        ByteMatrix_S input = code.getMatrix();
        if (input == null) {
            throw new IllegalStateException();
        }
        int inputWidth = input.getWidth();
        int inputHeight = input.getHeight();
        //----------------------------开始：修改位置--------------------------------------
//        int qrWidth = inputWidth + (quietZone * 2);
//        int qrHeight = inputHeight + (quietZone * 2);

        //修改--》去掉间距
        int qrWidth = inputWidth;
        int qrHeight = inputHeight;
        //----------------------------结束：修改位置--------------------------------------
        int outputWidth = Math.max(width, qrWidth);
        int outputHeight = Math.max(height, qrHeight);

        int multiple = Math.min(outputWidth / qrWidth, outputHeight / qrHeight);
        // Padding includes both the quiet zone and the extra white pixels to accommodate the requested
        // dimensions. For example, if input is 25x25 the QR will be 33x33 including the quiet zone.
        // If the requested size is 200x160, the multiple will be 4, for a QR of 132x132. These will
        // handle all the padding from 100x100 (the actual QR) up to 200x160.
        int leftPadding = (outputWidth - (inputWidth * multiple)) / 2;
        int topPadding = (outputHeight - (inputHeight * multiple)) / 2;

        BitMatrix_S output = new BitMatrix_S(outputWidth, outputHeight);

        for (int inputY = 0, outputY = topPadding; inputY < inputHeight; inputY++, outputY += multiple) {
            // Write the contents of this row of the barcode
            for (int inputX = 0, outputX = leftPadding; inputX < inputWidth; inputX++, outputX += multiple) {
                if (input.get(inputX, inputY) == 1) {
                    output.setRegion(outputX, outputY, multiple, multiple);
                }
            }
        }

        return output;
    }

}
