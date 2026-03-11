package com.android.settings.slices;

import android.content.Context;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;

public interface SlicesFeatureProvider {
    WifiCallingSliceHelper getNewWifiCallingSliceHelper(Context context);

    SliceDataConverter getSliceDataConverter(Context context);

    void indexSliceData(Context context);

    void indexSliceDataAsync(Context context);
}
