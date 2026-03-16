package java.security.cert;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import javax.security.auth.x500.X500Principal;

public abstract class X509CRL extends CRL implements X509Extension {
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

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof X509CRL)) {
            return false;
        }
        X509CRL obj = (X509CRL) other;
        try {
            return Arrays.equals(getEncoded(), obj.getEncoded());
        } catch (CRLException e) {
            return false;
        }
    }

    public int hashCode() {
        int res = 0;
        try {
            byte[] array = getEncoded();
            for (byte b : array) {
                res += b & Character.DIRECTIONALITY_UNDEFINED;
            }
            return res;
        } catch (CRLException e) {
            return 0;
        }
    }

    public X500Principal getIssuerX500Principal() {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509CRL crl = (X509CRL) factory.generateCRL(new ByteArrayInputStream(getEncoded()));
            return crl.getIssuerX500Principal();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get X500Principal issuer", e);
        }
    }

    public X509CRLEntry getRevokedCertificate(X509Certificate certificate) {
        if (certificate == null) {
            throw new NullPointerException("certificate == null");
        }
        return getRevokedCertificate(certificate.getSerialNumber());
    }
}
