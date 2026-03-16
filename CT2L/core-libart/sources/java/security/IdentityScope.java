package java.security;

import java.util.Enumeration;

@Deprecated
public abstract class IdentityScope extends Identity {
    private static final long serialVersionUID = -2337346281189773310L;
    private static IdentityScope systemScope;

    public abstract void addIdentity(Identity identity) throws KeyManagementException;

    public abstract Identity getIdentity(String str);

    public abstract Identity getIdentity(PublicKey publicKey);

    public abstract Enumeration<Identity> identities();

    public abstract void removeIdentity(Identity identity) throws KeyManagementException;

    public abstract int size();

    protected IdentityScope() {
    }

    public IdentityScope(String name) {
        super(name);
    }

    public IdentityScope(String name, IdentityScope scope) throws KeyManagementException {
        super(name, scope);
    }

    public static IdentityScope getSystemScope() {
        String className;
        if (systemScope == null && (className = Security.getProperty("system.scope")) != null) {
            try {
                systemScope = (IdentityScope) Class.forName(className).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return systemScope;
    }

    protected static void setSystemScope(IdentityScope scope) {
        systemScope = scope;
    }

    public Identity getIdentity(Principal principal) {
        return getIdentity(principal.getName());
    }

    @Override
    public String toString() {
        return super.toString() + "[" + size() + "]";
    }
}
