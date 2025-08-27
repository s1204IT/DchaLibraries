package com.android.settings.applications;

import android.content.ComponentName;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.ManagedServiceSettings;

/* loaded from: classes.dex */
public class VrListenerSettings extends ManagedServiceSettings {
    private static final String TAG = VrListenerSettings.class.getSimpleName();
    private static final ManagedServiceSettings.Config CONFIG = new ManagedServiceSettings.Config.Builder().setTag(TAG).setSetting("enabled_vr_listeners").setIntentAction("android.service.vr.VrListenerService").setPermission("android.permission.BIND_VR_LISTENER_SERVICE").setNoun("vr listener").setWarningDialogTitle(R.string.vr_listener_security_warning_title).setWarningDialogSummary(R.string.vr_listener_security_warning_summary).setEmptyText(R.string.no_vr_listeners).build();

    @Override // com.android.settings.utils.ManagedServiceSettings
    protected ManagedServiceSettings.Config getConfig() {
        return CONFIG;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 334;
    }

    @Override // com.android.settings.utils.ManagedServiceSettings
    protected boolean setEnabled(ComponentName componentName, String str, boolean z) {
        logSpecialPermissionChange(z, componentName.getPackageName());
        return super.setEnabled(componentName, str, z);
    }

    @Override // com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.vr_listeners_settings;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean z, String str) {
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(), z ? 772 : 773, str, new Pair[0]);
    }
}
