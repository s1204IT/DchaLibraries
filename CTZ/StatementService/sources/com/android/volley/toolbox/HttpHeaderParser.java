package com.android.volley.toolbox;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.VolleyLog;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class HttpHeaderParser {
    public static Cache.Entry parseCacheHeaders(NetworkResponse networkResponse) {
        long j;
        long j2;
        boolean z;
        boolean z2;
        long j3;
        long jCurrentTimeMillis = System.currentTimeMillis();
        Map<String, String> map = networkResponse.headers;
        String str = map.get("Date");
        long j4 = 0;
        long dateAsEpoch = str != null ? parseDateAsEpoch(str) : 0L;
        String str2 = map.get("Cache-Control");
        if (str2 != null) {
            j = 0;
            j2 = 0;
            z = false;
            for (String str3 : str2.split(",")) {
                String strTrim = str3.trim();
                if (strTrim.equals("no-cache") || strTrim.equals("no-store")) {
                    return null;
                }
                if (strTrim.startsWith("max-age=")) {
                    try {
                        j = Long.parseLong(strTrim.substring(8));
                    } catch (Exception e) {
                    }
                } else if (strTrim.startsWith("stale-while-revalidate=")) {
                    try {
                        j2 = Long.parseLong(strTrim.substring(23));
                    } catch (Exception e2) {
                    }
                } else if (strTrim.equals("must-revalidate") || strTrim.equals("proxy-revalidate")) {
                    z = true;
                }
            }
            z2 = true;
        } else {
            j = 0;
            j2 = 0;
            z = false;
            z2 = false;
        }
        String str4 = map.get("Expires");
        long dateAsEpoch2 = str4 != null ? parseDateAsEpoch(str4) : 0L;
        String str5 = map.get("Last-Modified");
        long dateAsEpoch3 = str5 != null ? parseDateAsEpoch(str5) : 0L;
        String str6 = map.get("ETag");
        if (z2) {
            long j5 = jCurrentTimeMillis + (j * 1000);
            j3 = z ? j5 : (j2 * 1000) + j5;
            j4 = j5;
        } else {
            if (dateAsEpoch > 0 && dateAsEpoch2 >= dateAsEpoch) {
                j4 = jCurrentTimeMillis + (dateAsEpoch2 - dateAsEpoch);
            }
            j3 = j4;
        }
        Cache.Entry entry = new Cache.Entry();
        entry.data = networkResponse.data;
        entry.etag = str6;
        entry.softTtl = j4;
        entry.ttl = j3;
        entry.serverDate = dateAsEpoch;
        entry.lastModified = dateAsEpoch3;
        entry.responseHeaders = map;
        entry.allResponseHeaders = networkResponse.allHeaders;
        return entry;
    }

    public static long parseDateAsEpoch(String str) {
        try {
            return newRfc1123Formatter().parse(str).getTime();
        } catch (ParseException e) {
            VolleyLog.e(e, "Unable to parse dateStr: %s, falling back to 0", str);
            return 0L;
        }
    }

    private static SimpleDateFormat newRfc1123Formatter() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return simpleDateFormat;
    }
}
