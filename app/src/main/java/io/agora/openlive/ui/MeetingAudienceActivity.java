package io.agora.openlive.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Audience;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.HostUser;
import com.hezy.guide.phone.entities.Material;
import com.hezy.guide.phone.entities.Meeting;
import com.hezy.guide.phone.entities.MeetingHostingStats;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.MeetingJoinStats;
import com.hezy.guide.phone.entities.MeetingMaterialsPublish;
import com.hezy.guide.phone.meetingcamera.activity.Camera1ByServiceActivity;
import com.hezy.guide.phone.meetingcamera.activity.Camera1ByServiceActivity;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.UIDUtil;
import com.squareup.picasso.Picasso;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Timer;
import java.util.TimerTask;

import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.openlive.model.AGEventHandler;
import io.agora.openlive.model.ConstantApp;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class MeetingAudienceActivity extends BaseActivity implements AGEventHandler {

    private final static Logger LOG = LoggerFactory.getLogger(MeetingAudienceActivity.class);

    private final String TAG = MeetingAudienceActivity.class.getSimpleName();
    private Timer timer;
    private TimerTask timerTask;

    private MeetingJoin meetingJoin;
    private Meeting meeting;
    private Agora agora;
    private String broadcastId;

    private FrameLayout broadcasterLayout, broadcasterSmallLayout, broadcasterSmallView, audienceLayout, audienceView;
    private TextView broadcastNameText, broadcastTipsText, countText, audienceNameText;
    private Button requestTalkButton, stopTalkButton;
    private TextView exitButton, pageText;
    private ImageView docImage;
    private ImageButton fullScreenButton;

    private boolean isDocShow = false;

    private Material currentMaterial;
    private MeetingMaterialsPublish currentMaterialPublish;
    private int doc_index = 0;

    private String channelName;

    private String audienceName;

    private SurfaceView remoteBroadcasterSurfaceView, remoteAudienceSurfaceView, localSurfaceView;

    private AgoraAPIOnlySignal agoraAPI;

    private static final String DOC_INFO = "doc_info";
    private static final String CALLING_AUDIENCE = "calling_audience";

    private int currentAudienceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_audience);
        TCAgent.onEvent(this, "进入会议直播界面");

    }

    @Override
    protected void onResume() {
        super.onResume();
        TCAgent.onPageStart(this, "视频通话");
    }

    @Override
    protected void onPause() {
        super.onPause();
        TCAgent.onPageEnd(this, "视频通话");
    }

    private boolean handsUp = false;
    private boolean isFullScreen = false;

    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);

        Intent intent = getIntent();
        agora = intent.getParcelableExtra("agora");
        meetingJoin = intent.getParcelableExtra("meeting");
        meeting = meetingJoin.getMeeting();

        broadcastId = meetingJoin.getHostUser().getClientUid();

        config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));
        Log.v("uid--->", "" + config().mUid);

        channelName = meetingJoin.getMeeting().getId();

        audienceName = (TextUtils.isEmpty(Preferences.getAreaName()) ? "" : Preferences.getAreaName()) + "-" + (TextUtils.isEmpty(Preferences.getUserCustom()) ? "" : Preferences.getUserCustom()) + "-" + Preferences.getUserName();

        broadcasterLayout = findViewById(R.id.broadcaster_view);
        broadcastTipsText = findViewById(R.id.broadcast_tips);
        broadcastNameText = findViewById(R.id.broadcaster);
        broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());
        broadcasterSmallLayout = findViewById(R.id.broadcaster_small_layout);
        broadcasterSmallView = findViewById(R.id.broadcaster_small_view);
        docImage = findViewById(R.id.doc_image);
        pageText = findViewById(R.id.page);
        fullScreenButton = findViewById(R.id.full_screen);
        fullScreenButton.setOnClickListener(v -> {
            if (!isFullScreen) {
                fullScreenButton.setImageResource(R.drawable.ic_full_screened);
                if (audienceView.getChildCount() > 0) {
                    stopTalkButton.setVisibility(View.GONE);
                    audienceView.removeAllViews();
                    audienceLayout.setVisibility(View.GONE);
                }
                requestTalkButton.setVisibility(View.GONE);
                if (broadcasterSmallView.getChildCount() > 0) {
                    broadcasterSmallLayout.setVisibility(View.GONE);
                    stopTalkButton.setVisibility(View.GONE);
                }
                isFullScreen = true;
            } else {
                fullScreenButton.setImageResource(R.drawable.ic_full_screen);
                if (localSurfaceView != null || remoteAudienceSurfaceView != null) {
                    audienceLayout.setVisibility(View.VISIBLE);
                    if (localSurfaceView != null) {
                        audienceView.removeAllViews();
                        audienceView.addView(localSurfaceView);
                        stopTalkButton.setVisibility(View.VISIBLE);
                        requestTalkButton.setVisibility(View.GONE);
                    }
                    if (remoteAudienceSurfaceView != null) {
                        audienceView.removeAllViews();
                        audienceView.addView(remoteAudienceSurfaceView);
                        requestTalkButton.setVisibility(View.VISIBLE);
                        stopTalkButton.setVisibility(View.GONE);
                    }
                } else {
                    requestTalkButton.setVisibility(View.VISIBLE);
                }
                if (broadcasterSmallView.getChildCount() > 0) {
                    broadcasterSmallLayout.setVisibility(View.VISIBLE);
                }
                isFullScreen = false;
            }
        });

        audienceLayout = findViewById(R.id.audience_layout);
        audienceView = findViewById(R.id.audience_view);
        audienceNameText = findViewById(R.id.audience_name);

        countText = findViewById(R.id.online_count);

        stopTalkButton = findViewById(R.id.stop_talk);
        stopTalkButton.setOnClickListener(view -> {
            showDialog(2, "确定终止发言吗？", "取消", "确定", null);
        });

        requestTalkButton = findViewById(R.id.request_talk);
        requestTalkButton.setOnClickListener(view -> {
            if (remoteBroadcasterSurfaceView != null) {
                if (handsUp) {
                    handsUp = false;
                    requestTalkButton.setText("我要发言");
                    requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                } else {
                    handsUp = true;
                    requestTalkButton.setText("放弃发言");
                    requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup_giveup, 0, 0, 0);
                }
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("uid", config().mUid);
                    jsonObject.put("uname", audienceName);
                    jsonObject.put("handsUp", handsUp);
                    jsonObject.put("callStatus", 0);
                    jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                    jsonObject.put("postTypeName", Preferences.getUserPostType());
                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(MeetingAudienceActivity.this, "主持人加入才能申请发言", Toast.LENGTH_SHORT).show();
            }
        });

        exitButton = findViewById(R.id.exit);
        exitButton.setOnClickListener(view -> {
            showDialog(1, "确定退出会议吗？", "取消", "确定", null);
        });

        doConfigEngine(Constants.CLIENT_ROLE_AUDIENCE);

        agoraAPI = AgoraAPIOnlySignal.getInstance(this, agora.getAppID());
        agoraAPI.callbackSet(new AgoraAPI.CallBack() {

            @Override
            public void onLoginSuccess(int uid, int fd) {
                super.onLoginSuccess(uid, fd);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登录信令系统成功", Toast.LENGTH_SHORT).show());
                }
                agoraAPI.channelJoin(channelName);
            }

            @Override
            public void onLoginFailed(final int ecode) {
                super.onLoginFailed(ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登录信令系统失败" + ecode, Toast.LENGTH_SHORT).show());
                }
                if ("true".equals(agora.getIsTest())) {
                    agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
                } else {
                    agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
                }

            }

            @Override
            public void onLogout(int ecode) {
                super.onLogout(ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登出信令系统成功" + ecode, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onChannelJoined(String channelID) {
                super.onChannelJoined(channelID);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "观众登录信令频道成功", Toast.LENGTH_SHORT).show();
                    }
                    agoraAPI.queryUserStatus(broadcastId);
                    agoraAPI.channelQueryUserNum(channelName);
                });
            }

            @Override
            public void onReconnecting(int nretry) {
                super.onReconnecting(nretry);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "信令重连失败第" + nretry + "次", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onReconnected(int fd) {
                super.onReconnected(fd);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "信令系统重连成功", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onQueryUserStatusResult(String name, String status) {
                super.onQueryUserStatusResult(name, status);
                if (name.equals(meetingJoin.getHostUser().getClientUid()) && "1".equals(status)) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("uid", config().mUid);
                        jsonObject.put("uname", audienceName);
                        jsonObject.put("handsUp", handsUp);
                        jsonObject.put("callStatus", 0);
                        jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                        jsonObject.put("postTypeName", Preferences.getUserPostType());
                        agoraAPI.messageInstantSend(name, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onChannelJoinFailed(String channelID, int ecode) {
                super.onChannelJoinFailed(channelID, ecode);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "观众登录信令频道失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onChannelQueryUserNumResult(String channelID, int ecode, final int num) {
                super.onChannelQueryUserNumResult(channelID, ecode, num);
                runOnUiThread(() -> countText.setText("在线人数：" + num));
            }

            @Override
            public void onChannelUserJoined(String account, int uid) {
                super.onChannelUserJoined(account, uid);
                runOnUiThread(() -> {
                    Log.v("onChannelUserJoined", "观众" + account + "加入房间了---" + meetingJoin.getHostUser().getClientUid());
                    agoraAPI.channelQueryUserNum(channelName);
                });
            }

            @Override
            public void onChannelUserLeaved(String account, int uid) {
                super.onChannelUserLeaved(account, uid);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "用户" + account + "退出信令频道", Toast.LENGTH_SHORT).show();
                    }
                    agoraAPI.channelQueryUserNum(channelName);
                });
            }

            @Override
            public void onUserAttrResult(final String account, final String name, final String value) {
                super.onUserAttrResult(account, name, value);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "获取到用户" + account + "的属性" + name + "的值为" + value, Toast.LENGTH_SHORT).show();
                    }

                    fullScreenButton.setVisibility(View.VISIBLE);

                    audienceLayout.setVisibility(View.VISIBLE);
                    if ("uname".equals(name)) {
                        if (TextUtils.isEmpty(value)) {
                            audienceNameText.setText("");
                        } else {
                            audienceNameText.setText(value);
                        }
                    }
                });
            }

            @Override
            public void onMessageSendSuccess(String messageID) {
                super.onMessageSendSuccess(messageID);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> {
                        Toast.makeText(MeetingAudienceActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onMessageSendError(String messageID, int ecode) {
                super.onMessageSendError(messageID, ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> {
                        Toast.makeText(MeetingAudienceActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onMessageInstantReceive(final String account, final int uid, final String msg) {
                super.onMessageInstantReceive(account, uid, msg);
                runOnUiThread(() -> {
                    try {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(MeetingAudienceActivity.this, "接收到消息" + msg, Toast.LENGTH_SHORT).show();
                        }
                        JSONObject jsonObject = new JSONObject(msg);
                        if (jsonObject.has("response")) {
                            boolean result = jsonObject.getBoolean("response");
                            if (result) { // 连麦成功
                                if (BuildConfig.DEBUG) {
                                    Toast.makeText(MeetingAudienceActivity.this, "主持人要和我连麦", Toast.LENGTH_SHORT).show();
                                }

                                fullScreenButton.setVisibility(View.VISIBLE);

                                audienceLayout.setVisibility(View.VISIBLE);

                                audienceView.removeAllViews();

                                remoteAudienceSurfaceView = null;

                                agoraAPI.setAttr("uname", audienceName); // 设置当前登录用户的相关属性值。

                                localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                                localSurfaceView.setZOrderOnTop(true);
                                localSurfaceView.setZOrderMediaOverlay(true);
                                rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
                                audienceView.addView(localSurfaceView);

                                audienceNameText.setText(audienceName);

                                stopTalkButton.setVisibility(View.VISIBLE);
                                requestTalkButton.setVisibility(View.GONE);

                                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

                                HashMap<String, Object> params = new HashMap<String, Object>();
                                params.put("status", 1);
                                params.put("meetingId", meetingJoin.getMeeting().getId());
                                ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                            } else {
                                if (BuildConfig.DEBUG) {
                                    Toast.makeText(MeetingAudienceActivity.this, "主持人拒绝和我连麦", Toast.LENGTH_SHORT).show();
                                }
                                stopTalkButton.setVisibility(View.GONE);
                                requestTalkButton.setVisibility(View.VISIBLE);
                            }
                            handsUp = false;
                            requestTalkButton.setText("我要发言");
                            requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                        }
                        if (jsonObject.has("finish")) {
                            boolean finish = jsonObject.getBoolean("finish");
                            if (finish) {
                                audienceView.removeAllViews();
                                audienceNameText.setText("");
                                audienceLayout.setVisibility(View.GONE);

                                agoraAPI.setAttr("uname", null);

                                localSurfaceView = null;

                                if (!isDocShow) {
                                    fullScreenButton.setVisibility(View.GONE);
                                }
                                requestTalkButton.setVisibility(View.VISIBLE);
                                stopTalkButton.setVisibility(View.GONE);

                                handsUp = false;
                                requestTalkButton.setText("我要发言");
                                requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);

                                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

                                if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                                    HashMap<String, Object> params = new HashMap<String, Object>();
                                    params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                                    params.put("status", 2);
                                    params.put("meetingId", meetingJoin.getMeeting().getId());
                                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onMessageChannelReceive(String channelID, String account, int uid, final String msg) {
                super.onMessageChannelReceive(channelID, account, uid, msg);
                runOnUiThread(() -> {
                    try {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(MeetingAudienceActivity.this, "接收到频道消息：" + msg, Toast.LENGTH_SHORT).show();
                        }
                        JSONObject jsonObject = new JSONObject(msg);
                        if (jsonObject.has("material_id") && jsonObject.has("doc_index")) {
                            agoraAPI.channelQueryUserNum(channelName);
                            doc_index = jsonObject.getInt("doc_index");
                            String materialId = jsonObject.getString("material_id");

                            if (currentMaterial != null) {
                                if (!materialId.equals(currentMaterial.getId())) {
                                    ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
                                } else {
                                    currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);
                                    if (remoteBroadcasterSurfaceView != null) {
                                        broadcasterLayout.removeView(remoteBroadcasterSurfaceView);
                                        broadcasterLayout.setVisibility(View.GONE);

                                        if (isFullScreen) {
                                            broadcasterSmallLayout.setVisibility(View.GONE);
                                        } else {
                                            broadcasterSmallLayout.setVisibility(View.VISIBLE);
                                        }
                                        if (broadcasterSmallView.getChildCount() == 0) {
                                            broadcasterSmallView.removeAllViews();
                                            broadcasterSmallView.addView(remoteBroadcasterSurfaceView);
                                        }
                                    }
                                    pageText.setVisibility(View.VISIBLE);
                                    pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
                                    docImage.setVisibility(View.VISIBLE);
                                    Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);

                                    fullScreenButton.setVisibility(View.VISIBLE);
                                }
                            } else {
                                ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
                            }
                        }
                        if (jsonObject.has("finish_meeting")) {
                            boolean finishMeeting = jsonObject.getBoolean("finish_meeting");
                            if (finishMeeting) {
                                if (BuildConfig.DEBUG) {
                                    Toast.makeText(MeetingAudienceActivity.this, "主持人结束了会议", Toast.LENGTH_SHORT).show();
                                }
                                doLeaveChannel();
                                if (agoraAPI.getStatus() == 2) {
                                    agoraAPI.logout();
                                }
                                finish();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onChannelAttrUpdated(String channelID, String name, String value, String type) {
                super.onChannelAttrUpdated(channelID, name, value, type);
                Log.v("onChannelAttrUpdated", "channelID:" + channelID + ", name:" + name + ", value:" + value + ", type:" + type);
                runOnUiThread(() -> {
                    if (CALLING_AUDIENCE.equals(name)) {
                        if (TextUtils.isEmpty(value)) {
                            if (remoteAudienceSurfaceView != null) {
                                remoteAudienceSurfaceView = null;
                            }
                            if (localSurfaceView != null) {
                                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                                audienceView.removeAllViews();
                                localSurfaceView = null;
                                if (!isDocShow) {
                                    fullScreenButton.setVisibility(View.GONE);
                                }
                                agoraAPI.setAttr("uname", null);

                                if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                                    HashMap<String, Object> params = new HashMap<String, Object>();
                                    params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                                    params.put("status", 2);
                                    params.put("meetingId", meetingJoin.getMeeting().getId());
                                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                                }
                            }
                            audienceView.removeAllViews();
                            audienceNameText.setText("");
                            audienceLayout.setVisibility(View.GONE);
                            stopTalkButton.setVisibility(View.GONE);
                            requestTalkButton.setVisibility(View.VISIBLE);

                            handsUp = false;
                            requestTalkButton.setText("我要发言");
                            requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                        } else {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(MeetingAudienceActivity.this, "收到主持人设置的连麦人ID：" + value + ", \ntype:" + type, Toast.LENGTH_SHORT).show();
                            }
                            currentAudienceId = Integer.parseInt(value);
                            if (currentAudienceId == config().mUid) { // 连麦人是我

                                agoraAPI.setAttr("uname", audienceName); // 设置正在连麦的用户名

                                remoteAudienceSurfaceView = null;

                                audienceLayout.setVisibility(View.VISIBLE);
                                audienceNameText.setText(audienceName);
                                localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                                localSurfaceView.setZOrderOnTop(true);
                                localSurfaceView.setZOrderMediaOverlay(true);
                                rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
                                audienceView.removeAllViews();
                                audienceView.addView(localSurfaceView);

                                requestTalkButton.setVisibility(View.GONE);
                                stopTalkButton.setVisibility(View.VISIBLE);
                                fullScreenButton.setVisibility(View.VISIBLE);

                                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

                                HashMap<String, Object> params = new HashMap<String, Object>();
                                params.put("status", 1);
                                params.put("meetingId", meetingJoin.getMeeting().getId());
                                ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                            } else {  // 连麦人不是我
                                if (localSurfaceView != null) {
                                    worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                                    agoraAPI.setAttr("uname", null);
                                    localSurfaceView = null;

                                    requestTalkButton.setVisibility(View.VISIBLE);
                                    stopTalkButton.setVisibility(View.GONE);

                                    if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                                        HashMap<String, Object> params = new HashMap<String, Object>();
                                        params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                                        params.put("status", 2);
                                        params.put("meetingId", meetingJoin.getMeeting().getId());
                                        ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                                    }
                                }
                            }
                        }
                    }
                    if ("doc_info".equals(name)) {
                        agoraAPI.channelQueryUserNum(channelName);
                        if (!TextUtils.isEmpty(value)) {
                            try {
                                JSONObject jsonObject = new JSONObject(value);
                                if (jsonObject.has("material_id") && jsonObject.has("doc_index")) {
                                    isDocShow = true;
                                    doc_index = jsonObject.getInt("doc_index");
                                    if (BuildConfig.DEBUG) {
                                        Toast.makeText(MeetingAudienceActivity.this, "收到主持人端index：" + doc_index, Toast.LENGTH_SHORT).show();
                                    }
                                    String materialId = jsonObject.getString("material_id");
                                    if (currentMaterial != null) {
                                        if (!materialId.equals(currentMaterial.getId())) {
                                            ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
                                        } else {
                                            currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);
                                            if (remoteBroadcasterSurfaceView != null) {
                                                broadcasterLayout.removeView(remoteBroadcasterSurfaceView);
                                                broadcasterLayout.setVisibility(View.GONE);

                                                if (isFullScreen) {
                                                    broadcasterSmallLayout.setVisibility(View.GONE);
                                                } else {
                                                    broadcasterSmallLayout.setVisibility(View.VISIBLE);
                                                }
                                                if (broadcasterSmallView.getChildCount() == 0) {
                                                    broadcasterSmallView.removeAllViews();
                                                    broadcasterSmallView.addView(remoteBroadcasterSurfaceView);
                                                }
                                            }
                                            pageText.setVisibility(View.VISIBLE);
                                            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
                                            docImage.setVisibility(View.VISIBLE);
                                            Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);

                                            fullScreenButton.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            broadcasterSmallView.removeAllViews();
                            if (remoteBroadcasterSurfaceView != null) {
                                broadcasterSmallView.removeView(remoteBroadcasterSurfaceView);
                            }
                            broadcasterSmallLayout.setVisibility(View.GONE);

                            pageText.setVisibility(View.GONE);
                            docImage.setVisibility(View.GONE);

                            currentMaterial = null;
                            currentMaterialPublish = null;

                            isDocShow = false;

                            broadcasterLayout.setVisibility(View.VISIBLE);
                            broadcasterLayout.removeAllViews();
                            if (remoteBroadcasterSurfaceView != null) {
                                broadcasterLayout.addView(remoteBroadcasterSurfaceView);
                            }

                            if (remoteAudienceSurfaceView != null) {
                                if (isFullScreen) {
                                    audienceLayout.setVisibility(View.GONE);
                                } else {
                                    audienceLayout.setVisibility(View.VISIBLE);
                                }
                                fullScreenButton.setVisibility(View.VISIBLE);
                                audienceView.removeAllViews();
                                audienceView.addView(remoteAudienceSurfaceView);
                            } else {
                                fullScreenButton.setVisibility(View.GONE);
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(final String name, final int ecode, final String desc) {
                super.onError(name, ecode, desc);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> {
                        if (ecode != 208)
                            Toast.makeText(MeetingAudienceActivity.this, "收到错误信息\nname: " + name + "\necode: " + ecode + "\ndesc: " + desc, Toast.LENGTH_SHORT).show();
                    });
                }
                if (agoraAPI.getStatus() != 1 && agoraAPI.getStatus() != 2 && agoraAPI.getStatus() != 3) {
                    if ("true".equals(agora.getIsTest())) {
                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
                    } else {
                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
                    }
                }
            }

            @Override
            public void onLog(String txt) {
                super.onLog(txt);
                Log.v("audience信令", txt);
            }
        });

        ApiClient.getInstance().getMeetingHost(TAG, meeting.getId(), joinMeetingCallback(0));
//        startMeetingCamera();
    }

    private void startMeetingCamera() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                startActivity(new Intent(MeetingAudienceActivity.this, Camera1ByServiceActivity.class));
            }
        };
        timer.schedule(timerTask, 10 * 1000, 10 * 1000);
    }

    private OkHttpCallback joinMeetingCallback(int uid){
        return new OkHttpCallback<Bucket<HostUser>>() {

            @Override
            public void onSuccess(Bucket<HostUser> meetingJoinBucket) {
                meetingJoin.setHostUser(meetingJoinBucket.getData());
                broadcastId = meetingJoinBucket.getData().getClientUid();
                broadcastNameText.setText("主持人：" + meetingJoinBucket.getData().getHostUserName());
                if (uid != 0) {
                    if (uid == Integer.parseInt(broadcastId)) {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(MeetingAudienceActivity.this, "主持人" + broadcastId + "---" + uid + meetingJoin.getHostUser().getHostUserName() + "进入了", Toast.LENGTH_SHORT).show();
                        }

                        agoraAPI.queryUserStatus(broadcastId);

                        broadcastTipsText.setVisibility(View.GONE);

                        remoteBroadcasterSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                        remoteBroadcasterSurfaceView.setZOrderOnTop(false);
                        remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
                        rtcEngine().setupRemoteVideo(new VideoCanvas(remoteBroadcasterSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                        if (isDocShow) {
                            broadcasterSmallLayout.setVisibility(View.VISIBLE);
                            broadcasterSmallView.removeAllViews();
                            broadcasterSmallView.addView(remoteBroadcasterSurfaceView);

                            fullScreenButton.setVisibility(View.VISIBLE);
                        } else {
                            broadcasterLayout.setVisibility(View.VISIBLE);
                            broadcasterLayout.removeAllViews();
                            broadcasterLayout.addView(remoteBroadcasterSurfaceView);
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(MeetingAudienceActivity.this, "参会人" + uid + "正在连麦", Toast.LENGTH_SHORT).show();
                        }

                        localSurfaceView = null;
                        audienceLayout.setVisibility(View.VISIBLE);
                        audienceView.removeAllViews();
                        remoteAudienceSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                        remoteAudienceSurfaceView.setZOrderOnTop(true);
                        remoteAudienceSurfaceView.setZOrderMediaOverlay(true);
                        rtcEngine().setupRemoteVideo(new VideoCanvas(remoteAudienceSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                        audienceView.addView(remoteAudienceSurfaceView);

                        agoraAPI.getUserAttr(String.valueOf(uid), "uname");
                    }
                } else {
                    if ("true".equals(agora.getIsTest())) {
                        worker().joinChannel(null, channelName, config().mUid);
                    } else {
                        worker().joinChannel(agora.getToken(), channelName, config().mUid);
                    }
                }
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                super.onFailure(errorCode, exception);
                Toast.makeText(MeetingAudienceActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private OkHttpCallback meetingMaterialCallback = new OkHttpCallback<Bucket<Material>>() {

        @Override
        public void onSuccess(Bucket<Material> materialBucket) {
            Log.v("material", materialBucket.toString());
            currentMaterial = materialBucket.getData();
            Collections.sort(currentMaterial.getMeetingMaterialsPublishList(), (o1, o2) -> (o1.getPriority() < o2.getPriority()) ? -1 : 1);

            currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);

            if (remoteBroadcasterSurfaceView != null) {
                broadcasterLayout.removeView(remoteBroadcasterSurfaceView);
                broadcasterLayout.setVisibility(View.GONE);

                if (isFullScreen) {
                    broadcasterSmallLayout.setVisibility(View.GONE);
                } else {
                    broadcasterSmallLayout.setVisibility(View.VISIBLE);
                }
                if (broadcasterSmallView.getChildCount() == 0) {
                    broadcasterSmallView.removeAllViews();
                    broadcasterSmallView.addView(remoteBroadcasterSurfaceView);
                }
            }
            pageText.setVisibility(View.VISIBLE);
            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
            docImage.setVisibility(View.VISIBLE);
            Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);

            fullScreenButton.setVisibility(View.VISIBLE);
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            Toast.makeText(MeetingAudienceActivity.this, errorCode + "---" + exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private String meetingHostJoinTraceId;

    private OkHttpCallback meetingHostJoinTraceCallback = new OkHttpCallback<Bucket<MeetingHostingStats>>() {

        @Override
        public void onSuccess(Bucket<MeetingHostingStats> meetingHostingStatsBucket) {
            if (TextUtils.isEmpty(meetingHostJoinTraceId)) {
                meetingHostJoinTraceId = meetingHostingStatsBucket.getData().getId();
            } else {
                meetingHostJoinTraceId = null;
            }
        }
    };

    private Dialog dialog;

    private void showDialog(final int type, final String title, final String leftText, final String rightText, final Audience audience) {
        View view = View.inflate(this, R.layout.dialog_selector, null);
        TextView titleText = view.findViewById(R.id.title);
        titleText.setText(title);

        Button leftButton = view.findViewById(R.id.left);
        leftButton.setText(leftText);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
            }
        });

        Button rightButton = view.findViewById(R.id.right);
        rightButton.setText(rightText);
        rightButton.setOnClickListener(view1 -> {
            dialog.cancel();
            if (type == 1) {
                if (localSurfaceView != null && remoteAudienceSurfaceView == null) {
                    audienceView.removeAllViews();
                    audienceNameText.setText("");
                    audienceLayout.setVisibility(View.GONE);

                    localSurfaceView = null;

                    agoraAPI.setAttr("uname", null);
                    agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);

                    if (!isDocShow) {
                        fullScreenButton.setVisibility(View.GONE);
                    }
                    requestTalkButton.setVisibility(View.VISIBLE);
                    stopTalkButton.setVisibility(View.GONE);

                    handsUp = false;
                    requestTalkButton.setText("我要发言");
                    requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);

                    worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("handsUp", handsUp);
                        jsonObject.put("uid", config().mUid);
                        jsonObject.put("uname", audienceName);
                        jsonObject.put("callStatus", 0);
                        jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                        jsonObject.put("postTypeName", Preferences.getUserPostType());
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                        HashMap<String, Object> params = new HashMap<String, Object>();
                        params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                        params.put("status", 2);
                        params.put("meetingId", meetingJoin.getMeeting().getId());
                        ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                    }
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (localSurfaceView == null && remoteAudienceSurfaceView != null){
                    audienceView.removeAllViews();
                    audienceNameText.setText("");
                    audienceLayout.setVisibility(View.GONE);

                    remoteAudienceSurfaceView = null;

                    if (!isDocShow) {
                        fullScreenButton.setVisibility(View.GONE);
                    }
                }

                doLeaveChannel();
                if (agoraAPI.getStatus() == 2) {
                    agoraAPI.logout();
                }
                finish();
            } else if (type == 2) {
                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                audienceView.removeAllViews();
                audienceNameText.setText("");
                audienceLayout.setVisibility(View.GONE);
                stopTalkButton.setVisibility(View.GONE);
                requestTalkButton.setVisibility(View.VISIBLE);
                localSurfaceView = null;
                if (!isDocShow) {
                    fullScreenButton.setVisibility(View.GONE);
                }

                agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                handsUp = false;
                requestTalkButton.setText("我要发言");
                requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("handsUp", handsUp);
                    jsonObject.put("uid", config().mUid);
                    jsonObject.put("uname", audienceName);
                    jsonObject.put("callStatus", 0);
                    jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                    jsonObject.put("postTypeName", Preferences.getUserPostType());
                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                    params.put("status", 2);
                    params.put("meetingId", meetingJoin.getMeeting().getId());
                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                }
            }
        });

        dialog = new Dialog(this, R.style.MyDialog);
        dialog.setContentView(view);

        dialog.show();
    }

    private void doConfigEngine(int cRole) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int prefIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
        int vProfile = ConstantApp.VIDEO_PROFILES[prefIndex];
        worker().configEngine(cRole, vProfile);
    }

    @Override
    protected void deInitUIandEvent() {
        doLeaveChannel();
        event().removeEventHandler(this);

    }

    private void doLeaveChannel() {
        worker().leaveChannel(config().mChannel);
        worker().preview(false, null, 0);

        if (!TextUtils.isEmpty(meetingJoinTraceId)) {
            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("meetingJoinTraceId", meetingJoinTraceId);
            params.put("meetingId", meetingJoin.getMeeting().getId());
            params.put("status", 2);
            params.put("type", 2);
            ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
        }
    }

    @Override
    public void onJoinChannelSuccess(final String channel, final int uid, final int elapsed) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            config().mUid = uid;
            channelName = channel;

            if ("true".equals(agora.getIsTest())) {
                agoraAPI.login2(agora.getAppID(), "" + uid, "noneed_token", 0, "", 20, 30);
            } else {
                agoraAPI.login2(agora.getAppID(), "" + uid, agora.getSignalingKey(), 0, "", 20, 30);
            }

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put("status", 1);
            params.put("type", 2);
            params.put("meetingId", meetingJoin.getMeeting().getId());
            ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
        });
    }

    private String meetingJoinTraceId;

    private OkHttpCallback meetingJoinStatsCallback = new OkHttpCallback<Bucket<MeetingJoinStats>>() {

        @Override
        public void onSuccess(Bucket<MeetingJoinStats> meetingJoinStatsBucket) {
            if (TextUtils.isEmpty(meetingJoinTraceId)) {
                meetingJoinTraceId = meetingJoinStatsBucket.getData().getId();
            } else {
                meetingJoinTraceId = null;
            }
        }
    };

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            ApiClient.getInstance().getMeetingHost(TAG, meeting.getId(), joinMeetingCallback(uid));

        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        LOG.debug("onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            if (uid == Integer.parseInt(broadcastId)) {
                broadcasterLayout.removeAllViews();
                broadcastTipsText.setText("等待主持人进入...");
                broadcastTipsText.setVisibility(View.VISIBLE);
                broadcastNameText.setText("");
                remoteBroadcasterSurfaceView = null;

                if (remoteAudienceSurfaceView != null) {
                    audienceLayout.removeView(remoteAudienceSurfaceView);
                    audienceNameText.setText("");
                    remoteAudienceSurfaceView = null;
                }
                if (localSurfaceView != null) {
                    worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                    audienceView.removeAllViews();
                    audienceNameText.setText("");
                    audienceLayout.setVisibility(View.GONE);
                    stopTalkButton.setVisibility(View.GONE);
                    requestTalkButton.setVisibility(View.VISIBLE);
                    localSurfaceView = null;
                    if (!isDocShow) {
                        fullScreenButton.setVisibility(View.GONE);
                    }
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                        HashMap<String, Object> params = new HashMap<String, Object>();
                        params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                        params.put("status", 2);
                        params.put("meetingId", meetingJoin.getMeeting().getId());
                        ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                    }
                }
            } else {
                if (BuildConfig.DEBUG)
                    Toast.makeText(MeetingAudienceActivity.this, "连麦观众" + uid + "退出了" + config().mUid, Toast.LENGTH_SHORT).show();

//                audienceView.removeAllViews();
//                audienceNameText.setText("");
//                audienceLayout.setVisibility(View.GONE);
//                if (!isDocShow) {
//                    fullScreenButton.setVisibility(View.GONE);
//                    requestTalkButton.setVisibility(View.VISIBLE);
//                }
//                remoteAudienceSurfaceView = null;
            }
        });
    }

    @Override
    public void onConnectionLost() {
        runOnUiThread(() -> {
            Toast.makeText(MeetingAudienceActivity.this, "声网服务器网络连接断开，请检查网络连接", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onConnectionInterrupted() {
        runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "网络连接不佳，视频将会有卡顿，可尝试降低分辨率", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onUserMuteVideo(final int uid, final boolean muted) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, uid + " 的视频被暂停了 " + muted, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onLastmileQuality(final int quality) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "本地网络质量报告：" + showNetQuality(quality), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> {
//                    Toast.makeText(MeetingAudienceActivity.this, "用户" + uid + "的\n上行网络质量：" + showNetQuality(txQuality) + "\n下行网络质量：" + showNetQuality(rxQuality), Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public void onWarning(int warn) {
//        runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "警告：" + warn, Toast.LENGTH_SHORT).show());
    }

    private String showNetQuality(int quality) {
        String lastmileQuality;
        switch (quality) {
            case 0:
                lastmileQuality = "UNKNOWN0";
                break;
            case 1:
                lastmileQuality = "EXCELLENT";
                break;
            case 2:
                lastmileQuality = "GOOD";
                break;
            case 3:
                lastmileQuality = "POOR";
                break;
            case 4:
                lastmileQuality = "BAD";
                break;
            case 5:
                lastmileQuality = "VBAD";
                break;
            case 6:
                lastmileQuality = "DOWN";
                break;
            default:
                lastmileQuality = "UNKNOWN";
        }
        return lastmileQuality;
    }

    @Override
    public void onError(final int err) {
        runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show());

//        if (BuildConfig.DEBUG) {
//            runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show());
//        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_HOME == keyCode) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;

    @Override
    public void onAttachedToWindow() {
        this.getWindow().addFlags(FLAG_HOMEKEY_DISPATCHED);
        super.onAttachedToWindow();
    }

    @Override
    public void onBackPressed() {
        if (dialog == null || !dialog.isShowing()) {
            showDialog(1, "确定退出会议吗？", "取消", "确定", null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TCAgent.onPageEnd(this, "MeetingAudienceActivity");

        if (timer != null && timerTask != null) {
            timer.cancel();
            timerTask.cancel();
        }

        BaseApplication.getInstance().deInitWorkerThread();
    }

}
