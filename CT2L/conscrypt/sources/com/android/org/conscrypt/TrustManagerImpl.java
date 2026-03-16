package com.android.org.conscrypt;

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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLSession;
import javax.net.ssl.X509TrustManager;

public final class TrustManagerImpl implements X509TrustManager {
    private final X509Certificate[] acceptedIssuers;
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
                trustedCertificateStoreLocal = certStore != null ? certStore : new TrustedCertificateStore();
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
        this.rootKeyStore = rootKeyStoreLocal;
        this.trustedCertificateStore = trustedCertificateStoreLocal;
        this.validator = validatorLocal;
        this.factory = factoryLocal;
        this.trustedCertificateIndex = trustedCertificateIndexLocal;
        this.intermediateIndex = new TrustedCertificateIndex();
        this.acceptedIssuers = acceptedIssuersLocal;
        this.err = errLocal;
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
        checkTrusted(chain, authType, null, true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkTrusted(chain, authType, null, false);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String host) throws CertificateException {
        return checkTrusted(chain, authType, host, false);
    }

    public boolean isUserAddedCertificate(X509Certificate cert) {
        if (this.trustedCertificateStore == null) {
            return false;
        }
        return this.trustedCertificateStore.isUserAddedCertificate(cert);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, SSLSession session) throws CertificateException {
        return checkTrusted(chain, authType, session.getPeerHost(), false);
    }

    public void handleTrustStorageUpdate() {
        if (this.acceptedIssuers == null) {
            this.trustedCertificateIndex.reset();
        } else {
            this.trustedCertificateIndex.reset(trustAnchors(this.acceptedIssuers));
        }
    }

    private List<X509Certificate> checkTrusted(X509Certificate[] chain, String authType, String host, boolean clientAuth) throws CertificateException {
        X509Certificate next;
        if (chain == null || chain.length == 0 || authType == null || authType.length() == 0) {
            throw new IllegalArgumentException("null or zero-length parameter");
        }
        if (this.err != null) {
            throw new CertificateException(this.err);
        }
        Set<TrustAnchor> trustAnchor = new HashSet<>();
        X509Certificate[] newChain = cleanupCertChainAndFindTrustAnchors(chain, trustAnchor);
        List<X509Certificate> wholeChain = new ArrayList<>();
        wholeChain.addAll(Arrays.asList(newChain));
        for (TrustAnchor trust : trustAnchor) {
            wholeChain.add(trust.getTrustedCert());
        }
        X509Certificate last = wholeChain.get(wholeChain.size() - 1);
        while (true) {
            TrustAnchor cachedTrust = this.trustedCertificateIndex.findByIssuerAndSignature(last);
            if (cachedTrust == null || (next = cachedTrust.getTrustedCert()) == last) {
                break;
            }
            wholeChain.add(next);
            last = next;
        }
        CertPath certPath = this.factory.generateCertPath(Arrays.asList(newChain));
        if (host != null) {
            try {
                boolean isChainValid = this.pinManager.isChainValid(host, wholeChain);
                if (!isChainValid) {
                    throw new CertificateException(new CertPathValidatorException("Certificate path is not properly pinned.", null, certPath, -1));
                }
            } catch (PinManagerException e) {
                throw new CertificateException(e);
            }
        }
        if (newChain.length != 0) {
            if (trustAnchor.isEmpty()) {
                throw new CertificateException(new CertPathValidatorException("Trust anchor for certification path not found.", null, certPath, -1));
            }
            ChainStrengthAnalyzer.check(newChain);
            try {
                PKIXParameters params = new PKIXParameters(trustAnchor);
                params.setRevocationEnabled(false);
                params.addCertPathChecker(new ExtendedKeyUsagePKIXCertPathChecker(clientAuth, newChain[0]));
                this.validator.validate(certPath, params);
                for (int i = 1; i < newChain.length; i++) {
                    this.intermediateIndex.index(newChain[i]);
                }
            } catch (InvalidAlgorithmParameterException e2) {
                throw new CertificateException(e2);
            } catch (CertPathValidatorException e3) {
                throw new CertificateException(e3);
            }
        }
        return wholeChain;
    }

    private X509Certificate[] cleanupCertChainAndFindTrustAnchors(X509Certificate[] chain, Set<TrustAnchor> trustAnchors) {
        TrustAnchor trustAnchor;
        int currIndex = 0;
        while (currIndex < chain.length) {
            boolean foundNext = false;
            int nextIndex = currIndex + 1;
            while (true) {
                if (nextIndex >= chain.length) {
                    break;
                }
                if (!chain[currIndex].getIssuerDN().equals(chain[nextIndex].getSubjectDN())) {
                    nextIndex++;
                } else {
                    foundNext = true;
                    if (nextIndex != currIndex + 1) {
                        if (chain == chain) {
                            chain = (X509Certificate[]) chain.clone();
                        }
                        X509Certificate tempCertificate = chain[nextIndex];
                        chain[nextIndex] = chain[currIndex + 1];
                        chain[currIndex + 1] = tempCertificate;
                    }
                }
            }
            if (!foundNext) {
                break;
            }
            currIndex++;
        }
        while (true) {
            TrustAnchor nextIntermediate = this.intermediateIndex.findByIssuerAndSignature(chain[currIndex]);
            if (nextIntermediate == null) {
                break;
            }
            X509Certificate cert = nextIntermediate.getTrustedCert();
            if (chain == chain) {
                chain = (X509Certificate[]) chain.clone();
            }
            if (currIndex == chain.length - 1) {
                chain = (X509Certificate[]) Arrays.copyOf(chain, chain.length * 2);
            }
            chain[currIndex + 1] = cert;
            currIndex++;
        }
        int anchorIndex = 0;
        while (true) {
            if (anchorIndex > currIndex) {
                break;
            }
            TrustAnchor trustAnchor2 = findTrustAnchorBySubjectAndPublicKey(chain[anchorIndex]);
            if (trustAnchor2 == null) {
                anchorIndex++;
            } else {
                trustAnchors.add(trustAnchor2);
                break;
            }
        }
        int chainLength = anchorIndex;
        X509Certificate[] newChain = chainLength == chain.length ? chain : (X509Certificate[]) Arrays.copyOf(chain, chainLength);
        if (trustAnchors.isEmpty() && (trustAnchor = findTrustAnchorByIssuerAndSignature(newChain[anchorIndex - 1])) != null) {
            trustAnchors.add(trustAnchor);
        }
        return newChain;
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
            if (c == this.leaf) {
                try {
                    List<String> ekuOids = this.leaf.getExtendedKeyUsage();
                    if (ekuOids != null) {
                        boolean goodExtendedKeyUsage = false;
                        Iterator<String> it = ekuOids.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            String ekuOid = it.next();
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
                    }
                } catch (CertificateParsingException e) {
                    throw new CertPathValidatorException(e);
                }
            }
        }
    }

    private TrustAnchor findTrustAnchorByIssuerAndSignature(X509Certificate lastCert) {
        X509Certificate issuer;
        TrustAnchor trustAnchor = this.trustedCertificateIndex.findByIssuerAndSignature(lastCert);
        if (trustAnchor == null) {
            if (this.trustedCertificateStore != null && (issuer = this.trustedCertificateStore.findIssuer(lastCert)) != null) {
                return this.trustedCertificateIndex.index(issuer);
            }
            return null;
        }
        return trustAnchor;
    }

    private TrustAnchor findTrustAnchorBySubjectAndPublicKey(X509Certificate cert) {
        X509Certificate systemCert;
        TrustAnchor trustAnchor = this.trustedCertificateIndex.findBySubjectAndPublicKey(cert);
        if (trustAnchor == null) {
            if (this.trustedCertificateStore != null && (systemCert = this.trustedCertificateStore.getTrustAnchor(cert)) != null) {
                return this.trustedCertificateIndex.index(systemCert);
            }
            return null;
        }
        return trustAnchor;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.acceptedIssuers != null ? (X509Certificate[]) this.acceptedIssuers.clone() : acceptedIssuers(this.rootKeyStore);
    }
}
