package org.apache.http.auth;

import java.security.Principal;
import org.apache.http.util.LangUtils;

@Deprecated
public final class BasicUserPrincipal implements Principal {
    private final String username;

    public BasicUserPrincipal(String username) {
        if (username == null) {
            throw new IllegalArgumentException("User name may not be null");
        }
        this.username = username;
    }

    @Override
    public String getName() {
        return this.username;
    }

    @Override
    public int hashCode() {
        int hash = LangUtils.hashCode(17, this.username);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return (obj instanceof BasicUserPrincipal) && LangUtils.equals(this.username, obj.username);
    }

    @Override
    public String toString() {
        return "[principal: " + this.username + "]";
    }
}
