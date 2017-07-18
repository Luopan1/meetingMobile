package com.hezy.guide.phone.entities.base;

import android.text.TextUtils;

import com.hezy.guide.phone.utils.ToastUtils;


/**http 返回200以外不解析业务bean,只解析错误数据
 * Created by wufan on 2016/12/20.
 * bean基类
 */

public class BaseErrorBean {
    private int errcode;
    private String errmsg;
    public static final  int ERRCODE_TOKEN_ERROR = 40003;

    public boolean isSuccess() {
        return errcode == 0;
    }

    public boolean isTokenError() {
        return errcode == ERRCODE_TOKEN_ERROR;
    }

    public static boolean isTokenError(int errcode) {
        return errcode == ERRCODE_TOKEN_ERROR;
    }

    public void showMsg(){
        if(!TextUtils.isEmpty(errmsg)){
            ToastUtils.showToast(errmsg);
        }else{
            ToastUtils.showToast("服务器返回错误Errmsg null");
        }
    }


    public int getErrcode() {
        return errcode;
    }

    public void setErrcode(int errcode) {
        this.errcode = errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    public void setErrmsg(String errmsg) {
        this.errmsg = errmsg;
    }

}
