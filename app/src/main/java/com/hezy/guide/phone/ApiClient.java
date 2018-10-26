package com.hezy.guide.phone;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.hezy.guide.phone.entities.LoginWithPhoneNumber;
import com.hezy.guide.phone.entities.RankInfo;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.RecordTotal;
import com.hezy.guide.phone.entities.StaticRes;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.UserData;
import com.hezy.guide.phone.entities.Version;
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Installation;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.OkHttpUtil;
import com.hezy.guide.phone.utils.RxBus;
import com.tendcloud.tenddata.TCAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;
import static com.hezy.guide.phone.utils.ToastUtils.showToast;

/**
 * Created by whatisjava on 16/4/12.
 */
public class ApiClient {

    private static final String API_DOMAIN_NAME = BuildConfig.API_DOMAIN_NAME;
    private static final String API_DOMAIN_NAME_YOYOTU = BuildConfig.API_DOMAIN_NAME_YOYOTU;

    private OkHttpUtil okHttpUtil;

    private static class SingletonHolder {
        private static ApiClient instance = new ApiClient();
    }

    public static ApiClient getInstance() {
        return SingletonHolder.instance;
    }

    private ApiClient() {
        okHttpUtil = OkHttpUtil.getInstance();
    }

    public static String jointParamsToUrl(String url, Map<String, String> params) {
        if (params != null && params.size() > 0) {
            Uri uri = Uri.parse(url);
            Uri.Builder b = uri.buildUpon();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                b.appendQueryParameter(entry.getKey(), entry.getValue());
            }
            return b.build().toString();
        }
        return url;
    }

    private static Map<String, String> getCommonHead() {
        Map<String, String> params = new HashMap<>();
        params.put("Content-Type", "application/json; charset=UTF-8");
        if (!TextUtils.isEmpty(Preferences.getToken())) {
            params.put("Authorization", "Token " + Preferences.getToken());
        }
        if (!TextUtils.isEmpty(Installation.id(BaseApplication.getInstance()))) {
            params.put("DeviceUuid", Installation.id(BaseApplication.getInstance()));
        }
        params.put("User-Agent", "HRZY_HOME"
                + "_"
                + BuildConfig.APPLICATION_ID
                + "_"
                + BuildConfig.VERSION_NAME
                + "_"
                + TCAgent.getDeviceId(BaseApplication.getInstance()) + "(android_OS_"
                + Build.VERSION.RELEASE + ";" + Build.MANUFACTURER
                + "_" + Build.MODEL + ")");

        return params;
    }

    public void expostorOnlineStats(Object tag, OkHttpCallback callback, Map<String, Object> values) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/user/expostor/online/stats", getCommonHead(), JSON.toJSONString(values), callback, tag);
    }

    public void meetingJoinStats(Object tag, OkHttpCallback callback, Map<String, Object> values) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/meeting/join/stats", getCommonHead(), JSON.toJSONString(values), callback, tag);
    }

    public void meetingHostStats(Object tag, OkHttpCallback callback, Map<String, Object> values) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/meeting/host/stats", getCommonHead(), JSON.toJSONString(values), callback, tag);
    }

    public void searchMeeting(Object tag, String meetingName, OkHttpCallback callback) {
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/meeting/all?title=" + meetingName, getCommonHead(), null, callback);
    }

    public void getAllMeeting(Object tag, String meetingName, OkHttpCallback callback) {
        if (TextUtils.isEmpty(meetingName)) {
            okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/meeting/list", getCommonHead(), null, callback);
        } else {
            okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/meeting/list?title=" + meetingName, getCommonHead(), null, callback);
        }
    }

    public void verifyRole(Object tag, OkHttpCallback callback, Map<String, Object> values) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/meeting/verify", getCommonHead(), JSON.toJSONString(values), callback, tag);
    }

    public void joinMeeting(Object tag, OkHttpCallback callback, Map<String, Object> values) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/meeting/join", getCommonHead(), JSON.toJSONString(values), callback, tag);
    }

    public void getMeeting(Object tag, String meetingId, OkHttpCallback callback) {
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/meeting/" + meetingId, getCommonHead(), null, callback);
    }

    public void finishMeeting(Object tag, String meetingId, int attendance, OkHttpCallback callback) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/meeting/" + meetingId + "/end?attendance=" + attendance, getCommonHead(), null, callback, tag);
    }

    //  软件更新
    public void versionCheck(Object tag, OkHttpCallback<BaseBean<Version>> callback) {
        okHttpUtil.get(Constant.VERSION_UPDATE_URL, tag, callback);
    }

    //获取全局配置信息接口
    public void urlConfig(OkHttpCallback<BaseBean<StaticRes>> callback) {
        okHttpUtil.get(API_DOMAIN_NAME_YOYOTU + "/dz/app/config", null, null, callback);
    }

    //获取大区（中心）接口
    public void districts(OkHttpCallback callback, String parentId) {
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/user/district?parentId=" + parentId, getCommonHead(), null, callback);
    }

    //  注册设备信息
    public void deviceRegister(Object tag, String jsonStr, OkHttpCallback callback) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/device", getCommonHead(), jsonStr, callback, tag);
    }

    /**
     * 收到客户退出通知或者主动退出房间时请求
     *
     * @param recordId
     * @param responseCallback
     * @return
     */
    public void startOrStopOrRejectCallExpostor(String recordId, String state, OkHttpCallback responseCallback) {
        okHttpUtil.put(API_DOMAIN_NAME + "/osg/app/call/record/" + recordId + "?state=" + state, getCommonHead(), null, responseCallback);
    }

    /**
     * 更新设备信息，目前只有socketid的更新
     *
     * @param tag
     * @param deviceId
     * @param responseCallback
     * @param jsonStr
     */
    public void updateDeviceInfo(Object tag, String deviceId, OkHttpCallback responseCallback, String jsonStr) {
        okHttpUtil.putJson(API_DOMAIN_NAME + "/osg/app/device/" + deviceId, getCommonHead(), jsonStr, responseCallback, tag);
    }

    /**
     * 获取声网参数
     *
     * @param params
     * @param responseCallback
     */
    public void getAgoraKey(Object tag, Map<String, String> params, OkHttpCallback responseCallback) {
        okHttpUtil.get(jointParamsToUrl(API_DOMAIN_NAME_YOYOTU + "/dz/agora/key/osgV2", params), tag, responseCallback);
    }

    /**
     *
     */
    public void requestWxToken(Object tag, OkHttpCallback callback) {
        Map<String, String> params = new HashMap<>();
        OkHttpUtil.getInstance().get("https://api.weixin.qq.com/sns/oauth2/access_token", params, tag, callback);
    }

    /**
     * 通过微信code登录
     *
     * @param code
     * @param state
     * @param tag
     * @param callback
     */
    public void requestWechat(String code, String state, Object tag, OkHttpCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("state", state);
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/wechat", params, tag, callback);
    }

    /**
     * 获取用户信息,每次启动刷新用户数据
     *
     * @param tag
     * @param callback
     */
    public void requestUser(Object tag, OkHttpCallback<BaseBean<UserData>> callback) {
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/user", getCommonHead(), null, callback, tag);
    }

    public void requestUser() {
        if (!Preferences.isLogin()) {
            return;
        }
        ApiClient.getInstance().requestUser(this, new OkHttpCallback<BaseBean<UserData>>() {
            @Override
            public void onSuccess(BaseBean<UserData> entity) {
                if (entity == null || entity.getData() == null || entity.getData().getUser() == null) {
                    showToast("数据为空");
                    return;
                }
                User user = entity.getData().getUser();
                Wechat wechat = entity.getData().getWechat();
                LoginHelper.savaUser(user);
//                initCurrentItem();
                if (wechat != null) {
                    LoginHelper.savaWeChat(wechat);
                }
                RxBus.sendMessage(new UserUpdateEvent());

            }
        });
    }

    /**
     * 设置用户信息
     *
     * @param tag
     * @param callback
     */
    public void requestUserExpostor(Object tag, Map<String, String> params, OkHttpCallback callback) {
        okHttpUtil.putJson(API_DOMAIN_NAME + "/osg/app/user/expostor/" + Preferences.getUserId(), getCommonHead(),
                OkHttpUtil.getGson().toJson(params), callback, tag);
    }

    /**
     * 心跳加在线状态
     *
     * @param tag
     * @param params
     * @param callback
     */
    public void requestUserExpostorState(Object tag, Map<String, String> params, OkHttpCallback callback) {
        okHttpUtil.putJson(API_DOMAIN_NAME + "/osg/app/user/expostor/" + Preferences.getUserId() + "/state"
                , getCommonHead(), OkHttpUtil.getGson().toJson(params), callback, tag);
    }

    /**
     * 获取七牛图片上传token
     *
     * @param tag
     * @param callback
     */
    public void requestQiniuToken(Object tag, OkHttpCallback callback) {
        //使用唷唷兔地址
        okHttpUtil.get(API_DOMAIN_NAME_YOYOTU + "/dz/resource/uploadtoken/image", tag, callback);
    }

    /**
     * 获取导购通话时间
     *
     * @param tag
     * @param callback
     */
    public void requestRecordTotal(Object tag, OkHttpCallback<BaseBean<RecordTotal>> callback) {
        //使用唷唷兔地址
        String userId = Preferences.getUserId();
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/call/expostor/" + userId + "/record/total", getCommonHead(), null, callback, tag);
    }

    /**
     * 获取导购通话记录
     *
     * @param tag
     * @param callback
     */
    public void requestRecord(Object tag, String starFilter, String pageNo, String pageSize, OkHttpCallback<BaseBean<RecordData>> callback) {
        //使用唷唷兔地址
        String userId = Preferences.getUserId();
        Map<String, String> params = new HashMap<>();
        if (!TextUtils.isEmpty(starFilter)) {
            params.put("starFilter", "1");
        }
        params.put("pageNo", pageNo);
        params.put("pageSize", pageSize);
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/call/expostor/" + userId + "/record", getCommonHead(), params, callback, tag);
    }

    /**
     * 获得手机验证码
     *
     * @param tag
     * @param mobile
     */
    public void requestVerifyCode(Object tag, String mobile, OkHttpCallback<BaseErrorBean> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("mobile", mobile);
            okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/user/verifyCode", getCommonHead(), jsonObject.toString(), callback, tag);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

    }

    /**
     * 获取导购员评分
     *
     * @param tag
     * @param callback
     */
    public void requestRankInfo(Object tag, OkHttpCallback<BaseBean<RankInfo>> callback) {
        //使用唷唷兔地址
        String userId = Preferences.getUserId();
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/call/rankInfo/" + userId, getCommonHead(), null, callback, tag);
    }

    public void requestReplayComment(Object tag, String callRecordId, String replyRating, OkHttpCallback<BaseErrorBean> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("callRecordId", callRecordId);
            jsonObject.put("replyRating", replyRating);
            okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/call/replyComment", getCommonHead(), jsonObject.toString(), callback, tag);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

    }

    /**
     * 手机号携带验证码进行用户信息校验
     *
     * @param tag
     * @param params
     * @param callback
     */
    public void requestLoginWithPhoneNumber(Object tag, Map<String, String> params, OkHttpCallback callback) {
        okHttpUtil.postJson(API_DOMAIN_NAME + "/osg/app/user/mobile/auth", getCommonHead(), JSON.toJSONString(params), callback, tag);
    }

    /**
     * 获取用户类型接口
     *
     * @param tag
     * @param callback
     */
    public void requestPostType(Object tag, OkHttpCallback callback) {
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/user/post/type", tag, callback);
    }

    /**
     * 获取用户网格接口
     *
     * @param tag
     * @param params
     * @param callback
     */
    public void requestGrid(Object tag, Map<String, String> params, OkHttpCallback callback) {
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/user/area/grid", params, tag, callback);
    }

    /**
     * 获取用户接口
     *
     * @param tag
     * @param params
     * @param callback
     */
    public void requestCustom(Object tag, Map<String, String> params, OkHttpCallback callback) {
        okHttpUtil.get(API_DOMAIN_NAME + "/osg/app/user/area/custom", params, tag, callback);
    }
}
