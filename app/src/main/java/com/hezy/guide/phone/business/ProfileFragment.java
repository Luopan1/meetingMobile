package com.hezy.guide.phone.business;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zy.guide.phone.R;
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
import com.hezy.guide.phone.business.adapter.ReviewAdapter;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.RxBus;

public class ProfileFragment extends BaseFragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayoutManager mLayoutManager;
    private ReviewAdapter mAdapter;
    private boolean isRefresh;
    private int mTotalPage = -1;
    private int mPageNo = -1;

    @Override
    public String getStatisticsTag() {
        return "我的";
    }

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, null, false);
        swipeRefreshLayout = view.findViewById(R.id.mSwipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onMyVisible();
            }
        });

        recyclerView = view.findViewById(R.id.mRecyclerView);
        mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        // 设置ItemAnimator
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
//        recyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
        mAdapter = new ReviewAdapter(mContext);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastVisibleItemPosition;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                Logger.i(TAG, "newState == RecyclerView.SCROLL_STATE_IDLE " + (newState == RecyclerView.SCROLL_STATE_IDLE));
                Logger.i(TAG, "!mBinding.mSwipeRefreshLayout.isRefreshing() " + !swipeRefreshLayout.isRefreshing());
                Logger.i(TAG, "lastVisibleItemPosition + 1 == mAdapter.getItemCount() " + (lastVisibleItemPosition + 1 == mAdapter.getItemCount()));
                Logger.i(TAG, "!(mPageNo == mTotalPage) " + !(mPageNo == mTotalPage));
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && !swipeRefreshLayout.isRefreshing()
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
                Logger.i(TAG, "lastVisibleItemPosition " + lastVisibleItemPosition);
            }
        });

        return view;
    }

    @Override
    public void onMyVisible() {
        super.onMyVisible();

        if (!TextUtils.isEmpty(Preferences.getToken())) {
            requestRankInfo();

            requestRecord("1", "20");

            requestUser();
        }

    }

    public void requestUser() {
        if (!Preferences.isLogin()) {
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
        apiClient.requestRankInfo(this, new OkHttpCallback<BaseBean<RankInfo>>() {
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

    private void requestRecord(String pageNo, String pageSize) {
        apiClient.requestRecord(this, "1", pageNo, pageSize, new OkHttpCallback<BaseBean<RecordData>>() {
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
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        isRefresh = true;
        mPageNo = 1;
    }

}
