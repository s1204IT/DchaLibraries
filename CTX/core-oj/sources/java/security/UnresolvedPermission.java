package java.security;

import java.io.Serializable;

public final class UnresolvedPermission extends Permission implements Serializable {
    public UnresolvedPermission(String str, String str2, String str3, java.security.cert.Certificate[] certificateArr) {
        super("");
    }

    @Override
    public boolean implies(Permission permission) {
        return false;
    }

    @Override
    public String getActions() {
        return null;
    }

    public String getUnresolvedType() {
        return null;
    }

    public String getUnresolvedName() {
        return null;
    }

    public String getUnresolvedActions() {
        return null;
    }

    public java.security.cert.Certificate[] getUnresolvedCerts() {
        return null;
    }
}
