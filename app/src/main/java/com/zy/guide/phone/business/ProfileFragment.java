package com.zy.guide.phone.business;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.makeramen.roundedimageview.RoundedImageView;
import com.zy.guide.phone.ApiClient;
import com.zy.guide.phone.R;
import com.zy.guide.phone.UserInfoActivity;
import com.zy.guide.phone.entities.User;
import com.zy.guide.phone.entities.UserData;
import com.zy.guide.phone.entities.Wechat;
import com.zy.guide.phone.entities.base.BaseBean;
import com.zy.guide.phone.event.UserUpdateEvent;
import com.zy.guide.phone.persistence.Preferences;
import com.zy.guide.phone.utils.Logger;
import com.zy.guide.phone.utils.Login.LoginHelper;
import com.zy.guide.phone.utils.OkHttpCallback;
import com.zy.guide.phone.utils.RxBus;


/**
 * @author luopan
 * 我的界面
 */
public class ProfileFragment extends BaseFragment implements View.OnClickListener {

	private SwipeRefreshLayout swipeRefreshLayout;
	private boolean isRefresh;
	private int mTotalPage = -1;
	private int mPageNo = -1;
	private LinearLayout mHeadLinearLayout;
	private LinearLayout mSetLinearLayout;
	private LinearLayout mEvaluationLauout;
	private RoundedImageView mImg_face;
	private TextView mTv_name;
	private TextView mTv_code;

	@Override
	public String getStatisticsTag() {
		return "我的";
	}

	public static ProfileFragment newInstance() {
		ProfileFragment fragment = new ProfileFragment();
		return fragment;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.profile_fragment, null, false);
		swipeRefreshLayout = view.findViewById(R.id.mSwipeRefreshLayout);
		swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				getUserInfo();
			}
		});
		mHeadLinearLayout = view.findViewById(R.id.layout_info);
		mSetLinearLayout = view.findViewById(R.id.layout_set);
		mEvaluationLauout = view.findViewById(R.id.layout_evaluation);

		mImg_face = view.findViewById(R.id.img_face);
		mTv_name = view.findViewById(R.id.tv_name);
		mTv_code = view.findViewById(R.id.tv_code);

		//设置监听
		mHeadLinearLayout.setOnClickListener(this);
		mSetLinearLayout.setOnClickListener(this);
		mEvaluationLauout.setOnClickListener(this);

		return view;
	}

	@Override
	public void onMyVisible() {
		super.onMyVisible();


	}

	private void getUserInfo() {
		if (!TextUtils.isEmpty(Preferences.getToken())) {
			requestUser();
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getUserInfo();
	}

	@Override
	public void onClick(View v) {
		super.onClick(v);
		switch (v.getId()) {
			case R.id.layout_info:
				boolean isUserAuthByHEZY = Preferences.getUserAuditStatus() == 1;
				UserInfoActivity.actionStart(mContext, false, isUserAuthByHEZY);
				break;
			case R.id.layout_set:
				Intent intent = new Intent(getActivity(), SettingActivity.class);
				startActivity(intent);
				break;
			case R.id.layout_evaluation:
				intent = new Intent(getActivity(), EvaluationActivity.class);
				startActivity(intent);
				break;
			default:
				Logger.v(TAG, "onClick has not current views`id:" + v.getId());
				break;

		}
	}

	public void requestUser() {
		if (!Preferences.isLogin()) {
			return;
		}
		ApiClient.getInstance().requestUser(this, new OkHttpCallback<BaseBean<UserData>>() {
			@Override
			public void onSuccess(BaseBean<UserData> entity) {
				if (entity == null || entity.getData() == null || entity.getData().getUser() == null) {
					showToast("数据为空");
					return;
				}
				User user = entity.getData().getUser();
				Wechat wechat = entity.getData().getWechat();
				LoginHelper.savaUser(user);
//                initCurrentItem();
				if (wechat != null) {
					LoginHelper.savaWeChat(wechat);
				}
				if (!TextUtils.isEmpty(user.getPhoto())) {
					Glide.with(getActivity()).load(user.getPhoto()).asBitmap()
							.error(R.mipmap.baby_default_avatar)
							.placeholder(R.drawable.default_icon)
							.into(mImg_face);
				}
				if (!TextUtils.isEmpty(user.getName())) {
					mTv_name.setText(user.getName());
				}
				RxBus.sendMessage(new UserUpdateEvent());
				swipeRefreshLayout.setRefreshing(false);
			}
		});
	}


}
