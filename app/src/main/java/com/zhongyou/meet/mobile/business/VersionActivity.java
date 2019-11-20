package com.zhongyou.meet.mobile.business;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ycbjie.ycupdatelib.UpdateFragment;
import com.ycbjie.ycupdatelib.UpdateUtils;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.BuildConfig;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.entities.Version;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.utils.ApkUtil;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.ToastUtils;
import com.zhongyou.meet.mobile.utils.Utils;
import com.zhongyou.meet.mobile.wxapi.WXEntryActivity;

import java.io.File;
import java.util.Locale;


public class VersionActivity extends BasicActivity {

	private Button mBtnUpgrade;
	private TextView mLabelViersion;
	private String upDataUrl;

	@Override
	public String getStatisticsTag() {
		return "版本更新";
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_version);
		mBtnUpgrade = findViewById(R.id.bt_update);
		mLabelViersion = findViewById(R.id.label_version);
		//设置标题和返回键
		findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		((TextView)findViewById(R.id.title)).setText(getStatisticsTag().toString());


		versionCheck();

	}
	private boolean isFoceUpDate=false;
	private void versionCheck() {
		ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
			@Override
			public void onSuccess(BaseBean<Version> entity) {
				Version version = entity.getData();
				if (version==null){
					return;
				}
				if (version.getImportance()==1){
					try {
						if (ApkUtil.compareVersion(version.getVersionDesc(),BuildConfig.VERSION_NAME)<=0){
							mBtnUpgrade.setEnabled(false);
							mBtnUpgrade.setBackground(getResources().getDrawable(R.drawable.setting_button_circle_blue_enable));
							mLabelViersion.setText(String.format(Locale.CHINA, "已是新版本：中幼在线%s", BuildConfig.VERSION_NAME));
							return;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				//1:最新版，不用更新 2：小改动，可以不更新 3：建议更新 4 强制更新
				if (version.getImportance()!=0){
					mBtnUpgrade.setEnabled(true);
					mBtnUpgrade.setBackground(getResources().getDrawable(R.drawable.setting_button_circle_blue));
					if (version.getVersionCode()!=null){
						mLabelViersion.setText(String.format(Locale.CHINA, "已有新版本：中幼在线%s", version.getVersionDesc()));
					}else {
						mLabelViersion.setText("已有新版本：中幼在线");
					}

					if (version.getImportance()==4){
						isFoceUpDate=true;
					}else if (version.getImportance()==2||version.getImportance()==3){
						isFoceUpDate=false;
					}
					mBtnUpgrade.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							UpdateUtils.APP_UPDATE_DOWN_APK_PATH = getResources().getString(R.string.app_name) + File.separator + "download";
							String desc = version.getName() + "\n" + "最新版本:" + version.getVersionDesc();
							UpdateFragment updateFragment = UpdateFragment.showFragment(VersionActivity.this,
									isFoceUpDate, version.getUrl(),
									version.getName() + "-" + version.getVersionDesc(),
									desc, BuildConfig.APPLICATION_ID);
						}
					});
				}
			}

			@Override
			public void onFailure(int errorCode, BaseException exception) {
				super.onFailure(errorCode, exception);
				Toast.makeText(mContext, "" + exception.getMessage(), Toast.LENGTH_SHORT).show();
			}
		});
	}
}
