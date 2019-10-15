package com.zhongyou.meet.mobile.business;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.entities.Version;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.ToastUtils;

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

	private void versionCheck() {
		ApiClient.getInstance().versionCheck(this, new OkHttpCallback<BaseBean<Version>>() {
			@Override
			public void onSuccess(BaseBean<Version> entity) {
				Version version = entity.getData();

				if (version!=null&&version.getImportance() != 1) {
					mBtnUpgrade.setEnabled(true);
					mBtnUpgrade.setBackground(getResources().getDrawable(R.drawable.setting_button_circle_blue));
					upDataUrl=version.getUrl();
					if (version.getVersionCode()!=null){
						mLabelViersion.setText(String.format(Locale.CHINA, "已有新版本：中幼在线%s", version.getVersionCode()));
					}else {
						mLabelViersion.setText("已有新版本：中幼在线");
					}

					mBtnUpgrade.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent();
							intent.setAction(Intent.ACTION_VIEW);
							if (upDataUrl==null){
								ToastUtils.showToast("下载连接失效，请去应用市场下载");
								return;
							}
							Uri content_url = Uri.parse(upDataUrl);
							intent.setData(content_url);
							startActivity(Intent.createChooser(intent, "请选择浏览器"));
						}
					});

				} else {
					mBtnUpgrade.setEnabled(false);
					mBtnUpgrade.setBackground(getResources().getDrawable(R.drawable.setting_button_circle_blue_enable));
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
