package com.hezy.guide.phone.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Agora implements Parcelable, Entity {

    private String isTest;

    private String channelKey;

    private String appID;

    private String recordingKey;

    private String signalingKey;

    public String getIsTest() {
        return isTest;
    }

    public void setIsTest(String isTest) {
        this.isTest = isTest;
    }

    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public String getAppID() {
        return appID;
    }

    public void setAppID(String appID) {
        this.appID = appID;
    }

    public String getRecordingKey() {
        return recordingKey;
    }

    public void setRecordingKey(String recordingKey) {
        this.recordingKey = recordingKey;
    }

    public String getSignalingKey() {
        return signalingKey;
    }

    public void setSignalingKey(String signalingKey) {
        this.signalingKey = signalingKey;
    }

    @Override
    public String toString() {
        return "Agora{" +
                "isTest='" + isTest + '\'' +
                ", channelKey='" + channelKey + '\'' +
                ", appID='" + appID + '\'' +
                ", recordingKey='" + recordingKey + '\'' +
                ", signalingKey='" + signalingKey + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Agora agora = (Agora) o;

        if (isTest != null ? !isTest.equals(agora.isTest) : agora.isTest != null) return false;
        if (channelKey != null ? !channelKey.equals(agora.channelKey) : agora.channelKey != null)
            return false;
        if (appID != null ? !appID.equals(agora.appID) : agora.appID != null) return false;
        if (recordingKey != null ? !recordingKey.equals(agora.recordingKey) : agora.recordingKey != null)
            return false;
        return signalingKey != null ? signalingKey.equals(agora.signalingKey) : agora.signalingKey == null;
    }

    @Override
    public int hashCode() {
        int result = isTest != null ? isTest.hashCode() : 0;
        result = 31 * result + (channelKey != null ? channelKey.hashCode() : 0);
        result = 31 * result + (appID != null ? appID.hashCode() : 0);
        result = 31 * result + (recordingKey != null ? recordingKey.hashCode() : 0);
        result = 31 * result + (signalingKey != null ? signalingKey.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.isTest);
        dest.writeString(this.channelKey);
        dest.writeString(this.appID);
        dest.writeString(this.recordingKey);
        dest.writeString(this.signalingKey);
    }

    public Agora() {
    }

    protected Agora(Parcel in) {
        this.isTest = in.readString();
        this.channelKey = in.readString();
        this.appID = in.readString();
        this.recordingKey = in.readString();
        this.signalingKey = in.readString();
    }

    public static final Creator<Agora> CREATOR = new Creator<Agora>() {
        @Override
        public Agora createFromParcel(Parcel source) {
            return new Agora(source);
        }

        @Override
        public Agora[] newArray(int size) {
            return new Agora[size];
        }
    };
}
