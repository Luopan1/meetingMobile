package com.hezy.guide.phone.ui;

import android.text.TextUtils;
import android.view.View;

import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.UserinfoFragmentBinding;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.UserState;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.service.HeartService;
import com.hezy.guide.phone.utils.RxBus;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import rx.Subscription;
import rx.functions.Action1;


/**
 * 用户信息fragment
 * Created by wufan on 2017/7/24.
 */

public class UserinfoFragment extends BaseDataBindingFragment<UserinfoFragmentBinding> {

    public static UserinfoFragment newInstance() {
        UserinfoFragment fragment = new UserinfoFragment();
        return fragment;
    }

    @Override
    protected int initContentView() {
        return R.layout.userinfo_fragment;
    }

    @Override
    protected void initView() {


        mBinding.mEtName.setText(Preferences.getUserName());
        mBinding.mEtPhone.setText(Preferences.getUserMobile());
        mBinding.mEtAddress.setText(Preferences.getUserAddress());
        mBinding.mEtSignature.setText(Preferences.getUserSignature());
        if (!TextUtils.isEmpty(Preferences.getUserPhoto())) {
            Picasso.with(BaseApplication.getInstance()).load(Preferences.getUserPhoto()).into(mBinding.mIvPicture);
        }

        mBinding.mEtName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtName.getText().toString();
                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserName().equals(str)) {
                        Map<String, String> params = new HashMap<>();
                        params.put("name", str);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                Preferences.setUserName(str);
                            }

                        });
                    }
                }
            }
        });

        mBinding.mEtPhone.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtPhone.getText().toString();
                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserMobile().equals(str)) {
                        Map<String, String> params = new HashMap<>();
                        params.put("mobile", str);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                Preferences.setUserMobile(str);
                            }

                        });
                    }
                }
            }
        });

        mBinding.mEtSignature.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtSignature.getText().toString();
                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserSignature().equals(str)) {
                        Map<String, String> params = new HashMap<>();
                        params.put("signature", str);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                Preferences.setUserSignature(str);
                            }

                        });
                    }
                }
            }
        });

        if (!TextUtils.isEmpty(Preferences.getWeiXinHead())) {
            Picasso.with(BaseApplication.getInstance()).load(Preferences.getWeiXinHead()).into(mBinding.views.mIvHead);
        }


        setState(HeartService.isOnline());

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserState) {
                    setState(HeartService.isOnline());
                }
            }
        });
    }


    private void setState(boolean isOnline) {
        if (isOnline) {
            mBinding.views.mTvState.setText("在线状态");
            mBinding.views.mTvState.setBackgroundResource(R.drawable.userinfo_set_state_online_bg_shape);
        } else {
            mBinding.views.mTvState.setText("离线状态");
            mBinding.views.mTvState.setBackgroundResource(R.drawable.userinfo_set_state_offline_bg_shape);
        }
    }

    private Subscription subscription;

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

}
