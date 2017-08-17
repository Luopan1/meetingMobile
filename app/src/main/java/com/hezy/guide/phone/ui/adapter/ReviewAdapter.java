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
import com.hezy.guide.phone.entities.RankInfo;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.event.ReplyReviewEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.ui.ReplyReviewActivity;
import com.hezy.guide.phone.ui.UserinfoActivity;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.StringUtils;
import com.hezy.guide.phone.utils.TimeUtil;
import com.hezy.guide.phone.utils.helper.ImageHelper;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import rx.Subscription;
import rx.functions.Action1;


/**
 * Created by wufan on 2017/8/16.
 */

public class ReviewAdapter extends BaseRecyclerAdapter<RecordData.PageDataEntity> implements View.OnClickListener {
    RankInfo mRankInfo;
    private RecordData.PageDataEntity bean;

    public enum ITEM_TYPE {
        ME,
        REVIEW
    }


    public ReviewAdapter(Context context) {
        super(context);
        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof ReplyReviewEvent) {
                    ReplyReviewEvent event = (ReplyReviewEvent) o;
                    RecordData.PageDataEntity changeBean = event.getBean();
                    if(changeBean.getId().equals(bean.getId()) ){
                        bean.setReplyRating(changeBean.getReplyRating());
                        notifyDataSetChanged();
                    }

                }
            }
        });
    }


    private Subscription subscription;

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    public void setRankInfo(RankInfo rankInfo) {
        mRankInfo = rankInfo;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return ITEM_TYPE.ME.ordinal();
        } else {
            return ITEM_TYPE.REVIEW.ordinal();
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    private int getRealPosotion(int position) {
        return position - 1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE.ME.ordinal()) {
            return new MeViewHolder(mInflater.inflate(R.layout.me_item, parent, false), this);
        } else {
            return new ViewHolder(mInflater.inflate(R.layout.review_item, parent, false), this);
        }


    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        LogUtils.i(TAG, "position " + position);

        if (getItemViewType(position) == ITEM_TYPE.ME.ordinal()) {
            MeViewHolder meHolder = (MeViewHolder) viewHolder;
            ImageHelper.loadImageDpIdRound(Preferences.getUserPhoto(), R.dimen.my_px_460, R.dimen.my_px_426, meHolder.mIvHead);
            ImageHelper.loadImageDpIdBlur(Preferences.getUserPhoto(), R.dimen.my_px_1080, R.dimen.my_px_530, meHolder.mIvBack);
            meHolder.mTvName.setText(Preferences.getUserName());
            meHolder.mTvAddress.setText(Preferences.getUserAddress());
            if (mRankInfo != null) {
                meHolder.mTvStar.setText(mRankInfo.getStar());
                //TODO 分数规则半颗星.
                float star = Float.parseFloat(mRankInfo.getStar());
                ArrayList<ImageView> views = new ArrayList<>();
                views.add(meHolder.mIvStar2);
                views.add(meHolder.mIvStar3);
                views.add(meHolder.mIvStar4);
                views.add(meHolder.mIvStar5);

                for (int i = 0; i < views.size(); i++) {
                    ImageView ivStar = views.get(i);
                    if (star > 1.5 + i) {
                        ivStar.setImageResource(R.mipmap.ic_star_title);
                    } else if (star > 1 + i) {
                        ivStar.setImageResource(R.mipmap.ic_star_title_half);
                    } else {
                        ivStar.setImageResource(R.mipmap.ic_star_ungood);
                    }
                }


                String percentStr = StringUtils.percent(mRankInfo.getRatingFrequency(), mRankInfo.getServiceFrequency());
                meHolder.mTvReviewRate.setText("评价率 " + percentStr);
                meHolder.mTvReviewCount.setText("连线" + mRankInfo.getServiceFrequency() + "次 评价" + mRankInfo.getRatingFrequency() + "次");

            }
        } else {
            RecordData.PageDataEntity bean = mData.get(getRealPosotion(position));
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
            holder.mTvCallDuration.setText(" " + String.format("%02d", bean.getMinuteInterval()) + ":" + String.format("%02d", bean.getSecondInterval()));
            if (bean.getRatingStar() > 1) {
                holder.mIvStar2.setImageResource(R.mipmap.ic_star_good_record);
            } else {
                holder.mIvStar2.setImageResource(R.mipmap.ic_star_ungood_record);
            }
            if (bean.getRatingStar() > 2) {
                holder.mIvStar3.setImageResource(R.mipmap.ic_star_good_record);
            } else {
                holder.mIvStar3.setImageResource(R.mipmap.ic_star_ungood_record);
            }
            if (bean.getRatingStar() > 3) {
                holder.mIvStar4.setImageResource(R.mipmap.ic_star_good_record);
            } else {
                holder.mIvStar4.setImageResource(R.mipmap.ic_star_ungood_record);
            }
            if (bean.getRatingStar() > 4) {
                holder.mIvStar5.setImageResource(R.mipmap.ic_star_good_record);
            } else {
                holder.mIvStar5.setImageResource(R.mipmap.ic_star_ungood_record);
            }

            if (TextUtils.isEmpty(bean.getRatingContent())) {
                holder.mTvReviewContent.setText("还未发表文字评价!");
                holder.mTvReviewContent.setTextColor(mContext.getResources().getColor(R.color.text_gray_909090));
                holder.mTvMeReplyContent.setVisibility(View.GONE);
                holder.mIvMeReplyContentArrows.setVisibility(View.GONE);
                holder.mTvReply.setVisibility(View.GONE);
                holder.mIvReplyArray.setVisibility(View.GONE);
            } else {
                holder.mTvReviewContent.setText(bean.getRatingContent());
                holder.mTvReviewContent.setTextColor(mContext.getResources().getColor(R.color.text_black_434343));
                if (TextUtils.isEmpty(bean.getReplyRating())) {
                    holder.mTvMeReplyContent.setVisibility(View.GONE);
                    holder.mIvMeReplyContentArrows.setVisibility(View.GONE);
                    holder.mTvReply.setVisibility(View.VISIBLE);
                    holder.mIvReplyArray.setVisibility(View.VISIBLE);

                } else {
                    holder.mTvMeReplyContent.setVisibility(View.VISIBLE);
                    holder.mIvMeReplyContentArrows.setVisibility(View.VISIBLE);
                    holder.mTvMeReplyContent.setText(bean.getReplyRating());
                    holder.mTvReply.setVisibility(View.GONE);
                    holder.mIvReplyArray.setVisibility(View.GONE);
                }
            }

            holder.mTvReply.setTag(R.id.tag_bean, bean);
            holder.mTvReply.setTag(R.id.tag_position, position);
        }


    }


    static class ViewHolder extends RecyclerView.ViewHolder {
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
        @BindView(R.id.mIvMeReplyContentArrows)
        ImageView mIvMeReplyContentArrows;
        @BindView(R.id.mTvMeReplyContent)
        TextView mTvMeReplyContent;
        @BindView(R.id.mIvReplyArray)
        ImageView mIvReplyArray;
        @BindView(R.id.mTvReply)
        TextView mTvReply;

        ViewHolder(View view, View.OnClickListener listener) {
            super(view);
            ButterKnife.bind(this, view);
            mTvReply.setOnClickListener(listener);
        }


    }


    static class MeViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.mIvBack)
        ImageView mIvBack;
        @BindView(R.id.mIvHead)
        ImageView mIvHead;
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
        @BindView(R.id.mTvStar)
        TextView mTvStar;
        @BindView(R.id.mLayoutStar)
        LinearLayout mLayoutStar;
        @BindView(R.id.mTvReviewRate)
        TextView mTvReviewRate;
        @BindView(R.id.mTvReviewCount)
        TextView mTvReviewCount;
        @BindView(R.id.mTvName)
        TextView mTvName;
        @BindView(R.id.mTvAddress)
        TextView mTvAddress;

        MeViewHolder(View view, View.OnClickListener listener) {
            super(view);
            ButterKnife.bind(this, view);
            mIvHead.setOnClickListener(listener);
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.mIvHead:
                UserinfoActivity.actionStart(mContext);
                break;
            case R.id.mTvReply:
                bean = (RecordData.PageDataEntity) v.getTag(R.id.tag_bean);
//                ToastUtils.showToast("点击");
                ReplyReviewActivity.actionStart(mContext, bean);
                break;
            default:
                break;
        }
    }
}