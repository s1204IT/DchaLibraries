package org.apache.http.auth;

import java.security.Principal;
import org.apache.http.util.LangUtils;

@Deprecated
public final class BasicUserPrincipal implements Principal {
    private final String username;

    public BasicUserPrincipal(String str) {
        if (str == null) {
            throw new IllegalArgumentException("User name may not be null");
        }
        this.username = str;
    }

    @Override
    public String getName() {
        return this.username;
    }

    @Override
    public int hashCode() {
        return LangUtils.hashCode(17, this.username);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BasicUserPrincipal) || !LangUtils.equals(this.username, obj.username)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "[principal: " + this.username + "]";
    }
}
