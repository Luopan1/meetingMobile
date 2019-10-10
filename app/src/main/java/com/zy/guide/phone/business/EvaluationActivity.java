package com.zy.guide.phone.business;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.zy.guide.phone.R;
import com.zy.guide.phone.business.adapter.ReviewAdapter;
import com.zy.guide.phone.entities.RankInfo;
import com.zy.guide.phone.entities.RecordData;
import com.zy.guide.phone.entities.base.BaseBean;
import com.zy.guide.phone.persistence.Preferences;
import com.zy.guide.phone.utils.Logger;
import com.zy.guide.phone.utils.OkHttpCallback;

/**
 * @author luopan
 * 评价
 */
public class EvaluationActivity extends BasicActivity {
	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView recyclerView;
	private LinearLayoutManager mLayoutManager;
	private ReviewAdapter mAdapter;
	private boolean isRefresh;
	private int mTotalPage = -1;
	private int mPageNo = -1;
	@Override
	public String getStatisticsTag() {
		return "评价";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_evaluation);
		//设置标题和返回键
		findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		((TextView)findViewById(R.id.title)).setText(getStatisticsTag().toString());
		initViews();
		//获取数据
		onMyVisible();
	}

	private void initViews() {
		swipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout);
		swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				onMyVisible();
			}
		});

		recyclerView = findViewById(R.id.mRecyclerView);
		mLayoutManager = new LinearLayoutManager(mContext);
		mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(mLayoutManager);
		// 设置ItemAnimator
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.setHasFixedSize(true);
		mAdapter = new ReviewAdapter(mContext);
		recyclerView.setAdapter(mAdapter);

		recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
			private int lastVisibleItemPosition;

			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				/*Logger.i(TAG, "newState == RecyclerView.SCROLL_STATE_IDLE " + (newState == RecyclerView.SCROLL_STATE_IDLE));
				Logger.i(TAG, "!mBinding.mSwipeRefreshLayout.isRefreshing() " + !swipeRefreshLayout.isRefreshing());
				Logger.i(TAG, "lastVisibleItemPosition + 1 == mAdapter.getItemCount() " + (lastVisibleItemPosition + 1 == mAdapter.getItemCount()));
				Logger.i(TAG, "!(mPageNo == mTotalPage) " + !(mPageNo == mTotalPage));*/
				if (newState == RecyclerView.SCROLL_STATE_IDLE
						&& !swipeRefreshLayout.isRefreshing()
						&& lastVisibleItemPosition + 1 == mAdapter.getItemCount()
						&& !(mPageNo == mTotalPage)) {
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

	}

	public void onMyVisible() {
		requestRankInfo();
		requestRecord("1", "20");

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
