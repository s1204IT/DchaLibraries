package com.android.settings.core;

import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.List;

public interface PreferenceControllerMixin {
    default void updateNonIndexableKeys(List<String> list) {
        if (this instanceof AbstractPreferenceController) {
            AbstractPreferenceController abstractPreferenceController = (AbstractPreferenceController) this;
            if (!abstractPreferenceController.isAvailable()) {
                String preferenceKey = abstractPreferenceController.getPreferenceKey();
                if (TextUtils.isEmpty(preferenceKey)) {
                    Log.w("PrefControllerMixin", "Skipping updateNonIndexableKeys due to empty key " + toString());
                    return;
                }
                list.add(preferenceKey);
            }
        }
    }
}
