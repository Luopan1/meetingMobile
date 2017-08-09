package com.hezy.guide.phone.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.HomeActivityBinding;
import com.hezy.guide.phone.entities.Version;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.PagerSetGuideLog;
import com.hezy.guide.phone.event.PagerSetUserinfo;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.service.WSService;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.UUIDUtils;
import com.hezy.guide.phone.wxapi.WXEntryActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import me.kaelaela.verticalviewpager.transforms.DefaultTransformer;
import rx.Subscription;
import rx.functions.Action1;


/**
 * 主页
 * Created by wufan on 2017/7/24.
 */

public class HomeActivity extends BaseDataBindingActivity<HomeActivityBinding> {
    private HomePagerAdapter mHomePagerAdapter;
    private ArrayList<Fragment> mFragments;
    private int mIntentType;
    private boolean isNewActivity;
    private Dialog dialog;


    public static void actionStart(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(savedInstanceState != null){
            isNewActivity=true;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int initContentView() {
        return R.layout.home_activity;
    }


    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
//        setIntent(intent);//must store the new intent unless getIntent() will return the old one
        //如果没销毁,注销只调用onNewIntent,需要在这里处理注销逻辑
        processExtraData(intent);


    }


    private void processExtraData(Intent intent) {
        mIntentType = intent.getIntExtra(LoginHelper.LOGIN_TYPE, 0);
        LogUtils.d(TAG,"mIntentType "+mIntentType);
        if (mIntentType == LoginHelper.LOGIN_TYPE_EXIT) {
            //退出应用
            LogUtils.d(TAG,"退出应用");
            quit();
        }else if(mIntentType == LoginHelper.LOGIN_TYPE_LOGOUT){
            //退出登录
            LogUtils.d(TAG,"退出登录");
//            showLogoutForceDialog();
//            if(!isNewActivity){
//                //非新activity,需要修改登录UI
//                Log.i(TAG, "发送LogoutEvent");
////                RxBus.sendMessage(new LogoutEvent());
//            }
            WXEntryActivity.actionStart(mContext);
            finish();
        }
        //复位标志
        mIntentType = 0;
    }




    @Override
    protected void initView() {
//        mBinding.mVerticalViewPager.setPageTransformer(true, new VerticalTransformer());
//        mBinding.mVerticalViewPager.setOverScrollMode(OVER_SCROLL_NEVER);
        //登录才能进入主页,启动心跳
//        HeartService.actionStart(mContext);
        WSService.actionStart(mContext);


        mBinding.mVerticalViewPager.setPageTransformer(false, new DefaultTransformer());

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof PagerSetUserinfo) {
                    mBinding.mVerticalViewPager.setCurrentItem(0);
                } else if (o instanceof PagerSetGuideLog) {
                    mBinding.mVerticalViewPager.setCurrentItem(1);
                }
            }
        });


    }

    @Override
    protected void requestData() {
        versionCheck();
        registerDevice();
    }

    @Override
    protected void initAdapter() {
        mHomePagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        mFragments = new ArrayList<>();
        mFragments.add(UserinfoFragment.newInstance());
        mFragments.add(GuideLogFragment.newInstance());
        mHomePagerAdapter.setData(mFragments);
        mBinding.mVerticalViewPager.setAdapter(mHomePagerAdapter);
        LogUtils.i(TAG, "getUserMobile" + Preferences.getUserMobile());
        if (!TextUtils.isEmpty(Preferences.getUserMobile()) && !TextUtils.isEmpty(Preferences.getUserPhoto())) {
            //手机非空,照片非空,默认进入日志页面
            LogUtils.i(TAG, "手机非空,默认进入日志页面");
            mBinding.mVerticalViewPager.setCurrentItem(1);
        }
    }


    private void versionCheck() {
        ApiClient.getInstance().versionCheck(this, new OkHttpBaseCallback<BaseBean<Version>>() {
            @Override
            public void onSuccess(BaseBean<Version> entity) {
                Version version = entity.getData();
                if (version == null || version.getImportance() == 0) {
                    return;
                }
                if (version.getImportance() != 1 && version.getImportance() != 2) {
                    startActivity(new Intent(mContext, UpdateDownloadActivity.class).putExtra("version", version));
                }


            }
        });
    }

    /**
     * 上传设备信息
     */
    private void registerDevice() {

        String uuid = UUIDUtils.getUUID(this);
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;
        int height = metric.heightPixels;
        float density = metric.density;
        int densityDpi = metric.densityDpi;


        StringBuffer msg = new StringBuffer("registerDevice ");
        if (TextUtils.isEmpty(uuid)) {
            msg.append("UUID为空");
            showToast(msg.toString());
            LogUtils.e(TAG, msg.toString());
        } else {

            try {
                JSONObject params = new JSONObject();
                params.put("uuid", uuid);
                params.put("androidId", TextUtils.isEmpty(Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID)) ? "" : Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                params.put("manufacturer", Build.MANUFACTURER);
                params.put("name", Build.BRAND);
                params.put("model", Build.MODEL);
                params.put("sdkVersion", Build.VERSION.SDK_INT);
                params.put("screenDensity", "width:" + width + ",height:" + height + ",density:" + density + ",densityDpi:" + densityDpi);
                params.put("display", Build.DISPLAY);
                params.put("finger", Build.FINGERPRINT);
                params.put("appVersion", BuildConfig.FLAVOR + "_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE);
                params.put("cpuSerial", Installation.getCPUSerial());
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                params.put("androidDeviceId", tm != null ? tm.getDeviceId() : "");
                params.put("buildSerial", Build.SERIAL);
                params.put("source", 2);
                ApiClient.getInstance().deviceRegister(this, params.toString(), registerDeviceCb);
                msg.append("client.deviceRegister call");
            } catch (JSONException e) {
                e.printStackTrace();
                msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
            }

        }
//        client.errorlog(mContext, 2, msg.toString(), respStatusCallback);


    }

    private OkHttpBaseCallback registerDeviceCb = new OkHttpBaseCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {
            Log.d(TAG, "registerDevice 成功===");
        }
    };

    private long mExitTime = 0;
    private long mLastKeyDownTime = 0;

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//            if ((System.currentTimeMillis() - mExitTime) > 2000) {//
//                // 如果两次按键时间间隔大于2000毫秒，则不退出
//                Toast.makeText(this, "再按一次退出在线导购", Toast.LENGTH_SHORT).show();
//                mExitTime = System.currentTimeMillis();// 更新mExitTime
//            } else {
//                quit(); // 否则退出程序
//            }
//            return true;
//        }
//        return super.onKeyDown(keyCode, event);
//
//    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - mExitTime) > 2000) {//
            // 如果两次按键时间间隔大于2000毫秒，则不退出
            Toast.makeText(this, "再按一次退出在线导购", Toast.LENGTH_SHORT).show();
            mExitTime = System.currentTimeMillis();// 更新mExitTime
        } else {
            quit(); // 否则退出程序
        }
    }

    private void quit() {
//        HeartService.stopService(this);
        WSService.stopService(this);
        finish();
        System.exit(0);
    }

    private Subscription subscription;

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }
}
