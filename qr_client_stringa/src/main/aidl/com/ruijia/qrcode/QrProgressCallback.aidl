// QrProgressCallback.aidl
package com.ruijia.qrcode;

// Declare any non-default types here with import statements
/**
* qr处理进度回调给客户端，方便了解进度
*/
interface QrProgressCallback {

    /**
    * 文件是否可以传递。
    */
    void isTrans(boolean isSuccess,String msg);

    /**
    * 文件转成字符流耗时。
    */
    void splitToIoTime(long time,String msg);

    /**
    * 字符流转List<String>耗时
    */
    void splitToArrayTime(long time,String msg);

    /**
     * List<String>转有标记List的耗时
     */
    void createNewArrayTime(long time,String msg);

    /**
    * 字符流分成片段 再合成二维码图的耗时
    */
    void createQrImgTime(long time,String msg);

    /**
    * 进度
    */
    void createQrImgProgress(int total,int position,String msg);
     /**
    * 二维码传输进度
    */
    void transProgress(long time,int total,int position,String msg);

    /**
    * 二维码传输耗时统计
    */
    void transTime(long time ,String msg);

    /**
    * 传输完成回调
    */
    void transComplete();
}
