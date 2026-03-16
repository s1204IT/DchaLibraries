package android.net.http;

import com.android.internal.content.NativeLibraryHelper;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.RequestContent;

class Request {
    private static final String ACCEPT_ENCODING_HEADER = "Accept-Encoding";
    private static final String CONTENT_LENGTH_HEADER = "content-length";
    private static final String HOST_HEADER = "Host";
    private static RequestContent requestContentProcessor = new RequestContent();
    private int mBodyLength;
    private InputStream mBodyProvider;
    private Connection mConnection;
    EventHandler mEventHandler;
    HttpHost mHost;
    BasicHttpRequest mHttpRequest;
    String mPath;
    HttpHost mProxyHost;
    volatile boolean mCancelled = false;
    int mFailCount = 0;
    private int mReceivedBytes = 0;
    private final Object mClientResource = new Object();
    private boolean mLoadingPaused = false;

    Request(String method, HttpHost host, HttpHost proxyHost, String path, InputStream bodyProvider, int bodyLength, EventHandler eventHandler, Map<String, String> headers) {
        this.mEventHandler = eventHandler;
        this.mHost = host;
        this.mProxyHost = proxyHost;
        this.mPath = path;
        this.mBodyProvider = bodyProvider;
        this.mBodyLength = bodyLength;
        if (bodyProvider == null && !"POST".equalsIgnoreCase(method)) {
            this.mHttpRequest = new BasicHttpRequest(method, getUri());
        } else {
            this.mHttpRequest = new BasicHttpEntityEnclosingRequest(method, getUri());
            if (bodyProvider != null) {
                setBodyProvider(bodyProvider, bodyLength);
            }
        }
        addHeader(HOST_HEADER, getHostPort());
        addHeader(ACCEPT_ENCODING_HEADER, "gzip");
        addHeaders(headers);
    }

    synchronized void setLoadingPaused(boolean pause) {
        this.mLoadingPaused = pause;
        if (!this.mLoadingPaused) {
            notify();
        }
    }

    void setConnection(Connection connection) {
        this.mConnection = connection;
    }

    EventHandler getEventHandler() {
        return this.mEventHandler;
    }

    void addHeader(String name, String value) {
        if (name == null) {
            HttpLog.e("Null http header name");
            throw new NullPointerException("Null http header name");
        }
        if (value == null || value.length() == 0) {
            String damage = "Null or empty value for header \"" + name + "\"";
            HttpLog.e(damage);
            throw new RuntimeException(damage);
        }
        this.mHttpRequest.addHeader(name, value);
    }

    void addHeaders(Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    void sendRequest(AndroidHttpClientConnection httpClientConnection) throws org.apache.http.HttpException, IOException {
        if (!this.mCancelled) {
            requestContentProcessor.process(this.mHttpRequest, this.mConnection.getHttpContext());
            httpClientConnection.sendRequestHeader(this.mHttpRequest);
            if (this.mHttpRequest instanceof HttpEntityEnclosingRequest) {
                httpClientConnection.sendRequestEntity((HttpEntityEnclosingRequest) this.mHttpRequest);
            }
        }
    }

    void readResponse(AndroidHttpClientConnection httpClientConnection) throws ParseException, IOException {
        StatusLine statusLine;
        int statusCode;
        if (this.mCancelled) {
            return;
        }
        httpClientConnection.flush();
        Headers header = new Headers();
        do {
            statusLine = httpClientConnection.parseResponseHeader(header);
            statusCode = statusLine.getStatusCode();
        } while (statusCode < 200);
        ProtocolVersion v = statusLine.getProtocolVersion();
        this.mEventHandler.status(v.getMajor(), v.getMinor(), statusCode, statusLine.getReasonPhrase());
        this.mEventHandler.headers(header);
        boolean hasBody = canResponseHaveBody(this.mHttpRequest, statusCode);
        HttpEntity entity = hasBody ? httpClientConnection.receiveResponseEntity(header) : null;
        boolean supportPartialContent = "bytes".equalsIgnoreCase(header.getAcceptRanges());
        if (entity != null) {
            InputStream is = entity.getContent();
            Header contentEncoding = entity.getContentEncoding();
            InputStream nis = null;
            byte[] buf = null;
            int count = 0;
            try {
                if (contentEncoding != null) {
                    try {
                        nis = contentEncoding.getValue().equals("gzip") ? new GZIPInputStream(is) : is;
                        buf = this.mConnection.getBuf();
                        int len = 0;
                        int lowWater = buf.length / 2;
                        while (len != -1) {
                            synchronized (this) {
                                while (this.mLoadingPaused) {
                                    try {
                                        wait();
                                    } catch (InterruptedException e) {
                                        HttpLog.e("Interrupted exception whilst network thread paused at WebCore's request. " + e.getMessage());
                                    }
                                }
                            }
                            len = nis.read(buf, count, buf.length - count);
                            if (len != -1) {
                                count += len;
                                if (supportPartialContent) {
                                    this.mReceivedBytes += len;
                                }
                            }
                            if (len == -1 || count >= lowWater) {
                                this.mEventHandler.data(buf, count);
                                count = 0;
                            }
                        }
                        if (nis != null) {
                            nis.close();
                        }
                    } catch (EOFException e2) {
                        if (count > 0) {
                            this.mEventHandler.data(buf, count);
                        }
                        if (nis != null) {
                            nis.close();
                        }
                    } catch (IOException e3) {
                        if (statusCode == 200 || statusCode == 206) {
                            if (supportPartialContent && count > 0) {
                                this.mEventHandler.data(buf, count);
                            }
                            throw e3;
                        }
                        if (nis != null) {
                            nis.close();
                        }
                    }
                }
            } catch (Throwable th) {
                if (nis != null) {
                    nis.close();
                }
                throw th;
            }
        }
        this.mConnection.setCanPersist(entity, statusLine.getProtocolVersion(), header.getConnectionType());
        this.mEventHandler.endData();
        complete();
    }

    synchronized void cancel() {
        this.mLoadingPaused = false;
        notify();
        this.mCancelled = true;
        if (this.mConnection != null) {
            this.mConnection.cancel();
        }
    }

    String getHostPort() {
        String myScheme = this.mHost.getSchemeName();
        int myPort = this.mHost.getPort();
        return ((myPort == 80 || !myScheme.equals("http")) && (myPort == 443 || !myScheme.equals("https"))) ? this.mHost.getHostName() : this.mHost.toHostString();
    }

    String getUri() {
        return (this.mProxyHost == null || this.mHost.getSchemeName().equals("https")) ? this.mPath : this.mHost.getSchemeName() + "://" + getHostPort() + this.mPath;
    }

    public String toString() {
        return this.mPath;
    }

    void reset() {
        this.mHttpRequest.removeHeaders("content-length");
        if (this.mBodyProvider != null) {
            try {
                this.mBodyProvider.reset();
            } catch (IOException e) {
            }
            setBodyProvider(this.mBodyProvider, this.mBodyLength);
        }
        if (this.mReceivedBytes > 0) {
            this.mFailCount = 0;
            HttpLog.v("*** Request.reset() to range:" + this.mReceivedBytes);
            this.mHttpRequest.setHeader("Range", "bytes=" + this.mReceivedBytes + NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
        }
    }

    void waitUntilComplete() {
        synchronized (this.mClientResource) {
            try {
                this.mClientResource.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    void complete() {
        synchronized (this.mClientResource) {
            this.mClientResource.notifyAll();
        }
    }

    private static boolean canResponseHaveBody(HttpRequest request, int status) {
        return ("HEAD".equalsIgnoreCase(request.getRequestLine().getMethod()) || status < 200 || status == 204 || status == 304) ? false : true;
    }

    private void setBodyProvider(InputStream bodyProvider, int bodyLength) {
        if (!bodyProvider.markSupported()) {
            throw new IllegalArgumentException("bodyProvider must support mark()");
        }
        bodyProvider.mark(Integer.MAX_VALUE);
        ((BasicHttpEntityEnclosingRequest) this.mHttpRequest).setEntity(new InputStreamEntity(bodyProvider, bodyLength));
    }

    public void handleSslErrorResponse(boolean proceed) {
        HttpsConnection connection = (HttpsConnection) this.mConnection;
        if (connection != null) {
            connection.restartConnection(proceed);
        }
    }

    void error(int errorId, int resourceId) {
        this.mEventHandler.error(errorId, this.mConnection.mContext.getText(resourceId).toString());
    }
}
