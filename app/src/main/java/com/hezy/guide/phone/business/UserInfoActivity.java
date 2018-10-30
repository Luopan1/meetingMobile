package com.hezy.guide.phone.business;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.adorkable.iosdialog.ActionSheetDialog;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.databinding.UserinfoActivityBinding;
import com.hezy.guide.phone.entities.Custom;
import com.hezy.guide.phone.entities.District;
import com.hezy.guide.phone.entities.Grid;
import com.hezy.guide.phone.entities.PostType;
import com.hezy.guide.phone.entities.QiniuToken;
import com.hezy.guide.phone.entities.StaticResource;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
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

public class UserInfoActivity extends BaseDataBindingActivity<UserinfoActivityBinding> implements TakePhoto.TakeResultListener, InvokeListener {

    private Subscription subscription;
    private InvokeParam invokeParam;
    private TakePhoto takePhoto;
    private String imagePath;

    public static boolean isFirst;

    //总部对应的ID
    private final String ID_DISTRICT = "7d8a40b5255845699f948c0b220b6a64";

    public static final int CODE_USERINFO_DISTRICT = 0x100;
    public static final int CODE_USERINFO_POSTTYPE = 0x101;
    public static final int CODE_USERINFO_GRID = 0x102;
    public static final int CODE_USERINFO_CUSTOM = 0x103;

    public static final String KEY_USERINFO_POSTTYPE = "key_posttype";
    public static final String KEY_USERINFO_DISTRICT = "key_district";
    public static final String KEY_DISTRICT_ID = "key_district_Id";
    public static final String KEY_USERINFO_GRID = "key_grid";
    public static final String KEY_USERINFO_CUSTOM = "key_custom";

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, UserInfoActivity.class);
        context.startActivity(intent);
    }

    public static void actionStart(Context context, boolean isFirst) {
        Intent intent = new Intent(context, UserInfoActivity.class);
        intent.putExtra("isFirst", isFirst);
        context.startActivity(intent);
    }

    @Override
    protected void initExtraIntent() {
        isFirst = getIntent().getBooleanExtra("isFirst", false);
    }

    @Override
    protected int initContentView() {
        return R.layout.userinfo_activity;
    }

    private void setUserUI() {
        hideEditTextFocus();
        //手机号
        mBinding.tvUserInfoMobile.setText(Preferences.getUserMobile());
        //姓名
        mBinding.edtUserInfoName.setText(Preferences.getUserName());
        //选择用户类型
        mBinding.edtUserInfoPostType.setText(Preferences.getUserPostType());
        //选择中心
        mBinding.edtUserInfoDistrict.setText(Preferences.getUserDistrict());
        //选择网格
        mBinding.edtUserInfoGrid.setText(Preferences.getUserGrid());
        //选择客户
        mBinding.edtUserInfoCustom.setText(Preferences.getUserCustom());
        //选择地址
        mBinding.edtUserInfoAddress.setText(Preferences.getUserAddress());

        if (!TextUtils.isEmpty(Preferences.getUserPhoto())) {
            Picasso.with(BaseApplication.getInstance()).load(Preferences.getUserPhoto()).into(mBinding.imgUserInfoHead);
        }
    }

    private void hideEditTextFocus() {
        mBinding.edtUserInfoPostType.setInputType(InputType.TYPE_NULL);
        mBinding.edtUserInfoDistrict.setInputType(InputType.TYPE_NULL);
        mBinding.edtUserInfoGrid.setInputType(InputType.TYPE_NULL);
        mBinding.edtUserInfoCustom.setInputType(InputType.TYPE_NULL);
        mBinding.edtUserInfoAddress.setInputType(InputType.TYPE_NULL);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getTakePhoto().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
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
        setUserUI();
    }

    @Override
    protected void initListener() {
        mBinding.btnUserInfoSave.setOnClickListener(this);
        mBinding.imgUserInfoHead.setOnClickListener(this);
        mBinding.edtUserInfoName.setOnFocusChangeListener(edtUserInfoNameFocusChangeListener);
        mBinding.edtUserInfoPostType.setOnTouchListener(onTouchListener);
        mBinding.edtUserInfoDistrict.setOnTouchListener(onTouchListener);
        mBinding.edtUserInfoGrid.setOnTouchListener(onTouchListener);
        mBinding.edtUserInfoCustom.setOnTouchListener(onTouchListener);
        mBinding.edtUserInfoAddress.setOnTouchListener(onTouchListener);
    }

    private View.OnFocusChangeListener edtUserInfoNameFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            String userName = ((EditText)v).getText().toString().trim();
            if (!hasFocus && !TextUtils.isEmpty(userName)){
                Map<String, String> params = new HashMap<>();
                params.put("name", userName);
                ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                    }
                });
            }
        }
    };

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                switch (v.getId()) {
                    case R.id.edtUserInfoPostType:
                        Intent postTypeIntent = new Intent(mContext, PostTypeActivity.class);
                        startActivityForResult(postTypeIntent, CODE_USERINFO_POSTTYPE);
                        break;
                    case R.id.edtUserInfoDistrict:
                        Intent districtIntent = new Intent(mContext, DistrictActivity.class);
                        startActivityForResult(districtIntent, CODE_USERINFO_DISTRICT);
                        break;
                    case R.id.edtUserInfoGrid:
                        String g_districtID = Preferences.getUserDistrictId();
                        if (TextUtils.isEmpty(g_districtID)) {
                            ToastUtils.showToast("请先选择中心");
                            return false;
                        }
                        Intent gridIntent = new Intent(mContext, GridActivity.class);
                        gridIntent.putExtra(KEY_DISTRICT_ID, g_districtID);
                        startActivityForResult(gridIntent, CODE_USERINFO_GRID);
                        break;
                    case R.id.edtUserInfoCustom:
                        String c_districtID = Preferences.getUserDistrictId();
                        if (TextUtils.isEmpty(c_districtID)) {
                            ToastUtils.showToast("请先选择中心");
                            return false;
                        }
                        Intent customIntent = new Intent(mContext, CustomActivity.class);
                        customIntent.putExtra(KEY_DISTRICT_ID, c_districtID);
                        startActivityForResult(customIntent, CODE_USERINFO_CUSTOM);
                        break;
                    case R.id.edtUserInfoAddress:
                        Intent intent = new Intent(mContext, CityPickerActivity.class);
                        startActivityForResult(intent, CityPickerActivity.REQUEST_CODE_PICK_CITY);
                        break;
                }
            }
            return false;
        }
    };

    @Override
    public String getStatisticsTag() {
        return "编辑资料";
    }

    @Override
    public void takeSuccess(TResult result) {
        Log.i(TAG, "takeSuccess：" + result.getImage().getOriginalPath());
        imagePath = result.getImage().getOriginalPath();
        Picasso.with(BaseApplication.getInstance()).load("file:" + imagePath).into(mBinding.imgUserInfoHead);
        uploadImage();
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
    protected void normalOnClick(View v) {

        switch (v.getId()) {
            case R.id.imgUserInfoHead:
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
            case R.id.btnUserInfoSave:
                save();
                RxBus.sendMessage(new UserUpdateEvent());
                break;
        }
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
    protected void onSaveInstanceState(Bundle outState) {
        getTakePhoto().onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
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

    private void configTakePhotoOption(TakePhoto takePhoto) {
        TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();
        builder.setCorrectImage(true);
        takePhoto.setTakePhotoOptions(builder.create());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getTakePhoto().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CityPickerActivity.REQUEST_CODE_PICK_CITY) {
                String result = data.getStringExtra(CityPickerActivity.KEY_PICKED_CITY);
                mBinding.edtUserInfoAddress.setText(result);

                if ((!TextUtils.isEmpty(result)) && !Preferences.getUserAddress().equals(result)) {
                    Preferences.setUserAddress(result);
                    Map<String, String> params = new HashMap<>();
                    params.put("address", result);
                    ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                        @Override
                        public void onSuccess(BaseErrorBean entity) {
                            showToast("设置地址成功");
                        }

                    });
                }
            } else if (requestCode == CODE_USERINFO_DISTRICT) {
                //设置大区、中心
                District district = data.getParcelableExtra(KEY_USERINFO_DISTRICT);
                mBinding.edtUserInfoDistrict.setText(district.getName());
                Preferences.setUserDistrict(district.getName());
                Preferences.setUserDistrictId(district.getId());

                Map<String, String> params = new HashMap<>();
                params.put("areaId", district.getId());
                ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        showToast("设置机构成功");
                    }
                });
            } else if (requestCode == CODE_USERINFO_POSTTYPE) {
                //用户类型
                PostType postType = data.getParcelableExtra(KEY_USERINFO_POSTTYPE);
                mBinding.edtUserInfoPostType.setText(postType.getName());
                Preferences.setUserPostType(postType.getName());
                Preferences.setUserPostTypeId(postType.getId());

                Map<String, String> params = new HashMap<>();
                params.put("postTypeId", postType.getId());
                ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        showToast("设置用户类型成功");
                    }

                });

            } else if (requestCode == CODE_USERINFO_GRID) {
                //网格
                Grid grid = data.getParcelableExtra(KEY_USERINFO_GRID);
                mBinding.edtUserInfoGrid.setText(grid.getName());
                Preferences.setUserGrid(grid.getName());
                Preferences.setUserGridId(grid.getId());

                Map<String, String> params = new HashMap<>();
                params.put("gridId", grid.getId());
                ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        showToast("设置用户网格成功");
                    }

                });

            } else if (requestCode == CODE_USERINFO_CUSTOM) {
                Custom custom = data.getParcelableExtra(KEY_USERINFO_CUSTOM);
                mBinding.edtUserInfoCustom.setText(custom.getName());
                Preferences.setUserCustom(custom.getName());
                Preferences.setUserCustomId(custom.getId());

                Map<String, String> params = new HashMap<>();
                params.put("customId", custom.getId());
                ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        showToast("设置客户成功");
                    }

                });
            }
        }
    }

    private void uploadImage() {
        ApiClient.getInstance().requestQiniuToken(this, new OkHttpCallback<BaseBean<QiniuToken>>() {

            @Override
            public void onSuccess(BaseBean<QiniuToken> result) {
                String token = result.getData().getToken();
                if (TextUtils.isEmpty(token)) {
                    showToast("七牛token获取错误");
                    return;
                }
                Configuration config = new Configuration.Builder().connectTimeout(5).responseTimeout(5).build();
                UploadManager uploadManager = new UploadManager(config);
                System.out.println(imagePath);
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
                            }

                        }, null));
            }
        });
    }

    private void relate(final String key) {
        Logger.i(TAG, "key " + key);
        if (!TextUtils.isEmpty(key)) {
            if (TextUtils.isEmpty(Preferences.getImgUrl())) {
                ApiClient.getInstance().urlConfig(staticResCallback(key));
            } else {
                Map<String, String> params = new HashMap<>();
                params.put("photo", Preferences.getImgUrl() + key); //服务器存储全路径
                ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        Preferences.setUserPhoto(Preferences.getImgUrl() + key);
                        ToastUtils.showToast("保存照片成功");
                    }
                });
            }
        }
    }

    private OkHttpCallback staticResCallback(final String key) {
        return new OkHttpCallback<BaseBean<StaticResource>>() {

            @Override
            public void onSuccess(BaseBean<StaticResource> entity) {
                Preferences.setImgUrl(entity.getData().getStaticRes().getImgUrl());
                Preferences.setVideoUrl(entity.getData().getStaticRes().getVideoUrl());
                Preferences.setDownloadUrl(entity.getData().getStaticRes().getDownloadUrl());
                Preferences.setCooperationUrl(entity.getData().getStaticRes().getDownloadUrl());

                Map<String, String> params = new HashMap<>();
                params.put("photo", Preferences.getImgUrl() + key); //服务器存储全路径
                ApiClient.getInstance().requestUserExpostor(this, params, new OkHttpCallback<BaseErrorBean>() {
                    @Override
                    public void onSuccess(BaseErrorBean entity) {
                        Logger.i("photo", entity.toString());
                        Preferences.setUserPhoto(Preferences.getImgUrl() + key);
                        ToastUtils.showToast("保存照片成功");
                    }
                });
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                super.onFailure(errorCode, exception);
                Toast.makeText(mContext, "获取前缀失败", Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void requestData() {
    }

    private void save() {

        if (TextUtils.isEmpty(Preferences.getUserPhoto())) {
            ToastUtils.showToast("请设置一张图片");
            return;
        }

        String userName = mBinding.edtUserInfoName.getText().toString().trim();
        if (TextUtils.isEmpty(userName)) {
            ToastUtils.showToast("请输入姓名");
            return;
        }
        Preferences.setUserName(userName);

        if (TextUtils.isEmpty(mBinding.edtUserInfoPostType.getText().toString().trim())) {
            ToastUtils.showToast("请选择用户类型");
            return;
        }
        if (TextUtils.isEmpty(mBinding.edtUserInfoDistrict.getText().toString().trim())) {
            ToastUtils.showToast("请选择中心");
            return;
        }
        if (TextUtils.isEmpty(mBinding.edtUserInfoAddress.getText().toString().trim())) {
            ToastUtils.showToast("请选择地址");
            return;
        }

        if (isFirst) {
            startActivity(new Intent(UserInfoActivity.this, HomeActivity.class));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

}
