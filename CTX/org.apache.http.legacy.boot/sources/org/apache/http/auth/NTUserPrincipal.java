package org.apache.http.auth;

import java.security.Principal;
import java.util.Locale;
import org.apache.http.util.LangUtils;

@Deprecated
public class NTUserPrincipal implements Principal {
    private final String domain;
    private final String ntname;
    private final String username;

    public NTUserPrincipal(String str, String str2) {
        if (str2 == null) {
            throw new IllegalArgumentException("User name may not be null");
        }
        this.username = str2;
        if (str != null) {
            this.domain = str.toUpperCase(Locale.ENGLISH);
        } else {
            this.domain = null;
        }
        if (this.domain != null && this.domain.length() > 0) {
            this.ntname = this.domain + '/' + this.username;
            return;
        }
        this.ntname = this.username;
    }

    @Override
    public String getName() {
        return this.ntname;
    }

    public String getDomain() {
        return this.domain;
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public int hashCode() {
        return LangUtils.hashCode(LangUtils.hashCode(17, this.username), this.domain);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NTUserPrincipal) || !LangUtils.equals(this.username, obj.username) || !LangUtils.equals(this.domain, obj.domain)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.ntname;
    }
}
