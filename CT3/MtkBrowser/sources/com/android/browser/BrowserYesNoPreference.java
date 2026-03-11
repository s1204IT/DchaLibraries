package com.android.browser;

import android.content.Context;
import android.util.AttributeSet;
import com.android.internal.preference.YesNoPreference;

class BrowserYesNoPreference extends YesNoPreference {
    public BrowserYesNoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (!positiveResult) {
            return;
        }
        setEnabled(false);
        BrowserSettings settings = BrowserSettings.getInstance();
        if ("privacy_clear_cache".equals(getKey())) {
            settings.clearCache();
            settings.clearDatabases();
            return;
        }
        if ("privacy_clear_cookies".equals(getKey())) {
            settings.clearCookies();
            return;
        }
        if ("privacy_clear_history".equals(getKey())) {
            settings.clearHistory();
            return;
        }
        if ("privacy_clear_form_data".equals(getKey())) {
            settings.clearFormData();
            return;
        }
        if ("privacy_clear_passwords".equals(getKey())) {
            settings.clearPasswords();
            return;
        }
        if ("reset_default_preferences".equals(getKey())) {
            settings.resetDefaultPreferences();
            setEnabled(true);
        } else {
            if (!"privacy_clear_geolocation_access".equals(getKey())) {
                return;
            }
            settings.clearLocationAccess();
        }
    }
}
