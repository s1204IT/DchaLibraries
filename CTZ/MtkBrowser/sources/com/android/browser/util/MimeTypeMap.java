package com.android.browser.util;

import android.text.TextUtils;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;
import libcore.net.MimeUtils;

public class MimeTypeMap {
    private static final MimeTypeMap sMimeTypeMap = new MimeTypeMap();

    private MimeTypeMap() {
    }

    public static String getFileExtensionFromUrl(String str) {
        int iLastIndexOf;
        if (!TextUtils.isEmpty(str)) {
            int iLastIndexOf2 = str.lastIndexOf(35);
            if (iLastIndexOf2 > 0) {
                str = str.substring(0, iLastIndexOf2);
            }
            int iLastIndexOf3 = str.lastIndexOf(63);
            if (iLastIndexOf3 > 0) {
                str = str.substring(0, iLastIndexOf3);
            }
            int iLastIndexOf4 = str.lastIndexOf(47);
            if (iLastIndexOf4 >= 0) {
                str = str.substring(iLastIndexOf4 + 1);
            }
            if (!str.isEmpty() && Pattern.matches("[a-zA-Z_0-9\\.\\-\\(\\)\\%]+", str) && (iLastIndexOf = str.lastIndexOf(46)) >= 0) {
                return str.substring(iLastIndexOf + 1);
            }
            return "";
        }
        return "";
    }

    public String getMimeTypeFromExtension(String str) {
        return MimeUtils.guessMimeTypeFromExtension(str);
    }

    public String remapGenericMimeTypePublic(String str, String str2, String str3) {
        return remapGenericMimeType(str, str2, str3);
    }

    String remapGenericMimeType(String str, String str2, String str3) throws UnsupportedEncodingException {
        String contentDisposition;
        if ("text/plain".equals(str) || "application/octet-stream".equals(str)) {
            if (str3 != null) {
                contentDisposition = URLUtil.parseContentDisposition(str3);
            } else {
                contentDisposition = null;
            }
            if (contentDisposition != null) {
                str2 = contentDisposition;
            }
            String mimeTypeFromExtension = getMimeTypeFromExtension(getFileExtensionFromUrl(str2));
            if (mimeTypeFromExtension != null) {
                return mimeTypeFromExtension;
            }
            return str;
        }
        if ("text/vnd.wap.wml".equals(str)) {
            return "text/plain";
        }
        if ("application/vnd.wap.xhtml+xml".equals(str)) {
            return "application/xhtml+xml";
        }
        return str;
    }

    public static MimeTypeMap getSingleton() {
        return sMimeTypeMap;
    }
}
