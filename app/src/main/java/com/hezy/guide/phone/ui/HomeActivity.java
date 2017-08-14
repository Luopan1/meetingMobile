package com.hezy.guide.phone.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.adorkable.iosdialog.ActionSheetDialog;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.HomeActivityBinding;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.UserData;
import com.hezy.guide.phone.entities.Version;
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.PagerSetGuideLog;
import com.hezy.guide.phone.event.PagerSetUserinfo;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.receiver.PhoneReceiver;
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

import rx.Subscription;
import rx.functions.Action1;

import static com.hezy.guide.phone.R.id.mTvState;


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

    private PhoneReceiver phoneReceiver;

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isNewActivity = true;
        }
        super.onCreate(savedInstanceState);

//        phoneReceiver = new PhoneReceiver();
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction("android.intent.action.PHONE_STATE");
//        intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
//        registerReceiver(phoneReceiver, intentFilter);

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
        LogUtils.d(TAG, "mIntentType " + mIntentType);
        if (mIntentType == LoginHelper.LOGIN_TYPE_EXIT) {
            //退出应用
            LogUtils.d(TAG, "退出应用");
            quit();
        } else if (mIntentType == LoginHelper.LOGIN_TYPE_LOGOUT) {
            //退出登录
            LogUtils.d(TAG, "退出登录");
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
        //登录才能进入主页,启动心跳
//        HeartService.actionStart(mContext);
        WSService.actionStart(mContext);

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserStateEvent) {
                    setState(WSService.isOnline());
                }
            }
        });


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


        mBinding.mRgHome.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId) {
                    case R.id.mRbLog:
                        mBinding.mVerticalViewPager.setCurrentItem(0);
                        break;
                    case R.id.mRbMe:
                        mBinding.mVerticalViewPager.setCurrentItem(1);
                        break;
                }
            }
        });


        mBinding.mVerticalViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        mBinding.mRbLog.setChecked(true);
                        break;
                    case 1:
                        mBinding.mRbMe.setChecked(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    @Override
    protected void initListener() {
        mBinding.mTvState.setOnClickListener(this);
    }

    @Override
    protected void requestData() {
        versionCheck();
        registerDevice();
        requestUser();
    }

    private void initCurrentItem() {
        if (!TextUtils.isEmpty(Preferences.getUserName()) && !TextUtils.isEmpty(Preferences.getUserMobile())
                && !TextUtils.isEmpty(Preferences.getUserPhoto())
                && !TextUtils.isEmpty(Preferences.getUserAddress())) {
            //手机非空,照片非空,默认进入日志页面
            LogUtils.i(TAG, "手机非空,默认进入日志页面");
            mBinding.mVerticalViewPager.setCurrentItem(1);
        }
    }

    @Override
    protected void initAdapter() {
        mHomePagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        mFragments = new ArrayList<>();
        mFragments.add(GuideLogFragment.newInstance());
        mFragments.add(UserinfoFragment.newInstance());
        mHomePagerAdapter.setData(mFragments);
        mBinding.mVerticalViewPager.setAdapter(mHomePagerAdapter);
        LogUtils.i(TAG, "getUserMobile" + Preferences.getUserMobile());
//        initCurrentItem();
    }


    @Override
    protected void checkNetWorkOnClick(View v) {
        switch (v.getId()) {
            case mTvState:
                new ActionSheetDialog(mContext).builder()//
                        .setCancelable(false)//
                        .setCanceledOnTouchOutside(false)//
                        .addSheetItem("在线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if (TextUtils.isEmpty(Preferences.getUserMobile()) || TextUtils.isEmpty(Preferences.getUserPhoto())
                                                || TextUtils.isEmpty(Preferences.getUserName()) || TextUtils.isEmpty(Preferences.getUserAddress())) {
                                            showToast("请先填写姓名,电话,地址,照片");
                                            return;
                                        }
                                        if (!WSService.isOnline()) {
                                            //当前状态离线,可切换在线
                                            Log.i(TAG, "当前状态离线,可切换在线");
                                            RxBus.sendMessage(new SetUserStateEvent(true));
                                        }


                                    }
                                })
                        .addSheetItem("离线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if (WSService.isOnline()) {
                                            //当前状态在线,可切换离线
                                            Log.i(TAG, "当前状态在线,可切换离线");
                                            RxBus.sendMessage(new SetUserStateEvent(false));
//                                            WSService.SOCKET_ONLINE =false;
//                                            setState(false);
                                        }
                                    }
                                }).show();
                break;

        }
    }

    private void setState(boolean isOnline) {
        if (isOnline) {
            mBinding.mTvState.setText("在线");
            mBinding.mTvState.setCompoundDrawablesWithIntrinsicBounds(0,R.mipmap.ic_online,0,0);
            mBinding.mTvState.setTextColor(getResources().getColor(R.color.text_yellow_fff000));
        } else {
            mBinding.mTvState.setText("离线");
            mBinding.mTvState.setCompoundDrawablesWithIntrinsicBounds(0,R.mipmap.ic_offline,0,0);
            mBinding.mTvState.setTextColor(getResources().getColor(R.color.text_gray_c784fb));
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

    private void requestUser() {
        ApiClient.getInstance().requestUser(this, new OkHttpBaseCallback<BaseBean<UserData>>() {
            @Override
            public void onSuccess(BaseBean<UserData> entity) {
                if (entity == null || entity.getData() == null || entity.getData().getUser() == null) {
                    showToast("数据为空");
                    return;
                }
                User user = entity.getData().getUser();
                Wechat wechat = entity.getData().getWechat();
                LoginHelper.savaUser(user);
                initCurrentItem();
                if (wechat != null) {
                    LoginHelper.savaWeChat(wechat);
                }
                RxBus.sendMessage(new UserUpdateEvent());

            }
        });
    }

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
//        unregisterReceiver(phoneReceiver);
        super.onDestroy();
    }
}
