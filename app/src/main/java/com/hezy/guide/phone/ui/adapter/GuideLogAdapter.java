package com.hezy.guide.phone.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
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
        TODAY,
        HISTORY_TOP,
        HISTORY
    }

    public GuideLogAdapter(Context context) {
        super(context);
    }

    @Override
    public int getItemViewType(int position) {
        RecordData.PageDataEntity bean = mData.get(position);
        String time = bean.getCallStartTime();
        if (TimeUtil.isToday(time)) {
            if (position == 0) {
                return ITEM_TYPE.TODAY_TOP.ordinal();
            } else {
                return ITEM_TYPE.TODAY.ordinal();
            }

        } else {
            if (position == 0 || TimeUtil.isToday(mData.get(position - 1).getCallStartTime())) {
                //第一个,或者上一个是今天
                return ITEM_TYPE.HISTORY_TOP.ordinal();
            } else {
                return ITEM_TYPE.HISTORY.ordinal();
            }

        }

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE.TODAY_TOP.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_today_top_item, parent, false));
        } else if (viewType == ITEM_TYPE.TODAY.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_today_item, parent, false));
        } else if (viewType == ITEM_TYPE.HISTORY_TOP.ordinal()) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_item_history_top, parent, false));
        } else {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_history_item, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        RecordData.PageDataEntity bean = mData.get(position);
        String time = bean.getCallStartTime();
        ViewHolder holder= (ViewHolder) viewHolder;
//        if(position != 0){
//            holder.
//        }else{
//            holder.mTvTimeDot.setVisibility(View.VISIBLE);
//        }

        holder.mTvTimeDot.setText(TimeUtil.getMonth(time));
        holder.mTvContent.setText(TimeUtil.getMonthDayHM(time)+" 为"+bean.getAddress()+"-"+bean.getName()
        +"("+bean.getMobile()+")"+"讲解了"+bean.getMinuteInterval()+"分钟");

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
