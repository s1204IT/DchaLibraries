package java.lang.reflect;

import java.security.BasicPermission;
import java.security.Permission;

public final class ReflectPermission extends BasicPermission {
    public ReflectPermission(String name) {
        super("");
    }

    public ReflectPermission(String name, String actions) {
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
