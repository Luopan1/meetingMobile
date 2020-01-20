package com.zhongyou.meet.mobile;

/**
 * Created by wufan on 2017/8/2.
 */

public class Constant {

	public static String APIHOSTURL = "http://osg.apitest.zhongyouie.cn";
	public static String WEBSOCKETURL = "";
	public static String DOWNLOADURL = "";
	public static final String RELOGIN_ACTION = ".ACTION.RELOGIN";


	public static final String MODEL_CHANGE = "model_change";
	public static final String EQUALLY = "equally";
	public static final String BIGSCREEN = "bigScreen";

	public static final String VIDEO="video";
	public static final String PLAYVIDEO="playVideo";
	public static final String PAUSEVIDEO="pauseVideo";
	public static final String STOPVIDEO="stopVideo";

	public static boolean isChairMan = false;
	static boolean debug = BuildConfig.DEBUG;

	public final static int delayTime=5000;

	//0 为主持人 1 为参会人  2为观众 默认为观众
	public static int videoType = 2;
	public static boolean isNeedRecord=false;

	/**
	 * 检查更新地址
	 */
   /* public static final String VERSION_UPDATE_URL = DOWNLOADURL+"/dz/app/version/"
            + BuildConfig.APPLICATION_ID+"/android/GA/latest?versionCode=" + BuildConfig.VERSION_CODE;*/

	public final static int NICKNAME_MAX = 8;
	public final static int NICKNAME_MIN = 2;

	//签名限制
	public final static int SIGNATURE_MAX = 30;

	//回复限制
	public final static int REPLY_REVIEW_MAX = 1000;


	public static String getAPIHOSTURL() {

		if (debug) {
			APIHOSTURL = "http://osg.apitest.zhongyouie.cn";

		} else {
			APIHOSTURL = "http://api.zhongyouie.com";
		}
		return APIHOSTURL;
	}

	public static String getImageHost() {
		if (debug) {
			return "http://syimage.zhongyouie.com/";
		} else {
			return "http://image.zhongyouie.com/";
		}
	}

	public static String getWEBSOCKETURL() {
		if (BuildConfig.DEBUG) {
			WEBSOCKETURL = "http://wstest.zhongyouie.cn/sales";
		} else {
			WEBSOCKETURL = "http://ws.zhongyouie.com/sales";
		}
		return WEBSOCKETURL;
	}

	public static String getDOWNLOADURL() {
		if (BuildConfig.DEBUG) {
			DOWNLOADURL = "http://tapi.zhongyouie.cn";
		} else {
			DOWNLOADURL = "http://api.zhongyouie.cn";
		}
		return DOWNLOADURL;
	}

}
