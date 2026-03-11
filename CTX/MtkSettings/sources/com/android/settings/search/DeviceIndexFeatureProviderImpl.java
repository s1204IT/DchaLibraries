package com.android.settings.search;

import android.content.Context;
import android.net.Uri;
import java.util.List;

public class DeviceIndexFeatureProviderImpl implements DeviceIndexFeatureProvider {
    @Override
    public boolean isIndexingEnabled() {
        return false;
    }

    @Override
    public void index(Context context, CharSequence charSequence, Uri uri, Uri uri2, List<String> list) {
    }

    @Override
    public void clearIndex(Context context) {
    }
}
