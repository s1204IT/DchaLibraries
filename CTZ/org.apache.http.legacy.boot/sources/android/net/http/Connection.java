package android.net.http;

import android.content.Context;
import android.os.SystemClock;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import javax.net.ssl.SSLHandshakeException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

abstract class Connection {
    private static final int DONE = 3;
    private static final int DRAIN = 2;
    private static final String HTTP_CONNECTION = "http.connection";
    private static final int MAX_PIPE = 3;
    private static final int MIN_PIPE = 2;
    private static final int READ = 1;
    private static final int RETRY_REQUEST_LIMIT = 2;
    private static final int SEND = 0;
    static final int SOCKET_TIMEOUT = 60000;
    private byte[] mBuf;
    Context mContext;
    HttpHost mHost;
    RequestFeeder mRequestFeeder;
    private static final String[] states = {"SEND", "READ", "DRAIN", "DONE"};
    private static int STATE_NORMAL = 0;
    private static int STATE_CANCEL_REQUESTED = 1;
    protected AndroidHttpClientConnection mHttpClientConnection = null;
    protected SslCertificate mCertificate = null;
    private int mActive = STATE_NORMAL;
    private boolean mCanPersist = false;
    private HttpContext mHttpContext = new BasicHttpContext(null);

    abstract void closeConnection();

    abstract String getScheme();

    abstract AndroidHttpClientConnection openConnection(Request request) throws IOException;

    protected Connection(Context context, HttpHost httpHost, RequestFeeder requestFeeder) {
        this.mContext = context;
        this.mHost = httpHost;
        this.mRequestFeeder = requestFeeder;
    }

    HttpHost getHost() {
        return this.mHost;
    }

    static Connection getConnection(Context context, HttpHost httpHost, HttpHost httpHost2, RequestFeeder requestFeeder) {
        if (httpHost.getSchemeName().equals(HttpHost.DEFAULT_SCHEME_NAME)) {
            return new HttpConnection(context, httpHost, requestFeeder);
        }
        return new HttpsConnection(context, httpHost, httpHost2, requestFeeder);
    }

    SslCertificate getCertificate() {
        return this.mCertificate;
    }

    void cancel() {
        this.mActive = STATE_CANCEL_REQUESTED;
        closeConnection();
    }

    void processRequests(Request request) throws Throwable {
        Request request2;
        LinkedList<Request> linkedList = new LinkedList<>();
        char c = 2;
        Request request3 = request;
        int i = 2;
        Exception exc = null;
        int i2 = 3;
        char c2 = 0;
        int i3 = 0;
        while (c2 != 3) {
            if (this.mActive == STATE_CANCEL_REQUESTED) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                }
                this.mActive = STATE_NORMAL;
            }
            int i4 = -7;
            switch (c2) {
                case 0:
                    if (linkedList.size() == i2) {
                        c2 = 1;
                    } else {
                        if (request3 == null) {
                            request2 = request3;
                            request3 = this.mRequestFeeder.getRequest(this.mHost);
                        } else {
                            request2 = null;
                        }
                        if (request3 != null) {
                            request3.setConnection(this);
                            if (request3.mCancelled) {
                                request3.complete();
                            } else if ((this.mHttpClientConnection != null && this.mHttpClientConnection.isOpen()) || openHttpConnection(request3)) {
                                request3.mEventHandler.certificate(this.mCertificate);
                                try {
                                    request3.sendRequest(this.mHttpClientConnection);
                                } catch (IOException e2) {
                                    exc = e2;
                                    i3 = -7;
                                } catch (IllegalStateException e3) {
                                    exc = e3;
                                    i3 = -7;
                                } catch (org.apache.http.HttpException e4) {
                                    exc = e4;
                                    i3 = -1;
                                }
                                if (exc == null) {
                                    linkedList.addLast(request3);
                                    if (!this.mCanPersist) {
                                        request3 = request2;
                                        c2 = 1;
                                    }
                                } else {
                                    if (httpFailure(request3, i3, exc) && !request3.mCancelled) {
                                        linkedList.addLast(request3);
                                    }
                                    c2 = clearPipe(linkedList) ? (char) 3 : (char) 0;
                                    request3 = request2;
                                    exc = null;
                                    i2 = 1;
                                    i = 1;
                                }
                            } else {
                                request3 = request2;
                                c2 = 3;
                            }
                            request3 = request2;
                        } else {
                            request3 = request2;
                            c2 = 2;
                        }
                    }
                    break;
                case 1:
                case 2:
                    boolean z = !this.mRequestFeeder.haveRequest(this.mHost);
                    int size = linkedList.size();
                    if (c2 != c && size < i && !z && this.mCanPersist) {
                        c2 = 0;
                    } else if (size != 0) {
                        Request requestRemoveFirst = linkedList.removeFirst();
                        try {
                            requestRemoveFirst.readResponse(this.mHttpClientConnection);
                            e = exc;
                            i4 = i3;
                        } catch (IOException e5) {
                            e = e5;
                        } catch (IllegalStateException e6) {
                            e = e6;
                        } catch (ParseException e7) {
                            e = e7;
                        }
                        if (e != null) {
                            if (httpFailure(requestRemoveFirst, i4, e) && !requestRemoveFirst.mCancelled) {
                                requestRemoveFirst.reset();
                                linkedList.addFirst(requestRemoveFirst);
                            }
                            this.mCanPersist = false;
                            e = null;
                        }
                        if (!this.mCanPersist) {
                            closeConnection();
                            this.mHttpContext.removeAttribute("http.connection");
                            clearPipe(linkedList);
                            exc = e;
                            c2 = 0;
                            i3 = i4;
                            i2 = 1;
                            i = 1;
                        } else {
                            exc = e;
                            i3 = i4;
                        }
                    } else {
                        c2 = z ? (char) 3 : (char) 0;
                    }
                    break;
            }
            c = 2;
        }
    }

    private boolean clearPipe(LinkedList<Request> linkedList) {
        boolean z;
        synchronized (this.mRequestFeeder) {
            z = true;
            while (!linkedList.isEmpty()) {
                this.mRequestFeeder.requeueRequest(linkedList.removeLast());
                z = false;
            }
            if (z) {
                z = !this.mRequestFeeder.haveRequest(this.mHost);
            }
        }
        return z;
    }

    private boolean openHttpConnection(Request request) {
        SystemClock.uptimeMillis();
        int i = -6;
        Exception e = null;
        try {
            this.mCertificate = null;
            this.mHttpClientConnection = openConnection(request);
        } catch (SSLConnectionClosedByUserException e2) {
            request.mFailCount = 2;
            return false;
        } catch (IllegalArgumentException e3) {
            e = e3;
            request.mFailCount = 2;
        } catch (UnknownHostException e4) {
            e = e4;
            i = -2;
        } catch (SSLHandshakeException e5) {
            e = e5;
            request.mFailCount = 2;
            i = -11;
        } catch (IOException e6) {
            e = e6;
        }
        if (this.mHttpClientConnection != null) {
            this.mHttpClientConnection.setSocketTimeout(SOCKET_TIMEOUT);
            this.mHttpContext.setAttribute("http.connection", this.mHttpClientConnection);
            i = 0;
            if (i == 0) {
                return true;
            }
            if (request.mFailCount < 2) {
                this.mRequestFeeder.requeueRequest(request);
                request.mFailCount++;
            } else {
                httpFailure(request, i, e);
            }
            return i == 0;
        }
        request.mFailCount = 2;
        return false;
    }

    private boolean httpFailure(Request request, int i, Exception exc) {
        String string;
        boolean z = true;
        int i2 = request.mFailCount + 1;
        request.mFailCount = i2;
        if (i2 >= 2) {
            z = false;
            if (i < 0) {
                string = getEventHandlerErrorString(i);
            } else {
                Throwable cause = exc.getCause();
                string = cause != null ? cause.toString() : exc.getMessage();
            }
            request.mEventHandler.error(i, string);
            request.complete();
        }
        closeConnection();
        this.mHttpContext.removeAttribute("http.connection");
        return z;
    }

    private static String getEventHandlerErrorString(int i) {
        switch (i) {
            case EventHandler.TOO_MANY_REQUESTS_ERROR:
                return "TOO_MANY_REQUESTS_ERROR";
            case EventHandler.FILE_NOT_FOUND_ERROR:
                return "FILE_NOT_FOUND_ERROR";
            case EventHandler.FILE_ERROR:
                return "FILE_ERROR";
            case EventHandler.ERROR_BAD_URL:
                return "ERROR_BAD_URL";
            case EventHandler.ERROR_FAILED_SSL_HANDSHAKE:
                return "ERROR_FAILED_SSL_HANDSHAKE";
            case EventHandler.ERROR_UNSUPPORTED_SCHEME:
                return "ERROR_UNSUPPORTED_SCHEME";
            case EventHandler.ERROR_REDIRECT_LOOP:
                return "ERROR_REDIRECT_LOOP";
            case EventHandler.ERROR_TIMEOUT:
                return "ERROR_TIMEOUT";
            case EventHandler.ERROR_IO:
                return "ERROR_IO";
            case EventHandler.ERROR_CONNECT:
                return "ERROR_CONNECT";
            case EventHandler.ERROR_PROXYAUTH:
                return "ERROR_PROXYAUTH";
            case EventHandler.ERROR_AUTH:
                return "ERROR_AUTH";
            case EventHandler.ERROR_UNSUPPORTED_AUTH_SCHEME:
                return "ERROR_UNSUPPORTED_AUTH_SCHEME";
            case -2:
                return "ERROR_LOOKUP";
            case -1:
                return "ERROR";
            case 0:
                return "OK";
            default:
                return "UNKNOWN_ERROR";
        }
    }

    HttpContext getHttpContext() {
        return this.mHttpContext;
    }

    private boolean keepAlive(HttpEntity httpEntity, ProtocolVersion protocolVersion, int i, HttpContext httpContext) {
        org.apache.http.HttpConnection httpConnection = (org.apache.http.HttpConnection) httpContext.getAttribute("http.connection");
        if (httpConnection != null && !httpConnection.isOpen()) {
            return false;
        }
        if ((httpEntity != null && httpEntity.getContentLength() < 0 && (!httpEntity.isChunked() || protocolVersion.lessEquals(HttpVersion.HTTP_1_0))) || i == 1) {
            return false;
        }
        if (i == 2) {
            return true;
        }
        return true ^ protocolVersion.lessEquals(HttpVersion.HTTP_1_0);
    }

    void setCanPersist(HttpEntity httpEntity, ProtocolVersion protocolVersion, int i) {
        this.mCanPersist = keepAlive(httpEntity, protocolVersion, i, this.mHttpContext);
    }

    void setCanPersist(boolean z) {
        this.mCanPersist = z;
    }

    boolean getCanPersist() {
        return this.mCanPersist;
    }

    public synchronized String toString() {
        return this.mHost.toString();
    }

    byte[] getBuf() {
        if (this.mBuf == null) {
            this.mBuf = new byte[8192];
        }
        return this.mBuf;
    }
}
