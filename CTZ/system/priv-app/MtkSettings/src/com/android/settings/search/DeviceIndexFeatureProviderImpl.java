package com.android.settings.search;

import android.content.Context;
import android.net.Uri;
import java.util.List;

/* loaded from: classes.dex */
public class DeviceIndexFeatureProviderImpl implements DeviceIndexFeatureProvider {
    @Override // com.android.settings.search.DeviceIndexFeatureProvider
    public boolean isIndexingEnabled() {
        return false;
    }

    @Override // com.android.settings.search.DeviceIndexFeatureProvider
    public void index(Context context, CharSequence charSequence, Uri uri, Uri uri2, List<String> list) {
    }

    @Override // com.android.settings.search.DeviceIndexFeatureProvider
    public void clearIndex(Context context) {
    }
}
