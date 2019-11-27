package com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.presenter;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.SurfaceView;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSONObject;
import com.jess.arms.integration.AppManager;
import com.jess.arms.di.scope.ActivityScope;
import com.jess.arms.mvp.BasePresenter;
import com.jess.arms.http.imageloader.ImageLoader;

import io.agora.openlive.model.ConstantApp;
import io.agora.openlive.model.EngineConfig;
import io.agora.openlive.model.MyEngineEventHandler;
import io.agora.openlive.model.WorkerThread;
import io.agora.rtc.RtcEngine;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import me.jessyan.rxerrorhandler.core.RxErrorHandler;
import me.jessyan.rxerrorhandler.handler.RetryWithDelay;

import javax.inject.Inject;

import com.jess.arms.utils.RxLifecycleUtils;
import com.orhanobut.logger.Logger;
import com.zhongyou.meet.mobile.BaseApplication;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.contract.MeetChairManActivityContract;
import com.zhongyou.meet.mobile.ameeting.network.RxSchedulersHelper;
import com.zhongyou.meet.mobile.ameeting.network.RxSubscriber;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.HostUser;
import com.zhongyou.meet.mobile.entities.Materials;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;


@ActivityScope
public class MeetChairManActivityPresenter extends BasePresenter<MeetChairManActivityContract.Model, MeetChairManActivityContract.View> {


	@Inject
	public MeetChairManActivityPresenter(MeetChairManActivityContract.Model model, MeetChairManActivityContract.View rootView) {
		super(model, rootView);
	}

	public void getUserCount() {
		if (mModel != null) {
			mModel.getUserCount()
					.compose(RxSchedulersHelper.io_main())
					.retryWhen(new RetryWithDelay(3, 2))
					.compose(RxLifecycleUtils.bindToLifecycle(mRootView))
					.subscribe(new RxSubscriber<JSONObject>() {


						@Override
						public void _onNext(JSONObject jsonObject) {
							Logger.i(jsonObject.toJSONString());
							mRootView.showMessage(jsonObject.toJSONString());
						}

					});


//            mRootView.chengeView(userCount);
		}
	}


	public void joinMeetingCallBack(String meetingID,int uid) {
		if (mModel != null) {
			mModel.joinMeeting(meetingID, new OkHttpCallback<Bucket<HostUser>>() {

				@Override
				public void onSuccess(Bucket<HostUser> entity) {
					mRootView.joinMeetingCallBack(entity,uid);
				}
			});
		}
	}

	public void getMeetingPPT(String meetingID) {
		if (mModel != null) {
			mModel.getMeetingPPt(meetingID, new OkHttpCallback<Bucket<Materials>>() {

				@Override
				public void onSuccess(Bucket<Materials> entity) {
					mRootView.getDataSuccessFormOrigin(entity, "pptPreview");
				}
			});
		}
	}

	public void showPPT(String meetingId,String ppiID){
		if (mModel!=null){
			mModel.showMeetingPPT(meetingId,ppiID, new OkHttpCallback<Bucket>(){

				@Override
				public void onSuccess(Bucket entity) {
					mRootView.getDataSuccessFormOrigin(entity,"showPPT");
				}

				@Override
				public void onFailure(int errorCode, BaseException exception) {

				}
			});
		}
	}

	public void stripSurfaceView(SurfaceView view) {
		ViewParent parent = view.getParent();
		if (parent != null) {
			((FrameLayout) parent).removeView(view);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	public RtcEngine rtcEngine() {
		return worker().getRtcEngine();
	}

	public final WorkerThread worker() {
		return BaseApplication.getInstance().getWorkerThread();
	}

	public final EngineConfig config() {
		return worker().getEngineConfig();
	}

	public final MyEngineEventHandler event() {
		return worker().eventHandler();
	}

	public void doConfigEngine(int cRole) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(BaseApplication.getInstance());
		int prefIndex = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
		int vProfile = ConstantApp.VIDEO_PROFILES[prefIndex];
		worker().configEngine(cRole, vProfile);
	}
}
