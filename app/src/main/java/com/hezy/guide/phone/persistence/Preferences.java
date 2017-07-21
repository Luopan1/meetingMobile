package com.hezy.guide.phone.persistence;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.utils.LogUtils;


public class Preferences {
    private static final String tag = Preferences.class.getSimpleName();

    private static final String PREFERENCE_IS_FIRST = "is_first";

    private static final String PREFERENCE_TOKEN = "token";

    private static final String PREFERENCE_USER_ID = "u_id";

    private static final String PREFERENCE_USER_ROLE_TYPE = "u_role_type";

    private static final String PREFERENCE_BABY_SHOW = "baby_show_first";
    /**
     * zyParentId
     */
    private static final String PREFERENCE_USER_ZY_ID = "u_zy_id";
    private static final String PREFERENCE_USER_NAME = "u_name";
    private static final String PREFERENCE_USER_MOBILE = "u_mobile";
    private static final String PREFERENCE_USER_SOURCE = "u_source";

    private static final String PREFERENCE_CLASS_ID = "c_id";
    private static final String PREFERENCE_CLASS_NAME = "c_name";
    private static final String PREFERENCE_CLASS_IS_DEMO_CLASS = "c_is_demo_CLASS";
    private static final String PREFERENCE_SCHOOL_NAME = "school_name";

    private static final String PREFERENCE_STUDENT_ID = "s_id";
    private static final String PREFERENCE_STUDENT_NAME = "s_name";
    private static final String PREFERENCE_STUDENT_SEX = "s_sex";
    private static final String PREFERENCE_STUDENT_HEAD = "s_head";
    private static final String PREFERENCE_STUDENT_TYPE = "s_type";
    private static final String PREFERENCE_STUDENT_AGE = "s_age";
    private static final String PREFERENCE_STUDENT_BIRTHDAY = "s_birthday";

    //weixin
    private static final String PREFERENCE_WEIXIN_NAME = "wenxin_name";
    private static final String PREFERENCE_WEIXIN_HEAD = "weixin_head";

    private static final String PREFERENCE_UUID = "uuid";

    /**
     * url前缀要持久化保存
     **/
    private static String PREFERENCE_IMG_URL = "imgUrl";
    private static String PREFERENCE_VIDEO_URL = "videoUrl";
    private static String PREFERENCE_DOWNLOAD_URL = "downloadUrl";
    private static String PREFERENCE_COOPERATION_URL = "cooperationUrl";

    private static String imgUrl;
    private static String videoUrl;
    private static String downloadUrl;
    private static String cooperationUrl;

    private static String PREFERENCE_IS_LOG = "isLog";


    private static final String PREFERENCE_JID = "j";
    private static final String PREFERENCE_TASK_FINISH_DATE = "PREFERENCE_TASK_FINISH_DATE";

    public static String getStudentBirthday() {
        return getPreferences().getString(PREFERENCE_STUDENT_BIRTHDAY, null);
    }

    public static void setStudentBirthday(String studentBirthday) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_STUDENT_BIRTHDAY, studentBirthday);
        if (!editor.commit()) {
            Log.d(tag, "student birthday save failure");
        } else {
            Log.d(tag, "student birthday save success");
        }
    }

    public static int getStudentAge() {
        return getPreferences().getInt(PREFERENCE_STUDENT_AGE, 0);
    }

    public static void setStudentAge(int studentAge) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(PREFERENCE_STUDENT_AGE, studentAge);
        if (!editor.commit()) {
            Log.d(tag, "student age save failure");
        } else {
            Log.d(tag, "student age save success");
        }
    }

    public static String getStudentType() {
        return getPreferences().getString(PREFERENCE_STUDENT_TYPE, null);
    }

    public static void setStudentType(String studentType) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_STUDENT_TYPE, studentType);
        if (!editor.commit()) {
            Log.d(tag, "student type save failure");
        } else {
            Log.d(tag, "student type save success");
        }
    }

    public static void setPreferenceBabyShow(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences("babyshow", 0).edit();
        editor.putBoolean(PREFERENCE_BABY_SHOW, true);
        if (!editor.commit()) {
            Log.d(tag, "Token save failure");
        } else {
            Log.d(tag, "Token save success");
        }
    }

    public static boolean getBabyShow(Context context) {
        return context.getSharedPreferences("babyshow", 0).getBoolean(PREFERENCE_BABY_SHOW, false);
    }

    public static String getStudentHead() {
        return getPreferences().getString(PREFERENCE_STUDENT_HEAD, null);
    }

    public static void setStudentHead(String studentId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_STUDENT_HEAD, studentId);
        if (!editor.commit()) {
            Log.d(tag, "student head save failure");
        } else {
            Log.d(tag, "student head save success");
        }
    }

    public static String getStudentSex() {
        return getPreferences().getString(PREFERENCE_STUDENT_SEX, null);
    }

    public static void setStudentSex(String studentId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_STUDENT_SEX, studentId);
        if (!editor.commit()) {
            Log.d(tag, "student name save failure");
        } else {
            Log.d(tag, "student name save success");
        }
    }

    public static String getStudentName() {
        String student_name = getPreferences().getString(PREFERENCE_STUDENT_NAME, null);
        if (TextUtils.isEmpty(student_name)) {
            student_name = "未填写";
        }
        return student_name;
    }

    public static void setStudentName(String studentId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_STUDENT_NAME, studentId);
        if (!editor.commit()) {
            Log.d(tag, "student name save failure");
        } else {
            Log.d(tag, "student name save success");
        }
    }

    public static String getStudentId() {
        return getPreferences().getString(PREFERENCE_STUDENT_ID, null);
    }

    public static void setStudentId(String studentId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_STUDENT_ID, studentId);
        if (!editor.commit()) {
            Log.d(tag, "student id save failure");
        } else {
            Log.d(tag, "student id save success");
        }
    }

    public static String getSchoolName() {
        return getPreferences().getString(PREFERENCE_SCHOOL_NAME, null);
    }

    public static void setSchoolName(String schoolName) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_SCHOOL_NAME, schoolName);
        if (!editor.commit()) {
            Log.d(tag, "class name save failure");
        } else {
            Log.d(tag, "class name save success");
        }
    }

    public static String getClassId() {
        return getPreferences().getString(PREFERENCE_CLASS_ID, null);
    }

    public static void setClassId(String classId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_CLASS_ID, classId);
        if (!editor.commit()) {
            Log.d(tag, "class id save failure");
        } else {
            Log.d(tag, "class id save success");
        }
    }

    public static String getClassName() {
        return getPreferences().getString(PREFERENCE_CLASS_NAME, "");
    }

    public static void setClassName(String classId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_CLASS_NAME, classId);
        if (!editor.commit()) {
            Log.d(tag, "class name save failure");
        } else {
            Log.d(tag, "class name save success");
        }
    }

    public static boolean getClassIsDemoClass() {
        return getPreferences().getBoolean(PREFERENCE_CLASS_IS_DEMO_CLASS, false);
    }

    public static void setClassIsDemoClass(boolean isDemo) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putBoolean(PREFERENCE_CLASS_IS_DEMO_CLASS, isDemo);
        if (!editor.commit()) {
            Log.d(tag, "class id save failure");
        } else {
            Log.d(tag, "class id save success");
        }
    }


    public static String getUserSource() {
        return getPreferences().getString(PREFERENCE_USER_SOURCE, null);
    }

    public static void setUserSource(String userSource) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_SOURCE, userSource);
        if (!editor.commit()) {
            Log.d(tag, "User mobile save failure");
        } else {
            Log.d(tag, "User mobile save success");
        }
    }

    public static String getUserMobile() {
        return getPreferences().getString(PREFERENCE_USER_MOBILE, null);
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
        return getPreferences().getString(PREFERENCE_USER_NAME, null);
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

    public static int getUserRoleType() {
        return getPreferences().getInt(PREFERENCE_USER_ROLE_TYPE, 1);
    }

    public static void setUserRoleType(int roleType) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(PREFERENCE_USER_ROLE_TYPE, roleType);
        if (!editor.commit()) {
            Log.d(tag, "setUserRoleType save failure");
        } else {
            Log.d(tag, "setUserRoleType save success");
        }
    }

    public static String getUserZYId() {
        return getPreferences().getString(PREFERENCE_USER_ZY_ID, null);
    }

    public static void setUserZYId(String userId) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_USER_ZY_ID, userId);
        if (!editor.commit()) {
            Log.d(tag, "User id save failure");
        } else {
            Log.d(tag, "User id save success");
        }
    }


    public static String getJId() {
        return getPreferences().getString(PREFERENCE_JID, null);
    }

    public static void setJId(String jid) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_JID, jid);
        if (!editor.commit()) {
            Log.d(tag, "jid save failure");
        } else {
            Log.d(tag, "jid save success");
        }
    }

    public static boolean isFirst() {
        return getPreferences().getBoolean(PREFERENCE_IS_FIRST, false);
    }

    public static void setIsFirst(boolean isFrist) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putBoolean(PREFERENCE_IS_FIRST, isFrist);
        if (!editor.commit()) {
            Log.d(tag, "isFrist save failure");
        } else {
            Log.d(tag, "isFrist save success");
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
        return (!TextUtils.isEmpty(getUserId())) && (!TextUtils.isEmpty(getToken()));
    }

    public static boolean hasClass() {
        return !TextUtils.isEmpty(getClassId());
    }


    public static String getImgUrl() {
        if (!TextUtils.isEmpty(imgUrl)) {
            return imgUrl;
        } else {
            return getPreferences().getString(PREFERENCE_IMG_URL, null);
        }
    }

    public static void setImgUrl(String str) {
        imgUrl = str;
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_IMG_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setImgUrl save failure");
        } else {
            Log.d(tag, "setImgUrl save success");
        }
    }

    public static String getVideoUrl() {
        if (!TextUtils.isEmpty(imgUrl)) {
            return videoUrl;
        } else {
            return getPreferences().getString(PREFERENCE_VIDEO_URL, null);
        }
    }

    public static void setVideoUrl(String str) {
        videoUrl = str;
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_VIDEO_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setVideoUrl save failure");
        } else {
            Log.d(tag, "setVideoUrl save success");
        }
    }

    public static String getDownloadUrl() {
        if (!TextUtils.isEmpty(downloadUrl)) {
            return downloadUrl;

        } else {
            return getPreferences().getString(PREFERENCE_DOWNLOAD_URL, null);
        }

    }

    public static void setDownloadUrl(String str) {
        downloadUrl = str;
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_DOWNLOAD_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setDownloadUrl save failure");
        } else {
            Log.d(tag, "setDownloadUrl save success");
        }
    }

    public static String getCooperationUrl() {
        if (!TextUtils.isEmpty(cooperationUrl)) {
            return cooperationUrl;
        } else {
            return getPreferences().getString(PREFERENCE_COOPERATION_URL, null);
        }

    }

    public static void setCooperationUrl(String str) {
        cooperationUrl = str;
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_COOPERATION_URL, str);
        if (!editor.commit()) {
            Log.d(tag, "setCooperationUrl save failure");
        } else {
            Log.d(tag, "setCooperationUrl save success");
        }
    }

   


    public static void initLog() {
        if (getPreferences().contains(PREFERENCE_IS_LOG)) {
            boolean isLog = getPreferences().getBoolean(PREFERENCE_IS_LOG, false);
            LogUtils.setIsDebugLog(isLog);
            LogUtils.setIsDebugLog(isLog);
        }

    }

    public static void setIsLog(boolean isLog) {
        LogUtils.setIsDebugLog(isLog);
        LogUtils.setIsDebugToast(isLog);
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putBoolean(PREFERENCE_IS_LOG, isLog);
        if (!editor.commit()) {
            Log.d(tag, "setIsLog save failure");
        } else {
            Log.d(tag, "setIsLog save success");
        }
    }


    public static String getUUID() {
        return getPreferences().getString(PREFERENCE_UUID, null);
    }

    public static void setUUID(String uuid) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_UUID, uuid);
        if (!editor.commit()) {
            Log.d(tag, "User name save failure");
        } else {
            Log.d(tag, "User name save success");
        }
    }

    public static void setWeiXinName(String weiXinName) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_WEIXIN_NAME, weiXinName);
        if (!editor.commit()) {
            Log.d(tag, "User mobile save failure");
        } else {
            Log.d(tag, "User mobile save success");
        }
    }

    public static void setWeiXinHead(String weiXinHead) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_WEIXIN_HEAD, weiXinHead);
        if (!editor.commit()) {
            Log.d(tag, "User mobile save failure");
        } else {
            Log.d(tag, "User mobile save success");
        }
    }

    public static String getWeiXinHead() {
        return getPreferences().getString(PREFERENCE_WEIXIN_HEAD, null);
    }

    public static String getWeiXinName() {
        return getPreferences().getString(PREFERENCE_WEIXIN_NAME, null);
    }

    public static String getPreferenceTaskFinishDate(String studentId){
        return getPreferences().getString(PREFERENCE_TASK_FINISH_DATE+studentId, "");
    }

    public static void setPreferenceTaskFinishDate(String studentId,String str) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(PREFERENCE_TASK_FINISH_DATE+studentId, str);
        if (!editor.commit()) {
            Log.d(tag, "User mobile save failure");
        } else {
            Log.d(tag, "User mobile save success");
        }
    }

}
