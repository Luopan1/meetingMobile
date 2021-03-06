package com.hezy.guide.phone.business;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.GeneralAdapter;
import com.hezy.guide.phone.business.adapter.MeetingAdapter;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.Meeting;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.base.BaseArrayBean;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.UIDUtil;

import java.util.HashMap;
import java.util.Map;

import io.agora.openlive.ui.MeetingInitActivity;

public class MeetingSearchActivity extends BasicActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private EditText searchEdit;
    private TextView cancelText;
    private LinearLayoutManager mLayoutManager;
    private MeetingAdapter meetingAdapter;
    private TextView emptyText;

    @Override
    public String getStatisticsTag() {
        return "会议搜索列表";
    }

    public MeetingAdapter.OnItemClickListener onItemClickListener = new MeetingAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, Meeting meeting) {
            initDialog(meeting);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_search);

        initView();
    }

    private void initView(){
        swipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (TextUtils.isEmpty(searchEdit.getText())) {
                    Toast.makeText(mContext, "搜索会议名称不能为空", Toast.LENGTH_SHORT).show();
                } else {
                    requestMeetings(searchEdit.getText().toString());
                }
            }
        });

        searchEdit = findViewById(R.id.search_text);
        cancelText = findViewById(R.id.cancel);
        cancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_SEARCH)) {
                    if (TextUtils.isEmpty(searchEdit.getText())) {
                        Toast.makeText(mContext, "搜索会议名称不能为空", Toast.LENGTH_SHORT).show();
                    } else {
                        requestMeetings(searchEdit.getText().toString());
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(s)){
                    cancelText.setVisibility(View.GONE);
                } else {
                    cancelText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        recyclerView = findViewById(R.id.mRecyclerView);
        mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        // 设置ItemAnimator
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
//        recyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
        emptyText = findViewById(R.id.emptyView);

        findViewById(R.id.mIvLeft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public void requestMeetings(String title) {
        swipeRefreshLayout.setRefreshing(true);
        apiClient.getAllMeeting(TAG, title, meetingsCallback);
    }

    private OkHttpCallback meetingsCallback = new OkHttpCallback<BaseArrayBean<Meeting>>() {

        @Override
        public void onSuccess(final BaseArrayBean<Meeting> meetingBucket) {
            if (meetingBucket.getData().size() > 0) {
                Logger.i("", meetingBucket.toString());
                meetingAdapter = new MeetingAdapter(mContext, meetingBucket.getData(), onItemClickListener);
                recyclerView.setAdapter(new GeneralAdapter(meetingAdapter));
                recyclerView.setVisibility(View.VISIBLE);
                emptyText.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
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
        final EditText codeEdit = view.findViewById(R.id.code);
        view.findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
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
            params.put("account", UIDUtil.generatorUID(Preferences.getUserId()));
            params.put("role", "Publisher");
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
