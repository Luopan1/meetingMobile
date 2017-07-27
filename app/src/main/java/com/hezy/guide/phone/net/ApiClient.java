package com.hezy.guide.phone.net;

import android.os.Build;
import android.text.TextUtils;

import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.UUIDUtils;
import com.tendcloud.tenddata.TCAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by whatisjava on 16/4/12.
 */
public class ApiClient {

    private static final String API_DOMAIN_NAME = BuildConfig.API_DOMAIN_NAME;

    private static final String URL_API_USER = "/liveapp/user";

    private OkHttpUtil okHttpUtil;

    public static final String HEART_URL="/state";

    private static class SingletonHolder {
        private static ApiClient instance = new ApiClient();
    }

    public static ApiClient getInstance() {
        return SingletonHolder.instance;
    }

    private ApiClient() {
        okHttpUtil = OkHttpUtil.getInstance();
    }

    private static Map<String, String> getCommonHead() {
        Map<String, String> params = new HashMap<>();
        params.put("Authorization", TextUtils.isEmpty(Preferences.getToken()) ? "" : "Token " + Preferences.getToken());
        params.put("Content-Type", "application/json; charset=UTF-8");
        params.put("DeviceUuid", UUIDUtils.getUUID(BaseApplication.getInstance()));
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


    /**
     *
     */
    public void requestWxToken(Object tag, OkHttpBaseCallback callback) {
        Map<String, String> params = new HashMap<>();
        OkHttpUtil.getInstance().get("https://api.weixin.qq.com/sns/oauth2/access_token", params, tag, callback);
    }

    /**
     * 通过微信code登录
     * @param code
     * @param state
     * @param tag
     * @param callback
     */
    public void requestWechat(String code, String state, Object tag, OkHttpBaseCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("state", state);
        OkHttpUtil.getInstance().get(API_DOMAIN_NAME + "/osg/app/wechat", params, tag, callback);
    }


    /**
     * 获取用户信息,每次启动刷新用户数据
     * @param tag
     * @param callback
     */
    public void requestUser(Object tag, OkHttpBaseCallback callback){
        OkHttpUtil.getInstance().get(API_DOMAIN_NAME + "/osg/app/user", null, tag, callback);
    }

    /**
     * 设置用户信息
     * @param tag
     * @param params
     * @param callback
     */
    public void requestUserExpostor(Object tag, Map<String, String> params, OkHttpBaseCallback callback) {
        okHttpUtil.getInstance().put(API_DOMAIN_NAME+"/osg/app/user/expostor/"+ Preferences.getUserId(),getCommonHead(),params,callback,tag);
    }


    public void requestUserExpostorState(Object tag, Map<String, String> params, OkHttpBaseCallback callback){
        okHttpUtil.getInstance().put(API_DOMAIN_NAME+"/osg/app/user/expostor/"+ Preferences.getUserId()+"/state"
                ,getCommonHead(),params,callback,tag);
    }


}
