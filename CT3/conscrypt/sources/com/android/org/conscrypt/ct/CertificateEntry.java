package com.android.org.conscrypt.ct;

import com.android.org.conscrypt.OpenSSLX509Certificate;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateEntry {
    private final byte[] certificate;
    private final LogEntryType entryType;
    private final byte[] issuerKeyHash;

    public enum LogEntryType {
        X509_ENTRY,
        PRECERT_ENTRY;

        public static LogEntryType[] valuesCustom() {
            return values();
        }
    }

    private CertificateEntry(LogEntryType entryType, byte[] certificate, byte[] issuerKeyHash) {
        if (entryType == LogEntryType.PRECERT_ENTRY && issuerKeyHash == null) {
            throw new IllegalArgumentException("issuerKeyHash missing for precert entry.");
        }
        if (entryType == LogEntryType.X509_ENTRY && issuerKeyHash != null) {
            throw new IllegalArgumentException("unexpected issuerKeyHash for X509 entry.");
        }
        if (issuerKeyHash != null && issuerKeyHash.length != 32) {
            throw new IllegalArgumentException("issuerKeyHash must be 32 bytes long");
        }
        this.entryType = entryType;
        this.issuerKeyHash = issuerKeyHash;
        this.certificate = certificate;
    }

    public static CertificateEntry createForPrecertificate(byte[] tbsCertificate, byte[] issuerKeyHash) {
        return new CertificateEntry(LogEntryType.PRECERT_ENTRY, tbsCertificate, issuerKeyHash);
    }

    public static CertificateEntry createForPrecertificate(OpenSSLX509Certificate leaf, OpenSSLX509Certificate issuer) throws CertificateException {
        try {
            if (!leaf.getNonCriticalExtensionOIDs().contains(CTConstants.X509_SCT_LIST_OID)) {
                throw new CertificateException("Certificate does not contain embedded signed timestamps");
            }
            OpenSSLX509Certificate preCert = leaf.withDeletedExtension(CTConstants.X509_SCT_LIST_OID);
            byte[] tbs = preCert.getTBSCertificate();
            byte[] issuerKey = issuer.getPublicKey().getEncoded();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(issuerKey);
            byte[] issuerKeyHash = md.digest();
            return createForPrecertificate(tbs, issuerKeyHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static CertificateEntry createForX509Certificate(byte[] x509Certificate) {
        return new CertificateEntry(LogEntryType.X509_ENTRY, x509Certificate, null);
    }

    public static CertificateEntry createForX509Certificate(X509Certificate cert) throws CertificateEncodingException {
        return createForX509Certificate(cert.getEncoded());
    }

    public LogEntryType getEntryType() {
        return this.entryType;
    }

    public byte[] getCertificate() {
        return this.certificate;
    }

    public byte[] getIssuerKeyHash() {
        return this.issuerKeyHash;
    }

    public void encode(OutputStream output) throws SerializationException {
        Serialization.writeNumber(output, this.entryType.ordinal(), 2);
        if (this.entryType == LogEntryType.PRECERT_ENTRY) {
            Serialization.writeFixedBytes(output, this.issuerKeyHash);
        }
        Serialization.writeVariableBytes(output, this.certificate, 3);
    }
}
