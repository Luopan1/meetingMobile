package com.hezy.guide.phone.business;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hezy.guide.phone.ApiClient;
import com.hezy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.GuideLogAdapter;
import com.hezy.guide.phone.business.adapter.chatAdapter;
import com.hezy.guide.phone.business.adapter.inputAdapter;
import com.hezy.guide.phone.entities.Bucket;
import com.hezy.guide.phone.entities.ChatMesData;
import com.hezy.guide.phone.entities.ExpostorOnlineStats;
import com.hezy.guide.phone.entities.RecordData;
import com.hezy.guide.phone.entities.base.BaseBean;
import com.hezy.guide.phone.persistence.Preferences;
import com.hezy.guide.phone.utils.Logger;
import com.hezy.guide.phone.utils.OkHttpCallback;
import com.hezy.guide.phone.utils.ToastUtils;
import com.jph.takephoto.app.TakePhoto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChatFragment extends BaseFragment implements chatAdapter.onClickCallBack{

//    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView recyclerViewChat,recyclerViewInput;
    private TextView emptyText;

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
    @Override
    public String getStatisticsTag() {
        return "聊天";
    }

    public static ChatFragment newInstance() {
        ChatFragment fragment = new ChatFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_meeting_chat, null, false);
//        mSwipeRefreshLayout = view.findViewById(R.id.mSwipeRefreshLayout);
        initView(view);
        initRecy();
        setData();
        requestRecord();
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

        return view;
    }

    private void setData(){

//        for(int i=0; i<25; i++){
//            String str = ""+i;
//            list.add(str);
//        }
//        adapter = new chatAdapter(getActivity(),list,this);
//        recyclerViewChat.setAdapter(adapter);
//

        ArrayList<String> list2 = new ArrayList<>();
        for(int i=0; i<2; i++){
            String str = "abd";
            list2.add(str);
        }
        inputAdapter iadapter = new inputAdapter(getActivity(),list2);
        recyclerViewInput.setAdapter(iadapter);
    }

    private void initView(View rootView){
        recyclerViewChat = (RecyclerView)rootView.findViewById(R.id.recycler1);
        recyclerViewInput = (RecyclerView)rootView.findViewById(R.id.recycler2);
        btnSend = (Button)rootView.findViewById(R.id.btn_send);
        mEditText = (EditText)rootView.findViewById(R.id.edit_text);

        rlContent = (RelativeLayout)rootView.findViewById(R.id.rl_content);
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
//                createItem();

                HashMap<String, Object> params = new HashMap<String, Object>();
                params.put("meetingId", "e7d627b750114191ba556d7ca188f33f");
                params.put("ts", System.currentTimeMillis());
                params.put("content", mEditText.getText().toString());
                params.put("atailUserId", Preferences.getUserId());
                ApiClient.getInstance().expostorPostChatMessage(TAG, expostorStatsCallback, params);
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
//
////                if(hideInput){
////                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
////                    hideInput = false;
////                }else {
////                    imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
////                    hideInput = true;
////                }
            }
        });
    }
    private OkHttpCallback expostorStatsCallback = new OkHttpCallback<Bucket<ChatMesData.PageDataEntity>>() {

        @Override
        public void onSuccess(Bucket<ChatMesData.PageDataEntity> entity) {

            ToastUtils.showToast("提交成功");
        }
    };
    private void initRecy(){
        LinearLayoutManager gridlayoutManager = new LinearLayoutManager(getActivity()); // 解决快速长按焦点丢失问题.
        gridlayoutManager.setOrientation(GridLayoutManager.VERTICAL);
        recyclerViewChat.setLayoutManager(gridlayoutManager);
        recyclerViewChat.setFocusable(false);
//        recyclerViewChat.addItemDecoration(new SpaceItemDecoration((int) (getResources().getDimension(R.dimen.my_px_15)), 0, (int) (getResources().getDimension(R.dimen.my_px_15)), 0));

        GridLayoutManager gridlayoutManager2 = new GridLayoutManager(getActivity(),4); // 解决快速长按焦点丢失问题.
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
        ApiClient.getInstance().getChatMessages(this,"e7d627b750114191ba556d7ca188f33f", pageNo, pageSize, new OkHttpCallback<BaseBean<ChatMesData>>() {
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


    private void initData(List<ChatMesData.PageDataEntity> data){
        adapter = new chatAdapter(getActivity(),data,this);
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
}
