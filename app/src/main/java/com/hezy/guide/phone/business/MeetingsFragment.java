package com.hezy.guide.phone.business;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.GeneralAdapter;
import com.hezy.guide.phone.business.adapter.MeetingAdapter;
import com.hezy.guide.phone.entities.Meetings;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;

public class MeetingsFragment extends BaseFragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayoutManager mLayoutManager;
    private MeetingAdapter meetingAdapter;

    @Override
    public String getStatisticsTag() {
        return "会议";
    }

    public static MeetingsFragment newInstance() {
        MeetingsFragment fragment = new MeetingsFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.meeting_fragment, null, false);
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

        return view;
    }

    @Override
    public void onMyVisible() {
        super.onMyVisible();

        requestMeetings();

    }

    public void requestMeetings() {
        apiClient.getAllMeeting(TAG, meetingsCallback);

    }

    private OkHttpCallback meetingsCallback = new OkHttpCallback<BaseBean<Meetings>>() {

        @Override
        public void onSuccess(final BaseBean<Meetings> meetingBucket) {
            if (meetingBucket.getData().getCount() > 0) {
                Logger.i("", meetingBucket.toString());
                meetingAdapter = new MeetingAdapter(mContext, meetingBucket.getData().getList());
                recyclerView.setAdapter(new GeneralAdapter(meetingAdapter));
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.GONE);
            }
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

}
