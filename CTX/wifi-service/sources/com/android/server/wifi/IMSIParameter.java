package com.android.server.wifi;

import android.text.TextUtils;

public class IMSIParameter {
    private static final int MAX_IMSI_LENGTH = 15;
    private static final int MCC_MNC_LENGTH = 6;
    private final String mImsi;
    private final boolean mPrefix;

    public IMSIParameter(String str, boolean z) {
        this.mImsi = str;
        this.mPrefix = z;
    }

    public static IMSIParameter build(String str) {
        if (TextUtils.isEmpty(str) || str.length() > 15) {
            return null;
        }
        int i = 0;
        char cCharAt = 0;
        while (i < str.length() && (cCharAt = str.charAt(i)) >= '0' && cCharAt <= '9') {
            i++;
        }
        if (i == str.length()) {
            return new IMSIParameter(str, false);
        }
        if (i == str.length() - 1 && cCharAt == '*') {
            return new IMSIParameter(str.substring(0, i), true);
        }
        return null;
    }

    public boolean matchesImsi(String str) {
        if (str == null) {
            return false;
        }
        if (this.mPrefix) {
            return this.mImsi.regionMatches(false, 0, str, 0, this.mImsi.length());
        }
        return this.mImsi.equals(str);
    }

    public boolean matchesMccMnc(String str) {
        if (str == null) {
            return false;
        }
        int length = 6;
        if (str.length() != 6) {
            return false;
        }
        if (this.mPrefix && this.mImsi.length() < 6) {
            length = this.mImsi.length();
        }
        return this.mImsi.regionMatches(false, 0, str, 0, length);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IMSIParameter)) {
            return false;
        }
        IMSIParameter iMSIParameter = (IMSIParameter) obj;
        return this.mPrefix == iMSIParameter.mPrefix && TextUtils.equals(this.mImsi, iMSIParameter.mImsi);
    }

    public int hashCode() {
        return (31 * (this.mImsi != null ? this.mImsi.hashCode() : 0)) + (this.mPrefix ? 1 : 0);
    }

    public String toString() {
        if (this.mPrefix) {
            return this.mImsi + '*';
        }
        return this.mImsi;
    }
}
