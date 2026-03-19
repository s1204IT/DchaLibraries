package com.android.server.wifi;

import java.io.IOException;

public class WnmData {
    private static final int ESS = 1;
    private final long mBssid;
    private final boolean mDeauthEvent;
    private final int mDelay;
    private final boolean mEss;
    private final int mMethod;
    private final String mUrl;

    public static WnmData buildWnmData(String event) throws IOException {
        String[] segments = event.split(" ");
        if (segments.length < 2) {
            throw new IOException("Short event");
        }
        String str = segments[1];
        if (!str.equals(WifiMonitor.HS20_SUB_REM_STR)) {
            if (str.equals(WifiMonitor.HS20_DEAUTH_STR)) {
                if (segments.length != 5) {
                    throw new IOException("Expected 5 segments");
                }
                int codeID = Integer.parseInt(segments[2]);
                if (codeID < 0 || codeID > 1) {
                    throw new IOException("Unknown code");
                }
                return new WnmData(Long.parseLong(segments[0], 16), segments[4], codeID == 1, Integer.parseInt(segments[3]));
            }
            throw new IOException("Unknown event type");
        }
        if (segments.length != 4) {
            throw new IOException("Expected 4 segments");
        }
        return new WnmData(Long.parseLong(segments[0], 16), segments[3], Integer.parseInt(segments[2]));
    }

    private WnmData(long bssid, String url, int method) {
        this.mBssid = bssid;
        this.mUrl = url;
        this.mMethod = method;
        this.mEss = false;
        this.mDelay = -1;
        this.mDeauthEvent = false;
    }

    private WnmData(long bssid, String url, boolean ess, int delay) {
        this.mBssid = bssid;
        this.mUrl = url;
        this.mEss = ess;
        this.mDelay = delay;
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
