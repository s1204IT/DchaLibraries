package com.android.se.security;

public class ChannelAccess {
    private final String mTag = "SecureElement-ChannelAccess";
    private String mPackageName = "";
    private ACCESS mAccess = ACCESS.UNDEFINED;
    private ACCESS mApduAccess = ACCESS.UNDEFINED;
    private boolean mUseApduFilter = false;
    private int mCallingPid = 0;
    private String mReason = "no access by default";
    private ACCESS mNFCEventAccess = ACCESS.UNDEFINED;
    private ApduFilter[] mApduFilter = null;

    public enum ACCESS {
        ALLOWED,
        DENIED,
        UNDEFINED
    }

    public ChannelAccess m1clone() {
        ChannelAccess channelAccess = new ChannelAccess();
        channelAccess.setAccess(this.mAccess, this.mReason);
        channelAccess.setPackageName(this.mPackageName);
        channelAccess.setApduAccess(this.mApduAccess);
        channelAccess.setCallingPid(this.mCallingPid);
        channelAccess.setNFCEventAccess(this.mNFCEventAccess);
        channelAccess.setUseApduFilter(this.mUseApduFilter);
        if (this.mApduFilter != null) {
            ApduFilter[] apduFilterArr = new ApduFilter[this.mApduFilter.length];
            ApduFilter[] apduFilterArr2 = this.mApduFilter;
            int length = apduFilterArr2.length;
            int i = 0;
            int i2 = 0;
            while (i < length) {
                apduFilterArr[i2] = apduFilterArr2[i].m0clone();
                i++;
                i2++;
            }
            channelAccess.setApduFilter(apduFilterArr);
        } else {
            channelAccess.setApduFilter(null);
        }
        return channelAccess;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }

    public ACCESS getApduAccess() {
        return this.mApduAccess;
    }

    public void setApduAccess(ACCESS access) {
        this.mApduAccess = access;
    }

    public ACCESS getAccess() {
        return this.mAccess;
    }

    public void setAccess(ACCESS access, String str) {
        this.mAccess = access;
        this.mReason = str;
    }

    public boolean isUseApduFilter() {
        return this.mUseApduFilter;
    }

    public void setUseApduFilter(boolean z) {
        this.mUseApduFilter = z;
    }

    public int getCallingPid() {
        return this.mCallingPid;
    }

    public void setCallingPid(int i) {
        this.mCallingPid = i;
    }

    public String getReason() {
        return this.mReason;
    }

    public ApduFilter[] getApduFilter() {
        return this.mApduFilter;
    }

    public void setApduFilter(ApduFilter[] apduFilterArr) {
        this.mApduFilter = apduFilterArr;
    }

    public ACCESS getNFCEventAccess() {
        return this.mNFCEventAccess;
    }

    public void setNFCEventAccess(ACCESS access) {
        this.mNFCEventAccess = access;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append("\n [mPackageName=");
        sb.append(this.mPackageName);
        sb.append(", mAccess=");
        sb.append(this.mAccess);
        sb.append(", mApduAccess=");
        sb.append(this.mApduAccess);
        sb.append(", mUseApduFilter=");
        sb.append(this.mUseApduFilter);
        sb.append(", mApduFilter=");
        if (this.mApduFilter != null) {
            for (ApduFilter apduFilter : this.mApduFilter) {
                sb.append(apduFilter.toString());
                sb.append(" ");
            }
        } else {
            sb.append("null");
        }
        sb.append(", mCallingPid=");
        sb.append(this.mCallingPid);
        sb.append(", mReason=");
        sb.append(this.mReason);
        sb.append(", mNFCEventAllowed=");
        sb.append(this.mNFCEventAccess);
        sb.append("]\n");
        return sb.toString();
    }
}
