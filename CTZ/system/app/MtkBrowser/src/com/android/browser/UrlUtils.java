package com.android.browser;

import android.net.Uri;
import android.util.Patterns;
import android.webkit.URLUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
public class UrlUtils {
    static final Pattern ACCEPTED_URI_SCHEMA_FOR_URLHANDLER = Pattern.compile("(?i)((?:http|https|file):\\/\\/|(?:inline|data|about|javascript):)(.*)");
    static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)((?:http|https|file):\\/\\/|(?:data|about|javascript):|(?:.*:.*@))(.*)");
    private static final Pattern STRIP_URL_PATTERN = Pattern.compile("^http://(.*?)/?$");

    public static String stripUrl(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = STRIP_URL_PATTERN.matcher(str);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return str;
    }

    protected static String smartUrlFilter(Uri uri) {
        if (uri != null) {
            return smartUrlFilter(uri.toString());
        }
        return null;
    }

    public static String smartUrlFilter(String str) {
        return smartUrlFilter(str, true);
    }

    public static String smartUrlFilter(String str, boolean z) {
        boolean z2;
        String strTrim = str.trim();
        if (strTrim.indexOf(32) == -1) {
            z2 = false;
        } else {
            z2 = true;
        }
        Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(strTrim);
        if (matcher.matches()) {
            String strGroup = matcher.group(1);
            String lowerCase = strGroup.toLowerCase();
            if (!lowerCase.equals(strGroup)) {
                strTrim = lowerCase + matcher.group(2);
            }
            if (z2 && Patterns.WEB_URL.matcher(strTrim).matches()) {
                return strTrim.replace(" ", "%20");
            }
            return strTrim;
        }
        if (!z2 && Patterns.WEB_URL.matcher(strTrim).matches()) {
            return URLUtil.guessUrl(strTrim);
        }
        if (z) {
            return URLUtil.composeSearchUrl(strTrim, "http://www.google.com/m?q=%s", "%s");
        }
        return null;
    }

    public static String fixUrl(String str) {
        int iIndexOf = str.indexOf(58);
        boolean zIsLowerCase = true;
        String str2 = str;
        for (int i = 0; i < iIndexOf; i++) {
            char cCharAt = str2.charAt(i);
            if (!Character.isLetter(cCharAt)) {
                break;
            }
            zIsLowerCase &= Character.isLowerCase(cCharAt);
            if (i == iIndexOf - 1 && !zIsLowerCase) {
                str2 = str2.substring(0, iIndexOf).toLowerCase() + str2.substring(iIndexOf);
            }
        }
        if (str2.startsWith("http://") || str2.startsWith("https://")) {
            return str2;
        }
        if (str2.startsWith("http:") || str2.startsWith("https:")) {
            if (str2.startsWith("http:/") || str2.startsWith("https:/")) {
                return str2.replaceFirst("/", "//");
            }
            return str2.replaceFirst(":", "://");
        }
        return str2;
    }

    static String filteredUrl(String str) {
        if (str == null || str.startsWith("content:") || str.startsWith("browser:")) {
            return "";
        }
        return str;
    }
}
