package com.android.settingslib.core;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

/* loaded from: classes.dex */
public abstract class AbstractPreferenceController {
    protected final Context mContext;

    public abstract String getPreferenceKey();

    public abstract boolean isAvailable();

    public AbstractPreferenceController(Context context) {
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.android.settingslib.core.AbstractPreferenceController */
    /* JADX WARN: Multi-variable type inference failed */
    public void displayPreference(PreferenceScreen preferenceScreen) {
        String preferenceKey = getPreferenceKey();
        if (isAvailable()) {
            setVisible(preferenceScreen, preferenceKey, true);
            if (this instanceof Preference.OnPreferenceChangeListener) {
                preferenceScreen.findPreference(preferenceKey).setOnPreferenceChangeListener((Preference.OnPreferenceChangeListener) this);
                return;
            }
            return;
        }
        setVisible(preferenceScreen, preferenceKey, false);
    }

    public void updateState(Preference preference) {
        refreshSummary(preference);
    }

    protected void refreshSummary(Preference preference) {
        CharSequence summary;
        if (preference == null || (summary = getSummary()) == null) {
            return;
        }
        preference.setSummary(summary);
    }

    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    protected final void setVisible(PreferenceGroup preferenceGroup, String str, boolean z) {
        Preference preferenceFindPreference = preferenceGroup.findPreference(str);
        if (preferenceFindPreference != null) {
            preferenceFindPreference.setVisible(z);
        }
    }

    public CharSequence getSummary() {
        return null;
    }
}
