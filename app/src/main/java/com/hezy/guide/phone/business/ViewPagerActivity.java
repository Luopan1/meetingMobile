package com.hezy.guide.phone.business;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.hezy.family.photolib.Info;
import com.hezy.family.photolib.PhotoView;
import com.hezy.guide.phone.BaseApplication;
import com.hezy.guide.phone.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by liuheng on 2015/8/19.
 */
public class ViewPagerActivity extends Activity {

    private ViewPager mPager;
    private Info info;
    private ArrayList<String> mListPhotp;

//    private int[] imgsId = new int[]{R.mipmap.baby_default_avatar,R.mipmap.baby_default_avatar};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pager);
        mListPhotp = getIntent().getStringArrayListExtra("imglist");

        info = getIntent().getParcelableExtra("info");
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMargin((int) (getResources().getDisplayMetrics().density * 15));
        mPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mListPhotp.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                PhotoView view = new PhotoView(ViewPagerActivity.this);
                view.enable();
                view.setScaleType(ImageView.ScaleType.FIT_CENTER);
//                view.setImageResource(imgsId[position]);
                Picasso.with(BaseApplication.getInstance()).load(mListPhotp.get(position)).into(view);
                if(position == 0){
                    view.animaFrom((Info) getIntent().getParcelableExtra("info"));
                }
                container.addView(view);
                return view;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView((View) object);
            }
        });
    }
}
