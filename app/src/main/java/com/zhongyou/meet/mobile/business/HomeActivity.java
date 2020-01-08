package com.zhongyou.meet.mobile.business;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.BuildConfig;
import com.zhongyou.meet.mobile.Constant;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.MeetingJoinStats;
import com.zhongyou.meet.mobile.entities.Version;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.event.SetUserChatEvent;
import com.zhongyou.meet.mobile.event.UserStateEvent;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.receiver.PhoneReceiver;
import com.zhongyou.meet.mobile.service.WSService;
import com.zhongyou.meet.mobile.utils.DeviceUtil;
import com.zhongyou.meet.mobile.utils.Installation;
import com.zhongyou.meet.mobile.utils.Logger;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.RxBus;
import com.zhongyou.meet.mobile.utils.statistics.ZYAgent;
import com.zhongyou.meet.mobile.wxapi.WXEntryActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import rx.Subscription;
import rx.functions.Action1;


/**
 * 主页
 * Created by wufan on 2017/7/24.
 */

public class HomeActivity extends BasicActivity implements View.OnClickListener {

	private ViewPager viewPager;
	private LinearLayout bottomLayout;
	private RadioGroup radioGroup;
	private RadioButton mettingRadio, discussRadio, logRadio,myRadio;

	private HomePagerAdapter mHomePagerAdapter;

	private ArrayList<Fragment> mFragments;

	private int mIntentType;


	private Subscription subscription;

	private PhoneReceiver phoneReceiver;
	private IntentFilter intentFilter;

	@Override
	public String getStatisticsTag() {
		return "主页";
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home_activity);

		WSService.actionStart(mContext);

		subscription = RxBus.handleMessage(new Action1() {
			@Override
			public void call(Object o) {
				if (o instanceof UserStateEvent) {
					setState(WSService.isPhoneOnline());
				}
			}
		});



		initView();
		versionCheck();
		initData();
		registerDevice();

		if (!TextUtils.isEmpty(Preferences.getMeetingTraceId())) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("meetingJoinTraceId", Preferences.getMeetingTraceId());
			params.put("meetingId", Preferences.getMeetingId());
			params.put("status", 2);
			params.put("type", 1);
			params.put("leaveType", 1);
			ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
		}
	}



	private OkHttpCallback meetingJoinStatsCallback = new OkHttpCallback<Bucket<MeetingJoinStats>>() {

		@Override
		public void onSuccess(Bucket<MeetingJoinStats> meetingJoinStatsBucket) {
			Preferences.setMeetingId(null);
			Preferences.setMeetingTraceId(null);
		}
	};

	private void initView() {
		viewPager = findViewById(R.id.view_pager);
		bottomLayout = findViewById(R.id.bottom_layout);

		radioGroup = findViewById(R.id.radio_group);
		//会议
		mettingRadio = findViewById(R.id.meeting);
		//讨论
		discussRadio = findViewById(R.id.meeting_discuss);
		//日志
//		logRadio = findViewById(R.id.meeting_log);
		// 我的
		myRadio = findViewById(R.id.meeting_my);


		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup radioGroup, int i) {
				switch (i) {
					case R.id.meeting:
						viewPager.setCurrentItem(0);
						break;
					case R.id.meeting_discuss:
						viewPager.setCurrentItem(1);
						break;
					/*case R.id.meeting_log:
						viewPager.setCurrentItem(2);
						break;*/
					case R.id.meeting_my:
						viewPager.setCurrentItem(2);
						break;
					default:
						Log.v("HomeActivity", "onCheckedChanged   has no current index:" + i);
						break;
				}
			}
		});

		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				switch (position) {
					case 0:
						mettingRadio.setChecked(true);
						break;
					case 1:
						discussRadio.setChecked(true);
						break;
					case 2:
						myRadio.setChecked(true);
//						logRadio.setChecked(true);
						break;
					case 3:
						myRadio.setChecked(true);
						break;
						default:
							Logger.v("HomeActivity","onPageSelected has no current position:"+position);
							break;
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});

		mHomePagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
		mFragments = new ArrayList<>();
		//会议
		mFragments.add(MeetingsFragment.newInstance());

		//讨论
		mFragments.add(DiscussFragment.newInstance());
		//日志
//		mFragments.add(RecordFragment.newInstance());
		// 我的
		mFragments.add(ProfileFragment.newInstance());
		mHomePagerAdapter.setData(mFragments);
		viewPager.setAdapter(mHomePagerAdapter);
		viewPager.setOffscreenPageLimit(3);
		phoneReceiver = new PhoneReceiver();
		intentFilter = new IntentFilter();
		intentFilter.addAction("android.intent.action.PHONE_STATE");
		intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
		registerReceiver(phoneReceiver, intentFilter);

	}

	private void initData() {
		mettingRadio.setChecked(true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setState(WSService.isPhoneOnline());
		if (!TextUtils.isEmpty(Preferences.getToken())) {
			ApiClient.getInstance().requestUser();
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();

		registerReceiver(phoneReceiver, intentFilter);

		if (TextUtils.isEmpty(Preferences.getToken())) {
			startActivity(new Intent(this, WXEntryActivity.class));
			finish();
		}
	}


	/*@Override
	protected void checkNetWorkOnClick(View v) {
		switch (v.getId()) {
			case R.id.state:
				if (Preferences.isUserinfoEmpty()) {
					showToast("请先填写姓名,电话,地址,照片");

					boolean isUserAuthByHEZY = Preferences.getUserAuditStatus() == 1;
					UserInfoActivity.actionStart(this, false, isUserAuthByHEZY);
					return;
				}
				new ActionSheetDialog(mContext).builder()
						.setCancelable(false)
						.setCanceledOnTouchOutside(false)
						.addSheetItem("在线", ActionSheetDialog.SheetItemColor.Blue,
								new ActionSheetDialog.OnSheetItemClickListener() {
									@Override
									public void onClick(int which) {
										ZYAgent.onEvent(mContext, "在线按钮");

										if (!WSService.isPhoneOnline()) {
											//当前状态离线,可切换在线
											ZYAgent.onEvent(mContext, "在线按钮,当前离线,切换到在线");
											Log.i(TAG, "当前状态离线,可切换在线");
											RxBus.sendMessage(new SetUserStateEvent(true));
										} else {
											ZYAgent.onEvent(mContext, "在线按钮,当前在线,,无效操作");
										}
									}
								})
						.addSheetItem("离线", ActionSheetDialog.SheetItemColor.Blue,
								new ActionSheetDialog.OnSheetItemClickListener() {
									@Override
									public void onClick(int which) {
										ZYAgent.onEvent(mContext, "离线按钮");
										if (WSService.isPhoneOnline()) {
											//当前状态在线,可切换离线
											Log.i(TAG, "当前状态在线,可切换离线");
											ZYAgent.onEvent(mContext, "离线按钮,当前在线,切换到离线");
											RxBus.sendMessage(new SetUserStateEvent(false));
//                                            WSService.SOCKET_ONLINE =false;
//                                            setState(false);
										} else {
											ZYAgent.onEvent(mContext, "离线按钮,当前离线,无效操作");
										}
									}
								}).show();
				break;

		}
	}*/

	private void setState(boolean isOnline) {
		/*if (isOnline) {
			stateText.setText("在线");
			stateText.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.ic_online, 0, 0);
			stateText.setTextColor(getResources().getColor(R.color.text_yellow_fff000));
		} else {
			stateText.setText("离线");
			stateText.setCompoundDrawablesWithIntrinsicBounds(0, R.mipmap.ic_offline, 0, 0);
			stateText.setTextColor(getResources().getColor(R.color.text_gray_c784fb));
		}*/
	}


	private void versionCheck() {
		ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
			@Override
			public void onSuccess(BaseBean<Version> entity) {
				Version version = entity.getData();
				if (version == null || version.getImportance() == 0) {
					return;
				}
				if (version.getImportance() != 1 && version.getImportance() != 2) {
					startActivity(new Intent(mContext, UpdateActivity.class).putExtra("version", version));
				}


			}
		});
	}

	/**
	 * 上传设备信息
	 */
	private void registerDevice() {
		String uuid = Installation.id(this);
		DisplayMetrics metric = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		int width = metric.widthPixels;
		int height = metric.heightPixels;
		float density = metric.density;
		int densityDpi = metric.densityDpi;

		StringBuffer msg = new StringBuffer("registerDevice ");
		if (TextUtils.isEmpty(uuid)) {
			msg.append("UUID为空");
			Logger.e(TAG, msg.toString());
		} else {
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
				msg.append("apiClient.deviceRegister call");
			} catch (JSONException e) {
				e.printStackTrace();
				msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
			} catch (SecurityException e) {
				e.printStackTrace();
				msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
			}

		}
	}

	private OkHttpCallback registerDeviceCb = new OkHttpCallback<BaseBean>() {
		@Override
		public void onSuccess(BaseBean entity) {
			Log.d(TAG, "registerDevice 成功===");
		}
	};


	private long mExitTime = 0;

	@Override
	public void onBackPressed() {
		if ((System.currentTimeMillis() - mExitTime) > 2000) {
			Toast.makeText(this, "再按一次退出中幼在线", Toast.LENGTH_SHORT).show();
			mExitTime = System.currentTimeMillis();
		} else {
			quit();
		}
	}

	private void quit() {
		ZYAgent.onEvent(getApplicationContext(), "返回退出应用 连接服务 请求停止");
		WSService.stopService(this);
		finish();
		System.exit(0);
	}

	@Override
	protected void onStop() {
		super.onStop();

		unregisterReceiver(phoneReceiver);

	}

	@Override
	public void onDestroy() {

		subscription.unsubscribe();

		try {
			if (phoneReceiver!=null){
				unregisterReceiver(phoneReceiver);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			super.onDestroy();
			if (WSService.isOnline()) {
				//当前状态在线,可切换离线
				Log.i(TAG, "当前状态在线,可切换离线");
				ZYAgent.onEvent(mContext, "离线按钮,当前在线,切换到离线");
				RxBus.sendMessage(new SetUserChatEvent(false));
//                                            WSService.SOCKET_ONLINE =false;
//                                            setState(false);
			} else {
				ZYAgent.onEvent(mContext, "离线按钮,当前离线,无效操作");
			}
		}

	}
}
