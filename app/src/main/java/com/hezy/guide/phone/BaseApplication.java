package com.hezy.guide.phone;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.squareup.leakcanary.LeakCanary;
import com.tencent.bugly.crashreport.CrashReport;
import com.tendcloud.tenddata.TCAgent;

import io.agora.openvcall.model.CurrentUserSettings;
import io.agora.openvcall.model.WorkerThread;

public class BaseApplication extends Application {
    public static final String TAG = "BaseApplication";
    private static BaseApplication instance;

    private WorkerThread mWorkerThread;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        //内存泄露检测工具,开发中最好开启
        if(BuildConfig.DEBUG){
            Log.i(TAG,"debug下开启LeakCanary");
            if (LeakCanary.isInAnalyzerProcess(this)) {
                // This process is dedicated to LeakCanary for heap analysis.
                // You should not init your app in this process.
                return;
            }
            LeakCanary.install(this);
        }

        //初始化bugly 4390f8350d
        CrashReport.initCrashReport(getApplicationContext(), BuildConfig.BUGLY_APPID, true);

        //初始化TD
        TCAgent.LOG_ON = true;
        // App ID: 在TalkingData创建应用后，进入数据报表页中，在“系统设置”-“编辑应用”页面里查看App ID。
        // 渠道 ID: 是渠道标识符，可通过不同渠道单独追踪数据。
        TCAgent.init(this);
        // 如果已经在AndroidManifest.xml配置了App ID和渠道ID，调用TCAgent.init(this)即可；或与AndroidManifest.xml中的对应参数保持一致。
        TCAgent.setReportUncaughtExceptions(true);
    }

    public static BaseApplication getInstance() {
        return instance;
    }

    private Activity mCurrentActivity = null;
    public Activity getCurrentActivity(){
        return mCurrentActivity;
    }
    public void setCurrentActivity(Activity mCurrentActivity){
        this.mCurrentActivity = mCurrentActivity;
//        if(mCurrentActivity!=null && LoginHelper.mIsLogout==true){
//            LoginHelper.mIsLogout = false;
//            LoginHelper.logout(mCurrentActivity);
//        }
    }


    public synchronized void initWorkerThread() {
        if (mWorkerThread == null) {
            mWorkerThread = new WorkerThread(getApplicationContext());
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

    public static final CurrentUserSettings mVideoSettings = new CurrentUserSettings();
}
