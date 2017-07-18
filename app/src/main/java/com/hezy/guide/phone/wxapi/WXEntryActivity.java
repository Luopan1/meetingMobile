package com.hezy.guide.phone.wxapi;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.LoginActivityBinding;
import com.hezy.guide.phone.utils.ToastUtils;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

/**
 * Created by wufan on 2017/7/14.
 */

public class WXEntryActivity extends BaseDataBindingActivity<LoginActivityBinding>  implements IWXAPIEventHandler {
    private IWXAPI mWxApi;

    @Override
    protected int initContentView() {
        return R.layout.login_activity;
    }

    @Override
    protected void initView() {
        reToWx();
        mWxApi.handleIntent(getIntent(), this);
    }

    @Override
    protected void initListener() {
        mBinding.mIvWeChat.setOnClickListener(this);
    }

    private void reToWx(){
        String app_id = BuildConfig.WEIXIN_APP_ID;
        Log.i(TAG,"Login BuildConfig.WEIXIN_APP_ID "+app_id);
        //AppConst.WEIXIN.APP_ID是指你应用在微信开放平台上的AppID，记得替换。
        mWxApi = WXAPIFactory.createWXAPI(this, app_id, false);
        // 将该app注册到微信
        mWxApi.registerApp(app_id);
    }

    public void wxLogin() {
        if (!mWxApi.isWXAppInstalled()) {
            ToastUtils.showToast("您还未安装微信客户端");
            return;
        }
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "GuideMobile_wx_login";
        mWxApi.sendReq(req);
    }

    @Override
    protected void checkNetWorkOnClick(View v) {
        switch (v.getId()) {
            case R.id.mIvWeChat:
                wxLogin();
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
        String result;
        switch (baseResp.errCode) {
            case BaseResp.ErrCode.ERR_OK:
                result = "登录成功";
                String code = ((SendAuth.Resp) baseResp).code;
                Log.i(TAG,"code = " + code);
                break;
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                result = "用户拒绝授权";
                break;
            case BaseResp.ErrCode.ERR_USER_CANCEL :
                result = "用户取消";
                break;
            default:
                result = "失败";
                break;
        }
        if (result != null) {
            Toast.makeText(this, baseResp.errCode + result, Toast.LENGTH_SHORT).show();
        }
    }
}
