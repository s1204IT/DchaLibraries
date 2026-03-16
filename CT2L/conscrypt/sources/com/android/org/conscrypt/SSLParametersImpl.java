package com.android.org.conscrypt;

import com.android.org.conscrypt.util.EmptyArray;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

public class SSLParametersImpl implements Cloneable {
    private static final String KEY_TYPE_DH_DSA = "DH_DSA";
    private static final String KEY_TYPE_DH_RSA = "DH_RSA";
    private static final String KEY_TYPE_DSA = "DSA";
    private static final String KEY_TYPE_EC = "EC";
    private static final String KEY_TYPE_EC_EC = "EC_EC";
    private static final String KEY_TYPE_EC_RSA = "EC_RSA";
    private static final String KEY_TYPE_RSA = "RSA";
    private static volatile SSLParametersImpl defaultParameters;
    private static volatile SecureRandom defaultSecureRandom;
    private static volatile X509KeyManager defaultX509KeyManager;
    private static volatile X509TrustManager defaultX509TrustManager;
    byte[] alpnProtocols;
    boolean channelIdEnabled;
    private final ClientSessionContext clientSessionContext;
    private String[] enabledCipherSuites;
    private String[] enabledProtocols;
    private String endpointIdentificationAlgorithm;
    byte[] npnProtocols;
    private final PSKKeyManager pskKeyManager;
    private SecureRandom secureRandom;
    private final ServerSessionContext serverSessionContext;
    boolean useSessionTickets;
    private Boolean useSni;
    private final X509KeyManager x509KeyManager;
    private final X509TrustManager x509TrustManager;
    private boolean client_mode = true;
    private boolean need_client_auth = false;
    private boolean want_client_auth = false;
    private boolean enable_session_creation = true;

    public interface AliasChooser {
        String chooseClientAlias(X509KeyManager x509KeyManager, X500Principal[] x500PrincipalArr, String[] strArr);

        String chooseServerAlias(X509KeyManager x509KeyManager, String str);
    }

    public interface PSKCallbacks {
        String chooseClientPSKIdentity(PSKKeyManager pSKKeyManager, String str);

        String chooseServerPSKIdentityHint(PSKKeyManager pSKKeyManager);

        SecretKey getPSKKey(PSKKeyManager pSKKeyManager, String str, String str2);
    }

    protected SSLParametersImpl(KeyManager[] kms, TrustManager[] tms, SecureRandom sr, ClientSessionContext clientSessionContext, ServerSessionContext serverSessionContext) throws KeyManagementException {
        this.serverSessionContext = serverSessionContext;
        this.clientSessionContext = clientSessionContext;
        if (kms == null) {
            this.x509KeyManager = getDefaultX509KeyManager();
            this.pskKeyManager = null;
        } else {
            this.x509KeyManager = findFirstX509KeyManager(kms);
            this.pskKeyManager = findFirstPSKKeyManager(kms);
        }
        if (tms == null) {
            this.x509TrustManager = getDefaultX509TrustManager();
        } else {
            this.x509TrustManager = findFirstX509TrustManager(tms);
        }
        this.secureRandom = sr;
        this.enabledProtocols = getDefaultProtocols();
        boolean x509CipherSuitesNeeded = (this.x509KeyManager == null && this.x509TrustManager == null) ? false : true;
        boolean pskCipherSuitesNeeded = this.pskKeyManager != null;
        this.enabledCipherSuites = getDefaultCipherSuites(x509CipherSuitesNeeded, pskCipherSuitesNeeded);
    }

    protected static SSLParametersImpl getDefault() throws KeyManagementException {
        SSLParametersImpl result = defaultParameters;
        if (result == null) {
            result = new SSLParametersImpl(null, null, null, new ClientSessionContext(), new ServerSessionContext());
            defaultParameters = result;
        }
        return (SSLParametersImpl) result.clone();
    }

    public AbstractSessionContext getSessionContext() {
        return this.client_mode ? this.clientSessionContext : this.serverSessionContext;
    }

    protected ServerSessionContext getServerSessionContext() {
        return this.serverSessionContext;
    }

    protected ClientSessionContext getClientSessionContext() {
        return this.clientSessionContext;
    }

    protected X509KeyManager getX509KeyManager() {
        return this.x509KeyManager;
    }

    protected PSKKeyManager getPSKKeyManager() {
        return this.pskKeyManager;
    }

    protected X509TrustManager getX509TrustManager() {
        return this.x509TrustManager;
    }

    protected SecureRandom getSecureRandom() {
        if (this.secureRandom != null) {
            return this.secureRandom;
        }
        SecureRandom result = defaultSecureRandom;
        if (result == null) {
            result = new SecureRandom();
            defaultSecureRandom = result;
        }
        this.secureRandom = result;
        return this.secureRandom;
    }

    protected SecureRandom getSecureRandomMember() {
        return this.secureRandom;
    }

    protected String[] getEnabledCipherSuites() {
        return (String[]) this.enabledCipherSuites.clone();
    }

    protected void setEnabledCipherSuites(String[] cipherSuites) {
        this.enabledCipherSuites = (String[]) NativeCrypto.checkEnabledCipherSuites(cipherSuites).clone();
    }

    protected String[] getEnabledProtocols() {
        return (String[]) this.enabledProtocols.clone();
    }

    protected void setEnabledProtocols(String[] protocols) {
        this.enabledProtocols = (String[]) NativeCrypto.checkEnabledProtocols(protocols).clone();
    }

    protected void setUseClientMode(boolean mode) {
        this.client_mode = mode;
    }

    protected boolean getUseClientMode() {
        return this.client_mode;
    }

    protected void setNeedClientAuth(boolean need) {
        this.need_client_auth = need;
        this.want_client_auth = false;
    }

    protected boolean getNeedClientAuth() {
        return this.need_client_auth;
    }

    protected void setWantClientAuth(boolean want) {
        this.want_client_auth = want;
        this.need_client_auth = false;
    }

    protected boolean getWantClientAuth() {
        return this.want_client_auth;
    }

    protected void setEnableSessionCreation(boolean flag) {
        this.enable_session_creation = flag;
    }

    protected boolean getEnableSessionCreation() {
        return this.enable_session_creation;
    }

    protected void setUseSni(boolean flag) {
        this.useSni = Boolean.valueOf(flag);
    }

    protected boolean getUseSni() {
        return this.useSni != null ? this.useSni.booleanValue() : isSniEnabledByDefault();
    }

    static byte[][] encodeIssuerX509Principals(X509Certificate[] certificates) throws CertificateEncodingException {
        byte[][] principalBytes = new byte[certificates.length][];
        for (int i = 0; i < certificates.length; i++) {
            principalBytes[i] = certificates[i].getIssuerX500Principal().getEncoded();
        }
        return principalBytes;
    }

    private static OpenSSLX509Certificate[] createCertChain(long[] certificateRefs) throws IOException {
        if (certificateRefs == null) {
            return null;
        }
        OpenSSLX509Certificate[] certificates = new OpenSSLX509Certificate[certificateRefs.length];
        for (int i = 0; i < certificateRefs.length; i++) {
            certificates[i] = new OpenSSLX509Certificate(certificateRefs[i]);
        }
        return certificates;
    }

    OpenSSLSessionImpl getSessionToReuse(long sslNativePointer, String hostname, int port) throws SSLException {
        if (this.client_mode) {
            OpenSSLSessionImpl sessionToReuse = getCachedClientSession(this.clientSessionContext, hostname, port);
            if (sessionToReuse != null) {
                NativeCrypto.SSL_set_session(sslNativePointer, sessionToReuse.sslSessionNativePointer);
                return sessionToReuse;
            }
            return sessionToReuse;
        }
        return null;
    }

    void setTlsChannelId(long sslNativePointer, OpenSSLKey channelIdPrivateKey) throws SSLException {
        if (this.channelIdEnabled) {
            if (this.client_mode) {
                if (channelIdPrivateKey == null) {
                    throw new SSLHandshakeException("Invalid TLS channel ID key specified");
                }
                NativeCrypto.SSL_set1_tls_channel_id(sslNativePointer, channelIdPrivateKey.getPkeyContext());
                return;
            }
            NativeCrypto.SSL_enable_tls_channel_id(sslNativePointer);
        }
    }

    void setCertificate(long sslNativePointer, String alias) throws SSLException, CertificateEncodingException {
        X509KeyManager keyManager;
        PrivateKey privateKey;
        X509Certificate[] certificates;
        if (alias != null && (keyManager = getX509KeyManager()) != null && (privateKey = keyManager.getPrivateKey(alias)) != null && (certificates = keyManager.getCertificateChain(alias)) != null) {
            OpenSSLX509Certificate[] openSslCerts = new OpenSSLX509Certificate[certificates.length];
            long[] x509refs = new long[certificates.length];
            for (int i = 0; i < certificates.length; i++) {
                OpenSSLX509Certificate openSslCert = OpenSSLX509Certificate.fromCertificate(certificates[i]);
                openSslCerts[i] = openSslCert;
                x509refs[i] = openSslCert.getContext();
            }
            NativeCrypto.SSL_use_certificate(sslNativePointer, x509refs);
            try {
                OpenSSLKey key = OpenSSLKey.fromPrivateKey(privateKey);
                NativeCrypto.SSL_use_PrivateKey(sslNativePointer, key.getPkeyContext());
                if (!key.isWrapped()) {
                    NativeCrypto.SSL_check_private_key(sslNativePointer);
                }
            } catch (InvalidKeyException e) {
                throw new SSLException(e);
            }
        }
    }

    void setSSLParameters(long sslCtxNativePointer, long sslNativePointer, AliasChooser chooser, PSKCallbacks pskCallbacks, String sniHostname) throws IOException {
        if (this.npnProtocols != null) {
            NativeCrypto.SSL_CTX_enable_npn(sslCtxNativePointer);
        }
        if (this.client_mode && this.alpnProtocols != null) {
            NativeCrypto.SSL_set_alpn_protos(sslNativePointer, this.alpnProtocols);
        }
        NativeCrypto.setEnabledProtocols(sslNativePointer, this.enabledProtocols);
        NativeCrypto.setEnabledCipherSuites(sslNativePointer, this.enabledCipherSuites);
        if (!this.client_mode) {
            Set<String> keyTypes = new HashSet<>();
            long[] arr$ = NativeCrypto.SSL_get_ciphers(sslNativePointer);
            for (long sslCipherNativePointer : arr$) {
                String keyType = getServerX509KeyType(sslCipherNativePointer);
                if (keyType != null) {
                    keyTypes.add(keyType);
                }
            }
            X509KeyManager keyManager = getX509KeyManager();
            if (keyManager != null) {
                Iterator<String> it = keyTypes.iterator();
                while (it.hasNext()) {
                    try {
                        setCertificate(sslNativePointer, chooser.chooseServerAlias(this.x509KeyManager, it.next()));
                    } catch (CertificateEncodingException e) {
                        throw new IOException(e);
                    }
                }
            }
        }
        PSKKeyManager pskKeyManager = getPSKKeyManager();
        if (pskKeyManager != null) {
            boolean pskEnabled = false;
            String[] arr$2 = this.enabledCipherSuites;
            int len$ = arr$2.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                String enabledCipherSuite = arr$2[i$];
                if (enabledCipherSuite == null || !enabledCipherSuite.contains("PSK")) {
                    i$++;
                } else {
                    pskEnabled = true;
                    break;
                }
            }
            if (pskEnabled) {
                if (this.client_mode) {
                    NativeCrypto.set_SSL_psk_client_callback_enabled(sslNativePointer, true);
                } else {
                    NativeCrypto.set_SSL_psk_server_callback_enabled(sslNativePointer, true);
                    String identityHint = pskCallbacks.chooseServerPSKIdentityHint(pskKeyManager);
                    NativeCrypto.SSL_use_psk_identity_hint(sslNativePointer, identityHint);
                }
            }
        }
        if (this.useSessionTickets) {
            NativeCrypto.SSL_clear_options(sslNativePointer, NativeCrypto.SSL_OP_NO_TICKET);
        }
        if (getUseSni() && AddressUtils.isValidSniHostname(sniHostname)) {
            NativeCrypto.SSL_set_tlsext_host_name(sslNativePointer, sniHostname);
        }
        NativeCrypto.SSL_set_mode(sslNativePointer, 256L);
        boolean enableSessionCreation = getEnableSessionCreation();
        if (!enableSessionCreation) {
            NativeCrypto.SSL_set_session_creation_enabled(sslNativePointer, enableSessionCreation);
        }
    }

    private static boolean isValidSniHostname(String sniHostname) {
        return (sniHostname == null || sniHostname.indexOf(46) == -1 || Platform.isLiteralIpAddress(sniHostname)) ? false : true;
    }

    private boolean isSniEnabledByDefault() {
        String enableSNI = System.getProperty("jsse.enableSNIExtension", Platform.isSniEnabledByDefault() ? "true" : "false");
        if ("true".equalsIgnoreCase(enableSNI)) {
            return true;
        }
        if ("false".equalsIgnoreCase(enableSNI)) {
            return false;
        }
        throw new RuntimeException("Can only set \"jsse.enableSNIExtension\" to \"true\" or \"false\"");
    }

    void setCertificateValidation(long sslNativePointer) throws IOException {
        boolean certRequested;
        if (!this.client_mode) {
            if (getNeedClientAuth()) {
                NativeCrypto.SSL_set_verify(sslNativePointer, 3);
                certRequested = true;
            } else if (getWantClientAuth()) {
                NativeCrypto.SSL_set_verify(sslNativePointer, 1);
                certRequested = true;
            } else {
                NativeCrypto.SSL_set_verify(sslNativePointer, 0);
                certRequested = false;
            }
            if (certRequested) {
                X509TrustManager trustManager = getX509TrustManager();
                X509Certificate[] issuers = trustManager.getAcceptedIssuers();
                if (issuers != null && issuers.length != 0) {
                    try {
                        byte[][] issuersBytes = encodeIssuerX509Principals(issuers);
                        NativeCrypto.SSL_set_client_CA_list(sslNativePointer, issuersBytes);
                    } catch (CertificateEncodingException e) {
                        throw new IOException("Problem encoding principals", e);
                    }
                }
            }
        }
    }

    OpenSSLSessionImpl setupSession(long sslSessionNativePointer, long sslNativePointer, OpenSSLSessionImpl sessionToReuse, String hostname, int port, boolean handshakeCompleted) throws IOException {
        if (sessionToReuse != null && NativeCrypto.SSL_session_reused(sslNativePointer)) {
            sessionToReuse.lastAccessedTime = System.currentTimeMillis();
            NativeCrypto.SSL_SESSION_free(sslSessionNativePointer);
            return sessionToReuse;
        }
        if (!getEnableSessionCreation()) {
            throw new IllegalStateException("SSL Session may not be created");
        }
        X509Certificate[] localCertificates = createCertChain(NativeCrypto.SSL_get_certificate(sslNativePointer));
        X509Certificate[] peerCertificates = createCertChain(NativeCrypto.SSL_get_peer_cert_chain(sslNativePointer));
        OpenSSLSessionImpl sslSession = new OpenSSLSessionImpl(sslSessionNativePointer, localCertificates, peerCertificates, hostname, port, getSessionContext());
        if (handshakeCompleted) {
            getSessionContext().putSession(sslSession);
            return sslSession;
        }
        return sslSession;
    }

    void chooseClientCertificate(byte[] keyTypeBytes, byte[][] asn1DerEncodedPrincipals, long sslNativePointer, AliasChooser chooser) throws SSLException, CertificateEncodingException {
        X500Principal[] issuers;
        String[] keyTypes = new String[keyTypeBytes.length];
        for (int i = 0; i < keyTypeBytes.length; i++) {
            keyTypes[i] = getClientKeyType(keyTypeBytes[i]);
        }
        if (asn1DerEncodedPrincipals == null) {
            issuers = null;
        } else {
            issuers = new X500Principal[asn1DerEncodedPrincipals.length];
            for (int i2 = 0; i2 < asn1DerEncodedPrincipals.length; i2++) {
                issuers[i2] = new X500Principal(asn1DerEncodedPrincipals[i2]);
            }
        }
        X509KeyManager keyManager = getX509KeyManager();
        String alias = keyManager != null ? chooser.chooseClientAlias(keyManager, issuers, keyTypes) : null;
        setCertificate(sslNativePointer, alias);
    }

    int clientPSKKeyRequested(String identityHint, byte[] identityBytesOut, byte[] key, PSKCallbacks pskCallbacks) {
        byte[] identityBytes;
        PSKKeyManager pskKeyManager = getPSKKeyManager();
        if (pskKeyManager == null) {
            return 0;
        }
        String identity = pskCallbacks.chooseClientPSKIdentity(pskKeyManager, identityHint);
        if (identity == null) {
            identity = "";
            identityBytes = EmptyArray.BYTE;
        } else if (identity.isEmpty()) {
            identityBytes = EmptyArray.BYTE;
        } else {
            try {
                identityBytes = identity.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 encoding not supported", e);
            }
        }
        if (identityBytes.length + 1 > identityBytesOut.length) {
            return 0;
        }
        if (identityBytes.length > 0) {
            System.arraycopy(identityBytes, 0, identityBytesOut, 0, identityBytes.length);
        }
        identityBytesOut[identityBytes.length] = 0;
        SecretKey secretKey = pskCallbacks.getPSKKey(pskKeyManager, identityHint, identity);
        byte[] secretKeyBytes = secretKey.getEncoded();
        if (secretKeyBytes == null || secretKeyBytes.length > key.length) {
            return 0;
        }
        System.arraycopy(secretKeyBytes, 0, key, 0, secretKeyBytes.length);
        return secretKeyBytes.length;
    }

    int serverPSKKeyRequested(String identityHint, String identity, byte[] key, PSKCallbacks pskCallbacks) {
        PSKKeyManager pskKeyManager = getPSKKeyManager();
        if (pskKeyManager == null) {
            return 0;
        }
        SecretKey secretKey = pskCallbacks.getPSKKey(pskKeyManager, identityHint, identity);
        byte[] secretKeyBytes = secretKey.getEncoded();
        if (secretKeyBytes == null || secretKeyBytes.length > key.length) {
            return 0;
        }
        System.arraycopy(secretKeyBytes, 0, key, 0, secretKeyBytes.length);
        return secretKeyBytes.length;
    }

    OpenSSLSessionImpl getCachedClientSession(ClientSessionContext sessionContext, String hostName, int port) {
        OpenSSLSessionImpl session;
        if (hostName != null && (session = (OpenSSLSessionImpl) sessionContext.getSession(hostName, port)) != null) {
            String protocol = session.getProtocol();
            boolean protocolFound = false;
            String[] arr$ = this.enabledProtocols;
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    break;
                }
                String enabledProtocol = arr$[i$];
                if (!protocol.equals(enabledProtocol)) {
                    i$++;
                } else {
                    protocolFound = true;
                    break;
                }
            }
            if (!protocolFound) {
                return null;
            }
            String cipherSuite = session.getCipherSuite();
            boolean cipherSuiteFound = false;
            String[] arr$2 = this.enabledCipherSuites;
            int len$2 = arr$2.length;
            int i$2 = 0;
            while (true) {
                if (i$2 >= len$2) {
                    break;
                }
                String enabledCipherSuite = arr$2[i$2];
                if (!cipherSuite.equals(enabledCipherSuite)) {
                    i$2++;
                } else {
                    cipherSuiteFound = true;
                    break;
                }
            }
            if (cipherSuiteFound) {
                return session;
            }
            return null;
        }
        return null;
    }

    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    private static X509KeyManager getDefaultX509KeyManager() throws KeyManagementException {
        X509KeyManager result = defaultX509KeyManager;
        if (result == null) {
            X509KeyManager result2 = createDefaultX509KeyManager();
            defaultX509KeyManager = result2;
            return result2;
        }
        return result;
    }

    private static X509KeyManager createDefaultX509KeyManager() throws KeyManagementException {
        try {
            String algorithm = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(null, null);
            KeyManager[] kms = kmf.getKeyManagers();
            X509KeyManager result = findFirstX509KeyManager(kms);
            if (result == null) {
                throw new KeyManagementException("No X509KeyManager among default KeyManagers: " + Arrays.toString(kms));
            }
            return result;
        } catch (KeyStoreException e) {
            throw new KeyManagementException(e);
        } catch (NoSuchAlgorithmException e2) {
            throw new KeyManagementException(e2);
        } catch (UnrecoverableKeyException e3) {
            throw new KeyManagementException(e3);
        }
    }

    private static X509KeyManager findFirstX509KeyManager(KeyManager[] kms) {
        for (KeyManager km : kms) {
            if (km instanceof X509KeyManager) {
                return (X509KeyManager) km;
            }
        }
        return null;
    }

    private static PSKKeyManager findFirstPSKKeyManager(KeyManager[] kms) {
        for (KeyManager km : kms) {
            if (km instanceof PSKKeyManager) {
                return (PSKKeyManager) km;
            }
        }
        return null;
    }

    public static X509TrustManager getDefaultX509TrustManager() throws KeyManagementException {
        X509TrustManager result = defaultX509TrustManager;
        if (result == null) {
            X509TrustManager result2 = createDefaultX509TrustManager();
            defaultX509TrustManager = result2;
            return result2;
        }
        return result;
    }

    private static X509TrustManager createDefaultX509TrustManager() throws KeyManagementException {
        try {
            String algorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
            tmf.init((KeyStore) null);
            TrustManager[] tms = tmf.getTrustManagers();
            X509TrustManager trustManager = findFirstX509TrustManager(tms);
            if (trustManager == null) {
                throw new KeyManagementException("No X509TrustManager in among default TrustManagers: " + Arrays.toString(tms));
            }
            return trustManager;
        } catch (KeyStoreException e) {
            throw new KeyManagementException(e);
        } catch (NoSuchAlgorithmException e2) {
            throw new KeyManagementException(e2);
        }
    }

    private static X509TrustManager findFirstX509TrustManager(TrustManager[] tms) {
        for (TrustManager tm : tms) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        return null;
    }

    public String getEndpointIdentificationAlgorithm() {
        return this.endpointIdentificationAlgorithm;
    }

    public void setEndpointIdentificationAlgorithm(String endpointIdentificationAlgorithm) {
        this.endpointIdentificationAlgorithm = endpointIdentificationAlgorithm;
    }

    private static String getServerX509KeyType(long sslCipherNative) throws SSLException {
        int algorithm_mkey = NativeCrypto.get_SSL_CIPHER_algorithm_mkey(sslCipherNative);
        int algorithm_auth = NativeCrypto.get_SSL_CIPHER_algorithm_auth(sslCipherNative);
        switch (algorithm_mkey) {
            case 1:
                return KEY_TYPE_RSA;
            case 8:
                switch (algorithm_auth) {
                    case 1:
                        return KEY_TYPE_RSA;
                    case 2:
                        return KEY_TYPE_DSA;
                    case 4:
                        return null;
                }
            case 32:
                return KEY_TYPE_EC_RSA;
            case 64:
                return KEY_TYPE_EC_EC;
            case 128:
                switch (algorithm_auth) {
                    case 1:
                        return KEY_TYPE_RSA;
                    case 4:
                    case 128:
                        return null;
                    case 64:
                        return KEY_TYPE_EC_EC;
                }
            case 256:
                return null;
        }
        throw new SSLException("Unsupported key exchange. mkey: 0x" + Long.toHexString(((long) algorithm_mkey) & 4294967295L) + ", auth: 0x" + Long.toHexString(((long) algorithm_auth) & 4294967295L));
    }

    public static String getClientKeyType(byte keyType) {
        switch (keyType) {
            case 1:
                return KEY_TYPE_RSA;
            case 2:
                return KEY_TYPE_DSA;
            case 3:
                return KEY_TYPE_DH_RSA;
            case 4:
                return KEY_TYPE_DH_DSA;
            case 64:
                return KEY_TYPE_EC;
            case 65:
                return KEY_TYPE_EC_RSA;
            case 66:
                return KEY_TYPE_EC_EC;
            default:
                return null;
        }
    }

    private static String[] getDefaultCipherSuites(boolean x509CipherSuitesNeeded, boolean pskCipherSuitesNeeded) {
        return x509CipherSuitesNeeded ? pskCipherSuitesNeeded ? concat(NativeCrypto.DEFAULT_PSK_CIPHER_SUITES, NativeCrypto.DEFAULT_X509_CIPHER_SUITES, new String[]{NativeCrypto.TLS_EMPTY_RENEGOTIATION_INFO_SCSV}) : concat(NativeCrypto.DEFAULT_X509_CIPHER_SUITES, new String[]{NativeCrypto.TLS_EMPTY_RENEGOTIATION_INFO_SCSV}) : pskCipherSuitesNeeded ? concat(NativeCrypto.DEFAULT_PSK_CIPHER_SUITES, new String[]{NativeCrypto.TLS_EMPTY_RENEGOTIATION_INFO_SCSV}) : new String[]{NativeCrypto.TLS_EMPTY_RENEGOTIATION_INFO_SCSV};
    }

    private static String[] getDefaultProtocols() {
        return (String[]) NativeCrypto.DEFAULT_PROTOCOLS.clone();
    }

    private static String[] concat(String[]... arrays) {
        int resultLength = 0;
        for (String[] strArr : arrays) {
            resultLength += strArr.length;
        }
        String[] result = new String[resultLength];
        int resultOffset = 0;
        for (String[] array : arrays) {
            System.arraycopy(array, 0, result, resultOffset, array.length);
            resultOffset += array.length;
        }
        return result;
    }
}
