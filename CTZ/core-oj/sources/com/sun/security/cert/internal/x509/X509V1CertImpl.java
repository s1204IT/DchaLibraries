package com.sun.security.cert.internal.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Date;
import javax.security.cert.CertificateEncodingException;
import javax.security.cert.CertificateExpiredException;
import javax.security.cert.CertificateNotYetValidException;
import javax.security.cert.X509Certificate;

public class X509V1CertImpl extends X509Certificate implements Serializable {
    static final long serialVersionUID = -2048442350420423405L;
    private java.security.cert.X509Certificate wrappedCert;

    private static synchronized CertificateFactory getFactory() throws CertificateException {
        return CertificateFactory.getInstance("X.509");
    }

    public X509V1CertImpl() {
    }

    public X509V1CertImpl(byte[] bArr) throws javax.security.cert.CertificateException {
        try {
            this.wrappedCert = (java.security.cert.X509Certificate) getFactory().generateCertificate(new ByteArrayInputStream(bArr));
        } catch (CertificateException e) {
            throw new javax.security.cert.CertificateException(e.getMessage());
        }
    }

    public X509V1CertImpl(InputStream inputStream) throws javax.security.cert.CertificateException {
        try {
            this.wrappedCert = (java.security.cert.X509Certificate) getFactory().generateCertificate(inputStream);
        } catch (CertificateException e) {
            throw new javax.security.cert.CertificateException(e.getMessage());
        }
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        try {
            return this.wrappedCert.getEncoded();
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateEncodingException(e.getMessage());
        }
    }

    @Override
    public void verify(PublicKey publicKey) throws javax.security.cert.CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        try {
            this.wrappedCert.verify(publicKey);
        } catch (CertificateException e) {
            throw new javax.security.cert.CertificateException(e.getMessage());
        }
    }

    @Override
    public void verify(PublicKey publicKey, String str) throws javax.security.cert.CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
        try {
            this.wrappedCert.verify(publicKey, str);
        } catch (CertificateException e) {
            throw new javax.security.cert.CertificateException(e.getMessage());
        }
    }

    @Override
    public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        checkValidity(new Date());
    }

    @Override
    public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
        try {
            this.wrappedCert.checkValidity(date);
        } catch (java.security.cert.CertificateExpiredException e) {
            throw new CertificateExpiredException(e.getMessage());
        } catch (java.security.cert.CertificateNotYetValidException e2) {
            throw new CertificateNotYetValidException(e2.getMessage());
        }
    }

    @Override
    public String toString() {
        return this.wrappedCert.toString();
    }

    @Override
    public PublicKey getPublicKey() {
        return this.wrappedCert.getPublicKey();
    }

    @Override
    public int getVersion() {
        return this.wrappedCert.getVersion() - 1;
    }

    @Override
    public BigInteger getSerialNumber() {
        return this.wrappedCert.getSerialNumber();
    }

    @Override
    public Principal getSubjectDN() {
        return this.wrappedCert.getSubjectDN();
    }

    @Override
    public Principal getIssuerDN() {
        return this.wrappedCert.getIssuerDN();
    }

    @Override
    public Date getNotBefore() {
        return this.wrappedCert.getNotBefore();
    }

    @Override
    public Date getNotAfter() {
        return this.wrappedCert.getNotAfter();
    }

    @Override
    public String getSigAlgName() {
        return this.wrappedCert.getSigAlgName();
    }

    @Override
    public String getSigAlgOID() {
        return this.wrappedCert.getSigAlgOID();
    }

    @Override
    public byte[] getSigAlgParams() {
        return this.wrappedCert.getSigAlgParams();
    }

    private synchronized void writeObject(ObjectOutputStream objectOutputStream) throws IOException {
        try {
            objectOutputStream.write(getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IOException("getEncoded failed: " + e.getMessage());
        }
    }

    private synchronized void readObject(ObjectInputStream objectInputStream) throws IOException {
        try {
            this.wrappedCert = (java.security.cert.X509Certificate) getFactory().generateCertificate(objectInputStream);
        } catch (CertificateException e) {
            throw new IOException("generateCertificate failed: " + e.getMessage());
        }
    }

    public java.security.cert.X509Certificate getX509Certificate() {
        return this.wrappedCert;
    }
}
