package com.hezy.guide.phone.business;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.utils.NetUtils;
import com.hezy.guide.phone.utils.OkHttpUtil;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.Logger;

import java.util.Calendar;

/**
 * Created by wufan on 2016/12/23.
 */

public abstract class BaseFragment extends Fragment implements View.OnClickListener {

    public String TAG = getClass().getSimpleName();
    public final String FTAG= Logger.lifecycle;
    protected Context mContext;

    protected SharedPreferences prefs;

    protected ApiClient apiClient;

//    protected String token;
    /**
     * 是否在viewpager的前台
     */
    public boolean mIsVisibleToUser=false;
    /**
     * 第一次resume时,setUserVisibleHint true不请求数据,这时候UI组件还没创造
     */
    public boolean isFirstResume=true;

    public String getTAG(){
        return TAG;
    }

    /**
     * 获得中文统计名
     *
     * @return
     */
    public abstract String getStatisticsTag() ;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Logger.d(FTAG+getTAG(), "onAttach");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(FTAG+getTAG(), "onCreate");

        apiClient = ApiClient.getInstance();

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//        token = Preferences.getToken();
        mContext = getActivity();
        if(mContext == null){
            Log.e(TAG," onCreate  mContext == null");
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d(FTAG+getTAG(), "onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Logger.d(FTAG+getTAG(), "onActivityCreated");
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.d(FTAG+getTAG(), "onViewCreated");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.d(FTAG+getTAG(), "onActivityResult");
    }

    public void onStart() {
        super.onStart();
        Logger.d(FTAG+getTAG(), "onStart");
    }

    public void onResume() {
        super.onResume();
        Logger.d(FTAG+getTAG(), "onResume");
        isFirstResume=false;
//        TCAgent.onPageStart(mContext,TAG);
        if(getUserVisibleHint()){
            onMyVisible();
        }
    }

    public void onPause() {
        super.onPause();
        Logger.d(FTAG+getTAG(), "onPause");
//        TCAgent.onPageEnd(mContext,TAG );

    }

    public void onStop() {
        super.onStop();
        Logger.d(FTAG+getTAG(), "onStop");
    }

    public void onDestroyView() {
        super.onDestroyView();
        Logger.d(FTAG+getTAG(), "onDestroyView");
    }

    public void onDestroy() {
//        OkHttpUtil.getInstance().cancelTag(this);
        super.onDestroy();
        Logger.d(FTAG+getTAG(), "onDestroy");
    }

    public void onDetach() {
        super.onDetach();
        Logger.d(FTAG+getTAG(), "onDetach");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Logger.d(FTAG+getTAG(), "setUserVisibleHint " + isVisibleToUser);
        mIsVisibleToUser=isVisibleToUser;
        if(isVisibleToUser && getView()!=null){  //第一次visible true,在onCreate前面,无view.
            onMyVisible();
        }
    }

    public void onMyVisible(){
        Logger.d(FTAG+getTAG(), "onMyVisible");
    }

    public void showToast(int resId) {
        ToastUtils.showToast(resId);
    }

    public void showToast(String str) {
        ToastUtils.showToast(str);
    }


    public static final int MIN_CLICK_DELAY_TIME = 1000;
    private long lastClickTime = 0;

    /**
     * 点击事件
     *
     * @param v
     */
    @Override
    public void onClick(View v) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        //防止重复提交订单，最小点击为1秒
        if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
            lastClickTime = currentTime;
            normalOnClick(v);
            if (!NetUtils.isNetworkConnected(getActivity())) {//not network
                Toast.makeText(getActivity(), getString(R.string.network_err_toast), Toast.LENGTH_SHORT).show();
            } else {//have network
                checkNetWorkOnClick(v);
            }

        }

    }


    /**
     * 检查网络，如果没有网络的话，就不能点击
     *
     * @param v
     */
    protected void checkNetWorkOnClick(View v) {

    }

    /**
     * 不用检查网络，可以直接触发的点击事件
     *
     * @param v
     */
    protected void normalOnClick(View v) {

    }

    /**
     * 在activity中调用,返回true消耗掉,返回false activity继续处理
     * @param keyCode
     * @param event
     * @return
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }


}
