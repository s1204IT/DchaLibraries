package com.android.systemui.statusbar.policy;

import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.ZenModeConfig;

public interface ZenModeController {
    void addCallback(Callback callback);

    ZenModeConfig getConfig();

    int getCurrentUser();

    ZenModeConfig.ZenRule getManualRule();

    long getNextAlarm();

    int getZen();

    boolean isCountdownConditionSupported();

    boolean isVolumeRestricted();

    void removeCallback(Callback callback);

    void setUserId(int i);

    void setZen(int i, Uri uri, String str);

    public static class Callback {
        public void onZenChanged(int zen) {
        }

        public void onConditionsChanged(Condition[] conditions) {
        }

        public void onNextAlarmChanged() {
        }

        public void onZenAvailableChanged(boolean available) {
        }

        public void onEffectsSupressorChanged() {
        }

        public void onManualRuleChanged(ZenModeConfig.ZenRule rule) {
        }

        public void onConfigChanged(ZenModeConfig config) {
        }
    }
}
