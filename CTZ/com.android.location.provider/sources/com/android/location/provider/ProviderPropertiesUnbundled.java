package com.android.location.provider;

import com.android.internal.location.ProviderProperties;

public final class ProviderPropertiesUnbundled {
    private final ProviderProperties mProperties;

    public static ProviderPropertiesUnbundled create(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, int i, int i2) {
        return new ProviderPropertiesUnbundled(new ProviderProperties(z, z2, z3, z4, z5, z6, z7, i, i2));
    }

    private ProviderPropertiesUnbundled(ProviderProperties providerProperties) {
        this.mProperties = providerProperties;
    }

    public ProviderProperties getProviderProperties() {
        return this.mProperties;
    }

    public String toString() {
        return this.mProperties.toString();
    }
}
