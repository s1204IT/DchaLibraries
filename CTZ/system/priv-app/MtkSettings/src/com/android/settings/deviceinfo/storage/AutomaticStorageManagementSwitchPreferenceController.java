package com.android.settings.deviceinfo.storage;

import android.app.ActivityManager;
import android.app.FragmentManager;
import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.deletionhelper.ActivationWarningFragment;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class AutomaticStorageManagementSwitchPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, SwitchWidgetController.OnSwitchChangeListener, LifecycleObserver, OnResume {
    static final String STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY = "ro.storage_manager.enabled";
    private final FragmentManager mFragmentManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private MasterSwitchPreference mSwitch;
    private MasterSwitchController mSwitchController;

    public AutomaticStorageManagementSwitchPreferenceController(Context context, MetricsFeatureProvider metricsFeatureProvider, FragmentManager fragmentManager) {
        super(context);
        this.mMetricsFeatureProvider = metricsFeatureProvider;
        this.mFragmentManager = fragmentManager;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mSwitch = (MasterSwitchPreference) preferenceScreen.findPreference("toggle_asm");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !ActivityManager.isLowRamDeviceStatic();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "toggle_asm";
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        if (!isAvailable()) {
            return;
        }
        this.mSwitch.setChecked(Utils.isStorageManagerEnabled(this.mContext));
        if (this.mSwitch != null) {
            this.mSwitchController = new MasterSwitchController(this.mSwitch);
            this.mSwitchController.setListener(this);
            this.mSwitchController.startListening();
        }
    }

    @Override // com.android.settings.widget.SwitchWidgetController.OnSwitchChangeListener
    public boolean onSwitchToggled(boolean z) {
        this.mMetricsFeatureProvider.action(this.mContext, 489, z);
        Settings.Secure.putInt(this.mContext.getContentResolver(), "automatic_storage_manager_enabled", z ? 1 : 0);
        boolean z2 = false;
        boolean z3 = SystemProperties.getBoolean(STORAGE_MANAGER_ENABLED_BY_DEFAULT_PROPERTY, false);
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "automatic_storage_manager_turned_off_by_policy", 0) != 0) {
            z2 = true;
        }
        if (z && (!z3 || z2)) {
            ActivationWarningFragment.newInstance().show(this.mFragmentManager, "ActivationWarningFragment");
        }
        return true;
    }
}
