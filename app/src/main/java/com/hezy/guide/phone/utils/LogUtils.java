package com.hezy.guide.phone.utils;

import android.util.Log;

import com.hezy.guide.phone.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtils {


	private static  boolean IS_DEBUG_LOG = BuildConfig.IS_DEBUG_LOG;
	private static  boolean IS_DEBUG_TOAST = BuildConfig.IS_DEBUG_TOAST;
	public static final String lifecycle="lifecycle ";

	public static boolean isDebugToast() {
		return IS_DEBUG_TOAST;
	}

	public static void setIsDebugToast(boolean isDebugToast) {
		IS_DEBUG_TOAST = isDebugToast;
	}

	public static boolean isDebugLog() {
		return IS_DEBUG_LOG;
	}

	public static void setIsDebugLog(boolean isDebugLog) {
		IS_DEBUG_LOG = isDebugLog;
	}


	public static void v(String tag, String message) {
		if (IS_DEBUG_LOG) {
			Log.v(tag, message);
		}
	}

	public static void i(String tag, String message) {
		if (IS_DEBUG_LOG) {
			Log.i(tag, message);
		}
	}

	public static void d(String tag, String message) {
		if (IS_DEBUG_LOG) {
			Log.d(tag, message);
		}
	}

	public static void w(String tag, String message) {
		if (IS_DEBUG_LOG) {
			Log.w(tag, message);
		}
	}

	public static void e(String tag, String message) {
		if (IS_DEBUG_LOG) {
			Log.e(tag, message);
//			待解决子线程toast问题
			if(IS_DEBUG_TOAST){
				ToastUtils.showToast("Log.e "+tag+" "+message);
			}
		}

	}

	public static void e(String tag, Exception e) {
		if (IS_DEBUG_LOG) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw, true));
			String str = sw.toString();
			Log.e(tag, str);
			if(IS_DEBUG_TOAST){
				ToastUtils.showToast("Log.e "+tag+" "+str);
			}
		}
	}
}
