package java.security;

public final class SecurityPermission extends BasicPermission {
    public SecurityPermission(String name) {
        super("");
    }

    public SecurityPermission(String name, String action) {
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
