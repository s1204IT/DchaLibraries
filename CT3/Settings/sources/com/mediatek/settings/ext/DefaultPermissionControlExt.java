package com.mediatek.settings.ext;

import android.content.Context;
import android.content.ContextWrapper;
import android.provider.SearchIndexableData;
import android.support.v7.preference.PreferenceGroup;
import android.util.Log;
import java.util.List;

public class DefaultPermissionControlExt extends ContextWrapper implements IPermissionControlExt {
    private static final String TAG = "DefaultPermissionControlExt";

    public DefaultPermissionControlExt(Context context) {
        super(context);
    }

    @Override
    public void addPermSwitchPrf(PreferenceGroup prefGroup) {
        Log.d(TAG, "will not add permission preference");
    }

    @Override
    public void enablerResume() {
        Log.d(TAG, "enablerResume() default");
    }

    @Override
    public void enablerPause() {
        Log.d(TAG, "enablerPause() default");
    }

    @Override
    public void addAutoBootPrf(PreferenceGroup prefGroup) {
        Log.d(TAG, "will not add auto boot entry preference");
    }

    @Override
    public List<SearchIndexableData> getRawDataToIndex(boolean enabled) {
        Log.d(TAG, "default , null");
        return null;
    }
}
