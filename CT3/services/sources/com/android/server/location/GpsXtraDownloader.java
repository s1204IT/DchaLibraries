package com.android.server.location;

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

    GpsXtraDownloader(Properties properties) {
        int count;
        int count2;
        String server1 = properties.getProperty("XTRA_SERVER_1");
        String server2 = properties.getProperty("XTRA_SERVER_2");
        String server3 = properties.getProperty("XTRA_SERVER_3");
        int count3 = server1 != null ? 1 : 0;
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
            this.mXtraServers[0] = server1;
            count = 1;
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
        byte[] result = null;
        int startIndex = this.mNextServerIndex;
        if (this.mXtraServers == null) {
            return null;
        }
        while (result == null) {
            result = doDownload(this.mXtraServers[this.mNextServerIndex]);
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

    protected byte[] doDownload(String url) {
        if (DEBUG) {
            Log.d(TAG, "Downloading XTRA data from " + url);
        }
        HttpURLConnection httpURLConnection = null;
        try {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("Accept", "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic");
                connection.setRequestProperty("x-wap-profile", "http://www.openmobilealliance.org/tech/profiles/UAPROF/ccppschema-20021212#");
                connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
                connection.connect();
                int statusCode = connection.getResponseCode();
                if (statusCode != 200) {
                    if (DEBUG) {
                        Log.d(TAG, "HTTP error downloading gps XTRA: " + statusCode);
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                    return null;
                }
                Throwable th = null;
                InputStream inputStream = null;
                try {
                    InputStream in = connection.getInputStream();
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    do {
                        int count = in.read(buffer);
                        if (count == -1) {
                            byte[] byteArray = bytes.toByteArray();
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            }
                            if (th != null) {
                                throw th;
                            }
                            if (connection != null) {
                                connection.disconnect();
                            }
                            return byteArray;
                        }
                        bytes.write(buffer, 0, count);
                    } while (bytes.size() <= MAXIMUM_CONTENT_LENGTH_BYTES);
                    if (DEBUG) {
                        Log.d(TAG, "XTRA file too large");
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    }
                    if (th != null) {
                        throw th;
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                    return null;
                } catch (Throwable th4) {
                    th = th4;
                    if (0 != 0) {
                    }
                    if (th == null) {
                    }
                }
            } catch (IOException ioe) {
                if (DEBUG) {
                    Log.d(TAG, "Error downloading gps XTRA: ", ioe);
                }
                if (0 == 0) {
                    return null;
                }
                httpURLConnection.disconnect();
                return null;
            }
        } catch (Throwable th5) {
            if (0 != 0) {
                httpURLConnection.disconnect();
            }
            throw th5;
        }
    }
}
