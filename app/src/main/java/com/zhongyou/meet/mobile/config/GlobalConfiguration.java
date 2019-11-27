package com.zhongyou.meet.mobile.config;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;

import com.jess.arms.base.delegate.AppLifecycles;
import com.jess.arms.di.module.GlobalConfigModule;
import com.jess.arms.http.imageloader.glide.GlideImageLoaderStrategy;
import com.jess.arms.integration.ConfigModule;
import com.zhongyou.meet.mobile.Constant;

import java.io.File;
import java.util.List;

/**
 * @author luopan@centerm.com
 * @date 2019-11-20 14:58.
 */
public class GlobalConfiguration implements ConfigModule {
	@Override
	public void applyOptions(@NonNull Context context, @NonNull GlobalConfigModule.Builder builder) {
		//使用 builder 可以为框架配置一些配置信息

		File file=new File(Environment.getExternalStorageDirectory().getPath()+"/中幼在线");
		if (!file.exists()){
			file.mkdir();
		}

		builder.baseurl(Constant.getAPIHOSTURL())
				.cacheFile(file)
				.imageLoaderStrategy(new GlideImageLoaderStrategy())
		;
	}

	@Override
	public void injectAppLifecycle(@NonNull Context context, @NonNull List<AppLifecycles> lifecycles) {
		//向 Application的 生命周期中注入一些自定义逻辑
	}

	@Override
	public void injectActivityLifecycle(@NonNull Context context, @NonNull List<Application.ActivityLifecycleCallbacks> lifecycles) {
				//向 Activity 的生命周期中注入一些自定义逻辑
	}

	@Override
	public void injectFragmentLifecycle(@NonNull Context context, @NonNull List<FragmentManager.FragmentLifecycleCallbacks> lifecycles) {
			//向 Fragment 的生命周期中注入一些自定义逻辑
	}
}
