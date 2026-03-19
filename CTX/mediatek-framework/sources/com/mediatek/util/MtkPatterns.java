package com.mediatek.util;

import android.os.Bundle;
import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MtkPatterns {
    public static final String KEY_URLDATA_END = "end";
    public static final String KEY_URLDATA_START = "start";
    public static final String KEY_URLDATA_VALUE = "value";
    private static final String TAG = "MtkPatterns";
    private static final String[] MTK_WEB_PROTOCOL_NAMES = {"http://", "https://", "rtsp://", "ftp://"};
    private static final String mValidCharRegex = "a-zA-Z0-9\\-_";
    private static final String mBadFrontRemovingRegex = String.format("(^[^.]*[^%s.://#&=]+)(?:[a-zA-Z]+://|[%s]+.)", mValidCharRegex, mValidCharRegex);
    private static final String mBadEndRemovingRegex = String.format("([\\.\\:][%s)]+[/%s]*)([\\.\\:]?[^%s\\.\\:\\s/]+[^\\.=&%%/]*$)", mValidCharRegex, mValidCharRegex, mValidCharRegex);

    public static String[] getWebProtocolNames(String[] strArr) {
        return MTK_WEB_PROTOCOL_NAMES;
    }

    private static final String replaceGroup(String str, String str2, int i, String str3) {
        return replaceGroup(str, str2, i, 1, str3);
    }

    private static final String replaceGroup(String str, String str2, int i, int i2, String str3) {
        Matcher matcher = Pattern.compile(str).matcher(str2);
        for (int i3 = 0; i3 < i2; i3++) {
            if (!matcher.find()) {
                return str2;
            }
        }
        return new StringBuilder(str2).replace(matcher.start(i), matcher.end(i), str3).toString();
    }

    public static Bundle getWebUrl(String str, int i, int i2) {
        Log.d("@M_MtkPatterns", "getWebUrl,  start=" + i + " end=" + i2);
        if (str != null) {
            if (Pattern.compile(mBadFrontRemovingRegex).matcher(str).find()) {
                str = replaceGroup(mBadFrontRemovingRegex, str, 1, "");
                i = i2 - str.length();
            }
            if (Pattern.compile(mBadEndRemovingRegex).matcher(str).find()) {
                str = replaceGroup(mBadEndRemovingRegex, str, 2, "");
                i2 = str.length() + i;
            }
        }
        Bundle bundle = new Bundle();
        bundle.putString(KEY_URLDATA_VALUE, str);
        bundle.putInt(KEY_URLDATA_START, i);
        bundle.putInt(KEY_URLDATA_END, i2);
        return bundle;
    }

    public static final Pattern getMtkWebUrlPattern(Pattern pattern) {
        return ChinaPatterns.CHINA_AUTOLINK_WEB_URL;
    }
}
