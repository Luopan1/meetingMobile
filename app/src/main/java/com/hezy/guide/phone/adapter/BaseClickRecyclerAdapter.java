package com.hezy.guide.phone.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**自定义的RecyclerView Adapter,实现了数据的添加,删除,点击事件
 * Created by wufan on 2017/8/3.
 */

public abstract class BaseClickRecyclerAdapter<T> extends RecyclerView.Adapter {
    public LayoutInflater mInflater;
    protected Context mContext;
    public List<T> mData = new ArrayList<>();


    public BaseClickRecyclerAdapter(Context context) {
        this.mContext = context;
    }

    public BaseClickRecyclerAdapter(Context context, List<T> data) {
        this.mContext = context;
        this.mData = data;

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(onSetItemLayout(), parent, false);
        return onSetViewHolder(view);
    }

    /**
     * 设置item的layout
     *
     * @return item对应的layout
     */
    protected abstract int onSetItemLayout();


    /**
     * 创建对应的ViewHolder
     * @param view
     * @return
     */
    protected abstract RecyclerView.ViewHolder onSetViewHolder(View view);


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        onSetItemData(holder, position);
        if(mListener != null){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onItemClick(v,position);
                }
            });
        }
    }


    /**
     * 为Item的内容设置数据
     *
     * @param viewHolder viewHolder
     * @param position   位置
     */
    protected abstract void onSetItemData(RecyclerView.ViewHolder viewHolder, int position);


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public OnItemClickListener mListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }


    @Override
    public int getItemCount() {
        if (mData != null) {
            return mData.size();
        } else {
            return 0;
        }
    }


    public void setData(List<T> data){
        mData.clear();
        mData.addAll(data);
    }

    public void addData(List<T> data){
        mData.addAll(data);
    }

    public void clearData() {
        mData.clear();
    }

    public List<T> getData(){
        return mData;
    }

}
