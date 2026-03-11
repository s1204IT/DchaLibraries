package com.android.browser;

import java.net.MalformedURLException;
import java.util.Base64;

public class DataUri {
    private byte[] mData;
    private String mMimeType;

    public DataUri(String str) throws MalformedURLException {
        if (!isDataUri(str)) {
            throw new MalformedURLException("Not a data URI");
        }
        int iIndexOf = str.indexOf(44, "data:".length());
        if (iIndexOf < 0) {
            throw new MalformedURLException("Comma expected in data URI");
        }
        String strSubstring = str.substring("data:".length(), iIndexOf);
        this.mData = str.substring(iIndexOf + 1).getBytes();
        if (strSubstring.contains(";base64")) {
            this.mData = Base64.getDecoder().decode(this.mData);
        }
        int iIndexOf2 = strSubstring.indexOf(59);
        if (iIndexOf2 > 0) {
            this.mMimeType = strSubstring.substring(0, iIndexOf2);
        } else {
            this.mMimeType = strSubstring;
        }
    }

    public static boolean isDataUri(String str) {
        return str.startsWith("data:");
    }

    public byte[] getData() {
        return this.mData;
    }

    public String getMimeType() {
        return this.mMimeType;
    }
}
