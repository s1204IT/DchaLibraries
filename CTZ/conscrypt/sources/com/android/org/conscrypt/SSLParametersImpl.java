package com.android.org.conscrypt;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

final class SSLParametersImpl implements Cloneable {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static volatile SSLParametersImpl defaultParameters;
    private static volatile X509KeyManager defaultX509KeyManager;
    private static volatile X509TrustManager defaultX509TrustManager;
    ApplicationProtocolSelectorAdapter applicationProtocolSelector;
    boolean channelIdEnabled;
    private final ClientSessionContext clientSessionContext;
    private boolean ctVerificationEnabled;
    String[] enabledCipherSuites;
    String[] enabledProtocols;
    private String endpointIdentificationAlgorithm;
    boolean isEnabledProtocolsFiltered;
    byte[] ocspResponse;
    private final PSKKeyManager pskKeyManager;
    byte[] sctExtension;
    private final ServerSessionContext serverSessionContext;
    private boolean useCipherSuitesOrder;
    boolean useSessionTickets;
    private Boolean useSni;
    private final X509KeyManager x509KeyManager;
    private final X509TrustManager x509TrustManager;
    private boolean client_mode = true;
    private boolean need_client_auth = false;
    private boolean want_client_auth = false;
    private boolean enable_session_creation = true;
    byte[] applicationProtocols = EmptyArray.BYTE;

    interface AliasChooser {
        String chooseClientAlias(X509KeyManager x509KeyManager, X500Principal[] x500PrincipalArr, String[] strArr);

        String chooseServerAlias(X509KeyManager x509KeyManager, String str);
    }

    interface PSKCallbacks {
        String chooseClientPSKIdentity(PSKKeyManager pSKKeyManager, String str);

        String chooseServerPSKIdentityHint(PSKKeyManager pSKKeyManager);

        SecretKey getPSKKey(PSKKeyManager pSKKeyManager, String str, String str2);
    }

    SSLParametersImpl(KeyManager[] keyManagerArr, TrustManager[] trustManagerArr, SecureRandom secureRandom, ClientSessionContext clientSessionContext, ServerSessionContext serverSessionContext, String[] strArr) throws KeyManagementException {
        this.serverSessionContext = serverSessionContext;
        this.clientSessionContext = clientSessionContext;
        if (keyManagerArr == null) {
            this.x509KeyManager = getDefaultX509KeyManager();
            this.pskKeyManager = null;
        } else {
            this.x509KeyManager = findFirstX509KeyManager(keyManagerArr);
            this.pskKeyManager = findFirstPSKKeyManager(keyManagerArr);
        }
        if (trustManagerArr == null) {
            this.x509TrustManager = getDefaultX509TrustManager();
        } else {
            this.x509TrustManager = findFirstX509TrustManager(trustManagerArr);
        }
        this.enabledProtocols = (String[]) NativeCrypto.checkEnabledProtocols(strArr == null ? NativeCrypto.DEFAULT_PROTOCOLS : strArr).clone();
        this.enabledCipherSuites = getDefaultCipherSuites((this.x509KeyManager == null && this.x509TrustManager == null) ? false : true, this.pskKeyManager != null);
    }

    static SSLParametersImpl getDefault() throws KeyManagementException {
        SSLParametersImpl sSLParametersImpl = defaultParameters;
        if (sSLParametersImpl == null) {
            sSLParametersImpl = new SSLParametersImpl(null, null, null, new ClientSessionContext(), new ServerSessionContext(), null);
            defaultParameters = sSLParametersImpl;
        }
        return (SSLParametersImpl) sSLParametersImpl.clone();
    }

    AbstractSessionContext getSessionContext() {
        return this.client_mode ? this.clientSessionContext : this.serverSessionContext;
    }

    ClientSessionContext getClientSessionContext() {
        return this.clientSessionContext;
    }

    X509KeyManager getX509KeyManager() {
        return this.x509KeyManager;
    }

    PSKKeyManager getPSKKeyManager() {
        return this.pskKeyManager;
    }

    X509TrustManager getX509TrustManager() {
        return this.x509TrustManager;
    }

    String[] getEnabledCipherSuites() {
        return (String[]) this.enabledCipherSuites.clone();
    }

    void setEnabledCipherSuites(String[] strArr) {
        this.enabledCipherSuites = (String[]) NativeCrypto.checkEnabledCipherSuites(strArr).clone();
    }

    String[] getEnabledProtocols() {
        return (String[]) this.enabledProtocols.clone();
    }

    void setEnabledProtocols(String[] strArr) {
        if (strArr == null) {
            throw new IllegalArgumentException("protocols == null");
        }
        String[] strArrFilterFromProtocols = filterFromProtocols(strArr, "SSLv3");
        this.isEnabledProtocolsFiltered = strArr.length != strArrFilterFromProtocols.length;
        this.enabledProtocols = (String[]) NativeCrypto.checkEnabledProtocols(strArrFilterFromProtocols).clone();
    }

    void setApplicationProtocols(String[] strArr) {
        this.applicationProtocols = SSLUtils.encodeProtocols(strArr);
    }

    String[] getApplicationProtocols() {
        return SSLUtils.decodeProtocols(this.applicationProtocols);
    }

    void setApplicationProtocolSelector(ApplicationProtocolSelectorAdapter applicationProtocolSelectorAdapter) {
        this.applicationProtocolSelector = applicationProtocolSelectorAdapter;
    }

    void setUseClientMode(boolean z) {
        this.client_mode = z;
    }

    boolean getUseClientMode() {
        return this.client_mode;
    }

    void setNeedClientAuth(boolean z) {
        this.need_client_auth = z;
        this.want_client_auth = false;
    }

    boolean getNeedClientAuth() {
        return this.need_client_auth;
    }

    void setWantClientAuth(boolean z) {
        this.want_client_auth = z;
        this.need_client_auth = false;
    }

    boolean getWantClientAuth() {
        return this.want_client_auth;
    }

    void setEnableSessionCreation(boolean z) {
        this.enable_session_creation = z;
    }

    boolean getEnableSessionCreation() {
        return this.enable_session_creation;
    }

    void setUseSessionTickets(boolean z) {
        this.useSessionTickets = z;
    }

    void setUseSni(boolean z) {
        this.useSni = Boolean.valueOf(z);
    }

    boolean getUseSni() {
        return this.useSni != null ? this.useSni.booleanValue() : isSniEnabledByDefault();
    }

    void setCTVerificationEnabled(boolean z) {
        this.ctVerificationEnabled = z;
    }

    void setSCTExtension(byte[] bArr) {
        this.sctExtension = bArr;
    }

    void setOCSPResponse(byte[] bArr) {
        this.ocspResponse = bArr;
    }

    byte[] getOCSPResponse() {
        return this.ocspResponse;
    }

    private static String[] filterFromProtocols(String[] strArr, String str) {
        if (strArr.length == 1 && str.equals(strArr[0])) {
            return EMPTY_STRING_ARRAY;
        }
        ArrayList arrayList = new ArrayList();
        for (String str2 : strArr) {
            if (!str.equals(str2)) {
                arrayList.add(str2);
            }
        }
        return (String[]) arrayList.toArray(EMPTY_STRING_ARRAY);
    }

    private boolean isSniEnabledByDefault() {
        try {
            String property = System.getProperty("jsse.enableSNIExtension", "true");
            if ("true".equalsIgnoreCase(property)) {
                return true;
            }
            if ("false".equalsIgnoreCase(property)) {
                return false;
            }
            throw new RuntimeException("Can only set \"jsse.enableSNIExtension\" to \"true\" or \"false\"");
        } catch (SecurityException e) {
            return true;
        }
    }

    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    private static X509KeyManager getDefaultX509KeyManager() throws KeyManagementException {
        X509KeyManager x509KeyManager = defaultX509KeyManager;
        if (x509KeyManager == null) {
            X509KeyManager x509KeyManagerCreateDefaultX509KeyManager = createDefaultX509KeyManager();
            defaultX509KeyManager = x509KeyManagerCreateDefaultX509KeyManager;
            return x509KeyManagerCreateDefaultX509KeyManager;
        }
        return x509KeyManager;
    }

    private static X509KeyManager createDefaultX509KeyManager() throws KeyManagementException {
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(null, null);
            KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            X509KeyManager x509KeyManagerFindFirstX509KeyManager = findFirstX509KeyManager(keyManagers);
            if (x509KeyManagerFindFirstX509KeyManager == null) {
                throw new KeyManagementException("No X509KeyManager among default KeyManagers: " + Arrays.toString(keyManagers));
            }
            return x509KeyManagerFindFirstX509KeyManager;
        } catch (KeyStoreException e) {
            throw new KeyManagementException(e);
        } catch (NoSuchAlgorithmException e2) {
            throw new KeyManagementException(e2);
        } catch (UnrecoverableKeyException e3) {
            throw new KeyManagementException(e3);
        }
    }

    private static X509KeyManager findFirstX509KeyManager(KeyManager[] keyManagerArr) {
        for (KeyManager keyManager : keyManagerArr) {
            if (keyManager instanceof X509KeyManager) {
                return (X509KeyManager) keyManager;
            }
        }
        return null;
    }

    private static PSKKeyManager findFirstPSKKeyManager(KeyManager[] keyManagerArr) {
        for (KeyManager keyManager : keyManagerArr) {
            if (keyManager instanceof PSKKeyManager) {
                return (PSKKeyManager) keyManager;
            }
            if (keyManager != null) {
                try {
                    return DuckTypedPSKKeyManager.getInstance(keyManager);
                } catch (NoSuchMethodException e) {
                }
            }
        }
        return null;
    }

    static X509TrustManager getDefaultX509TrustManager() throws KeyManagementException {
        X509TrustManager x509TrustManager = defaultX509TrustManager;
        if (x509TrustManager == null) {
            X509TrustManager x509TrustManagerCreateDefaultX509TrustManager = createDefaultX509TrustManager();
            defaultX509TrustManager = x509TrustManagerCreateDefaultX509TrustManager;
            return x509TrustManagerCreateDefaultX509TrustManager;
        }
        return x509TrustManager;
    }

    private static X509TrustManager createDefaultX509TrustManager() throws KeyManagementException {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            X509TrustManager x509TrustManagerFindFirstX509TrustManager = findFirstX509TrustManager(trustManagers);
            if (x509TrustManagerFindFirstX509TrustManager == null) {
                throw new KeyManagementException("No X509TrustManager in among default TrustManagers: " + Arrays.toString(trustManagers));
            }
            return x509TrustManagerFindFirstX509TrustManager;
        } catch (KeyStoreException e) {
            throw new KeyManagementException(e);
        } catch (NoSuchAlgorithmException e2) {
            throw new KeyManagementException(e2);
        }
    }

    private static X509TrustManager findFirstX509TrustManager(TrustManager[] trustManagerArr) {
        for (TrustManager trustManager : trustManagerArr) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }

    String getEndpointIdentificationAlgorithm() {
        return this.endpointIdentificationAlgorithm;
    }

    void setEndpointIdentificationAlgorithm(String str) {
        this.endpointIdentificationAlgorithm = str;
    }

    boolean getUseCipherSuitesOrder() {
        return this.useCipherSuitesOrder;
    }

    void setUseCipherSuitesOrder(boolean z) {
        this.useCipherSuitesOrder = z;
    }

    private static String[] getDefaultCipherSuites(boolean z, boolean z2) {
        return z ? z2 ? concat(NativeCrypto.DEFAULT_PSK_CIPHER_SUITES, NativeCrypto.DEFAULT_X509_CIPHER_SUITES, new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"}) : concat(NativeCrypto.DEFAULT_X509_CIPHER_SUITES, new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"}) : z2 ? concat(NativeCrypto.DEFAULT_PSK_CIPHER_SUITES, new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"}) : new String[]{"TLS_EMPTY_RENEGOTIATION_INFO_SCSV"};
    }

    private static String[] concat(String[]... strArr) {
        int length = 0;
        for (String[] strArr2 : strArr) {
            length += strArr2.length;
        }
        String[] strArr3 = new String[length];
        int length2 = 0;
        for (String[] strArr4 : strArr) {
            System.arraycopy(strArr4, 0, strArr3, length2, strArr4.length);
            length2 += strArr4.length;
        }
        return strArr3;
    }

    boolean isCTVerificationEnabled(String str) {
        if (str == null) {
            return false;
        }
        if (this.ctVerificationEnabled) {
            return true;
        }
        return Platform.isCTVerificationRequired(str);
    }
}
