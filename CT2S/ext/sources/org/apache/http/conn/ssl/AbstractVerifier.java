package org.apache.http.conn.ssl;

import gov.nist.javax.sip.header.ParameterNames;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.DistinguishedNameParser;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.http.conn.util.InetAddressUtils;

@Deprecated
public abstract class AbstractVerifier implements X509HostnameVerifier {
    private static final String[] BAD_COUNTRY_2LDS = {"ac", "co", "com", "ed", "edu", "go", "gouv", "gov", ParameterNames.INFO, "lg", "ne", "net", "or", "org"};

    static {
        Arrays.sort(BAD_COUNTRY_2LDS);
    }

    @Override
    public final void verify(String host, SSLSocket ssl) throws IOException {
        if (host == null) {
            throw new NullPointerException("host to verify is null");
        }
        SSLSession session = ssl.getSession();
        Certificate[] certs = session.getPeerCertificates();
        X509Certificate x509 = (X509Certificate) certs[0];
        verify(host, x509);
    }

    @Override
    public final boolean verify(String host, SSLSession session) {
        try {
            Certificate[] certs = session.getPeerCertificates();
            X509Certificate x509 = (X509Certificate) certs[0];
            verify(host, x509);
            return true;
        } catch (SSLException e) {
            return false;
        }
    }

    @Override
    public final void verify(String host, X509Certificate cert) throws SSLException {
        String[] cns = getCNs(cert);
        String[] subjectAlts = getDNSSubjectAlts(cert);
        verify(host, cns, subjectAlts);
    }

    public final void verify(String host, String[] cns, String[] subjectAlts, boolean strictWithSubDomains) throws SSLException {
        LinkedList<String> names = new LinkedList<>();
        if (cns != null && cns.length > 0 && cns[0] != null) {
            names.add(cns[0]);
        }
        if (subjectAlts != null) {
            for (String subjectAlt : subjectAlts) {
                if (subjectAlt != null) {
                    names.add(subjectAlt);
                }
            }
        }
        if (names.isEmpty()) {
            String msg = "Certificate for <" + host + "> doesn't contain CN or DNS subjectAlt";
            throw new SSLException(msg);
        }
        StringBuffer buf = new StringBuffer();
        String hostName = host.trim().toLowerCase(Locale.ENGLISH);
        boolean match = false;
        Iterator<String> it = names.iterator();
        while (it.hasNext()) {
            String cn = it.next().toLowerCase(Locale.ENGLISH);
            buf.append(" <");
            buf.append(cn);
            buf.append('>');
            if (it.hasNext()) {
                buf.append(" OR");
            }
            boolean doWildcard = cn.startsWith("*.") && cn.indexOf(46, 2) != -1 && acceptableCountryWildcard(cn) && !InetAddressUtils.isIPv4Address(host);
            if (doWildcard) {
                match = hostName.endsWith(cn.substring(1));
                if (match && strictWithSubDomains) {
                    match = countDots(hostName) == countDots(cn);
                }
            } else {
                match = hostName.equals(cn);
            }
            if (match) {
                break;
            }
        }
        if (!match) {
            throw new SSLException("hostname in certificate didn't match: <" + host + "> !=" + ((Object) buf));
        }
    }

    public static boolean acceptableCountryWildcard(String cn) {
        int cnLen = cn.length();
        if (cnLen < 7 || cnLen > 9 || cn.charAt(cnLen - 3) != '.') {
            return true;
        }
        String s = cn.substring(2, cnLen - 3);
        int x = Arrays.binarySearch(BAD_COUNTRY_2LDS, s);
        return x < 0;
    }

    public static String[] getCNs(X509Certificate cert) {
        DistinguishedNameParser dnParser = new DistinguishedNameParser(cert.getSubjectX500Principal());
        List<String> cnList = dnParser.getAllMostSpecificFirst("cn");
        if (cnList.isEmpty()) {
            return null;
        }
        String[] cns = new String[cnList.size()];
        cnList.toArray(cns);
        return cns;
    }

    public static String[] getDNSSubjectAlts(X509Certificate cert) {
        LinkedList<String> subjectAltList = new LinkedList<>();
        Collection<List<?>> c = null;
        try {
            c = cert.getSubjectAlternativeNames();
        } catch (CertificateParsingException cpe) {
            Logger.getLogger(AbstractVerifier.class.getName()).log(Level.FINE, "Error parsing certificate.", (Throwable) cpe);
        }
        if (c != null) {
            for (List<?> aC : c) {
                int type = ((Integer) aC.get(0)).intValue();
                if (type == 2) {
                    String s = (String) aC.get(1);
                    subjectAltList.add(s);
                }
            }
        }
        if (subjectAltList.isEmpty()) {
            return null;
        }
        String[] subjectAlts = new String[subjectAltList.size()];
        subjectAltList.toArray(subjectAlts);
        return subjectAlts;
    }

    public static int countDots(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }
}
