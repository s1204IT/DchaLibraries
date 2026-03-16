package java.net;

import java.security.BasicPermission;
import java.security.Permission;

public final class NetPermission extends BasicPermission {
    public NetPermission(String name) {
        super("");
    }

    public NetPermission(String name, String actions) {
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
