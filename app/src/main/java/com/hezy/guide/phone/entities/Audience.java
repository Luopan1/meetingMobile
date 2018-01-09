package com.hezy.guide.phone.entities;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by yuna on 2017/7/27.
 */

public class Audience implements Entity, Parcelable {

    private String uid;

    private String uname;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
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

        if (uid != null ? !uid.equals(audience.uid) : audience.uid != null) return false;
        return uname != null ? uname.equals(audience.uname) : audience.uname == null;
    }

    @Override
    public int hashCode() {
        int result = uid != null ? uid.hashCode() : 0;
        result = 31 * result + (uname != null ? uname.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uid);
        dest.writeString(this.uname);
    }

    public Audience() {
    }

    protected Audience(Parcel in) {
        this.uid = in.readString();
        this.uname = in.readString();
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
