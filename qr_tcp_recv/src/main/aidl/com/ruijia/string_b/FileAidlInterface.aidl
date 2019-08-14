// FileAidlInterface.aidl
package com.ruijia.string_b;

import com.ruijia.string_b.SearchResultCallback;

/**
* 测试b与链路层的aidl（测试b是服务端，链路层是客户端，单向通讯）
*/
interface FileAidlInterface {

   /**
   * 链路层将文件/搜索发送给 测试B软件
   */
    boolean QRRecv(String str);

    void register(SearchResultCallback listener);

    void unregister(SearchResultCallback listener);
}
