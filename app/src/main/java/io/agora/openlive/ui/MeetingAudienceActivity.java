package io.agora.openlive.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.hezy.guide.phone.entities.Material;
import com.hezy.guide.phone.entities.MeetingHostingStats;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.MeetingJoinStats;
import com.hezy.guide.phone.entities.MeetingMaterialsPublish;
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

    private FrameLayout broadcasterLayout, broadcasterSmallLayout, broadcasterSmallView, audienceLayout, audienceView;
    private TextView broadcastNameText, broadcastTipsText, countText, audienceNameText;
    private Button requestTalkButton, stopTalkButton;
    private TextView exitButton, pageText;
    private ImageView docImage;
    private ImageButton fullScreenButton;

    private Material currentMaterial;
    private MeetingMaterialsPublish currentMaterialPublish;
    private int doc_index = 0;
    private boolean calling;

    private String channelName;

    private AgoraAPIOnlySignal agoraAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_audience);
        TCAgent.onEvent(this, "进入会议直播界面");

        registerReceiver(homeKeyEventReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

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

        config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));
        Log.v("uid--->", "" + config().mUid);

        channelName = meetingJoin.getMeeting().getId();

        broadcasterLayout = findViewById(R.id.broadcaster_view);
        broadcastTipsText = findViewById(R.id.broadcast_tips);
        broadcastNameText = findViewById(R.id.broadcaster);
        broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());
        broadcasterSmallLayout = findViewById(R.id.broadcaster_small_layout);
        broadcasterSmallView = findViewById(R.id.broadcaster_small_view);
        docImage = findViewById(R.id.doc_image);
        pageText = findViewById(R.id.page);
        fullScreenButton = findViewById(R.id.full_screen);
        fullScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
            if (!TextUtils.isEmpty(broadcastId)) {
                if (request) {
                    request = false;
                    requestTalkButton.setText("我要发言");
                    requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("request", request);
                        jsonObject.put("uid", config().mUid);
                        jsonObject.put("calling", calling);
                        jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                        jsonObject.put("postTypeName", Preferences.getUserPostTypeName());
                        jsonObject.put("uname", TextUtils.isEmpty(Preferences.getAreaInfo()) ? "讲解员-" + Preferences.getUserName() : "讲解员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    request = true;
                    requestTalkButton.setText("放弃发言");
                    requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup_giveup, 0, 0, 0);
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("request", request);
                        jsonObject.put("uid", config().mUid);
                        jsonObject.put("calling", calling);
                        jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                        jsonObject.put("postTypeName", Preferences.getUserPostTypeName());
                        jsonObject.put("uname", TextUtils.isEmpty(Preferences.getAreaInfo()) ? "讲解员-" + Preferences.getUserName() : "讲解员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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

        if ("true".equals(agora.getIsTest())) {
            worker().joinChannel(null, channelName, config().mUid);
        } else {
            worker().joinChannel(agora.getToken(), channelName, config().mUid);
        }

        agoraAPI = AgoraAPIOnlySignal.getInstance(this, agora.getAppID());
        agoraAPI.callbackSet(new AgoraAPI.CallBack() {

            @Override
            public void onLoginSuccess(int uid, int fd) {
                super.onLoginSuccess(uid, fd);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登陆信令系统成功", Toast.LENGTH_SHORT).show());
                }
                agoraAPI.channelJoin(channelName);
            }

            @Override
            public void onLoginFailed(final int ecode) {
                super.onLoginFailed(ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登陆信令系统失败" + ecode, Toast.LENGTH_SHORT).show());
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
                        Toast.makeText(MeetingAudienceActivity.this, "观众登陆信令频道成功", Toast.LENGTH_SHORT).show();
                    }
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("request", request);
                        jsonObject.put("uid", config().mUid);
                        jsonObject.put("calling", calling);
                        jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                        jsonObject.put("postTypeName", Preferences.getUserPostTypeName());
                        jsonObject.put("uname", TextUtils.isEmpty(Preferences.getAreaInfo()) ? "讲解员-" + Preferences.getUserName() : "讲解员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    agoraAPI.channelQueryUserNum(channelName);
                });
            }

            @Override
            public void onChannelJoinFailed(String channelID, int ecode) {
                super.onChannelJoinFailed(channelID, ecode);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "观众登陆信令频道失败", Toast.LENGTH_SHORT).show();
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
                Log.v("onChannelUserJoined", "观众" + account + "加入房间了---" + meetingJoin.getHostUser().getClientUid());
                if (account.equals(meetingJoin.getHostUser().getClientUid())) { // 讲解员异常退出后重新进入
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("request", request);
                        jsonObject.put("uid", config().mUid);
                        jsonObject.put("calling", calling);
                        jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                        jsonObject.put("postTypeName", Preferences.getUserPostTypeName());
                        jsonObject.put("uname", TextUtils.isEmpty(Preferences.getAreaInfo()) ? "讲解员-" + Preferences.getUserName() : "讲解员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingAudienceActivity.this, "获取用户" + account + "的属性" + name + "的值为" + value, Toast.LENGTH_SHORT).show();
                    }

                    audienceLayout.setVisibility(View.VISIBLE);

                    audienceNameText.setText(value);

                    SurfaceView remoteSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                    remoteSurfaceView.setZOrderOnTop(true);
                    remoteSurfaceView.setZOrderMediaOverlay(true);
                    rtcEngine().setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, Integer.parseInt(account)));
                    audienceView.addView(remoteSurfaceView);

                });
            }

            @Override
            public void onUserAttrAllResult(String account, String value) {
                super.onUserAttrAllResult(account, value);

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
                            if (result) {
                                if (BuildConfig.DEBUG) {
                                    Toast.makeText(MeetingAudienceActivity.this, "主持人要和我连麦", Toast.LENGTH_SHORT).show();
                                }

                                audienceLayout.setVisibility(View.VISIBLE);

                                SurfaceView localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                                localSurfaceView.setZOrderOnTop(true);
                                localSurfaceView.setZOrderMediaOverlay(true);
                                rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
                                audienceView.addView(localSurfaceView);

                                String audienceName = jsonObject.getString("name");
                                audienceNameText.setText(audienceName);

//                                Audience audience = new Audience();
//                                audience.setUid(config().mUid);
//                                audience.setUname(audienceName);
//                                audienceNameText.setTag(audience);

                                stopTalkButton.setVisibility(View.VISIBLE);
                                requestTalkButton.setVisibility(View.GONE);

                                calling = true;

//                                agoraAPI.setAttr("uname", audienceName);

                                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

                                try {
                                    JSONObject jsonObject1 = new JSONObject();
                                    jsonObject1.put("request", request);
                                    jsonObject1.put("uid", config().mUid);
                                    jsonObject1.put("calling", calling);
                                    jsonObject1.put("auditStatus", Preferences.getUserAuditStatus());
                                    jsonObject1.put("postTypeName", Preferences.getUserPostTypeName());
                                    jsonObject1.put("uname", TextUtils.isEmpty(Preferences.getAreaInfo()) ? "讲解员-" + Preferences.getUserName() : "讲解员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject1.toString(), "");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

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
                            request = false;
                            requestTalkButton.setText("我要发言");
                            requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                        }
                        if (jsonObject.has("finish")) {
                            boolean finish = jsonObject.getBoolean("finish");
                            if (finish) {
                                audienceView.removeAllViews();
                                audienceNameText.setText("");
                                audienceLayout.setVisibility(View.GONE);

                                requestTalkButton.setVisibility(View.VISIBLE);
                                stopTalkButton.setVisibility(View.GONE);

                                request = false;
                                requestTalkButton.setText("我要发言");
                                requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);

                                isExit = false;

                                calling = false;

                                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

                                try {
                                    JSONObject jsonObject2 = new JSONObject();
                                    jsonObject2.put("request", request);
                                    jsonObject2.put("uid", config().mUid);
                                    jsonObject2.put("calling", calling);
                                    jsonObject2.put("auditStatus", Preferences.getUserAuditStatus());
                                    jsonObject2.put("postTypeName", Preferences.getUserPostTypeName());
                                    jsonObject2.put("uname", TextUtils.isEmpty(Preferences.getAreaInfo()) ? "讲解员-" + Preferences.getUserName() : "讲解员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
                                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject2.toString(), "");
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
                        if (jsonObject.has("material_id")) {
                            String materialId = jsonObject.getString("material_id");
                            if (currentMaterial != null && !materialId.equals(currentMaterial.getId())) {
                                ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
                            }
                        }
                        if (jsonObject.has("doc_index")) {
                            doc_index = Integer.parseInt(jsonObject.getString("doc_index"));
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(MeetingAudienceActivity.this, "收到主持人端index：" + doc_index, Toast.LENGTH_SHORT).show();
                            }
                            if (currentMaterial != null) {
                                currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);
                                pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
                                Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);

                            } else {
                                Toast.makeText(MeetingAudienceActivity.this, "收到主持人端doc_index的时候material为null", Toast.LENGTH_SHORT).show();
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
                    if ("material_id".equals(name)) {
                        if (!TextUtils.isEmpty(value)) {
                            ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, value);
                        } else {
                            Toast.makeText(MeetingAudienceActivity.this, "收到主持人端发的material_id值为null", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if ("doc_index".equals(name)) {
                        if (!TextUtils.isEmpty(value)) {
                            doc_index = Integer.parseInt(value);
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(MeetingAudienceActivity.this, "收到主持人端index：" + doc_index, Toast.LENGTH_SHORT).show();
                            }
                            if (currentMaterial != null) {
                                currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);
                                pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
                                Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);
                            } else {
                                Toast.makeText(MeetingAudienceActivity.this, "收到主持人端doc_index的时候material为null", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            broadcasterSmallView.removeAllViews();
                            broadcasterSmallLayout.setVisibility(View.GONE);

                            pageText.setVisibility(View.GONE);
                            docImage.setVisibility(View.GONE);

                            broadcasterLayout.setVisibility(View.VISIBLE);
                            broadcasterLayout.addView(remoteSurfaceView);
                        }
                    }
                });
            }

            @Override
            public void onError(final String name, final int ecode, final String desc) {
                super.onError(name, ecode, desc);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "name: " + name + "\necode: " + ecode + "\ndesc: " + desc, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onLog(String txt) {
                super.onLog(txt);
                Log.v("信令audience", txt);
            }
        });

    }

    private OkHttpCallback meetingMaterialCallback = new OkHttpCallback<Bucket<Material>>() {

        @Override
        public void onSuccess(Bucket<Material> materialBucket) {
            Log.v("material", materialBucket.toString());
            currentMaterial = materialBucket.getData();
            Collections.sort(currentMaterial.getMeetingMaterialsPublishList(), (o1, o2) -> (o1.getPriority() < o2.getPriority()) ? -1 : 1);

            currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);

            while (remoteSurfaceView != null) {
                broadcasterLayout.removeView(remoteSurfaceView);
                broadcasterLayout.setVisibility(View.GONE);

                broadcasterSmallLayout.setVisibility(View.VISIBLE);
                broadcasterSmallView.removeAllViews();
                broadcasterSmallView.addView(remoteSurfaceView);
                Log.v("while", "while loop.............");
                break;
            }
            pageText.setVisibility(View.VISIBLE);
            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
            docImage.setVisibility(View.VISIBLE);
            Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);

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
                if (request) {
                    request = false;
                }
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                calling = false;
                agoraAPI.channelLeave(channelName);
                if (agoraAPI.getStatus() == 2) {
                    agoraAPI.logout();
                }
                finish();
            } else if (type == 2) {
                calling = false;
                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                audienceLayout.removeAllViews();
                audienceNameText.setText("");
                stopTalkButton.setVisibility(View.GONE);
                requestTalkButton.setVisibility(View.VISIBLE);
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("request", request);
                    jsonObject.put("uid", config().mUid);
                    jsonObject.put("calling", calling);
                    jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
                    jsonObject.put("postTypeName", Preferences.getUserPostTypeName());
                    jsonObject.put("uname", TextUtils.isEmpty(Preferences.getAreaInfo()) ? "讲解员-" + Preferences.getUserName() : "讲解员-" + Preferences.getAreaInfo() + "-" + Preferences.getUserName());
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
            Log.v("onJoinChannelSuccess", "channel--->" + channel);
            Log.v("onJoinChannelSuccess", "uid--->" + uid);

            if ("true".equals(agora.getIsTest())) {
                agoraAPI.login2(agora.getAppID(), "" + uid, "noneed_token", 0, "", 30, 3);
            } else {
                agoraAPI.login2(agora.getAppID(), "" + uid, agora.getSignalingKey(), 0, "", 30, 3);
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
        doRenderRemoteUi(uid);
    }

    private SurfaceView remoteSurfaceView;

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }

            TCAgent.onEvent(MeetingAudienceActivity.this, "讲解员进入当前通话");
            if (uid == Integer.parseInt(meetingJoin.getHostUser().getClientUid())) {
                broadcastId = String.valueOf(uid);
                broadcastTipsText.setVisibility(View.GONE);
                broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());

                if (BuildConfig.DEBUG) {
                    Toast.makeText(MeetingAudienceActivity.this, "主持人" + broadcastId + "---" + uid + meetingJoin.getHostUser().getHostUserName() + "进入了", Toast.LENGTH_SHORT).show();
                }

                broadcasterLayout.setVisibility(View.VISIBLE);
                remoteSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                remoteSurfaceView.setZOrderOnTop(false);
                remoteSurfaceView.setZOrderMediaOverlay(false);
                rtcEngine().setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                broadcasterLayout.removeAllViews();
                broadcasterLayout.addView(remoteSurfaceView);
            } else {
                if (BuildConfig.DEBUG) {
                    Toast.makeText(MeetingAudienceActivity.this, "连麦观众" + uid + "进入了，去获取连麦观众的名字", Toast.LENGTH_SHORT).show();
                }
                isExit = false;
                agoraAPI.getUserAttr("" + uid, "uname");

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
        runOnUiThread(() -> {
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
                        audienceView.removeAllViews();
                        audienceNameText.setText("");
                        audienceLayout.setVisibility(View.GONE);
                        isExit = true;
                    } else {
                        Toast.makeText(MeetingAudienceActivity.this, "is me", Toast.LENGTH_SHORT).show();
                        audienceLayout.setVisibility(View.GONE);
                    }
                } else {
                    Toast.makeText(MeetingAudienceActivity.this, "is exit", Toast.LENGTH_SHORT).show();
                    audienceLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onConnectionLost() {
        runOnUiThread(() -> {
            Toast.makeText(MeetingAudienceActivity.this, "agor网络连接断开，请检查网络连接", Toast.LENGTH_SHORT).show();
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
            runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show());
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

        unregisterReceiver(homeKeyEventReceiver);

        BaseApplication.getInstance().deInitWorkerThread();
    }

    private BroadcastReceiver homeKeyEventReceiver = new BroadcastReceiver() {
        String REASON = "reason";
        String HOMEKEY = "homekey";
        String RECENTAPPS = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action) || Intent.ACTION_SHUTDOWN.equals(action)) {
                String reason = intent.getStringExtra(REASON);
                if (TextUtils.equals(reason, HOMEKEY)) {
                    // 点击 Home键
                    Toast.makeText(getApplicationContext(), "您点击了Home键", Toast.LENGTH_SHORT).show();
                    worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                    audienceLayout.removeAllViews();
                    audienceNameText.setText("");
                    stopTalkButton.setVisibility(View.GONE);
                    requestTalkButton.setVisibility(View.VISIBLE);

                    if (request) {
                        request = false;
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("finish", true);
                            agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    agoraAPI.channelLeave(channelName);
                    if (agoraAPI.getStatus() == 2) {
                        agoraAPI.logout();
                    }
                    finish();
                } else if (TextUtils.equals(reason, RECENTAPPS)) {
                    // 点击 菜单键
                    Toast.makeText(getApplicationContext(), "您点击了菜单键", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

}
