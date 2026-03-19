package com.android.server.location;

import android.net.TrafficStats;
import android.text.TextUtils;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GpsXtraDownloader {
    private static final String DEFAULT_USER_AGENT = "Android";
    private static final long MAXIMUM_CONTENT_LENGTH_BYTES = 1000000;
    private int mNextServerIndex;
    private final String mUserAgent;
    private final String[] mXtraServers;
    private static final String TAG = "GpsXtraDownloader";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int CONNECTION_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(30);
    private static final int READ_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(60);

    GpsXtraDownloader(Properties properties) {
        int i;
        int i2;
        String property = properties.getProperty("XTRA_SERVER_1");
        String property2 = properties.getProperty("XTRA_SERVER_2");
        String property3 = properties.getProperty("XTRA_SERVER_3");
        int i3 = 0;
        if (property == null) {
            i = 0;
        } else {
            i = 1;
        }
        i = property2 != null ? i + 1 : i;
        i = property3 != null ? i + 1 : i;
        String property4 = properties.getProperty("XTRA_USER_AGENT");
        if (TextUtils.isEmpty(property4)) {
            this.mUserAgent = DEFAULT_USER_AGENT;
        } else {
            this.mUserAgent = property4;
        }
        if (i == 0) {
            Log.e(TAG, "No XTRA servers were specified in the GPS configuration");
            this.mXtraServers = null;
            return;
        }
        this.mXtraServers = new String[i];
        if (property != null) {
            this.mXtraServers[0] = property;
            i3 = 1;
        }
        if (property2 != null) {
            i2 = i3 + 1;
            this.mXtraServers[i3] = property2;
        } else {
            i2 = i3;
        }
        if (property3 != null) {
            this.mXtraServers[i2] = property3;
            i2++;
        }
        this.mNextServerIndex = new Random().nextInt(i2);
    }

    byte[] downloadXtraData() {
        int i = this.mNextServerIndex;
        byte[] bArrDoDownload = null;
        if (this.mXtraServers == null) {
            return null;
        }
        while (bArrDoDownload == null) {
            int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-188);
            try {
                bArrDoDownload = doDownload(this.mXtraServers[this.mNextServerIndex]);
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                this.mNextServerIndex++;
                if (this.mNextServerIndex == this.mXtraServers.length) {
                    this.mNextServerIndex = 0;
                }
                if (this.mNextServerIndex == i) {
                    break;
                }
            } catch (Throwable th) {
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                throw th;
            }
        }
        return bArrDoDownload;
    }

    protected byte[] doDownload(String str) throws Throwable {
        Throwable th;
        HttpURLConnection httpURLConnection;
        Throwable th2;
        Throwable th3;
        if (DEBUG) {
            Log.d(TAG, "Downloading XTRA data from " + ((String) str));
        }
        try {
            try {
                httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
                try {
                    httpURLConnection.setRequestProperty("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
                    httpURLConnection.setRequestProperty("x-wap-profile", "http://www.openmobilealliance.org/tech/profiles/UAPROF/ccppschema-20021212#");
                    httpURLConnection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                    httpURLConnection.setReadTimeout(READ_TIMEOUT_MS);
                    httpURLConnection.connect();
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode != 200) {
                        if (DEBUG) {
                            Log.d(TAG, "HTTP error downloading gps XTRA: " + responseCode);
                        }
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                        return null;
                    }
                    InputStream inputStream = httpURLConnection.getInputStream();
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] bArr = new byte[1024];
                        do {
                            int i = inputStream.read(bArr);
                            if (i == -1) {
                                byte[] byteArray = byteArrayOutputStream.toByteArray();
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                if (httpURLConnection != null) {
                                    httpURLConnection.disconnect();
                                }
                                return byteArray;
                            }
                            byteArrayOutputStream.write(bArr, 0, i);
                        } while (byteArrayOutputStream.size() <= MAXIMUM_CONTENT_LENGTH_BYTES);
                        if (DEBUG) {
                            Log.d(TAG, "XTRA file too large");
                        }
                        if (inputStream != null) {
                            inputStream.close();
                        }
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                        return null;
                    } catch (Throwable th4) {
                        try {
                            throw th4;
                        } catch (Throwable th5) {
                            th2 = th4;
                            th3 = th5;
                            if (inputStream != null) {
                                throw th3;
                            }
                            if (th2 == null) {
                                inputStream.close();
                                throw th3;
                            }
                            try {
                                inputStream.close();
                                throw th3;
                            } catch (Throwable th6) {
                                th2.addSuppressed(th6);
                                throw th3;
                            }
                        }
                    }
                } catch (IOException e) {
                    e = e;
                    if (DEBUG) {
                        Log.d(TAG, "Error downloading gps XTRA: ", e);
                    }
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                    return null;
                }
            } catch (Throwable th7) {
                th = th7;
                if (str != 0) {
                    str.disconnect();
                }
                throw th;
            }
        } catch (IOException e2) {
            e = e2;
            httpURLConnection = null;
        } catch (Throwable th8) {
            th = th8;
            str = 0;
            if (str != 0) {
            }
            throw th;
        }
    }
}
