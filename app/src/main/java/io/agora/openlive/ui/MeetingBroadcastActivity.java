package io.agora.openlive.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.Agora;
import com.hezy.guide.phone.entities.Audience;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.Material;
import com.hezy.guide.phone.entities.Materials;
import com.hezy.guide.phone.entities.Meeting;
import com.hezy.guide.phone.entities.MeetingJoin;
import com.hezy.guide.phone.entities.MeetingJoinStats;
import com.hezy.guide.phone.entities.MeetingMaterialsPublish;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.UIDUtil;
import com.hezy.guide.phone.utils.helper.ImageHelper;
import com.hezy.guide.phone.view.FocusFixedLinearLayoutManager;
import com.hezy.guide.phone.view.SpaceItemDecoration;
import com.squareup.picasso.Picasso;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
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
    private HashMap<Integer, Audience> audienceHashMap = new HashMap<Integer, Audience>();
    private ArrayList<Audience> audiences = new ArrayList<Audience>();
    private Material currentMaterial;
    private int position;

    private String channelName;
    private int memberCount;

    private boolean isMuted = false;

    private boolean isFullScreen = false;

    private FrameLayout broadcasterLayout, broadcasterSmallLayout, broadcasterSmallView, audienceView, audienceLayout;
    private TextView broadcastNameText, broadcastTipsText, audienceNameText;
    private Button audiencesButton, stopButton, docButton, previewButton, nextButton, exitDocButton;
    private TextView exitButton;
    private AgoraAPIOnlySignal agoraAPI;
    private ImageButton muteButton;
    private ImageButton fullScreenButton;
    private ImageView docImage;
    private TextView pageText;
    private SurfaceView localBroadcasterSurfaceView, remoteAudienceSurfaceView;
    private LinearLayout docLayout;

    private Audience currentAudience, newAudience;
    private int currentAiducenceId;

    private static final String DOC_INFO = "doc_info";
    private static final String CALLING_AUDIENCE = "calling_audience";

    private Handler connectingHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isConnecting) {
                Toast.makeText(MeetingBroadcastActivity.this, "连麦超时，请稍后再试!", Toast.LENGTH_SHORT).show();

                isConnecting = false;

                agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);

                if (currentAudience != null) {
                    currentAudience.setCallStatus(0);
                    audienceHashMap.put(currentAudience.getUid(), currentAudience);
                    updateAudienceList();

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend("" + currentAudience.getUid(), 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (newAudience != null) {
                    newAudience.setCallStatus(0);
                    audienceHashMap.put(newAudience.getUid(), newAudience);
                    updateAudienceList();
                    newAudience = null;
                } else {
                    currentAudience = null;
                }
            }
        }
    };

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

        broadcasterSmallLayout = findViewById(R.id.broadcaster_small_layout);
        broadcasterSmallView = findViewById(R.id.broadcaster_small_view);

        docLayout = findViewById(R.id.doc_layout);

        audienceLayout = findViewById(R.id.audience_layout);
        audienceView = findViewById(R.id.audience_view);

        docImage = findViewById(R.id.doc_image);
        pageText = findViewById(R.id.page);

        previewButton = findViewById(R.id.preview);
        previewButton.setOnClickListener(view -> {
            if (currentMaterial != null) {
                if (position > 0) {
                    position--;
                    MeetingMaterialsPublish currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(position);
                    docImage.setVisibility(View.VISIBLE);
                    String imageUrl = ImageHelper.getThumb(currentMaterialPublish.getUrl());
                    Picasso.with(MeetingBroadcastActivity.this).load(imageUrl).into(docImage);
                    pageText.setVisibility(View.VISIBLE);
                    pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("material_id", currentMaterial.getId());
                        jsonObject.put("doc_index", position);
                        agoraAPI.channelSetAttr(channelName, DOC_INFO, jsonObject.toString());
                        agoraAPI.messageChannelSend(channelName, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MeetingBroadcastActivity.this, "当前是第一张了", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MeetingBroadcastActivity.this, "没找到ppt", Toast.LENGTH_SHORT).show();
            }
        });

        nextButton = findViewById(R.id.next);
        nextButton.setOnClickListener(view -> {
            if (currentMaterial != null) {
                if (position < (currentMaterial.getMeetingMaterialsPublishList().size() - 1)) {
                    position++;
                    MeetingMaterialsPublish currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(position);
                    docImage.setVisibility(View.VISIBLE);
                    String imageUrl = ImageHelper.getThumb(currentMaterialPublish.getUrl());
                    Picasso.with(MeetingBroadcastActivity.this).load(imageUrl).into(docImage);
                    pageText.setVisibility(View.VISIBLE);
                    pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("material_id", currentMaterial.getId());
                        jsonObject.put("doc_index", position);
                        agoraAPI.channelSetAttr(channelName, DOC_INFO, jsonObject.toString());
                        agoraAPI.messageChannelSend(channelName, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(MeetingBroadcastActivity.this, "当前是最后一张了", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MeetingBroadcastActivity.this, "没找到ppt", Toast.LENGTH_SHORT).show();
            }

        });

        exitDocButton = findViewById(R.id.exit_ppt);
        exitDocButton.setOnClickListener(view -> {
            docImage.setVisibility(View.GONE);
            pageText.setVisibility(View.GONE);

            broadcasterSmallView.removeView(localBroadcasterSurfaceView);
            broadcasterSmallLayout.setVisibility(View.GONE);

            docLayout.setVisibility(View.GONE);

            broadcasterLayout.setVisibility(View.VISIBLE);
            broadcasterLayout.removeAllViews();
            broadcasterLayout.addView(localBroadcasterSurfaceView);

            currentMaterial = null;
            agoraAPI.channelDelAttr(channelName, DOC_INFO);

        });

        docButton = findViewById(R.id.doc);
        docButton.setOnClickListener((view) -> {
            ApiClient.getInstance().meetingMaterials(TAG, new OkHttpCallback<Bucket<Materials>>() {
                @Override
                public void onSuccess(Bucket<Materials> materialsBucket) {
                    showPPTListDialog(materialsBucket.getData().getPageData());
                }

                @Override
                public void onFailure(int errorCode, BaseException exception) {
                    super.onFailure(errorCode, exception);
                    Toast.makeText(MeetingBroadcastActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }, meetingJoin.getMeeting().getId());
        });

        fullScreenButton = findViewById(R.id.full_screen);
        fullScreenButton.setOnClickListener(v -> {
            if (!isFullScreen) {
                fullScreenButton.setImageResource(R.drawable.ic_full_screened);
                if (remoteAudienceSurfaceView != null) {
                    audienceView.removeView(remoteAudienceSurfaceView);
                    audienceLayout.setVisibility(View.GONE);
                    stopButton.setVisibility(View.GONE);
                }
                if (localBroadcasterSurfaceView != null) {
                    broadcasterSmallLayout.setVisibility(View.GONE);
                }
                if (currentMaterial != null) {
                    docLayout.setVisibility(View.GONE);
                }
                docButton.setVisibility(View.GONE);
                muteButton.setVisibility(View.GONE);
                audiencesButton.setVisibility(View.GONE);
                isFullScreen = true;
            } else {
                fullScreenButton.setImageResource(R.drawable.ic_full_screen);
                if (remoteAudienceSurfaceView != null) {
                    audienceLayout.setVisibility(View.VISIBLE);
                    audienceView.removeAllViews();
                    audienceView.addView(remoteAudienceSurfaceView);
                    stopButton.setVisibility(View.VISIBLE);
                }
                if (localBroadcasterSurfaceView != null) {
                    broadcasterSmallLayout.setVisibility(View.VISIBLE);
                }
                if (currentMaterial != null) {
                    docLayout.setVisibility(View.VISIBLE);
                }
                docButton.setVisibility(View.VISIBLE);
                muteButton.setVisibility(View.VISIBLE);
                audiencesButton.setVisibility(View.VISIBLE);
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

        audiencesButton = findViewById(R.id.waiter);
        audiencesButton.setOnClickListener(view -> {
            if (audiences.size() > 0) {
                showAlertDialog();
            } else {
                agoraAPI.channelClearAttr(channelName);
                if (currentAiducenceId != 0) {
                    stopButton.setVisibility(View.GONE);
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend("" + currentAiducenceId, 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            }
        });

        exitButton = findViewById(R.id.exit);
        exitButton.setOnClickListener(view -> showDialog(1, "确定结束会议吗？", "暂时离开", "结束会议", null));

        stopButton = findViewById(R.id.stop_audience);
        stopButton.setOnClickListener(view -> {
            if (currentAudience != null) {
                showDialog(3, "结束" + currentAudience.getUname() + "的发言？", "取消", "确定", currentAudience);
            } else {
                Toast.makeText(this, "当前没有连麦的参会人", Toast.LENGTH_SHORT).show();
            }
        });

        config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));

        doConfigEngine(Constants.CLIENT_ROLE_BROADCASTER);

        localBroadcasterSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
        rtcEngine().setupLocalVideo(new VideoCanvas(localBroadcasterSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
        localBroadcasterSurfaceView.setZOrderOnTop(false);
        localBroadcasterSurfaceView.setZOrderMediaOverlay(false);
        broadcasterLayout.addView(localBroadcasterSurfaceView);
        worker().preview(true, localBroadcasterSurfaceView, config().mUid);

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
                        runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, "信令系统登录成功", Toast.LENGTH_SHORT).show());
                    }
                    agoraAPI.channelJoin(channelName);
                });

            }

            @Override
            public void onLoginFailed(final int ecode) {
                super.onLoginFailed(ecode);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, "信令系统登录失败" + ecode, Toast.LENGTH_SHORT).show();
                    }
                });

                if ("true".equals(agora.getIsTest())) {
                    agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
                } else {
                    agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
                }
            }

            @Override
            public void onReconnecting(int nretry) {
                super.onReconnecting(nretry);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, "信令重连失败第" + nretry + "次", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onReconnected(int fd) {
                super.onReconnected(fd);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(() -> Toast.makeText(MeetingBroadcastActivity.this, "信令系统重连成功", Toast.LENGTH_SHORT).show());
                }
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
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, "参会人" + account + "进入信令频道", Toast.LENGTH_SHORT).show();
                    }
                    agoraAPI.channelQueryUserNum(channelName);

                    if (currentAudience != null) { // 正在连麦
                        agoraAPI.channelSetAttr(channelName, CALLING_AUDIENCE, "" + currentAudience.getUid());
                    } else { // 没有正在连麦
                        agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
                    }
                    if (currentMaterial != null) { //正在演示ppt
                        try {
                            if (BuildConfig.DEBUG) {
                                Toast.makeText(MeetingBroadcastActivity.this, "向参会人" + account + "发送ppt信息", Toast.LENGTH_SHORT).show();
                            }
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("material_id", currentMaterial.getId());
                            jsonObject.put("doc_index", position);
                            agoraAPI.channelSetAttr(channelName, DOC_INFO, jsonObject.toString());
                            agoraAPI.messageChannelSend(channelName, jsonObject.toString(), "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else { // 没有在演示ppt
                        agoraAPI.channelDelAttr(channelName, DOC_INFO);
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(MeetingBroadcastActivity.this, "参会人" + account + "上来时主持人端没有ppt信息", Toast.LENGTH_SHORT).show();
                        }
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
                    agoraAPI.channelQueryUserNum(channelName);
                    Audience audience = audienceHashMap.remove(Integer.parseInt(account));
                    if (audience != null) {
                        if (BuildConfig.DEBUG) {
                            Toast.makeText(MeetingBroadcastActivity.this, audience.getUname() + "退出信令频道", Toast.LENGTH_SHORT).show();
                        }
                    }
                    updateAudienceList();
                });
            }

            @Override
            public void onUserAttrResult(String account, String name, String value) {
                super.onUserAttrResult(account, name, value);
                runOnUiThread(() -> {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(MeetingBroadcastActivity.this, "获取到用户" + account + "的属性" + name + "的值为" + value, Toast.LENGTH_SHORT).show();
                    }
                    audienceNameText.setText(TextUtils.isEmpty(value) ? "" : value);
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
                        if (jsonObject.has("handsUp")) {
                            Audience audience = JSON.parseObject(jsonObject.toString(), Audience.class);
                            if (audience.getCallStatus() == 2) {
                                currentAudience = audience;
                            }
                            audienceHashMap.put(audience.getUid(), audience);
                            updateAudienceList();
                        }
                        if (jsonObject.has("finish")) {
                            boolean finish = jsonObject.getBoolean("finish");
                            if (finish) {
                                if (currentAudience != null && account.equals("" + currentAudience.getUid())) {
                                    stopButton.setVisibility(View.GONE);

                                    audienceView.removeAllViews();
                                    audienceNameText.setText("");
                                    audienceLayout.setVisibility(View.GONE);

                                    agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
                                }
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
                Log.v("onChannelAttrUpdated", "" + channelID + "" + name + "" + value + "" + type);
                runOnUiThread(() -> {
                    if (CALLING_AUDIENCE.equals(name)) {
                        if (TextUtils.isEmpty(value)) {
                            if (currentAudience != null) {
                                currentAudience.setCallStatus(0);
                                currentAudience.setHandsUp(false);
                                audienceHashMap.put(currentAudience.getUid(), currentAudience);
                                updateAudienceList();
                                currentAudience = null;
                            }

                            stopButton.setVisibility(View.GONE);
                            audienceView.removeAllViews();
                            audienceNameText.setText("");
                            audienceLayout.setVisibility(View.GONE);
                            if (currentMaterial != null) {
                                fullScreenButton.setVisibility(View.VISIBLE);
                            } else {
                                fullScreenButton.setVisibility(View.GONE);
                            }
                        } else {
                            currentAiducenceId = Integer.parseInt(value);
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
                            Toast.makeText(MeetingBroadcastActivity.this, "收到错误信息\nname: " + name + "\necode: " + ecode + "\ndesc: " + desc, Toast.LENGTH_SHORT).show();
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
                Log.v("信令broadcast", txt);
            }
        });

    }

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
                if (currentAudience != null) {
                    agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend("" + currentAudience.getUid(), 0, jsonObject.toString(), "");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Toast.makeText(this, "当前没有连麦人", Toast.LENGTH_SHORT).show();
                    }
                    if (currentAiducenceId != 0) {
                        stopButton.setVisibility(View.GONE);
                        try {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("finish", true);
                            agoraAPI.messageInstantSend("" + currentAiducenceId, 0, jsonObject.toString(), "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                doLeaveChannel();
                if (agoraAPI.getStatus() == 2) {
                    agoraAPI.logout();
                }
                finish();
            } else if (type == 2 || type == 5) {
                isConnecting = true;
                connectingHandler.sendEmptyMessageDelayed(0, 10000);
                currentAudience = audience;
                currentAudience.setCallStatus(1);
                currentAudience.setHandsUp(false);
                audienceHashMap.put(currentAudience.getUid(), currentAudience);
                updateAudienceList();
                audienceNameText.setText(currentAudience.getUname());

                agoraAPI.channelSetAttr(channelName, CALLING_AUDIENCE, "" + currentAudience.getUid());

            }
        });

        Button rightButton = view.findViewById(R.id.right);
        rightButton.setText(rightText);
        rightButton.setOnClickListener(view1 -> {
            dialog.cancel();
            if (type == 1) {
                ApiClient.getInstance().finishMeeting(TAG, meetingJoin.getMeeting().getId(), memberCount, finishMeetingCallback);
            } else if (type == 2) {
                audience.setCallStatus(0);
                audience.setHandsUp(false);
                audienceHashMap.put(audience.getUid(), audience);
                updateAudienceList();
                stopButton.setVisibility(View.GONE);
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("response", false);
                    agoraAPI.messageInstantSend("" + audience.getUid(), 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type == 3) {
                audience.setCallStatus(0);
                audience.setHandsUp(false);
                audienceHashMap.put(audience.getUid(), audience);
                updateAudienceList();

                stopButton.setVisibility(View.GONE);

                if (currentMaterial != null) {
                    fullScreenButton.setVisibility(View.VISIBLE);
                }

                audienceView.removeAllViews();
                audienceNameText.setText("");
                audienceLayout.setVisibility(View.GONE);

                currentAudience = null;
                agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend("" + audience.getUid(), 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type == 4) {
                newAudience = audience;

                isConnecting = true;
                connectingHandler.sendEmptyMessageDelayed(0, 10000);

                if (currentAudience != null) {
                    currentAudience.setCallStatus(0);
                    currentAudience.setHandsUp(false);
                    audienceHashMap.put(currentAudience.getUid(), currentAudience);

                    newAudience.setCallStatus(1);
                    newAudience.setHandsUp(false);
                    audienceHashMap.put(newAudience.getUid(), newAudience);

                    updateAudienceList();

                    audienceView.removeAllViews();
                    audienceNameText.setText("");
                    audienceLayout.setVisibility(View.GONE);

                    stopButton.setVisibility(View.GONE);

                    agoraAPI.channelSetAttr(channelName, CALLING_AUDIENCE, "" + newAudience.getUid());
                }
            }
        });

        dialog = new Dialog(this, R.style.MyDialog);
        dialog.setContentView(view);
        dialog.show();
    }

    AlertDialog alertDialog, pptAlertDialog, pptDetailDialog;
    AudienceAdapter audienceAdapter;

    private AudienceAdapter.OnAudienceButtonClickListener listener = new AudienceAdapter.OnAudienceButtonClickListener() {
        @Override
        public void onTalkButtonClick(Audience audience) {
            if (isConnecting) {
                Toast.makeText(MeetingBroadcastActivity.this, "暂时无法切换连麦，请10秒后尝试", Toast.LENGTH_SHORT).show();
            } else {
                if (currentAudience != null) {
                    if (currentAudience.getCallStatus() == 2 && currentAudience.getUid() != audience.getUid()) {
                        showDialog(4, "中断当前" + currentAudience.getUname() + "的连麦，连接" + audience.getUname() + "的连麦", "取消", "确定", audience);
                    } else {
                        Toast.makeText(MeetingBroadcastActivity.this, "正在与当前参会人连麦中", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (audience.getCallStatus() == 0) {
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
        }
    };

    private TextView audienceCountText;
    private EditText searchEdit;
    private Button searchButton;
    private boolean isConnecting = false;

    private void showAlertDialog() {
        View view = View.inflate(this, R.layout.dialog_audience_list, null);
        audienceCountText = view.findViewById(R.id.audience_count);
        audienceCountText.setText("所有参会人 (" + audiences.size() + ")");
        searchEdit = view.findViewById(R.id.search_edit);
        searchButton = view.findViewById(R.id.search_button);
        searchButton.setOnClickListener((view1) -> {
            if (TextUtils.isEmpty(searchEdit.getText())) {
                Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            } else {
                if (audienceAdapter != null) {
                    audienceAdapter.setData(searchAudiences(audiences, searchEdit.getText().toString()));
                }
            }
        });
        ListView listView = view.findViewById(R.id.list_view);
        if (audienceAdapter == null) {
            audienceAdapter = new AudienceAdapter(this, audiences, listener);
        } else {
            audienceAdapter.setData(audiences);
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

    private ArrayList<Audience> searchAudiences(ArrayList<Audience> audiences, String keyword) {
        ArrayList<Audience> audienceArrayList = new ArrayList<>();
        for (Audience audience : audiences) {
            if (audience.getUname().contains(keyword)) {
                audienceArrayList.add(audience);
            }
        }
        return audienceArrayList;
    }

    private void showPPTListDialog(ArrayList<Material> materials) {
        View view = View.inflate(this, R.layout.dialog_ppt_list, null);
        view.findViewById(R.id.exit).setOnClickListener(v -> {
            if (pptAlertDialog.isShowing()) {
                pptAlertDialog.dismiss();
            }
        });
        RecyclerView recyclerViewTV = view.findViewById(R.id.meeting_doc_list);
        FocusFixedLinearLayoutManager gridlayoutManager = new FocusFixedLinearLayoutManager(this); // 解决快速长按焦点丢失问题.
        gridlayoutManager.setOrientation(GridLayoutManager.HORIZONTAL);
        recyclerViewTV.setLayoutManager(gridlayoutManager);
        recyclerViewTV.setFocusable(false);
        recyclerViewTV.addItemDecoration(new SpaceItemDecoration((int) (getResources().getDimension(R.dimen.my_px_20)), 0, (int) (getResources().getDimension(R.dimen.my_px_20)), 0));
        MaterialAdapter materialAdapter = new MaterialAdapter(this, materials);
        recyclerViewTV.setAdapter(materialAdapter);
        materialAdapter.setOnClickListener((v, material, position) -> showPPTDetailDialog(material));
        AlertDialog.Builder builder = new AlertDialog.Builder(MeetingBroadcastActivity.this, R.style.MyDialog);
        builder.setView(view);
        pptAlertDialog = builder.create();
        if (!pptAlertDialog.isShowing()) {
            pptAlertDialog.show();
        }
    }

    private void showPPTDetailDialog(Material material) {
        View view = View.inflate(this, R.layout.dialog_ppt_detail, null);
        ViewPager viewPager = view.findViewById(R.id.view_pager);
        TextView pageText = view.findViewById(R.id.page);
        pageText.setText("第1/" + material.getMeetingMaterialsPublishList().size() + "页");
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return material.getMeetingMaterialsPublishList().size();
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View view = View.inflate(container.getContext(), R.layout.item_doc_detail, null);
                ImageView imageView = view.findViewById(R.id.image_view);
                String imageUrl = ImageHelper.getThumb(material.getMeetingMaterialsPublishList().get(position).getUrl());
                Picasso.with(MeetingBroadcastActivity.this).load(imageUrl).into(imageView);
                container.addView(view);
                return view;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
                return view == object;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                pageText.setText("第" + (position + 1) + "/" + material.getMeetingMaterialsPublishList().size() + "页");
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        TextView nameText = view.findViewById(R.id.name);
        nameText.setText(material.getName());
        TextView timeText = view.findViewById(R.id.time);
        timeText.setText(material.getCreateDate() + "创建");
        view.findViewById(R.id.use_doc).setOnClickListener(v -> {
            currentMaterial = material;
            Collections.sort(currentMaterial.getMeetingMaterialsPublishList(), (o1, o2) -> (o1.getPriority() < o2.getPriority()) ? -1 : 1);
            ApiClient.getInstance().meetingSetMaterial(TAG, setMaterialCallback, meetingJoin.getMeeting().getId(), currentMaterial.getId());
        });
        view.findViewById(R.id.exit_preview).setOnClickListener(v -> {
            if (pptDetailDialog.isShowing()) {
                pptDetailDialog.dismiss();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(MeetingBroadcastActivity.this, R.style.MyDialog);
        builder.setView(view);
        pptDetailDialog = builder.create();
        if (!pptDetailDialog.isShowing()) {
            pptDetailDialog.show();
        }
    }

    private OkHttpCallback setMaterialCallback = new OkHttpCallback<Bucket>() {
        @Override
        public void onSuccess(Bucket bucket) {
            Log.v("material_set", bucket.toString());
            if (pptDetailDialog.isShowing()) {
                pptDetailDialog.dismiss();
            }
            if (pptAlertDialog.isShowing()) {
                pptAlertDialog.dismiss();
            }

            broadcasterLayout.removeAllViews();
            broadcasterLayout.setVisibility(View.GONE);
            broadcasterSmallLayout.setVisibility(View.VISIBLE);
            broadcasterSmallView.removeAllViews();
            broadcasterSmallView.addView(localBroadcasterSurfaceView);

            docLayout.setVisibility(View.VISIBLE);

            docImage.setVisibility(View.VISIBLE);

            fullScreenButton.setVisibility(View.VISIBLE);

            position = 0;
            MeetingMaterialsPublish currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(position);

            pageText.setVisibility(View.VISIBLE);
            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");

            String imageUrl = ImageHelper.getThumb(currentMaterialPublish.getUrl());
            Picasso.with(MeetingBroadcastActivity.this).load(imageUrl).into(docImage);

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("material_id", currentMaterial.getId());
                jsonObject.put("doc_index", position);
                agoraAPI.channelSetAttr(channelName, DOC_INFO, jsonObject.toString());
                agoraAPI.messageChannelSend(channelName, jsonObject.toString(), "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            Toast.makeText(MeetingBroadcastActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    };

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

    private void doTEnterChannel(){
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("status", 1);
        params.put("type", 2);
        params.put("meetingId", ((MeetingJoin)(getIntent().getParcelableExtra("meeting"))).getMeeting().getId());
        ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);

    }

    private void doTLeaveChannel(){
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("meetingJoinTraceId", meetingJoinTraceId);
        params.put("meetingId", meetingJoin.getMeeting().getId());
        params.put("status", 2);
        params.put("type", 1);
        params.put("leaveType", 1);
        ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
    }

    private void doLeaveChannel() {

            worker().leaveChannel(config().mChannel);
            worker().preview(false, null, 0);
    }

    @Override
    public void onJoinChannelSuccess(final String channel, final int uid, final int elapsed) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            worker().getEngineConfig().mUid = uid;
            if ("true".equals(agora.getIsTest())) {
                agoraAPI.login2(agora.getAppID(), "" + uid, "noneed_token", 0, "", 20, 30);
            } else {
                agoraAPI.login2(agora.getAppID(), "" + uid, agora.getSignalingKey(), 0, "", 20, 30);
            }
        });
    }

    private String meetingJoinTraceId;

    private OkHttpCallback meetingJoinStatsCallback = new OkHttpCallback<Bucket<MeetingJoinStats>>() {

        @Override
        public void onSuccess(Bucket<MeetingJoinStats> meetingJoinStatsBucket) {
            if (TextUtils.isEmpty(meetingJoinTraceId)) {
                meetingJoinTraceId = meetingJoinStatsBucket.getData().getId();
                Preferences.setMeetingId(meetingJoin.getMeeting().getId());
            } else {
                meetingJoinTraceId = null;
                Preferences.setMeetingId(null);
            }
            Preferences.setMeetingTraceId(meetingJoinTraceId);
        }
    };

    @Override
    public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            if (BuildConfig.DEBUG) {
                Toast.makeText(MeetingBroadcastActivity.this, "参会人" + uid + "的视频流进入", Toast.LENGTH_SHORT).show();
            }

            fullScreenButton.setVisibility(View.VISIBLE);

            currentAiducenceId = uid;

            audienceLayout.setVisibility(View.VISIBLE);
            audienceView.removeAllViews();
            remoteAudienceSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
            remoteAudienceSurfaceView.setZOrderOnTop(true);
            remoteAudienceSurfaceView.setZOrderMediaOverlay(true);
            rtcEngine().setupRemoteVideo(new VideoCanvas(remoteAudienceSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            audienceView.addView(remoteAudienceSurfaceView);

            agoraAPI.getUserAttr(String.valueOf(uid), "uname");

            if (connectingHandler.hasMessages(0)) {
                connectingHandler.removeMessages(0);
            }

            isConnecting = false;

            if (newAudience != null) {
                currentAudience = newAudience;
                newAudience = null;
            }
            if (currentAudience != null) {
                currentAudience.setCallStatus(2);
                audienceHashMap.put(currentAudience.getUid(), currentAudience);
                updateAudienceList();
            } else {
                Toast.makeText(this, "current audience is null", Toast.LENGTH_SHORT).show();
            }

            stopButton.setVisibility(View.VISIBLE);

        });
    }

    private void updateAudienceList() {
        Iterator iter = audienceHashMap.entrySet().iterator();
        audiences.clear();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            audiences.add((Audience) entry.getValue());
        }
        if (audienceAdapter != null) {
            audienceAdapter.setData(audiences);
        }

        if (audienceCountText != null) {
            audienceCountText.setText("所有参会人 (" + audiences.size() + ")");
        }
        audiencesButton.setText("参会人（" + audiences.size() + "）");
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
            if (BuildConfig.DEBUG) {
                Toast.makeText(MeetingBroadcastActivity.this, uid + "退出了", Toast.LENGTH_SHORT).show();
            }
//            audienceView.removeAllViews();
//            audienceNameText.setText("");
//            audienceLayout.setVisibility(View.GONE);
//
//            fullScreenButton.setVisibility(View.GONE);
//            audiencesButton.setVisibility(View.VISIBLE);
//
//            remoteAudienceSurfaceView = null;
//
//            currentAudience = null;

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

    @Override
    public void onWarning(int warn) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    Toast.makeText(MeetingBroadcastActivity.this, "警告码：" + warn, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MeetingBroadcastActivity.this, "错误码：" + err, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        initUIandEvent();
        doTEnterChannel();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        if (BuildConfig.DEBUG) {
//            Toast.makeText(this, "当前没有连麦人", Toast.LENGTH_SHORT).show();
//        }
        doTLeaveChannel();

    }

//    @Override
//    protected void onUserLeaveHint() {
//        super.onUserLeaveHint();
//        doLeaveChannel();
//    }

    @Override
    public void onBackPressed() {
        showDialog(1, "确定结束会议吗？", "暂时离开", "结束会议", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        TCAgent.onPageEnd(this, "MeetingAudienceActivity");

        doLeaveChannel();
        if (agoraAPI.getStatus() == 2) {
            agoraAPI.channelClearAttr(channelName);

            if (currentAudience != null) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("finish", true);
                    agoraAPI.messageInstantSend("" + currentAudience.getUid(), 0, jsonObject.toString(), "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                currentAudience = null;
            }
            agoraAPI.logout();
        }
        agoraAPI.destroy();

//        BaseApplication.getInstance().deInitWorkerThread();
    }

}
