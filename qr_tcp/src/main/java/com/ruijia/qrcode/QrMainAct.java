package com.ruijia.qrcode;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.ruijia.qrcode.base.QrBaseAct;
import com.ruijia.qrcode.database.DatabaseSQL;
import com.ruijia.qrcode.listener.OnServiceAndActListener;
import com.ruijia.qrcode.module.MyData;
import com.ruijia.qrcode.service.QRXmitService;
import com.ruijia.qrcode.utils.BitmapCacheUtils;
import com.ruijia.qrcode.utils.CacheUtils;
import com.ruijia.qrcode.utils.CodeUtils;
import com.ruijia.qrcode.utils.Constants;
import com.ruijia.qrcode.utils.ConvertUtils;
import com.ruijia.qrcode.utils.IOUtils;
import com.ruijia.qrcode.utils.SPUtil;
import com.ruijia.qrcode.utils.ViewUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lib.ruijia.zbar.ZBarContinueView;
import lib.ruijia.zbar.qrcodecore.BarcodeType;
import lib.ruijia.zbar.qrodecontinue.ContinueQRCodeView;

/**
 * 大优化2019-0124后
 *
 * <p>
 * 链路层 物理连接
 * 开启MainAct识别功能有两种方式，1：若MainAct没启动，使用service的serviceStartAct()方法启动 2：若MainAct已在前端显示，service使用接口回调启动
 *
 * @author sjy 2019-01-25
 */

public class QrMainAct extends QrBaseAct implements ContinueQRCodeView.Delegate {
    private static final String SCAN_TAG = "scan";
    private static final String QR_TAG = "qr_sjy";
    private static final String TAG = "trans";

    //========================通用变量==========================
    //控件
    private ZBarContinueView mZBarView; //zbar
    private ImageView img_result;
    private RelativeLayout ly_img;
    private DatabaseSQL sql;
    private Handler handler = new Handler();

    //=====单独显示bitmap的倒计时参数=====
    private Timer showTimer;//标记倒计时类（重要）
    private int showTimerCount = 0;//

    //===================发送端/接收端 通用标记=====================

    private String lastText;//
    //

    private int sendCount = 0;//发送端倒计时
    private int recvCount = 0;//接收端倒计时
    private int lastTextCount = 0;//链路通讯中 倒计时使用,避免接收端拒绝重复文件

    //==发送端标记==
    private MyData sendData;//该标记包含 文件路径和文件大小，所以文件成功后，需要清除
    private MyData sendOverData;//
    //==接收端标记==
    //～～～～～～线程池管理 ～～～～～～
    ExecutorService executorService = Executors.newFixedThreadPool(5);
    //～～～～～～统计 ～～～～～～
    //时间设置

    //===================发送端操作=====================

    //==发送文件的数据信息==
    private String sendSearch;//发送端


    //==service相关(关联发送端)==
    private ServiceConnection conn;
    private QRXmitService myService = null;
    private QRXmitService.QrAIDLServiceBinder myBinder = null;

    //===================接收端操作=====================


    private String recSearch;//接收端 搜索词

//============================================预览聚焦=====================================================

    /**
     * 定时聚焦
     */
    Runnable focusTask = new Runnable() {
        @Override
        public void run() {
            if (mZBarView != null) {
                //暴力聚焦
                mZBarView.setMyFoucus();
                if (handler != null) {
                    handler.removeCallbacks(this);
                    handler.postDelayed(this, Constants.FOCUS_TIME);
                }
            }
        }
    };

//=================================================================================================================
//=====================================识别结果,细分为接收端处理+发送端处理============================================
//=================================================================================================================


    /**
     * zbar识别
     * <p>
     * 发送端：
     * 发送数据后，接收端使用该方法处理另一个app的反馈结果，并根据反馈结果，重新发送缺失数据，等待再次反馈。直到反馈结果为识别成功
     * <p>
     * 接收端：
     * 发送端固定时间间隔发送的数据，将数据拼接并处理缺失的数据，并将缺失数据反馈给发送端。
     */

    //QRCodeView.Delegate
    @Override
    public void onScanQRCodeSuccess(String resultStr) {
        Log.d(SCAN_TAG, resultStr);
        //过滤
        if (TextUtils.isEmpty(resultStr) || resultStr.length() < 14 || resultStr.equals(lastText)) {
            Log.d(SCAN_TAG, "重复扫描");
            return;
        }
        lastText = resultStr;

        //接收端，收到结束标记，处理发送端的数据
        if (resultStr.contains(Constants.sendOver_Contnet)) {
            RecvTerminalOver(resultStr);
            //需加倒计时，避免接收端死机不接受反复发送的数据
            updateLastTextListener();
            return;
        }
        //发送端，收到结束标记，处理文件是否传输完成/不处理缺失了（原因是耗时长，转移到SndTerminalScan中处理了）
        if (resultStr.contains(Constants.receiveOver_Content)) {//接收端 结束标记
            sendTerminalOver(resultStr);
            //添加监听
            updateLastTextListener();
            return;
        }
    }

    // 二维码 QRCodeView.Delegate
    @Override
    public void onScanQRCodeOpenCameraError() {
        Log.e(TAG, "QRCodeView.Delegate--ScanQRCodeOpenCameraError()");
    }

    /**
     * 添加lastText监听
     * <p>
     * 慎重使用此方法，加入该方法，新能会下降
     * 禁止在高频率下使用
     */
    private void updateLastTextListener() {
        lastTextCount = 0;
        handler.removeCallbacks(updateLastTextTask);
        handler.post(updateLastTextTask);
    }

    /**
     * 原理：避免二维码的识别结果一直被屏蔽
     */
    private Runnable updateLastTextTask = new Runnable() {
        @Override
        public void run() {
            if (lastTextCount < 15) {
                lastTextCount++;
                handler.removeCallbacks(this);
                handler.postDelayed(this, 800);
            } else {
                handler.removeCallbacks(this);
                lastText = "";
            }
        }
    };


    //========================================================================================
    //=====================================接收端处理==========================================
    //========================================================================================

    /**
     * 接收端 清空数据
     * <p>
     * 使用位置：1 接收端等待下一次传输,2 如果确定为发送端操作，清空接收端数据
     */
    private void clearRecvParams() {
        recvCount = 0;
        lastText = "";
        recSearch = null;//接收端
        clearImageView();
        handler.removeCallbacks(focusTask);
    }

    /**
     * 接收端 识别结束处理（实时扫描结果）
     * 数据拼接类型：QrcodeContentSendOver+搜索词+7位的文件大小
     * <p>
     * 这里需要讨论情况：
     * （1）接收端只拿到该结束标记，则receveContentMap对象没有值，识别流程失败
     * （2）接收端拿到部分（或全部数据）识别数据+结束标记，则符合设计要求
     */
    private void RecvTerminalOver(String resultStr) {
        //流程识别成功，清空 接收端
        if (resultStr.contains(Constants.SUCCESS)) {
            clearRecvParams();
            return;
        }
        //开启聚焦任务
        handler.removeCallbacks(focusTask);
        handler.post(focusTask);

        Log.d(TAG, "接收端：获取数据：" + recSearch);
        //如果速度快,接收端收到了数据，而接收端还在显示标记bitmap,则在此处清空显示+倒计时
        if (showTimer != null) {
            showTimer.cancel();
            showTimer = null;
            clearImageView();
        } else {
            clearImageView();
        }
        //提取数据
        String pathAndPos = resultStr.substring(Constants.sendOver_Contnet.length());
        String positionStr = pathAndPos.substring((pathAndPos.length() - 7));
//        receveFileSize = Integer.parseInt(positionStr); //拿到发送端的数据大小
        recSearch = pathAndPos.substring(0, (pathAndPos.length() - 7)); //拿到发送端文件类型
        //
        if (!TextUtils.isEmpty(recSearch)) {
            getSearchResult();
        }
    }

    /**
     * 拿到发送端的搜索词，开始查询b软件
     */
    private void getSearchResult() {
        Log.d(TAG, "接收端：查询数据" + recSearch);
        if (!TextUtils.isEmpty(recSearch)) {
            String val = sql.findByKey(recSearch);
            recvTerminalFinish(1, val);
            //TODO 测试，暂时不连接进程B
//            connectClientBForResult(recSearch);
        } else {
            recvTerminalFinish(0, "没有获取到搜索词");
        }
    }

    /**
     * 接收端，返回查询结果（最终步骤）
     *
     * @param msg
     */
    private void recvTerminalFinish(int code, String msg) {
        if (code == 0) {
            Log.d(QR_TAG, "接收端：返回搜索数据：空");
            showRecvBitmap(Constants.receiveOver_Content + Constants.FAILED, Constants.RECV_FLAG_TIME * 3);
        } else {
            Log.d(QR_TAG, "接收端：返回搜索数据:" + msg);
            showRecvBitmap(Constants.receiveOver_Content + Constants.SUCCESS + msg, Constants.RECV_FLAG_TIME * 3);
        }
    }

    //=====================================接收端 链路层向接收端b软件发送查询==========================================

    /**
     * TODO 连接B软件,将查询结果发送给测试B软件（客户端（链路层）向服务端（软件b）发送数据）
     */
//    private void connectClientBForResult(String searchData) {
//        //再次确保连接
//        bind();
//        //aidl 与测试b通讯
//        Log.e(TAG, "测试B端app进程间通讯");
//        //
//        try {
//            if (fileBinder != null) {
//                //向服务端发送查询数据
//                Log.e(TAG, "向测试B端发送查询数据" + searchData);
//                fileBinder.QRRecv(searchData);
//            } else {
//                Log.e(TAG, "测试B端app进程间通讯失败");
//            }
//
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//    }

    //========接收端 接收端b软件（服务端）向链路层（客户端）回调结果========

    /**
     * 拿到b软件的查询结果，二维码回调给发送端
     *
     * @param code
     * @param msg
     */
//    @Override
//    public void recevClientBResult(int code, String msg) {
//        recvTerminalFinish(code, msg);
//    }


    //=======================================================================================
    //=====================================发送端处理==========================================
    //=======================================================================================

    /**
     * 初始化/清空 发送端数据(超时清空，接受到结果清空)
     */
    private void clearSendParams() {
        sendCount = 0;
        sendSearch = null;
        lastText = "";
        sendData = null;//清空发送标记
        clearImageView();//清空显示
        handler.removeCallbacks(focusTask);
        removeSendListener();
    }


    /**
     * 发送端向接收端发送连接测试，连接通了，则发送数据，连接不通，则回调 连接失败
     */
    Runnable SendConnectTask = new Runnable() {
        @Override
        public void run() {
            if (sendCount < Constants.TIMEOUT) {
                sendCount++;
                handler.postDelayed(this, 950);
            } else {//连接超时
                //回调
                myService.isTrans(false, "连接对端超时:" + Constants.TIMEOUT + "S");
                //清除图片
                clearImageView();
                //清除发送端所有数据
                clearSendParams();

            }
        }
    };

    /**
     * 添加意外中断监听（最好在最小方法内监听）
     * <p>
     * 慎重使用此方法，加入该方法，新能会下降
     * 禁止在高频率下使用
     */
    private void updateSendListener() {
        //触发超时统计
        handler.removeCallbacks(SendConnectTask);
        handler.post(SendConnectTask);
    }

    /**
     * 移除中断监听（如果调用了如上监听，有耗时操作，最好在耗时操作前移除监听，耗时后再开启监听）
     */
    private void removeSendListener() {
        handler.removeCallbacks(SendConnectTask);
    }


    /**
     * 发送端 结束标记处理（实时扫描结果）
     * 数据格式：QrCodeContentReceiveOver或QrCodeContentReceiveOverSuccess
     *
     * @param resultStr
     */
    private void sendTerminalOver(String resultStr) {

        //如果速度快,发送端收到了数据，而发送端还在显示标记bitmap,则在此处清空倒计时显示
        if (showTimer != null) {
            showTimer.cancel();
            showTimer = null;
            clearImageView();
        }

        //格式:QrCodeContentReceiveOverSuccess/QrCodeContentReceiveOverFailed
        // 文件传输完成，回调aidl。
        if (resultStr.length() > Constants.receiveOver_Content.length()) {
            if (resultStr.contains(Constants.FAILED)) {
                myService.isTrans(false, "接收端查询失败");
                sendComplete("查询失败");
            } else if (resultStr.contains(Constants.SUCCESS)) {
                Log.d(TAG, "接收端查询成功");
                //截取结果：
                String result = resultStr.substring("QrCodeContentReceiveOverSuccess".length());
                sendComplete(result);
            }
        } else {
            Log.e(TAG, "接收端异常代码");
        }

    }

    /**
     * 发送数据
     */
    private void startSend() {
        Log.d(TAG, "发送端：发送数据");
        //取缓存
        if (sendData == null) {
            Bitmap bitmap = CacheUtils.getInstance().getBitmap(Constants.flag_send_over);
            String len = CacheUtils.getInstance().getString(Constants.flag_send_over_length);
            sendData = new MyData(bitmap, Integer.parseInt(len), -1);
        }
        if (sendOverData == null) {
            Bitmap bitmap = CacheUtils.getInstance().getBitmap(Constants.flag_send_complete);
            String len = CacheUtils.getInstance().getString(Constants.flag_send_complete_length);
            sendOverData = new MyData(bitmap, Integer.parseInt(len), -1);
        }
        //初始化连接图片倒计时可能没有结束，此处强制结束,避免影响发送数据的识别效率
        if (showTimer != null) {
            showTimer.cancel();
            showTimer = null;
            clearImageView();
        } else {
            clearImageView();
        }
        //结束初始化连接
        //保存当前时间节点。
        SPUtil.putLong(Constants.START_SEND_TIME, System.currentTimeMillis());//存储统计时间
        //带有搜索词的二维码图
        showSendBitmap(sendData, Constants.SEND_FLAG_TIME * 3, true);//结束符
        //聚焦设置
        handler.removeCallbacks(focusTask);
        handler.post(focusTask);
    }

    /**
     * <p>
     * 发送端接收结束标记
     */
    private void sendComplete(String result) {
        //统计 回调
        long qrstartTime = SPUtil.getLong(Constants.START_SEND_TIME, System.currentTimeMillis());//二维码开始时间
        long qrTime = System.currentTimeMillis() - qrstartTime;//总耗时
        StringBuffer buffer = new StringBuffer();
        buffer.append("查询结果=" + result).append("\n");
        buffer.append("查询总耗时:" + qrTime + "ms");
        //回调
        myService.setAidlQrCodeComplete(qrTime, buffer.toString());

        //清空超时监听
        removeSendListener();
        //告知接收端，清空数据
        showSendBitmap(sendOverData, 1000, false);//结束符
        //清空发送端数据数据
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearSendParams();
            }
        }, 950);
    }

    //===============================================================================
    //=====================================复写==========================================
    //===============================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //屏幕常量
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.act_main);
        initView();

        clearRecvParams();
        clearSendParams();
        //TODO

        sql = new DatabaseSQL(this);
        sql.addTestData();
    }


    /**
     * 初始化控件
     */
    private void initView() {
        //zbar
        mZBarView = (ZBarContinueView) findViewById(R.id.zbarview);
        mZBarView.setDelegate(this);
        //
        img_result = (ImageView) findViewById(R.id.img_qr);
        //辅助设置，方便设置设备二维码对焦问题
        ly_img = (RelativeLayout) findViewById(R.id.ly_img);
        ly_img.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ly_img.setVisibility(View.INVISIBLE);
                return false;
            }
        });

        handler = new Handler();
        //开启扫描
        startPreview();

        //开启聚焦任务
        handler.postDelayed(focusTask, 500);
        //开启线程池
        executorService = Executors.newFixedThreadPool(5);
    }

    /**
     * 播放震动
     */
    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(100);
    }


    /**
     * 开始识别（其实布局绑定就已经识别，此处设置识别样式）
     */
    private void startPreview() {
        //前置摄像头(不加显示后置)
        mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
        mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
//        mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
        mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mZBarView != null) {
            mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
            mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
            mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
//            mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
            mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mZBarView != null) {
            //前置摄像头(不加显示后置)
            mZBarView.startCamera(); // 打开后置摄像头开始预览，但是并未开始识别
            mZBarView.setType(BarcodeType.ONLY_QR_CODE, null); // 只识别 QR_CODE
            mZBarView.getScanBoxView().setOnlyDecodeScanBoxArea(false); // 仅识别扫描框中的码
//            mZBarView.startCamera(cameraId); // 打开前置摄像头开始预览，但是并未开始识别
            mZBarView.startSpot(); // 显示扫描框，并且延迟0.1秒后开始识别
        }
        //
        if (conn != null && myBinder != null && myService != null) {
            //act通知service,可以发送数据传输了,
            myService.startServiceTrans();
        } else {
            initService();
        }
        //
        mZBarView.setMyFoucus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mZBarView != null) {
            mZBarView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        }
    }

    @Override
    protected void onStop() {
        mZBarView.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mZBarView.onDestroy(); // 销毁二维码扫描控件
        handler.removeCallbacks(focusTask);

        if (conn != null) {
            unbindService(conn);
        }
        //屏幕常亮
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onDestroy();

    }

    //=====================================发送端 act与service相关==========================================

    /**
     * service连接
     */
    private void initService() {
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                myBinder = (QRXmitService.QrAIDLServiceBinder) service;
                myService = myBinder.geSerVice();
                //绑定监听
                myService.setListener(myListener);
                //act通知service,可以发送数据传输了,
                myService.startServiceTrans();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        //act绑定service
        Intent intent = new Intent(QrMainAct.this, QRXmitService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);//开启服务
    }

    /**
     * service的回调
     */
    private OnServiceAndActListener myListener = new OnServiceAndActListener() {
        //说明：字符串查询，不用path
        @Override
        public void onQrsend(String path, List<String> newData, long mfileSize) {
            //发送端：发送前，所有数据清空
            clearRecvParams();
            clearSendParams();

            //重新赋值
            sendSearch = path;
            if (!TextUtils.isEmpty(sendSearch)) {
                startSend();
            }
        }
    };

    //=====================================private处理==========================================


    /**
     * 发送端显示 结束标记
     *
     * @param data     不为空
     * @param showTime ms,显示时长
     * @return
     */
    private void showSendBitmap(final MyData data, final long showTime, final boolean hasListener) {
        showTimerCount = 0;

        if (data != null) {
            //开启倒计时显示
            if (showTimer != null) {
                showTimer.cancel();
                showTimer = null;
            }
            //
            showTimer = new Timer();
            showTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //终止倒计时
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (showTimerCount < 1) {//
                                ViewUtils.setImageViewWidth(data.width, img_result);//01
                                img_result.setImageBitmap(data.bitmap);//02
                                showTimerCount++;
                            } else {
                                if (hasListener) {
                                    updateSendListener();//发送端耗时完成，添加监听(不可少)
                                }
                                clearImageView();
                                if (showTimer != null) {
                                    showTimer.cancel();
                                    showTimer = null;
                                }
                            }
                        }
                    });
                }
            }, 0, showTime);
        } else {
            Log.e(TAG, "发送端--没有标记位显示");
        }
    }


    /**
     * 创建并显示
     *
     * @param content  不为空
     * @param showTime ms,显示时长 默认1000
     * @return
     */
    private void showRecvBitmap(final String content, final long showTime) {

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                return CodeUtils.createByMultiFormatWriter(content, Constants.qrBitmapSize);
            }

            @Override
            protected void onPostExecute(final Bitmap bitmap) {
                showTimerCount = 0;
                if (bitmap != null) {
                    if (showTimer != null) {
                        showTimer.cancel();
                        showTimer = null;
                    }
                    showTimer = new Timer();
                    showTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            //终止倒计时
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (showTimerCount < 1) {//
                                        ViewUtils.setImageViewWidth(content.length(), img_result);//01
                                        img_result.setImageBitmap(bitmap);//02
                                        showTimerCount++;
                                    } else {
                                        clearRecvParams();
                                        if (showTimer != null) {
                                            showTimer.cancel();
                                            showTimer = null;
                                        }
                                    }
                                }
                            });
                        }
                    }, 0, showTime);
                } else {
                    Log.e(TAG, "生成二维码失败--showRecvBitmap");
                }
            }
        }.execute();
    }

    /**
     * 接收端 标记bitmap显示
     *
     * @param data
     * @param showTime
     */
    private void showRecvBitmap(final MyData data, final long showTime) {
        showTimerCount = 0;

        if (data != null) {
            if (showTimer != null) {
                showTimer.cancel();
                showTimer = null;
            }
            showTimer = new Timer();
            showTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //终止倒计时
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (showTimerCount < 1) {
                                ViewUtils.setImageViewWidth(data.width, img_result);//01
                                img_result.setImageBitmap(data.bitmap);//02
                                showTimerCount++;
                            } else {
                                clearImageView();
                                if (showTimer != null) {
                                    showTimer.cancel();
                                    showTimer = null;
                                }
                            }
                        }
                    });
                }
            }, 0, showTime);
        } else {
            Log.e(TAG, "showRecvBitmap--生成二维码失败");
        }
    }

    /**
     * 只要不显示二维码，就讲ImagView缩小，让摄像头聚焦文字，加速聚焦
     */
    private void clearImageView() {
        //清空
        img_result.setImageBitmap(null);
        //缩小
        ViewGroup.LayoutParams lp = img_result.getLayoutParams();
        lp.width = 1;
        lp.height = 1;
        img_result.setLayoutParams(lp);
    }

}

