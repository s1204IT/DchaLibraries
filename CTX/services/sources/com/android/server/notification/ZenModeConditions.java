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
import java.util.Iterator;
import java.util.Objects;

public class ZenModeConditions implements ConditionProviders.Callback {
    private static final boolean DEBUG = ZenModeHelper.DEBUG;
    private static final String TAG = "ZenModeHelper";
    private final ConditionProviders mConditionProviders;
    private final ZenModeHelper mHelper;
    private final ArrayMap<Uri, ComponentName> mSubscriptions = new ArrayMap<>();
    private boolean mFirstEvaluation = true;

    public ZenModeConditions(ZenModeHelper zenModeHelper, ConditionProviders conditionProviders) {
        this.mHelper = zenModeHelper;
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

    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.print("mSubscriptions=");
        printWriter.println(this.mSubscriptions);
    }

    public void evaluateConfig(ZenModeConfig zenModeConfig, boolean z) {
        if (zenModeConfig == null) {
            return;
        }
        if (zenModeConfig.manualRule != null && zenModeConfig.manualRule.condition != null && !zenModeConfig.manualRule.isTrueOrUnknown()) {
            if (DEBUG) {
                Log.d(TAG, "evaluateConfig: clearing manual rule");
            }
            zenModeConfig.manualRule = null;
        }
        ArraySet<Uri> arraySet = new ArraySet<>();
        evaluateRule(zenModeConfig.manualRule, arraySet, z);
        for (ZenModeConfig.ZenRule zenRule : zenModeConfig.automaticRules.values()) {
            evaluateRule(zenRule, arraySet, z);
            updateSnoozing(zenRule);
        }
        synchronized (this.mSubscriptions) {
            for (int size = this.mSubscriptions.size() - 1; size >= 0; size--) {
                Uri uriKeyAt = this.mSubscriptions.keyAt(size);
                ComponentName componentNameValueAt = this.mSubscriptions.valueAt(size);
                if (z && !arraySet.contains(uriKeyAt)) {
                    this.mConditionProviders.unsubscribeIfNecessary(componentNameValueAt, uriKeyAt);
                    this.mSubscriptions.removeAt(size);
                }
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
    public void onServiceAdded(ComponentName componentName) {
        if (DEBUG) {
            Log.d(TAG, "onServiceAdded " + componentName);
        }
        this.mHelper.setConfig(this.mHelper.getConfig(), "zmc.onServiceAdded");
    }

    @Override
    public void onConditionChanged(Uri uri, Condition condition) {
        if (DEBUG) {
            Log.d(TAG, "onConditionChanged " + uri + " " + condition);
        }
        ZenModeConfig config = this.mHelper.getConfig();
        if (config == null) {
            return;
        }
        boolean zUpdateCondition = updateCondition(uri, condition, config.manualRule);
        for (ZenModeConfig.ZenRule zenRule : config.automaticRules.values()) {
            zUpdateCondition = zUpdateCondition | updateCondition(uri, condition, zenRule) | updateSnoozing(zenRule);
        }
        if (zUpdateCondition) {
            this.mHelper.setConfig(config, "conditionChanged");
        }
    }

    private void evaluateRule(ZenModeConfig.ZenRule zenRule, ArraySet<Uri> arraySet, boolean z) {
        if (zenRule == null || zenRule.conditionId == null) {
            return;
        }
        Uri uri = zenRule.conditionId;
        Iterator<SystemConditionProviderService> it = this.mConditionProviders.getSystemProviders().iterator();
        boolean z2 = false;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            SystemConditionProviderService next = it.next();
            if (next.isValidConditionId(uri)) {
                this.mConditionProviders.ensureRecordExists(next.getComponent(), uri, next.asInterface());
                zenRule.component = next.getComponent();
                z2 = true;
            }
        }
        if (!z2) {
            IConditionProvider iConditionProviderFindConditionProvider = this.mConditionProviders.findConditionProvider(zenRule.component);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Ensure external rule exists: ");
                sb.append(iConditionProviderFindConditionProvider != null);
                sb.append(" for ");
                sb.append(uri);
                Log.d(TAG, sb.toString());
            }
            if (iConditionProviderFindConditionProvider != null) {
                this.mConditionProviders.ensureRecordExists(zenRule.component, uri, iConditionProviderFindConditionProvider);
            }
        }
        if (zenRule.component == null) {
            Log.w(TAG, "No component found for automatic rule: " + zenRule.conditionId);
            zenRule.enabled = false;
            return;
        }
        if (arraySet != null) {
            arraySet.add(uri);
        }
        if (z) {
            if (this.mConditionProviders.subscribeIfNecessary(zenRule.component, zenRule.conditionId)) {
                synchronized (this.mSubscriptions) {
                    this.mSubscriptions.put(zenRule.conditionId, zenRule.component);
                }
            } else {
                zenRule.condition = null;
                if (DEBUG) {
                    Log.d(TAG, "zmc failed to subscribe");
                }
            }
        }
        if (zenRule.condition == null) {
            zenRule.condition = this.mConditionProviders.findCondition(zenRule.component, zenRule.conditionId);
            if (zenRule.condition == null || !DEBUG) {
                return;
            }
            Log.d(TAG, "Found existing condition for: " + zenRule.conditionId);
        }
    }

    private boolean isAutomaticActive(ComponentName componentName) {
        ZenModeConfig config;
        if (componentName == null || (config = this.mHelper.getConfig()) == null) {
            return false;
        }
        for (ZenModeConfig.ZenRule zenRule : config.automaticRules.values()) {
            if (componentName.equals(zenRule.component) && zenRule.isAutomaticActive()) {
                return true;
            }
        }
        return false;
    }

    private boolean updateSnoozing(ZenModeConfig.ZenRule zenRule) {
        if (zenRule == null || !zenRule.snoozing || (!this.mFirstEvaluation && zenRule.isTrueOrUnknown())) {
            return false;
        }
        zenRule.snoozing = false;
        if (DEBUG) {
            Log.d(TAG, "Snoozing reset for " + zenRule.conditionId);
            return true;
        }
        return true;
    }

    private boolean updateCondition(Uri uri, Condition condition, ZenModeConfig.ZenRule zenRule) {
        if (uri == null || zenRule == null || zenRule.conditionId == null || !zenRule.conditionId.equals(uri) || Objects.equals(condition, zenRule.condition)) {
            return false;
        }
        zenRule.condition = condition;
        return true;
    }
}
