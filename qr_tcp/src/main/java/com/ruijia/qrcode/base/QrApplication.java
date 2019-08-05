package com.ruijia.qrcode.base;

import android.app.Application;

public class QrApplication extends Application {
    private static QrApplication MyApplication;
    public static QrApplication getInstance() {
        return MyApplication;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication = this;
    }
}
