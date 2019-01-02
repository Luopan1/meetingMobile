package com.hezy.guide.phone.business;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.zy.guide.phone.R;
import com.hezy.guide.phone.business.adapter.ImageBrowerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ImageBrowerActivity extends BasicActivity {

    private RecyclerView mRecycler;
    private ImageBrowerAdapter adapter;
    private List<String> imgList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_brower);
        initView();
        imgList = getIntent().getStringArrayListExtra("imglist");
        adapter = new ImageBrowerAdapter(this,imgList,getIntent().getParcelableExtra("info"));
        mRecycler.setAdapter(adapter);
    }
    private void initView(){
        mRecycler = (RecyclerView)this.findViewById(R.id.recycler);
        LinearLayoutManager gridlayoutManager = new LinearLayoutManager(this); // 解决快速长按焦点丢失问题.
        gridlayoutManager.setOrientation(GridLayoutManager.HORIZONTAL);
        mRecycler.setLayoutManager(gridlayoutManager);

    }

    @Override
    public String getStatisticsTag() {
        return "浏览图片";
    }
}
