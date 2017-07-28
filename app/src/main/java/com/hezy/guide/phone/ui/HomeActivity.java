package com.hezy.guide.phone.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.HomeActivityBinding;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.service.HeartService;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.UUIDUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import me.kaelaela.verticalviewpager.transforms.DefaultTransformer;


/**主页
 * Created by wufan on 2017/7/24.
 */

public class HomeActivity extends BaseDataBindingActivity<HomeActivityBinding>{
    private HomePagerAdapter mHomePagerAdapter;
    private ArrayList<Fragment> mFragments;


    public static void actionStart(Context  context) {
         Intent intent = new Intent(context, HomeActivity.class);
         context.startActivity(intent);
     }

    @Override
    protected int initContentView() {
        return R.layout.home_activity;
    }

    @Override
    protected void initView() {
//        mBinding.mVerticalViewPager.setPageTransformer(true, new VerticalTransformer());
//        mBinding.mVerticalViewPager.setOverScrollMode(OVER_SCROLL_NEVER);
        //登录才能进入主页,启动心跳
        HeartService.actionStart(mContext);


        mBinding.mVerticalViewPager.setPageTransformer(false, new DefaultTransformer());



    }

    @Override
    protected void requestData() {
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
                ApiClient.getInstance().deviceRegister(this,params.toString(),registerDeviceCb);
                msg.append("client.deviceRegister call");
            } catch (JSONException e) {
                e.printStackTrace();
                msg.append("registerDevice error jsonObject.put e.getMessage() = "+e.getMessage());
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



}
