package java.security;

import java.util.Enumeration;

final class AllPermissionCollection extends PermissionCollection {
    AllPermissionCollection() {
    }

    @Override
    public void add(Permission permission) {
    }

    @Override
    public Enumeration<Permission> elements() {
        return null;
    }

    @Override
    public boolean implies(Permission permission) {
        return true;
    }
}
