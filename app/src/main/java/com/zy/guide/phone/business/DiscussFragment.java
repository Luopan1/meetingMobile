package com.zy.guide.phone.business;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zy.guide.phone.R;

/**
 * @author luopan@centerm.com
 * @date 2019-10-09 17:05.
 * 讨论界面
 */
public class DiscussFragment extends BaseFragment {
	static DiscussFragment fragment = new DiscussFragment();

	@Override
	public String getStatisticsTag() {
		return "讨论";
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_discuss, null, false);
		return view;
	}

	public static DiscussFragment newInstance() {
		if (fragment == null) {
			fragment = new DiscussFragment();
		}
		return fragment;
	}

}
