package com.hezy.guide.phone.business;

import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.Constant;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.NetUtils;
import com.hezy.guide.phone.utils.OkHttpUtil;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;

import java.util.Calendar;

public abstract class BasicActivity extends FragmentActivity implements View.OnClickListener {

    public final String TAG = getClass().getSimpleName();

    private static final String RELOGINACTION =  BuildConfig.APPLICATION_ID + Constant.RELOGIN_ACTION;

    private ReLoginBroadcastReceiver reLoginBroadcastReceiver = new ReLoginBroadcastReceiver();

    protected Context mContext;
    private BaseApplication mMyApp;

    protected ApiClient apiClient;
    protected String userId;
    protected String token;

    protected ProgressDialog progressDialog;

    public abstract String getStatisticsTag() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        IntentFilter filter = new IntentFilter();
        filter.addAction(RELOGINACTION);
        registerReceiver(reLoginBroadcastReceiver, filter);

        mMyApp = (BaseApplication) this.getApplicationContext();
        userId = Preferences.getUserId();
        token = Preferences.getToken();

        apiClient = ApiClient.getInstance();

        registerReceiver(mHomeKeyEventReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZYAgent.onPageStart(this, getStatisticsTag());
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZYAgent.onPageEnd(this, getStatisticsTag());
    }

    @Override
    protected void onDestroy() {
        OkHttpUtil.getInstance().cancelTag(this);
        unregisterReceiver(mHomeKeyEventReceiver);
        unregisterReceiver(reLoginBroadcastReceiver);
        cancelDialog();
        super.onDestroy();
    }

    public void showToast(int resId) {
        ToastUtils.showToast(resId);
    }

    public void showToast(String str) {
        ToastUtils.showToast(str);
    }


    protected void showDialog(String message) {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(this, R.style.MyDialog);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
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
                    Log.i(TAG, "mHomeKeyEventReceiver SYSTEM_HOME_KEY");
                    onHomeKey();
                } else if (TextUtils.equals(reason, SYSTEM_HOME_KEY_LONG)) {
                    //表示长按home键,显示最近使用的程序列表
                    Log.i(TAG, "mHomeKeyEventReceiver SYSTEM_HOME_KEY_LONG");
                    onHomeKeyLong();
                }
            }
        }
    };

    protected void onHomeKey() {

    }

    protected void onHomeKeyLong() {

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
                Toast.makeText(this, getResources().getString(R.string.network_err_toast), Toast.LENGTH_SHORT).show();
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

    private AlertDialog dialog;

    class ReLoginBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(BuildConfig.DEBUG) {
                Toast.makeText(context, "40001|40003", Toast.LENGTH_SHORT).show();
            }
            if (dialog != null) {
                if (!dialog.isShowing()) {
                    dialog.show();
                }
            } else {
                createDialog().show();
            }
        }
    }

    private AlertDialog createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(this, R.layout.dialog_common_ok, null);
        TextView titleText = view.findViewById(R.id.title);
        titleText.setText("您的登录信息已过期，请重新登录");
        Button button = view.findViewById(R.id.ok);
        button.requestFocus();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                BasicActivity.this.finish();
            }
        });
        builder.setView(view);
        dialog = builder.create();
        return dialog;
    }

}
