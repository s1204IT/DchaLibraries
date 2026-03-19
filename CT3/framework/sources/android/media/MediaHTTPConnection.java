package android.media;

import android.content.IntentFilter;
import android.media.IMediaHTTPConnection;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Map;

public class MediaHTTPConnection extends IMediaHTTPConnection.Stub {
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int HTTP_TEMP_REDIRECT = 307;
    private static final int MAX_REDIRECTS = 20;
    private static final String TAG = "MediaHTTPConnection";
    private static final boolean VERBOSE = false;
    private long mNativeContext;
    private long mCurrentOffset = -1;
    private URL mURL = null;
    private Map<String, String> mHeaders = null;
    private HttpURLConnection mConnection = null;
    private long mTotalSize = -1;
    private InputStream mInputStream = null;
    private boolean mAllowCrossDomainRedirect = true;
    private boolean mAllowCrossProtocolRedirect = true;

    private final native void native_finalize();

    private final native IBinder native_getIMemory();

    private static final native void native_init();

    private final native int native_readAt(long j, int i);

    private final native void native_setup();

    public MediaHTTPConnection() {
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(new CookieManager());
        }
        native_setup();
    }

    @Override
    public IBinder connect(String uri, String headers) {
        try {
            disconnect();
            this.mAllowCrossDomainRedirect = true;
            this.mURL = new URL(uri);
            this.mHeaders = convertHeaderStringToMap(headers);
            return native_getIMemory();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private boolean parseBoolean(String val) {
        try {
            return Long.parseLong(val) != 0;
        } catch (NumberFormatException e) {
            if ("true".equalsIgnoreCase(val)) {
                return true;
            }
            return "yes".equalsIgnoreCase(val);
        }
    }

    private boolean filterOutInternalHeaders(String key, String val) {
        if ("android-allow-cross-domain-redirect".equalsIgnoreCase(key)) {
            this.mAllowCrossDomainRedirect = parseBoolean(val);
            this.mAllowCrossProtocolRedirect = this.mAllowCrossDomainRedirect;
            return true;
        }
        return false;
    }

    private Map<String, String> convertHeaderStringToMap(String headers) {
        HashMap<String, String> map = new HashMap<>();
        String[] pairs = headers.split("\r\n");
        for (String pair : pairs) {
            int colonPos = pair.indexOf(":");
            if (colonPos >= 0) {
                String key = pair.substring(0, colonPos);
                String val = pair.substring(colonPos + 1);
                if (!filterOutInternalHeaders(key, val)) {
                    map.put(key, val);
                }
            }
        }
        return map;
    }

    @Override
    public void disconnect() {
        teardownConnection();
        this.mHeaders = null;
        this.mURL = null;
        Log.d(TAG, "disconnect finish");
    }

    private void teardownConnection() {
        if (this.mConnection == null) {
            return;
        }
        Log.d(TAG, "teardownConnection");
        if (this.mInputStream != null) {
            try {
                this.mInputStream.close();
            } catch (IOException e) {
            }
            this.mInputStream = null;
        }
        this.mConnection.disconnect();
        this.mConnection = null;
        this.mCurrentOffset = -1L;
    }

    private static final boolean isLocalHost(URL url) {
        String host;
        if (url == null || (host = url.getHost()) == null) {
            return false;
        }
        try {
            if (host.equalsIgnoreCase(ProxyInfo.LOCAL_HOST)) {
                return true;
            }
            if (NetworkUtils.numericToInetAddress(host).isLoopbackAddress()) {
                return true;
            }
        } catch (IllegalArgumentException e) {
        }
        return false;
    }

    private void seekTo(long offset) throws IOException {
        int response;
        teardownConnection();
        int redirectCount = 0;
        try {
            URL url = this.mURL;
            boolean noProxy = isLocalHost(url);
            while (true) {
                if (noProxy) {
                    this.mConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                } else {
                    this.mConnection = (HttpURLConnection) url.openConnection();
                }
                this.mConnection.setConnectTimeout(30000);
                this.mConnection.setReadTimeout(60000);
                this.mConnection.setInstanceFollowRedirects(this.mAllowCrossDomainRedirect);
                if (this.mHeaders != null) {
                    for (Map.Entry<String, String> entry : this.mHeaders.entrySet()) {
                        this.mConnection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
                if (offset > 0) {
                    this.mConnection.setRequestProperty("Range", "bytes=" + offset + ContactsContract.Aas.ENCODE_SYMBOL);
                }
                response = this.mConnection.getResponseCode();
                if (response != 300 && response != 301 && response != 302 && response != 303 && response != 307) {
                    break;
                }
                redirectCount++;
                if (redirectCount > 20) {
                    throw new NoRouteToHostException("Too many redirects: " + redirectCount);
                }
                String method = this.mConnection.getRequestMethod();
                if (response == 307 && !method.equals("GET") && !method.equals("HEAD")) {
                    throw new NoRouteToHostException("Invalid redirect");
                }
                String location = this.mConnection.getHeaderField("Location");
                if (location == null) {
                    throw new NoRouteToHostException("Invalid redirect");
                }
                url = new URL(this.mURL, location);
                if (!url.getProtocol().equals(IntentFilter.SCHEME_HTTPS) && !url.getProtocol().equals(IntentFilter.SCHEME_HTTP)) {
                    throw new NoRouteToHostException("Unsupported protocol redirect");
                }
                boolean sameProtocol = this.mURL.getProtocol().equals(url.getProtocol());
                if (!this.mAllowCrossProtocolRedirect && !sameProtocol) {
                    throw new NoRouteToHostException("Cross-protocol redirects are disallowed");
                }
                boolean sameHost = this.mURL.getHost().equals(url.getHost());
                if (!this.mAllowCrossDomainRedirect && !sameHost) {
                    throw new NoRouteToHostException("Cross-domain redirects are disallowed");
                }
                if (response != 307) {
                    this.mURL = url;
                }
            }
            Log.d(TAG, "mTotalSize=" + this.mTotalSize);
            if (offset <= 0 && response != 206) {
                throw new ProtocolException();
            }
            this.mInputStream = new BufferedInputStream(this.mConnection.getInputStream());
            this.mCurrentOffset = offset;
            if (offset <= 0) {
            }
            this.mInputStream = new BufferedInputStream(this.mConnection.getInputStream());
            this.mCurrentOffset = offset;
        } catch (IOException e) {
            this.mTotalSize = -1L;
            teardownConnection();
            this.mCurrentOffset = -1L;
            throw e;
        }
    }

    @Override
    public int readAt(long offset, int size) {
        return native_readAt(offset, size);
    }

    private int readAt(long offset, byte[] data, int size) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            if (offset != this.mCurrentOffset) {
                seekTo(offset);
            }
            if (this.mInputStream == null) {
                return -1;
            }
            int n = this.mInputStream.read(data, 0, size);
            if (n == -1) {
                n = 0;
            }
            this.mCurrentOffset += (long) n;
            return n;
        } catch (NoRouteToHostException e) {
            Log.w(TAG, "readAt " + offset + " / " + size + " => " + e);
            return MediaPlayer.MEDIA_ERROR_UNSUPPORTED;
        } catch (ProtocolException e2) {
            String msg = e2.getMessage();
            Log.w(TAG, "readAt " + offset + " / " + size + " => " + e2);
            if (msg != null && msg.indexOf("unexpected end of stream") != -1) {
                return -1;
            }
            return MediaPlayer.MEDIA_ERROR_UNSUPPORTED;
        } catch (UnknownServiceException e3) {
            Log.w(TAG, "readAt " + offset + " / " + size + " => " + e3);
            return MediaPlayer.MEDIA_ERROR_UNSUPPORTED;
        } catch (IOException e4) {
            return -1;
        } catch (Exception e5) {
            return -1;
        }
    }

    @Override
    public long getSize() {
        if (this.mConnection == null) {
            try {
                seekTo(0L);
            } catch (IOException e) {
                return -1L;
            }
        }
        return this.mTotalSize;
    }

    @Override
    public String getMIMEType() {
        if (this.mConnection == null) {
            try {
                seekTo(0L);
            } catch (IOException e) {
                return "application/octet-stream";
            }
        }
        return this.mConnection.getContentType();
    }

    @Override
    public String getUri() {
        return this.mURL.toString();
    }

    @Override
    protected void finalize() {
        native_finalize();
    }

    static {
        System.loadLibrary("media_jni");
        native_init();
    }
}
