package com.hezy.guide.phone.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.hezy.guide.phone.persistence.Preferences;
import com.yunos.baseservice.impl.YunOSApiImpl;

import java.io.File;
import java.io.IOException;

/**
 * Created by yuna on 2017/7/24.
 */

public class UUIDUtils {
    private static String UUID = null;
    private static boolean yunosID = true;
    private static final String INSTALLATION = "INSTALLATION";

    public static final String TAG = "UUID";
    //当前获取UUID, 先尝试获取阿里云ID,如果为空在代码生成UUID

    public static String getUUID(Context context){
        if(UUID == null){
            if(TextUtils.isEmpty(Preferences.getUUID())){
                YunOSApiImpl api = new YunOSApiImpl(context);
                UUID = api.getCloudUUID();
                if(TextUtils.isEmpty(UUID) || "false".equals(UUID)){
                    UUID = Installation.id(context).replace("-","");
                    yunosID = false;
                }else {

                }
                Preferences.setUUID(UUID);
            }else {
                UUID = Preferences.getUUID();
                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    File extFile = new File(Environment.getExternalStorageDirectory(),INSTALLATION);
                    if(!extFile.exists()){
                        try {
                            Installation.writeFile(extFile,UUID);
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return UUID;

    }

    public static String getALIDeviceUUID(Context context){
        YunOSApiImpl api = new YunOSApiImpl(context);
        return api.getCloudUUID();
    }

    public static String uuid(){
        return  java.util.UUID.randomUUID().toString().replace("-","");
    }

}
