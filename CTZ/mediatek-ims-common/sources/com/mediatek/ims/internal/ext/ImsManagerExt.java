package com.mediatek.ims.internal.ext;

import android.content.Context;
import android.util.Log;

public class ImsManagerExt implements IImsManagerExt {
    private static final String TAG = "ImsManagerExt";

    @Override
    public boolean isFeatureEnabledByPlatform(Context context, int i, int i2) {
        return true;
    }

    @Override
    public int getImsPhoneId(Context context, int i) {
        Log.d(TAG, "phoneId = " + i);
        return i;
    }
}
