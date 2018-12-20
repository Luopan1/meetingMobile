package io.agora.openlive.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.hezy.guide.phone.R;
import com.hezy.guide.phone.view.SpaceItemDecoration;

import java.util.HashMap;

import io.agora.openlive.model.VideoStatusData;

public class AudienceRecyclerView extends RecyclerView {
    public AudienceRecyclerView(Context context) {
        super(context);
    }

    public AudienceRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AudienceRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private AudienceRecyclerViewAdapter audienceRecyclerViewAdapter;

    private VideoViewEventListener mEventListener;

    public void setItemEventHandler(VideoViewEventListener listener) {
        this.mEventListener = listener;
    }

    private boolean initAdapter(int localUid, HashMap<Integer, SurfaceView> uids) {
        if (audienceRecyclerViewAdapter == null) {
            audienceRecyclerViewAdapter = new AudienceRecyclerViewAdapter(getContext(), localUid, uids, mEventListener);
            audienceRecyclerViewAdapter.setHasStableIds(true);
            return true;
        }
        return false;
    }

    public void initViewContainer(Context context, int localUid, HashMap<Integer, SurfaceView> uids) {
        boolean newCreated = initAdapter(localUid, uids);

        if (!newCreated) {
            audienceRecyclerViewAdapter.setLocalUid(localUid);
            audienceRecyclerViewAdapter.init(uids, localUid, true);
        }

        this.setAdapter(audienceRecyclerViewAdapter);

        this.setLayoutManager(new GridLayoutManager(context, 3, RecyclerView.VERTICAL, false));

        this.addItemDecoration(new SpaceItemDecoration((int) (getResources().getDimension(R.dimen.my_px_12)), 0, (int) (getResources().getDimension(R.dimen.my_px_12)), 0));

        audienceRecyclerViewAdapter.notifyDataSetChanged();
    }

    public SurfaceView getSurfaceView(int index) {
        return audienceRecyclerViewAdapter.getItem(index).mView;
    }

    public VideoStatusData getItem(int position) {
        return audienceRecyclerViewAdapter.getItem(position);
    }
}
