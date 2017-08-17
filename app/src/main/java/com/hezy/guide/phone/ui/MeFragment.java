package com.hezy.guide.phone.ui;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.MeFragmentBinding;
import com.hezy.guide.phone.entities.RankInfo;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.ui.adapter.ReviewAdapter;
import com.hezy.guide.phone.utils.LogUtils;

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
        mBinding.mRecyclerView.setLayoutManager(mLayoutManager);
        // 设置ItemAnimator
        mBinding.mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mBinding.mRecyclerView.setHasFixedSize(true);
//        mBinding.mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
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
                LogUtils.i(TAG,"newState == RecyclerView.SCROLL_STATE_IDLE "+(newState == RecyclerView.SCROLL_STATE_IDLE));
                LogUtils.i(TAG,"!mBinding.mSwipeRefreshLayout.isRefreshing() "+!mBinding.mSwipeRefreshLayout.isRefreshing());
                LogUtils.i(TAG,"lastVisibleItemPosition + 1 == mAdapter.getItemCount() "+(lastVisibleItemPosition + 1 == mAdapter.getItemCount()));
                LogUtils.i(TAG,"!(mPageNo == mTotalPage) "+!(mPageNo == mTotalPage));
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
                LogUtils.i(TAG,"lastVisibleItemPosition "+lastVisibleItemPosition);
            }
        });
    }

    @Override
    protected void initListener() {
    }

    @Override
    protected void requestData() {
        requestRankInfo();
        requestRecord();
    }

    @Override
    public void onMyVisible() {
        super.onMyVisible();


    }




    private void requestRankInfo() {
        ApiClient.getInstance().requestRankInfo(this, new OkHttpBaseCallback<BaseBean<RankInfo>>() {
            @Override
            public void onSuccess(BaseBean<RankInfo> entity) {
                if (entity == null || entity.getData() == null || TextUtils.isEmpty(entity.getData().getStar())) {
                    LogUtils.e(TAG, "获取评价信息数据为空");
                    return;
                }
                mAdapter.setRankInfo(entity.getData());
                mAdapter.notifyItemChanged(0);

            }
        });
    }




    private void requestRecord() {
        requestRecord("1", "20");
        isRefresh = true;
        mPageNo = 1;
    }

    private void requestRecord(String pageNo, String pageSize) {
        ApiClient.getInstance().requestRecord(this,"1", pageNo, pageSize, new OkHttpBaseCallback<BaseBean<RecordData>>() {
            @Override
            public void onSuccess(BaseBean<RecordData> entity) {
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
