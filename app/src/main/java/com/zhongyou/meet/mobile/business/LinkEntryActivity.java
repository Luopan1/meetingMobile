package com.zhongyou.meet.mobile.business;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import com.alibaba.fastjson.JSON;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.entities.Agora;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.MeetingJoin;
import com.zhongyou.meet.mobile.entities.Version;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.ToastUtils;
import com.zhongyou.meet.mobile.utils.UIDUtil;
import com.tendcloud.tenddata.TCAgent;

import java.util.HashMap;
import java.util.Map;

import io.agora.openlive.ui.MeetingInitActivity;

/**
 * H5链接跳转进的Activity
 *
 * @author Dongce
 * create time: 2018/10/15
 */
public class LinkEntryActivity extends AppCompatActivity {

    private static final String TAG = LinkEntryActivity.class.getSimpleName();

    private Agora agora;
    private MeetingJoin meetingJoin;
    private ApiClient apiClient;
    private final String TCTAG = "LinkEntryMeetingRoom";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Preferences.isLogin()) {
            ToastUtils.showToast("您还未登录，请先登录" + getString(R.string.app_name));
            this.finish();
            return;
        }

        initView();
        initData();
        versionCheck();
    }

    private void versionCheck() {
        ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
            @Override
            public void onSuccess(BaseBean<Version> entity) {
                Version version = entity.getData();
                if (version == null || version.getImportance() == 0) {
                    return;
                }
                if (version.getImportance() != 1 && version.getImportance() != 2) {
                    startActivity(new Intent(getApplication(), UpdateActivity.class).putExtra("version", version));
                    finish();
                }


            }
        });
    }

    private void initData() {
        apiClient = ApiClient.getInstance();

        Uri h5Uri = getIntent().getData();
        if (h5Uri != null) {
            /*
             * H5页面内链接，直接跳转到会议直播界面<p>
             * 详见jira任务：OSG-273 {http://scrum.haierzhongyou.com/browse/OSG-273}
             */
            String joinCode = h5Uri.getQueryParameter("j");
            String meetingId = h5Uri.getQueryParameter("m");
            TCAgent.onEvent(this, TCTAG, "H5跳转进加入会议Activity，接收数据：joinCode=" + joinCode + " meetingId=" + meetingId);

            getMeetingInfo(joinCode, meetingId);
        }
    }

    private void initView() {
        Window window = getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);
        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);
    }

    private void getMeetingInfo(final String joinCode, final String meetingId) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
        params.put("meetingId", meetingId);
        params.put("token", joinCode);
        apiClient.verifyRole(TAG, verifyRoleCallback(meetingId, joinCode), params);
    }

    private OkHttpCallback verifyRoleCallback(final String meetingId, final String joinCode) {
        return new OkHttpCallback<Bucket<MeetingJoin>>() {
            @Override
            public void onSuccess(Bucket<MeetingJoin> meetingJoinBucket) {
                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
                params.put("meetingId", meetingId);
                params.put("token", joinCode);
                TCAgent.onEvent(LinkEntryActivity.this, TCTAG,
                        "clientUid=" + UIDUtil.generatorUID(Preferences.getUserId()) +
                                " | meetingId=" + meetingId +
                                " | token=" + joinCode);
                apiClient.joinMeeting(TAG, joinMeetingCallback, params);
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                TCAgent.onEvent(LinkEntryActivity.this, TCTAG,
                        "verifyRoleCallbackError errorCode=" +
                                errorCode +
                                " exception=" + exception.getMessage());
                super.onFailure(errorCode, exception);
            }
        };
    }

    private OkHttpCallback joinMeetingCallback = new OkHttpCallback<Bucket<MeetingJoin>>() {

        @Override
        public void onSuccess(Bucket<MeetingJoin> meetingJoinBucket) {
            meetingJoin = meetingJoinBucket.getData();
            Map<String, String> params = new HashMap<String, String>();
            params.put("channel", meetingJoin.getMeeting().getId());
            params.put("account", UIDUtil.generatorUID(Preferences.getUserId()));
            params.put("role", meetingJoin.getRole() == 0 ? "Publisher" : "Subscriber");
            apiClient.getAgoraKey(TAG, params, getAgoraCallback());
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            TCAgent.onEvent(LinkEntryActivity.this, TCTAG,
                    "joinMeetingCallback errorCode=" +
                            errorCode +
                            " exception=" + exception.getMessage());
            ToastUtils.showToast(exception.getMessage());
            finish();
        }

    };

    private OkHttpCallback getAgoraCallback() {
        return new OkHttpCallback<Bucket<Agora>>() {

            @Override
            public void onSuccess(Bucket<Agora> agoraBucket) {
                agora = agoraBucket.getData();
                TCAgent.onEvent(LinkEntryActivity.this, TCTAG,
                        "meetingJoin=" + JSON.toJSONString(meetingJoin) + " agora=" + JSON.toJSONString(agora));

                Intent intent = new Intent(LinkEntryActivity.this, MeetingInitActivity.class);
                intent.putExtra("meeting", meetingJoin);
                intent.putExtra("agora", agora);
                startActivity(intent);
                LinkEntryActivity.this.finish();
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                TCAgent.onEvent(LinkEntryActivity.this, TCTAG,
                        "getAgoraCallback errorCode=" + errorCode +
                                " exception=" + exception.getMessage());
            }
        };
    }
}
