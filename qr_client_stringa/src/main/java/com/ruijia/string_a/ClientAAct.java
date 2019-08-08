package com.ruijia.string_a;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 应用层/发送端/业务层demo
 */
public class ClientAAct extends ClientABaseAct implements View.OnClickListener {
    public static final String TAG = "SJY";
    //=================控件=================
    private EditText et_timeInteral, et_maxSize, et_timeout;
    private Button btn_select, btn_QRCtrl, btn_QRRecv, btn_QRSend, btn_bind, btn_unbind, btn_time_sure, btn_maxSize_sure, btn_timeout;
    private EditText tv_path;

    //
    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMyView();
    }


    private void initMyView() {

        et_timeInteral = findViewById(R.id.et_timeInteral);
        et_maxSize = findViewById(R.id.et_maxSize);
        et_timeout = findViewById(R.id.et_timeout);
        tv_path = findViewById(R.id.tv_path);
        btn_maxSize_sure = findViewById(R.id.btn_maxSize_sure);
        btn_timeout = findViewById(R.id.btn_timeout);
        btn_time_sure = findViewById(R.id.btn_time_sure);
        btn_bind = findViewById(R.id.btn_bind);
        btn_unbind = findViewById(R.id.btn_unbind);
        btn_select = findViewById(R.id.btn_select);
        btn_QRCtrl = findViewById(R.id.btn_QRCtrl);
        btn_QRRecv = findViewById(R.id.btn_QRRecv);
        btn_QRSend = findViewById(R.id.btn_QRSend);
        btn_maxSize_sure.setOnClickListener(this);
        btn_timeout.setOnClickListener(this);
        btn_time_sure.setOnClickListener(this);
        tv_path.setOnClickListener(this);
        btn_bind.setOnClickListener(this);
        btn_unbind.setOnClickListener(this);
        btn_select.setOnClickListener(this);
        btn_QRCtrl.setOnClickListener(this);
        btn_QRRecv.setOnClickListener(this);
        btn_QRSend.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == btn_select) {
            selectPath = null;
            tv_path.setText("");
        } else if (v == btn_time_sure) {
            String str = et_timeInteral.getText().toString();
            if (TextUtils.isEmpty(str)) {
                timeInterval = 400;
                et_timeInteral.setText("");
                et_timeInteral.setHint("默认发送时间间隔：" + timeInterval + "ms");
            } else {
                timeInterval = Integer.parseInt(str);
                et_timeInteral.setText("");
                et_timeInteral.setHint("设置发送时间间隔：" + timeInterval + "ms");
            }
        } else if (v == btn_maxSize_sure) {
            String str = et_maxSize.getText().toString();
            if (TextUtils.isEmpty(str)) {
                maxSize = 5;
                et_maxSize.setText("");
                et_maxSize.setHint("默认最大文件：" + maxSize + "M");
            } else {
                maxSize = Integer.parseInt(str);
                et_maxSize.setText("");
                et_maxSize.setHint("设置文件最大：" + maxSize + "M");
            }

        } else if (v == btn_timeout) {
            String str = et_timeout.getText().toString();
            if (TextUtils.isEmpty(str)) {
                timeout = 15;
                et_maxSize.setText("");
                et_maxSize.setHint("超时判断：默认" + timeout + "s");
            } else {
                timeout = Integer.parseInt(str);
                et_maxSize.setText("");
                et_maxSize.setHint("超时判断：默认" + timeout + "s");
            }

        } else if (v == btn_QRCtrl) {
            qrCtrl();
        } else if (v == btn_QRRecv) {
            QrRecv();
        } else if (v == btn_QRSend) {
            selectPath = tv_path.getText().toString();
            if (TextUtils.isEmpty(selectPath)) {
                Toast.makeText(this, "字符串不可为空", Toast.LENGTH_SHORT).show();
            } else if (selectPath.length() > 2900) {
                Toast.makeText(this, "字符串超出查询范围", Toast.LENGTH_SHORT).show();
            } else {
                qrSend();
            }

        } else if (v == btn_bind) {
            bind();
        } else if (v == btn_unbind) {
            unbind();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbind();
    }

    /**
     *
     */
//    private void selectDialog() {
//        //文件选择器
//        FilePicker picker = new FilePicker(this, FilePicker.FILE);
////        picker.setShowUpDir(true);
//        picker.setRootPath(Environment.getExternalStorageDirectory().getAbsolutePath());
////        picker.setAllowExtensions(new String[]{".txt"});
//        picker.setAllowExtensions(null);//没有过滤
//        picker.setFileIcon(getResources().getDrawable(android.R.drawable.ic_menu_agenda));
//        picker.setFolderIcon(getResources().getDrawable(android.R.drawable.ic_menu_upload_you_tube));
//        //picker.setArrowIcon(getResources().getDrawable(android.R.drawable.arrow_down_float));
//        picker.setOnFilePickListener(new FilePicker.OnFilePickListener() {
//            @Override
//            public void onFilePicked(String currentPath) {
//                Log.d("SJY", "currentPath=" + currentPath);
//                selectPath = currentPath;
//                tv_path.setText(selectPath);
//            }
//        });
//        picker.show();
//    }
    //-------------------------进程间通讯 客户端向服务端发送请求-------------------------------

    /**
     * 控制设置
     */
    private void qrCtrl() {
        try {
            //调用服务端接口
            ibinder.QrCtrl(timeInterval, maxSize);
            //TODO
//            ibinder.QrCtrl(timeInterval, maxSize,timeout);

        } catch (RemoteException e) {
            //异常
            e.printStackTrace();
            Log.e("SJY", "binder" + e.toString());
        }

    }

    /**
     * 发送文件
     */
    private long startTime = 0;

    private void qrSend() {
        startTime = System.currentTimeMillis();
        try {
            //调用服务端接口
            ibinder.QRSend(selectPath);
        } catch (RemoteException e) {
            //异常
            e.printStackTrace();
            Log.e(TAG, "binder" + e.toString());
        }
    }

    private void QrRecv() {
        try {
            Log.d(TAG, ibinder.QRRecv());
        } catch (RemoteException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }
    }

    //-------------------------服务端（链路层）回调-------------------------------

    //01
    @Override
    void clientIsTrans(boolean isSuccess, String msg) {
        super.clientIsTrans(isSuccess, msg);
        if (isSuccess) {
            Log.d(TAG, "实时状态监听" + isSuccess + "--msg=" + msg);
        } else {
            //TODO 失败，必须处理
            Log.e(TAG, "本次传输失败：" + msg);
        }

    }

    //02
    @Override
    void clientSplitToIoTime(long time, String msg) {
        super.clientSplitToIoTime(time, msg);
        //文件转IO耗时
        Log.d(TAG, "splitToIoTime-time=" + time + "ms--msg=" + msg);
    }

    //03
    @Override
    void clientSplitToArrayTime(long time, String msg) {
        super.clientSplitToIoTime(time, msg);
        //IO转String耗时
        Log.d(TAG, "SplitToArrayTime-time=" + time + "ms--msg=" + msg);
    }

    //04
    @Override
    void clientCreateNewArrayTime(long time, String msg) {
        super.clientSplitToIoTime(time, msg);
        // String分成String片段耗时
        Log.d(TAG, "createNewArrayTime-time=" + time + "ms--msg=" + msg);
    }

    //05
    @Override
    void clientCreateQrImgProgress(int total, int position, String msg) {
        super.clientCreateQrImgProgress(total, position, msg);
        //可以做成进度条

        Log.d(TAG, "文件集中转二维码图进度=" + (100 * (position + 1) / total) + "%--createQrImgProgress-total=" + total + "--position=" + position + "--msg=" + msg + "ms");
    }

    //06
    @Override
    void clientCreateQrImgTime(long time, String msg) {
        super.clientCreateQrImgTime(time, msg);
        //后台传送前，二维码耗时
        Log.d(TAG, "String片段转二维码图总耗时：" + time + "ms-----------msg=" + msg);
    }

    //07
    @Override
    void clientTransProgress(long time, int total, int position, String msg) {
        super.clientTransProgress(time, total, position, msg);
        //二维码发送进度，这里只统计了handler处的发送，不统计是否被识别的进度。
        Log.i(TAG, "二维码发送进度：" + (100 * position / total) + "%--图片显示在界面的时长=" + time + "ms----msg=" + msg + "--position=" + position);
    }

    //08 信息都在msg中，time为文件总耗时
    @Override
    void clientTransTime(long time, String msg) {
        super.clientTransTime(time, msg);
        //传送完成 总耗时
        Log.d(TAG, "发送端返回\n" + msg);
    }

    //09
    @Override
    void clientTransComplete() {
        super.clientTransComplete();
        Log.d(TAG, "transComplete");
    }
}
