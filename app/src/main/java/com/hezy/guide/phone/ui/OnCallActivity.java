package com.hezy.guide.phone.ui;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.OnCallActivityBinding;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.CallEvent;
import com.hezy.guide.phone.event.TvLeaveChannel;
import com.hezy.guide.phone.event.TvTimeoutHangUp;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;

import io.agora.openvcall.ui.MainActivity;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by wufan on 2017/8/3.
 */

public class OnCallActivity extends BaseDataBindingActivity<OnCallActivityBinding> {
    public static final String TAG = "OnCallActivity";

    private String channelId;
    private String tvSocketId;
    private String callInfo;

    private MediaPlayer mp;

    @Override
    public String getStatisticsTag() {
        return "被呼叫";
    }

    public static void actionStart(Context context, String channelId, String tvSocketId, String callInfo) {
        Intent intent = new Intent(context, OnCallActivity.class);
        //service中调用需要添加flag
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("channelId", channelId);
        intent.putExtra("tvSocketId", tvSocketId);
        intent.putExtra("callInfo", callInfo);
        context.startActivity(intent);
    }

    @Override
    protected void initExtraIntent() {
        channelId = getIntent().getStringExtra("channelId");
        tvSocketId = getIntent().getStringExtra("tvSocketId");
        callInfo = getIntent().getStringExtra("callInfo");
    }

    private AudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof TvLeaveChannel) {
                    ToastUtils.showToast("顾客已经挂断");
                    releaseMP();
                    ApiClient.getInstance().startOrStopOrRejectCallExpostor(channelId, "8", new OkHttpCallback<BaseErrorBean>() {
                        @Override
                        public void onSuccess(BaseErrorBean entity) {
                            Log.d(TAG, entity.toString());
                            finish();
                        }

                        @Override
                        public void onFailure(int errorCode, BaseException exception) {
                            Toast.makeText(getApplication(), "" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFinish() {
                            releaseMP();
                        }
                    });
                }else if(o instanceof TvTimeoutHangUp){
                    subscription.unsubscribe();
                    ToastUtils.showToast("未接听呼叫");
                    releaseMP();
                    finish();
                }
            }
        });

        mp = new MediaPlayer();
        try {
            mp.setDataSource(OnCallActivity.this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
            mp.prepare();
            mp.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

        mBinding.mWaterWave.startAnimation(AnimationUtils.loadAnimation(this, R.anim.water_wave));

    }

    @Override
    protected int initContentView() {
        return R.layout.on_call_activity;
    }


    @Override
    protected void initView() {
        mBinding.mIvAccept.setOnClickListener(this);
        mBinding.mIvReject.setOnClickListener(this);
        mBinding.mTvCallInfo.setText(callInfo);
    }

    @Override
    public void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mIvAccept:
                ZYAgent.onEvent(mContext,"接听按钮");
                releaseMP();
                ApiClient.getInstance().startOrStopOrRejectCallExpostor(channelId, "1", new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        Log.d("start receive", entity.toString());
                        RxBus.sendMessage(new CallEvent(true, tvSocketId));
                        Intent intent = new Intent(mContext, MainActivity.class);
                        intent.putExtra("channelId", channelId);
                        intent.putExtra("callInfo", callInfo);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(int errorCode, BaseException exception) {
                        Toast.makeText(getApplication(), "" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.mIvReject:
                ZYAgent.onEvent(mContext,"拒绝按钮");
                releaseMP();
                ApiClient.getInstance().startOrStopOrRejectCallExpostor(channelId, "2", new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        Log.d("reject receive", entity.toString());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        RxBus.sendMessage(new CallEvent(false, tvSocketId));
                        finish();
                    }

                    @Override
                    public void onFailure(int errorCode, BaseException exception) {
                        Toast.makeText(getApplication(), "" + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private Subscription subscription;

    @Override
    public void onBackPressed() {
        //返回键禁用
    }

    private void releaseMP(){
        if (mp != null) {
            mp.stop();
            mp.release();
            mp = null;
        }
    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        releaseMP();
        super.onDestroy();
    }


    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.d(TAG, "AUDIOFOCUS_GAIN [" + this.hashCode() + "]");
                    if (!mp.isPlaying()) {
                        try {
                            mp.setDataSource(OnCallActivity.this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
                            mp.prepare();
                            mp.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
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

}
