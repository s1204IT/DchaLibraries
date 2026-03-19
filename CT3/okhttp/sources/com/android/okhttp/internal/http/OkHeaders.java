package com.android.okhttp.internal.http;

import com.android.okhttp.Authenticator;
import com.android.okhttp.Challenge;
import com.android.okhttp.Headers;
import com.android.okhttp.Request;
import com.android.okhttp.Response;
import com.android.okhttp.internal.Platform;
import com.android.okhttp.internal.Util;
import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
        int size = headers.size();
        for (int i = 0; i < size; i++) {
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
        int size = cookies.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(cookies.get(i));
        }
        return sb.toString();
    }

    public static boolean varyMatches(Response cachedResponse, Headers cachedRequest, Request newRequest) {
        for (String field : varyFields(cachedResponse)) {
            if (!Util.equal(cachedRequest.values(field), newRequest.headers(field))) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasVaryAll(Response response) {
        return hasVaryAll(response.headers());
    }

    public static boolean hasVaryAll(Headers responseHeaders) {
        return varyFields(responseHeaders).contains("*");
    }

    private static Set<String> varyFields(Response response) {
        return varyFields(response.headers());
    }

    public static Set<String> varyFields(Headers responseHeaders) {
        Set<String> result = Collections.emptySet();
        int size = responseHeaders.size();
        for (int i = 0; i < size; i++) {
            if ("Vary".equalsIgnoreCase(responseHeaders.name(i))) {
                String value = responseHeaders.value(i);
                if (result.isEmpty()) {
                    result = new TreeSet<>((Comparator<? super String>) String.CASE_INSENSITIVE_ORDER);
                }
                for (String varyField : value.split(",")) {
                    result.add(varyField.trim());
                }
            }
        }
        return result;
    }

    public static Headers varyHeaders(Response response) {
        Headers requestHeaders = response.networkResponse().request().headers();
        Headers responseHeaders = response.headers();
        return varyHeaders(requestHeaders, responseHeaders);
    }

    public static Headers varyHeaders(Headers requestHeaders, Headers responseHeaders) {
        Set<String> varyFields = varyFields(responseHeaders);
        if (varyFields.isEmpty()) {
            return new Headers.Builder().build();
        }
        Headers.Builder result = new Headers.Builder();
        int size = requestHeaders.size();
        for (int i = 0; i < size; i++) {
            String fieldName = requestHeaders.name(i);
            if (varyFields.contains(fieldName)) {
                result.add(fieldName, requestHeaders.value(i));
            }
        }
        return result.build();
    }

    static boolean isEndToEnd(String fieldName) {
        return ("Connection".equalsIgnoreCase(fieldName) || "Keep-Alive".equalsIgnoreCase(fieldName) || "Proxy-Authenticate".equalsIgnoreCase(fieldName) || "Proxy-Authorization".equalsIgnoreCase(fieldName) || "TE".equalsIgnoreCase(fieldName) || "Trailers".equalsIgnoreCase(fieldName) || "Transfer-Encoding".equalsIgnoreCase(fieldName) || "Upgrade".equalsIgnoreCase(fieldName)) ? false : true;
    }

    public static List<Challenge> parseChallenges(Headers responseHeaders, String challengeHeader) {
        String value;
        String kv;
        List<com.squareup.okhttp.Challenge> result = new ArrayList<>();
        int size = responseHeaders.size();
        for (int i = 0; i < size; i++) {
            if (challengeHeader.equalsIgnoreCase(responseHeaders.name(i))) {
                String value2 = responseHeaders.value(i);
                String[] schemeParts = value2.split("[r|R][e|E][a|A][l|L][m|M]=\"");
                int schemeCount = schemeParts.length - 1;
                System.out.println("Scheme count=" + schemeCount);
                if (schemeCount == 0) {
                    System.out.println("no scheme found!!!");
                    return result;
                }
                for (int l = 0; l < schemeCount; l++) {
                    if (schemeCount == 1) {
                        value = schemeParts[l] + " realm=\"" + schemeParts[l + 1].trim();
                    } else if (l == 0) {
                        value = schemeParts[l] + " realm=\"" + schemeParts[l + 1].substring(0, schemeParts[l + 1].lastIndexOf(",")).trim();
                    } else if (l == schemeCount - 1) {
                        value = schemeParts[l].substring(schemeParts[l].lastIndexOf(",") + 1).trim() + " realm=\"" + schemeParts[l + 1];
                    } else {
                        value = schemeParts[l].substring(schemeParts[l].lastIndexOf(",") + 1).trim() + " realm=\"" + schemeParts[l + 1].substring(0, schemeParts[l + 1].lastIndexOf(",")).trim();
                    }
                    System.out.println("Round " + l + " Scheme value: " + value);
                    int pos = 0;
                    while (pos < value.length()) {
                        int tokenStart = pos;
                        int pos2 = HeaderParser.skipUntil(value, pos, " ");
                        String scheme = value.substring(tokenStart, pos2).trim();
                        System.out.println("scheme=" + scheme);
                        int pos3 = HeaderParser.skipWhitespace(value, pos2);
                        String rest = value.substring(pos3);
                        pos = pos3 + rest.length();
                        String realm = null;
                        String nonce = null;
                        String stale = null;
                        String qop = null;
                        String opaque = null;
                        String[] fields = rest.split(",");
                        int j = 0;
                        for (String field : fields) {
                            System.out.println("field[" + j + "]: " + field);
                            String[] keyValue = field.trim().split("=");
                            if (keyValue.length < 2) {
                                System.out.println("No support:" + field);
                            } else {
                                String key = keyValue[0];
                                if (keyValue.length > 2) {
                                    kv = field.trim().substring(key.length() + 1);
                                } else {
                                    kv = keyValue[1];
                                }
                                if (kv.indexOf("\"") >= 0) {
                                    kv = HeaderParser.getQuoteString(kv, key, 0);
                                }
                                System.out.println("key=" + key + ", value=" + kv);
                                if ("realm".equals(key)) {
                                    realm = kv;
                                    System.setProperty("digest.realm", kv);
                                } else if (!"uri".equals(key) && !"algorithm".equals(key) && !"domain".equals(key)) {
                                    if ("nonce".equals(key)) {
                                        nonce = kv;
                                    } else if ("stale".equals(key)) {
                                        stale = kv;
                                    } else if ("qop".equals(key)) {
                                        qop = kv;
                                    } else if ("opaque".equals(key)) {
                                        opaque = kv;
                                    }
                                }
                            }
                            j++;
                        }
                        Challenge ch = new Challenge(scheme, realm, nonce, stale, qop, opaque);
                        result.add(ch);
                        System.out.println("ch(allenge)=" + ch);
                    }
                }
            }
        }
        System.out.println(result);
        return result;
    }

    public static Request processAuthHeader(Authenticator authenticator, Response response, Proxy proxy) throws IOException {
        if (response.code() == 407) {
            return authenticator.authenticateProxy(proxy, response);
        }
        return authenticator.authenticate(proxy, response);
    }
}
