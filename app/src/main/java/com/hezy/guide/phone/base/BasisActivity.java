package com.hezy.guide.phone.base;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.utils.NetUtils;
import com.hezy.guide.phone.utils.OkHttpUtil;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.LogUtils;

import java.util.Calendar;

public class BasisActivity extends FragmentActivity implements View.OnClickListener{

    public final String TAG = getClass().getSimpleName();

    public final String FTAG = LogUtils.lifecycle;

    protected Context mContext;
    private BaseApplication mMyApp;

    protected ApiClient client;
    protected String userId;
    protected String token;

    protected ProgressDialog progressDialog;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        LogUtils.d(FTAG + TAG, "onNewIntent");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogUtils.d(FTAG + TAG, "onCreate");
        mContext = this;
        mMyApp = (BaseApplication)this.getApplicationContext();
//        userId = Preferences.getUserId();
//        token = Preferences.getToken();

        client = ApiClient.getInstance();

        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(  Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        LogUtils.d(FTAG + TAG, "onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        LogUtils.d(FTAG + TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.d(FTAG + TAG, "onResume");
//        TCAgent.onPageStart(this, TAG);
        mMyApp.setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.d(FTAG + TAG, "onPause");
//        TCAgent.onPageEnd(this, TAG);
    }


    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.d(FTAG + TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        LogUtils.d(FTAG + TAG, "onDestroy");
        OkHttpUtil.getInstance().cancelTag(this);
        unregisterReceiver(mHomeKeyEventReceiver);
        cancelDialog();
        clearReferences();
        super.onDestroy();
    }

    private void clearReferences(){
        Activity currActivity = mMyApp.getCurrentActivity();
        if (this.equals(currActivity))
            mMyApp.setCurrentActivity(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LogUtils.d(FTAG + TAG, "onSaveInstanceState");
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        LogUtils.d(FTAG + TAG, "onRestoreInstanceState");
    }

    public void showToast(int resId) {
        ToastUtils.showToast(resId);
    }

    public void showToast(String str) {
        ToastUtils.showToast(str);
    }


    protected void showDialog(String message) {
//        if(progressDialog == null){
//            progressDialog = new ProgressDialog(this, R.style.MyDialog);
//            progressDialog.setMessage(message);
//            progressDialog.setCancelable(false);
//        }
//        progressDialog.show();
    }

    protected void cancelDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }

    protected BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_HOME_KEY_LONG = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)) {
                    //表示按了home键,程序到了后台
                    Log.i(TAG,"mHomeKeyEventReceiver SYSTEM_HOME_KEY");
                    onHomeKey();
                }else if(TextUtils.equals(reason, SYSTEM_HOME_KEY_LONG)){
                    //表示长按home键,显示最近使用的程序列表
                    Log.i(TAG,"mHomeKeyEventReceiver SYSTEM_HOME_KEY_LONG");
                    onHomeKeyLong();
                }
            }
        }
    };

    protected void onHomeKey(){

    }

    protected void onHomeKeyLong(){

    }

    public static final int MIN_CLICK_DELAY_TIME = 1000;
    private long lastClickTime = 0;

    /**
     * 点击事件
     *
     * @param
     */
    @Override
    public void onClick(View v) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        //防止重复提交订单，最小点击为1秒
        if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
            lastClickTime = currentTime;

            normalOnClick(v);
            if (!NetUtils.isNetworkConnected(this)) {//not network
                Toast.makeText(this,getResources().getString(R.string.network_err_toast), Toast.LENGTH_SHORT).show();
            } else {//have network
                checkNetWorkOnClick(v);
            }
        }

    }


    /**
     * 检查网络，如果没有网络的话，就不能点击
     *
     * @param v
     */
    protected void checkNetWorkOnClick(View v) {

    }

    /**
     * 不用检查网络，可以直接触发的点击事件
     *
     * @param v
     */
    protected void normalOnClick(View v) {

    }

}
