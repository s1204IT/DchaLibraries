package sun.security.provider.certpath;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.cert.CRLException;
import java.security.cert.CRLSelector;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertStoreSpi;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import javax.security.auth.x500.X500Principal;
import sun.security.action.GetIntegerAction;
import sun.security.provider.certpath.PKIX;
import sun.security.util.Cache;
import sun.security.util.Debug;
import sun.security.x509.AccessDescription;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.URIName;

class URICertStore extends CertStoreSpi {
    private static final int CHECK_INTERVAL = 30000;
    private static final int DEFAULT_CRL_CONNECT_TIMEOUT = 15000;
    private Collection<X509Certificate> certs;
    private X509CRL crl;
    private final CertificateFactory factory;
    private long lastChecked;
    private long lastModified;
    private boolean ldap;
    private CertStore ldapCertStore;
    private CertStoreHelper ldapHelper;
    private String ldapPath;
    private URI uri;
    private static final Debug debug = Debug.getInstance("certpath");
    private static final int CRL_CONNECT_TIMEOUT = initializeTimeout();
    private static final int CACHE_SIZE = 185;
    private static final Cache<URICertStoreParameters, CertStore> certStoreCache = Cache.newSoftMemoryCache(CACHE_SIZE);

    private static int initializeTimeout() {
        Integer num = (Integer) AccessController.doPrivileged(new GetIntegerAction("com.sun.security.crl.timeout"));
        if (num == null || num.intValue() < 0) {
            return DEFAULT_CRL_CONNECT_TIMEOUT;
        }
        return num.intValue() * 1000;
    }

    URICertStore(CertStoreParameters certStoreParameters) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        super(certStoreParameters);
        this.certs = Collections.emptySet();
        this.ldap = false;
        if (!(certStoreParameters instanceof URICertStoreParameters)) {
            throw new InvalidAlgorithmParameterException("params must be instanceof URICertStoreParameters");
        }
        this.uri = ((URICertStoreParameters) certStoreParameters).uri;
        if (this.uri.getScheme().toLowerCase(Locale.ENGLISH).equals("ldap")) {
            this.ldap = true;
            this.ldapHelper = CertStoreHelper.getInstance("LDAP");
            this.ldapCertStore = this.ldapHelper.getCertStore(this.uri);
            this.ldapPath = this.uri.getPath();
            if (this.ldapPath.charAt(0) == '/') {
                this.ldapPath = this.ldapPath.substring(1);
            }
        }
        try {
            this.factory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException();
        }
    }

    static synchronized CertStore getInstance(URICertStoreParameters uRICertStoreParameters) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        CertStore ucs;
        if (debug != null) {
            debug.println("CertStore URI:" + ((Object) uRICertStoreParameters.uri));
        }
        ucs = certStoreCache.get(uRICertStoreParameters);
        if (ucs == null) {
            ucs = new UCS(new URICertStore(uRICertStoreParameters), null, "URI", uRICertStoreParameters);
            certStoreCache.put(uRICertStoreParameters, ucs);
        } else if (debug != null) {
            debug.println("URICertStore.getInstance: cache hit");
        }
        return ucs;
    }

    static CertStore getInstance(AccessDescription accessDescription) {
        if (!accessDescription.getAccessMethod().equals((Object) AccessDescription.Ad_CAISSUERS_Id)) {
            return null;
        }
        GeneralNameInterface name = accessDescription.getAccessLocation().getName();
        if (!(name instanceof URIName)) {
            return null;
        }
        try {
            return getInstance(new URICertStoreParameters(((URIName) name).getURI()));
        } catch (Exception e) {
            if (debug != null) {
                debug.println("exception creating CertStore: " + ((Object) e));
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public synchronized Collection<X509Certificate> engineGetCertificates(CertSelector certSelector) throws CertStoreException {
        if (this.ldap) {
            X509CertSelector x509CertSelector = (X509CertSelector) certSelector;
            try {
                return this.ldapCertStore.getCertificates(this.ldapHelper.wrap(x509CertSelector, x509CertSelector.getSubject(), this.ldapPath));
            } catch (IOException e) {
                throw new CertStoreException(e);
            }
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis - this.lastChecked < 30000) {
            if (debug != null) {
                debug.println("Returning certificates from cache");
            }
            return getMatchingCerts(this.certs, certSelector);
        }
        this.lastChecked = jCurrentTimeMillis;
        try {
            URLConnection uRLConnectionOpenConnection = this.uri.toURL().openConnection();
            if (this.lastModified != 0) {
                uRLConnectionOpenConnection.setIfModifiedSince(this.lastModified);
            }
            long j = this.lastModified;
            InputStream inputStream = uRLConnectionOpenConnection.getInputStream();
            try {
                this.lastModified = uRLConnectionOpenConnection.getLastModified();
                if (j != 0) {
                    if (j == this.lastModified) {
                        if (debug != null) {
                            debug.println("Not modified, using cached copy");
                        }
                        return getMatchingCerts(this.certs, certSelector);
                    }
                    if ((uRLConnectionOpenConnection instanceof HttpURLConnection) && ((HttpURLConnection) uRLConnectionOpenConnection).getResponseCode() == 304) {
                        if (debug != null) {
                            debug.println("Not modified, using cached copy");
                        }
                        Collection<X509Certificate> matchingCerts = getMatchingCerts(this.certs, certSelector);
                        if (inputStream != null) {
                            $closeResource(null, inputStream);
                        }
                        return matchingCerts;
                    }
                }
                if (debug != null) {
                    debug.println("Downloading new certificates...");
                }
                this.certs = this.factory.generateCertificates(inputStream);
                if (inputStream != null) {
                    $closeResource(null, inputStream);
                }
                return getMatchingCerts(this.certs, certSelector);
            } finally {
                if (inputStream != null) {
                    $closeResource(null, inputStream);
                }
            }
        } catch (IOException | CertificateException e2) {
            if (debug != null) {
                debug.println("Exception fetching certificates:");
                e2.printStackTrace();
            }
            this.lastModified = 0L;
            this.certs = Collections.emptySet();
            return this.certs;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    private static Collection<X509Certificate> getMatchingCerts(Collection<X509Certificate> collection, CertSelector certSelector) {
        if (certSelector == null) {
            return collection;
        }
        ArrayList arrayList = new ArrayList(collection.size());
        for (X509Certificate x509Certificate : collection) {
            if (certSelector.match(x509Certificate)) {
                arrayList.add(x509Certificate);
            }
        }
        return arrayList;
    }

    @Override
    public synchronized Collection<X509CRL> engineGetCRLs(CRLSelector cRLSelector) throws CertStoreException {
        Throwable th;
        if (this.ldap) {
            try {
                try {
                    return this.ldapCertStore.getCRLs(this.ldapHelper.wrap((X509CRLSelector) cRLSelector, (Collection<X500Principal>) null, this.ldapPath));
                } catch (CertStoreException e) {
                    throw new PKIX.CertStoreTypeException("LDAP", e);
                }
            } catch (IOException e2) {
                throw new CertStoreException(e2);
            }
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (jCurrentTimeMillis - this.lastChecked < 30000) {
            if (debug != null) {
                debug.println("Returning CRL from cache");
            }
            return getMatchingCRLs(this.crl, cRLSelector);
        }
        this.lastChecked = jCurrentTimeMillis;
        try {
            URLConnection uRLConnectionOpenConnection = this.uri.toURL().openConnection();
            if (this.lastModified != 0) {
                uRLConnectionOpenConnection.setIfModifiedSince(this.lastModified);
            }
            long j = this.lastModified;
            uRLConnectionOpenConnection.setConnectTimeout(CRL_CONNECT_TIMEOUT);
            InputStream inputStream = uRLConnectionOpenConnection.getInputStream();
            try {
                this.lastModified = uRLConnectionOpenConnection.getLastModified();
                if (j != 0) {
                    if (j == this.lastModified) {
                        if (debug != null) {
                            debug.println("Not modified, using cached copy");
                        }
                        Collection<X509CRL> matchingCRLs = getMatchingCRLs(this.crl, cRLSelector);
                        if (inputStream != null) {
                            $closeResource(null, inputStream);
                        }
                        return matchingCRLs;
                    }
                    if ((uRLConnectionOpenConnection instanceof HttpURLConnection) && ((HttpURLConnection) uRLConnectionOpenConnection).getResponseCode() == 304) {
                        if (debug != null) {
                            debug.println("Not modified, using cached copy");
                        }
                        Collection<X509CRL> matchingCRLs2 = getMatchingCRLs(this.crl, cRLSelector);
                        if (inputStream != null) {
                            $closeResource(null, inputStream);
                        }
                        return matchingCRLs2;
                    }
                }
                if (debug != null) {
                    debug.println("Downloading new CRL...");
                }
                this.crl = (X509CRL) this.factory.generateCRL(inputStream);
                if (inputStream != null) {
                    $closeResource(null, inputStream);
                }
                return getMatchingCRLs(this.crl, cRLSelector);
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (inputStream != null) {
                }
            }
        } catch (IOException | CRLException e3) {
            if (debug != null) {
                debug.println("Exception fetching CRL:");
                e3.printStackTrace();
            }
            this.lastModified = 0L;
            this.crl = null;
            throw new PKIX.CertStoreTypeException("URI", new CertStoreException(e3));
        }
    }

    private static Collection<X509CRL> getMatchingCRLs(X509CRL x509crl, CRLSelector cRLSelector) {
        if (cRLSelector == null || (x509crl != null && cRLSelector.match(x509crl))) {
            return Collections.singletonList(x509crl);
        }
        return Collections.emptyList();
    }

    static class URICertStoreParameters implements CertStoreParameters {
        private volatile int hashCode = 0;
        private final URI uri;

        URICertStoreParameters(URI uri) {
            this.uri = uri;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof URICertStoreParameters)) {
                return false;
            }
            return this.uri.equals(((URICertStoreParameters) obj).uri);
        }

        public int hashCode() {
            if (this.hashCode == 0) {
                this.hashCode = 629 + this.uri.hashCode();
            }
            return this.hashCode;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                throw new InternalError(e.toString(), e);
            }
        }
    }

    private static class UCS extends CertStore {
        protected UCS(CertStoreSpi certStoreSpi, Provider provider, String str, CertStoreParameters certStoreParameters) {
            super(certStoreSpi, provider, str, certStoreParameters);
        }
    }
}
