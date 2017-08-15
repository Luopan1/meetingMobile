package com.hezy.guide.phone.ui;

import android.view.View;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.MeFragmentBinding;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.helper.ImageHelper;

/**我的
 * Created by wufan on 2017/8/15.
 */

public class MeFragment extends BaseDataBindingFragment<MeFragmentBinding> {

    public static MeFragment newInstance() {
        MeFragment fragment = new MeFragment();
        return fragment;
    }

    @Override
    protected int initContentView() {
        return R.layout.me_fragment;
    }


    @Override
    protected void initView() {

    }

    @Override
    protected void initListener() {
        mBinding.mIvHead.setOnClickListener(this);
    }

    @Override
    public void onMyVisible() {
        super.onMyVisible();
        ImageHelper.loadImageDpIdRound(Preferences.getUserPhoto(),R.dimen.my_px_460,R.dimen.my_px_426,mBinding.mIvHead);
        ImageHelper.loadImageDpIdBlur(Preferences.getUserPhoto(),R.dimen.my_px_1080,R.dimen.my_px_530,mBinding.mIvBack);

        mBinding.mTvName.setText(Preferences.getUserName());
        mBinding.mTvAddress.setText(Preferences.getUserAddress());

    }

     @Override
         protected void normalOnClick(View v) {
             switch (v.getId()) {
                 case R.id.mIvHead:
                     UserinfoActivity.actionStart(mContext);
                     break;

             }
         }

}
