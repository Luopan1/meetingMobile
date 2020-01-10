package io.agora.openlive.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.GridLayoutHelper;
import com.alibaba.android.vlayout.layout.OnePlusNLayoutHelper;
import com.alibaba.android.vlayout.layout.StaggeredGridLayoutHelper;
import com.alibaba.fastjson.JSON;
import com.elvishew.xlog.XLog;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnSelectListener;
import com.orhanobut.logger.Logger;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.squareup.picasso.Picasso;
import com.tendcloud.tenddata.TCAgent;
import com.zhongyou.meet.mobile.ApiClient;
import com.zhongyou.meet.mobile.BaseException;
import com.zhongyou.meet.mobile.BuildConfig;
import com.zhongyou.meet.mobile.Constant;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.ameeting.adater.NewAudienceVideoAdapter;
import com.zhongyou.meet.mobile.entities.Agora;
import com.zhongyou.meet.mobile.entities.Audience;
import com.zhongyou.meet.mobile.entities.AudienceVideo;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.ChatMesData;
import com.zhongyou.meet.mobile.entities.HostUser;
import com.zhongyou.meet.mobile.entities.Material;
import com.zhongyou.meet.mobile.entities.Meeting;
import com.zhongyou.meet.mobile.entities.MeetingHostingStats;
import com.zhongyou.meet.mobile.entities.MeetingJoin;
import com.zhongyou.meet.mobile.entities.MeetingJoinStats;
import com.zhongyou.meet.mobile.entities.MeetingMaterialsPublish;
import com.zhongyou.meet.mobile.entities.MeetingScreenShot;
import com.zhongyou.meet.mobile.entities.QiniuToken;
import com.zhongyou.meet.mobile.entities.base.BaseBean;
import com.zhongyou.meet.mobile.event.ForumRevokeEvent;
import com.zhongyou.meet.mobile.event.ForumSendEvent;
import com.zhongyou.meet.mobile.meetingcamera.activity.Camera1ByServiceActivity;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.DisplayUtil;
import com.zhongyou.meet.mobile.utils.OkHttpCallback;
import com.zhongyou.meet.mobile.utils.RxBus;
import com.zhongyou.meet.mobile.utils.SizeUtils;
import com.zhongyou.meet.mobile.utils.ToastUtils;
import com.zhongyou.meet.mobile.utils.UIDUtil;
import com.zhongyou.meet.mobile.utils.statistics.ZYAgent;
import com.zhongyou.meet.mobile.view.MyGridLayoutHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import cn.bingoogolapple.transformerstip.TransformersTip;
import cn.bingoogolapple.transformerstip.gravity.TipGravity;
import io.agora.AgoraAPI;
import io.agora.AgoraAPIOnlySignal;
import io.agora.openlive.model.AGEventHandler;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.internal.RtcEngineMessage;
import io.agora.rtc.video.VideoCanvas;
import rx.Subscription;
import rx.functions.Action1;

public class MeetingAudienceActivity extends BaseActivity implements AGEventHandler {

	private final String TAG = MeetingAudienceActivity.class.getSimpleName();
	private Timer takePhotoTimer;
	private TimerTask takePhotoTimerTask;
	private final int CODE_REQUEST_TAKEPHOTO = 8011;

	private MeetingJoin meetingJoin;
	private Meeting meeting;
	private Agora agora;
	private String broadcastId;

	private FrameLayout broadcasterLayout, localAudienceFrameView;
	private TextView broadcastNameText, broadcastTipsText, countText;
	private Button requestTalkButton, stopTalkButton, disCussButton, fullScreenButton;
	private TextView exitButton, pageText, tvChat, tvChatName, tvChatAddress, tvName, tvAddress, tvContent, tvOpenComment;

	private ImageView docImage;

	private boolean isDocShow = false;

	private Material currentMaterial;
	private MeetingMaterialsPublish currentMaterialPublish;
	private int doc_index = 0;

	private String channelName;

	private String audienceName;

	private Subscription subscription;
	private SurfaceView remoteBroadcasterSurfaceView, remoteAudienceSurfaceView, localSurfaceView;

	private AgoraAPIOnlySignal agoraAPI;
	private LinearLayout llMsg, llChat, llSmallChat;
	private static final String DOC_INFO = "doc_info";
	private static final String CALLING_AUDIENCE = "calling_audience";
	private int count = 0;//当意外退出时  再重新进入会议 会重复收到几条一样的消息 导致视频画面可能不可见 使用次数来判断
	private int currentAudienceId;
	InMeetChatFragment fragment;
	boolean hideFragment = false;
	boolean JoinSuc = false;
	private com.elvishew.xlog.Logger mLogger;
	private MyGridLayoutHelper mGridLayoutHelper;
	private VirtualLayoutManager mVirtualLayoutManager;
	private DelegateAdapter mDelegateAdapter;
	private RecyclerView mAudienceRecyclerView;
	private NewAudienceVideoAdapter mVideoAdapter;
	private AudienceVideo mCurrentAudienceVideo;
	private SurfaceView mAudienceVideoSurfaceView;
	private AudienceVideo mLocalAudienceVideo;
	private Button mMuteAudio;
	private boolean isMuted, isSplitView;
	private TextView mSwtichCamera;
	private SizeUtils mSizeUtils;
	private TransformersTip mTransformersTipPop;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_meeting_audience);
		TCAgent.onEvent(this, "进入会议直播界面");
//        if (!WSService.isOnline()) {
//            //当前状态离线,可切换在线
//            ZYAgent.onEvent(this, "在线按钮,当前离线,切换到在线");
//            Log.i(TAG, "当前状态离线,可切换在线");
//            RxBus.sendMessage(new SetUserChatEvent(true));
//        } else {
//            ZYAgent.onEvent(this, "在线按钮,当前在线,,无效操作");
//        }

		registerReceiver(homeKeyEventReceiver, new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
		mSizeUtils = new SizeUtils(this);
		subscription = RxBus.handleMessage(new Action1() {
			@Override
			public void call(Object o) {
				String meetingid = "";
				if (((MeetingJoin) (getIntent().getParcelableExtra("meeting"))) != null && ((MeetingJoin) (getIntent().getParcelableExtra("meeting"))).getMeeting() != null) {
					meetingid = ((MeetingJoin) (getIntent().getParcelableExtra("meeting"))).getMeeting().getId();
				}

				if (o instanceof ForumSendEvent) {

					if (((ForumSendEvent) o).getEntity().getMeetingId().equals(meetingid)) {
						Message msg = new Message();
						msg.obj = ((ForumSendEvent) o).getEntity();
//                    tvChat.setText(((ForumSendEvent) o).getEntity().getContent());
						ChatHandler.sendMessage(msg);
					}


				} else if (o instanceof ForumRevokeEvent) {
//                    requestRecordOnlyLast(true);

					if (((ForumRevokeEvent) o).getEntity().getMeetingId().equals(meetingid)) {
						Message msg = new Message();
						msg.obj = ((ForumRevokeEvent) o).getEntity();
						ChatHandler.sendMessage(msg);
					}

				}

			}
		});
		localAudienceFrameView = findViewById(R.id.localAudienceFrameView);

		mAudienceRecyclerView = findViewById(R.id.audience_list);

		mGridLayoutHelper = new MyGridLayoutHelper(2);
		mGridLayoutHelper.setGap(10);
		mGridLayoutHelper.setItemCount(8);
		mGridLayoutHelper.setAutoExpand(false);

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(DisplayUtil.dip2px(this, 240), RelativeLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		layoutParams.setMargins(0, DisplayUtil.dip2px(this, 40), DisplayUtil.dip2px(this, 10), DisplayUtil.dip2px(this, 60));
		mAudienceRecyclerView.setLayoutParams(layoutParams);

		RelativeLayout relative = findViewById(R.id.relative);

		FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) relative.getLayoutParams();
		layoutParams1.setMargins(0, 0, 0, 0);
		relative.setLayoutParams(layoutParams1);


		mVirtualLayoutManager = new VirtualLayoutManager(this);
		mDelegateAdapter = new DelegateAdapter(mVirtualLayoutManager);
		RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
		mAudienceRecyclerView.setRecycledViewPool(viewPool);
		viewPool.setMaxRecycledViews(0, 8);
		mAudienceRecyclerView.setLayoutManager(mVirtualLayoutManager);

		mVideoAdapter = new NewAudienceVideoAdapter(this, mGridLayoutHelper);
		mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
		mDelegateAdapter.addAdapter(mVideoAdapter);
		mAudienceRecyclerView.setAdapter(mDelegateAdapter);

		mVideoAdapter.setOnDoucleClickListener((parent, view, position) -> {
			if (isSplitView || isDocShow) {
				return;
			}
			if (mVideoAdapter.isHaveChairMan()) {

				if (isHostCommeIn) {
					broadcastTipsText.setVisibility(View.GONE);
				} else {
					broadcastTipsText.setVisibility(View.VISIBLE);
					count = 0;
				}
				//点击的如果是主持人
				if (mVideoAdapter.getAudienceVideoLists().get(position).isBroadcaster()) {
					if (mCurrentAudienceVideo != null) {
						mVideoAdapter.removeItem(position);
						mVideoAdapter.insertItem(position, mCurrentAudienceVideo);
						broadcasterLayout.removeAllViews();
						stripSurfaceView(remoteAudienceSurfaceView);
						stripSurfaceView(remoteBroadcasterSurfaceView);
						if (remoteBroadcasterSurfaceView != null) {
							broadcasterLayout.addView(remoteBroadcasterSurfaceView);
						}
					}
					mCurrentAudienceVideo = null;
					return;
				} else {
					//如果点击的不是主持人 先将大的画面broadcasterView   添加到列表中
					// 然后再将点击的画面添加到大的broadcasterView中 主持人的画面再添加到列表中去
					if (mCurrentAudienceVideo != null) {
						AudienceVideo audienceVideo = new AudienceVideo();
						audienceVideo.setUid(mCurrentAudienceVideo.getUid());
						audienceVideo.setName(mCurrentAudienceVideo.getName());
						audienceVideo.setBroadcaster(false);
						stripSurfaceView(mAudienceVideoSurfaceView);
						audienceVideo.setSurfaceView(mAudienceVideoSurfaceView);
						int i = (int) mAudienceVideoSurfaceView.getTag();
						mVideoAdapter.removeItem(i);
						mVideoAdapter.insertItem(i, audienceVideo);

					}

				}
			}
			broadcastTipsText.setVisibility(View.GONE);
			//将参会人的画面移到主持人界面
			broadcasterLayout.removeAllViews();
			mCurrentAudienceVideo = mVideoAdapter.getAudienceVideoLists().get(position);
			mAudienceVideoSurfaceView = mCurrentAudienceVideo.getSurfaceView();
			mAudienceVideoSurfaceView.setTag(position);

			mVideoAdapter.removeItem(position);
			stripSurfaceView(mAudienceVideoSurfaceView);
			broadcasterLayout.addView(mAudienceVideoSurfaceView);


			stripSurfaceView(remoteBroadcasterSurfaceView);
			//主持人画面 加入到列表中
			AudienceVideo audienceVideo = new AudienceVideo();
			audienceVideo.setUid(config().mUid);
			audienceVideo.setName("主持人" + meetingJoin.getHostUser().getHostUserName());
			audienceVideo.setBroadcaster(true);
			audienceVideo.setSurfaceView(remoteBroadcasterSurfaceView);
			mVideoAdapter.insetChairMan(position, audienceVideo);
		});

		mLogger = XLog.tag(TAG)
				.t()
				.st(2)
				.b()
				.build();

		showOperatorHandler.sendEmptyMessageDelayed(0, 5000);
	}

	private void stripSurfaceView(SurfaceView view) {
		if (view == null) {
			mLogger.e("view==null");
			return;
		}
		ViewParent parent = view.getParent();
		if (parent != null) {
			((FrameLayout) parent).removeView(view);
		}
	}

	@SuppressLint("HandlerLeak")
	private Handler ChatHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 23) {
				llChat.setVisibility(View.INVISIBLE);
				return;
			}
			if (msg.what == 22) {
				/*Log.v("llchat989890", tvChat.getWidth() + "****tvChat***后");
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
				Log.v("llchat9898llChat", findViewById(R.id.small_chat).getWidth() + "*******后");
				Log.v("llchat9898", disCussButton.getLeft() + "*******2");
				params.bottomMargin = 166;
				params.gravity = Gravity.BOTTOM;
				params.leftMargin = disCussButton.getLeft() - (llSmallChat.getWidth() / 2) + disCussButton.getWidth() / 2;
				Log.v("llchat9898", params.leftMargin + "*******3");
				if (params.leftMargin < 90) {
					params.leftMargin = 90;
					LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					params2.leftMargin = disCussButton.getLeft() - 90 + (disCussButton.getWidth() / 2);
					findViewById(R.id.img_tri).setLayoutParams(params2);
					llChat.setLayoutParams(params);
				} else {
					LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					params2.gravity = Gravity.CENTER_HORIZONTAL;
					findViewById(R.id.img_tri).setLayoutParams(params2);
					llChat.setLayoutParams(params);
				}*/
				FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) llChat.getLayoutParams();
				int[] location = DisplayUtil.getLocation(disCussButton);

				layoutParams.setMargins(location[0] - 5 + disCussButton.getWidth() / 4, 0, 0, 0);
				llChat.setLayoutParams(layoutParams);
				return;
			}
			if (hideFragment) {
				llMsg.setVisibility(View.GONE);
				llChat.setVisibility(View.INVISIBLE);
			} else {
				if (isFullScreen) {
					llMsg.setVisibility(View.VISIBLE);
					llChat.setVisibility(View.INVISIBLE);
				} else {
					llMsg.setVisibility(View.GONE);
					llChat.setVisibility(View.VISIBLE);
					if (ChatHandler.hasMessages(23)) {
						ChatHandler.removeMessages(23);
					}
					ChatHandler.sendEmptyMessageDelayed(23, 5100);
					tvChat.setVisibility(View.VISIBLE);
				}
			}
			if (((ChatMesData.PageDataEntity) msg.obj).getType() == 1) {
				tvChat.setTextColor(getResources().getColor(R.color.color_7FBAFF));
				tvChat.setText("[发送一张图片]");
			} else {
				tvChat.setTextColor(getResources().getColor(R.color.white));
				tvChat.setText(((ChatMesData.PageDataEntity) msg.obj).getContent());
			}
//            tvChat.setText(" : "+((ChatMesData.PageDataEntity)msg.obj).getContent());
			tvAddress.setText("未填写");
			tvName.setText(((ChatMesData.PageDataEntity) msg.obj).getUserName() + "");

			if (((ChatMesData.PageDataEntity) msg.obj).getType() == 1) {
				tvContent.setTextColor(getResources().getColor(R.color.color_7FBAFF));
				tvContent.setText(" ：[发送一张图片]");
			} else {
				tvContent.setTextColor(getResources().getColor(R.color.white));
				tvContent.setText(" : " + ((ChatMesData.PageDataEntity) msg.obj).getContent());
			}

			if (((ChatMesData.PageDataEntity) msg.obj).getMsgType() == 1) {
				tvContent.setTextColor(getResources().getColor(R.color.color_7FBAFF));
				tvContent.setText(" ：[撤回一条消息]");
			}

			if (((ChatMesData.PageDataEntity) msg.obj).getMsgType() == 1) {
				tvChat.setTextColor(getResources().getColor(R.color.color_7FBAFF));
				tvChat.setText("[撤回一条消息]");
			}
			tvChatAddress.setText("未填写");
			tvChatName.setText(((ChatMesData.PageDataEntity) msg.obj).getUserName() + " : ");
			ChatHandler.sendEmptyMessageDelayed(22, 100);
		}
	};


	@Override
	protected void onResume() {
		super.onResume();
		TCAgent.onPageStart(this, "视频通话");
	}

	private void setTextViewDrawableTop(TextView view, int drawable) {
		Drawable top = getResources().getDrawable(drawable);
		view.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
	}

	@Override
	protected void onPause() {
		super.onPause();
		TCAgent.onPageEnd(this, "视频通话");
	}

	private boolean handsUp = false;
	private boolean isFullScreen = false;

	@Override
	protected void initUIandEvent() {
		event().addEventHandler(this);

		Intent intent = getIntent();
		agora = intent.getParcelableExtra("agora");
		meetingJoin = intent.getParcelableExtra("meeting");
		meeting = meetingJoin.getMeeting();

		broadcastId = meetingJoin.getHostUser().getClientUid();

		config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));
		Log.v("uid--->", "" + config().mUid);

		channelName = meetingJoin.getMeeting().getId();
		fragment = InMeetChatFragment.newInstance(meetingJoin.getMeeting().getId());
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.add(R.id.rl_content, fragment);
		fragmentTransaction.hide(fragment);
		fragmentTransaction.commitAllowingStateLoss();
		audienceName = (TextUtils.isEmpty(Preferences.getAreaName()) ? "" : Preferences.getAreaName()) + "-" + (TextUtils.isEmpty(Preferences.getUserCustom()) ? "" : Preferences.getUserCustom()) + "-" + Preferences.getUserName();

		broadcasterLayout = findViewById(R.id.broadcaster_view);
		broadcasterLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				hideFragment();
				if (isFullScreen) {
					if (!tvContent.getText().toString().isEmpty())
						llMsg.setVisibility(View.VISIBLE);

				} else {
//                    if (!tvChat.getText().toString().isEmpty())
//                        llChat.setVisibility(View.VISIBLE);

				}
			}
		});
		broadcastTipsText = findViewById(R.id.broadcast_tips);
		broadcastNameText = findViewById(R.id.broadcaster);
		tvChat = findViewById(R.id.tv_chat);
		broadcastNameText.setText("主持人：" + meetingJoin.getHostUser().getHostUserName());
		docImage = findViewById(R.id.doc_image);
		docImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				hideFragment();
				if (isFullScreen) {
					if (!tvContent.getText().toString().isEmpty())
						llMsg.setVisibility(View.VISIBLE);
				} else {
//                    if (!tvChat.getText().toString().isEmpty())
//                        llChat.setVisibility(View.VISIBLE);
				}
			}
		});
		pageText = findViewById(R.id.page);
		llMsg = findViewById(R.id.ll_msg);
		llChat = findViewById(R.id.ll_chat);

		llSmallChat = findViewById(R.id.small_chat);
		tvChatAddress = findViewById(R.id.tv_chat_address);
		tvChatName = findViewById(R.id.tv_chat_name);
		tvName = findViewById(R.id.tv_name);
		tvAddress = findViewById(R.id.tv_addres);
		tvContent = findViewById(R.id.tv_content);
		tvOpenComment = findViewById(R.id.open_comment);
		disCussButton = findViewById(R.id.discuss);
		disCussButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				initFragment();
				llChat.setVisibility(View.INVISIBLE);
			}
		});
		tvOpenComment.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				initFragment();
				llMsg.setVisibility(View.GONE);
			}
		});

		fullScreenButton = findViewById(R.id.full_screen);

		fullScreenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isDocShow) {


					if (isFullScreen) {
						isFullScreen = false;
						fullScreenButton.setText("全屏");
						findViewById(R.id.toolsbar).setVisibility(View.VISIBLE);
						if (isDocShow) {

						} else {
							mVideoAdapter.setVisibility(View.VISIBLE);
							mAudienceRecyclerView.setVisibility(View.VISIBLE);
						}


					} else {
						isFullScreen = true;
						fullScreenButton.setText("恢复");
						findViewById(R.id.toolsbar).setVisibility(View.GONE);
						mVideoAdapter.setVisibility(View.GONE);
						mAudienceRecyclerView.setVisibility(View.GONE);

					}
				} else {
					mTransformersTipPop = new TransformersTip(v, R.layout.pop_full_screen_choose) {
						@Override
						protected void initView(View contentView) {
							contentView.findViewById(R.id.not_full_screen).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									mTransformersTipPop.dismissTip();

									notFullScreenState();

								}
							});
							contentView.findViewById(R.id.full_screen).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									mTransformersTipPop.dismissTip();
									FullScreenState();
								}
							});

							contentView.findViewById(R.id.clearAll).setOnClickListener(new View.OnClickListener() {
								@Override
								public void onClick(View v) {
									mTransformersTipPop.dismissTip();
									clearAllState();
								}
							});
						}
					}
							.setTipGravity(TipGravity.TO_TOP_CENTER) // 设置浮窗相对于锚点控件展示的位置
							/*.setTipOffsetXDp(0) // 设置浮窗在 x 轴的偏移量
							.setTipOffsetYDp(-6) // 设置浮窗在 y 轴的偏移量*/

							.setBackgroundDimEnabled(false) // 设置是否允许浮窗的背景变暗
							.setDismissOnTouchOutside(true);
					if (!mTransformersTipPop.isShowing()) {
						mTransformersTipPop // 设置点击浮窗外部时是否自动关闭浮窗
								.show(); // 显示浮窗
					}
				}

			}
		});


		countText = findViewById(R.id.online_count);

		stopTalkButton = findViewById(R.id.stop_talk);
		stopTalkButton.setOnClickListener(view -> {
			showDialog(2, "确定终止发言吗？", "取消", "确定", null);
		});

		requestTalkButton = findViewById(R.id.request_talk);
		requestTalkButton.setOnClickListener(view -> {
			if (remoteBroadcasterSurfaceView != null) {
				if (handsUp) {
					handsUp = false;
					requestTalkButton.setText("申请发言");
				} else {
					handsUp = true;
					requestTalkButton.setText("取消申请");
				}
				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("uid", config().mUid);
					jsonObject.put("uname", audienceName);
					jsonObject.put("handsUp", handsUp);
					jsonObject.put("callStatus", 0);
					jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
					jsonObject.put("postTypeName", Preferences.getUserPostType());
					if (Constant.videoType == 1) {
						jsonObject.put("isAudience", false);
					} else {
						jsonObject.put("isAudience", true);
					}

					agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				Toast.makeText(MeetingAudienceActivity.this, "主持人加入才能申请发言", Toast.LENGTH_SHORT).show();
			}
		});

		exitButton = findViewById(R.id.exit);
		mMuteAudio = findViewById(R.id.mute_audio);
		mSwtichCamera = findViewById(R.id.switch_camera);
		mSwtichCamera.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				rtcEngine().switchCamera();
			}
		});
		mMuteAudio.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isMuted) {
					isMuted = true;
					setTextViewDrawableTop(mMuteAudio, R.drawable.icon_unspeek);
					mMuteAudio.setText("话筒关闭");
				} else {
					isMuted = false;
					setTextViewDrawableTop(mMuteAudio, R.drawable.icon_speek);
					mMuteAudio.setText("话筒打开");
				}
				rtcEngine().muteLocalAudioStream(isMuted);
			}
		});
		exitButton.setOnClickListener(view -> {
			showDialog(1, "确定退出会议吗？", "取消", "确定", null);
		});
		// 观众的方式进入
		if (Constant.videoType == 2) {
			worker().configEngine(Constants.CLIENT_ROLE_AUDIENCE, Constants.VIDEO_PROFILE_180P);
			mMuteAudio.setVisibility(View.GONE);
			mSwtichCamera.setVisibility(View.GONE);

		} else if (Constant.videoType == 1) {
			//参会人的方式进入
			localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
			rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
			worker().preview(true, localSurfaceView, config().mUid);

			mLocalAudienceVideo = new AudienceVideo();
			mLocalAudienceVideo.setUid(config().mUid);
			mLocalAudienceVideo.setName("参会人" + config().mUid);
			mLocalAudienceVideo.setBroadcaster(false);
			mLocalAudienceVideo.setSurfaceView(localSurfaceView);
			mVideoAdapter.insertItem(mLocalAudienceVideo);
			worker().configEngine(Constants.CLIENT_ROLE_BROADCASTER, Constants.VIDEO_PROFILE_360P);
			rtcEngine().enableAudioVolumeIndication(400, 3);

			requestTalkButton.setVisibility(View.GONE);
			mMuteAudio.setVisibility(View.VISIBLE);
			mSwtichCamera.setVisibility(View.VISIBLE);
		}


		agoraAPI = AgoraAPIOnlySignal.getInstance(this, agora.getAppID());
		agoraAPI.callbackSet(new AgoraAPI.CallBack() {

			@Override
			public void onLoginSuccess(int uid, int fd) {
				super.onLoginSuccess(uid, fd);
				Logger.e("onLoginSuccess   uid:" + uid + " --- " + "fd=" + fd);
				mLogger.e("onLoginSuccess   uid:" + uid + " --- " + "fd=" + fd);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登录信令系统成功", Toast.LENGTH_SHORT).show());
				}
				agoraAPI.channelJoin(channelName);
			}

			@Override
			public void onLoginFailed(final int ecode) {
				super.onLoginFailed(ecode);
				Logger.e("onLoginFailed   ecode:" + ecode);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登录信令系统失败" + ecode, Toast.LENGTH_SHORT).show());
				}
				if ("true".equals(agora.getIsTest())) {
					agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
				} else {
					agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
				}

			}

			@Override
			public void onLogout(int ecode) {
				super.onLogout(ecode);
				Logger.e("onLogout   ecode:" + ecode);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "观众登出信令系统成功" + ecode, Toast.LENGTH_SHORT).show());
				}
			}

			@Override
			public void onChannelJoined(String channelID) {
				super.onChannelJoined(channelID);
				Logger.e("onChannelJoined   channelID:" + channelID);
				runOnUiThread(() -> {
					if (BuildConfig.DEBUG) {
						Toast.makeText(MeetingAudienceActivity.this, "观众登录信令频道成功", Toast.LENGTH_SHORT).show();
					}
					agoraAPI.queryUserStatus(broadcastId);
					agoraAPI.channelQueryUserNum(channelName);
				});
			}

			@Override
			public void onReconnecting(int nretry) {
				super.onReconnecting(nretry);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "信令重连失败第" + nretry + "次", Toast.LENGTH_SHORT).show());
				}
			}

			@Override
			public void onReconnected(int fd) {
				super.onReconnected(fd);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "信令系统重连成功", Toast.LENGTH_SHORT).show());
				}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						agoraAPI.channelJoin(channelName);
					}
				});

			}

			@Override
			public void onQueryUserStatusResult(String name, String status) {
				super.onQueryUserStatusResult(name, status);
				if (name.equals(meetingJoin.getHostUser().getClientUid()) && "1".equals(status)) {
					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("uid", config().mUid);
						jsonObject.put("uname", audienceName);

						if (Constant.videoType == 1) {
							jsonObject.put("callStatus", 2);
							jsonObject.put("isAudience", false);
						} else {
							if (currentAudienceId == config().mUid) {
								jsonObject.put("callStatus", 2);
								jsonObject.put("isAudience", true);
							} else {
								jsonObject.put("callStatus", 0);
								jsonObject.put("isAudience", true);
							}

						}

						jsonObject.put("handsUp", handsUp);
						jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
						jsonObject.put("postTypeName", Preferences.getUserPostType());
						agoraAPI.messageInstantSend(name, 0, jsonObject.toString(), "");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onChannelJoinFailed(String channelID, int ecode) {
				super.onChannelJoinFailed(channelID, ecode);
				runOnUiThread(() -> {
					if (BuildConfig.DEBUG) {
						Toast.makeText(MeetingAudienceActivity.this, "观众登录信令频道失败", Toast.LENGTH_SHORT).show();
					}
				});
			}

			@Override
			public void onChannelQueryUserNumResult(String channelID, int ecode, final int num) {
				super.onChannelQueryUserNumResult(channelID, ecode, num);
				runOnUiThread(() -> countText.setText("在线人数：" + num));
			}

			@Override
			public void onChannelUserJoined(String account, int uid) {
				super.onChannelUserJoined(account, uid);
				runOnUiThread(() -> {
					mLogger.e("onChannelUserJoined", "观众" + account + "加入房间了---" + meetingJoin.getHostUser().getClientUid());
					agoraAPI.channelQueryUserNum(channelName);
				});
			}

			@Override
			public void onChannelUserLeaved(String account, int uid) {
				super.onChannelUserLeaved(account, uid);

				mLogger.e("用户account:" + account + "---" + "uid:" + uid + "退出信令频道");
				runOnUiThread(() -> {
					if (BuildConfig.DEBUG) {
						Toast.makeText(MeetingAudienceActivity.this, "用户" + account + "退出信令频道", Toast.LENGTH_SHORT).show();
					}
					if (account.equals(broadcastId)) {
						isDocShow = false;
						isHostCommeIn = false;
						broadcasterLayout.removeView(remoteBroadcasterSurfaceView);
						broadcasterLayout.setVisibility(View.VISIBLE);

						docImage.setVisibility(View.GONE);
						onUserOffline(Integer.parseInt(account), Constants.USER_OFFLINE_QUIT);

					}
					agoraAPI.channelQueryUserNum(channelName);

				});
			}

			@Override
			public void onUserAttrResult(final String account, final String name, final String value) {
				super.onUserAttrResult(account, name, value);
				mLogger.e("获取到用户" + account + "的属性" + name + "的值为" + value);
				runOnUiThread(() -> {
					if (BuildConfig.DEBUG) {
						Toast.makeText(MeetingAudienceActivity.this, "获取到用户" + account + "的属性" + name + "的值为" + value, Toast.LENGTH_SHORT).show();
					}


				});
			}

			@Override
			public void onMessageSendSuccess(String messageID) {
				super.onMessageSendSuccess(messageID);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> {
						Toast.makeText(MeetingAudienceActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
					});
				}
			}

			@Override
			public void onMessageSendError(String messageID, int ecode) {
				super.onMessageSendError(messageID, ecode);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> {
						Toast.makeText(MeetingAudienceActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
					});
				}
			}

			@Override
			public void onMessageInstantReceive(final String account, final int uid, final String msg) {
				super.onMessageInstantReceive(account, uid, msg);
				com.orhanobut.logger.Logger.e(msg);
				mLogger.e("account:==%s,uid:==%d,msg:==%s", account, uid, msg);
				runOnUiThread(() -> {
					try {
						if (BuildConfig.DEBUG) {
							Toast.makeText(MeetingAudienceActivity.this, "接收到消息" + msg, Toast.LENGTH_SHORT).show();
						}
						JSONObject jsonObject = new JSONObject(msg);
						Logger.e(jsonObject.toString());

						if (jsonObject.has("response")) {
							boolean result = jsonObject.getBoolean("response");
							if (result) { // 连麦成功
								if (BuildConfig.DEBUG) {
									Toast.makeText(MeetingAudienceActivity.this, "主持人要和我连麦", Toast.LENGTH_SHORT).show();
								}

								mMuteAudio.setVisibility(View.VISIBLE);
								mSwtichCamera.setVisibility(View.VISIBLE);


								remoteAudienceSurfaceView = null;

								agoraAPI.setAttr("uname", audienceName); // 设置当前登录用户的相关属性值。
								localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
								localSurfaceView.setZOrderOnTop(true);
								localSurfaceView.setZOrderMediaOverlay(true);
								rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));


								stopTalkButton.setVisibility(View.VISIBLE);
								requestTalkButton.setVisibility(View.GONE);

								worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

								HashMap<String, Object> params = new HashMap<String, Object>();
								params.put("status", 1);
								params.put("meetingId", meetingJoin.getMeeting().getId());
								ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
							} else {
								if (BuildConfig.DEBUG) {
									Toast.makeText(MeetingAudienceActivity.this, "主持人拒绝和我连麦", Toast.LENGTH_SHORT).show();
								}
								stopTalkButton.setVisibility(View.GONE);
								requestTalkButton.setVisibility(View.VISIBLE);
								if (Constant.videoType == 2) {
									mMuteAudio.setVisibility(View.GONE);
									mSwtichCamera.setVisibility(View.GONE);
								} else if (Constant.videoType == 1) {
									mMuteAudio.setVisibility(View.VISIBLE);
									mSwtichCamera.setVisibility(View.VISIBLE);
								}
							}
							handsUp = false;
							requestTalkButton.setText("申请发言");
						}
						if (jsonObject.has("finish")) {
							boolean finish = jsonObject.getBoolean("finish");
							if (finish) {
								mLogger.e("当前观众的ID是:" + currentAudienceId);
								//account:==242007174,uid:==0,msg:=={"finish":true}
								//此时观众在列表中
								/*观众此时在列表中*/

								if (!isHostCommeIn) {
									//主持人离开了
									if (mVideoAdapter.isHaveChairMan()) {
										int chairManPosition = mVideoAdapter.getChairManPosition();
										if (chairManPosition != -1) {
											mVideoAdapter.removeItem(chairManPosition);
										}
									} else {
										if (currentAudienceId != 0) {
											mVideoAdapter.deleteItemById(currentAudienceId);
										}

									}
									broadcasterLayout.removeAllViews();
									broadcasterLayout.setVisibility(View.GONE);
									broadcastTipsText.setVisibility(View.VISIBLE);
									count = 0;

									if (mVideoAdapter.getDataSize() > 0) {
										mAudienceRecyclerView.setVisibility(View.VISIBLE);
										mVideoAdapter.setVisibility(View.VISIBLE);

									} else {
										mAudienceRecyclerView.setVisibility(View.GONE);
										mVideoAdapter.setVisibility(View.GONE);

									}
								}

								agoraAPI.setAttr("uname", null);

								localSurfaceView = null;

//								if (!isDocShow) {
//									fullScreenButton.setVisibility(View.GONE);
//								}


								if (Constant.videoType == 2) {
									mMuteAudio.setVisibility(View.GONE);
									mSwtichCamera.setVisibility(View.GONE);
									requestTalkButton.setVisibility(View.VISIBLE);
									stopTalkButton.setVisibility(View.GONE);
									worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
								} else if (Constant.videoType == 1) {
									mMuteAudio.setVisibility(View.VISIBLE);
									mSwtichCamera.setVisibility(View.VISIBLE);
									worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
								}

								handsUp = false;
								requestTalkButton.setText("申请发言");

								if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
									HashMap<String, Object> params = new HashMap<String, Object>();
									params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
									params.put("status", 2);
									params.put("meetingId", meetingJoin.getMeeting().getId());
									params.put("type", 2);
									params.put("leaveType", 1);
									ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
								}

							}
						}
						/*if (jsonObject.has("getInformation")) {
							JSONObject json = new JSONObject();
							String audienceName = (TextUtils.isEmpty(Preferences.getAreaName()) ? "" : Preferences.getAreaName()) + "-" + (TextUtils.isEmpty(Preferences.getUserCustom()) ? "" : Preferences.getUserCustom()) + "-" + Preferences.getUserName();
							json.put("uid", config().mUid);
							json.put("uname", audienceName);
							json.put("callStatus", 2);
							json.put("returnInformation", 1);
							json.put("auditStatus", Preferences.getUserAuditStatus());
							json.put("postTypeName", Preferences.getUserPostType());
							agoraAPI.messageInstantSend(meetingJoin.getHostUser().getClientUid(), 0, json.toString(), "");
						}*/

					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}

			@Override
			public void onMessageChannelReceive(String channelID, String account, int uid, final String msg) {
				super.onMessageChannelReceive(channelID, account, uid, msg);
				mLogger.e("channelID " + channelID + ", account:" + account + ", uid:" + uid + ", msg:" + msg);
				runOnUiThread(() -> {
					try {
						if (BuildConfig.DEBUG) {
							Toast.makeText(MeetingAudienceActivity.this, "接收到频道消息：" + msg, Toast.LENGTH_SHORT).show();
						}
						JSONObject jsonObject = new JSONObject(msg);
						Logger.e(jsonObject.toString());
						if (jsonObject.has("material_id") && jsonObject.has("doc_index")) {
							agoraAPI.channelQueryUserNum(channelName);
							doc_index = jsonObject.getInt("doc_index");
							String materialId = jsonObject.getString("material_id");
							if (model == 0 || model == 1) {
								notFullScreenState();
							} else if (model == 2) {
								FullScreenState();
							} else if (model == 3) {
								clearAllState();
							}
							if (isSplitView && !isDocShow) {
								fullScreenButton.setVisibility(View.GONE);
							} else {
								fullScreenButton.setVisibility(View.VISIBLE);
							}
							if (currentMaterial != null) {
								if (!materialId.equals(currentMaterial.getId())) {
									ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
								} else {
									currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);
									if (remoteBroadcasterSurfaceView != null) {
										broadcasterLayout.removeView(remoteBroadcasterSurfaceView);
										broadcasterLayout.setVisibility(View.GONE);

									}
									pageText.setVisibility(View.VISIBLE);
									pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
									docImage.setVisibility(View.VISIBLE);
									Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);


								}
							} else {
								ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
							}
						}
						if (jsonObject.has("finish_meeting")) {
							boolean finishMeeting = jsonObject.getBoolean("finish_meeting");
							if (finishMeeting) {
								if (BuildConfig.DEBUG) {
									Toast.makeText(MeetingAudienceActivity.this, "主持人结束了会议", Toast.LENGTH_SHORT).show();
								}
								doLeaveChannel();
								if (agoraAPI.getStatus() == 2) {
									agoraAPI.logout();
								}
								finish();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}

			@Override
			public void onChannelAttrUpdated(String channelID, String name, String value, String type) {
				super.onChannelAttrUpdated(channelID, name, value, type);
				mLogger.e("channelID:" + channelID + ", name:" + name + ", value:" + value + ", type:" + type);
				runOnUiThread(() -> {
					if (CALLING_AUDIENCE.equals(name)) {
						if (TextUtils.isEmpty(value)) {//取消连麦
							if (remoteAudienceSurfaceView != null) {
								remoteAudienceSurfaceView = null;
							}

							if (localSurfaceView != null) {
								worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
								localSurfaceView = null;
								/*if (!isDocShow) {
									fullScreenButton.setVisibility(View.GONE);
								}*/
								agoraAPI.setAttr("uname", null);

								if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
									HashMap<String, Object> params = new HashMap<String, Object>();
									params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
									params.put("status", 2);
									params.put("meetingId", meetingJoin.getMeeting().getId());
									params.put("type", 2);
									params.put("leaveType", 1);
									ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
								}
							}

							stopTalkButton.setVisibility(View.GONE);
							if (Constant.videoType == 2) {
								requestTalkButton.setVisibility(View.VISIBLE);
							} else {
								requestTalkButton.setVisibility(View.GONE);
							}
							setTextViewDrawableTop(mMuteAudio, R.drawable.icon_speek);
							mMuteAudio.setText("话筒打开");

							if (remoteBroadcasterSurfaceView == null) {
								mVideoAdapter.deleteItem(currentAudienceId);
								broadcastTipsText.setVisibility(View.VISIBLE);
								return;
							}
							if (currentAudienceId == config().mUid) {
								/*观众此时在列表中*/
								if (mVideoAdapter.getPositionById(config().mUid) != -1) {
									mVideoAdapter.deleteItem(config().mUid);
									stripSurfaceView(remoteBroadcasterSurfaceView);
									broadcasterLayout.removeAllViews();
									remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
									remoteBroadcasterSurfaceView.setZOrderOnTop(false);
									broadcasterLayout.addView(remoteBroadcasterSurfaceView);
									mLogger.e("观众在列表中");
								} else {
									mLogger.e("观众不  在列表中");
									/*观众不再列表中 此时主持人在列表中*/
									if (mVideoAdapter.isHaveChairMan()) {
										mLogger.e("主持人在列表中");
										int chairManPosition = mVideoAdapter.getChairManPosition();
										if (chairManPosition != -1) {
											stripSurfaceView(remoteBroadcasterSurfaceView);
											remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
											remoteBroadcasterSurfaceView.setZOrderOnTop(false);
											broadcasterLayout.removeAllViews();
											broadcasterLayout.addView(remoteBroadcasterSurfaceView);
											mVideoAdapter.deleteItem(chairManPosition);
										}
									}
								}

								handsUp = false;
								requestTalkButton.setText("申请发言");
								currentAudienceId = 0;
								count = 0;//取消连麦 重置count
								if (isFullScreen || model == 2 || model == 3) {
									mVideoAdapter.setVisibility(View.GONE);
									mAudienceRecyclerView.setVisibility(View.GONE);
								} else {
									mVideoAdapter.setVisibility(View.VISIBLE);
									mAudienceRecyclerView.setVisibility(View.VISIBLE);
								}

							}
						} else {
							if (BuildConfig.DEBUG) {
								Toast.makeText(MeetingAudienceActivity.this, "收到主持人设置的连麦人ID：" + value + ", \ntype:" + type, Toast.LENGTH_SHORT).show();
							}
							currentAudienceId = Integer.parseInt(value);
							if (currentAudienceId == config().mUid) { // 连麦人是我
								mLogger.e("连麦人是我" + currentAudienceId);


								mMuteAudio.setVisibility(View.VISIBLE);
								mSwtichCamera.setVisibility(View.VISIBLE);


								JSONObject jsonObject = new JSONObject();
								try {
									jsonObject.put("uid", config().mUid);
									jsonObject.put("uname", audienceName);
									jsonObject.put("handsUp", handsUp);
									if (Constant.videoType == 1) {
										jsonObject.put("callStatus", 2);
										jsonObject.put("isAudience", false);
									} else {
										if (currentAudienceId == config().mUid) {
											jsonObject.put("callStatus", 2);
											jsonObject.put("isAudience", true);
										} else {
											jsonObject.put("callStatus", 0);
											jsonObject.put("isAudience", true);
										}

									}
									jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
									jsonObject.put("postTypeName", Preferences.getUserPostType());
									agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
								} catch (JSONException e) {
									e.printStackTrace();
								}

								//全屏模式下 列表不可见  在ppt模式下 model==2或者3下 列表不可见

								if (isFullScreen || model == 2 || model == 3) {
									mVideoAdapter.setVisibility(View.GONE);
									mAudienceRecyclerView.setVisibility(View.GONE);
								} else {
									mVideoAdapter.setVisibility(View.VISIBLE);
									mAudienceRecyclerView.setVisibility(View.VISIBLE);
								}

								if (count != 0) {//当意外退出后再进入会议 会收到重复的连麦消息 导致视频画面显示错误 当count不等于0 就不往下走
									return;
								}
								count++;


								if (Constant.videoType == 1) {

								} else if (Constant.videoType == 2) {
									agoraAPI.setAttr("uname", audienceName); // 设置正在连麦的用户名
									remoteAudienceSurfaceView = null;
									localSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
									localSurfaceView.setZOrderOnTop(true);
									localSurfaceView.setZOrderMediaOverlay(true);
									rtcEngine().setupLocalVideo(new VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));

									mLocalAudienceVideo = new AudienceVideo();
									mLocalAudienceVideo.setUid(config().mUid);
									mLocalAudienceVideo.setName("参会人" + config().mUid);
									mLocalAudienceVideo.setBroadcaster(false);
									mLocalAudienceVideo.setSurfaceView(localSurfaceView);
									mVideoAdapter.insertItem(mLocalAudienceVideo);
									requestTalkButton.setVisibility(View.GONE);
									stopTalkButton.setVisibility(View.VISIBLE);
//									fullScreenButton.setVisibility(View.GONE);
									worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

									rtcEngine().muteLocalAudioStream(true);

									HashMap<String, Object> params = new HashMap<String, Object>();
									params.put("status", 1);
									params.put("meetingId", meetingJoin.getMeeting().getId());
									ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);


								}


							} else {  // 连麦人不是我
								mLogger.e("连麦人不是我！！" + currentAudienceId);
								count = 0;//连麦人不是我 重置count
								if (Constant.videoType == 2) {
									worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
									requestTalkButton.setVisibility(View.VISIBLE);
									requestTalkButton.setText("申请发言");
									stopTalkButton.setVisibility(View.GONE);
									//参会人（我）此时在列表中
									int positionById = mVideoAdapter.getPositionById(config().mUid);
									if (positionById != -1) {
										AudienceVideo video = mVideoAdapter.getAudienceVideoLists().get(positionById);
										if (video != null && video.getSurfaceView() != null) {
											video.getSurfaceView().setZOrderMediaOverlay(false);
											video.getSurfaceView().setZOrderOnTop(false);
										}
										mVideoAdapter.deleteItemById(config().mUid);
									} else {
										//参会人（我）此时在大的视图里面 主持人在列表中
										if (mVideoAdapter.isHaveChairMan()) {
											int chairManPosition = mVideoAdapter.getChairManPosition();
											if (chairManPosition != -1) {
												AudienceVideo chairManVideo = mVideoAdapter.getAudienceVideoLists().get(chairManPosition);
												if (chairManVideo != null && chairManVideo.getSurfaceView() != null) {
													chairManVideo.getSurfaceView().setZOrderMediaOverlay(false);
													chairManVideo.getSurfaceView().setZOrderOnTop(false);
													stripSurfaceView(chairManVideo.getSurfaceView());
												}
												broadcasterLayout.removeAllViews();
												broadcasterLayout.addView(remoteBroadcasterSurfaceView);
											}

										}
									}

								} else {
									worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
									requestTalkButton.setVisibility(View.GONE);
									stopTalkButton.setVisibility(View.GONE);
								}
								agoraAPI.setAttr("uname", null);

								if (Constant.videoType == 2) {
									mMuteAudio.setVisibility(View.GONE);
									mSwtichCamera.setVisibility(View.GONE);
								} else if (Constant.videoType == 1) {
									mMuteAudio.setVisibility(View.VISIBLE);
									mSwtichCamera.setVisibility(View.VISIBLE);
								}

								if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
									HashMap<String, Object> params = new HashMap<String, Object>();
									params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
									params.put("status", 2);
									params.put("meetingId", meetingJoin.getMeeting().getId());
									params.put("type", 2);
									params.put("leaveType", 1);
									ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
								}
								if (localSurfaceView != null) {
									localSurfaceView = null;
								}

								//全屏模式下 列表不可见  在ppt模式下 model==2或者3下 列表不可见

								if (isFullScreen || model == 2 || model == 3) {
									mVideoAdapter.setVisibility(View.GONE);
									mAudienceRecyclerView.setVisibility(View.GONE);
								} else {
									mVideoAdapter.setVisibility(View.VISIBLE);
									mAudienceRecyclerView.setVisibility(View.VISIBLE);
								}

							}
						}
					}
					if ("doc_info".equals(name)) {
						agoraAPI.channelQueryUserNum(channelName);
						if (!TextUtils.isEmpty(value)) {
							mLogger.e("当前是ppt模式");
							isDocShow = true;
							try {
								JSONObject jsonObject = new JSONObject(value);
								if (jsonObject.has("material_id") && jsonObject.has("doc_index")) {

									doc_index = jsonObject.getInt("doc_index");
									if (BuildConfig.DEBUG) {
										Toast.makeText(MeetingAudienceActivity.this, "收到主持人端index：" + doc_index, Toast.LENGTH_SHORT).show();
									}
									String materialId = jsonObject.getString("material_id");
								/*	mAudienceRecyclerView.setVisibility(View.GONE);
									mVideoAdapter.setVisibility(View.GONE);*/

									notFullScreenState();

									if (isSplitView && !isDocShow) {
										fullScreenButton.setVisibility(View.GONE);
									} else {
										fullScreenButton.setVisibility(View.VISIBLE);
									}
									if (Constant.videoType == 1) {
										stopTalkButton.setVisibility(View.GONE);
									} else if (currentAudienceId == config().mUid) {
										stopTalkButton.setVisibility(View.VISIBLE);
									}

									if (currentMaterial != null) {
										if (!materialId.equals(currentMaterial.getId())) {
											ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
										} else {
											currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);

											if (remoteBroadcasterSurfaceView != null) {
												broadcasterLayout.removeView(remoteBroadcasterSurfaceView);
												broadcasterLayout.setVisibility(View.GONE);

											}
											pageText.setVisibility(View.VISIBLE);
											pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
											docImage.setVisibility(View.VISIBLE);
											Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);


										}
									} else {
										ApiClient.getInstance().meetingMaterial(TAG, meetingMaterialCallback, materialId);
									}
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						} else {
							mLogger.e("当前是取消ppt模式");
							model=0;
							pageText.setVisibility(View.GONE);
							docImage.setVisibility(View.GONE);

							if (Constant.videoType == 1) {
								stopTalkButton.setVisibility(View.GONE);
							} else if (currentAudienceId == config().mUid) {
								stopTalkButton.setVisibility(View.VISIBLE);
							}
							currentMaterial = null;
							currentMaterialPublish = null;

							isDocShow = false;
							if (isSplitView) {
								fullScreenButton.setVisibility(View.GONE);
							} else {
								fullScreenButton.setVisibility(View.VISIBLE);
							}

							//退出ppt模式
							//需要判断自己是否是参会人或者连麦人 如果是 需要将自己加入到adapter中
							//还需要判断当前是否是分屏模式 如果是分屏模式 需要重新进行分屏
							remoteBroadcasterSurfaceView.setVisibility(View.VISIBLE);
							if (Constant.videoType == 1 || currentAudienceId == config().mUid) {
								if (mVideoAdapter.getPositionById(config().mUid) == -1) {
									AudienceVideo audienceVideo = new AudienceVideo();
									audienceVideo.setUid(config().mUid);
									audienceVideo.setName("参会人" + meetingJoin.getHostUser().getHostUserName());
									audienceVideo.setBroadcaster(false);
									audienceVideo.setSurfaceView(localSurfaceView);
									mVideoAdapter.insertItem(audienceVideo);
								}
							}

							if (isSplitView) {
								exitSpliteMode();
								SpliteViews(1);
							} else {
								//如果不是分屏模式 需要判断主持人是否在列表中
								if (mVideoAdapter.isHaveChairMan()) {
									//主持人在列表中 删除主持人
									int chairManPosition = mVideoAdapter.getChairManPosition();
									if (chairManPosition != -1) {
										mVideoAdapter.removeItem(chairManPosition);
									}
								}
								//将主持人加入到大的view中
								broadcasterLayout.removeAllViews();
								broadcasterLayout.setVisibility(View.VISIBLE);
								if (remoteBroadcasterSurfaceView != null) {
									stripSurfaceView(remoteBroadcasterSurfaceView);
									remoteBroadcasterSurfaceView.setZOrderOnTop(false);
									remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
									broadcasterLayout.addView(remoteBroadcasterSurfaceView);
								}

							}
							mAudienceRecyclerView.setVisibility(View.VISIBLE);
							mVideoAdapter.setVisibility(View.VISIBLE);


						}
					}

					if (Constant.MODEL_CHANGE.equals(name)) {
						if (value.equals(Constant.EQUALLY)) {//均分
							mLogger.e("均分模式");
							isSplitView = true;
							mVideoAdapter.setVisibility(View.VISIBLE);
							mVideoAdapter.notifyDataSetChanged();
							mAudienceRecyclerView.setVisibility(View.VISIBLE);

							if (!mVideoAdapter.isHaveChairMan()) {
								mLogger.e("均分模式中  列表中没有主持人");
								if (remoteBroadcasterSurfaceView != null) {
									stripSurfaceView(remoteBroadcasterSurfaceView);
								}
								AudienceVideo audienceVideo = new AudienceVideo();
								audienceVideo.setUid(config().mUid);
								audienceVideo.setName("主持人" + meetingJoin.getHostUser().getHostUserName());
								audienceVideo.setBroadcaster(true);
								audienceVideo.setSurfaceView(remoteBroadcasterSurfaceView);
								mVideoAdapter.getAudienceVideoLists().add(audienceVideo);
								mVideoAdapter.notifyDataSetChanged();
								broadcasterLayout.removeAllViews();
							}
							mLogger.e("当前集合的大小=  " + mVideoAdapter.getDataSize());
							if (isHostCommeIn && mVideoAdapter.getDataSize() > 1 && !isDocShow) {
								SpliteViews(5);
							} /*else {
								int chairManPosition = mVideoAdapter.getChairManPosition();
								if (chairManPosition != -1) {
									mVideoAdapter.removeItem(chairManPosition);
								}
								if (remoteBroadcasterSurfaceView != null) {
									stripSurfaceView(remoteBroadcasterSurfaceView);
									broadcasterLayout.removeAllViews();
									broadcasterLayout.addView(remoteBroadcasterSurfaceView);
								} else {
									broadcastTipsText.setVisibility(View.VISIBLE);
								}

							}*/
							if (isDocShow) {
								fullScreenButton.setVisibility(View.VISIBLE);
							} else {
								fullScreenButton.setVisibility(View.GONE);
							}
							mLogger.e("分屏模式");


						} else if (value.equals(Constant.BIGSCREEN)) {//大屏
							mLogger.e("大屏模式");
							isSplitView = false;
							mVideoAdapter.notifyDataSetChanged();
							if (isFullScreen || model == 2 || model == 3) {
								mVideoAdapter.setVisibility(View.GONE);
								mAudienceRecyclerView.setVisibility(View.GONE);
							} else {
								mVideoAdapter.setVisibility(View.VISIBLE);
								mAudienceRecyclerView.setVisibility(View.VISIBLE);
							}

							fullScreenButton.setVisibility(View.VISIBLE);
							if (!isDocShow) {
								exitSpliteMode();
							}
						}


					}


					if (TextUtils.isEmpty(name) && type.equals("clear")) {
						isDocShow = false;

						if (Constant.videoType == 2) {
							requestTalkButton.setVisibility(View.VISIBLE);
						} else {
							requestTalkButton.setVisibility(View.GONE);
						}
						docImage.setVisibility(View.GONE);
						pageText.setVisibility(View.GONE);

						if (Constant.videoType == 2) {
							mMuteAudio.setVisibility(View.GONE);
							mSwtichCamera.setVisibility(View.GONE);
						} else if (Constant.videoType == 1) {
							mMuteAudio.setVisibility(View.VISIBLE);
							mSwtichCamera.setVisibility(View.VISIBLE);
						}
						setTextViewDrawableTop(mMuteAudio, R.drawable.icon_speek);
						mMuteAudio.setText("话筒打开");
						if (isSplitView && !isDocShow) {
							fullScreenButton.setVisibility(View.GONE);
						} else {
							fullScreenButton.setVisibility(View.VISIBLE);
						}
						currentAudienceId = 0;
						currentMaterial = null;



						/*	*//*观众此时在列表中*//*
						if (mVideoAdapter.getPositionById(currentAudienceId) != -1 && remoteBroadcasterSurfaceView != null) {
							AudienceVideo video = mVideoAdapter.getAudienceVideoLists().get(mVideoAdapter.getPositionById(currentAudienceId));
							if (video.getSurfaceView() != null) {
								video.getSurfaceView().setZOrderOnTop(false);
								video.getSurfaceView().setZOrderMediaOverlay(false);
							}
							mVideoAdapter.deleteItem(currentAudienceId);

							stripSurfaceView(remoteBroadcasterSurfaceView);
							broadcasterLayout.removeAllViews();
							remoteBroadcasterSurfaceView.setZOrderOnTop(false);
							remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
							broadcasterLayout.addView(remoteBroadcasterSurfaceView);

							mLogger.e("观众在列表中");
						} else {
							mLogger.e("观众不  在列表中");
							*//*观众不再列表中 此时主持人在列表中*//*
							if (mVideoAdapter.isHaveChairMan()) {
								mLogger.e("主持人在列表中");
								int chairManPosition = mVideoAdapter.getChairManPosition();
								if (chairManPosition != -1) {
									stripSurfaceView(remoteBroadcasterSurfaceView);
									broadcasterLayout.removeAllViews();
									remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
									remoteBroadcasterSurfaceView.setZOrderOnTop(false);
									broadcasterLayout.addView(remoteBroadcasterSurfaceView);
									mVideoAdapter.deleteItem(chairManPosition);
								}
							} else {
								if (remoteBroadcasterSurfaceView != null) {
									stripSurfaceView(remoteBroadcasterSurfaceView);
									broadcasterLayout.setVisibility(View.VISIBLE);
									broadcasterLayout.removeAllViews();
									remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
									remoteBroadcasterSurfaceView.setZOrderOnTop(false);
									broadcasterLayout.addView(remoteBroadcasterSurfaceView);

								}
							}
						}*/

						//退出ppt模式
						//需要判断自己是否是参会人或者连麦人 如果是 需要将自己加入到adapter中
						//还需要判断当前是否是分屏模式 如果是分屏模式 需要重新进行分屏

						if (Constant.videoType == 1 || currentAudienceId == config().mUid) {
							if (mVideoAdapter.getPositionById(config().mUid) == -1) {
								AudienceVideo audienceVideo = new AudienceVideo();
								audienceVideo.setUid(config().mUid);
								audienceVideo.setName("参会人" + meetingJoin.getHostUser().getHostUserName());
								audienceVideo.setBroadcaster(false);
								audienceVideo.setSurfaceView(localSurfaceView);
								mVideoAdapter.insertItem(audienceVideo);
							}
						}

						remoteBroadcasterSurfaceView.setVisibility(View.VISIBLE);
						if (isSplitView) {
							SpliteViews(2);
						} else {
							//如果不是分屏模式 需要判断主持人是否在列表中
							if (mVideoAdapter.isHaveChairMan()) {
								//主持人在列表中 删除主持人
								int chairManPosition = mVideoAdapter.getChairManPosition();
								if (chairManPosition != -1) {
									mVideoAdapter.removeItem(chairManPosition);
								}
							}
							//将主持人加入到大的view中
							broadcasterLayout.removeAllViews();
							broadcasterLayout.setVisibility(View.VISIBLE);
							if (remoteBroadcasterSurfaceView != null) {
								stripSurfaceView(remoteBroadcasterSurfaceView);
								remoteBroadcasterSurfaceView.setZOrderOnTop(false);
								remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
								broadcasterLayout.addView(remoteBroadcasterSurfaceView);
							}

						}
						mAudienceRecyclerView.setVisibility(View.VISIBLE);
						mVideoAdapter.setVisibility(View.VISIBLE);


						if (mVideoAdapter.getDataSize() <= 0) {
							mAudienceRecyclerView.setVisibility(View.GONE);
							mVideoAdapter.setVisibility(View.GONE);
						} else {
							mAudienceRecyclerView.setVisibility(View.VISIBLE);
							mVideoAdapter.setVisibility(View.VISIBLE);
						}
						count = 0;//清除所有  重置count
						stopTalkButton.setVisibility(View.GONE);


					}

				});
			}

			@Override
			public void onError(final String name, final int ecode, final String desc) {
				super.onError(name, ecode, desc);
				if (BuildConfig.DEBUG) {
					runOnUiThread(() -> {
						if (ecode != 208)
							Toast.makeText(MeetingAudienceActivity.this, "收到错误信息\nname: " + name + "\necode: " + ecode + "\ndesc: " + desc, Toast.LENGTH_SHORT).show();
					});
				}
				if (ecode == 1002) {
					return;
				}
//                if (agoraAPI.getStatus() != 1 && agoraAPI.getStatus() != 2 && agoraAPI.getStatus() != 3) {
//                    if ("true".equals(agora.getIsTest())) {
//                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, "noneed_token", 0, "", 20, 30);
//                    } else {
//                        agoraAPI.login2(agora.getAppID(), "" + config().mUid, agora.getSignalingKey(), 0, "", 20, 30);
//                    }
//                }
			}

			@Override
			public void onLog(String txt) {
				super.onLog(txt);
				Log.v("audience信令", txt);
			}
		});

		ApiClient.getInstance().getMeetingHost(TAG, meeting.getId(), joinMeetingCallback(0));
		startMeetingCamera(meeting.getScreenshotFrequency());
	}

	private void startMeetingCamera(int screenshotFrequency) {
		if (screenshotFrequency == Meeting.SCREENSHOTFREQUENCY_INVALID) {
			//不抓拍
			return;
		}
		takePhotoTimer = new Timer();
		takePhotoTimerTask = new TimerTask() {
			@Override
			public void run() {
				Intent intent = new Intent(MeetingAudienceActivity.this, Camera1ByServiceActivity.class);
				intent.putExtra(Camera1ByServiceActivity.KEY_IMAGE_COMPRESSION_RATIO, meeting.getScreenshotCompressionRatio());
				startActivityForResult(intent, CODE_REQUEST_TAKEPHOTO);
				overridePendingTransition(0, 0);
			}
		};
		takePhotoTimer.schedule(takePhotoTimerTask, screenshotFrequency * 1000, screenshotFrequency * 1000);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CODE_REQUEST_TAKEPHOTO) {
			try {
				String pictureLocalPath = data.getStringExtra("pictureLocalPath");
				uploadMeetingImageToQiniu(pictureLocalPath);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * 上传参会人参会时图片到七牛服务器
	 *
	 * @param imagePath
	 */
	private void uploadMeetingImageToQiniu(String imagePath) {
		ApiClient.getInstance().requestQiniuToken(TAG, new OkHttpCallback<BaseBean<QiniuToken>>() {

			@Override
			public void onSuccess(BaseBean<QiniuToken> result) {
				String token = result.getData().getToken();
				if (TextUtils.isEmpty(token)) {
					String errorMsg = "七牛token获取错误";
					ZYAgent.onEvent(getApplicationContext(), errorMsg);
					ToastUtils.showToast(errorMsg);
					return;
				}
				Configuration config = new Configuration.Builder().connectTimeout(5).responseTimeout(5).build();
				UploadManager uploadManager = new UploadManager(config);
				ZYAgent.onEvent(getApplicationContext(), "拍照完毕，准备上传本地图片：" + imagePath);

				String uploadKey = BuildConfig.QINIU_IMAGE_UPLOAD_PATH + meeting.getId() + "/" + Preferences.getUserId() + "/" + imagePath.substring(imagePath.lastIndexOf('/') + 1);
				uploadManager.put(new File(imagePath), uploadKey, token, meetingImageUpCompletionHandler, new UploadOptions(null, null, true, new UpProgressHandler() {
					@Override
					public void progress(final String key, final double percent) {
					}
				}, null));
			}
		});
	}

	private UpCompletionHandler meetingImageUpCompletionHandler = new UpCompletionHandler() {
		@Override
		public void complete(String key, ResponseInfo info, JSONObject response) {
			if (info.isNetworkBroken() || info.isServerError()) {
				ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传七牛云失败");
				return;
			}
			if (info.isOK()) {
				String meetingImageUrl = BuildConfig.QINIU_IMAGE_DOMAIN + key;
				ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传七牛云成功，地址：" + meetingImageUrl);
				uploadMeetingImageToServer(meeting.getId(), meetingImageUrl);
			} else {
				ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传七牛云失败");
			}
		}
	};

	/**
	 * 上传参会人参会时图片到Server
	 *
	 * @param meetingId
	 * @param qiniuImageUrl
	 */
	public void uploadMeetingImageToServer(String meetingId, String qiniuImageUrl) {
		Map<String, Object> params = new HashMap<>();
		params.put("meetingId", meetingId);
		params.put("imgUrl", qiniuImageUrl);
		params.put("ts", System.currentTimeMillis());
		ApiClient.getInstance().meetingScreenshot(this, params, uploadMeetingImageToServerCallback);
	}

	private OkHttpCallback<Bucket<MeetingScreenShot>> uploadMeetingImageToServerCallback = new OkHttpCallback<Bucket<MeetingScreenShot>>() {

		@Override
		public void onSuccess(Bucket<MeetingScreenShot> meetingScreenShotBucket) {
			ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传服务器成功");
		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
			ZYAgent.onEvent(getApplicationContext(), "参会人直播图像上传服务器失败，错误提示: " + errorCode + ", Excepton: " + exception.getMessage());
		}
	};
	private boolean isHostCommeIn = false;
	private int model = 0;

	private OkHttpCallback joinMeetingCallback(int uid) {
		return new OkHttpCallback<Bucket<HostUser>>() {

			@Override
			public void onSuccess(Bucket<HostUser> meetingJoinBucket) {
				mLogger.e(JSON.toJSONString(meetingJoinBucket));
				meetingJoin.setHostUser(meetingJoinBucket.getData());
				broadcastId = meetingJoinBucket.getData().getClientUid();
				broadcastNameText.setText("主持人：" + meetingJoinBucket.getData().getHostUserName());
				if (uid != 0 && broadcastId != null) {
					Logger.e("uid:  " + uid + "---------" + "broadcastId:" + broadcastId);
					if (String.valueOf(uid).equals(broadcastId)) {
						if (BuildConfig.DEBUG) {
							mLogger.e("主持人进入");
							Toast.makeText(MeetingAudienceActivity.this, "主持人" + broadcastId + "---" + uid + meetingJoin.getHostUser().getHostUserName() + "进入了", Toast.LENGTH_SHORT).show();
						}
						isHostCommeIn = true;
						agoraAPI.channelJoin(channelName);
						agoraAPI.queryUserStatus(broadcastId);

						broadcastTipsText.setVisibility(View.GONE);

						remoteBroadcasterSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
						remoteBroadcasterSurfaceView.setZOrderOnTop(false);
						remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
						rtcEngine().setupRemoteVideo(new VideoCanvas(remoteBroadcasterSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));


						/*broadcasterSmallLayout.setVisibility(View.INVISIBLE);
						broadcasterSmallView.removeAllViews();
						docImage.setVisibility(View.GONE);*/


						if (isDocShow) {
							if (model == 2 || model == 3) {
								mVideoAdapter.setVisibility(View.GONE);
								mAudienceRecyclerView.setVisibility(View.GONE);

							} else if (model == 0 || model == 1) {
								//主持人加入到列表中
								if (!mVideoAdapter.isHaveChairMan()) {
									AudienceVideo audienceVideo = new AudienceVideo();
									audienceVideo.setUid(Integer.parseInt(meetingJoin.getHostUser().getClientUid()));
									audienceVideo.setName("主持人" + meetingJoin.getHostUser().getHostUserName());
									audienceVideo.setBroadcaster(true);
									audienceVideo.setSurfaceView(remoteBroadcasterSurfaceView);
									mVideoAdapter.getAudienceVideoLists().add(audienceVideo);
								} else {
									int chairManPosition = mVideoAdapter.getChairManPosition();
									if (chairManPosition != -1) {
										mVideoAdapter.getAudienceVideoLists().get(chairManPosition).setSurfaceView(remoteBroadcasterSurfaceView);
										mVideoAdapter.notifyDataSetChanged();
									}
								}
								mVideoAdapter.setVisibility(View.VISIBLE);
								mAudienceRecyclerView.setVisibility(View.VISIBLE);
							}
						} else {

							docImage.setVisibility(View.GONE);
							broadcasterLayout.setVisibility(View.VISIBLE);
							broadcasterLayout.removeAllViews();
							broadcasterLayout.addView(remoteBroadcasterSurfaceView);
							mAudienceRecyclerView.setVisibility(View.VISIBLE);
							mVideoAdapter.setVisibility(View.VISIBLE);
						}
					} else {
						if (BuildConfig.DEBUG) {
							Toast.makeText(MeetingAudienceActivity.this, "参会人" + uid + "正在连麦", Toast.LENGTH_SHORT).show();
						}
						mLogger.e("参会人" + uid + "正在连麦");

						/*audienceLayout.setVisibility(View.VISIBLE);
						audienceView.removeAllViews();*/
						remoteAudienceSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
						remoteAudienceSurfaceView.setZOrderOnTop(true);
						remoteAudienceSurfaceView.setZOrderMediaOverlay(true);
						rtcEngine().setupRemoteVideo(new VideoCanvas(remoteAudienceSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
//						audienceView.addView(remoteAudienceSurfaceView);

						AudienceVideo audienceVideo = new AudienceVideo();
						audienceVideo.setUid(uid);
						audienceVideo.setName("参会人" + uid);
						audienceVideo.setBroadcaster(false);
						audienceVideo.setSurfaceView(remoteAudienceSurfaceView);
						mVideoAdapter.insertItem(audienceVideo);


						agoraAPI.getUserAttr(String.valueOf(uid), "uname");
					}
				} else {
					if ("true".equals(agora.getIsTest())) {
						worker().joinChannel(null, channelName, config().mUid);
					} else {
						worker().joinChannel(agora.getToken(), channelName, config().mUid);
					}
				}
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.put("uid", config().mUid);
					jsonObject.put("uname", audienceName);
					jsonObject.put("handsUp", handsUp);
					if (Constant.videoType == 1) {
						jsonObject.put("callStatus", 2);
						jsonObject.put("isAudience", false);

					} else {
						if (currentAudienceId == config().mUid) {
							jsonObject.put("callStatus", 2);
							jsonObject.put("isAudience", true);
						} else {
							jsonObject.put("callStatus", 0);
							jsonObject.put("isAudience", true);
						}

					}
					jsonObject.put("auditStatus", Preferences.getUserAuditStatus());
					jsonObject.put("postTypeName", Preferences.getUserPostType());
				} catch (JSONException e) {
					e.printStackTrace();
				}

				agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");


				if (isSplitView && mVideoAdapter.getDataSize() > 1) {
					SpliteViews(3);
				}


				if (model == 2 || model == 3 || isFullScreen) {
					mVideoAdapter.setVisibility(View.GONE);
					mAudienceRecyclerView.setVisibility(View.GONE);
				} else {
					if (model == 0 || model == 1) {
						notFullScreenState();
					}
					mVideoAdapter.setVisibility(View.VISIBLE);
					mAudienceRecyclerView.setVisibility(View.VISIBLE);
				}


				if (isDocShow) {
					mVideoAdapter.notifyDataSetChanged();
				}

			}

			@Override
			public void onFailure(int errorCode, BaseException exception) {
				super.onFailure(errorCode, exception);
				Toast.makeText(MeetingAudienceActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
			}
		};
	}


	private OkHttpCallback meetingMaterialCallback = new OkHttpCallback<Bucket<Material>>() {

		@Override
		public void onSuccess(Bucket<Material> materialBucket) {
			Log.v("material", materialBucket.toString());
			currentMaterial = materialBucket.getData();
			Collections.sort(currentMaterial.getMeetingMaterialsPublishList(), (o1, o2) -> (o1.getPriority() < o2.getPriority()) ? -1 : 1);

			currentMaterialPublish = currentMaterial.getMeetingMaterialsPublishList().get(doc_index);

			if (remoteBroadcasterSurfaceView != null) {
				broadcasterLayout.removeView(remoteBroadcasterSurfaceView);
				broadcasterLayout.setVisibility(View.GONE);
			}
			pageText.setVisibility(View.VISIBLE);
			pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
			docImage.setVisibility(View.VISIBLE);
			Picasso.with(MeetingAudienceActivity.this).load(currentMaterialPublish.getUrl()).into(docImage);

		}

		@Override
		public void onFailure(int errorCode, BaseException exception) {
			super.onFailure(errorCode, exception);
			Toast.makeText(MeetingAudienceActivity.this, errorCode + "---" + exception.getMessage(), Toast.LENGTH_SHORT).show();
		}
	};

	private String meetingHostJoinTraceId;

	private OkHttpCallback meetingHostJoinTraceCallback = new OkHttpCallback<Bucket<MeetingHostingStats>>() {

		@Override
		public void onSuccess(Bucket<MeetingHostingStats> meetingHostingStatsBucket) {
			if (TextUtils.isEmpty(meetingHostJoinTraceId)) {
				meetingHostJoinTraceId = meetingHostingStatsBucket.getData().getId();
			} else {
				meetingHostJoinTraceId = null;
			}
		}
	};

	private Dialog dialog;

	private void showDialog(final int type, final String title, final String leftText, final String rightText, final Audience audience) {
		View view = View.inflate(this, R.layout.dialog_selector, null);
		TextView titleText = view.findViewById(R.id.title);
		titleText.setText(title);

		Button leftButton = view.findViewById(R.id.left);
		leftButton.setText(leftText);
		leftButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				dialog.cancel();
			}
		});

		Button rightButton = view.findViewById(R.id.right);
		rightButton.setText(rightText);
		rightButton.setOnClickListener(view1 -> {
			dialog.cancel();
			if (type == 1) {
				if (localSurfaceView != null && remoteAudienceSurfaceView == null) {

					localSurfaceView = null;

					if (agoraAPI.getStatus() == 2) {
						agoraAPI.setAttr("uname", null);
						agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
					}

					requestTalkButton.setVisibility(View.VISIBLE);
					stopTalkButton.setVisibility(View.GONE);

					handsUp = false;
					requestTalkButton.setText("申请发言");

					worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

					if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
						HashMap<String, Object> params = new HashMap<String, Object>();
						params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
						params.put("status", 2);
						params.put("meetingId", meetingJoin.getMeeting().getId());
						params.put("type", 2);
						params.put("leaveType", 1);
						ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
					}
					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("finish", true);
						agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (localSurfaceView == null && remoteAudienceSurfaceView != null) {

					remoteAudienceSurfaceView = null;


				}

				doLeaveChannel();
				if (agoraAPI.getStatus() == 2) {
					agoraAPI.logout();
				}
				finish();
			} else if (type == 2) {
				worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
				stopTalkButton.setVisibility(View.GONE);
				requestTalkButton.setVisibility(View.VISIBLE);
				localSurfaceView = null;


				agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);


				handsUp = false;
				requestTalkButton.setText("申请发言");

				if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
					HashMap<String, Object> params = new HashMap<String, Object>();
					params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
					params.put("status", 2);
					params.put("meetingId", meetingJoin.getMeeting().getId());
					params.put("type", 2);
					params.put("leaveType", 1);
					ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
				}
			}
		});

		dialog = new Dialog(this, R.style.MyDialog);
		dialog.setContentView(view);

		dialog.show();
	}

	@Override
	protected void deInitUIandEvent() {
		doLeaveChannel();
		event().removeEventHandler(this);

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (JoinSuc) {
			if (TextUtils.isEmpty(meetingJoinTraceId)) {
				doTEnterChannel();
			}
		}


	}

	private void doTEnterChannel() {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("status", 1);
		params.put("type", 2);
		params.put("meetingId", ((MeetingJoin) (getIntent().getParcelableExtra("meeting"))).getMeeting().getId());
		ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
	}

	private void doLeaveChannel() {
		worker().leaveChannel(config().mChannel);
		worker().preview(false, null, 0);

		if (!TextUtils.isEmpty(meetingJoinTraceId)) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("meetingJoinTraceId", meetingJoinTraceId);
			params.put("meetingId", meetingJoin.getMeeting().getId());
			params.put("status", 2);
			params.put("type", 2);
			params.put("leaveType", 1);
			ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
		}
	}

	@Override
	public void onJoinChannelSuccess(final String channel, final int uid, final int elapsed) {
		runOnUiThread(() -> {
			if (isFinishing()) {
				return;
			}
			config().mUid = uid;
			channelName = channel;

			if ("true".equals(agora.getIsTest())) {
				agoraAPI.login2(agora.getAppID(), "" + uid, "noneed_token", 0, "", 20, 30);
			} else {
				agoraAPI.login2(agora.getAppID(), "" + uid, agora.getSignalingKey(), 0, "", 20, 30);
			}

			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("status", 1);
			params.put("type", 2);
			params.put("meetingId", meetingJoin.getMeeting().getId());
			JoinSuc = true;
			ApiClient.getInstance().meetingJoinStats(TAG, meetingJoinStatsCallback, params);
		});
	}

	private String meetingJoinTraceId;

	private OkHttpCallback meetingJoinStatsCallback = new OkHttpCallback<Bucket<MeetingJoinStats>>() {

		@Override
		public void onSuccess(Bucket<MeetingJoinStats> meetingJoinStatsBucket) {
			if (TextUtils.isEmpty(meetingJoinTraceId)) {
				meetingJoinTraceId = meetingJoinStatsBucket.getData().getId();
			} else {
				meetingJoinTraceId = null;
			}
		}
	};

	@Override
	public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
		runOnUiThread(() -> {
			if (isFinishing()) {
				return;
			}
			ApiClient.getInstance().getMeetingHost(TAG, meeting.getId(), joinMeetingCallback(uid));
		});
	}

	@Override
	public void onUserOffline(int uid, int reason) {

		mLogger.e("onUserOffline   uid:" + uid + "   ----   " + "reason:" + reason);
		runOnUiThread(() -> {
			if (isFinishing()) {
				return;
			}
			if (String.valueOf(uid).equals(broadcastId)) {
				if (reason == Constants.USER_OFFLINE_DROPPED) {//因过长时间收不到对方数据包，超时掉线
					if (Constant.videoType == 2 && config().mUid == currentAudienceId) {//我是连麦观众
						if (mVideoAdapter.getPositionById(config().mUid) != 1) {
							AudienceVideo video = mVideoAdapter.getAudienceVideoLists().get(mVideoAdapter.getPositionById(config().mUid));
							if (video != null && video.getSurfaceView() != null) {
								video.getSurfaceView().setVisibility(View.GONE);
								video.getSurfaceView().setZOrderMediaOverlay(false);
							}
							mVideoAdapter.deleteItemById(config().mUid);
						}
						mSwtichCamera.setVisibility(View.GONE);
						requestTalkButton.setText("申请发言");
						stopTalkButton.setVisibility(View.GONE);
						mMuteAudio.setVisibility(View.GONE);
					}

				}
				mLogger.e("主持人退出了");
				if (mVideoAdapter.isHaveChairMan()) {
					int chairManPosition = mVideoAdapter.getChairManPosition();
					if (chairManPosition != -1) {
						mVideoAdapter.removeItem(chairManPosition);
						if (mCurrentAudienceVideo != null) {
							mVideoAdapter.insertItem(chairManPosition, mCurrentAudienceVideo);
						}
					}

				}
				isHostCommeIn = false;
				broadcasterLayout.removeAllViews();
				broadcastTipsText.setText("等待主持人进入...");
				broadcastTipsText.setVisibility(View.VISIBLE);
				count = 0;
				broadcastNameText.setText("");
				remoteBroadcasterSurfaceView = null;

				if (remoteAudienceSurfaceView != null) {
					remoteAudienceSurfaceView = null;
				}
				if (localSurfaceView != null) {
					if (Constant.videoType == 1) {
						worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
						int positionById = mVideoAdapter.getPositionById(config().mUid);
						AudienceVideo video = mVideoAdapter.getAudienceVideoLists().get(positionById);
						rtcEngine().setupLocalVideo(new VideoCanvas(video.getSurfaceView(), VideoCanvas.RENDER_MODE_HIDDEN, config().mUid));
						worker().preview(true, video.getSurfaceView(), config().mUid);

					} else {
						worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
						stopTalkButton.setVisibility(View.GONE);
						requestTalkButton.setVisibility(View.VISIBLE);
						localSurfaceView = null;
					}


					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("finish", true);
						agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
						HashMap<String, Object> params = new HashMap<String, Object>();
						params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
						params.put("status", 2);
						params.put("meetingId", meetingJoin.getMeeting().getId());
						params.put("type", 2);
						params.put("leaveType", 1);
						ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
					}
				}
			} else {
				if (BuildConfig.DEBUG) {
					Toast.makeText(MeetingAudienceActivity.this, "连麦观众" + uid + "退出了" + config().mUid, Toast.LENGTH_SHORT).show();
				}
				//如果连麦的观众 在大的视图 那就移除这个人  把主持人放回到大视图
				// TODO: 2019-12-16 有问题  mCurrentAudienceVideo在主持人回到大的视图里面后 就置成空
				if (mCurrentAudienceVideo != null && mCurrentAudienceVideo.getUid() == uid) {
					mLogger.e("连麦的观众 在大的视图   把主持人放回到大视图 ");
					broadcasterLayout.removeAllViews();
					if (mVideoAdapter.isHaveChairMan()) {
						int chairManPosition = mVideoAdapter.getChairManPosition();
						if (chairManPosition != -1) {
							AudienceVideo video = mVideoAdapter.getAudienceVideoLists().get(chairManPosition);
							if (video != null && video.getSurfaceView() != null) {
								video.getSurfaceView().setZOrderMediaOverlay(false);
								video.getSurfaceView().setZOrderOnTop(false);
								stripSurfaceView(video.getSurfaceView());
								broadcasterLayout.addView(video.getSurfaceView());
							}
							mVideoAdapter.removeItem(chairManPosition);
						}
					}
				} else {
					mLogger.e("集合数据为" + mVideoAdapter.getAudienceVideoLists().toString());
					mLogger.e("连麦观众在列表中");
					int postion = mVideoAdapter.getPositionById(uid);
					if (postion != -1) {
						AudienceVideo audienceVideo = mVideoAdapter.getAudienceVideoLists().get(postion);
						if (audienceVideo != null && audienceVideo.getSurfaceView() != null) {
							audienceVideo.getSurfaceView().setZOrderOnTop(false);
							audienceVideo.getSurfaceView().setZOrderMediaOverlay(false);
						}
					}

					mVideoAdapter.deleteItemById(uid);
//					mVideoAdapter.getAudienceVideoLists().remove(mVideoAdapter.getPositionById(uid));
				}
				remoteAudienceSurfaceView = null;
			}

			if (isSplitView) {
				if (mVideoAdapter.getDataSize() <= 1) {
					mLogger.e("当前集合大小是小于等于1");
					exitSpliteMode();
					isSplitView = false;
				} else {
					mLogger.e("当前集合大小是大于1的");
					changeViewLayout(4);
					isSplitView = true;
				}
			}

		});
	}

	@Override
	public void onConnectionLost() {
		runOnUiThread(() -> {
			Toast.makeText(MeetingAudienceActivity.this, "声网服务器网络连接断开，请检查网络连接", Toast.LENGTH_SHORT).show();
			finish();
		});
	}

	@Override
	public void onConnectionInterrupted() {
		runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "网络连接不佳，视频将会有卡顿，可尝试降低分辨率", Toast.LENGTH_SHORT).show());
	}

	@Override
	public void onUserMuteVideo(final int uid, final boolean muted) {
		if (BuildConfig.DEBUG) {
			runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, uid + " 的视频被暂停了 " + muted, Toast.LENGTH_SHORT).show());
		}
	}

	@Override
	public void onUserMuteAudio(int uid, boolean muted) {

	}

	@Override
	public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {

	}

	@Override
	public void onLastmileQuality(final int quality) {
		if (BuildConfig.DEBUG) {
			runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "本地网络质量报告：" + showNetQuality(quality), Toast.LENGTH_SHORT).show());
		}
	}

	@Override
	public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
		if (BuildConfig.DEBUG) {
			runOnUiThread(() -> {
//                    Toast.makeText(MeetingAudienceActivity.this, "用户" + uid + "的\n上行网络质量：" + showNetQuality(txQuality) + "\n下行网络质量：" + showNetQuality(rxQuality), Toast.LENGTH_SHORT).show();
			});
		}
	}

	@Override
	public void onWarning(int warn) {
//        runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "警告：" + warn, Toast.LENGTH_SHORT).show());
	}

	private String showNetQuality(int quality) {
		String lastmileQuality;
		switch (quality) {
			case 0:
				lastmileQuality = "UNKNOWN0";
				break;
			case 1:
				lastmileQuality = "EXCELLENT";
				break;
			case 2:
				lastmileQuality = "GOOD";
				break;
			case 3:
				lastmileQuality = "POOR";
				break;
			case 4:
				lastmileQuality = "BAD";
				break;
			case 5:
				lastmileQuality = "VBAD";
				break;
			case 6:
				lastmileQuality = "DOWN";
				break;
			default:
				lastmileQuality = "UNKNOWN";
		}
		return lastmileQuality;
	}

	@Override
	public void onError(final int err) {
		runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show());

//        if (BuildConfig.DEBUG) {
//            runOnUiThread(() -> Toast.makeText(MeetingAudienceActivity.this, "发生错误的错误码：" + err, Toast.LENGTH_SHORT).show());
//        }
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_HOME == keyCode) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;

	@Override
	public void onAttachedToWindow() {
		this.getWindow().addFlags(FLAG_HOMEKEY_DISPATCHED);
		super.onAttachedToWindow();
	}

	@Override
	public void onBackPressed() {
		if (dialog == null || !dialog.isShowing()) {
			showDialog(1, "确定退出会议吗？", "取消", "确定", null);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (ChatHandler.hasMessages(22)) {
			ChatHandler.removeMessages(22);
		}
		if (ChatHandler.hasMessages(23)) {
			ChatHandler.removeMessages(23);
		}

		unregisterReceiver(homeKeyEventReceiver);

		subscription.unsubscribe();
//        if (WSService.isOnline()) {
//            //当前状态在线,可切换离线
//            Log.i(TAG, "当前状态在线,可切换离线");
//            ZYAgent.onEvent(this, "离线按钮,当前在线,切换到离线");
//            RxBus.sendMessage(new SetUserChatEvent(false));
////                                            WSService.SOCKET_ONLINE =false;
////                                            setState(false);
//        } else {
//            ZYAgent.onEvent(this, "离线按钮,当前离线,无效操作");
//        }
		TCAgent.onPageEnd(this, "MeetingAudienceActivity");

		if (takePhotoTimer != null && takePhotoTimerTask != null) {
			takePhotoTimer.cancel();
			takePhotoTimerTask.cancel();
		}

//        BaseApplication.getInstance().deInitWorkerThread();
	}

	private void initFragment() {
		hideFragment = true;
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.show(fragment);
		fragmentTransaction.commitAllowingStateLoss();
	}

	private void hideFragment() {
		hideFragment = false;
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
		fragmentTransaction.hide(fragment);
		fragmentTransaction.commitAllowingStateLoss();
	}

	private BroadcastReceiver homeKeyEventReceiver = new BroadcastReceiver() {
		String REASON = "reason";
		String HOMEKEY = "homekey";
		String RECENTAPPS = "recentapps";

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Logger.e(action);
			if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(action) || Intent.ACTION_SHUTDOWN.equals(action)) {
				String reason = intent.getStringExtra(REASON);
				if (TextUtils.equals(reason, HOMEKEY)) {
					// 点击 Home键
					if (BuildConfig.DEBUG)
						Toast.makeText(getApplicationContext(), "您点击了Home键", Toast.LENGTH_SHORT).show();

					if (localSurfaceView != null && remoteAudienceSurfaceView == null) {

						localSurfaceView = null;

						if (agoraAPI.getStatus() == 2) {
							agoraAPI.setAttr("uname", null);
							agoraAPI.channelDelAttr(channelName, CALLING_AUDIENCE);
						}

						requestTalkButton.setVisibility(View.VISIBLE);
						stopTalkButton.setVisibility(View.GONE);

						handsUp = false;
						requestTalkButton.setText("申请发言");

						worker().getRtcEngine().setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

						if (!TextUtils.isEmpty(meetingHostJoinTraceId)) {
							HashMap<String, Object> params = new HashMap<String, Object>();
							params.put("meetingHostJoinTraceId", meetingHostJoinTraceId);
							params.put("status", 2);
							params.put("meetingId", meetingJoin.getMeeting().getId());
							params.put("type", 2);
							params.put("leaveType", 1);
							ApiClient.getInstance().meetingHostStats(TAG, meetingHostJoinTraceCallback, params);
						}
						try {
							JSONObject jsonObject = new JSONObject();
							jsonObject.put("finish", true);
							agoraAPI.messageInstantSend(broadcastId, 0, jsonObject.toString(), "");
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (localSurfaceView == null && remoteAudienceSurfaceView != null) {

						remoteAudienceSurfaceView = null;


					}
					doLeaveChannel();
					if (agoraAPI.getStatus() == 2) {
						agoraAPI.logout();
					}
					finish();
				} else if (TextUtils.equals(reason, RECENTAPPS)) {
					// 点击 菜单键
					if (BuildConfig.DEBUG)
						Toast.makeText(getApplicationContext(), "您点击了菜单键", Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	@SuppressLint("HandlerLeak")
	private Handler showOperatorHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case 0:
					findViewById(R.id.toolsbar).setVisibility(View.GONE);
					break;
				case 1:
					findViewById(R.id.toolsbar).setVisibility(View.VISIBLE);
					showOperatorHandler.sendEmptyMessageDelayed(0, Constant.delayTime);
					break;
			}
		}
	};

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (showOperatorHandler.hasMessages(0)) {
					showOperatorHandler.removeMessages(0);
				}
				if (findViewById(R.id.toolsbar).getVisibility() == View.VISIBLE) {
					showOperatorHandler.sendEmptyMessageDelayed(0, Constant.delayTime);
				} else if (findViewById(R.id.toolsbar).getVisibility() == View.GONE) {
					showOperatorHandler.sendEmptyMessage(1);
				}
				break;

		}
		return super.dispatchTouchEvent(ev);
	}

	private void SpliteViews(int x) {
		mLogger.e("SpliteViews:=" + x);
		//主持人在列表中 则将大的broadcasterView的视频加入到receclerview中去  将主持人移动到集合第一个去
		if (mVideoAdapter.isHaveChairMan()) {
			mLogger.e("主持人再列表中");
			mVideoAdapter.getAudienceVideoLists().get(mVideoAdapter.getChairManPosition()).setSurfaceView(remoteBroadcasterSurfaceView);
			mVideoAdapter.notifyDataSetChanged();
			if (mCurrentAudienceVideo != null) {
				broadcasterLayout.removeAllViews();
				stripSurfaceView(mCurrentAudienceVideo.getSurfaceView());
				mVideoAdapter.getAudienceVideoLists().add(mCurrentAudienceVideo);
				mVideoAdapter.notifyDataSetChanged();
				//将主持人移动到集合第一个
							/*int chairManPosition = audienceVideoAdapter.getChairManPosition();
							if (chairManPosition!=-1){
								audienceVideoAdapter.insertItem(0,audienceVideoAdapter.getAudienceVideoLists().get(chairManPosition));
								audienceVideoAdapter.removeItem(chairManPosition+1);
							}*/
				mCurrentAudienceVideo = null;
			}
		} else {
			mLogger.e("主持人不再列表中");
			/*if (remoteBroadcasterSurfaceView == null) {
				return;
			}
			//将主持人加入到recyclerView中去
			if (mVideoAdapter.getChairManPosition() != -1) {
				return;
			}*/
			stripSurfaceView(remoteBroadcasterSurfaceView);
			AudienceVideo audienceVideo = new AudienceVideo();
			audienceVideo.setUid(config().mUid);
			audienceVideo.setName("主持人" + meetingJoin.getHostUser().getHostUserName());
			audienceVideo.setBroadcaster(true);
			audienceVideo.setSurfaceView(remoteBroadcasterSurfaceView);
			mVideoAdapter.getAudienceVideoLists().add(audienceVideo);
			mVideoAdapter.notifyDataSetChanged();
			broadcasterLayout.removeAllViews();

		}


		//将recyclerview变成全屏布局
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mAudienceRecyclerView.getLayoutParams();
		mAudienceRecyclerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		layoutParams.setMargins(0, 0, 0, 0);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);


		findViewById(R.id.relative).setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) findViewById(R.id.relative).getLayoutParams();
		layoutParams1.setMargins(0, 0, 0, 0);
		findViewById(R.id.relative).setLayoutParams(layoutParams1);

		layoutParams.width = RelativeLayout.LayoutParams.MATCH_PARENT;
		layoutParams.height = RelativeLayout.LayoutParams.MATCH_PARENT;
		mAudienceRecyclerView.setLayoutParams(layoutParams);
		mSizeUtils.setViewMatchParent(mAudienceRecyclerView);
		changeViewLayout(x);
	}

	private void changeViewLayout(int x) {
		mLogger.e("changeViewLayout:=" + x);
		int dataSize = mVideoAdapter.getDataSize();
		mLogger.e("集合大小：%d", dataSize);
		mLogger.e("当前currentMaterial==null   " + (currentMaterial == null));

		if (dataSize == 1) {
			if (currentMaterial == null) {
				//如果不是分屏模式 需要判断主持人是否在列表中
				if (mVideoAdapter.isHaveChairMan()) {
					//主持人在列表中 删除主持人
					int chairManPosition = mVideoAdapter.getChairManPosition();
					if (chairManPosition != -1) {
						mVideoAdapter.removeItem(chairManPosition);
					}
				}
				//将主持人加入到大的view中
				broadcasterLayout.removeAllViews();
				broadcasterLayout.setVisibility(View.VISIBLE);
				if (remoteBroadcasterSurfaceView != null) {
					stripSurfaceView(remoteBroadcasterSurfaceView);
					remoteBroadcasterSurfaceView.setZOrderOnTop(false);
					remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
					broadcasterLayout.addView(remoteBroadcasterSurfaceView);
				}
			}
		} else if (dataSize == 2) {

			mDelegateAdapter.clear();
			if (currentMaterial == null) {

				GridLayoutHelper mGridLayoutHelper = new GridLayoutHelper(2);

				mGridLayoutHelper.setItemCount(8);
				mVideoAdapter.setLayoutHelper(mGridLayoutHelper);
				mVideoAdapter.setItemSize(DisplayUtil.getHeight(this), DisplayUtil.getWidth(this) / 2);
				mVideoAdapter.notifyDataSetChanged();

			} else {
				mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
				mVideoAdapter.notifyDataSetChanged();

				MyGridLayoutHelper mGridLayoutHelper = new MyGridLayoutHelper(2);
				mGridLayoutHelper.setItemCount(8);
				mGridLayoutHelper.setGap(10);
				mGridLayoutHelper.setAutoExpand(false);
				mVideoAdapter.setLayoutHelper(mGridLayoutHelper);
			}


			mVideoAdapter.notifyDataSetChanged();
			mDelegateAdapter.addAdapter(mVideoAdapter);
		} else if (dataSize == 3) {
			mDelegateAdapter.clear();
			if (currentMaterial == null) {
				mVideoAdapter.setItemSize(DisplayUtil.getHeight(this), DisplayUtil.getWidth(this));
			} else {
				mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
			}

			mVideoAdapter.notifyDataSetChanged();

			OnePlusNLayoutHelper helper = new OnePlusNLayoutHelper(3);
			helper.setItemCount(3);
			helper.setColWeights(new float[]{50f});
			helper.setRowWeight(50f);
			mVideoAdapter.setLayoutHelper(helper);

			mVideoAdapter.notifyDataSetChanged();
			mDelegateAdapter.addAdapter(mVideoAdapter);

		} else if (dataSize == 4) {
			mDelegateAdapter.clear();
			if (currentMaterial == null) {
				mVideoAdapter.setItemSize(DisplayUtil.getHeight(this) / 2, DisplayUtil.getWidth(this) / 2);
				mVideoAdapter.notifyDataSetChanged();

				GridLayoutHelper mGridLayoutHelper = new GridLayoutHelper(2);
				mGridLayoutHelper.setWeights(new float[]{50f});
				mGridLayoutHelper.setItemCount(4);
				mVideoAdapter.setLayoutHelper(mGridLayoutHelper);
			} else {
				mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
				mVideoAdapter.notifyDataSetChanged();

				MyGridLayoutHelper mGridLayoutHelper = new MyGridLayoutHelper(2);
				mGridLayoutHelper.setItemCount(4);
				mGridLayoutHelper.setGap(10);
				mGridLayoutHelper.setAutoExpand(false);
				mVideoAdapter.setLayoutHelper(mGridLayoutHelper);
			}


			mVideoAdapter.notifyDataSetChanged();
			mDelegateAdapter.addAdapter(mVideoAdapter);
		} else if (dataSize == 5) {
			mDelegateAdapter.clear();
			mVideoAdapter.notifyDataSetChanged();

			StaggeredGridLayoutHelper helper = new StaggeredGridLayoutHelper(2, 10);
			helper.setItemCount(5);
			mVideoAdapter.setLayoutHelper(helper);
			mVideoAdapter.notifyDataSetChanged();
			mDelegateAdapter.addAdapter(mVideoAdapter);
		} else if (dataSize == 6) {
			mDelegateAdapter.clear();
			if (currentMaterial == null) {
				mVideoAdapter.setItemSize(DisplayUtil.getHeight(this) / 3, DisplayUtil.getWidth(this) / 2);
				mVideoAdapter.notifyDataSetChanged();
				GridLayoutHelper helper = new GridLayoutHelper(2);
				helper.setWeights(new float[]{50f});
				helper.setItemCount(6);
				mVideoAdapter.setLayoutHelper(helper);

			} else {
				mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
				mVideoAdapter.notifyDataSetChanged();
				MyGridLayoutHelper mGridLayoutHelper = new MyGridLayoutHelper(2);
				mGridLayoutHelper.setItemCount(6);
				mGridLayoutHelper.setGap(10);
				mGridLayoutHelper.setAutoExpand(false);
				mVideoAdapter.setLayoutHelper(mGridLayoutHelper);
			}

			mVideoAdapter.notifyDataSetChanged();
			mDelegateAdapter.addAdapter(mVideoAdapter);
		} else if (dataSize == 7) {
			mDelegateAdapter.clear();
			if (currentMaterial == null) {
				mVideoAdapter.setItemSize(DisplayUtil.getHeight(this) / 2, DisplayUtil.getWidth(this) / 4);
				mVideoAdapter.notifyDataSetChanged();

				GridLayoutHelper helper = new GridLayoutHelper(4);
				helper.setSpanSizeLookup(new GridLayoutHelper.SpanSizeLookup() {
					@Override
					public int getSpanSize(int position) {
						if (position == 4) {
							return 2;
						}
						return 1;
					}
				});
				helper.setWeights(new float[]{100f / 4});
				helper.setItemCount(7);
				mVideoAdapter.setLayoutHelper(helper);

			} else {
				mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
				mVideoAdapter.notifyDataSetChanged();
				MyGridLayoutHelper mGridLayoutHelper = new MyGridLayoutHelper(2);
				mGridLayoutHelper.setItemCount(6);
				mGridLayoutHelper.setGap(10);
				mGridLayoutHelper.setAutoExpand(false);
				mVideoAdapter.setLayoutHelper(mGridLayoutHelper);
			}


			mVideoAdapter.notifyDataSetChanged();
			mDelegateAdapter.addAdapter(mVideoAdapter);
		} else if (dataSize >= 8) {
			mDelegateAdapter.clear();
			if (currentMaterial == null) {
				mVideoAdapter.setItemSize(DisplayUtil.getHeight(this) / 2, DisplayUtil.getWidth(this) / 4);
				mVideoAdapter.notifyDataSetChanged();
				GridLayoutHelper helper = new GridLayoutHelper(4);
				helper.setWeights(new float[]{100f / 4});
				helper.setItemCount(8);
				mVideoAdapter.setLayoutHelper(helper);
			} else {
				mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
				mVideoAdapter.notifyDataSetChanged();
				MyGridLayoutHelper mGridLayoutHelper = new MyGridLayoutHelper(2);
				mGridLayoutHelper.setItemCount(6);
				mGridLayoutHelper.setGap(10);
				mGridLayoutHelper.setAutoExpand(false);
				mVideoAdapter.setLayoutHelper(mGridLayoutHelper);
			}

			mDelegateAdapter.addAdapter(mVideoAdapter);
		}
	}


	private void exitSpliteMode() {

		//将主持人拿出来
		int chairManPosition = mVideoAdapter.getChairManPosition();
		//如果主持人 在的话
		if (chairManPosition != -1) {
			//将主持人添加到大的画面上
			if (remoteBroadcasterSurfaceView != null) {
				stripSurfaceView(remoteBroadcasterSurfaceView);
				broadcasterLayout.removeAllViews();
				remoteBroadcasterSurfaceView.setZOrderMediaOverlay(false);
				remoteBroadcasterSurfaceView.setZOrderOnTop(false);
				broadcasterLayout.setVisibility(View.VISIBLE);

				remoteBroadcasterSurfaceView.setVisibility(View.VISIBLE);
				mVideoAdapter.getAudienceVideoLists().remove(chairManPosition);
				mVideoAdapter.notifyDataSetChanged();
				broadcasterLayout.addView(remoteBroadcasterSurfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			} else {
				mLogger.e("remoteBroadcasterSurfaceView == null");
			}

		} else {
			// TODO: 2019-11-26 主持人不在
		}
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(DisplayUtil.dip2px(this, 240), RelativeLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		layoutParams.setMargins(0, DisplayUtil.dip2px(this, 40), DisplayUtil.dip2px(this, 10), DisplayUtil.dip2px(this, 60));
		mAudienceRecyclerView.setLayoutParams(layoutParams);

		RelativeLayout relative = findViewById(R.id.relative);

		FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) relative.getLayoutParams();
		layoutParams1.setMargins(0, 0, 0, 0);
		relative.setLayoutParams(layoutParams1);

		mDelegateAdapter.clear();
		MyGridLayoutHelper helper = new MyGridLayoutHelper(2);
		helper.setAutoExpand(false);
		helper.setGap(10);
		helper.setItemCount(8);
		mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
		mVideoAdapter.setLayoutHelper(helper);
		mVideoAdapter.notifyDataSetChanged();
		mDelegateAdapter.addAdapter(mVideoAdapter);


		if (currentMaterial != null) {
			broadcasterLayout.setVisibility(View.GONE);
		} else {
			broadcasterLayout.setVisibility(View.VISIBLE);
		}


	}

	/**
	 * 非全屏状态，画面背景为ppt，主持人 参会人悬浮在ppt内容上
	 * <p>
	 * **需要判断集合大小，先将主持人加入到集合中 再判断  如果大于8人  就不显示自己  如果小于8人，所以的都显示
	 */
	private void notFullScreenState() {
		model = 1;
		//主持人加入到列表中
		if (!mVideoAdapter.isHaveChairMan()) {
			AudienceVideo audienceVideo = new AudienceVideo();
			audienceVideo.setUid(Integer.parseInt(meetingJoin.getHostUser().getClientUid()));
			audienceVideo.setName("主持人" + meetingJoin.getHostUser().getHostUserName());
			audienceVideo.setBroadcaster(true);
			audienceVideo.setSurfaceView(remoteBroadcasterSurfaceView);
			mVideoAdapter.getAudienceVideoLists().add(audienceVideo);
			mVideoAdapter.notifyDataSetChanged();
		}else {
			mVideoAdapter.getAudienceVideoLists().get(mVideoAdapter.getChairManPosition()).setSurfaceView(remoteBroadcasterSurfaceView);
			mVideoAdapter.notifyDataSetChanged();
		}

		localAudienceFrameView.removeAllViews();
		localAudienceFrameView.setVisibility(View.GONE);

		//如果自己不再列表中 将自己也加入到列表中

		if (mVideoAdapter.getPositionById(config().mUid) == -1) {
			//判断当前连麦人是否是自己或者是否是参会人
			if (Constant.videoType == 1 || currentAudienceId == config().mUid) {
				if (localSurfaceView == null) {
					return;
				}
				localSurfaceView.setVisibility(View.VISIBLE);
				AudienceVideo audienceVideo = new AudienceVideo();
				audienceVideo.setUid(config().mUid);
				audienceVideo.setName("参会人" + config().mUid);
				audienceVideo.setBroadcaster(false);
				audienceVideo.setSurfaceView(localSurfaceView);
				mVideoAdapter.insertItem(audienceVideo);
			}
		}


		if (mVideoAdapter.getDataSize() > 8) {
			int chairManPosition = mVideoAdapter.getChairManPosition();
			if (mVideoAdapter.getChairManPosition() != -1) {
				mVideoAdapter.getAudienceVideoLists().get(chairManPosition).getSurfaceView().setVisibility(View.GONE);
				mVideoAdapter.removeItem(mVideoAdapter.getChairManPosition());
			}
		}

		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(DisplayUtil.dip2px(this, 240), RelativeLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		layoutParams.setMargins(0, DisplayUtil.dip2px(this, 40), DisplayUtil.dip2px(this, 10), DisplayUtil.dip2px(this, 60));
		mAudienceRecyclerView.setLayoutParams(layoutParams);

		RelativeLayout relative = findViewById(R.id.relative);

		FrameLayout.LayoutParams layoutParams1 = (FrameLayout.LayoutParams) relative.getLayoutParams();
		layoutParams1.setMargins(0, 0, 0, 0);
		relative.setLayoutParams(layoutParams1);

		mDelegateAdapter.clear();

		mVideoAdapter.setItemSize(DisplayUtil.dip2px(this, 70), DisplayUtil.dip2px(this, 114));
		mVideoAdapter.notifyDataSetChanged();

		MyGridLayoutHelper helper = new MyGridLayoutHelper(2);
		helper.setAutoExpand(false);
		helper.setVGap(10);
		helper.setHGap(10);
		helper.setItemCount(8);

		mVideoAdapter.setLayoutHelper(helper);

		mDelegateAdapter.addAdapter(mVideoAdapter);
		mVideoAdapter.notifyDataSetChanged();

		mVideoAdapter.setVisibility(View.VISIBLE);
		mAudienceRecyclerView.setVisibility(View.VISIBLE);

		broadcasterLayout.setVisibility(View.GONE);

	}

	/**
	 * 全屏状态：画面背景为PPT内容，右下角悬浮自己的画面 悬浮画面可以拖动
	 */
	private void FullScreenState() {
		model = 2;
		//将自己的画面从列表中移除 然后悬浮
		mAudienceRecyclerView.setVisibility(View.GONE);
		mVideoAdapter.setVisibility(View.GONE);

		if (currentAudienceId != config().mUid && Constant.videoType != 1) {
			return;
		}


		int currentAudiencePosition = mVideoAdapter.getPositionById(config().mUid);
		mLogger.e(currentAudiencePosition + "-----" + config().mUid);
		if (currentAudiencePosition != -1) {
			mVideoAdapter.deleteItem(config().mUid);

		}

		if (localSurfaceView == null) {
			return;
		}
		localAudienceFrameView.removeAllViews();
		localAudienceFrameView.setVisibility(View.VISIBLE);
		localSurfaceView.setVisibility(View.VISIBLE);
		localSurfaceView.setZOrderOnTop(true);
		localSurfaceView.setZOrderMediaOverlay(true);
		stripSurfaceView(localSurfaceView);

		localAudienceFrameView.addView(localSurfaceView);
	}

	/**
	 * 隐藏浮窗状态：画面只有PPT内容；
	 */
	private void clearAllState() {
		model = 3;
		mVideoAdapter.setVisibility(View.GONE);
		mAudienceRecyclerView.setVisibility(View.GONE);

		localAudienceFrameView.setVisibility(View.GONE);
		if (localSurfaceView != null) {
			localSurfaceView.setVisibility(View.GONE);
		}
	}


}
