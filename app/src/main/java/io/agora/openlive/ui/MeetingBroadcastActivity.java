package io.agora.openlive.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
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

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

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
    private ArrayList<Audience> audiences = new ArrayList<Audience>();

    private String channelName;
    private int memberCount;

    private FrameLayout broadcasterLayout, audienceLayout;
    private TextView broadcastNameText, broadcastTipsText, countText, audienceNameText, audienceTipsText;
    private Button waiterButton, exitButton, stopButton;

    private AgoraAPIOnlySignal agoraAPI;

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

        broadcastTipsText = (TextView) findViewById(R.id.broadcast_tips);
        audienceNameText = (TextView) findViewById(R.id.audience_name);
        broadcastNameText = (TextView) findViewById(R.id.broadcaster);
        broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());
        broadcasterLayout = (FrameLayout) findViewById(R.id.broadcaster_view);
        audienceTipsText = (TextView) findViewById(R.id.audience_tips);
        audienceLayout = (FrameLayout) findViewById(R.id.audience_view);
        countText = (TextView) findViewById(R.id.online_count);

        waiterButton = (Button) findViewById(R.id.waiter);
        waiterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (audiences.size() > 0) {
                    showAlertDialog();
                }
            }
        });

        exitButton = (Button) findViewById(R.id.exit);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(1, "结束会议？", "取消", "确定", null);
            }
        });

        stopButton = (Button) findViewById(R.id.stop_audience);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Audience audience = (Audience) audienceNameText.getTag();
                showDialog(3, "结束" + audience.getUname() + "的发言？", "取消", "确定", audience);
            }
        });

        config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));

        doConfigEngine(Constants.CLIENT_ROLE_BROADCASTER);

        SurfaceView localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
        rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
        localSurfaceView.setZOrderOnTop(false);
        localSurfaceView.setZOrderMediaOverlay(false);
        broadcasterLayout.addView(localSurfaceView);
        worker().preview(true, localSurfaceView, config().mUid);

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
                            Toast.makeText(MeetingBroadcastActivity.this, "信令系统登陆成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                agoraAPI.channelJoin(channelName);
            }

            @Override
            public void onLoginFailed(final int ecode) {
                super.onLoginFailed(ecode);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeetingBroadcastActivity.this, "信令系统登陆失败" + ecode, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onChannelJoined(String channelID) {
                super.onChannelJoined(channelID);
                
                agoraAPI.setAttr("role", "0");
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
                        memberCount = num;
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
            public void onMessageSendSuccess(String messageID) {
                super.onMessageSendSuccess(messageID);
                if (BuildConfig.DEBUG) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MeetingBroadcastActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(MeetingBroadcastActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
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
                            JSONObject jsonObject = new JSONObject(msg);
                            if (jsonObject.has("request")) {
                                boolean request = jsonObject.getBoolean("request");
                                if (request) {
                                    Audience audience = JSON.parseObject(jsonObject.toString(), Audience.class);
                                    if (!audiences.contains(audience)) {
                                        audiences.add(audience);
                                    }
                                    waiterButton.setText("等待发言（" + audiences.size() + "）");

                                    if (audienceAdapter != null) {
                                        audienceAdapter.setDate(audiences);
                                    }
                                    Toast.makeText(MeetingBroadcastActivity.this, audience.getUname() + "请求发言", Toast.LENGTH_SHORT).show();
                                } else {
                                    Audience audience = JSON.parseObject(jsonObject.toString(), Audience.class);
                                    if (audiences.contains(audience)) {
                                        audiences.remove(audience);
                                    }
                                    waiterButton.setText("等待发言（" + audiences.size() + "）");

                                    if (audienceAdapter != null) {
                                        audienceAdapter.setDate(audiences);
                                    }
                                    Toast.makeText(MeetingBroadcastActivity.this, audience.getUname() + "取消了请求发言", Toast.LENGTH_SHORT).show();
                                }
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
                            Toast.makeText(MeetingBroadcastActivity.this, "name: " + name + "ecode: " + ecode + "desc: " + desc, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

    }

    private Audience currentAudience, newAudience;

    private Dialog dialog;

    private void showDialog(final int type, final String title, final String leftText, final String rightText, final Audience audience) {
        View view = View.inflate(this, R.layout.dialog_selector, null);
        TextView titleText = (TextView) view.findViewById(R.id.title);
        titleText.setText(title);

        Button leftButton = (Button) view.findViewById(R.id.left);
        leftButton.setText(leftText);
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                if (type == 1) {

                } else if (type == 2) {
                    try {
                        audienceNameText.setTag(audience);
                        audienceNameText.setText(audience.getUname());

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("response", true);
                        jsonObject.put("name", audience.getUname());
                        agoraAPI.messageInstantSend(audience.getUid(), 0, jsonObject.toString(), "");
                        if (audiences.contains(audience)) {
                            audiences.remove(audience);
                        }
                        waiterButton.setText("等待发言（" + audiences.size() + "）");
                        stopButton.setVisibility(View.VISIBLE);

                        currentAudience = audience;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (type == 3) {

                } else if (type == 4) {

                }
            }
        });

        Button rightButton = (Button) view.findViewById(R.id.right);
        rightButton.setText(rightText);
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.cancel();
                if (type == 1) {
                    ApiClient.getInstance().finishMeeting(TAG, meetingJoin.getMeeting().getId(), memberCount, finishMeetingCallback);
                } else if (type == 2) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("response", false);
                        agoraAPI.messageInstantSend(audience.getUid(), 0, jsonObject.toString(), "");

                        if (audiences.contains(audience)) {
                            audiences.remove(audience);
                        }
                        waiterButton.setText("等待发言（" + audiences.size() + "）");
                        stopButton.setVisibility(View.GONE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (type == 3) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend(audience.getUid(), 0, jsonObject.toString(), "");

                        stopButton.setVisibility(View.GONE);

                        audienceNameText.setText("");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (type == 4) {
                    newAudience = audience;
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("finish", true);
                        agoraAPI.messageInstantSend(currentAudience.getUid(), 0, jsonObject.toString(), "");

                        stopButton.setVisibility(View.GONE);

                        audienceNameText.setText("");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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

    AlertDialog alertDialog;
    AudienceAdapter audienceAdapter;

    private void showAlertDialog() {
        View view = View.inflate(this, R.layout.dialog_audience_list, null);
        ListView listView = view.findViewById(R.id.list_view);
        if (audienceAdapter == null) {
            audienceAdapter = new AudienceAdapter(this, audiences);
        }
        listView.setAdapter(audienceAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Audience audience = (Audience) audienceAdapter.getItem(i);
                if (audienceLayout.getChildCount() > 0) {
                    showDialog(4, "中断当前" + currentAudience.getUname() + "的连麦，连接" + audience.getUname() + "的连麦", "取消", "确定", audience);
                } else {
                    showDialog(2, audience.getUname() + "请求连麦", "接受", "拒绝", audience);
                }
                alertDialog.cancel();
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialog);
        builder.setView(view);
        alertDialog = builder.create();
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
            agoraAPI.logout();
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
                if (BuildConfig.DEBUG)
                    Toast.makeText(MeetingBroadcastActivity.this,  "观众" + uid + "进入了", Toast.LENGTH_SHORT).show();
                if (audienceLayout.getChildCount() > 0) {
                    audienceLayout.removeAllViews();
                }
                SurfaceView remoteSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
                remoteSurfaceView.setZOrderOnTop(true);
                remoteSurfaceView.setZOrderMediaOverlay(true);
                rtcEngine().setupRemoteVideo(new VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
                audienceLayout.addView(remoteSurfaceView);
                audienceTipsText.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        LOG.debug("onUserOffline " + (uid & 0xFFFFFFFFL) + " " + reason);
        doRemoveRemoteUi(uid);
    }

    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                if (BuildConfig.DEBUG)
                    Toast.makeText(MeetingBroadcastActivity.this,  uid + "退出了", Toast.LENGTH_SHORT).show();
                if (currentAudience != null && uid == Integer.parseInt(currentAudience.getUid())) {
                    audienceLayout.removeAllViews();
                    audienceTipsText.setVisibility(View.VISIBLE);
                    audienceNameText.setText("");

                    if (newAudience != null) {
                        try {
                            audienceNameText.setTag(newAudience);
                            audienceNameText.setText(newAudience.getUname());

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("response", true);
                            jsonObject.put("name", newAudience.getUname());
                            agoraAPI.messageInstantSend(newAudience.getUid(), 0, jsonObject.toString(), "");

                            if (audiences.contains(newAudience)) {
                                audiences.remove(newAudience);
                            }
                            if (audienceAdapter != null) {
                                audienceAdapter.setDate(audiences);
                            }
                            waiterButton.setText("等待发言（" + audiences.size() + "）");
                            stopButton.setVisibility(View.VISIBLE);

                            currentAudience = newAudience;
                            newAudience = null;
                        } catch (Exception e) {
                            e.printStackTrace();
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
                Toast.makeText(MeetingBroadcastActivity.this, "网络连接断开，请检查网络连接", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onConnectionInterrupted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MeetingBroadcastActivity.this, "网络连接不佳，视频将会有卡顿，可尝试降低分辨率", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUserMuteVideo(final int uid, final boolean muted) {
        if (BuildConfig.DEBUG) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MeetingBroadcastActivity.this, uid + " 的视频被暂停了 " + muted, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MeetingBroadcastActivity.this, "本地网络质量报告：" + showNetQuality(quality), Toast.LENGTH_SHORT).show();
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
//                    Toast.makeText(MeetingBroadcastActivity.this, "用户" + uid + "的\n上行网络质量：" + showNetQuality(txQuality) + "\n下行网络质量：" + showNetQuality(rxQuality), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(MeetingBroadcastActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        doLeaveChannel();
        agoraAPI.logout();
        finish();
    }

    @Override
    public void onBackPressed() {
        showDialog(1, "结束会议？", "取消", "确定", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        TCAgent.onPageEnd(this, "MeetingAudienceActivity");

        agoraAPI.logout();

        BaseApplication.getInstance().deInitWorkerThread();
    }

}
