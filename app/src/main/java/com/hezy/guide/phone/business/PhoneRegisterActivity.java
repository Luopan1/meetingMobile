package com.hezy.guide.phone.business;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;

import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.databinding.PhoneRegistLayoutBinding;
import com.hezy.guide.phone.entities.LoginWithPhoneNumber;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;

import java.util.HashMap;
import java.util.Map;

import rx.Subscription;
import rx.functions.Action1;


/**
 * 手机绑定页面
 * @author Dongce
 * create time: 2018/10/22
 */
public class PhoneRegisterActivity extends BaseDataBindingActivity<PhoneRegistLayoutBinding> {

    private Subscription subscription;
    private CountDownTimer countDownTimer;
    private final int SHOW_VERIFICATION_BTN = 1;
    private final int HIDE_VERIFICATION_BTN = 2;
    private final int SHOW_LOGIN_BTN = 3;
    private final int HIDE_LOGIN_BTN = 4;
    private NotificationHandler notificationHandler = new NotificationHandler();

    @Override
    public String getStatisticsTag() {
        return "手机号登陆";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected int initContentView() {
        return R.layout.phone_regist_layout;
    }

    @Override
    protected void initView() {
        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserUpdateEvent) {
                    setUserUI();
                }
            }
        });

        countDownTimer = new CountDownTimer(60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mBinding.btnPhoneRequestVerificationCode.setText(millisUntilFinished / 1000 + "S 后重新获取 ");
            }

            @Override
            public void onFinish() {
                notificationHandler.sendEmptyMessage(SHOW_VERIFICATION_BTN);
                mBinding.btnPhoneRequestVerificationCode.setText("获取验证码");
            }
        };

    }

    @Override
    protected void initListener() {
        mBinding.btnPhoneRequestVerificationCode.setOnClickListener(this);
        mBinding.btnPhoneLogin.setOnClickListener(this);
    }

    private void setUserUI() {
    }

    @Override
    protected void normalOnClick(View v) {

        final String phoneNumber = mBinding.edtPhoneNumber.getText().toString().trim();

        switch (v.getId()) {

            case R.id.btn_phone_request_verification_code:

                if (TextUtils.isEmpty(phoneNumber)) {
                    showToast("当前手机号不能为空");
                    return;
                }
                if (phoneNumber.equals(Preferences.getUserMobile())) {
                    showToast("当前手机号已设置成功");
                    return;
                }

                countDownTimer.start();
                notificationHandler.sendEmptyMessage(HIDE_VERIFICATION_BTN);

                apiClient.requestVerifyCode(this, phoneNumber, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {

                    }

                    @Override
                    public void onFailure(int errorCode, BaseException exception) {
                        super.onFailure(errorCode, exception);
                        String errorMsg = "获取验证码失败，错误码：" + errorCode + "，错误信息：" + exception.getMessage();
                        ZYAgent.onEvent(PhoneRegisterActivity.this, errorMsg);
                        countDownTimer.cancel();
                        notificationHandler.sendEmptyMessage(SHOW_VERIFICATION_BTN);
                        mBinding.btnPhoneRequestVerificationCode.setText("获取验证码");
                        ToastUtils.showToast(errorMsg);
                    }
                });

                break;

            case R.id.btn_phone_login:

                if (TextUtils.isEmpty(phoneNumber)) {
                    showToast("当前手机号不能为空");
                    return;
                }

                final String verificationCode = mBinding.edtPhoneVerificationCode.getText().toString().trim();
                if (TextUtils.isEmpty(verificationCode)) {
                    showToast("验证码不能为空");
                    return;
                }

                Map<String, String> params = new HashMap<>();
                params.put("mobile", phoneNumber);
                params.put("verifyCode", verificationCode);

                apiClient.requestLoginWithPhoneNumber(this, params, loginWithPhoneNumberCallback);
                break;
        }
    }

    private OkHttpCallback<BaseBean<LoginWithPhoneNumber>> loginWithPhoneNumberCallback = new OkHttpCallback<BaseBean<LoginWithPhoneNumber>>() {

        @Override
        public void onStart() {
            super.onStart();
            notificationHandler.sendEmptyMessage(HIDE_LOGIN_BTN);
        }

        @Override
        public void onSuccess(BaseBean<LoginWithPhoneNumber> entity) {

            if (entity.getData() == null) {
                return;
            }
            if (entity.getErrcode() != 0) {
                ToastUtils.showToast("用户登陆请求失败，错误码：" + entity.getErrcode());
                return;
            }
            LoginWithPhoneNumber loginWithPhoneNumber = entity.getData();
            User user = loginWithPhoneNumber.getUser();
            LoginHelper.savaUser(user);

            if (loginWithPhoneNumber.getAuthPass()) {
                //已经认证过，是海尔员工
                startActivity(new Intent(PhoneRegisterActivity.this, HomeActivity.class));
            } else {
                //未认证过，不是海尔员工
                UserInfoActivity.actionStart(PhoneRegisterActivity.this, true);
            }
            finish();
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            String errorMsg = "错误码：" + errorCode + "，错误信息：" + exception.getMessage();
            ZYAgent.onEvent(PhoneRegisterActivity.this, errorMsg);
            ToastUtils.showToast(errorMsg);
        }

        @Override
        public void onFinish() {
            super.onFinish();
            notificationHandler.sendEmptyMessage(SHOW_LOGIN_BTN);
        }
    };

    private class NotificationHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case SHOW_VERIFICATION_BTN:
                    mBinding.btnPhoneRequestVerificationCode.setTextColor(getResources().getColor(R.color.white));
                    mBinding.btnPhoneRequestVerificationCode.setClickable(true);
                    mBinding.btnPhoneRequestVerificationCode.setBackground(getResources().getDrawable(R.drawable.btn_verification_fillet_bg_normal));
                    break;
                case HIDE_VERIFICATION_BTN:
                    mBinding.btnPhoneRequestVerificationCode.setTextColor(getResources().getColor(R.color.purple_B07EDA));
                    mBinding.btnPhoneRequestVerificationCode.setClickable(false);
                    mBinding.btnPhoneRequestVerificationCode.setBackground(getResources().getDrawable(R.drawable.btn_verification_fillet_bg_pressed));
                    break;
                case SHOW_LOGIN_BTN:
                    mBinding.btnPhoneLogin.setClickable(true);
                    mBinding.btnPhoneLogin.setBackground(getResources().getDrawable(R.drawable.btn_phone_login_fillet_bg_normal));
                    break;
                case HIDE_LOGIN_BTN:
                    mBinding.btnPhoneLogin.setClickable(false);
                    mBinding.btnPhoneLogin.setBackground(getResources().getDrawable(R.drawable.btn_phone_login_fillet_bg_pressed));
                    break;
            }

        }
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }
}
