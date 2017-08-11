package com.hezy.guide.phone.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Created by whatisjava on 16-12-2.
 * 在程序安装后第一次运行时生成一个ID，该方式和设备唯一标识不一样，不同的应用程序会产生不同的ID，
 * 同一个程序重新安装也会不同。可以说是用来标识每一份应用程序的唯一ID（即Installtion ID）
 * ，可以用来跟踪应用的安装数量等（其实就是UUID）。
 */

public class Installation {
    public  static final String TAG = "UUID";
    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    public synchronized static String id(Context context) {
        if (sID == null) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                //有外部SD卡情况,优先从SD卡取,并且同时存储到SD和内部存储
                Log.i(TAG,"有外部SD卡情况,优先从SD卡取,并且同时存储到SD和内部存储");
                File extFile = new File(Environment.getExternalStorageDirectory(), INSTALLATION);
                File installation = new File(context.getFilesDir(), INSTALLATION);
                try {
                    if (!extFile.exists() && !installation.exists()) {
                        //都为空的话,生成UUID.并且同时存储到SD和内部存储
                        Log.i(TAG,"都为空的话,生成UUID.并且同时存储到SD和内部存储");
                        sID = UUID.randomUUID().toString().replace("-", "");
                        writeFile(extFile, sID);
                        writeFile(installation, sID);

                    }else if(!extFile.exists() ){
                        //外部存储空,内部存储有值,从内部读取值,并写入外部存储
                        Log.i(TAG,"外部存储空,内部存储有值,从内部读取值,并写入外部存储");
                        sID = readInstallationFile(installation);
                        writeFile(extFile,sID);
                    }else if(extFile.exists() && installation.exists()){
                        //都有值的情况.从内部读取值
                        Log.i(TAG,"都有值的情况.从内部读取值");
                        sID = readInstallationFile(installation);

                    }else{
                        //只有外部存储有值时,从外部读取值,并写入内部存储
                        Log.i(TAG,"只有外部存储有值时,从外部读取值,并写入内部存储");
                        sID = readInstallationFile(extFile);
                        writeFile(installation,sID);
                    }

                } catch (Exception e) {
                    LogUtils.e(TAG,e.getMessage());
                    throw new RuntimeException(e);
                }

            } else {
                File installation = new File(context.getFilesDir(), INSTALLATION);
                try {
                    if (!installation.exists())
                        writeInstallationFile(installation);
                    sID = readInstallationFile(installation);
                } catch (Exception e) {
                    LogUtils.e(TAG,e.getMessage());
                    throw new RuntimeException(e);

                }
            }

        }
        Log.i(TAG,"sID "+sID);
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString().replace("-", "");
        out.write(id.getBytes());
        out.close();
    }

    public static void writeFile(File installation, String id) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        out.write(id.getBytes());
        out.close();
    }

    public static String getCPUSerial() {
        String str = "", strCPU = "", cpuAddress = "0000000000000000";
        try {
            // 读取CPU信息
            Process pp = Runtime. getRuntime().exec("cat/proc/cpuinfo");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            // 查找CPU序列号
            for ( int i = 1; i < 100; i++) {
                str = input.readLine();
                if (str != null) {
                    // 查找到序列号所在行
                    if (str.indexOf( "Serial") > -1) {
                        // 提取序列号
                        strCPU = str.substring(str.indexOf(":" ) + 1, str.length());
                        // 去空格
                        cpuAddress = strCPU.trim();
                        break;
                    }
                } else {
                    // 文件结尾
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }
        return cpuAddress;
    }


}
