package com.hezy.guide.phone.business;

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
import com.hezy.guide.phone.databinding.HomeActivityBinding;
import com.hezy.guide.phone.entities.Version;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.service.WSService;
import com.hezy.guide.phone.utils.DeviceUtil;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.UUIDUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;
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

    @Override
    public String getStatisticsTag() {
        return "主页";
    }

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, HomeActivity.class);
        context.startActivity(intent);
    }

    public static void actionStart(Context context, boolean isLogin) {
        Intent intent = new Intent(context, HomeActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isNewActivity = true;
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
        Logger.d(TAG, "mIntentType " + mIntentType);
        if (mIntentType == LoginHelper.LOGIN_TYPE_EXIT) {
            //退出应用
            Logger.d(TAG, "退出应用");
            quit();
        } else if (mIntentType == LoginHelper.LOGIN_TYPE_LOGOUT) {
            //退出登录
            Logger.d(TAG, "退出登录");
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


//        subscription = RxBus.handleMessage(new Action1() {
//            @Override
//            public void call(Object o) {
//                if (o instanceof PagerSetUserinfo) {
//                    mBinding.mVerticalViewPager.setCurrentItem(0);
//                } else if (o instanceof PagerSetGuideLog) {
//                    mBinding.mVerticalViewPager.setCurrentItem(1);
//                }
//            }
//        });


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
    protected void onResume() {
        super.onResume();
        setState(WSService.isOnline());
        ApiClient.getInstance().requestUser();
    }

    @Override
    protected void requestData() {
        versionCheck();
        registerDevice();
//        requestUser();
    }

//    private void initCurrentItem() {
//        if (!TextUtils.isEmpty(Preferences.getUserName()) && !TextUtils.isEmpty(Preferences.getUserMobile())
//                && !TextUtils.isEmpty(Preferences.getUserPhoto())
//                && !TextUtils.isEmpty(Preferences.getUserAddress())) {
//            //手机非空,照片非空,默认进入日志页面
//            LogUtils.i(TAG, "手机非空,默认进入日志页面");
//            mBinding.mVerticalViewPager.setCurrentItem(1);
//        }
//    }

    @Override
    protected void initAdapter() {
        mHomePagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        mFragments = new ArrayList<>();
        mFragments.add(GuideLogFragment.newInstance());
        mFragments.add(MeFragment.newInstance());
        mHomePagerAdapter.setData(mFragments);
        mBinding.mVerticalViewPager.setAdapter(mHomePagerAdapter);
        Logger.i(TAG, "getUserMobile" + Preferences.getUserMobile());
//        initCurrentItem();
    }


    @Override
    protected void checkNetWorkOnClick(View v) {
        switch (v.getId()) {
            case mTvState:
                if (Preferences.isUserinfoEmpty()) {
                    showToast("请先填写姓名,电话,地址,照片");
                    UserinfoActivity.actionStart(this);
                    return;
                }
                new ActionSheetDialog(mContext).builder()//
                        .setCancelable(false)//
                        .setCanceledOnTouchOutside(false)//
                        .addSheetItem("在线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        ZYAgent.onEvent(mContext,"在线按钮");

                                        if (!WSService.isOnline()) {
                                            //当前状态离线,可切换在线
                                            ZYAgent.onEvent(mContext,"在线按钮,当前离线,切换到在线");
                                            Log.i(TAG, "当前状态离线,可切换在线");
                                            RxBus.sendMessage(new SetUserStateEvent(true));
                                        }else{
                                            ZYAgent.onEvent(mContext,"在线按钮,当前在线,,无效操作");
                                        }


                                    }
                                })
                        .addSheetItem("离线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        ZYAgent.onEvent(mContext,"离线按钮");
                                        if (WSService.isOnline()) {
                                            //当前状态在线,可切换离线
                                            Log.i(TAG, "当前状态在线,可切换离线");
                                            ZYAgent.onEvent(mContext,"离线按钮,当前在线,切换到离线");
                                            RxBus.sendMessage(new SetUserStateEvent(false));
//                                            WSService.SOCKET_ONLINE =false;
//                                            setState(false);
                                        }else{
                                            ZYAgent.onEvent(mContext,"离线按钮,当前离线,无效操作");
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
                    startActivity(new Intent(mContext, UpdateActivity.class).putExtra("version", version));
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
            Logger.e(TAG, msg.toString());
        } else {

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uuid", uuid);
                jsonObject.put("androidId", TextUtils.isEmpty(Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID)) ? "" : Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                jsonObject.put("manufacturer", Build.MANUFACTURER);
                jsonObject.put("name", Build.BRAND);
                jsonObject.put("model", Build.MODEL);
                jsonObject.put("sdkVersion", Build.VERSION.SDK_INT);
                jsonObject.put("screenDensity", "width:" + width + ",height:" + height + ",density:" + density + ",densityDpi:" + densityDpi);
                jsonObject.put("display", Build.DISPLAY);
                jsonObject.put("finger", Build.FINGERPRINT);
                jsonObject.put("appVersion", BuildConfig.FLAVOR + "_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE);
                jsonObject.put("cpuSerial", Installation.getCPUSerial());
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                jsonObject.put("androidDeviceId", tm != null ? tm.getDeviceId() : "");
                jsonObject.put("buildSerial", Build.SERIAL);
                jsonObject.put("source", 2);
                jsonObject.put("internalSpace", DeviceUtil.getDeviceTotalMemory(this));
                jsonObject.put("internalFreeSpace", DeviceUtil.getDeviceAvailMemory(this));
                jsonObject.put("sdSpace", DeviceUtil.getDeviceTotalInternalStorage());
                jsonObject.put("sdFreeSpace", DeviceUtil.getDeviceAvailInternalStorage());
                ApiClient.getInstance().deviceRegister(this, jsonObject.toString(), registerDeviceCb);
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
        ZYAgent.onEvent(getApplicationContext(),"返回退出应用");
        ZYAgent.onEvent(getApplicationContext(),"返回退出应用 连接服务 请求停止");
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
