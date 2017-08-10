package com.hezy.guide.phone.wxapi;

import android.content.Context;
import android.content.Intent;
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
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.ui.HomeActivity;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by wufan on 2017/7/14.
 */

public class WXEntryActivity extends BaseDataBindingActivity<LoginActivityBinding> implements IWXAPIEventHandler {
    public static final String TAG = "WXEntryActivity";
    private IWXAPI mWxApi;
    private static final int RETURN_MSG_TYPE_LOGIN = 1;
    private static final int RETURN_MSG_TYPE_SHARE = 2;
    /**
     * 微信登录中点击返回
     */
    private boolean isWxLoging;

    private static boolean isFirst=true;

    @Override
    protected int initContentView() {
        return R.layout.login_activity;
    }

    public static void actionStart(Context context) {
         Intent intent = new Intent(context, WXEntryActivity.class);
         context.startActivity(intent);
     }

    @Override
    protected void initView() {

        if(BuildConfig.IS_LOGIN && isFirst){
            LogUtils.i(TAG,"直接登录 BuildConfig.LOGIN_TOKEN "+BuildConfig.LOGIN_TOKEN);
            Preferences.setToken(BuildConfig.LOGIN_TOKEN);
        }
        isFirst = false;

        if (Preferences.isLogin()){
            HomeActivity.actionStart(mContext);
            finish();
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
    }

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
            return;
        }
        if(isWxLoging){
            Log.i(TAG,"isWxLoging == true return");
            return;
        }
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "GuideMobile_wx_login";
        mWxApi.sendReq(req);
        isWxLoging = true;
        Log.i(TAG,"wxLogin() isWxLoging = true ");
    }

    @Override
    protected void checkNetWorkOnClick(View v) {
        switch (v.getId()) {
            case R.id.mIvWeChat:
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
                        final SendAuth.Resp sendResp = ((SendAuth.Resp) baseResp);
                        String code = sendResp.code;
                        Log.i(TAG, "sendResp.code " + code);
                        Log.i(TAG, "sendResp.state " + sendResp.state);
                        Log.i(TAG, "sendResp.lang " + sendResp.lang);
                        Log.i(TAG, "sendResp.country " + sendResp.country);
                        requestWechatLogin(sendResp.code, sendResp.state);
                        break;
//                    case RETURN_MSG_TYPE_SHARE:
//                        UIUtils.showToast("微信分享成功");
//                        finish();
//                        break;
                }
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = "用户拒绝授权";
                isWxLoging=false;
                Log.i(TAG,"用户拒绝授权 isWxLoging = false ");
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                result = "用户取消";
                isWxLoging=false;
                Log.i(TAG,"用户取消 isWxLoging = false ");
                break;
            default:
                result = "失败";
                isWxLoging=false;
                Log.i(TAG,"失败 isWxLoging = false ");
                break;
        }
        if (result != null) {
            Toast.makeText(this, baseResp.errCode + result, Toast.LENGTH_SHORT).show();
        }
    }


    private void requestWechatLogin(String code, String state) {
        ApiClient.getInstance().requestWechat(code, state, this, new OkHttpBaseCallback<BaseBean<LoginWechat>>() {

            @Override
            public void onSuccess(BaseBean<LoginWechat> entity) {
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
                    showToast("用户登录成功");
                    LoginHelper.savaUser(user);
                    HomeActivity.actionStart(mContext);
                    RxBus.sendMessage(new FinishWX());
                    finish();
                }


            }

            @Override
            public void onFinish() {
                isWxLoging=false;
                Log.i(WXEntryActivity.TAG,"requestWechatLogin onFinish()  isWxLoging = false ");
            }
        });
    }

    private Subscription subscription;

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        mWxApi.detach();
        super.onDestroy();
    }

}
