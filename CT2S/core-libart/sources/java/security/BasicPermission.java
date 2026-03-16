package java.security;

import java.io.Serializable;

public abstract class BasicPermission extends Permission implements Serializable {
    public BasicPermission(String name) {
        super("");
    }

    public BasicPermission(String name, String action) {
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
