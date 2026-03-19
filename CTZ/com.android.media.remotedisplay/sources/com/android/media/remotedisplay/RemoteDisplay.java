package com.android.media.remotedisplay;

import android.media.RemoteDisplayState;
import android.text.TextUtils;
import java.util.Objects;

public class RemoteDisplay {
    public static final int PLAYBACK_VOLUME_FIXED = 0;
    public static final int PLAYBACK_VOLUME_VARIABLE = 1;
    public static final int STATUS_AVAILABLE = 2;
    public static final int STATUS_CONNECTED = 4;
    public static final int STATUS_CONNECTING = 3;
    public static final int STATUS_IN_USE = 1;
    public static final int STATUS_NOT_AVAILABLE = 0;
    private RemoteDisplayState.RemoteDisplayInfo mImmutableInfo;
    private final RemoteDisplayState.RemoteDisplayInfo mMutableInfo;

    public RemoteDisplay(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            throw new IllegalArgumentException("id must not be null or empty");
        }
        this.mMutableInfo = new RemoteDisplayState.RemoteDisplayInfo(str);
        setName(str2);
    }

    public String getId() {
        return this.mMutableInfo.id;
    }

    public String getName() {
        return this.mMutableInfo.name;
    }

    public void setName(String str) {
        if (!Objects.equals(this.mMutableInfo.name, str)) {
            this.mMutableInfo.name = str;
            this.mImmutableInfo = null;
        }
    }

    public String getDescription() {
        return this.mMutableInfo.description;
    }

    public void setDescription(String str) {
        if (!Objects.equals(this.mMutableInfo.description, str)) {
            this.mMutableInfo.description = str;
            this.mImmutableInfo = null;
        }
    }

    public int getStatus() {
        return this.mMutableInfo.status;
    }

    public void setStatus(int i) {
        if (this.mMutableInfo.status != i) {
            this.mMutableInfo.status = i;
            this.mImmutableInfo = null;
        }
    }

    public int getVolume() {
        return this.mMutableInfo.volume;
    }

    public void setVolume(int i) {
        if (this.mMutableInfo.volume != i) {
            this.mMutableInfo.volume = i;
            this.mImmutableInfo = null;
        }
    }

    public int getVolumeMax() {
        return this.mMutableInfo.volumeMax;
    }

    public void setVolumeMax(int i) {
        if (this.mMutableInfo.volumeMax != i) {
            this.mMutableInfo.volumeMax = i;
            this.mImmutableInfo = null;
        }
    }

    public int getVolumeHandling() {
        return this.mMutableInfo.volumeHandling;
    }

    public void setVolumeHandling(int i) {
        if (this.mMutableInfo.volumeHandling != i) {
            this.mMutableInfo.volumeHandling = i;
            this.mImmutableInfo = null;
        }
    }

    public int getPresentationDisplayId() {
        return this.mMutableInfo.presentationDisplayId;
    }

    public void setPresentationDisplayId(int i) {
        if (this.mMutableInfo.presentationDisplayId != i) {
            this.mMutableInfo.presentationDisplayId = i;
            this.mImmutableInfo = null;
        }
    }

    public String toString() {
        return "RemoteDisplay{" + this.mMutableInfo.toString() + "}";
    }

    RemoteDisplayState.RemoteDisplayInfo getInfo() {
        if (this.mImmutableInfo == null) {
            this.mImmutableInfo = new RemoteDisplayState.RemoteDisplayInfo(this.mMutableInfo);
        }
        return this.mImmutableInfo;
    }
}
