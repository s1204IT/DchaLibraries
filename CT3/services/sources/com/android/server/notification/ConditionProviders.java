package com.android.server.notification;

import android.R;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.provider.Settings;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class ConditionProviders extends ManagedServices {
    private Callback mCallback;
    private final ArrayList<ConditionRecord> mRecords;
    private final ArraySet<String> mSystemConditionProviderNames;
    private final ArraySet<SystemConditionProviderService> mSystemConditionProviders;

    public interface Callback {
        void onBootComplete();

        void onConditionChanged(Uri uri, Condition condition);

        void onServiceAdded(ComponentName componentName);

        void onUserSwitched();
    }

    public ConditionProviders(Context context, Handler handler, ManagedServices.UserProfiles userProfiles) {
        super(context, handler, new Object(), userProfiles);
        this.mRecords = new ArrayList<>();
        this.mSystemConditionProviders = new ArraySet<>();
        this.mSystemConditionProviderNames = safeSet(PropConfig.getStringArray(this.mContext, "system.condition.providers", R.array.config_deviceStatesAvailableForAppRequests));
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public boolean isSystemProviderEnabled(String path) {
        return this.mSystemConditionProviderNames.contains(path);
    }

    public void addSystemProvider(SystemConditionProviderService service) {
        this.mSystemConditionProviders.add(service);
        service.attachBase(this.mContext);
        registerService(service.asInterface(), service.getComponent(), 0);
    }

    public Iterable<SystemConditionProviderService> getSystemProviders() {
        return this.mSystemConditionProviders;
    }

    @Override
    protected ManagedServices.Config getConfig() {
        ManagedServices.Config c = new ManagedServices.Config();
        c.caption = "condition provider";
        c.serviceInterface = "android.service.notification.ConditionProviderService";
        c.secureSettingName = "enabled_notification_policy_access_packages";
        c.secondarySettingName = "enabled_notification_listeners";
        c.bindPermission = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
        c.settingsAction = "android.settings.ACTION_CONDITION_PROVIDER_SETTINGS";
        if (BenesseExtension.getDchaState() != 0) {
            c.settingsAction = null;
        }
        c.clientLabel = R.string.fp_power_button_bp_message;
        return c;
    }

    @Override
    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        super.dump(pw, filter);
        synchronized (this.mMutex) {
            pw.print("    mRecords(");
            pw.print(this.mRecords.size());
            pw.println("):");
            for (int i = 0; i < this.mRecords.size(); i++) {
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
        }
        pw.print("    mSystemConditionProviders: ");
        pw.println(this.mSystemConditionProviderNames);
        for (int i2 = 0; i2 < this.mSystemConditionProviders.size(); i2++) {
            this.mSystemConditionProviders.valueAt(i2).dump(pw, filter);
        }
    }

    @Override
    protected IInterface asInterface(IBinder binder) {
        return IConditionProvider.Stub.asInterface(binder);
    }

    @Override
    protected boolean checkType(IInterface service) {
        return service instanceof IConditionProvider;
    }

    @Override
    public void onBootPhaseAppsCanStart() {
        super.onBootPhaseAppsCanStart();
        for (int i = 0; i < this.mSystemConditionProviders.size(); i++) {
            this.mSystemConditionProviders.valueAt(i).onBootComplete();
        }
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onBootComplete();
    }

    @Override
    public void onUserSwitched(int user) {
        super.onUserSwitched(user);
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onUserSwitched();
    }

    @Override
    protected void onServiceAdded(ManagedServices.ManagedServiceInfo info) {
        IConditionProvider provider = provider(info);
        try {
            provider.onConnected();
        } catch (RemoteException e) {
        }
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onServiceAdded(info.component);
    }

    @Override
    protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo removed) {
        if (removed == null) {
            return;
        }
        for (int i = this.mRecords.size() - 1; i >= 0; i--) {
            ConditionRecord r = this.mRecords.get(i);
            if (r.component.equals(removed.component)) {
                this.mRecords.remove(i);
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

    private Condition[] removeDuplicateConditions(String pkg, Condition[] conditions) {
        if (conditions == null || conditions.length == 0) {
            return null;
        }
        int N = conditions.length;
        ArrayMap<Uri, Condition> valid = new ArrayMap<>(N);
        for (int i = 0; i < N; i++) {
            Uri id = conditions[i].id;
            if (valid.containsKey(id)) {
                Slog.w(this.TAG, "Ignoring condition from " + pkg + " for duplicate id: " + id);
            } else {
                valid.put(id, conditions[i]);
            }
        }
        if (valid.size() == 0) {
            return null;
        }
        if (valid.size() == N) {
            return conditions;
        }
        Condition[] rt = new Condition[valid.size()];
        for (int i2 = 0; i2 < rt.length; i2++) {
            rt[i2] = valid.valueAt(i2);
        }
        return rt;
    }

    private ConditionRecord getRecordLocked(Uri id, ComponentName component, boolean create) {
        ConditionRecord conditionRecord = null;
        if (id == null || component == null) {
            return null;
        }
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            ConditionRecord r = this.mRecords.get(i);
            if (r.id.equals(id) && r.component.equals(component)) {
                return r;
            }
        }
        if (!create) {
            return null;
        }
        ConditionRecord r2 = new ConditionRecord(id, component, conditionRecord);
        this.mRecords.add(r2);
        return r2;
    }

    public void notifyConditions(String pkg, ManagedServices.ManagedServiceInfo info, Condition[] conditions) {
        synchronized (this.mMutex) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "notifyConditions pkg=" + pkg + " info=" + info + " conditions=" + (conditions != null ? Arrays.asList(conditions) : null));
            }
            Condition[] conditions2 = removeDuplicateConditions(pkg, conditions);
            if (conditions2 == null || conditions2.length == 0) {
                return;
            }
            for (Condition c : conditions2) {
                ConditionRecord r = getRecordLocked(c.id, info.component, true);
                r.info = info;
                r.condition = c;
            }
            for (Condition c2 : conditions2) {
                if (this.mCallback != null) {
                    this.mCallback.onConditionChanged(c2.id, c2);
                }
            }
        }
    }

    public IConditionProvider findConditionProvider(ComponentName component) {
        if (component == null) {
            return null;
        }
        for (ManagedServices.ManagedServiceInfo service : this.mServices) {
            if (component.equals(service.component)) {
                return provider(service);
            }
        }
        return null;
    }

    public Condition findCondition(ComponentName component, Uri conditionId) {
        Condition condition;
        if (component == null || conditionId == null) {
            return null;
        }
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, false);
            condition = r != null ? r.condition : null;
        }
        return condition;
    }

    public void ensureRecordExists(ComponentName component, Uri conditionId, IConditionProvider provider) {
        ConditionRecord r = getRecordLocked(conditionId, component, true);
        if (r.info != null) {
            return;
        }
        r.info = checkServiceTokenLocked(provider);
    }

    @Override
    protected ArraySet<ComponentName> loadComponentNamesFromSetting(String settingName, int userId) {
        ContentResolver cr = this.mContext.getContentResolver();
        String settingValue = Settings.Secure.getStringForUser(cr, settingName, userId);
        if (TextUtils.isEmpty(settingValue)) {
            return new ArraySet<>();
        }
        String[] packages = settingValue.split(":");
        ArraySet<ComponentName> result = new ArraySet<>(packages.length);
        for (int i = 0; i < packages.length; i++) {
            if (!TextUtils.isEmpty(packages[i])) {
                ComponentName component = ComponentName.unflattenFromString(packages[i]);
                if (component != null) {
                    result.addAll(queryPackageForServices(component.getPackageName(), userId));
                } else {
                    result.addAll(queryPackageForServices(packages[i], userId));
                }
            }
        }
        return result;
    }

    public boolean subscribeIfNecessary(ComponentName component, Uri conditionId) {
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, false);
            if (r == null) {
                Slog.w(this.TAG, "Unable to subscribe to " + component + " " + conditionId);
                return false;
            }
            if (r.subscribed) {
                return true;
            }
            subscribeLocked(r);
            return r.subscribed;
        }
    }

    public void unsubscribeIfNecessary(ComponentName component, Uri conditionId) {
        synchronized (this.mMutex) {
            ConditionRecord r = getRecordLocked(conditionId, component, false);
            if (r == null) {
                Slog.w(this.TAG, "Unable to unsubscribe to " + component + " " + conditionId);
            } else if (r.subscribed) {
                unsubscribeLocked(r);
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
                Slog.d(this.TAG, "Subscribing to " + r.id + " with " + r.component);
                provider.onSubscribe(r.id);
                r.subscribed = true;
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
        if (items == null || items.length == 0) {
            return rt;
        }
        for (T item : items) {
            if (item != null) {
                rt.add(item);
            }
        }
        return rt;
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
            r.subscribed = false;
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

    private static class ConditionRecord {
        public final ComponentName component;
        public Condition condition;
        public final Uri id;
        public ManagedServices.ManagedServiceInfo info;
        public boolean subscribed;

        ConditionRecord(Uri id, ComponentName component, ConditionRecord conditionRecord) {
            this(id, component);
        }

        private ConditionRecord(Uri id, ComponentName component) {
            this.id = id;
            this.component = component;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("ConditionRecord[id=").append(this.id).append(",component=").append(this.component).append(",subscribed=").append(this.subscribed);
            return sb.append(']').toString();
        }
    }
}
