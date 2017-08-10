package com.hezy.guide.phone.utils.Login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.entities.User;
import com.hezy.guide.phone.entities.Wechat;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.service.WSService;
import com.hezy.guide.phone.ui.HomeActivity;
import com.hezy.guide.phone.utils.LogUtils;


/**
 * 登录,注销,退出帮助类
 * Created by wufan on 2017/1/22.
 */

public class LoginHelper {
    public static final String TAG ="LoginHelper";
    public static final String LOGIN_TYPE="login_type";
    public static final int LOGIN_TYPE_EXIT=1;
    public static final int LOGIN_TYPE_LOGOUT=2;
    public static  boolean mIsLogout=false;


    /**
     * 非mainTabActivity页面调用
     * @param activity
     */
    public static void exit(Activity activity) {
        Intent intent;
        intent = new Intent(activity, HomeActivity.class);
        intent.putExtra(LOGIN_TYPE, LOGIN_TYPE_EXIT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }


    /**
     * 退出登录-退出到主页activity
     * @param activity
     */
    public static void logout(Activity activity) {
        LogUtils.d(TAG,activity.getClass().getSimpleName());
//        if(activity instanceof MainTabActivity){
//
//        }else{
//
//        }
        logoutCustom(activity);

        Intent intent;
        intent = new Intent(activity, HomeActivity.class);
        intent.putExtra(LOGIN_TYPE, LOGIN_TYPE_LOGOUT);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
//            activity.finish(); //不需要调用finish.clear_top flag会把当前activity finish.

    }


    /**
     * 退出登录-全局调用
     */
    public static void logout(){
        if(Preferences.isLogin()){
            Activity currentActivity = BaseApplication.getInstance().getCurrentActivity();
            if(currentActivity !=null){
                LoginHelper.logout(currentActivity);
            }else{
                //应用不再前台,延迟到应用到前台处理
                LoginHelper.mIsLogout=true;
            }

        }
    }


    /**
     * 退出登录-本应用自己业务相关,本地用户数据,服务
     * @param context
     */
    public static void logoutCustom(Context context) {
        Preferences.clear();
//        HeartService.stopService(context);
        WSService.stopService(context);
    }

    public static void savaUser(User user){
        Preferences.setToken(user.getToken());
        Preferences.setUserId(user.getId());
        Preferences.setUserName(user.getName());
        Preferences.setUserMobile(user.getMobile());
        Preferences.setUserAddress(user.getAddress());
        Preferences.setUserPhoto(user.getPhoto());
        Preferences.setUserSignature(user.getSignature());
    }

    public static void savaWeChat(Wechat wechat){
        Preferences.setWeiXinHead(wechat.getHeadimgurl());
    }

}
