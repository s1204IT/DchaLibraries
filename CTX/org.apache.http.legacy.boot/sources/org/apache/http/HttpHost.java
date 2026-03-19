package org.apache.http;

import java.util.Locale;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.LangUtils;

@Deprecated
public final class HttpHost implements Cloneable {
    public static final String DEFAULT_SCHEME_NAME = "http";
    protected final String hostname;
    protected final String lcHostname;
    protected final int port;
    protected final String schemeName;

    public HttpHost(String str, int i, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("Host name may not be null");
        }
        this.hostname = str;
        this.lcHostname = str.toLowerCase(Locale.ENGLISH);
        if (str2 != null) {
            this.schemeName = str2.toLowerCase(Locale.ENGLISH);
        } else {
            this.schemeName = DEFAULT_SCHEME_NAME;
        }
        this.port = i;
    }

    public HttpHost(String str, int i) {
        this(str, i, null);
    }

    public HttpHost(String str) {
        this(str, -1, null);
    }

    public HttpHost(HttpHost httpHost) {
        this(httpHost.hostname, httpHost.port, httpHost.schemeName);
    }

    public String getHostName() {
        return this.hostname;
    }

    public int getPort() {
        return this.port;
    }

    public String getSchemeName() {
        return this.schemeName;
    }

    public String toURI() {
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(32);
        charArrayBuffer.append(this.schemeName);
        charArrayBuffer.append("://");
        charArrayBuffer.append(this.hostname);
        if (this.port != -1) {
            charArrayBuffer.append(':');
            charArrayBuffer.append(Integer.toString(this.port));
        }
        return charArrayBuffer.toString();
    }

    public String toHostString() {
        CharArrayBuffer charArrayBuffer = new CharArrayBuffer(32);
        charArrayBuffer.append(this.hostname);
        if (this.port != -1) {
            charArrayBuffer.append(':');
            charArrayBuffer.append(Integer.toString(this.port));
        }
        return charArrayBuffer.toString();
    }

    public String toString() {
        return toURI();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HttpHost)) {
            return false;
        }
        HttpHost httpHost = (HttpHost) obj;
        if (!this.lcHostname.equals(httpHost.lcHostname) || this.port != httpHost.port || !this.schemeName.equals(httpHost.schemeName)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return LangUtils.hashCode(LangUtils.hashCode(LangUtils.hashCode(17, this.lcHostname), this.port), this.schemeName);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
