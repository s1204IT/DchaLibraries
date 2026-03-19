package android.net.http;

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
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
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

    Request(String str, HttpHost httpHost, HttpHost httpHost2, String str2, InputStream inputStream, int i, EventHandler eventHandler, Map<String, String> map) {
        this.mEventHandler = eventHandler;
        this.mHost = httpHost;
        this.mProxyHost = httpHost2;
        this.mPath = str2;
        this.mBodyProvider = inputStream;
        this.mBodyLength = i;
        if (inputStream == null && !HttpPost.METHOD_NAME.equalsIgnoreCase(str)) {
            this.mHttpRequest = new BasicHttpRequest(str, getUri());
        } else {
            this.mHttpRequest = new BasicHttpEntityEnclosingRequest(str, getUri());
            if (inputStream != null) {
                setBodyProvider(inputStream, i);
            }
        }
        addHeader("Host", getHostPort());
        addHeader(ACCEPT_ENCODING_HEADER, "gzip");
        addHeaders(map);
    }

    synchronized void setLoadingPaused(boolean z) {
        this.mLoadingPaused = z;
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

    void addHeader(String str, String str2) {
        if (str == null) {
            HttpLog.e("Null http header name");
            throw new NullPointerException("Null http header name");
        }
        if (str2 == null || str2.length() == 0) {
            String str3 = "Null or empty value for header \"" + str + "\"";
            HttpLog.e(str3);
            throw new RuntimeException(str3);
        }
        this.mHttpRequest.addHeader(str, str2);
    }

    void addHeaders(Map<String, String> map) {
        if (map == null) {
            return;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            addHeader(entry.getKey(), entry.getValue());
        }
    }

    void sendRequest(AndroidHttpClientConnection androidHttpClientConnection) throws org.apache.http.HttpException, IOException {
        if (this.mCancelled) {
            return;
        }
        requestContentProcessor.process(this.mHttpRequest, this.mConnection.getHttpContext());
        androidHttpClientConnection.sendRequestHeader(this.mHttpRequest);
        if (this.mHttpRequest instanceof HttpEntityEnclosingRequest) {
            androidHttpClientConnection.sendRequestEntity((HttpEntityEnclosingRequest) this.mHttpRequest);
        }
    }

    void readResponse(AndroidHttpClientConnection androidHttpClientConnection) throws Throwable {
        StatusLine responseHeader;
        int statusCode;
        InputStream gZIPInputStream;
        byte[] buf;
        if (this.mCancelled) {
            return;
        }
        androidHttpClientConnection.flush();
        Headers headers = new Headers();
        do {
            responseHeader = androidHttpClientConnection.parseResponseHeader(headers);
            statusCode = responseHeader.getStatusCode();
        } while (statusCode < 200);
        ProtocolVersion protocolVersion = responseHeader.getProtocolVersion();
        this.mEventHandler.status(protocolVersion.getMajor(), protocolVersion.getMinor(), statusCode, responseHeader.getReasonPhrase());
        this.mEventHandler.headers(headers);
        InputStream inputStream = null;
        HttpEntity httpEntityReceiveResponseEntity = canResponseHaveBody(this.mHttpRequest, statusCode) ? androidHttpClientConnection.receiveResponseEntity(headers) : null;
        boolean zEqualsIgnoreCase = "bytes".equalsIgnoreCase(headers.getAcceptRanges());
        if (httpEntityReceiveResponseEntity != null) {
            InputStream content = httpEntityReceiveResponseEntity.getContent();
            Header contentEncoding = httpEntityReceiveResponseEntity.getContentEncoding();
            int i = 0;
            try {
                if (contentEncoding != null) {
                    try {
                        gZIPInputStream = contentEncoding.getValue().equals("gzip") ? new GZIPInputStream(content) : content;
                        try {
                            try {
                                buf = this.mConnection.getBuf();
                            } catch (Throwable th) {
                                th = th;
                                if (gZIPInputStream != null) {
                                    gZIPInputStream.close();
                                }
                                throw th;
                            }
                        } catch (EOFException e) {
                            buf = null;
                        } catch (IOException e2) {
                            e = e2;
                            buf = null;
                        }
                    } catch (EOFException e3) {
                        buf = null;
                        if (i > 0) {
                        }
                        if (inputStream != null) {
                        }
                        this.mConnection.setCanPersist(httpEntityReceiveResponseEntity, responseHeader.getProtocolVersion(), headers.getConnectionType());
                        this.mEventHandler.endData();
                        complete();
                    } catch (IOException e4) {
                        e = e4;
                        buf = null;
                        if (statusCode != 200) {
                        }
                        if (zEqualsIgnoreCase) {
                            this.mEventHandler.data(buf, i);
                        }
                        throw e;
                    }
                    try {
                        int length = buf.length / 2;
                        int i2 = 0;
                        int i3 = 0;
                        while (i2 != -1) {
                            try {
                                synchronized (this) {
                                    while (this.mLoadingPaused) {
                                        try {
                                            wait();
                                        } catch (InterruptedException e5) {
                                            HttpLog.e("Interrupted exception whilst network thread paused at WebCore's request. " + e5.getMessage());
                                        }
                                    }
                                }
                                i2 = gZIPInputStream.read(buf, i3, buf.length - i3);
                                if (i2 != -1) {
                                    i3 += i2;
                                    if (zEqualsIgnoreCase) {
                                        this.mReceivedBytes += i2;
                                    }
                                }
                                if (i2 == -1 || i3 >= length) {
                                    this.mEventHandler.data(buf, i3);
                                    i3 = 0;
                                }
                            } catch (EOFException e6) {
                                inputStream = gZIPInputStream;
                                i = i3;
                                if (i > 0) {
                                    this.mEventHandler.data(buf, i);
                                }
                                if (inputStream != null) {
                                    inputStream.close();
                                }
                                this.mConnection.setCanPersist(httpEntityReceiveResponseEntity, responseHeader.getProtocolVersion(), headers.getConnectionType());
                                this.mEventHandler.endData();
                                complete();
                            } catch (IOException e7) {
                                e = e7;
                                inputStream = gZIPInputStream;
                                i = i3;
                                if (statusCode != 200 || statusCode == 206) {
                                    if (zEqualsIgnoreCase && i > 0) {
                                        this.mEventHandler.data(buf, i);
                                    }
                                    throw e;
                                }
                                if (inputStream != null) {
                                }
                                this.mConnection.setCanPersist(httpEntityReceiveResponseEntity, responseHeader.getProtocolVersion(), headers.getConnectionType());
                                this.mEventHandler.endData();
                                complete();
                            }
                        }
                        if (gZIPInputStream != null) {
                            gZIPInputStream.close();
                        }
                    } catch (EOFException e8) {
                        inputStream = gZIPInputStream;
                        if (i > 0) {
                        }
                        if (inputStream != null) {
                        }
                        this.mConnection.setCanPersist(httpEntityReceiveResponseEntity, responseHeader.getProtocolVersion(), headers.getConnectionType());
                        this.mEventHandler.endData();
                        complete();
                    } catch (IOException e9) {
                        e = e9;
                        inputStream = gZIPInputStream;
                        if (statusCode != 200) {
                        }
                        if (zEqualsIgnoreCase) {
                        }
                        throw e;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                gZIPInputStream = null;
            }
        }
        this.mConnection.setCanPersist(httpEntityReceiveResponseEntity, responseHeader.getProtocolVersion(), headers.getConnectionType());
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
        String schemeName = this.mHost.getSchemeName();
        int port = this.mHost.getPort();
        if ((port != 80 && schemeName.equals(HttpHost.DEFAULT_SCHEME_NAME)) || (port != 443 && schemeName.equals("https"))) {
            return this.mHost.toHostString();
        }
        return this.mHost.getHostName();
    }

    String getUri() {
        if (this.mProxyHost == null || this.mHost.getSchemeName().equals("https")) {
            return this.mPath;
        }
        return this.mHost.getSchemeName() + "://" + getHostPort() + this.mPath;
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
            this.mHttpRequest.setHeader("Range", "bytes=" + this.mReceivedBytes + "-");
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

    private static boolean canResponseHaveBody(HttpRequest httpRequest, int i) {
        return (HttpHead.METHOD_NAME.equalsIgnoreCase(httpRequest.getRequestLine().getMethod()) || i < 200 || i == 204 || i == 304) ? false : true;
    }

    private void setBodyProvider(InputStream inputStream, int i) {
        if (!inputStream.markSupported()) {
            throw new IllegalArgumentException("bodyProvider must support mark()");
        }
        inputStream.mark(Integer.MAX_VALUE);
        ((BasicHttpEntityEnclosingRequest) this.mHttpRequest).setEntity(new InputStreamEntity(inputStream, i));
    }

    public void handleSslErrorResponse(boolean z) {
        HttpsConnection httpsConnection = (HttpsConnection) this.mConnection;
        if (httpsConnection != null) {
            httpsConnection.restartConnection(z);
        }
    }

    void error(int i, String str) {
        this.mEventHandler.error(i, str);
    }
}
