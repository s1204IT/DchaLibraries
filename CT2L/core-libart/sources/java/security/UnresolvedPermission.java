package java.security;

import java.io.Serializable;

public final class UnresolvedPermission extends Permission implements Serializable {
    public UnresolvedPermission(String type, String name, String actions, java.security.cert.Certificate[] certs) {
        super("");
    }

    public String getUnresolvedName() {
        return null;
    }

    public String getUnresolvedActions() {
        return null;
    }

    public String getUnresolvedType() {
        return null;
    }

    public java.security.cert.Certificate[] getUnresolvedCerts() {
        return null;
    }

    @Override
    public String getActions() {
        return null;
    }

    @Override
    public boolean implies(Permission permission) {
        return true;
    }
}
