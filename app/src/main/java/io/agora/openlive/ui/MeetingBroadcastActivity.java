package io.agora.openlive.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Audience;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.Meeting;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.MeetingJoinStats;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.UIDUtil;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.openlive.model.AGEventHandler;
import io.agora.openlive.model.ConstantApp;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class MeetingBroadcastActivity extends BaseActivity implements AGEventHandler {

    private final static Logger LOG = LoggerFactory.getLogger(MeetingBroadcastActivity.class);

    private final String TAG = MeetingBroadcastActivity.class.getSimpleName();

    private MeetingJoin meetingJoin;
    private Agora agora;
    private HashMap<String, Audience> audienceHashMap = new HashMap<String, Audience>();
    private ArrayList<Audience> audiences = new ArrayList<Audience>();

    private String channelName;
    private int memberCount;

    private boolean isMuted = false;

    private boolean isFullScreen = false;

    private FrameLayout broadcasterLayout, broadcastSmallLayout, broadcasterSmallView, audienceView, audienceLayout;
    private TextView broadcastNameText, broadcastTipsText, audienceNameText;
    private Button waiterButton, stopButton;
    private TextView exitButton;
    private AgoraAPIOnlySignal agoraAPI;
    private ImageButton muteButton;
    private ImageButton fullScreenButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_broadcast);

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

    @Override
    protected void initUIandEvent() {
        event().addEventHandler(this);

        Intent intent = getIntent();
        agora = intent.getParcelableExtra("agora");
        meetingJoin = intent.getParcelableExtra("meeting");
        channelName = meetingJoin.getMeeting().getId();

        broadcastTipsText = findViewById(R.id.broadcast_tips);
        audienceNameText = findViewById(R.id.audience_name);
        broadcastNameText = findViewById(R.id.broadcaster);
        broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());
        broadcasterLayout = findViewById(R.id.broadcaster_view);

        broadcastSmallLayout = findViewById(R.id.broadcaster_small_layout);
        broadcasterSmallView = findViewById(R.id.broadcaster_small_view);

        audienceLayout = findViewById(R.id.audience_layout);
        audienceView = findViewById(R.id.audience_view);

        fullScreenButton = findViewById(R.id.full_screen);
        fullScreenButton.setOnClickListener(v -> {
            if (!isFullScreen) {
                fullScreenButton.setImageResource(R.drawable.ic_full_screened);
                if (audienceView.getChildCount() > 0) {
                    audienceLayout.setVisibility(View.GONE);
                }
                if (broadcasterSmallView.getChildCount() > 0) {
                    broadcastSmallLayout.setVisibility(View.GONE);
                }
                muteButton.setVisibility(View.GONE);
                waiterButton.setVisibility(View.GONE);
                exitButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.GONE);
                isFullScreen = true;
            } else {
                fullScreenButton.setImageResource(R.drawable.ic_full_screen);
                if (audienceView.getChildCount() > 0) {
                    audienceLayout.setVisibility(View.VISIBLE);
                }
                if (broadcasterSmallView.getChildCount() > 0) {
                    broadcastSmallLayout.setVisibility(View.VISIBLE);
                }
                muteButton.setVisibility(View.VISIBLE);
                waiterButton.setVisibility(View.VISIBLE);
                exitButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.VISIBLE);
                isFullScreen = false;
            }
        });
        muteButton = findViewById(R.id.mute_audio);
        muteButton.setOnClickListener(v -> {
            if (!isMuted) {
                isMuted = true;
                muteButton.setImageResource(R.drawable.ic_muted);
            } else {
                isMuted = false;
                muteButton.setImageResource(R.drawable.ic_unmuted);
            }
            rtcEngine().muteLocalAudioStream(isMuted);
        });

        waiterButton = findViewById(R.id.waiter);
        waiterButton.setOnClickListener(view -> {
            if (audiences.size() > 0) {
                showAlertDialog();
            }
        });

        exitButton = findViewById(R.id.exit);
        exitButton.setOnClickListener(view -> showDialog(1, "确定结束会议吗？", "暂时离开", "结束会议", null));

        stopButton = findViewById(R.id.stop_audience);
        stopButton.setOnClickListener(view -> {
            Audience audience = (Audience) audienceNameText.getTag();
            showDialog(3, "结束" + audience.getUname() + "的发言？", "取消", "确定", audience);
        });

        config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));

        doConfigEngine(Constants.CLIENT_ROLE_BROADCASTER);

        SurfaceView localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
        rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
        localSurfaceView.setZOrderOnTop(false);
        localSurfaceView.setZOrderMediaOverlay(false);
        broadcasterLayout.addView(localSurfaceView);
        worker().preview(true, localSurfaceView, config().mUid);

        broadcastTipsText.setVisibility(View.GONE);

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
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, "信令系统登陆成功", Toast.LENGTH_SHORT).show());
                    }
                    agoraAPI.channelJoin(channelName);
                });

            }

            @Override
            public void onLoginFailed(final int ecode) {
                super.onLoginFailed(ecode);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, "信令系统登陆失败" + ecode, Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onChannelJoined(String channelID) {
                super.onChannelJoined(channelID);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, "加入信令频道成功", Toast.LENGTH_SHORT).show();
                    }
                    agoraAPI.channelQueryUserNum(channelName);
                });
            }

            @Override
            public void onChannelJoinFailed(String channelID, int ecode) {
                super.onChannelJoinFailed(channelID, ecode);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, "加入信令频道失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onLogout(int ecode) {
                super.onLogout(ecode);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, "退出信令频道成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onChannelQueryUserNumResult(String channelID, int ecode, int num) {
                super.onChannelQueryUserNumResult(channelID, ecode, num);
                runOnUiThread(() -> {
                    memberCount = num;
                });
            }

            @Override
            public void onChannelUserJoined(String account, int uid) {
                super.onChannelUserJoined(account, uid);
                runOnUiThread(() -> {
                    if (agoraAPI.getStatus() == 2) {
                        agoraAPI.channelQueryUserNum(channelName);
                    }
                });
            }

            @Override
            public void onChannelUserLeaved(String account, int uid) {
                super.onChannelUserLeaved(account, uid);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, account + "退出信令频道", Toast.LENGTH_SHORT).show();
                    }
                    if (agoraAPI.getStatus() == 2) {
                        agoraAPI.channelQueryUserNum(channelName);
                    }

                    audienceHashMap.remove(account);
                    Iterator iter = audienceHashMap.entrySet().iterator();
                    audiences.clear();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        audiences.add((Audience) entry.getValue());
                    }
                    if (audienceAdapter != null) {
                        audienceAdapter.setData(audiences);
                    }
                    waiterButton.setText("参会人（" + audiences.size() + "）");
                });
            }

            @Override
            public void onUserAttrResult(String account, String name, String value) {
                super.onUserAttrResult(account, name, value);
                runOnUiThread(() -> {
                    Log.v("onUserAttrResult", "account:" + account + "---name:" + name + "---value:" + value);
                    try {
                        if (!TextUtils.isEmpty(value)) {
                            JSONObject jsonObject = new JSONObject(value);
                            Audience audience = JSON.parseObject(jsonObject.toString(), Audience.class);
                            audienceHashMap.put(account, audience);
                            Iterator iter = audienceHashMap.entrySet().iterator();
                            audiences.clear();
                            while (iter.hasNext()) {
                                Map.Entry entry = (Map.Entry) iter.next();
                                audiences.add((Audience) entry.getValue());
                            }
                            if (audienceAdapter != null) {
                                audienceAdapter.setData(audiences);
                            }
                            waiterButton.setText("参会人（" + audiences.size() + "）");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onMessageSendSuccess(String messageID) {
                super.onMessageSendSuccess(messageID);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, messageID + "-发送成功", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onMessageSendError(String messageID, int ecode) {
                super.onMessageSendError(messageID, ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, messageID + "-发送失败", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onMessageInstantReceive(final String account, final int uid, final String msg) {
                super.onMessageInstantReceive(account, uid, msg);
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonObject = new JSONObject(msg);
                        if (jsonObject.has("request")) {
                            boolean request = jsonObject.getBoolean("request");
                            Audience audience = JSON.parseObject(jsonObject.toString(), Audience.class);
                            audience.setHandsUp(request);
                            if (audience.isCalling()) {
                                currentAudience = audience;
                                audienceNameText.setTag(currentAudience);
                            }
                            audienceHashMap.put("" + audience.getUid(), audience);

                            Toast.makeText(MeetingBroadcastActivity.this, "" + audience.isCalling(), Toast.LENGTH_SHORT).show();

                            Iterator iter = audienceHashMap.entrySet().iterator();
                            audiences.clear();
                            while (iter.hasNext()) {
                                Map.Entry entry = (Map.Entry) iter.next();
                                audiences.add((Audience) entry.getValue());
                            }
                            if (audienceAdapter != null) {
                                audienceAdapter.setData(audiences);
                            }
                            waiterButton.setText("参会人（" + audiences.size() + "）");
                        }
                        if (jsonObject.has("finish")) {
                            boolean finish = jsonObject.getBoolean("finish");
                            if (finish) {
                                stopButton.setVisibility(View.GONE);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(final String name, final int ecode, final String desc) {
                super.onError(name, ecode, desc);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, "name: " + name + "ecode: " + ecode + "desc: " + desc, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onLog(String txt) {
                super.onLog(txt);
                Log.v("信令broadcast", txt);
            }
        });

    }

    private Audience currentAudience, newAudience;

    private Dialog dialog;

    private void showDialog(final int type, final String title, final String leftText, final String rightText, final Audience audience) {
        View view = View.inflate(this, R.layout.dialog_selector, null);
        TextView titleText = view.findViewById(R.id.title);
        titleText.setText(title);

        Button leftButton = view.findViewById(R.id.left);
        leftButton.setText(leftText);
        leftButton.setOnClickListener(view12 -> {
            dialog.cancel();
            if (type == 1) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend("" + audience.getUid(), 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                doLeaveChannel();
                if (agoraAPI.getStatus() == 2) {

                    agoraAPI.channelDelAttr(channelName, "doc_index");
                    agoraAPI.channelDelAttr(channelName, "material_id");

                    agoraAPI.logout();
                }
                finish();
            } else if (type == 2) {
                try {
                    audience.setCalling(true);
                    audienceNameText.setTag(audience);
                    audienceNameText.setText(audience.getUname());

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("response", true);
                    agoraAPI.messageInstantSend("" + audience.getUid(), 0, jsonObject.toString(), "");
                    Log.v("audience info--->", audience.getUid() + "---" + audience.getUname());
                    stopButton.setVisibility(View.VISIBLE);
                    currentAudience = audience;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type == 3) {

            } else if (type == 4) {

            } else if (type == 5) {
                audience.setCalling(true);
                audienceNameText.setTag(audience);
                audienceNameText.setText(audience.getUname());

                stopButton.setVisibility(View.VISIBLE);

                currentAudience = audience;

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("response", true);
                    agoraAPI.messageInstantSend("" + audience.getUid(), 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Button rightButton = view.findViewById(R.id.right);
        rightButton.setText(rightText);
        rightButton.setOnClickListener(view1 -> {
            dialog.cancel();
            if (type == 1) {
                ApiClient.getInstance().finishMeeting(TAG, meetingJoin.getMeeting().getId(), memberCount, finishMeetingCallback);
            } else if (type == 2) {
                try {
                    audience.setCalling(false);
                    audience.setHandsUp(false);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("response", false);
                    agoraAPI.messageInstantSend("" + audience.getUid(), 0, jsonObject.toString(), "");
                    Log.v("audience info--->", audience.getUid() + "---" + audience.getUname());

                    stopButton.setVisibility(View.GONE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type == 3) {
                try {
                    audience.setCalling(false);
                    audienceHashMap.put("" + audience.getUid(), audience);
                    Iterator iter = audienceHashMap.entrySet().iterator();
                    audiences.clear();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        audiences.add((Audience) entry.getValue());
                    }
                    if (audienceAdapter != null) {
                        audienceAdapter.setData(audiences);
                    }
                    waiterButton.setText("参会人（" + audiences.size() + "）");

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend("" + audience.getUid(), 0, jsonObject.toString(), "");

                    stopButton.setVisibility(View.GONE);

                    audienceNameText.setText("");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type == 4) {
                currentAudience.setCalling(false);
                currentAudience.setHandsUp(false);
                newAudience = audience;
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend("" + currentAudience.getUid(), 0, jsonObject.toString(), "");

                    stopButton.setVisibility(View.GONE);

                    audienceNameText.setText("");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type == 5) {

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

    AlertDialog alertDialog;
    AudienceAdapter audienceAdapter;

    private AudienceAdapter.OnAudienceButtonClickListener listener = new AudienceAdapter.OnAudienceButtonClickListener() {
        @Override
        public void onTalkButtonClick(Audience audience) {
            if (audienceView.getChildCount() > 0) {
                if (!audience.isCalling()) {
                    showDialog(4, "中断当前" + currentAudience.getUname() + "的连麦，连接" + audience.getUname() + "的连麦", "取消", "确定", audience);
                } else {
                    Toast.makeText(MeetingBroadcastActivity.this, "正在与当前参会人连麦中", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (!audience.isCalling()) {
                    if (audience.isHandsUp()) {
                        showDialog(2, audience.getUname() + "请求连麦", "接受", "拒绝", audience);
                    } else {
                        showDialog(5, "确定与" + audience.getUname() + "连麦", "确定", "取消", audience);
                    }
                } else {
                    Toast.makeText(MeetingBroadcastActivity.this, "正在与当前参会人连麦中", Toast.LENGTH_SHORT).show();
                }
            }
            alertDialog.cancel();
        }
    };

    private void showAlertDialog() {
        View view = View.inflate(this, R.layout.dialog_audience_list, null);
        ListView listView = view.findViewById(R.id.list_view);
        if (audienceAdapter == null) {
            audienceAdapter = new AudienceAdapter(this, audiences, listener);
        }
        listView.setAdapter(audienceAdapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialog);
        builder.setView(view);
        alertDialog = builder.create();

        Window dialogWindow = alertDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = 1000;
        lp.height = 600;
        dialogWindow.setAttributes(lp);

        alertDialog.show();
    }

    private OkHttpCallback finishMeetingCallback = new OkHttpCallback<Bucket<Meeting>>() {
        @Override
        public void onSuccess(Bucket<Meeting> meetingBucket) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("finish_meeting", true);
                agoraAPI.messageChannelSend(channelName, jsonObject.toString(), "");

                stopButton.setVisibility(View.GONE);

                audienceNameText.setText("");
            } catch (Exception e) {
                e.printStackTrace();
            }

            doLeaveChannel();
            if (agoraAPI.getStatus() == 2) {
                agoraAPI.logout();
            }
            finish();
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            Toast.makeText(MeetingBroadcastActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

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
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            worker().getEngineConfig().mUid = uid;
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

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            if (BuildConfig.DEBUG){
                Toast.makeText(MeetingBroadcastActivity.this,  "观众" + uid + "进入了", Toast.LENGTH_SHORT).show();
            }

            audienceLayout.setVisibility(View.VISIBLE);
            audienceView.removeAllViews();

            SurfaceView remoteSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
            remoteSurfaceView.setZOrderOnTop(true);
            remoteSurfaceView.setZOrderMediaOverlay(true);
            rtcEngine().setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            audienceView.addView(remoteSurfaceView);

            stopButton.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        LOG.debug("onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
        doRemoveRemoteUi(uid);
    }

    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            if (BuildConfig.DEBUG){
                Toast.makeText(MeetingBroadcastActivity.this,  uid + "退出了", Toast.LENGTH_SHORT).show();
            }
            if (currentAudience != null && uid == currentAudience.getUid()) {
                audienceView.removeAllViews();
                audienceNameText.setText("");
                audienceLayout.setVisibility(View.GONE);

                if (newAudience != null) {
                    try {
                        audienceNameText.setTag(newAudience);
                        audienceNameText.setText(newAudience.getUname());

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("response", true);
                        jsonObject.put("name", newAudience.getUname());
                        agoraAPI.messageInstantSend("" + newAudience.getUid(), 0, jsonObject.toString(), "");

                        if (audiences.contains(newAudience)) {
                            audiences.remove(newAudience);
                        }
                        if (audienceAdapter != null) {
                            audienceAdapter.setData(audiences);
                        }
                        waiterButton.setText("参会人（" + audiences.size() + "）");
                        stopButton.setVisibility(View.VISIBLE);

                        currentAudience = newAudience;
                        newAudience = null;

                        audienceLayout.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionLost() {
        runOnUiThread(() -> {
            Toast.makeText(MeetingBroadcastActivity.this, "网络连接断开，请检查网络连接", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onConnectionInterrupted() {
        runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, "网络连接不佳，视频将会有卡顿，可尝试降低分辨率", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onUserMuteVideo(final int uid, final boolean muted) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, uid + " 的视频被暂停了 " + muted, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onLastmileQuality(final int quality) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, "本地网络质量报告：" + showNetQuality(quality), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> {
//                    Toast.makeText(MeetingBroadcastActivity.this, "用户" + uid + "的\n上行网络质量：" + showNetQuality(txQuality) + "\n下行网络质量：" + showNetQuality(rxQuality), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MeetingBroadcastActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }




    @Override
    protected void onStop() {
        super.onStop();
        doLeaveChannel();
        if (agoraAPI.getStatus() == 2) {
            agoraAPI.logout();
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        showDialog(1, "确定结束会议吗？", "暂时离开", "结束会议", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        TCAgent.onPageEnd(this, "MeetingAudienceActivity");


        if (agoraAPI.getStatus() == 2) {

            agoraAPI.channelDelAttr(channelName, "doc_index");
            agoraAPI.channelDelAttr(channelName, "material_id");

            agoraAPI.logout();
        }
        agoraAPI.destroy();

        BaseApplication.getInstance().deInitWorkerThread();
    }

}
