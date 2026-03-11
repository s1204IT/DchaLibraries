package com.android.browser;

import android.net.Uri;
import android.util.Patterns;
import android.webkit.URLUtil;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {
    static final Pattern ACCEPTED_URI_SCHEMA_FOR_URLHANDLER = Pattern.compile("(?i)((?:http|https|file):\\/\\/|(?:inline|data|about|javascript):)(.*)");
    static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile("(?i)((?:http|https|file):\\/\\/|(?:data|about|javascript):|(?:.*:.*@))(.*)");
    private static final Pattern STRIP_URL_PATTERN = Pattern.compile("^http://(.*?)/?$");

    static String filteredUrl(String str) {
        return (str == null || str.startsWith("content:") || str.startsWith("browser:")) ? "" : str;
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
        return (str2.startsWith("http://") || str2.startsWith("https://")) ? str2 : (str2.startsWith("http:") || str2.startsWith("https:")) ? (str2.startsWith("http:/") || str2.startsWith("https:/")) ? str2.replaceFirst("/", "//") : str2.replaceFirst(":", "://") : str2;
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
        String str2;
        String strTrim = str.trim();
        boolean z2 = strTrim.indexOf(32) != -1;
        Matcher matcher = ACCEPTED_URI_SCHEMA.matcher(strTrim);
        if (!matcher.matches()) {
            if (!z2 && Patterns.WEB_URL.matcher(strTrim).matches()) {
                return URLUtil.guessUrl(strTrim);
            }
            if (z) {
                return URLUtil.composeSearchUrl(strTrim, "http://www.google.com/m?q=%s", "%s");
            }
            return null;
        }
        String strGroup = matcher.group(1);
        String lowerCase = strGroup.toLowerCase();
        if (lowerCase.equals(strGroup)) {
            str2 = strTrim;
        } else {
            str2 = lowerCase + matcher.group(2);
        }
        return (z2 && Patterns.WEB_URL.matcher(str2).matches()) ? str2.replace(" ", "%20") : str2;
    }

    public static String stripUrl(String str) {
        if (str == null) {
            return null;
        }
        Matcher matcher = STRIP_URL_PATTERN.matcher(str);
        return matcher.matches() ? matcher.group(1) : str;
    }
}
