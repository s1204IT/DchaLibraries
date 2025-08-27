package com.android.settings.notification;

import android.R;
import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.DisabledCheckBoxPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeVisEffectPreferenceController extends AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    protected final int mEffect;
    protected final String mKey;
    protected final int mMetricsCategory;
    protected final int[] mParentSuppressedEffects;
    private PreferenceScreen mScreen;

    public ZenModeVisEffectPreferenceController(Context context, Lifecycle lifecycle, String str, int i, int i2, int[] iArr) {
        super(context, str, lifecycle);
        this.mKey = str;
        this.mEffect = i;
        this.mMetricsCategory = i2;
        this.mParentSuppressedEffects = iArr;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return this.mKey;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        if (this.mEffect == 8) {
            return this.mContext.getResources().getBoolean(R.^attr-private.headerLayout);
        }
        return true;
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mScreen = preferenceScreen;
        super.displayPreference(preferenceScreen);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        boolean zIsVisualEffectSuppressed;
        super.updateState(preference);
        boolean zIsVisualEffectSuppressed2 = this.mBackend.isVisualEffectSuppressed(this.mEffect);
        if (this.mParentSuppressedEffects != null) {
            zIsVisualEffectSuppressed = false;
            for (int i : this.mParentSuppressedEffects) {
                zIsVisualEffectSuppressed |= this.mBackend.isVisualEffectSuppressed(i);
            }
        } else {
            zIsVisualEffectSuppressed = false;
        }
        if (zIsVisualEffectSuppressed) {
            ((CheckBoxPreference) preference).setChecked(zIsVisualEffectSuppressed);
            onPreferenceChange(preference, Boolean.valueOf(zIsVisualEffectSuppressed));
            ((DisabledCheckBoxPreference) preference).enableCheckbox(false);
        } else {
            ((DisabledCheckBoxPreference) preference).enableCheckbox(true);
            ((CheckBoxPreference) preference).setChecked(zIsVisualEffectSuppressed2);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        this.mMetricsFeatureProvider.action(this.mContext, this.mMetricsCategory, zBooleanValue);
        this.mBackend.saveVisualEffectsPolicy(this.mEffect, zBooleanValue);
        return true;
    }
}
