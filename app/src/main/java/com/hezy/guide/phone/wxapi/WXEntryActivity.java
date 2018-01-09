package com.hezy.guide.phone.wxapi;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.BasicActivity;
import com.hezy.guide.phone.business.HomeActivity;
import com.hezy.guide.phone.business.UserinfoActivity;
import com.hezy.guide.phone.entities.FinishWX;
import com.hezy.guide.phone.entities.LoginWechat;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.UserData;
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.DeviceUtil;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.UUIDUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONObject;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by wufan on 2017/7/14.
 */

public class WXEntryActivity extends BasicActivity implements IWXAPIEventHandler {

    private ImageView wchatLoginImage;
    private IWXAPI mWxApi;

    private Subscription subscription;

    @Override
    public String getStatisticsTag() {
        return "微信登录页";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        initView();

//        Preferences.setToken("4f8752dcd9e34a1b876aae2ecfd20712");

        if (Preferences.isLogin()) {
            apiClient.requestUser(this, new OkHttpCallback<BaseBean<UserData>>() {
                @Override
                public void onSuccess(BaseBean<UserData> entity) {
                    if (entity == null || entity.getData() == null || entity.getData().getUser() == null) {
                        showToast("获取用户数据为空");
                        return;
                    }
                    User user = entity.getData().getUser();
                    LoginHelper.savaUser(user);

                    Wechat wechat = entity.getData().getWechat();
                    if (wechat != null) {
                        LoginHelper.savaWeChat(wechat);
                    }

                    if (Preferences.isUserinfoEmpty()) {
                        UserinfoActivity.actionStart(mContext, true);
                        return;
                    }
                    startActivity(new Intent(WXEntryActivity.this, HomeActivity.class));
                    finish();

                }
            });
            return;
        }

        reToWx();

        mWxApi.handleIntent(getIntent(), this);

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof FinishWX) {
                    //会打开两个微信界面
                    finish();
                }
            }
        });

        registerDevice();
    }

    protected void initView() {
        wchatLoginImage = findViewById(R.id.wchat_login);
        wchatLoginImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wchatLogin();
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
            apiClient.deviceRegister(this, jsonObject.toString(), registerDeviceCb);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OkHttpCallback registerDeviceCb = new OkHttpCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {
            Log.d(TAG, "registerDevice 成功===");
        }
    };

    private void reToWx() {
        String app_id = BuildConfig.WEIXIN_APP_ID;
        mWxApi = WXAPIFactory.createWXAPI(this.getApplicationContext(), app_id, false);
        mWxApi.registerApp(app_id);
    }

    public void wchatLogin() {
        if (!mWxApi.isWXAppInstalled()) {
            ToastUtils.showToast("您还未安装微信客户端，请先安装");
            return;
        }
//        if (isWxLoging) {
//            Log.i(TAG, "isWxLoging == true return");
//            ZYAgent.onEvent(mContext,"微信按钮 重复点击返回");
//            return;
//        }
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "GuideMobile_wx_login";
        mWxApi.sendReq(req);
        showDialog("正在加载...");
        Log.i(TAG, "wchatLogin() isWxLoging = true ");
    }

    @Override
    public void onReq(BaseReq baseReq) {

    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    //app发送消息给微信，处理返回消息的回调
    @Override
    public void onResp(BaseResp baseResp) {
        Log.i(TAG, "baseResp errStr " + baseResp.errStr);
        Log.i(TAG, "baseResp transaction " + baseResp.transaction);
        Log.i(TAG, "baseResp openId " + baseResp.openId);
        String result;
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                result = "登录成功";
                if (baseResp.getType() == 1) {
                    ZYAgent.onEvent(mContext,"微信按钮 登录成功");
                    final SendAuth.Resp sendResp = ((SendAuth.Resp) baseResp);
                    String code = sendResp.code;
                    Log.i(TAG, "sendResp.code " + code);
                    Log.i(TAG, "sendResp.state " + sendResp.state);
                    Log.i(TAG, "sendResp.lang " + sendResp.lang);
                    Log.i(TAG, "sendResp.country " + sendResp.country);
                    requestWechatLogin(sendResp.code, sendResp.state);
                    ZYAgent.onEvent(mContext,"请求微信登录");
                }
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                ZYAgent.onEvent(mContext,"微信按钮 用户拒绝授权");
                result = "用户拒绝授权";
                cancelDialog();
                Log.i(TAG, "用户拒绝授权 isWxLoging = false ");
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                ZYAgent.onEvent(mContext,"微信按钮 用户取消");
                result = "用户取消";
                cancelDialog();
                Log.i(TAG, "用户取消 isWxLoging = false ");
                break;
            default:
                ZYAgent.onEvent(mContext,"微信按钮 失败");
                result = "失败";
                cancelDialog();
                Log.i(TAG, "失败 isWxLoging = false ");
                break;
        }

        if (!TextUtils.isEmpty(result) && baseResp.errCode != BaseResp.ErrCode.ERR_OK) {
            Toast.makeText(this, baseResp.errCode + result, Toast.LENGTH_SHORT).show();
        }
    }


    private void requestWechatLogin(String code, String state) {
        ApiClient.getInstance().requestWechat(code, state, this, new OkHttpCallback<BaseBean<LoginWechat>>() {

            @Override
            public void onSuccess(BaseBean<LoginWechat> entity) {
                ZYAgent.onEvent(mContext,"请求微信登录回调 成功");
                if (entity.getData() == null) {
                    Log.i(TAG, "entity.getData() == null");
                    showToast("没有数据");
                    return;
                }
                LoginWechat loginWechat = entity.getData();
                Wechat wechat = loginWechat.getWechat();
                Preferences.setWeiXinHead(wechat.getHeadimgurl());
                User user = loginWechat.getUser();
                if (loginWechat.getUser() == null) {
                    //没有获取到用户
                    Log.i(TAG, "没有用户数据");
                    showToast("没有用户数据");
                } else {
                    //保存用户,进入主页
                    Log.i(TAG, "用户登录成功");
//                    showToast("用户登录成功");
                    LoginHelper.savaUser(user);
                    if (Preferences.isUserinfoEmpty()) {
//                        showToast("请先填写姓名,电话,地址,照片");
                        UserinfoActivity.actionStart(mContext, true);
                        return;
                    } else {
                        startActivity(new Intent(WXEntryActivity.this, HomeActivity.class));
                    }
                    RxBus.sendMessage(new FinishWX());
                    finish();
                }


            }

            @Override
            public void onFinish() {
                cancelDialog();
                Log.i(TAG, "requestWechatLogin onFinish()  isWxLoging = false ");
            }
        });
    }


    protected void showDialog(String message) {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(message);
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    protected void cancelDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }


    @Override
    public void onDestroy() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        if (mWxApi != null) {
            mWxApi.detach();
        }
        cancelDialog();
        super.onDestroy();
    }

}
