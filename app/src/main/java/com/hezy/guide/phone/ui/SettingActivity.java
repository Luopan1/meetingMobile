package com.hezy.guide.phone.ui;

import android.os.Bundle;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.base.BasisActivity;

/**
 * Created by whatisjava on 17-9-5.
 */

public class SettingActivity extends BasisActivity {

    @Override
    public String getStatisticsTag() {
        return "设置";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
    }
}
