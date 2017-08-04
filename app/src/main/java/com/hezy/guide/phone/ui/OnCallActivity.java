package com.hezy.guide.phone.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.OnCallActivityBinding;
import com.hezy.guide.phone.event.CallEvent;
import com.hezy.guide.phone.utils.RxBus;

import io.agora.openvcall.ui.MainActivity;

/**
 * Created by wufan on 2017/8/3.
 */

public class OnCallActivity extends BaseDataBindingActivity<OnCallActivityBinding> {

    private String channelId;
    private String tvSocketId;
    private String callInfo;

    public static void actionStart(Context context,String channelId,  String tvSocketId,String callInfo) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
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
                RxBus.sendMessage(new CallEvent(true,tvSocketId));
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.putExtra("channelId", channelId);
                startActivity(intent);
                finish();
                break;
            case R.id.mIvReject:
                RxBus.sendMessage(new CallEvent(false,tvSocketId));
                finish();
                break;
        }
    }
}
