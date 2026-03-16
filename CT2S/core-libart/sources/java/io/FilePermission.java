package java.io;

import java.security.Permission;

public final class FilePermission extends Permission implements Serializable {
    public FilePermission(String path, String actions) {
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
