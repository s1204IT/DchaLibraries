package java.util;

import java.security.BasicPermission;
import java.security.Permission;

public final class PropertyPermission extends BasicPermission {
    public PropertyPermission(String name, String actions) {
        super("");
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
