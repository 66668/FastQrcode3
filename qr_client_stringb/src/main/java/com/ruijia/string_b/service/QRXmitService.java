package com.ruijia.string_b.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.ruijia.string_b.FileAidlInterface;
import com.ruijia.string_b.MyApplication;
import com.ruijia.string_b.SearchResultCallback;
import com.ruijia.string_b.database.DatabaseSQL;
import com.ruijia.string_b.listener.OnServiceAndActListener;
import com.ruijia.string_b.utils.CheckUtils;


/**
 * aidl服务端,测试b的service,给链路层提供service接口，
 * <p>
 * 链路层与测试b通讯的service（单向通讯，链路层是客户端--->测试b是服务端）
 */
public class QRXmitService extends Service {
    private String filepath;
    private RemoteCallbackList<SearchResultCallback> mListener = new RemoteCallbackList<>();
    private OnServiceAndActListener listener;
    private Handler handler;
    private DatabaseSQL sql;

    /**
     * 客户端开启连接后，自动执行
     */
    public QRXmitService() {
        handler = new Handler();
        sql = new DatabaseSQL(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new FileAIDLServiceBinder();
    }

    //---------------------------------------------AIDL接口实现--------------------------------------------

    public class FileAIDLServiceBinder extends FileAidlInterface.Stub {
        //act与service交互使用
        public QRXmitService geSerVice() {
            return QRXmitService.this;
        }

        //拿到客户端信息，测试b使用该数据。
        @Override
        public boolean QRRecv(final String searchData) throws RemoteException {
            Log.d("SJY", "测试B软件--" + searchData);
            //查询数据库，获取数据，返回
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //查询数据库：
                    String val = sql.findByKey(searchData);

                    //回调数据给客户端(不调起b的界面)
                    if (mListener == null) {
                        return;
                    }
                    //检查注册的回调个数
                    final int N = mListener.beginBroadcast();//成对出现1
                    //遍历出要用的callback
                    try {
                        for (int i = 0; i < N; i++) {
                            SearchResultCallback callback = mListener.getBroadcastItem(i);
                            //处理回调
                            if (TextUtils.isEmpty(val)) {
                                Log.d("SJY", "查询数据库--没有对应数据");
                                callback.searchResult(0, "没有对应数据");
                            } else {
                                Log.d("SJY", "查询数据库--" + val);
                                callback.searchResult(1, val);
                            }
                            mListener.finishBroadcast();//成对出现2

                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }

        @Override
        public void register(SearchResultCallback listener) throws RemoteException {
            //绑定
            mListener.register(listener);
        }

        @Override
        public void unregister(SearchResultCallback listener) throws RemoteException {
            //解除
            mListener.unregister(listener);
        }
    }

    //===========================================================================================================================
    //=================================以下为同一进程下，act与service的交互：包括service回调act,act回调service===================================
    //===========================================================================================================================

    /**
     *
     */
    private void startToAct() {
//        if (checkActAlive() && isActFrontShow()) {
        if (listener != null) {
            listener.onQrRecv(filepath);
        }
//        } else {
//            Log.d("SJY", "MainAct不在前台，正在开启");
//            startApp();
//        }
    }

    /**
     * 设置回调
     *
     * @param listener
     */
    public void setListener(OnServiceAndActListener listener) {
        this.listener = listener;
    }

    private boolean checkActAlive() {
        return CheckUtils.isActivityAlive(MyApplication.getInstance(), "com.ruijia.string_b", "MainActivity");
    }

    private boolean isActFrontShow() {
        return CheckUtils.isActFrontShow(MyApplication.getInstance(), "com.ruijia.string_b.MainActivity");
    }

    /**
     * 由service调起app的act界面
     * 由于intent传值 不能传大数据，所以使用接口回调方式。
     */
    private void startApp() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                //启动应用，参数为需要自动启动的应用的包名
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.ruijia.qrcode");
                startActivity(launchIntent);
            }
        });
    }

}