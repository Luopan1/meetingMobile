package com.hezy.guide.phone.wxapi;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.HomeActivity;
import com.hezy.guide.phone.business.PhoneRegisterActivity;
import com.hezy.guide.phone.business.UserInfoActivity;
import com.hezy.guide.phone.entities.FinishWX;
import com.hezy.guide.phone.entities.LoginWechat;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.UserData;
import com.hezy.guide.phone.entities.Version;
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.DeviceUtil;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONObject;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by wufan on 2017/7/14.
 */

public class WXEntryActivity extends FragmentActivity implements IWXAPIEventHandler, EasyPermissions.PermissionCallbacks {

    private ImageView wchatLoginImage;
    private IWXAPI mWxApi;

    private Subscription subscription;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int i, @NonNull List<String> list) {
        initView();
    }

    @Override
    public void onPermissionsDenied(int i, @NonNull List<String> list) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, list)) {
            new AppSettingsDialog.Builder(this)
                    .setTitle("权限不足")
                    .setRationale("请授予必须的权限，否则应用无法正常运行")
                    .build().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
//            Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show();
        }
    }

    private String[] perms = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        if (EasyPermissions.hasPermissions(this, perms)) {
            initView();
        } else {
            EasyPermissions.requestPermissions(this, "请授予必要的权限", 0, perms);
        }

    }

    protected void initView() {
        wchatLoginImage = findViewById(R.id.wchat_login);
        wchatLoginImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wchatLogin();
            }
        });

//        Preferences.setToken("90dd5c25795d4436ae3cb68cf844e3f2");

        if (Preferences.isLogin()) {
            ApiClient.getInstance().requestUser(this, new OkHttpCallback<BaseBean<UserData>>() {
                @Override
                public void onSuccess(BaseBean<UserData> entity) {
                    if (entity == null || entity.getData() == null || entity.getData().getUser() == null) {
                        Toast.makeText(WXEntryActivity.this, "用户数据为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Wechat wechat = entity.getData().getWechat();
                    if (wechat != null) {
                        LoginHelper.savaWeChat(wechat);
                    }

                    //验证用户是否存在
                    User user = entity.getData().getUser();
                    if (!TextUtils.isEmpty(user.getMobile())) {
                        //用户手机不为空，持久化用户数据，直接进入主程序
                        LoginHelper.savaUser(user);
                        startActivity(new Intent(WXEntryActivity.this, HomeActivity.class));
                    } else {
                        //用户手机为空，进入手机注册页面
                        startActivity(new Intent(WXEntryActivity.this, PhoneRegisterActivity.class));
                    }
                    finish();
                }
            });
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
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OkHttpCallback registerDeviceCb = new OkHttpCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {

        }

        @Override
        public void onFinish() {
            versionCheck();
        }
    };

    private void versionCheck() {
        ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
            @Override
            public void onSuccess(BaseBean<Version> entity) {
                Version version = entity.getData();
                if (version.getImportance() != 1) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri content_url = Uri.parse(version.getUrl());
                    intent.setData(content_url);
                    startActivity(Intent.createChooser(intent, "请选择浏览器"));
                }
            }

        });
    }

    private void reToWx() {
        String app_id = BuildConfig.WEIXIN_APP_ID;
        mWxApi = WXAPIFactory.createWXAPI(this.getApplicationContext(), app_id, false);
        mWxApi.registerApp(app_id);
    }

    public void wchatLogin() {
        if (mWxApi != null && !mWxApi.isWXAppInstalled()) {
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
    }

    @Override
    public void onReq(BaseReq baseReq) {

    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    //app发送消息给微信，处理返回消息的回调
    @Override
    public void onResp(BaseResp baseResp) {
        String result;
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                result = "登录成功";
                if (baseResp.getType() == 1) {
                    ZYAgent.onEvent(WXEntryActivity.this, "微信按钮 登录成功");
                    final SendAuth.Resp sendResp = ((SendAuth.Resp) baseResp);
                    String code = sendResp.code;
                    requestWechatLogin(sendResp.code, sendResp.state);
                    ZYAgent.onEvent(WXEntryActivity.this, "请求微信登录");
                }
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                ZYAgent.onEvent(WXEntryActivity.this, "微信按钮 用户拒绝授权");
                result = "用户拒绝授权";
                cancelDialog();
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                ZYAgent.onEvent(WXEntryActivity.this, "微信按钮 用户取消");
                result = "用户取消";
                cancelDialog();
                break;
            default:
                ZYAgent.onEvent(WXEntryActivity.this, "微信按钮 失败");
                result = "失败";
                cancelDialog();
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
                ZYAgent.onEvent(WXEntryActivity.this, "请求微信登录回调 成功");
                if (entity.getData() == null) {
                    return;
                }
                LoginWechat loginWechat = entity.getData();
                Wechat wechat = loginWechat.getWechat();
                Preferences.setWeiXinHead(wechat.getHeadimgurl());
                User user = loginWechat.getUser();
                if (loginWechat.getUser() == null) {
                } else {
                    LoginHelper.savaUser(user);

                    if (Preferences.isUserinfoEmpty()) {
                        startActivity(new Intent(WXEntryActivity.this, PhoneRegisterActivity.class));
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
            }
        });
    }

    private ProgressDialog progressDialog;

    protected void showDialog(String message) {
        if (progressDialog == null) {
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
