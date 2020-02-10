package com.zhongyou.meet.mobile.business;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.business.adapter.ForumMeetingAdapter;
import com.zhongyou.meet.mobile.business.adapter.GeneralAdapter;
import com.zhongyou.meet.mobile.business.adapter.MeetingAdapter;
import com.zhongyou.meet.mobile.entities.Agora;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.Forum;
import com.zhongyou.meet.mobile.entities.ForumMeeting;
import com.zhongyou.meet.mobile.entities.Meeting;
import com.zhongyou.meet.mobile.entities.MeetingJoin;
import com.zhongyou.meet.mobile.entities.base.BaseArrayBean;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.Logger;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.ToastUtils;
import com.zhongyou.meet.mobile.utils.UIDUtil;
import com.zhongyou.meet.mobile.utils.statistics.ZYAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.dmoral.toasty.Toasty;
import io.agora.openlive.ui.MeetingInitActivity;

public class MeetingSearchActivity extends BasicActivity {

	private SmartRefreshLayout swipeRefreshLayout;
	private RecyclerView recyclerView;
	private EditText searchEdit;
	private TextView cancelText;
	private LinearLayoutManager mLayoutManager;
	private MeetingAdapter meetingAdapter;
	private TextView emptyText;
	//会议类型
	private int meetingType;
	private ForumMeetingAdapter forumMeetingAdapter;
	private String mKeyWords;

	@Override
	public String getStatisticsTag() {
		return "会议搜索列表";
	}

	private MeetingAdapter.OnItemClickListener onItemClickListener = new MeetingAdapter.OnItemClickListener() {
		@Override
		public void onItemClick(View view, Meeting meeting) {
			initDialog(meeting);
		}
	};

	private ForumMeetingAdapter.OnItemClickListener onForumMeetingItemClickListener = (view, forumMeeting, position) -> {
		forumMeeting.setNewMsgCnt(0);
		if (forumMeeting.isAtailFlag()) {
			forumMeeting.setAtailFlag(!forumMeeting.isAtailFlag());
		}
		forumMeetingAdapter.notifyItemChanged(position);
		startActivity(new Intent(getApplication(), ChatActivity.class).putExtra("title", forumMeeting.getTitle()).putExtra("meetingId", forumMeeting.getMeetingId()).putExtra("num", forumMeeting.getUserCnt()));
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_meeting_search);
		initView();
		initData();
	}

	private void initView() {
		swipeRefreshLayout = findViewById(R.id.mSwipeRefreshLayout);
		swipeRefreshLayout.setRefreshHeader(new MaterialHeader(this));
		swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
			@Override
			public void onRefresh(@NonNull RefreshLayout refreshLayout) {
				mKeyWords = searchEdit.getText().toString();
				if (TextUtils.isEmpty(mKeyWords)) {
					Toast.makeText(mContext, "搜索会议名称不能为空", Toast.LENGTH_SHORT).show();
				} else {
					currentPage=1;
					mKeyWords=searchEdit.getText().toString();
					requestMeetings(mKeyWords, meetingType);
				}
			}
		});


		swipeRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
			@Override
			public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
				currentPage++;
				requestMeetings(mKeyWords, meetingType);
			}
		});

		searchEdit = findViewById(R.id.search_text);

		cancelText = findViewById(R.id.cancel);
		cancelText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		searchEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_SEARCH)) {
					if (TextUtils.isEmpty(searchEdit.getText())) {
						Toast.makeText(mContext, "搜索会议名称不能为空", Toast.LENGTH_SHORT).show();
					} else {
						swipeRefreshLayout.autoRefresh();
//						requestMeetings(searchEdit.getText().toString(), meetingType);
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
					}
					return true;
				} else {
					return false;
				}
			}
		});

		searchEdit.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (TextUtils.isEmpty(s)) {
					cancelText.setVisibility(View.GONE);
				} else {
					cancelText.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {

			}
		});

		recyclerView = findViewById(R.id.mRecyclerView);
		mLayoutManager = new LinearLayoutManager(mContext);
		mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(mLayoutManager);
		// 设置ItemAnimator
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.setHasFixedSize(true);
//        recyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
		emptyText = findViewById(R.id.emptyView);

		findViewById(R.id.mIvLeft).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

	}

	private void initData() {
		meetingType = getIntent().getIntExtra(MeetingsFragment.KEY_MEETING_TYPE, MeetingsFragment.TYPE_PUBLIC_MEETING);
		setResult(meetingType);
	}

	private int currentPage = 1;

	private void requestMeetings(String meetingTitle, int meetingType) {
		if (meetingType == MeetingsFragment.TYPE_FORUM_MEETING) {
			Map<String, String> params = new HashMap<>();
			params.put(ApiClient.PAGE_NO, "1");
			params.put(ApiClient.PAGE_SIZE, "20");
			if (!meetingTitle.equals("")) {
				params.put("title", meetingTitle);
			}
			apiClient.getAllForumMeeting(TAG, params, forumMeetingsCallback);
		} else {
			apiClient.getAllMeeting(TAG, meetingTitle, meetingType, currentPage, meetingsCallback);
		}
	}

	private GeneralAdapter mGeneralAdapter;
	private OkHttpCallback meetingsCallback = new OkHttpCallback<JSONObject>() {

		@Override
		public void onSuccess(final JSONObject meetingBucket) {


			JSONArray jsonArray = meetingBucket.getJSONObject("data").getJSONArray("list");
			List<Meeting> meetings = jsonArray.toJavaList(Meeting.class);
			if (meetingBucket.getJSONObject("data").getInteger("totalPage") <= currentPage) {
				swipeRefreshLayout.setEnableLoadMore(false);
				swipeRefreshLayout.setNoMoreData(true);
			} else {
				swipeRefreshLayout.setEnableLoadMore(true);
				swipeRefreshLayout.setNoMoreData(false);
			}

			if (currentPage == 1) {
				meetingAdapter = null;
			}
			if (meetings.size() > 0) {
				if (meetingAdapter == null) {
					meetingAdapter = new MeetingAdapter(mContext, meetings, onItemClickListener);
					mGeneralAdapter = new GeneralAdapter(meetingAdapter);
					recyclerView.setAdapter(mGeneralAdapter);
				} else {
					meetingAdapter.notifyDataSetChanged(meetings);
				}
			} else {
				recyclerView.setVisibility(View.GONE);
				emptyText.setVisibility(View.VISIBLE);
			}

		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
//            Toasty.error(mContext, exception.getMessage(), Toast.LENGTH_SHORT, true).show();
		}

		@Override
		public void onFinish() {
			super.onFinish();
			swipeRefreshLayout.finishRefresh();
			swipeRefreshLayout.finishLoadMore();
		}
	};

	private OkHttpCallback forumMeetingsCallback = new OkHttpCallback<Bucket<Forum>>() {

		@Override
		public void onSuccess(Bucket<Forum> entity) {
			Forum forum = entity.getData();
			ArrayList<ForumMeeting> forumMeetingList = forum.getPageData();
			if (forumMeetingList.size() == 0) {
				recyclerView.setVisibility(View.GONE);
				emptyText.setVisibility(View.VISIBLE);
				return;
			}

			forumMeetingAdapter = new ForumMeetingAdapter(mContext, onForumMeetingItemClickListener);
			recyclerView.setAdapter(forumMeetingAdapter);
			forumMeetingAdapter.addData(forumMeetingList);
			recyclerView.setVisibility(View.VISIBLE);
			emptyText.setVisibility(View.GONE);
		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
			ZYAgent.onEvent(getApplicationContext(), exception.getMessage());
//            ToastUtils.showToast("请求讨论区数据失败");
//            Toasty.error(mContext, exception.getMessage(), Toast.LENGTH_SHORT, true).show();
		}

		@Override
		public void onFinish() {
			super.onFinish();
			swipeRefreshLayout.finishRefresh();
			swipeRefreshLayout.finishLoadMore();
		}
	};

	private Dialog dialog;

	private void initDialog(final Meeting meeting) {
		View view = View.inflate(mContext, R.layout.dialog_meeting_input_code, null);
		final EditText codeEdit = view.findViewById(R.id.code);
		view.findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
						imm.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
					}
				}, 100);

				dialog.dismiss();
				if (!TextUtils.isEmpty(codeEdit.getText())) {
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
					params.put("meetingId", meeting.getId());
					params.put("token", codeEdit.getText().toString());
					apiClient.verifyRole(TAG, verifyRoleCallback(meeting, codeEdit.getText().toString()), params);
				} else {
					codeEdit.setError("会议加入码不能为空");
				}
			}
		});
		view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		dialog = new Dialog(mContext, R.style.CustomDialog);
		dialog.setContentView(view);
		dialog.show();
	}

	private OkHttpCallback verifyRoleCallback(final Meeting meeting, final String token) {
		return new OkHttpCallback<Bucket<MeetingJoin>>() {

			@Override
			public void onSuccess(Bucket<MeetingJoin> meetingJoinBucket) {
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
				params.put("meetingId", meeting.getId());
				params.put("token", token);
				apiClient.joinMeeting(TAG, joinMeetingCallback, params);
			}

			@Override
			public void onFailure(int errorCode, BaseException exception) {
				super.onFailure(errorCode, exception);
//                Toasty.error(mContext, exception.getMessage(), Toast.LENGTH_SHORT, true).show();
//                Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
			}
		};
	}

	private OkHttpCallback joinMeetingCallback = new OkHttpCallback<Bucket<MeetingJoin>>() {

		@Override
		public void onSuccess(Bucket<MeetingJoin> meetingJoinBucket) {
			MeetingJoin meetingJoin = meetingJoinBucket.getData();
			Map<String, String> params = new HashMap<String, String>();
			params.put("channel", meetingJoin.getMeeting().getId());
			params.put("account", UIDUtil.generatorUID(Preferences.getUserId()));
			params.put("role", "Publisher");
			apiClient.getAgoraKey(mContext, params, getAgoraCallback(meetingJoin));
		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
//            Toasty.error(mContext, exception.getMessage(), Toast.LENGTH_SHORT, true).show();
		}
	};

	private OkHttpCallback getAgoraCallback(final MeetingJoin meetingJoin) {
		return new OkHttpCallback<Bucket<Agora>>() {

			@Override

			public void onSuccess(Bucket<Agora> agoraBucket) {
				dialog.dismiss();
				Intent intent = new Intent(mContext, MeetingInitActivity.class);
				intent.putExtra("agora", agoraBucket.getData());
				intent.putExtra("meeting", meetingJoin);
				startActivity(intent);
			}

			@Override
			public void onFailure(int errorCode, BaseException exception) {
				Toast.makeText(mContext, "网络异常，请稍后重试！", Toast.LENGTH_SHORT).show();
//                Toasty.error(mContext, exception.getMessage(), Toast.LENGTH_SHORT, true).show();
			}

		};
	}
}
