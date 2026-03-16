package com.android.server.notification;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.IConditionProvider;
import android.service.notification.ZenModeConfig;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.ZenModeHelper;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class ConditionProviders extends ManagedServices {
    private static final Condition[] NO_CONDITIONS = new Condition[0];
    private final CountdownConditionProvider mCountdown;
    private final DowntimeConditionProvider mDowntime;
    private Condition mExitCondition;
    private ComponentName mExitConditionComponent;
    private final ArrayMap<IBinder, IConditionListener> mListeners;
    private final NextAlarmConditionProvider mNextAlarm;
    private final NextAlarmTracker mNextAlarmTracker;
    private final ArrayList<ConditionRecord> mRecords;
    private final ArraySet<String> mSystemConditionProviders;
    private final ZenModeHelper mZenModeHelper;

    public ConditionProviders(Context context, Handler handler, ManagedServices.UserProfiles userProfiles, ZenModeHelper zenModeHelper) {
        super(context, handler, new Object(), userProfiles);
        this.mListeners = new ArrayMap<>();
        this.mRecords = new ArrayList<>();
        this.mZenModeHelper = zenModeHelper;
        this.mZenModeHelper.addCallback(new ZenModeHelperCallback());
        this.mSystemConditionProviders = safeSet(PropConfig.getStringArray(this.mContext, "system.condition.providers", R.array.config_defaultImperceptibleKillingExemptionPkgs));
        boolean zContains = this.mSystemConditionProviders.contains("countdown");
        boolean zContains2 = this.mSystemConditionProviders.contains("downtime");
        boolean zContains3 = this.mSystemConditionProviders.contains("next_alarm");
        this.mNextAlarmTracker = (zContains2 || zContains3) ? new NextAlarmTracker(this.mContext) : null;
        this.mCountdown = zContains ? new CountdownConditionProvider() : null;
        this.mDowntime = zContains2 ? new DowntimeConditionProvider(this, this.mNextAlarmTracker, this.mZenModeHelper) : null;
        this.mNextAlarm = zContains3 ? new NextAlarmConditionProvider(this.mNextAlarmTracker) : null;
        loadZenConfig();
    }

    public boolean isSystemConditionProviderEnabled(String path) {
        return this.mSystemConditionProviders.contains(path);
    }

    @Override
    protected ManagedServices.Config getConfig() {
        ManagedServices.Config c = new ManagedServices.Config();
        c.caption = "condition provider";
        c.serviceInterface = "android.service.notification.ConditionProviderService";
        c.secureSettingName = "enabled_condition_providers";
        c.bindPermission = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
        c.settingsAction = "android.settings.ACTION_CONDITION_PROVIDER_SETTINGS";
        c.clientLabel = R.string.keyguard_accessibility_widget_deleted;
        return c;
    }

    @Override
    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        int i;
        super.dump(pw, filter);
        synchronized (this.mMutex) {
            if (filter == null) {
                pw.print("    mListeners(");
                pw.print(this.mListeners.size());
                pw.println("):");
                for (int i2 = 0; i2 < this.mListeners.size(); i2++) {
                    pw.print("      ");
                    pw.println(this.mListeners.keyAt(i2));
                }
                pw.print("    mRecords(");
                pw.print(this.mRecords.size());
                pw.println("):");
                for (i = 0; i < this.mRecords.size(); i++) {
                    ConditionRecord r = this.mRecords.get(i);
                    if (filter == null || filter.matches(r.component)) {
                        pw.print("      ");
                        pw.println(r);
                        String countdownDesc = CountdownConditionProvider.tryParseDescription(r.id);
                        if (countdownDesc != null) {
                            pw.print("        (");
                            pw.print(countdownDesc);
                            pw.println(")");
                        }
                    }
                }
            } else {
                pw.print("    mRecords(");
                pw.print(this.mRecords.size());
                pw.println("):");
                while (i < this.mRecords.size()) {
                }
            }
        }
        pw.print("    mSystemConditionProviders: ");
        pw.println(this.mSystemConditionProviders);
        if (this.mCountdown != null) {
            this.mCountdown.dump(pw, filter);
        }
        if (this.mDowntime != null) {
            this.mDowntime.dump(pw, filter);
        }
        if (this.mNextAlarm != null) {
            this.mNextAlarm.dump(pw, filter);
        }
        if (this.mNextAlarmTracker != null) {
            this.mNextAlarmTracker.dump(pw, filter);
        }
    }

    @Override
    protected IInterface asInterface(IBinder binder) {
        return IConditionProvider.Stub.asInterface(binder);
    }

    @Override
    public void onBootPhaseAppsCanStart() {
        super.onBootPhaseAppsCanStart();
        if (this.mNextAlarmTracker != null) {
            this.mNextAlarmTracker.init();
        }
        if (this.mCountdown != null) {
            this.mCountdown.attachBase(this.mContext);
            registerService(this.mCountdown.asInterface(), CountdownConditionProvider.COMPONENT, 0);
        }
        if (this.mDowntime != null) {
            this.mDowntime.attachBase(this.mContext);
            registerService(this.mDowntime.asInterface(), DowntimeConditionProvider.COMPONENT, 0);
        }
        if (this.mNextAlarm != null) {
            this.mNextAlarm.attachBase(this.mContext);
            registerService(this.mNextAlarm.asInterface(), NextAlarmConditionProvider.COMPONENT, 0);
        }
    }

    @Override
    public void onUserSwitched() {
        super.onUserSwitched();
        if (this.mNextAlarmTracker != null) {
            this.mNextAlarmTracker.onUserSwitched();
        }
    }

    @Override
    protected void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
        IConditionProvider provider = provider(info);
        try {
            provider.onConnected();
        } catch (RemoteException e) {
        }
        synchronized (this.mMutex) {
            if (info.component.equals(this.mExitConditionComponent)) {
                ConditionRecord manualRecord = getRecordLocked(this.mExitCondition.id, this.mExitConditionComponent);
                manualRecord.isManual = true;
            }
            int N = this.mRecords.size();
            for (int i = 0; i < N; i++) {
                ConditionRecord r = this.mRecords.get(i);
                if (r.component.equals(info.component)) {
                    r.info = info;
                    if (r.isAutomatic || r.isManual) {
                        subscribeLocked(r);
                    }
                }
            }
        }
    }

    @Override
    protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
        if (removed != null) {
            for (int i = this.mRecords.size() - 1; i >= 0; i--) {
                ConditionRecord r = this.mRecords.get(i);
                if (r.component.equals(removed.component)) {
                    if (r.isManual) {
                        onManualConditionClearing();
                        this.mZenModeHelper.setZenMode(0, "manualServiceRemoved");
                    }
                    if (r.isAutomatic) {
                        this.mZenModeHelper.setZenMode(0, "automaticServiceRemoved");
                    }
                    this.mRecords.remove(i);
                }
            }
        }
    }

    public ManagedServices.ManagedServiceInfo checkServiceToken(IConditionProvider provider) {
        ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked;
        synchronized (this.mMutex) {
            managedServiceInfoCheckServiceTokenLocked = checkServiceTokenLocked(provider);
        }
        return managedServiceInfoCheckServiceTokenLocked;
    }

    public void requestZenModeConditions(IConditionListener callback, int relevance) {
        synchronized (this.mMutex) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "requestZenModeConditions callback=" + callback + " relevance=" + Condition.relevanceToString(relevance));
            }
            if (callback != null) {
                int relevance2 = relevance & 3;
                if (relevance2 != 0) {
                    this.mListeners.put(callback.asBinder(), callback);
                    requestConditionsLocked(relevance2);
                } else {
                    this.mListeners.remove(callback.asBinder());
                    if (this.mListeners.isEmpty()) {
                        requestConditionsLocked(0);
                    }
                }
            }
        }
    }

    private Condition[] validateConditions(String pkg, Condition[] conditions) {
        if (conditions == null || conditions.length == 0) {
            return null;
        }
        int N = conditions.length;
        ArrayMap<Uri, Condition> valid = new ArrayMap<>(N);
        for (int i = 0; i < N; i++) {
            Uri id = conditions[i].id;
            if (!Condition.isValidId(id, pkg)) {
                Slog.w(this.TAG, "Ignoring condition from " + pkg + " for invalid id: " + id);
            } else if (valid.containsKey(id)) {
                Slog.w(this.TAG, "Ignoring condition from " + pkg + " for duplicate id: " + id);
            } else {
                valid.put(id, conditions[i]);
            }
        }
        if (valid.size() == 0) {
            return null;
        }
        if (valid.size() != N) {
            Condition[] rt = new Condition[valid.size()];
            for (int i2 = 0; i2 < rt.length; i2++) {
                rt[i2] = valid.valueAt(i2);
            }
            return rt;
        }
        return conditions;
    }

    private ConditionRecord getRecordLocked(Uri id, ComponentName component) {
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            ConditionRecord r = this.mRecords.get(i);
            if (r.id.equals(id) && r.component.equals(component)) {
                return r;
            }
        }
        ConditionRecord r2 = new ConditionRecord(id, component);
        this.mRecords.add(r2);
        return r2;
    }

    public void notifyConditions(String pkg, ManagedServices.ManagedServiceInfo info, Condition[] conditions) {
        synchronized (this.mMutex) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "notifyConditions pkg=" + pkg + " info=" + info + " conditions=" + (conditions == null ? null : Arrays.asList(conditions)));
            }
            Condition[] conditions2 = validateConditions(pkg, conditions);
            if (conditions2 != null && conditions2.length != 0) {
                for (IConditionListener listener : this.mListeners.values()) {
                    try {
                        listener.onConditionsReceived(conditions2);
                    } catch (RemoteException e) {
                        Slog.w(this.TAG, "Error sending conditions to listener " + listener, e);
                    }
                }
                for (Condition c : conditions2) {
                    ConditionRecord r = getRecordLocked(c.id, info.component);
                    Condition oldCondition = r.condition;
                    boolean conditionUpdate = (oldCondition == null || oldCondition.equals(c)) ? false : true;
                    r.info = info;
                    r.condition = c;
                    if (r.isManual) {
                        if (c.state == 0 || c.state == 3) {
                            boolean failed = c.state == 3;
                            if (failed) {
                                Slog.w(this.TAG, "Exit zen: manual condition failed: " + c);
                            } else if (this.DEBUG) {
                                Slog.d(this.TAG, "Exit zen: manual condition false: " + c);
                            }
                            onManualConditionClearing();
                            this.mZenModeHelper.setZenMode(0, "manualConditionExit");
                            unsubscribeLocked(r);
                            r.isManual = false;
                        } else if (c.state == 1 && conditionUpdate) {
                            if (this.DEBUG) {
                                Slog.d(this.TAG, "Current condition updated, still true. old=" + oldCondition + " new=" + c);
                            }
                            setZenModeCondition(c, "conditionUpdate");
                        }
                    }
                    if (r.isAutomatic) {
                        if (c.state == 0 || c.state == 3) {
                            boolean failed2 = c.state == 3;
                            if (failed2) {
                                Slog.w(this.TAG, "Exit zen: automatic condition failed: " + c);
                            } else if (this.DEBUG) {
                                Slog.d(this.TAG, "Exit zen: automatic condition false: " + c);
                            }
                            this.mZenModeHelper.setZenMode(0, "automaticConditionExit");
                        } else if (c.state == 1) {
                            Slog.d(this.TAG, "Enter zen: automatic condition true: " + c);
                            this.mZenModeHelper.setZenMode(1, "automaticConditionEnter");
                        }
                    }
                }
            }
        }
    }

    private void ensureRecordExists(Condition condition, IConditionProvider provider, ComponentName component) {
        ConditionRecord r = getRecordLocked(condition.id, component);
        if (r.info == null) {
            r.info = checkServiceTokenLocked(provider);
        }
    }

    public void setZenModeCondition(Condition condition, String reason) {
        int N;
        int i;
        if (this.DEBUG) {
            Slog.d(this.TAG, "setZenModeCondition " + condition + " reason=" + reason);
        }
        synchronized (this.mMutex) {
            ComponentName conditionComponent = null;
            if (condition != null) {
                if (this.mCountdown != null && ZenModeConfig.isValidCountdownConditionId(condition.id)) {
                    ensureRecordExists(condition, this.mCountdown.asInterface(), CountdownConditionProvider.COMPONENT);
                }
                if (this.mDowntime != null && ZenModeConfig.isValidDowntimeConditionId(condition.id)) {
                    ensureRecordExists(condition, this.mDowntime.asInterface(), DowntimeConditionProvider.COMPONENT);
                }
                N = this.mRecords.size();
                for (i = 0; i < N; i++) {
                    ConditionRecord r = this.mRecords.get(i);
                    boolean idEqual = condition != null && r.id.equals(condition.id);
                    if (r.isManual && !idEqual) {
                        unsubscribeLocked(r);
                        r.isManual = false;
                    } else if (idEqual && !r.isManual) {
                        subscribeLocked(r);
                        r.isManual = true;
                    }
                    if (idEqual) {
                        conditionComponent = r.component;
                    }
                }
                if (!Objects.equals(this.mExitCondition, condition)) {
                    this.mExitCondition = condition;
                    this.mExitConditionComponent = conditionComponent;
                    ZenLog.traceExitCondition(this.mExitCondition, this.mExitConditionComponent, reason);
                    saveZenConfigLocked();
                }
            } else {
                N = this.mRecords.size();
                while (i < N) {
                }
                if (!Objects.equals(this.mExitCondition, condition)) {
                }
            }
        }
    }

    private void subscribeLocked(ConditionRecord r) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "subscribeLocked " + r);
        }
        IConditionProvider provider = provider(r);
        RemoteException re = null;
        if (provider != null) {
            try {
                Slog.d(this.TAG, "Subscribing to " + r.id + " with " + provider);
                provider.onSubscribe(r.id);
            } catch (RemoteException e) {
                Slog.w(this.TAG, "Error subscribing to " + r, e);
                re = e;
            }
        }
        ZenLog.traceSubscribe(r != null ? r.id : null, provider, re);
    }

    @SafeVarargs
    private static <T> ArraySet<T> safeSet(T... items) {
        ArraySet<T> rt = new ArraySet<>();
        if (items != null && items.length != 0) {
            for (T item : items) {
                if (item != null) {
                    rt.add(item);
                }
            }
        }
        return rt;
    }

    public void setAutomaticZenModeConditions(Uri[] conditionIds) {
        setAutomaticZenModeConditions(conditionIds, true);
    }

    private void setAutomaticZenModeConditions(Uri[] conditionIds, boolean save) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "setAutomaticZenModeConditions " + (conditionIds == null ? null : Arrays.asList(conditionIds)));
        }
        synchronized (this.mMutex) {
            ArraySet<Uri> newIds = safeSet(conditionIds);
            int N = this.mRecords.size();
            boolean changed = false;
            for (int i = 0; i < N; i++) {
                ConditionRecord r = this.mRecords.get(i);
                boolean automatic = newIds.contains(r.id);
                if (!r.isAutomatic && automatic) {
                    subscribeLocked(r);
                    r.isAutomatic = true;
                    changed = true;
                } else if (r.isAutomatic && !automatic) {
                    unsubscribeLocked(r);
                    r.isAutomatic = false;
                    changed = true;
                }
            }
            if (save && changed) {
                saveZenConfigLocked();
            }
        }
    }

    public Condition[] getAutomaticZenModeConditions() {
        Condition[] conditionArr;
        synchronized (this.mMutex) {
            int N = this.mRecords.size();
            ArrayList<Condition> rt = null;
            for (int i = 0; i < N; i++) {
                ConditionRecord r = this.mRecords.get(i);
                if (r.isAutomatic && r.condition != null) {
                    if (rt == null) {
                        rt = new ArrayList<>();
                    }
                    rt.add(r.condition);
                }
            }
            conditionArr = rt == null ? NO_CONDITIONS : (Condition[]) rt.toArray(new Condition[rt.size()]);
        }
        return conditionArr;
    }

    private void unsubscribeLocked(ConditionRecord r) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "unsubscribeLocked " + r);
        }
        IConditionProvider provider = provider(r);
        RemoteException re = null;
        if (provider != null) {
            try {
                provider.onUnsubscribe(r.id);
            } catch (RemoteException e) {
                Slog.w(this.TAG, "Error unsubscribing to " + r, e);
                re = e;
            }
        }
        ZenLog.traceUnsubscribe(r != null ? r.id : null, provider, re);
    }

    private static IConditionProvider provider(ConditionRecord r) {
        if (r == null) {
            return null;
        }
        return provider(r.info);
    }

    private static IConditionProvider provider(ManagedServices.ManagedServiceInfo info) {
        if (info == null) {
            return null;
        }
        return info.service;
    }

    private void requestConditionsLocked(int flags) {
        for (ManagedServices.ManagedServiceInfo info : this.mServices) {
            IConditionProvider provider = provider(info);
            if (provider != null) {
                for (int i = this.mRecords.size() - 1; i >= 0; i--) {
                    ConditionRecord r = this.mRecords.get(i);
                    if (r.info == info && !r.isManual && !r.isAutomatic) {
                        this.mRecords.remove(i);
                    }
                }
                try {
                    provider.onRequestConditions(flags);
                } catch (RemoteException e) {
                    Slog.w(this.TAG, "Error requesting conditions from " + info.component, e);
                }
            }
        }
    }

    private void loadZenConfig() {
        ZenModeConfig config = this.mZenModeHelper.getConfig();
        if (config == null) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "loadZenConfig: no config");
                return;
            }
            return;
        }
        synchronized (this.mMutex) {
            boolean changingExit = Objects.equals(this.mExitCondition, config.exitCondition) ? false : true;
            this.mExitCondition = config.exitCondition;
            this.mExitConditionComponent = config.exitConditionComponent;
            if (changingExit) {
                ZenLog.traceExitCondition(this.mExitCondition, this.mExitConditionComponent, "config");
            }
            if (this.mDowntime != null) {
                this.mDowntime.setConfig(config);
            }
            if (config.conditionComponents == null || config.conditionIds == null || config.conditionComponents.length != config.conditionIds.length) {
                if (this.DEBUG) {
                    Slog.d(this.TAG, "loadZenConfig: no conditions");
                }
                setAutomaticZenModeConditions(null, false);
                return;
            }
            ArraySet<Uri> newIds = new ArraySet<>();
            int N = config.conditionComponents.length;
            for (int i = 0; i < N; i++) {
                ComponentName component = config.conditionComponents[i];
                Uri id = config.conditionIds[i];
                if (component != null && id != null) {
                    getRecordLocked(id, component);
                    newIds.add(id);
                }
            }
            if (this.DEBUG) {
                Slog.d(this.TAG, "loadZenConfig: N=" + N);
            }
            setAutomaticZenModeConditions((Uri[]) newIds.toArray(new Uri[newIds.size()]), false);
        }
    }

    private void saveZenConfigLocked() {
        ZenModeConfig config = this.mZenModeHelper.getConfig();
        if (config != null) {
            ZenModeConfig config2 = config.copy();
            ArrayList<ConditionRecord> automatic = new ArrayList<>();
            int automaticN = this.mRecords.size();
            for (int i = 0; i < automaticN; i++) {
                ConditionRecord r = this.mRecords.get(i);
                if (r.isAutomatic) {
                    automatic.add(r);
                }
            }
            if (automatic.isEmpty()) {
                config2.conditionComponents = null;
                config2.conditionIds = null;
            } else {
                int N = automatic.size();
                config2.conditionComponents = new ComponentName[N];
                config2.conditionIds = new Uri[N];
                for (int i2 = 0; i2 < N; i2++) {
                    ConditionRecord r2 = automatic.get(i2);
                    config2.conditionComponents[i2] = r2.component;
                    config2.conditionIds[i2] = r2.id;
                }
            }
            config2.exitCondition = this.mExitCondition;
            config2.exitConditionComponent = this.mExitConditionComponent;
            if (this.DEBUG) {
                Slog.d(this.TAG, "Setting zen config to: " + config2);
            }
            this.mZenModeHelper.setConfig(config2);
        }
    }

    private void onManualConditionClearing() {
        if (this.mDowntime != null) {
            this.mDowntime.onManualConditionClearing();
        }
    }

    private class ZenModeHelperCallback extends ZenModeHelper.Callback {
        private ZenModeHelperCallback() {
        }

        @Override
        void onConfigChanged() {
            ConditionProviders.this.loadZenConfig();
        }

        @Override
        void onZenModeChanged() {
            int mode = ConditionProviders.this.mZenModeHelper.getZenMode();
            if (mode == 0) {
                ConditionProviders.this.setZenModeCondition(null, "zenOff");
            }
        }
    }

    private static class ConditionRecord {
        public final ComponentName component;
        public Condition condition;
        public final Uri id;
        public ManagedServices.ManagedServiceInfo info;
        public boolean isAutomatic;
        public boolean isManual;

        private ConditionRecord(Uri id, ComponentName component) {
            this.id = id;
            this.component = component;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ConditionRecord[id=").append(this.id).append(",component=").append(this.component);
            if (this.isAutomatic) {
                sb.append(",automatic");
            }
            if (this.isManual) {
                sb.append(",manual");
            }
            return sb.append(']').toString();
        }
    }
}
