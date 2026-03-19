package java.security;

import java.io.Serializable;

public abstract class BasicPermission extends Permission implements Serializable {
    public BasicPermission(String str) {
        super("");
    }

    public BasicPermission(String str, String str2) {
        super("");
    }

    @Override
    public boolean implies(Permission permission) {
        return true;
    }

    @Override
    public String getActions() {
        return "";
    }
}
