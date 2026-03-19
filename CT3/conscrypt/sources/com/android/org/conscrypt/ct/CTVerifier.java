package com.android.org.conscrypt.ct;

import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.OpenSSLX509Certificate;
import com.android.org.conscrypt.ct.SignedCertificateTimestamp;
import com.android.org.conscrypt.ct.VerifiedSCT;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CTVerifier {
    private final CTLogStore store;

    public CTVerifier(CTLogStore store) {
        this.store = store;
    }

    public CTVerificationResult verifySignedCertificateTimestamps(OpenSSLX509Certificate[] chain, byte[] tlsData, byte[] ocspData) throws CertificateEncodingException {
        if (chain.length == 0) {
            throw new IllegalArgumentException("Chain of certificates mustn't be empty.");
        }
        OpenSSLX509Certificate leaf = chain[0];
        CTVerificationResult result = new CTVerificationResult();
        verifyExternalSCTs(getSCTsFromTLSExtension(tlsData), leaf, result);
        verifyExternalSCTs(getSCTsFromOCSPResponse(ocspData, chain), leaf, result);
        verifyEmbeddedSCTs(getSCTsFromX509Extension(chain[0]), chain, result);
        return result;
    }

    private void verifyEmbeddedSCTs(List<SignedCertificateTimestamp> list, OpenSSLX509Certificate[] chain, CTVerificationResult result) {
        if (list.isEmpty()) {
            return;
        }
        CertificateEntry precertEntry = null;
        if (chain.length >= 2) {
            OpenSSLX509Certificate leaf = chain[0];
            OpenSSLX509Certificate issuer = chain[1];
            try {
                precertEntry = CertificateEntry.createForPrecertificate(leaf, issuer);
            } catch (CertificateException e) {
            }
        }
        if (precertEntry == null) {
            markSCTsAsInvalid(list, result);
            return;
        }
        for (SignedCertificateTimestamp sct : list) {
            VerifiedSCT.Status status = verifySingleSCT(sct, precertEntry);
            result.add(new VerifiedSCT(sct, status));
        }
    }

    private void verifyExternalSCTs(List<SignedCertificateTimestamp> list, OpenSSLX509Certificate leaf, CTVerificationResult result) {
        if (list.isEmpty()) {
            return;
        }
        try {
            CertificateEntry x509Entry = CertificateEntry.createForX509Certificate(leaf);
            for (SignedCertificateTimestamp sct : list) {
                VerifiedSCT.Status status = verifySingleSCT(sct, x509Entry);
                result.add(new VerifiedSCT(sct, status));
            }
        } catch (CertificateException e) {
            markSCTsAsInvalid(list, result);
        }
    }

    private VerifiedSCT.Status verifySingleSCT(SignedCertificateTimestamp sct, CertificateEntry certEntry) {
        CTLogInfo log = this.store.getKnownLog(sct.getLogID());
        if (log == null) {
            return VerifiedSCT.Status.UNKNOWN_LOG;
        }
        return log.verifySingleSCT(sct, certEntry);
    }

    private void markSCTsAsInvalid(List<SignedCertificateTimestamp> list, CTVerificationResult result) {
        for (SignedCertificateTimestamp sct : list) {
            result.add(new VerifiedSCT(sct, VerifiedSCT.Status.INVALID_SCT));
        }
    }

    private List<SignedCertificateTimestamp> getSCTsFromSCTList(byte[] data, SignedCertificateTimestamp.Origin origin) {
        if (data == null) {
            return Collections.EMPTY_LIST;
        }
        try {
            byte[][] sctList = Serialization.readList(data, 2, 2);
            List<org.conscrypt.ct.SignedCertificateTimestamp> scts = new ArrayList<>();
            for (byte[] encodedSCT : sctList) {
                try {
                    SignedCertificateTimestamp sct = SignedCertificateTimestamp.decode(encodedSCT, origin);
                    scts.add(sct);
                } catch (SerializationException e) {
                }
            }
            return scts;
        } catch (SerializationException e2) {
            return Collections.EMPTY_LIST;
        }
    }

    private List<SignedCertificateTimestamp> getSCTsFromTLSExtension(byte[] data) {
        return getSCTsFromSCTList(data, SignedCertificateTimestamp.Origin.TLS_EXTENSION);
    }

    private List<SignedCertificateTimestamp> getSCTsFromOCSPResponse(byte[] data, OpenSSLX509Certificate[] chain) {
        if (data == null || chain.length < 2) {
            return Collections.EMPTY_LIST;
        }
        byte[] extData = NativeCrypto.get_ocsp_single_extension(data, CTConstants.OCSP_SCT_LIST_OID, chain[0].getContext(), chain[1].getContext());
        if (extData == null) {
            return Collections.EMPTY_LIST;
        }
        try {
            return getSCTsFromSCTList(Serialization.readDEROctetString(Serialization.readDEROctetString(extData)), SignedCertificateTimestamp.Origin.OCSP_RESPONSE);
        } catch (SerializationException e) {
            return Collections.EMPTY_LIST;
        }
    }

    private List<SignedCertificateTimestamp> getSCTsFromX509Extension(OpenSSLX509Certificate leaf) {
        byte[] extData = leaf.getExtensionValue(CTConstants.X509_SCT_LIST_OID);
        if (extData == null) {
            return Collections.EMPTY_LIST;
        }
        try {
            return getSCTsFromSCTList(Serialization.readDEROctetString(Serialization.readDEROctetString(extData)), SignedCertificateTimestamp.Origin.EMBEDDED);
        } catch (SerializationException e) {
            return Collections.EMPTY_LIST;
        }
    }
}
