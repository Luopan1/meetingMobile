package com.hezy.guide.phone.ui;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.MeFragmentBinding;
import com.hezy.guide.phone.entities.RankInfo;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.ui.adapter.ReviewAdapter;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.StringUtils;
import com.hezy.guide.phone.utils.helper.ImageHelper;

import java.util.ArrayList;

/**
 * 我的
 * Created by wufan on 2017/8/15.
 */

public class MeFragment extends BaseDataBindingFragment<MeFragmentBinding> {

    private LinearLayoutManager mLayoutManager;
    private ReviewAdapter mAdapter;
    private boolean isRefresh;
    private int mTotalPage = -1;
    private int mPageNo = -1;

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
    protected void initAdapter() {
        mAdapter = new ReviewAdapter(mContext);
        //设置布局管理器
        mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mLayoutManager.setSmoothScrollbarEnabled(true);
        mLayoutManager.setAutoMeasureEnabled(true);
        mBinding.mRecyclerView.setLayoutManager(mLayoutManager);
        mBinding.mRecyclerView.setHasFixedSize(true);
        mBinding.mRecyclerView.setNestedScrollingEnabled(false);
        mBinding.mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
        mBinding.mRecyclerView.setAdapter(mAdapter);

        //刷新与分页加载
        mBinding.mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestData();
            }
        });

        mBinding.mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastVisibleItemPosition;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView,
                                             int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && !mBinding.mSwipeRefreshLayout.isRefreshing()
                        && lastVisibleItemPosition + 1 == mAdapter.getItemCount()
                        && !(mPageNo == mTotalPage)) {
//                    requestLiveVideoListNext();
                    if (mPageNo != -1 && mTotalPage != -1 && !(mPageNo == mTotalPage)) {
                        requestRecord(String.valueOf(mPageNo + 1), "20");
                    }

                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
            }
        });
    }

    @Override
    protected void initListener() {
        mBinding.mIvHead.setOnClickListener(this);
    }

    @Override
    protected void requestData() {
        ImageHelper.loadImageDpIdRound(Preferences.getUserPhoto(), R.dimen.my_px_460, R.dimen.my_px_426, mBinding.mIvHead);
        ImageHelper.loadImageDpIdBlur(Preferences.getUserPhoto(), R.dimen.my_px_1080, R.dimen.my_px_530, mBinding.mIvBack);

        mBinding.mTvName.setText(Preferences.getUserName());
        mBinding.mTvAddress.setText(Preferences.getUserAddress());
        requestRankInfo();
        requestRecord();
    }

    @Override
    public void onMyVisible() {
        super.onMyVisible();


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


    private void requestRecord() {
        requestRecord("1", "20");
        isRefresh = true;
        mPageNo = 1;
    }

    private void requestRecord(String pageNo, String pageSize) {
        ApiClient.getInstance().requestRecord(this, pageNo, pageSize, new OkHttpBaseCallback<BaseBean<RecordData>>() {
            @Override
            public void onSuccess(BaseBean<RecordData> entity) {
                if (entity.getData().getTotalCount() == 0) {
                    mBinding.mLayoutNoData.setVisibility(View.VISIBLE);
                    mBinding.mRecyclerView.setVisibility(View.GONE);
                } else {
                    mBinding.mLayoutNoData.setVisibility(View.GONE);
                    mBinding.mRecyclerView.setVisibility(View.VISIBLE);
                }
                if (isRefresh) {
                    isRefresh = false;
                    mAdapter.setData(entity.getData().getPageData());
                } else {
                    mAdapter.addData(entity.getData().getPageData());
                }
                mPageNo = entity.getData().getPageNo();
                mTotalPage = entity.getData().getTotalPage();
                mAdapter.notifyDataSetChanged();

            }

            @Override
            public void onFinish() {
                super.onFinish();
                mBinding.mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

}
