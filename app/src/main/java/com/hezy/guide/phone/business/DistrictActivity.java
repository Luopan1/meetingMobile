package com.hezy.guide.phone.business;

import android.os.Bundle;

import com.hezy.guide.phone.R;

/**
 * Created by whatisjava on 18-1-22.
 */

public class DistrictActivity extends BasicActivity {

    @Override
    public String getStatisticsTag() {
        return "大区中心列表页";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_district);
    }

}
