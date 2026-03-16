package com.android.phone.settings;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import com.android.phone.R;

public class TtyModeListPreference extends ListPreference implements Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = TtyModeListPreference.class.getSimpleName();

    public TtyModeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init() {
        setOnPreferenceChangeListener(this);
        int settingsTtyMode = Settings.Secure.getInt(getContext().getContentResolver(), "preferred_tty_mode", 0);
        setValue(Integer.toString(settingsTtyMode));
        updatePreferredTtyModeSummary(settingsTtyMode);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == this) {
            int buttonTtyMode = Integer.valueOf((String) objValue).intValue();
            int settingsTtyMode = Settings.Secure.getInt(getContext().getContentResolver(), "preferred_tty_mode", 0);
            log("handleTTYChange: requesting set TTY mode enable (TTY) to" + Integer.toString(buttonTtyMode));
            if (buttonTtyMode != settingsTtyMode) {
                switch (buttonTtyMode) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        Settings.Secure.putInt(getContext().getContentResolver(), "preferred_tty_mode", buttonTtyMode);
                        break;
                    default:
                        buttonTtyMode = 0;
                        break;
                }
                setValue(Integer.toString(buttonTtyMode));
                updatePreferredTtyModeSummary(buttonTtyMode);
                Intent ttyModeChanged = new Intent("android.telecom.action.TTY_PREFERRED_MODE_CHANGED");
                ttyModeChanged.putExtra("android.telecom.intent.extra.TTY_PREFERRED", buttonTtyMode);
                getContext().sendBroadcastAsUser(ttyModeChanged, UserHandle.ALL);
                return true;
            }
            return true;
        }
        return true;
    }

    private void updatePreferredTtyModeSummary(int TtyMode) {
        String[] txts = getContext().getResources().getStringArray(R.array.tty_mode_entries);
        switch (TtyMode) {
            case 0:
            case 1:
            case 2:
            case 3:
                setSummary(txts[TtyMode]);
                break;
            default:
                setEnabled(false);
                setSummary(txts[0]);
                break;
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
