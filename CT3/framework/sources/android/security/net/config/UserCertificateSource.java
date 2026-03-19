package android.security.net.config;

import android.os.Environment;
import android.os.UserHandle;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.Set;

public final class UserCertificateSource extends DirectoryCertificateSource {
    UserCertificateSource(UserCertificateSource userCertificateSource) {
        this();
    }

    @Override
    public Set findAllByIssuerAndSignature(X509Certificate cert) {
        return super.findAllByIssuerAndSignature(cert);
    }

    @Override
    public X509Certificate findByIssuerAndSignature(X509Certificate cert) {
        return super.findByIssuerAndSignature(cert);
    }

    @Override
    public X509Certificate findBySubjectAndPublicKey(X509Certificate cert) {
        return super.findBySubjectAndPublicKey(cert);
    }

    @Override
    public Set getCertificates() {
        return super.getCertificates();
    }

    @Override
    public void handleTrustStorageUpdate() {
        super.handleTrustStorageUpdate();
    }

    private static class NoPreloadHolder {
        private static final UserCertificateSource INSTANCE = new UserCertificateSource(null);

        private NoPreloadHolder() {
        }
    }

    private UserCertificateSource() {
        super(new File(Environment.getUserConfigDirectory(UserHandle.myUserId()), "cacerts-added"));
    }

    public static UserCertificateSource getInstance() {
        return NoPreloadHolder.INSTANCE;
    }

    @Override
    protected boolean isCertMarkedAsRemoved(String caFile) {
        return false;
    }
}
