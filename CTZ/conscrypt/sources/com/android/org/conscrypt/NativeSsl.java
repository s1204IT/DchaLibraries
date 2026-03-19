package com.android.org.conscrypt;

import com.android.org.conscrypt.NativeCrypto;
import com.android.org.conscrypt.SSLParametersImpl;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.X509KeyManager;
import javax.security.auth.x500.X500Principal;

final class NativeSsl {
    private final SSLParametersImpl.AliasChooser aliasChooser;
    private final NativeCrypto.SSLHandshakeCallbacks handshakeCallbacks;
    private X509Certificate[] localCertificates;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final SSLParametersImpl parameters;
    private final SSLParametersImpl.PSKCallbacks pskCallbacks;
    private volatile long ssl;

    private NativeSsl(long j, SSLParametersImpl sSLParametersImpl, NativeCrypto.SSLHandshakeCallbacks sSLHandshakeCallbacks, SSLParametersImpl.AliasChooser aliasChooser, SSLParametersImpl.PSKCallbacks pSKCallbacks) {
        this.ssl = j;
        this.parameters = sSLParametersImpl;
        this.handshakeCallbacks = sSLHandshakeCallbacks;
        this.aliasChooser = aliasChooser;
        this.pskCallbacks = pSKCallbacks;
    }

    static NativeSsl newInstance(SSLParametersImpl sSLParametersImpl, NativeCrypto.SSLHandshakeCallbacks sSLHandshakeCallbacks, SSLParametersImpl.AliasChooser aliasChooser, SSLParametersImpl.PSKCallbacks pSKCallbacks) throws SSLException {
        AbstractSessionContext sessionContext = sSLParametersImpl.getSessionContext();
        return new NativeSsl(NativeCrypto.SSL_new(sessionContext.sslCtxNativePointer, sessionContext), sSLParametersImpl, sSLHandshakeCallbacks, aliasChooser, pSKCallbacks);
    }

    BioWrapper newBio() {
        try {
            return new BioWrapper();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    void offerToResumeSession(long j) throws SSLException {
        NativeCrypto.SSL_set_session(this.ssl, this, j);
    }

    byte[] getSessionId() {
        return NativeCrypto.SSL_session_id(this.ssl, this);
    }

    long getTime() {
        return NativeCrypto.SSL_get_time(this.ssl, this);
    }

    long getTimeout() {
        return NativeCrypto.SSL_get_timeout(this.ssl, this);
    }

    void setTimeout(long j) {
        NativeCrypto.SSL_set_timeout(this.ssl, this, j);
    }

    String getCipherSuite() {
        return NativeCrypto.cipherSuiteToJava(NativeCrypto.SSL_get_current_cipher(this.ssl, this));
    }

    X509Certificate[] getPeerCertificates() throws CertificateException {
        byte[][] bArrSSL_get0_peer_certificates = NativeCrypto.SSL_get0_peer_certificates(this.ssl, this);
        if (bArrSSL_get0_peer_certificates == null) {
            return null;
        }
        return SSLUtils.decodeX509CertificateChain(bArrSSL_get0_peer_certificates);
    }

    X509Certificate[] getLocalCertificates() {
        return this.localCertificates;
    }

    byte[] getPeerCertificateOcspData() {
        return NativeCrypto.SSL_get_ocsp_response(this.ssl, this);
    }

    byte[] getTlsUnique() {
        return NativeCrypto.SSL_get_tls_unique(this.ssl, this);
    }

    byte[] getPeerTlsSctData() {
        return NativeCrypto.SSL_get_signed_cert_timestamp_list(this.ssl, this);
    }

    int clientPSKKeyRequested(String str, byte[] bArr, byte[] bArr2) {
        byte[] bytes;
        PSKKeyManager pSKKeyManager = this.parameters.getPSKKeyManager();
        if (pSKKeyManager == null) {
            return 0;
        }
        String strChooseClientPSKIdentity = this.pskCallbacks.chooseClientPSKIdentity(pSKKeyManager, str);
        if (strChooseClientPSKIdentity == null) {
            strChooseClientPSKIdentity = "";
            bytes = EmptyArray.BYTE;
        } else if (strChooseClientPSKIdentity.isEmpty()) {
            bytes = EmptyArray.BYTE;
        } else {
            try {
                bytes = strChooseClientPSKIdentity.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 encoding not supported", e);
            }
        }
        if (bytes.length + 1 > bArr.length) {
            return 0;
        }
        if (bytes.length > 0) {
            System.arraycopy(bytes, 0, bArr, 0, bytes.length);
        }
        bArr[bytes.length] = 0;
        byte[] encoded = this.pskCallbacks.getPSKKey(pSKKeyManager, str, strChooseClientPSKIdentity).getEncoded();
        if (encoded != null && encoded.length <= bArr2.length) {
            System.arraycopy(encoded, 0, bArr2, 0, encoded.length);
            return encoded.length;
        }
        return 0;
    }

    int serverPSKKeyRequested(String str, String str2, byte[] bArr) {
        byte[] encoded;
        PSKKeyManager pSKKeyManager = this.parameters.getPSKKeyManager();
        if (pSKKeyManager != null && (encoded = this.pskCallbacks.getPSKKey(pSKKeyManager, str, str2).getEncoded()) != null && encoded.length <= bArr.length) {
            System.arraycopy(encoded, 0, bArr, 0, encoded.length);
            return encoded.length;
        }
        return 0;
    }

    void chooseClientCertificate(byte[] bArr, byte[][] bArr2) throws SSLException, CertificateEncodingException {
        X500Principal[] x500PrincipalArr;
        Set<String> supportedClientKeyTypes = SSLUtils.getSupportedClientKeyTypes(bArr);
        String[] strArr = (String[]) supportedClientKeyTypes.toArray(new String[supportedClientKeyTypes.size()]);
        String strChooseClientAlias = null;
        if (bArr2 != null) {
            x500PrincipalArr = new X500Principal[bArr2.length];
            for (int i = 0; i < bArr2.length; i++) {
                x500PrincipalArr[i] = new X500Principal(bArr2[i]);
            }
        } else {
            x500PrincipalArr = null;
        }
        X509KeyManager x509KeyManager = this.parameters.getX509KeyManager();
        if (x509KeyManager != null) {
            strChooseClientAlias = this.aliasChooser.chooseClientAlias(x509KeyManager, x500PrincipalArr, strArr);
        }
        setCertificate(strChooseClientAlias);
    }

    void setCertificate(String str) throws SSLException, CertificateEncodingException {
        X509KeyManager x509KeyManager;
        PrivateKey privateKey;
        if (str == null || (x509KeyManager = this.parameters.getX509KeyManager()) == null || (privateKey = x509KeyManager.getPrivateKey(str)) == null) {
            return;
        }
        this.localCertificates = x509KeyManager.getCertificateChain(str);
        if (this.localCertificates == null) {
            return;
        }
        int length = this.localCertificates.length;
        PublicKey publicKey = length > 0 ? this.localCertificates[0].getPublicKey() : null;
        byte[][] bArr = new byte[length][];
        for (int i = 0; i < length; i++) {
            bArr[i] = this.localCertificates[i].getEncoded();
        }
        try {
            NativeCrypto.setLocalCertsAndPrivateKey(this.ssl, this, bArr, OpenSSLKey.fromPrivateKeyForTLSStackOnly(privateKey, publicKey).getNativeRef());
        } catch (InvalidKeyException e) {
            throw new SSLException(e);
        }
    }

    String getVersion() {
        return NativeCrypto.SSL_get_version(this.ssl, this);
    }

    String getRequestedServerName() {
        return NativeCrypto.SSL_get_servername(this.ssl, this);
    }

    byte[] getTlsChannelId() throws SSLException {
        return NativeCrypto.SSL_get_tls_channel_id(this.ssl, this);
    }

    void initialize(String str, OpenSSLKey openSSLKey) throws IOException {
        if (!this.parameters.getEnableSessionCreation()) {
            NativeCrypto.SSL_set_session_creation_enabled(this.ssl, this, false);
        }
        NativeCrypto.SSL_accept_renegotiations(this.ssl, this);
        if (isClient()) {
            NativeCrypto.SSL_set_connect_state(this.ssl, this);
            NativeCrypto.SSL_enable_ocsp_stapling(this.ssl, this);
            if (this.parameters.isCTVerificationEnabled(str)) {
                NativeCrypto.SSL_enable_signed_cert_timestamps(this.ssl, this);
            }
        } else {
            NativeCrypto.SSL_set_accept_state(this.ssl, this);
            if (this.parameters.getOCSPResponse() != null) {
                NativeCrypto.SSL_enable_ocsp_stapling(this.ssl, this);
            }
        }
        if (this.parameters.getEnabledProtocols().length == 0 && this.parameters.isEnabledProtocolsFiltered) {
            throw new SSLHandshakeException("No enabled protocols; SSLv3 is no longer supported and was filtered from the list");
        }
        NativeCrypto.setEnabledProtocols(this.ssl, this, this.parameters.enabledProtocols);
        NativeCrypto.setEnabledCipherSuites(this.ssl, this, this.parameters.enabledCipherSuites);
        if (this.parameters.applicationProtocols.length > 0) {
            NativeCrypto.setApplicationProtocols(this.ssl, this, isClient(), this.parameters.applicationProtocols);
        }
        if (!isClient() && this.parameters.applicationProtocolSelector != null) {
            NativeCrypto.setApplicationProtocolSelector(this.ssl, this, this.parameters.applicationProtocolSelector);
        }
        if (!isClient()) {
            HashSet hashSet = new HashSet();
            for (long j : NativeCrypto.SSL_get_ciphers(this.ssl, this)) {
                String serverX509KeyType = SSLUtils.getServerX509KeyType(j);
                if (serverX509KeyType != null) {
                    hashSet.add(serverX509KeyType);
                }
            }
            X509KeyManager x509KeyManager = this.parameters.getX509KeyManager();
            if (x509KeyManager != null) {
                Iterator it = hashSet.iterator();
                while (it.hasNext()) {
                    try {
                        setCertificate(this.aliasChooser.chooseServerAlias(x509KeyManager, (String) it.next()));
                    } catch (CertificateEncodingException e) {
                        throw new IOException(e);
                    }
                }
            }
            NativeCrypto.SSL_set_options(this.ssl, this, 4194304L);
            if (this.parameters.sctExtension != null) {
                NativeCrypto.SSL_set_signed_cert_timestamp_list(this.ssl, this, this.parameters.sctExtension);
            }
            if (this.parameters.ocspResponse != null) {
                NativeCrypto.SSL_set_ocsp_response(this.ssl, this, this.parameters.ocspResponse);
            }
        }
        enablePSKKeyManagerIfRequested();
        if (this.parameters.useSessionTickets) {
            NativeCrypto.SSL_clear_options(this.ssl, this, 16384L);
        } else {
            NativeCrypto.SSL_set_options(this.ssl, this, NativeCrypto.SSL_get_options(this.ssl, this) | 16384);
        }
        if (this.parameters.getUseSni() && AddressUtils.isValidSniHostname(str)) {
            NativeCrypto.SSL_set_tlsext_host_name(this.ssl, this, str);
        }
        NativeCrypto.SSL_set_mode(this.ssl, this, 256L);
        setCertificateValidation();
        setTlsChannelId(openSSLKey);
    }

    void doHandshake(FileDescriptor fileDescriptor, int i) throws IOException, CertificateException {
        this.lock.readLock().lock();
        try {
            if (isClosed() || fileDescriptor == null || !fileDescriptor.valid()) {
                throw new SocketException("Socket is closed");
            }
            NativeCrypto.SSL_do_handshake(this.ssl, this, fileDescriptor, this.handshakeCallbacks, i);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    int doHandshake() throws IOException {
        this.lock.readLock().lock();
        try {
            return NativeCrypto.ENGINE_SSL_do_handshake(this.ssl, this, this.handshakeCallbacks);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    int read(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3) throws IOException {
        this.lock.readLock().lock();
        try {
            if (isClosed() || fileDescriptor == null || !fileDescriptor.valid()) {
                throw new SocketException("Socket is closed");
            }
            return NativeCrypto.SSL_read(this.ssl, this, fileDescriptor, this.handshakeCallbacks, bArr, i, i2, i3);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    void write(FileDescriptor fileDescriptor, byte[] bArr, int i, int i2, int i3) throws IOException {
        this.lock.readLock().lock();
        try {
            if (isClosed() || fileDescriptor == null || !fileDescriptor.valid()) {
                throw new SocketException("Socket is closed");
            }
            NativeCrypto.SSL_write(this.ssl, this, fileDescriptor, this.handshakeCallbacks, bArr, i, i2, i3);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private void enablePSKKeyManagerIfRequested() throws SSLException {
        PSKKeyManager pSKKeyManager = this.parameters.getPSKKeyManager();
        if (pSKKeyManager != null) {
            String[] strArr = this.parameters.enabledCipherSuites;
            int length = strArr.length;
            boolean z = false;
            int i = 0;
            while (true) {
                if (i < length) {
                    String str = strArr[i];
                    if (str == null || !str.contains("PSK")) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (z) {
                if (isClient()) {
                    NativeCrypto.set_SSL_psk_client_callback_enabled(this.ssl, this, true);
                    return;
                }
                NativeCrypto.set_SSL_psk_server_callback_enabled(this.ssl, this, true);
                NativeCrypto.SSL_use_psk_identity_hint(this.ssl, this, this.pskCallbacks.chooseServerPSKIdentityHint(pSKKeyManager));
            }
        }
    }

    private void setTlsChannelId(OpenSSLKey openSSLKey) throws SSLException {
        if (!this.parameters.channelIdEnabled) {
            return;
        }
        if (this.parameters.getUseClientMode()) {
            if (openSSLKey == null) {
                throw new SSLHandshakeException("Invalid TLS channel ID key specified");
            }
            NativeCrypto.SSL_set1_tls_channel_id(this.ssl, this, openSSLKey.getNativeRef());
            return;
        }
        NativeCrypto.SSL_enable_tls_channel_id(this.ssl, this);
    }

    private void setCertificateValidation() throws SSLException {
        X509Certificate[] acceptedIssuers;
        if (!isClient()) {
            boolean z = true;
            if (this.parameters.getNeedClientAuth()) {
                NativeCrypto.SSL_set_verify(this.ssl, this, 3);
            } else if (this.parameters.getWantClientAuth()) {
                NativeCrypto.SSL_set_verify(this.ssl, this, 1);
            } else {
                NativeCrypto.SSL_set_verify(this.ssl, this, 0);
                z = false;
            }
            if (z && (acceptedIssuers = this.parameters.getX509TrustManager().getAcceptedIssuers()) != null && acceptedIssuers.length != 0) {
                try {
                    NativeCrypto.SSL_set_client_CA_list(this.ssl, this, SSLUtils.encodeIssuerX509Principals(acceptedIssuers));
                } catch (CertificateEncodingException e) {
                    throw new SSLException("Problem encoding principals", e);
                }
            }
        }
    }

    void interrupt() {
        NativeCrypto.SSL_interrupt(this.ssl, this);
    }

    void shutdown(FileDescriptor fileDescriptor) throws IOException {
        NativeCrypto.SSL_shutdown(this.ssl, this, fileDescriptor, this.handshakeCallbacks);
    }

    void shutdown() throws IOException {
        NativeCrypto.ENGINE_SSL_shutdown(this.ssl, this, this.handshakeCallbacks);
    }

    boolean wasShutdownReceived() {
        return (NativeCrypto.SSL_get_shutdown(this.ssl, this) & 2) != 0;
    }

    boolean wasShutdownSent() {
        return (NativeCrypto.SSL_get_shutdown(this.ssl, this) & 1) != 0;
    }

    int readDirectByteBuffer(long j, int i) throws IOException, CertificateException {
        this.lock.readLock().lock();
        try {
            return NativeCrypto.ENGINE_SSL_read_direct(this.ssl, this, j, i, this.handshakeCallbacks);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    int writeDirectByteBuffer(long j, int i) throws IOException {
        this.lock.readLock().lock();
        try {
            return NativeCrypto.ENGINE_SSL_write_direct(this.ssl, this, j, i, this.handshakeCallbacks);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    int getPendingReadableBytes() {
        return NativeCrypto.SSL_pending_readable_bytes(this.ssl, this);
    }

    int getMaxSealOverhead() {
        return NativeCrypto.SSL_max_seal_overhead(this.ssl, this);
    }

    void close() {
        this.lock.writeLock().lock();
        try {
            if (!isClosed()) {
                long j = this.ssl;
                this.ssl = 0L;
                NativeCrypto.SSL_free(j, this);
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    boolean isClosed() {
        return this.ssl == 0;
    }

    int getError(int i) {
        return NativeCrypto.SSL_get_error(this.ssl, this, i);
    }

    byte[] getApplicationProtocol() {
        return NativeCrypto.getApplicationProtocol(this.ssl, this);
    }

    private boolean isClient() {
        return this.parameters.getUseClientMode();
    }

    protected final void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    final class BioWrapper {
        private long bio;

        private BioWrapper() throws SSLException {
            this.bio = NativeCrypto.SSL_BIO_new(NativeSsl.this.ssl, NativeSsl.this);
        }

        int getPendingWrittenBytes() {
            return NativeCrypto.SSL_pending_written_bytes_in_BIO(this.bio);
        }

        int writeDirectByteBuffer(long j, int i) throws IOException {
            return NativeCrypto.ENGINE_SSL_write_BIO_direct(NativeSsl.this.ssl, NativeSsl.this, this.bio, j, i, NativeSsl.this.handshakeCallbacks);
        }

        int readDirectByteBuffer(long j, int i) throws IOException {
            return NativeCrypto.ENGINE_SSL_read_BIO_direct(NativeSsl.this.ssl, NativeSsl.this, this.bio, j, i, NativeSsl.this.handshakeCallbacks);
        }

        void close() {
            NativeCrypto.BIO_free_all(this.bio);
            this.bio = 0L;
        }
    }
}
