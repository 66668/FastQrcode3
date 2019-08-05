package com.ruijia.string_b.database;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.ruijia.string_b.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 揽收扫描-数据展示
 */

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.LanshouScanHolder> {
    private List<TestBean> list = new ArrayList<>();
    LayoutInflater inflater;
    Context context;

    public ListAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        notifyDataSetChanged();
    }

    public ListAdapter(Context context, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.onItemListener = onItemClickListener;
        inflater = LayoutInflater.from(context);
        notifyDataSetChanged();
    }

    public ListAdapter(Context context, List<TestBean> dataList, OnItemClickListener onItemClickListener) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.list = dataList;
        this.onItemListener = onItemClickListener;
        notifyDataSetChanged();
    }

    public void setList(List<TestBean> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    //构造点击接口
    public OnItemClickListener onItemListener;

    public interface OnItemClickListener {
        void onItemDeleteListener(View view, int position, TestBean bean);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemListener = onItemClickListener;
    }

    @Override
    public LanshouScanHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_data, parent, false);
        LanshouScanHolder holder = new LanshouScanHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final LanshouScanHolder holder, final int position) {
        final TestBean bean = list.get(position);
        if (bean != null) {
            holder.tv_num.setText(bean.getExpNum());
            holder.tv_content.setText(bean.getContent());
        }
    }


    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    /**
     * 自定义holder
     */
    class LanshouScanHolder extends RecyclerView.ViewHolder {
        TextView tv_num;
        TextView tv_content;

        public LanshouScanHolder(View itemView) {
            super(itemView);
            tv_num = itemView.findViewById(R.id.tv_num);
            tv_content = itemView.findViewById(R.id.tv_content);
        }
    }
}
