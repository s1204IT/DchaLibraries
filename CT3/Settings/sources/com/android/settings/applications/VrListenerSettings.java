package com.android.settings.applications;

import com.android.settings.R;
import com.android.settings.utils.ManagedServiceSettings;

public class VrListenerSettings extends ManagedServiceSettings {
    private static final String TAG = VrListenerSettings.class.getSimpleName();
    private static final ManagedServiceSettings.Config CONFIG = getVrListenerConfig();

    private static final ManagedServiceSettings.Config getVrListenerConfig() {
        ManagedServiceSettings.Config c = new ManagedServiceSettings.Config();
        c.tag = TAG;
        c.setting = "enabled_vr_listeners";
        c.intentAction = "android.service.vr.VrListenerService";
        c.permission = "android.permission.BIND_VR_LISTENER_SERVICE";
        c.noun = "vr listener";
        c.warningDialogTitle = R.string.vr_listener_security_warning_title;
        c.warningDialogSummary = R.string.vr_listener_security_warning_summary;
        c.emptyText = R.string.no_vr_listeners;
        return c;
    }

    @Override
    protected ManagedServiceSettings.Config getConfig() {
        return CONFIG;
    }

    @Override
    protected int getMetricsCategory() {
        return 334;
    }
}
