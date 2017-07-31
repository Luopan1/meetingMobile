package com.hezy.guide.phone.utils.helper;

import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.CropOptions;

/**
 * Created by ruoyun on 2016/10/19.
 */

public class TakePhotoHelper {

    public static void configCompress(TakePhoto takePhoto) {
        int maxSize = 102400;//大小
        //        int maxPixel = 300;//压缩大小
        CompressConfig config = new CompressConfig.Builder().setMaxPixel(maxSize).create();
        takePhoto.onEnableCompress(config, true);
    }

//    public static CropOptions getCropOptions() {//是否裁剪
//        int height = 800;
//        int width = 800;
//        CropOptions.Builder builder = new CropOptions.Builder();
//        builder.setAspectX(width).setAspectY(height);
//        builder.setWithOwnCrop(true);//使用第三方裁剪库
//        return builder.create();
//    }

    public static CropOptions getCropOptions() {
        int height = 800;
        int width = 800;
        CropOptions.Builder builder = new CropOptions.Builder();
        builder.setAspectX(width).setAspectY(height);
        builder.setWithOwnCrop(true);
        return builder.create();
    }

}
