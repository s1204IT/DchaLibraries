package com.android.setupwizardlib.util;

import android.content.Intent;
import java.util.Arrays;

/* loaded from: classes.dex */
public class WizardManagerHelper {
    static final String EXTRA_ACTION_ID = "actionId";
    static final String EXTRA_IS_DEFERRED_SETUP = "deferredSetup";
    static final String EXTRA_IS_FIRST_RUN = "firstRun";
    static final String EXTRA_IS_PRE_DEFERRED_SETUP = "preDeferredSetup";
    static final String EXTRA_SCRIPT_URI = "scriptUri";
    static final String EXTRA_WIZARD_BUNDLE = "wizardBundle";

    public static void copyWizardManagerExtras(Intent intent, Intent intent2) {
        intent2.putExtra(EXTRA_WIZARD_BUNDLE, intent.getBundleExtra(EXTRA_WIZARD_BUNDLE));
        for (String str : Arrays.asList(EXTRA_IS_FIRST_RUN, EXTRA_IS_DEFERRED_SETUP, EXTRA_IS_PRE_DEFERRED_SETUP)) {
            intent2.putExtra(str, intent.getBooleanExtra(str, false));
        }
        for (String str2 : Arrays.asList("theme", EXTRA_SCRIPT_URI, EXTRA_ACTION_ID)) {
            intent2.putExtra(str2, intent.getStringExtra(str2));
        }
    }

    public static boolean isSetupWizardIntent(Intent intent) {
        return intent.getBooleanExtra(EXTRA_IS_FIRST_RUN, false);
    }
}
