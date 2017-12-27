package com.hezy.guide.phone.business;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.adapter.ReviewAdapter;
import com.hezy.guide.phone.databinding.MeFragmentBinding;
import com.hezy.guide.phone.entities.RankInfo;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.UserData;
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.RxBus;

public class MeetingsFragment extends BaseDataBindingFragment<MeFragmentBinding> {

    private LinearLayoutManager mLayoutManager;
    private ReviewAdapter mAdapter;
    private boolean isRefresh;
    private int mTotalPage = -1;
    private int mPageNo = -1;

    @Override
    public String getStatisticsTag() {
        return "会议";
    }

    public static MeetingsFragment newInstance() {
        MeetingsFragment fragment = new MeetingsFragment();
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
                Logger.i(TAG,"newState == RecyclerView.SCROLL_STATE_IDLE "+(newState == RecyclerView.SCROLL_STATE_IDLE));
                Logger.i(TAG,"!mBinding.mSwipeRefreshLayout.isRefreshing() "+!mBinding.mSwipeRefreshLayout.isRefreshing());
                Logger.i(TAG,"lastVisibleItemPosition + 1 == mAdapter.getItemCount() "+(lastVisibleItemPosition + 1 == mAdapter.getItemCount()));
                Logger.i(TAG,"!(mPageNo == mTotalPage) "+!(mPageNo == mTotalPage));
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
                Logger.i(TAG,"lastVisibleItemPosition "+lastVisibleItemPosition);
            }
        });
    }

    @Override
    protected void initListener() {
    }

    @Override
    protected void requestData() {

    }

    @Override
    public void onMyVisible() {
        super.onMyVisible();
        requestRankInfo();
        requestRecord();
        //用户信息也每次前台刷新
        requestUser();

    }

    public void requestUser() {
        if(!Preferences.isLogin()){
            return;
        }
        ApiClient.getInstance().requestUser(this, new OkHttpCallback<BaseBean<UserData>>() {
            @Override
            public void onSuccess(BaseBean<UserData> entity) {
                if (entity == null || entity.getData() == null || entity.getData().getUser() == null) {
                    showToast("数据为空");
                    return;
                }
                User user = entity.getData().getUser();
                Wechat wechat = entity.getData().getWechat();
                LoginHelper.savaUser(user);
//                initCurrentItem();
                if (wechat != null) {
                    LoginHelper.savaWeChat(wechat);
                }
                RxBus.sendMessage(new UserUpdateEvent());
                mAdapter.notifyDataSetChanged();

            }
        });
    }




    private void requestRankInfo() {
        ApiClient.getInstance().requestRankInfo(this, new OkHttpCallback<BaseBean<RankInfo>>() {
            @Override
            public void onSuccess(BaseBean<RankInfo> entity) {
                if (entity == null || entity.getData() == null || TextUtils.isEmpty(entity.getData().getStar())) {
                    Logger.e(TAG, "获取评价信息数据为空");
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
        ApiClient.getInstance().requestRecord(this,"1", pageNo, pageSize, new OkHttpCallback<BaseBean<RecordData>>() {
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
