package org.apache.http.auth;

import java.security.Principal;
import org.apache.http.util.LangUtils;

@Deprecated
public class UsernamePasswordCredentials implements Credentials {
    private final String password;
    private final BasicUserPrincipal principal;

    public UsernamePasswordCredentials(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Username:password string may not be null");
        }
        int iIndexOf = str.indexOf(58);
        if (iIndexOf >= 0) {
            this.principal = new BasicUserPrincipal(str.substring(0, iIndexOf));
            this.password = str.substring(iIndexOf + 1);
        } else {
            this.principal = new BasicUserPrincipal(str);
            this.password = null;
        }
    }

    public UsernamePasswordCredentials(String str, String str2) {
        if (str == null) {
            throw new IllegalArgumentException("Username may not be null");
        }
        this.principal = new BasicUserPrincipal(str);
        this.password = str2;
    }

    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }

    public String getUserName() {
        return this.principal.getName();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public int hashCode() {
        return this.principal.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof UsernamePasswordCredentials) || !LangUtils.equals(this.principal, obj.principal)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return this.principal.toString();
    }
}
