package com.android.providers.calendar;

import android.net.Uri;

public class QueryParameterUtils {
    public static boolean readBooleanQueryParameter(Uri uri, String name, boolean defaultValue) {
        String flag = getQueryParameter(uri, name);
        return flag == null ? defaultValue : ("false".equals(flag.toLowerCase()) || "0".equals(flag.toLowerCase())) ? false : true;
    }

    public static String getQueryParameter(Uri uri, String parameter) {
        String value;
        String query = uri.getEncodedQuery();
        if (query == null) {
            return null;
        }
        int queryLength = query.length();
        int parameterLength = parameter.length();
        int index = 0;
        do {
            int index2 = query.indexOf(parameter, index);
            if (index2 == -1 || queryLength == (index = index2 + parameterLength)) {
                return null;
            }
        } while (query.charAt(index) != '=');
        int index3 = index + 1;
        int ampIndex = query.indexOf(38, index3);
        if (ampIndex == -1) {
            value = query.substring(index3);
        } else {
            value = query.substring(index3, ampIndex);
        }
        return Uri.decode(value);
    }
}
