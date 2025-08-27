package com.android.settings.slices;

import android.content.Context;
import com.android.settings.wifi.calling.WifiCallingSliceHelper;
import com.android.settingslib.utils.ThreadUtils;

/* loaded from: classes.dex */
public class SlicesFeatureProviderImpl implements SlicesFeatureProvider {
    private SliceDataConverter mSliceDataConverter;
    private SlicesIndexer mSlicesIndexer;

    public SlicesIndexer getSliceIndexer(Context context) {
        if (this.mSlicesIndexer == null) {
            this.mSlicesIndexer = new SlicesIndexer(context);
        }
        return this.mSlicesIndexer;
    }

    @Override // com.android.settings.slices.SlicesFeatureProvider
    public SliceDataConverter getSliceDataConverter(Context context) {
        if (this.mSliceDataConverter == null) {
            this.mSliceDataConverter = new SliceDataConverter(context.getApplicationContext());
        }
        return this.mSliceDataConverter;
    }

    @Override // com.android.settings.slices.SlicesFeatureProvider
    public void indexSliceDataAsync(Context context) {
        ThreadUtils.postOnBackgroundThread(getSliceIndexer(context));
    }

    @Override // com.android.settings.slices.SlicesFeatureProvider
    public void indexSliceData(Context context) {
        getSliceIndexer(context).indexSliceData();
    }

    @Override // com.android.settings.slices.SlicesFeatureProvider
    public WifiCallingSliceHelper getNewWifiCallingSliceHelper(Context context) {
        return new WifiCallingSliceHelper(context);
    }
}
