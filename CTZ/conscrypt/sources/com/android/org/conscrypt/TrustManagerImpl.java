package com.android.org.conscrypt;

import com.android.org.conscrypt.ct.CTLogStore;
import com.android.org.conscrypt.ct.CTLogStoreImpl;
import com.android.org.conscrypt.ct.CTPolicy;
import com.android.org.conscrypt.ct.CTPolicyImpl;
import com.android.org.conscrypt.ct.CTVerifier;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.security.cert.PKIXRevocationChecker;
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
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;

public final class TrustManagerImpl extends X509ExtendedTrustManager {
    private static final TrustAnchorComparator TRUST_ANCHOR_COMPARATOR = new TrustAnchorComparator();
    private final X509Certificate[] acceptedIssuers;
    private final CertBlacklist blacklist;
    private boolean ctEnabledOverride;
    private CTPolicy ctPolicy;
    private CTVerifier ctVerifier;
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

    public TrustManagerImpl(KeyStore keyStore, CertPinManager certPinManager) {
        this(keyStore, certPinManager, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager certPinManager, TrustedCertificateStore trustedCertificateStore) {
        this(keyStore, certPinManager, trustedCertificateStore, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager certPinManager, TrustedCertificateStore trustedCertificateStore, CertBlacklist certBlacklist) {
        this(keyStore, certPinManager, trustedCertificateStore, certBlacklist, null, null, null);
    }

    public TrustManagerImpl(KeyStore keyStore, CertPinManager certPinManager, TrustedCertificateStore trustedCertificateStore, CertBlacklist certBlacklist, CTLogStore cTLogStore, CTVerifier cTVerifier, CTPolicy cTPolicy) {
        KeyStore keyStore2;
        CertPathValidator certPathValidator;
        CertificateFactory certificateFactory;
        CertificateFactory certificateFactory2;
        Object obj;
        TrustedCertificateStore trustedCertificateStore2;
        X509Certificate[] x509CertificateArr;
        Exception exc;
        Object obj2;
        X509Certificate[] x509CertificateArrAcceptedIssuers;
        TrustedCertificateIndex trustedCertificateIndex;
        X509Certificate[] x509CertificateArr2;
        TrustedCertificateStore trustedCertificateStore3;
        TrustedCertificateIndex trustedCertificateIndex2 = null;
        try {
            certPathValidator = CertPathValidator.getInstance("PKIX");
        } catch (Exception e) {
            e = e;
            keyStore2 = null;
            certPathValidator = null;
            certificateFactory = null;
        }
        try {
            certificateFactory = CertificateFactory.getInstance("X509");
        } catch (Exception e2) {
            e = e2;
            keyStore2 = null;
            certificateFactory = null;
            certificateFactory2 = certificateFactory;
            obj = certificateFactory2;
            obj2 = certificateFactory2;
            Object obj3 = obj2;
            exc = e;
            keyStore = keyStore2;
            trustedCertificateStore2 = obj3;
            x509CertificateArr = obj;
            if (certBlacklist == null) {
            }
            if (cTLogStore == null) {
            }
            if (cTPolicy == null) {
            }
            this.pinManager = certPinManager;
            this.rootKeyStore = keyStore;
            this.trustedCertificateStore = trustedCertificateStore2;
            this.validator = certPathValidator;
            this.factory = certificateFactory;
            this.trustedCertificateIndex = trustedCertificateIndex2;
            this.intermediateIndex = new TrustedCertificateIndex();
            this.acceptedIssuers = x509CertificateArr;
            this.err = exc;
            this.blacklist = certBlacklist;
            this.ctVerifier = new CTVerifier(cTLogStore);
            this.ctPolicy = cTPolicy;
        }
        try {
            TrustedCertificateStore trustedCertificateStore4 = trustedCertificateStore;
            if ("AndroidCAStore".equals(keyStore.getType())) {
                if (trustedCertificateStore != null) {
                    try {
                        trustedCertificateIndex = new TrustedCertificateIndex();
                        x509CertificateArr2 = null;
                        trustedCertificateStore3 = trustedCertificateStore4;
                        TrustedCertificateIndex trustedCertificateIndex3 = trustedCertificateIndex;
                        exc = null;
                        trustedCertificateIndex2 = trustedCertificateIndex3;
                        x509CertificateArr = x509CertificateArr2;
                        trustedCertificateStore2 = trustedCertificateStore3;
                    } catch (Exception e3) {
                        obj = null;
                        TrustedCertificateStore trustedCertificateStore5 = trustedCertificateStore4;
                        keyStore2 = keyStore;
                        e = e3;
                        obj2 = trustedCertificateStore5;
                        Object obj32 = obj2;
                        exc = e;
                        keyStore = keyStore2;
                        trustedCertificateStore2 = obj32;
                        x509CertificateArr = obj;
                    }
                } else {
                    try {
                        trustedCertificateStore4 = new TrustedCertificateStore();
                        trustedCertificateIndex = new TrustedCertificateIndex();
                        x509CertificateArr2 = null;
                        trustedCertificateStore3 = trustedCertificateStore4;
                        TrustedCertificateIndex trustedCertificateIndex32 = trustedCertificateIndex;
                        exc = null;
                        trustedCertificateIndex2 = trustedCertificateIndex32;
                        x509CertificateArr = x509CertificateArr2;
                        trustedCertificateStore2 = trustedCertificateStore3;
                    } catch (Exception e4) {
                        obj2 = null;
                        obj = null;
                        keyStore2 = keyStore;
                        e = e4;
                        Object obj322 = obj2;
                        exc = e;
                        keyStore = keyStore2;
                        trustedCertificateStore2 = obj322;
                        x509CertificateArr = obj;
                        if (certBlacklist == null) {
                        }
                        if (cTLogStore == null) {
                        }
                        if (cTPolicy == null) {
                        }
                        this.pinManager = certPinManager;
                        this.rootKeyStore = keyStore;
                        this.trustedCertificateStore = trustedCertificateStore2;
                        this.validator = certPathValidator;
                        this.factory = certificateFactory;
                        this.trustedCertificateIndex = trustedCertificateIndex2;
                        this.intermediateIndex = new TrustedCertificateIndex();
                        this.acceptedIssuers = x509CertificateArr;
                        this.err = exc;
                        this.blacklist = certBlacklist;
                        this.ctVerifier = new CTVerifier(cTLogStore);
                        this.ctPolicy = cTPolicy;
                    }
                }
            } else {
                try {
                    x509CertificateArrAcceptedIssuers = acceptedIssuers(keyStore);
                } catch (Exception e5) {
                    e = e5;
                    obj2 = trustedCertificateStore;
                    keyStore2 = null;
                    obj = null;
                }
                try {
                    trustedCertificateIndex = new TrustedCertificateIndex(trustAnchors(x509CertificateArrAcceptedIssuers));
                    x509CertificateArr2 = x509CertificateArrAcceptedIssuers;
                    keyStore = null;
                    trustedCertificateStore3 = trustedCertificateStore;
                    TrustedCertificateIndex trustedCertificateIndex322 = trustedCertificateIndex;
                    exc = null;
                    trustedCertificateIndex2 = trustedCertificateIndex322;
                    x509CertificateArr = x509CertificateArr2;
                    trustedCertificateStore2 = trustedCertificateStore3;
                } catch (Exception e6) {
                    obj = x509CertificateArrAcceptedIssuers;
                    e = e6;
                    obj2 = trustedCertificateStore;
                    keyStore2 = null;
                    Object obj3222 = obj2;
                    exc = e;
                    keyStore = keyStore2;
                    trustedCertificateStore2 = obj3222;
                    x509CertificateArr = obj;
                }
            }
        } catch (Exception e7) {
            e = e7;
            keyStore2 = null;
            certificateFactory2 = null;
            obj = certificateFactory2;
            obj2 = certificateFactory2;
            Object obj32222 = obj2;
            exc = e;
            keyStore = keyStore2;
            trustedCertificateStore2 = obj32222;
            x509CertificateArr = obj;
            if (certBlacklist == null) {
            }
            if (cTLogStore == null) {
            }
            if (cTPolicy == null) {
            }
            this.pinManager = certPinManager;
            this.rootKeyStore = keyStore;
            this.trustedCertificateStore = trustedCertificateStore2;
            this.validator = certPathValidator;
            this.factory = certificateFactory;
            this.trustedCertificateIndex = trustedCertificateIndex2;
            this.intermediateIndex = new TrustedCertificateIndex();
            this.acceptedIssuers = x509CertificateArr;
            this.err = exc;
            this.blacklist = certBlacklist;
            this.ctVerifier = new CTVerifier(cTLogStore);
            this.ctPolicy = cTPolicy;
        }
        certBlacklist = certBlacklist == null ? CertBlacklist.getDefault() : certBlacklist;
        cTLogStore = cTLogStore == null ? new CTLogStoreImpl() : cTLogStore;
        cTPolicy = cTPolicy == null ? new CTPolicyImpl(cTLogStore, 2) : cTPolicy;
        this.pinManager = certPinManager;
        this.rootKeyStore = keyStore;
        this.trustedCertificateStore = trustedCertificateStore2;
        this.validator = certPathValidator;
        this.factory = certificateFactory;
        this.trustedCertificateIndex = trustedCertificateIndex2;
        this.intermediateIndex = new TrustedCertificateIndex();
        this.acceptedIssuers = x509CertificateArr;
        this.err = exc;
        this.blacklist = certBlacklist;
        this.ctVerifier = new CTVerifier(cTLogStore);
        this.ctPolicy = cTPolicy;
    }

    private static X509Certificate[] acceptedIssuers(KeyStore keyStore) {
        try {
            ArrayList arrayList = new ArrayList();
            Enumeration<String> enumerationAliases = keyStore.aliases();
            while (enumerationAliases.hasMoreElements()) {
                X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(enumerationAliases.nextElement());
                if (x509Certificate != null) {
                    arrayList.add(x509Certificate);
                }
            }
            return (X509Certificate[]) arrayList.toArray(new X509Certificate[arrayList.size()]);
        } catch (KeyStoreException e) {
            return new X509Certificate[0];
        }
    }

    private static Set<TrustAnchor> trustAnchors(X509Certificate[] x509CertificateArr) {
        HashSet hashSet = new HashSet(x509CertificateArr.length);
        for (X509Certificate x509Certificate : x509CertificateArr) {
            hashSet.add(new TrustAnchor(x509Certificate, null));
        }
        return hashSet;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
        checkTrusted(x509CertificateArr, str, null, null, true);
    }

    public List<X509Certificate> checkClientTrusted(X509Certificate[] x509CertificateArr, String str, String str2) throws CertificateException {
        return checkTrusted(x509CertificateArr, null, null, str, str2, true);
    }

    private static SSLSession getHandshakeSessionOrThrow(SSLSocket sSLSocket) throws CertificateException {
        SSLSession handshakeSession = sSLSocket.getHandshakeSession();
        if (handshakeSession == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        return handshakeSession;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str, Socket socket) throws CertificateException {
        SSLSession sSLSession;
        SSLParameters sSLParameters;
        if (socket instanceof SSLSocket) {
            SSLSocket sSLSocket = (SSLSocket) socket;
            SSLSession handshakeSessionOrThrow = getHandshakeSessionOrThrow(sSLSocket);
            sSLParameters = sSLSocket.getSSLParameters();
            sSLSession = handshakeSessionOrThrow;
        } else {
            sSLSession = null;
            sSLParameters = null;
        }
        checkTrusted(x509CertificateArr, str, sSLSession, sSLParameters, true);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str, SSLEngine sSLEngine) throws CertificateException {
        SSLSession handshakeSession = sSLEngine.getHandshakeSession();
        if (handshakeSession == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        checkTrusted(x509CertificateArr, str, handshakeSession, sSLEngine.getSSLParameters(), true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
        checkTrusted(x509CertificateArr, str, null, null, false);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] x509CertificateArr, String str, String str2) throws CertificateException {
        return checkTrusted(x509CertificateArr, null, null, str, str2, false);
    }

    public List<X509Certificate> getTrustedChainForServer(X509Certificate[] x509CertificateArr, String str, Socket socket) throws CertificateException {
        SSLSession sSLSession;
        SSLParameters sSLParameters;
        if (socket instanceof SSLSocket) {
            SSLSocket sSLSocket = (SSLSocket) socket;
            SSLSession handshakeSessionOrThrow = getHandshakeSessionOrThrow(sSLSocket);
            sSLParameters = sSLSocket.getSSLParameters();
            sSLSession = handshakeSessionOrThrow;
        } else {
            sSLSession = null;
            sSLParameters = null;
        }
        return checkTrusted(x509CertificateArr, str, sSLSession, sSLParameters, false);
    }

    public List<X509Certificate> getTrustedChainForServer(X509Certificate[] x509CertificateArr, String str, SSLEngine sSLEngine) throws CertificateException {
        SSLSession handshakeSession = sSLEngine.getHandshakeSession();
        if (handshakeSession == null) {
            throw new CertificateException("Not in handshake; no session available");
        }
        return checkTrusted(x509CertificateArr, str, handshakeSession, sSLEngine.getSSLParameters(), false);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str, Socket socket) throws CertificateException {
        getTrustedChainForServer(x509CertificateArr, str, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str, SSLEngine sSLEngine) throws CertificateException {
        getTrustedChainForServer(x509CertificateArr, str, sSLEngine);
    }

    public boolean isUserAddedCertificate(X509Certificate x509Certificate) {
        if (this.trustedCertificateStore == null) {
            return false;
        }
        return this.trustedCertificateStore.isUserAddedCertificate(x509Certificate);
    }

    public List<X509Certificate> checkServerTrusted(X509Certificate[] x509CertificateArr, String str, SSLSession sSLSession) throws CertificateException {
        return checkTrusted(x509CertificateArr, str, sSLSession, null, false);
    }

    public void handleTrustStorageUpdate() {
        if (this.acceptedIssuers == null) {
            this.trustedCertificateIndex.reset();
        } else {
            this.trustedCertificateIndex.reset(trustAnchors(this.acceptedIssuers));
        }
    }

    private List<X509Certificate> checkTrusted(X509Certificate[] x509CertificateArr, String str, SSLSession sSLSession, SSLParameters sSLParameters, boolean z) throws CertificateException {
        byte[] ocspDataFromSession;
        byte[] tlsSctDataFromSession;
        String peerHost;
        String endpointIdentificationAlgorithm;
        if (sSLSession == null) {
            ocspDataFromSession = null;
            tlsSctDataFromSession = null;
            peerHost = null;
        } else {
            peerHost = sSLSession.getPeerHost();
            ocspDataFromSession = getOcspDataFromSession(sSLSession);
            tlsSctDataFromSession = getTlsSctDataFromSession(sSLSession);
        }
        if (sSLSession != null && sSLParameters != null && (endpointIdentificationAlgorithm = sSLParameters.getEndpointIdentificationAlgorithm()) != null && "HTTPS".equals(endpointIdentificationAlgorithm.toUpperCase(Locale.US)) && !HttpsURLConnection.getDefaultHostnameVerifier().verify(peerHost, sSLSession)) {
            throw new CertificateException("No subjectAltNames on the certificate match");
        }
        return checkTrusted(x509CertificateArr, ocspDataFromSession, tlsSctDataFromSession, str, peerHost, z);
    }

    private byte[] getOcspDataFromSession(SSLSession sSLSession) {
        List<byte[]> statusResponses;
        if (sSLSession instanceof ConscryptSession) {
            statusResponses = ((ConscryptSession) sSLSession).getStatusResponses();
        } else {
            try {
                Method declaredMethod = sSLSession.getClass().getDeclaredMethod("getStatusResponses", new Class[0]);
                declaredMethod.setAccessible(true);
                Object objInvoke = declaredMethod.invoke(sSLSession, new Object[0]);
                if (objInvoke instanceof List) {
                    statusResponses = (List) objInvoke;
                } else {
                    statusResponses = null;
                }
            } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
                statusResponses = null;
            } catch (InvocationTargetException e2) {
                throw new RuntimeException(e2.getCause());
            }
        }
        if (statusResponses == null || statusResponses.isEmpty()) {
            return null;
        }
        return statusResponses.get(0);
    }

    private byte[] getTlsSctDataFromSession(SSLSession sSLSession) {
        if (sSLSession instanceof ConscryptSession) {
            return ((ConscryptSession) sSLSession).getPeerSignedCertificateTimestamp();
        }
        try {
            Method declaredMethod = sSLSession.getClass().getDeclaredMethod("getPeerSignedCertificateTimestamp", new Class[0]);
            declaredMethod.setAccessible(true);
            Object objInvoke = declaredMethod.invoke(sSLSession, new Object[0]);
            if (!(objInvoke instanceof byte[])) {
                return null;
            }
            return (byte[]) objInvoke;
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
            return null;
        } catch (InvocationTargetException e2) {
            throw new RuntimeException(e2.getCause());
        }
    }

    private List<X509Certificate> checkTrusted(X509Certificate[] x509CertificateArr, byte[] bArr, byte[] bArr2, String str, String str2, boolean z) throws CertificateException {
        if (x509CertificateArr == null || x509CertificateArr.length == 0 || str == null || str.length() == 0) {
            throw new IllegalArgumentException("null or zero-length parameter");
        }
        if (this.err != null) {
            throw new CertificateException(this.err);
        }
        HashSet hashSet = new HashSet();
        ArrayList<X509Certificate> arrayList = new ArrayList<>();
        ArrayList<TrustAnchor> arrayList2 = new ArrayList<>();
        X509Certificate x509Certificate = x509CertificateArr[0];
        TrustAnchor trustAnchorFindTrustAnchorBySubjectAndPublicKey = findTrustAnchorBySubjectAndPublicKey(x509Certificate);
        if (trustAnchorFindTrustAnchorBySubjectAndPublicKey != null) {
            arrayList2.add(trustAnchorFindTrustAnchorBySubjectAndPublicKey);
            hashSet.add(trustAnchorFindTrustAnchorBySubjectAndPublicKey.getTrustedCert());
        } else {
            arrayList.add(x509Certificate);
        }
        hashSet.add(x509Certificate);
        return checkTrustedRecursive(x509CertificateArr, bArr, bArr2, str2, z, arrayList, arrayList2, hashSet);
    }

    private List<X509Certificate> checkTrustedRecursive(X509Certificate[] x509CertificateArr, byte[] bArr, byte[] bArr2, String str, boolean z, ArrayList<X509Certificate> arrayList, ArrayList<TrustAnchor> arrayList2, Set<X509Certificate> set) throws CertificateException {
        X509Certificate trustedCert;
        if (arrayList2.isEmpty()) {
            trustedCert = arrayList.get(arrayList.size() - 1);
        } else {
            trustedCert = arrayList2.get(arrayList2.size() - 1).getTrustedCert();
        }
        X509Certificate x509Certificate = trustedCert;
        checkBlacklist(x509Certificate);
        if (x509Certificate.getIssuerDN().equals(x509Certificate.getSubjectDN())) {
            return verifyChain(arrayList, arrayList2, str, z, bArr, bArr2);
        }
        boolean z2 = false;
        CertificateException e = null;
        for (TrustAnchor trustAnchor : sortPotentialAnchors(findAllTrustAnchorsByIssuerAndSignature(x509Certificate))) {
            X509Certificate trustedCert2 = trustAnchor.getTrustedCert();
            if (!set.contains(trustedCert2)) {
                set.add(trustedCert2);
                arrayList2.add(trustAnchor);
                try {
                    return checkTrustedRecursive(x509CertificateArr, bArr, bArr2, str, z, arrayList, arrayList2, set);
                } catch (CertificateException e2) {
                    e = e2;
                    arrayList2.remove(arrayList2.size() - 1);
                    set.remove(trustedCert2);
                    z2 = true;
                }
            }
        }
        if (!arrayList2.isEmpty()) {
            if (!z2) {
                return verifyChain(arrayList, arrayList2, str, z, bArr, bArr2);
            }
            throw e;
        }
        for (int i = 1; i < x509CertificateArr.length; i++) {
            X509Certificate x509Certificate2 = x509CertificateArr[i];
            if (!set.contains(x509Certificate2) && x509Certificate.getIssuerDN().equals(x509Certificate2.getSubjectDN())) {
                try {
                    x509Certificate2.checkValidity();
                    ChainStrengthAnalyzer.checkCert(x509Certificate2);
                    set.add(x509Certificate2);
                    arrayList.add(x509Certificate2);
                    try {
                        return checkTrustedRecursive(x509CertificateArr, bArr, bArr2, str, z, arrayList, arrayList2, set);
                    } catch (CertificateException e3) {
                        e = e3;
                        set.remove(x509Certificate2);
                        arrayList.remove(arrayList.size() - 1);
                    }
                } catch (CertificateException e4) {
                    e = new CertificateException("Unacceptable certificate: " + x509Certificate2.getSubjectX500Principal(), e4);
                }
            }
        }
        Iterator<TrustAnchor> it = sortPotentialAnchors(this.intermediateIndex.findAllByIssuerAndSignature(x509Certificate)).iterator();
        while (it.hasNext()) {
            X509Certificate trustedCert3 = it.next().getTrustedCert();
            if (!set.contains(trustedCert3)) {
                set.add(trustedCert3);
                arrayList.add(trustedCert3);
                try {
                    return checkTrustedRecursive(x509CertificateArr, bArr, bArr2, str, z, arrayList, arrayList2, set);
                } catch (CertificateException e5) {
                    e = e5;
                    arrayList.remove(arrayList.size() - 1);
                    set.remove(trustedCert3);
                }
            }
        }
        if (e != null) {
            throw e;
        }
        throw new CertificateException(new CertPathValidatorException("Trust anchor for certification path not found.", null, this.factory.generateCertPath(arrayList), -1));
    }

    private List<X509Certificate> verifyChain(List<X509Certificate> list, List<TrustAnchor> list2, String str, boolean z, byte[] bArr, byte[] bArr2) throws CertificateException {
        CertPath certPathGenerateCertPath = this.factory.generateCertPath(list);
        if (list2.isEmpty()) {
            throw new CertificateException(new CertPathValidatorException("Trust anchor for certification path not found.", null, certPathGenerateCertPath, -1));
        }
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(list);
        Iterator<TrustAnchor> it = list2.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getTrustedCert());
        }
        if (this.pinManager != null) {
            this.pinManager.checkChainPinning(str, arrayList);
        }
        Iterator<X509Certificate> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            checkBlacklist(it2.next());
        }
        if (!z && (this.ctEnabledOverride || (str != null && Platform.isCTVerificationRequired(str)))) {
            checkCT(str, arrayList, bArr, bArr2);
        }
        if (list.isEmpty()) {
            return arrayList;
        }
        ChainStrengthAnalyzer.check(list);
        try {
            HashSet hashSet = new HashSet();
            hashSet.add(list2.get(0));
            PKIXParameters pKIXParameters = new PKIXParameters(hashSet);
            pKIXParameters.setRevocationEnabled(false);
            X509Certificate x509Certificate = list.get(0);
            setOcspResponses(pKIXParameters, x509Certificate, bArr);
            pKIXParameters.addCertPathChecker(new ExtendedKeyUsagePKIXCertPathChecker(z, x509Certificate));
            this.validator.validate(certPathGenerateCertPath, pKIXParameters);
            for (int i = 1; i < list.size(); i++) {
                this.intermediateIndex.index(list.get(i));
            }
            return arrayList;
        } catch (InvalidAlgorithmParameterException e) {
            throw new CertificateException("Chain validation failed", e);
        } catch (CertPathValidatorException e2) {
            throw new CertificateException("Chain validation failed", e2);
        }
    }

    private void checkBlacklist(X509Certificate x509Certificate) throws CertificateException {
        if (this.blacklist.isPublicKeyBlackListed(x509Certificate.getPublicKey())) {
            throw new CertificateException("Certificate blacklisted by public key: " + x509Certificate);
        }
    }

    private void checkCT(String str, List<X509Certificate> list, byte[] bArr, byte[] bArr2) throws CertificateException {
        if (!this.ctPolicy.doesResultConformToPolicy(this.ctVerifier.verifySignedCertificateTimestamps(list, bArr2, bArr), str, (X509Certificate[]) list.toArray(new X509Certificate[list.size()]))) {
            throw new CertificateException("Certificate chain does not conform to required transparency policy.");
        }
    }

    private void setOcspResponses(PKIXParameters pKIXParameters, X509Certificate x509Certificate, byte[] bArr) {
        if (bArr == null) {
            return;
        }
        PKIXRevocationChecker pKIXRevocationChecker = null;
        List<PKIXCertPathChecker> arrayList = new ArrayList<>(pKIXParameters.getCertPathCheckers());
        Iterator<PKIXCertPathChecker> it = arrayList.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            PKIXCertPathChecker next = it.next();
            if (next instanceof PKIXRevocationChecker) {
                pKIXRevocationChecker = (PKIXRevocationChecker) next;
                break;
            }
        }
        if (pKIXRevocationChecker == null) {
            try {
                pKIXRevocationChecker = (PKIXRevocationChecker) this.validator.getRevocationChecker();
                arrayList.add(pKIXRevocationChecker);
                pKIXRevocationChecker.setOptions(Collections.singleton(PKIXRevocationChecker.Option.ONLY_END_ENTITY));
            } catch (UnsupportedOperationException e) {
                return;
            }
        }
        pKIXRevocationChecker.setOcspResponses(Collections.singletonMap(x509Certificate, bArr));
        pKIXParameters.setCertPathCheckers(arrayList);
    }

    private static Collection<TrustAnchor> sortPotentialAnchors(Set<TrustAnchor> set) {
        if (set.size() <= 1) {
            return set;
        }
        ArrayList arrayList = new ArrayList(set);
        Collections.sort(arrayList, TRUST_ANCHOR_COMPARATOR);
        return arrayList;
    }

    private static class TrustAnchorComparator implements Comparator<TrustAnchor> {
        private static final CertificatePriorityComparator CERT_COMPARATOR = new CertificatePriorityComparator();

        private TrustAnchorComparator() {
        }

        @Override
        public int compare(TrustAnchor trustAnchor, TrustAnchor trustAnchor2) {
            return CERT_COMPARATOR.compare(trustAnchor.getTrustedCert(), trustAnchor2.getTrustedCert());
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

        private ExtendedKeyUsagePKIXCertPathChecker(boolean z, X509Certificate x509Certificate) {
            this.clientAuth = z;
            this.leaf = x509Certificate;
        }

        @Override
        public void init(boolean z) throws CertPathValidatorException {
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
        public void check(Certificate certificate, Collection<String> collection) throws CertPathValidatorException {
            boolean z;
            if (certificate != this.leaf) {
                return;
            }
            try {
                List<String> extendedKeyUsage = this.leaf.getExtendedKeyUsage();
                if (extendedKeyUsage == null) {
                    return;
                }
                Iterator<String> it = extendedKeyUsage.iterator();
                while (true) {
                    z = true;
                    if (it.hasNext()) {
                        String next = it.next();
                        if (!next.equals(EKU_anyExtendedKeyUsage)) {
                            if (this.clientAuth) {
                                if (next.equals(EKU_clientAuth)) {
                                    break;
                                }
                            } else if (next.equals(EKU_serverAuth) || next.equals(EKU_nsSGC) || next.equals(EKU_msSGC)) {
                                break;
                            }
                        } else {
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (z) {
                    collection.remove(EKU_OID);
                    return;
                }
                throw new CertPathValidatorException("End-entity certificate does not have a valid extendedKeyUsage.");
            } catch (CertificateParsingException e) {
                throw new CertPathValidatorException(e);
            }
        }
    }

    private Set<TrustAnchor> findAllTrustAnchorsByIssuerAndSignature(X509Certificate x509Certificate) {
        Set<TrustAnchor> setFindAllByIssuerAndSignature = this.trustedCertificateIndex.findAllByIssuerAndSignature(x509Certificate);
        if (!setFindAllByIssuerAndSignature.isEmpty() || this.trustedCertificateStore == null) {
            return setFindAllByIssuerAndSignature;
        }
        Set<X509Certificate> setFindAllIssuers = this.trustedCertificateStore.findAllIssuers(x509Certificate);
        if (setFindAllIssuers.isEmpty()) {
            return setFindAllByIssuerAndSignature;
        }
        HashSet hashSet = new HashSet(setFindAllIssuers.size());
        Iterator<X509Certificate> it = setFindAllIssuers.iterator();
        while (it.hasNext()) {
            hashSet.add(this.trustedCertificateIndex.index(it.next()));
        }
        return hashSet;
    }

    private TrustAnchor findTrustAnchorBySubjectAndPublicKey(X509Certificate x509Certificate) {
        X509Certificate trustAnchor;
        TrustAnchor trustAnchorFindBySubjectAndPublicKey = this.trustedCertificateIndex.findBySubjectAndPublicKey(x509Certificate);
        if (trustAnchorFindBySubjectAndPublicKey != null) {
            return trustAnchorFindBySubjectAndPublicKey;
        }
        if (this.trustedCertificateStore == null || (trustAnchor = this.trustedCertificateStore.getTrustAnchor(x509Certificate)) == null) {
            return null;
        }
        return new TrustAnchor(trustAnchor, null);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return this.acceptedIssuers != null ? (X509Certificate[]) this.acceptedIssuers.clone() : acceptedIssuers(this.rootKeyStore);
    }

    public void setCTEnabledOverride(boolean z) {
        this.ctEnabledOverride = z;
    }

    public void setCTVerifier(CTVerifier cTVerifier) {
        this.ctVerifier = cTVerifier;
    }

    public void setCTPolicy(CTPolicy cTPolicy) {
        this.ctPolicy = cTPolicy;
    }
}
