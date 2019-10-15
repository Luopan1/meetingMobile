package com.zhongyou.meet.mobile.business;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.business.adapter.GuideLogAdapter;
import com.zhongyou.meet.mobile.entities.RecordData;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;

import static com.zhongyou.meet.mobile.business.MeetingsFragment.KEY_MEETING_TYPE;


/**
 * Created by wufan on 2017/7/26.
 */

public class RecordFragment extends BaseFragment {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private TextView emptyText;

    private LinearLayoutManager mLayoutManager;
    private GuideLogAdapter mAdapter;
    private boolean isRefresh;
    private int mTotalPage = -1;
    private int mPageNo = -1;
    private TextView mSearchView;

    @Override
    public String getStatisticsTag() {
        return "日志";
    }

    public static RecordFragment newInstance() {
        RecordFragment fragment = new RecordFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.record_fragment, null, false);
        mSwipeRefreshLayout = view.findViewById(R.id.mSwipeRefreshLayout);
        mRecyclerView = view.findViewById(R.id.mRecyclerView);
        emptyText = view.findViewById(R.id.emptyView);
        mAdapter = new GuideLogAdapter(mContext);

        mSearchView = view.findViewById(R.id.input_keyword);

        //设置布局管理器
        mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastVisibleItemPosition;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView,
                                             int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && !mSwipeRefreshLayout.isRefreshing()
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

        mSearchView.setOnClickListener(v -> {
            Intent searchMeetingIntent = new Intent(RecordFragment.this.getContext(), MeetingSearchActivity.class);
            searchMeetingIntent.putExtra(KEY_MEETING_TYPE, MeetingsFragment.TYPE_PUBLIC_MEETING);
            startActivity(searchMeetingIntent);
        });

        //刷新与分页加载
        mSwipeRefreshLayout.setOnRefreshListener(() -> requestRecord());

        requestRecord();

        return view;
    }

    private void requestRecord() {
        requestRecord("1", "20");
        isRefresh = true;
        mPageNo = 1;
    }

    private void requestRecord(String pageNo, String pageSize) {
        ApiClient.getInstance().requestRecord(this,null, pageNo, pageSize, new OkHttpCallback<BaseBean<RecordData>>() {
            @Override
            public void onSuccess(BaseBean<RecordData> entity) {
                if (entity.getData().getTotalCount() == 0) {
                    emptyText.setVisibility(View.VISIBLE);
                    mSwipeRefreshLayout.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    mSwipeRefreshLayout.setVisibility(View.VISIBLE);
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
                mSwipeRefreshLayout.setRefreshing(false);
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

}
