package com.ruijia.qrcode.listener;

import java.util.List;

/**
 * service向act发起调用
 */
public interface OnServiceAndActListener {
    void onQrsend(String selectPath, List<String> newData, long fileSize);
}
