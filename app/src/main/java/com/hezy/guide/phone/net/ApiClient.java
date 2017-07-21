package com.hezy.guide.phone.net;

import com.hezy.guide.phone.BuildConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by whatisjava on 16/4/12.
 */
public class ApiClient {

    private static final String API_DOMAIN_NAME = BuildConfig.API_DOMAIN_NAME;

    private static final String URL_API_USER = "/liveapp/user";

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


    /**
     *
     */
    public void requestWxToken(Object tag, OkHttpBaseCallback callback) {
        Map<String, String> params = new HashMap<>();
        OkHttpUtil.getInstance().get("https://api.weixin.qq.com/sns/oauth2/access_token", params, tag, callback);
    }

    /**
     *
     */
    public void requestWechat(String code, String state, Object tag, OkHttpBaseCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("state", state);
        OkHttpUtil.getInstance().get(API_DOMAIN_NAME + "/osg/app/wechat",params, tag, callback);
    }


}
