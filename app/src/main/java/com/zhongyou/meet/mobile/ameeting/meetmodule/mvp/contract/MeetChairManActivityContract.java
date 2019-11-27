package com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.contract;

import com.alibaba.fastjson.JSONObject;
import com.jess.arms.mvp.IView;
import com.jess.arms.mvp.IModel;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.HostUser;
import com.zhongyou.meet.mobile.entities.Material;
import com.zhongyou.meet.mobile.entities.Materials;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;

import javax.security.auth.callback.Callback;

import io.reactivex.Observable;


public interface MeetChairManActivityContract {
    //对于经常使用的关于UI的方法可以定义到IView中,如显示隐藏进度条,和显示文字消息
    interface View extends IView {
        void chengeView(int count);
        void joinMeetingCallBack(Bucket<HostUser> entity,int uid);

        void getDataSuccessFormOrigin(Bucket t,String type);
    }
    //Model层定义接口,外部只需关心Model返回的数据,无需关心内部细节,即是否使用缓存
    interface Model extends IModel{
        Observable<JSONObject> getUserCount();

        void joinMeeting(String meetingId,OkHttpCallback callback);

        void getMeetingPPt(String meetingId,OkHttpCallback callback);

        void showMeetingPPT(String meetingId,String pptId,OkHttpCallback callback);
    }
}
