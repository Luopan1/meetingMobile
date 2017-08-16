package com.hezy.guide.phone.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.listadapter.BaseRecyclerAdapter;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.utils.TimeUtil;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.helper.ImageHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;


/**
 * Created by wufan on 2017/8/16.
 */

public class ReviewAdapter extends BaseRecyclerAdapter<RecordData.PageDataEntity> {



    public ReviewAdapter(Context context) {
        super(context);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(mInflater.inflate(R.layout.review_item, parent, false));

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        RecordData.PageDataEntity bean = mData.get(position);
        String time = bean.getOrderTime();
        ViewHolder holder = (ViewHolder) viewHolder;

        ImageHelper.loadImageDpId(bean.getPhoto(), R.dimen.my_px_100, R.dimen.my_px_100, holder.mIvHead);
        holder.mTvName.setText(bean.getAddress() + " " + bean.getName());

        if (TimeUtil.isToday(time)) {
            holder.mTvTime.setText("今天" + " " + TimeUtil.getHM(time));
        } else {
            holder.mTvTime.setText(TimeUtil.getyyyyMMddHHmm(time));
        }

        //后台对评价过滤了的.一定有星星,也就是都是接听了的.有通话时间
        holder.mTvCallDuration.setText(" "+String.format("%02d", bean.getMinuteInterval()) + ":" + String.format("%02d", bean.getSecondInterval()));
        if(bean.getRatingStar()>1){
            holder.mIvStar2.setImageResource(R.mipmap.ic_star_good_record);
        }else{
            holder.mIvStar2.setImageResource(R.mipmap.ic_star_ungood_record);
        }
        if(bean.getRatingStar()>2){
            holder.mIvStar3.setImageResource(R.mipmap.ic_star_good_record);
        }else{
            holder.mIvStar3.setImageResource(R.mipmap.ic_star_ungood_record);
        }
        if(bean.getRatingStar()>3){
            holder.mIvStar4.setImageResource(R.mipmap.ic_star_good_record);
        }else{
            holder.mIvStar4.setImageResource(R.mipmap.ic_star_ungood_record);
        }
        if(bean.getRatingStar()>4){
            holder.mIvStar5.setImageResource(R.mipmap.ic_star_good_record);
        }else{
            holder.mIvStar5.setImageResource(R.mipmap.ic_star_ungood_record);
        }

        if(TextUtils.isEmpty(bean.getRatingContent())){
            holder.mTvReviewContent.setText("还未发表文字评价!");
            holder.mTvReviewContent.setTextColor(mContext.getResources().getColor(R.color.text_gray_909090));
            holder.mTvMeReplyContent.setVisibility(View.GONE);
            holder.mTvReply.setVisibility(View.GONE);
        }else{
            holder.mTvReviewContent.setText(bean.getRatingContent());
            holder.mTvReviewContent.setTextColor(mContext.getResources().getColor(R.color.text_black_434343));
            if(TextUtils.isEmpty(bean.getReplyRating())){
                holder.mTvMeReplyContent.setVisibility(View.GONE);
                holder.mTvReply.setVisibility(View.VISIBLE);
            }else{
                holder.mTvMeReplyContent.setVisibility(View.VISIBLE);
                holder.mTvReply.setVisibility(View.GONE);
            }
        }

        holder.mTvReply.setTag(R.id.tag_bean,bean);
        holder.mTvReply.setTag(R.id.tag_position,position);



    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener  {
        @BindView(R.id.mIvHead)
        CircleImageView mIvHead;
        @BindView(R.id.mTvName)
        TextView mTvName;
        @BindView(R.id.mTvTime)
        TextView mTvTime;
        @BindView(R.id.mTvCallDuration)
        TextView mTvCallDuration;
        @BindView(R.id.mIvStar1)
        ImageView mIvStar1;
        @BindView(R.id.mIvStar2)
        ImageView mIvStar2;
        @BindView(R.id.mIvStar3)
        ImageView mIvStar3;
        @BindView(R.id.mIvStar4)
        ImageView mIvStar4;
        @BindView(R.id.mIvStar5)
        ImageView mIvStar5;
        @BindView(R.id.mLayoutStar)
        LinearLayout mLayoutStar;
        @BindView(R.id.mTvReviewContent)
        TextView mTvReviewContent;
        @BindView(R.id.mTvMeReplyContent)
        TextView mTvMeReplyContent;
        @BindView(R.id.mTvReply)
        TextView mTvReply;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            mTvReply.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            RecordData.PageDataEntity bean=(RecordData.PageDataEntity) v.getTag(R.id.tag_bean);
            final int position=(int) v.getTag(R.id.tag_position);
            switch (v.getId()) {
                case R.id.mTvReply:
                    ToastUtils.showToast("点击");
                    break;
                default:
                    break;
            }
        }
    }



}
