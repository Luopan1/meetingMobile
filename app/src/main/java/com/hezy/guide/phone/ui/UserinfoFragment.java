package com.hezy.guide.phone.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.adorkable.iosdialog.ActionSheetDialog;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.UserinfoFragmentBinding;
import com.hezy.guide.phone.entities.QiniuToken;
import com.hezy.guide.phone.entities.RecordTotal;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.PagerSetGuideLog;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.service.WSService;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.helper.TakePhotoHelper;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoImpl;
import com.jph.takephoto.model.InvokeParam;
import com.jph.takephoto.model.TContextWrap;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.permission.InvokeListener;
import com.jph.takephoto.permission.PermissionManager;
import com.jph.takephoto.permission.TakePhotoInvocationHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rx.Subscription;
import rx.functions.Action1;



/**
 * 用户信息fragment
 * Created by wufan on 2017/7/24.
 */

public class UserinfoFragment extends BaseDataBindingFragment<UserinfoFragmentBinding> implements TakePhoto.TakeResultListener, InvokeListener {

    private Subscription subscription;
    private InvokeParam invokeParam;
    private TakePhoto takePhoto;
    private String imagePath;
    private CountDownTimer countDownTimer;

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
        mBinding.mEtAddress.setText(Preferences.getUserAddress());

        countDownTimer = new CountDownTimer(60 * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mBinding.mTvObtainCaptcha.setText(millisUntilFinished / 1000 + "S 后重新获取 ");
            }

            @Override
            public void onFinish() {
                mBinding.mTvObtainCaptcha.setEnabled(true);
                mBinding.mTvObtainCaptcha.setText("获取验证码");
            }
        };

        if (!TextUtils.isEmpty(Preferences.getUserPhoto())) {
            Picasso.with(BaseApplication.getInstance()).load(Preferences.getUserPhoto()).into(mBinding.mIvPicture);
        }

        mBinding.mEtName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtName.getText().toString().trim();
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
                if (hasFocus) {
                    //获取焦点显示获取验证啊
                    mBinding.mLayoutCaptcha.setVisibility(View.VISIBLE);
                }
            }
        });


        mBinding.mEtCaptchaNum.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtPhone.getText().toString().trim().trim();
                    final String verifyCode = mBinding.mTvObtainCaptcha.getText().toString().trim();

                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserMobile().equals(str) && !TextUtils.isEmpty(verifyCode)) {
                        Map<String, String> params = new HashMap<>();
                        params.put("mobile", str);
                        params.put("verifyCode", str);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                showToast("设置手机号成功");
                                Preferences.setUserMobile(str);
                                mBinding.mLayoutCaptcha.setVisibility(View.GONE);
                            }

                        });
                    }
                }
            }
        });

        mBinding.mEtAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtAddress.getText().toString().trim();
                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserAddress().equals(str)) {
                        Map<String, String> params = new HashMap<>();
                        params.put("address", str);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                Preferences.setUserAddress(str);
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
                    final String str = mBinding.mEtSignature.getText().toString().trim();
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


        setState(WSService.isOnline());

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserStateEvent) {
                    setState(WSService.isOnline());
                }
            }
        });
    }

    @Override
    protected void initListener() {
        mBinding.mIvPicture.setOnClickListener(this);
        mBinding.views.mTvState.setOnClickListener(this);
        mBinding.mBtnSavePhoto.setOnClickListener(this);
        mBinding.views.mIvHead.setOnClickListener(this);
        mBinding.mTvObtainCaptcha.setOnClickListener(this);
    }

    @Override
    protected void requestData() {
//        requestRecordTotal();
    }

    @Override
    public void onResume() {
        super.onResume();
        requestRecordTotal();
    }

    private void requestRecordTotal() {
        ApiClient.getInstance().requestRecordTotal(this, new OkHttpBaseCallback<BaseBean<RecordTotal>>() {
            @Override
            public void onSuccess(BaseBean<RecordTotal> entity) {
                String time = String.valueOf(entity.getData().getTotal());
                mBinding.views.mTvTime.setText(time);
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

    @Override
    protected void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mIvPicture:
                File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
                if (!file.getParentFile().exists())
                    file.getParentFile().mkdirs();
                final Uri imageUri = Uri.fromFile(file);

                new ActionSheetDialog(mContext).builder()//
                        .setCancelable(false)//
                        .setCanceledOnTouchOutside(false)//
                        .addSheetItem("拍照", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        takePhoto.onPickFromCaptureWithCrop(imageUri, TakePhotoHelper.getCropOptions());
                                    }
                                })
                        .addSheetItem("相册", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        takePhoto.onPickFromGalleryWithCrop(imageUri, TakePhotoHelper.getCropOptions());
                                    }
                                }).show();
                break;
            case R.id.mTvState:
                new ActionSheetDialog(mContext).builder()//
                        .setCancelable(false)//
                        .setCanceledOnTouchOutside(false)//
                        .addSheetItem("在线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if (TextUtils.isEmpty(Preferences.getUserMobile())) {
                                            showToast("请先填写电话号码");
                                            return;
                                        }
                                        if (!WSService.isOnline()) {
                                            //当前状态离线,可切换在线
                                            Log.i(TAG, "当前状态离线,可切换在线");
                                            RxBus.sendMessage(new SetUserStateEvent(true));
                                        }


                                    }
                                })
                        .addSheetItem("离线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if (WSService.isOnline()) {
                                            //当前状态在线,可切换离线
                                            Log.i(TAG, "当前状态在线,可切换离线");
                                            RxBus.sendMessage(new SetUserStateEvent(false));
//                                            WSService.SOCKET_ONLINE =false;
//                                            setState(false);
                                        }
                                    }
                                }).show();
                break;
            case R.id.mBtnSavePhoto:
                uploadImage();
                break;
            case R.id.mIvHead:
                RxBus.sendMessage(new PagerSetGuideLog());
                break;
            case R.id.mTvObtainCaptcha:
                final String str = mBinding.mEtPhone.getText().toString().trim();
                if(TextUtils.isEmpty(str)){
                    showToast("当前手机号为空");
                    return;
                }
                if(str.equals(Preferences.getUserMobile())){
                    showToast("当前手机号已设置成功");
                    return;
                }
                mBinding.mTvObtainCaptcha.setEnabled(false);
                countDownTimer.start();
                ApiClient.getInstance().requestVerifyCode(this, str, new OkHttpBaseCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {

                    }

                    @Override
                    public void onErrorAll(Exception e) {
                        super.onErrorAll(e);
                        countDownTimer.cancel();
                        mBinding.mTvObtainCaptcha.setEnabled(true);
                        mBinding.mTvObtainCaptcha.setText("获取验证码");
                    }
                });
                break;

        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        getTakePhoto().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        getTakePhoto().onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getTakePhoto().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 获取TakePhoto实例
     *
     * @return
     */
    public TakePhoto getTakePhoto() {
        if (takePhoto == null) {
            takePhoto = (TakePhoto) TakePhotoInvocationHandler.of(this).bind(new TakePhotoImpl(this, this));
        }
        return takePhoto;
    }

    @Override
    public void takeSuccess(TResult result) {
        Log.i(TAG, "takeSuccess：" + result.getImage().getOriginalPath());
        imagePath = result.getImage().getOriginalPath();
        Picasso.with(BaseApplication.getInstance()).load("file:" + imagePath).into(mBinding.mIvPicture);
        uploadImage();
    }

    private void uploadImage() {
        ApiClient.getInstance().requestQiniuToken(this, new OkHttpBaseCallback<BaseBean<QiniuToken>>() {

            @Override
            public void onSuccess(BaseBean<QiniuToken> result) {
                String token = result.getData().getToken();
                if (TextUtils.isEmpty(token)) {
                    showToast("七牛token获取错误");
                    return;
                }
                Configuration config = new Configuration.Builder().connectTimeout(5).responseTimeout(5).build();
                UploadManager uploadManager = new UploadManager(config);
                uploadManager.put(
                        new File(imagePath),
                        "osg/user/expostor/photo/" + UUID.randomUUID().toString().replace("-", "") + ".jpg",
                        token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject response) {
                                if (info.isNetworkBroken() || info.isServerError()) {
                                    showToast("上传失败");
                                    return;
                                }
                                if (info.isOK()) {
                                    relate(key);
                                } else {
                                    showToast("上传失败");
                                }
                            }
                        },
                        new UploadOptions(null, null, true, new UpProgressHandler() {
                            @Override
                            public void progress(final String key, final double percent) {
//                                NumberFormat nf = NumberFormat.getPercentInstance();
//                                nf.setMinimumFractionDigits(0);
//                                nf.setRoundingMode(RoundingMode.HALF_UP);
//                                String rates = nf.format(percent);
//                                uploadPercentText.setText(rates);
//                                progressBar.setProgress((int) (percent * 100));
                            }

                        }, null));

            }
        });
    }


    private void relate(String key) {
        LogUtils.i(TAG, "key " + key);
        final String str = key;
        if ((!TextUtils.isEmpty(str)) && !Preferences.getUserMobile().equals(str)) {
            Map<String, String> params = new HashMap<>();
            params.put("photo", Preferences.getImgUrl() + str); //服务器存储全路径
            ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                @Override
                public void onSuccess(BaseErrorBean entity) {
                    Preferences.setUserPhoto(Preferences.getImgUrl() + str);
                    ToastUtils.showToast("保存照片成功");
                }
            });
        }

    }

    @Override
    public void takeFail(TResult result, String msg) {
        Log.i(TAG, "takeFail:" + msg);
    }

    @Override
    public void takeCancel() {
        Log.i(TAG, getResources().getString(R.string.msg_operation_canceled));
    }


    @Override
    public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
        PermissionManager.TPermissionType type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod());
        if (PermissionManager.TPermissionType.WAIT.equals(type)) {
            this.invokeParam = invokeParam;
        }
        return type;
    }


    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

}
