package java.lang;

import java.security.BasicPermission;
import java.security.Permission;

public final class RuntimePermission extends BasicPermission {
    public RuntimePermission(String permissionName) {
        super("");
    }

    public RuntimePermission(String name, String actions) {
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
