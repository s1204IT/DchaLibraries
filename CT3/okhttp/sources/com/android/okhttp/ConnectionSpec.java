package com.android.okhttp;

import com.android.okhttp.internal.Util;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLSocket;

public final class ConnectionSpec {
    private final String[] cipherSuites;
    final boolean supportsTlsExtensions;
    final boolean tls;
    private final String[] tlsVersions;
    private static final CipherSuite[] APPROVED_CIPHER_SUITES = {CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_DHE_DSS_WITH_AES_128_CBC_SHA, CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA};
    public static final ConnectionSpec MODERN_TLS = new Builder(true).cipherSuites(APPROVED_CIPHER_SUITES).tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0).supportsTlsExtensions(true).build();
    public static final ConnectionSpec COMPATIBLE_TLS = new Builder(MODERN_TLS).tlsVersions(TlsVersion.TLS_1_0).supportsTlsExtensions(true).build();
    public static final ConnectionSpec CLEARTEXT = new Builder(false).build();

    ConnectionSpec(Builder builder, ConnectionSpec connectionSpec) {
        this(builder);
    }

    private ConnectionSpec(Builder builder) {
        this.tls = builder.tls;
        this.cipherSuites = builder.cipherSuites;
        this.tlsVersions = builder.tlsVersions;
        this.supportsTlsExtensions = builder.supportsTlsExtensions;
    }

    public boolean isTls() {
        return this.tls;
    }

    public List<CipherSuite> cipherSuites() {
        if (this.cipherSuites == null) {
            return null;
        }
        CipherSuite[] result = new CipherSuite[this.cipherSuites.length];
        for (int i = 0; i < this.cipherSuites.length; i++) {
            result[i] = CipherSuite.forJavaName(this.cipherSuites[i]);
        }
        return Util.immutableList(result);
    }

    public List<TlsVersion> tlsVersions() {
        TlsVersion[] result = new TlsVersion[this.tlsVersions.length];
        for (int i = 0; i < this.tlsVersions.length; i++) {
            result[i] = TlsVersion.forJavaName(this.tlsVersions[i]);
        }
        return Util.immutableList(result);
    }

    public boolean supportsTlsExtensions() {
        return this.supportsTlsExtensions;
    }

    void apply(SSLSocket sslSocket, boolean isFallback) {
        ConnectionSpec specToApply = supportedSpec(sslSocket, isFallback);
        sslSocket.setEnabledProtocols(specToApply.tlsVersions);
        String[] cipherSuitesToEnable = specToApply.cipherSuites;
        if (cipherSuitesToEnable == null) {
            return;
        }
        sslSocket.setEnabledCipherSuites(cipherSuitesToEnable);
    }

    private ConnectionSpec supportedSpec(SSLSocket sslSocket, boolean isFallback) {
        String[] cipherSuitesToEnable = null;
        if (this.cipherSuites != null) {
            String[] cipherSuitesToSelectFrom = sslSocket.getEnabledCipherSuites();
            cipherSuitesToEnable = (String[]) Util.intersect(String.class, this.cipherSuites, cipherSuitesToSelectFrom);
        }
        if (isFallback) {
            boolean socketSupportsFallbackScsv = Arrays.asList(sslSocket.getSupportedCipherSuites()).contains("TLS_FALLBACK_SCSV");
            if (socketSupportsFallbackScsv) {
                String[] oldEnabledCipherSuites = cipherSuitesToEnable != null ? cipherSuitesToEnable : sslSocket.getEnabledCipherSuites();
                String[] newEnabledCipherSuites = new String[oldEnabledCipherSuites.length + 1];
                System.arraycopy(oldEnabledCipherSuites, 0, newEnabledCipherSuites, 0, oldEnabledCipherSuites.length);
                newEnabledCipherSuites[newEnabledCipherSuites.length - 1] = "TLS_FALLBACK_SCSV";
                cipherSuitesToEnable = newEnabledCipherSuites;
            }
        }
        String[] protocolsToSelectFrom = sslSocket.getEnabledProtocols();
        String[] protocolsToEnable = (String[]) Util.intersect(String.class, this.tlsVersions, protocolsToSelectFrom);
        return new Builder(this).cipherSuites(cipherSuitesToEnable).tlsVersions(protocolsToEnable).build();
    }

    public boolean isCompatible(SSLSocket socket) {
        if (!this.tls) {
            return false;
        }
        String[] enabledProtocols = socket.getEnabledProtocols();
        boolean requiredProtocolsEnabled = nonEmptyIntersection(this.tlsVersions, enabledProtocols);
        if (!requiredProtocolsEnabled) {
            return false;
        }
        if (this.cipherSuites == null) {
            return socket.getEnabledCipherSuites().length > 0;
        }
        String[] enabledCipherSuites = socket.getEnabledCipherSuites();
        return nonEmptyIntersection(this.cipherSuites, enabledCipherSuites);
    }

    private static boolean nonEmptyIntersection(String[] a, String[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return false;
        }
        for (String toFind : a) {
            if (contains(b, toFind)) {
                return true;
            }
        }
        return false;
    }

    private static <T> boolean contains(T[] array, T value) {
        for (T arrayValue : array) {
            if (Util.equal(value, arrayValue)) {
                return true;
            }
        }
        return false;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ConnectionSpec)) {
            return false;
        }
        if (other == this) {
            return true;
        }
        ConnectionSpec that = (ConnectionSpec) other;
        if (this.tls != that.tls) {
            return false;
        }
        return !this.tls || (Arrays.equals(this.cipherSuites, that.cipherSuites) && Arrays.equals(this.tlsVersions, that.tlsVersions) && this.supportsTlsExtensions == that.supportsTlsExtensions);
    }

    public int hashCode() {
        if (!this.tls) {
            return 17;
        }
        int result = Arrays.hashCode(this.cipherSuites) + 527;
        return (((result * 31) + Arrays.hashCode(this.tlsVersions)) * 31) + (this.supportsTlsExtensions ? 0 : 1);
    }

    public String toString() {
        if (!this.tls) {
            return "ConnectionSpec()";
        }
        List<CipherSuite> listCipherSuites = cipherSuites();
        String cipherSuitesString = listCipherSuites == null ? "[use default]" : listCipherSuites.toString();
        return "ConnectionSpec(cipherSuites=" + cipherSuitesString + ", tlsVersions=" + tlsVersions() + ", supportsTlsExtensions=" + this.supportsTlsExtensions + ")";
    }

    public static final class Builder {
        private String[] cipherSuites;
        private boolean supportsTlsExtensions;
        private boolean tls;
        private String[] tlsVersions;

        Builder(boolean tls) {
            this.tls = tls;
        }

        public Builder(ConnectionSpec connectionSpec) {
            this.tls = connectionSpec.tls;
            this.cipherSuites = connectionSpec.cipherSuites;
            this.tlsVersions = connectionSpec.tlsVersions;
            this.supportsTlsExtensions = connectionSpec.supportsTlsExtensions;
        }

        public Builder cipherSuites(CipherSuite... cipherSuites) {
            if (!this.tls) {
                throw new IllegalStateException("no cipher suites for cleartext connections");
            }
            String[] strings = new String[cipherSuites.length];
            for (int i = 0; i < cipherSuites.length; i++) {
                strings[i] = cipherSuites[i].javaName;
            }
            this.cipherSuites = strings;
            return this;
        }

        public Builder cipherSuites(String... cipherSuites) {
            if (!this.tls) {
                throw new IllegalStateException("no cipher suites for cleartext connections");
            }
            if (cipherSuites == null) {
                this.cipherSuites = null;
            } else {
                this.cipherSuites = (String[]) cipherSuites.clone();
            }
            return this;
        }

        public Builder tlsVersions(TlsVersion... tlsVersions) {
            if (!this.tls) {
                throw new IllegalStateException("no TLS versions for cleartext connections");
            }
            if (tlsVersions.length == 0) {
                throw new IllegalArgumentException("At least one TlsVersion is required");
            }
            String[] strings = new String[tlsVersions.length];
            for (int i = 0; i < tlsVersions.length; i++) {
                strings[i] = tlsVersions[i].javaName;
            }
            this.tlsVersions = strings;
            return this;
        }

        public Builder tlsVersions(String... tlsVersions) {
            if (!this.tls) {
                throw new IllegalStateException("no TLS versions for cleartext connections");
            }
            if (tlsVersions == null) {
                this.tlsVersions = null;
            } else {
                this.tlsVersions = (String[]) tlsVersions.clone();
            }
            return this;
        }

        public Builder supportsTlsExtensions(boolean supportsTlsExtensions) {
            if (!this.tls) {
                throw new IllegalStateException("no TLS extensions for cleartext connections");
            }
            this.supportsTlsExtensions = supportsTlsExtensions;
            return this;
        }

        public ConnectionSpec build() {
            return new ConnectionSpec(this, null);
        }
    }
}
