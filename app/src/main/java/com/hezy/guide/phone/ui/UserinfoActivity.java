package com.hezy.guide.phone.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import com.adorkable.iosdialog.ActionSheetDialog;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.Constant;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.UserinfoActivityBinding;
import com.hezy.guide.phone.entities.QiniuToken;
import com.hezy.guide.phone.entities.RecordTotal;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.StringCheckUtil;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.helper.TakePhotoHelper;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoImpl;
import com.jph.takephoto.model.InvokeParam;
import com.jph.takephoto.model.TContextWrap;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.model.TakePhotoOptions;
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
import com.zaaach.citypicker.CityPickerActivity;

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

public class UserinfoActivity extends BaseDataBindingActivity<UserinfoActivityBinding> implements TakePhoto.TakeResultListener, InvokeListener {

    private Subscription subscription;
    private InvokeParam invokeParam;
    private TakePhoto takePhoto;
    private String imagePath;
    private CountDownTimer countDownTimer;

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, UserinfoActivity.class);
        context.startActivity(intent);
    }

    public static void actionStart(Context context, boolean isFirst) {
        Intent intent = new Intent(context, UserinfoActivity.class);
        intent.putExtra("isFirst", isFirst);
        context.startActivity(intent);
    }

    private boolean isFirst;


    @Override
    protected void initExtraIntent() {
        isFirst = getIntent().getBooleanExtra("isFirst", false);
    }

    @Override
    protected int initContentView() {
        return R.layout.userinfo_activity;
    }

    private void setUserUI() {
        mBinding.mEtName.setText(Preferences.getUserName());
        mBinding.mEtPhone.setText(Preferences.getUserMobile());
        mBinding.mEtAddress.setText(Preferences.getUserAddress());
        mBinding.mEtSignature.setText(Preferences.getUserSignature());
//        mBinding.mEtAddress.setText(Preferences.getUserAddress());
        mBinding.mTvAddress.setText(Preferences.getUserAddress());
        if (!TextUtils.isEmpty(Preferences.getUserPhoto())) {
            Picasso.with(BaseApplication.getInstance()).load(Preferences.getUserPhoto()).into(mBinding.mIvPicture);
        }

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
        if (Preferences.isUserinfoEmpty()) {
            //第一次设置为空,返回键隐藏
            mBinding.mIvLeft.setVisibility(View.GONE);
        }
        setUserUI();


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


        mBinding.mEtName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtName.getText().toString().trim();
                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserName().equals(str)) {
                        long len = StringCheckUtil.calculateLength(str);
                        if (len < Constant.NICKNAME_MIN || len > Constant.NICKNAME_MAX) {
                            showToast("姓名为2-8个汉字或者4-16个英文,请重新输入");
                            return;
                        }

                        Map<String, String> params = new HashMap<>();
                        params.put("name", str);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                showToast("设置姓名成功");
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
                    final String verifyCode = mBinding.mEtCaptchaNum.getText().toString().trim();

                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserMobile().equals(str) && !TextUtils.isEmpty(verifyCode)) {
                        Map<String, String> params = new HashMap<>();
                        params.put("mobile", str);
                        params.put("verifyCode", verifyCode);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                showToast("设置手机号成功");
                                Preferences.setUserMobile(str);
                                mBinding.mLayoutCaptcha.setVisibility(View.GONE);
                                mBinding.mEtCaptchaNum.setText("");
                            }

                        });
                    }
                }
            }
        });

//        mBinding.mEtAddress.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (!hasFocus) {
//                    //失去焦点提交请求
//                    final String str = mBinding.mEtAddress.getText().toString().trim();
//                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserAddress().equals(str)) {
//                        Map<String, String> params = new HashMap<>();
//                        params.put("address", str);
//                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
//                            @Override
//                            public void onSuccess(BaseErrorBean entity) {
//                                showToast("设置地址成功");
//                                Preferences.setUserAddress(str);
//                            }
//
//                        });
//                    }
//                }
//            }
//        });


        mBinding.mEtSignature.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //失去焦点提交请求
                    final String str = mBinding.mEtSignature.getText().toString().trim();
                    long len = StringCheckUtil.calculateLength(str);
                    if (len > Constant.SIGNATURE_MAX) {
                        showToast("签名小于30字,请重新输入");
                        return;
                    }
                    if ((!TextUtils.isEmpty(str)) && !Preferences.getUserSignature().equals(str)) {
                        Map<String, String> params = new HashMap<>();
                        params.put("signature", str);
                        ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                            @Override
                            public void onSuccess(BaseErrorBean entity) {
                                showToast("设置签名成功");
                                Preferences.setUserSignature(str);
                            }

                        });
                    }
                }
            }
        });


    }

    @Override
    protected void initListener() {
        mBinding.mIvPicture.setOnClickListener(this);
        mBinding.mBtnSavePhoto.setOnClickListener(this);
        mBinding.mTvObtainCaptcha.setOnClickListener(this);
        mBinding.mTvAddress.setOnClickListener(this);
        mBinding.mIvLeft.setOnClickListener(this);
        mBinding.mTvRight.setOnClickListener(this);

        mBinding.mEtName.addTextChangedListener(mTextWatcher);

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
            }
        });
    }


    private void save(){

        final String str = mBinding.mEtName.getText().toString().trim();
        final String phoneStr = mBinding.mEtPhone.getText().toString().trim().trim();
        final String verifyCode = mBinding.mEtCaptchaNum.getText().toString().trim();

        if(TextUtils.isEmpty(str)){
            showToast("姓名不能为空");
            return;
        }


        if(TextUtils.isEmpty(phoneStr)){
            showToast("电话不能为空");
            return;
        }
        if ( !Preferences.getUserName().equals(str)|| !Preferences.getUserMobile().equals(phoneStr)) {
            //性别或者电话改变
            long len = StringCheckUtil.calculateLength(str);
            if (len < Constant.NICKNAME_MIN || len > Constant.NICKNAME_MAX) {
                showToast("姓名为2-8个汉字或者4-16个英文,请重新输入");
                return;
            }


            if ((!TextUtils.isEmpty(phoneStr)) && !Preferences.getUserMobile().equals(phoneStr) && TextUtils.isEmpty(verifyCode)) {
                showToast("验证码不能为空");
                return;
            }

            Map<String, String> params = new HashMap<>();
            if (!Preferences.getUserMobile().equals(str)) {
                params.put("name", str);
            }
            if ( !Preferences.getUserMobile().equals(phoneStr) ) {
                params.put("mobile", phoneStr);
                params.put("verifyCode", verifyCode);

            }

            ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                @Override
                public void onSuccess(BaseErrorBean entity) {
                    if (!Preferences.getUserMobile().equals(str)) {
                        Preferences.setUserName(str);
                    }
                    if ((!TextUtils.isEmpty(phoneStr)) && !Preferences.getUserMobile().equals(phoneStr) && !TextUtils.isEmpty(verifyCode)) {
                        Preferences.setUserMobile(phoneStr);
                    }
                    if (Preferences.isUserinfoEmpty()) {
                        showToast("请先填写姓名,电话,地址,照片");
                        return;
                    }
                    if(isFirst){
                        HomeActivity.actionStart(mContext);
                    }
                    finish();

                }

            });
        }

        if (Preferences.isUserinfoEmpty()) {
            showToast("请先填写姓名,电话,地址,照片");
            return;
        }
        if(isFirst){
            HomeActivity.actionStart(mContext);
        }
        finish();
    }

    @Override
    protected void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mIvLeft:
                finish();
                break;
            case R.id.mTvRight:
                save();
                break;
            case R.id.mIvPicture:
                configTakePhotoOption(takePhoto);
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
            case R.id.mBtnSavePhoto:
                uploadImage();
                break;
            case R.id.mTvObtainCaptcha:
                final String str = mBinding.mEtPhone.getText().toString().trim();
                if (TextUtils.isEmpty(str)) {
                    showToast("当前手机号为空");
                    return;
                }
                if (str.equals(Preferences.getUserMobile())) {
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
            case R.id.mTvAddress:
                //地址定位
                mBinding.mTvAddress.setFocusable(true);
                mBinding.mTvAddress.setFocusableInTouchMode(true);
                mBinding.mTvAddress.requestFocus();
                Intent intent = new Intent(mContext, CityPickerActivity.class);
                startActivityForResult(intent, CityPickerActivity.REQUEST_CODE_PICK_CITY);
                break;

        }
    }


    private TextWatcher mTextWatcher =new TextWatcher() {
        private int editStart;
        private int editEnd;
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            editStart = mBinding.mEtName.getSelectionStart();
            editEnd = mBinding.mEtName.getSelectionEnd();

            // 先去掉监听器，否则会出现栈溢出
            mBinding.mEtName.removeTextChangedListener(mTextWatcher);

            // 注意这里只能每次都对整个EditText的内容求长度，不能对删除的单个字符求长度
            // 因为是中英文混合，单个字符而言，calculateLength函数都会返回1
            LogUtils.d(TAG,"StringCheckUtil"+ StringCheckUtil.calculateLength(s.toString()) );
            while (StringCheckUtil.calculateLength(s.toString()) > Constant.NICKNAME_MAX) { // 当输入字符个数超过限制的大小时，进行截断操作
                s.delete(editStart - 1, editEnd);
                editStart--;
                editEnd--;
                LogUtils.d(TAG,"while"+s.toString());
                LogUtils.d(TAG,"while"+StringCheckUtil.calculateLength(s.toString()));
            }
            // mEtNickname.setText(s);将这行代码注释掉就不会出现后面所说的输入法在数字界面自动跳转回主界面的问题了，多谢@ainiyidiandian的提醒
            mBinding.mEtName.setSelection(editStart);

            // 恢复监听器
            mBinding.mEtName.addTextChangedListener(mTextWatcher);




        }
    };

  

    /**
     * 获取用户输入的分享内容字数
     *
     * @return
     */
    private long getInputCount() {
        return StringCheckUtil.calculateLength(mBinding.mEtName.getText().toString());
    }


    private void configTakePhotoOption(TakePhoto takePhoto) {
        TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();
        builder.setCorrectImage(true);
        takePhoto.setTakePhotoOptions(builder.create());
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
        if (requestCode == CityPickerActivity.REQUEST_CODE_PICK_CITY) {
            if (resultCode == RESULT_OK) {
                String result = data.getStringExtra(CityPickerActivity.KEY_PICKED_CITY);
                mBinding.mTvAddress.setText(result);
                final String str = result;
                if ((!TextUtils.isEmpty(str)) && !Preferences.getUserAddress().equals(str)) {
                    Map<String, String> params = new HashMap<>();
                    params.put("address", str);
                    ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpBaseCallback<BaseErrorBean>() {
                        @Override
                        public void onSuccess(BaseErrorBean entity) {
                            showToast("设置地址成功");
                            Preferences.setUserAddress(str);
                        }

                    });
                }
            }
        }
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
