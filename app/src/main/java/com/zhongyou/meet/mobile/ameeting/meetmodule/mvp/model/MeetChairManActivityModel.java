package com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.model;

import android.app.Application;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.jess.arms.integration.IRepositoryManager;
import com.jess.arms.mvp.BaseModel;

import com.jess.arms.di.scope.ActivityScope;

import javax.inject.Inject;

import com.orhanobut.logger.Logger;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.contract.MeetChairManActivityContract;
import com.zhongyou.meet.mobile.ameeting.network.ApiService;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;


@ActivityScope
public class MeetChairManActivityModel extends BaseModel implements MeetChairManActivityContract.Model {
	private String TAG=this.getClass().getSimpleName();
	@Inject
	public MeetChairManActivityModel(IRepositoryManager repositoryManager) {
		super(repositoryManager);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	@Override
	public Observable<JSONObject> getUserCount() {
		/*return mRepositoryManager.obtainRetrofitService(ApiService.class).index();*/

		return null;

	}

	@Override
	public void joinMeeting(String meetingId, OkHttpCallback callback) {
		ApiClient.getInstance().getMeetingHost("MeetChairManActivityModel", meetingId, callback);
	}

	@Override
	public void getMeetingPPt(String meetingId, OkHttpCallback callback) {
		ApiClient.getInstance().meetingMaterials("MeetChairManActivityModel",callback,meetingId);
	}

	@Override
	public void showMeetingPPT(String meetingId, String pptId,OkHttpCallback callback) {
		ApiClient.getInstance().meetingSetMaterial(TAG, callback, meetingId, pptId);
	}


}