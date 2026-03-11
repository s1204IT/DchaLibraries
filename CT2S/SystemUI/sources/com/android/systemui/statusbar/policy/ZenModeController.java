package com.android.systemui.statusbar.policy;

import android.content.ComponentName;
import android.service.notification.Condition;

public interface ZenModeController {
    void addCallback(Callback callback);

    ComponentName getEffectsSuppressor();

    Condition getExitCondition();

    int getZen();

    boolean isZenAvailable();

    void requestConditions(boolean z);

    void setExitCondition(Condition condition);

    void setUserId(int i);

    void setZen(int i);

    public static class Callback {
        public void onZenChanged(int zen) {
        }

        public void onExitConditionChanged(Condition exitCondition) {
        }

        public void onConditionsChanged(Condition[] conditions) {
        }

        public void onNextAlarmChanged() {
        }

        public void onZenAvailableChanged(boolean available) {
        }

        public void onEffectsSupressorChanged() {
        }
    }
}
