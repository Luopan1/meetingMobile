package com.hezy.guide.phone.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hezy.guide.phone.BuildConfig;
import com.hezy.guide.phone.Constant;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BasisActivity;
import com.hezy.guide.phone.entities.Version;
import com.hezy.guide.phone.net.download.DownloadInfo;
import com.hezy.guide.phone.net.download.DownloadManager;
import com.hezy.guide.phone.net.download.DownloadService;
import com.hezy.guide.phone.utils.ApkController;
import com.hezy.guide.phone.utils.ApkUtil;
import com.hezy.guide.phone.utils.AppUtil;
import com.hezy.guide.phone.utils.CheckNetWorkStatus;
import com.hezy.guide.phone.utils.DeviceUtil;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.Login.LoginHelper;
import com.hezy.guide.phone.utils.MyDialog;
import com.hezy.guide.phone.utils.SDCardUtils;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.view.IconProgressBar;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.DbException;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import java.io.File;
import java.util.HashMap;


/**
 * 升级下载界面,基于启动器的下载
 * Created by wufan on 2017/2/12.
 */

public class UpdateDownloadActivity extends BasisActivity {
    private static final String TAG = "UpdateDownloadActivity";
    private static final int MESSAGE_UPDATE_UI = 0;
    private static final int MESSAGE_CHECKDOWNLOAD = 1;

    private static final int MSG_CHECK_PING = 7;
    private static final int MSG_NETWORK_DIALOG = 8;
    private static final int MSG_CHECK_UPDATE = 3;
    private static final int MSG_NEED_UPDATE = 4;
    private static final int MSG_NOT_NEED_UPDATE = 5;
    private static final int MSG_NEED_DOWNLOAD_REGISTER = 9;
    //	public static final String PACKAGE_CLIENT = "com.zhongyou.special";
    public static final String PACKAGE_CLIENT = BuildConfig.APPLICATION_ID;
    //	public static final String PACKAGE_SERVER = "com.yoyo.icontrol.server";
    private String appDownloadingPath = Environment.getExternalStorageDirectory() + "/prof/downloading/guide_phone.apk";
    private String appDownloadedPath = Environment.getExternalStorageDirectory() + "/prof/downloaded/guide_phone_success.apk";
    //	private String serverDownlodingPath = Environment.getExternalStorageDirectory()+"/prof/downloading/server_downloading.apk";
//	private String serverDownloadedPath = Environment.getExternalStorageDirectory()+"/prof/downloaded/server_success.apk";
    private DownloadManager downloadManager;
    //    private TextView mDownloadStatusTextview;
    private TextView mDownloadPercentTextview;
    //    private TextView mDownloadFileSizeTextView;
    private TextView mVersionTextView;
    private TextView mWifiTextView;
    private TextView mDeviceIdTextView;
    private IconProgressBar mDownloadProgressBar;
    //	private LinearLayout mProgressLayout;
    private HashMap<String, Version> mPackage2UpdateInfo = new HashMap<String, Version>();
    private int retryCount = 0;
    private Context mCtx;
    private boolean inTransaction = false;
    private Handler handler = new UIHandler();
    private Version mVersion;


    private static final int MIN_SIZE = 30;
    private MyDialog mNetworkDialog;
    private int install_millisecond;
    //估计安装完成时间 毫秒
    private static final int INSTALL_FAKE_TIME = 30 * 1000;
    private static final int INSTALL_INTERVAL_TIME = 200;
    private boolean flag_install_update;
    private ImageView mIvStep;
    private boolean mFinishFlag;
    //开启启动网络需要时间,延迟8秒弹窗
    private static final int mDelayShowNetworkDialog = 6;
    private boolean mIsForceUpdate;
    private boolean mDownloadError;

    @Override
    public String getStatisticsTag() {
        return "下载升级";
    }

    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_UI: {
                    if (mFinishFlag)
                        return;
                    long totalSize = msg.getData().getLong("total");
                    long size = msg.getData().getLong("size");
                    mDownloadProgressBar.setVisibility(View.VISIBLE);
                    mDownloadPercentTextview.setVisibility(View.VISIBLE);
//                    mDownloadFileSizeTextView.setVisibility(View.VISIBLE);
                    mDownloadProgressBar.setMax((int) totalSize);
                    mDownloadProgressBar.setProgress((int) size);
                    float num = (float) mDownloadProgressBar.getProgress() / (float) mDownloadProgressBar.getMax();
                    int result = (int) (num * 100);
                    mDownloadPercentTextview.setText(result + "%");
//                    mDownloadFileSizeTextView.setText("(" + AppUtil.formatAppSize((int) size) + "/" + AppUtil.formatAppSize((int) totalSize) + ")");
//				downLoadRateText.setText("速度："+((size - preSize)/1024)+"kb/s");
//				preSize = size;
                    break;
                }



                case MESSAGE_CHECKDOWNLOAD:
                    if (mFinishFlag)
                        return;
                    retryCount++;
                    if (retryCount >= 3) {
//                        mDownloadStatusTextview.setText("下载失败，请稍后重试...");
//					Toast.makeText(mCtx, "请检查网络后重试", Toast.LENGTH_SHORT).show();
//                        mDownloadStatusTextview.setText("网络连接异常，请检查网络后重试");
                        mIvStep.setImageResource(R.mipmap.starter_1_check_network);
                        inTransaction = false;
                        mDownloadError = true;
                        Log.i(TAG, "MESSAGE_CHECKDOWNLOAD mDownloadError true");
                        pingNetWork();
                    } else {
                        checkProfessionalDownload();
                    }
                    break;
                case MSG_CHECK_PING:
                    if (mFinishFlag)
                        return;
                    pingNetWork();
                    break;
                case MSG_NETWORK_DIALOG:
                    if (mFinishFlag)
                        return;
                    showNetworkDialog();
                    break;
                case MSG_CHECK_UPDATE:
                    if (mFinishFlag)
                        return;
                    checkVersionUpdate();
                    break;
                case MSG_NEED_UPDATE:
                    if (mFinishFlag)
                        return;
//                    mDownloadStatusTextview.setText("系统更新信息检测完毕,准备更新");
                    checkProfessionalDownload();
                    break;
                case MSG_NEED_DOWNLOAD_REGISTER: {
                    if (mFinishFlag)
                        return;
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                    registerReceiver(networkReceiver, intentFilter);
                    break;
                }
                case MSG_NOT_NEED_UPDATE:
                    if (mFinishFlag)
                        return;
                    ToastUtils.showToast("系统更新信息检测错误");
//                    mDownloadStatusTextview.setText("系统更新信息检测完毕,启动中");
//                    launchProfessionalApp();
                    finish();
                    break;
            }

        }
    }












    private boolean isToasted;
    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"networkReceiver onReceive");
            if (mFinishFlag)
                return;
            initInfo();
            ConnectivityManager connectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetInfo != null) {
                if (!isToasted) {
                    isToasted = true;
                    Toast.makeText(mCtx, "网络已连接", Toast.LENGTH_SHORT).show();
                }
                Log.i(TAG,"网络已连接");
                if (NetworkInfo.State.CONNECTED.equals(activeNetInfo.getState())) {
                    if (!inTransaction) {
                        pingNetWork(); //不会执行.ping直接执行了.
                    }
                }
            }
        }
    };





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirstPingTime = System.currentTimeMillis();
        Log.i(TAG, "onCreate");
        mCtx = UpdateDownloadActivity.this;

        setContentView(R.layout.activity_launcher2);

        initView();
        downloadManager = DownloadService.getDownloadManager(getApplicationContext());
        downloadManager.setMaxDownloadThread(1);

        Log.i(TAG, "onCreate end");
    }

    private void checkNetWorkStatus() {
        if (!CheckNetWorkStatus.isNetworkAvailable(mCtx)) {//未联网
            showNetworkDialog();
        }
    }

    private void showNetworkDialog() {
        MyDialog.Builder builder = new MyDialog.Builder(this);
        builder.setMessage("未检测到网络连接，请先设置网络");
        builder.setOnClickListener(new MyDialog.ClickListener() {
            @Override
            public void onClick(int tags) {
                switch (tags) {
                    case MyDialog.BUTTON_POSITIVE:
                        Intent wifiSettingsIntent = new Intent("android.settings.WIFI_SETTINGS");
                        startActivity(wifiSettingsIntent);
                        break;
                }
            }
        });
        mNetworkDialog = builder.create();
        mNetworkDialog.show();
    }

    private void dismissNetworkDialog() {
        if (mNetworkDialog != null) {
            mNetworkDialog.dismiss();
            mNetworkDialog = null;
        }
    }

    /**
     * 检查sd卡有无和剩余存储
     */
    private boolean checkStorage() {
        SDCardUtils.getSDCardSize();
        SDCardUtils.getSDCardAvailableSize();
        SDCardUtils.getSystemSize();
        SDCardUtils.getSystemAvailableSize();
        long getSystemAvailableSizeMB = SDCardUtils.getSystemAvailableSizeMB();
        long getSDCardAvailableSizeMB = SDCardUtils.getSDCardAvailableSizeMB();

        if (getSystemAvailableSizeMB < MIN_SIZE && SDCardUtils.getSDCardAvailableSizeMB() < MIN_SIZE) {  //
            StringBuilder stringBuilder=new StringBuilder("内部存储可用:" + getSystemAvailableSizeMB + "M，");
            //不提示SD卡信息
//            if(SDCardUtils.isSDCardEnable()){
//                stringBuilder.append("SD存储可用:" + getSDCardAvailableSizeMB + "M，");
//            }
            stringBuilder.append("下载和运行需要至少" + MIN_SIZE + "M存储空间，请先清理再启动本应用");
            Log.i(TAG,stringBuilder.toString());
            MyDialog.Builder builder = new MyDialog.Builder(this);
            builder.setMessage(getString(R.string.app_storage_show));
            builder.setOnClickListener(new MyDialog.ClickListener() {
                @Override
                public void onClick(int tags) {
                    switch (tags) {
                        case MyDialog.BUTTON_POSITIVE:
                            finish();
                            break;
                    }
                }
            });
            builder.create().show();
            return false;
        } else {
            if (SDCardUtils.isSDCardEnable() && getSDCardAvailableSizeMB > MIN_SIZE) {
                //有SD卡且空间够
            } else {
                //内部存储空间够,下载到内部存储
                appDownloadingPath = mCtx.getFilesDir().getAbsolutePath() + "/tvprofessional_downloading.apk";
                appDownloadedPath = mCtx.getFilesDir().getAbsolutePath() + "/tvprofessional_success.apk";
            }


            Log.d(TAG, "appDownloadingPath" + appDownloadingPath);
            Log.d(TAG, "appDownloadedPath" + appDownloadedPath);

            //启动删除文件
            deleteFileApk(appDownloadedPath);
            deleteFileApk(appDownloadingPath);

            return true;
        }

    }

    private void initView() {
//        mDownloadStatusTextview = (TextView) findViewById(R.id.download_name_textview);
//        mDownloadStatusTextview.setVisibility(View.INVISIBLE);
        mDownloadPercentTextview = (TextView) findViewById(R.id.download_percent);
        mDownloadPercentTextview.setVisibility(View.INVISIBLE);
//        mDownloadFileSizeTextView = (TextView) findViewById(R.id.file_size);
//        mDownloadFileSizeTextView.setVisibility(View.INVISIBLE);
        mDownloadProgressBar = (IconProgressBar) findViewById(R.id.download_progress_bar);
        mDownloadProgressBar.setVisibility(View.INVISIBLE);
        mVersionTextView = (TextView) findViewById(R.id.version_textview);
        mWifiTextView = (TextView) findViewById(R.id.wifi_textview);
        mDeviceIdTextView = (TextView) findViewById(R.id.deviceid_textview);
        mIvStep = (ImageView) findViewById(R.id.mIvStep);
        initInfo();
    }

    private void initInfo() {
        String netMsg = "";
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                netMsg = wifiInfo.getSSID() + "(" + intToIp(wifiInfo.getIpAddress()) + ")";
            }
        }
        if (mWifiTextView != null) {
            mWifiTextView.setText("WIFI : " + netMsg);
        }
        mVersionTextView.setText("桌面版本:" + AppUtil.getCurrentAppVersionName(mCtx) + "(" + AppUtil.getVersion(mCtx) + ")");
        mDeviceIdTextView.setText("设备ID:" + AppUtil.getDeviceId(mCtx));
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + (i >> 24 & 0xFF);
    }

    private int mPingCount;
    private long mFirstPingTime;
    private boolean mIsShowNetworkDialog;


    /**
     * 尝试show网络超时dialog
     */
    private void showCheckDialog(){
        Log.i(TAG, " showCheckDialog");
        long costTime = System.currentTimeMillis() - mFirstPingTime;
        Log.i(TAG, "costtime " + costTime);
        if (mIsShowNetworkDialog == false && costTime > 5000) {
            mIsShowNetworkDialog = true;
            Log.i(TAG, " send show dialog");
            handler.sendEmptyMessage(MSG_NETWORK_DIALOG);
        }

    }


    /**
     * 不在ping直接轮询
     */
    private void pingNetWork() {
        inTransaction = true;
        mIvStep.setImageResource(R.mipmap.starter_1_check_network);
        if (mFinishFlag) {
            return;
        }
        if (mVersion == null) {
            handler.sendEmptyMessage(MSG_CHECK_UPDATE);
            Log.i(TAG, "mVersion==null handler.sendEmptyMessage(MSG_CHECK_UPDATE)");
            return;
        }
        Log.i(TAG, "mDownloadError " +mDownloadError);
        if (mDownloadError) {
            handler.sendEmptyMessage(MSG_NEED_UPDATE);
            Log.i(TAG, "mDownloadError true handler.sendEmptyMessage(MSG_NEED_UPDATE)");
            mDownloadError = false;
        }

    }

    // 版本号
    public int getVersionCode(Context context, String packeName) {
        if (!ApkUtil.checkApkInstall(mCtx, packeName))
            return 0;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packeName, PackageManager.GET_CONFIGURATIONS);
            return pi.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void checkVersionUpdate() {
        Log.i(TAG, "launcher  checkVersionUpdate");
        if (mVersion != null) {
            Log.i(TAG, "launcher  checkVersionUpdate mVersion!=null return");
            return;
        }
//        mDownloadStatusTextview.setText("系统更新信息检测中...");
//        mIvStep.setImageResource(R.drawable.starter_2_download); //onloding中下载
        final String versionUpdateUrl = Constant.VERSION_UPDATE_URL;
//		try {
//			params.setBodyEntity(new StringEntity(paramsJson.toString()));
//		} catch (UnsupportedEncodingException e1) {
//			e1.printStackTrace();
//		}
        Log.i(TAG, "http request:  " + versionUpdateUrl);
        HttpUtils http = new HttpUtils();
        http.configUserAgent(DeviceUtil.getUserAgent(mCtx));
        http.send(HttpRequest.HttpMethod.GET,
                versionUpdateUrl,
                new RequestCallBack<String>() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onLoading(long total, long current, boolean isUploading) {
                    }

                    @Override
                    public void onSuccess(ResponseInfo<String> responseInfo) {
                        if (mVersion != null) {
                            return;
                        }
                        mIsShowNetworkDialog = false;
                        dismissNetworkDialog();
                        String resultJson = responseInfo.result;
                        mPackage2UpdateInfo.clear();
                        Log.i(TAG, "http request onSuccess:  " + resultJson);
                        if (!TextUtils.isEmpty(resultJson)) {
                            try {
                                JSONObject jsonObject = JSON.parseObject(resultJson);
                                if (jsonObject.containsKey("errcode")) {
                                    int code = jsonObject.getIntValue("errcode");
                                    if (code == 0) {
                                        mVersion = JSON.parseObject(jsonObject.getJSONObject("data").toJSONString(), Version.class);
                                        if (mVersion.getImportance() == 2 | mVersion.getImportance() == 3) {
                                            mIsForceUpdate=false;
                                        } else if (mVersion.getImportance() == 4) {
                                            mIsForceUpdate=true;
                                        }
                                        mPackage2UpdateInfo.put(PACKAGE_CLIENT, mVersion);
                                        //获取完升级信息
                                        handler.sendEmptyMessage(MSG_NEED_UPDATE);
                                    } else {
                                        handler.sendEmptyMessage(MSG_NOT_NEED_UPDATE);
                                    }
                                } else {
                                    handler.sendEmptyMessage(MSG_NOT_NEED_UPDATE);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            //没有升级信息
                            handler.sendEmptyMessage(MSG_NOT_NEED_UPDATE);
                        }
                    }

//

                    @Override
                    public void onFailure(HttpException error, String msg) {
                        String errorMsg = msg;
                        Log.i(TAG,
                                " http request onFailure: " + errorMsg);
//                        handler.sendEmptyMessage(MSG_NOT_NEED_UPDATE); //没有内置检查网络错误,必须重新检查
                        showCheckDialog();
                        handler.sendEmptyMessage(MSG_CHECK_PING);
                    }
                });
    }

    private void checkProfessionalDownload() {
        if (mFinishFlag)
            return;
        Log.i(TAG, "checkProfessionalDownload");

        if (!ApkUtil.checkApkInstall(mCtx, PACKAGE_CLIENT)) {//未安装
            Log.i(TAG,"未安装");
            if (isFileExist(appDownloadedPath)) { //已下载-去安装
                Log.i(TAG,"未安装,已下载,去安装");
                mDownloadProgressBar.setProgress(mDownloadProgressBar.getMax());
                //TODO 安装
                installApp();

            } else {//未下载-去下载
                Log.i(TAG,"未安装,未下载,去下载");
                if (mPackage2UpdateInfo.containsKey(PACKAGE_CLIENT)) {
                    downloadTvApp(mPackage2UpdateInfo.get(PACKAGE_CLIENT));
                    Log.i(TAG, "checkProfessionalDownload   first  download  app");
                } else {
                    Log.i(TAG, "checkProfessionalDownload   first  download  app  without download info");
                }
            }
            return;
        }
        if (ApkUtil.checkApkInstall(mCtx, PACKAGE_CLIENT)) {//已安装-检查版本小于去下载
            if (mPackage2UpdateInfo.containsKey(PACKAGE_CLIENT)) {
                final Version updateInfo = mPackage2UpdateInfo.get(PACKAGE_CLIENT);
                if (ApkUtil.getVersionCodeByPackage(mCtx, PACKAGE_CLIENT) < Integer.valueOf(updateInfo.getVersionCode())) {
                    Log.i(TAG, "checkProfessionalDownload   update   app");
                    downloadTvApp(updateInfo);
                    return;
                }
            }
        }
        //都装了，启动专业版 安装后才启动.
//        launchProfessionalApp();
    }

    private boolean isFileExist(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.isFile() && file.exists()) {
                return true;
            }
        }
        return false;
    }


    private void deleteFileApk(String path) {
        if (path != null) {
            File file = new File(path);
            if (file.isFile() && file.exists()) {
                file.delete();
            }
        }
    }

    private void installApp() {
        if (mFinishFlag)
            return;
        Log.i(TAG, "installApp appDownloadedPath");
//        mDownloadStatusTextview.setText(getString(R.string.app_downloaded, "唷唷兔"));
        mIvStep.setImageResource(R.mipmap.starter_3_install);
        install_millisecond = 0;
        flag_install_update = true;
        //TODO
//        AppUtil.install(MainActivity.this, appDownloadedPath, PACKAGE_CLIENT);
        ApkController.install(appDownloadedPath, mCtx);
    }

    private void launchProfessionalApp() {
        if (mFinishFlag)
            return;
        inTransaction = false;
        Log.i(TAG, "launchProfessionalApp ");
        if (ApkUtil.checkApkInstall(mCtx, PACKAGE_CLIENT)) {
//            ZYAgentClass.onEvent(mCtx, "唷唷兔启动");
            ApkUtil.launcherAppByPackageName(this, PACKAGE_CLIENT);

            Log.i(TAG, "launchProfessionalApp  launch success");
        } else {
            Log.i(TAG, "launchProfessionalApp  launch fail....either server or client not install,we need ping net work agin...");
            pingNetWork();
        }
    }

    private void downloadTvApp(Version clientUpdateInfo) {
        if (mFinishFlag)
            return;
//		mProgressLayout.setVisibility(View.VISIBLE);
        RequestCallBack<File> requestCallBack = new RequestCallBack<File>() {
            @Override
            public void onStart() {
                Log.i("download", "xutil onstart");
            }

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
//                mDownloadStatusTextview.setVisibility(View.VISIBLE);
//                mDownloadStatusTextview.setText(getString(R.string.app_downloading, "唷唷兔"));
                mIvStep.setImageResource(R.mipmap.starter_2_download);
                Log.i("download", "xutil onloading  " + "total   " + total + "  current " + current);
                Message msMessage = new Message();
                msMessage.what = MESSAGE_UPDATE_UI;
                msMessage.getData().putLong("total", total);
                msMessage.getData().putLong("size", current);
                handler.sendMessage(msMessage);
                mIsShowNetworkDialog = false;
                dismissNetworkDialog();
            }

            @Override
            public void onSuccess(ResponseInfo<File> responseInfo) {
                if (mFinishFlag) return;
                mIsShowNetworkDialog = false;
                dismissNetworkDialog();
                File traget = new File(Environment.getExternalStorageDirectory() + "/prof/downloaded/");
                if (!traget.exists()) {
                    traget.mkdir();
                }
                responseInfo.result.getAbsoluteFile().renameTo(new File(appDownloadedPath));
//                ZYAgentClass.onEvent(mCtx, "唷唷兔下载成功");
                Log.i("download", "success   : " + responseInfo.result.getAbsolutePath());
                installApp();
            }

            @Override
            public void onFailure(HttpException error, String msg) {
                Log.i("download", "xutil onFailure  " + "msg   " + msg);
//                ZYAgentClass.onEvent(mCtx, "唷唷兔下载失败", msg);
//                mDownloadStatusTextview.setText(getString(R.string.app_re_download, "唷唷兔"));
                mIvStep.setImageResource(R.mipmap.starter_1_check_network);
                Message msMessage = new Message();
                msMessage.what = MESSAGE_CHECKDOWNLOAD;
                showCheckDialog();
                handler.sendMessage(msMessage);
            }
        };
//		DownloadInfo downloadInfo = downloadManager.getDownloadInfoByDownloadUrl(clientUpdateInfo.getUrl());
        DownloadInfo downloadInfo = downloadManager.getDownloadInfoByDownloadUrl(clientUpdateInfo.getUrl());
        try {
            if (downloadInfo != null) {
                downloadManager.removeDownload(downloadInfo);
                if (new File(appDownloadingPath).exists()) {
                    new File(appDownloadingPath).delete();
                }
            }
            downloadManager.addNewDownload(clientUpdateInfo.getUrl(),
                    "唷唷兔下载",
                    appDownloadingPath,
                    true, // 如果目标文件存在，接着未完成的部分继续下载。服务器不支持RANGE时将从新下载。
                    false, // 如果从请求返回信息中获取到文件名，下载完成后自动重命名。
                    requestCallBack);
        } catch (DbException e) {
            Logger.e(e.getMessage(), e);
        }
    }


    @Override
    protected void  onResume() {
        super.onResume();
        Log.i(TAG, "launcher  onResume");



//        ZYAgentClass.onResume(this);
        if (!inTransaction) {
            if (!checkStorage()) return;
//            mDownloadStatusTextview.setVisibility(View.INVISIBLE);
            mDownloadPercentTextview.setVisibility(View.INVISIBLE);
//            mDownloadFileSizeTextView.setVisibility(View.INVISIBLE);
            mDownloadProgressBar.setVisibility(View.INVISIBLE);
            handler.sendEmptyMessage(MSG_NEED_DOWNLOAD_REGISTER);
            pingNetWork();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "launcher  onPause");

        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
            }
        }

//        ZYAgentClass.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        inTransaction = false;
        Log.i(TAG, "launcher  onStop");
    }

    @Override
    protected void onDestroy() {
        mFinishFlag = true;
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        handler.removeCallbacksAndMessages(null);


        try {
            downloadManager.stopAllDownload();
        } catch (DbException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "launcher  onDestroy end");
    }


    @Override
    public void finish() {
        mFinishFlag = true;
        super.finish();
        Log.i(TAG, "finish");
        handler.removeCallbacksAndMessages(null);
        try {
            downloadManager.stopAllDownload();
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        if(mIsForceUpdate){
            showExitDialog("确定中断升级退出应用？");
        }else{
            showExitDialog("确定中断升级？");
        }

    }



    private void showExitDialog(String title){
        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx, R.style.CustomDialog);
        builder.setTitle(title);
//        builder.setView(View.inflate(this, R.layout.exit_dialog_layout, null));
        builder.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(mIsForceUpdate){
                    LoginHelper.exit(UpdateDownloadActivity.this);
                }else{
                    finish();
                }


            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


}
