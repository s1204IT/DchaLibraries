package javax.security.auth;

import java.security.Permission;
import java.security.Principal;
import java.util.Set;

public final class PrivateCredentialPermission extends Permission {
    PrivateCredentialPermission(String str, Set<Principal> set) {
        super("");
    }

    public PrivateCredentialPermission(String str, String str2) {
        super("");
    }

    public String getCredentialClass() {
        return null;
    }

    public String[][] getPrincipals() {
        return null;
    }

    @Override
    public boolean implies(Permission permission) {
        return true;
    }

    @Override
    public String getActions() {
        return null;
    }
}
