package com.hezy.guide.phone.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.listadapter.BaseRecyclerAdapter;
import com.hezy.guide.phone.entities.RecordData;

/**
 * Created by wufan on 2017/8/3.
 */

public class GuideLogAdapter2 extends BaseRecyclerAdapter<RecordData.PageDataEntity> {


    public enum ITEM_TYPE {
        TODAY_TOP,
        TODAY,
        HISTORY_TOP,
        HISTORY
    }

    public GuideLogAdapter2(Context context) {
        super(context);
    }



    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.guide_log_item, parent, false));
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

//        holder.mTvTimeDot.setText(TimeUtil.getMonth(time));
//        holder.mTvContent.setText(TimeUtil.getMonthDayHM(time)+" 为"+bean.getAddress()+"-"+bean.getName()
//        +"("+bean.getMobile()+")"+"讲解了"+bean.getMinuteInterval()+"分钟");

    }


    private static class ViewHolder extends RecyclerView.ViewHolder {
         View mLineTop;
         View mLineBottom;
         TextView mTvTimeDot;
         View mIvDot;
         TextView mTvContent;

        public ViewHolder(View view) {
            super(view);
//            mLineTop = view.findViewById(R.id.mLineTop);
//            mLineBottom = view.findViewById(R.id.mLineBottom);
//            mTvTimeDot = (TextView) view.findViewById(R.id.mTvTimeDot);
//            mIvDot = view.findViewById(R.id.mIvDot);
//            mTvContent = (TextView) view.findViewById(R.id.mTvContent);
        }
    }


}
