package com.zhongyou.meet.mobile.business;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.view.View;


import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.UserInfoActivity;
import com.zhongyou.meet.mobile.databinding.ActivityBindingBinding;
import com.zhongyou.meet.mobile.databinding.UserinfoActivityBinding;
import com.zhongyou.meet.mobile.entities.base.BaseErrorBean;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.ToastUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author luopan@centerm.com
 * @date 2019-10-08 17:08.
 *
 * 微信登录后 绑定手机号界面
 *
 */
public  class BindActivity extends BaseDataBindingActivity<ActivityBindingBinding> {
    private CountDownTimer countDownTimer;
    private static boolean userIsAuthByHEZY;


    public static boolean isFirst;


    @Override
    protected int initContentView() {
        return R.layout.activity_binding;
    }

    @Override
    public String getStatisticsTag() {
        return "绑定微信";
    }

    @Override
    protected void initView() {
        mBinding.txtName.setText(Preferences.getUserName());
        mBinding.btGetcodeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBinding.txtPhone.getText().toString().trim().length() != 11) {
                    showToast("手机号位数不正确");
                } else {
                    getCheckCode(mBinding.txtPhone.getText().toString().trim());
                }
            }
        });

        countDownTimer = new CountDownTimer(60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mBinding.btGetcodeText.setText(millisUntilFinished / 1000 + "S 后重新获取 ");
            }

            @Override
            public void onFinish() {
                mBinding.btGetcodeText.setEnabled(true);
                mBinding.btGetcodeText.setText("获取验证码");
            }
        };

        mBinding.btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindUserWithWeChat();
            }
        });
    }

    /**
     * 绑定手机号
     */
    private void bindUserWithWeChat() {

        Map<String, String> params = new HashMap<>();
        String userName = mBinding.txtName.getText().toString().trim();
        String userPhone = mBinding.txtPhone.getText().toString().trim();
        String userCheckCode = mBinding.txtCode.getText().toString().trim();
//        if (!TextUtils.isEmpty(userName)) {
//            params.put("name", userName);
//        }
        if (userPhone.length() == 11 && userCheckCode.length() >= 4) {
            params.put("mobile", userPhone);
            params.put("verifyCode", userCheckCode);

        }

        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
            @Override
            public void onSuccess(BaseErrorBean entity) {

                if (isFirst) {
                    // TODO: 2019-10-08 成功之后根据用户手机号是否存在来判断是否登录成功
                    Preferences.setUserMobile(userPhone);
                    UserInfoActivity.actionStart(BindActivity.this,true,true);
                }
                finish();
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                ToastUtils.showToast(exception.getMessage());
            }


        });
    }

    private void getCheckCode(String phone) {
        mBinding.btGetcodeText.setEnabled(false);
        countDownTimer.start();
        ApiClient.getInstance().requestVerifyCode(this, phone, new OkHttpCallback<BaseErrorBean>() {
            @Override
            public void onSuccess(BaseErrorBean entity) {

            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                super.onFailure(errorCode, exception);
                countDownTimer.cancel();
                mBinding.btGetcodeText.setEnabled(true);
                mBinding.btGetcodeText.setText("获取验证码");
            }

        });
    }


    public static void actionStart(Context context, boolean isFirst, boolean userIsAuthByHEZY) {
        Intent intent = new Intent(context, BindActivity.class);
        intent.putExtra("isFirst", isFirst);
        intent.putExtra("userIsAuthByHEZY", userIsAuthByHEZY);
        context.startActivity(intent);
    }

    @Override
    protected void initExtraIntent() {
        isFirst = getIntent().getBooleanExtra("isFirst", false);
        userIsAuthByHEZY = getIntent().getBooleanExtra("userIsAuthByHEZY", false);
    }
}
