package com.android.settings.core;

import android.content.Context;
import android.content.IntentFilter;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import com.android.settings.search.ResultPayload;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.core.AbstractPreferenceController;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/* loaded from: classes.dex */
public abstract class BasePreferenceController extends AbstractPreferenceController {
    public static final int AVAILABLE = 0;
    public static final int CONDITIONALLY_UNAVAILABLE = 1;
    public static final int DISABLED_DEPENDENT_SETTING = 4;
    public static final int DISABLED_FOR_USER = 3;
    private static final String TAG = "SettingsPrefController";
    public static final int UNSUPPORTED_ON_DEVICE = 2;
    protected final String mPreferenceKey;

    public abstract int getAvailabilityStatus();

    public static BasePreferenceController createInstance(Context context, String str, String str2) {
        try {
            return (BasePreferenceController) Class.forName(str).getConstructor(Context.class, String.class).newInstance(context, str2);
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Invalid preference controller: " + str, e);
        }
    }

    public static BasePreferenceController createInstance(Context context, String str) {
        try {
            return (BasePreferenceController) Class.forName(str).getConstructor(Context.class).newInstance(context);
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException("Invalid preference controller: " + str, e);
        }
    }

    public BasePreferenceController(Context context, String str) {
        super(context);
        this.mPreferenceKey = str;
        if (TextUtils.isEmpty(this.mPreferenceKey)) {
            throw new IllegalArgumentException("Preference key must be set");
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mPreferenceKey;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public final boolean isAvailable() {
        int availabilityStatus = getAvailabilityStatus();
        return availabilityStatus == 0 || availabilityStatus == 4;
    }

    public final boolean isSupported() {
        return getAvailabilityStatus() != 2;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        Preference preferenceFindPreference;
        super.displayPreference(preferenceScreen);
        if (getAvailabilityStatus() == 4 && (preferenceFindPreference = preferenceScreen.findPreference(getPreferenceKey())) != null) {
            preferenceFindPreference.setEnabled(false);
        }
    }

    public int getSliceType() {
        return 0;
    }

    public IntentFilter getIntentFilter() {
        return null;
    }

    public boolean isSliceable() {
        return false;
    }

    public boolean hasAsyncUpdate() {
        return false;
    }

    public void updateNonIndexableKeys(List<String> list) {
        if ((this instanceof AbstractPreferenceController) && !isAvailable()) {
            String preferenceKey = getPreferenceKey();
            if (TextUtils.isEmpty(preferenceKey)) {
                Log.w(TAG, "Skipping updateNonIndexableKeys due to empty key " + toString());
                return;
            }
            list.add(preferenceKey);
        }
    }

    public void updateRawDataToIndex(List<SearchIndexableRaw> list) {
    }

    @Deprecated
    public ResultPayload getResultPayload() {
        return null;
    }
}
