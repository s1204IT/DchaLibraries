package com.android.org.bouncycastle.jcajce.provider.asymmetric.x509;

import com.android.org.bouncycastle.asn1.ASN1Encodable;
import com.android.org.bouncycastle.asn1.ASN1InputStream;
import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.ASN1Sequence;
import com.android.org.bouncycastle.asn1.ASN1Set;
import com.android.org.bouncycastle.asn1.ASN1TaggedObject;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.SignedData;
import com.android.org.bouncycastle.asn1.x509.CertificateList;
import com.android.org.bouncycastle.jcajce.util.BCJcaJceHelper;
import com.android.org.bouncycastle.jcajce.util.JcaJceHelper;
import com.android.org.bouncycastle.util.io.Streams;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactorySpi;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CertificateFactory extends CertificateFactorySpi {
    private static final PEMUtil PEM_CERT_PARSER = new PEMUtil("CERTIFICATE");
    private static final PEMUtil PEM_CRL_PARSER = new PEMUtil("CRL");
    private static final PEMUtil PEM_PKCS7_PARSER = new PEMUtil("PKCS7");
    private final JcaJceHelper bcHelper = new BCJcaJceHelper();
    private ASN1Set sData = null;
    private int sDataObjectCount = 0;
    private InputStream currentStream = null;
    private ASN1Set sCrlData = null;
    private int sCrlDataObjectCount = 0;
    private InputStream currentCrlStream = null;

    private Certificate readDERCertificate(ASN1InputStream aSN1InputStream) throws CertificateParsingException, IOException {
        return getCertificate(ASN1Sequence.getInstance(aSN1InputStream.readObject()));
    }

    private Certificate readPEMCertificate(InputStream inputStream) throws CertificateParsingException, IOException {
        return getCertificate(PEM_CERT_PARSER.readPEMObject(inputStream));
    }

    private Certificate getCertificate(ASN1Sequence aSN1Sequence) throws CertificateParsingException {
        if (aSN1Sequence == null) {
            return null;
        }
        if (aSN1Sequence.size() > 1 && (aSN1Sequence.getObjectAt(0) instanceof ASN1ObjectIdentifier) && aSN1Sequence.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData)) {
            this.sData = SignedData.getInstance(ASN1Sequence.getInstance((ASN1TaggedObject) aSN1Sequence.getObjectAt(1), true)).getCertificates();
            return getCertificate();
        }
        return new X509CertificateObject(this.bcHelper, com.android.org.bouncycastle.asn1.x509.Certificate.getInstance(aSN1Sequence));
    }

    private Certificate getCertificate() throws CertificateParsingException {
        if (this.sData != null) {
            while (this.sDataObjectCount < this.sData.size()) {
                ASN1Set aSN1Set = this.sData;
                int i = this.sDataObjectCount;
                this.sDataObjectCount = i + 1;
                ASN1Encodable objectAt = aSN1Set.getObjectAt(i);
                if (objectAt instanceof ASN1Sequence) {
                    return new X509CertificateObject(this.bcHelper, com.android.org.bouncycastle.asn1.x509.Certificate.getInstance(objectAt));
                }
            }
            return null;
        }
        return null;
    }

    protected CRL createCRL(CertificateList certificateList) throws CRLException {
        return new X509CRLObject(this.bcHelper, certificateList);
    }

    private CRL readPEMCRL(InputStream inputStream) throws IOException, CRLException {
        return getCRL(PEM_CRL_PARSER.readPEMObject(inputStream));
    }

    private CRL readDERCRL(ASN1InputStream aSN1InputStream) throws IOException, CRLException {
        return getCRL(ASN1Sequence.getInstance(aSN1InputStream.readObject()));
    }

    private CRL getCRL(ASN1Sequence aSN1Sequence) throws CRLException {
        if (aSN1Sequence == null) {
            return null;
        }
        if (aSN1Sequence.size() > 1 && (aSN1Sequence.getObjectAt(0) instanceof ASN1ObjectIdentifier) && aSN1Sequence.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData)) {
            this.sCrlData = SignedData.getInstance(ASN1Sequence.getInstance((ASN1TaggedObject) aSN1Sequence.getObjectAt(1), true)).getCRLs();
            return getCRL();
        }
        return createCRL(CertificateList.getInstance(aSN1Sequence));
    }

    private CRL getCRL() throws CRLException {
        if (this.sCrlData == null || this.sCrlDataObjectCount >= this.sCrlData.size()) {
            return null;
        }
        ASN1Set aSN1Set = this.sCrlData;
        int i = this.sCrlDataObjectCount;
        this.sCrlDataObjectCount = i + 1;
        return createCRL(CertificateList.getInstance(aSN1Set.getObjectAt(i)));
    }

    @Override
    public Certificate engineGenerateCertificate(InputStream inputStream) throws CertificateException {
        InputStream pushbackInputStream;
        if (this.currentStream == null || this.currentStream != inputStream) {
            this.currentStream = inputStream;
            this.sData = null;
            this.sDataObjectCount = 0;
        }
        try {
            if (this.sData != null) {
                if (this.sDataObjectCount != this.sData.size()) {
                    return getCertificate();
                }
                this.sData = null;
                this.sDataObjectCount = 0;
                return null;
            }
            if (!inputStream.markSupported()) {
                pushbackInputStream = new PushbackInputStream(inputStream);
            } else {
                pushbackInputStream = inputStream;
            }
            if (inputStream.markSupported()) {
                pushbackInputStream.mark(1);
            }
            int i = pushbackInputStream.read();
            if (i == -1) {
                return null;
            }
            if (inputStream.markSupported()) {
                pushbackInputStream.reset();
            } else {
                ((PushbackInputStream) pushbackInputStream).unread(i);
            }
            if (i != 48) {
                return readPEMCertificate(pushbackInputStream);
            }
            return readDERCertificate(new ASN1InputStream(pushbackInputStream));
        } catch (Exception e) {
            throw new ExCertificateException(e);
        }
    }

    @Override
    public Collection engineGenerateCertificates(InputStream inputStream) throws CertificateException {
        ArrayList arrayList = new ArrayList();
        while (true) {
            Certificate certificateEngineGenerateCertificate = engineGenerateCertificate(inputStream);
            if (certificateEngineGenerateCertificate != null) {
                arrayList.add(certificateEngineGenerateCertificate);
            } else {
                return arrayList;
            }
        }
    }

    @Override
    public CRL engineGenerateCRL(InputStream inputStream) throws CRLException {
        if (this.currentCrlStream == null || this.currentCrlStream != inputStream) {
            this.currentCrlStream = inputStream;
            this.sCrlData = null;
            this.sCrlDataObjectCount = 0;
        }
        try {
            if (this.sCrlData != null) {
                if (this.sCrlDataObjectCount != this.sCrlData.size()) {
                    return getCRL();
                }
                this.sCrlData = null;
                this.sCrlDataObjectCount = 0;
                return null;
            }
            if (!inputStream.markSupported()) {
                inputStream = new ByteArrayInputStream(Streams.readAll(inputStream));
            }
            inputStream.mark(1);
            int i = inputStream.read();
            if (i == -1) {
                return null;
            }
            inputStream.reset();
            if (i != 48) {
                return readPEMCRL(inputStream);
            }
            return readDERCRL(new ASN1InputStream(inputStream, true));
        } catch (CRLException e) {
            throw e;
        } catch (Exception e2) {
            throw new CRLException(e2.toString());
        }
    }

    @Override
    public Collection engineGenerateCRLs(InputStream inputStream) throws CRLException {
        ArrayList arrayList = new ArrayList();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        while (true) {
            CRL crlEngineGenerateCRL = engineGenerateCRL(bufferedInputStream);
            if (crlEngineGenerateCRL != null) {
                arrayList.add(crlEngineGenerateCRL);
            } else {
                return arrayList;
            }
        }
    }

    @Override
    public Iterator engineGetCertPathEncodings() {
        return PKIXCertPath.certPathEncodings.iterator();
    }

    @Override
    public CertPath engineGenerateCertPath(InputStream inputStream) throws CertificateException {
        return engineGenerateCertPath(inputStream, "PkiPath");
    }

    @Override
    public CertPath engineGenerateCertPath(InputStream inputStream, String str) throws CertificateException {
        return new PKIXCertPath(inputStream, str);
    }

    @Override
    public CertPath engineGenerateCertPath(List list) throws CertificateException {
        for (Object obj : list) {
            if (obj != null && !(obj instanceof X509Certificate)) {
                throw new CertificateException("list contains non X509Certificate object while creating CertPath\n" + obj.toString());
            }
        }
        return new PKIXCertPath(list);
    }

    private class ExCertificateException extends CertificateException {
        private Throwable cause;

        public ExCertificateException(Throwable th) {
            this.cause = th;
        }

        public ExCertificateException(String str, Throwable th) {
            super(str);
            this.cause = th;
        }

        @Override
        public Throwable getCause() {
            return this.cause;
        }
    }
}
