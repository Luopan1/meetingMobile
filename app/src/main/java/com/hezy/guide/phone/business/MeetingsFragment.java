package com.hezy.guide.phone.business;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.GeneralAdapter;
import com.hezy.guide.phone.business.adapter.MeetingAdapter;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.Meeting;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.Meetings;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.UIDUtil;

import java.util.HashMap;
import java.util.Map;

import io.agora.openlive.ui.MeetingInitActivity;
import io.agora.rtc.video.CameraHelper;

public class MeetingsFragment extends BaseFragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayoutManager mLayoutManager;
    private MeetingAdapter meetingAdapter;

    @Override
    public String getStatisticsTag() {
        return "会议";
    }

    public MeetingAdapter.OnItemClickListener onItemClickListener = new MeetingAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, Meeting meeting) {
            initDialog(meeting);
        }
    };

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
                meetingAdapter = new MeetingAdapter(mContext, meetingBucket.getData().getList(), onItemClickListener);
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

        @Override
        public void onFinish() {
            super.onFinish();
            swipeRefreshLayout.setRefreshing(false);
        }
    };

    private Dialog dialog;

    private void initDialog(final Meeting meeting) {
        View view = View.inflate(mContext, R.layout.dialog_meeting_code, null);
        final EditText codeEdit = (EditText) view.findViewById(R.id.code);
        view.findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(codeEdit.getText())) {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
                    params.put("meetingId", meeting.getId());
                    params.put("token", codeEdit.getText().toString());
                    apiClient.verifyRole(TAG, verifyRoleCallback(meeting, codeEdit.getText().toString()), params);
                } else {
                    codeEdit.setError("会议加入码不能为空");
                }
            }
        });
        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog = new Dialog(mContext, R.style.CustomDialog);
        dialog.setContentView(view);
        dialog.show();
    }

    private OkHttpCallback verifyRoleCallback(final Meeting meeting, final String token) {
        return new OkHttpCallback<Bucket<MeetingJoin>>() {

            @Override
            public void onSuccess(Bucket<MeetingJoin> meetingJoinBucket) {
                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
                params.put("meetingId", meeting.getId());
                params.put("token", token);
                apiClient.joinMeeting(TAG, joinMeetingCallback, params);
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                super.onFailure(errorCode, exception);
                Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private OkHttpCallback joinMeetingCallback = new OkHttpCallback<Bucket<MeetingJoin>>() {

        @Override
        public void onSuccess(Bucket<MeetingJoin> meetingJoinBucket) {
            MeetingJoin meetingJoin = meetingJoinBucket.getData();
            Map<String, String> params = new HashMap<String, String>();
            params.put("channel", meetingJoin.getMeeting().getId());
            params.put("uid", UIDUtil.generatorUID(Preferences.getUserId()));
            apiClient.getAgoraKey(mContext, params, getAgoraCallback(meetingJoin));
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private OkHttpCallback getAgoraCallback(final MeetingJoin meetingJoin) {
        return new OkHttpCallback<Bucket<Agora>>() {

            @Override

            public void onSuccess(Bucket<Agora> agoraBucket) {
                dialog.dismiss();
                Intent intent = new Intent(mContext, MeetingInitActivity.class);
                intent.putExtra("agora", agoraBucket.getData());
                intent.putExtra("meeting", meetingJoin);
                startActivity(intent);
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                Toast.makeText(mContext, "网络异常，请稍后重试！", Toast.LENGTH_SHORT).show();
            }

        };
    }

}
