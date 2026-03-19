package sun.security.provider.certpath;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import sun.security.util.Debug;
import sun.security.util.DerInputStream;
import sun.security.x509.AuthorityKeyIdentifierExtension;
import sun.security.x509.SerialNumber;

class AdaptableX509CertSelector extends X509CertSelector {
    private static final Debug debug = Debug.getInstance("certpath");
    private Date endDate;
    private BigInteger serial;
    private byte[] ski;
    private Date startDate;

    AdaptableX509CertSelector() {
    }

    void setValidityPeriod(Date date, Date date2) {
        this.startDate = date;
        this.endDate = date2;
    }

    @Override
    public void setSubjectKeyIdentifier(byte[] bArr) {
        throw new IllegalArgumentException();
    }

    @Override
    public void setSerialNumber(BigInteger bigInteger) {
        throw new IllegalArgumentException();
    }

    void setSkiAndSerialNumber(AuthorityKeyIdentifierExtension authorityKeyIdentifierExtension) throws IOException {
        this.ski = null;
        this.serial = null;
        if (authorityKeyIdentifierExtension != null) {
            this.ski = authorityKeyIdentifierExtension.getEncodedKeyIdentifier();
            SerialNumber serialNumber = (SerialNumber) authorityKeyIdentifierExtension.get(AuthorityKeyIdentifierExtension.SERIAL_NUMBER);
            if (serialNumber != null) {
                this.serial = serialNumber.getNumber();
            }
        }
    }

    @Override
    public boolean match(Certificate certificate) {
        X509Certificate x509Certificate = (X509Certificate) certificate;
        if (!matchSubjectKeyID(x509Certificate)) {
            return false;
        }
        int version = x509Certificate.getVersion();
        if (this.serial != null && version > 2 && !this.serial.equals(x509Certificate.getSerialNumber())) {
            return false;
        }
        if (version < 3) {
            if (this.startDate != null) {
                try {
                    x509Certificate.checkValidity(this.startDate);
                } catch (CertificateException e) {
                    return false;
                }
            }
            if (this.endDate != null) {
                try {
                    x509Certificate.checkValidity(this.endDate);
                } catch (CertificateException e2) {
                    return false;
                }
            }
        }
        return super.match(certificate);
    }

    private boolean matchSubjectKeyID(X509Certificate x509Certificate) {
        if (this.ski == null) {
            return true;
        }
        try {
            byte[] extensionValue = x509Certificate.getExtensionValue("2.5.29.14");
            if (extensionValue == null) {
                if (debug != null) {
                    debug.println("AdaptableX509CertSelector.match: no subject key ID extension. Subject: " + ((Object) x509Certificate.getSubjectX500Principal()));
                }
                return true;
            }
            byte[] octetString = new DerInputStream(extensionValue).getOctetString();
            if (octetString != null && Arrays.equals(this.ski, octetString)) {
                return true;
            }
            if (debug != null) {
                debug.println("AdaptableX509CertSelector.match: subject key IDs don't match. Expected: " + Arrays.toString(this.ski) + " Cert's: " + Arrays.toString(octetString));
            }
            return false;
        } catch (IOException e) {
            if (debug != null) {
                debug.println("AdaptableX509CertSelector.match: exception in subject key ID check");
            }
            return false;
        }
    }

    @Override
    public Object clone() {
        AdaptableX509CertSelector adaptableX509CertSelector = (AdaptableX509CertSelector) super.clone();
        if (this.startDate != null) {
            adaptableX509CertSelector.startDate = (Date) this.startDate.clone();
        }
        if (this.endDate != null) {
            adaptableX509CertSelector.endDate = (Date) this.endDate.clone();
        }
        if (this.ski != null) {
            adaptableX509CertSelector.ski = (byte[]) this.ski.clone();
        }
        return adaptableX509CertSelector;
    }
}
