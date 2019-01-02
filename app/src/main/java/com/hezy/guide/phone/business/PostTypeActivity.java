package com.hezy.guide.phone.business;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.hezy.guide.phone.BaseException;
import com.zy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.PostTypeAdapter;
import com.zy.guide.phone.databinding.ActivityPosttypeBinding;
import com.hezy.guide.phone.entities.PostType;
import com.hezy.guide.phone.entities.base.BaseArrayBean;
import com.hezy.guide.phone.event.UserUpdateEvent;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.statistics.ZYAgent;

import rx.Subscription;
import rx.functions.Action1;

/**
 * 用户信息编辑页面-用户类型Activity
 *
 * @author Dongce
 * create time: 2018/10/24
 */
public class PostTypeActivity extends BaseDataBindingActivity<ActivityPosttypeBinding> {

    private Subscription subscription;
    private PostTypeAdapter postTypeAdapter;

    @Override
    protected int initContentView() {
        return R.layout.activity_posttype;
    }

    @Override
    public String getStatisticsTag() {
        return "选择用户类型";
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

    @Override
    protected void initListener() {
        mBinding.lvPostType.setOnItemClickListener(itemClickListener);
        mBinding.mIvLeft.setOnClickListener(this);
    }

    @Override
    protected void normalOnClick(View v) {
        if (v.getId() == R.id.mIvLeft) {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postTypeAdapter = new PostTypeAdapter(this);
        mBinding.lvPostType.setAdapter(postTypeAdapter);

        apiClient.requestPostType(this, postTypeCallback);
    }

    private OkHttpCallback<BaseArrayBean<PostType>> postTypeCallback = new OkHttpCallback<BaseArrayBean<PostType>>() {

        @Override
        public void onSuccess(BaseArrayBean<PostType> entity) {
            postTypeAdapter.add(entity.getData());
            postTypeAdapter.notifyDataSetChanged();
        }

        @Override
        public void onFailure(int errorCode, BaseException exception) {
            super.onFailure(errorCode, exception);
            String errorMsg = "错误码：" + errorCode + "，错误信息：" + exception.getMessage();
            ZYAgent.onEvent(PostTypeActivity.this, errorMsg);
            ToastUtils.showToast(errorMsg);
        }
    };

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            PostType postType = (PostType) postTypeAdapter.getItem(position);

            Intent intent = new Intent();
            intent.putExtra(UserInfoActivity.KEY_USERINFO_POSTTYPE, postType);
            setResult(RESULT_OK, intent);
            finish();
        }
    };

    private void setUserUI() {
    }

    @Override
    protected void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }
}
