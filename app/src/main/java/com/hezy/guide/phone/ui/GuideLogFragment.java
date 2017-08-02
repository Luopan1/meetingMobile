package com.hezy.guide.phone.ui;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.adorkable.iosdialog.ActionSheetDialog;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.GuideLogFragmentBinding;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.event.UserStateEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.service.WSService;
import com.hezy.guide.phone.utils.RxBus;
import com.squareup.picasso.Picasso;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by wufan on 2017/7/26.
 */

public class GuideLogFragment extends BaseDataBindingFragment<GuideLogFragmentBinding> {

    private Subscription subscription;

    public static GuideLogFragment newInstance() {
        GuideLogFragment fragment = new GuideLogFragment();
        return fragment;
    }

    @Override
    protected int initContentView() {
        return R.layout.guide_log_fragment;
    }

    @Override
    protected void initView() {
        if (!TextUtils.isEmpty(Preferences.getWeiXinHead())) {
            Picasso.with(BaseApplication.getInstance()).load(Preferences.getWeiXinHead()).into(mBinding.views.mIvHead);
        }

        setState(WSService.SOCKET_ONLINE);

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserStateEvent) {

                    setState(WSService.SOCKET_ONLINE);
                }
            }
        });
    }

    @Override
    protected void initListener() {
        mBinding.views.mIvHead.setOnClickListener(this);
        mBinding.views.mTvState.setOnClickListener(this);
    }


    @Override
    protected void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mTvState:
                new ActionSheetDialog(mContext).builder()//
                        .setCancelable(false)//
                        .setCanceledOnTouchOutside(false)//
                        .addSheetItem("在线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if (!WSService.SOCKET_ONLINE) {
                                            //当前状态离线,可切换在线
                                            Log.i(TAG, "当前状态离线,可切换在线");
                                            RxBus.sendMessage(new SetUserStateEvent(true));
                                        }


                                    }
                                })
                        .addSheetItem("离线", ActionSheetDialog.SheetItemColor.Blue,//
                                new ActionSheetDialog.OnSheetItemClickListener() {//
                                    @Override
                                    public void onClick(int which) {
                                        if (WSService.SOCKET_ONLINE) {
                                            //当前状态在线,可切换离线
                                            Log.i(TAG, "当前状态在线,可切换离线");
                                            WSService.SOCKET_ONLINE = false;
                                            setState(false);
                                        }
                                    }
                                }).show();
                break;


        }
    }

    private void setState(boolean isOnline) {
        if (isOnline) {
            mBinding.views.mTvState.setText("在线状态");
            mBinding.views.mTvState.setBackgroundResource(R.drawable.userinfo_set_state_online_bg_shape);
        } else {
            mBinding.views.mTvState.setText("离线状态");
            mBinding.views.mTvState.setBackgroundResource(R.drawable.userinfo_set_state_offline_bg_shape);
        }
    }
}
