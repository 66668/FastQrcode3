package com.ruijia.string_a;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ruijia.qrcode.QrAIDLInterface;
import com.ruijia.qrcode.QrProgressCallback;
import com.ruijia.string_a.permission.PermissionHelper;
import com.ruijia.string_a.permission.PermissionInterface;


/**
 * 基类，封装aidl的调用
 */
public class ClientABaseAct extends AppCompatActivity {
    //权限相关
    private String[] permissionArray;
    PermissionHelper permissionHelper;
    //=================AIDL=================
    public QrAIDLInterface ibinder;//接口

    //=================变量=================
    public String selectPath = null;//文件路径/要搜索的String字符串（小于2900的长度）
    public int timeInterval = 150;//发送间隔
    public int maxSize = 5;//最大文件5M
    public int timeout = 15;//链路超时判断 默认15s

    //回调
    public QrProgressCallback callback = new QrProgressCallback.Stub() {
        @Override
        public void isTrans(boolean isSuccess, String msg) throws RemoteException {
            clientIsTrans(isSuccess, msg);
        }

        @Override
        public void splitToIoTime(long time, String msg) throws RemoteException {
            clientSplitToIoTime(time, msg);
        }

        @Override
        public void splitToArrayTime(long time, String msg) throws RemoteException {
            clientSplitToArrayTime(time, msg);
        }

        @Override
        public void createNewArrayTime(long time, String msg) throws RemoteException {
            clientCreateNewArrayTime(time, msg);
        }

        @Override
        public void createQrImgTime(long time, String msg) throws RemoteException {
            clientCreateQrImgTime(time, msg);
        }

        @Override
        public void createQrImgProgress(int total, int position, String msg) throws RemoteException {
            clientCreateQrImgProgress(total, position, msg);
        }

        @Override
        public void transProgress(long time, int total, int position, String msg) throws RemoteException {
            clientTransProgress(time, total, position, msg);
        }

        @Override
        public void transTime(long time, String msg) throws RemoteException {
            clientTransTime(time, msg);
        }

        @Override
        public void transComplete() throws RemoteException {
            clientTransComplete();
        }

        @Override
        public IBinder asBinder() {
            return super.asBinder();
        }
    };

    /**
     * ==================================================================================================================
     * ============================测试a与链路层的aild连接（链路层是服务端，测试a是客户端）====================================
     * ==================================================================================================================
     */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //连接服务端的binder
            ibinder = QrAIDLInterface.Stub.asInterface(service);

            //绑定回调
            try {
                ibinder.register(callback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            //设置死亡代理
            try {
                service.linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            ibinder = null;
        }
    };

    /**
     * 监听Binder是否死亡
     */
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (ibinder == null) {
                return;
            }
            ibinder.asBinder().unlinkToDeath(mDeathRecipient, 0);
            ibinder = null;
            //重新绑定
            bind();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bind();
        //必要权限
        permissionArray = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        toStartPermission();
    }

    /**
     * 设置aidl过滤，连接另一个app的service
     */
    public void bind() {
        Intent intent = new Intent();
        //设置要调用的app包和对应的service
        //方式1：
//        ComponentName componentName= new ComponentName("com.ruijia.qrcode","QRXmitService");
////        intent.setComponent(componentName);
        //方式2：
        //从 Android 5.0开始 隐式Intent绑定服务的方式已不能使用,所以这里需要设置Service所在服务端的包名
        intent.setPackage("com.ruijia.qrcode");//服务端的包名
        //通过intent-filter设置的name,找到这个service
//        intent.setAction("com.aidlservice.qrmodule");//过滤 （模组版）
        intent.setAction("com.aidlservice.qrcamera");//过滤(原生摄像头版)
        bindService(intent, connection, Context.BIND_AUTO_CREATE);//开启Service
        Log.d("SJY", "绑定链路层的服务");
    }

    public void unbind() {
        if (connection != null && ibinder != null && ibinder.asBinder().isBinderAlive()) {
            try {
                ibinder.unregister(callback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            unbindService(connection);
        } else {
            Log.e("SJY", "进程间通讯中断");
            //act销毁需要调用，不可以缺失
            unbindService(connection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbind();
    }

    //==============================================================================

    private void toStartPermission() {
        permissionHelper = new PermissionHelper(this, new PermissionInterface() {
            @Override
            public int getPermissionsRequestCode() {
                //设置权限请求requestCode，只有不跟onRequestPermissionsResult方法中的其他请求码冲突即可。
                return 10002;
            }

            @Override
            public String[] getPermissions() {
                //设置该界面所需的全部权限
                return permissionArray;
            }

            @Override
            public void requestPermissionsSuccess() {
                //权限请求用户已经全部允许
            }

            @Override
            public void requestPermissionsFail() {

            }

        });
        //发起调用：
        permissionHelper.requestPermissions();
    }

    //权限回调处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionHelper.requestPermissionsResult(requestCode, permissions, grantResults)) {
            //权限请求已处理，不用再处理
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    //子类act复写
    void clientIsTrans(boolean isSuccess, String msg) {

    }

    void clientSplitToIoTime(long time, String msg) {

    }

    void clientSplitToArrayTime(long time, String msg) {

    }

    void clientCreateNewArrayTime(long time, String msg) {

    }

    void clientCreateQrImgTime(long time, String msg) {

    }

    void clientCreateQrImgProgress(int total, int position, String msg) {

    }

    void clientTransProgress(long time, int total, int position, String msg) {

    }

    void clientTransTime(long time, String msg) {

    }

    void clientTransComplete() {


    }
}
