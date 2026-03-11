package com.android.settings;

import android.content.Intent;
import android.os.SystemProperties;

public class SetupWizardUtils {
    static final String SYSTEM_PROP_SETUPWIZARD_THEME = "setupwizard.theme";

    public static int getTheme(Intent intent) {
        String stringExtra = intent.getStringExtra("theme");
        if (stringExtra == null) {
            stringExtra = SystemProperties.get(SYSTEM_PROP_SETUPWIZARD_THEME);
        }
        if (stringExtra != null) {
            switch (stringExtra) {
            }
            return R.style.GlifTheme_Light;
        }
        return R.style.GlifTheme_Light;
    }

    public static int getTransparentTheme(Intent intent) {
        int theme = getTheme(intent);
        if (theme == R.style.GlifV3Theme) {
            return R.style.GlifV3Theme_Transparent;
        }
        if (theme == 2131951805) {
            return R.style.GlifV3Theme_Light_Transparent;
        }
        if (theme == R.style.GlifV2Theme) {
            return R.style.GlifV2Theme_Transparent;
        }
        if (theme == 2131951797) {
            return R.style.SetupWizardTheme_Light_Transparent;
        }
        if (theme == R.style.GlifTheme) {
            return R.style.SetupWizardTheme_Transparent;
        }
        return R.style.GlifV2Theme_Light_Transparent;
    }

    public static void copySetupExtras(Intent intent, Intent intent2) {
        intent2.putExtra("theme", intent.getStringExtra("theme"));
        intent2.putExtra("useImmersiveMode", intent.getBooleanExtra("useImmersiveMode", false));
    }
}
