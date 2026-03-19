package android.security.net.config;

import libcore.net.NetworkSecurityPolicy;

public class ConfigNetworkSecurityPolicy extends NetworkSecurityPolicy {
    private final ApplicationConfig mConfig;

    public ConfigNetworkSecurityPolicy(ApplicationConfig applicationConfig) {
        this.mConfig = applicationConfig;
    }

    public boolean isCleartextTrafficPermitted() {
        return this.mConfig.isCleartextTrafficPermitted();
    }

    public boolean isCleartextTrafficPermitted(String str) {
        return this.mConfig.isCleartextTrafficPermitted(str);
    }

    public boolean isCertificateTransparencyVerificationRequired(String str) {
        return false;
    }
}
