package com.hezy.guide.phone.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.OnCallActivity;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.ChatMesData;
import com.hezy.guide.phone.entities.ExpostorOnlineStats;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.CallEvent;
import com.hezy.guide.phone.event.ForumRevokeEvent;
import com.hezy.guide.phone.event.ForumSendEvent;
import com.hezy.guide.phone.event.HangDownEvent;
import com.hezy.guide.phone.event.HangOnEvent;
import com.hezy.guide.phone.event.HangUpEvent;
import com.hezy.guide.phone.event.ResolutionChangeEvent;
import com.hezy.guide.phone.event.SetUserChatEvent;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.TvLeaveChannel;
import com.hezy.guide.phone.event.TvTimeoutHangUp;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.OkHttpUtil;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.statistics.ZYAgent;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

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

    private static Socket mSocket;

    private Subscription subscription;
    private Handler mHandler;
    private static boolean PHONE_ONLINE = false;

    public static boolean isOnline() {
        return mSocket != null && mSocket.connected();
    }

    public static boolean isPhoneOnline() {
         if(mSocket != null && mSocket.connected()){
             return PHONE_ONLINE;
         }
         return false;
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
        Log.i(TAG,"onCreate");
        ZYAgent.onEvent(getApplicationContext(),"连接服务 创建");
        serviceIsCreate = true;
        mSocket = BaseApplication.getInstance().getSocket();
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
                        PHONE_ONLINE = true;
                        disconnectChatSocket();
                        handler.sendEmptyMessageDelayed(11,100);
//                        connectSocket();
//                        HashMap<String, Object> params = new HashMap<String, Object>();
//                        params.put("status", 1);
//                        ApiClient.getInstance().expostorOnlineStats(TAG, expostorStatsCallback, params);
                    } else {
                        PHONE_ONLINE = false;
                        disConnectSocket();
                        HashMap<String, Object> params = new HashMap<String, Object>();
                        params.put("onlineTraceId", onlineTraceId);
                        params.put("status", 2);
                        ApiClient.getInstance().expostorOnlineStats(TAG, expostorStatsCallback, params);
                        handler.sendEmptyMessageDelayed(12,100);
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
                    if (isOnline()) {
                        IS_ON_PHONE = true;
                        Log.i(TAG, " HangOnEvent 接听电话转离线 isOnline() true");
                        disConnectSocket();
                    } else {
                        Log.i(TAG, " HangOnEvent 接听电话转离线 isOnline() false");
                    }
                } else if (o instanceof HangDownEvent) {
                    Log.i(TAG, " HangDownEvent 挂断电话转在线");
                    if (IS_ON_PHONE) {
                        Log.i(TAG, " HangDownEvent 挂断电话转在线 IS_ON_PHONE true");
                        IS_ON_PHONE = false;
                        connectSocket();
                    } else {
                        Log.i(TAG, " HangDownEvent 挂断电话转在线 IS_ON_PHONE false");
                    }
                } else if (o instanceof ResolutionChangeEvent) {
                    ResolutionChangeEvent resolutionChangeEvent = (ResolutionChangeEvent) o;
                    resolutionChanged(resolutionChangeEvent.getResolution());
                }else if(o instanceof SetUserChatEvent){
                    SetUserChatEvent event = (SetUserChatEvent) o;
                    if (event.isOnline()) {
                        connectChatSocket();
                    } else {
                        disconnectChatSocket();
                    }
                }

            }
        });
//        if (!WSService.isOnline()) {
            //当前状态离线,可切换在线
//            ZYAgent.onEvent(mContext, "在线按钮,当前离线,切换到在线");
//            Log.i(TAG, "当前状态离线,可切换在线");
            RxBus.sendMessage(new SetUserChatEvent(true));
//        } else {
////            ZYAgent.onEvent(mContext, "在线按钮,当前在线,,无效操作");
//        }
    }

    private String onlineTraceId;

    private OkHttpCallback expostorStatsCallback = new OkHttpCallback<Bucket<ExpostorOnlineStats>>() {

        @Override
        public void onSuccess(Bucket<ExpostorOnlineStats> entity) {
            Logger.i("stats", entity.getData().toString());
            if (TextUtils.isEmpty(onlineTraceId)) {
                onlineTraceId = entity.getData().getId();
            } else {
                onlineTraceId = null;
            }
        }
    };

    private void resolutionChanged(int resolution) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("resolution", resolution + 2);
            mSocket.emit("CHANGE_RESOLUTION", jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("im_channel_id","System", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this,"im_channel_id")
//                    .setAutoCancel(true)
                    .setContentTitle("应用正常运行")
                    .setContentText("这个通知是为了标识应用是否正常运行中")
                    .setSmallIcon(R.mipmap.icon_launcher)
                    .setWhen(System.currentTimeMillis())
                    .build();
//           .setContentIntent(pendingIntent)
            startForeground(1, notification);
        } else {
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


    }


    /**
     * 上传设备信息
     */
    private void registerDevice(String socketid) {
        String uuid = Installation.id(this);
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
            Logger.e(TAG, msg.toString());
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
                msg.append("apiClient.deviceRegister call");
            } catch (JSONException e) {
                e.printStackTrace();
                msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
            } catch (SecurityException e) {
                e.printStackTrace();
                msg.append("registerDevice error jsonObject.put e.getMessage() = " + e.getMessage());
            }

        }
//        apiClient.errorlog(mContext, 2, msg.toString(), respStatusCallback);

    }

    private void updateSocketId(String socketid) {
        String uuid = Installation.id(getApplication());
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("socketId", socketid);
        ApiClient.getInstance().updateDeviceInfo(this, uuid, new OkHttpCallback<BaseErrorBean>() {
            @Override
            public void onSuccess(BaseErrorBean bucket) {
                Log.d("update socket id", bucket.toString());
                TCAgent.onEvent(BaseApplication.getInstance(),"成功更新socket id");
            }

            @Override
            public void onFailure(int errorCode, BaseException exception) {
                super.onFailure(errorCode, exception);
                TCAgent.onEvent(BaseApplication.getInstance(),"更新socket id失败");
            }
        }, JSON.toJSONString(params));
    }

    private void runOnUiThread(Runnable task) {
        mHandler.post(task);
    }

    private OkHttpCallback registerDeviceCb = new OkHttpCallback<BaseBean>() {
        @Override
        public void onSuccess(BaseBean entity) {
            Log.d(TAG, "registerDevice 成功===");
        }
    };

    private void connectChatSocket(){
        Log.i(TAG, "connectChatSocket");
        if (isOnline()) {
            Log.i(TAG, "connectChatSocket SOCKET_ONLINE == true re");
            return;
        }
        ZYAgent.onEvent(getApplicationContext(),"长连接 主动调用连接");
        Logger.i(TAG,WS_URL);

        mSocket.on(Socket.EVENT_DISCONNECT, onChatDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onChatConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onChatConnectError);
        mSocket.on("FORUM_REVOKE", ON_FORUM_REVOKE);
        mSocket.on("FORUM_SEND_CONTENT", ON_FORUM_SEND_CONTENT);
//        mSocket.on("CHANGE_RESOLUTION_EVENT", resolutionChangedListener);
        mSocket.connect();

    }

    private void disconnectChatSocket(){
        Log.i(TAG, "disconnectChatSocket");
        if (mSocket == null) {
            Log.i(TAG, "disConnectSocket mSocket==null return");
            return;
        }
        ZYAgent.onEvent(getApplicationContext(),"长连接 主动调用断开连接");
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_DISCONNECT, onChatDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onChatConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onChatConnectError);
        mSocket.off("FORUM_REVOKE", ON_FORUM_REVOKE);
        mSocket.off("FORUM_SEND_CONTENT", ON_FORUM_SEND_CONTENT);

    }
    private void connectSocket() {
        Log.i(TAG, "connectSocket");
        if (isOnline()) {
            Log.i(TAG, "connectSocket SOCKET_ONLINE == true re");
            return;
        }
        ZYAgent.onEvent(getApplicationContext(),"长连接 主动调用连接");
        Logger.i(TAG,WS_URL);
//        try {
//            LogUtils.i(TAG,WS_URL);
//            mSocket = IO.socket(WS_URL);
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
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
        mSocket.on("OLD_DISCONNECT", ON_OLD_DISCONNECT);
        mSocket.on("FORUM_REVOKE", ON_FORUM_REVOKE);
        mSocket.on("FORUM_SEND_CONTENT", ON_FORUM_SEND_CONTENT);
//        mSocket.on("CHANGE_RESOLUTION_EVENT", resolutionChangedListener);
        mSocket.connect();

    }

    private void disConnectSocket() {
        Log.i(TAG, "disConnectSocket");
        if (mSocket == null) {
            Log.i(TAG, "disConnectSocket mSocket==null return");
            return;
        }
        ZYAgent.onEvent(getApplicationContext(),"长连接 主动调用断开连接");
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
        mSocket.off("OLD_DISCONNECT", ON_OLD_DISCONNECT);
        mSocket.off("FORUM_REVOKE", ON_FORUM_REVOKE);
        mSocket.off("FORUM_SEND_CONTENT", ON_FORUM_SEND_CONTENT);
//        mSocket.off("CHANGE_RESOLUTION_EVENT", resolutionChangedListener);
//        SOCKET_ONLINE = false;
        sendUserStateEvent();

    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"连接回调");
//            JSONObject data = (JSONObject) args;
            Log.i("wsserver", "Listener onConnect ");
//            SOCKET_ONLINE = true;
//            Log.i(TAG, Thread.currentThread().getName());
            sendUserStateEvent();

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("salesId", Preferences.getUserId());
                mSocket.emit("SALES_ONLINE_WITH_STATUS", jsonObject);
                Logger.i(TAG, "emit SALES_ONLINE_WITH_STATUS salesId " + Preferences.getUserId());
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

    private Emitter.Listener onChatDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"断开连接回调");
            Log.i("wsserver", "Listener onChatDisconnect diconnected");
//            SOCKET_ONLINE = false;
//            sendUserStateEvent();

        }
    };
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 10){
                Log.i("wsserver", "延时1s去连接 ");
                connectChatSocket();
            }else if(msg.what == 11){
                connectSocket();
                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put("status", 1);
                ApiClient.getInstance().expostorOnlineStats(TAG, expostorStatsCallback, params);
            }else if(msg.what==12){
                connectChatSocket();
            }


        }
    };
    private Emitter.Listener onChatConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"断开连接回调");
            disconnectChatSocket();
            if(handler.hasMessages(10)){
                handler.removeMessages(10);
            }
            handler.sendEmptyMessageDelayed(10,10000);
//            mSocket.disconnect();
//            mSocket.connect();
            Log.i("wsserver", "Listener onChatConnectError ");
//            SOCKET_ONLINE = false;
//            sendUserStateEvent();
//            connectChatSocket();

        }
    };
    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"断开连接回调");
            Log.i("wsserver", "Listener onDisconnect diconnected");
//            SOCKET_ONLINE = false;
            sendUserStateEvent();

        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"连接错误回调");
            Log.e("wsserver", "Listener onConnectError Error ");
//            SOCKET_ONLINE = false;
            sendUserStateEvent();

        }
    };

    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"onUserJoined用户加入回调");
            JSONObject data = (JSONObject) args[0];
            String socketid = "";
            try {
                socketid = data.getString("socket_id");
                Log.i("wsserver", "Listener onUserJoined  socketid==" + socketid);
                updateSocketId(socketid);
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


//    private Emitter.Listener resolutionChangedListener = new Emitter.Listener() {
//        @Override
//        public void call(Object... args) {
//            int prefIndex = PreferenceManager.getDefaultSharedPreferences(
//                    getApplication()).getInt(ConstantApp.PrefManager.PREF_PROPERTY_PROFILE_IDX, ConstantApp.DEFAULT_PROFILE_IDX);
//            try {
//                JSONObject jsonObject = new JSONObject();
//                jsonObject.put("resolution", prefIndex + 2);
//                mSocket.emit("CHANGE_RESOLUTION", jsonObject);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
//    };

    private Emitter.Listener onCall = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"onCall呼叫回调");
            JSONObject data = (JSONObject) args[0];
            try {
                String tvSocketId = data.getString("tvSocketId");
                String channelId = data.getString("channelId");
                String deviceInfo = data.getString("deviceInfo");
                int shopOwnerResolution = data.getInt("shopOwnerResolution");

                JSONObject caller = data.getJSONObject("caller");
                String name = caller.getString("name");
                String address = caller.getString("address");
                String shopPhoto = caller.getString("shopPhoto");
                String photo = caller.getString("photo");

                OnCallActivity.actionStart(WSService.this, channelId, tvSocketId, address + " " + name, photo, deviceInfo, shopOwnerResolution, shopPhoto);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "onCall e " + e);
            }


        }
    };

    private Emitter.Listener onListenSalesSocketId = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"onListenSalesSocketId");
            JSONObject data = (JSONObject) args[0];
            try {
                String socketId = data.getString("socketId");
                Log.i("wsserver", "Listener onListenSalesSocketId socketId == " + socketId);
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("socketId", socketId);
                jsonObject.put("salesId", Preferences.getUserId());
                mSocket.emit("RE_CHECK_SOCKET_ID", jsonObject);
                Log.i("wsserver", "emit(RE_CHECK_SOCKET_ID, jsonObject)");
                updateSocketId(socketId);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "onListenSalesSocketId e " + e);
            }


        }
    };

    private Emitter.Listener ON_SALES_ONLINE_WITH_STATUS_RETURN = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"ON_SALES_ONLINE_WITH_STATUS_RETURN");
            JSONObject data = (JSONObject) args[0];
            Log.i("wsserver", "Listener ON_SALES_ONLINE_WITH_STATUS_RETURN data ==" + data);
            try {
                String msg = data.getString("message");
//                Log.i("wsserver", "Listener ON_SALES_ONLINE_WITH_STATUS_RETURN msg ==" + msg);
            }catch (JSONException e){
                e.printStackTrace();
                Log.e(TAG, "Listener ON_SALES_ONLINE_WITH_STATUS_RETURN e " + e);
            }
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
////                    showToast("ON_SALES_ONLINE_WITH_STATUS_RETURN msg ==" + msg);
//                }
//            });

        }
    };

    private Emitter.Listener ON_LISTEN_TV_LEAVE_CHANNEL = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"ON_LISTEN_TV_LEAVE_CHANNEL");
            Log.i("wsserver", "Listener ON_LISTEN_TV_LEAVE_CHANNEL ");
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
            ZYAgent.onEvent(getApplicationContext(),"ON_TIMEOUT_WITHOUT_REPLY");
            Log.i("wsserver", "Listener ON_TIMEOUT_WITHOUT_REPLY ");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    RxBus.sendMessage(new TvTimeoutHangUp());
                }
            });

        }
    };


    private Emitter.Listener ON_OLD_DISCONNECT = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"ON_OLD_DISCONNECT");
            Log.i("wsserver", "Listener ON_OLD_DISCONNECT");
            String str = (String) args[0];
            Log.i(TAG,str);


        }
    };

    private Emitter.Listener ON_FORUM_REVOKE = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"ON_OLD_DISCONNECT");
            Log.i("wsserver", "Listener ON_OLD_DISCONNECT=="+args);
            ForumRevokeEvent event = new ForumRevokeEvent();
            JSONObject json = (JSONObject) args[0];
            ChatMesData.PageDataEntity entity = new ChatMesData.PageDataEntity();
            try {
//                entity.setContent(json.getString("content"));
                entity.setId(json.getString("contentId"));
                entity.setMsgType(1);
//                entity.setReplyTimestamp(json.getLong("replyTimestamp"));
//                entity.setType(json.getInt("type"));
                entity.setUserName(json.getString("userName"));
                entity.setUserId(json.getString("userId"));
                entity.setUserLogo(json.getString("userLogo"));
                entity.setMeetingId(json.getString("meetingId"));
//                entity.setLocalState(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            event.setEntity(entity);
            RxBus.sendMessage(event);
//            String str = (String) args[0];
//            Log.i(TAG,str);


        }
    };

    private Emitter.Listener ON_FORUM_SEND_CONTENT = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ZYAgent.onEvent(getApplicationContext(),"ON_FORUM_SEND_CONTENT");
            Log.i("wsserver", "Listener ON_FORUM_SEND_CONTENT=="+args);
            ForumSendEvent event = new ForumSendEvent();
            JSONObject json = (JSONObject) args[0];
            ChatMesData.PageDataEntity entity = new ChatMesData.PageDataEntity();
            try {
                entity.setContent(json.getString("content"));
                entity.setId(json.getString("id"));
                entity.setMsgType(0);
                entity.setReplyTimestamp(json.getLong("replyTimestamp"));
                entity.setType(json.getInt("type"));
                entity.setUserName(json.getString("userName"));
                entity.setUserId(json.getString("userId"));
                entity.setUserLogo(json.getString("userLogo"));
                entity.setTs(json.getLong("ts"));
                entity.setLocalState(0);
                entity.setMeetingId(json.getString("meetingId"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            event.setEntity(entity);
            RxBus.sendMessage(event);


        }
    };

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        ZYAgent.onEvent(getApplicationContext(),"连接服务 销毁");
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知

        if (!TextUtils.isEmpty(Preferences.getToken())) {
            HashMap<String, Object> params = new HashMap<String, Object>();
            if (!TextUtils.isEmpty(onlineTraceId)) {
                params.put("onlineTraceId", onlineTraceId);
                params.put("status", 2);
                ApiClient.getInstance().expostorOnlineStats(TAG, expostorStatsCallback, params);
            }
        }
//        if (isOnline()) {
//            disConnectSocket();
//        }
        //网络不稳定非在线也要删除监听
        disConnectSocket();
        subscription.unsubscribe();
        mHandler.removeCallbacksAndMessages(null);
//        OkHttpUtil.getInstance().cancelTag(this);
        super.onDestroy();
    }

}
