package com.android.okhttp;

import com.android.okhttp.internal.Util;
import com.android.okhttp.okio.ByteString;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLPeerUnverifiedException;

public final class CertificatePinner {
    public static final CertificatePinner DEFAULT = new Builder().build();
    private final Map<String, Set<ByteString>> hostnameToPins;

    CertificatePinner(Builder builder, CertificatePinner certificatePinner) {
        this(builder);
    }

    private CertificatePinner(Builder builder) {
        this.hostnameToPins = Util.immutableMap(builder.hostnameToPins);
    }

    public void check(String hostname, List<Certificate> peerCertificates) throws SSLPeerUnverifiedException {
        Set<ByteString> setFindMatchingPins = findMatchingPins(hostname);
        if (setFindMatchingPins == null) {
            return;
        }
        int size = peerCertificates.size();
        for (int i = 0; i < size; i++) {
            if (setFindMatchingPins.contains(sha1((X509Certificate) peerCertificates.get(i)))) {
                return;
            }
        }
        StringBuilder message = new StringBuilder().append("Certificate pinning failure!").append("\n  Peer certificate chain:");
        int size2 = peerCertificates.size();
        for (int i2 = 0; i2 < size2; i2++) {
            X509Certificate x509Certificate = (X509Certificate) peerCertificates.get(i2);
            message.append("\n    ").append(pin(x509Certificate)).append(": ").append(x509Certificate.getSubjectDN().getName());
        }
        message.append("\n  Pinned certificates for ").append(hostname).append(":");
        for (ByteString pin : setFindMatchingPins) {
            message.append("\n    sha1/").append(pin.base64());
        }
        throw new SSLPeerUnverifiedException(message.toString());
    }

    public void check(String hostname, Certificate... peerCertificates) throws SSLPeerUnverifiedException {
        check(hostname, Arrays.asList(peerCertificates));
    }

    Set<ByteString> findMatchingPins(String hostname) {
        Set<ByteString> set = this.hostnameToPins.get(hostname);
        Set<ByteString> set2 = null;
        int indexOfFirstDot = hostname.indexOf(46);
        int indexOfLastDot = hostname.lastIndexOf(46);
        if (indexOfFirstDot != indexOfLastDot) {
            set2 = this.hostnameToPins.get("*." + hostname.substring(indexOfFirstDot + 1));
        }
        if (set == null && set2 == null) {
            return null;
        }
        if (set == null || set2 == null) {
            return set != null ? set : set2;
        }
        Set<okio.ByteString> pins = new LinkedHashSet<>();
        pins.addAll(set);
        pins.addAll(set2);
        return pins;
    }

    public static String pin(Certificate certificate) {
        if (!(certificate instanceof X509Certificate)) {
            throw new IllegalArgumentException("Certificate pinning requires X509 certificates");
        }
        return "sha1/" + sha1((X509Certificate) certificate).base64();
    }

    private static ByteString sha1(X509Certificate x509Certificate) {
        return Util.sha1(ByteString.of(x509Certificate.getPublicKey().getEncoded()));
    }

    public static final class Builder {
        private final Map<String, Set<ByteString>> hostnameToPins = new LinkedHashMap();

        public Builder add(String hostname, String... pins) {
            if (hostname == null) {
                throw new IllegalArgumentException("hostname == null");
            }
            Set<okio.ByteString> hostPins = new LinkedHashSet<>();
            Set<okio.ByteString> previousPins = this.hostnameToPins.put(hostname, Collections.unmodifiableSet(hostPins));
            if (previousPins != null) {
                hostPins.addAll(previousPins);
            }
            for (String pin : pins) {
                if (!pin.startsWith("sha1/")) {
                    throw new IllegalArgumentException("pins must start with 'sha1/': " + pin);
                }
                ByteString decodedPin = ByteString.decodeBase64(pin.substring("sha1/".length()));
                if (decodedPin == null) {
                    throw new IllegalArgumentException("pins must be base64: " + pin);
                }
                hostPins.add(decodedPin);
            }
            return this;
        }

        public CertificatePinner build() {
            return new CertificatePinner(this, null);
        }
    }
}
