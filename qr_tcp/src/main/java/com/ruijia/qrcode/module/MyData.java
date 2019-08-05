package com.ruijia.qrcode.module;

import android.graphics.Bitmap;

/**
 * 存储一张片段的数据类
 */
public class MyData {
    public Bitmap bitmap;
    /**
     * 片段长度
     * 用于bitmap适应多大的ImageView尺寸
     */
    public int width;//
    public int position;

    public MyData(Bitmap bitmap, int width, int position) {
        this.bitmap = bitmap;
        this.width = width;
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
