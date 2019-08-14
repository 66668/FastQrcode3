package com.ruijia.qrcode.utils;

import android.view.View;
import android.view.ViewGroup;

/**
 * MainAct调用的方法,重构出来，方便管理
 */
public class ViewUtils {
    /**
     * 每次发送二维码前，适配显示大小，识别更快
     *
     * @param contentSize
     */
    public static void setImageViewWidth(int contentSize, View view) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        int width = 1000;
        if (contentSize >= 1000) {
            width = 1000;
        } else if (contentSize > 600) {
            width = 900;
        } else if (contentSize > 500) {
            width = 800;
        } else if (contentSize > 450) {
            width = 750;
        } else if (contentSize > 300) {
            width = 670;
        } else if (contentSize > 150) {
            width = 600;
        } else if (contentSize > 100) {
            width = 550;
        } else if (contentSize > 50) {
            width = 500;
        } else if (contentSize > 30) {
            width = 450;
        } else {
            width = 400;
        }
        if (lp.width == width) {
            return;
        }
        lp.width = width;
        lp.height = width;
        view.setLayoutParams(lp);

    }

    /**
     * @param contentSize 文件长度决定ImageView的大小
     */
    public static int getImageViewWidth(int contentSize) {
        if (contentSize >= 1000) {
            return 1000;
        } else if (contentSize > 600) {
            return 900;
        } else if (contentSize > 500) {
            return 800;
        } else if (contentSize > 450) {
            return 750;
        } else if (contentSize > 300) {
            return 670;
        } else if (contentSize > 150) {
            return 600;
        } else if (contentSize > 100) {
            return 550;
        } else if (contentSize > 50) {
            return 500;
        } else if (contentSize > 30) {
            return 450;
        } else {
            return 400;
        }
    }


}
