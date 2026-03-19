package com.android.volley;

import android.text.TextUtils;

public final class Header {
    private final String mName;
    private final String mValue;

    public Header(String str, String str2) {
        this.mName = str;
        this.mValue = str2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Header header = (Header) obj;
        if (TextUtils.equals(this.mName, header.mName) && TextUtils.equals(this.mValue, header.mValue)) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * this.mName.hashCode()) + this.mValue.hashCode();
    }

    public String toString() {
        return "Header[name=" + this.mName + ",value=" + this.mValue + "]";
    }
}
