package android.security.net.config;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.util.Pair;
import java.util.Set;

public class ManifestConfigSource implements ConfigSource {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "NetworkSecurityConfig";
    private final ApplicationInfo mApplicationInfo;
    private ConfigSource mConfigSource;
    private final Context mContext;
    private final Object mLock = new Object();

    public ManifestConfigSource(Context context) {
        this.mContext = context;
        this.mApplicationInfo = new ApplicationInfo(context.getApplicationInfo());
    }

    @Override
    public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
        return getConfigSource().getPerDomainConfigs();
    }

    @Override
    public NetworkSecurityConfig getDefaultConfig() {
        return getConfigSource().getDefaultConfig();
    }

    private ConfigSource getConfigSource() {
        ConfigSource defaultConfigSource;
        synchronized (this.mLock) {
            if (this.mConfigSource != null) {
                return this.mConfigSource;
            }
            int i = this.mApplicationInfo.networkSecurityConfigRes;
            boolean z = false;
            if (i != 0) {
                if ((2 & this.mApplicationInfo.flags) != 0) {
                    z = true;
                }
                Log.d(LOG_TAG, "Using Network Security Config from resource " + this.mContext.getResources().getResourceEntryName(i) + " debugBuild: " + z);
                defaultConfigSource = new XmlConfigSource(this.mContext, i, this.mApplicationInfo);
            } else {
                Log.d(LOG_TAG, "No Network Security Config specified, using platform default");
                if ((this.mApplicationInfo.flags & 134217728) != 0 && this.mApplicationInfo.targetSandboxVersion < 2) {
                    z = true;
                }
                defaultConfigSource = new DefaultConfigSource(z, this.mApplicationInfo);
            }
            this.mConfigSource = defaultConfigSource;
            return this.mConfigSource;
        }
    }

    private static final class DefaultConfigSource implements ConfigSource {
        private final NetworkSecurityConfig mDefaultConfig;

        DefaultConfigSource(boolean z, ApplicationInfo applicationInfo) {
            this.mDefaultConfig = NetworkSecurityConfig.getDefaultBuilder(applicationInfo).setCleartextTrafficPermitted(z).build();
        }

        @Override
        public NetworkSecurityConfig getDefaultConfig() {
            return this.mDefaultConfig;
        }

        @Override
        public Set<Pair<Domain, NetworkSecurityConfig>> getPerDomainConfigs() {
            return null;
        }
    }
}
