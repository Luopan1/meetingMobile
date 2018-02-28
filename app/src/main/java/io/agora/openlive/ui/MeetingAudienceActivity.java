package io.agora.openlive.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Audience;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.MeetingHostingStats;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.MeetingJoinStats;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.UIDUtil;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

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

    private MeetingJoin meetingJoin;
    private Agora agora;
    private String broadcastId;

    private FrameLayout broadcasterLayout, audienceLayout;
    private TextView broadcastNameText, broadcastTipsText, countText, audienceNameText, audienceTipsText;
    private Button micButton, finishButton, exitButton;

    private String channelName;

    private AgoraAPIOnlySignal agoraAPI;

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

    private boolean request = false;

    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);

        Intent intent = getIntent();
        agora = intent.getParcelableExtra("agora");
        meetingJoin = intent.getParcelableExtra("meeting");

        channelName = meetingJoin.getMeeting().getId();

        broadcasterLayout = (FrameLayout) findViewById(R.id.broadcaster_view);
        broadcastTipsText = (TextView) findViewById(R.id.broadcast_tips);
        broadcastNameText = (TextView) findViewById(R.id.broadcaster);
        broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());

        audienceLayout = (FrameLayout) findViewById(R.id.audience_view);
        audienceTipsText = (TextView) findViewById(R.id.audience_tips);
        audienceNameText = (TextView) findViewById(R.id.audience_name);

        countText = (TextView) findViewById(R.id.online_count);

        finishButton = (Button) findViewById(R.id.finish);
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE, "");
                audienceLayout.removeAllViews();
                audienceNameText.setText("");
                finishButton.setVisibility(View.GONE);
                micButton.setVisibility(View.VISIBLE);
                audienceTipsText.setVisibility(View.VISIBLE);
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        micButton = (Button) findViewById(R.id.waiter);
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(broadcastId)) {
                    if (request) {
                        request = false;
                        micButton.setText("我要发言");
                        micButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("request", false);
                            jsonObject.put("uid", UIDUtil.generatorUID(Preferences.getUserId()));
                            if (TextUtils.isEmpty(Preferences.getAreaInfo())) {
                                jsonObject.put("uname", "店员-" + Preferences.getUserName());
                            } else {
                                jsonObject.put("uname", "店员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                            }
                            agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        request = true;
                        micButton.setText("放弃发言");
                        micButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup_giveup, 0, 0, 0);
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("request", true);
                            jsonObject.put("uid", UIDUtil.generatorUID(Preferences.getUserId()));
                            if (TextUtils.isEmpty(Preferences.getAreaInfo())) {
                                jsonObject.put("uname", "店员-" +  Preferences.getUserName());
                            } else {
                                jsonObject.put("uname", "店员-" + Preferences.getAreaInfo() + Preferences.getUserName());
                            }
                            agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(MeetingAudienceActivity.this, "请先等待主持人加入", Toast.LENGTH_SHORT).show();
                }
            }
        });

        exitButton = (Button) findViewById(R.id.exit);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (request) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("request", false);
                        jsonObject.put("uid", UIDUtil.generatorUID(Preferences.getUserId()));
                        if (TextUtils.isEmpty(Preferences.getAreaInfo())) {
                            jsonObject.put("uname", "店员-" + "-"+ Preferences.getUserName());
                        } else {
                            jsonObject.put("uname", "店员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                        }
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                showDialog(1, "确定退出会议？", "取消", "确定", null);
            }
        });

        config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));
        doConfigEngine(Constants.CLIENT_ROLE_AUDIENCE);

        worker().joinChannel(agora.getChannelKey(), channelName, config().mUid);

        agoraAPI = AgoraAPIOnlySignal.getInstance(this, agora.getAppID());
        agoraAPI.callbackSet(new AgoraAPI.CallBack() {

            @Override
            public void onLoginSuccess(int uid, int fd) {
                super.onLoginSuccess(uid, fd);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeetingAudienceActivity.this, "观众登陆信令系统成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                agoraAPI.setAttr("role", "1");
                agoraAPI.channelJoin(channelName);
            }

            @Override
            public void onLoginFailed(final int ecode) {
                super.onLoginFailed(ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeetingAudienceActivity.this, "观众登陆信令系统失败" + ecode, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onChannelJoined(String channelID) {
                super.onChannelJoined(channelID);
                agoraAPI.channelQueryUserNum(channelName);

            }

            @Override
            public void onChannelJoinFailed(String channelID, int ecode) {
                super.onChannelJoinFailed(channelID, ecode);

            }

            @Override
            public void onChannelQueryUserNumResult(String channelID, int ecode, final int num) {
                super.onChannelQueryUserNumResult(channelID, ecode, num);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countText.setText("在线人数：" + num);
                    }
                });
            }

            @Override
            public void onChannelUserJoined(String account, int uid) {
                super.onChannelUserJoined(account, uid);
                agoraAPI.channelQueryUserNum(channelName);
            }

            @Override
            public void onChannelUserLeaved(String account, int uid) {
                super.onChannelUserLeaved(account, uid);
                agoraAPI.channelQueryUserNum(channelName);
            }

            @Override
            public void onUserAttrResult(final String account, final String name, final String value) {
                super.onUserAttrResult(account, name, value);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(MeetingAudienceActivity.this, "获取用户" + account + "的属性" + name + "的值为" + value, Toast.LENGTH_SHORT).show();
                        }

                        audienceNameText.setText(value);

                        SurfaceView remoteSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                        remoteSurfaceView.setZOrderOnTop(true);
                        remoteSurfaceView.setZOrderMediaOverlay(true);
                        rtcEngine().setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, Integer.parseInt(account)));
                        audienceTipsText.setVisibility(View.GONE);
                        audienceLayout.addView(remoteSurfaceView);

                    }
                });
            }

            @Override
            public void onMessageSendSuccess(String messageID) {
                super.onMessageSendSuccess(messageID);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeetingAudienceActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onMessageSendError(String messageID, int ecode) {
                super.onMessageSendError(messageID, ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeetingAudienceActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onMessageInstantReceive(final String account, final int uid, final String msg) {
                super.onMessageInstantReceive(account, uid, msg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(MeetingAudienceActivity.this, "接收到消息" + msg, Toast.LENGTH_SHORT).show();
                            }
                            JSONObject jsonObject = new JSONObject(msg);
                            if (jsonObject.has("response")) {
                                boolean result = jsonObject.getBoolean("response");
                                if (result) {
                                    if (BuildConfig.DEBUG) {
                                        Toast.makeText(MeetingAudienceActivity.this, "接受连麦", Toast.LENGTH_SHORT).show();
                                    }
                                    SurfaceView localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                                    localSurfaceView.setZOrderOnTop(true);
                                    localSurfaceView.setZOrderMediaOverlay(true);
                                    rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
                                    audienceLayout.addView(localSurfaceView);

                                    String audienceName = jsonObject.getString("name");
                                    audienceNameText.setText(audienceName);
                                    Audience audience = new Audience();
                                    audience.setUid(config().mUid + "");
                                    audience.setUname(audienceName);
                                    audienceNameText.setTag(audience);
                                    audienceTipsText.setVisibility(View.GONE);

                                    finishButton.setVisibility(View.VISIBLE);
                                    micButton.setVisibility(View.GONE);

                                    agoraAPI.setAttr("uname", audienceName);

                                    worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER, "");
                                    HashMap<String, Object> params = new HashMap<String, Object>();
                                    params.put("status", 1);
                                    params.put("meetingId", meetingJoin.getMeeting().getId());
                                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                                } else {
                                    if (BuildConfig.DEBUG) {
                                        Toast.makeText(MeetingAudienceActivity.this, "拒绝连麦", Toast.LENGTH_SHORT).show();
                                    }
                                    finishButton.setVisibility(View.GONE);
                                    micButton.setVisibility(View.VISIBLE);
                                    audienceTipsText.setVisibility(View.VISIBLE);
                                }
                                request = false;
                                micButton.setText("我要发言");
                                micButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                            }
                            if (jsonObject.has("finish")) {
                                boolean finish = jsonObject.getBoolean("finish");
                                if (finish) {
                                    audienceLayout.removeAllViews();
                                    audienceNameText.setText("");
                                    audienceTipsText.setVisibility(View.VISIBLE);
                                    audienceTipsText.setText("等待参会人连麦");

                                    micButton.setVisibility(View.VISIBLE);
                                    finishButton.setVisibility(View.GONE);

                                    request = false;
                                    micButton.setText("我要发言");
                                    micButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);

                                    isExit = false;

                                    worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE, "");

                                    HashMap<String, Object> params = new HashMap<String, Object>();
                                    params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                                    params.put("status", 2);
                                    params.put("meetingId", meetingJoin.getMeeting().getId());
                                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void onMessageChannelReceive(String channelID, String account, int uid, final String msg) {
                super.onMessageChannelReceive(channelID, account, uid, msg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(MeetingAudienceActivity.this, "" + msg, Toast.LENGTH_SHORT).show();
                            }
                            JSONObject jsonObject = new JSONObject(msg);
                            if (jsonObject.has("finish_meeting")) {
                                boolean finishMeeting = jsonObject.getBoolean("finish_meeting");
                                if (finishMeeting) {
                                    if (BuildConfig.DEBUG)
                                    Toast.makeText(MeetingAudienceActivity.this, "主持人结束了会议", Toast.LENGTH_SHORT).show();
                                    agoraAPI.channelLeave(channelName);
                                    agoraAPI.logout();
                                    finish();
                                }
                            }
                        } catch (Exception e) {

                        }
                    }
                });
            }

            @Override
            public void onError(final String name, final int ecode, final String desc) {
                super.onError(name, ecode, desc);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeetingAudienceActivity.this, "name: " + name + "ecode: " + ecode + "desc: " + desc, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

    }

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
                if (type == 1) {

                }
            }
        });

        Button rightButton = view.findViewById(R.id.right);
        rightButton.setText(rightText);
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                if (type == 1) {
                    if (request) {
                        request = false;
                        micButton.setText("我要发言");
//                        micButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                        micButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("request", false);
                            jsonObject.put("uid", UIDUtil.generatorUID(Preferences.getUserId()));
                            if (TextUtils.isEmpty(Preferences.getAreaInfo())) {
                                jsonObject.put("uname", "店员-" +  Preferences.getUserName());
                            } else {
                                jsonObject.put("uname", "店员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                            }
                            agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    agoraAPI.channelLeave(channelName);
                    agoraAPI.logout();
                    finish();
                }
            }
        });

        dialog = new Dialog(this, R.style.MyDialog);
        dialog.setContentView(view);

//        Window dialogWindow = dialog.getWindow();
//        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
//        lp.width = 740;
//        lp.height = 480;
//        dialogWindow.setAttributes(lp);

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

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("meetingJoinTraceId", meetingJoinTraceId);
        params.put("meetingId", meetingJoin.getMeeting().getId());
        params.put("status", 2);
        params.put("type", 2);
        ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
    }

    @Override
    public void onJoinChannelSuccess(final String channel, final int uid, final int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                worker().getEngineConfig().mUid = uid;
                agoraAPI.login(agora.getAppID(), "" + uid, agora.getSignalingKey(), 0, "");

                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put("status", 1);
                params.put("type", 2);
                params.put("meetingId", meetingJoin.getMeeting().getId());
                ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
            }
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
        doRenderRemoteUi(uid);
    }

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                TCAgent.onEvent(MeetingAudienceActivity.this, "讲解员进入当前通话");
                if (uid == Integer.parseInt(meetingJoin.getHostUser().getClientUid())) {
                    broadcastId = "" + uid;
                    broadcastTipsText.setVisibility(View.GONE);
                    broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());

                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "主持人" + broadcastId + "---" + uid + meetingJoin.getHostUser().getHostUserName() + "进入了", Toast.LENGTH_SHORT).show();
                    }

                    SurfaceView remoteSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                    remoteSurfaceView.setZOrderOnTop(false);
                    remoteSurfaceView.setZOrderMediaOverlay(false);
                    rtcEngine().setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                    broadcasterLayout.addView(remoteSurfaceView);
                } else {

                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "连麦观众" + uid + "进入了，去获取连麦观众的名字", Toast.LENGTH_SHORT).show();
                    }

                    agoraAPI.getUserAttr("" + uid, "uname");
                }
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        LOG.debug("onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
        doRemoveRemoteUi(uid);
    }

    private boolean isExit = false;

    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                if (!TextUtils.isEmpty(broadcastId) && uid == Integer.parseInt(broadcastId)) {
                    broadcasterLayout.removeAllViews();
                    broadcastTipsText.setText("等待主持人进入...");
                    broadcastTipsText.setVisibility(View.VISIBLE);
                    broadcastNameText.setText("");
                } else {
                    if (BuildConfig.DEBUG)
                        Toast.makeText(MeetingAudienceActivity.this, "连麦观众" + uid + "退出了" + config().mUid, Toast.LENGTH_SHORT).show();

                    if (!isExit) {
                        if (uid != config().mUid) {
                            audienceLayout.removeAllViews();
                            audienceNameText.setText("");
                            audienceTipsText.setVisibility(View.VISIBLE);
                            isExit = true;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionLost() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MeetingAudienceActivity.this, "网络连接断开，请检查网络连接", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onConnectionInterrupted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MeetingAudienceActivity.this, "网络连接不佳，视频将会有卡顿，可尝试降低分辨率", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUserMuteVideo(final int uid, final boolean muted) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MeetingAudienceActivity.this, uid + " 的视频被暂停了 " + muted, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onLastmileQuality(final int quality) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MeetingAudienceActivity.this, "本地网络质量报告：" + showNetQuality(quality), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(MeetingAudienceActivity.this, "用户" + uid + "的\n上行网络质量：" + showNetQuality(txQuality) + "\n下行网络质量：" + showNetQuality(rxQuality), Toast.LENGTH_SHORT).show();
                }
            });
        }
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
        if (BuildConfig.DEBUG) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MeetingAudienceActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show();
                }
            });
        }
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
            showDialog(1, "确定退出会议？", "取消", "确定", null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TCAgent.onPageEnd(this, "MeetingAudienceActivity");

        BaseApplication.getInstance().deInitWorkerThread();
    }

}
