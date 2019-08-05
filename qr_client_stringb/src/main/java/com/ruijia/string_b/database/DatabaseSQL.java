package com.ruijia.string_b.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 2018-4.19 sjy
 * <p>
 * 首页-到件扫描数据
 * <p>
 */

public class DatabaseSQL extends SQLiteOpenHelper {

    //**********************************基本设置**********************************
    private static final int DB_VERSION = 1;//数据库版本 默认都是1,涉及到更新需要修改

    private static final String DB_NAME = "clientb.db";//数据库文件名
    private static final String DB_TABLE_NAME = "persion_user";//表名

    /************************************表-字段属性 开始*************************************
     */

    //0 排序
    private static final String ORDERID = "_id";//表排序（非后台数据）
    //1
    private static final String EXP_NUMBER = "ExpNum";//身份证号
    //2
    private static final String EXP_CONTENT = "content";//信息

    //************************************表-字段属性 结束*************************************


    // **********************************构造*************************************
    public DatabaseSQL(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public DatabaseSQL(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE IF NOT EXISTS " +
                DB_TABLE_NAME + "(" +
                ORDERID + " integer primary key autoincrement ," +//参数0
                EXP_NUMBER + " text  UNIQUE," +//参数1 不为空且唯一
                EXP_CONTENT + " text" +//参数2
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_NAME);
        onCreate(db);
    }
    //******************************************添加操作***********************************************

    public long addOne(TestBean bean) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(EXP_NUMBER, bean.ExpNum);//1
        values.put(EXP_CONTENT, bean.content);//2


        long result = db.insert(DB_TABLE_NAME, null, values); // 组拼sql语句实现的.带返回值
        db.close();// 释放资源 TODO 需测试
        return result;
    }

    //******************************************删除操作***********************************************

    /**
     * 删除一条数据
     *
     * @param ExpNum 单号
     * @return result 删除了几行 0 代表删除失败
     */
    public int deleteOne(String ExpNum) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(DB_TABLE_NAME
                , EXP_NUMBER + "=?"
                , new String[]{ExpNum});
        db.close();// 释放资源
        return result;
    }

    /**
     * 查询一条数据
     *
     * @param key
     * @return
     */
    public String findByKey(String key) {
        String val = "";
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(DB_TABLE_NAME,
                new String[]{EXP_CONTENT},
                EXP_NUMBER + " = ?",
                new String[]{"%"+key+"%"},
                null,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            val = cursor.getString(0);
        }
        cursor.close();
//        db.close();
        return val;
    }

    /**
     * 异步  查询所有数据
     *
     * @return
     */
    public List<TestBean> findAll() {
        List<TestBean> listData = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(DB_TABLE_NAME,
                new String[]{EXP_NUMBER,//1
                        EXP_CONTENT//2
                },
                null,
                null,
                null,
                null,
                ORDERID + " desc",
                null);
        if (cursor != null) {
            while (cursor.moveToNext()) {

                String expNum = cursor.getString(0);//1
                String did = cursor.getString(1);//2

                TestBean bean = new TestBean(expNum, did);

                //添加列表中
                listData.add(bean);
            }
            cursor.close();
        } else {
        }
//                db.close();
        return listData;
    }

    /**
     * 上传数据成功，则清除所有的表数据，同时关闭 db---db.close()；
     */
    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + DB_TABLE_NAME);
        db.close();// 释放资源
    }

}
