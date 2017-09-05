package com.hezy.guide.phone.wxapi;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.LoginActivityBinding;
import com.hezy.guide.phone.entities.FinishWX;
import com.hezy.guide.phone.entities.LoginWechat;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.UserData;
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.ui.HomeActivity;
import com.hezy.guide.phone.ui.UserinfoActivity;
import com.hezy.guide.phone.utils.DeviceUtil;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.Logger;
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

import org.json.JSONException;
import org.json.JSONObject;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by wufan on 2017/7/14.
 */

public class WXEntryActivity extends BaseDataBindingActivity<LoginActivityBinding> implements IWXAPIEventHandler {
    public static final String TAG = "WXEntryActivity";
    private IWXAPI mWxApi;
    private int mIntentType;
    private boolean isNewActivity;
    private static final int RETURN_MSG_TYPE_LOGIN = 1;
    private static final int RETURN_MSG_TYPE_SHARE = 2;
    /**
     * 微信登录中点击返回
     */
    private boolean isWxLoging;

    private static boolean isFirst = true;
    private Dialog dialog;

    @Override
    public String getStatisticsTag() {
        return "登录";
    }

    @Override
    protected int initContentView() {
        return R.layout.login_activity;
    }

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, WXEntryActivity.class);
        context.startActivity(intent);
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
//            quit();
        } else if (mIntentType == LoginHelper.LOGIN_TYPE_LOGOUT) {
            //退出登录
            Logger.d(TAG, "退出登录");
            //判断是否是强制退出,强制退出在登录页有弹窗提醒
            boolean isUserLogout = intent.getBooleanExtra("IS_USER_LOGOUT",false);
            if(!isUserLogout){
                showLogoutForceDialog();
            }
        }

    }


    /**
     * 强制退出dialog
     */
    private void showLogoutForceDialog() {
//        if (dialog == null) {
//            View view = View.inflate(getApplicationContext(), R.layout.dialog_common_ok, null);
//            TextView title = (TextView) view.findViewById(R.id.title);
//            title.setText(getString(R.string.login_invalid));
//            view.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dialog.cancel();
//                }
//            });
//            dialog = new Dialog(this, R.style.MyDialog);
//            dialog.setContentView(view);
//        }
//        dialog.show();

    }

    @Override
    protected void initView() {

        if (BuildConfig.IS_LOGIN && isFirst) {
            Logger.i(TAG, "直接登录 BuildConfig.LOGIN_TOKEN " + BuildConfig.LOGIN_TOKEN);
            Preferences.setToken(BuildConfig.LOGIN_TOKEN);
        }
        isFirst = false;

        if (Preferences.isLogin()) {
            //TODO 需要请求用户信息
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
                    if (wechat != null) {
                        LoginHelper.savaWeChat(wechat);
                    }

                    if (Preferences.isUserinfoEmpty()) {
                        UserinfoActivity.actionStart(mContext, true);
                        return;
                    }
                    HomeActivity.actionStart(mContext);
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
            ApiClient.getInstance().deviceRegister(this, jsonObject.toString(), registerDeviceCb);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private OkHttpBaseCallback registerDeviceCb = new OkHttpBaseCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {
            Log.d(TAG, "registerDevice 成功===");
        }
    };

    @Override
    protected void initListener() {
        mBinding.mIvWeChat.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZYAgent.onPageStart(mContext, "登录");
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZYAgent.onPageEnd(mContext, "登录");
    }

    private void reToWx() {
        String app_id = BuildConfig.WEIXIN_APP_ID;
        Log.i(TAG, "Login BuildConfig.WEIXIN_APP_ID " + app_id);
        //AppConst.WEIXIN.APP_ID是指你应用在微信开放平台上的AppID，记得替换。
        mWxApi = WXAPIFactory.createWXAPI(this.getApplicationContext(), app_id, false);
        // 将该app注册到微信
        mWxApi.registerApp(app_id);
    }

    public void wxLogin() {
        if (!mWxApi.isWXAppInstalled()) {
            ToastUtils.showToast("您还未安装微信客户端");
            ZYAgent.onEvent(mContext,"微信按钮 您还未安装微信客户端");
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
        isWxLoging = true;
        showDialog("正在加载...");
        Log.i(TAG, "wxLogin() isWxLoging = true ");
    }

    @Override
    protected void checkNetWorkOnClick(View v) {
        switch (v.getId()) {
            case R.id.mIvWeChat:
                ZYAgent.onEvent(mContext,"微信按钮");
                wxLogin();
//                requestWechatLogin("081U5X7c2qUcTQ0LUX8c2adM7c2U5X7M","GuideMobile_wx_login");
                break;

        }
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
                switch (baseResp.getType()) {
                    case RETURN_MSG_TYPE_LOGIN:
                        ZYAgent.onEvent(mContext,"微信按钮 登录成功");
                        final SendAuth.Resp sendResp = ((SendAuth.Resp) baseResp);
                        String code = sendResp.code;
                        Log.i(TAG, "sendResp.code " + code);
                        Log.i(TAG, "sendResp.state " + sendResp.state);
                        Log.i(TAG, "sendResp.lang " + sendResp.lang);
                        Log.i(TAG, "sendResp.country " + sendResp.country);
                        requestWechatLogin(sendResp.code, sendResp.state);
                        ZYAgent.onEvent(mContext,"请求微信登录");
                        break;
//                    case RETURN_MSG_TYPE_SHARE:
//                        UIUtils.showToast("微信分享成功");
//                        finish();
//                        break;
                }
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                ZYAgent.onEvent(mContext,"微信按钮 用户拒绝授权");
                result = "用户拒绝授权";
                isWxLoging = false;
                cancelDialog();
                Log.i(TAG, "用户拒绝授权 isWxLoging = false ");
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                ZYAgent.onEvent(mContext,"微信按钮 用户取消");
                result = "用户取消";
                isWxLoging = false;
                cancelDialog();
                Log.i(TAG, "用户取消 isWxLoging = false ");
                break;
            default:
                ZYAgent.onEvent(mContext,"微信按钮 失败");
                result = "失败";
                isWxLoging = false;
                cancelDialog();
                Log.i(TAG, "失败 isWxLoging = false ");
                break;
        }

        if (!TextUtils.isEmpty(result) && baseResp.errCode != BaseResp.ErrCode.ERR_OK) {
            Toast.makeText(this, baseResp.errCode + result, Toast.LENGTH_SHORT).show();
        }
    }


    private void requestWechatLogin(String code, String state) {
        ApiClient.getInstance().requestWechat(code, state, this, new OkHttpBaseCallback<BaseBean<LoginWechat>>() {

            @Override
            public void onSuccess(BaseBean<LoginWechat> entity) {
                ZYAgent.onEvent(mContext,"请求微信登录回调 成功");
                if (entity.getData() == null) {
                    Log.i(WXEntryActivity.TAG, "entity.getData() == null");
                    showToast("没有数据");
                    return;
                }
                LoginWechat loginWechat = entity.getData();
                Wechat wechat = loginWechat.getWechat();
                Preferences.setWeiXinHead(wechat.getHeadimgurl());
                User user = loginWechat.getUser();
                if (loginWechat.getUser() == null) {
                    //没有获取到用户
                    Log.i(WXEntryActivity.TAG, "没有用户数据");
                    showToast("没有用户数据");
                } else {
                    //保存用户,进入主页
                    Log.i(WXEntryActivity.TAG, "用户登录成功");
//                    showToast("用户登录成功");
                    LoginHelper.savaUser(user);
                    if (Preferences.isUserinfoEmpty()) {
//                        showToast("请先填写姓名,电话,地址,照片");
                        UserinfoActivity.actionStart(mContext, true);
                        return;
                    } else {
                        HomeActivity.actionStart(mContext);
                    }
                    RxBus.sendMessage(new FinishWX());
                    finish();
                }


            }

            @Override
            public void onErrorAll(Exception e) {
                super.onErrorAll(e);
                ZYAgent.onEvent(mContext,"请求微信登录回调 失败");
            }

            @Override
            public void onFinish() {
                isWxLoging = false;
                cancelDialog();
                Log.i(WXEntryActivity.TAG, "requestWechatLogin onFinish()  isWxLoging = false ");
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


    private Subscription subscription;

    @Override
    public void onDestroy() {
        if (subscription != null) {
            subscription.unsubscribe();
        }
        if (mWxApi != null) {
            mWxApi.detach();
        }
        if(dialog != null){
            dialog.cancel();
        }
        cancelDialog();
        super.onDestroy();
    }

}
