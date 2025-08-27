package com.android.quicksearchbox.util;

import android.os.Build;
import com.android.quicksearchbox.util.HttpHelper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/* loaded from: classes.dex */
public class JavaNetHttpHelper implements HttpHelper {
    private int mConnectTimeout;
    private int mReadTimeout;
    private final HttpHelper.UrlRewriter mRewriter;
    private final String mUserAgent;

    public JavaNetHttpHelper(HttpHelper.UrlRewriter urlRewriter, String str) {
        this.mUserAgent = str + " (" + Build.DEVICE + " " + Build.ID + ")";
        this.mRewriter = urlRewriter;
    }

    @Override // com.android.quicksearchbox.util.HttpHelper
    public String get(HttpHelper.GetRequest getRequest) throws IOException {
        return get(getRequest.getUrl(), getRequest.getHeaders());
    }

    public String get(String str, Map<String, String> map) throws Throwable {
        HttpURLConnection httpURLConnectionCreateConnection;
        try {
            httpURLConnectionCreateConnection = createConnection(str, map);
        } catch (Throwable th) {
            th = th;
            httpURLConnectionCreateConnection = null;
        }
        try {
            httpURLConnectionCreateConnection.setRequestMethod("GET");
            httpURLConnectionCreateConnection.connect();
            String responseFrom = getResponseFrom(httpURLConnectionCreateConnection);
            if (httpURLConnectionCreateConnection != null) {
                httpURLConnectionCreateConnection.disconnect();
            }
            return responseFrom;
        } catch (Throwable th2) {
            th = th2;
            if (httpURLConnectionCreateConnection != null) {
                httpURLConnectionCreateConnection.disconnect();
            }
            throw th;
        }
    }

    private HttpURLConnection createConnection(String str, Map<String, String> map) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(this.mRewriter.rewrite(str)).openConnection();
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                httpURLConnection.addRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        httpURLConnection.addRequestProperty("User-Agent", this.mUserAgent);
        if (this.mConnectTimeout != 0) {
            httpURLConnection.setConnectTimeout(this.mConnectTimeout);
        }
        if (this.mReadTimeout != 0) {
            httpURLConnection.setReadTimeout(this.mReadTimeout);
        }
        return httpURLConnection;
    }

    private String getResponseFrom(HttpURLConnection httpURLConnection) throws IOException {
        if (httpURLConnection.getResponseCode() != 200) {
            throw new HttpHelper.HttpException(httpURLConnection.getResponseCode(), httpURLConnection.getResponseMessage());
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        char[] cArr = new char[4096];
        while (true) {
            int i = bufferedReader.read(cArr);
            if (i != -1) {
                sb.append(cArr, 0, i);
            } else {
                return sb.toString();
            }
        }
    }

    public static class PassThroughRewriter implements HttpHelper.UrlRewriter {
        @Override // com.android.quicksearchbox.util.HttpHelper.UrlRewriter
        public String rewrite(String str) {
            return str;
        }
    }
}
