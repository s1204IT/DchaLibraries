package com.android.server.location;

import android.content.Context;
import android.net.Proxy;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.util.Log;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRouteParams;

public class GpsXtraDownloader {
    private static final String DEFAULT_USER_AGENT = "Android";
    private static final long MAXIMUM_CONTENT_LENGTH_BYTES = 1000000;
    private final Context mContext;
    private int mNextServerIndex;
    private final String mUserAgent;
    private final String[] mXtraServers;
    private static final String TAG = "GpsXtraDownloader";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    GpsXtraDownloader(Context context, Properties properties) {
        int count;
        int count2;
        this.mContext = context;
        String server1 = properties.getProperty("XTRA_SERVER_1");
        String server2 = properties.getProperty("XTRA_SERVER_2");
        String server3 = properties.getProperty("XTRA_SERVER_3");
        int count3 = server1 != null ? 0 + 1 : 0;
        count3 = server2 != null ? count3 + 1 : count3;
        count3 = server3 != null ? count3 + 1 : count3;
        String agent = properties.getProperty("XTRA_USER_AGENT");
        if (TextUtils.isEmpty(agent)) {
            this.mUserAgent = DEFAULT_USER_AGENT;
        } else {
            this.mUserAgent = agent;
        }
        if (count3 == 0) {
            Log.e(TAG, "No XTRA servers were specified in the GPS configuration");
            this.mXtraServers = null;
            return;
        }
        this.mXtraServers = new String[count3];
        if (server1 != null) {
            count = 0 + 1;
            this.mXtraServers[0] = server1;
        } else {
            count = 0;
        }
        if (server2 != null) {
            this.mXtraServers[count] = server2;
            count++;
        }
        if (server3 != null) {
            count2 = count + 1;
            this.mXtraServers[count] = server3;
        } else {
            count2 = count;
        }
        Random random = new Random();
        this.mNextServerIndex = random.nextInt(count2);
    }

    byte[] downloadXtraData() {
        String proxyHost = Proxy.getHost(this.mContext);
        int proxyPort = Proxy.getPort(this.mContext);
        boolean useProxy = (proxyHost == null || proxyPort == -1) ? false : true;
        byte[] result = null;
        int startIndex = this.mNextServerIndex;
        if (this.mXtraServers == null) {
            return null;
        }
        while (result == null) {
            result = doDownload(this.mXtraServers[this.mNextServerIndex], useProxy, proxyHost, proxyPort);
            this.mNextServerIndex++;
            if (this.mNextServerIndex == this.mXtraServers.length) {
                this.mNextServerIndex = 0;
            }
            if (this.mNextServerIndex == startIndex) {
                break;
            }
        }
        return result;
    }

    protected byte[] doDownload(String url, boolean isProxySet, String proxyHost, int proxyPort) {
        if (DEBUG) {
            Log.d(TAG, "Downloading XTRA data from " + url);
        }
        AndroidHttpClient client = null;
        try {
            try {
                if (DEBUG) {
                    Log.d(TAG, "XTRA user agent: " + this.mUserAgent);
                }
                AndroidHttpClient client2 = AndroidHttpClient.newInstance(this.mUserAgent);
                HttpUriRequest req = new HttpGet(url);
                if (isProxySet) {
                    HttpHost proxy = new HttpHost(proxyHost, proxyPort);
                    ConnRouteParams.setDefaultProxy(req.getParams(), proxy);
                }
                req.addHeader("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
                req.addHeader("x-wap-profile", "http://www.openmobilealliance.org/tech/profiles/UAPROF/ccppschema-20021212#");
                HttpResponse response = client2.execute(req);
                StatusLine status = response.getStatusLine();
                if (status.getStatusCode() != 200) {
                    if (DEBUG) {
                        Log.d(TAG, "HTTP error: " + status.getReasonPhrase());
                    }
                    if (client2 == null) {
                        return null;
                    }
                    client2.close();
                    return null;
                }
                HttpEntity entity = response.getEntity();
                byte[] body = null;
                if (entity != null) {
                    try {
                        long contentLength = entity.getContentLength();
                        if (contentLength > 0 && contentLength <= MAXIMUM_CONTENT_LENGTH_BYTES) {
                            body = new byte[(int) contentLength];
                            DataInputStream dis = new DataInputStream(entity.getContent());
                            try {
                                dis.readFully(body);
                            } finally {
                                try {
                                    dis.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Unexpected IOException.", e);
                                }
                            }
                        }
                    } finally {
                        if (entity != null) {
                            entity.consumeContent();
                        }
                    }
                }
                if (client2 == null) {
                    return body;
                }
                client2.close();
                return body;
            } catch (Exception e2) {
                if (DEBUG) {
                    Log.d(TAG, "error " + e2);
                }
                if (0 != 0) {
                    client.close();
                }
                return null;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                client.close();
            }
            throw th;
        }
    }
}
