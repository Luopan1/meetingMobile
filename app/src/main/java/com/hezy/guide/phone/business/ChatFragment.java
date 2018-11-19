package com.hezy.guide.phone.business;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.GuideLogAdapter;
import com.hezy.guide.phone.business.adapter.chatAdapter;
import com.hezy.guide.phone.business.adapter.inputAdapter;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.ChatMesData;
import com.hezy.guide.phone.entities.ExpostorOnlineStats;
import com.hezy.guide.phone.entities.QiniuToken;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.event.CallEvent;
import com.hezy.guide.phone.event.ForumSendEvent;
import com.hezy.guide.phone.event.HangDownEvent;
import com.hezy.guide.phone.event.HangOnEvent;
import com.hezy.guide.phone.event.HangUpEvent;
import com.hezy.guide.phone.event.ResolutionChangeEvent;
import com.hezy.guide.phone.event.SetUserStateEvent;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.RxBus;
import com.hezy.guide.phone.utils.ToastUtils;
import com.hezy.guide.phone.utils.helper.TakePhotoHelper;
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoImpl;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.InvokeParam;
import com.jph.takephoto.model.TContextWrap;
import com.jph.takephoto.model.TResult;
import com.jph.takephoto.model.TakePhotoOptions;
import com.jph.takephoto.permission.InvokeListener;
import com.jph.takephoto.permission.PermissionManager;
import com.jph.takephoto.permission.TakePhotoInvocationHandler;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import rx.Subscription;
import rx.functions.Action1;

public class ChatFragment extends BaseFragment implements chatAdapter.onClickCallBack, TakePhoto.TakeResultListener, InvokeListener {

    //    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView recyclerViewChat, recyclerViewInput;
    private TextView emptyText;
    private String imagePath;
    private InvokeParam invokeParam;
    private static String FORUM_REVOKE = "FORUM_REVOKE";
    private static String FORUM_SEND_CONTENT = "FORUM_SEND_CONTENT";
    private LinearLayoutManager mLayoutManager;
    private chatAdapter adapter;
    private boolean isRefresh;
    private int mTotalPage = -1;
    private int mPageNo = -1;
    private Button btnSend;
    private boolean hideInput = false;
    private EditText mEditText;
    private int count = 0;
    private RelativeLayout rlContent;
    ArrayList<String> list = new ArrayList<>();
    private TakePhoto takePhoto;
    private Subscription subscription;

    @Override
    public String getStatisticsTag() {
        return "聊天";
    }

    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        return fragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        getTakePhoto().onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        getTakePhoto().onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void uploadImage() {
        ApiClient.getInstance().requestQiniuToken(this, new OkHttpCallback<BaseBean<QiniuToken>>() {

            @Override
            public void onSuccess(BaseBean<QiniuToken> result) {
                String token = result.getData().getToken();
                if (TextUtils.isEmpty(token)) {
                    showToast("七牛token获取错误");
                    return;
                }
                Configuration config = new Configuration.Builder().connectTimeout(5).responseTimeout(5).build();
                UploadManager uploadManager = new UploadManager(config);
                System.out.println(imagePath);
                uploadManager.put(
                        new File(imagePath),
                        "osg/user/expostor/photo/" + UUID.randomUUID().toString().replace("-", "") + ".jpg",
                        token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject response) {
                                if (info.isNetworkBroken() || info.isServerError()) {
                                    showToast("上传失败");
                                    return;
                                }
                                if (info.isOK()) {
                                    HashMap<String, Object> params = new HashMap<String, Object>();
                                    params.put("meetingId", "e7d627b750114191ba556d7ca188f33f");
                                    params.put("ts", System.currentTimeMillis());
                                    params.put("content", Preferences.getImgUrl() + key);
                                    params.put("type", 1);
                                    ApiClient.getInstance().expostorPostChatMessage(TAG, expostorStatsCallback, params);

                                } else {
                                    showToast("上传失败"+info.error);
                                }
                            }
                        },
                        new UploadOptions(null, null, true, new UpProgressHandler() {
                            @Override
                            public void progress(final String key, final double percent) {
                            }

                        }, null));
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getTakePhoto().onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_meeting_chat, null, false);
//        mSwipeRefreshLayout = view.findViewById(R.id.mSwipeRefreshLayout);
        initView(view);
        initRecy();
        setData();
        requestRecord();
//        getTakePhoto();
//        configTakePhotoOption(takePhoto);
//        mRecyclerView = view.findViewById(R.id.recycler1);
////        emptyText = view.findViewById(R.id.emptyView);
//        mAdapter = new chatAdapter(mContext);
//
//        //设置布局管理器
//        mLayoutManager = new LinearLayoutManager(mContext);
//        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
//        mRecyclerView.setLayoutManager(mLayoutManager);
//        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext,DividerItemDecoration.VERTICAL));
//        mRecyclerView.setAdapter(mAdapter);

//        recyclerViewChat.setOnScrollListener(new RecyclerView.OnScrollListener() {
//            private int lastVisibleItemPosition;
//
//            @Override
//            public void onScrollStateChanged(RecyclerView recyclerView,
//                                             int newState) {
//                super.onScrollStateChanged(recyclerView, newState);
//                if (newState == RecyclerView.SCROLL_STATE_IDLE
//                        && !mSwipeRefreshLayout.isRefreshing()
//                        && lastVisibleItemPosition + 1 == adapter.getItemCount()
//                        && !(mPageNo == mTotalPage)) {
////                    requestLiveVideoListNext();
//                    if (mPageNo != -1 && mTotalPage != -1 && !(mPageNo == mTotalPage)) {
////                        requestRecord(String.valueOf(mPageNo + 1), "20");
//                    }
//
//                }
//            }
//
//            @Override
//            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
//                super.onScrolled(recyclerView, dx, dy);
////                lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
//            }
//        });
//
//        //刷新与分页加载
//        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
////                requestRecord();
//            }
//        });

//        requestRecord();

        subscription = RxBus.handleMessage(new Action1() {
            @Override
            public void call(Object o) {
                if (o instanceof ForumSendEvent) {
                    requestRecord();
                }

            }
        });
        return view;
    }

    public TakePhoto getTakePhoto() {
        if (takePhoto == null) {
            takePhoto = (TakePhoto) TakePhotoInvocationHandler.of(this).bind(new TakePhotoImpl(this, this));
        }
        CompressConfig compressConfig=new CompressConfig.Builder().setMaxPixel(800).create();
        takePhoto.onEnableCompress(compressConfig,true);
        return takePhoto;
    }

    private void configTakePhotoOption(TakePhoto takePhoto) {
        TakePhotoOptions.Builder builder = new TakePhotoOptions.Builder();

        builder.setCorrectImage(true);
        takePhoto.setTakePhotoOptions(builder.create());
    }

    private void setData() {
        ArrayList<String> list2 = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String str = "abd";
            list2.add(str);
        }
        inputAdapter iadapter = new inputAdapter(getActivity(), list2);
        recyclerViewInput.setAdapter(iadapter);
    }

    private void initView(View rootView) {
        recyclerViewChat = (RecyclerView) rootView.findViewById(R.id.recycler1);
        recyclerViewInput = (RecyclerView) rootView.findViewById(R.id.recycler2);
        btnSend = (Button) rootView.findViewById(R.id.btn_send);
        mEditText = (EditText) rootView.findViewById(R.id.edit_text);

        rlContent = (RelativeLayout) rootView.findViewById(R.id.rl_content);
        rlContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerViewInput.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            }
        });


        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configTakePhotoOption(takePhoto);
                File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
                if (!file.getParentFile().exists())
                    file.getParentFile().mkdirs();
                final Uri imageUri = Uri.fromFile(file);
                takePhoto.onPickFromCapture(imageUri);
//                takePhoto.onPickFromCapture(imageUri);
//                createItem();

//                HashMap<String, Object> params = new HashMap<String, Object>();
//                params.put("meetingId", "e7d627b750114191ba556d7ca188f33f");
//                params.put("ts", System.currentTimeMillis());
//                params.put("content", mEditText.getText().toString());
//                params.put("type", 2);
//                ApiClient.getInstance().expostorPostChatMessage(TAG, expostorStatsCallback, params);
//                if(count == 0){
//                    recyclerViewInput.setVisibility(View.VISIBLE);
//                    count = 1;
//                }else if(count == 1){
//
//                    recyclerViewInput.setVisibility(View.GONE);
//                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
//                    count = 2;
//                }else if(count ==2){
//                    recyclerViewInput.setVisibility(View.VISIBLE);
//                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
//                    count = 1;
//                }

//                if(hideInput){
//                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
//                    hideInput = false;
//                }else {
//                    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
//                    hideInput = true;
//                }
            }
        });
    }

    private OkHttpCallback expostorStatsCallback = new OkHttpCallback<Bucket<ChatMesData.PageDataEntity>>() {

        @Override
        public void onSuccess(Bucket<ChatMesData.PageDataEntity> entity) {

            ToastUtils.showToast("提交成功");
        }
    };

    private void initRecy() {
        LinearLayoutManager gridlayoutManager = new LinearLayoutManager(getActivity()); // 解决快速长按焦点丢失问题.
        gridlayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        recyclerViewChat.setLayoutManager(gridlayoutManager);
        recyclerViewChat.setFocusable(false);
//        recyclerViewChat.addItemDecoration(new SpaceItemDecoration((int) (getResources().getDimension(R.dimen.my_px_15)), 0, (int) (getResources().getDimension(R.dimen.my_px_15)), 0));

        GridLayoutManager gridlayoutManager2 = new GridLayoutManager(getActivity(), 4); // 解决快速长按焦点丢失问题.
        gridlayoutManager2.setOrientation(GridLayoutManager.VERTICAL);
        recyclerViewInput.setLayoutManager(gridlayoutManager2);
        recyclerViewInput.setFocusable(false);
        recyclerViewInput.setVisibility(View.GONE);
    }

    private void requestRecord() {
        requestRecord("1", "20");
        isRefresh = true;
        mPageNo = 1;
    }

    private void requestRecord(String pageNo, String pageSize) {
        ApiClient.getInstance().getChatMessages(this, "e7d627b750114191ba556d7ca188f33f", pageNo, pageSize, new OkHttpCallback<BaseBean<ChatMesData>>() {
            @Override
            public void onSuccess(BaseBean<ChatMesData> entity) {
//                if (entity.getData().getTotalCount() == 0) {
//                    emptyText.setVisibility(View.VISIBLE);
////                    mSwipeRefreshLayout.setVisibility(View.GONE);
//                } else {
//                    emptyText.setVisibility(View.GONE);
////                    mSwipeRefreshLayout.setVisibility(View.VISIBLE);
//                }
                initData(entity.getData().getPageData());
//                if (isRefresh) {
//                    isRefresh = false;
////                    mAdapter.setData(entity.getData().getPageData());
//                } else {
////                    mAdapter.addData(entity.getData().getPageData());
//                }
//                mPageNo = entity.getData().getPageNo();
//                mTotalPage = entity.getData().getTotalPage();
//                adapter.notifyDataSetChanged();


            }

            @Override
            public void onFinish() {
                super.onFinish();
//                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    private void initData(List<ChatMesData.PageDataEntity> data) {
        adapter = new chatAdapter(getActivity(), data, this);
        recyclerViewChat.setAdapter(adapter);
    }

    @Override
    protected void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mLayoutNoData:
                requestRecord();
                break;
        }
    }

    @Override
    public void onClickCallBackFuc() {
        recyclerViewInput.setVisibility(View.GONE);
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
    }

    @Override
    public void takeSuccess(TResult result) {
        imagePath = result.getImage().getOriginalPath();
//        Picasso.with(BaseApplication.getInstance()).load("file:" + imagePath).into(mBinding.imgUserInfoHead);
        uploadImage();
        Log.i(TAG, "takeSuccess：" + result.getImage().getOriginalPath());
    }

    @Override
    public void takeFail(TResult result, String msg) {
        Log.i(TAG, "takeSuccess：" + msg);
    }

    @Override
    public void takeCancel() {
        Log.i(TAG, "takeSuccess：" );
    }

    @Override
    public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
        PermissionManager.TPermissionType type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod());
        if (PermissionManager.TPermissionType.WAIT.equals(type)) {
            this.invokeParam = invokeParam;
        }
        return type;
    }
}
