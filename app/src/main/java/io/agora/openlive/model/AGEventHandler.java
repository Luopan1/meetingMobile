package io.agora.openlive.model;

public interface AGEventHandler {
    void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed);

    void onJoinChannelSuccess(String channel, int uid, int elapsed);

    void onUserOffline(int uid, int reason);

    void onConnectionLost();

    void onConnectionInterrupted();

    void onUserMuteVideo(int uid, boolean muted);

    void onLastmileQuality(int quality);

    void onNetworkQuality(int uid, int txQuality, int rxQuality);

    void onWarning(int warn);

    void onError(int err);
}
