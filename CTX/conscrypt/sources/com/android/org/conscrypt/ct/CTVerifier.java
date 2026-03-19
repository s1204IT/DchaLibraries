package com.android.org.conscrypt.ct;

import com.android.org.conscrypt.InternalUtil;
import com.android.org.conscrypt.OpenSSLX509Certificate;
import com.android.org.conscrypt.ct.SignedCertificateTimestamp;
import com.android.org.conscrypt.ct.VerifiedSCT;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class CTVerifier {
    private final CTLogStore store;

    public CTVerifier(CTLogStore cTLogStore) {
        this.store = cTLogStore;
    }

    public CTVerificationResult verifySignedCertificateTimestamps(List<X509Certificate> list, byte[] bArr, byte[] bArr2) throws CertificateEncodingException {
        OpenSSLX509Certificate[] openSSLX509CertificateArr = new OpenSSLX509Certificate[list.size()];
        Iterator<X509Certificate> it = list.iterator();
        int i = 0;
        while (it.hasNext()) {
            openSSLX509CertificateArr[i] = OpenSSLX509Certificate.fromCertificate(it.next());
            i++;
        }
        return verifySignedCertificateTimestamps(openSSLX509CertificateArr, bArr, bArr2);
    }

    public CTVerificationResult verifySignedCertificateTimestamps(OpenSSLX509Certificate[] openSSLX509CertificateArr, byte[] bArr, byte[] bArr2) throws CertificateEncodingException {
        if (openSSLX509CertificateArr.length == 0) {
            throw new IllegalArgumentException("Chain of certificates mustn't be empty.");
        }
        OpenSSLX509Certificate openSSLX509Certificate = openSSLX509CertificateArr[0];
        CTVerificationResult cTVerificationResult = new CTVerificationResult();
        verifyExternalSCTs(getSCTsFromTLSExtension(bArr), openSSLX509Certificate, cTVerificationResult);
        verifyExternalSCTs(getSCTsFromOCSPResponse(bArr2, openSSLX509CertificateArr), openSSLX509Certificate, cTVerificationResult);
        verifyEmbeddedSCTs(getSCTsFromX509Extension(openSSLX509CertificateArr[0]), openSSLX509CertificateArr, cTVerificationResult);
        return cTVerificationResult;
    }

    private void verifyEmbeddedSCTs(List<SignedCertificateTimestamp> list, OpenSSLX509Certificate[] openSSLX509CertificateArr, CTVerificationResult cTVerificationResult) {
        CertificateEntry certificateEntryCreateForPrecertificate;
        if (list.isEmpty()) {
            return;
        }
        if (openSSLX509CertificateArr.length >= 2) {
            try {
                certificateEntryCreateForPrecertificate = CertificateEntry.createForPrecertificate(openSSLX509CertificateArr[0], openSSLX509CertificateArr[1]);
            } catch (CertificateException e) {
                certificateEntryCreateForPrecertificate = null;
            }
        } else {
            certificateEntryCreateForPrecertificate = null;
        }
        if (certificateEntryCreateForPrecertificate == null) {
            markSCTsAsInvalid(list, cTVerificationResult);
            return;
        }
        for (SignedCertificateTimestamp signedCertificateTimestamp : list) {
            cTVerificationResult.add(new VerifiedSCT(signedCertificateTimestamp, verifySingleSCT(signedCertificateTimestamp, certificateEntryCreateForPrecertificate)));
        }
    }

    private void verifyExternalSCTs(List<SignedCertificateTimestamp> list, OpenSSLX509Certificate openSSLX509Certificate, CTVerificationResult cTVerificationResult) {
        if (list.isEmpty()) {
            return;
        }
        try {
            CertificateEntry certificateEntryCreateForX509Certificate = CertificateEntry.createForX509Certificate(openSSLX509Certificate);
            for (SignedCertificateTimestamp signedCertificateTimestamp : list) {
                cTVerificationResult.add(new VerifiedSCT(signedCertificateTimestamp, verifySingleSCT(signedCertificateTimestamp, certificateEntryCreateForX509Certificate)));
            }
        } catch (CertificateException e) {
            markSCTsAsInvalid(list, cTVerificationResult);
        }
    }

    private VerifiedSCT.Status verifySingleSCT(SignedCertificateTimestamp signedCertificateTimestamp, CertificateEntry certificateEntry) {
        CTLogInfo knownLog = this.store.getKnownLog(signedCertificateTimestamp.getLogID());
        if (knownLog == null) {
            return VerifiedSCT.Status.UNKNOWN_LOG;
        }
        return knownLog.verifySingleSCT(signedCertificateTimestamp, certificateEntry);
    }

    private void markSCTsAsInvalid(List<SignedCertificateTimestamp> list, CTVerificationResult cTVerificationResult) {
        Iterator<SignedCertificateTimestamp> it = list.iterator();
        while (it.hasNext()) {
            cTVerificationResult.add(new VerifiedSCT(it.next(), VerifiedSCT.Status.INVALID_SCT));
        }
    }

    private List<SignedCertificateTimestamp> getSCTsFromSCTList(byte[] bArr, SignedCertificateTimestamp.Origin origin) {
        if (bArr == null) {
            return Collections.emptyList();
        }
        try {
            byte[][] list = Serialization.readList(bArr, 2, 2);
            ArrayList arrayList = new ArrayList();
            for (byte[] bArr2 : list) {
                try {
                    arrayList.add(SignedCertificateTimestamp.decode(bArr2, origin));
                } catch (SerializationException e) {
                }
            }
            return arrayList;
        } catch (SerializationException e2) {
            return Collections.emptyList();
        }
    }

    private List<SignedCertificateTimestamp> getSCTsFromTLSExtension(byte[] bArr) {
        return getSCTsFromSCTList(bArr, SignedCertificateTimestamp.Origin.TLS_EXTENSION);
    }

    private List<SignedCertificateTimestamp> getSCTsFromOCSPResponse(byte[] bArr, OpenSSLX509Certificate[] openSSLX509CertificateArr) {
        if (bArr == null || openSSLX509CertificateArr.length < 2) {
            return Collections.emptyList();
        }
        byte[] ocspSingleExtension = InternalUtil.getOcspSingleExtension(bArr, CTConstants.OCSP_SCT_LIST_OID, openSSLX509CertificateArr[0], openSSLX509CertificateArr[1]);
        if (ocspSingleExtension == null) {
            return Collections.emptyList();
        }
        try {
            return getSCTsFromSCTList(Serialization.readDEROctetString(Serialization.readDEROctetString(ocspSingleExtension)), SignedCertificateTimestamp.Origin.OCSP_RESPONSE);
        } catch (SerializationException e) {
            return Collections.emptyList();
        }
    }

    private List<SignedCertificateTimestamp> getSCTsFromX509Extension(OpenSSLX509Certificate openSSLX509Certificate) {
        byte[] extensionValue = openSSLX509Certificate.getExtensionValue(CTConstants.X509_SCT_LIST_OID);
        if (extensionValue == null) {
            return Collections.emptyList();
        }
        try {
            return getSCTsFromSCTList(Serialization.readDEROctetString(Serialization.readDEROctetString(extensionValue)), SignedCertificateTimestamp.Origin.EMBEDDED);
        } catch (SerializationException e) {
            return Collections.emptyList();
        }
    }
}
