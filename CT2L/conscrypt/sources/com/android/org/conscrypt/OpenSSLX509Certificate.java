package com.android.org.conscrypt;

import com.android.org.conscrypt.OpenSSLX509CertificateFactory;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.crypto.BadPaddingException;
import javax.security.auth.x500.X500Principal;
import org.apache.harmony.security.utils.AlgNameMapper;

public class OpenSSLX509Certificate extends X509Certificate {
    private final transient long mContext;

    OpenSSLX509Certificate(long ctx) {
        this.mContext = ctx;
    }

    public static OpenSSLX509Certificate fromX509DerInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is);
        try {
            try {
                long certCtx = NativeCrypto.d2i_X509_bio(bis.getBioContext());
                if (certCtx != 0) {
                    return new OpenSSLX509Certificate(certCtx);
                }
                return null;
            } catch (Exception e) {
                throw new OpenSSLX509CertificateFactory.ParsingException(e);
            }
        } finally {
            bis.release();
        }
    }

    public static OpenSSLX509Certificate fromX509Der(byte[] encoded) {
        long certCtx = NativeCrypto.d2i_X509(encoded);
        if (certCtx == 0) {
            return null;
        }
        return new OpenSSLX509Certificate(certCtx);
    }

    public static List<OpenSSLX509Certificate> fromPkcs7DerInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is);
        try {
            try {
                long[] certRefs = NativeCrypto.d2i_PKCS7_bio(bis.getBioContext(), 1);
                if (certRefs == null) {
                    return Collections.emptyList();
                }
                List<OpenSSLX509Certificate> certs = new ArrayList<>(certRefs.length);
                for (int i = 0; i < certRefs.length; i++) {
                    if (certRefs[i] != 0) {
                        certs.add(new OpenSSLX509Certificate(certRefs[i]));
                    }
                }
                return certs;
            } catch (Exception e) {
                throw new OpenSSLX509CertificateFactory.ParsingException(e);
            }
        } finally {
            bis.release();
        }
    }

    public static OpenSSLX509Certificate fromX509PemInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is);
        try {
            try {
                long certCtx = NativeCrypto.PEM_read_bio_X509(bis.getBioContext());
                if (certCtx != 0) {
                    return new OpenSSLX509Certificate(certCtx);
                }
                return null;
            } catch (Exception e) {
                throw new OpenSSLX509CertificateFactory.ParsingException(e);
            }
        } finally {
            bis.release();
        }
    }

    public static List<OpenSSLX509Certificate> fromPkcs7PemInputStream(InputStream is) throws OpenSSLX509CertificateFactory.ParsingException {
        OpenSSLBIOInputStream bis = new OpenSSLBIOInputStream(is);
        try {
            try {
                long[] certRefs = NativeCrypto.PEM_read_bio_PKCS7(bis.getBioContext(), 1);
                bis.release();
                List<OpenSSLX509Certificate> certs = new ArrayList<>(certRefs.length);
                for (int i = 0; i < certRefs.length; i++) {
                    if (certRefs[i] != 0) {
                        certs.add(new OpenSSLX509Certificate(certRefs[i]));
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

    public static OpenSSLX509Certificate fromCertificate(Certificate cert) throws CertificateEncodingException {
        if (cert instanceof OpenSSLX509Certificate) {
            return (OpenSSLX509Certificate) cert;
        }
        if (cert instanceof X509Certificate) {
            return fromX509Der(cert.getEncoded());
        }
        throw new CertificateEncodingException("Only X.509 certificates are supported");
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        String[] critOids = NativeCrypto.get_X509_ext_oids(this.mContext, 1);
        if (critOids.length == 0 && NativeCrypto.get_X509_ext_oids(this.mContext, 0).length == 0) {
            return null;
        }
        return new HashSet(Arrays.asList(critOids));
    }

    @Override
    public byte[] getExtensionValue(String oid) {
        return NativeCrypto.X509_get_ext_oid(this.mContext, oid);
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        String[] nonCritOids = NativeCrypto.get_X509_ext_oids(this.mContext, 0);
        if (nonCritOids.length == 0 && NativeCrypto.get_X509_ext_oids(this.mContext, 1).length == 0) {
            return null;
        }
        return new HashSet(Arrays.asList(nonCritOids));
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        return (NativeCrypto.get_X509_ex_flags(this.mContext) & 512) != 0;
    }

    @Override
    public void checkValidity() throws CertificateNotYetValidException, CertificateExpiredException {
        checkValidity(new Date());
    }

    @Override
    public void checkValidity(Date date) throws CertificateNotYetValidException, CertificateExpiredException {
        if (getNotBefore().compareTo(date) > 0) {
            throw new CertificateNotYetValidException("Certificate not valid until " + getNotBefore().toString() + " (compared to " + date.toString() + ")");
        }
        if (getNotAfter().compareTo(date) < 0) {
            throw new CertificateExpiredException("Certificate expired at " + getNotAfter().toString() + " (compared to " + date.toString() + ")");
        }
    }

    @Override
    public int getVersion() {
        return ((int) NativeCrypto.X509_get_version(this.mContext)) + 1;
    }

    @Override
    public BigInteger getSerialNumber() {
        return new BigInteger(NativeCrypto.X509_get_serialNumber(this.mContext));
    }

    @Override
    public Principal getIssuerDN() {
        return getIssuerX500Principal();
    }

    @Override
    public Principal getSubjectDN() {
        return getSubjectX500Principal();
    }

    @Override
    public Date getNotBefore() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(14, 0);
        NativeCrypto.ASN1_TIME_to_Calendar(NativeCrypto.X509_get_notBefore(this.mContext), calendar);
        return calendar.getTime();
    }

    @Override
    public Date getNotAfter() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(14, 0);
        NativeCrypto.ASN1_TIME_to_Calendar(NativeCrypto.X509_get_notAfter(this.mContext), calendar);
        return calendar.getTime();
    }

    @Override
    public byte[] getTBSCertificate() throws CertificateEncodingException {
        return NativeCrypto.get_X509_cert_info_enc(this.mContext);
    }

    @Override
    public byte[] getSignature() {
        return NativeCrypto.get_X509_signature(this.mContext);
    }

    @Override
    public String getSigAlgName() {
        return AlgNameMapper.map2AlgName(getSigAlgOID());
    }

    @Override
    public String getSigAlgOID() {
        return NativeCrypto.get_X509_sig_alg_oid(this.mContext);
    }

    @Override
    public byte[] getSigAlgParams() {
        return NativeCrypto.get_X509_sig_alg_parameter(this.mContext);
    }

    @Override
    public boolean[] getIssuerUniqueID() {
        return NativeCrypto.get_X509_issuerUID(this.mContext);
    }

    @Override
    public boolean[] getSubjectUniqueID() {
        return NativeCrypto.get_X509_subjectUID(this.mContext);
    }

    @Override
    public boolean[] getKeyUsage() {
        boolean[] kusage = NativeCrypto.get_X509_ex_kusage(this.mContext);
        if (kusage == null) {
            return null;
        }
        if (kusage.length < 9) {
            boolean[] resized = new boolean[9];
            System.arraycopy(kusage, 0, resized, 0, kusage.length);
            return resized;
        }
        return kusage;
    }

    @Override
    public int getBasicConstraints() {
        if ((NativeCrypto.get_X509_ex_flags(this.mContext) & 16) == 0) {
            return -1;
        }
        int pathLen = NativeCrypto.get_X509_ex_pathlen(this.mContext);
        if (pathLen == -1) {
            return Integer.MAX_VALUE;
        }
        return pathLen;
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        return NativeCrypto.i2d_X509(this.mContext);
    }

    private void verifyOpenSSL(OpenSSLKey pkey) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, CertificateException, NoSuchProviderException {
        try {
            NativeCrypto.X509_verify(this.mContext, pkey.getPkeyContext());
        } catch (RuntimeException e) {
            throw new CertificateException(e);
        } catch (BadPaddingException e2) {
            throw new SignatureException();
        }
    }

    private void verifyInternal(PublicKey key, String sigProvider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
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
        sig.update(getTBSCertificate());
        if (!sig.verify(getSignature())) {
            throw new SignatureException("signature did not verify");
        }
    }

    @Override
    public void verify(PublicKey key) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException, CertificateException, NoSuchProviderException {
        if (key instanceof OpenSSLKeyHolder) {
            OpenSSLKey pkey = ((OpenSSLKeyHolder) key).getOpenSSLKey();
            verifyOpenSSL(pkey);
        } else {
            verifyInternal(key, null);
        }
    }

    @Override
    public void verify(PublicKey key, String sigProvider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
        verifyInternal(key, sigProvider);
    }

    @Override
    public String toString() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        long bioCtx = NativeCrypto.create_BIO_OutputStream(os);
        try {
            NativeCrypto.X509_print_ex(bioCtx, this.mContext, 0L, 0L);
            return os.toString();
        } finally {
            NativeCrypto.BIO_free_all(bioCtx);
        }
    }

    @Override
    public PublicKey getPublicKey() {
        try {
            OpenSSLKey pkey = new OpenSSLKey(NativeCrypto.X509_get_pubkey(this.mContext));
            return pkey.getPublicKey();
        } catch (NoSuchAlgorithmException e) {
            String oid = NativeCrypto.get_X509_pubkey_oid(this.mContext);
            byte[] encoded = NativeCrypto.i2d_X509_PUBKEY(this.mContext);
            try {
                KeyFactory kf = KeyFactory.getInstance(oid);
                return kf.generatePublic(new X509EncodedKeySpec(encoded));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e2) {
                return new X509PublicKey(oid, encoded);
            }
        }
    }

    @Override
    public X500Principal getIssuerX500Principal() {
        byte[] issuer = NativeCrypto.X509_get_issuer_name(this.mContext);
        return new X500Principal(issuer);
    }

    @Override
    public X500Principal getSubjectX500Principal() {
        byte[] subject = NativeCrypto.X509_get_subject_name(this.mContext);
        return new X500Principal(subject);
    }

    @Override
    public List<String> getExtendedKeyUsage() throws CertificateParsingException {
        String[] extUsage = NativeCrypto.get_X509_ex_xkusage(this.mContext);
        if (extUsage == null) {
            return null;
        }
        return Arrays.asList(extUsage);
    }

    private static Collection<List<?>> alternativeNameArrayToList(Object[][] altNameArray) {
        if (altNameArray == null) {
            return null;
        }
        Collection<List<?>> coll = new ArrayList<>(altNameArray.length);
        for (Object[] objArr : altNameArray) {
            coll.add(Collections.unmodifiableList(Arrays.asList(objArr)));
        }
        return Collections.unmodifiableCollection(coll);
    }

    @Override
    public Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
        return alternativeNameArrayToList(NativeCrypto.get_X509_GENERAL_NAME_stack(this.mContext, 1));
    }

    @Override
    public Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
        return alternativeNameArrayToList(NativeCrypto.get_X509_GENERAL_NAME_stack(this.mContext, 2));
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof OpenSSLX509Certificate) {
            OpenSSLX509Certificate o = (OpenSSLX509Certificate) other;
            return NativeCrypto.X509_cmp(this.mContext, o.mContext) == 0;
        }
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return NativeCrypto.get_X509_hashCode(this.mContext);
    }

    public long getContext() {
        return this.mContext;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mContext != 0) {
                NativeCrypto.X509_free(this.mContext);
            }
        } finally {
            super.finalize();
        }
    }
}
