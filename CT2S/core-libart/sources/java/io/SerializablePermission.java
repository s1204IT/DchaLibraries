package java.io;

import java.security.BasicPermission;
import java.security.Permission;

public final class SerializablePermission extends BasicPermission {
    public SerializablePermission(String permissionName) {
        super("");
    }

    public SerializablePermission(String name, String actions) {
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
