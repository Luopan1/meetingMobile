package com.zhongyou.meet.mobile.ameeting.meetmodule.di.module;



import com.zhongyou.meet.mobile.ameeting.network.ApiService;
import com.zhongyou.meet.mobile.ameeting.network.HttpsRequest;

import dagger.Module;
import dagger.Provides;

@Module
public class NetworkModule {
	@Provides
	public ApiService providerOkHttpClient() {
		return  HttpsRequest.provideClientApi();
	}
}
