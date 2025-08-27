package com.android.settings.core;

import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.List;

/* loaded from: classes.dex */
public interface PreferenceControllerMixin {
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.settings.core.PreferenceControllerMixin */
    /* JADX WARN: Multi-variable type inference failed */
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
