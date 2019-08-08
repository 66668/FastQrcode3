package com.ruijia.qrcode.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.ruijia.qrcode.QrAIDLInterface;
import com.ruijia.qrcode.QrProgressCallback;
import com.ruijia.qrcode.base.QrApplication;
import com.ruijia.qrcode.listener.OnServiceAndActListener;
import com.ruijia.qrcode.utils.CacheUtils;
import com.ruijia.qrcode.utils.CheckUtils;
import com.ruijia.qrcode.utils.CodeUtils;
import com.ruijia.qrcode.utils.Constants;
import com.ruijia.qrcode.utils.ConvertUtils;
import com.ruijia.qrcode.utils.SPUtil;
import com.ruijia.qrcode.utils.ViewUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * aidl服务端，链路层的service,给aidl客户端提供service接口
 */
public class QRXmitService extends Service {
    public static final String TAG = "SJY";
    //---------------------------变量-------------------------------
    private Handler handler;

    //---------------------------变量-------------------------------
    private AtomicBoolean isServiceDestory = new AtomicBoolean(false);
    //RemoteCallbackList是专门用于删除跨进程listener的接口，它是一个泛型，支持管理多个回调。
    private RemoteCallbackList<QrProgressCallback> mListener = new RemoteCallbackList<>();
    private String searchStr;//当前传输的文件/搜索的字符串
    private OnServiceAndActListener listener;//
    private List<String> newDatas = new ArrayList<>();

    /**
     * 客户端开启连接后，自动执行
     */
    public QRXmitService() {
        handler = new Handler();
        //设置默认发送时间间隔
        SPUtil.putInt(Constants.TIME_INTERVAL, Constants.DEFAULT_TIME);
        //默认文件大小
        SPUtil.putInt(Constants.FILE_SIZE, Constants.DEFAULT_SIZE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //通过ServiceConnection在activity中拿到Binder对象
        return new QrAIDLServiceBinder();
    }


    //==========================================================================================================================
    //=================================以下为app 进程间的交互，包括客户端调用服务端，服务端回调客户端===================================
    //==========================================================================================================================


    //---------------------------------------------AIDL接口实现--------------------------------------------

    /**
     * 接口方法，由service实现
     */
    public class QrAIDLServiceBinder extends QrAIDLInterface.Stub {

        //act与service交互使用
        public QRXmitService geSerVice() {
            return QRXmitService.this;
        }

        //aidl使用
        @Override
        public void QRSend(String localPath) throws RemoteException {
            srvQrSend(localPath);
        }

        @Override
        public String QRRecv() throws RemoteException {
            return srvQRRecv();
        }

        @Override
        public boolean QrCtrl(int timeInterval, int StrLen) throws RemoteException {
            return srvQRCtrl(timeInterval, StrLen);
        }

        @Override
        public void register(QrProgressCallback listener) throws RemoteException {
            //绑定
            mListener.register(listener);
        }

        @Override
        public void unregister(QrProgressCallback listener) throws RemoteException {
            //解除
            mListener.unregister(listener);
        }
    }


    @Override
    public void onDestroy() {
        isServiceDestory.set(true);
        super.onDestroy();
    }


    //-----------------------《客户端-->服务端》操作（不同进程）----------------------

    /**
     * 设置参数
     */
    public boolean srvQRCtrl(int timeInterval, int fileSize) {
        Log.d(TAG, "服务端设置参数-QRCtrl--timeInterval=" + timeInterval + "--fileSize=" + fileSize);

        SPUtil.putInt(Constants.TIME_INTERVAL, timeInterval);
        SPUtil.putInt(Constants.FILE_SIZE, fileSize);
        SPUtil.putInt(Constants.CON_TIME_OUT, 20);//TODO

        return (SPUtil.getInt(Constants.TIME_INTERVAL, 0) != 0
                && SPUtil.getInt(Constants.FILE_SIZE, 0) != 0
                && SPUtil.getInt(Constants.CON_TIME_OUT, 0) != 0);
    }

    /**
     * 接收到进程的数据，处理数据
     *
     * @param searchStr
     */
    public void srvQrSend(String searchStr) {
        Log.d("SJY", "QRXmitService--QrSend-搜索=" + searchStr);
        //
        clearLastData();

        //判断string是否为空
        if (TextUtils.isEmpty(searchStr)) {
            isTrans(false, "搜索不存在");
        } else {
            //保存时间节点，用于统计传输总耗时
            SPUtil.putLong(Constants.START_TIME, System.currentTimeMillis());
            this.searchStr = searchStr;
            serviceStartAct();
        }
    }

    /**
     *
     */
    public String srvQRRecv() {
        return "请参考回调";
    }

    /**
     * <p>
     * 清空处理
     */
    private void clearLastData() {
        searchStr = "";//当前传输的文件
        newDatas = new ArrayList<>();

        //清空上一次传输的缓存
        File bitmapFile = new File(Constants.BASE_PATH, Constants.FILE_BITMAP_NAME);
        if (bitmapFile.exists() && bitmapFile.isFile()) {
            bitmapFile.delete();
        }
    }

    //-----------------------《服务端-->客户端》回调（不同进程）----------------------

    /**
     * 回调客户端
     * <p>
     * 文件是否可以传输
     * <p>
     * 请安这个步骤操作
     */
    public void isTrans(final boolean isSuccess, final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener == null) {
                    return;
                }
                //检查注册的回调个数
                final int N = mListener.beginBroadcast();//成对出现
                //遍历出要用的callback
                try {
                    for (int i = 0; i < N; i++) {
                        QrProgressCallback callback = mListener.getBroadcastItem(i);
                        //处理回调
                        callback.isTrans(isSuccess, msg);
                        mListener.finishBroadcast();//成对出现2
                        // 解绑callback
//                        mListener.unregister(callback);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        //false情况下，基本宣告这次文件传输是失败的，所以清空所有数据
        if (!isSuccess) {
            clearLastData();
        }

    }

    /**
     * 回调客户端
     * <p>
     * 传输进度
     * <p>
     * 请安这个步骤操作
     *
     * @param total
     * @param msg
     */
    public void qrTransProgress(final long time, final int total, final int position, final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener == null) {
                    return;
                }
                //检查注册的回调个数
                final int N = mListener.beginBroadcast();
                //遍历出要用的callback
                try {
                    for (int i = 0; i < N; i++) {
                        QrProgressCallback callback = mListener.getBroadcastItem(i);
                        //处理回调
                        callback.transProgress(time, total, position, msg);
                        mListener.finishBroadcast();//成对出现2
                        // 解绑callback
//                        mListener.unregister(callback);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * 回调客户端
     * <p>
     * 二维码传输耗时统计
     * <p>
     * 请安这个步骤操作
     *
     * @param time
     * @param msg
     */
    public void transcomplete(final long time, final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener == null) {
                    return;
                }
                //检查注册的回调个数
                final int N = mListener.beginBroadcast();
                //遍历出要用的callback
                try {
                    for (int i = 0; i < N; i++) {
                        QrProgressCallback callback = mListener.getBroadcastItem(i);
                        //处理回调
                        callback.transTime(time, msg);
                        callback.transComplete();
                        mListener.finishBroadcast();//成对出现2
                        // 解绑callback
//                        mListener.unregister(callback);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //===========================================================================================================================
    //=================================以下为同一进程下，act与service的交互：包括service回调act,act回调service===================================
    //===========================================================================================================================

    //service与act通讯有两种方式1接口，2广播。本demo使用接口。

    /**
     * 由service调起act
     */
    private void serviceStartAct() {
        if (checkActAlive() && isActFrontShow()) {
            isTrans(true, "链路层已打开");
            //接口回调
            if (listener != null) {
                // 字符串查询，不用 selectPath
                listener.onQrsend(searchStr, null, 1);
            } else {
                isTrans(false, "链路层未启动：回调监听为空");
            }

        } else {
            isTrans(true, "打开链路层");
            startApp();
        }
    }

    private boolean checkActAlive() {
        return CheckUtils.isActivityAlive(QrApplication.getInstance(), "com.ruijia.qrcode", "MainAct");
    }

    private boolean isActFrontShow() {
        return CheckUtils.isActFrontShow(QrApplication.getInstance(), "com.ruijia.qrcode.MainAct");
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

    /**
     * act调用，回调给另一个app
     * 识别完成的回调
     */
    public void setAidlQrCodeComplete(long time, String result) {
        transcomplete(time, result);
    }

    /**
     * act调用，发送端发送一条二维码实际耗时
     */
    public void sendAidlQrUnitTime(long successTime, int size, int pos, String msg) {
        qrTransProgress(successTime, size, pos, msg);
    }


    /**
     * 设置回调
     *
     * @param listener
     */
    public void setListener(OnServiceAndActListener listener) {
        this.listener = listener;
    }

    /**
     * act的service连接完成后，通知service回调act，将数据传给act
     */
    public void startServiceTrans() {
        //
        //接口回调
        if (listener != null) {
            // 字符串查询，不用 selectPath
            listener.onQrsend(searchStr, null, 1);
        } else {
            isTrans(false, "链路层未启动，回调无法使用listener=null");
        }
    }
}
