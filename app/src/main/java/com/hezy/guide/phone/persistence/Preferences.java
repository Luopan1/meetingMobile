package com.hezy.guide.phone.persistence;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.hezy.guide.phone.BaseApplication;


public class Preferences {
    private static final String tag = Preferences.class.getSimpleName();

    private static final String PREFERENCE_TOKEN = "token";

    private static final String PREFERENCE_USER_ID = "u_id";

    private static final String PREFERENCE_USER_NAME = "u_name";
    private static final String PREFERENCE_USER_MOBILE = "u_mobile";
    private static final String PREFERENCE_USER_ADDRESS = "u_address";
    private static final String PREFERENCE_USER_DISTRICT = "u_district";
    private static final String PREFERENCE_USER_PHOTO = "u_photo";
    private static final String PREFERENCE_USER_SIGNATURE = "u_signature";
    private static final String PREFERENCE_USER_AREA_INFO = "u_area_info";
    private static final String PREFERENCE_USER_RANK = "u_rank";

    private static final String PREFERENCE_STUDENT_ID = "s_id";

    private static final String PREFERENCE_WEIXIN_HEAD = "weixin_head";

    private static final String PREFERENCE_UUID = "uuid";

    /**
     * url前缀要持久化保存
     **/
    private static String PREFERENCE_IMG_URL = "imgUrl";
    private static String PREFERENCE_VIDEO_URL = "videoUrl";
    private static String PREFERENCE_DOWNLOAD_URL = "downloadUrl";
    private static String PREFERENCE_COOPERATION_URL = "cooperationUrl";

    public static void setStudentId(String studentId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_STUDENT_ID, studentId);
        if (!editor.commit()) {
            Log.d(tag, "student id save failure");
        } else {
            Log.d(tag, "student id save success");
        }
    }

    public static String getUserId() {
        return getPreferences().getString(PREFERENCE_USER_ID, null);
    }

    public static void setUserId(String userId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_ID, userId);
        if (!editor.commit()) {
            Log.d(tag, "User id save failure");
        } else {
            Log.d(tag, "User id save success");
        }
    }

    public static int getUserRank() {
        return getPreferences().getInt(PREFERENCE_USER_RANK, 0);
    }

    public static void setUserRank(int  rank) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(PREFERENCE_USER_RANK, rank);
        if (!editor.commit()) {
            Log.d(tag, "User rank save failure");
        } else {
            Log.d(tag, "User rank save success");
        }
    }

    public static String getUserMobile() {
        return getPreferences().getString(PREFERENCE_USER_MOBILE, "");
    }

    public static void setUserMobile(String userMobile) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_MOBILE, userMobile);
        if (!editor.commit()) {
            Log.d(tag, "User mobile save failure");
        } else {
            Log.d(tag, "User mobile save success");
        }
    }

    public static String getUserName() {
        return getPreferences().getString(PREFERENCE_USER_NAME, "");
    }

    public static void setUserName(String userName) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_NAME, userName);
        if (!editor.commit()) {
            Log.d(tag, "User name save failure");
        } else {
            Log.d(tag, "User name save success");
        }
    }

    public static String getUserAddress() {
        return getPreferences().getString(PREFERENCE_USER_ADDRESS, "");
    }

    public static void setUserAddress(String userName) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_ADDRESS, userName);
        if (!editor.commit()) {
            Log.d(tag, "User address save failure");
        } else {
            Log.d(tag, "User address save success");
        }
    }

    public static String getUserDistrict() {
        return getPreferences().getString(PREFERENCE_USER_DISTRICT, "");
    }

    public static void setUserDistrict(String district) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_DISTRICT, district);
        if (!editor.commit()) {
            Log.d(tag, "User district save failure");
        } else {
            Log.d(tag, "User address save success");
        }
    }

    public static String getUserPhoto() {
        return getPreferences().getString(PREFERENCE_USER_PHOTO, "");
    }

    public static void setUserPhoto(String userName) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_PHOTO, userName);
        if (!editor.commit()) {
            Log.d(tag, "User photo save failure");
        } else {
            Log.d(tag, "User photo save success");
        }
    }

    public static void setAreaInfo(String areaInfo){
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_AREA_INFO, areaInfo);
        if (!editor.commit()) {
            Log.d(tag, "User Address save failure");
        } else {
            Log.d(tag, "User Address save success");
        }
    }

    public static String getAreaInfo(){
        return getPreferences().getString(PREFERENCE_USER_AREA_INFO, null);
    }

    public static String getUserSignature() {
        return getPreferences().getString(PREFERENCE_USER_SIGNATURE, "");
    }

    public static void setUserSignature(String userName) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_SIGNATURE, userName);
        if (!editor.commit()) {
            Log.d(tag, "User Signature save failure");
        } else {
            Log.d(tag, "User Signature save success");
        }
    }

    public static String getToken() {
        return getPreferences().getString(PREFERENCE_TOKEN, null);
    }

    public static void setToken(String token) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_TOKEN, token);
        if (!editor.commit()) {
            Log.d(tag, "Token save failure");
        } else {
            Log.d(tag, "Token save success");
        }
    }

    public static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(BaseApplication.getInstance());
    }

    public static void clear() {
        setToken(null);
        setUserId(null);
        setStudentId(null);
        getPreferences().edit().clear().apply();
    }

    public static boolean isLogin() {
        return (!TextUtils.isEmpty(getToken()));
    }

    public static String getImgUrl() {
        return getPreferences().getString(PREFERENCE_IMG_URL, null);
    }

    public static void setImgUrl(String str) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_IMG_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setImgUrl save failure");
        } else {
            Log.d(tag, "setImgUrl save success");
        }
    }

    public static void setVideoUrl(String str) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_VIDEO_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setVideoUrl save failure");
        } else {
            Log.d(tag, "setVideoUrl save success");
        }
    }

    public static void setDownloadUrl(String str) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_DOWNLOAD_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setDownloadUrl save failure");
        } else {
            Log.d(tag, "setDownloadUrl save success");
        }
    }

    public static void setCooperationUrl(String str) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_COOPERATION_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setCooperationUrl save failure");
        } else {
            Log.d(tag, "setCooperationUrl save success");
        }
    }

    public static String getUUID() {
        return getPreferences().getString(PREFERENCE_UUID, null);
    }

    public static void setUUID(String uuid) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_UUID, uuid);
        if (!editor.commit()) {
            Log.d(tag, "UUID save failure");
        } else {
            Log.d(tag, "UUID save success");
        }
    }

    public static void setWeiXinHead(String weiXinHead) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_WEIXIN_HEAD, weiXinHead);
        if (!editor.commit()) {
            Log.d(tag, "WeiXinHead save failure");
        } else {
            Log.d(tag, "WeiXinHead save success");
        }
    }

    /**
     * 没有设置完用户信息
     * @return
     */
    public static boolean isUserinfoEmpty(){
        return (TextUtils.isEmpty(Preferences.getUserMobile()) || TextUtils.isEmpty(Preferences.getUserPhoto())
                || TextUtils.isEmpty(Preferences.getUserName()) || TextUtils.isEmpty(Preferences.getUserAddress()));
    }

}
