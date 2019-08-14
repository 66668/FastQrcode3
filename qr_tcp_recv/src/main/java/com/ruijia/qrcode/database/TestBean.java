package com.ruijia.qrcode.database;

/**
 * 本地单号数据的存储
 */
public class TestBean {
    public TestBean(String expNum, String content) {
        this.content = content;
        this.ExpNum = expNum;
    }

    // 派件信息id
    public String content;

    // 快递单号
    public String ExpNum;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExpNum() {
        return ExpNum;
    }

    public void setExpNum(String expNum) {
        ExpNum = expNum;
    }
}
