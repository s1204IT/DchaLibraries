package com.android.managedprovisioning.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import com.android.internal.annotations.Immutable;
import com.android.managedprovisioning.common.PersistableBundlable;

@Immutable
public final class WifiInfo extends PersistableBundlable {
    public static final Parcelable.Creator<WifiInfo> CREATOR = new Parcelable.Creator<WifiInfo>() {
        @Override
        public WifiInfo createFromParcel(Parcel parcel) {
            return new WifiInfo(parcel);
        }

        @Override
        public WifiInfo[] newArray(int i) {
            return new WifiInfo[i];
        }
    };
    public final boolean hidden;
    public final String pacUrl;
    public final String password;
    public final String proxyBypassHosts;
    public final String proxyHost;
    public final int proxyPort;
    public final String securityType;
    public final String ssid;

    @Override
    public PersistableBundle toPersistableBundle() {
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putString("android.app.extra.PROVISIONING_WIFI_SSID", this.ssid);
        persistableBundle.putBoolean("android.app.extra.PROVISIONING_WIFI_HIDDEN", this.hidden);
        persistableBundle.putString("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE", this.securityType);
        persistableBundle.putString("android.app.extra.PROVISIONING_WIFI_PASSWORD", this.password);
        persistableBundle.putString("android.app.extra.PROVISIONING_WIFI_PROXY_HOST", this.proxyHost);
        persistableBundle.putInt("android.app.extra.PROVISIONING_WIFI_PROXY_PORT", this.proxyPort);
        persistableBundle.putString("android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS", this.proxyBypassHosts);
        persistableBundle.putString("android.app.extra.PROVISIONING_WIFI_PAC_URL", this.pacUrl);
        return persistableBundle;
    }

    static WifiInfo fromPersistableBundle(PersistableBundle persistableBundle) {
        return createBuilderFromPersistableBundle(persistableBundle).build();
    }

    private static Builder createBuilderFromPersistableBundle(PersistableBundle persistableBundle) {
        Builder builder = new Builder();
        builder.setSsid(persistableBundle.getString("android.app.extra.PROVISIONING_WIFI_SSID"));
        builder.setHidden(persistableBundle.getBoolean("android.app.extra.PROVISIONING_WIFI_HIDDEN"));
        builder.setSecurityType(persistableBundle.getString("android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE"));
        builder.setPassword(persistableBundle.getString("android.app.extra.PROVISIONING_WIFI_PASSWORD"));
        builder.setProxyHost(persistableBundle.getString("android.app.extra.PROVISIONING_WIFI_PROXY_HOST"));
        builder.setProxyPort(persistableBundle.getInt("android.app.extra.PROVISIONING_WIFI_PROXY_PORT"));
        builder.setProxyBypassHosts(persistableBundle.getString("android.app.extra.PROVISIONING_WIFI_PROXY_BYPASS"));
        builder.setPacUrl(persistableBundle.getString("android.app.extra.PROVISIONING_WIFI_PAC_URL"));
        return builder;
    }

    private WifiInfo(Builder builder) {
        this.ssid = builder.mSsid;
        this.hidden = builder.mHidden;
        this.securityType = builder.mSecurityType;
        this.password = builder.mPassword;
        this.proxyHost = builder.mProxyHost;
        this.proxyPort = builder.mProxyPort;
        this.proxyBypassHosts = builder.mProxyBypassHosts;
        this.pacUrl = builder.mPacUrl;
        validateFields();
    }

    private WifiInfo(Parcel parcel) {
        this(createBuilderFromPersistableBundle(PersistableBundlable.getPersistableBundleFromParcel(parcel)));
    }

    private void validateFields() {
        if (TextUtils.isEmpty(this.ssid)) {
            throw new IllegalArgumentException("Ssid must not be empty!");
        }
    }

    public static final class Builder {
        private String mPacUrl;
        private String mPassword;
        private String mProxyBypassHosts;
        private String mProxyHost;
        private String mSecurityType;
        private String mSsid;
        private boolean mHidden = false;
        private int mProxyPort = 0;

        public Builder setSsid(String str) {
            this.mSsid = str;
            return this;
        }

        public Builder setHidden(boolean z) {
            this.mHidden = z;
            return this;
        }

        public Builder setSecurityType(String str) {
            this.mSecurityType = str;
            return this;
        }

        public Builder setPassword(String str) {
            this.mPassword = str;
            return this;
        }

        public Builder setProxyHost(String str) {
            this.mProxyHost = str;
            return this;
        }

        public Builder setProxyPort(int i) {
            this.mProxyPort = i;
            return this;
        }

        public Builder setProxyBypassHosts(String str) {
            this.mProxyBypassHosts = str;
            return this;
        }

        public Builder setPacUrl(String str) {
            this.mPacUrl = str;
            return this;
        }

        public WifiInfo build() {
            return new WifiInfo(this);
        }

        public static Builder builder() {
            return new Builder();
        }
    }
}
