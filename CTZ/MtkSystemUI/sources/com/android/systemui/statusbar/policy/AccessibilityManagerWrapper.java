package com.android.systemui.statusbar.policy;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.Handler;
import android.view.accessibility.AccessibilityManager;
import java.util.List;

public class AccessibilityManagerWrapper implements CallbackController<AccessibilityManager.AccessibilityServicesStateChangeListener> {
    private final AccessibilityManager mAccessibilityManager;

    public AccessibilityManagerWrapper(Context context) {
        this.mAccessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
    }

    @Override
    public void addCallback(AccessibilityManager.AccessibilityServicesStateChangeListener accessibilityServicesStateChangeListener) {
        this.mAccessibilityManager.addAccessibilityServicesStateChangeListener(accessibilityServicesStateChangeListener, (Handler) null);
    }

    @Override
    public void removeCallback(AccessibilityManager.AccessibilityServicesStateChangeListener accessibilityServicesStateChangeListener) {
        this.mAccessibilityManager.removeAccessibilityServicesStateChangeListener(accessibilityServicesStateChangeListener);
    }

    public List<AccessibilityServiceInfo> getEnabledAccessibilityServiceList(int i) {
        return this.mAccessibilityManager.getEnabledAccessibilityServiceList(i);
    }
}
