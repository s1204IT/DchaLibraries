package com.android.server.locksettings.recoverablekeystore.certificate;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.slice.SliceClientPermissions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class CertUtils {
    private static final String CERT_FORMAT = "X.509";
    private static final String CERT_PATH_ALG = "PKIX";
    private static final String CERT_STORE_ALG = "Collection";
    static final int MUST_EXIST_AT_LEAST_ONE = 2;
    static final int MUST_EXIST_EXACTLY_ONE = 1;
    static final int MUST_EXIST_UNENFORCED = 0;
    private static final String SIGNATURE_ALG = "SHA256withRSA";

    @Retention(RetentionPolicy.SOURCE)
    @interface MustExist {
    }

    private CertUtils() {
    }

    static X509Certificate decodeCert(byte[] bArr) throws CertParsingException {
        return decodeCert(new ByteArrayInputStream(bArr));
    }

    static X509Certificate decodeCert(InputStream inputStream) throws CertParsingException {
        try {
            try {
                return (X509Certificate) CertificateFactory.getInstance(CERT_FORMAT).generateCertificate(inputStream);
            } catch (CertificateException e) {
                throw new CertParsingException(e);
            }
        } catch (CertificateException e2) {
            throw new RuntimeException(e2);
        }
    }

    static Element getXmlRootNode(byte[] bArr) throws CertParsingException {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bArr));
            document.getDocumentElement().normalize();
            return document.getDocumentElement();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new CertParsingException(e);
        }
    }

    static List<String> getXmlNodeContents(int i, Element element, String... strArr) throws CertParsingException {
        String strJoin = String.join(SliceClientPermissions.SliceAuthority.DELIMITER, strArr);
        try {
            NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath().compile(strJoin).evaluate(element, XPathConstants.NODESET);
            switch (i) {
                case 0:
                    break;
                case 1:
                    if (nodeList.getLength() != 1) {
                        throw new CertParsingException("The XML file must contain exactly one node with the path " + strJoin);
                    }
                    break;
                case 2:
                    if (nodeList.getLength() == 0) {
                        throw new CertParsingException("The XML file must contain at least one node with the path " + strJoin);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("This value of MustExist is not supported: " + i);
            }
            ArrayList arrayList = new ArrayList();
            for (int i2 = 0; i2 < nodeList.getLength(); i2++) {
                arrayList.add(nodeList.item(i2).getTextContent().replaceAll("\\s", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS));
            }
            return arrayList;
        } catch (XPathExpressionException e) {
            throw new CertParsingException(e);
        }
    }

    public static byte[] decodeBase64(String str) throws CertParsingException {
        try {
            return Base64.getDecoder().decode(str);
        } catch (IllegalArgumentException e) {
            throw new CertParsingException(e);
        }
    }

    static void verifyRsaSha256Signature(PublicKey publicKey, byte[] bArr, byte[] bArr2) throws CertValidationException {
        try {
            Signature signature = Signature.getInstance(SIGNATURE_ALG);
            try {
                signature.initVerify(publicKey);
                signature.update(bArr2);
                if (!signature.verify(bArr)) {
                    throw new CertValidationException("The signature is invalid");
                }
            } catch (InvalidKeyException | SignatureException e) {
                throw new CertValidationException(e);
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        }
    }

    static CertPath validateCert(Date date, X509Certificate x509Certificate, List<X509Certificate> list, X509Certificate x509Certificate2) throws CertValidationException {
        PKIXParameters pKIXParametersBuildPkixParams = buildPkixParams(date, x509Certificate, list, x509Certificate2);
        CertPath certPathBuildCertPath = buildCertPath(pKIXParametersBuildPkixParams);
        try {
            try {
                CertPathValidator.getInstance(CERT_PATH_ALG).validate(certPathBuildCertPath, pKIXParametersBuildPkixParams);
                return certPathBuildCertPath;
            } catch (InvalidAlgorithmParameterException | CertPathValidatorException e) {
                throw new CertValidationException(e);
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        }
    }

    public static void validateCertPath(X509Certificate x509Certificate, CertPath certPath) throws CertValidationException {
        validateCertPath(null, x509Certificate, certPath);
    }

    @VisibleForTesting
    static void validateCertPath(Date date, X509Certificate x509Certificate, CertPath certPath) throws CertValidationException {
        if (certPath.getCertificates().isEmpty()) {
            throw new CertValidationException("The given certificate path is empty");
        }
        if (!(certPath.getCertificates().get(0) instanceof X509Certificate)) {
            throw new CertValidationException("The given certificate path does not contain X509 certificates");
        }
        List<? extends Certificate> certificates = certPath.getCertificates();
        validateCert(date, x509Certificate, certificates.subList(1, certificates.size()), (X509Certificate) certificates.get(0));
    }

    @VisibleForTesting
    static CertPath buildCertPath(PKIXParameters pKIXParameters) throws CertValidationException {
        try {
            try {
                return CertPathBuilder.getInstance(CERT_PATH_ALG).build(pKIXParameters).getCertPath();
            } catch (InvalidAlgorithmParameterException | CertPathBuilderException e) {
                throw new CertValidationException(e);
            }
        } catch (NoSuchAlgorithmException e2) {
            throw new RuntimeException(e2);
        }
    }

    @VisibleForTesting
    static PKIXParameters buildPkixParams(Date date, X509Certificate x509Certificate, List<X509Certificate> list, X509Certificate x509Certificate2) throws CertValidationException {
        HashSet hashSet = new HashSet();
        hashSet.add(new TrustAnchor(x509Certificate, null));
        ArrayList arrayList = new ArrayList(list);
        arrayList.add(x509Certificate2);
        try {
            CertStore certStore = CertStore.getInstance(CERT_STORE_ALG, new CollectionCertStoreParameters(arrayList));
            X509CertSelector x509CertSelector = new X509CertSelector();
            x509CertSelector.setCertificate(x509Certificate2);
            try {
                PKIXBuilderParameters pKIXBuilderParameters = new PKIXBuilderParameters(hashSet, x509CertSelector);
                pKIXBuilderParameters.addCertStore(certStore);
                pKIXBuilderParameters.setDate(date);
                pKIXBuilderParameters.setRevocationEnabled(false);
                return pKIXBuilderParameters;
            } catch (InvalidAlgorithmParameterException e) {
                throw new CertValidationException(e);
            }
        } catch (InvalidAlgorithmParameterException e2) {
            throw new CertValidationException(e2);
        } catch (NoSuchAlgorithmException e3) {
            throw new RuntimeException(e3);
        }
    }
}
