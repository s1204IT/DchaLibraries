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
import com.mediatek.phone.ext.ExtensionManager;

public class TtyModeListPreference extends ListPreference implements Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = TtyModeListPreference.class.getSimpleName();

    public TtyModeListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void init() {
        setOnPreferenceChangeListener(this);
        int i = Settings.Secure.getInt(getContext().getContentResolver(), "preferred_tty_mode", 0);
        setValue(Integer.toString(i));
        updatePreferredTtyModeSummary(i);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this) {
            int i = Integer.parseInt((String) obj);
            int i2 = Settings.Secure.getInt(getContext().getContentResolver(), "preferred_tty_mode", 0);
            log("handleTTYChange: requesting set TTY mode enable (TTY) to" + Integer.toString(i));
            if (i != i2) {
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        Settings.Secure.putInt(getContext().getContentResolver(), "preferred_tty_mode", i);
                        break;
                    default:
                        i = 0;
                        break;
                }
                setValue(Integer.toString(i));
                updatePreferredTtyModeSummary(i);
                ExtensionManager.getTtyModeListPreferenceExt().handleWfcUpdateAndShowMessage(i);
                Intent intent = new Intent("android.telecom.action.TTY_PREFERRED_MODE_CHANGED");
                intent.putExtra("android.telecom.intent.extra.TTY_PREFERRED", i);
                getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
                return true;
            }
            return true;
        }
        return true;
    }

    private void updatePreferredTtyModeSummary(int i) {
        String[] stringArray = getContext().getResources().getStringArray(R.array.tty_mode_entries);
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
                setSummary(stringArray[i]);
                break;
            default:
                setEnabled(false);
                setSummary(stringArray[0]);
                break;
        }
    }

    private static void log(String str) {
        Log.d(LOG_TAG, str);
    }
}
