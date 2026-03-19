package android.security.net.config;

import android.os.Environment;
import android.os.UserHandle;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Set;

public final class SystemCertificateSource extends DirectoryCertificateSource {
    private final File mUserRemovedCaDir;

    private static class NoPreloadHolder {
        private static final SystemCertificateSource INSTANCE = new SystemCertificateSource();

        private NoPreloadHolder() {
        }
    }

    @Override
    public Set findAllByIssuerAndSignature(X509Certificate x509Certificate) {
        return super.findAllByIssuerAndSignature(x509Certificate);
    }

    @Override
    public X509Certificate findByIssuerAndSignature(X509Certificate x509Certificate) {
        return super.findByIssuerAndSignature(x509Certificate);
    }

    @Override
    public X509Certificate findBySubjectAndPublicKey(X509Certificate x509Certificate) {
        return super.findBySubjectAndPublicKey(x509Certificate);
    }

    @Override
    public Set getCertificates() {
        return super.getCertificates();
    }

    @Override
    public void handleTrustStorageUpdate() {
        super.handleTrustStorageUpdate();
    }

    private SystemCertificateSource() {
        super(new File(System.getenv("ANDROID_ROOT") + "/etc/security/cacerts"));
        this.mUserRemovedCaDir = new File(Environment.getUserConfigDirectory(UserHandle.myUserId()), "cacerts-removed");
    }

    public static SystemCertificateSource getInstance() {
        return NoPreloadHolder.INSTANCE;
    }

    @Override
    protected boolean isCertMarkedAsRemoved(String str) {
        return new File(this.mUserRemovedCaDir, str).exists();
    }
}
