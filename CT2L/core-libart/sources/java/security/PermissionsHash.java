package java.security;

import java.util.Enumeration;
import java.util.Hashtable;

final class PermissionsHash extends PermissionCollection {
    private static final long serialVersionUID = -8491988220802933440L;
    private final Hashtable perms = new Hashtable();

    PermissionsHash() {
    }

    @Override
    public void add(Permission permission) {
        this.perms.put(permission, permission);
    }

    @Override
    public Enumeration elements() {
        return this.perms.elements();
    }

    @Override
    public boolean implies(Permission permission) {
        Enumeration elements = elements();
        while (elements.hasMoreElements()) {
            if (((Permission) elements.nextElement()).implies(permission)) {
                return true;
            }
        }
        return false;
    }
}
