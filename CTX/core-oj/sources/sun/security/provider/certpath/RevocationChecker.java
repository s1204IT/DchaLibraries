package sun.security.provider.certpath;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CRLReason;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateRevokedException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.Extension;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509CRLSelector;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import sun.security.provider.certpath.OCSP;
import sun.security.provider.certpath.OCSPResponse;
import sun.security.provider.certpath.PKIX;
import sun.security.util.Debug;
import sun.security.x509.AccessDescription;
import sun.security.x509.AuthorityInfoAccessExtension;
import sun.security.x509.CRLDistributionPointsExtension;
import sun.security.x509.DistributionPoint;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.X500Name;
import sun.security.x509.X509CRLEntryImpl;
import sun.security.x509.X509CertImpl;

class RevocationChecker extends PKIXRevocationChecker {
    private static final String HEX_DIGITS = "0123456789ABCDEFabcdef";
    private static final long MAX_CLOCK_SKEW = 900000;
    private TrustAnchor anchor;
    private int certIndex;
    private List<CertStore> certStores;
    private boolean crlDP;
    private boolean crlSignFlag;
    private X509Certificate issuerCert;
    private List<Extension> ocspExtensions;
    private Map<X509Certificate, byte[]> ocspResponses;
    private boolean onlyEE;
    private PKIX.ValidatorParams params;
    private PublicKey prevPubKey;
    private X509Certificate responderCert;
    private URI responderURI;
    private boolean softFail;
    private static final Debug debug = Debug.getInstance("certpath");
    private static final boolean[] ALL_REASONS = {true, true, true, true, true, true, true, true, true};
    private static final boolean[] CRL_SIGN_USAGE = {false, false, false, false, false, false, true};
    private LinkedList<CertPathValidatorException> softFailExceptions = new LinkedList<>();
    private Mode mode = Mode.PREFER_OCSP;
    private boolean legacy = false;

    private enum Mode {
        PREFER_OCSP,
        PREFER_CRLS,
        ONLY_CRLS,
        ONLY_OCSP
    }

    private static class RevocationProperties {
        boolean crlDPEnabled;
        boolean ocspEnabled;
        String ocspIssuer;
        String ocspSerial;
        String ocspSubject;
        String ocspUrl;
        boolean onlyEE;

        private RevocationProperties() {
        }
    }

    RevocationChecker() {
    }

    RevocationChecker(TrustAnchor trustAnchor, PKIX.ValidatorParams validatorParams) throws CertPathValidatorException {
        init(trustAnchor, validatorParams);
    }

    void init(TrustAnchor trustAnchor, PKIX.ValidatorParams validatorParams) throws CertPathValidatorException {
        RevocationProperties revocationProperties = getRevocationProperties();
        URI ocspResponder = getOcspResponder();
        if (ocspResponder == null) {
            ocspResponder = toURI(revocationProperties.ocspUrl);
        }
        this.responderURI = ocspResponder;
        X509Certificate ocspResponderCert = getOcspResponderCert();
        if (ocspResponderCert == null) {
            ocspResponderCert = getResponderCert(revocationProperties, validatorParams.trustAnchors(), validatorParams.certStores());
        }
        this.responderCert = ocspResponderCert;
        Set<PKIXRevocationChecker.Option> options = getOptions();
        for (PKIXRevocationChecker.Option option : options) {
            switch (option) {
                case ONLY_END_ENTITY:
                case PREFER_CRLS:
                case SOFT_FAIL:
                case NO_FALLBACK:
                    break;
                default:
                    throw new CertPathValidatorException("Unrecognized revocation parameter option: " + ((Object) option));
            }
        }
        this.softFail = options.contains(PKIXRevocationChecker.Option.SOFT_FAIL);
        if (this.legacy) {
            this.mode = revocationProperties.ocspEnabled ? Mode.PREFER_OCSP : Mode.ONLY_CRLS;
            this.onlyEE = revocationProperties.onlyEE;
        } else {
            if (options.contains(PKIXRevocationChecker.Option.NO_FALLBACK)) {
                if (options.contains(PKIXRevocationChecker.Option.PREFER_CRLS)) {
                    this.mode = Mode.ONLY_CRLS;
                } else {
                    this.mode = Mode.ONLY_OCSP;
                }
            } else if (options.contains(PKIXRevocationChecker.Option.PREFER_CRLS)) {
                this.mode = Mode.PREFER_CRLS;
            }
            this.onlyEE = options.contains(PKIXRevocationChecker.Option.ONLY_END_ENTITY);
        }
        if (this.legacy) {
            this.crlDP = revocationProperties.crlDPEnabled;
        } else {
            this.crlDP = true;
        }
        this.ocspResponses = getOcspResponses();
        this.ocspExtensions = getOcspExtensions();
        this.anchor = trustAnchor;
        this.params = validatorParams;
        this.certStores = new ArrayList(validatorParams.certStores());
        try {
            this.certStores.add(CertStore.getInstance("Collection", new CollectionCertStoreParameters(validatorParams.certificates())));
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            if (debug != null) {
                debug.println("RevocationChecker: error creating Collection CertStore: " + e);
            }
        }
    }

    private static URI toURI(String str) throws CertPathValidatorException {
        if (str != null) {
            try {
                return new URI(str);
            } catch (URISyntaxException e) {
                throw new CertPathValidatorException("cannot parse ocsp.responderURL property", e);
            }
        }
        return null;
    }

    private static RevocationProperties getRevocationProperties() {
        return (RevocationProperties) AccessController.doPrivileged(new PrivilegedAction<RevocationProperties>() {
            @Override
            public RevocationProperties run() {
                RevocationProperties revocationProperties = new RevocationProperties();
                String property = Security.getProperty("com.sun.security.onlyCheckRevocationOfEECert");
                boolean z = false;
                revocationProperties.onlyEE = property != null && property.equalsIgnoreCase("true");
                String property2 = Security.getProperty("ocsp.enable");
                if (property2 != null && property2.equalsIgnoreCase("true")) {
                    z = true;
                }
                revocationProperties.ocspEnabled = z;
                revocationProperties.ocspUrl = Security.getProperty("ocsp.responderURL");
                revocationProperties.ocspSubject = Security.getProperty("ocsp.responderCertSubjectName");
                revocationProperties.ocspIssuer = Security.getProperty("ocsp.responderCertIssuerName");
                revocationProperties.ocspSerial = Security.getProperty("ocsp.responderCertSerialNumber");
                revocationProperties.crlDPEnabled = Boolean.getBoolean("com.sun.security.enableCRLDP");
                return revocationProperties;
            }
        });
    }

    private static X509Certificate getResponderCert(RevocationProperties revocationProperties, Set<TrustAnchor> set, List<CertStore> list) throws CertPathValidatorException {
        if (revocationProperties.ocspSubject != null) {
            return getResponderCert(revocationProperties.ocspSubject, set, list);
        }
        if (revocationProperties.ocspIssuer != null && revocationProperties.ocspSerial != null) {
            return getResponderCert(revocationProperties.ocspIssuer, revocationProperties.ocspSerial, set, list);
        }
        if (revocationProperties.ocspIssuer != null || revocationProperties.ocspSerial != null) {
            throw new CertPathValidatorException("Must specify both ocsp.responderCertIssuerName and ocsp.responderCertSerialNumber properties");
        }
        return null;
    }

    private static X509Certificate getResponderCert(String str, Set<TrustAnchor> set, List<CertStore> list) throws CertPathValidatorException {
        X509CertSelector x509CertSelector = new X509CertSelector();
        try {
            x509CertSelector.setSubject(new X500Principal(str));
            return getResponderCert(x509CertSelector, set, list);
        } catch (IllegalArgumentException e) {
            throw new CertPathValidatorException("cannot parse ocsp.responderCertSubjectName property", e);
        }
    }

    private static X509Certificate getResponderCert(String str, String str2, Set<TrustAnchor> set, List<CertStore> list) throws CertPathValidatorException {
        X509CertSelector x509CertSelector = new X509CertSelector();
        try {
            x509CertSelector.setIssuer(new X500Principal(str));
            try {
                x509CertSelector.setSerialNumber(new BigInteger(stripOutSeparators(str2), 16));
                return getResponderCert(x509CertSelector, set, list);
            } catch (NumberFormatException e) {
                throw new CertPathValidatorException("cannot parse ocsp.responderCertSerialNumber property", e);
            }
        } catch (IllegalArgumentException e2) {
            throw new CertPathValidatorException("cannot parse ocsp.responderCertIssuerName property", e2);
        }
    }

    private static X509Certificate getResponderCert(X509CertSelector x509CertSelector, Set<TrustAnchor> set, List<CertStore> list) throws CertPathValidatorException {
        Iterator<TrustAnchor> it = set.iterator();
        while (it.hasNext()) {
            X509Certificate trustedCert = it.next().getTrustedCert();
            if (trustedCert != null && x509CertSelector.match(trustedCert)) {
                return trustedCert;
            }
        }
        Iterator<CertStore> it2 = list.iterator();
        while (it2.hasNext()) {
            try {
                Collection<? extends Certificate> certificates = it2.next().getCertificates(x509CertSelector);
                if (!certificates.isEmpty()) {
                    return (X509Certificate) certificates.iterator().next();
                }
                continue;
            } catch (CertStoreException e) {
                if (debug != null) {
                    debug.println("CertStore exception:" + ((Object) e));
                }
            }
        }
        throw new CertPathValidatorException("Cannot find the responder's certificate (set using the OCSP security properties).");
    }

    @Override
    public void init(boolean z) throws CertPathValidatorException {
        if (z) {
            throw new CertPathValidatorException("forward checking not supported");
        }
        if (this.anchor != null) {
            this.issuerCert = this.anchor.getTrustedCert();
            this.prevPubKey = this.issuerCert != null ? this.issuerCert.getPublicKey() : this.anchor.getCAPublicKey();
        }
        this.crlSignFlag = true;
        if (this.params != null && this.params.certPath() != null) {
            this.certIndex = this.params.certPath().getCertificates().size() - 1;
        } else {
            this.certIndex = -1;
        }
        this.softFailExceptions.clear();
    }

    @Override
    public boolean isForwardCheckingSupported() {
        return false;
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return null;
    }

    @Override
    public List<CertPathValidatorException> getSoftFailExceptions() {
        return Collections.unmodifiableList(this.softFailExceptions);
    }

    @Override
    public void check(Certificate certificate, Collection<String> collection) throws CertPathValidatorException {
        check((X509Certificate) certificate, collection, this.prevPubKey, this.crlSignFlag);
    }

    private void check(X509Certificate x509Certificate, Collection<String> collection, PublicKey publicKey, boolean z) throws CertPathValidatorException {
        if (debug != null) {
            debug.println("RevocationChecker.check: checking cert\n  SN: " + Debug.toHexString(x509Certificate.getSerialNumber()) + "\n  Subject: " + ((Object) x509Certificate.getSubjectX500Principal()) + "\n  Issuer: " + ((Object) x509Certificate.getIssuerX500Principal()));
        }
        try {
            try {
            } catch (CertPathValidatorException e) {
                if (e.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                    throw e;
                }
                boolean zIsSoftFailException = isSoftFailException(e);
                if (zIsSoftFailException) {
                    if (this.mode == Mode.ONLY_OCSP || this.mode == Mode.ONLY_CRLS) {
                        updateState(x509Certificate);
                        return;
                    }
                } else if (this.mode == Mode.ONLY_OCSP || this.mode == Mode.ONLY_CRLS) {
                    throw e;
                }
                if (debug != null) {
                    debug.println("RevocationChecker.check() " + e.getMessage());
                    debug.println("RevocationChecker.check() preparing to failover");
                }
                try {
                    int i = AnonymousClass2.$SwitchMap$sun$security$provider$certpath$RevocationChecker$Mode[this.mode.ordinal()];
                    if (i == 1) {
                        checkCRLs(x509Certificate, collection, null, publicKey, z);
                    } else if (i == 3) {
                        checkOCSP(x509Certificate, collection);
                    }
                } catch (CertPathValidatorException e2) {
                    if (debug != null) {
                        debug.println("RevocationChecker.check() failover failed");
                        debug.println("RevocationChecker.check() " + e2.getMessage());
                    }
                    if (e2.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                        throw e2;
                    }
                    if (!isSoftFailException(e2)) {
                        e.addSuppressed(e2);
                        throw e;
                    }
                    if (!zIsSoftFailException) {
                        throw e;
                    }
                }
            }
            if (this.onlyEE && x509Certificate.getBasicConstraints() != -1) {
                if (debug != null) {
                    debug.println("Skipping revocation check; cert is not an end entity cert");
                }
                updateState(x509Certificate);
            } else {
                switch (this.mode) {
                    case PREFER_OCSP:
                    case ONLY_OCSP:
                        checkOCSP(x509Certificate, collection);
                        break;
                    case PREFER_CRLS:
                    case ONLY_CRLS:
                        checkCRLs(x509Certificate, collection, null, publicKey, z);
                        break;
                }
                updateState(x509Certificate);
            }
        } catch (Throwable th) {
            updateState(x509Certificate);
            throw th;
        }
    }

    private boolean isSoftFailException(CertPathValidatorException certPathValidatorException) {
        if (this.softFail && certPathValidatorException.getReason() == CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS) {
            this.softFailExceptions.addFirst(new CertPathValidatorException(certPathValidatorException.getMessage(), certPathValidatorException.getCause(), this.params.certPath(), this.certIndex, certPathValidatorException.getReason()));
            return true;
        }
        return false;
    }

    private void updateState(X509Certificate x509Certificate) throws CertPathValidatorException {
        this.issuerCert = x509Certificate;
        PublicKey publicKey = x509Certificate.getPublicKey();
        if (PKIX.isDSAPublicKeyWithoutParams(publicKey)) {
            publicKey = BasicChecker.makeInheritedParamsKey(publicKey, this.prevPubKey);
        }
        this.prevPubKey = publicKey;
        this.crlSignFlag = certCanSignCrl(x509Certificate);
        if (this.certIndex > 0) {
            this.certIndex--;
        }
    }

    private void checkCRLs(X509Certificate x509Certificate, Collection<String> collection, Set<X509Certificate> set, PublicKey publicKey, boolean z) throws CertPathValidatorException {
        checkCRLs(x509Certificate, publicKey, null, z, true, set, this.params.trustAnchors());
    }

    private void checkCRLs(X509Certificate x509Certificate, PublicKey publicKey, X509Certificate x509Certificate2, boolean z, boolean z2, Set<X509Certificate> set, Set<TrustAnchor> set2) throws CertPathValidatorException {
        if (debug != null) {
            debug.println("RevocationChecker.checkCRLs() ---checking revocation status ...");
        }
        if (set != null && set.contains(x509Certificate)) {
            if (debug != null) {
                debug.println("RevocationChecker.checkCRLs() circular dependency");
            }
            throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
        }
        HashSet hashSet = new HashSet();
        HashSet hashSet2 = new HashSet();
        X509CRLSelector x509CRLSelector = new X509CRLSelector();
        x509CRLSelector.setCertificateChecking(x509Certificate);
        CertPathHelper.setDateAndTime(x509CRLSelector, this.params.date(), MAX_CLOCK_SKEW);
        CertPathValidatorException certPathValidatorException = null;
        for (CertStore certStore : this.certStores) {
            try {
                Iterator<? extends CRL> it = certStore.getCRLs(x509CRLSelector).iterator();
                while (it.hasNext()) {
                    hashSet.add((X509CRL) it.next());
                }
            } catch (CertStoreException e) {
                if (debug != null) {
                    debug.println("RevocationChecker.checkCRLs() CertStoreException: " + e.getMessage());
                }
                if (certPathValidatorException == null && CertStoreHelper.isCausedByNetworkIssue(certStore.getType(), e)) {
                    certPathValidatorException = new CertPathValidatorException("Unable to determine revocation status due to network error", e, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
                }
            }
        }
        if (debug != null) {
            debug.println("RevocationChecker.checkCRLs() possible crls.size() = " + hashSet.size());
        }
        boolean[] zArr = new boolean[9];
        if (!hashSet.isEmpty()) {
            hashSet2.addAll(verifyPossibleCRLs(hashSet, x509Certificate, publicKey, z, zArr, set2));
        }
        if (debug != null) {
            debug.println("RevocationChecker.checkCRLs() approved crls.size() = " + hashSet2.size());
        }
        if (!hashSet2.isEmpty() && Arrays.equals(zArr, ALL_REASONS)) {
            checkApprovedCRLs(x509Certificate, hashSet2);
            return;
        }
        try {
            if (this.crlDP) {
                hashSet2.addAll(DistributionPointFetcher.getCRLs(x509CRLSelector, z, publicKey, x509Certificate2, this.params.sigProvider(), this.certStores, zArr, set2, null));
            }
            if (!hashSet2.isEmpty() && Arrays.equals(zArr, ALL_REASONS)) {
                checkApprovedCRLs(x509Certificate, hashSet2);
                return;
            }
            if (z2) {
                try {
                    verifyWithSeparateSigningKey(x509Certificate, publicKey, z, set);
                    return;
                } catch (CertPathValidatorException e2) {
                    if (certPathValidatorException != null) {
                        throw certPathValidatorException;
                    }
                    throw e2;
                }
            }
            if (certPathValidatorException != null) {
                throw certPathValidatorException;
            }
            throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
        } catch (CertStoreException e3) {
            if ((e3 instanceof PKIX.CertStoreTypeException) && CertStoreHelper.isCausedByNetworkIssue(((PKIX.CertStoreTypeException) e3).getType(), e3)) {
                throw new CertPathValidatorException("Unable to determine revocation status due to network error", e3, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
            }
            throw new CertPathValidatorException(e3);
        }
    }

    private void checkApprovedCRLs(X509Certificate x509Certificate, Set<X509CRL> set) throws CertPathValidatorException {
        if (debug != null) {
            BigInteger serialNumber = x509Certificate.getSerialNumber();
            debug.println("RevocationChecker.checkApprovedCRLs() starting the final sweep...");
            debug.println("RevocationChecker.checkApprovedCRLs() cert SN: " + serialNumber.toString());
        }
        CRLReason cRLReason = CRLReason.UNSPECIFIED;
        for (X509CRL x509crl : set) {
            X509CRLEntry revokedCertificate = x509crl.getRevokedCertificate(x509Certificate);
            if (revokedCertificate != null) {
                try {
                    X509CRLEntryImpl impl = X509CRLEntryImpl.toImpl(revokedCertificate);
                    if (debug != null) {
                        debug.println("RevocationChecker.checkApprovedCRLs() CRL entry: " + impl.toString());
                    }
                    Set<String> criticalExtensionOIDs = impl.getCriticalExtensionOIDs();
                    if (criticalExtensionOIDs != null && !criticalExtensionOIDs.isEmpty()) {
                        criticalExtensionOIDs.remove(PKIXExtensions.ReasonCode_Id.toString());
                        criticalExtensionOIDs.remove(PKIXExtensions.CertificateIssuer_Id.toString());
                        if (!criticalExtensionOIDs.isEmpty()) {
                            throw new CertPathValidatorException("Unrecognized critical extension(s) in revoked CRL entry");
                        }
                    }
                    CRLReason revocationReason = impl.getRevocationReason();
                    if (revocationReason == null) {
                        revocationReason = CRLReason.UNSPECIFIED;
                    }
                    Date revocationDate = impl.getRevocationDate();
                    if (revocationDate.before(this.params.date())) {
                        CertificateRevokedException certificateRevokedException = new CertificateRevokedException(revocationDate, revocationReason, x509crl.getIssuerX500Principal(), impl.getExtensions());
                        throw new CertPathValidatorException(certificateRevokedException.getMessage(), certificateRevokedException, null, -1, CertPathValidatorException.BasicReason.REVOKED);
                    }
                } catch (CRLException e) {
                    throw new CertPathValidatorException(e);
                }
            }
        }
    }

    private void checkOCSP(X509Certificate x509Certificate, Collection<String> collection) throws Throwable {
        CertId certId;
        URI responderURI;
        OCSPResponse oCSPResponseCheck;
        try {
            X509CertImpl impl = X509CertImpl.toImpl(x509Certificate);
            try {
                if (this.issuerCert != null) {
                    certId = new CertId(this.issuerCert, impl.getSerialNumberObject());
                } else {
                    certId = new CertId(this.anchor.getCA(), this.anchor.getCAPublicKey(), impl.getSerialNumberObject());
                }
                byte[] bArr = this.ocspResponses.get(x509Certificate);
                if (bArr != null) {
                    if (debug != null) {
                        debug.println("Found cached OCSP response");
                    }
                    oCSPResponseCheck = new OCSPResponse(bArr);
                    byte[] value = null;
                    for (Extension extension : this.ocspExtensions) {
                        if (extension.getId().equals("1.3.6.1.5.5.7.48.1.2")) {
                            value = extension.getValue();
                        }
                    }
                    oCSPResponseCheck.verify(Collections.singletonList(certId), this.issuerCert, this.responderCert, this.params.date(), value);
                } else {
                    if (this.responderURI != null) {
                        responderURI = this.responderURI;
                    } else {
                        responderURI = OCSP.getResponderURI(impl);
                    }
                    URI uri = responderURI;
                    if (uri == null) {
                        throw new CertPathValidatorException("Certificate does not specify OCSP responder", null, null, -1);
                    }
                    oCSPResponseCheck = OCSP.check((List<CertId>) Collections.singletonList(certId), uri, this.issuerCert, this.responderCert, (Date) null, this.ocspExtensions);
                }
                OCSPResponse.SingleResponse singleResponse = oCSPResponseCheck.getSingleResponse(certId);
                OCSP.RevocationStatus.CertStatus certStatus = singleResponse.getCertStatus();
                if (certStatus == OCSP.RevocationStatus.CertStatus.REVOKED) {
                    Date revocationTime = singleResponse.getRevocationTime();
                    if (revocationTime.before(this.params.date())) {
                        CertificateRevokedException certificateRevokedException = new CertificateRevokedException(revocationTime, singleResponse.getRevocationReason(), oCSPResponseCheck.getSignerCertificate().getSubjectX500Principal(), singleResponse.getSingleExtensions());
                        throw new CertPathValidatorException(certificateRevokedException.getMessage(), certificateRevokedException, null, -1, CertPathValidatorException.BasicReason.REVOKED);
                    }
                    return;
                }
                if (certStatus == OCSP.RevocationStatus.CertStatus.UNKNOWN) {
                    throw new CertPathValidatorException("Certificate's revocation status is unknown", null, this.params.certPath(), -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
                }
            } catch (IOException e) {
                throw new CertPathValidatorException("Unable to determine revocation status due to network error", e, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
            }
        } catch (CertificateException e2) {
            throw new CertPathValidatorException(e2);
        }
    }

    private static String stripOutSeparators(String str) {
        char[] charArray = str.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < charArray.length; i++) {
            if (HEX_DIGITS.indexOf(charArray[i]) != -1) {
                sb.append(charArray[i]);
            }
        }
        return sb.toString();
    }

    static boolean certCanSignCrl(X509Certificate x509Certificate) {
        boolean[] keyUsage = x509Certificate.getKeyUsage();
        if (keyUsage != null) {
            return keyUsage[6];
        }
        return false;
    }

    private Collection<X509CRL> verifyPossibleCRLs(Set<X509CRL> set, X509Certificate x509Certificate, PublicKey publicKey, boolean z, boolean[] zArr, Set<TrustAnchor> set2) throws CertPathValidatorException {
        List<DistributionPoint> listSingletonList;
        try {
            X509CertImpl impl = X509CertImpl.toImpl(x509Certificate);
            if (debug != null) {
                debug.println("RevocationChecker.verifyPossibleCRLs: Checking CRLDPs for " + ((Object) impl.getSubjectX500Principal()));
            }
            CRLDistributionPointsExtension cRLDistributionPointsExtension = impl.getCRLDistributionPointsExtension();
            if (cRLDistributionPointsExtension == null) {
                listSingletonList = Collections.singletonList(new DistributionPoint(new GeneralNames().add(new GeneralName((X500Name) impl.getIssuerDN())), (boolean[]) null, (GeneralNames) null));
            } else {
                listSingletonList = cRLDistributionPointsExtension.get(CRLDistributionPointsExtension.POINTS);
            }
            HashSet hashSet = new HashSet();
            for (DistributionPoint distributionPoint : listSingletonList) {
                Iterator<X509CRL> it = set.iterator();
                while (it.hasNext()) {
                    X509CRL next = it.next();
                    Iterator<X509CRL> it2 = it;
                    if (DistributionPointFetcher.verifyCRL(impl, distributionPoint, next, zArr, z, publicKey, null, this.params.sigProvider(), set2, this.certStores, this.params.date())) {
                        hashSet.add(next);
                    }
                    it = it2;
                }
                if (Arrays.equals(zArr, ALL_REASONS)) {
                    break;
                }
            }
            return hashSet;
        } catch (IOException | CRLException | CertificateException e) {
            if (debug != null) {
                debug.println("Exception while verifying CRL: " + e.getMessage());
                e.printStackTrace();
            }
            return Collections.emptySet();
        }
    }

    private void verifyWithSeparateSigningKey(X509Certificate x509Certificate, PublicKey publicKey, boolean z, Set<X509Certificate> set) throws CertPathValidatorException {
        if (debug != null) {
            debug.println("RevocationChecker.verifyWithSeparateSigningKey() ---checking revocation status...");
        }
        if (set != null && set.contains(x509Certificate)) {
            if (debug != null) {
                debug.println("RevocationChecker.verifyWithSeparateSigningKey() circular dependency");
            }
            throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
        }
        if (!z) {
            buildToNewKey(x509Certificate, null, set);
        } else {
            buildToNewKey(x509Certificate, publicKey, set);
        }
    }

    private void buildToNewKey(X509Certificate x509Certificate, PublicKey publicKey, Set<X509Certificate> set) throws CertPathValidatorException {
        Set<TrustAnchor> setSingleton;
        Set<X509Certificate> set2;
        PKIXCertPathBuilderResult pKIXCertPathBuilderResult;
        CertPathBuilder certPathBuilder;
        X509Certificate x509Certificate2;
        X509CertImpl impl;
        AuthorityInfoAccessExtension authorityInfoAccessExtension;
        List<AccessDescription> accessDescriptions;
        if (debug != null) {
            debug.println("RevocationChecker.buildToNewKey() starting work");
        }
        HashSet hashSet = new HashSet();
        if (publicKey != null) {
            hashSet.add(publicKey);
        }
        RejectKeySelector rejectKeySelector = new RejectKeySelector(hashSet);
        rejectKeySelector.setSubject(x509Certificate.getIssuerX500Principal());
        rejectKeySelector.setKeyUsage(CRL_SIGN_USAGE);
        if (this.anchor == null) {
            setSingleton = this.params.trustAnchors();
        } else {
            setSingleton = Collections.singleton(this.anchor);
        }
        Set<TrustAnchor> set3 = setSingleton;
        try {
            PKIXBuilderParameters pKIXBuilderParameters = new PKIXBuilderParameters(set3, rejectKeySelector);
            pKIXBuilderParameters.setInitialPolicies(this.params.initialPolicies());
            pKIXBuilderParameters.setCertStores(this.certStores);
            pKIXBuilderParameters.setExplicitPolicyRequired(this.params.explicitPolicyRequired());
            pKIXBuilderParameters.setPolicyMappingInhibited(this.params.policyMappingInhibited());
            pKIXBuilderParameters.setAnyPolicyInhibited(this.params.anyPolicyInhibited());
            pKIXBuilderParameters.setDate(this.params.date());
            pKIXBuilderParameters.setCertPathCheckers(this.params.getPKIXParameters().getCertPathCheckers());
            pKIXBuilderParameters.setSigProvider(this.params.sigProvider());
            pKIXBuilderParameters.setRevocationEnabled(false);
            int i = 1;
            if (Builder.USE_AIA) {
                try {
                    impl = X509CertImpl.toImpl(x509Certificate);
                } catch (CertificateException e) {
                    if (debug != null) {
                        debug.println("RevocationChecker.buildToNewKey: error decoding cert: " + ((Object) e));
                    }
                    impl = null;
                }
                if (impl != null) {
                    authorityInfoAccessExtension = impl.getAuthorityInfoAccessExtension();
                } else {
                    authorityInfoAccessExtension = null;
                }
                if (authorityInfoAccessExtension != null && (accessDescriptions = authorityInfoAccessExtension.getAccessDescriptions()) != null) {
                    Iterator<AccessDescription> it = accessDescriptions.iterator();
                    while (it.hasNext()) {
                        CertStore uRICertStore = URICertStore.getInstance(it.next());
                        if (uRICertStore != null) {
                            if (debug != null) {
                                debug.println("adding AIAext CertStore");
                            }
                            pKIXBuilderParameters.addCertStore(uRICertStore);
                        }
                    }
                }
            }
            try {
                CertPathBuilder certPathBuilder2 = CertPathBuilder.getInstance("PKIX");
                Set<X509Certificate> hashSet2 = set;
                while (true) {
                    try {
                        if (debug != null) {
                            debug.println("RevocationChecker.buildToNewKey() about to try build ...");
                        }
                        PKIXCertPathBuilderResult pKIXCertPathBuilderResult2 = (PKIXCertPathBuilderResult) certPathBuilder2.build(pKIXBuilderParameters);
                        if (debug != null) {
                            debug.println("RevocationChecker.buildToNewKey() about to check revocation ...");
                        }
                        if (hashSet2 == null) {
                            hashSet2 = new HashSet<>();
                        }
                        Set<X509Certificate> set4 = hashSet2;
                        set4.add(x509Certificate);
                        TrustAnchor trustAnchor = pKIXCertPathBuilderResult2.getTrustAnchor();
                        PublicKey cAPublicKey = trustAnchor.getCAPublicKey();
                        if (cAPublicKey == null) {
                            cAPublicKey = trustAnchor.getTrustedCert().getPublicKey();
                        }
                        List<? extends Certificate> certificates = pKIXCertPathBuilderResult2.getCertPath().getCertificates();
                        try {
                            PublicKey publicKey2 = cAPublicKey;
                            int size = certificates.size() - i;
                            ?? r16 = i;
                            while (size >= 0) {
                                X509Certificate x509Certificate3 = (X509Certificate) certificates.get(size);
                                if (debug != null) {
                                    debug.println("RevocationChecker.buildToNewKey() index " + size + " checking " + ((Object) x509Certificate3));
                                }
                                int i2 = size;
                                ?? r5 = r16;
                                set2 = set4;
                                pKIXCertPathBuilderResult = pKIXCertPathBuilderResult2;
                                certPathBuilder = certPathBuilder2;
                                try {
                                    checkCRLs(x509Certificate3, publicKey2, null, r5, true, set2, set3);
                                    boolean zCertCanSignCrl = certCanSignCrl(x509Certificate3);
                                    publicKey2 = x509Certificate3.getPublicKey();
                                    size = i2 - 1;
                                    set4 = set2;
                                    pKIXCertPathBuilderResult2 = pKIXCertPathBuilderResult;
                                    certPathBuilder2 = certPathBuilder;
                                    r16 = zCertCanSignCrl;
                                } catch (CertPathValidatorException e2) {
                                    hashSet.add(pKIXCertPathBuilderResult.getPublicKey());
                                    hashSet2 = set2;
                                    certPathBuilder2 = certPathBuilder;
                                    i = 1;
                                }
                            }
                            set2 = set4;
                            PKIXCertPathBuilderResult pKIXCertPathBuilderResult3 = pKIXCertPathBuilderResult2;
                            certPathBuilder = certPathBuilder2;
                            if (debug != null) {
                                debug.println("RevocationChecker.buildToNewKey() got key " + ((Object) pKIXCertPathBuilderResult3.getPublicKey()));
                            }
                            PublicKey publicKey3 = pKIXCertPathBuilderResult3.getPublicKey();
                            if (!certificates.isEmpty()) {
                                x509Certificate2 = (X509Certificate) certificates.get(0);
                            } else {
                                x509Certificate2 = null;
                            }
                            try {
                                checkCRLs(x509Certificate, publicKey3, x509Certificate2, true, false, null, this.params.trustAnchors());
                                return;
                            } catch (CertPathValidatorException e3) {
                                if (e3.getReason() == CertPathValidatorException.BasicReason.REVOKED) {
                                    throw e3;
                                }
                                hashSet.add(publicKey3);
                            }
                        } catch (CertPathValidatorException e4) {
                            set2 = set4;
                            pKIXCertPathBuilderResult = pKIXCertPathBuilderResult2;
                            certPathBuilder = certPathBuilder2;
                        }
                        hashSet2 = set2;
                        certPathBuilder2 = certPathBuilder;
                        i = 1;
                    } catch (InvalidAlgorithmParameterException e5) {
                        throw new CertPathValidatorException(e5);
                    } catch (CertPathBuilderException e6) {
                        throw new CertPathValidatorException("Could not determine revocation status", null, null, -1, CertPathValidatorException.BasicReason.UNDETERMINED_REVOCATION_STATUS);
                    }
                }
            } catch (NoSuchAlgorithmException e7) {
                throw new CertPathValidatorException(e7);
            }
        } catch (InvalidAlgorithmParameterException e8) {
            throw new RuntimeException(e8);
        }
    }

    @Override
    public RevocationChecker clone() {
        RevocationChecker revocationChecker = (RevocationChecker) super.clone();
        revocationChecker.softFailExceptions = new LinkedList<>(this.softFailExceptions);
        return revocationChecker;
    }

    private static class RejectKeySelector extends X509CertSelector {
        private final Set<PublicKey> badKeySet;

        RejectKeySelector(Set<PublicKey> set) {
            this.badKeySet = set;
        }

        @Override
        public boolean match(Certificate certificate) {
            if (!super.match(certificate)) {
                return false;
            }
            if (this.badKeySet.contains(certificate.getPublicKey())) {
                if (RevocationChecker.debug != null) {
                    RevocationChecker.debug.println("RejectKeySelector.match: bad key");
                }
                return false;
            }
            if (RevocationChecker.debug != null) {
                RevocationChecker.debug.println("RejectKeySelector.match: returning true");
                return true;
            }
            return true;
        }

        @Override
        public String toString() {
            return "RejectKeySelector: [\n" + super.toString() + ((Object) this.badKeySet) + "]";
        }
    }
}
