package java.security;

public final class AccessControlContext {
    public AccessControlContext(ProtectionDomain[] protectionDomainArr) {
    }

    public AccessControlContext(AccessControlContext accessControlContext, DomainCombiner domainCombiner) {
    }

    public DomainCombiner getDomainCombiner() {
        return null;
    }

    public void checkPermission(Permission permission) throws AccessControlException {
    }
}
