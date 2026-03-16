package com.android.settings.notification;

import android.content.Context;
import android.content.pm.PackageManager;
import com.android.settings.R;
import com.android.settings.notification.ManagedServiceSettings;

public class ConditionProviderSettings extends ManagedServiceSettings {
    private static final String TAG = ConditionProviderSettings.class.getSimpleName();
    private static final ManagedServiceSettings.Config CONFIG = getConditionProviderConfig();

    private static ManagedServiceSettings.Config getConditionProviderConfig() {
        ManagedServiceSettings.Config c = new ManagedServiceSettings.Config();
        c.tag = TAG;
        c.setting = "enabled_condition_providers";
        c.intentAction = "android.service.notification.ConditionProviderService";
        c.permission = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
        c.noun = "condition provider";
        c.warningDialogTitle = R.string.condition_provider_security_warning_title;
        c.warningDialogSummary = R.string.condition_provider_security_warning_summary;
        c.emptyText = R.string.no_condition_providers;
        return c;
    }

    @Override
    protected ManagedServiceSettings.Config getConfig() {
        return CONFIG;
    }

    public static int getProviderCount(PackageManager pm) {
        return getServicesCount(CONFIG, pm);
    }

    public static int getEnabledProviderCount(Context context) {
        return getEnabledServicesCount(CONFIG, context);
    }
}
