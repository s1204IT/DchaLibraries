package com.android.server.wifi.hotspot2;

public class WnmData {
    public static final int ESS = 1;
    private final long mBssid;
    private final boolean mDeauthEvent;
    private final int mDelay;
    private final boolean mEss;
    private final int mMethod;
    private final String mUrl;

    public WnmData(long j, String str, int i) {
        this.mBssid = j;
        this.mUrl = str;
        this.mMethod = i;
        this.mEss = false;
        this.mDelay = -1;
        this.mDeauthEvent = false;
    }

    public WnmData(long j, String str, boolean z, int i) {
        this.mBssid = j;
        this.mUrl = str;
        this.mEss = z;
        this.mDelay = i;
        this.mMethod = -1;
        this.mDeauthEvent = true;
    }

    public long getBssid() {
        return this.mBssid;
    }

    public String getUrl() {
        return this.mUrl;
    }

    public boolean isDeauthEvent() {
        return this.mDeauthEvent;
    }

    public int getMethod() {
        return this.mMethod;
    }

    public boolean isEss() {
        return this.mEss;
    }

    public int getDelay() {
        return this.mDelay;
    }
}
