package com.hezy.guide.phone.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.UUIDUtils;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;
import static com.hezy.guide.phone.utils.ToastUtils.showToast;

/**
 * ws全局服务,整个应用生命周期内接听电话
 * Created by wufan on 2017/7/28.
 */

public class WSService extends Service {
    private Socket mSocket;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        initWS();
    }

    /**
     * 上传设备信息
     */
    private void registerDevice(String socketid) {

        String uuid = UUIDUtils.getUUID(this);
        DisplayMetrics metric = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(metric);
        metric = getResources().getDisplayMetrics();
        int width = metric.widthPixels;
        int height = metric.heightPixels;
        float density = metric.density;
        int densityDpi = metric.densityDpi;


        StringBuffer msg = new StringBuffer("registerDevice ");
        if (TextUtils.isEmpty(uuid)) {
            msg.append("UUID为空");
            showToast(msg.toString());
            LogUtils.e(TAG, msg.toString());
        } else {

            //            {
//                "uuid": "string",
//                    "androidId": "string",
//                    "manufacturer": "string",
//                    "name": "string",
//                    "model": "string",
//                    "version": "string",
//                    "sdkVersion": 0,
//                    "screenDensity": "string",
//                    "display": "string",
//                    "finger": "string",
//                    "socketId": "string",
//                    "appVersion": "string",
//                    "aliDeviceUUID": "string",
//                    "cpuSerial": "string",
//                    "androidDeviceId": "string",
//                    "buildSerial": "string",
//                    "source": 0
//            }

                    try {
                        JSONObject params = new JSONObject();
                        params.put("uuid", uuid);
                        params.put("androidId", TextUtils.isEmpty(Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID)) ? "" : Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                        params.put("manufacturer", Build.MANUFACTURER);
                        params.put("name", Build.BRAND);
                        params.put("model", Build.MODEL);
                        params.put("sdkVersion", Build.VERSION.SDK_INT);
                        params.put("screenDensity", "width:" + width + ",height:" + height + ",density:" + density + ",densityDpi:" + densityDpi);
                        params.put("display", Build.DISPLAY);
                        params.put("finger", Build.FINGERPRINT);
                        params.put("appVersion", BuildConfig.FLAVOR + "_" + BuildConfig.VERSION_NAME + "_" + BuildConfig.VERSION_CODE);
                        params.put("cpuSerial", Installation.getCPUSerial());
                        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        params.put("androidDeviceId", tm != null ? tm.getDeviceId() : "");
                        params.put("buildSerial", Build.SERIAL);
                        params.put("source", 2);
                        params.put("socketId",socketid);
                        ApiClient.getInstance().deviceRegister(this,params.toString(),registerDeviceCb);
                        msg.append("client.deviceRegister call");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        msg.append("registerDevice error jsonObject.put e.getMessage() = "+e.getMessage());
                    }



        }
//        client.errorlog(mContext, 2, msg.toString(), respStatusCallback);


    }

    private OkHttpBaseCallback registerDeviceCb = new OkHttpBaseCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {
            Log.d(TAG, "registerDevice 成功===");
        }
    };

    private void initWS() {
        BaseApplication app = (BaseApplication) getApplication();
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("LISTEN_SOCKET_ID", onUserJoined);
        mSocket.connect();
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
//            JSONObject data = (JSONObject) args;
            Log.i("wsserver", "connected==" + args);

        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("wsserver", "diconnected");

        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("wsserver", "Error connecting");
        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            JSONObject data = (JSONObject) args[0];
            String socketid = "";
            try {
                socketid = data.getString("socket_id");
                Log.i("wsserver", "socketid==" + socketid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            registerDevice(socketid);

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    JSONObject data = (JSONObject) args[0];
//                    String socketid = "";
//                    try {
//                        socketid = data.getString("socket_id");
//                        Log.i("wsserver", "socketid==" + socketid);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    registerDevice(socketid);
//                }
//            });
        }
    };


    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("user joined", onUserJoined);
    }
}
