package com.android.server.locksettings.recoverablekeystore.certificate;

import com.android.internal.annotations.VisibleForTesting;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Element;

public final class SigXml {
    private static final String INTERMEDIATE_CERT_ITEM_TAG = "cert";
    private static final String INTERMEDIATE_CERT_LIST_TAG = "intermediates";
    private static final String SIGNATURE_NODE_TAG = "value";
    private static final String SIGNER_CERT_NODE_TAG = "certificate";
    private final List<X509Certificate> intermediateCerts;
    private final byte[] signature;
    private final X509Certificate signerCert;

    private SigXml(List<X509Certificate> list, X509Certificate x509Certificate, byte[] bArr) {
        this.intermediateCerts = list;
        this.signerCert = x509Certificate;
        this.signature = bArr;
    }

    public void verifyFileSignature(X509Certificate x509Certificate, byte[] bArr) throws CertValidationException {
        verifyFileSignature(x509Certificate, bArr, null);
    }

    @VisibleForTesting
    void verifyFileSignature(X509Certificate x509Certificate, byte[] bArr, Date date) throws CertValidationException {
        CertUtils.validateCert(date, x509Certificate, this.intermediateCerts, this.signerCert);
        CertUtils.verifyRsaSha256Signature(this.signerCert.getPublicKey(), this.signature, bArr);
    }

    public static SigXml parse(byte[] bArr) throws CertParsingException {
        Element xmlRootNode = CertUtils.getXmlRootNode(bArr);
        return new SigXml(parseIntermediateCerts(xmlRootNode), parseSignerCert(xmlRootNode), parseFileSignature(xmlRootNode));
    }

    private static List<X509Certificate> parseIntermediateCerts(Element element) throws CertParsingException {
        List<String> xmlNodeContents = CertUtils.getXmlNodeContents(0, element, INTERMEDIATE_CERT_LIST_TAG, INTERMEDIATE_CERT_ITEM_TAG);
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = xmlNodeContents.iterator();
        while (it.hasNext()) {
            arrayList.add(CertUtils.decodeCert(CertUtils.decodeBase64(it.next())));
        }
        return Collections.unmodifiableList(arrayList);
    }

    private static X509Certificate parseSignerCert(Element element) throws CertParsingException {
        return CertUtils.decodeCert(CertUtils.decodeBase64(CertUtils.getXmlNodeContents(1, element, SIGNER_CERT_NODE_TAG).get(0)));
    }

    private static byte[] parseFileSignature(Element element) throws CertParsingException {
        return CertUtils.decodeBase64(CertUtils.getXmlNodeContents(1, element, SIGNATURE_NODE_TAG).get(0));
    }
}
