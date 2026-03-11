package com.android.browser;

import java.net.MalformedURLException;
import libcore.io.Base64;

public class DataUri {
    private byte[] mData;
    private String mMimeType;

    public DataUri(String uri) throws MalformedURLException {
        if (!isDataUri(uri)) {
            throw new MalformedURLException("Not a data URI");
        }
        int commaIndex = uri.indexOf(44, "data:".length());
        if (commaIndex < 0) {
            throw new MalformedURLException("Comma expected in data URI");
        }
        String contentType = uri.substring("data:".length(), commaIndex);
        this.mData = uri.substring(commaIndex + 1).getBytes();
        if (contentType.contains(";base64")) {
            this.mData = Base64.decode(this.mData);
        }
        int semiIndex = contentType.indexOf(59);
        if (semiIndex > 0) {
            this.mMimeType = contentType.substring(0, semiIndex);
        } else {
            this.mMimeType = contentType;
        }
    }

    public static boolean isDataUri(String text) {
        return text.startsWith("data:");
    }

    public String getMimeType() {
        return this.mMimeType;
    }

    public byte[] getData() {
        return this.mData;
    }
}
