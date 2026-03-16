package javax.net.ssl;

import java.security.BasicPermission;
import java.security.Permission;

public final class SSLPermission extends BasicPermission {
    public SSLPermission(String name) {
        super("");
    }

    public SSLPermission(String name, String actions) {
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
