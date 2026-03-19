package java.io;

import java.security.Permission;

public final class FilePermission extends Permission implements Serializable {
    public FilePermission(String str, String str2) {
        super(str);
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
