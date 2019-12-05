package io.agora.openlive.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.zhongyou.meet.mobile.Constant;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.ameeting.ChairManActivity;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.ui.activity.MeetChairManActivityActivity;
import com.zhongyou.meet.mobile.entities.Agora;
import com.zhongyou.meet.mobile.entities.MeetingJoin;
import com.tendcloud.tenddata.TCAgent;

import io.agora.rtc.Constants;

public class MeetingInitActivity extends BaseActivity {

    private static final String TAG = MeetingInitActivity.class.getSimpleName();

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.e(TAG,"角色为:"+meetingJoin.getRole());
            int role = getIntent().getIntExtra("role", 0);
            Log.e(TAG,"角色为role:============="+role+"======================");
            if (role == 0) {
                forwardToLiveRoom(0);
            } else if (role==1){
                forwardToLiveRoom(1);
            }else if (role==2){
                forwardToLiveRoom(2);
            }
        }
    };

    private Agora agora;
    private MeetingJoin meetingJoin;

    private AudioManager mAudioManager;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_openlive);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }

    }

    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.d(TAG, "AUDIOFOCUS_GAIN [" + this.hashCode() + "]");

                    break;
                case AudioManager.AUDIOFOCUS_LOSS:

                    Log.d(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:

                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:

                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK [" + this.hashCode() + "]");
                    break;
            }
        }
    };

    @Override
    protected void initUIandEvent() {
        meetingJoin = getIntent().getParcelableExtra("meeting");
        agora = getIntent().getParcelableExtra("agora");
        setAppId(agora.getAppID());

        handler.sendEmptyMessageDelayed(0, 600);
    }

    @Override
    protected void deInitUIandEvent() {
    }
    Intent intent;
    public void forwardToLiveRoom(int cRole) {

        Logger.e("cRole :"+cRole+"-----"+"meetingJoin.getMeeting().getType(): "+meetingJoin.getMeeting().getType());
        Log.e(TAG, "forwardToLiveRoom: "+"cRole :"+cRole+"-----"+"meetingJoin.getMeeting().getType(): "+meetingJoin.getMeeting().getType() );
        if (cRole == 0) {//主持人进入
            Constant.videoType=0;
            Constant.isChairMan=true;
            intent = new Intent(MeetingInitActivity.this, ChairManActivity.class);

//            intent = new Intent(MeetingInitActivity.this, InviteMeetingBroadcastActivity.class);
           /* if (meetingJoin.getMeeting().getType() == 0) {
                intent = new Intent(MeetingInitActivity.this, MeetingBroadcastActivity.class);
            } else {
                intent = new Intent(MeetingInitActivity.this, InviteMeetingBroadcastActivity.class);
            }*/
        } else if (cRole==1){//参会人进入
            Constant.videoType=1;
            Constant.isChairMan=false;
            intent = new Intent(MeetingInitActivity.this, MeetingAudienceActivity.class);
//            intent = new Intent(MeetingInitActivity.this, InviteMeetingBroadcastActivity.class);
//            intent = new Intent(MeetingInitActivity.this, InviteMeetingAudienceActivity.class);
           /* if (meetingJoin.getMeeting().getType() == 0) {
                intent = new Intent(MeetingInitActivity.this, MeetingAudienceActivity.class);
            } else {
                intent = new Intent(MeetingInitActivity.this, InviteMeetingAudienceActivity.class);
            }*/
        }else if (cRole==2){
            Constant.videoType=2;
            Constant.isChairMan=false;
            intent = new Intent(MeetingInitActivity.this, MeetingAudienceActivity.class);
        }
        intent.putExtra("meeting", meetingJoin);
        intent.putExtra("agora", agora);
        startActivity(intent);
        finish();
    }
}
