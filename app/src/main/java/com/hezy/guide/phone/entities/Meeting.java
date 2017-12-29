package com.hezy.guide.phone.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Meeting implements Parcelable, Entity {

    private String id;

    private String title;

    private String description;

    private String startTime;

    private int totalParticipants;

    private int totalAttendance;

    private int screenshotFrequency;

    private int meetingProcess;

    private int approved;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public void setTotalParticipants(int totalParticipants) {
        this.totalParticipants = totalParticipants;
    }

    public int getTotalAttendance() {
        return totalAttendance;
    }

    public void setTotalAttendance(int totalAttendance) {
        this.totalAttendance = totalAttendance;
    }

    public int getScreenshotFrequency() {
        return screenshotFrequency;
    }

    public void setScreenshotFrequency(int screenshotFrequency) {
        this.screenshotFrequency = screenshotFrequency;
    }

    public int getMeetingProcess() {
        return meetingProcess;
    }

    public void setMeetingProcess(int meetingProcess) {
        this.meetingProcess = meetingProcess;
    }

    public int getApproved() {
        return approved;
    }

    public void setApproved(int approved) {
        this.approved = approved;
    }

    @Override
    public String toString() {
        return "Meeting{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startTime='" + startTime + '\'' +
                ", totalParticipants=" + totalParticipants +
                ", totalAttendance=" + totalAttendance +
                ", screenshotFrequency=" + screenshotFrequency +
                ", meetingProcess=" + meetingProcess +
                ", approved=" + approved +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Meeting meeting = (Meeting) o;

        if (totalParticipants != meeting.totalParticipants) return false;
        if (totalAttendance != meeting.totalAttendance) return false;
        if (screenshotFrequency != meeting.screenshotFrequency) return false;
        if (meetingProcess != meeting.meetingProcess) return false;
        if (approved != meeting.approved) return false;
        if (id != null ? !id.equals(meeting.id) : meeting.id != null) return false;
        if (title != null ? !title.equals(meeting.title) : meeting.title != null) return false;
        if (description != null ? !description.equals(meeting.description) : meeting.description != null)
            return false;
        return startTime != null ? startTime.equals(meeting.startTime) : meeting.startTime == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        result = 31 * result + totalParticipants;
        result = 31 * result + totalAttendance;
        result = 31 * result + screenshotFrequency;
        result = 31 * result + meetingProcess;
        result = 31 * result + approved;
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.startTime);
        dest.writeInt(this.totalParticipants);
        dest.writeInt(this.totalAttendance);
        dest.writeInt(this.screenshotFrequency);
        dest.writeInt(this.meetingProcess);
        dest.writeInt(this.approved);
    }

    public Meeting() {
    }

    protected Meeting(Parcel in) {
        this.id = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.startTime = in.readString();
        this.totalParticipants = in.readInt();
        this.totalAttendance = in.readInt();
        this.screenshotFrequency = in.readInt();
        this.meetingProcess = in.readInt();
        this.approved = in.readInt();
    }

    public static final Creator<Meeting> CREATOR = new Creator<Meeting>() {
        @Override
        public Meeting createFromParcel(Parcel source) {
            return new Meeting(source);
        }

        @Override
        public Meeting[] newArray(int size) {
            return new Meeting[size];
        }
    };
}
