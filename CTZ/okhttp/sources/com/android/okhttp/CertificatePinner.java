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

    private CertificatePinner(Builder builder) {
        this.hostnameToPins = Util.immutableMap(builder.hostnameToPins);
    }

    public void check(String str, List<Certificate> list) throws SSLPeerUnverifiedException {
        Set<ByteString> setFindMatchingPins = findMatchingPins(str);
        if (setFindMatchingPins == null) {
            return;
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (setFindMatchingPins.contains(sha1((X509Certificate) list.get(i)))) {
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Certificate pinning failure!");
        sb.append("\n  Peer certificate chain:");
        int size2 = list.size();
        for (int i2 = 0; i2 < size2; i2++) {
            X509Certificate x509Certificate = (X509Certificate) list.get(i2);
            sb.append("\n    ");
            sb.append(pin(x509Certificate));
            sb.append(": ");
            sb.append(x509Certificate.getSubjectDN().getName());
        }
        sb.append("\n  Pinned certificates for ");
        sb.append(str);
        sb.append(":");
        for (ByteString byteString : setFindMatchingPins) {
            sb.append("\n    sha1/");
            sb.append(byteString.base64());
        }
        throw new SSLPeerUnverifiedException(sb.toString());
    }

    public void check(String str, Certificate... certificateArr) throws SSLPeerUnverifiedException {
        check(str, Arrays.asList(certificateArr));
    }

    Set<ByteString> findMatchingPins(String str) {
        Set<ByteString> set;
        Set<ByteString> set2 = this.hostnameToPins.get(str);
        int iIndexOf = str.indexOf(46);
        if (iIndexOf != str.lastIndexOf(46)) {
            set = this.hostnameToPins.get("*." + str.substring(iIndexOf + 1));
        } else {
            set = null;
        }
        if (set2 == null && set == null) {
            return null;
        }
        if (set2 == null || set == null) {
            return set2 != null ? set2 : set;
        }
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        linkedHashSet.addAll(set2);
        linkedHashSet.addAll(set);
        return linkedHashSet;
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

        public Builder add(String str, String... strArr) {
            if (str == null) {
                throw new IllegalArgumentException("hostname == null");
            }
            LinkedHashSet linkedHashSet = new LinkedHashSet();
            Set<ByteString> setPut = this.hostnameToPins.put(str, Collections.unmodifiableSet(linkedHashSet));
            if (setPut != null) {
                linkedHashSet.addAll(setPut);
            }
            for (String str2 : strArr) {
                if (!str2.startsWith("sha1/")) {
                    throw new IllegalArgumentException("pins must start with 'sha1/': " + str2);
                }
                ByteString byteStringDecodeBase64 = ByteString.decodeBase64(str2.substring("sha1/".length()));
                if (byteStringDecodeBase64 == null) {
                    throw new IllegalArgumentException("pins must be base64: " + str2);
                }
                linkedHashSet.add(byteStringDecodeBase64);
            }
            return this;
        }

        public CertificatePinner build() {
            return new CertificatePinner(this);
        }
    }
}
