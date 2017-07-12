package com.hezy.guide.phone;

import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.OkHttpUtil;

/**
 * Created by whatisjava on 16/4/12.
 */
public class ApiClient {

    private static final String API_DOMAIN_NAME_TEST = "apitest.haierzhongyou.com";
    private static final String API_DOMAIN_NAME = "api.haierzhongyou.com";

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

    private String jointBaseUrl(String apiName) {
        if (BuildConfig.DEBUG) {
            return "http://" + API_DOMAIN_NAME_TEST + "/dz" + apiName;
        } else {
            return "http://" + API_DOMAIN_NAME + "/dz" + apiName;
        }
    }

    //  登录
    public void signIn(OkHttpCallback responseCallback) {
        okHttpUtil.get(jointBaseUrl(URL_API_USER), null, null, responseCallback);
    }


}
