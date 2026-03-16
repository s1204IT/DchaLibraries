package java.security;

@Deprecated
public abstract class Signer extends Identity {
    private static final long serialVersionUID = -1763464102261361480L;
    private PrivateKey privateKey;

    protected Signer() {
    }

    public Signer(String name) {
        super(name);
    }

    public Signer(String name, IdentityScope scope) throws KeyManagementException {
        super(name, scope);
    }

    public PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    public final void setKeyPair(KeyPair pair) throws InvalidParameterException, KeyException {
        if (pair == null) {
            throw new NullPointerException("pair == null");
        }
        if (pair.getPrivate() == null || pair.getPublic() == null) {
            throw new InvalidParameterException();
        }
        setPublicKey(pair.getPublic());
        this.privateKey = pair.getPrivate();
    }

    @Override
    public String toString() {
        String s = "[Signer]" + getName();
        if (getScope() != null) {
            return s + '[' + getScope().toString() + ']';
        }
        return s;
    }
}
