package com.hezy.guide.phone.business;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.adorkable.iosdialog.ActionSheetDialog;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.Version;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.receiver.PhoneReceiver;
import com.hezy.guide.phone.service.WSService;
import com.hezy.guide.phone.utils.DeviceUtil;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.statistics.ZYAgent;
import com.hezy.guide.phone.wxapi.WXEntryActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import rx.Subscription;
import rx.functions.Action1;


/**
 * 主页
 * Created by wufan on 2017/7/24.
 */

public class HomeActivity extends BasicActivity implements View.OnClickListener {

    private ViewPager viewPager;
    private LinearLayout bottomLayout;
    private TextView stateText;
    private RadioGroup radioGroup;
    private RadioButton recordRadio, meetingRadio, profileRadio;

    private HomePagerAdapter mHomePagerAdapter;

    private ArrayList<Fragment> mFragments;

    private int mIntentType;

    private Subscription subscription;

    private PhoneReceiver phoneReceiver;

    @Override
    public String getStatisticsTag() {
        return "主页";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);

        WSService.actionStart(mContext);

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserStateEvent) {
                    setState(WSService.isOnline());
                }
            }
        });

        initView();

//        versionCheck();

        registerDevice();

    }

    private void initView(){
        viewPager = findViewById(R.id.view_pager);
        bottomLayout = findViewById(R.id.bottom_layout);
        stateText = findViewById(R.id.state);
        radioGroup = findViewById(R.id.radio_group);
        recordRadio = findViewById(R.id.record_radio);
        meetingRadio = findViewById(R.id.meeting_radio);
        profileRadio = findViewById(R.id.profile_radio);

        stateText.setOnClickListener(this);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.record_radio:
                        viewPager.setCurrentItem(0);
                        break;
                    case R.id.meeting_radio:
                        viewPager.setCurrentItem(1);
                        break;
                    case R.id.profile_radio:
                        viewPager.setCurrentItem(2);
                        break;
                }
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        recordRadio.setChecked(true);
                        break;
                    case 1:
                        meetingRadio.setChecked(true);
                        break;
                    case 2:
                        profileRadio.setChecked(true);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mHomePagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        mFragments = new ArrayList<>();
        mFragments.add(RecordFragment.newInstance());
        mFragments.add(MeetingsFragment.newInstance());
        mFragments.add(ProfileFragment.newInstance());
        mHomePagerAdapter.setData(mFragments);
        viewPager.setAdapter(mHomePagerAdapter);

        phoneReceiver = new PhoneReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        registerReceiver(phoneReceiver, intentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        setState(WSService.isOnline());
        if (!TextUtils.isEmpty(Preferences.getToken())) {
            ApiClient.getInstance().requestUser();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (TextUtils.isEmpty(Preferences.getToken())) {
            startActivity(new Intent(this, WXEntryActivity.class));
            finish();
        }
    }

    @Override
    protected void checkNetWorkOnClick(View v) {
        switch (v.getId()) {
            case R.id.state:
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
            stateText.setText("在线");
            stateText.setCompoundDrawablesWithIntrinsicBounds(0,R.mipmap.ic_online,0,0);
            stateText.setTextColor(getResources().getColor(R.color.text_yellow_fff000));
        } else {
            stateText.setText("离线");
            stateText.setCompoundDrawablesWithIntrinsicBounds(0,R.mipmap.ic_offline,0,0);
            stateText.setTextColor(getResources().getColor(R.color.text_gray_c784fb));
        }
    }


    private void versionCheck() {
        ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
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
        String uuid = Installation.id(this);
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
                msg.append("apiClient.deviceRegister call");
            } catch (JSONException e) {
                e.printStackTrace();
                msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
            } catch (SecurityException e) {
                e.printStackTrace();
                msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
            }

        }
    }

    private OkHttpCallback registerDeviceCb = new OkHttpCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {
            Log.d(TAG, "registerDevice 成功===");
        }
    };



    private long mExitTime = 0;
    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - mExitTime) > 2000) {
            Toast.makeText(this, "再按一次退出在线导购", Toast.LENGTH_SHORT).show();
            mExitTime = System.currentTimeMillis();
        } else {
            quit();
        }
    }

    private void quit() {
        ZYAgent.onEvent(getApplicationContext(),"返回退出应用 连接服务 请求停止");
        WSService.stopService(this);
        finish();
        System.exit(0);
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(phoneReceiver);

        subscription.unsubscribe();
        super.onDestroy();
    }
}
