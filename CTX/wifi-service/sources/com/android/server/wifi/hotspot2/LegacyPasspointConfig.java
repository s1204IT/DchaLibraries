package com.android.server.wifi.hotspot2;

import android.text.TextUtils;
import java.util.Arrays;
import java.util.Objects;

public class LegacyPasspointConfig {
    public String mFqdn;
    public String mFriendlyName;
    public String mImsi;
    public String mRealm;
    public long[] mRoamingConsortiumOis;

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LegacyPasspointConfig)) {
            return false;
        }
        LegacyPasspointConfig legacyPasspointConfig = (LegacyPasspointConfig) obj;
        return TextUtils.equals(this.mFqdn, legacyPasspointConfig.mFqdn) && TextUtils.equals(this.mFriendlyName, legacyPasspointConfig.mFriendlyName) && Arrays.equals(this.mRoamingConsortiumOis, legacyPasspointConfig.mRoamingConsortiumOis) && TextUtils.equals(this.mRealm, legacyPasspointConfig.mRealm) && TextUtils.equals(this.mImsi, legacyPasspointConfig.mImsi);
    }

    public int hashCode() {
        return Objects.hash(this.mFqdn, this.mFriendlyName, this.mRoamingConsortiumOis, this.mRealm, this.mImsi);
    }
}
