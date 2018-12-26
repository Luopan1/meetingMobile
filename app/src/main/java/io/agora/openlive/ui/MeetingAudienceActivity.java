package io.agora.openlive.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Audience;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.ChatMesData;
import com.hezy.guide.phone.entities.HostUser;
import com.hezy.guide.phone.entities.Material;
import com.hezy.guide.phone.entities.Meeting;
import com.hezy.guide.phone.entities.MeetingHostingStats;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.MeetingJoinStats;
import com.hezy.guide.phone.entities.MeetingMaterialsPublish;
import com.hezy.guide.phone.entities.MeetingScreenShot;
import com.hezy.guide.phone.entities.QiniuToken;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.ForumRevokeEvent;
import com.hezy.guide.phone.event.ForumSendEvent;
import com.hezy.guide.phone.meetingcamera.activity.Camera1ByServiceActivity;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.UIDUtil;
import com.hezy.guide.phone.utils.statistics.ZYAgent;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.squareup.picasso.Picasso;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.openlive.model.AGEventHandler;
import io.agora.openlive.model.ConstantApp;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import rx.Subscription;
import rx.functions.Action1;

public class MeetingAudienceActivity extends BaseActivity implements AGEventHandler {

    private final static Logger LOG = LoggerFactory.getLogger(MeetingAudienceActivity.class);

    private final String TAG = MeetingAudienceActivity.class.getSimpleName();
    private Timer takePhotoTimer;
    private TimerTask takePhotoTimerTask;
    private final int CODE_REQUEST_TAKEPHOTO = 8011;

    private MeetingJoin meetingJoin;
    private Meeting meeting;
    private Agora agora;
    private String broadcastId;

    private FrameLayout broadcasterLayout, broadcasterSmallLayout, broadcasterSmallView, audienceLayout, audienceView;
    private TextView broadcastNameText, broadcastTipsText, countText, audienceNameText;
    private Button requestTalkButton, stopTalkButton, disCussButton;
    private TextView exitButton, pageText, tvChat, tvChatName, tvChatAddress, tvName, tvAddress, tvContent, tvOpenComment;

    private ImageView docImage;
    private ImageButton fullScreenButton;

    private boolean isDocShow = false;

    private Material currentMaterial;
    private MeetingMaterialsPublish currentMaterialPublish;
    private int doc_index = 0;

    private String channelName;

    private String audienceName;

    private Subscription subscription;
    private SurfaceView remoteBroadcasterSurfaceView, remoteAudienceSurfaceView, localSurfaceView;

    private AgoraAPIOnlySignal agoraAPI;
    private LinearLayout llMsg, llChat, llSmallChat;
    private static final String DOC_INFO = "doc_info";
    private static final String CALLING_AUDIENCE = "calling_audience";

    private int currentAudienceId;
    InMeetChatFragment fragment;
    boolean hideFragment = false;
    boolean JoinSuc = false;

    private Handler ChatHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 23){
                llChat.setVisibility(View.INVISIBLE);
                return;
            }
            if (msg.what == 22) {
                Log.v("llchat989890", tvChat.getWidth() + "****tvChat***后");
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                Log.v("llchat9898llChat", findViewById(R.id.small_chat).getWidth() + "*******后");
                Log.v("llchat9898", disCussButton.getLeft() + "*******2");
                params.bottomMargin = 106;
                params.gravity = Gravity.BOTTOM;
                params.leftMargin = disCussButton.getLeft() - (llSmallChat.getWidth() / 2) + disCussButton.getWidth() / 2;
                Log.v("llchat9898", params.leftMargin + "*******3");
                if (params.leftMargin < 90) {
                    params.leftMargin = 90;
                    LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params2.leftMargin = disCussButton.getLeft() - 90 + (disCussButton.getWidth() / 2);
                    findViewById(R.id.img_tri).setLayoutParams(params2);
                    llChat.setLayoutParams(params);
                } else {
                    LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params2.gravity = Gravity.CENTER_HORIZONTAL;
                    findViewById(R.id.img_tri).setLayoutParams(params2);
                    llChat.setLayoutParams(params);
                }
                return;
            }
            if (hideFragment) {
                llMsg.setVisibility(View.GONE);
                llChat.setVisibility(View.INVISIBLE);
            } else {
                if (isFullScreen) {
                    llMsg.setVisibility(View.VISIBLE);
                    llChat.setVisibility(View.INVISIBLE);
                } else {
                    llMsg.setVisibility(View.GONE);
                    llChat.setVisibility(View.VISIBLE);
                    if(ChatHandler.hasMessages(23)){
                        ChatHandler.removeMessages(23);
                    }
                    ChatHandler.sendEmptyMessageDelayed(23, 5100);
                    tvChat.setVisibility(View.VISIBLE);
                }
            }
            if (((ChatMesData.PageDataEntity) msg.obj).getType() == 1) {
                tvChat.setTextColor(getResources().getColor(R.color.color_7FBAFF));
                tvChat.setText("[发送一张图片]");
            } else {
                tvChat.setTextColor(getResources().getColor(R.color.white));
                tvChat.setText(((ChatMesData.PageDataEntity) msg.obj).getContent());
            }
//            tvChat.setText(" : "+((ChatMesData.PageDataEntity)msg.obj).getContent());
            tvAddress.setText("未填写");
            tvName.setText(((ChatMesData.PageDataEntity) msg.obj).getUserName() + "");

            if (((ChatMesData.PageDataEntity) msg.obj).getType() == 1) {
                tvContent.setTextColor(getResources().getColor(R.color.color_7FBAFF));
                tvContent.setText(" ：[发送一张图片]");
            } else {
                tvContent.setTextColor(getResources().getColor(R.color.white));
                tvContent.setText(" : " + ((ChatMesData.PageDataEntity) msg.obj).getContent());
            }

            if (((ChatMesData.PageDataEntity) msg.obj).getMsgType() == 1) {
                tvContent.setTextColor(getResources().getColor(R.color.color_7FBAFF));
                tvContent.setText(" ：[撤回一条消息]");
            }

            if (((ChatMesData.PageDataEntity) msg.obj).getMsgType() == 1) {
                tvChat.setTextColor(getResources().getColor(R.color.color_7FBAFF));
                tvChat.setText("[撤回一条消息]");
            }
            tvChatAddress.setText("未填写");
            tvChatName.setText(((ChatMesData.PageDataEntity) msg.obj).getUserName() + " : ");
            ChatHandler.sendEmptyMessageDelayed(22, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_audience);
        TCAgent.onEvent(this, "进入会议直播界面");
//        if (!WSService.isOnline()) {
//            //当前状态离线,可切换在线
//            ZYAgent.onEvent(this, "在线按钮,当前离线,切换到在线");
//            Log.i(TAG, "当前状态离线,可切换在线");
//            RxBus.sendMessage(new SetUserChatEvent(true));
//        } else {
//            ZYAgent.onEvent(this, "在线按钮,当前在线,,无效操作");
//        }

        registerReceiver(homeKeyEventReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                String meetingid = "";
                if (((MeetingJoin) (getIntent().getParcelableExtra("meeting"))) != null && ((MeetingJoin) (getIntent().getParcelableExtra("meeting"))).getMeeting() != null) {
                    meetingid = ((MeetingJoin) (getIntent().getParcelableExtra("meeting"))).getMeeting().getId();
                }

                if (o instanceof ForumSendEvent) {

                    if (((ForumSendEvent) o).getEntity().getMeetingId().equals(meetingid)) {
                        Message msg = new Message();
                        msg.obj = ((ForumSendEvent) o).getEntity();
//                    tvChat.setText(((ForumSendEvent) o).getEntity().getContent());
                        ChatHandler.sendMessage(msg);
                    }


                } else if (o instanceof ForumRevokeEvent) {
//                    requestRecordOnlyLast(true);

                    if (((ForumRevokeEvent) o).getEntity().getMeetingId().equals(meetingid)) {
                        Message msg = new Message();
                        msg.obj = ((ForumRevokeEvent) o).getEntity();
                        ChatHandler.sendMessage(msg);
                    }

                }

            }
        });
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
        fragment = InMeetChatFragment.newInstance(meetingJoin.getMeeting().getId());
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.rl_content, fragment);
        fragmentTransaction.hide(fragment);
        fragmentTransaction.commitAllowingStateLoss();
        audienceName = (TextUtils.isEmpty(Preferences.getAreaName()) ? "" : Preferences.getAreaName()) + "-" + (TextUtils.isEmpty(Preferences.getUserCustom()) ? "" : Preferences.getUserCustom()) + "-" + Preferences.getUserName();

        broadcasterLayout = findViewById(R.id.broadcaster_view);
        broadcasterLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideFragment();
                if (isFullScreen) {
                    if (!tvContent.getText().toString().isEmpty())
                        llMsg.setVisibility(View.VISIBLE);

                } else {
//                    if (!tvChat.getText().toString().isEmpty())
//                        llChat.setVisibility(View.VISIBLE);

                }
            }
        });
        broadcastTipsText = findViewById(R.id.broadcast_tips);
        broadcastNameText = findViewById(R.id.broadcaster);
        tvChat = findViewById(R.id.tv_chat);
        broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());
        broadcasterSmallLayout = findViewById(R.id.broadcaster_small_layout);
        broadcasterSmallView = findViewById(R.id.broadcaster_small_view);
        docImage = findViewById(R.id.doc_image);
        docImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideFragment();
                if (isFullScreen) {
                    if (!tvContent.getText().toString().isEmpty())
                        llMsg.setVisibility(View.VISIBLE);
                } else {
//                    if (!tvChat.getText().toString().isEmpty())
//                        llChat.setVisibility(View.VISIBLE);
                }
            }
        });
        pageText = findViewById(R.id.page);
        llMsg = findViewById(R.id.ll_msg);
        llChat = findViewById(R.id.ll_chat);

        llSmallChat = findViewById(R.id.small_chat);
        tvChatAddress = findViewById(R.id.tv_chat_address);
        tvChatName = findViewById(R.id.tv_chat_name);
        tvName = findViewById(R.id.tv_name);
        tvAddress = findViewById(R.id.tv_addres);
        tvContent = findViewById(R.id.tv_content);
        tvOpenComment = findViewById(R.id.open_comment);
        disCussButton = findViewById(R.id.discuss);
        disCussButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initFragment();
                llChat.setVisibility(View.INVISIBLE);
            }
        });
        tvOpenComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initFragment();
                llMsg.setVisibility(View.GONE);
            }
        });

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
                disCussButton.setVisibility(View.GONE);
                if (!tvContent.getText().toString().isEmpty())
                    llMsg.setVisibility(View.VISIBLE);
                llChat.setVisibility(View.INVISIBLE);
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
                llMsg.setVisibility(View.GONE);
//                if (!tvChat.getText().toString().isEmpty())
//                    llChat.setVisibility(View.VISIBLE);
                disCussButton.setVisibility(View.VISIBLE);
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

        worker().configEngine(Constants.CLIENT_ROLE_AUDIENCE, Constants.VIDEO_PROFILE_180P);

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
                                    params.put("type", 2);
                                    params.put("leaveType", 1);
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
                        if (jsonObject.has("ic_finish_meeting")) {
                            boolean finishMeeting = jsonObject.getBoolean("ic_finish_meeting");
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
                                    params.put("type", 2);
                                    params.put("leaveType", 1);
                                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                                }
                            }
                            audienceView.removeAllViews();
                            audienceNameText.setText("");
                            audienceLayout.setVisibility(View.GONE);
                            stopTalkButton.setVisibility(View.GONE);
                            requestTalkButton.setVisibility(View.VISIBLE);

                            if (currentAudienceId == config().mUid) {
                                handsUp = false;
                                requestTalkButton.setText("我要发言");
                                requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);
                            }
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
                                worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                                agoraAPI.setAttr("uname", null);

                                requestTalkButton.setVisibility(View.VISIBLE);
                                stopTalkButton.setVisibility(View.GONE);

                                if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                                    HashMap<String, Object> params = new HashMap<String, Object>();
                                    params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                                    params.put("status", 2);
                                    params.put("meetingId", meetingJoin.getMeeting().getId());
                                    params.put("type", 2);
                                    params.put("leaveType", 1);
                                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                                }
                                if (localSurfaceView != null) {
                                    localSurfaceView = null;
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
                if (ecode == 1002) {
                    return;
                }
//                if (agoraAPI.getStatus() != 1 && agoraAPI.getStatus() != 2 && agoraAPI.getStatus() != 3) {
//                    if ("true".equals(agora.getIsTest())) {
//                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
//                    } else {
//                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
//                    }
//                }
            }

            @Override
            public void onLog(String txt) {
                super.onLog(txt);
                Log.v("audience信令", txt);
            }
        });

        ApiClient.getInstance().getMeetingHost(TAG, meeting.getId(), joinMeetingCallback(0));
        startMeetingCamera(meeting.getScreenshotFrequency());
    }

    private void startMeetingCamera(int screenshotFrequency) {
        if (screenshotFrequency == Meeting.SCREENSHOTFREQUENCY_INVALID) {
            //不抓拍
            return;
        }
        takePhotoTimer = new Timer();
        takePhotoTimerTask = new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(MeetingAudienceActivity.this, Camera1ByServiceActivity.class);
                intent.putExtra(Camera1ByServiceActivity.KEY_IMAGE_COMPRESSION_RATIO, meeting.getScreenshotCompressionRatio());
                startActivityForResult(intent, CODE_REQUEST_TAKEPHOTO);
                overridePendingTransition(0, 0);
            }
        };
        takePhotoTimer.schedule(takePhotoTimerTask, screenshotFrequency * 1000, screenshotFrequency * 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_REQUEST_TAKEPHOTO) {
            try {
                String pictureLocalPath = data.getStringExtra("pictureLocalPath");
                uploadMeetingImageToQiniu(pictureLocalPath);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * 上传参会人参会时图片到七牛服务器
     *
     * @param imagePath
     */
    private void uploadMeetingImageToQiniu(String imagePath) {
        ApiClient.getInstance().requestQiniuToken(this, new OkHttpCallback<BaseBean<QiniuToken>>() {

            @Override
            public void onSuccess(BaseBean<QiniuToken> result) {
                String token = result.getData().getToken();
                if (TextUtils.isEmpty(token)) {
                    String errorMsg = "七牛token获取错误";
                    ZYAgent.onEvent(getApplicationContext(), errorMsg);
                    ToastUtils.showToast(errorMsg);
                    return;
                }
                Configuration config = new Configuration.Builder().connectTimeout(5).responseTimeout(5).build();
                UploadManager uploadManager = new UploadManager(config);
                ZYAgent.onEvent(getApplicationContext(), "拍照完毕，准备上传本地图片：" + imagePath);

                String uploadKey = BuildConfig.QINIU_IMAGE_UPLOAD_PATH + meeting.getId() + "/" + Preferences.getUserId() + "/" + imagePath.substring(imagePath.lastIndexOf('/') + 1);
                uploadManager.put(new File(imagePath), uploadKey, token, meetingImageUpCompletionHandler, new UploadOptions(null, null, true, new UpProgressHandler() {
                    @Override
                    public void progress(final String key, final double percent) {
                    }
                }, null));
            }
        });
    }

    private UpCompletionHandler meetingImageUpCompletionHandler = new UpCompletionHandler() {
        @Override
        public void complete(String key, ResponseInfo info, JSONObject response) {
            if (info.isNetworkBroken() || info.isServerError()) {
                ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传七牛云失败");
                return;
            }
            if (info.isOK()) {
                String meetingImageUrl = BuildConfig.QINIU_IMAGE_DOMAIN + key;
                ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传七牛云成功，地址：" + meetingImageUrl);
                uploadMeetingImageToServer(meeting.getId(), meetingImageUrl);
            } else {
                ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传七牛云失败");
            }
        }
    };

    /**
     * 上传参会人参会时图片到Server
     *
     * @param meetingId
     * @param qiniuImageUrl
     */
    public void uploadMeetingImageToServer(String meetingId, String qiniuImageUrl) {
        Map<String, Object> params = new HashMap<>();
        params.put("meetingId", meetingId);
        params.put("imgUrl", qiniuImageUrl);
        params.put("ts", System.currentTimeMillis());
        ApiClient.getInstance().meetingScreenshot(this, params, uploadMeetingImageToServerCallback);
    }

    private OkHttpCallback<Bucket<MeetingScreenShot>> uploadMeetingImageToServerCallback = new OkHttpCallback<Bucket<MeetingScreenShot>>() {

        @Override
        public void onSuccess(Bucket<MeetingScreenShot> meetingScreenShotBucket) {
            ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传服务器成功");
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传服务器失败，错误提示: " + errorCode + ", Excepton: " + exception.getMessage());
        }
    };

    private OkHttpCallback joinMeetingCallback(int uid) {
        return new OkHttpCallback<Bucket<HostUser>>() {

            @Override
            public void onSuccess(Bucket<HostUser> meetingJoinBucket) {
                meetingJoin.setHostUser(meetingJoinBucket.getData());
                broadcastId = meetingJoinBucket.getData().getClientUid();
                broadcastNameText.setText("主持人：" + meetingJoinBucket.getData().getHostUserName());
                if (uid != 0 && broadcastId != null) {
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

                    if (agoraAPI.getStatus() == 2) {
                        agoraAPI.setAttr("uname", null);
                        agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
                    }

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
                        params.put("type", 2);
                        params.put("leaveType", 1);
                        ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                    }
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (localSurfaceView == null && remoteAudienceSurfaceView != null) {
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

                handsUp = false;
                requestTalkButton.setText("我要发言");
                requestTalkButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_meeting_signup, 0, 0, 0);

                if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
                    params.put("status", 2);
                    params.put("meetingId", meetingJoin.getMeeting().getId());
                    params.put("type", 2);
                    params.put("leaveType", 1);
                    ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                }
            }
        });

        dialog = new Dialog(this, R.style.MyDialog);
        dialog.setContentView(view);

        dialog.show();
    }

    @Override
    protected void deInitUIandEvent() {
        doLeaveChannel();
        event().removeEventHandler(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (JoinSuc) {
            if (TextUtils.isEmpty(meetingJoinTraceId)) {
                doTEnterChannel();
            }
        }


    }

    private void doTEnterChannel() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("status", 1);
        params.put("type", 2);
        params.put("meetingId", ((MeetingJoin) (getIntent().getParcelableExtra("meeting"))).getMeeting().getId());
        ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
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
            params.put("leaveType", 1);
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
            JoinSuc = true;
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
                        params.put("type", 2);
                        params.put("leaveType", 1);
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
    public void onUserMuteAudio(int uid, boolean muted) {

    }

    @Override
    public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {

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
        if (ChatHandler.hasMessages(22)) {
            ChatHandler.removeMessages(22);
        }
        if (ChatHandler.hasMessages(23)) {
            ChatHandler.removeMessages(23);
        }

        unregisterReceiver(homeKeyEventReceiver);

        subscription.unsubscribe();
//        if (WSService.isOnline()) {
//            //当前状态在线,可切换离线
//            Log.i(TAG, "当前状态在线,可切换离线");
//            ZYAgent.onEvent(this, "离线按钮,当前在线,切换到离线");
//            RxBus.sendMessage(new SetUserChatEvent(false));
////                                            WSService.SOCKET_ONLINE =false;
////                                            setState(false);
//        } else {
//            ZYAgent.onEvent(this, "离线按钮,当前离线,无效操作");
//        }
        TCAgent.onPageEnd(this, "MeetingAudienceActivity");

        if (takePhotoTimer != null && takePhotoTimerTask != null) {
            takePhotoTimer.cancel();
            takePhotoTimerTask.cancel();
        }

//        BaseApplication.getInstance().deInitWorkerThread();
    }

    private void initFragment() {
        hideFragment = true;
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.show(fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void hideFragment() {
        hideFragment = false;
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.hide(fragment);
        fragmentTransaction.commitAllowingStateLoss();
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
                    if (BuildConfig.DEBUG)
                        Toast.makeText(getApplicationContext(), "您点击了Home键", Toast.LENGTH_SHORT).show();

                    if (localSurfaceView != null && remoteAudienceSurfaceView == null) {
                        audienceView.removeAllViews();
                        audienceNameText.setText("");
                        audienceLayout.setVisibility(View.GONE);

                        localSurfaceView = null;

                        if (agoraAPI.getStatus() == 2) {
                            agoraAPI.setAttr("uname", null);
                            agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
                        }

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
                            params.put("type", 2);
                            params.put("leaveType", 1);
                            ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
                        }
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("finish", true);
                            agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (localSurfaceView == null && remoteAudienceSurfaceView != null) {
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
                } else if (TextUtils.equals(reason, RECENTAPPS)) {
                    // 点击 菜单键
                    if (BuildConfig.DEBUG)
                        Toast.makeText(getApplicationContext(), "您点击了菜单键", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

}
