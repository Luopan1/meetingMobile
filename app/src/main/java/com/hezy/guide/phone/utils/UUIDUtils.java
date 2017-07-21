package com.hezy.guide.phone.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.hezy.guide.phone.persistence.Preferences;

import java.io.File;
import java.io.IOException;


/**
 * Created by wufan on 2017/1/13.
 */

public class UUIDUtils {
    private static String UUID = null;
    private static boolean yunosID = true;
    private static final String INSTALLATION = "INSTALLATION";
    public static final String TAG = "UUID";

    /**
     * 当前获取UUID,先尝试获取阿里云ID,如果为空在代码生成UUID
     *
     * @param context
     * @return
     */
    public static String getUUID(Context context) {
        if (UUID == null) {
            if (TextUtils.isEmpty(Preferences.getUUID())) {
                UUID = Installation.id(context).replace("-", "");
                Preferences.setUUID(UUID);
            } else {
                UUID = Preferences.getUUID();
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    //有外部SD卡情况,优先从SD卡取,并且同时存储到SD和内部存储
                    Log.i(TAG, "UUIDUtils SP有UUID,有外部SD卡情况,");
                    File extFile = new File(Environment.getExternalStorageDirectory(), INSTALLATION);
                    if (!extFile.exists()) {
                        //为了将现在sp中的阿里云id或者uuid保存到外存储,卸载后依然有值
                        Log.i(TAG, "UUIDUtils SP有UUID,有外部SD卡情况,SD无UUID,保存到SD卡");
                        try {
                            Installation.writeFile(extFile, UUID);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }

        }
        Log.i(TAG, "UUIDUtils UUID "+UUID);
        return UUID;

    }

    public static String uuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    public static boolean isYunosID() {
        return yunosID;
    }

    public static void setYunosID(boolean yunosID) {
        UUIDUtils.yunosID = yunosID;
    }
}
