package com.hezy.guide.phone.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Created by yuna on 2017/7/27.
 */

public class Audience implements Entity, Parcelable {

    private int uid;

    private String uname;

    private boolean handsUp;

    private boolean calling;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }

    public boolean isHandsUp() {
        return handsUp;
    }

    public void setHandsUp(boolean handsUp) {
        this.handsUp = handsUp;
    }

    public boolean isCalling() {
        return calling;
    }

    public void setCalling(boolean calling) {
        this.calling = calling;
    }
    //    @Override
//    public String toString() {
//        return "Audience{" +
//                "uid='" + uid + '\'' +
//                ", uname='" + uname + '\'' +
//                '}';
//    }

    @Override
    public String toString() {
        return uname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Audience audience = (Audience) o;
        return uid == audience.uid &&
                Objects.equals(uname, audience.uname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid, uname);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.uid);
        dest.writeString(this.uname);
        dest.writeByte(this.handsUp ? (byte) 1 : (byte) 0);
        dest.writeByte(this.calling ? (byte) 1 : (byte) 0);
    }

    public Audience() {
    }

    protected Audience(Parcel in) {
        this.uid = in.readInt();
        this.uname = in.readString();
        this.handsUp = in.readByte() != 0;
        this.calling = in.readByte() != 0;
    }

    public static final Creator<Audience> CREATOR = new Creator<Audience>() {
        @Override
        public Audience createFromParcel(Parcel source) {
            return new Audience(source);
        }

        @Override
        public Audience[] newArray(int size) {
            return new Audience[size];
        }
    };
}
