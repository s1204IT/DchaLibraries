package javax.security.auth;

import java.security.Permission;

public final class PrivateCredentialPermission extends Permission {
    public PrivateCredentialPermission(String name, String action) {
        super("");
    }

    public String[][] getPrincipals() {
        return (String[][]) null;
    }

    public String getCredentialClass() {
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
