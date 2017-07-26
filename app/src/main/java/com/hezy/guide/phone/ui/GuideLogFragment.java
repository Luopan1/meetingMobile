package com.hezy.guide.phone.ui;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BaseDataBindingFragment;
import com.hezy.guide.phone.databinding.GuideLogFragmentBinding;

/**
 * Created by wufan on 2017/7/26.
 */

public class GuideLogFragment extends BaseDataBindingFragment<GuideLogFragmentBinding> {

    public static GuideLogFragment newInstance() {
        GuideLogFragment fragment = new GuideLogFragment();
        return fragment;
    }

    @Override
    protected int initContentView() {
        return R.layout.guide_log_fragment;
    }
}
