package com.hezy.guide.phone.entities;

import android.view.SurfaceView;

public class AudienceVideo {

    private String name;

    private boolean broadcaster;

    private SurfaceView surfaceView;

    private boolean muted;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isBroadcaster() {
        return broadcaster;
    }

    public void setBroadcaster(boolean broadcaster) {
        this.broadcaster = broadcaster;
    }

    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AudienceVideo that = (AudienceVideo) o;

        if (broadcaster != that.broadcaster) return false;
        if (muted != that.muted) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (broadcaster ? 1 : 0);
        result = 31 * result + (muted ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AudienceVideo{" +
                "name='" + name + '\'' +
                ", broadcaster=" + broadcaster +
                ", surfaceView=" + surfaceView +
                ", muted=" + muted +
                '}';
    }
}
