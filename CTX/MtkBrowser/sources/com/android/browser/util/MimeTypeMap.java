package com.android.browser.util;

import android.text.TextUtils;
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
        }
        return "";
    }

    public static MimeTypeMap getSingleton() {
        return sMimeTypeMap;
    }

    public String getMimeTypeFromExtension(String str) {
        return MimeUtils.guessMimeTypeFromExtension(str);
    }

    String remapGenericMimeType(String str, String str2, String str3) {
        if (!"text/plain".equals(str) && !"application/octet-stream".equals(str)) {
            return "text/vnd.wap.wml".equals(str) ? "text/plain" : "application/vnd.wap.xhtml+xml".equals(str) ? "application/xhtml+xml" : str;
        }
        String contentDisposition = str3 != null ? URLUtil.parseContentDisposition(str3) : null;
        if (contentDisposition != null) {
            str2 = contentDisposition;
        }
        String mimeTypeFromExtension = getMimeTypeFromExtension(getFileExtensionFromUrl(str2));
        return mimeTypeFromExtension != null ? mimeTypeFromExtension : str;
    }

    public String remapGenericMimeTypePublic(String str, String str2, String str3) {
        return remapGenericMimeType(str, str2, str3);
    }
}
