package com.ruijia.qrcode.utils;

import android.os.Environment;

/**
 * 原生摄像头的常量池
 */
public class Constants {

    public static String BASE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/qrcode_cache";
    public static String FILE_BITMAP_NAME = "qrcode_bitmap";
    public static String FILE_COMMON_NAME = "qrcode";
    /**
     * ===================发送端/接收端 通用标记=====================
     */
    public static String sendOver_Contnet = "QrcodeContentSendOver";//发送端 所有数据一次发送完成，发送结束标记
    public static String receiveOver_Content = "QrCodeContentReceiveOver";//接收端 完全收到数据，发送结束标记
    public static String recv_loss_all = "QrCodeReceiveLossAll";//接收端 丢失全部数据
    //
    public static String SUCCESS = "Success";//传输成功结束标记，和sendOver_Contnet和receiveOver_Content拼接使用 不要动
    public static String FAILED = "Failed";//传输失败结束标记，和sendOver_Contnet和receiveOver_Content拼接使用 不要动
    public static String endTag = "RJQR";//不要动
    public static String send_init = "QrcodeSENDCONNECTQrcodeSENDCONNECT";//发送端 发送连接信息，通知接收端初始化数据
    public static String recv_init = "QrcodeRECVCONNECTQrcodeRECVCONNECT";//接收端 发送连接信息，通知发送端发送数据

    /**
     * ===================发送端/接收端 通用标记--缓存文件对应的key值=====================
     */
    //~~~标记图的key~~~
    //
    public static String flag_recv_init = "flag_recv_init";
    public static String flag_recv_init_length = "flag_recv_init_length";
    //
    public static String flag_recv_success = "flag_recv_success";
    public static String flag_recv_success_length = "flag_recv_success_length";
    //
    public static String flag_recv_failed = "flag_recv_failed";
    public static String flag_recv_failed_length = "flag_recv_failed_length";

    public static String flag_send_over = "flag_send_over";
    public static String flag_send_over_length = "flag_send_over_length";

    public static String flag_send_complete = "flag_send_com";
    public static String flag_send_complete_length = "flag_send_com_length";

    //~~~内容的key~~~
    //bitmap key
    public static String key_bitmap = "key_bitmap";
    //内容长度 key
    public static String key_len = "key_len";

    //================================================================================
    //==========================流程控制 参数配置============================
    //================================================================================

    public static final int FOCUS_TIME = 550;//摄像头聚焦间隔（使二维码更好识别的暴力方式） ms

    public static final int TIMEOUT = 10;//连接超时 次数

    public static final int CONNECT_TIMEOUT = 30;//通讯超时  次数

    public static final int SEND_FLAG_TIME = 2500;//发送端：最后一张图显示时间 ms

    public static final int RECV_FLAG_TIME = 4000;//接收端：最后一张图显示时间 ms

    public static final int INIT_CONNECT_DELAY = 2000;//延迟连接对端，给线程池准备数据的时间

    public static final int MAX_QR_COUNT = 60;//链路线程池中最大二维码数量

    public static final int MIN_QR_COUNT = 10;//链路线程池中最小二维码数量

    /**
     * 发送时间间隔
     * <p>
     * 默认150
     */
    public static final String TIME_INTERVAL = "time_interval";

    /**
     * 发送时间间隔
     * <p>
     * 关键参数
     * <p>
     * 说明：和手机性能有关，性能好，可以设置低一些，最低大概在60～90之间（相机预览取帧的间隔时间）
     * 使用：
     * 小米5s的最低间隔是130，400～500的间隔数据能完全传输。
     * 所以在130～400之间找最低值且保证速率
     */
    public static final int DEFAULT_TIME = 190;//关键

    /**
     * 最大文件大小 默认3M
     */
    public static final String FILE_SIZE = "fileSize";
    public static final int DEFAULT_SIZE = 3;
    /**
     * 一张缺失图最大内容长度
     */
    public static final int LOST_LENGTH = 2600;

    public static final String CON_TIME_OUT = "connect_timeout";

    /**
     * 字符流 截取长度
     * <p>
     * zxing core 3.3.3 最大的传输容量2952,17长度做标记头和标记尾。2952-17=2935
     * <p>
     * 最强性能长度 2935
     */
    public static final int qrSize = 2930;//(保证最大传输，最好不要改)

    /**
     * zxing3.3.3库最大string值是2952
     */
    public static final int MAX_QR_SIZE = 2952;//

    public static final int qrBitmapSize = 200;//二维码最大支持的尺寸，默认为200（越小，二维码图片越模糊）


    /**
     * 识别过程，最大20次来回传图没有结果，强制结束
     */
    public static final int MAX_SEND_TIMES = 20;

    /**
     * 最开始的时间
     * <p>
     * 从链路层的service接收到文件开始的时间，
     * <p>
     * 很重要的标记，当识别完成后，最新时间 减去 该时间，就是文件传输的总耗时。
     */
    public static final String START_TIME = "startTime";

    /**
     * 准备发送二维码的时间
     * <p>
     * 很重要的标记，当识别完成后，最新时间 减去 该时间，就是二维码识别的总耗时。
     */
    public static final String START_SEND_TIME = "sendQrTime";



}

