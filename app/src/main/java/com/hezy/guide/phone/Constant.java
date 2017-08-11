package com.hezy.guide.phone;

/**
 * Created by wufan on 2017/8/2.
 */

public class Constant {

    /**
     * 检查更新地址
     */
    public static final String VERSION_UPDATE_URL = BuildConfig.API_DOMAIN_NAME_YOYOTU+"/dz/app/version/"
            + BuildConfig.APPLICATION_ID+"/android/GA/latest?versionCode=" + BuildConfig.VERSION_CODE;

//    public static final String VERSION_UPDATE_URL = BuildConfig.API_DOMAIN_NAME_YOYOTU+"/dz/app/version/"
//            + "com.hezy.family"+"/android/GA/latest?versionCode=" + BuildConfig.VERSION_CODE;


    //昵称限制最大15个汉字,最小2个汉字或者4个英文
    public final static int NICKNAME_MAX = 8;
    public final static int NICKNAME_MIN = 2;

    //签名限制
    public final static int SIGNATURE_MAX = 30;
}
