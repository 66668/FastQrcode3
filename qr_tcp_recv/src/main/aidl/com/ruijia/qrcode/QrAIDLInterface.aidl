// QrAIDLInterface.aidl
package com.ruijia.qrcode;

import com.ruijia.qrcode.QrProgressCallback;
/**
* 测试a与链路层的aidl,链路层做服务端，测试a做客户端
*/
interface QrAIDLInterface {


   void QRSend( String filePath);

   String QRRecv();

   boolean QrCtrl(int timeInterval,int StrLen);

   void register(QrProgressCallback listener);

   void unregister(QrProgressCallback listener);

}
