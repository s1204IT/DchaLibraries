package com.android.browser.util;

import android.util.Base64;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLUtil {
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$", 2);
    private static final Pattern BASE64_CONTENT_DISPOSITION_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)=\\?([a-zA-Z0-9-_]+)\\?B\\?([a-zA-Z0-9+/]*[=]{0,2})(\\?=)\\1\\s*$", 2);
    private static final Pattern CONTENT_DISPOSITION_EXTRA_PATTERN = Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1.*$", 2);
    private static final Pattern CONTENT_DISPOSITION_EXTRA_INLINE_PATTERN = Pattern.compile("inline;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1.*$", 2);

    static String parseContentDisposition(String str) throws UnsupportedEncodingException {
        String strGroup = null;
        try {
            Matcher matcher = BASE64_CONTENT_DISPOSITION_PATTERN.matcher(str);
            if (matcher.find()) {
                try {
                    try {
                        strGroup = URLDecoder.decode(new String(Base64.decode(matcher.group(3), 0), matcher.group(2)), matcher.group(2));
                    } catch (UnsupportedEncodingException e) {
                        Log.d("webkit", "UnsupportedEncodingException: " + str);
                    }
                } catch (IllegalArgumentException e2) {
                    Log.d("webkit", "IllegalArgumentException: " + str);
                }
            }
        } catch (IllegalStateException e3) {
            Log.d("webkit", "IllegalStateException: illBase64Ex: " + str);
        }
        if (strGroup == null) {
            try {
                Matcher matcher2 = CONTENT_DISPOSITION_PATTERN.matcher(str);
                if (matcher2.find()) {
                    strGroup = matcher2.group(2);
                }
            } catch (IllegalStateException e4) {
                Log.d("webkit", "IllegalStateException: ex: " + str);
            }
        }
        if (strGroup == null) {
            try {
                Matcher matcher3 = CONTENT_DISPOSITION_EXTRA_PATTERN.matcher(str);
                if (matcher3.find()) {
                    strGroup = matcher3.group(2);
                }
            } catch (IllegalStateException e5) {
                Log.d("webkit", "Extra IllegalStateException: ex: " + str);
            }
        }
        if (strGroup == null) {
            try {
                Matcher matcher4 = CONTENT_DISPOSITION_EXTRA_INLINE_PATTERN.matcher(str);
                if (matcher4.find()) {
                    return matcher4.group(2);
                }
                return strGroup;
            } catch (IllegalStateException e6) {
                Log.d("webkit", "Extra inline IllegalStateException: ex: " + str);
                return strGroup;
            }
        }
        return strGroup;
    }
}
