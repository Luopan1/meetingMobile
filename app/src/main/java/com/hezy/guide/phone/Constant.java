package com.hezy.guide.phone;

/**
 * Created by wufan on 2017/8/2.
 */

public class Constant {

    public static final String RELOGIN_ACTION = ".ACTION.RELOGIN";

    /**
     * 检查更新地址
     */
    public static final String VERSION_UPDATE_URL = BuildConfig.API_DOMAIN_NAME_YOYOTU+"/dz/app/version/"
            + BuildConfig.APPLICATION_ID+"/android/GA/latest?versionCode=" + BuildConfig.VERSION_CODE;

    public final static int NICKNAME_MAX = 8;
    public final static int NICKNAME_MIN = 2;

    //签名限制
    public final static int SIGNATURE_MAX = 30;

    //回复限制
    public final static int REPLY_REVIEW_MAX = 1000;
}
