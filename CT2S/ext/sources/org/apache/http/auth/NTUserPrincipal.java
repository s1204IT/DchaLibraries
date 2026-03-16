package org.apache.http.auth;

import java.security.Principal;
import java.util.Locale;
import org.apache.http.util.LangUtils;

@Deprecated
public class NTUserPrincipal implements Principal {
    private final String domain;
    private final String ntname;
    private final String username;

    public NTUserPrincipal(String domain, String username) {
        if (username == null) {
            throw new IllegalArgumentException("User name may not be null");
        }
        this.username = username;
        if (domain != null) {
            this.domain = domain.toUpperCase(Locale.ENGLISH);
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
        int hash = LangUtils.hashCode(17, this.username);
        return LangUtils.hashCode(hash, this.domain);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof NTUserPrincipal)) {
            return false;
        }
        NTUserPrincipal that = (NTUserPrincipal) o;
        return LangUtils.equals(this.username, that.username) && LangUtils.equals(this.domain, that.domain);
    }

    @Override
    public String toString() {
        return this.ntname;
    }
}
