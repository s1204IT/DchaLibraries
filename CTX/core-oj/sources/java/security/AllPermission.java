package java.security;

public final class AllPermission extends Permission {
    public AllPermission() {
        super("");
    }

    public AllPermission(String str, String str2) {
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
