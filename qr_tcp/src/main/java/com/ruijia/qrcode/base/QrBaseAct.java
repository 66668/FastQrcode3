package com.ruijia.qrcode.base;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ruijia.qrcode.persmission.PermissionHelper;
import com.ruijia.qrcode.persmission.PermissionInterface;
import com.ruijia.string_b.FileAidlInterface;
import com.ruijia.string_b.SearchResultCallback;

public class QrBaseAct extends AppCompatActivity {
    public static final String TAG = "QrBaseAct";
    //权限相关
    String[] permissionArray;
    PermissionHelper permissionHelper;


    /**
     * 测试B与链路层aidl
     */
//    public FileAidlInterface fileBinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        //必要权限
        permissionArray = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        toStartPermission();
        //
        bind();
    }

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

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbind();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    /**
     * ==================================================================================================================
     * ============================测试b与链路层的aild连接（链路层是客户端，测试b是服务端）====================================
     * ==================================================================================================================
     */
//    private ServiceConnection connection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            //连接服务端的binder
//            fileBinder = FileAidlInterface.Stub.asInterface(service);
//            //绑定回调
//            try {
//                fileBinder.register(callback);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//            //设置死亡代理
//            try {
//                service.linkToDeath(mDeathRecipient, 0);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            fileBinder = null;
//        }
//    };


//    /**
//     * 监听Binder是否死亡
//     */
//    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
//        @Override
//        public void binderDied() {
//            if (fileBinder == null) {
//                return;
//            }
//            fileBinder.asBinder().unlinkToDeath(mDeathRecipient, 0);
//            fileBinder = null;
//            //重新绑定
//            bind();
//        }
//    };

    /**
     * 设置aidl过滤，连接另一个app的service
     */
    public void bind() {
//        Intent intent = new Intent();
//        //设置要调用的app包和对应的service
//        //方式1：
////        ComponentName componentName= new ComponentName("com.ruijia.qrcode","QRXmitService");
//////        intent.setComponent(componentName);
//        //方式2：
//        //从 Android 5.0开始 隐式Intent绑定服务的方式已不能使用,所以这里需要设置Service所在服务端的包名
//        intent.setPackage("com.ruijia.string_b");//服务端的包名
//        //通过intent-filter设置的name,找到这个service
//        intent.setAction("com.aidl.filter.fileservice");//过滤
//        bindService(intent, connection, Context.BIND_AUTO_CREATE);//开启Service
//        Log.d("SJY", "绑定b软件的aidl服务");
    }

    public void unbind() {
//        if (connection != null && fileBinder != null && fileBinder.asBinder().isBinderAlive()) {
//
//            try {
//                fileBinder.unregister(callback);
//            } catch (RemoteException e) {
//                e.printStackTrace();
//            }
//
//            unbindService(connection);
//        } else {
//            Log.e("SJY", "进程间通讯中断");
//            unbindService(connection);
//        }
    }

    /**
     * 服务端（软件测试b）回调给客户端(链路层)
     */
//    public SearchResultCallback callback = new SearchResultCallback.Stub() {
//        @Override
//        public void searchResult(int code, String msg) throws RemoteException {
//            recevClientBResult(code, msg);
//        }
//    };

    //子类具体实现
    public void recevClientBResult(int code, String msg) {

    }


}
