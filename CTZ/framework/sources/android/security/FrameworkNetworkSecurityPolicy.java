package android.security;

public class FrameworkNetworkSecurityPolicy extends libcore.net.NetworkSecurityPolicy {
    private final boolean mCleartextTrafficPermitted;

    public FrameworkNetworkSecurityPolicy(boolean z) {
        this.mCleartextTrafficPermitted = z;
    }

    public boolean isCleartextTrafficPermitted() {
        return this.mCleartextTrafficPermitted;
    }

    public boolean isCleartextTrafficPermitted(String str) {
        return isCleartextTrafficPermitted();
    }

    public boolean isCertificateTransparencyVerificationRequired(String str) {
        return false;
    }
}
