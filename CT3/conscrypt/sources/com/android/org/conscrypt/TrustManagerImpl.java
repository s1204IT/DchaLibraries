package com.android.org.conscrypt;

import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;

public final class TrustManagerImpl extends X509ExtendedTrustManager {
    private static final TrustAnchorComparator TRUST_ANCHOR_COMPARATOR = new TrustAnchorComparator(null);
    private final X509Certificate[] acceptedIssuers;
    private final CertBlacklist blacklist;
    private final Exception err;
    private final CertificateFactory factory;
    private final TrustedCertificateIndex intermediateIndex;
    private CertPinManager pinManager;
    private final KeyStore rootKeyStore;
    private final TrustedCertificateIndex trustedCertificateIndex;
    private final TrustedCertificateStore trustedCertificateStore;
    private final CertPathValidator validator;

    public TrustManagerImpl(KeyStore keyStore) {
        this(keyStore, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager manager) {
        this(keyStore, manager, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager manager, TrustedCertificateStore certStore) {
        this(keyStore, manager, certStore, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager manager, TrustedCertificateStore certStore, CertBlacklist blacklist) {
        CertPathValidator validatorLocal = null;
        CertificateFactory factoryLocal = null;
        KeyStore rootKeyStoreLocal = null;
        TrustedCertificateStore trustedCertificateStoreLocal = null;
        TrustedCertificateIndex trustedCertificateIndexLocal = null;
        X509Certificate[] acceptedIssuersLocal = null;
        Exception errLocal = null;
        try {
            validatorLocal = CertPathValidator.getInstance("PKIX");
            factoryLocal = CertificateFactory.getInstance("X509");
            if ("AndroidCAStore".equals(keyStore.getType())) {
                rootKeyStoreLocal = keyStore;
                if (certStore != null) {
                    trustedCertificateStoreLocal = certStore;
                } else {
                    TrustedCertificateStore trustedCertificateStoreLocal2 = new TrustedCertificateStore();
                    trustedCertificateStoreLocal = trustedCertificateStoreLocal2;
                }
                acceptedIssuersLocal = null;
                TrustedCertificateIndex trustedCertificateIndexLocal2 = new TrustedCertificateIndex();
                trustedCertificateIndexLocal = trustedCertificateIndexLocal2;
            } else {
                rootKeyStoreLocal = null;
                trustedCertificateStoreLocal = certStore;
                acceptedIssuersLocal = acceptedIssuers(keyStore);
                TrustedCertificateIndex trustedCertificateIndexLocal3 = new TrustedCertificateIndex(trustAnchors(acceptedIssuersLocal));
                trustedCertificateIndexLocal = trustedCertificateIndexLocal3;
            }
        } catch (Exception e) {
            errLocal = e;
        }
        if (manager != null) {
            this.pinManager = manager;
        } else {
            try {
                this.pinManager = new CertPinManager(trustedCertificateStoreLocal);
            } catch (PinManagerException e2) {
                throw new SecurityException("Could not initialize CertPinManager", e2);
            }
        }
        blacklist = blacklist == null ? new CertBlacklist() : blacklist;
        this.rootKeyStore = rootKeyStoreLocal;
        this.trustedCertificateStore = trustedCertificateStoreLocal;
        this.validator = validatorLocal;
        this.factory = factoryLocal;
        this.trustedCertificateIndex = trustedCertificateIndexLocal;
        this.intermediateIndex = new TrustedCertificateIndex();
        this.acceptedIssuers = acceptedIssuersLocal;
        this.err = errLocal;
        this.blacklist = blacklist;
    }

    private static X509Certificate[] acceptedIssuers(KeyStore ks) {
        try {
            List<X509Certificate> trusted = new ArrayList<>();
            Enumeration<String> en = ks.aliases();
            while (en.hasMoreElements()) {
                String alias = en.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                if (cert != null) {
                    trusted.add(cert);
                }
            }
            return (X509Certificate[]) trusted.toArray(new X509Certificate[trusted.size()]);
        } catch (KeyStoreException e) {
            return new X509Certificate[0];
        }
    }

    private static Set<TrustAnchor> trustAnchors(X509Certificate[] certs) {
        Set<TrustAnchor> trustAnchors = new HashSet<>(certs.length);
        for (X509Certificate cert : certs) {
            trustAnchors.add(new TrustAnchor(cert, null));
        }
        return trustAnchors;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkTrusted(chain, authType, null, null, true);
    }

    public List<X509Certificate> checkClientTrusted(X509Certificate[] chain, String authType, String hostname) throws CertificateException {
        return checkTrusted(chain, authType, hostname, true);
    }

    private static SSLSession getHandshakeSessionOrThrow(SSLSocket sslSocket) throws CertificateException {
        SSLSession session = sslSocket.getHandshakeSession();
        if (session == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        return session;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        SSLSession session = null;
        SSLParameters parameters = null;
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            session = getHandshakeSessionOrThrow(sslSocket);
            parameters = sslSocket.getSSLParameters();
        }
        checkTrusted(chain, authType, session, parameters, true);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        SSLSession session = engine.getHandshakeSession();
        if (session == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        checkTrusted(chain, authType, session, engine.getSSLParameters(), true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkTrusted(chain, authType, null, null, false);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String hostname) throws CertificateException {
        return checkTrusted(chain, authType, hostname, false);
    }

    public List<X509Certificate> getTrustedChainForServer(X509Certificate[] certs, String authType, Socket socket) throws CertificateException {
        SSLSession session = null;
        SSLParameters parameters = null;
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            session = getHandshakeSessionOrThrow(sslSocket);
            parameters = sslSocket.getSSLParameters();
        }
        return checkTrusted(certs, authType, session, parameters, false);
    }

    public List<X509Certificate> getTrustedChainForServer(X509Certificate[] certs, String authType, SSLEngine engine) throws CertificateException {
        SSLSession session = engine.getHandshakeSession();
        if (session == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        return checkTrusted(certs, authType, session, engine.getSSLParameters(), false);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        getTrustedChainForServer(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        getTrustedChainForServer(chain, authType, engine);
    }

    public boolean isUserAddedCertificate(X509Certificate cert) {
        if (this.trustedCertificateStore == null) {
            return false;
        }
        return this.trustedCertificateStore.isUserAddedCertificate(cert);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, SSLSession session) throws CertificateException {
        return checkTrusted(chain, authType, session, null, false);
    }

    public void handleTrustStorageUpdate() {
        if (this.acceptedIssuers == null) {
            this.trustedCertificateIndex.reset();
        } else {
            this.trustedCertificateIndex.reset(trustAnchors(this.acceptedIssuers));
        }
    }

    private List<X509Certificate> checkTrusted(X509Certificate[] certs, String authType, SSLSession session, SSLParameters parameters, boolean clientAuth) throws CertificateException {
        String identificationAlgorithm;
        String hostname = session != null ? session.getPeerHost() : null;
        if (session != null && parameters != null && (identificationAlgorithm = parameters.getEndpointIdentificationAlgorithm()) != null && "HTTPS".equals(identificationAlgorithm.toUpperCase(Locale.US))) {
            HostnameVerifier verifier = HttpsURLConnection.getDefaultHostnameVerifier();
            if (!verifier.verify(hostname, session)) {
                throw new CertificateException("No subjectAltNames on the certificate match");
            }
        }
        return checkTrusted(certs, authType, hostname, clientAuth);
    }

    private List<X509Certificate> checkTrusted(X509Certificate[] certs, String authType, String host, boolean clientAuth) throws CertificateException {
        if (certs == null || certs.length == 0 || authType == null || authType.length() == 0) {
            throw new IllegalArgumentException("null or zero-length parameter");
        }
        if (this.err != null) {
            throw new CertificateException(this.err);
        }
        Set<X509Certificate> used = new HashSet<>();
        ArrayList<X509Certificate> untrustedChain = new ArrayList<>();
        ArrayList<TrustAnchor> trustedChain = new ArrayList<>();
        X509Certificate leaf = certs[0];
        TrustAnchor leafAsAnchor = findTrustAnchorBySubjectAndPublicKey(leaf);
        if (leafAsAnchor != null) {
            trustedChain.add(leafAsAnchor);
            used.add(leafAsAnchor.getTrustedCert());
        } else {
            untrustedChain.add(leaf);
        }
        used.add(leaf);
        return checkTrustedRecursive(certs, host, clientAuth, untrustedChain, trustedChain, used);
    }

    private List<X509Certificate> checkTrustedRecursive(X509Certificate[] certs, String host, boolean clientAuth, ArrayList<X509Certificate> untrustedChain, ArrayList<TrustAnchor> trustAnchorChain, Set<X509Certificate> used) throws CertificateException {
        X509Certificate current;
        CertificateException lastException = null;
        if (trustAnchorChain.isEmpty()) {
            current = untrustedChain.get(untrustedChain.size() - 1);
        } else {
            current = trustAnchorChain.get(trustAnchorChain.size() - 1).getTrustedCert();
        }
        checkBlacklist(current);
        if (current.getIssuerDN().equals(current.getSubjectDN())) {
            return verifyChain(untrustedChain, trustAnchorChain, host, clientAuth);
        }
        Set<TrustAnchor> anchors = findAllTrustAnchorsByIssuerAndSignature(current);
        boolean seenIssuer = false;
        for (TrustAnchor anchor : sortPotentialAnchors(anchors)) {
            X509Certificate anchorCert = anchor.getTrustedCert();
            if (!used.contains(anchorCert)) {
                seenIssuer = true;
                used.add(anchorCert);
                trustAnchorChain.add(anchor);
                try {
                    return checkTrustedRecursive(certs, host, clientAuth, untrustedChain, trustAnchorChain, used);
                } catch (CertificateException ex) {
                    lastException = ex;
                    trustAnchorChain.remove(trustAnchorChain.size() - 1);
                    used.remove(anchorCert);
                }
            }
        }
        if (!trustAnchorChain.isEmpty()) {
            if (!seenIssuer) {
                return verifyChain(untrustedChain, trustAnchorChain, host, clientAuth);
            }
            throw lastException;
        }
        for (int i = 1; i < certs.length; i++) {
            X509Certificate candidateIssuer = certs[i];
            if (!used.contains(candidateIssuer) && current.getIssuerDN().equals(candidateIssuer.getSubjectDN())) {
                try {
                    candidateIssuer.checkValidity();
                    ChainStrengthAnalyzer.checkCert(candidateIssuer);
                    used.add(candidateIssuer);
                    untrustedChain.add(candidateIssuer);
                    try {
                        return checkTrustedRecursive(certs, host, clientAuth, untrustedChain, trustAnchorChain, used);
                    } catch (CertificateException ex2) {
                        lastException = ex2;
                        used.remove(candidateIssuer);
                        untrustedChain.remove(untrustedChain.size() - 1);
                    }
                } catch (CertificateException ex3) {
                    lastException = new CertificateException("Unacceptable certificate: " + candidateIssuer.getSubjectX500Principal(), ex3);
                }
            }
        }
        Set<TrustAnchor> intermediateAnchors = this.intermediateIndex.findAllByIssuerAndSignature(current);
        for (TrustAnchor intermediate : sortPotentialAnchors(intermediateAnchors)) {
            X509Certificate intermediateCert = intermediate.getTrustedCert();
            if (!used.contains(intermediateCert)) {
                used.add(intermediateCert);
                untrustedChain.add(intermediateCert);
                try {
                    return checkTrustedRecursive(certs, host, clientAuth, untrustedChain, trustAnchorChain, used);
                } catch (CertificateException ex4) {
                    lastException = ex4;
                    untrustedChain.remove(untrustedChain.size() - 1);
                    used.remove(intermediateCert);
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        CertPath certPath = this.factory.generateCertPath(untrustedChain);
        throw new CertificateException(new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath, -1));
    }

    private List<X509Certificate> verifyChain(List<X509Certificate> untrustedChain, List<TrustAnchor> trustAnchorChain, String host, boolean clientAuth) throws CertificateException {
        CertPath certPath = this.factory.generateCertPath(untrustedChain);
        if (trustAnchorChain.isEmpty()) {
            throw new CertificateException(new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath, -1));
        }
        List<X509Certificate> wholeChain = new ArrayList<>();
        wholeChain.addAll(untrustedChain);
        for (TrustAnchor anchor : trustAnchorChain) {
            wholeChain.add(anchor.getTrustedCert());
        }
        if (host != null) {
            try {
                boolean chainValid = this.pinManager.isChainValid(host, wholeChain);
                if (!chainValid) {
                    throw new CertificateException("Pinning failure", new CertPathValidatorException("Certificate path is not properly pinned.", null, certPath, -1));
                }
            } catch (PinManagerException e) {
                throw new CertificateException("Failed to check pinning", e);
            }
        }
        for (X509Certificate cert : wholeChain) {
            checkBlacklist(cert);
        }
        if (untrustedChain.isEmpty()) {
            return wholeChain;
        }
        ChainStrengthAnalyzer.check(untrustedChain);
        try {
            Set<TrustAnchor> anchorSet = new HashSet<>();
            anchorSet.add(trustAnchorChain.get(0));
            PKIXParameters params = new PKIXParameters(anchorSet);
            params.setRevocationEnabled(false);
            params.addCertPathChecker(new ExtendedKeyUsagePKIXCertPathChecker(clientAuth, untrustedChain.get(0), null));
            this.validator.validate(certPath, params);
            for (int i = 1; i < untrustedChain.size(); i++) {
                this.intermediateIndex.index(untrustedChain.get(i));
            }
            return wholeChain;
        } catch (InvalidAlgorithmParameterException e2) {
            throw new CertificateException("Chain validation failed", e2);
        } catch (CertPathValidatorException e3) {
            throw new CertificateException("Chain validation failed", e3);
        }
    }

    private void checkBlacklist(X509Certificate cert) throws CertificateException {
        if (!this.blacklist.isPublicKeyBlackListed(cert.getPublicKey())) {
        } else {
            throw new CertificateException("Certificate blacklisted by public key: " + cert);
        }
    }

    private static Collection<TrustAnchor> sortPotentialAnchors(Set<TrustAnchor> anchors) {
        if (anchors.size() <= 1) {
            return anchors;
        }
        List<TrustAnchor> sortedAnchors = new ArrayList<>(anchors);
        Collections.sort(sortedAnchors, TRUST_ANCHOR_COMPARATOR);
        return sortedAnchors;
    }

    private static class TrustAnchorComparator implements Comparator<TrustAnchor> {
        private static final CertificatePriorityComparator CERT_COMPARATOR = new CertificatePriorityComparator();

        TrustAnchorComparator(TrustAnchorComparator trustAnchorComparator) {
            this();
        }

        private TrustAnchorComparator() {
        }

        @Override
        public int compare(TrustAnchor lhs, TrustAnchor rhs) {
            X509Certificate lhsCert = lhs.getTrustedCert();
            X509Certificate rhsCert = rhs.getTrustedCert();
            return CERT_COMPARATOR.compare(lhsCert, rhsCert);
        }
    }

    private static class ExtendedKeyUsagePKIXCertPathChecker extends PKIXCertPathChecker {
        private static final String EKU_anyExtendedKeyUsage = "2.5.29.37.0";
        private static final String EKU_clientAuth = "1.3.6.1.5.5.7.3.2";
        private static final String EKU_msSGC = "1.3.6.1.4.1.311.10.3.3";
        private static final String EKU_nsSGC = "2.16.840.1.113730.4.1";
        private static final String EKU_serverAuth = "1.3.6.1.5.5.7.3.1";
        private final boolean clientAuth;
        private final X509Certificate leaf;
        private static final String EKU_OID = "2.5.29.37";
        private static final Set<String> SUPPORTED_EXTENSIONS = Collections.unmodifiableSet(new HashSet(Arrays.asList(EKU_OID)));

        ExtendedKeyUsagePKIXCertPathChecker(boolean clientAuth, X509Certificate leaf, ExtendedKeyUsagePKIXCertPathChecker extendedKeyUsagePKIXCertPathChecker) {
            this(clientAuth, leaf);
        }

        private ExtendedKeyUsagePKIXCertPathChecker(boolean clientAuth, X509Certificate leaf) {
            this.clientAuth = clientAuth;
            this.leaf = leaf;
        }

        @Override
        public void init(boolean forward) throws CertPathValidatorException {
        }

        @Override
        public boolean isForwardCheckingSupported() {
            return true;
        }

        @Override
        public Set<String> getSupportedExtensions() {
            return SUPPORTED_EXTENSIONS;
        }

        @Override
        public void check(Certificate c, Collection<String> unresolvedCritExts) throws CertPathValidatorException {
            if (c != this.leaf) {
                return;
            }
            try {
                List<String> ekuOids = this.leaf.getExtendedKeyUsage();
                if (ekuOids == null) {
                    return;
                }
                boolean goodExtendedKeyUsage = false;
                Iterator ekuOid$iterator = ekuOids.iterator();
                while (true) {
                    if (!ekuOid$iterator.hasNext()) {
                        break;
                    }
                    String ekuOid = (String) ekuOid$iterator.next();
                    if (ekuOid.equals(EKU_anyExtendedKeyUsage)) {
                        goodExtendedKeyUsage = true;
                        break;
                    }
                    if (this.clientAuth) {
                        if (ekuOid.equals(EKU_clientAuth)) {
                            goodExtendedKeyUsage = true;
                            break;
                        }
                    } else if (ekuOid.equals(EKU_serverAuth)) {
                        goodExtendedKeyUsage = true;
                        break;
                    } else if (ekuOid.equals(EKU_nsSGC)) {
                        goodExtendedKeyUsage = true;
                        break;
                    } else if (ekuOid.equals(EKU_msSGC)) {
                        goodExtendedKeyUsage = true;
                        break;
                    }
                }
                if (goodExtendedKeyUsage) {
                    unresolvedCritExts.remove(EKU_OID);
                    return;
                }
                throw new CertPathValidatorException("End-entity certificate does not have a valid extendedKeyUsage.");
            } catch (CertificateParsingException e) {
                throw new CertPathValidatorException(e);
            }
        }
    }

    private Set<TrustAnchor> findAllTrustAnchorsByIssuerAndSignature(X509Certificate cert) {
        Set<TrustAnchor> indexedAnchors = this.trustedCertificateIndex.findAllByIssuerAndSignature(cert);
        if (!indexedAnchors.isEmpty() || this.trustedCertificateStore == null) {
            return indexedAnchors;
        }
        Set<X509Certificate> storeAnchors = this.trustedCertificateStore.findAllIssuers(cert);
        if (storeAnchors.isEmpty()) {
            return indexedAnchors;
        }
        Set<TrustAnchor> result = new HashSet<>(storeAnchors.size());
        for (X509Certificate storeCert : storeAnchors) {
            result.add(this.trustedCertificateIndex.index(storeCert));
        }
        return result;
    }

    private TrustAnchor findTrustAnchorBySubjectAndPublicKey(X509Certificate cert) {
        X509Certificate systemCert;
        TrustAnchor trustAnchor = this.trustedCertificateIndex.findBySubjectAndPublicKey(cert);
        if (trustAnchor != null) {
            return trustAnchor;
        }
        if (this.trustedCertificateStore == null || (systemCert = this.trustedCertificateStore.getTrustAnchor(cert)) == null) {
            return null;
        }
        return new TrustAnchor(systemCert, null);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.acceptedIssuers != null ? (X509Certificate[]) this.acceptedIssuers.clone() : acceptedIssuers(this.rootKeyStore);
    }
}
