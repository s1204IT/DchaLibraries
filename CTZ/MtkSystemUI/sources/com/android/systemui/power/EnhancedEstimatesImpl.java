package com.android.systemui.power;

public class EnhancedEstimatesImpl implements EnhancedEstimates {
    @Override
    public boolean isHybridNotificationEnabled() {
        return false;
    }

    @Override
    public Estimate getEstimate() {
        return null;
    }

    @Override
    public long getLowWarningThreshold() {
        return 0L;
    }

    @Override
    public long getSevereWarningThreshold() {
        return 0L;
    }
}
