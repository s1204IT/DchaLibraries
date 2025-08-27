package com.android.settings.accessibility;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

/* loaded from: classes.dex */
public class MagnificationNavbarPreferenceController extends TogglePreferenceController {
    private boolean mIsFromSUW;

    public MagnificationNavbarPreferenceController(Context context, String str) {
        super(context, str);
        this.mIsFromSUW = false;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return MagnificationPreferenceFragment.isChecked(this.mContext.getContentResolver(), "accessibility_display_magnification_navbar_enabled");
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        return MagnificationPreferenceFragment.setChecked(this.mContext.getContentResolver(), "accessibility_display_magnification_navbar_enabled", z);
    }

    public void setIsFromSUW(boolean z) {
        this.mIsFromSUW = z;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            Bundle extras = preference.getExtras();
            extras.putString("preference_key", "accessibility_display_magnification_navbar_enabled");
            extras.putInt("title_res", R.string.accessibility_screen_magnification_navbar_title);
            extras.putInt("summary_res", R.string.accessibility_screen_magnification_navbar_summary);
            extras.putBoolean("checked", isChecked());
            extras.putBoolean("from_suw", this.mIsFromSUW);
            return false;
        }
        return false;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        if (MagnificationPreferenceFragment.isApplicable(this.mContext.getResources())) {
            return 0;
        }
        return 2;
    }

    @Override // com.android.settings.core.BasePreferenceController
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "screen_magnification_navbar_preference_screen");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public CharSequence getSummary() {
        int i;
        if (this.mIsFromSUW) {
            i = R.string.accessibility_screen_magnification_navbar_short_summary;
        } else {
            i = isChecked() ? R.string.accessibility_feature_state_on : R.string.accessibility_feature_state_off;
        }
        return this.mContext.getText(i);
    }
}
