package com.android.systemui.power;

public interface EnhancedEstimates {
    Estimate getEstimate();

    long getLowWarningThreshold();

    long getSevereWarningThreshold();

    boolean isHybridNotificationEnabled();
}
