package com.ruijia.qrcode.listener;

import android.graphics.Bitmap;

import java.util.List;
import java.util.Map;

/**
 * service向act发起调用
 */
public interface OnServiceAndActListener {
    void onQrsend(String selectPath, List<String> newData, long fileSize);
}
