package com.zy.guide.phone.business;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zy.guide.phone.ApiClient;
import com.zy.guide.phone.BaseException;
import com.zy.guide.phone.R;
import com.zy.guide.phone.entities.Version;
import com.zy.guide.phone.entities.base.BaseBean;
import com.zy.guide.phone.utils.OkHttpCallback;

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
		mBtnUpgrade.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				Uri content_url = Uri.parse(upDataUrl);
				intent.setData(content_url);
				startActivity(Intent.createChooser(intent, "请选择浏览器"));
			}
		});
	}

	private void versionCheck() {
		ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
			@Override
			public void onSuccess(BaseBean<Version> entity) {
				Version version = entity.getData();
				if (version.getImportance() != 1) {
					mBtnUpgrade.setEnabled(true);
					mBtnUpgrade.setBackground(getResources().getDrawable(R.drawable.button_circle_blue));
					upDataUrl=version.getUrl();
					mLabelViersion.setText(String.format(Locale.CHINA, "已有新版本：中幼在线%s", version.getVersionCode()));
				} else {
					mBtnUpgrade.setEnabled(false);
					mBtnUpgrade.setBackground(getResources().getDrawable(R.drawable.button_circle_blue_enable));
					mLabelViersion.setText(String.format(Locale.CHINA, "已是新版本：中幼在线%s", version.getVersionCode()));

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
