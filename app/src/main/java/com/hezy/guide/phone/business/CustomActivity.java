package com.hezy.guide.phone.business;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.hezy.guide.phone.BaseException;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.CustomAdapter;
import com.hezy.guide.phone.databinding.ActivityCustomBinding;
import com.hezy.guide.phone.entities.Custom;
import com.hezy.guide.phone.entities.base.BaseArrayBean;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;

import java.util.HashMap;
import java.util.Map;

import rx.Subscription;
import rx.functions.Action1;

/**
 * 个人资料-用户 Activity
 *
 * @author Dongce
 * create time: 2018/10/25
 */
public class CustomActivity extends BaseDataBindingActivity<ActivityCustomBinding> {

    private Subscription subscription;
    private CustomAdapter customAdapter;

    private String areaId;

    @Override
    protected void initExtraIntent() {
        areaId = getIntent().getStringExtra(UserInfoActivity.KEY_DISTRICT_ID);
    }

    @Override
    protected int initContentView() {
        return R.layout.activity_custom;
    }

    @Override
    public String getStatisticsTag() {
        return "选择客户";
    }

    @Override
    protected void initView() {
        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof UserUpdateEvent) {
                    setUserUI();
                }
            }
        });
    }

    private void setUserUI() {
    }

    @Override
    protected void initListener() {
        mBinding.lvCustom.setOnItemClickListener(itemClickListener);
        mBinding.mIvLeft.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customAdapter = new CustomAdapter(this);
        mBinding.lvCustom.setAdapter(customAdapter);

        Map<String, String> params = new HashMap<>();
        params.put("areaId", areaId);
        apiClient.requestCustom(this, params, customCallback);
    }

    private OkHttpCallback<BaseArrayBean<Custom>> customCallback = new OkHttpCallback<BaseArrayBean<Custom>>() {
        @Override
        public void onSuccess(BaseArrayBean<Custom> entity) {
            customAdapter.add(entity.getData());
            customAdapter.notifyDataSetChanged();
            if (customAdapter.getCount() == 0) {
                ToastUtils.showToast("没有客户信息，请返回");
            }
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            String errorMsg = "错误码：" + errorCode + "，错误信息：" + exception.getMessage();
            ZYAgent.onEvent(CustomActivity.this, errorMsg);
            ToastUtils.showToast(errorMsg);
        }
    };

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Custom custom = (Custom) customAdapter.getItem(position);

            Intent intent = new Intent();
            intent.putExtra(UserInfoActivity.KEY_USERINFO_CUSTOM, custom);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    @Override
    protected void normalOnClick(View v) {
        if (v.getId() == R.id.mIvLeft) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }
}
