package java.security;

import java.util.Enumeration;

public abstract class Policy {
    public static final PermissionCollection UNSUPPORTED_EMPTY_COLLECTION = new UnsupportedEmptyCollection();

    public interface Parameters {
    }

    public static Policy getPolicy() {
        return null;
    }

    public static void setPolicy(Policy policy) {
    }

    public static Policy getInstance(String str, Parameters parameters) throws NoSuchAlgorithmException {
        return null;
    }

    public static Policy getInstance(String str, Parameters parameters, String str2) throws NoSuchAlgorithmException, NoSuchProviderException {
        return null;
    }

    public static Policy getInstance(String str, Parameters parameters, Provider provider) throws NoSuchAlgorithmException {
        return null;
    }

    public Provider getProvider() {
        return null;
    }

    public String getType() {
        return null;
    }

    public Parameters getParameters() {
        return null;
    }

    public PermissionCollection getPermissions(CodeSource codeSource) {
        return null;
    }

    public PermissionCollection getPermissions(ProtectionDomain protectionDomain) {
        return null;
    }

    public boolean implies(ProtectionDomain protectionDomain, Permission permission) {
        return true;
    }

    public void refresh() {
    }

    private static class UnsupportedEmptyCollection extends PermissionCollection {
        @Override
        public void add(Permission permission) {
        }

        @Override
        public boolean implies(Permission permission) {
            return true;
        }

        @Override
        public Enumeration<Permission> elements() {
            return null;
        }
    }
}
