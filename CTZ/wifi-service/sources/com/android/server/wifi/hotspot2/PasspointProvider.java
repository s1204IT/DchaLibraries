package com.android.server.wifi.hotspot2;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.Credential;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import com.android.server.wifi.IMSIParameter;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.DomainNameElement;
import com.android.server.wifi.hotspot2.anqp.NAIRealmElement;
import com.android.server.wifi.hotspot2.anqp.RoamingConsortiumElement;
import com.android.server.wifi.hotspot2.anqp.ThreeGPPNetworkElement;
import com.android.server.wifi.hotspot2.anqp.eap.AuthParam;
import com.android.server.wifi.hotspot2.anqp.eap.NonEAPInnerAuth;
import com.android.server.wifi.util.InformationElementUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PasspointProvider {
    private static final String ALIAS_HS_TYPE = "HS2_";
    private static final String TAG = "PasspointProvider";
    private final AuthParam mAuthParam;
    private String mCaCertificateAlias;
    private String mClientCertificateAlias;
    private String mClientPrivateKeyAlias;
    private final PasspointConfiguration mConfig;
    private final int mCreatorUid;
    private final int mEAPMethodID;
    private boolean mHasEverConnected;
    private final IMSIParameter mImsiParameter;
    private boolean mIsShared;
    private final WifiKeyStore mKeyStore;
    private final List<String> mMatchingSIMImsiList;
    private final long mProviderId;

    public PasspointProvider(PasspointConfiguration passpointConfiguration, WifiKeyStore wifiKeyStore, SIMAccessor sIMAccessor, long j, int i) {
        this(passpointConfiguration, wifiKeyStore, sIMAccessor, j, i, null, null, null, false, false);
    }

    public PasspointProvider(PasspointConfiguration passpointConfiguration, WifiKeyStore wifiKeyStore, SIMAccessor sIMAccessor, long j, int i, String str, String str2, String str3, boolean z, boolean z2) {
        this.mConfig = new PasspointConfiguration(passpointConfiguration);
        this.mKeyStore = wifiKeyStore;
        this.mProviderId = j;
        this.mCreatorUid = i;
        this.mCaCertificateAlias = str;
        this.mClientCertificateAlias = str2;
        this.mClientPrivateKeyAlias = str3;
        this.mHasEverConnected = z;
        this.mIsShared = z2;
        if (this.mConfig.getCredential().getUserCredential() != null) {
            this.mEAPMethodID = 21;
            this.mAuthParam = new NonEAPInnerAuth(NonEAPInnerAuth.getAuthTypeID(this.mConfig.getCredential().getUserCredential().getNonEapInnerMethod()));
            this.mImsiParameter = null;
            this.mMatchingSIMImsiList = null;
            return;
        }
        if (this.mConfig.getCredential().getCertCredential() != null) {
            this.mEAPMethodID = 13;
            this.mAuthParam = null;
            this.mImsiParameter = null;
            this.mMatchingSIMImsiList = null;
            return;
        }
        this.mEAPMethodID = this.mConfig.getCredential().getSimCredential().getEapType();
        this.mAuthParam = null;
        this.mImsiParameter = IMSIParameter.build(this.mConfig.getCredential().getSimCredential().getImsi());
        this.mMatchingSIMImsiList = sIMAccessor.getMatchingImsis(this.mImsiParameter);
    }

    public PasspointConfiguration getConfig() {
        return new PasspointConfiguration(this.mConfig);
    }

    public String getCaCertificateAlias() {
        return this.mCaCertificateAlias;
    }

    public String getClientPrivateKeyAlias() {
        return this.mClientPrivateKeyAlias;
    }

    public String getClientCertificateAlias() {
        return this.mClientCertificateAlias;
    }

    public long getProviderId() {
        return this.mProviderId;
    }

    public int getCreatorUid() {
        return this.mCreatorUid;
    }

    public boolean getHasEverConnected() {
        return this.mHasEverConnected;
    }

    public void setHasEverConnected(boolean z) {
        this.mHasEverConnected = z;
    }

    public boolean installCertsAndKeys() {
        if (this.mConfig.getCredential().getCaCertificate() != null) {
            if (!this.mKeyStore.putCertInKeyStore("CACERT_HS2_" + this.mProviderId, this.mConfig.getCredential().getCaCertificate())) {
                Log.e(TAG, "Failed to install CA Certificate");
                uninstallCertsAndKeys();
                return false;
            }
            this.mCaCertificateAlias = ALIAS_HS_TYPE + this.mProviderId;
        }
        if (this.mConfig.getCredential().getClientPrivateKey() != null) {
            if (!this.mKeyStore.putKeyInKeyStore("USRPKEY_HS2_" + this.mProviderId, this.mConfig.getCredential().getClientPrivateKey())) {
                Log.e(TAG, "Failed to install client private key");
                uninstallCertsAndKeys();
                return false;
            }
            this.mClientPrivateKeyAlias = ALIAS_HS_TYPE + this.mProviderId;
        }
        if (this.mConfig.getCredential().getClientCertificateChain() != null) {
            X509Certificate clientCertificate = getClientCertificate(this.mConfig.getCredential().getClientCertificateChain(), this.mConfig.getCredential().getCertCredential().getCertSha256Fingerprint());
            if (clientCertificate == null) {
                Log.e(TAG, "Failed to locate client certificate");
                uninstallCertsAndKeys();
                return false;
            }
            if (!this.mKeyStore.putCertInKeyStore("USRCERT_HS2_" + this.mProviderId, clientCertificate)) {
                Log.e(TAG, "Failed to install client certificate");
                uninstallCertsAndKeys();
                return false;
            }
            this.mClientCertificateAlias = ALIAS_HS_TYPE + this.mProviderId;
        }
        this.mConfig.getCredential().setCaCertificate(null);
        this.mConfig.getCredential().setClientPrivateKey(null);
        this.mConfig.getCredential().setClientCertificateChain(null);
        return true;
    }

    public void uninstallCertsAndKeys() {
        if (this.mCaCertificateAlias != null) {
            if (!this.mKeyStore.removeEntryFromKeyStore("CACERT_" + this.mCaCertificateAlias)) {
                Log.e(TAG, "Failed to remove entry: " + this.mCaCertificateAlias);
            }
            this.mCaCertificateAlias = null;
        }
        if (this.mClientPrivateKeyAlias != null) {
            if (!this.mKeyStore.removeEntryFromKeyStore("USRPKEY_" + this.mClientPrivateKeyAlias)) {
                Log.e(TAG, "Failed to remove entry: " + this.mClientPrivateKeyAlias);
            }
            this.mClientPrivateKeyAlias = null;
        }
        if (this.mClientCertificateAlias != null) {
            if (!this.mKeyStore.removeEntryFromKeyStore("USRCERT_" + this.mClientCertificateAlias)) {
                Log.e(TAG, "Failed to remove entry: " + this.mClientCertificateAlias);
            }
            this.mClientCertificateAlias = null;
        }
    }

    public PasspointMatch match(Map<Constants.ANQPElementType, ANQPElement> map, InformationElementUtil.RoamingConsortium roamingConsortium) {
        PasspointMatch passpointMatchMatchProvider = matchProvider(map, roamingConsortium);
        int iMatchNAIRealm = ANQPMatcher.matchNAIRealm((NAIRealmElement) map.get(Constants.ANQPElementType.ANQPNAIRealm), this.mConfig.getCredential().getRealm(), this.mEAPMethodID, this.mAuthParam);
        if (iMatchNAIRealm == -1) {
            return PasspointMatch.None;
        }
        return ((iMatchNAIRealm & 4) != 0 && passpointMatchMatchProvider == PasspointMatch.None) ? PasspointMatch.RoamingProvider : passpointMatchMatchProvider;
    }

    public WifiConfiguration getWifiConfig() {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.FQDN = this.mConfig.getHomeSp().getFqdn();
        if (this.mConfig.getHomeSp().getRoamingConsortiumOis() != null) {
            wifiConfiguration.roamingConsortiumIds = Arrays.copyOf(this.mConfig.getHomeSp().getRoamingConsortiumOis(), this.mConfig.getHomeSp().getRoamingConsortiumOis().length);
        }
        wifiConfiguration.providerFriendlyName = this.mConfig.getHomeSp().getFriendlyName();
        wifiConfiguration.allowedKeyManagement.set(2);
        wifiConfiguration.allowedKeyManagement.set(3);
        WifiEnterpriseConfig wifiEnterpriseConfig = new WifiEnterpriseConfig();
        wifiEnterpriseConfig.setRealm(this.mConfig.getCredential().getRealm());
        wifiEnterpriseConfig.setDomainSuffixMatch(this.mConfig.getHomeSp().getFqdn());
        if (this.mConfig.getCredential().getUserCredential() != null) {
            buildEnterpriseConfigForUserCredential(wifiEnterpriseConfig, this.mConfig.getCredential().getUserCredential());
            setAnonymousIdentityToNaiRealm(wifiEnterpriseConfig, this.mConfig.getCredential().getRealm());
        } else if (this.mConfig.getCredential().getCertCredential() != null) {
            buildEnterpriseConfigForCertCredential(wifiEnterpriseConfig);
            setAnonymousIdentityToNaiRealm(wifiEnterpriseConfig, this.mConfig.getCredential().getRealm());
        } else {
            buildEnterpriseConfigForSimCredential(wifiEnterpriseConfig, this.mConfig.getCredential().getSimCredential());
        }
        wifiConfiguration.enterpriseConfig = wifiEnterpriseConfig;
        wifiConfiguration.shared = this.mIsShared;
        return wifiConfiguration;
    }

    public boolean isSimCredential() {
        return this.mConfig.getCredential().getSimCredential() != null;
    }

    public static PasspointConfiguration convertFromWifiConfig(WifiConfiguration wifiConfiguration) {
        PasspointConfiguration passpointConfiguration = new PasspointConfiguration();
        HomeSp homeSp = new HomeSp();
        if (TextUtils.isEmpty(wifiConfiguration.FQDN)) {
            Log.e(TAG, "Missing FQDN");
            return null;
        }
        homeSp.setFqdn(wifiConfiguration.FQDN);
        homeSp.setFriendlyName(wifiConfiguration.providerFriendlyName);
        if (wifiConfiguration.roamingConsortiumIds != null) {
            homeSp.setRoamingConsortiumOis(Arrays.copyOf(wifiConfiguration.roamingConsortiumIds, wifiConfiguration.roamingConsortiumIds.length));
        }
        passpointConfiguration.setHomeSp(homeSp);
        Credential credential = new Credential();
        credential.setRealm(wifiConfiguration.enterpriseConfig.getRealm());
        switch (wifiConfiguration.enterpriseConfig.getEapMethod()) {
            case 1:
                Credential.CertificateCredential certificateCredential = new Credential.CertificateCredential();
                certificateCredential.setCertType("x509v3");
                credential.setCertCredential(certificateCredential);
                break;
            case 2:
                credential.setUserCredential(buildUserCredentialFromEnterpriseConfig(wifiConfiguration.enterpriseConfig));
                break;
            case 3:
            default:
                Log.e(TAG, "Unsupport EAP method: " + wifiConfiguration.enterpriseConfig.getEapMethod());
                return null;
            case 4:
                credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(18, wifiConfiguration.enterpriseConfig));
                break;
            case 5:
                credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(23, wifiConfiguration.enterpriseConfig));
                break;
            case 6:
                credential.setSimCredential(buildSimCredentialFromEnterpriseConfig(50, wifiConfiguration.enterpriseConfig));
                break;
        }
        if (credential.getUserCredential() == null && credential.getCertCredential() == null && credential.getSimCredential() == null) {
            Log.e(TAG, "Missing credential");
            return null;
        }
        passpointConfiguration.setCredential(credential);
        return passpointConfiguration;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PasspointProvider)) {
            return false;
        }
        PasspointProvider passpointProvider = (PasspointProvider) obj;
        if (this.mProviderId == passpointProvider.mProviderId && TextUtils.equals(this.mCaCertificateAlias, passpointProvider.mCaCertificateAlias) && TextUtils.equals(this.mClientCertificateAlias, passpointProvider.mClientCertificateAlias) && TextUtils.equals(this.mClientPrivateKeyAlias, passpointProvider.mClientPrivateKeyAlias)) {
            if (this.mConfig == null) {
                if (passpointProvider.mConfig == null) {
                    return true;
                }
            } else if (this.mConfig.equals(passpointProvider.mConfig)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(Long.valueOf(this.mProviderId), this.mCaCertificateAlias, this.mClientCertificateAlias, this.mClientPrivateKeyAlias, this.mConfig);
    }

    public String toString() {
        return "ProviderId: " + this.mProviderId + "\nCreatorUID: " + this.mCreatorUid + "\nConfiguration Begin ---\n" + this.mConfig + "Configuration End ---\n";
    }

    private static X509Certificate getClientCertificate(X509Certificate[] x509CertificateArr, byte[] bArr) {
        if (x509CertificateArr == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            for (X509Certificate x509Certificate : x509CertificateArr) {
                messageDigest.reset();
                if (Arrays.equals(bArr, messageDigest.digest(x509Certificate.getEncoded()))) {
                    return x509Certificate;
                }
            }
            return null;
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            return null;
        }
    }

    private PasspointMatch matchProvider(Map<Constants.ANQPElementType, ANQPElement> map, InformationElementUtil.RoamingConsortium roamingConsortium) {
        if (ANQPMatcher.matchDomainName((DomainNameElement) map.get(Constants.ANQPElementType.ANQPDomName), this.mConfig.getHomeSp().getFqdn(), this.mImsiParameter, this.mMatchingSIMImsiList)) {
            return PasspointMatch.HomeProvider;
        }
        long[] roamingConsortiumOis = this.mConfig.getHomeSp().getRoamingConsortiumOis();
        if (ANQPMatcher.matchRoamingConsortium((RoamingConsortiumElement) map.get(Constants.ANQPElementType.ANQPRoamingConsortium), roamingConsortiumOis)) {
            return PasspointMatch.RoamingProvider;
        }
        long[] roamingConsortiums = roamingConsortium.getRoamingConsortiums();
        if (roamingConsortiums != null && roamingConsortiumOis != null) {
            for (long j : roamingConsortiums) {
                for (long j2 : roamingConsortiumOis) {
                    if (j == j2) {
                        return PasspointMatch.RoamingProvider;
                    }
                }
            }
        }
        if (ANQPMatcher.matchThreeGPPNetwork((ThreeGPPNetworkElement) map.get(Constants.ANQPElementType.ANQP3GPPNetwork), this.mImsiParameter, this.mMatchingSIMImsiList)) {
            return PasspointMatch.RoamingProvider;
        }
        return PasspointMatch.None;
    }

    private void buildEnterpriseConfigForUserCredential(WifiEnterpriseConfig wifiEnterpriseConfig, Credential.UserCredential userCredential) {
        byte b;
        int i = 0;
        String str = new String(Base64.decode(userCredential.getPassword(), 0), StandardCharsets.UTF_8);
        wifiEnterpriseConfig.setEapMethod(2);
        wifiEnterpriseConfig.setIdentity(userCredential.getUsername());
        wifiEnterpriseConfig.setPassword(str);
        wifiEnterpriseConfig.setCaCertificateAlias(this.mCaCertificateAlias);
        String nonEapInnerMethod = userCredential.getNonEapInnerMethod();
        int iHashCode = nonEapInnerMethod.hashCode();
        if (iHashCode != 78975) {
            if (iHashCode != 632512142) {
                b = (iHashCode == 2038151963 && nonEapInnerMethod.equals("MS-CHAP")) ? (byte) 1 : (byte) -1;
            } else if (nonEapInnerMethod.equals("MS-CHAP-V2")) {
                b = 2;
            }
        } else if (nonEapInnerMethod.equals("PAP")) {
            b = 0;
        }
        switch (b) {
            case 0:
                i = 1;
                break;
            case 1:
                i = 2;
                break;
            case 2:
                i = 3;
                break;
            default:
                Log.wtf(TAG, "Unsupported Auth: " + userCredential.getNonEapInnerMethod());
                break;
        }
        wifiEnterpriseConfig.setPhase2Method(i);
    }

    private void buildEnterpriseConfigForCertCredential(WifiEnterpriseConfig wifiEnterpriseConfig) {
        wifiEnterpriseConfig.setEapMethod(1);
        wifiEnterpriseConfig.setClientCertificateAlias(this.mClientCertificateAlias);
        wifiEnterpriseConfig.setCaCertificateAlias(this.mCaCertificateAlias);
    }

    private void buildEnterpriseConfigForSimCredential(WifiEnterpriseConfig wifiEnterpriseConfig, Credential.SimCredential simCredential) {
        int i;
        int eapType = simCredential.getEapType();
        if (eapType == 18) {
            i = 4;
        } else if (eapType == 23) {
            i = 5;
        } else if (eapType == 50) {
            i = 6;
        } else {
            Log.wtf(TAG, "Unsupported EAP Method: " + simCredential.getEapType());
            i = -1;
        }
        wifiEnterpriseConfig.setEapMethod(i);
        wifiEnterpriseConfig.setPlmn(simCredential.getImsi());
    }

    private static void setAnonymousIdentityToNaiRealm(WifiEnterpriseConfig wifiEnterpriseConfig, String str) {
        wifiEnterpriseConfig.setAnonymousIdentity("anonymous@" + str);
    }

    private static Credential.UserCredential buildUserCredentialFromEnterpriseConfig(WifiEnterpriseConfig wifiEnterpriseConfig) {
        Credential.UserCredential userCredential = new Credential.UserCredential();
        userCredential.setEapType(21);
        if (TextUtils.isEmpty(wifiEnterpriseConfig.getIdentity())) {
            Log.e(TAG, "Missing username for user credential");
            return null;
        }
        userCredential.setUsername(wifiEnterpriseConfig.getIdentity());
        if (TextUtils.isEmpty(wifiEnterpriseConfig.getPassword())) {
            Log.e(TAG, "Missing password for user credential");
            return null;
        }
        userCredential.setPassword(new String(Base64.encode(wifiEnterpriseConfig.getPassword().getBytes(StandardCharsets.UTF_8), 0), StandardCharsets.UTF_8));
        switch (wifiEnterpriseConfig.getPhase2Method()) {
            case 1:
                userCredential.setNonEapInnerMethod("PAP");
                return userCredential;
            case 2:
                userCredential.setNonEapInnerMethod("MS-CHAP");
                return userCredential;
            case 3:
                userCredential.setNonEapInnerMethod("MS-CHAP-V2");
                return userCredential;
            default:
                Log.e(TAG, "Unsupported phase2 method for TTLS: " + wifiEnterpriseConfig.getPhase2Method());
                return null;
        }
    }

    private static Credential.SimCredential buildSimCredentialFromEnterpriseConfig(int i, WifiEnterpriseConfig wifiEnterpriseConfig) {
        Credential.SimCredential simCredential = new Credential.SimCredential();
        if (TextUtils.isEmpty(wifiEnterpriseConfig.getPlmn())) {
            Log.e(TAG, "Missing IMSI for SIM credential");
            return null;
        }
        simCredential.setImsi(wifiEnterpriseConfig.getPlmn());
        simCredential.setEapType(i);
        return simCredential;
    }
}
