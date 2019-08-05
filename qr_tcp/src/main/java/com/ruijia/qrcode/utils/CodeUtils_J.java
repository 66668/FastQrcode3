package com.ruijia.qrcode.utils;

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import com.google.myzxing2.BarcodeFormat_S;
import com.google.myzxing2.EncodeHintType_S;
import com.google.myzxing2.MultiFormatWriter_S;
import com.google.myzxing2.common.BitMatrix_S;
import com.google.myzxing2.qrcode.QRCodeWriter_S;
import com.google.myzxing2.qrcode.decoder.ErrorCorrectionLevel_S;

import java.util.Hashtable;

/**
 * 本地zxing库：lib_my_zxing2 生成二维码的工具类,
 * 说明：本app使用了三个zing库用于生成二维码图片，该工具类对应的本地库是：lib_my_zxing2
 */
public class CodeUtils_J {

    /**
     * MultiFormatWriter 类
     *
     * @param content
     * @param size    size很重要，对于连续识别功能，size关乎图片的清晰度，但是最大值200，所以设置200即可
     * @return
     */
    public static Bitmap createByMultiFormatWriter(String content, int size) {
        if (content == null || TextUtils.isEmpty(content) || content.length() > Constants.MAX_QR_SIZE) {
            return null;
        }
        Hashtable<EncodeHintType_S, Object> hints = new Hashtable<EncodeHintType_S, Object>();
        // 指定纠错等级 请参考：https://github.com/66668/FastQrDemo
        hints.put(EncodeHintType_S.ERROR_CORRECTION, ErrorCorrectionLevel_S.L);
        /**
         *
         *
         *设置 version, version用于控制二维码的尺寸
         * 二维码一共有40个尺寸,官方叫版本Version。
         *Version 1是21 x 21的矩阵，Version 2是 25 x 25的矩阵，Version 3是29的尺寸，每增加一个version，就会增加4的尺寸，
         *公式是：(V-1)4 + 21（V是版本号）
         *最高Version 40，(40-1)4+21 = 177，所以最高是177 x 177 的正方形
         *version一般不要设置, zxing会自动选择何时的version（确实不用设置，设置也没用，也不会扩大容量--sjy）
         *eg: hints.put(EncodeHintType.QR_VERSION, 40);
         */

        // 指定编码格式
        hints.put(EncodeHintType_S.CHARACTER_SET, "UTF-8");
//        hints.put(EncodeHintType.MARGIN, 0);   //设置白边
        try {
            BitMatrix_S matrix = new MultiFormatWriter_S().encode(content, BarcodeFormat_S.QR_CODE, size, size, hints);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (matrix.get(x, y)) {
                        pixels[y * size + x] = 0xff000000;//黑
                    } else {
                        pixels[y * size + x] = 0xffffffff;//白
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SJY", "生成qrcode异常=" + e.toString());
            return null;
        }

    }


    /**
     * QRCodeWriter 类(核心同上没有区别)
     *
     * @param content
     * @param size
     * @return
     */

    public static Bitmap createByQRCodeWriter(String content, int size) {
        if (content == null || TextUtils.isEmpty(content) || content.length() > Constants.MAX_QR_SIZE) {
            return null;
        }
        Hashtable<EncodeHintType_S, Object> hints = new Hashtable<EncodeHintType_S, Object>();
        // 指定纠错等级 请参考：https://github.com/66668/FastQrDemo
        hints.put(EncodeHintType_S.ERROR_CORRECTION, ErrorCorrectionLevel_S.L);
        // 指定编码格式
        hints.put(EncodeHintType_S.CHARACTER_SET, "UTF-8");
//        hints.put(EncodeHintType.MARGIN, 0);   //设置白边
        try {
            BitMatrix_S matrix = new QRCodeWriter_S().encode(content, BarcodeFormat_S.QR_CODE, size, size, hints);
            int[] pixels = new int[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (matrix.get(x, y)) {
                        pixels[y * size + x] = 0xff000000;//黑
                    } else {
                        pixels[y * size + x] = 0xffffffff;//白
                    }
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("SJY", "生成qrcode异常=" + e.toString());
            return null;
        }
    }


}
