package com.zy.guide.phone.business;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.zy.guide.phone.BaseException;
import com.zy.guide.phone.R;
import com.zy.guide.phone.business.adapter.ForumMeetingAdapter;
import com.zy.guide.phone.business.adapter.GeneralAdapter;
import com.zy.guide.phone.business.adapter.MeetingAdapter;
import com.zy.guide.phone.entities.Agora;
import com.zy.guide.phone.entities.Bucket;
import com.zy.guide.phone.entities.ChatMesData;
import com.zy.guide.phone.entities.ForumMeeting;
import com.zy.guide.phone.entities.MeeetingAdmin;
import com.zy.guide.phone.entities.Meeting;
import com.zy.guide.phone.entities.MeetingJoin;
import com.zy.guide.phone.entities.base.BaseArrayBean;
import com.zy.guide.phone.event.ForumSendEvent;
import com.zy.guide.phone.persistence.Preferences;
import com.zy.guide.phone.utils.Logger;
import com.zy.guide.phone.utils.OkHttpCallback;
import com.zy.guide.phone.utils.RxBus;
import com.zy.guide.phone.utils.UIDUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.agora.openlive.ui.MeetingInitActivity;
import rx.Subscription;

public class MeetingsFragment extends BaseFragment {

	private Subscription subscription;
	private SwipeRefreshLayout swipeRefreshLayout;
	private RecyclerView recyclerView;
	private LinearLayoutManager mLayoutManager;
	private MeetingAdapter meetingAdapter;
	private TextView tv_meeting_public, tv_meeting_private;
	private Dialog dialog;
	private ForumMeetingAdapter forumMeetingAdapter;
	private ArrayList<ForumMeeting> forumMeetingList = new ArrayList<>();

	private View v_public,v_invite;

	public static final int TYPE_PUBLIC_MEETING = 0;
	public static final int TYPE_PRIVATE_MEETING = 1;
	public static final int TYPE_OWNER_MEETING = 2;
	public static final int TYPE_FORUM_MEETING = 3;
	private final int SEARCH_REQUEST_CODE = 1001;
	private final int CODE_MEETING_LIST_REQUEST = 101;
	public static final String KEY_MEETING_TYPE = "meetingType";
	private int currentMeetingListPageIndex = TYPE_PUBLIC_MEETING;
	private TextView mEmptyView;

	@Override
	public String getStatisticsTag() {
		return "会议列表";
	}

	private MeetingAdapter.OnItemClickListener onMeetingListItemClickListener = (view, meeting) -> {
		if (Build.VERSION.SDK_INT >= 23) {
			//视频会议拍照功能
			int REQUEST_CODE_CONTACT = 101;
			String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
					Manifest.permission.CAMERA};
			//验证是否许可权限
			for (String str : permissions) {
				if (getActivity().checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
					//申请权限
					getActivity().requestPermissions(permissions, REQUEST_CODE_CONTACT);
					return;
				}
			}
		}
		initDialog(meeting);
	};

	private void clearForumListUnReadInformationAndAtailFlag(ForumMeeting forumMeeting, ForumMeetingAdapter forumMeetingAdapter, int itemPosition) {
		forumMeeting.setNewMsgCnt(0);
		if (forumMeeting.isAtailFlag()) {
			forumMeeting.setAtailFlag(!forumMeeting.isAtailFlag());
		}
		forumMeetingAdapter.notifyItemChanged(itemPosition);
	}

	private void updateForumListUnReadInformationAndAtailFlag(ForumMeeting forumMeeting, ChatMesData.PageDataEntity entity, ForumMeetingAdapter forumMeetingAdapter) {
		forumMeeting.setNewMsgCnt(forumMeeting.getNewMsgCnt() + 1);
		//讨论区功能里区分用户的userId字段作用，等同于User实体类里的id字段。在微信授权进入app时，已进行存储。
		//找到userId是不是自己，已决定是否显示被@标示
		if (Preferences.getUserId().equals(entity.getAtailUserId())) {
			forumMeeting.setAtailFlag(!forumMeeting.isAtailFlag());
		}
		forumMeetingAdapter.notifyDataSetChanged();
	}

	private ForumMeetingAdapter.OnItemClickListener onForumMeetingItemClickListener = new ForumMeetingAdapter.OnItemClickListener() {
		@Override
		public void onItemClick(View view, ForumMeeting forumMeeting, int position) {
			clearForumListUnReadInformationAndAtailFlag(forumMeeting, forumMeetingAdapter, position);
			startActivity(new Intent(getActivity(), ChatActivity.class).putExtra("title", forumMeeting.getTitle()).putExtra("meetingId", forumMeeting.getMeetingId()).putExtra("num", forumMeeting.getUserCnt()));
		}
	};

	public static MeetingsFragment newInstance() {
		MeetingsFragment fragment = new MeetingsFragment();
		return fragment;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		forumMeetingAdapter = new ForumMeetingAdapter(mContext, onForumMeetingItemClickListener);
		forumMeetingAdapter.addData(forumMeetingList);

		//设置会议tag
		showMeeting(currentMeetingListPageIndex);
		checkAdminAccount();

		subscription = RxBus.handleMessage(o -> {
			if (o instanceof ForumSendEvent) {
				ChatMesData.PageDataEntity entity = ((ForumSendEvent) o).getEntity();
				for (ForumMeeting forumMeeting : forumMeetingList) {
					if (entity.getMeetingId().equals(forumMeeting.getMeetingId())) {
						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateForumListUnReadInformationAndAtailFlag(forumMeeting, entity, forumMeetingAdapter);
							}
						});
					}
				}
			}
		});
		super.onActivityCreated(savedInstanceState);
	}

	/**
	 * 检测当前账户是否为会议管理员。如是显示"会议管理"功能
	 */
	private void checkAdminAccount() {
		apiClient.requestMeetingAdmin(this, requestMeetingAdminCallback);
	}

	private OkHttpCallback<Bucket<MeeetingAdmin>> requestMeetingAdminCallback = new OkHttpCallback<Bucket<MeeetingAdmin>>() {
		@Override
		public void onSuccess(Bucket<MeeetingAdmin> entity) {
			MeeetingAdmin meeetingAdmin = entity.getData();
			if (meeetingAdmin.isMeetingAdmin()) {
//				showOwnerMeeting(meeetingAdmin.getMeetingMgrUrl());

			} else {
//				hideOwnerMeeting();

			}
		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
//			hideOwnerMeeting();
		}
	};

	@Override
	public void onDestroyView() {
		subscription.unsubscribe();
		super.onDestroyView();
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.meeting_fragment, null, false);
		swipeRefreshLayout = view.findViewById(R.id.mSwipeRefreshLayout);
		swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				showMeeting(currentMeetingListPageIndex);
			}
		});


		view.findViewById(R.id.txt_search).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent searchMeetingIntent = new Intent(mContext, MeetingSearchActivity.class);
				searchMeetingIntent.putExtra(KEY_MEETING_TYPE, currentMeetingListPageIndex);
				MeetingsFragment.this.startActivityForResult(searchMeetingIntent, SEARCH_REQUEST_CODE);
			}
		});

		recyclerView = view.findViewById(R.id.mRecyclerView);
		mLayoutManager = new LinearLayoutManager(mContext);
		mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		recyclerView.setLayoutManager(mLayoutManager);

		// 设置ItemAnimator
		recyclerView.setItemAnimator(new DefaultItemAnimator());
		recyclerView.setHasFixedSize(true);
		mEmptyView = view.findViewById(R.id.emptyView);

		tv_meeting_public = view.findViewById(R.id.tv_meet_public);
		tv_meeting_private = view.findViewById(R.id.tv_meet_invite);
		tv_meeting_public.setOnClickListener(tvMeetingOnClickListener);
		tv_meeting_private.setOnClickListener(tvMeetingOnClickListener);

		v_public = view.findViewById(R.id.v_public);
		v_invite = view.findViewById(R.id.v_invite);
		return view;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case SEARCH_REQUEST_CODE:
				showMeeting(resultCode);
				break;
			case CODE_MEETING_LIST_REQUEST:
				if (resultCode == TYPE_FORUM_MEETING) {
					startForumActivity();
				} else {
					showMeeting(resultCode);
				}
				break;
			default:
				Logger.v("MeetingFragments", "onActivityResult has no requestCode: " + requestCode);
				break;
		}
	}

	private void startForumActivity() {
		startActivity(new Intent(mContext, ForumActivity.class));
	}

	private View.OnClickListener tvMeetingOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.tv_meet_public:
					showMeeting(TYPE_PUBLIC_MEETING);
					v_invite.setVisibility(View.GONE);
					v_public.setVisibility(View.VISIBLE);
					break;
				case R.id.tv_meet_invite:
					showMeeting(TYPE_PRIVATE_MEETING);
					v_invite.setVisibility(View.VISIBLE);
					v_public.setVisibility(View.GONE);
					break;
				//进入讨论区
				case R.id.emptyView:
					startForumActivity();
					break;
				default:
					Logger.v("MeetingFragments", "onClick has no views`id");
					break;
			}
		}
	};

	private void showMeeting(int type) {
		//停止一切动画效果，包括recyclerView滚动效果，让appBarLayout常显，让刷新功能生效
		swipeRefreshLayout.setEnabled(true);
		recyclerView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));

		switch (type) {
			case TYPE_PUBLIC_MEETING:
				showPublicMeeting();
				break;
			case TYPE_PRIVATE_MEETING:
				showPrivateMeeting();
				break;
			default:
				Logger.v("MeetdingsFragment","showMeeting has no current type:"+type);
				break;
		}
	}

	private void showPublicMeeting() {
		TextViewCompat.setTextAppearance(tv_meeting_public, R.style.MeetingTypeFocus);
		TextViewCompat.setTextAppearance(tv_meeting_private, R.style.MeetingTypeUnFocus);
		currentMeetingListPageIndex = TYPE_PUBLIC_MEETING;
		requestMeetings(TYPE_PUBLIC_MEETING);
	}

	private void showPrivateMeeting() {
		TextViewCompat.setTextAppearance(tv_meeting_public, R.style.MeetingTypeUnFocus);
		TextViewCompat.setTextAppearance(tv_meeting_private, R.style.MeetingTypeFocus);
		currentMeetingListPageIndex = TYPE_PRIVATE_MEETING;
		requestMeetings(TYPE_PRIVATE_MEETING);
	}

/*	private void showOwnerMeeting(String meetingMgrUrl) {
		tv_meeting_owner.setVisibility(View.VISIBLE);
		tv_meeting_owner.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//                //mockData 模拟url
//                String url = "http://192.168.1.124";
				Intent intent = new Intent(getActivity(), MeetingManagementActivity.class);
				intent.putExtra(MeetingManagementActivity.KEY_MEETINGMGRURL, meetingMgrUrl);
				startActivityForResult(intent, CODE_MEETING_LIST_REQUEST);
			}
		});
	}

	private void hideOwnerMeeting() {
		tv_meeting_owner.setVisibility(View.GONE);
	}*/

	/**
	 * 请求会议类型
	 * @param type
	 */
	private void requestMeetings(int type) {
		swipeRefreshLayout.setRefreshing(true);
		apiClient.getAllMeeting(TAG, null, type, meetingsCallback);
	}

	private OkHttpCallback meetingsCallback = new OkHttpCallback<BaseArrayBean<Meeting>>() {

		@Override
		public void onSuccess(final BaseArrayBean<Meeting> meetingBucket) {
			if (meetingBucket.getData().size() > 0) {
				Logger.i("", meetingBucket.toString());
				meetingAdapter = new MeetingAdapter(mContext, meetingBucket.getData(), onMeetingListItemClickListener);
				recyclerView.setAdapter(new GeneralAdapter(meetingAdapter));
				loadForumListSuccessView();
			} else {
				loadForumListFaildView();
			}
		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
			loadForumListFaildView();
			Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onFinish() {
			super.onFinish();
			swipeRefreshLayout.setRefreshing(false);
		}
	};

	private void loadForumListSuccessView() {
		recyclerView.setVisibility(View.VISIBLE);
		mEmptyView.setVisibility(View.GONE);
	}

	private void loadForumListFaildView() {
		recyclerView.setVisibility(View.GONE);
		mEmptyView.setVisibility(View.VISIBLE);
	}

	private void initDialog(final Meeting meeting) {
		View view = View.inflate(mContext, R.layout.dialog_meeting_code, null);
		if (meeting.getScreenshotFrequency() == Meeting.SCREENSHOTFREQUENCY_INVALID) {
			view.findViewById(R.id.dialog_meeting_warnning).setVisibility(View.GONE);
			view.findViewById(R.id.dialog_meeting_warnning_text).setVisibility(View.GONE);
		} else {
			view.findViewById(R.id.dialog_meeting_warnning).setVisibility(View.VISIBLE);
			view.findViewById(R.id.dialog_meeting_warnning_text).setVisibility(View.VISIBLE);
		}
		final EditText codeEdit = view.findViewById(R.id.code);
		view.findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
				if (!TextUtils.isEmpty(codeEdit.getText())) {
					enterMeeting(codeEdit.getText().toString(), meeting);
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

	/**
	 * 进入会议直播间
	 *
	 * @param joinCode 会议加入码
	 * @param meeting  会议
	 */
	private void enterMeeting(String joinCode, Meeting meeting) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("clientUid", UIDUtil.generatorUID(Preferences.getUserId()));
		params.put("meetingId", meeting.getId());
		params.put("token", joinCode);
		apiClient.verifyRole(TAG, verifyRoleCallback(meeting, joinCode), params);
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
				Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
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
			params.put("role", meetingJoin.getRole() == 0 ? "Publisher" : "Subscriber");
			apiClient.getAgoraKey(mContext, params, getAgoraCallback(meetingJoin));
		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
			Toast.makeText(mContext, exception.getMessage(), Toast.LENGTH_SHORT).show();
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
			}

		};
	}
}
