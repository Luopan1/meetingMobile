package com.hezy.guide.phone.ui;

import android.content.Context;
import android.content.Intent;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingActivity;
import com.hezy.guide.phone.databinding.HomeActivityBinding;

/**主页
 * Created by wufan on 2017/7/24.
 */

public class HomeActivity extends BaseDataBindingActivity<HomeActivityBinding>{

    public static void actionStart(Context  context) {
         Intent intent = new Intent(context, HomeActivity.class);
         context.startActivity(intent);
     }

    @Override
    protected int initContentView() {
        return R.layout.home_activity;
    }
}
