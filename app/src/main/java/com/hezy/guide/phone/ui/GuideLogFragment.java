package com.hezy.guide.phone.ui;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.adorkable.iosdialog.ActionSheetDialog;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.GuideLogFragmentBinding;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.RecordTotal;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.service.WSService;
import com.hezy.guide.phone.ui.adapter.GuideLogAdapter;
import com.hezy.guide.phone.utils.RxBus;
import com.squareup.picasso.Picasso;

import rx.Subscription;
import rx.functions.Action1;


/**
 * Created by wufan on 2017/7/26.
 */

public class GuideLogFragment extends BaseDataBindingFragment<GuideLogFragmentBinding> {

    private Subscription subscription;
    private LinearLayoutManager mLayoutManager;
    private GuideLogAdapter mAdapter;

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
        if (!TextUtils.isEmpty(Preferences.getWeiXinHead())) {
            Picasso.with(BaseApplication.getInstance()).load(Preferences.getWeiXinHead()).into(mBinding.views.mIvHead);
        }

        setState(WSService.SOCKET_ONLINE);

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserStateEvent) {

                    setState(WSService.SOCKET_ONLINE);
                }
            }
        });
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
                requestRecordTotal();
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
                        && lastVisibleItemPosition + 1 == mAdapter.getItemCount() && !  mBinding.mSwipeRefreshLayout.isRefreshing()) {
//                    requestLiveVideoListNext();
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
        mBinding.views.mIvHead.setOnClickListener(this);
        mBinding.views.mTvState.setOnClickListener(this);
    }

    @Override
    protected void requestData() {
        requestRecordTotal();
        requestRecord();
    }

    private void requestRecordTotal(){
        ApiClient.getInstance().requestRecordTotal(this, new OkHttpBaseCallback<BaseBean<RecordTotal>>() {
            @Override
            public void onSuccess(BaseBean<RecordTotal> entity) {
                String time = String.valueOf(entity.getData().getTotal());
                mBinding.views.mTvTime.setText(time);
            }


        });
    }

    private void requestRecord(){
        ApiClient.getInstance().requestRecord(this, new OkHttpBaseCallback<BaseBean<RecordData>>() {
            @Override
            public void onSuccess(BaseBean<RecordData> entity) {
                mAdapter.setData(entity.getData().getPageData());
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
            case R.id.mTvState:
                new ActionSheetDialog(mContext).builder()//
                        .setCancelable(false)//
                        .setCanceledOnTouchOutside(false)//
                        .addSheetItem("在线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if(TextUtils.isEmpty(Preferences.getUserMobile())){
                                            showToast("请先填写电话号码");
                                            return;
                                        }
                                        if (!WSService.SOCKET_ONLINE) {
                                            //当前状态离线,可切换在线
                                            Log.i(TAG, "当前状态离线,可切换在线");
                                            RxBus.sendMessage(new SetUserStateEvent(true));
                                        }


                                    }
                                })
                        .addSheetItem("离线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if (WSService.SOCKET_ONLINE) {
                                            //当前状态在线,可切换离线
                                            Log.i(TAG, "当前状态在线,可切换离线");
                                            WSService.SOCKET_ONLINE = false;
                                            setState(false);
                                        }
                                    }
                                }).show();
                break;


        }
    }

    private void setState(boolean isOnline) {
        if (isOnline) {
            mBinding.views.mTvState.setText("在线状态");
            mBinding.views.mTvState.setBackgroundResource(R.drawable.userinfo_set_state_online_bg_shape);
        } else {
            mBinding.views.mTvState.setText("离线状态");
            mBinding.views.mTvState.setBackgroundResource(R.drawable.userinfo_set_state_offline_bg_shape);
        }
    }
}
