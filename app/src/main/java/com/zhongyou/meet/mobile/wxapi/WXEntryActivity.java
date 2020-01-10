package com.zhongyou.meet.mobile.wxapi;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.alibaba.fastjson.JSON;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.orhanobut.logger.Logger;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import com.ycbjie.ycupdatelib.UpdateFragment;
import com.ycbjie.ycupdatelib.UpdateUtils;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.BuildConfig;
import com.zhongyou.meet.mobile.Constant;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.UserInfoActivity;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.ui.activity.MeetChairManActivityActivity;
import com.zhongyou.meet.mobile.business.BindActivity;
import com.zhongyou.meet.mobile.business.HomeActivity;
import com.zhongyou.meet.mobile.business.UpdateActivity;
import com.zhongyou.meet.mobile.entities.FinishWX;
import com.zhongyou.meet.mobile.entities.LoginWechat;
import com.zhongyou.meet.mobile.entities.User;
import com.zhongyou.meet.mobile.entities.UserData;
import com.zhongyou.meet.mobile.entities.Version;
import com.zhongyou.meet.mobile.entities.Wechat;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.ApkUtil;
import com.zhongyou.meet.mobile.utils.DeviceUtil;
import com.zhongyou.meet.mobile.utils.Installation;
import com.zhongyou.meet.mobile.utils.Login.LoginHelper;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.RxBus;
import com.zhongyou.meet.mobile.utils.ToastUtils;
import com.zhongyou.meet.mobile.utils.statistics.ZYAgent;

import org.json.JSONObject;

import java.io.File;
import java.util.List;

import es.dmoral.toasty.Toasty;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import rx.Subscription;
import rx.functions.Action1;

import static com.zhongyou.meet.mobile.BaseApplication.getInstance;

/**
 * Created by wufan on 2017/7/14.
 */

public class WXEntryActivity extends FragmentActivity implements IWXAPIEventHandler, EasyPermissions.PermissionCallbacks {

	private RelativeLayout wchatLoginImage;
	private IWXAPI mWxApi;

	private Subscription subscription;
	private BasePopupView mLoadingDialog;



	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
	}

	@Override
	public void onPermissionsGranted(int i, @NonNull List<String> list) {
		registerDevice();

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
			Log.e("onCreate++","onCreate");


		wchatLoginImage = findViewById(R.id.layout_wx);
		wchatLoginImage.setClickable(true);
		wchatLoginImage.setEnabled(true);
		wchatLoginImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (EasyPermissions.hasPermissions(WXEntryActivity.this, perms)) {
					wchatLogin();
				} else {
					EasyPermissions.requestPermissions(WXEntryActivity.this, "请授予必要的权限", 0, perms);
				}

			}
		});

		reToWx();

		/*if (TextUtils.isEmpty(Preferences.getWeiXinHead())){
			getHostUrl(2);
		}else {
			getHostUrl(1);
		}*/
		registerDevice();
		if (EasyPermissions.hasPermissions(this, perms)) {
		} else {
			EasyPermissions.requestPermissions(this, "请授予必要的权限", 0, perms);
		}


	}


	protected void initView() {
		Log.e("initView()","initViews");
		if (Preferences.isLogin()) {
			Logger.e("Preferences.isLogin()");

			ApiClient.getInstance().requestUser(this, new OkHttpCallback<BaseBean<UserData>>() {
				@Override
				public void onSuccess(BaseBean<UserData> entity) {
					if (entity == null || entity.getData() == null || entity.getData().getUser() == null) {
						Toast.makeText(WXEntryActivity.this, "用户数据为空", Toast.LENGTH_SHORT).show();
						return;
					}
					Logger.i(JSON.toJSONString(entity));

//                    BindActivity.actionStart(WXEntryActivity.this,true,true);

					Wechat wechat = entity.getData().getWechat();
					if (wechat != null) {
						LoginHelper.savaWeChat(wechat);
					}

					//验证用户是否存在
					User user = entity.getData().getUser();

					if (TextUtils.isEmpty(user.getMobile())) {
						BindActivity.actionStart(WXEntryActivity.this, true, false);
					} else if (Preferences.isUserinfoEmpty()) {
						boolean isUserAuthByHEZY = user.getAuditStatus() == 1;
						UserInfoActivity.actionStart(WXEntryActivity.this, true, isUserAuthByHEZY);
					} else {
						startActivity(new Intent(WXEntryActivity.this, HomeActivity.class));
					}

					finish();
				}
			});
		}

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

	private void getHostUrl(int type) {
		if (type==1){
			mLoadingDialog = new XPopup.Builder(this)
					.dismissOnBackPressed(false)
					.dismissOnTouchOutside(false)
					.asLoading("正在加载中")
					.show();
		}

		ApiClient.getInstance().getHttpBaseUrl(this, new OkHttpCallback<com.alibaba.fastjson.JSONObject>() {

			@Override
			public void onSuccess(com.alibaba.fastjson.JSONObject entity) {

				if (entity.getInteger("errcode") == 0.0) {
					Constant.WEBSOCKETURL = entity.getJSONObject("data").getJSONObject("staticRes").getString("websocket");
					Constant.APIHOSTURL = entity.getJSONObject("data").getJSONObject("staticRes").getString("domain");
					Constant.DOWNLOADURL = entity.getJSONObject("data").getJSONObject("staticRes").getString("apiDownloadUrl");
					com.orhanobut.logger.Logger.e("webSocket:=" + Constant.WEBSOCKETURL);
					com.orhanobut.logger.Logger.e("ApiHost:=" + Constant.APIHOSTURL);
					com.orhanobut.logger.Logger.e("DownLoadUrl:=" + Constant.DOWNLOADURL);
					if (Constant.WEBSOCKETURL == null || Constant.APIHOSTURL == null) {
						return;
					}
					registerDevice();

				}

			}

			@Override
			public void onFailure(int errorCode, BaseException exception) {
				com.orhanobut.logger.Logger.e(exception.getMessage());
				if (mLoadingDialog!=null){
					mLoadingDialog.dismiss();
				}
				Toasty.error(getInstance(), exception.getMessage(), Toast.LENGTH_SHORT, true).show();
			}

			@Override
			public void onFinish() {

			}
		});
	}


	/**
	 * 上传设备信息
	 */
	private void registerDevice() {
		mLoadingDialog = new XPopup.Builder(this)
				.dismissOnBackPressed(false)
				.dismissOnTouchOutside(false)
				.asLoading("正在加载中")
				.show();
		Logger.e(Constant.APIHOSTURL);
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

	private void initEntry() {
		if (EasyPermissions.hasPermissions(this, perms)) {
			Log.e("initEntry()","hasPermissions");
			initView();
		} else {
			EasyPermissions.requestPermissions(this, "请授予必要的权限", 0, perms);
		}
	}

	private void versionCheck() {
		Logger.e(Constant.DOWNLOADURL);
		ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
			@Override
			public void onSuccess(BaseBean<Version> entity) {
				Version version = entity.getData();
				if (version == null || version.getImportance() == 0||version.getImportance()==1) {
					initEntry();
					return;
				}
				//1:最新版，不用更新 2：小改动，可以不更新 3：建议更新 4 强制更新
				boolean isForceUpDate = false;
				if (version.getImportance() == 4) {
					/*startActivity(new Intent(getApplication(), UpdateActivity.class).putExtra("version", version));
					finish();*/
					isForceUpDate = true;
				} else if (version.getImportance() == 2||version.getImportance()==3) {
					//弹窗提醒更新
					isForceUpDate = false;
				}

				try {
					if (ApkUtil.compareVersion(version.getVersionDesc(),BuildConfig.VERSION_NAME)>0){
						UpdateUtils.APP_UPDATE_DOWN_APK_PATH = getResources().getString(R.string.app_name) + File.separator + "download";
						String desc = version.getName() + "\n" + "最新版本:" + version.getVersionDesc();
						UpdateFragment updateFragment = UpdateFragment.showFragment(WXEntryActivity.this,
								isForceUpDate, version.getUrl(),
								version.getName() + "-" + version.getVersionDesc(),
								desc, BuildConfig.APPLICATION_ID);

						if (mLoadingDialog!=null){
							mLoadingDialog.dismiss();
						}

						updateFragment.setOnClickListener(new UpdateFragment.OnClickListener() {
							@Override
							public void onClick(int i) {
								//点击下载 1   暂停下载 1  或者下次再说 0
								if (i == 0) {
									initEntry();
								} else if (i == 1) {
									int downloadStatus = updateFragment.getDownloadStatus();
									if (downloadStatus == UpdateUtils.DownloadStatus.UPLOADING) {
										//正在下载中  不能跳转到首页
										Toasty.info(WXEntryActivity.this, "当前下载已暂停", Toast.LENGTH_SHORT, true).show();
									}
								}


							}
						});
					}else {
						initEntry();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}



			}


			@Override
			public void onFailure(int errorCode, BaseException exception) {
				super.onFailure(errorCode, exception);
				if (BuildConfig.DEBUG){
					initEntry();
				}
				if (mLoadingDialog!=null){
					mLoadingDialog.dismiss();
				}
				Toasty.error(WXEntryActivity.this,exception.getMessage(),Toast.LENGTH_SHORT,true).show();
				Logger.e(exception.getMessage());
//                initEntry();
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

		final SendAuth.Req req = new SendAuth.Req();
		req.scope = "snsapi_userinfo";
		req.state = "GuideMobile_wx_login";
		mWxApi.sendReq(req);
		Log.v("wxphone98", "1");
		showDialog("正在加载...");
	}

	@Override
	public void onReq(BaseReq baseReq) {
		Log.v("wxphone98", "2");
	}

	// 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
	//app发送消息给微信，处理返回消息的回调
	@Override
	public void onResp(BaseResp baseResp) {
		Log.v("wxphone98", "3");
		String result;
		Log.e("WXEntryActivity", "onResp: " + baseResp.errCode);
		switch (baseResp.errCode) {
			case BaseResp.ErrCode.ERR_OK:
				result = "登录成功";
				if (baseResp.getType() == 1) {
					ZYAgent.onEvent(WXEntryActivity.this, "微信按钮 登录成功");
					final SendAuth.Resp sendResp = ((SendAuth.Resp) baseResp);
					String code = sendResp.code;
					Log.e("WXEntryActivity", "onResp: " + code + " -  state -  " + sendResp.state);
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
			Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
			return;
		}
	}


	private void requestWechatLogin(String code, String state) {
		Logger.e(Constant.APIHOSTURL);
		ApiClient.getInstance().requestWechat(code, state, this, new OkHttpCallback<BaseBean<LoginWechat>>() {

			@Override
			public void onSuccess(BaseBean<LoginWechat> entity) {
				ZYAgent.onEvent(WXEntryActivity.this, "请求微信登录回调 成功");
				Logger.e(entity.getData().toString());
				if (entity.getData() == null) {
					return;
				}
				LoginWechat loginWechat = entity.getData();
				Wechat wechat = loginWechat.getWechat();
				Preferences.setWeiXinHead(wechat.getHeadimgurl());
				Preferences.setUserPhoto(wechat.getHeadimgurl());
				Preferences.setUserName(wechat.getNickname());// TODO: 2019-10-09 需要判断昵称是否为空字符串
				User user = loginWechat.getUser();
				if (loginWechat.getUser() == null) {
				} else {
					LoginHelper.savaUser(user);
					if (TextUtils.isEmpty(user.getMobile())) {
						BindActivity.actionStart(WXEntryActivity.this, true, false);
					} else if (Preferences.isUserinfoEmpty()) {
						boolean isUserAuthByHEZY = user.getAuditStatus() == 1;
						UserInfoActivity.actionStart(WXEntryActivity.this, true, isUserAuthByHEZY);
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
