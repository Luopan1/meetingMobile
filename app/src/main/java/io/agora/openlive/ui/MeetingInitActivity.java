package io.agora.openlive.ui;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.entities.Agora;
import com.zhongyou.meet.mobile.entities.MeetingJoin;
import com.tendcloud.tenddata.TCAgent;

import io.agora.rtc.Constants;

public class MeetingInitActivity extends BaseActivity {

    private static final String TAG = MeetingInitActivity.class.getSimpleName();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Logger.e("角色为:"+meetingJoin.getRole());
            if (meetingJoin.getRole() == 0) {
                forwardToLiveRoom(Constants.CLIENT_ROLE_BROADCASTER);
            } else {
                forwardToLiveRoom(Constants.CLIENT_ROLE_AUDIENCE);
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

    public void forwardToLiveRoom(int cRole) {
        Intent intent;
        Logger.e("cRole :"+cRole+"-----"+"meetingJoin.getMeeting().getType(): "+meetingJoin.getMeeting().getType());
        if (cRole == 1) {//主持人进入
            if (meetingJoin.getMeeting().getType() == 0) {
                intent = new Intent(MeetingInitActivity.this, MeetingBroadcastActivity.class);
            } else {
                intent = new Intent(MeetingInitActivity.this, InviteMeetingBroadcastActivity.class);
            }
        } else {//参会人进入
            if (meetingJoin.getMeeting().getType() == 0) {
                intent = new Intent(MeetingInitActivity.this, MeetingAudienceActivity.class);
            } else {
                intent = new Intent(MeetingInitActivity.this, InviteMeetingAudienceActivity.class);
            }
        }
        intent.putExtra("meeting", meetingJoin);
        intent.putExtra("agora", agora);
        startActivity(intent);
        finish();
    }
}
