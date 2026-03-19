package android.security.net.config;

import java.security.cert.X509Certificate;

public final class TrustAnchor {
    public final X509Certificate certificate;
    public final boolean overridesPins;

    public TrustAnchor(X509Certificate x509Certificate, boolean z) {
        if (x509Certificate == null) {
            throw new NullPointerException("certificate");
        }
        this.certificate = x509Certificate;
        this.overridesPins = z;
    }
}
