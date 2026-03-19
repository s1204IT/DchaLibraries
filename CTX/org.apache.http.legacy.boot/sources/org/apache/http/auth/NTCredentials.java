package org.apache.http.auth;

import java.security.Principal;
import java.util.Locale;
import org.apache.http.util.LangUtils;

@Deprecated
public class NTCredentials implements Credentials {
    private final String password;
    private final NTUserPrincipal principal;
    private final String workstation;

    public NTCredentials(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Username:password string may not be null");
        }
        int iIndexOf = str.indexOf(58);
        if (iIndexOf >= 0) {
            String strSubstring = str.substring(0, iIndexOf);
            this.password = str.substring(iIndexOf + 1);
            str = strSubstring;
        } else {
            this.password = null;
        }
        int iIndexOf2 = str.indexOf(47);
        if (iIndexOf2 >= 0) {
            this.principal = new NTUserPrincipal(str.substring(0, iIndexOf2).toUpperCase(Locale.ENGLISH), str.substring(iIndexOf2 + 1));
        } else {
            this.principal = new NTUserPrincipal(null, str.substring(iIndexOf2 + 1));
        }
        this.workstation = null;
    }

    public NTCredentials(String str, String str2, String str3, String str4) {
        if (str == null) {
            throw new IllegalArgumentException("User name may not be null");
        }
        this.principal = new NTUserPrincipal(str4, str);
        this.password = str2;
        if (str3 != null) {
            this.workstation = str3.toUpperCase(Locale.ENGLISH);
        } else {
            this.workstation = null;
        }
    }

    @Override
    public Principal getUserPrincipal() {
        return this.principal;
    }

    public String getUserName() {
        return this.principal.getUsername();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    public String getDomain() {
        return this.principal.getDomain();
    }

    public String getWorkstation() {
        return this.workstation;
    }

    public int hashCode() {
        return LangUtils.hashCode(LangUtils.hashCode(17, this.principal), this.workstation);
    }

    public boolean equals(Object obj) {
        if (obj == 0) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NTCredentials) || !LangUtils.equals(this.principal, obj.principal) || !LangUtils.equals(this.workstation, obj.workstation)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return "[principal: " + this.principal + "][workstation: " + this.workstation + "]";
    }
}
