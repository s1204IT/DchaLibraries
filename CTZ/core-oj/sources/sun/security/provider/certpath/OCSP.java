package sun.security.provider.certpath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.cert.CRLReason;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.Extension;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import sun.security.action.GetIntegerAction;
import sun.security.util.Debug;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AccessDescription;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.GeneralName;
import sun.security.x509.URIName;
import sun.security.x509.X509CertImpl;

public final class OCSP {
    private static final int DEFAULT_CONNECT_TIMEOUT = 15000;
    static final ObjectIdentifier NONCE_EXTENSION_OID = ObjectIdentifier.newInternal(new int[]{1, 3, 6, 1, 5, 5, 7, 48, 1, 2});
    private static final Debug debug = Debug.getInstance("certpath");
    private static final int CONNECT_TIMEOUT = initializeTimeout();

    public interface RevocationStatus {

        public enum CertStatus {
            GOOD,
            REVOKED,
            UNKNOWN
        }

        CertStatus getCertStatus();

        CRLReason getRevocationReason();

        Date getRevocationTime();

        Map<String, Extension> getSingleExtensions();
    }

    private static int initializeTimeout() {
        Integer num = (Integer) AccessController.doPrivileged(new GetIntegerAction("com.sun.security.ocsp.timeout"));
        if (num == null || num.intValue() < 0) {
            return DEFAULT_CONNECT_TIMEOUT;
        }
        return num.intValue() * 1000;
    }

    private OCSP() {
    }

    public static RevocationStatus check(X509Certificate x509Certificate, X509Certificate x509Certificate2) throws IOException, CertPathValidatorException {
        try {
            X509CertImpl impl = X509CertImpl.toImpl(x509Certificate);
            URI responderURI = getResponderURI(impl);
            if (responderURI == null) {
                throw new CertPathValidatorException("No OCSP Responder URI in certificate");
            }
            CertId certId = new CertId(x509Certificate2, impl.getSerialNumberObject());
            return check((List<CertId>) Collections.singletonList(certId), responderURI, x509Certificate2, (X509Certificate) null, (Date) null, (List<Extension>) Collections.emptyList()).getSingleResponse(certId);
        } catch (IOException | CertificateException e) {
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", e);
        }
    }

    public static RevocationStatus check(X509Certificate x509Certificate, X509Certificate x509Certificate2, URI uri, X509Certificate x509Certificate3, Date date) throws IOException, CertPathValidatorException {
        return check(x509Certificate, x509Certificate2, uri, x509Certificate3, date, (List<Extension>) Collections.emptyList());
    }

    public static RevocationStatus check(X509Certificate x509Certificate, X509Certificate x509Certificate2, URI uri, X509Certificate x509Certificate3, Date date, List<Extension> list) throws IOException, CertPathValidatorException {
        try {
            CertId certId = new CertId(x509Certificate2, X509CertImpl.toImpl(x509Certificate).getSerialNumberObject());
            return check((List<CertId>) Collections.singletonList(certId), uri, x509Certificate2, x509Certificate3, date, list).getSingleResponse(certId);
        } catch (IOException | CertificateException e) {
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", e);
        }
    }

    static OCSPResponse check(List<CertId> list, URI uri, X509Certificate x509Certificate, X509Certificate x509Certificate2, Date date, List<Extension> list2) throws Throwable {
        OutputStream outputStream;
        IOException iOException;
        OutputStream outputStream2;
        try {
            OCSPRequest oCSPRequest = new OCSPRequest(list, list2);
            byte[] bArrEncodeBytes = oCSPRequest.encodeBytes();
            InputStream inputStream = null;
            try {
                URL url = uri.toURL();
                if (debug != null) {
                    debug.println("connecting to OCSP service at: " + ((Object) url));
                }
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setConnectTimeout(CONNECT_TIMEOUT);
                httpURLConnection.setReadTimeout(CONNECT_TIMEOUT);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setRequestProperty("Content-type", "application/ocsp-request");
                httpURLConnection.setRequestProperty("Content-length", String.valueOf(bArrEncodeBytes.length));
                outputStream = httpURLConnection.getOutputStream();
                try {
                    outputStream.write(bArrEncodeBytes);
                    outputStream.flush();
                    if (debug != null && httpURLConnection.getResponseCode() != 200) {
                        debug.println("Received HTTP error: " + httpURLConnection.getResponseCode() + " - " + httpURLConnection.getResponseMessage());
                    }
                    InputStream inputStream2 = httpURLConnection.getInputStream();
                    try {
                        int contentLength = httpURLConnection.getContentLength();
                        if (contentLength == -1) {
                            contentLength = Integer.MAX_VALUE;
                        }
                        int i = 2048;
                        if (contentLength <= 2048) {
                            i = contentLength;
                        }
                        byte[] bArrCopyOf = new byte[i];
                        int i2 = 0;
                        while (i2 < contentLength) {
                            int i3 = inputStream2.read(bArrCopyOf, i2, bArrCopyOf.length - i2);
                            if (i3 < 0) {
                                break;
                            }
                            i2 += i3;
                            if (i2 >= bArrCopyOf.length && i2 < contentLength) {
                                bArrCopyOf = Arrays.copyOf(bArrCopyOf, i2 * 2);
                            }
                        }
                        byte[] bArrCopyOf2 = Arrays.copyOf(bArrCopyOf, i2);
                        if (inputStream2 != null) {
                            try {
                                inputStream2.close();
                            } catch (IOException e) {
                                throw e;
                            }
                        }
                        if (outputStream != null) {
                            try {
                                outputStream.close();
                            } catch (IOException e2) {
                                throw e2;
                            }
                        }
                        try {
                            OCSPResponse oCSPResponse = new OCSPResponse(bArrCopyOf2);
                            oCSPResponse.verify(list, x509Certificate, x509Certificate2, date, oCSPRequest.getNonce());
                            return oCSPResponse;
                        } catch (IOException e3) {
                            throw new CertPathValidatorException(e3);
                        }
                    } catch (IOException e4) {
                        iOException = e4;
                        inputStream = inputStream2;
                        outputStream2 = outputStream;
                        try {
                            throw new CertPathValidatorException("Unable to determine revocation status due to network error", iOException, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
                        } catch (Throwable th) {
                            outputStream = outputStream2;
                            th = th;
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (IOException e5) {
                                    throw e5;
                                }
                            }
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (IOException e6) {
                                    throw e6;
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        inputStream = inputStream2;
                        if (inputStream != null) {
                        }
                        if (outputStream != null) {
                        }
                        throw th;
                    }
                } catch (IOException e7) {
                    iOException = e7;
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (IOException e8) {
                iOException = e8;
                outputStream2 = null;
            } catch (Throwable th4) {
                th = th4;
                outputStream = null;
            }
        } catch (IOException e9) {
            throw new CertPathValidatorException("Exception while encoding OCSPRequest", e9);
        }
    }

    public static URI getResponderURI(X509Certificate x509Certificate) {
        try {
            return getResponderURI(X509CertImpl.toImpl(x509Certificate));
        } catch (CertificateException e) {
            return null;
        }
    }

    static URI getResponderURI(X509CertImpl x509CertImpl) {
        AuthorityInfoAccessExtension authorityInfoAccessExtension = x509CertImpl.getAuthorityInfoAccessExtension();
        if (authorityInfoAccessExtension == null) {
            return null;
        }
        for (AccessDescription accessDescription : authorityInfoAccessExtension.getAccessDescriptions()) {
            if (accessDescription.getAccessMethod().equals((Object) AccessDescription.Ad_OCSP_Id)) {
                GeneralName accessLocation = accessDescription.getAccessLocation();
                if (accessLocation.getType() == 6) {
                    return ((URIName) accessLocation.getName()).getURI();
                }
            }
        }
        return null;
    }
}
