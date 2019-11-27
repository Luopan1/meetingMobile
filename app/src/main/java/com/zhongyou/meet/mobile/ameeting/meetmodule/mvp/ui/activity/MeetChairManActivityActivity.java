package com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.FloatLayoutHelper;
import com.alibaba.android.vlayout.layout.OnePlusNLayoutHelper;
import com.alibaba.android.vlayout.layout.SingleLayoutHelper;
import com.alibaba.fastjson.JSON;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.Logger;
import com.elvishew.xlog.XLog;
import com.jess.arms.base.BaseActivity;
import com.jess.arms.di.component.AppComponent;
import com.jess.arms.http.imageloader.glide.ImageConfigImpl;
import com.jess.arms.utils.ArmsUtils;
import com.squareup.picasso.Picasso;
import com.zhongyou.meet.mobile.BuildConfig;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.ameeting.adater.BaseRecyclerHolder;
import com.zhongyou.meet.mobile.ameeting.adater.BaseRecyclerVLayoutAdapter;
import com.zhongyou.meet.mobile.ameeting.adater.BaseRecyclerVLayoutHolder;
import com.zhongyou.meet.mobile.ameeting.adater.BaseRecyclerViewAdapter;
import com.zhongyou.meet.mobile.ameeting.meetmodule.di.component.DaggerMeetChairManActivityComponent;
import com.zhongyou.meet.mobile.ameeting.meetmodule.di.module.MeetChairManActivityModule;
import com.zhongyou.meet.mobile.ameeting.meetmodule.di.module.NetworkModule;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.contract.MeetChairManActivityContract;
import com.zhongyou.meet.mobile.ameeting.meetmodule.mvp.presenter.MeetChairManActivityPresenter;
import com.zhongyou.meet.mobile.ameeting.network.LoadingDialog;
import com.zhongyou.meet.mobile.business.adapter.BaseRecyclerAdapter;
import com.zhongyou.meet.mobile.entities.Agora;
import com.zhongyou.meet.mobile.entities.AudienceVideo;
import com.zhongyou.meet.mobile.entities.Bucket;
import com.zhongyou.meet.mobile.entities.HostUser;
import com.zhongyou.meet.mobile.entities.Material;
import com.zhongyou.meet.mobile.entities.Materials;
import com.zhongyou.meet.mobile.entities.MeetingJoin;
import com.zhongyou.meet.mobile.entities.MeetingMaterialsPublish;
import com.zhongyou.meet.mobile.persistence.Preferences;
import com.zhongyou.meet.mobile.utils.DisplayUtil;
import com.zhongyou.meet.mobile.utils.SizeUtils;
import com.zhongyou.meet.mobile.utils.UIDUtil;
import com.zhongyou.meet.mobile.utils.helper.ImageHelper;
import com.zhongyou.meet.mobile.view.FocusFixedLinearLayoutManager;
import com.zhongyou.meet.mobile.view.SpaceItemDecoration;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.AgoraAPIOnlySignal;
import io.agora.NativeAgoraAPI;
import io.agora.openlive.model.AGEventHandler;
import io.agora.openlive.ui.MaterialAdapter;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import me.jessyan.autosize.utils.ScreenUtils;

import static com.jess.arms.utils.Preconditions.checkNotNull;


public class MeetChairManActivityActivity extends BaseActivity<MeetChairManActivityPresenter> implements MeetChairManActivityContract.View, AGEventHandler {

	@BindView(R.id.recyclerView)
	RecyclerView recyclerView;
	@BindView(R.id.splitViewButton)
	Button splitViewButton;
	@BindView(R.id.toolsLayout)
	LinearLayout toolsLayout;
	@BindView(R.id.chairmanVideoLayout)
	FrameLayout chairmanVideoLayout;
	@BindView(R.id.PPTImageView)
	ImageView PPTImageView;
	@BindView(R.id.waiter)
	Button waiter;
	@BindView(R.id.stop_audience)
	Button stopAudience;
	@BindView(R.id.discuss)
	Button discuss;
	@BindView(R.id.doc)
	Button doc;
	@BindView(R.id.privioesPage)
	Button privioesPage;
	@BindView(R.id.nextPage)
	Button nextPage;
	@BindView(R.id.exitPPT)
	Button exitPPT;
	@BindView(R.id.pptTools)
	LinearLayout pptTools;
	@BindView(R.id.videoViewRecyclerView)
	RecyclerView mVideoViewRecyclerView;
	private MeetingJoin meetingJoin;
	private Agora agora;
	private String channelName;
	private SurfaceView mChairmanSurfaceView;
	private AgoraAPIOnlySignal agoraAPI;
	private Logger mLogger;
	private BaseRecyclerViewAdapter<AudienceVideo> mAudienceAdater;
	List<AudienceVideo> mAttendeeLists = new ArrayList<>();
	private AlertDialog pptAlertDialog;
	private AlertDialog mPPtPreviewDialog;
	private Material mCurrentMaterial;

	private static final String DOC_INFO = "doc_info";
	private static final String CALLING_AUDIENCE = "calling_audience";
	private List<DelegateAdapter.Adapter> mAdapters;
	private DelegateAdapter delegateAdapter;
	private SizeUtils mSizeUtils;

	@Override
	public void setupActivityComponent(@NonNull AppComponent appComponent) {
		DaggerMeetChairManActivityComponent //如找不到该类,请编译一下项目
				.builder()
				.appComponent(appComponent)
				.networkModule(new NetworkModule())
				.meetChairManActivityModule(new MeetChairManActivityModule(this))
				.build()
				.inject(this);
	}

	@Override
	public int initView(@Nullable Bundle savedInstanceState) {
		return R.layout.activity_meet_chair_man; //如果你不需要框架帮你设置 setContentView(id) 需要自行设置,请返回 0
	}

	@Override
	public void initData(@Nullable Bundle savedInstanceState) {

		mLogger = XLog.tag(TAG)
				.logLevel(BuildConfig.DEBUG ? LogLevel.ALL : LogLevel.NONE)
				.t()
				.st(2)
				.b()
				.build();

		mSizeUtils = new SizeUtils(this);

		if (mPresenter == null) {
			mLogger.e("当前的mPresenter为null 请检查代码设置 ");
			return;
		}
		mPresenter.event().addEventHandler(this);
		mPresenter.config().mUid = Integer.parseInt(UIDUtil.generatorUID(Preferences.getUserId()));
		mPresenter.doConfigEngine(Constants.CLIENT_ROLE_BROADCASTER);

		Intent intent = getIntent();
		agora = intent.getParcelableExtra("agora");
		meetingJoin = intent.getParcelableExtra("meeting");
		channelName = meetingJoin.getMeeting().getId();

		mLogger.e(JSON.toJSONString(meetingJoin) + "\n" + "channelName:" + channelName);


		mAdapters = new LinkedList<>();
		VirtualLayoutManager layoutManager = new VirtualLayoutManager(this);
		mVideoViewRecyclerView.setLayoutManager(layoutManager);
		RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
		mVideoViewRecyclerView.setRecycledViewPool(viewPool);
		viewPool.setMaxRecycledViews(0, 10);//只针对type=0的item设置了复用池的大小，如果有多种type，需要为每一种类型的分别调整复用池大小参数。
		delegateAdapter = new DelegateAdapter(layoutManager, false);
		mVideoViewRecyclerView.setAdapter(delegateAdapter);


		mChairmanSurfaceView = RtcEngine.CreateRendererView(MeetChairManActivityActivity.this);

//		chairmanVideoLayout.addView(mChairmanSurfaceView);


		List<SurfaceView> chairmanLists=new ArrayList<>();
		chairmanLists.add(mChairmanSurfaceView);
		SingleLayoutHelper singleLayoutHelper=new SingleLayoutHelper();
		singleLayoutHelper.setItemCount(1);
		BaseRecyclerVLayoutAdapter<SurfaceView>  chairmanAdapter=
		new BaseRecyclerVLayoutAdapter<SurfaceView>(this, chairmanLists, R.layout.item_chairman, singleLayoutHelper) {
			@Override
			public void convert(BaseRecyclerVLayoutHolder holder, SurfaceView item, int position, boolean isScrolling) {

				mPresenter.rtcEngine().setupLocalVideo(new VideoCanvas(item, VideoCanvas.RENDER_MODE_HIDDEN, mPresenter.config().mUid));
				item.setZOrderOnTop(false);
				item.setZOrderMediaOverlay(false);
				mPresenter.worker().preview(true, item, mPresenter.config().mUid);
				mPresenter.stripSurfaceView(item);
				FrameLayout view = holder.getView(R.id.chairmanVideoFrame);
				view.addView(item);
				mSizeUtils.setViewMatchParent(view);

			}
		};

		mAdapters.add(chairmanAdapter);
		delegateAdapter.addAdapters(mAdapters);



		if ("true".equals(agora.getIsTest())) {
			mPresenter.worker().joinChannel(null, channelName, mPresenter.config().mUid);
		} else {
			mPresenter.worker().joinChannel(agora.getToken(), channelName, mPresenter.config().mUid);
		}

		agoraAPI = AgoraAPIOnlySignal.getInstance(this, agora.getAppID());

		if (agoraAPI != null) {
			setAgoraCallBack(agoraAPI);
		}
	}




	@Override
	public void showLoading() {
		LoadingDialog.createLoadingDialog(this, "加载中……");
	}

	@Override
	public void hideLoading() {
		LoadingDialog.closeDialog();
	}

	@Override
	public void showMessage(@NonNull String message) {
		checkNotNull(message);
		mLogger.e(message);
	}

	@Override
	public void launchActivity(@NonNull Intent intent) {
		checkNotNull(intent);
		ArmsUtils.startActivity(intent);
	}

	@Override
	public void killMyself() {
		finish();
	}

	@Override
	public void chengeView(int count) {
		mLogger.e("当前人数为:" + count);
	}

	/**
	 * 用户加入频道的回调
	 */
	@Override
	public void joinMeetingCallBack(Bucket<HostUser> entity,int uid) {
		mLogger.e("uid===:"+uid);
		mLogger.json(JSON.toJSONString(entity));
		HostUser data = entity.getData();

		SurfaceView localAudienceSurfaceView = RtcEngine.CreateRendererView(getApplicationContext());
		localAudienceSurfaceView.setZOrderOnTop(true);
		localAudienceSurfaceView.setZOrderMediaOverlay(true);
		mPresenter.rtcEngine().setupRemoteVideo(new VideoCanvas(localAudienceSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN,uid));
		mPresenter.worker().preview(true, localAudienceSurfaceView, uid);


		AudienceVideo audienceVideo = new AudienceVideo();
		audienceVideo.setUid(uid);
		audienceVideo.setName("参会人" +data.getHostUserName() );
		audienceVideo.setBroadcaster(false);
		audienceVideo.setSurfaceView(localAudienceSurfaceView);

		mAttendeeLists.add(audienceVideo);

		FloatLayoutHelper floatLayoutHelper=new FloatLayoutHelper();
		floatLayoutHelper.setItemCount(mAttendeeLists.size());
//		floatLayoutHelper.setDefaultLocation(DisplayUtil.getWidth(this),0);
		floatLayoutHelper.setDragEnable(true);

		/*BaseRecyclerVLayoutAdapter<AudienceVideo> attdenteeAdapter=new BaseRecyclerVLayoutAdapter<AudienceVideo>(this,mAttendeeLists,R.layout.item_attendee,floatLayoutHelper) {
			@Override
			public void convert(BaseRecyclerVLayoutHolder holder, AudienceVideo item, int position, boolean isScrolling) {
				mLogger.e(item.getSurfaceView());
				if (item.getSurfaceView() == null) {
					holder.itemView.setVisibility(View.INVISIBLE);
				} else {
					mPresenter.stripSurfaceView(item.getSurfaceView());
					if (item.getUid() == 0) {
						holder.getView(R.id.chairManLabel).setVisibility(View.VISIBLE);
						holder.getView(R.id.name).setVisibility(View.GONE);
					} else {
						holder.getView(R.id.chairManLabel).setVisibility(View.GONE);
						holder.getView(R.id.name).setVisibility(View.VISIBLE);
						holder.setText(R.id.name, item.getName());
					}
					FrameLayout videoView = (FrameLayout) holder.getView(R.id.attendeeVideoLayout);

					TextView textView=new TextView(MeetChairManActivityActivity.this);
					textView.setTextColor(getResources().getColor(R.color.red));
					textView.setTextSize(30);
					textView.setText("FrameLayout");
					videoView.addView(textView);


					ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
					layoutParams.width=ViewGroup.LayoutParams.MATCH_PARENT;
					layoutParams.height=ViewGroup.LayoutParams.MATCH_PARENT;
					item.getSurfaceView().setLayoutParams(layoutParams);

					videoView.addView(item.getSurfaceView());

					mLogger.e(item.getSurfaceView().getLayoutParams().width);
					mLogger.e(item.getSurfaceView().getLayoutParams().height);
				}
			}
		};

		if ("true".equals(agora.getIsTest())) {
			mPresenter.worker().joinChannel(null, channelName, uid);
		} else {
			mPresenter.worker().joinChannel(agora.getToken(), channelName, uid);
		}

		mAdapters.add(attdenteeAdapter);*/
		delegateAdapter.addAdapters(mAdapters);
	}


	/**
	 * 从服务器获取到的数据
	 */
	@Override
	public void getDataSuccessFormOrigin(Bucket t, String type) {
		switch (type) {
			//ppt的资料获取
			case "pptPreview":
				hideLoading();
				showPPTListDialog(((Materials) t.getData()).getPageData());
				break;
			//点击使用ppt 开始展示ppt资料
			case "showPPT":
				hideLoading();
				pptTools.setVisibility(View.VISIBLE);
				toolsLayout.setVisibility(View.GONE);
				startShowPPT(t);
				break;
		}
	}

	private int mPPTCurrentPosition = 0;

	private void startShowPPT(Bucket t) {
		if (mPPtPreviewDialog.isShowing()) {
			mPPtPreviewDialog.dismiss();
		}
		if (pptAlertDialog.isShowing()) {
			pptAlertDialog.dismiss();
		}

		/*
		 * 参会人 画面全部消失
		 * */
		recyclerView.setVisibility(View.GONE);
		/*
		 * 主持人的画面移动到小的画面
		 * */


		ViewGroup.LayoutParams layoutParams = chairmanVideoLayout.getLayoutParams();
//		mLogger.e("height:"+DisplayUtil.getHeight(this)+"\n  width:"+DisplayUtil.getWidth(this));
		layoutParams.height = DisplayUtil.getHeight(this) / 4;
		layoutParams.width = DisplayUtil.getWidth(this) / 4;
		chairmanVideoLayout.setLayoutParams(layoutParams);
		translation(chairmanVideoLayout, DisplayUtil.getWidth(this) - DisplayUtil.getWidth(this) / 4, DisplayUtil.getHeight(this) - DisplayUtil.getHeight(this) / 4);

		/*
		 * 处理ppt
		 * */
		mPPTCurrentPosition = 0;
		mLogger.e(JSON.toJSONString(mCurrentMaterial));
		MeetingMaterialsPublish currentMaterialPublish = mCurrentMaterial.getMeetingMaterialsPublishList().get(mPPTCurrentPosition);
		/*
		*  pageText.setVisibility(View.VISIBLE);
            pageText.setText("第" + currentMaterialPublish.getPriority() + "/" + currentMaterial.getMeetingMaterialsPublishList().size() + "页");
		* */
		ArmsUtils.obtainAppComponentFromContext(this)
				.imageLoader()
				.loadImage(this,
						ImageConfigImpl.builder().url(ImageHelper.getThumb(currentMaterialPublish.getUrl())).imageView(PPTImageView).build());
		PPTImageView.setVisibility(View.VISIBLE);
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("material_id", mCurrentMaterial.getId());
			jsonObject.put("doc_index", mPPTCurrentPosition);
			agoraAPI.channelSetAttr(channelName, DOC_INFO, jsonObject.toString());
//                agoraAPI.messageChannelSend(channelName, jsonObject.toString(), "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	public boolean useEventBus() {
		return false;
	}


	private void setAgoraCallBack(AgoraAPIOnlySignal arora) {
		arora.callbackSet(new NativeAgoraAPI.CallBack() {

			@Override
			public void onReconnecting(int nretry) {
				super.onReconnecting(nretry);
				mLogger.i("onReconnecting：" + nretry);
			}

			@Override
			public void onReconnected(int fd) {
				super.onReconnected(fd);
				mLogger.i("onReconnected：" + fd);
			}

			@Override
			public void onLoginSuccess(int uid, int fd) {
				super.onLoginSuccess(uid, fd);
				mLogger.i("onLoginSuccess：uid" + uid + "   fd:" + fd);
				arora.channelJoin(channelName);
			}

			@Override
			public void onLogout(int ecode) {
				super.onLogout(ecode);
				mLogger.i("onLogout：" + ecode);
			}

			@Override
			public void onLoginFailed(int ecode) {
				super.onLoginFailed(ecode);
				mLogger.i("onLoginFailed：" + ecode);
			}

			@Override
			public void onChannelJoined(String channelID) {
				super.onChannelJoined(channelID);
				mLogger.i("onChannelJoined：" + channelID);
			}

			@Override
			public void onChannelJoinFailed(String channelID, int ecode) {
				super.onChannelJoinFailed(channelID, ecode);
				mLogger.i("onChannelJoinFailed：channelID  " + channelID + "   ecode:" + ecode);
			}

			@Override
			public void onChannelLeaved(String channelID, int ecode) {
				super.onChannelLeaved(channelID, ecode);
				mLogger.i("onChannelLeaved：channelID  " + channelID + "   ecode:" + ecode);
			}

			@Override
			public void onChannelUserJoined(String account, int uid) {
				super.onChannelUserJoined(account, uid);
				mLogger.i("用户加入频道onChannelUserJoined：account  " + account + "   uid:" + uid);

				//加入频道 查询用户数据
				mPresenter.joinMeetingCallBack(meetingJoin.getMeeting().getId(),uid);


			}

			@Override
			public void onChannelUserLeaved(String account, int uid) {
				super.onChannelUserLeaved(account, uid);

				mLogger.i("onChannelUserLeaved：account  " + account + "   uid:" + uid);

			}

			@Override
			public void onChannelUserList(String[] accounts, int[] uids) {
				super.onChannelUserList(accounts, uids);
				mLogger.i("onChannelUserList：accounts[]  " + JSON.toJSONString(accounts) + "   uids[]:" + JSON.toJSONString(uids));
			}

			@Override
			public void onChannelQueryUserNumResult(String channelID, int ecode, int num) {
				super.onChannelQueryUserNumResult(channelID, ecode, num);
				mLogger.i("onChannelQueryUserNumResult：channelID  " + channelID + "   ecode:" + ecode + "  num:" + num);
			}

			@Override
			public void onChannelQueryUserIsIn(String channelID, String account, int isIn) {
				super.onChannelQueryUserIsIn(channelID, account, isIn);
				mLogger.i("onChannelQueryUserIsIn：channelID  " + channelID + "   account:" + account + "  isIn:" + isIn);
			}

			@Override
			public void onChannelAttrUpdated(String channelID, String name, String value, String type) {
				super.onChannelAttrUpdated(channelID, name, value, type);
				mLogger.i("onChannelAttrUpdated：channelID  " + channelID + "   name:" + name + "  value:" + value + " type:" + type);


			}

			@Override
			public void onInviteReceived(String channelID, String account, int uid, String extra) {
				super.onInviteReceived(channelID, account, uid, extra);
				mLogger.i("onInviteReceived：channelID  " + channelID + "   account:" + account + "  uid:" + uid + "  extra:" + extra);
			}

			@Override
			public void onInviteReceivedByPeer(String channelID, String account, int uid) {
				super.onInviteReceivedByPeer(channelID, account, uid);
				mLogger.i("onInviteReceivedByPeer：channelID  " + channelID + "   account:" + account + "  uid:" + uid);
			}

			@Override
			public void onInviteAcceptedByPeer(String channelID, String account, int uid, String extra) {
				super.onInviteAcceptedByPeer(channelID, account, uid, extra);
				mLogger.i("onInviteAcceptedByPeer：channelID  " + channelID + "   account:" + account + "  uid:" + uid + "  extra:" + extra);
			}

			@Override
			public void onInviteRefusedByPeer(String channelID, String account, int uid, String extra) {
				super.onInviteRefusedByPeer(channelID, account, uid, extra);
				mLogger.i("onInviteRefusedByPeer：channelID  " + channelID + "   account:" + account + "  uid:" + uid + "  extra:" + extra);
			}

			@Override
			public void onInviteFailed(String channelID, String account, int uid, int ecode, String extra) {
				super.onInviteFailed(channelID, account, uid, ecode, extra);
				mLogger.i("onInviteFailed：channelID  " + channelID + "   account:" + account + "  uid:" + uid + "  ecode:" + ecode + "  extra:" + extra);
			}

			@Override
			public void onInviteEndByPeer(String channelID, String account, int uid, String extra) {
				super.onInviteEndByPeer(channelID, account, uid, extra);
				mLogger.i("onInviteEndByPeer：channelID  " + channelID + "   account:" + account + "  uid:" + uid + "  extra:" + extra);

			}

			@Override
			public void onInviteEndByMyself(String channelID, String account, int uid) {
				super.onInviteEndByMyself(channelID, account, uid);
				mLogger.i("onInviteEndByMyself：channelID  " + channelID + "   account:" + account + "  uid:" + uid);
			}

			@Override
			public void onInviteMsg(String channelID, String account, int uid, String msgType, String msgData, String extra) {
				super.onInviteMsg(channelID, account, uid, msgType, msgData, extra);
				mLogger.i("onInviteMsg：channelID  " + channelID + "   account:" + account + "  uid:" + uid + "  msgType:" + msgType + "  msgData:" + msgData + "  extra:" + extra);
			}

			@Override
			public void onMessageSendError(String messageID, int ecode) {
				super.onMessageSendError(messageID, ecode);
				mLogger.i("onMessageSendError：messageID  " + messageID + "   ecode:" + ecode);
			}

			@Override
			public void onMessageSendProgress(String account, String messageID, String type, String info) {
				super.onMessageSendProgress(account, messageID, type, info);
				mLogger.i("onMessageSendProgress：account  " + account + "   messageID:" + messageID + "  type:" + type + "  info:" + info);
			}

			@Override
			public void onMessageSendSuccess(String messageID) {
				super.onMessageSendSuccess(messageID);
				mLogger.i("onMessageSendSuccess：messageID  " + messageID);
			}

			@Override
			public void onMessageAppReceived(String msg) {
				super.onMessageAppReceived(msg);
				mLogger.i("onMessageAppReceived：msg  " + msg);
			}

			@Override
			public void onMessageInstantReceive(String account, int uid, String msg) {
				super.onMessageInstantReceive(account, uid, msg);
				mLogger.i("onMessageInstantReceive：account  " + account + "  uid:" + uid + "  msg:" + msg);
			}

			@Override
			public void onMessageChannelReceive(String channelID, String account, int uid, String msg) {
				super.onMessageChannelReceive(channelID, account, uid, msg);
				mLogger.i("onMessageChannelReceive：channelID:" + channelID + "account  " + account + "  uid:" + uid + "  msg:" + msg);
			}


			@Override
			public void onMsg(String from, String t, String msg) {
				super.onMsg(from, t, msg);
				mLogger.i("onMsg：from  " + from + "  t:" + t + "  msg:" + msg);
			}

			@Override
			public void onUserAttrResult(String account, String name, String value) {
				super.onUserAttrResult(account, name, value);
				mLogger.i("onUserAttrResult：account  " + account + "  name:" + name + "  value:" + value);
			}

			@Override
			public void onUserAttrAllResult(String account, String value) {
				super.onUserAttrAllResult(account, value);
				mLogger.i("onUserAttrAllResult：account  " + account + "  value:" + value);
			}

			@Override
			public void onError(String name, int ecode, String desc) {
				super.onError(name, ecode, desc);
				mLogger.i("onError：name  " + name + "  ecode:" + ecode + "  desc:" + desc);
			}

			@Override
			public void onQueryUserStatusResult(String name, String status) {
				super.onQueryUserStatusResult(name, status);
				mLogger.i("onQueryUserStatusResult：name  " + name + "  status:" + status);
			}
		});

	}


	@Override
	public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {

		mLogger.e("onFirstRemoteVideoDecoded:uid:" + uid + "   width:" + width + "   :height" + height + "   elapsed" + elapsed);

	}

	@Override
	public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
		runOnUiThread(() -> {
			if (isFinishing()) {
				return;
			}
			mPresenter.worker().getEngineConfig().mUid = uid;
			if ("true".equals(agora.getIsTest())) {
				agoraAPI.login2(agora.getAppID(), "" + uid, "noneed_token", 0, "", 20, 30);
			} else {
				agoraAPI.login2(agora.getAppID(), "" + uid, agora.getSignalingKey(), 0, "", 20, 30);
			}
		});
	}

	@Override
	public void onUserOffline(int uid, int reason) {

	}

	@Override
	public void onConnectionLost() {

	}

	@Override
	public void onConnectionInterrupted() {

	}

	@Override
	public void onUserMuteVideo(int uid, boolean muted) {

	}

	@Override
	public void onUserMuteAudio(int uid, boolean muted) {

	}

	@Override
	public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {

	}

	@Override
	public void onLastmileQuality(int quality) {

	}

	@Override
	public void onNetworkQuality(int uid, int txQuality, int rxQuality) {

	}

	@Override
	public void onWarning(int warn) {

	}

	@Override
	public void onError(int err) {

	}


	private void translation(View view, float endX, float endY) {
		view.setTranslationX(endX);
		view.setTranslationY(endY);
	}


	@OnClick({R.id.waiter, R.id.stop_audience, R.id.discuss, R.id.doc, R.id.splitViewButton, R.id.privioesPage, R.id.nextPage, R.id.exitPPT})
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.waiter:
				break;
			case R.id.stop_audience:
				break;
			case R.id.discuss:
				break;
			case R.id.doc:
				showLoading();
				mPresenter.getMeetingPPT(meetingJoin.getMeeting().getId());
				break;
			case R.id.splitViewButton:
				final AnimatorSet animatorSet = new AnimatorSet();
				chairmanVideoLayout.setPivotX(0f);
				chairmanVideoLayout.setPivotY(chairmanVideoLayout.getHeight());
				animatorSet.playTogether(
					/*	ObjectAnimator.ofFloat(chairmanVideoLayout, "scaleX", 1f, 0.5f)
								.setDuration(2000),*/
						ObjectAnimator.ofFloat(chairmanVideoLayout, "scaleX", 1f, 0.5f)
								.setDuration(1000)
				);
				animatorSet.start();
				animatorSet.addListener(new Animator.AnimatorListener() {
					@Override
					public void onAnimationStart(Animator animation) {

					}

					@Override
					public void onAnimationEnd(Animator animation) {
						FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) chairmanVideoLayout.getLayoutParams();
						layoutParams.width = DisplayUtil.getWidth(MeetChairManActivityActivity.this);
						chairmanVideoLayout.setLayoutParams(layoutParams);
					}

					@Override
					public void onAnimationCancel(Animator animation) {

					}

					@Override
					public void onAnimationRepeat(Animator animation) {

					}
				});
				break;

			case R.id.privioesPage:

				break;
			case R.id.nextPage:

				break;
			case R.id.exitPPT:
				agoraAPI.channelDelAttr(channelName, DOC_INFO);
				// TODO: 2019-11-21 等回调完成后再修改界面
				PPTImageView.setVisibility(View.GONE);
				recyclerView.setVisibility(View.VISIBLE);
				chairmanVideoLayout.setX(0);
				chairmanVideoLayout.setY(0);
				chairmanVideoLayout.invalidate();
				FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) chairmanVideoLayout.getLayoutParams();
				layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
				layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
				layoutParams.topMargin = 0;
				layoutParams.leftMargin = 0;
				chairmanVideoLayout.setLayoutParams(layoutParams);

				break;
		}
	}


	/**
	 * ppt资料获取后的展示
	 */
	private void showPPTListDialog(ArrayList<Material> materials) {
		View view = View.inflate(this, R.layout.dialog_ppt_list, null);
		view.findViewById(R.id.exit).setOnClickListener(v -> {
			if (pptAlertDialog.isShowing()) {
				pptAlertDialog.dismiss();
			}
		});
		RecyclerView recyclerViewTV = view.findViewById(R.id.meeting_doc_list);
		FocusFixedLinearLayoutManager gridlayoutManager = new FocusFixedLinearLayoutManager(this); // 解决快速长按焦点丢失问题.
		gridlayoutManager.setOrientation(GridLayoutManager.HORIZONTAL);
		recyclerViewTV.setLayoutManager(gridlayoutManager);
		recyclerViewTV.setFocusable(false);
		recyclerViewTV.addItemDecoration(new SpaceItemDecoration((int) (getResources().getDimension(R.dimen.my_px_20)), 0, (int) (getResources().getDimension(R.dimen.my_px_20)), 0));
		MaterialAdapter materialAdapter = new MaterialAdapter(this, materials);
		recyclerViewTV.setAdapter(materialAdapter);
		materialAdapter.setOnClickListener((v, material, position) -> {

			showPPTDetailDialog(material);
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialog);
		builder.setView(view);
		pptAlertDialog = builder.create();
		if (!pptAlertDialog.isShowing()) {
			pptAlertDialog.show();
		}
	}

	private void showPPTDetailDialog(Material material) {
		View view = View.inflate(this, R.layout.dialog_ppt_detail, null);
		ViewPager viewPager = view.findViewById(R.id.view_pager);
		TextView pageText = view.findViewById(R.id.page);
		pageText.setText("第1/" + material.getMeetingMaterialsPublishList().size() + "页");
		viewPager.setOffscreenPageLimit(3);
		viewPager.setAdapter(new PagerAdapter() {
			@Override
			public int getCount() {
				return material.getMeetingMaterialsPublishList().size();
			}

			@NonNull
			@Override
			public Object instantiateItem(@NonNull ViewGroup container, int position) {
				View view = View.inflate(container.getContext(), R.layout.item_doc_detail, null);
				ImageView imageView = view.findViewById(R.id.image_view);
				String imageUrl = ImageHelper.getThumb(material.getMeetingMaterialsPublishList().get(position).getUrl());
				Picasso.with(MeetChairManActivityActivity.this).load(imageUrl).into(imageView);
				container.addView(view);
				return view;
			}

			@Override
			public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
				return view == object;
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
				container.removeView((View) object);
			}
		});
		viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				pageText.setText("第" + (position + 1) + "/" + material.getMeetingMaterialsPublishList().size() + "页");
			}

			@Override
			public void onPageScrollStateChanged(int state) {

			}
		});
		TextView nameText = view.findViewById(R.id.name);
		nameText.setText(material.getName());
		TextView timeText = view.findViewById(R.id.time);
		timeText.setText(material.getCreateDate() + "创建");
		view.findViewById(R.id.use_doc).setOnClickListener(v -> {
			showLoading();
			mCurrentMaterial = material;
			Collections.sort(mCurrentMaterial.getMeetingMaterialsPublishList(), (o1, o2) -> (o1.getPriority() < o2.getPriority()) ? -1 : 1);
			mPresenter.showPPT(meetingJoin.getMeeting().getId(), material.getId());
		});
		view.findViewById(R.id.exit_preview).setOnClickListener(v -> {
			if (mPPtPreviewDialog.isShowing()) {
				mPPtPreviewDialog.dismiss();
			}
		});
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialog);
		builder.setView(view);
		mPPtPreviewDialog = builder.create();
		if (!mPPtPreviewDialog.isShowing()) {
			mPPtPreviewDialog.show();
		}
	}


}
