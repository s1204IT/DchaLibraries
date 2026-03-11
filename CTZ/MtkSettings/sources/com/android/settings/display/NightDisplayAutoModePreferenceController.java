package com.android.settings.display;

import android.content.Context;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class NightDisplayAutoModePreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    private ColorDisplayController mController;
    private DropDownPreference mPreference;

    public NightDisplayAutoModePreferenceController(Context context, String str) {
        super(context, str);
        this.mController = new ColorDisplayController(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayController.isAvailable(this.mContext) ? 0 : 2;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = (DropDownPreference) preferenceScreen.findPreference(getPreferenceKey());
        this.mPreference.setEntries(new CharSequence[]{this.mContext.getString(R.string.night_display_auto_mode_never), this.mContext.getString(R.string.night_display_auto_mode_custom), this.mContext.getString(R.string.night_display_auto_mode_twilight)});
        this.mPreference.setEntryValues(new CharSequence[]{String.valueOf(0), String.valueOf(1), String.valueOf(2)});
    }

    @Override
    public final void updateState(Preference preference) {
        this.mPreference.setValue(String.valueOf(this.mController.getAutoMode()));
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object obj) {
        return this.mController.setAutoMode(Integer.parseInt((String) obj));
    }
}
