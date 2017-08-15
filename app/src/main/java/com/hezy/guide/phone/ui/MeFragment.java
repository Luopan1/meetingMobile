package com.hezy.guide.phone.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.MeFragmentBinding;
import com.hezy.guide.phone.entities.RankInfo;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.StringUtils;
import com.hezy.guide.phone.utils.helper.ImageHelper;

import java.util.ArrayList;

/**
 * 我的
 * Created by wufan on 2017/8/15.
 */

public class MeFragment extends BaseDataBindingFragment<MeFragmentBinding> {

    public static MeFragment newInstance() {
        MeFragment fragment = new MeFragment();
        return fragment;
    }

    @Override
    protected int initContentView() {
        return R.layout.me_fragment;
    }


    @Override
    protected void initView() {

    }

    @Override
    protected void initListener() {
        mBinding.mIvHead.setOnClickListener(this);
    }

    @Override
    public void onMyVisible() {
        super.onMyVisible();
        ImageHelper.loadImageDpIdRound(Preferences.getUserPhoto(), R.dimen.my_px_460, R.dimen.my_px_426, mBinding.mIvHead);
        ImageHelper.loadImageDpIdBlur(Preferences.getUserPhoto(), R.dimen.my_px_1080, R.dimen.my_px_530, mBinding.mIvBack);

        mBinding.mTvName.setText(Preferences.getUserName());
        mBinding.mTvAddress.setText(Preferences.getUserAddress());
        requestRankInfo();

    }

    @Override
    protected void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mIvHead:
                UserinfoActivity.actionStart(mContext);
                break;

        }
    }

    private void requestRankInfo() {
        ApiClient.getInstance().requestRankInfo(this, new OkHttpBaseCallback<BaseBean<RankInfo>>() {
            @Override
            public void onSuccess(BaseBean<RankInfo> entity) {
                if (entity == null || entity.getData() == null || TextUtils.isEmpty(entity.getData().getStar())) {
                    LogUtils.e(TAG, "获取评价信息数据为空");
                    return;
                }
                setUIRankInfo(entity.getData());

            }
        });
    }

    private void setUIRankInfo(RankInfo rankInfo) {
        mBinding.mTvStar.setText(rankInfo.getStar());
        //TODO 分数规则半颗星.
        float star = Float.parseFloat(rankInfo.getStar());
        ArrayList<ImageView> views = new ArrayList<>();
        views.add(mBinding.mIvStar2);
        views.add(mBinding.mIvStar3);
        views.add(mBinding.mIvStar4);
        views.add(mBinding.mIvStar5);

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


        String percentStr = StringUtils.percent(rankInfo.getRatingFrequency(), rankInfo.getServiceFrequency());
        mBinding.mTvReviewRate.setText("评价率 " + percentStr);
        mBinding.mTvReviewCount.setText("连线" + rankInfo.getServiceFrequency() + "次 评价" + rankInfo.getRatingFrequency() + "次");

    }

}
