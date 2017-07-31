package com.hezy.guide.phone.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.UserState;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.net.OkHttpUtil;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;

import java.util.HashMap;
import java.util.Map;

/**心跳服务
 * Created by wufan on 2017/7/27.
 */

public class HeartService   extends Service{
    public final String TAG = "HeartService";
    private Handler handler = new Handler();
    private Runnable heartRunnable;
    /** 服务是否已经创建 */
    public static boolean serviceIsCreate = false;
    /** 是否已离线 */
    public static boolean OffLineFlagStage = false;
    /**
     * 用户设置离线
     */
    public static boolean USER_SET_OFFLINE =false;
    /**
     * 心跳请求事件间隔3秒
     */
    private static int KEEP_ALIVE_TIME=3000;

    public static void actionStart(Context context) {
        // 启动心跳轮询服务
        if (!HeartService.serviceIsCreate) {
            Intent heartService = new Intent(context, HeartService.class);
            context.startService(heartService);
        }
    }
    public static void stopService(Context context){
        if (HeartService.serviceIsCreate) {
            Intent heartService = new Intent(context, HeartService.class);
            context.stopService(heartService);
            HeartService.serviceIsCreate = false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        LogUtils.i(TAG,"后台心跳服务启动");
        serviceIsCreate = true;
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        heartRunnable = new Runnable() {
            @Override
            public void run() {
//				Logger.v(TAG,"心跳包运行run方法");
                requestUserExpostorState();
            }
        };
        handler.post(heartRunnable);

        return START_STICKY;
    }

    private void requestUserExpostorState(){
        Map<String,String> params=new HashMap<>();
        if(!USER_SET_OFFLINE){
            params.put("state","1");
        }else{
            params.put("state","2");
        }

        ApiClient.getInstance().requestUserExpostorState(this,params,requestUserExpostorStateCallback);
    }

    private OkHttpBaseCallback<BaseErrorBean> requestUserExpostorStateCallback = new OkHttpBaseCallback<BaseErrorBean>() {
        @Override
        public void onSuccess(BaseErrorBean entity) {
            if(!USER_SET_OFFLINE && OffLineFlagStage ){
                OffLineFlagStage = false;
                //用户设置为离线,不受心跳影响.
                //用户设置为在线,心跳为离线,通知改变为在线
                LogUtils.i(TAG,"心跳成功,用户设置为在线,旧状态心跳为离线,通知改变为在线");
                ToastUtils.showToast("心跳成功,用户设置为在线,旧状态心跳为离线,通知改变为在线");
                RxBus.sendMessage(new UserState());
            }
            OffLineFlagStage = false;
        }

        @Override
        public void onErrorAll(Exception e) {
            super.onErrorAll(e);
            if(!USER_SET_OFFLINE && !OffLineFlagStage ){
                OffLineFlagStage = true;
                //用户设置为在线,心跳为离线,通知改变为在线
                LogUtils.i(TAG,"心跳失败,用户设置为在线,旧状态心跳为在线,通知改变为离线");
                ToastUtils.showToast("心跳失败,用户设置为在线,旧状态心跳为在线,通知改变为离线");
                RxBus.sendMessage(new UserState());
            }
            OffLineFlagStage = true;
        }

        @Override
        public void onFinish() {
            poll();
        }
    };

    private void poll(){
        handler.removeCallbacksAndMessages(null);
        if(serviceIsCreate){
            handler.postDelayed(heartRunnable, KEEP_ALIVE_TIME);
        }
    }

    public static boolean isOnline(){
        if(USER_SET_OFFLINE){
            return false;
        }else{
            if(OffLineFlagStage){
                return false;
            }else{
                return true;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.i(TAG,"后台心跳服务被销毁");
        serviceIsCreate = false;
        OffLineFlagStage = true;
        handler.removeCallbacksAndMessages(null);
        OkHttpUtil.getInstance().cancelTag(this);
    }
}
