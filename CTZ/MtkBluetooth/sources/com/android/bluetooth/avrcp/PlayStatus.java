package com.android.bluetooth.avrcp;

import android.media.session.PlaybackState;

class PlayStatus {
    static final byte ERROR = -1;
    static final byte FWD_SEEK = 3;
    static final byte PAUSED = 2;
    static final byte PLAYING = 1;
    static final byte REV_SEEK = 4;
    static final byte STOPPED = 0;
    public long position = -1;
    public long duration = 0;
    public byte state = 0;

    PlayStatus() {
    }

    static PlayStatus fromPlaybackState(PlaybackState playbackState, long j) {
        PlayStatus playStatus = new PlayStatus();
        if (playbackState == null) {
            return playStatus;
        }
        playStatus.state = playbackStateToAvrcpState(playbackState.getState());
        playStatus.position = playbackState.getPosition();
        playStatus.duration = j;
        return playStatus;
    }

    static byte playbackStateToAvrcpState(int i) {
        switch (i) {
            case 0:
            case 1:
            case 8:
                return (byte) 0;
            case 2:
                return (byte) 2;
            case 3:
            case 6:
                return (byte) 1;
            case 4:
            case 10:
            case 11:
                return FWD_SEEK;
            case 5:
            case 9:
                return (byte) 4;
            case 7:
            default:
                return ERROR;
        }
    }

    public String toString() {
        return "{ state=" + ((int) this.state) + " position=" + this.position + " duration=" + this.duration + " }";
    }
}
