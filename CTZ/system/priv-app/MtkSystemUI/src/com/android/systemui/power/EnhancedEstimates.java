package com.android.systemui.power;

/* loaded from: classes.dex */
public interface EnhancedEstimates {
    Estimate getEstimate();

    long getLowWarningThreshold();

    long getSevereWarningThreshold();

    boolean isHybridNotificationEnabled();
}
