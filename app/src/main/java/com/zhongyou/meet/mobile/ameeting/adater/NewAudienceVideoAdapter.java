package com.zhongyou.meet.mobile.ameeting.adater;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.android.vlayout.DelegateAdapter;
import com.alibaba.android.vlayout.LayoutHelper;
import com.alibaba.android.vlayout.VirtualLayoutManager;
import com.alibaba.android.vlayout.layout.StaggeredGridLayoutHelper;
import com.zhongyou.meet.mobile.BuildConfig;
import com.zhongyou.meet.mobile.R;
import com.zhongyou.meet.mobile.entities.AudienceVideo;
import com.zhongyou.meet.mobile.utils.DisplayUtil;
import com.zhongyou.meet.mobile.utils.Logger;
import com.zhongyou.meet.mobile.utils.SizeUtils;
import com.zhongyou.meet.mobile.utils.listener.OnDoubleClickListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import timber.log.Timber;

public class NewAudienceVideoAdapter extends DelegateAdapter.Adapter<NewAudienceVideoAdapter.AudienceVideoViewHolder> {

	private Context context;
	private ArrayList<AudienceVideo> audienceVideos = new ArrayList<AudienceVideo>();
	private AudienceVideoViewHolder mAudienceVideoViewHolder;
	private int height, width;

	public NewAudienceVideoAdapter(Context context, LayoutHelper layoutHelper) {
		this.context = context;
		this.mLayoutHelper = layoutHelper;
		height = DisplayUtil.dip2px(context, 70);
		width = DisplayUtil.dip2px(context, 114);

	}


	public void setLayoutHelper(LayoutHelper layoutHelper) {
		this.mLayoutHelper = layoutHelper;
	}

	public synchronized void insertItem(AudienceVideo audienceVideo) {
		for (int i = 0; i <audienceVideos.size() ; i++) {
			if (audienceVideos.get(i).getUid()==audienceVideo.getUid()){
				return;
			}
		}
		if (audienceVideo.isBroadcaster()) {
			this.audienceVideos.add(0, audienceVideo);
			notifyDataSetChanged();
		} else {
			this.audienceVideos.add(audienceVideos.size(), audienceVideo);
			notifyDataSetChanged();
		}
	}

	public void changeLists(ArrayList<AudienceVideo> audienceVideos) {
		this.audienceVideos = audienceVideos;
	}

	public synchronized void insertItem(int position, AudienceVideo audienceVideo) {
		if (audienceVideo.isBroadcaster()) {
			this.audienceVideos.add(0, audienceVideo);
			notifyDataSetChanged();
		} else {
			this.audienceVideos.add(position, audienceVideo);
			notifyDataSetChanged();
		}
	}

	public synchronized void insetChairMan(int position, AudienceVideo audienceVideo) {
		this.audienceVideos.add(position, audienceVideo);
		notifyItemInserted(audienceVideos.size());
	}

	/*public void changItemHeight(SizeUtils sizeUtils){
		if (mAudienceVideoViewHolder!=null){
		}
	}*/

	public int getDataSize() {
		if (audienceVideos != null) {
			return audienceVideos.size();
		}
		return 0;
	}

	public ArrayList<AudienceVideo> getAudienceVideoLists() {
		return audienceVideos;
	}

	public synchronized void deleteItem(int uid) {
		Iterator<AudienceVideo> iterator = audienceVideos.iterator();
		while (iterator.hasNext()) {
			AudienceVideo audienceVideo = iterator.next();
			if (audienceVideo.getUid() == uid) {
				int position = audienceVideos.indexOf(audienceVideo);
				audienceVideo.getSurfaceView().setZOrderMediaOverlay(false);
				audienceVideo.getSurfaceView().setZOrderOnTop(false);
				Log.e("deleteItem", "deleteItem: "+position);
//                Log.v("delete", "1--" + audienceVideos.size() + "--position--" + position);
				iterator.remove();
//                Log.v("delete", "2--" + audienceVideos.size());
				notifyDataSetChanged();
			}
		}
	}


	public synchronized void deleteItemById(int uid){
		for (int i = 0; i <audienceVideos.size() ; i++) {
			if (audienceVideos.get(i).getUid()==uid){
				audienceVideos.remove(i);
				Log.e("deleteItemById", "deleteItem: i==="+i);
				notifyItemRemoved(i);
				break;
			}
		}
	}
	public boolean isHaveChairMan() {
		for (int i = 0; i < audienceVideos.size(); i++) {
			if (audienceVideos.get(i).isBroadcaster()) {
				return true;
			}
		}
		return false;
	}


	public int getPositionById(int uid){
		for (int i = 0; i <audienceVideos.size() ; i++) {
			if (audienceVideos.get(i).getUid()==uid){
				return i;

			}
		}
		return -1;
	}


	public int getChairManPosition() {
		for (int i = 0; i < audienceVideos.size(); i++) {
			if (audienceVideos.get(i).isBroadcaster()) {
				return i;
			}
		}
		return -1;
	}

	public synchronized void removeItem(int position) {
		audienceVideos.remove(position);
		notifyDataSetChanged();

	}

	public void setVolumeByUid(int uid, int volume) {
		for (AudienceVideo audienceVideo : audienceVideos) {
			if (audienceVideo.getUid() == uid) {
//                Log.v("update_volume", "1--" + audienceVideo.toString());
				audienceVideo.setVolume(volume);
				int position = audienceVideos.indexOf(audienceVideo);
//                Log.v("update_volume", "2--" + audienceVideo.toString() + "--position--" + position);
				notifyItemChanged(position, 1);
			} else {
//                Log.v("update_volume", "不是更新它");
			}
		}
	}

	public void setMutedStatusByUid(int uid, boolean muted) {
		for (AudienceVideo audienceVideo : audienceVideos) {
			if (audienceVideo.getUid() == uid) {
//                Log.v("update_mute", "1--" + audienceVideo.toString());
				audienceVideo.setMuted(muted);
				int position = audienceVideos.indexOf(audienceVideo);
//                Log.v("update_mute", "2--" + audienceVideo.toString() + "--position--" + position);
				notifyItemChanged(position, 1);
			} else {
//                Log.v("update_mute", "不是更新它");
			}
		}
	}

	@Override
	public AudienceVideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context).inflate(R.layout.item_meeting_audience_video, parent, false);
		mAudienceVideoViewHolder = new AudienceVideoViewHolder(view);
		return mAudienceVideoViewHolder;

	}


	public void setItemSize(int height, int width) {
		this.height = height;
		this.width = width;
	}

	public AudienceVideoViewHolder getHolder() {
		return mAudienceVideoViewHolder;
	}




	@Override
	public void onBindViewHolder(@NonNull final AudienceVideoViewHolder holder, final int position) {
		if (mLayoutHelper instanceof StaggeredGridLayoutHelper) {
			if (position == 0 || position == 3) {
				ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
				layoutParams.width = DisplayUtil.getWidth(context) / 2;
				layoutParams.height = DisplayUtil.getHeight(context) / 2;
				holder.itemView.setLayoutParams(layoutParams);
			} else {
				ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
				layoutParams.width = DisplayUtil.getWidth(context) / 2;
				layoutParams.height = DisplayUtil.getHeight(context) / 3;
				holder.itemView.setLayoutParams(layoutParams);
			}
		} else {
			ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
			layoutParams.width = width;
			layoutParams.height = height;
			holder.itemView.setLayoutParams(
					new VirtualLayoutManager.LayoutParams(layoutParams));
		}

		if (position == 8) {
			ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
			layoutParams.width = 0;
			layoutParams.height = 0;
			holder.itemView.setLayoutParams(layoutParams);
		}

		final AudienceVideo audienceVideo = audienceVideos.get(position);
		if (audienceVideo.getSurfaceView() == null && !audienceVideo.isBroadcaster()) {
			holder.itemView.setVisibility(View.VISIBLE);
			holder.nameText.setText(audienceVideo.getName() + "-" + position);
			holder.nameText.setTextColor(context.getResources().getColor(R.color.black));
			holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.red));
		} else if (audienceVideo.isBroadcaster() && audienceVideo.getSurfaceView() == null) {
			holder.itemView.setVisibility(View.VISIBLE);
			holder.chairManHint.setVisibility(View.VISIBLE);
			holder.nameText.setText(audienceVideo.getName());
			holder.labelText.setVisibility(View.VISIBLE);
			holder.itemView.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
				@Override
				public void onDoubleClick(View v) {
					if (mDoubleClickListener != null) {
						mDoubleClickListener.onDoubleClick(recyclerView, v, position);
					}
				}
			}));
		} else {
			holder.chairManHint.setVisibility(View.GONE);
			holder.itemView.setVisibility(View.VISIBLE);
			holder.itemView.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
				@Override
				public void onDoubleClick(View v) {
					if (mDoubleClickListener != null) {
						mDoubleClickListener.onDoubleClick(recyclerView, v, position);
					}
				}
			}));
			holder.nameText.setText(audienceVideo.getName());
			if (BuildConfig.DEBUG) {
				holder.nameText.setVisibility(View.VISIBLE);
			} else {
				holder.nameText.setVisibility(View.GONE);
			}

			if (audienceVideo.isBroadcaster()) {
				holder.labelText.setVisibility(View.VISIBLE);
			} else {
				holder.labelText.setVisibility(View.GONE);
			}

			if (audienceVideo.isMuted() || audienceVideo.getVolume() <= 100) {
				holder.volumeImage.setVisibility(View.GONE);
			} else {
				holder.volumeImage.setVisibility(View.VISIBLE);
			}

			if (holder.videoLayout.getChildCount() > 0) {
				holder.videoLayout.removeAllViews();
			}
			final SurfaceView surfaceView = audienceVideo.getSurfaceView();
			surfaceView.setZOrderMediaOverlay(true);
			stripSurfaceView(surfaceView);
			surfaceView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			holder.videoLayout.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}


	}

	@Override
	public void onBindViewHolder(@NonNull AudienceVideoViewHolder holder, int position, @NonNull List<Object> payloads) {
		if (payloads.isEmpty()) {
			onBindViewHolder(holder, position);
		} else {
			final AudienceVideo audienceVideo = audienceVideos.get(position);

			if (audienceVideo.isMuted() || audienceVideo.getVolume() <= 100) {
				holder.volumeImage.setVisibility(View.GONE);
			} else {
				holder.volumeImage.setVisibility(View.VISIBLE);
			}

		}
	}

	private void stripSurfaceView(SurfaceView view) {
		ViewParent parent = view.getParent();
		if (parent != null) {
			((FrameLayout) parent).removeView(view);
		}
	}


	@Override
	public int getItemCount() {
		return audienceVideos != null ? audienceVideos.size() : 0;
	}

	private LayoutHelper mLayoutHelper;

	@Override
	public LayoutHelper onCreateLayoutHelper() {
		return mLayoutHelper;
	}

	public class AudienceVideoViewHolder extends RecyclerView.ViewHolder {

		private TextView labelText, nameText, chairManHint;
		private FrameLayout videoLayout;
		private ImageView volumeImage;

		public AudienceVideoViewHolder(View itemView) {
			super(itemView);
			videoLayout = itemView.findViewById(R.id.audience_video_layout);
			labelText = itemView.findViewById(R.id.label);
			chairManHint = itemView.findViewById(R.id.chairManHint);
			nameText = itemView.findViewById(R.id.name);
			volumeImage = itemView.findViewById(R.id.volume_status);

		}
	}


	/**
	 * 定义一个点击事件接口回调
	 */

	public interface onDoubleClickListener {
		void onDoubleClick(View parent, View view, int position);
	}

	public interface OnItemClickListener {
		void onItemClick(RecyclerView parent, View view, int position);
	}

	public interface OnItemLongClickListener {
		boolean onItemLongClick(RecyclerView parent, View view, int position);
	}

	private OnItemClickListener listener;//点击事件监听器
	private OnItemLongClickListener longClickListener;//长按监听器
	private onDoubleClickListener mDoubleClickListener;

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.listener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
		this.longClickListener = longClickListener;
	}

	public void setOnDoucleClickListener(onDoubleClickListener onDoucleClickListener) {
		this.mDoubleClickListener = onDoucleClickListener;
	}

	private RecyclerView recyclerView;

	//在RecyclerView提供数据的时候调用
	@Override
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
		super.onAttachedToRecyclerView(recyclerView);
		this.recyclerView = recyclerView;
	}

	@Override
	public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
		super.onDetachedFromRecyclerView(recyclerView);
		this.recyclerView = null;
	}
}

