package org.apache.harmony.security.utils;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;

public class WrappedX509Certificate extends X509Certificate {
    private final X509Certificate wrapped;

    public WrappedX509Certificate(X509Certificate wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        return this.wrapped.getCriticalExtensionOIDs();
    }

    @Override
    public byte[] getExtensionValue(String oid) {
        return this.wrapped.getExtensionValue(oid);
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        return this.wrapped.getNonCriticalExtensionOIDs();
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        return this.wrapped.hasUnsupportedCriticalExtension();
    }

    @Override
    public void checkValidity() throws CertificateNotYetValidException, CertificateExpiredException {
        this.wrapped.checkValidity();
    }

    @Override
    public void checkValidity(Date date) throws CertificateNotYetValidException, CertificateExpiredException {
        this.wrapped.checkValidity(date);
    }

    @Override
    public int getVersion() {
        return this.wrapped.getVersion();
    }

    @Override
    public BigInteger getSerialNumber() {
        return this.wrapped.getSerialNumber();
    }

    @Override
    public Principal getIssuerDN() {
        return this.wrapped.getIssuerDN();
    }

    @Override
    public Principal getSubjectDN() {
        return this.wrapped.getSubjectDN();
    }

    @Override
    public Date getNotBefore() {
        return this.wrapped.getNotBefore();
    }

    @Override
    public Date getNotAfter() {
        return this.wrapped.getNotAfter();
    }

    @Override
    public byte[] getTBSCertificate() throws CertificateEncodingException {
        return this.wrapped.getTBSCertificate();
    }

    @Override
    public byte[] getSignature() {
        return this.wrapped.getSignature();
    }

    @Override
    public String getSigAlgName() {
        return this.wrapped.getSigAlgName();
    }

    @Override
    public String getSigAlgOID() {
        return this.wrapped.getSigAlgOID();
    }

    @Override
    public byte[] getSigAlgParams() {
        return this.wrapped.getSigAlgParams();
    }

    @Override
    public boolean[] getIssuerUniqueID() {
        return this.wrapped.getIssuerUniqueID();
    }

    @Override
    public boolean[] getSubjectUniqueID() {
        return this.wrapped.getSubjectUniqueID();
    }

    @Override
    public boolean[] getKeyUsage() {
        return this.wrapped.getKeyUsage();
    }

    @Override
    public int getBasicConstraints() {
        return this.wrapped.getBasicConstraints();
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        return this.wrapped.getEncoded();
    }

    @Override
    public void verify(PublicKey key) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
        this.wrapped.verify(key);
    }

    @Override
    public void verify(PublicKey key, String sigProvider) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException, NoSuchProviderException {
        verify(key, sigProvider);
    }

    @Override
    public String toString() {
        return this.wrapped.toString();
    }

    @Override
    public PublicKey getPublicKey() {
        return this.wrapped.getPublicKey();
    }
}
