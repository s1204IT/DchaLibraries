package java.security;

public abstract class Policy {
    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION = new AllPermissionCollection();

    public interface Parameters {
    }

    public static Policy getInstance(String type, Parameters params) throws NoSuchAlgorithmException {
        return null;
    }

    public static Policy getInstance(String type, Parameters params, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        return null;
    }

    public static Policy getInstance(String type, Parameters params, Provider provider) throws NoSuchAlgorithmException {
        return null;
    }

    public Parameters getParameters() {
        return null;
    }

    public Provider getProvider() {
        return null;
    }

    public String getType() {
        return null;
    }

    public PermissionCollection getPermissions(CodeSource cs) {
        return null;
    }

    public void refresh() {
    }

    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return null;
    }

    public boolean implies(ProtectionDomain domain, Permission permission) {
        return true;
    }

    public static Policy getPolicy() {
        return null;
    }

    public static void setPolicy(Policy policy) {
    }
}
