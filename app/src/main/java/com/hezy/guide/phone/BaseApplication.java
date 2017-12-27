package com.hezy.guide.phone;

import android.app.Activity;
import android.support.multidex.MultiDexApplication;

import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.tencent.bugly.crashreport.CrashReport;
import com.tendcloud.tenddata.TCAgent;

import java.net.URISyntaxException;

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

        instance = this;

        initSocket();
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

//        requestGlobalConfig();

    }

    public static BaseApplication getInstance() {
        return instance;
    }

    private Activity mCurrentActivity = null;

    public Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    public void setCurrentActivity(Activity mCurrentActivity) {
        this.mCurrentActivity = mCurrentActivity;
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

    public void initSocket(){
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

//    private void requestGlobalConfig() {
//        String url = ApiClient.getGlobalConfigurationInformation();
//
//        OkHttpUtil.getInstance().get(url, this, new OkHttpBaseCallback<GlobalConfigurationInformation>() {
//
//            @Override
//            public void onSuccess(final GlobalConfigurationInformation result) {
//                Preferences.setImgUrl(result.getData().getStaticRes().getImgUrl());
//                Preferences.setVideoUrl(result.getData().getStaticRes().getVideoUrl());
//                Preferences.setDownloadUrl(result.getData().getStaticRes().getDownloadUrl());
//                Preferences.setCooperationUrl(result.getData().getStaticRes().getDownloadUrl());
//            }
//
//
//        });
//    }


}
