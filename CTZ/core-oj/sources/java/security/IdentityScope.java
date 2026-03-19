package java.security;

import java.util.Enumeration;

@Deprecated
public abstract class IdentityScope extends Identity {
    private static IdentityScope scope = null;
    private static final long serialVersionUID = -2337346281189773310L;

    public abstract void addIdentity(Identity identity) throws KeyManagementException;

    public abstract Identity getIdentity(String str);

    public abstract Identity getIdentity(PublicKey publicKey);

    public abstract Enumeration<Identity> identities();

    public abstract void removeIdentity(Identity identity) throws KeyManagementException;

    public abstract int size();

    private static void initializeSystemScope() {
        String str = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return Security.getProperty("system.scope");
            }
        });
        if (str == null) {
            return;
        }
        try {
            scope = (IdentityScope) Class.forName(str).newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected IdentityScope() {
        this("restoring...");
    }

    public IdentityScope(String str) {
        super(str);
    }

    public IdentityScope(String str, IdentityScope identityScope) throws KeyManagementException {
        super(str, identityScope);
    }

    public static IdentityScope getSystemScope() {
        if (scope == null) {
            initializeSystemScope();
        }
        return scope;
    }

    protected static void setSystemScope(IdentityScope identityScope) {
        check("setSystemScope");
        scope = identityScope;
    }

    public Identity getIdentity(Principal principal) {
        return getIdentity(principal.getName());
    }

    @Override
    public String toString() {
        return super.toString() + "[" + size() + "]";
    }

    private static void check(String str) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSecurityAccess(str);
        }
    }
}
