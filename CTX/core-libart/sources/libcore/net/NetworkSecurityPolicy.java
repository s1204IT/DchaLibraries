package libcore.net;

public abstract class NetworkSecurityPolicy {
    private static volatile NetworkSecurityPolicy instance = new DefaultNetworkSecurityPolicy();

    public abstract boolean isCertificateTransparencyVerificationRequired(String str);

    public abstract boolean isCleartextTrafficPermitted();

    public abstract boolean isCleartextTrafficPermitted(String str);

    public static NetworkSecurityPolicy getInstance() {
        return instance;
    }

    public static void setInstance(NetworkSecurityPolicy networkSecurityPolicy) {
        if (networkSecurityPolicy == null) {
            throw new NullPointerException("policy == null");
        }
        instance = networkSecurityPolicy;
    }

    public static final class DefaultNetworkSecurityPolicy extends NetworkSecurityPolicy {
        @Override
        public boolean isCleartextTrafficPermitted() {
            return true;
        }

        @Override
        public boolean isCleartextTrafficPermitted(String str) {
            return isCleartextTrafficPermitted();
        }

        @Override
        public boolean isCertificateTransparencyVerificationRequired(String str) {
            return false;
        }
    }
}
