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
    private static final String TAG = "SJY";

    //========================通用变量==========================
    //控件
    private ZBarContinueView mZBarView; //zbar
    private ImageView img_result;
    RelativeLayout ly_img;

    private Handler handler = new Handler();
    /**
     * 确认发送端还是接收端，重要的判断
     * <p>
     * onCreate中默认初始化为 接收端
     * <p>
     * Service和act交互后，传来发送数据后，则修改为发送端
     */


    //=====单独显示bitmap的倒计时参数=====
    private Timer timer;//发送二维码倒计时类（重要）
    private Timer showTimer;//标记倒计时类（重要）
    private int showTimerCount = 0;//

    //===================发送端/接收端 通用标记=====================

    private String lastText;//
    private String lastRecvOver = "";//接收端使用的标记
    private String lastSendOver = "";//发送端使用的标记
    //

    private int timeoutCount = 0;//初始化倒计时使用
    private int connectCount = 0;//链路通讯中 倒计时使用
    private int lastTextCount = 0;//链路通讯中 倒计时使用,避免接收端拒绝重复文件
    private int SendMoreCount = 0;//如果某个二维码不识别，发送再多也没用，到达20次,强制关闭通讯
    private String recv_lastStr = null;//需要清空，否则终止操作

    //==发送端标记==
    private MyData flag_send_over;//该标记包含 文件路径和文件大小，所以文件成功后，需要清除
    //==接收端标记==
    private MyData flag_recv_init;//初始化连接
    private MyData flag_recv_success;//保存数据成功
    private MyData flag_recv_failed;//保存数据失败

    //～～～～～～线程池管理 ～～～～～～
    ExecutorService executorService = Executors.newFixedThreadPool(5);
    //～～～～～～统计 ～～～～～～
    //时间设置
    private long handler_lastTime;//用于计算文件/片段list发送总耗时。
    /*
     *用于计算单张二维码发送耗时,
     *发送端 统计发送二维码速率
     */
    private long lastSaveTime;
    String send_transMSG = "";

    /*
     *用于计算发送完成后，再此发送缺失片段的总耗时
     *发送端 统计发送完成后，再此发送缺失片段的总耗时
     */
    private long feedbackTime;
    String send_feedBackMSG = "";
    //单独的计数
    private long sendSuccessTime = 0;//最后一次发送后，接收端反馈成功结果的耗时，可以计算时单独减去

    //===================发送端操作=====================

    //==发送文件的数据信息==
    private List<String> sendDatas = new ArrayList<>();//发送端 数据
    //    private List<MyData> sendMaps = new ArrayList<>();//发送端 数据（数据由消息队列的发送数据组成，变相保存 原始数据）
    private String sendSearch;//发送端
    private long fileSize = 0;//文件 字符流大小
    private int sendSize = 0;//发送端 文件大小

    //==service相关(关联发送端)==
    private ServiceConnection conn;
    private QRXmitService myService = null;
    private QRXmitService.QrAIDLServiceBinder myBinder = null;

    //操作数据
    private ArrayDeque<MyData> firstSendQueue = new ArrayDeque();//第一次发送 数据队列
    private List<Integer> sendBackList = new ArrayList<>();//发送端 返回缺失数据位置
    private List<String> sendBackLists = new ArrayList<>();//发送端 返回缺失bitmap的个数（用于大文件缺失）
    private List<MyData> sendImgsMore = new ArrayList<>();//缺失的数据； Bitmap样式
    //
    private int sendCounts = 0;//发送次数统计(初次发送和二次+发送都是用)
    private boolean isSending = false;//代码成对出现，用于保护第一次发送，当第一次发送结束，设置为false

    //接收数据处理
    private int rcvImgSize = 0;//缺失bitmap个数(通常只有一个)

    //====第一次发送-生产者参数====
    //
    private boolean isFirstProducerExit = false;//是否退出
    private int firstProducerPos = 0;//生产者标记生成bitmap的位置


    //===================接收端操作=====================

    private Map<Integer, String> receveContentMap = new HashMap<Integer, String>();//接收的数据暂时保存到map中，最终保存到receiveDatas
    private List<String> receiveContentDatas = new ArrayList<>();//文件内容存储
    private List<Integer> feedBackFlagList = new ArrayList<>();//缺失pos标记list,用于拼接数据
    private StringBuffer feedBackBuffer = new StringBuffer();  //统计结果
    private List<String> feedBackDatas = new ArrayList<>();//接收端处理结果，反馈list
    private String recSearch;//接收端 搜索词
    private int receveFileSize = 0;//接收端 标记 总数据长度
    private int receve = 0;//接收端 标记 总数据长度
    private int recvCounts = 0;//发送次数统计，handler发送使用

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
        /**
         *  （一）数据过滤,包括（重复结果，接收端识别完成，发送端识别完成)
         */
        //结果相同不处理
        if (TextUtils.isEmpty(resultStr) || resultStr.length() < 14 || resultStr.equals(lastText)) {
            Log.d(SCAN_TAG, "重复扫描");
            return;
        }
        long startTime = System.currentTimeMillis();
        lastText = resultStr;
        //======01-初始化连接=======

        //接收端：发送端发送初始化信息 接收端初始化使用
        if (resultStr.contains(Constants.send_init)) {
            //初始化
            initRecvConnect(resultStr);
        }

        //发送端：接收端发送初始化信息，发送端接收后发送数据
        if (resultStr.contains(Constants.recv_init)) {
            if (!isSending) {
                //第一次发送结束后，设置为falsef
                isSending = true;
                //第一次发送数据
                startSend();
            }
        }

        //======02-数据传输结束=======
        //接收端，收到结束标记，处理发送端的数据
        if (resultStr.contains(Constants.sendOver_Contnet)) {
            RecvTerminalOver(resultStr);
            return;
        }

        //发送端，收到结束标记，处理文件是否传输完成/不处理缺失了（原因是耗时长，转移到SndTerminalScan中处理了）
        if (resultStr.contains(Constants.receiveOver_Content)) {//接收端 结束标记
            sendTerminalOver(resultStr);
            return;
        }
        //需加倒计时，避免接收端死机不接受反复发送的数据
        updateLastTextListener();
    }

    //QRCodeView.Delegate
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
     * 原理：每次识别出结果，更新该异步，如果发送端识别不出二维码，倒计时20s,超过20s则链路层连接失败
     */
    private Runnable updateLastTextTask = new Runnable() {
        @Override
        public void run() {
            if (lastTextCount < Constants.CONNECT_TIMEOUT) {
                lastTextCount++;
                handler.removeCallbacks(this);
                handler.postDelayed(this, 950);
            } else {
                handler.removeCallbacks(this);
                //超时清空 lasttext
                Log.d(SCAN_TAG, "清空lastText");
                lastText = "";
            }
        }
    };


    //========================================================================================
    //=====================================发送端监听：链路意外中断（不是接收端，一定要注意 ）==========================================
    //========================================================================================


    /**
     * 添加意外中断监听（最好在最小方法内监听）
     * <p>
     * 慎重使用此方法，加入该方法，新能会下降
     * 禁止在高频率下使用
     */
    private void updateConnectListener() {
        connectCount = 0;
        handler.removeCallbacks(updateConnectTask);
        handler.post(updateConnectTask);
    }

    /**
     * 移除中断监听（如果调用了如上监听，有耗时操作，最好在耗时操作前移除监听，耗时后再开启监听）
     */
    private void removeConnectListener() {
        connectCount = 0;
        handler.removeCallbacks(updateConnectTask);
    }

    /**
     * 原理：每次识别出结果，更新该异步，如果发送端识别不出二维码，倒计时20s,超过20s则链路层连接失败
     */
    private Runnable updateConnectTask = new Runnable() {
        @Override
        public void run() {
            if (connectCount < Constants.CONNECT_TIMEOUT) {
                connectCount++;
                handler.removeCallbacks(this);
                handler.postDelayed(this, 950);
            } else {
                handler.removeCallbacks(this);
                //连接超时
                //回调
                myService.isTrans(false, "通讯意外中断，有一端无法识别二维码，超时" + Constants.TIMEOUT + "S");
                //不需要清空数据，万一起死回生，清空容易bug
            }
        }
    };

    //========================================================================================
    //=====================================接收端处理==========================================
    //========================================================================================

    /**
     * 接收端 初始化接收端数据(onCreate中默认设置为接收端，如果拿到发送数据，该方法内的变量都清空)
     */
    private void initRecvParams() {

        //初始化参数
        receveContentMap = new HashMap<Integer, String>();//接收的数据暂时保存到map中，最终保存到receiveDatas
        receiveContentDatas = new ArrayList<String>();//文件内容存储
        feedBackFlagList = new ArrayList<Integer>();//缺失标记list,用于拼接数据
        feedBackBuffer = new StringBuffer();  //统计结果
        feedBackDatas = new ArrayList<String>();//接收端处理结果，反馈list
        recSearch = null;//接收端
        receveFileSize = 0;//接收端 标记 总数据长度
        recvCounts = 0;//发送次数统计，handler发送使用

    }

    /**
     * 接收端 清空数据
     * <p>
     * 使用位置：1 接收端等待下一次传输,2 如果确定为发送端操作，清空接收端数据
     */
    private void clearRecvParams() {
        //
        clearInitConnect();
        //
        receveContentMap = new HashMap<Integer, String>();//接收的数据暂时保存到map中，最终保存到receiveDatas
        Log.d(QR_TAG, "clearRecvParams--清空所有缓存receveContentMap");
        receiveContentDatas = new ArrayList<>();//文件内容存储
        feedBackFlagList = new ArrayList<>();//缺失标记list,用于拼接数据
        feedBackBuffer = new StringBuffer();  //统计结果
        feedBackDatas = new ArrayList<>();//接收端处理结果，反馈list
        recSearch = null;//接收端
        receveFileSize = 0;//接收端 标记 总数据长度
        recvCounts = 0;//发送次数统计，handler发送使用
        //
        clearImageView();
    }

    /**
     * 接收端：（接收完成）清空数据
     * <p>
     * 由于设置了最后一张多显示ns,所以清空数据也延迟ns执行，避免bug
     */

    private void clearRecvParamsDelay() {
        showTimerCount = 0;
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
                        if (showTimerCount < 2) {
                            showTimerCount++;
                        } else {
                            clearRecvParams();
                            showTimerCount = 0;
                            if (showTimer != null) {
                                showTimer.cancel();
                                showTimer = null;
                            }
                        }
                    }
                });
            }
        }, 0, Constants.RECV_FLAG_TIME);
    }

    /**
     * 接收端+发送端：（传输中）关闭显示
     * <p>
     * 由于设置了最后一张多显示ns,所以清空数据也延迟ns执行，避免bug
     */

    private void clearShowDelay() {
        showTimerCount = 0;
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
                        if (showTimerCount < 2) {
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
        }, 0, Constants.RECV_FLAG_TIME * 2);
    }

    /**
     * 异步倒计时，处理接收端 脏数据倒计时清理，如果不是脏数据了/倒计时结束了
     * 关闭该异步的判断是 接收端接收数据/倒计时结束
     * <p>
     * 问题：
     * 如果接收端数据接收中，突然触发该方法，不会造成数据损坏，不影响数据接收
     */
    Runnable initRecvTimeoutTask = new Runnable() {
        @Override
        public void run() {
            if (timeoutCount > Constants.TIMEOUT) {
                //该种情况，链路层传输失败。
                //清空脏数据
                clearImageView();
                clearRecvParams();
                //结束倒计时
                handler.removeCallbacks(this);
            } else {
                //倒计时
                timeoutCount++;
                handler.postDelayed(this, 950);
            }

        }
    };

    /**
     * 接收端初始化
     * <p>
     * 发送端发送初始化信息，接收端接收后，初始化信息，并返回结果。
     * <p>
     * 需要做倒计时处理，如果接收端数据反馈不到发送端，以后接收端就无法再使用。
     * <p>
     * 所以清除该参数有两处，倒计时超时处，和数据识别处，
     * <p>
     * 倒计时处使用，说明链路层连接失败，
     * <p>
     * 数据识别处，说明链路正常连接（需要修改recv_lastStr+结束倒计时）
     * <p>
     */

    private void initRecvConnect(String result) {
        if (TextUtils.isEmpty(recv_lastStr) || !result.equals(recv_lastStr)) {
            clearRecvParams();
            //发送信息，通知发送端，可以发送数据了
            if (flag_recv_init != null) {
                showRecvBitmap(flag_recv_init, Constants.RECV_FLAG_TIME * 2);
            } else {
                showRecvBitmap(Constants.recv_init, Constants.RECV_FLAG_TIME * 2);
            }

            //该参数需要在适当位置清空，否则出问题。
            recv_lastStr = result;

            //处理可能成为脏数据的情况
            timeoutCount = 0;
            handler.removeCallbacks(initRecvTimeoutTask);
            handler.post(initRecvTimeoutTask);

            //
            //开启聚焦任务
            handler.removeCallbacks(focusTask);
            handler.postDelayed(focusTask, 500);
        }
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

        //如果速度快,接收端收到了数据，而接收端还在显示标记bitmap,则在此处清空显示+倒计时
        if (showTimer != null) {
            showTimer.cancel();
            showTimer = null;
            clearImageView();
        }

        //处理标记
        if (resultStr.equals(lastRecvOver)) {
            //再一次过滤，保证拿到结束标记 只处理一次
            return;
        }

        //注意该标记需要清除，否则容易出问题，清除时间在：接收端发送二维码处
        lastRecvOver = resultStr;//需清除

        //提取结束端信息：路径+数据长度，用于判断接收端数据是否接收全，否则就通知发送端再次发送缺失数据。
        String pathAndPos = resultStr.substring(Constants.sendOver_Contnet.length());
        String positionStr = pathAndPos.substring((pathAndPos.length() - 7));
        receveFileSize = Integer.parseInt(positionStr); //拿到发送端的数据大小
        recSearch = pathAndPos.substring(0, (pathAndPos.length() - 7)); //拿到发送端文件类型
        Log.d(QR_TAG, "接收端:发送端单次发送完成--拿到recvFlePath=" + recSearch + "--receveFileSize=" + receveFileSize);
        //
        if (!TextUtils.isEmpty(recSearch)) {
            getSearchResult();
        }
    }
    /**
     * 拿到发送端的搜索词，开始查询b软件
     */
    private void getSearchResult() {
        if (!TextUtils.isEmpty(recSearch)) {
            connectClientBForResult(recSearch);
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
            Log.d(QR_TAG, "接收端：获取搜索数据为空");
            showRecvBitmap(Constants.receiveOver_Content + Constants.SUCCESS + msg, Constants.RECV_FLAG_TIME * 3);
            // 开启聚焦
            handler.removeCallbacks(focusTask);
            handler.postDelayed(focusTask, 500);
        } else {
            Log.d(QR_TAG, "接收端：获取搜索数据");
            showRecvBitmap(Constants.receiveOver_Content + Constants.FAILED, Constants.RECV_FLAG_TIME * 3);
        }
        //延迟清空数据
        clearRecvParamsDelay();
    }

    //=====================================接收端 链路层向接收端b软件发送查询==========================================

    /**
     * 连接B软件,将查询结果发送给测试B软件（客户端（链路层）向服务端（软件b）发送数据）
     */
    private void connectClientBForResult(String searchData) {
        //再次确保连接
        bind();
        //aidl 与测试b通讯
        Log.e(TAG, "测试B端app进程间通讯");
        //
        try {
            if (fileBinder != null) {
                //向服务端发送查询数据
                Log.e(TAG, "向测试B端发送查询数据" + searchData);
                fileBinder.QRRecv(searchData);
            } else {
                Log.e(TAG, "测试B端app进程间通讯失败");
            }

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //=====================================接收端 接收端b软件（服务端）向链路层（客户端）回调结果==========================================

    /**
     * 拿到b软件的查询结果，二维码回调给发送端
     *
     * @param code
     * @param msg
     */
    @Override
    public void recevClientBResult(int code, String msg) {
        recvTerminalFinish(code, msg);
    }


    //=======================================================================================
    //=====================================发送端处理==========================================
    //=======================================================================================

    /**
     * 连接测试清空
     */
    private void clearInitConnect() {
        SendMoreCount = 0;
        timeoutCount = 0;
        recv_lastStr = null;
    }

    /**
     * 初始化发送端数据
     */
    private void initSendParams() {

        sendDatas = new ArrayList<>();//发送端 数据
        sendSearch = null;//发送端 文件路径
        fileSize = 0;//文件 字符流大小
        sendSize = 0;//发送端 文件大小

        //操作数据
        firstSendQueue = new ArrayDeque();//第一次发送 数据队列
        sendBackList = new ArrayList<>();//发送端 返回缺失数据位置
        sendBackLists = new ArrayList<>();//发送端 返回缺失bitmap的个数（用于大文件缺失）
        sendImgsMore = new ArrayList<>();//缺失的数据； Bitmap样式
        //
        sendCounts = 0;//发送次数统计(初次发送和二次+发送都是用)
        rcvImgSize = 0;//缺失bitmap个数(通常只有一个)
        //
        clearImageView();
        //统计参数
        send_transMSG = "";
        send_feedBackMSG = "";
    }

    /**
     * 发送端数据清空
     */
    private void clearSendParams() {
        SendMoreCount = 0;
        clearInitConnect();
        sendDatas = new ArrayList<>();//发送端 数据
//        sendImgs = new ArrayList<>();//发送端 数据
        sendBackList = new ArrayList<>();//发送端 返回缺失数据
//        sendImgsMore = new ArrayList<>();//缺失的数据； Bitmap样式
        sendSearch = null;//发送端 文件路径
        sendSize = 0;//发送端 文件路径
        sendCounts = 0;//发送次数统计，handler发送使用
        isSending = false;//代码成对出现，用于保护发送
        //时间设置
        handler_lastTime = 0;//用于计算发送耗时+显示到界面的时长
        lastSaveTime = 0;//用于计算发送耗时,
        flag_send_over = null;//清空发送标记
        //统计
        send_transMSG = "";
        send_feedBackMSG = "";
    }

    /**
     * 发送端向接收端发送连接测试，连接通了，则发送数据，连接不通，则回调 连接失败
     */
    Runnable initSendConnectTask = new Runnable() {
        @Override
        public void run() {
            if (timeoutCount < Constants.TIMEOUT) {
                timeoutCount++;
                handler.postDelayed(this, 950);
            } else {//连接超时
                //清除图片
                clearImageView();
                //清除发送端所有数据
                clearSendParams();
                //回调
                myService.isTrans(false, "连接接收端设备失败，连接超时:" + Constants.TIMEOUT + "S");
            }
        }
    };


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

        //处理标记
        if (resultStr.equals(lastSendOver)) {
            //再一次过滤，保证拿到结束标记 只处理一次
            return;
        }

        //注意该标记需要清除，否则容易出问题，清除时间在发送二维码处
        lastSendOver = resultStr;

        //格式:QrCodeContentReceiveOverSuccess/QrCodeContentReceiveOverFailed
        // 文件传输完成，回调aidl。
        if (resultStr.length() > Constants.receiveOver_Content.length()) {
            if (resultStr.contains(Constants.FAILED)) {
                myService.isTrans(false, "接收端查询失败");
            } else if (resultStr.contains(Constants.SUCCESS)) {
                Log.d(TAG, "接收端查询成功");
                //截取结果：
                String result = resultStr.substring("QrCodeContentReceiveOverSuccess".length());
                //加入缺失文件反馈的计时统计
                long myfeedbackTime = System.currentTimeMillis() - feedbackTime;
                sendSuccessTime = myfeedbackTime;
                send_feedBackMSG += "返回文件结果耗时=" + myfeedbackTime + "ms\n";
                sendComplete(result);
            }
        } else {
            Log.e(TAG, "接收端异常代码");
        }


    }

    /**
     * 发送前的连接初始化，用于测试是否可以使用链路层，同时通知接收端，初始化参数。
     * <p>
     * 如果接收端接收到数据，则反馈给发送端可以发送数据，则触发发送。
     * <p>
     * <p>
     * 如果连接超时，则连接失败，倒计时关闭连接，回调通知失败
     * 如果连接通了，则在startSend处结束此异步
     */
    private void initSendConnect() {
        //
        //初始化
        timeoutCount = 0;
        //回调
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showRecvBitmap(Constants.send_init, Constants.SEND_FLAG_TIME * 3);//不使用showSendBitmap
            }
        }, Constants.INIT_CONNECT_DELAY);//

        //触发异步
        handler.removeCallbacks(initSendConnectTask);
        handler.post(initSendConnectTask);

    }


    /**
     * 发送端 发送数据
     *
     * <p>
     * 第一次发送(确保只有一次发送)
     */
    private void startSend() {
        //初始化连接图片倒计时可能没有结束，此处强制结束,避免影响发送数据的识别效率
        if (showTimer != null) {
            showTimer.cancel();
            showTimer = null;
            clearImageView();
        }
        removeConnectListener();//发送耗时，需要解除监听
        //结束初始化连接
        handler.removeCallbacks(initSendConnectTask);
        //保存当前时间节点。
        handler_lastTime = System.currentTimeMillis();
        lastSaveTime = System.currentTimeMillis();
        SPUtil.putLong(Constants.START_SEND_TIME, handler_lastTime);
        sendCounts = 0;
        SendMoreCount = 0;
        rcvImgSize = 0;
        sendBackLists = new ArrayList<>();//初始化数据
        clearImageView();

        //开启倒计时
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        //
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //发送结束标记，结束标记为：QrcodeContentSendOver+文件路径+文件大小（7位数）
                        try {
                            showSendBitmap(flag_send_over, Constants.SEND_FLAG_TIME);//结束符
                            feedbackTime = System.currentTimeMillis();//发送结束-加入时间
                            //结束倒计时
                            if (timer != null) {
                                timer.cancel();
                                timer = null;
                            }
                        } catch (Exception e) {
                            //已处理
                            e.printStackTrace();
                            if (timer != null) {
                                timer.cancel();
                                timer = null;
                            }
                        }
                        //开启聚焦任务
                        handler.removeCallbacks(focusTask);
                        handler.postDelayed(focusTask, 500);

                        long onceTime = System.currentTimeMillis() - handler_lastTime;//ms
                        myService.qrTransProgress(onceTime, 1, 1, send_transMSG);
                        lastSaveTime = System.currentTimeMillis();

                    }
                });

            }
        }, 100, Constants.DEFAULT_TIME);
    }

    /**
     * <p>
     * 发送端接收结束标记
     */
    private void sendComplete(String result) {
        removeConnectListener();//删除监听
        handler.removeCallbacks(initSendConnectTask);
        //统计 回调
        long qrstartTime = SPUtil.getLong(Constants.START_SEND_TIME, System.currentTimeMillis());//二维码开始时间
        long startTime = SPUtil.getLong(Constants.START_TIME, System.currentTimeMillis());//总时间
        long qrTime = System.currentTimeMillis() - qrstartTime;//二维码总耗时
        long time = System.currentTimeMillis() - startTime;//文件传输总耗时
        StringBuffer buffer = new StringBuffer();
//        buffer.append("传输的文件大小=" + fileSize + "B").append("\n");
        buffer.append("查询结果=" + result).append("\n");
        buffer.append(send_transMSG);//二维码发送速率
        buffer.append(send_feedBackMSG);//二维码缺失统计
        buffer.append("查询总耗时:" + qrTime + "ms");
        //回调
        myService.setAidlQrCodeComplete(time, buffer.toString());
        //清空数据
        clearSendParams();
        //清空其他数据
        SPUtil.putLong(Constants.START_SEND_TIME, 0);
        SPUtil.putLong(Constants.START_TIME, 0);
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

        //默认初始化为 接收端
        initRecvParams();
        initSendParams();


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

        getCacheFlag();


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

            //设置识别端
            clearRecvParams();
            initSendParams();//初始化发送参数
            SendMoreCount = 0;//初始化强制关闭的计数

            //赋值
            sendSearch = path;
            sendDatas = newData;
            sendSize = sendDatas.size();
            fileSize = mfileSize;
            Log.d(TAG, "onQrsend监听 拿到发送数据");
            //
            if (sendDatas != null &&
                    sendDatas.size() > 0 &&
                    sendSize > 0) {
                //发送数据

                //开启发送端的生产者
                firstProducerPos = 0;//初始化位置为0
                isFirstProducerExit = false;
                initFirstSendProducerTask();

                //发送对端数据，初始化二维码连接是否可用
                initSendConnect();
            } else {
                myService.isTrans(false, "myListener获取到空数据，无法发送");
            }
        }
    };

    /**
     * ======================================发送端 生产者(都在act中操作，方便直接拿到数据发送)==========================================
     */

    /**
     * 第一次发送的生产者(每次耗时)
     * 任务：
     * 1 创建发送标记bitmap
     * 2 创建发送数据（边生产边发送）
     */
    private void initFirstSendProducerTask() {
        if (executorService != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    //创建发送标记bitmap
                    if (flag_send_over == null) {
                        //取缓存
                        Bitmap bitmap = CacheUtils.getInstance().getBitmap(Constants.flag_send_over);
                        String len = CacheUtils.getInstance().getString(Constants.flag_send_over_length);
                        flag_send_over = new MyData(bitmap, Integer.parseInt(len), -1);
                    }

                    //创建发送数据
                    while (!isFirstProducerExit &&
                            firstProducerPos < sendSize
                            && firstSendQueue.size() < Constants.MAX_QR_COUNT) {

                        long time = System.currentTimeMillis();
                        //取缓存
                        Bitmap bitmap = BitmapCacheUtils.getInstance().getBitmap(Constants.key_bitmap + firstProducerPos);
                        String len = BitmapCacheUtils.getInstance().getString(Constants.key_len + firstProducerPos);
                        MyData data = new MyData(bitmap, Integer.parseInt(len), firstProducerPos);
                        //保存到队列中
                        firstSendQueue.addFirst(data);
                        firstProducerPos++;
                        //回调
                        myService.qrTransProgress((System.currentTimeMillis() - time), sendSize, firstProducerPos, "生产者二维码进度=" + (firstProducerPos * 100 / sendSize) + "%--获取缓存二维码耗时=" + (System.currentTimeMillis() - time) + "ms");
                    }
                }
            });
        }
    }


    //=====================================private处理==========================================


    /**
     * 发送端显示 结束标记
     *
     * @param data     不为空
     * @param showTime ms,显示时长
     * @return
     */
    private void showSendBitmap(final MyData data, final long showTime) {
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
                                updateConnectListener();//发送端耗时完成，添加监听(不可少)
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
            Log.e(TAG, "发送端--¬没有标记位显示");
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

    /**
     * 缓存获取标记
     * 发送端：初次登陆没有走service时，缓存为空，启动service保存了缓存走该方法，才能获取标记
     * 接收端：始终需要自己创建
     */
    private void getCacheFlag() {

        if (executorService != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (flag_recv_init == null) {//取缓存
                        String len = "";
                        Bitmap recv_init_bitmap = CacheUtils.getInstance().getBitmap(Constants.flag_recv_init);
                        if (recv_init_bitmap == null) {//创建
                            recv_init_bitmap = CodeUtils.createByMultiFormatWriter(Constants.recv_init, Constants.qrBitmapSize);
                            len = "" + ViewUtils.getImageViewWidth(Constants.recv_init.length());
                            //保存在缓存中
                            CacheUtils.getInstance().put(Constants.flag_recv_init, recv_init_bitmap);
                            CacheUtils.getInstance().put(Constants.flag_recv_init_length, len);
                        } else {//取缓存
                            len = CacheUtils.getInstance().getString(Constants.flag_recv_init_length);
                        }
                        Log.d(TAG, "onResume--flag_recv_init--len=" + len);
                        flag_recv_init = new MyData(recv_init_bitmap, Integer.parseInt(len), -1);
                    }
                    //02
                    if (flag_recv_success == null) {//取缓存
                        String len = "";
                        Bitmap save_success_bitmap = CacheUtils.getInstance().getBitmap(Constants.flag_recv_success);
                        if (flag_recv_success == null) {//创建
                            save_success_bitmap = CodeUtils.createByMultiFormatWriter(Constants.receiveOver_Content + Constants.SUCCESS, Constants.qrBitmapSize);
                            len = "" + ViewUtils.getImageViewWidth((Constants.receiveOver_Content + Constants.SUCCESS).length());
                            //保存在缓存中
                            CacheUtils.getInstance().put(Constants.flag_recv_success, save_success_bitmap);
                            CacheUtils.getInstance().put(Constants.flag_recv_success_length, len);
                        } else {
                            len = CacheUtils.getInstance().getString(Constants.flag_recv_success_length);
                        }
                        flag_recv_success = new MyData(save_success_bitmap, Integer.parseInt(len), -1);
                    }
                    //03
                    if (flag_recv_failed == null) {//取缓存
                        String len = "";
                        Bitmap save_failed_bitmap = CacheUtils.getInstance().getBitmap(Constants.flag_recv_failed);
                        if (flag_recv_failed == null) {////创建
                            save_failed_bitmap = CodeUtils.createByMultiFormatWriter(Constants.receiveOver_Content + Constants.FAILED, Constants.qrBitmapSize);
                            len = "" + ViewUtils.getImageViewWidth((Constants.receiveOver_Content + Constants.FAILED).length());
                            //保存在缓存中
                            CacheUtils.getInstance().put(Constants.flag_recv_failed, save_failed_bitmap);
                            CacheUtils.getInstance().put(Constants.flag_recv_failed_length, len);
                        } else {
                            len = CacheUtils.getInstance().getString(Constants.flag_recv_failed_length);
                        }
                        flag_recv_failed = new MyData(save_failed_bitmap, Integer.parseInt(len), -1);
                    }
                }
            });
        }
    }

}

