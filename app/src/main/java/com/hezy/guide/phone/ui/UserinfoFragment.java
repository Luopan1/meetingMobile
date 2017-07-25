package com.hezy.guide.phone.ui;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.UserinfoFragmentBinding;

/**用户信息fragment
 * Created by wufan on 2017/7/24.
 */

public class UserinfoFragment extends BaseDataBindingFragment<UserinfoFragmentBinding> {
    @Override
    protected int initContentView() {
        return R.layout.userinfo_fragment;
    }
}
