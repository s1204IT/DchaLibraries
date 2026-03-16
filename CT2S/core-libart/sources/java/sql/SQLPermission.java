package java.sql;

import java.io.Serializable;
import java.security.BasicPermission;
import java.security.Guard;
import java.security.Permission;

public final class SQLPermission extends BasicPermission implements Guard, Serializable {
    public SQLPermission(String name) {
        super("");
    }

    public SQLPermission(String name, String actions) {
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
