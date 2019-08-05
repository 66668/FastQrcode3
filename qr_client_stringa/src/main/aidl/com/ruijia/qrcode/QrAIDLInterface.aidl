// QrAIDLInterface.aidl
package com.ruijia.qrcode;

import com.ruijia.qrcode.QrProgressCallback;
// Declare any non-default types here with import statements

interface QrAIDLInterface {


   void QRSend( String filePath);

   String QRRecv();

   boolean QrCtrl(int timeInterval,int StrLen);

   void register(QrProgressCallback listener);

   void unregister(QrProgressCallback listener);

}
