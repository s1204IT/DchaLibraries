package java.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Vector;
import libcore.util.Objects;

@Deprecated
public abstract class Identity implements Principal, Serializable {
    private static final long serialVersionUID = 3609922007826600659L;
    private Vector<Certificate> certificates;
    private String info;
    private String name;
    private PublicKey publicKey;
    private IdentityScope scope;

    protected Identity() {
        this.info = "no additional info";
    }

    public Identity(String name) {
        this.info = "no additional info";
        this.name = name;
    }

    public Identity(String name, IdentityScope scope) throws KeyManagementException {
        this(name);
        if (scope != null) {
            scope.addIdentity(this);
            this.scope = scope;
        }
    }

    public void addCertificate(Certificate certificate) throws KeyManagementException {
        PublicKey certPK = certificate.getPublicKey();
        if (this.publicKey != null) {
            if (!checkKeysEqual(this.publicKey, certPK)) {
                throw new KeyManagementException("Cert's public key does not match Identity's public key");
            }
        } else {
            this.publicKey = certPK;
        }
        if (this.certificates == null) {
            this.certificates = new Vector<>();
        }
        this.certificates.add(certificate);
    }

    private static boolean checkKeysEqual(PublicKey pk1, PublicKey pk2) {
        String format1 = pk1.getFormat();
        if (pk2 == null) {
            return false;
        }
        String format2 = pk2.getFormat();
        if ((format1 != null) ^ (format2 != null)) {
            return false;
        }
        if (format1 == null || format1.equals(format2)) {
            return Arrays.equals(pk1.getEncoded(), pk2.getEncoded());
        }
        return false;
    }

    public void removeCertificate(Certificate certificate) throws KeyManagementException {
        if (this.certificates != null) {
            if (!this.certificates.contains(certificate)) {
                throw new KeyManagementException("Certificate not found");
            }
            this.certificates.removeElement(certificate);
        }
    }

    public Certificate[] certificates() {
        if (this.certificates == null) {
            return new Certificate[0];
        }
        Certificate[] ret = new Certificate[this.certificates.size()];
        this.certificates.copyInto(ret);
        return ret;
    }

    protected boolean identityEquals(Identity identity) {
        if (!this.name.equals(identity.name)) {
            return false;
        }
        if (this.publicKey == null) {
            return identity.publicKey == null;
        }
        return checkKeysEqual(this.publicKey, identity.publicKey);
    }

    public String toString(boolean detailed) {
        String s = toString();
        if (detailed) {
            return s + " " + this.info;
        }
        return s;
    }

    public final IdentityScope getScope() {
        return this.scope;
    }

    public void setPublicKey(PublicKey key) throws KeyManagementException {
        Identity i;
        if (this.scope != null && key != null && (i = this.scope.getIdentity(key)) != null && i != this) {
            throw new KeyManagementException("key already used in scope");
        }
        this.publicKey = key;
        this.certificates = null;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return this.info;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Identity)) {
            return false;
        }
        Identity i = (Identity) obj;
        if (Objects.equal(this.name, i.name) && Objects.equal(this.scope, i.scope)) {
            return true;
        }
        return identityEquals(i);
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        int hash = this.name != null ? 0 + this.name.hashCode() : 0;
        if (this.scope != null) {
            return hash + this.scope.hashCode();
        }
        return hash;
    }

    @Override
    public String toString() {
        String s = this.name == null ? "" : this.name;
        if (this.scope != null) {
            return s + " [" + this.scope.getName() + "]";
        }
        return s;
    }
}
