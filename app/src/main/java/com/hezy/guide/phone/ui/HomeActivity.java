package com.hezy.guide.phone.ui;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.HomeActivityBinding;
import com.hezy.guide.phone.service.HeartService;

import java.util.ArrayList;

import me.kaelaela.verticalviewpager.transforms.DefaultTransformer;


/**主页
 * Created by wufan on 2017/7/24.
 */

public class HomeActivity extends BaseDataBindingActivity<HomeActivityBinding>{
    private HomePagerAdapter mHomePagerAdapter;
    private ArrayList<Fragment> mFragments;

    public static void actionStart(Context  context) {
         Intent intent = new Intent(context, HomeActivity.class);
         context.startActivity(intent);
     }

    @Override
    protected int initContentView() {
        return R.layout.home_activity;
    }

    @Override
    protected void initView() {
//        mBinding.mVerticalViewPager.setPageTransformer(true, new VerticalTransformer());
//        mBinding.mVerticalViewPager.setOverScrollMode(OVER_SCROLL_NEVER);
        //登录才能进入主页,启动心跳
        HeartService.actionStart(mContext);
        mBinding.mVerticalViewPager.setPageTransformer(false, new DefaultTransformer());

    }

    @Override
    protected void initAdapter() {
        mHomePagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        mFragments = new ArrayList<>();
        mFragments.add(UserinfoFragment.newInstance());
        mFragments.add(GuideLogFragment.newInstance());
        mHomePagerAdapter.setData(mFragments);
        mBinding.mVerticalViewPager.setAdapter(mHomePagerAdapter);
    }
}
