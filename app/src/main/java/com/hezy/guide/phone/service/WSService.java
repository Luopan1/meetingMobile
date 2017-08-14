package com.hezy.guide.phone.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.CallEvent;
import com.hezy.guide.phone.event.HangDownEvent;
import com.hezy.guide.phone.event.HangOnEvent;
import com.hezy.guide.phone.event.HangUpEvent;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.TvLeaveChannel;
import com.hezy.guide.phone.event.TvTimeoutHangUp;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpBaseCallback;
import com.hezy.guide.phone.net.OkHttpUtil;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.ui.OnCallActivity;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.LogUtils;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.UUIDUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import rx.Subscription;
import rx.functions.Action1;

import static com.hezy.guide.phone.utils.ToastUtils.showToast;

/**
 * ws全局服务,整个应用生命周期内接听电话
 * Created by wufan on 2017/7/28.
 */

public class WSService extends Service {
    public static final String TAG = "wsserver";
    /**
     * 服务是否已经创建
     */
    public static boolean serviceIsCreate = false;
    /**
     * 是否接电话中
     */
    public static boolean IS_ON_PHONE = false;
    private static String WS_URL = BuildConfig.WS_DOMAIN_NAME;
    //    /**
//     * 是否已离线
//     */
//    public static boolean SOCKET_ONLINE = false;
//    /**
//     * 用户设置离线,废弃
//     */
//    public static boolean USER_SET_OFFLINE = false;
    private static Socket mSocket;

    private Subscription subscription;
    private Handler mHandler;

    public static boolean isOnline() {
        return mSocket != null && mSocket.connected();
    }

    public static void actionStart(Context context) {
        if (!WSService.serviceIsCreate) {
            Log.i(TAG,"!WSService.serviceIsCreate actionStart");
            Intent intent = new Intent(context, WSService.class);
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        if (WSService.serviceIsCreate) {
            Log.i(TAG,"WSService.serviceIsCreate stopService");
            RxBus.sendMessage(new SetUserStateEvent(false));
            Intent intent = new Intent(context, WSService.class);
            context.stopService(intent);
            WSService.serviceIsCreate = false;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @Override
    public void onCreate() {
        serviceIsCreate = true;
        //默认离线,用户手动切换在线
//        connectSocket();
        setStartForeground();
        mHandler = new Handler(Looper.getMainLooper());
        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof SetUserStateEvent) {
                    SetUserStateEvent event = (SetUserStateEvent) o;
                    if (event.isOnline()) {
                        connectSocket();
                    } else {
                        disConnectSocket();
                    }

                } else if (o instanceof CallEvent) {
                    CallEvent event = (CallEvent) o;
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("tvSocketId", event.getTvSocketId());
                        jsonObject.put("reply", event.isCall() ? 1 : 0);
                        mSocket.emit("REPLY_TV", jsonObject);
                        Log.i(TAG, jsonObject.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                } else if (o instanceof HangUpEvent) {
                    mSocket.emit("END_CALL", "END_CALL");
                    Log.i(TAG, " mSocket.emit(\"END_CALL\",null)");
                } else if (o instanceof HangOnEvent) {
                    Log.i(TAG, " HangOnEvent 接听电话转离线");
//                    if (isOnline()) {
//                        IS_ON_PHONE = true;
//                        Log.i(TAG, " HangOnEvent 接听电话转离线 isOnline() true");
//                        disConnectSocket();
//                    } else {
//                        Log.i(TAG, " HangOnEvent 接听电话转离线 isOnline() false");
//                    }
                    disConnectSocket();
                } else if (o instanceof HangDownEvent) {
                    Log.i(TAG, " HangDownEvent 挂断电话转在线");
//                    if (IS_ON_PHONE) {
//                        Log.i(TAG, " HangDownEvent 挂断电话转在线 IS_ON_PHONE true");
//                        IS_ON_PHONE = false;
//                        connectSocket();
//                    } else {
//                        Log.i(TAG, " HangDownEvent 挂断电话转在线 IS_ON_PHONE false");
//                    }
                    connectSocket();
                }

            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setStartForeground();
        return super.onStartCommand(intent, flags, startId);
    }

    private void setStartForeground() {
//        Notification notification = new Notification(R.mipmap.icon_launcher, getText(R.string.app_name)+"正在运行",
//                System.currentTimeMillis());
//        Intent intent = new Intent(Intent.ACTION_MAIN);
//        intent.addCategory(Intent.CATEGORY_LAUNCHER);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);


        Notification notification = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle("应用正常运行")
                .setContentText("这个通知是为了标识应用是否正常运行中")
                .setSmallIcon(R.mipmap.icon_launcher)
                .setWhen(System.currentTimeMillis())
                .build();
//           .setContentIntent(pendingIntent)
        startForeground(1, notification);

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
                params.put("socketId", socketid);
                ApiClient.getInstance().deviceRegister(this, params.toString(), registerDeviceCb);
                msg.append("client.deviceRegister call");
            } catch (JSONException e) {
                e.printStackTrace();
                msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
            }


        }
//        client.errorlog(mContext, 2, msg.toString(), respStatusCallback);


    }

    private void runOnUiThread(Runnable task) {
        mHandler.post(task);
    }

    private OkHttpBaseCallback registerDeviceCb = new OkHttpBaseCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {
            Log.d(TAG, "registerDevice 成功===");
        }
    };

    private void connectSocket() {
        Log.i(TAG, "connectSocket");
        if (isOnline()) {
            Log.i(TAG, "connectSocket SOCKET_ONLINE == true re");
            return;
        }

        try {
            LogUtils.i(TAG,WS_URL);
            mSocket = IO.socket(WS_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
//        BaseApplication app = (BaseApplication) getApplication();
//        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("LISTEN_SOCKET_ID", onUserJoined);
        mSocket.on("ON_CALL", onCall);
        mSocket.on("LISTEN_SALES_SOCKET_ID", onListenSalesSocketId);
        mSocket.on("SALES_ONLINE_WITH_STATUS_RETURN", ON_SALES_ONLINE_WITH_STATUS_RETURN);
        mSocket.on("LISTEN_TV_LEAVE_CHANNEL", ON_LISTEN_TV_LEAVE_CHANNEL);
        mSocket.on("TIMEOUT_WITHOUT_REPLY", ON_TIMEOUT_WITHOUT_REPLY);
        mSocket.connect();

    }

    private void disConnectSocket() {
        Log.i(TAG, "disConnectSocket");
        if (mSocket == null) {
            Log.i(TAG, "disConnectSocket mSocket==null return");
            return;
        }
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("user joined", onUserJoined);
        mSocket.off("ON_CALL", onCall);
        mSocket.off("LISTEN_SALES_SOCKET_ID", onListenSalesSocketId);
        mSocket.off("SALES_ONLINE_WITH_STATUS_RETURN", ON_SALES_ONLINE_WITH_STATUS_RETURN);
        mSocket.off("LISTEN_TV_LEAVE_CHANNEL", ON_LISTEN_TV_LEAVE_CHANNEL);
//        SOCKET_ONLINE = false;
        sendUserStateEvent();

    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
//            JSONObject data = (JSONObject) args;
            Log.i("wsserver", "onConnect ");
//            SOCKET_ONLINE = true;
//            Log.i(TAG, Thread.currentThread().getName());
            sendUserStateEvent();

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("salesId", Preferences.getUserId());
                mSocket.emit("SALES_ONLINE_WITH_STATUS", jsonObject);
                LogUtils.i(TAG, "emit SALES_ONLINE_WITH_STATUS salesId " + Preferences.getUserId());
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    };

    private void sendUserStateEvent() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RxBus.sendMessage(new UserStateEvent());
            }
        });
    }

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.i("wsserver", "onDisconnect diconnected");
//            SOCKET_ONLINE = false;
            sendUserStateEvent();

        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("wsserver", "onConnectError Error ");
//            SOCKET_ONLINE = false;
            sendUserStateEvent();

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
                registerDevice(socketid);
            } catch (JSONException e) {
                e.printStackTrace();
            }


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


    private Emitter.Listener onCall = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            JSONObject data = (JSONObject) args[0];
            try {
                String tvSocketId = data.getString("tvSocketId");
                String channelId = data.getString("channelId");
                JSONObject caller = data.getJSONObject("caller");
                String name = caller.getString("name");
                String address = caller.getString("address");
                Log.i("wsserver", "onCall tvSocketId ==" + tvSocketId);
                Log.i("wsserver", "onCall channelId ==" + channelId);
                OnCallActivity.actionStart(WSService.this, channelId, tvSocketId, address + " " + name);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "onCall e " + e);
            }


        }
    };

    private Emitter.Listener onListenSalesSocketId = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

            JSONObject data = (JSONObject) args[0];
            try {
                String socketId = data.getString("socketId");
                Log.i("wsserver", "onListenSalesSocketId socketId ==" + socketId);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("socketId", socketId);
                jsonObject.put("salesId", Preferences.getUserId());
                mSocket.emit("RE_CHECK_SOCKET_ID", jsonObject);
                Log.i("wsserver", "emit(RE_CHECK_SOCKET_ID, jsonObject)");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "onListenSalesSocketId e " + e);
            }


        }
    };

    private Emitter.Listener ON_SALES_ONLINE_WITH_STATUS_RETURN = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if(!(args[0] instanceof String)){
                LogUtils.e("wsserver", "ON_SALES_ONLINE_WITH_STATUS_RETURN !(args[0] instanceof String");
            }
            final String msg = (String) args[0];
            Log.i("wsserver", "ON_SALES_ONLINE_WITH_STATUS_RETURN msg ==" + msg);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    showToast("ON_SALES_ONLINE_WITH_STATUS_RETURN msg ==" + msg);
                }
            });


        }
    };

    private Emitter.Listener ON_LISTEN_TV_LEAVE_CHANNEL = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("wsserver", "ON_LISTEN_TV_LEAVE_CHANNEL ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RxBus.sendMessage(new TvLeaveChannel());
                }
            });

        }
    };

    private Emitter.Listener ON_TIMEOUT_WITHOUT_REPLY = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.i("wsserver", "ON_TIMEOUT_WITHOUT_REPLY ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RxBus.sendMessage(new TvTimeoutHangUp());
                }
            });

        }
    };


    @Override
    public void onDestroy() {
        Log.i(TAG, "life onDestroy");
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
        if (isOnline()) {
            disConnectSocket();
        }
        subscription.unsubscribe();
        mHandler.removeCallbacksAndMessages(null);
        OkHttpUtil.getInstance().cancelTag(this);
        super.onDestroy();
    }

}
