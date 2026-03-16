package com.android.okhttp.internal.http;

import com.android.okhttp.Headers;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.internal.Platform;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class OkHeaders {
    private static final Comparator<String> FIELD_NAME_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String a, String b) {
            if (a == b) {
                return 0;
            }
            if (a == null) {
                return -1;
            }
            if (b == null) {
                return 1;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        }
    };
    static final String PREFIX = Platform.get().getPrefix();
    public static final String SENT_MILLIS = PREFIX + "-Sent-Millis";
    public static final String RECEIVED_MILLIS = PREFIX + "-Received-Millis";
    public static final String RESPONSE_SOURCE = PREFIX + "-Response-Source";
    public static final String SELECTED_PROTOCOL = PREFIX + "-Selected-Protocol";

    private OkHeaders() {
    }

    public static long contentLength(Request request) {
        return contentLength(request.headers());
    }

    public static long contentLength(Response response) {
        return contentLength(response.headers());
    }

    public static long contentLength(Headers headers) {
        return stringToLong(headers.get("Content-Length"));
    }

    private static long stringToLong(String s) {
        if (s == null) {
            return -1L;
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    public static Map<String, List<String>> toMultimap(Headers headers, String valueForNullKey) {
        Map<String, List<String>> result = new TreeMap<>(FIELD_NAME_COMPARATOR);
        for (int i = 0; i < headers.size(); i++) {
            String fieldName = headers.name(i);
            String value = headers.value(i);
            List<String> allValues = new ArrayList<>();
            List<String> otherValues = result.get(fieldName);
            if (otherValues != null) {
                allValues.addAll(otherValues);
            }
            allValues.add(value);
            result.put(fieldName, Collections.unmodifiableList(allValues));
        }
        if (valueForNullKey != null) {
            result.put(null, Collections.unmodifiableList(Collections.singletonList(valueForNullKey)));
        }
        return Collections.unmodifiableMap(result);
    }

    public static void addCookies(Request.Builder builder, Map<String, List<String>> cookieHeaders) {
        for (Map.Entry<String, List<String>> entry : cookieHeaders.entrySet()) {
            String key = entry.getKey();
            if ("Cookie".equalsIgnoreCase(key) || "Cookie2".equalsIgnoreCase(key)) {
                if (!entry.getValue().isEmpty()) {
                    builder.addHeader(key, buildCookieHeader(entry.getValue()));
                }
            }
        }
    }

    private static String buildCookieHeader(List<String> cookies) {
        if (cookies.size() == 1) {
            return cookies.get(0);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cookies.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(cookies.get(i));
        }
        return sb.toString();
    }
}
