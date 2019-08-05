package com.ruijia.string_b;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ruijia.string_b.database.DatabaseSQL;
import com.ruijia.string_b.database.ListAdapter;
import com.ruijia.string_b.database.TestBean;
import com.ruijia.string_b.listener.OnServiceAndActListener;
import com.ruijia.string_b.service.QRXmitService;

import java.util.List;

/**
 * 客户端B
 */
public class ClientBAct extends AppCompatActivity implements View.OnClickListener {
    EditText et_key, et_val;
    Button btn_save, btn_delete, btn_search;
    RecyclerView recylerView;

    //service相关
    ServiceConnection conn;
    QRXmitService myService = null;
    QRXmitService.FileAIDLServiceBinder myBinder = null;

    //数据库相关
    DatabaseSQL sql;
    ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_key = findViewById(R.id.et_key);
        et_val = findViewById(R.id.et_val);
        btn_save = findViewById(R.id.btn_save);
        btn_delete = findViewById(R.id.btn_delete);
        btn_search = findViewById(R.id.btn_search);
        recylerView = findViewById(R.id.recylerView);
        btn_save.setOnClickListener(this);
        btn_search.setOnClickListener(this);
        btn_delete.setOnClickListener(this);
        //
        sql = new DatabaseSQL(this);
        //
        initService();
        initAdatper();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conn != null) {
            unbindService(conn);
        }
    }

    //===============================假数据存储===============================

    private void initAdatper() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        adapter = new ListAdapter(this);
        recylerView.setLayoutManager(manager);
        recylerView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        if (v == btn_save) {
            String key = et_key.getText().toString();
            String val = et_val.getText().toString();
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(val)) {
                Toast.makeText(this, "数据不为空", Toast.LENGTH_SHORT).show();
            } else {
                TestBean bean = new TestBean(key, val);
                sql.addOne(bean);
            }

        } else if (btn_search == v) {
            String val = sql.findByKey("111111");
            et_val.setText("查询结果"+val);
            List<TestBean> beans = sql.findAll();
            if (beans != null && beans.size() > 0) {
                adapter.setList(beans);
            }

        } else if (btn_delete == v) {
            sql.clearAll();
            List<TestBean> beans = sql.findAll();
            if (beans != null && beans.size() > 0) {
                adapter.setList(beans);
            }
        }
    }

    //===============================act绑定service===============================
    private void initService() {
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                myBinder = (QRXmitService.FileAIDLServiceBinder) service;
                myService = myBinder.geSerVice();
                //绑定监听
                myService.setListener(myListener);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        //act绑定service
        Intent intent = new Intent(ClientBAct.this, QRXmitService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);//开启服务
    }

    /**
     * service的自定义回调
     */
    private OnServiceAndActListener myListener = new OnServiceAndActListener() {
        @Override
        public void onQrRecv(String selectPath) {
            Log.d("SJY", "MianAct获取路径 string_b=" + selectPath);

            //拿到路径后，测试B软件实现其他业务需要，需要开发方自己实现自己的业务即可。
        }
    };


}
