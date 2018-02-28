package com.hezy.guide.phone.entities;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by whatisjava on 18-2-27.
 */

public class MeetingJoinStats implements Parcelable {

    private String id;
    private String meetingId;
    private String userId;
    private String deviceId;
    private Date joinTime;
    private Date leaveTime;
    private int joinType;

    @Override
    public String toString() {
        return "MeetingJoinStats{" +
                "id='" + id + '\'' +
                ", meetingId='" + meetingId + '\'' +
                ", userId='" + userId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", joinTime=" + joinTime +
                ", leaveTime=" + leaveTime +
                ", joinType=" + joinType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MeetingJoinStats that = (MeetingJoinStats) o;

        if (joinType != that.joinType) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (meetingId != null ? !meetingId.equals(that.meetingId) : that.meetingId != null)
            return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (deviceId != null ? !deviceId.equals(that.deviceId) : that.deviceId != null)
            return false;
        if (joinTime != null ? !joinTime.equals(that.joinTime) : that.joinTime != null)
            return false;
        return leaveTime != null ? leaveTime.equals(that.leaveTime) : that.leaveTime == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (meetingId != null ? meetingId.hashCode() : 0);
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (deviceId != null ? deviceId.hashCode() : 0);
        result = 31 * result + (joinTime != null ? joinTime.hashCode() : 0);
        result = 31 * result + (leaveTime != null ? leaveTime.hashCode() : 0);
        result = 31 * result + joinType;
        return result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Date getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(Date joinTime) {
        this.joinTime = joinTime;
    }

    public Date getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(Date leaveTime) {
        this.leaveTime = leaveTime;
    }

    public int getJoinType() {
        return joinType;
    }

    public void setJoinType(int joinType) {
        this.joinType = joinType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.meetingId);
        dest.writeString(this.userId);
        dest.writeString(this.deviceId);
        dest.writeLong(this.joinTime != null ? this.joinTime.getTime() : -1);
        dest.writeLong(this.leaveTime != null ? this.leaveTime.getTime() : -1);
        dest.writeInt(this.joinType);
    }

    public MeetingJoinStats() {
    }

    protected MeetingJoinStats(Parcel in) {
        this.id = in.readString();
        this.meetingId = in.readString();
        this.userId = in.readString();
        this.deviceId = in.readString();
        long tmpJoinTime = in.readLong();
        this.joinTime = tmpJoinTime == -1 ? null : new Date(tmpJoinTime);
        long tmpLeaveTime = in.readLong();
        this.leaveTime = tmpLeaveTime == -1 ? null : new Date(tmpLeaveTime);
        this.joinType = in.readInt();
    }

    public static final Creator<MeetingJoinStats> CREATOR = new Creator<MeetingJoinStats>() {
        @Override
        public MeetingJoinStats createFromParcel(Parcel source) {
            return new MeetingJoinStats(source);
        }

        @Override
        public MeetingJoinStats[] newArray(int size) {
            return new MeetingJoinStats[size];
        }
    };
}
