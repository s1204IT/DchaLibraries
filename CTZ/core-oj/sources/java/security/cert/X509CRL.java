package java.security.cert;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.X509CRLImpl;

public abstract class X509CRL extends CRL implements X509Extension {
    private transient X500Principal issuerPrincipal;

    public abstract byte[] getEncoded() throws CRLException;

    public abstract Principal getIssuerDN();

    public abstract Date getNextUpdate();

    public abstract X509CRLEntry getRevokedCertificate(BigInteger bigInteger);

    public abstract Set<? extends X509CRLEntry> getRevokedCertificates();

    public abstract String getSigAlgName();

    public abstract String getSigAlgOID();

    public abstract byte[] getSigAlgParams();

    public abstract byte[] getSignature();

    public abstract byte[] getTBSCertList() throws CRLException;

    public abstract Date getThisUpdate();

    public abstract int getVersion();

    public abstract void verify(PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException;

    public abstract void verify(PublicKey publicKey, String str) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException;

    protected X509CRL() {
        super("X.509");
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof X509CRL)) {
            return false;
        }
        try {
            return Arrays.equals(X509CRLImpl.getEncodedInternal(this), X509CRLImpl.getEncodedInternal((X509CRL) obj));
        } catch (CRLException e) {
            return false;
        }
    }

    public int hashCode() {
        int i = 0;
        try {
            byte[] encodedInternal = X509CRLImpl.getEncodedInternal(this);
            for (int i2 = 1; i2 < encodedInternal.length; i2++) {
                i += encodedInternal[i2] * i2;
            }
            return i;
        } catch (CRLException e) {
            return i;
        }
    }

    public void verify(PublicKey publicKey, Provider provider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException {
        throw new UnsupportedOperationException("X509CRL instance doesn't not support X509CRL#verify(PublicKey, Provider)");
    }

    public X500Principal getIssuerX500Principal() {
        if (this.issuerPrincipal == null) {
            this.issuerPrincipal = X509CRLImpl.getIssuerX500Principal(this);
        }
        return this.issuerPrincipal;
    }

    public X509CRLEntry getRevokedCertificate(X509Certificate x509Certificate) {
        if (!x509Certificate.getIssuerX500Principal().equals(getIssuerX500Principal())) {
            return null;
        }
        return getRevokedCertificate(x509Certificate.getSerialNumber());
    }
}
