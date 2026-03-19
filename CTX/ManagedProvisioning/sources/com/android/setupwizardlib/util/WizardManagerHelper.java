package com.android.setupwizardlib.util;

import com.android.setupwizardlib.R;

public class WizardManagerHelper {
    static final String EXTRA_ACTION_ID = "actionId";
    static final String EXTRA_IS_DEFERRED_SETUP = "deferredSetup";
    static final String EXTRA_IS_FIRST_RUN = "firstRun";
    static final String EXTRA_IS_PRE_DEFERRED_SETUP = "preDeferredSetup";
    static final String EXTRA_SCRIPT_URI = "scriptUri";
    static final String EXTRA_WIZARD_BUNDLE = "wizardBundle";

    public static int getThemeRes(String str, int i) {
        if (str != null) {
            switch (str) {
                case "glif_v3_light":
                    return R.style.SuwThemeGlifV3_Light;
                case "glif_v3":
                    return R.style.SuwThemeGlifV3;
                case "glif_v2_light":
                    return R.style.SuwThemeGlifV2_Light;
                case "glif_v2":
                    return R.style.SuwThemeGlifV2;
                case "glif_light":
                    return R.style.SuwThemeGlif_Light;
                case "glif":
                    return R.style.SuwThemeGlif;
                case "material_light":
                    return R.style.SuwThemeMaterial_Light;
                case "material":
                    return R.style.SuwThemeMaterial;
            }
        }
        return i;
    }
}
