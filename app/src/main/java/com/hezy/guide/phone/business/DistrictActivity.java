package com.hezy.guide.phone.business;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.DistrictAdapter;
import com.hezy.guide.phone.entities.District;
import com.hezy.guide.phone.entities.base.BaseArrayBean;
import com.hezy.guide.phone.utils.OkHttpCallback;

/**
 * Created by whatisjava on 18-1-22.
 */

public class DistrictActivity extends BasicActivity {

    private ImageView backImage;
    private ListView listView, listView1;
    private DistrictAdapter districtAdapter, districtAdapter1;

    @Override
    public String getStatisticsTag() {
        return "大区中心列表页";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_district);

        backImage = findViewById(R.id.mIvLeft);
        backImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        listView = findViewById(R.id.list_view);
        listView1 = findViewById(R.id.list_view_1);

        apiClient.districts(districtCallback, "0");

    }

    private OkHttpCallback<BaseArrayBean<District>> districtCallback = new OkHttpCallback<BaseArrayBean<District>>() {
        @Override
        public void onSuccess(BaseArrayBean<District> entity) {
            districtAdapter = new DistrictAdapter(getApplicationContext(), entity.getData());
            listView.setAdapter(districtAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    District district = (District) districtAdapter.getItem(i);
                    apiClient.districts(districtCallback1, district.getId());
                }
            });
            listView.setSelection(0);
        }
    };

    private OkHttpCallback<BaseArrayBean<District>> districtCallback1 = new OkHttpCallback<BaseArrayBean<District>>() {
        @Override
        public void onSuccess(BaseArrayBean<District> entity) {
            districtAdapter1 = new DistrictAdapter(getApplicationContext(), entity.getData());
            listView1.setAdapter(districtAdapter1);
            listView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    District district = (District) districtAdapter1.getItem(i);

                    Intent intent = new Intent();
                    intent.putExtra("district", district);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }
    };

}
