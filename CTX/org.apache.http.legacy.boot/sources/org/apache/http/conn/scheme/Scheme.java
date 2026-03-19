package org.apache.http.conn.scheme;

import java.util.Locale;
import org.apache.http.util.LangUtils;

@Deprecated
public final class Scheme {
    private final int defaultPort;
    private final boolean layered;
    private final String name;
    private final SocketFactory socketFactory;
    private String stringRep;

    public Scheme(String str, SocketFactory socketFactory, int i) {
        if (str == null) {
            throw new IllegalArgumentException("Scheme name may not be null");
        }
        if (socketFactory == null) {
            throw new IllegalArgumentException("Socket factory may not be null");
        }
        if (i <= 0 || i > 65535) {
            throw new IllegalArgumentException("Port is invalid: " + i);
        }
        this.name = str.toLowerCase(Locale.ENGLISH);
        this.socketFactory = socketFactory;
        this.defaultPort = i;
        this.layered = socketFactory instanceof LayeredSocketFactory;
    }

    public final int getDefaultPort() {
        return this.defaultPort;
    }

    public final SocketFactory getSocketFactory() {
        return this.socketFactory;
    }

    public final String getName() {
        return this.name;
    }

    public final boolean isLayered() {
        return this.layered;
    }

    public final int resolvePort(int i) {
        return (i <= 0 || i > 65535) ? this.defaultPort : i;
    }

    public final String toString() {
        if (this.stringRep == null) {
            this.stringRep = this.name + ':' + Integer.toString(this.defaultPort);
        }
        return this.stringRep;
    }

    public final boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Scheme) || !this.name.equals(obj.name) || this.defaultPort != obj.defaultPort || this.layered != obj.layered || !this.socketFactory.equals(obj.socketFactory)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return LangUtils.hashCode(LangUtils.hashCode(LangUtils.hashCode(LangUtils.hashCode(17, this.defaultPort), this.name), this.layered), this.socketFactory);
    }
}
