package com.hezy.guide.phone.business;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.ForumMeetingAdapter;
import com.hezy.guide.phone.business.adapter.GeneralAdapter;
import com.hezy.guide.phone.business.adapter.MeetingAdapter;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.Forum;
import com.hezy.guide.phone.entities.ForumMeeting;
import com.hezy.guide.phone.entities.Meeting;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.base.BaseArrayBean;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.UIDUtil;
import com.hezy.guide.phone.utils.statistics.ZYAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.agora.openlive.ui.MeetingInitActivity;

public class MeetingsFragment extends BaseFragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private LinearLayoutManager mLayoutManager;
    private MeetingAdapter meetingAdapter;

    private TextView emptyText, tv_meeting_public, tv_meeting_private
    , tv_meeting_forum;
//    , tv_meeting_forum;
//    private AppBarLayout appBarLayout;

    public static final int TYPE_PUBLIC_MEETING = 0;
    public static final int TYPE_PRIVATE_MEETING = 1;
    public static final int TYPE_FORUM_MEETING = 2;
    private final int SEARCH_REQUEST_CODE = 1001;
    public static final String KEY_MEETING_TYPE = "meetingType";
    private int currentMeetingListPageIndex = TYPE_PUBLIC_MEETING;

    @Override
    public String getStatisticsTag() {
        return "会议列表";
    }

    private MeetingAdapter.OnItemClickListener onMeetingListItemClickListener = new MeetingAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, Meeting meeting) {
            if (Build.VERSION.SDK_INT >= 23) {
                //视频会议拍照功能
                int REQUEST_CODE_CONTACT = 101;
                String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA};
                //验证是否许可权限
                for (String str : permissions) {
                    if (getActivity().checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                        //申请权限
                        getActivity().requestPermissions(permissions, REQUEST_CODE_CONTACT);
                        return;
                    }
                }
            }
            initDialog(meeting);
        }
    };

    private ForumMeetingAdapter.OnItemClickListener onForumMeetingItemClickListener = new ForumMeetingAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(View view, ForumMeeting forumMeeting) {
            //TODO 跳转进讨论室界面
            ToastUtils.showToast(forumMeeting.getTitle());
            startActivity(new Intent(getActivity(),ChatActivity.class).putExtra("title",forumMeeting.getTitle()).putExtra("meetingId",forumMeeting.getMeetingId()));
        }
    };

    public static MeetingsFragment newInstance() {
        MeetingsFragment fragment = new MeetingsFragment();
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        showMeeting(currentMeetingListPageIndex);
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.meeting_fragment, null, false);
        swipeRefreshLayout = view.findViewById(R.id.mSwipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showMeeting(currentMeetingListPageIndex);
            }
        });
//        appBarLayout = view.findViewById(R.id.appBarLayout);
//        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
//            @Override
//            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
//                if (verticalOffset >= 0) {
//                    swipeRefreshLayout.setEnabled(true);
//                } else {
//                    swipeRefreshLayout.setEnabled(false);
//                }
//            }
//        });

        view.findViewById(R.id.search_text).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent searchMeetingIntent = new Intent(mContext, MeetingSearchActivity.class);
                searchMeetingIntent.putExtra(KEY_MEETING_TYPE, currentMeetingListPageIndex);
                MeetingsFragment.this.startActivityForResult(searchMeetingIntent, SEARCH_REQUEST_CODE);
            }
        });

        recyclerView = view.findViewById(R.id.mRecyclerView);
        mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(mLayoutManager);
        // 设置ItemAnimator
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
        emptyText = view.findViewById(R.id.emptyView);
        tv_meeting_public = view.findViewById(R.id.tv_meeting_public);
        tv_meeting_private = view.findViewById(R.id.tv_meeting_private);
        tv_meeting_forum = view.findViewById(R.id.tv_meeting_forum);
        tv_meeting_public.setOnClickListener(tvMeetingOnClickListener);
        tv_meeting_private.setOnClickListener(tvMeetingOnClickListener);
        tv_meeting_forum.setOnClickListener(tvMeetingOnClickListener);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SEARCH_REQUEST_CODE) {
            showMeeting(resultCode);
        }
    }

    private View.OnClickListener tvMeetingOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.tv_meeting_public:
                    showMeeting(TYPE_PUBLIC_MEETING);
                    break;
                case R.id.tv_meeting_private:
                    showMeeting(TYPE_PRIVATE_MEETING);
                    break;
                case R.id.tv_meeting_forum:
                    showMeeting(TYPE_FORUM_MEETING);
                    break;
            }
        }
    };

    private void showMeeting(int type) {
        //停止一切动画效果，包括recyclerView滚动效果，让appBarLayout常显，让刷新功能生效
        swipeRefreshLayout.setEnabled(true);
        recyclerView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
//        appBarLayout.setExpanded(true, true);

        switch (type) {
            case TYPE_PUBLIC_MEETING:
                showPublicMeeting();
                break;
            case TYPE_PRIVATE_MEETING:
                showPrivateMeeting();
                break;
            case TYPE_FORUM_MEETING:
                showForumMeeting();
                break;
        }
    }

    private void showPublicMeeting() {
        showPublicMeetingView();
        currentMeetingListPageIndex = TYPE_PUBLIC_MEETING;
        requestMeetings(TYPE_PUBLIC_MEETING);
    }

    private void showPrivateMeeting() {
        showPrivateMeetingView();
        currentMeetingListPageIndex = TYPE_PRIVATE_MEETING;
        requestMeetings(TYPE_PRIVATE_MEETING);
    }

    private void showForumMeeting() {
        showForumMeetingView();
        currentMeetingListPageIndex = TYPE_FORUM_MEETING;
        requestForum(null);
    }

    private void showPublicMeetingView() {
        TextViewCompat.setTextAppearance(tv_meeting_public, R.style.MeetingTypeFocus);
        TextViewCompat.setTextAppearance(tv_meeting_private, R.style.MeetingTypeUnFocus);
        TextViewCompat.setTextAppearance(tv_meeting_forum, R.style.MeetingTypeUnFocus);
    }

    private void showPrivateMeetingView() {
        TextViewCompat.setTextAppearance(tv_meeting_public, R.style.MeetingTypeUnFocus);
        TextViewCompat.setTextAppearance(tv_meeting_private, R.style.MeetingTypeFocus);
        TextViewCompat.setTextAppearance(tv_meeting_forum, R.style.MeetingTypeUnFocus);
    }

    private void showForumMeetingView() {
        TextViewCompat.setTextAppearance(tv_meeting_public, R.style.MeetingTypeUnFocus);
        TextViewCompat.setTextAppearance(tv_meeting_private, R.style.MeetingTypeUnFocus);
        TextViewCompat.setTextAppearance(tv_meeting_forum, R.style.MeetingTypeFocus);
    }

    /**
     * 请求会议类型
     *
     * @param type
     */
    private void requestMeetings(int type) {
        swipeRefreshLayout.setRefreshing(true);
        apiClient.getAllMeeting(TAG, null, type, meetingsCallback);
    }

    /**
     * 请求讨论组会议数据
     *
     * @param title
     */
    private void requestForum(String title) {
        if (title == null || title.equals("")) {
            title = null;
        }
        swipeRefreshLayout.setRefreshing(true);
        apiClient.getAllForumMeeting(TAG, title, forumMeetingsCallback);
    }

    private OkHttpCallback meetingsCallback = new OkHttpCallback<BaseArrayBean<Meeting>>() {

        @Override
        public void onSuccess(final BaseArrayBean<Meeting> meetingBucket) {
            if (meetingBucket.getData().size() > 0) {
                Logger.i("", meetingBucket.toString());
                meetingAdapter = new MeetingAdapter(mContext, meetingBucket.getData(), onMeetingListItemClickListener);
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

    private OkHttpCallback forumMeetingsCallback = new OkHttpCallback<Bucket<Forum>>() {

        @Override
        public void onSuccess(Bucket<Forum> entity) {
            Forum forum = entity.getData();
            ArrayList<ForumMeeting> forumMeetingList = forum.getPageData();
            if (forumMeetingList.size() == 0) {
                recyclerView.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
                return;
            }

            ForumMeetingAdapter forumMeetingAdapter = new ForumMeetingAdapter(mContext, forumMeetingList, onForumMeetingItemClickListener);
            recyclerView.setAdapter(new GeneralAdapter(forumMeetingAdapter));
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            ZYAgent.onEvent(getActivity().getApplicationContext(), exception.getMessage());
            ToastUtils.showToast("请求讨论区数据失败");
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
                    enterMeeting(codeEdit.getText().toString(), meeting);
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

    /**
     * 进入会议直播间
     *
     * @param joinCode 会议加入码
     * @param meeting  会议
     */
    private void enterMeeting(String joinCode, Meeting meeting) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
        params.put("meetingId", meeting.getId());
        params.put("token", joinCode);
        apiClient.verifyRole(TAG, verifyRoleCallback(meeting, joinCode), params);
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
            params.put("role", meetingJoin.getRole() == 0 ? "Publisher" : "Subscriber");
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
