package javax.security.auth;

import java.security.BasicPermission;
import java.security.Permission;

public final class AuthPermission extends BasicPermission {
    public AuthPermission(String name) {
        super("");
    }

    public AuthPermission(String name, String actions) {
        super("", "");
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
