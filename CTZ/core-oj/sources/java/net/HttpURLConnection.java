package java.net;

import java.io.IOException;
import java.io.InputStream;
import java.security.Permission;
import java.util.Date;
import sun.security.util.SecurityConstants;

public abstract class HttpURLConnection extends URLConnection {
    private static final int DEFAULT_CHUNK_SIZE = 4096;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_INTERNAL_ERROR = 500;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_OK = 200;
    public static final int HTTP_PARTIAL = 206;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_SEE_OTHER = 303;

    @Deprecated
    public static final int HTTP_SERVER_ERROR = 500;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;
    public static final int HTTP_USE_PROXY = 305;
    public static final int HTTP_VERSION = 505;
    private static boolean followRedirects = true;
    private static final String[] methods = {"GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"};
    protected int chunkLength;
    protected int fixedContentLength;
    protected long fixedContentLengthLong;
    protected boolean instanceFollowRedirects;
    protected String method;
    protected int responseCode;
    protected String responseMessage;

    public abstract void disconnect();

    public abstract boolean usingProxy();

    @Override
    public String getHeaderFieldKey(int i) {
        return null;
    }

    public void setFixedLengthStreamingMode(int i) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        if (this.chunkLength != -1) {
            throw new IllegalStateException("Chunked encoding streaming mode set");
        }
        if (i < 0) {
            throw new IllegalArgumentException("invalid content length");
        }
        this.fixedContentLength = i;
    }

    public void setFixedLengthStreamingMode(long j) {
        if (this.connected) {
            throw new IllegalStateException("Already connected");
        }
        if (this.chunkLength != -1) {
            throw new IllegalStateException("Chunked encoding streaming mode set");
        }
        if (j < 0) {
            throw new IllegalArgumentException("invalid content length");
        }
        this.fixedContentLengthLong = j;
    }

    public void setChunkedStreamingMode(int i) {
        if (this.connected) {
            throw new IllegalStateException("Can't set streaming mode: already connected");
        }
        if (this.fixedContentLength != -1 || this.fixedContentLengthLong != -1) {
            throw new IllegalStateException("Fixed length streaming mode set");
        }
        if (i <= 0) {
            i = 4096;
        }
        this.chunkLength = i;
    }

    @Override
    public String getHeaderField(int i) {
        return null;
    }

    protected HttpURLConnection(URL url) {
        super(url);
        this.method = "GET";
        this.chunkLength = -1;
        this.fixedContentLength = -1;
        this.fixedContentLengthLong = -1L;
        this.responseCode = -1;
        this.responseMessage = null;
        this.instanceFollowRedirects = followRedirects;
    }

    public static void setFollowRedirects(boolean z) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSetFactory();
        }
        followRedirects = z;
    }

    public static boolean getFollowRedirects() {
        return followRedirects;
    }

    public void setInstanceFollowRedirects(boolean z) {
        this.instanceFollowRedirects = z;
    }

    public boolean getInstanceFollowRedirects() {
        return this.instanceFollowRedirects;
    }

    public void setRequestMethod(String str) throws ProtocolException {
        SecurityManager securityManager;
        if (this.connected) {
            throw new ProtocolException("Can't reset method: already connected");
        }
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].equals(str)) {
                if (str.equals("TRACE") && (securityManager = System.getSecurityManager()) != null) {
                    securityManager.checkPermission(new NetPermission("allowHttpTrace"));
                }
                this.method = str;
                return;
            }
        }
        throw new ProtocolException("Invalid HTTP method: " + str);
    }

    public String getRequestMethod() {
        return this.method;
    }

    public int getResponseCode() throws IOException {
        int iIndexOf;
        if (this.responseCode != -1) {
            return this.responseCode;
        }
        Exception e = null;
        try {
            getInputStream();
        } catch (Exception e2) {
            e = e2;
        }
        String headerField = getHeaderField(0);
        if (headerField == null) {
            if (e == null) {
                return -1;
            }
            if (e instanceof RuntimeException) {
                throw ((RuntimeException) e);
            }
            throw ((IOException) e);
        }
        if (headerField.startsWith("HTTP/1.") && (iIndexOf = headerField.indexOf(32)) > 0) {
            int i = iIndexOf + 1;
            int iIndexOf2 = headerField.indexOf(32, i);
            if (iIndexOf2 > 0 && iIndexOf2 < headerField.length()) {
                this.responseMessage = headerField.substring(iIndexOf2 + 1);
            }
            if (iIndexOf2 < 0) {
                iIndexOf2 = headerField.length();
            }
            try {
                this.responseCode = Integer.parseInt(headerField.substring(i, iIndexOf2));
                return this.responseCode;
            } catch (NumberFormatException e3) {
            }
        }
        return -1;
    }

    public String getResponseMessage() throws IOException {
        getResponseCode();
        return this.responseMessage;
    }

    @Override
    public long getHeaderFieldDate(String str, long j) {
        String headerField = getHeaderField(str);
        try {
            if (headerField.indexOf("GMT") == -1) {
                headerField = headerField + " GMT";
            }
            return Date.parse(headerField);
        } catch (Exception e) {
            return j;
        }
    }

    @Override
    public Permission getPermission() throws IOException {
        int port = this.url.getPort();
        if (port < 0) {
            port = 80;
        }
        return new SocketPermission(this.url.getHost() + ":" + port, SecurityConstants.SOCKET_CONNECT_ACTION);
    }

    public InputStream getErrorStream() {
        return null;
    }
}
