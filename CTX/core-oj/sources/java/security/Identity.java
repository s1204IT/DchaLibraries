package java.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Vector;

@Deprecated
public abstract class Identity implements Principal, Serializable {
    private static final long serialVersionUID = 3609922007826600659L;
    Vector<Certificate> certificates;
    String info;
    private String name;
    private PublicKey publicKey;
    IdentityScope scope;

    protected Identity() {
        this("restoring...");
    }

    public Identity(String str, IdentityScope identityScope) throws KeyManagementException {
        this(str);
        if (identityScope != null) {
            identityScope.addIdentity(this);
        }
        this.scope = identityScope;
    }

    public Identity(String str) {
        this.info = "No further information available.";
        this.name = str;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    public final IdentityScope getScope() {
        return this.scope;
    }

    public PublicKey getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(PublicKey publicKey) throws KeyManagementException {
        check("setIdentityPublicKey");
        this.publicKey = publicKey;
        this.certificates = new Vector<>();
    }

    public void setInfo(String str) {
        check("setIdentityInfo");
        this.info = str;
    }

    public String getInfo() {
        return this.info;
    }

    public void addCertificate(Certificate certificate) throws KeyManagementException {
        check("addIdentityCertificate");
        if (this.certificates == null) {
            this.certificates = new Vector<>();
        }
        if (this.publicKey != null) {
            if (!keyEquals(this.publicKey, certificate.getPublicKey())) {
                throw new KeyManagementException("public key different from cert public key");
            }
        } else {
            this.publicKey = certificate.getPublicKey();
        }
        this.certificates.addElement(certificate);
    }

    private boolean keyEquals(PublicKey publicKey, PublicKey publicKey2) {
        String format = publicKey.getFormat();
        String format2 = publicKey2.getFormat();
        if ((format2 == null) ^ (format == null)) {
            return false;
        }
        if (format == null || format2 == null || format.equalsIgnoreCase(format2)) {
            return Arrays.equals(publicKey.getEncoded(), publicKey2.getEncoded());
        }
        return false;
    }

    public void removeCertificate(Certificate certificate) throws KeyManagementException {
        check("removeIdentityCertificate");
        if (this.certificates != null) {
            if (certificate == null || !this.certificates.contains(certificate)) {
                throw new KeyManagementException();
            }
            this.certificates.removeElement(certificate);
        }
    }

    public Certificate[] certificates() {
        if (this.certificates == null) {
            return new Certificate[0];
        }
        Certificate[] certificateArr = new Certificate[this.certificates.size()];
        this.certificates.copyInto(certificateArr);
        return certificateArr;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Identity) {
            Identity identity = (Identity) obj;
            if (fullName().equals(identity.fullName())) {
                return true;
            }
            return identityEquals(identity);
        }
        return false;
    }

    protected boolean identityEquals(Identity identity) {
        if (!this.name.equalsIgnoreCase(identity.name)) {
            return false;
        }
        if ((this.publicKey == null) ^ (identity.publicKey == null)) {
            return false;
        }
        return this.publicKey == null || identity.publicKey == null || this.publicKey.equals(identity.publicKey);
    }

    String fullName() {
        String str = this.name;
        if (this.scope != null) {
            return str + "." + this.scope.getName();
        }
        return str;
    }

    @Override
    public String toString() {
        check("printIdentity");
        String str = this.name;
        if (this.scope != null) {
            return str + "[" + this.scope.getName() + "]";
        }
        return str;
    }

    public String toString(boolean z) {
        String string = toString();
        if (z) {
            String str = ((string + "\n") + printKeys()) + "\n" + printCertificates();
            if (this.info != null) {
                return str + "\n\t" + this.info;
            }
            return str + "\n\tno additional information available.";
        }
        return string;
    }

    String printKeys() {
        if (this.publicKey != null) {
            return "\tpublic key initialized";
        }
        return "\tno public key";
    }

    String printCertificates() {
        if (this.certificates == null) {
            return "\tno certificates";
        }
        String str = "\tcertificates: \n";
        int i = 1;
        for (Certificate certificate : this.certificates) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append("\tcertificate ");
            int i2 = i + 1;
            sb.append(i);
            sb.append("\tfor  : ");
            sb.append((Object) certificate.getPrincipal());
            sb.append("\n");
            i = i2;
            str = sb.toString() + "\t\t\tfrom : " + ((Object) certificate.getGuarantor()) + "\n";
        }
        return str;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    private static void check(String str) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkSecurityAccess(str);
        }
    }
}
