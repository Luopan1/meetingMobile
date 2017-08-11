package com.hezy.guide.phone.ui;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.GuideLogFragmentBinding;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.RecordTotal;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.ui.adapter.GuideLogAdapter;

import rx.Subscription;


/**
 * Created by wufan on 2017/7/26.
 */

public class GuideLogFragment extends BaseDataBindingFragment<GuideLogFragmentBinding> {

    private LinearLayoutManager mLayoutManager;
    private GuideLogAdapter mAdapter;
    private boolean isRefresh;
    private int mTotalPage = -1;
    private int mPageNo = -1;

    public static GuideLogFragment newInstance() {
        GuideLogFragment fragment = new GuideLogFragment();
        return fragment;
    }

    @Override
    protected int initContentView() {
        return R.layout.guide_log_fragment;
    }



    @Override
    protected void initView() {

    }

    @Override
    protected void initAdapter() {
        mAdapter = new GuideLogAdapter(mContext);

        //设置布局管理器
        mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mBinding.mRecyclerView.setLayoutManager(mLayoutManager);
        mBinding.mRecyclerView.setAdapter(mAdapter);

        //刷新与分页加载
        mBinding.mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestRecord();
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
        mBinding.mLayoutNoData.setOnClickListener(this);
    }

    @Override
    protected void requestData() {
//        requestRecord();
    }

    @Override
    public void onResume() {
        super.onResume();
        requestRecord();

    }

    private void requestRecordTotal() {
        ApiClient.getInstance().requestRecordTotal(this, new OkHttpBaseCallback<BaseBean<RecordTotal>>() {
            @Override
            public void onSuccess(BaseBean<RecordTotal> entity) {
                if (entity == null || entity.getData() == null) {
                    showToast("数据为空");
                    return;
                }
                String time = String.valueOf(entity.getData().getTotal());
            }


        });
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
                    mBinding.mSwipeRefreshLayout.setVisibility(View.GONE);
                } else {
                    mBinding.mLayoutNoData.setVisibility(View.GONE);
                    mBinding.mSwipeRefreshLayout.setVisibility(View.VISIBLE);
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


    @Override
    protected void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mLayoutNoData:
                requestRecord();
                break;


        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
