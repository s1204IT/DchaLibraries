package com.android.settings.accessibility;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.provider.Settings;
import android.text.TextUtils;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class AccessibilityUtils {
    static Set<ComponentName> getEnabledServicesFromSettings(Context context) {
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");
        if (enabledServicesSetting == null) {
            return Collections.emptySet();
        }
        Set<ComponentName> enabledServices = new HashSet<>();
        TextUtils.SimpleStringSplitter colonSplitter = AccessibilitySettings.sStringColonSplitter;
        colonSplitter.setString(enabledServicesSetting);
        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null) {
                enabledServices.add(enabledService);
            }
        }
        return enabledServices;
    }

    static CharSequence getTextForLocale(Context context, Locale locale, int resId) {
        Resources res = context.getResources();
        Configuration config = res.getConfiguration();
        Locale prevLocale = config.locale;
        try {
            config.locale = locale;
            res.updateConfiguration(config, null);
            return res.getText(resId);
        } finally {
            config.locale = prevLocale;
            res.updateConfiguration(config, null);
        }
    }
}
