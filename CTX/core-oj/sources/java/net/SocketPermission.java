package java.net;

import java.io.Serializable;
import java.security.Permission;

public final class SocketPermission extends Permission implements Serializable {
    public SocketPermission(String str, String str2) {
        super("");
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
