package org.apache.http.auth;

import java.util.Locale;
import org.apache.http.util.LangUtils;

@Deprecated
public class AuthScope {
    public static final int ANY_PORT = -1;
    private final String host;
    private final int port;
    private final String realm;
    private final String scheme;
    public static final String ANY_HOST = null;
    public static final String ANY_REALM = null;
    public static final String ANY_SCHEME = null;
    public static final AuthScope ANY = new AuthScope(ANY_HOST, -1, ANY_REALM, ANY_SCHEME);

    public AuthScope(String str, int i, String str2, String str3) {
        this.host = str == null ? ANY_HOST : str.toLowerCase(Locale.ENGLISH);
        this.port = i < 0 ? -1 : i;
        this.realm = str2 == null ? ANY_REALM : str2;
        this.scheme = str3 == null ? ANY_SCHEME : str3.toUpperCase(Locale.ENGLISH);
    }

    public AuthScope(String str, int i, String str2) {
        this(str, i, str2, ANY_SCHEME);
    }

    public AuthScope(String str, int i) {
        this(str, i, ANY_REALM, ANY_SCHEME);
    }

    public AuthScope(AuthScope authScope) {
        if (authScope == null) {
            throw new IllegalArgumentException("Scope may not be null");
        }
        this.host = authScope.getHost();
        this.port = authScope.getPort();
        this.realm = authScope.getRealm();
        this.scheme = authScope.getScheme();
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public String getRealm() {
        return this.realm;
    }

    public String getScheme() {
        return this.scheme;
    }

    public int match(AuthScope authScope) {
        int i;
        if (LangUtils.equals(this.scheme, authScope.scheme)) {
            i = 1;
        } else {
            if (this.scheme != ANY_SCHEME && authScope.scheme != ANY_SCHEME) {
                return -1;
            }
            i = 0;
        }
        if (LangUtils.equals(this.realm, authScope.realm)) {
            i += 2;
        } else if (this.realm != ANY_REALM && authScope.realm != ANY_REALM) {
            return -1;
        }
        if (this.port == authScope.port) {
            i += 4;
        } else if (this.port != -1 && authScope.port != -1) {
            return -1;
        }
        if (LangUtils.equals(this.host, authScope.host)) {
            return i + 8;
        }
        if (this.host == ANY_HOST || authScope.host == ANY_HOST) {
            return i;
        }
        return -1;
    }

    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AuthScope)) {
            return super.equals(obj);
        }
        if (!LangUtils.equals(this.host, obj.host) || this.port != obj.port || !LangUtils.equals(this.realm, obj.realm) || !LangUtils.equals(this.scheme, obj.scheme)) {
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.scheme != null) {
            stringBuffer.append(this.scheme.toUpperCase(Locale.ENGLISH));
            stringBuffer.append(' ');
        }
        if (this.realm != null) {
            stringBuffer.append('\'');
            stringBuffer.append(this.realm);
            stringBuffer.append('\'');
        } else {
            stringBuffer.append("<any realm>");
        }
        if (this.host != null) {
            stringBuffer.append('@');
            stringBuffer.append(this.host);
            if (this.port >= 0) {
                stringBuffer.append(':');
                stringBuffer.append(this.port);
            }
        }
        return stringBuffer.toString();
    }

    public int hashCode() {
        return LangUtils.hashCode(LangUtils.hashCode(LangUtils.hashCode(LangUtils.hashCode(17, this.host), this.port), this.realm), this.scheme);
    }
}
