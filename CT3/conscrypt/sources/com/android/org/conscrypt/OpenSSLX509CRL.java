package com.android.org.conscrypt;

import com.android.org.conscrypt.OpenSSLX509CertificateFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.security.auth.x500.X500Principal;

public class OpenSSLX509CRL extends X509CRL {
    private final long mContext;

    private OpenSSLX509CRL(long ctx) {
        this.mContext = ctx;
    }

    public static OpenSSLX509CRL fromX509DerInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is, true);
        try {
            try {
                long crlCtx = NativeCrypto.d2i_X509_CRL_bio(bis.getBioContext());
                if (crlCtx != 0) {
                    return new OpenSSLX509CRL(crlCtx);
                }
                return null;
            } catch (Exception e) {
                throw new OpenSSLX509CertificateFactory.ParsingException(e);
            }
        } finally {
            bis.release();
        }
    }

    public static List<OpenSSLX509CRL> fromPkcs7DerInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is, true);
        try {
            try {
                long[] certRefs = NativeCrypto.d2i_PKCS7_bio(bis.getBioContext(), 2);
                bis.release();
                List<org.conscrypt.OpenSSLX509CRL> certs = new ArrayList<>(certRefs.length);
                for (int i = 0; i < certRefs.length; i++) {
                    if (certRefs[i] != 0) {
                        certs.add(new OpenSSLX509CRL(certRefs[i]));
                    }
                }
                return certs;
            } catch (Exception e) {
                throw new OpenSSLX509CertificateFactory.ParsingException(e);
            }
        } catch (Throwable th) {
            bis.release();
            throw th;
        }
    }

    public static OpenSSLX509CRL fromX509PemInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is, true);
        try {
            try {
                long crlCtx = NativeCrypto.PEM_read_bio_X509_CRL(bis.getBioContext());
                if (crlCtx != 0) {
                    return new OpenSSLX509CRL(crlCtx);
                }
                return null;
            } catch (Exception e) {
                throw new OpenSSLX509CertificateFactory.ParsingException(e);
            }
        } finally {
            bis.release();
        }
    }

    public static List<OpenSSLX509CRL> fromPkcs7PemInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is, true);
        try {
            try {
                long[] certRefs = NativeCrypto.PEM_read_bio_PKCS7(bis.getBioContext(), 2);
                bis.release();
                List<org.conscrypt.OpenSSLX509CRL> certs = new ArrayList<>(certRefs.length);
                for (int i = 0; i < certRefs.length; i++) {
                    if (certRefs[i] != 0) {
                        certs.add(new OpenSSLX509CRL(certRefs[i]));
                    }
                }
                return certs;
            } catch (Exception e) {
                throw new OpenSSLX509CertificateFactory.ParsingException(e);
            }
        } catch (Throwable th) {
            bis.release();
            throw th;
        }
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        String[] critOids = NativeCrypto.get_X509_CRL_ext_oids(this.mContext, 1);
        if (critOids.length == 0 && NativeCrypto.get_X509_CRL_ext_oids(this.mContext, 0).length == 0) {
            return null;
        }
        return new HashSet(Arrays.asList(critOids));
    }

    @Override
    public byte[] getExtensionValue(String oid) {
        return NativeCrypto.X509_CRL_get_ext_oid(this.mContext, oid);
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        String[] nonCritOids = NativeCrypto.get_X509_CRL_ext_oids(this.mContext, 0);
        if (nonCritOids.length == 0 && NativeCrypto.get_X509_CRL_ext_oids(this.mContext, 1).length == 0) {
            return null;
        }
        return new HashSet(Arrays.asList(nonCritOids));
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        String[] criticalOids = NativeCrypto.get_X509_CRL_ext_oids(this.mContext, 1);
        for (String oid : criticalOids) {
            long extensionRef = NativeCrypto.X509_CRL_get_ext(this.mContext, oid);
            if (NativeCrypto.X509_supported_extension(extensionRef) != 1) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] getEncoded() throws CRLException {
        return NativeCrypto.i2d_X509_CRL(this.mContext);
    }

    private void verifyOpenSSL(OpenSSLKey pkey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        NativeCrypto.X509_CRL_verify(this.mContext, pkey.getNativeRef());
    }

    private void verifyInternal(PublicKey key, String sigProvider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        Signature sig;
        String sigAlg = getSigAlgName();
        if (sigAlg == null) {
            sigAlg = getSigAlgOID();
        }
        if (sigProvider == null) {
            sig = Signature.getInstance(sigAlg);
        } else {
            sig = Signature.getInstance(sigAlg, sigProvider);
        }
        sig.initVerify(key);
        sig.update(getTBSCertList());
        if (sig.verify(getSignature())) {
        } else {
            throw new SignatureException("signature did not verify");
        }
    }

    @Override
    public void verify(PublicKey key) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        if (key instanceof OpenSSLKeyHolder) {
            OpenSSLKey pkey = ((OpenSSLKeyHolder) key).getOpenSSLKey();
            verifyOpenSSL(pkey);
        } else {
            verifyInternal(key, null);
        }
    }

    @Override
    public void verify(PublicKey key, String sigProvider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CRLException, NoSuchProviderException {
        verifyInternal(key, sigProvider);
    }

    @Override
    public int getVersion() {
        return ((int) NativeCrypto.X509_CRL_get_version(this.mContext)) + 1;
    }

    @Override
    public Principal getIssuerDN() {
        return getIssuerX500Principal();
    }

    @Override
    public X500Principal getIssuerX500Principal() {
        byte[] issuer = NativeCrypto.X509_CRL_get_issuer_name(this.mContext);
        return new X500Principal(issuer);
    }

    @Override
    public Date getThisUpdate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(14, 0);
        NativeCrypto.ASN1_TIME_to_Calendar(NativeCrypto.X509_CRL_get_lastUpdate(this.mContext), calendar);
        return calendar.getTime();
    }

    @Override
    public Date getNextUpdate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(14, 0);
        NativeCrypto.ASN1_TIME_to_Calendar(NativeCrypto.X509_CRL_get_nextUpdate(this.mContext), calendar);
        return calendar.getTime();
    }

    @Override
    public X509CRLEntry getRevokedCertificate(BigInteger serialNumber) {
        long revokedRef = NativeCrypto.X509_CRL_get0_by_serial(this.mContext, serialNumber.toByteArray());
        if (revokedRef == 0) {
            return null;
        }
        return new OpenSSLX509CRLEntry(NativeCrypto.X509_REVOKED_dup(revokedRef));
    }

    @Override
    public X509CRLEntry getRevokedCertificate(X509Certificate x509Certificate) {
        if (x509Certificate instanceof OpenSSLX509Certificate) {
            long x509RevokedRef = NativeCrypto.X509_CRL_get0_by_cert(this.mContext, x509Certificate.getContext());
            if (x509RevokedRef == 0) {
                return null;
            }
            return new OpenSSLX509CRLEntry(NativeCrypto.X509_REVOKED_dup(x509RevokedRef));
        }
        return getRevokedCertificate(x509Certificate.getSerialNumber());
    }

    @Override
    public Set<? extends X509CRLEntry> getRevokedCertificates() {
        long[] entryRefs = NativeCrypto.X509_CRL_get_REVOKED(this.mContext);
        if (entryRefs == null || entryRefs.length == 0) {
            return null;
        }
        Set<org.conscrypt.OpenSSLX509CRLEntry> crlSet = new HashSet<>();
        for (long entryRef : entryRefs) {
            crlSet.add(new OpenSSLX509CRLEntry(entryRef));
        }
        return crlSet;
    }

    @Override
    public byte[] getTBSCertList() throws CRLException {
        return NativeCrypto.get_X509_CRL_crl_enc(this.mContext);
    }

    @Override
    public byte[] getSignature() {
        return NativeCrypto.get_X509_CRL_signature(this.mContext);
    }

    @Override
    public String getSigAlgName() {
        String oid = getSigAlgOID();
        String algName = Platform.oidToAlgorithmName(oid);
        if (algName != null) {
            return algName;
        }
        return oid;
    }

    @Override
    public String getSigAlgOID() {
        return NativeCrypto.get_X509_CRL_sig_alg_oid(this.mContext);
    }

    @Override
    public byte[] getSigAlgParams() {
        return NativeCrypto.get_X509_CRL_sig_alg_parameter(this.mContext);
    }

    @Override
    public boolean isRevoked(Certificate certificate) {
        OpenSSLX509Certificate openSSLX509CertificateFromX509DerInputStream;
        if (!(certificate instanceof X509Certificate)) {
            return false;
        }
        if (certificate instanceof OpenSSLX509Certificate) {
            openSSLX509CertificateFromX509DerInputStream = certificate;
        } else {
            try {
                openSSLX509CertificateFromX509DerInputStream = OpenSSLX509Certificate.fromX509DerInputStream(new ByteArrayInputStream(certificate.getEncoded()));
            } catch (Exception e) {
                throw new RuntimeException("cannot convert certificate", e);
            }
        }
        return NativeCrypto.X509_CRL_get0_by_cert(this.mContext, openSSLX509CertificateFromX509DerInputStream.getContext()) != 0;
    }

    @Override
    public String toString() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        long bioCtx = NativeCrypto.create_BIO_OutputStream(os);
        try {
            NativeCrypto.X509_CRL_print(bioCtx, this.mContext);
            return os.toString();
        } finally {
            NativeCrypto.BIO_free_all(bioCtx);
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mContext != 0) {
                NativeCrypto.X509_CRL_free(this.mContext);
            }
        } finally {
            super.finalize();
        }
    }
}
