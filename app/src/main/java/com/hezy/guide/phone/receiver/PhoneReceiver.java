package com.hezy.guide.phone.receiver;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.hezy.guide.phone.entities.base.BaseErrorBean;
import com.hezy.guide.phone.event.HangDownEvent;
import com.hezy.guide.phone.event.HangOnEvent;
import com.hezy.guide.phone.net.ApiClient;
import com.hezy.guide.phone.net.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;

/**
 * Created by whatisjava on 17-8-7.
 */

public class PhoneReceiver extends BroadcastReceiver {

    private static boolean incomingFlag = false;

    private Context mContext;
    private String recordId;

    public PhoneReceiver(){

    }
    public PhoneReceiver(String recordId){
        this();
        this.recordId = recordId;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        //拨打电话
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            incomingFlag = false;
            final String phoneNum = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d("PhoneReceiver", "phoneNum: " + phoneNum);
        } else {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    final PhoneStateListener listener=new PhoneStateListener(){
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch(state){
                //电话等待接听
                case TelephonyManager.CALL_STATE_RINGING:
                    incomingFlag = true;
                    Log.i("PhoneReceiver", "CALL IN RINGING :" + incomingNumber);
                    break;
                //电话接听
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (incomingFlag) {
                        Log.i("PhoneReceiver", "CALL IN ACCEPT :" + incomingNumber);
                        ApiClient.getInstance().startOrStopOrRejectCallExpostor(recordId, "7", new ExpostorCallback());
                        RxBus.sendMessage(new HangOnEvent());
                    }
                    break;
                //电话挂机
                case TelephonyManager.CALL_STATE_IDLE:
                    if (incomingFlag) {
                        Log.i("PhoneReceiver", "CALL IDLE");
                        RxBus.sendMessage(new HangDownEvent());
                    }
                    break;
            }
        }
    };

     class ExpostorCallback extends OkHttpCallback<BaseErrorBean> {

         @Override
         public void onSuccess(BaseErrorBean entity) {
            Log.d("stop when calling", entity.toString());
         }

     }
}