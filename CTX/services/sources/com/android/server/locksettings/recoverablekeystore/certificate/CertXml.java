package com.android.server.locksettings.recoverablekeystore.certificate;

import com.android.internal.annotations.VisibleForTesting;
import java.security.SecureRandom;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Element;

public final class CertXml {
    private static final String ENDPOINT_CERT_ITEM_TAG = "cert";
    private static final String ENDPOINT_CERT_LIST_TAG = "endpoints";
    private static final String INTERMEDIATE_CERT_ITEM_TAG = "cert";
    private static final String INTERMEDIATE_CERT_LIST_TAG = "intermediates";
    private static final String METADATA_NODE_TAG = "metadata";
    private static final String METADATA_REFRESH_INTERVAL_NODE_TAG = "refresh-interval";
    private static final String METADATA_SERIAL_NODE_TAG = "serial";
    private final List<X509Certificate> endpointCerts;
    private final List<X509Certificate> intermediateCerts;
    private final long refreshInterval;
    private final long serial;

    private CertXml(long j, long j2, List<X509Certificate> list, List<X509Certificate> list2) {
        this.serial = j;
        this.refreshInterval = j2;
        this.intermediateCerts = list;
        this.endpointCerts = list2;
    }

    public long getSerial() {
        return this.serial;
    }

    public long getRefreshInterval() {
        return this.refreshInterval;
    }

    @VisibleForTesting
    List<X509Certificate> getAllIntermediateCerts() {
        return this.intermediateCerts;
    }

    @VisibleForTesting
    List<X509Certificate> getAllEndpointCerts() {
        return this.endpointCerts;
    }

    public CertPath getRandomEndpointCert(X509Certificate x509Certificate) throws CertValidationException {
        return getEndpointCert(new SecureRandom().nextInt(this.endpointCerts.size()), null, x509Certificate);
    }

    @VisibleForTesting
    CertPath getEndpointCert(int i, Date date, X509Certificate x509Certificate) throws CertValidationException {
        return CertUtils.validateCert(date, x509Certificate, this.intermediateCerts, this.endpointCerts.get(i));
    }

    public static CertXml parse(byte[] bArr) throws CertParsingException {
        Element xmlRootNode = CertUtils.getXmlRootNode(bArr);
        return new CertXml(parseSerial(xmlRootNode), parseRefreshInterval(xmlRootNode), parseIntermediateCerts(xmlRootNode), parseEndpointCerts(xmlRootNode));
    }

    private static long parseSerial(Element element) throws CertParsingException {
        return Long.parseLong(CertUtils.getXmlNodeContents(1, element, METADATA_NODE_TAG, METADATA_SERIAL_NODE_TAG).get(0));
    }

    private static long parseRefreshInterval(Element element) throws CertParsingException {
        return Long.parseLong(CertUtils.getXmlNodeContents(1, element, METADATA_NODE_TAG, METADATA_REFRESH_INTERVAL_NODE_TAG).get(0));
    }

    private static List<X509Certificate> parseIntermediateCerts(Element element) throws CertParsingException {
        List<String> xmlNodeContents = CertUtils.getXmlNodeContents(0, element, INTERMEDIATE_CERT_LIST_TAG, "cert");
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = xmlNodeContents.iterator();
        while (it.hasNext()) {
            arrayList.add(CertUtils.decodeCert(CertUtils.decodeBase64(it.next())));
        }
        return Collections.unmodifiableList(arrayList);
    }

    private static List<X509Certificate> parseEndpointCerts(Element element) throws CertParsingException {
        List<String> xmlNodeContents = CertUtils.getXmlNodeContents(2, element, ENDPOINT_CERT_LIST_TAG, "cert");
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = xmlNodeContents.iterator();
        while (it.hasNext()) {
            arrayList.add(CertUtils.decodeCert(CertUtils.decodeBase64(it.next())));
        }
        return Collections.unmodifiableList(arrayList);
    }
}
