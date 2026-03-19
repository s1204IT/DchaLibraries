package com.android.server.notification;

import android.content.ComponentName;
import android.net.Uri;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.notification.ConditionProviders;
import java.io.PrintWriter;
import java.util.Objects;

public class ZenModeConditions implements ConditionProviders.Callback {
    private static final boolean DEBUG = ZenModeHelper.DEBUG;
    private static final String TAG = "ZenModeHelper";
    private final ConditionProviders mConditionProviders;
    private final ZenModeHelper mHelper;
    private final ArrayMap<Uri, ComponentName> mSubscriptions = new ArrayMap<>();
    private boolean mFirstEvaluation = true;

    public ZenModeConditions(ZenModeHelper helper, ConditionProviders conditionProviders) {
        this.mHelper = helper;
        this.mConditionProviders = conditionProviders;
        if (this.mConditionProviders.isSystemProviderEnabled("countdown")) {
            this.mConditionProviders.addSystemProvider(new CountdownConditionProvider());
        }
        if (this.mConditionProviders.isSystemProviderEnabled("schedule")) {
            this.mConditionProviders.addSystemProvider(new ScheduleConditionProvider());
        }
        if (this.mConditionProviders.isSystemProviderEnabled("event")) {
            this.mConditionProviders.addSystemProvider(new EventConditionProvider());
        }
        this.mConditionProviders.setCallback(this);
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mSubscriptions=");
        pw.println(this.mSubscriptions);
    }

    public void evaluateConfig(ZenModeConfig config, boolean processSubscriptions) {
        if (config == null) {
            return;
        }
        if (config.manualRule != null && config.manualRule.condition != null && !config.manualRule.isTrueOrUnknown()) {
            if (DEBUG) {
                Log.d(TAG, "evaluateConfig: clearing manual rule");
            }
            config.manualRule = null;
        }
        ArraySet<Uri> current = new ArraySet<>();
        evaluateRule(config.manualRule, current, processSubscriptions);
        for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
            evaluateRule(automaticRule, current, processSubscriptions);
            updateSnoozing(automaticRule);
        }
        int N = this.mSubscriptions.size();
        for (int i = N - 1; i >= 0; i--) {
            Uri id = this.mSubscriptions.keyAt(i);
            ComponentName component = this.mSubscriptions.valueAt(i);
            if (processSubscriptions && !current.contains(id)) {
                this.mConditionProviders.unsubscribeIfNecessary(component, id);
                this.mSubscriptions.removeAt(i);
            }
        }
        this.mFirstEvaluation = false;
    }

    @Override
    public void onBootComplete() {
    }

    @Override
    public void onUserSwitched() {
    }

    @Override
    public void onServiceAdded(ComponentName component) {
        if (DEBUG) {
            Log.d(TAG, "onServiceAdded " + component);
        }
        this.mHelper.setConfigAsync(this.mHelper.getConfig(), "zmc.onServiceAdded");
    }

    @Override
    public void onConditionChanged(Uri id, Condition condition) {
        if (DEBUG) {
            Log.d(TAG, "onConditionChanged " + id + " " + condition);
        }
        ZenModeConfig config = this.mHelper.getConfig();
        if (config == null) {
            return;
        }
        boolean updated = updateCondition(id, condition, config.manualRule);
        for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
            updated = updated | updateCondition(id, condition, automaticRule) | updateSnoozing(automaticRule);
        }
        if (!updated) {
            return;
        }
        this.mHelper.setConfigAsync(config, "conditionChanged");
    }

    private void evaluateRule(ZenModeConfig.ZenRule rule, ArraySet<Uri> current, boolean processSubscriptions) {
        if (rule == null || rule.conditionId == null) {
            return;
        }
        Uri id = rule.conditionId;
        boolean isSystemCondition = false;
        for (SystemConditionProviderService sp : this.mConditionProviders.getSystemProviders()) {
            if (sp.isValidConditionId(id)) {
                this.mConditionProviders.ensureRecordExists(sp.getComponent(), id, sp.asInterface());
                rule.component = sp.getComponent();
                isSystemCondition = true;
            }
        }
        if (!isSystemCondition) {
            IConditionProvider cp = this.mConditionProviders.findConditionProvider(rule.component);
            if (DEBUG) {
                Log.d(TAG, "Ensure external rule exists: " + (cp != null) + " for " + id);
            }
            if (cp != null) {
                this.mConditionProviders.ensureRecordExists(rule.component, id, cp);
            }
        }
        if (rule.component == null) {
            Log.w(TAG, "No component found for automatic rule: " + rule.conditionId);
            rule.enabled = false;
            return;
        }
        if (current != null) {
            current.add(id);
        }
        if (processSubscriptions) {
            if (this.mConditionProviders.subscribeIfNecessary(rule.component, rule.conditionId)) {
                this.mSubscriptions.put(rule.conditionId, rule.component);
            } else if (DEBUG) {
                Log.d(TAG, "zmc failed to subscribe");
            }
        }
        if (rule.condition != null) {
            return;
        }
        rule.condition = this.mConditionProviders.findCondition(rule.component, rule.conditionId);
        if (rule.condition == null || !DEBUG) {
            return;
        }
        Log.d(TAG, "Found existing condition for: " + rule.conditionId);
    }

    private boolean isAutomaticActive(ComponentName component) {
        ZenModeConfig config;
        if (component == null || (config = this.mHelper.getConfig()) == null) {
            return false;
        }
        for (ZenModeConfig.ZenRule rule : config.automaticRules.values()) {
            if (component.equals(rule.component) && rule.isAutomaticActive()) {
                return true;
            }
        }
        return false;
    }

    private boolean updateSnoozing(ZenModeConfig.ZenRule rule) {
        if (rule == null || !rule.snoozing || (!this.mFirstEvaluation && rule.isTrueOrUnknown())) {
            return false;
        }
        rule.snoozing = false;
        if (DEBUG) {
            Log.d(TAG, "Snoozing reset for " + rule.conditionId);
            return true;
        }
        return true;
    }

    private boolean updateCondition(Uri id, Condition condition, ZenModeConfig.ZenRule rule) {
        if (id == null || rule == null || rule.conditionId == null || !rule.conditionId.equals(id) || Objects.equals(condition, rule.condition)) {
            return false;
        }
        rule.condition = condition;
        return true;
    }
}
