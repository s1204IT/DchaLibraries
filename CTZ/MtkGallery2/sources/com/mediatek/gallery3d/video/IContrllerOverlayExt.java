package com.mediatek.gallery3d.video;

public interface IContrllerOverlayExt {
    void clearBuffering();

    boolean isPlayingEnd();

    void onCancelHiding();

    void setBottomPanel(boolean z, boolean z2);

    void setCanPause(boolean z);

    void setCanScrubbing(boolean z);

    void setLogoPic(byte[] bArr);

    void setPlayingInfo(boolean z);

    void showBuffering(boolean z, int i);

    void showReconnecting(int i);

    void showReconnectingError();
}
