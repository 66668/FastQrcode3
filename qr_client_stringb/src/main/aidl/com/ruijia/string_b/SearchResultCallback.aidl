// QrProgressCallback.aidl
package com.ruijia.string_b;

// Declare any non-default types here with import statements
/**
* qr处理进度回调给客户端，方便了解进度
*/
interface SearchResultCallback {

    /**
    *  返回结果
    */
    void searchResult(int code,String msg);
}
