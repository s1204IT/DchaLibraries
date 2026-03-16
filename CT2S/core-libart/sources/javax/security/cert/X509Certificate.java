package javax.security.cert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateFactory;
import java.util.Date;

public abstract class X509Certificate extends Certificate {
    private static Constructor constructor;

    public abstract void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException;

    public abstract void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException;

    public abstract Principal getIssuerDN();

    public abstract Date getNotAfter();

    public abstract Date getNotBefore();

    public abstract BigInteger getSerialNumber();

    public abstract String getSigAlgName();

    public abstract String getSigAlgOID();

    public abstract byte[] getSigAlgParams();

    public abstract Principal getSubjectDN();

    public abstract int getVersion();

    static {
        try {
            String classname = Security.getProperty("cert.provider.x509v1");
            constructor = Class.forName(classname).getConstructor(InputStream.class);
        } catch (Throwable th) {
        }
    }

    public static final X509Certificate getInstance(InputStream inStream) throws CertificateException {
        if (inStream == null) {
            throw new CertificateException("inStream == null");
        }
        if (constructor != null) {
            try {
                return (X509Certificate) constructor.newInstance(inStream);
            } catch (Throwable e) {
                throw new CertificateException(e.getMessage());
            }
        }
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            final java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) cf.generateCertificate(inStream);
            return new X509Certificate() {
                @Override
                public byte[] getEncoded() throws CertificateEncodingException {
                    try {
                        return cert.getEncoded();
                    } catch (java.security.cert.CertificateEncodingException e2) {
                        throw new CertificateEncodingException(e2.getMessage());
                    }
                }

                @Override
                public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
                    try {
                        cert.verify(key);
                    } catch (java.security.cert.CertificateException e2) {
                        throw new CertificateException(e2.getMessage());
                    }
                }

                @Override
                public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, NoSuchProviderException {
                    try {
                        cert.verify(key, sigProvider);
                    } catch (java.security.cert.CertificateException e2) {
                        throw new CertificateException(e2.getMessage());
                    }
                }

                @Override
                public String toString() {
                    return cert.toString();
                }

                @Override
                public PublicKey getPublicKey() {
                    return cert.getPublicKey();
                }

                @Override
                public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
                    try {
                        cert.checkValidity();
                    } catch (java.security.cert.CertificateExpiredException e2) {
                        throw new CertificateExpiredException(e2.getMessage());
                    } catch (java.security.cert.CertificateNotYetValidException e3) {
                        throw new CertificateNotYetValidException(e3.getMessage());
                    }
                }

                @Override
                public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
                    try {
                        cert.checkValidity(date);
                    } catch (java.security.cert.CertificateExpiredException e2) {
                        throw new CertificateExpiredException(e2.getMessage());
                    } catch (java.security.cert.CertificateNotYetValidException e3) {
                        throw new CertificateNotYetValidException(e3.getMessage());
                    }
                }

                @Override
                public int getVersion() {
                    return 2;
                }

                @Override
                public BigInteger getSerialNumber() {
                    return cert.getSerialNumber();
                }

                @Override
                public Principal getIssuerDN() {
                    return cert.getIssuerDN();
                }

                @Override
                public Principal getSubjectDN() {
                    return cert.getSubjectDN();
                }

                @Override
                public Date getNotBefore() {
                    return cert.getNotBefore();
                }

                @Override
                public Date getNotAfter() {
                    return cert.getNotAfter();
                }

                @Override
                public String getSigAlgName() {
                    return cert.getSigAlgName();
                }

                @Override
                public String getSigAlgOID() {
                    return cert.getSigAlgOID();
                }

                @Override
                public byte[] getSigAlgParams() {
                    return cert.getSigAlgParams();
                }
            };
        } catch (java.security.cert.CertificateException e2) {
            throw new CertificateException(e2.getMessage());
        }
    }

    public static final X509Certificate getInstance(byte[] certData) throws CertificateException {
        if (certData == null) {
            throw new CertificateException("certData == null");
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(certData);
        return getInstance(bais);
    }
}
