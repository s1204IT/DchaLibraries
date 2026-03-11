package com.android.settings.development;

import android.app.UiModeManager;
import android.content.Context;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class DarkUIPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final UiModeManager mUiModeManager;

    public DarkUIPreferenceController(Context context) {
        this(context, (UiModeManager) context.getSystemService(UiModeManager.class));
    }

    DarkUIPreferenceController(Context context, UiModeManager uiModeManager) {
        super(context);
        this.mUiModeManager = uiModeManager;
    }

    @Override
    public String getPreferenceKey() {
        return "dark_ui_mode";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        this.mUiModeManager.setNightMode(modeToInt((String) obj));
        updateSummary(preference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary(preference);
    }

    private void updateSummary(Preference preference) {
        int nightMode = this.mUiModeManager.getNightMode();
        ((ListPreference) preference).setValue(modeToString(nightMode));
        preference.setSummary(modeToDescription(nightMode));
    }

    private String modeToDescription(int i) {
        String[] stringArray = this.mContext.getResources().getStringArray(R.array.dark_ui_mode_entries);
        if (i == 0) {
            return stringArray[0];
        }
        if (i == 2) {
            return stringArray[1];
        }
        return stringArray[2];
    }

    private String modeToString(int i) {
        if (i == 0) {
            return "auto";
        }
        if (i == 2) {
            return "yes";
        }
        return "no";
    }

    private int modeToInt(String str) {
        byte b;
        int iHashCode = str.hashCode();
        if (iHashCode != 3521) {
            if (iHashCode != 119527) {
                b = (iHashCode == 3005871 && str.equals("auto")) ? (byte) 0 : (byte) -1;
            } else if (str.equals("yes")) {
                b = 1;
            }
        } else if (str.equals("no")) {
            b = 2;
        }
        switch (b) {
            case 0:
                return 0;
            case 1:
                return 2;
            default:
                return 1;
        }
    }
}
