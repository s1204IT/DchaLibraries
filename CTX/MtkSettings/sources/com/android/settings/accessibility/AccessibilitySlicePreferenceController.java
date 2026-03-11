package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.accessibility.AccessibilityUtils;

public class AccessibilitySlicePreferenceController extends TogglePreferenceController {
    private final int OFF;
    private final int ON;
    private final ComponentName mComponentName;

    public AccessibilitySlicePreferenceController(Context context, String str) {
        super(context, str);
        this.ON = 1;
        this.OFF = 0;
        this.mComponentName = ComponentName.unflattenFromString(getPreferenceKey());
        if (this.mComponentName == null) {
            throw new IllegalArgumentException("Illegal Component Name from: " + str);
        }
    }

    @Override
    public CharSequence getSummary() {
        AccessibilityServiceInfo accessibilityServiceInfo = getAccessibilityServiceInfo();
        return accessibilityServiceInfo == null ? "" : AccessibilitySettings.getServiceSummary(this.mContext, accessibilityServiceInfo, isChecked());
    }

    @Override
    public boolean isChecked() {
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), "accessibility_enabled", 0) == 1) {
            return AccessibilityUtils.getEnabledServicesFromSettings(this.mContext).contains(this.mComponentName);
        }
        return false;
    }

    @Override
    public boolean setChecked(boolean z) {
        if (getAccessibilityServiceInfo() == null) {
            return false;
        }
        AccessibilityUtils.setAccessibilityServiceState(this.mContext, this.mComponentName, z);
        return z == isChecked();
    }

    @Override
    public int getAvailabilityStatus() {
        return getAccessibilityServiceInfo() == null ? 2 : 0;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    private AccessibilityServiceInfo getAccessibilityServiceInfo() {
        for (AccessibilityServiceInfo accessibilityServiceInfo : ((AccessibilityManager) this.mContext.getSystemService(AccessibilityManager.class)).getInstalledAccessibilityServiceList()) {
            if (this.mComponentName.equals(accessibilityServiceInfo.getComponentName())) {
                return accessibilityServiceInfo;
            }
        }
        return null;
    }
}
