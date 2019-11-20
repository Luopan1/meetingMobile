package io.agora.openlive.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.BuildConfig;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.entities.Agora;
import com.zhongyou.meet.mobile.entities.AudienceVideo;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.HostUser;
import com.zhongyou.meet.mobile.entities.Material;
import com.zhongyou.meet.mobile.entities.Meeting;
import com.zhongyou.meet.mobile.entities.MeetingJoin;
import com.zhongyou.meet.mobile.entities.MeetingJoinStats;
import com.zhongyou.meet.mobile.entities.MeetingMaterialsPublish;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.DensityUtil;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.ToastUtils;
import com.zhongyou.meet.mobile.utils.UIDUtil;
import com.zhongyou.meet.mobile.utils.helper.ImageHelper;
import com.zhongyou.meet.mobile.utils.statistics.ZYAgent;
import com.zhongyou.meet.mobile.view.SpaceItemDecoration;
import com.squareup.picasso.Picasso;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;

import es.dmoral.toasty.Toasty;
import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.openlive.model.AGEventHandler;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

public class InviteMeetingAudienceActivity extends BaseActivity implements AGEventHandler {

    private final static Logger LOG = LoggerFactory.getLogger(InviteMeetingAudienceActivity.class);

    private final String TAG = InviteMeetingAudienceActivity.class.getSimpleName();

    private MeetingJoin meetingJoin;
    private Meeting meeting;
    private Agora agora;
    private String broadcasterId;
    private Material currentMaterial;
    private int doc_index = 0;

    private RecyclerView audienceRecyclerView;
    private AudienceVideoAdapter audienceVideoAdapter;

    private FrameLayout broadcasterView, broadcasterFullView, audienceLayout;
    private TextView broadcastNameText, broadcastTipsText;
    private ImageButton muteAudioButton, fullScreenButton, switchCameraButton;
    private Button exitButton;
    private ImageView docImage, fullDocImage;
    private TextView pageText;

    private String channelName;

    private boolean isMuted = false;

    private boolean isFullScreen = false;

    private static final String DOC_INFO = "doc_info";

    private SurfaceView remoteBroadcasterSurfaceView;

    private AgoraAPIOnlySignal agoraAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_meeting_audience);

        registerReceiver(homeKeyEventReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        TCAgent.onEvent(this, "进入会议直播界面");

        audienceRecyclerView = findViewById(R.id.audience_list);
        audienceRecyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 3, RecyclerView.VERTICAL, false));
        audienceRecyclerView.addItemDecoration(new SpaceItemDecoration(DensityUtil.dip2px(getApplicationContext(), 4), 0, 0, DensityUtil.dip2px(getApplicationContext(), 4)));
        audienceVideoAdapter = new AudienceVideoAdapter(this);
        audienceRecyclerView.setAdapter(audienceVideoAdapter);

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
        ZYAgent.onEvent(getApplicationContext(), "meetingId=" + meetingJoin.getMeeting().getId());
        meeting = meetingJoin.getMeeting();

        if(meetingJoin==null||meetingJoin.getHostUser()==null||meetingJoin.getHostUser().getClientUid()==null){
            Toasty.error(this,"主持人还未加入会议 请稍等").show();
            finish();
            return;
        }
        broadcasterId = meetingJoin.getHostUser().getClientUid();

        channelName = meeting.getId();

        config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));



        audienceLayout = findViewById(R.id.audience_layout);
        broadcasterView = findViewById(R.id.broadcaster_view);
        broadcasterFullView = findViewById(R.id.broadcaster1_view);
        broadcastTipsText = findViewById(R.id.broadcaster_tips);
        broadcastNameText = findViewById(R.id.broadcaster_name);
        broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());
        docImage = findViewById(R.id.doc_image);
        fullDocImage = findViewById(R.id.doc1_image);
        pageText = findViewById(R.id.page);

        muteAudioButton = findViewById(R.id.mute_audio);
        muteAudioButton.setOnClickListener(v -> {
            if (!isMuted) {
                isMuted = true;
                muteAudioButton.setImageResource(R.drawable.ic_muted);
            } else {
                isMuted = false;
                muteAudioButton.setImageResource(R.drawable.ic_unmuted);
            }
            rtcEngine().muteLocalAudioStream(isMuted);
        });

        switchCameraButton = findViewById(R.id.camera_switch);
        switchCameraButton.setOnClickListener(view -> {
            rtcEngine().switchCamera();
        });

        fullScreenButton = findViewById(R.id.full_screen);
        fullScreenButton.setOnClickListener(view -> {
            if (!isFullScreen) {
                fullScreenButton.setImageResource(R.drawable.ic_full_screened);
                exitButton.setVisibility(View.GONE);
                muteAudioButton.setVisibility(View.GONE);
                switchCameraButton.setVisibility(View.GONE);
                if (currentMaterial == null) {
                    if (remoteBroadcasterSurfaceView != null) {
                        stripSurfaceView(remoteBroadcasterSurfaceView);
                    }
                    broadcasterView.removeAllViews();
                    broadcasterView.setVisibility(View.GONE);
                    broadcasterFullView.setVisibility(View.VISIBLE);
                    if (remoteBroadcasterSurfaceView != null) {
                        broadcasterFullView.addView(remoteBroadcasterSurfaceView);
                    }
                } else {
                    docImage.setVisibility(View.GONE);
                    fullDocImage.setVisibility(View.VISIBLE);
                    Picasso.with(this).load(currentMaterial.getMeetingMaterialsPublishList().get(doc_index).getUrl()).into(fullDocImage);
                }
                audienceLayout.removeView(audienceRecyclerView);
                audienceRecyclerView.setVisibility(View.GONE);
                audienceLayout.setVisibility(View.INVISIBLE);
                isFullScreen = true;
            } else {
                fullScreenButton.setImageResource(R.drawable.ic_full_screen);
                exitButton.setVisibility(View.VISIBLE);
                muteAudioButton.setVisibility(View.VISIBLE);
                switchCameraButton.setVisibility(View.VISIBLE);
                if (currentMaterial == null) {
                    if (remoteBroadcasterSurfaceView != null) {
                        stripSurfaceView(remoteBroadcasterSurfaceView);
                    }
                    broadcasterFullView.removeAllViews();
                    broadcasterFullView.setVisibility(View.GONE);
                    broadcasterView.setVisibility(View.VISIBLE);
                    if (remoteBroadcasterSurfaceView != null) {
                        broadcasterView.addView(remoteBroadcasterSurfaceView);
                    }
                } else {
                    docImage.setVisibility(View.VISIBLE);
                    fullDocImage.setVisibility(View.GONE);
                }
                audienceRecyclerView.setVisibility(View.VISIBLE);
                audienceLayout.setVisibility(View.VISIBLE);
                audienceLayout.addView(audienceRecyclerView);
                isFullScreen = false;
            }
        });

        exitButton = findViewById(R.id.finish_meeting);
        exitButton.setOnClickListener(view -> {
            showDialog(1, "确定退出会议吗？", "取消", "确定");
        });

        findViewById(R.id.exit).setOnClickListener(view -> {
            showDialog(1, "确定退出会议吗？", "取消", "确定");
        });

        worker().configEngine(Constants.CLIENT_ROLE_BROADCASTER, Constants.VIDEO_PROFILE_180P);
        rtcEngine().enableAudioVolumeIndication(400, 3);

        agoraAPI = AgoraAPIOnlySignal.getInstance(this, agora.getAppID());
        agoraAPI.callbackSet(new AgoraAPI.CallBack() {

            @Override
            public void onLoginSuccess(int uid, int fd) {
                super.onLoginSuccess(uid, fd);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "观众登陆信令系统成功", Toast.LENGTH_SHORT).show());
                }
                agoraAPI.channelJoin(channelName);
            }

            @Override
            public void onLoginFailed(final int ecode) {
                super.onLoginFailed(ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "观众登陆信令系统失败" + ecode, Toast.LENGTH_SHORT).show());
                }
                // 重新登录信令系统
                if ("true".equals(agora.getIsTest())) {
                    agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
                } else {
                    agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
                }
            }

            @Override
            public void onLogout(int ecode) {
                super.onLogout(ecode);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(InviteMeetingAudienceActivity.this, "退出信令频道成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onChannelJoined(String channelID) {
                super.onChannelJoined(channelID);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(InviteMeetingAudienceActivity.this, "观众登陆信令频道成功", Toast.LENGTH_SHORT).show();
                    }
                    if (agoraAPI.getStatus() == 2) {
                        agoraAPI.queryUserStatus(broadcasterId);
                    }
                });
            }

            @Override
            public void onReconnecting(int nretry) {
                super.onReconnecting(nretry);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "信令重连失败第" + nretry + "次", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onReconnected(int fd) {
                super.onReconnected(fd);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "信令系统重连成功", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onChannelJoinFailed(String channelID, int ecode) {
                super.onChannelJoinFailed(channelID, ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "观众登陆信令频道失败" + ecode, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onChannelQueryUserNumResult(String channelID, int ecode, final int num) {
                super.onChannelQueryUserNumResult(channelID, ecode, num);
                runOnUiThread(() -> {
                    HashMap<String, String> params = new HashMap<>();
                    params.put("count", "" + num);
                    ApiClient.getInstance().channelCount(TAG, channelCountCallback(), meetingJoin.getMeeting().getId(), params);
                });
            }

            @Override
            public void onChannelUserJoined(String account, int uid) {
                super.onChannelUserJoined(account, uid);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(InviteMeetingAudienceActivity.this, "用户" + account + "进入信令频道", Toast.LENGTH_SHORT).show();
                    }

                    agoraAPI.channelQueryUserNum(channelName);
                });

            }

            @Override
            public void onChannelUserLeaved(String account, int uid) {
                super.onChannelUserLeaved(account, uid);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(InviteMeetingAudienceActivity.this, "用户" + account + "退出信令频道", Toast.LENGTH_SHORT).show();
                    }

                    agoraAPI.channelQueryUserNum(channelName);
                });
            }

            @Override
            public void onUserAttrResult(final String account, final String name, final String value) {
                super.onUserAttrResult(account, name, value);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(InviteMeetingAudienceActivity.this, "onUserAttrResult 获取正在连麦用户" + account + "的属性" + name + "的值为" + value, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onMessageSendSuccess(String messageID) {
                super.onMessageSendSuccess(messageID);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, messageID + "发送成功", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onMessageSendError(String messageID, int ecode) {
                super.onMessageSendError(messageID, ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, messageID + "发送失败", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onMessageInstantReceive(final String account, final int uid, final String msg) {
                super.onMessageInstantReceive(account, uid, msg);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(InviteMeetingAudienceActivity.this, "onMessageInstantReceive 收到主持人" + account + "发来的消息" + msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onMessageChannelReceive(String channelID, String account, int uid, final String msg) {
                super.onMessageChannelReceive(channelID, account, uid, msg);
                runOnUiThread(() -> {
                    try {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(InviteMeetingAudienceActivity.this, "onMessageChannelReceive 收到" + account + "发的频道消息" + msg, Toast.LENGTH_SHORT).show();
                        }
                        JSONObject jsonObject = new JSONObject(msg);
                        if (jsonObject.has("finish_meeting")) {
                            boolean finishMeeting = jsonObject.getBoolean("finish_meeting");
                            if (finishMeeting) {
                                if (BuildConfig.DEBUG) {
                                    Toast.makeText(InviteMeetingAudienceActivity.this, "主持人结束了会议", Toast.LENGTH_SHORT).show();
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
                com.orhanobut.logger.Logger.e("onChannelAttrUpdated:" + "name:   " + name + "   value:  " + value + "    type:      " + type);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(InviteMeetingAudienceActivity.this, "onChannelAttrUpdated:" + "\nname:" + name + ", \nvalue:" + value + ", \ntype:" + type, Toast.LENGTH_SHORT).show();
                    }
                    if (DOC_INFO.equals(name)) {
                        if (!TextUtils.isEmpty(value)) {
                            try {
                                JSONObject jsonObject = new JSONObject(value);
                                if (jsonObject.has("material_id") && jsonObject.has("doc_index")) {
                                    doc_index = jsonObject.getInt("doc_index");
                                    if (BuildConfig.DEBUG) {
                                        Toast.makeText(InviteMeetingAudienceActivity.this, "收到主持人端index：" + doc_index, Toast.LENGTH_SHORT).show();
                                    }
                                    String materialId = jsonObject.getString("material_id");
                                    if (currentMaterial != null) {
                                        if (!materialId.equals(currentMaterial.getId())) {
                                            ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
                                        } else {
                                            if (remoteBroadcasterSurfaceView != null) {
                                                broadcasterView.removeView(remoteBroadcasterSurfaceView);
                                                broadcasterView.setVisibility(View.GONE);
                                            }
                                            MeetingMaterialsPublish currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);

                                            if (!isFullScreen) {
                                                docImage.setVisibility(View.VISIBLE);
                                                String imageUrl = ImageHelper.getThumb(currentMaterialPublish.getUrl());
                                                Picasso.with(InviteMeetingAudienceActivity.this).load(imageUrl).into(docImage);
                                            } else {
                                                broadcasterFullView.setVisibility(View.GONE);
                                                fullDocImage.setVisibility(View.VISIBLE);
                                                String imageUrl = ImageHelper.getThumb(currentMaterialPublish.getUrl());
                                                Picasso.with(InviteMeetingAudienceActivity.this).load(imageUrl).into(fullDocImage);
                                            }

                                            pageText.setVisibility(View.VISIBLE);
                                            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
                                        }
                                    } else {
                                        ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            pageText.setVisibility(View.GONE);

                            if (currentMaterial != null) {
                                currentMaterial = null;
                                audienceVideoAdapter.deleteItem(Integer.parseInt(broadcasterId));
                            }

                            if (isFullScreen) {
                                fullDocImage.setVisibility(View.GONE);
                                broadcasterFullView.setVisibility(View.VISIBLE);
                                broadcasterFullView.removeAllViews();
                                if (remoteBroadcasterSurfaceView != null) {
                                    stripSurfaceView(remoteBroadcasterSurfaceView);
                                    broadcasterFullView.addView(remoteBroadcasterSurfaceView);
                                    Log.v("add view", "ppt退出");
                                }
                            } else {
                                docImage.setVisibility(View.GONE);

                                broadcasterView.setVisibility(View.VISIBLE);
                                if (broadcasterView.getChildCount() > 0) {

                                } else {
                                    broadcasterView.removeAllViews();
                                    if (remoteBroadcasterSurfaceView != null) {
                                        stripSurfaceView(remoteBroadcasterSurfaceView);
                                        broadcasterView.addView(remoteBroadcasterSurfaceView);
                                        Log.v("add view", "ppt退出");
                                    }
                                }
                            }



                        }
                    }
                    if (type.equals("clear")){
                        pageText.setVisibility(View.GONE);

                        if (currentMaterial != null) {
                            currentMaterial = null;
                            audienceVideoAdapter.deleteItem(Integer.parseInt(broadcasterId));
                        }

                        if (isFullScreen) {
                            fullDocImage.setVisibility(View.GONE);
                            broadcasterFullView.setVisibility(View.VISIBLE);
                            broadcasterFullView.removeAllViews();
                            if (remoteBroadcasterSurfaceView != null) {
                                stripSurfaceView(remoteBroadcasterSurfaceView);
                                broadcasterFullView.addView(remoteBroadcasterSurfaceView);
                                Log.v("add view", "ppt退出");
                            }
                        } else {
                            docImage.setVisibility(View.GONE);

                            broadcasterView.setVisibility(View.VISIBLE);
                            if (broadcasterView.getChildCount() > 0) {

                            } else {
                                broadcasterView.removeAllViews();
                                if (remoteBroadcasterSurfaceView != null) {
                                    stripSurfaceView(remoteBroadcasterSurfaceView);
                                    broadcasterView.addView(remoteBroadcasterSurfaceView);
                                    Log.v("add view", "ppt退出");
                                }
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
                            Toast.makeText(InviteMeetingAudienceActivity.this, "收到错误信息\nname: " + name + "\necode: " + ecode + "\ndesc: " + desc, Toast.LENGTH_SHORT).show();
                    });
                }
//                if (agoraAPI.getStatus() != 1 && agoraAPI.getStatus() != 2 && agoraAPI.getStatus() != 3) {
//                    if ("true".equals(agora.getIsTest())) {
//                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
//                    } else {
//                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
//                    }
//                }
            }
        });

        ApiClient.getInstance().getMeetingHost(TAG, meeting.getId(), joinMeetingCallback(0));

    }

    private void stripSurfaceView(SurfaceView view) {
        ViewParent parent = view.getParent();
        if (parent != null) {
            ((FrameLayout) parent).removeView(view);
        }
    }

    private OkHttpCallback joinMeetingCallback(int uid) {
        return new OkHttpCallback<Bucket<HostUser>>() {
            @Override
            public void onSuccess(Bucket<HostUser> meetingJoinBucket) {
                com.orhanobut.logger.Logger.e(JSON.toJSONString(meetingJoinBucket));
                meetingJoin.setHostUser(meetingJoinBucket.getData());
                broadcasterId = meetingJoinBucket.getData().getClientUid();
                broadcastNameText.setText("主持人：" + meetingJoinBucket.getData().getHostUserName());
                com.orhanobut.logger.Logger.e("uid:"+uid+"---"+"broadcasterId:"+broadcasterId);
                if (uid != 0 && broadcasterId != null) {
                    if (uid == Integer.parseInt(broadcasterId)) {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(InviteMeetingAudienceActivity.this, "主持人" + uid + "回来了", Toast.LENGTH_SHORT).show();
                        }
                        remoteBroadcasterSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                        remoteBroadcasterSurfaceView.setZOrderOnTop(false);
                        remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
                        rtcEngine().setupRemoteVideo(new VideoCanvas(remoteBroadcasterSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                        broadcastTipsText.setVisibility(View.GONE);
                        fullScreenButton.setVisibility(View.VISIBLE);

                        if (currentMaterial != null) {
                            broadcasterView.setVisibility(View.GONE);
                            pageText.setVisibility(View.VISIBLE);
                            docImage.setVisibility(View.VISIBLE);
                            MeetingMaterialsPublish currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);
                            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
                            Picasso.with(InviteMeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);

                            AudienceVideo audienceVideo = new AudienceVideo();
                            audienceVideo.setUid(Integer.parseInt(broadcasterId));
                            audienceVideo.setName("主持人" + meetingJoinBucket.getData().getHostUserName());
                            audienceVideo.setBroadcaster(true);
                            audienceVideo.setSurfaceView(remoteBroadcasterSurfaceView);
                            audienceVideoAdapter.insertItem(audienceVideo);
                        } else {
                            docImage.setVisibility(View.GONE);
                            pageText.setVisibility(View.GONE);
                            broadcasterView.setVisibility(View.VISIBLE);
                            broadcasterView.removeAllViews();
                            broadcasterView.addView(remoteBroadcasterSurfaceView);
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(InviteMeetingAudienceActivity.this, "参会人" + uid + "加入", Toast.LENGTH_SHORT).show();
                        }
                        SurfaceView remoteAudienceSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                        remoteAudienceSurfaceView.setZOrderOnTop(true);
                        remoteAudienceSurfaceView.setZOrderMediaOverlay(true);
                        rtcEngine().setupRemoteVideo(new VideoCanvas(remoteAudienceSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                        audienceRecyclerView.setVisibility(View.VISIBLE);
                        com.orhanobut.logger.Logger.e(meetingJoinBucket.getData().getHostUserName());
                        AudienceVideo audienceVideo = new AudienceVideo();
                        audienceVideo.setUid(uid);
                        audienceVideo.setName("参会人" + meetingJoinBucket.getData().getHostUserName());
                        audienceVideo.setBroadcaster(false);
                        audienceVideo.setSurfaceView(remoteAudienceSurfaceView);
                        audienceVideoAdapter.insertItem(audienceVideo);
                    }
                } else {
                    SurfaceView localAudienceSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                    localAudienceSurfaceView.setZOrderOnTop(true);
                    localAudienceSurfaceView.setZOrderMediaOverlay(true);
                    rtcEngine().setupLocalVideo(new VideoCanvas(localAudienceSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
                    worker().preview(true, localAudienceSurfaceView, config().mUid);

                    audienceRecyclerView.setVisibility(View.VISIBLE);

                    AudienceVideo audienceVideo = new AudienceVideo();
                    audienceVideo.setUid(config().mUid);
                    audienceVideo.setName("参会人" +meetingJoinBucket.getData().getHostUserName() );
                    audienceVideo.setBroadcaster(false);
                    audienceVideo.setSurfaceView(localAudienceSurfaceView);
                    audienceVideoAdapter.insertItem(audienceVideo);

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
                Toast.makeText(InviteMeetingAudienceActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private OkHttpCallback meetingMaterialCallback = new OkHttpCallback<Bucket<Material>>() {

        @Override
        public void onSuccess(Bucket<Material> materialBucket) {
            Log.v("material", materialBucket.toString());
            currentMaterial = materialBucket.getData();
            Collections.sort(currentMaterial.getMeetingMaterialsPublishList(), (o1, o2) -> (o1.getPriority() < o2.getPriority()) ? -1 : 1);

            MeetingMaterialsPublish currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);

            if (remoteBroadcasterSurfaceView != null) {
                broadcasterView.removeView(remoteBroadcasterSurfaceView);
                broadcasterView.setVisibility(View.GONE);

                AudienceVideo audienceVideo = new AudienceVideo();
                audienceVideo.setUid(Integer.parseInt(broadcasterId));
                audienceVideo.setName("主持人" + meetingJoin.getHostUser().getHostUserName());
                audienceVideo.setBroadcaster(true);
                audienceVideo.setSurfaceView(remoteBroadcasterSurfaceView);
                audienceVideoAdapter.insertItem(audienceVideo);
            }

            if (!isFullScreen) {
                broadcasterFullView.setVisibility(View.GONE);
                docImage.setVisibility(View.VISIBLE);
                String imageUrl = ImageHelper.getThumb(currentMaterialPublish.getUrl());
                Picasso.with(InviteMeetingAudienceActivity.this).load(imageUrl).into(docImage);
            } else {
                broadcasterFullView.setVisibility(View.GONE);
                docImage.setVisibility(View.GONE);
                fullDocImage.setVisibility(View.VISIBLE);
                String imageUrl = ImageHelper.getThumb(currentMaterialPublish.getUrl());
                Picasso.with(InviteMeetingAudienceActivity.this).load(imageUrl).into(fullDocImage);
            }

            pageText.setVisibility(View.VISIBLE);
            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");

        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            Toast.makeText(InviteMeetingAudienceActivity.this, errorCode + "---" + exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

    private Dialog dialog;

    private void showDialog(final int type, final String title, final String leftText, final String rightText) {
        View view = View.inflate(this, R.layout.dialog_selector, null);
        TextView titleText = view.findViewById(R.id.title);
        titleText.setText(title);

        Button leftButton = view.findViewById(R.id.left);
        leftButton.setText(leftText);
        leftButton.setOnClickListener(view1 -> dialog.cancel());

        Button rightButton = view.findViewById(R.id.right);
        rightButton.setText(rightText);
        rightButton.setOnClickListener(view2 -> {
            dialog.cancel();
            if (type == 1) {
                doLeaveChannel();
                if (agoraAPI.getStatus() == 2) {
                    agoraAPI.logout();
                }
                finish();
            }
        });
        dialog = new Dialog(this, R.style.MyDialog);
        dialog.setContentView(view);
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = 740;
        lp.height = 480;
        dialogWindow.setAttributes(lp);

        dialog.show();
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
            params.put("leaveType", 1);
            ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
        }
    }

    @Override
    public void onJoinChannelSuccess(final String channel, final int uid, final int elapsed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                params.put("meetingId", meeting.getId());
                params.put("status", 1);
                params.put("type", 2);
                ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
            }
        });
    }

    private OkHttpCallback channelCountCallback() {
        return new OkHttpCallback<Bucket>() {
            @Override
            public void onSuccess(Bucket bucket) {
                Log.v("channel_count", bucket.toString());
            }
        };
    }

    private String meetingJoinTraceId;

    private OkHttpCallback meetingJoinStatsCallback = new OkHttpCallback<Bucket<MeetingJoinStats>>() {

        @Override
        public void onSuccess(Bucket<MeetingJoinStats> meetingJoinStatsBucket) {
            if (TextUtils.isEmpty(meetingJoinTraceId)) {
                meetingJoinTraceId = meetingJoinStatsBucket.getData().getId();
                Log.v("meeting_join", meetingJoinStatsBucket.toString());
            } else {
                meetingJoinTraceId = null;
                Log.v("meeting_join", "meeting join trace id is null");
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
            if (uid == Integer.parseInt(meetingJoin.getHostUser().getClientUid())) {
                broadcastTipsText.setVisibility(View.VISIBLE);
                fullScreenButton.setVisibility(View.GONE);

                broadcasterView.removeView(remoteBroadcasterSurfaceView);
                broadcasterView.setVisibility(View.GONE);

                remoteBroadcasterSurfaceView = null;
                if (currentMaterial != null) {
                    currentMaterial = null;
                    docImage.setVisibility(View.GONE);
                    fullDocImage.setVisibility(View.GONE);
                    audienceVideoAdapter.deleteItem(Integer.parseInt(broadcasterId));
                }
            } else {
                audienceVideoAdapter.deleteItem(uid);

                if (audienceVideoAdapter.getItemCount() == 0) {
                    audienceRecyclerView.setVisibility(View.GONE);
                }
            }

        });
    }

    @Override
    public void onConnectionLost() {
        runOnUiThread(() -> {
            Toast.makeText(InviteMeetingAudienceActivity.this, "网络连接断开，请检查网络连接", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    @Override
    public void onConnectionInterrupted() {
        runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "网络连接不佳，视频将会有卡顿，可尝试降低分辨率", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onUserMuteVideo(final int uid, final boolean muted) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, uid + " 的视频被暂停了 " + muted, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onUserMuteAudio(int uid, boolean muted) {
        runOnUiThread(() -> {
            audienceVideoAdapter.setMutedStatusByUid(uid, muted);
        });
    }

    @Override
    public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
        runOnUiThread(() -> {
            for (IRtcEngineEventHandler.AudioVolumeInfo audioVolumeInfo : speakers) {
                audienceVideoAdapter.setVolumeByUid(audioVolumeInfo.uid, audioVolumeInfo.volume);
            }
        });
    }

    @Override
    public void onLastmileQuality(final int quality) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "本地网络质量报告：" + showNetQuality(quality), Toast.LENGTH_SHORT).show());
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
    public void onWarning(int warn) {
        if (BuildConfig.DEBUG) {
//            runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "警告码：" + warn, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onError(final int err) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(() -> Toast.makeText(InviteMeetingAudienceActivity.this, "错误码：" + err, Toast.LENGTH_SHORT).show());
        }
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
            showDialog(1, "确定退出会议吗？", "取消", "确定");
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();

        doLeaveChannel();
        if (agoraAPI.getStatus() == 2) {
            agoraAPI.logout();
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        TCAgent.onPageEnd(this, "MeetingAudienceActivity");

        unregisterReceiver(homeKeyEventReceiver);

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
