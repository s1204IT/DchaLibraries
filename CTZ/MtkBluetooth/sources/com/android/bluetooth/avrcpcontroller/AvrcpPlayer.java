package com.android.bluetooth.avrcpcontroller;

import android.media.session.PlaybackState;
import android.util.Log;

class AvrcpPlayer {
    private static final boolean DBG = true;
    public static final int INVALID_ID = -1;
    private static final String TAG = "AvrcpPlayer";
    private TrackInfo mCurrentTrack;
    private int mId;
    private String mName;
    private int mPlayStatus;
    private long mPlayTime;
    private int mPlayerType;

    AvrcpPlayer() {
        this.mPlayStatus = 0;
        this.mPlayTime = -1L;
        this.mName = "";
        this.mCurrentTrack = new TrackInfo();
        this.mId = -1;
    }

    AvrcpPlayer(int i, String str, int i2, int i3, int i4) {
        this.mPlayStatus = 0;
        this.mPlayTime = -1L;
        this.mName = "";
        this.mCurrentTrack = new TrackInfo();
        this.mId = i;
        this.mName = str;
        this.mPlayerType = i4;
    }

    public int getId() {
        return this.mId;
    }

    public String getName() {
        return this.mName;
    }

    public void setPlayTime(int i) {
        this.mPlayTime = i;
    }

    public long getPlayTime() {
        return this.mPlayTime;
    }

    public void setPlayStatus(int i) {
        this.mPlayStatus = i;
    }

    public PlaybackState getPlaybackState() {
        Log.d(TAG, "getPlayBackState state " + this.mPlayStatus + " time " + this.mPlayTime);
        long j = this.mPlayTime;
        float f = 0.0f;
        switch (this.mPlayStatus) {
            case 1:
                j = 0;
                break;
            case 2:
                break;
            case 3:
            default:
                f = 1.0f;
                break;
            case 4:
                f = 3.0f;
                break;
            case 5:
                f = -3.0f;
                break;
        }
        return new PlaybackState.Builder().setState(this.mPlayStatus, j, f).build();
    }

    public synchronized void updateCurrentTrack(TrackInfo trackInfo) {
        this.mCurrentTrack = trackInfo;
    }

    public synchronized TrackInfo getCurrentTrack() {
        return this.mCurrentTrack;
    }
}
