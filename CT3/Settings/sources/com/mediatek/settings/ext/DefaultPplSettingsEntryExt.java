package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;

public class DefaultPplSettingsEntryExt extends ContextWrapper implements IPplSettingsEntryExt {
    private static final String TAG = "PPL/PplSettingsEntryExt";

    public DefaultPplSettingsEntryExt(Context context) {
        super(context);
    }

    @Override
    public void addPplPrf(PreferenceGroup prefGroup) {
        Log.d(TAG, "addPplPrf() default");
    }

    @Override
    public void enablerResume() {
        Log.d(TAG, "enablerResume() default");
    }

    @Override
    public void enablerPause() {
        Log.d(TAG, "enablerPause() default");
    }
}
