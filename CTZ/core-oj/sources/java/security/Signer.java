package java.security;

@Deprecated
public abstract class Signer extends Identity {
    private static final long serialVersionUID = -1763464102261361480L;
    private PrivateKey privateKey;

    protected Signer() {
    }

    public Signer(String str) {
        super(str);
    }

    public Signer(String str, IdentityScope identityScope) throws KeyManagementException {
        super(str, identityScope);
    }

    public PrivateKey getPrivateKey() {
        check("getSignerPrivateKey");
        return this.privateKey;
    }

    public final void setKeyPair(KeyPair keyPair) throws InvalidParameterException, KeyException {
        check("setSignerKeyPair");
        final PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        if (publicKey == null || privateKey == null) {
            throw new InvalidParameterException();
        }
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws KeyManagementException {
                    Signer.this.setPublicKey(publicKey);
                    return null;
                }
            });
            this.privateKey = privateKey;
        } catch (PrivilegedActionException e) {
            throw ((KeyManagementException) e.getException());
        }
    }

    @Override
    String printKeys() {
        if (getPublicKey() != null && this.privateKey != null) {
            return "\tpublic and private keys initialized";
        }
        return "\tno keys";
    }

    @Override
    public String toString() {
        return "[Signer]" + super.toString();
    }

    private static void check(String str) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSecurityAccess(str);
        }
    }
}
