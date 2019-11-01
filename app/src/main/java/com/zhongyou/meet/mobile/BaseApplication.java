package com.zhongyou.meet.mobile;

import android.graphics.Typeface;
import android.support.multidex.MultiDexApplication;

import com.alibaba.fastjson.JSON;
import com.orhanobut.logger.AndroidLogAdapter;
import com.tencent.bugly.crashreport.CrashReport;
import com.tendcloud.tenddata.TCAgent;
import com.zhongyou.meet.mobile.entities.StaticResource;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.Logger;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;

import java.net.URISyntaxException;

import es.dmoral.toasty.Toasty;
import io.agora.openlive.model.WorkerThread;
import io.socket.client.IO;
import io.socket.client.Socket;

public class BaseApplication extends MultiDexApplication {

	public static final String TAG = "BaseApplication";

	private static BaseApplication instance;

	private Socket mSocket;

	@Override
	public void onCreate() {
		super.onCreate();
		com.orhanobut.logger.Logger.addLogAdapter(new AndroidLogAdapter());
		instance = this;

		initSocket();

		Toasty.Config.getInstance()
				.setToastTypeface(Typeface.createFromAsset(getAssets(), "PCap Terminal.otf"))
				.allowQueue(false)
				.apply();

		//内存泄露检测工具,开发中最好开启
//        if (BuildConfig.DEBUG) {
//            Log.i(TAG, "debug下开启LeakCanary");
//            if (LeakCanary.isInAnalyzerProcess(this)) {
//                // This process is dedicated to LeakCanary for heap analysis.
//                // You should not init your app in this process.
//                return;
//            }
//            LeakCanary.install(this);
//        }

		//初始化bugly
		CrashReport.initCrashReport(getApplicationContext(), BuildConfig.BUGLY_APPID, true);

		//初始化TD
		TCAgent.LOG_ON = false;
		TCAgent.init(this);
		TCAgent.setReportUncaughtExceptions(true);

		ApiClient.getInstance().urlConfig(staticResCallback);
      /*  //获取图片地址
        ApiClient.getInstance().getImageUrlPath(TAG, new OkHttpCallback<BaseBean<Object>>() {
            @Override
            public void onSuccess(BaseBean<Object> entity) {
                try {
                    JSONObject obj = JSON.parseObject(JSON.toJSONString(entity));
                    if (obj.getInteger("errcode")==0){
                        String imageUrl = obj.getJSONObject("data").getString("host");
                        if (imageUrl!=null&& !TextUtils.isEmpty(imageUrl)){
                            com.orhanobut.logger.Logger.e(imageUrl);

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });*/
	}

	public static BaseApplication getInstance() {
		return instance;
	}

	private WorkerThread mWorkerThread;

	public synchronized void initWorkerThread(String appId) {
		if (mWorkerThread == null) {
			mWorkerThread = new WorkerThread(getApplicationContext(), appId);
			mWorkerThread.start();

			mWorkerThread.waitForReady();
		}
	}

	public synchronized WorkerThread getWorkerThread() {
		return mWorkerThread;
	}

	public synchronized void deInitWorkerThread() {
		mWorkerThread.exit();
		try {
			mWorkerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		mWorkerThread = null;
	}

	public void initSocket() {
		try {
			IO.Options options = new IO.Options();
			options.forceNew = false;
			options.reconnection = true;
			options.reconnectionDelay = 1000;
			options.reconnectionDelayMax = 5000;
			options.reconnectionAttempts = 10;
			options.query = "userId=" + Preferences.getUserId();
			mSocket = IO.socket(BuildConfig.WS_DOMAIN_NAME, options);
			Logger.i(TAG, "初始化WebSocket成功");
			TCAgent.onEvent(this, "WebSocket", "初始化WebSocket成功");
		} catch (URISyntaxException e) {
			Logger.i(TAG, "初始化WebSocket失败" + e.getMessage());
			TCAgent.onEvent(this, "WebSocket", "初始化WebSocket失败" + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public Socket getSocket() {
		return mSocket;
	}

	private OkHttpCallback staticResCallback = new OkHttpCallback<BaseBean<StaticResource>>() {

		@Override
		public void onSuccess(BaseBean<StaticResource> entity) {

			com.orhanobut.logger.Logger.e(JSON.toJSONString(entity));
			Preferences.setImgUrl(entity.getData().getStaticRes().getImgUrl());
			Preferences.setVideoUrl(entity.getData().getStaticRes().getVideoUrl());
			Preferences.setDownloadUrl(entity.getData().getStaticRes().getDownloadUrl());
			Preferences.setCooperationUrl(entity.getData().getStaticRes().getDownloadUrl());
		}
	};

}
