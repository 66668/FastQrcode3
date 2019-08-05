package com.ruijia.string_b;

import android.app.Application;

public class MyApplication extends Application {
    private static MyApplication MyApplication;
    public static MyApplication getInstance() {
        return MyApplication;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication = this;
    }
}
