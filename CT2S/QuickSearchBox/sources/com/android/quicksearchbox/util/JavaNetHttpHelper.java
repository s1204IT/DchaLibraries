package com.android.quicksearchbox.util;

import android.os.Build;
import com.android.quicksearchbox.util.HttpHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class JavaNetHttpHelper implements HttpHelper {
    private int mConnectTimeout;
    private int mReadTimeout;
    private final HttpHelper.UrlRewriter mRewriter;
    private final String mUserAgent;

    public JavaNetHttpHelper(HttpHelper.UrlRewriter rewriter, String userAgent) {
        this.mUserAgent = userAgent + " (" + Build.DEVICE + " " + Build.ID + ")";
        this.mRewriter = rewriter;
    }

    @Override
    public String get(HttpHelper.GetRequest request) throws IOException {
        return get(request.getUrl(), request.getHeaders());
    }

    public String get(String url, Map<String, String> requestHeaders) throws IOException {
        HttpURLConnection c = null;
        try {
            c = createConnection(url, requestHeaders);
            c.setRequestMethod("GET");
            c.connect();
            return getResponseFrom(c);
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    private HttpURLConnection createConnection(String url, Map<String, String> headers) throws IOException {
        URL u = new URL(this.mRewriter.rewrite(url));
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                String name = e.getKey();
                String value = e.getValue();
                c.addRequestProperty(name, value);
            }
        }
        c.addRequestProperty("User-Agent", this.mUserAgent);
        if (this.mConnectTimeout != 0) {
            c.setConnectTimeout(this.mConnectTimeout);
        }
        if (this.mReadTimeout != 0) {
            c.setReadTimeout(this.mReadTimeout);
        }
        return c;
    }

    private String getResponseFrom(HttpURLConnection c) throws IOException {
        if (c.getResponseCode() != 200) {
            throw new HttpHelper.HttpException(c.getResponseCode(), c.getResponseMessage());
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
        StringBuilder string = new StringBuilder();
        char[] chars = new char[4096];
        while (true) {
            int bytes = reader.read(chars);
            if (bytes != -1) {
                string.append(chars, 0, bytes);
            } else {
                return string.toString();
            }
        }
    }

    public static class PassThroughRewriter implements HttpHelper.UrlRewriter {
        @Override
        public String rewrite(String url) {
            return url;
        }
    }
}
