package java.security;

import java.io.Serializable;

public abstract class Permission implements Guard, Serializable {
    private String name;

    public abstract String getActions();

    public abstract boolean implies(Permission permission);

    public Permission(String str) {
        this.name = str;
    }

    @Override
    public void checkGuard(Object obj) throws SecurityException {
    }

    public final String getName() {
        return this.name;
    }

    public PermissionCollection newPermissionCollection() {
        return new Permissions();
    }
}
