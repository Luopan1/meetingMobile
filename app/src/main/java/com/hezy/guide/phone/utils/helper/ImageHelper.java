package com.hezy.guide.phone.utils.helper;

import android.content.Context;
import android.util.Log;

import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.persistence.Preferences;

/**
 * Created by wufan on 2017/7/26.
 */

public class ImageHelper {
    public static final String TAG = "ImageHelper";
    public static boolean isContextEmpty() {
        if (BaseApplication.getInstance() == null) {
            Log.e(TAG, "BaseApplication.getInstance()==null");
            return true;
        } else {
            return false;
        }

    }

    public static Context getContext() {
        return BaseApplication.getInstance();
    }


    public static String getUrlJoin(String url) {
        if (url != null){
            if (url.contains("http")) {
                return url;
            } else {
                return Preferences.getImgUrl() + url;
            }
        } else {
            return null;
        }
    }

    /**
     * 获得拼接host和缩放thum的url
     *
     * @param url
     * @param w
     * @param h
     * @return
     */
    public static String getUrlJoinAndThum(String url, int w, int h) {
        url = getUrlJoin(url);
        if (w < 0 || w > 1920 || h < 0 || h > 1080) {
            return url;
        } else {
            url = url + "?imageMogr2/auto-orient/thumbnail/" + w + "x" + h + "/interlace/" + 1;
            return url;
        }
    }
}
