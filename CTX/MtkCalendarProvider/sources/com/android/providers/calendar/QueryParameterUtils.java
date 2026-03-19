package com.android.providers.calendar;

import android.net.Uri;

public class QueryParameterUtils {
    public static boolean readBooleanQueryParameter(Uri uri, String str, boolean z) {
        String queryParameter = getQueryParameter(uri, str);
        if (queryParameter == null) {
            return z;
        }
        return ("false".equals(queryParameter.toLowerCase()) || "0".equals(queryParameter.toLowerCase())) ? false : true;
    }

    public static String getQueryParameter(Uri uri, String str) {
        String strSubstring;
        String encodedQuery = uri.getEncodedQuery();
        if (encodedQuery == null) {
            return null;
        }
        int length = encodedQuery.length();
        int length2 = str.length();
        int i = 0;
        do {
            int iIndexOf = encodedQuery.indexOf(str, i);
            if (iIndexOf == -1 || length == (i = iIndexOf + length2)) {
                return null;
            }
        } while (encodedQuery.charAt(i) != '=');
        int i2 = i + 1;
        int iIndexOf2 = encodedQuery.indexOf(38, i2);
        if (iIndexOf2 == -1) {
            strSubstring = encodedQuery.substring(i2);
        } else {
            strSubstring = encodedQuery.substring(i2, iIndexOf2);
        }
        return Uri.decode(strSubstring);
    }
}
