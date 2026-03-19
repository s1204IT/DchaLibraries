package com.android.statementservice.retriever;

import android.util.Log;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class URLFetcher {
    private static final String TAG = URLFetcher.class.getSimpleName();

    public WebContent getWebContentFromUrlWithRetry(URL url, long j, int i, int i2, int i3) throws InterruptedException, AssociationServiceException, IOException {
        if (i3 <= 0) {
            throw new IllegalArgumentException("retry should be a postive inetger.");
        }
        while (i3 > 0) {
            try {
                return getWebContentFromUrl(url, j, i);
            } catch (IOException e) {
                i3--;
                if (i3 == 0) {
                    throw e;
                }
                Thread.sleep(i2);
            }
        }
        return null;
    }

    public WebContent getWebContentFromUrl(URL url, long j, int i) throws Throwable {
        HttpURLConnection httpURLConnection;
        String lowerCase = url.getProtocol().toLowerCase(Locale.US);
        if (!lowerCase.equals("http") && !lowerCase.equals("https")) {
            throw new IllegalArgumentException("The url protocol should be on http or https.");
        }
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
            try {
                httpURLConnection.setInstanceFollowRedirects(true);
                httpURLConnection.setConnectTimeout(i);
                httpURLConnection.setReadTimeout(i);
                httpURLConnection.setUseCaches(true);
                httpURLConnection.setInstanceFollowRedirects(false);
                httpURLConnection.addRequestProperty("Cache-Control", "max-stale=60");
                if (httpURLConnection.getResponseCode() != 200) {
                    Log.e(TAG, "The responses code is not 200 but " + httpURLConnection.getResponseCode());
                    WebContent webContent = new WebContent("", 0L);
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    return webContent;
                }
                if (httpURLConnection.getContentLength() <= j) {
                    WebContent webContent2 = new WebContent(inputStreamToString(httpURLConnection.getInputStream(), httpURLConnection.getContentLength(), j), getExpirationTimeMillisFromHTTPHeader(httpURLConnection.getHeaderFields()));
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    return webContent2;
                }
                Log.e(TAG, "The content size of the url is larger than " + j);
                WebContent webContent3 = new WebContent("", 0L);
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                return webContent3;
            } catch (Throwable th) {
                th = th;
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            httpURLConnection = null;
        }
    }

    public static String inputStreamToString(InputStream inputStream, int i, long j) throws AssociationServiceException, IOException {
        if (i < 0) {
            i = 0;
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(i);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        byte[] bArr = new byte[1024];
        do {
            int i2 = bufferedInputStream.read(bArr);
            if (i2 != -1) {
                byteArrayOutputStream.write(bArr, 0, i2);
            } else {
                return byteArrayOutputStream.toString("UTF-8");
            }
        } while (byteArrayOutputStream.size() <= j);
        throw new AssociationServiceException("The content size of the url is larger than " + j);
    }

    private Long getExpirationTimeMillisFromHTTPHeader(Map<String, List<String>> map) {
        if (map == null) {
            return null;
        }
        Cache.Entry cacheHeaders = HttpHeaderParser.parseCacheHeaders(new NetworkResponse(null, joinHttpHeaders(map)));
        if (cacheHeaders != null && cacheHeaders.ttl != 0) {
            return Long.valueOf(cacheHeaders.ttl);
        }
        return 0L;
    }

    private Map<String, String> joinHttpHeaders(Map<String, List<String>> map) {
        HashMap map2 = new HashMap();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            List<String> value = entry.getValue();
            if (value.size() == 1) {
                map2.put(entry.getKey(), value.get(0));
            } else {
                map2.put(entry.getKey(), Utils.joinStrings(", ", value));
            }
        }
        return map2;
    }
}
