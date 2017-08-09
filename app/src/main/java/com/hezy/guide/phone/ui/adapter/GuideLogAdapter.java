package com.hezy.guide.phone.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.listadapter.BaseRecyclerAdapter;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.utils.TimeUtil;

/**
 * Created by wufan on 2017/8/3.
 */

public class GuideLogAdapter extends BaseRecyclerAdapter<RecordData.PageDataEntity> {


    public enum ITEM_TYPE {
        TODAY_TOP,
        TODAY_END,
        TODAY,
        HISTORY_TOP,
        HISTORY_END,
        HISTORY
    }

    public GuideLogAdapter(Context context) {
        super(context);
    }

    @Override
    public int getItemViewType(int position) {
        RecordData.PageDataEntity bean = mData.get(position);
        String time = bean.getOrderTime();

        if (TimeUtil.isToday(time)) {
            if (position == 0) {
                return ITEM_TYPE.TODAY_TOP.ordinal();
            } else if (position == getItemCount() - 1 || !TimeUtil.isToday(mData.get(position + 1).getOrderTime())) {

                return ITEM_TYPE.TODAY_END.ordinal();
            } else {
                return ITEM_TYPE.TODAY.ordinal();
            }

        } else {
            if (position == 0 || TimeUtil.isToday(mData.get(position - 1).getOrderTime())) {
                //第一个,或者上一个是今天
                return ITEM_TYPE.HISTORY_TOP.ordinal();
            } else if (position == getItemCount() - 1) {
                return ITEM_TYPE.HISTORY_END.ordinal();
            } else {
                return ITEM_TYPE.HISTORY.ordinal();
            }

        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE.TODAY_TOP.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_today_top_item, parent, false));
        } else if (viewType == ITEM_TYPE.TODAY_END.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_today_end_item, parent, false));
        } else if (viewType == ITEM_TYPE.TODAY.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_today_item, parent, false));
        } else if (viewType == ITEM_TYPE.HISTORY_TOP.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_history_top_item, parent, false));
        } else if (viewType == ITEM_TYPE.HISTORY_END.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_history_end_item, parent, false));
        } else {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_history_item, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        RecordData.PageDataEntity bean = mData.get(position);
        String time = bean.getOrderTime();
        ViewHolder holder = (ViewHolder) viewHolder;
        if (position != 0 && TimeUtil.isDayEqual(time, mData.get(position - 1).getOrderTime())) {
            holder.mTvTimeDot.setVisibility(View.INVISIBLE);
            holder.mIvDot.setVisibility(View.VISIBLE);
        } else {
            holder.mTvTimeDot.setVisibility(View.VISIBLE);
            holder.mIvDot.setVisibility(View.INVISIBLE);
        }

        if(TimeUtil.isToday(time)){
            holder.mTvTimeDot.setText("今");
        }else{
            holder.mTvTimeDot.setText(TimeUtil.getDay(time));
        }

        String html;
        if (bean.getStatus() == 2) {
            html = "<font color='#ff6482'>" + TimeUtil.getMonthDayHM(time)
                    + " 您拒绝接听" + bean.getAddress() + "-" + bean.getName()
                    + "(" + "<font color='#b985e2'>" + bean.getMobile() + "</font>" + ")" + "的通话" + "</font>";
        } else if (bean.getStatus() == 8) {
            html ="<font color='#ff6482'>" + TimeUtil.getMonthDayHM(time)
                    + " " + bean.getAddress() + "-" + bean.getName()
                    + "(" + "<font color='#b985e2'>" + bean.getMobile() + "</font>" + ")" + "挂断了呼叫" + "</font>";
        } else if (bean.getMinuteInterval() != 0) {
            html = TimeUtil.getMonthDayHM(time) + " 为" + bean.getAddress() + "-" + bean.getName()
                    + "(" + "<font color='#b985e2'>" + bean.getMobile() + "</font>" + ")" + "讲解了"
                    + "<font color='#ff9c00'>" + bean.getMinuteInterval() + "</font>" + "分钟"
                    + "<font color='#ff9c00'>" + bean.getSecondInterval() + "</font>" + "秒";
        } else {
            html = TimeUtil.getMonthDayHM(time) + " 为" + bean.getAddress() + "-" + bean.getName()
                    + "(" + "<font color='#b985e2'>" + bean.getMobile() + "</font>" + ")" + "讲解了"
                    + "<font color='#ff9c00'>" + bean.getSecondInterval() + "</font>" + "秒";
        }


        holder.mTvContent.setText(Html.fromHtml(html));

    }


    private static class ViewHolder extends RecyclerView.ViewHolder {
        View mLineTop;
        View mLineBottom;
        TextView mTvTimeDot;
        View mIvDot;
        TextView mTvContent;

        public ViewHolder(View view) {
            super(view);
            mLineTop = view.findViewById(R.id.mLineTop);
            mLineBottom = view.findViewById(R.id.mLineBottom);
            mTvTimeDot = (TextView) view.findViewById(R.id.mTvTimeDot);
            mIvDot = view.findViewById(R.id.mIvDot);
            mTvContent = (TextView) view.findViewById(R.id.mTvContent);
        }
    }


}
