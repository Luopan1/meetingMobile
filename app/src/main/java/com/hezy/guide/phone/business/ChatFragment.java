package com.hezy.guide.phone.business;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hezy.family.photolib.Info;
import com.hezy.family.photolib.PhotoView;
import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.GuideLogAdapter;
import com.hezy.guide.phone.business.adapter.ImageBrowerAdapter;
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

public class ChatFragment extends BaseFragment implements chatAdapter.onClickCallBack, TakePhoto.TakeResultListener, InvokeListener,inputAdapter.onItemClickInt {

    //    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView recyclerViewChat, recyclerViewInput;
    //    private RecyclerView mRecycler;
    private TextView emptyText;
    private String imagePath;
    private InvokeParam invokeParam;
    private static String FORUM_REVOKE = "FORUM_REVOKE";
    private static String FORUM_SEND_CONTENT = "FORUM_SEND_CONTENT";
    private LinearLayoutManager mLayoutManager;
    private chatAdapter adapter;
    private ImageBrowerAdapter imgAdapter;
    private boolean isRefresh;
    private int mTotalPage = -1;
    private int mPageNo = 1;
    private Button btnSend;
    private boolean hideInput = false;
    private EditText mEditText;
    private int count = 0;
    private RelativeLayout rlContent;
    ArrayList<String> list = new ArrayList<>();
    private TakePhoto takePhoto;
    private Subscription subscription;
    private RelativeLayout openCamera;
    private boolean onCreate = true;
    private String mMeetingId = "";
    private String atUserId="";
    private List<ChatMesData.PageDataEntity> dataChat = new ArrayList<>();

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
                                    params.put("meetingId",mMeetingId);
                                    params.put("ts", System.currentTimeMillis());
                                    params.put("content", Preferences.getImgUrl() + key);
                                    params.put("type", 1);
                                    ApiClient.getInstance().expostorPostChatMessage(TAG, expostorStatsCallback, params);

                                } else {
                                    showToast("上传失败" + info.error);
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
        mMeetingId = getActivity().getIntent().getStringExtra("meetingId");
//        initViewImage(view);
        initRecy();
        setData();
        requestRecord(true);
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

        recyclerViewChat.setOnScrollListener(new RecyclerView.OnScrollListener() {
            private int lastVisibleItemPosition;

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView,
                                             int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                        && lastVisibleItemPosition == 0
                        && !((mPageNo-1) == mTotalPage)) {
                    Log.v("onscrolllistener", "进入停止状态==" + lastVisibleItemPosition);
                    requestRecord(false);
//                    requestLiveVideoListNext();
                    if (mPageNo != -1 && mTotalPage != -1 && !(mPageNo == mTotalPage)) {
//                        requestRecord(String.valueOf(mPageNo + 1), "20");
                    }

                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (onCreate) {
                    onCreate = false;
                } else {
                    lastVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();
                    Log.v("onscrolllistener", "lastvisibleitemp==" + lastVisibleItemPosition);
                    if (lastVisibleItemPosition == 0) {
//                        requestRecord(false);
                    }
                }


            }
        });
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
                    requestRecordOnlyLast(true);
                }

            }
        });
        return view;
    }

    public TakePhoto getTakePhoto() {
        if (takePhoto == null) {
            takePhoto = (TakePhoto) TakePhotoInvocationHandler.of(this).bind(new TakePhotoImpl(this, this));
        }
        CompressConfig compressConfig = new CompressConfig.Builder().setMaxPixel(800).create();
        takePhoto.onEnableCompress(compressConfig, true);
        return takePhoto;
    }

    //    private void initViewImage(View rootview){
//        mRecycler = (RecyclerView)rootview.findViewById(R.id.recycler);
//        LinearLayoutManager gridlayoutManager = new LinearLayoutManager(getActivity()); // 解决快速长按焦点丢失问题.
//        gridlayoutManager.setOrientation(GridLayoutManager.HORIZONTAL);
//        mRecycler.setLayoutManager(gridlayoutManager);
//
//    }
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
        inputAdapter iadapter = new inputAdapter(getActivity(), list2,this);

        recyclerViewInput.setAdapter(iadapter);
    }

    private void initView(View rootView) {
        openCamera = (RelativeLayout) rootView.findViewById(R.id.open_camera);
        recyclerViewChat = (RecyclerView) rootView.findViewById(R.id.recycler1);
        recyclerViewInput = (RecyclerView) rootView.findViewById(R.id.recycler2);
        btnSend = (Button) rootView.findViewById(R.id.btn_send);
        mEditText = (EditText) rootView.findViewById(R.id.edit_text);

        openCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("imput98989","count=="+count);
                if (count == 0) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
//                    recyclerViewInput.setVisibility(View.VISIBLE);
                    handler.sendEmptyMessageDelayed(11,100);
                    count = 1;
                } else if (count == 1) {

                    recyclerViewInput.setVisibility(View.GONE);
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                    count = 2;
                } else if (count == 2) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
//                    recyclerViewInput.setVisibility(View.VISIBLE);
                    handler.sendEmptyMessageDelayed(11,100);

                    count = 1;
                }

            }
        });
        rlContent = (RelativeLayout) rootView.findViewById(R.id.rl_content);
        rlContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerViewInput.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            }
        });
        mEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("medittext98", "点击事件");
                handler.sendEmptyMessageDelayed(10, 100);
//                recyclerViewChat.scrollToPosition(dataChat.size()-1);
            }
        });
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                Log.v("medittext98", "setOnEditorActionListener==" + i);
                return false;
            }
        });

        mEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                Log.v("medittext98", "onkey事件==" + keyEvent.getAction());
                return false;
            }
        });
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (mEditText.getText().toString().isEmpty()) {
                    openCamera.setVisibility(View.VISIBLE);
                    btnSend.setVisibility(View.GONE);
                } else {
                    openCamera.setVisibility(View.GONE);
                    btnSend.setVisibility(View.VISIBLE);

                }
            }
        });
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendAction();
//                    HashMap<String, Object> params = new HashMap<String, Object>();
//                    params.put("meetingId", "e7d627b750114191ba556d7ca188f33f");
//                    params.put("ts", System.currentTimeMillis());
//                    params.put("content", mEditText.getText().toString());
//                    params.put("type", 2);
//                    ApiClient.getInstance().expostorPostChatMessage(TAG, expostorStatsCallback, params);
                }
                return false;
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAction();

//                configTakePhotoOption(takePhoto);
//                File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
//                if (!file.getParentFile().exists())
//                    file.getParentFile().mkdirs();
//                final Uri imageUri = Uri.fromFile(file);
////                takePhoto.onPickFromGallery();
//
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

    private void sendAction(){
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("meetingId", mMeetingId);
        params.put("ts", System.currentTimeMillis());
        params.put("content", mEditText.getText().toString());
        params.put("type", 0);
        if(!atUserId.isEmpty()){
            params.put("atailUserId",atUserId);
            atUserId = "";
        }
        mEditText.setText("");
        ApiClient.getInstance().expostorPostChatMessage(TAG, expostorStatsCallback, params);
    }

    private OkHttpCallback expostorStatsCallback = new OkHttpCallback<Bucket<ChatMesData.PageDataEntity>>() {

        @Override
        public void onSuccess(Bucket<ChatMesData.PageDataEntity> entity) {

            ToastUtils.showToast("提交成功");
        }
    };

    private void initRecy() {
        mLayoutManager = new LinearLayoutManager(getActivity()); // 解决快速长按焦点丢失问题.
        mLayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        recyclerViewChat.setLayoutManager(mLayoutManager);
        recyclerViewChat.setFocusable(false);
//        recyclerViewChat.addItemDecoration(new SpaceItemDecoration((int) (getResources().getDimension(R.dimen.my_px_15)), 0, (int) (getResources().getDimension(R.dimen.my_px_15)), 0));

        GridLayoutManager gridlayoutManager2 = new GridLayoutManager(getActivity(), 4); // 解决快速长按焦点丢失问题.
        gridlayoutManager2.setOrientation(GridLayoutManager.VERTICAL);
        recyclerViewInput.setLayoutManager(gridlayoutManager2);
        recyclerViewInput.setFocusable(false);
        recyclerViewInput.setVisibility(View.GONE);
    }

    private void requestRecord(boolean last) {
        requestRecord(mPageNo + "", "10");
        isRefresh = true;
        mPageNo = mPageNo + 1;
    }

    private void requestRecordOnlyLast(boolean last) {
        requestRecordOnlyLast(1 + "", "1");
    }

    private void requestRecordOnlyLast(String pageNo, String pageSize) {
        ApiClient.getInstance().getChatMessages(this, mMeetingId, pageNo, pageSize, new OkHttpCallback<BaseBean<ChatMesData>>() {
            @Override
            public void onSuccess(BaseBean<ChatMesData> entity) {
                    dataChat.addAll(entity.getData().getPageData());
                    initData(dataChat,true);
//

            }

            @Override
            public void onFinish() {
                super.onFinish();
//                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }
    private void requestRecord(String pageNo, String pageSize) {
        ApiClient.getInstance().getChatMessages(this, mMeetingId, pageNo, pageSize, new OkHttpCallback<BaseBean<ChatMesData>>() {
            @Override
            public void onSuccess(BaseBean<ChatMesData> entity) {
                mTotalPage = entity.getData().getTotalPage();
                if (dataChat.size() == 0) {
                    dataChat = entity.getData().getPageData();
                    initData(dataChat,true);
                } else {
                    dataChat.addAll(0, entity.getData().getPageData());
                    initData(dataChat,false);
//                    dataChat.addAll();
                }


            }

            @Override
            public void onFinish() {
                super.onFinish();
//                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    private void initData(List<ChatMesData.PageDataEntity> data, boolean last) {
        adapter = new chatAdapter(getActivity(), data, this);
        recyclerViewChat.setAdapter(adapter);

        if(last){
            if(handler.hasMessages(10)){
                handler.removeMessages(10);
            }
            handler.sendEmptyMessageDelayed(10, 100);
        }

//        ArrayList<String> imgList = new ArrayList<>();
//        for(int i=0; i<data.size();i++){
//            imgList.add(data.get(i).getContent());
//        }
//        imgAdapter = new ImageBrowerAdapter(getActivity(),imgList);
//        mRecycler.setAdapter(imgAdapter);
    }

    @Override
    protected void normalOnClick(View v) {
        switch (v.getId()) {
            case R.id.mLayoutNoData:
//                requestRecord();
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
    public void onLongImgHead(String name, String userId) {
        mEditText.setText("@"+name);
        mEditText.setSelection(mEditText.getText().length());
        atUserId = userId;
//        recyclerViewChat.scrollToPosition(dataChat.size() - 1);
    }

    @Override
    public void onLongContent(View view, String id) {
        showPopupWindow(view, id);
    }

    @Override
    public void onEditCallBack(String content) {
        mEditText.setText(content);
        mEditText.setSelection(mEditText.getText().length());
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 11){
                recyclerViewInput.setVisibility(View.VISIBLE);
            }else {
                recyclerViewChat.scrollToPosition(dataChat.size() - 1);
            }

        }
    };
//    @Override
//    public void onClickImage(Info info) {
//        mRecycler.setVisibility(View.VISIBLE);
////
//        Message msg = new Message();
//        msg.obj = info;
//        handler.sendMessageDelayed(msg,10);
//
//    }

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
        Log.i(TAG, "takeSuccess：");
    }

    @Override
    public PermissionManager.TPermissionType invoke(InvokeParam invokeParam) {
        PermissionManager.TPermissionType type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod());
        if (PermissionManager.TPermissionType.WAIT.equals(type)) {
            this.invokeParam = invokeParam;
        }
        return type;
    }

    @Override
    public void onItemClick(int pos) {
        configTakePhotoOption(takePhoto);
        if(pos == 0){
            takePhoto.onPickFromGallery();
        }else {
            File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            final Uri imageUri = Uri.fromFile(file);
            takePhoto.onPickFromCapture(imageUri);
        }

    }

    private void showPopupWindow(View view, String id) {

        // 一个自定义的布局，作为显示的内容
        View contentView = LayoutInflater.from(mContext).inflate(
                R.layout.pop_window, null);
        // 设置按钮的点击事件
        TextView button = (TextView) contentView.findViewById(R.id.del);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
//                Toast.makeText(mContext, "button is pressed",
//                        Toast.LENGTH_SHORT).show();
                ApiClient.getInstance().expostorDeleteChatMessage(this,expostorStatsCallback,id);

            }
        });

        final PopupWindow popupWindow = new PopupWindow(contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setTouchable(true);

        popupWindow.setTouchInterceptor(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                Log.i("mengdd", "onTouch : ");

                return false;
                // 这里如果返回true的话，touch事件将被拦截
                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
            }
        });

        // 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
        // 我觉得这里是API的一个bug
        popupWindow.setBackgroundDrawable(getResources().getDrawable(
                R.color.actionsheet_blue));

        // 设置好参数之后再show
        popupWindow.showAsDropDown(view);

    }
}
