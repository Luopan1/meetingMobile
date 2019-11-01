package com.zhongyou.meet.mobile.entities;

import android.graphics.Rect;
import android.os.Parcel;
import android.support.annotation.Nullable;

import com.previewlibrary.enitity.IThumbViewInfo;

/**
 * @author golangdorid@gmail.com
 * @date 2019-10-30 18:50.
 */
public class PreViewImages implements IThumbViewInfo {
	private String url;
	private Rect mBounds;
	private String user;
	private String videoUrl;

	public PreViewImages(String url) {
		this.url = url;
	}

	@Override
	public String getUrl() {//将你的图片地址字段返回
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	@Override
	public Rect getBounds() {//将你的图片显示坐标字段返回
		return mBounds;
	}

	@Nullable
	@Override
	public String getVideoUrl() {
		return null;
	}

	public void setBounds(Rect bounds) {
		mBounds = bounds;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.url);
		dest.writeParcelable(this.mBounds, flags);
		dest.writeString(this.user);
		dest.writeString(this.videoUrl);
	}

	protected PreViewImages(Parcel in) {
		this.url = in.readString();
		this.mBounds = in.readParcelable(Rect.class.getClassLoader());
		this.user = in.readString();
		this.videoUrl = in.readString();
	}

	public static final Creator<PreViewImages> CREATOR = new Creator<PreViewImages>() {
		@Override
		public PreViewImages createFromParcel(Parcel source) {
			return new PreViewImages(source);
		}

		@Override
		public PreViewImages[] newArray(int size) {
			return new PreViewImages[size];
		}
	};
}
