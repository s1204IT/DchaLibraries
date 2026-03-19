package com.android.server.notification;

import android.R;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionProvider;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.notification.ManagedServices;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class ConditionProviders extends ManagedServices {

    @VisibleForTesting
    static final String TAG_ENABLED_DND_APPS = "dnd_apps";
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

    public ConditionProviders(Context context, ManagedServices.UserProfiles userProfiles, IPackageManager iPackageManager) {
        super(context, new Object(), userProfiles, iPackageManager);
        this.mRecords = new ArrayList<>();
        this.mSystemConditionProviders = new ArraySet<>();
        this.mSystemConditionProviderNames = safeSet(PropConfig.getStringArray(this.mContext, "system.condition.providers", R.array.config_deviceStatesAvailableForAppRequests));
        this.mApprovalLevel = 0;
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public boolean isSystemProviderEnabled(String str) {
        return this.mSystemConditionProviderNames.contains(str);
    }

    public void addSystemProvider(SystemConditionProviderService systemConditionProviderService) {
        this.mSystemConditionProviders.add(systemConditionProviderService);
        systemConditionProviderService.attachBase(this.mContext);
        registerService(systemConditionProviderService.asInterface(), systemConditionProviderService.getComponent(), 0);
    }

    public Iterable<SystemConditionProviderService> getSystemProviders() {
        return this.mSystemConditionProviders;
    }

    @Override
    protected ManagedServices.Config getConfig() {
        ManagedServices.Config config = new ManagedServices.Config();
        config.caption = "condition provider";
        config.serviceInterface = "android.service.notification.ConditionProviderService";
        config.secureSettingName = "enabled_notification_policy_access_packages";
        config.xmlTag = TAG_ENABLED_DND_APPS;
        config.secondarySettingName = "enabled_notification_listeners";
        config.bindPermission = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
        config.settingsAction = "android.settings.ACTION_CONDITION_PROVIDER_SETTINGS";
        if (BenesseExtension.getDchaState() != 0) {
            config.settingsAction = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        config.clientLabel = R.string.accessibility_shortcut_warning_dialog_title;
        return config;
    }

    @Override
    public void dump(PrintWriter printWriter, NotificationManagerService.DumpFilter dumpFilter) {
        int i;
        super.dump(printWriter, dumpFilter);
        synchronized (this.mMutex) {
            printWriter.print("    mRecords(");
            printWriter.print(this.mRecords.size());
            printWriter.println("):");
            for (int i2 = 0; i2 < this.mRecords.size(); i2++) {
                ConditionRecord conditionRecord = this.mRecords.get(i2);
                if (dumpFilter == null || dumpFilter.matches(conditionRecord.component)) {
                    printWriter.print("      ");
                    printWriter.println(conditionRecord);
                    String strTryParseDescription = CountdownConditionProvider.tryParseDescription(conditionRecord.id);
                    if (strTryParseDescription != null) {
                        printWriter.print("        (");
                        printWriter.print(strTryParseDescription);
                        printWriter.println(")");
                    }
                }
            }
        }
        printWriter.print("    mSystemConditionProviders: ");
        printWriter.println(this.mSystemConditionProviderNames);
        for (i = 0; i < this.mSystemConditionProviders.size(); i++) {
            this.mSystemConditionProviders.valueAt(i).dump(printWriter, dumpFilter);
        }
    }

    @Override
    protected IInterface asInterface(IBinder iBinder) {
        return IConditionProvider.Stub.asInterface(iBinder);
    }

    @Override
    protected boolean checkType(IInterface iInterface) {
        return iInterface instanceof IConditionProvider;
    }

    @Override
    public void onBootPhaseAppsCanStart() {
        super.onBootPhaseAppsCanStart();
        for (int i = 0; i < this.mSystemConditionProviders.size(); i++) {
            this.mSystemConditionProviders.valueAt(i).onBootComplete();
        }
        if (this.mCallback != null) {
            this.mCallback.onBootComplete();
        }
    }

    @Override
    public void onUserSwitched(int i) {
        super.onUserSwitched(i);
        if (this.mCallback != null) {
            this.mCallback.onUserSwitched();
        }
    }

    @Override
    protected void onServiceAdded(ManagedServices.ManagedServiceInfo managedServiceInfo) {
        try {
            provider(managedServiceInfo).onConnected();
        } catch (RemoteException e) {
        }
        if (this.mCallback != null) {
            this.mCallback.onServiceAdded(managedServiceInfo.component);
        }
    }

    @Override
    protected void onServiceRemovedLocked(ManagedServices.ManagedServiceInfo managedServiceInfo) {
        if (managedServiceInfo == null) {
            return;
        }
        for (int size = this.mRecords.size() - 1; size >= 0; size--) {
            if (this.mRecords.get(size).component.equals(managedServiceInfo.component)) {
                this.mRecords.remove(size);
            }
        }
    }

    @Override
    public void onPackagesChanged(boolean z, String[] strArr, int[] iArr) {
        if (z) {
            INotificationManager service = NotificationManager.getService();
            if (strArr != null && strArr.length > 0) {
                for (String str : strArr) {
                    try {
                        service.removeAutomaticZenRules(str);
                        service.setNotificationPolicyAccessGranted(str, false);
                    } catch (Exception e) {
                        Slog.e(this.TAG, "Failed to clean up rules for " + str, e);
                    }
                }
            }
        }
        super.onPackagesChanged(z, strArr, iArr);
    }

    @Override
    protected boolean isValidEntry(String str, int i) {
        return true;
    }

    public ManagedServices.ManagedServiceInfo checkServiceToken(IConditionProvider iConditionProvider) {
        ManagedServices.ManagedServiceInfo managedServiceInfoCheckServiceTokenLocked;
        synchronized (this.mMutex) {
            managedServiceInfoCheckServiceTokenLocked = checkServiceTokenLocked(iConditionProvider);
        }
        return managedServiceInfoCheckServiceTokenLocked;
    }

    private Condition[] removeDuplicateConditions(String str, Condition[] conditionArr) {
        if (conditionArr == null || conditionArr.length == 0) {
            return null;
        }
        int length = conditionArr.length;
        ArrayMap arrayMap = new ArrayMap(length);
        for (int i = 0; i < length; i++) {
            Uri uri = conditionArr[i].id;
            if (arrayMap.containsKey(uri)) {
                Slog.w(this.TAG, "Ignoring condition from " + str + " for duplicate id: " + uri);
            } else {
                arrayMap.put(uri, conditionArr[i]);
            }
        }
        if (arrayMap.size() == 0) {
            return null;
        }
        if (arrayMap.size() == length) {
            return conditionArr;
        }
        Condition[] conditionArr2 = new Condition[arrayMap.size()];
        for (int i2 = 0; i2 < conditionArr2.length; i2++) {
            conditionArr2[i2] = (Condition) arrayMap.valueAt(i2);
        }
        return conditionArr2;
    }

    private ConditionRecord getRecordLocked(Uri uri, ComponentName componentName, boolean z) {
        if (uri == null || componentName == null) {
            return null;
        }
        int size = this.mRecords.size();
        for (int i = 0; i < size; i++) {
            ConditionRecord conditionRecord = this.mRecords.get(i);
            if (conditionRecord.id.equals(uri) && conditionRecord.component.equals(componentName)) {
                return conditionRecord;
            }
        }
        if (!z) {
            return null;
        }
        ConditionRecord conditionRecord2 = new ConditionRecord(uri, componentName);
        this.mRecords.add(conditionRecord2);
        return conditionRecord2;
    }

    public void notifyConditions(String str, ManagedServices.ManagedServiceInfo managedServiceInfo, Condition[] conditionArr) {
        synchronized (this.mMutex) {
            if (this.DEBUG) {
                String str2 = this.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("notifyConditions pkg=");
                sb.append(str);
                sb.append(" info=");
                sb.append(managedServiceInfo);
                sb.append(" conditions=");
                sb.append(conditionArr == null ? null : Arrays.asList(conditionArr));
                Slog.d(str2, sb.toString());
            }
            Condition[] conditionArrRemoveDuplicateConditions = removeDuplicateConditions(str, conditionArr);
            if (conditionArrRemoveDuplicateConditions != null && conditionArrRemoveDuplicateConditions.length != 0) {
                for (Condition condition : conditionArrRemoveDuplicateConditions) {
                    ConditionRecord recordLocked = getRecordLocked(condition.id, managedServiceInfo.component, true);
                    recordLocked.info = managedServiceInfo;
                    recordLocked.condition = condition;
                }
                for (Condition condition2 : conditionArrRemoveDuplicateConditions) {
                    if (this.mCallback != null) {
                        this.mCallback.onConditionChanged(condition2.id, condition2);
                    }
                }
            }
        }
    }

    public IConditionProvider findConditionProvider(ComponentName componentName) {
        if (componentName == null) {
            return null;
        }
        for (ManagedServices.ManagedServiceInfo managedServiceInfo : getServices()) {
            if (componentName.equals(managedServiceInfo.component)) {
                return provider(managedServiceInfo);
            }
        }
        return null;
    }

    public Condition findCondition(ComponentName componentName, Uri uri) {
        Condition condition;
        if (componentName == null || uri == null) {
            return null;
        }
        synchronized (this.mMutex) {
            ConditionRecord recordLocked = getRecordLocked(uri, componentName, false);
            condition = recordLocked != null ? recordLocked.condition : null;
        }
        return condition;
    }

    public void ensureRecordExists(ComponentName componentName, Uri uri, IConditionProvider iConditionProvider) {
        ConditionRecord recordLocked = getRecordLocked(uri, componentName, true);
        if (recordLocked.info == null) {
            recordLocked.info = checkServiceTokenLocked(iConditionProvider);
        }
    }

    public boolean subscribeIfNecessary(ComponentName componentName, Uri uri) {
        synchronized (this.mMutex) {
            ConditionRecord recordLocked = getRecordLocked(uri, componentName, false);
            if (recordLocked == null) {
                Slog.w(this.TAG, "Unable to subscribe to " + componentName + " " + uri);
                return false;
            }
            if (recordLocked.subscribed) {
                return true;
            }
            subscribeLocked(recordLocked);
            return recordLocked.subscribed;
        }
    }

    public void unsubscribeIfNecessary(ComponentName componentName, Uri uri) {
        synchronized (this.mMutex) {
            ConditionRecord recordLocked = getRecordLocked(uri, componentName, false);
            if (recordLocked == null) {
                Slog.w(this.TAG, "Unable to unsubscribe to " + componentName + " " + uri);
                return;
            }
            if (recordLocked.subscribed) {
                unsubscribeLocked(recordLocked);
            }
        }
    }

    private void subscribeLocked(ConditionRecord conditionRecord) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "subscribeLocked " + conditionRecord);
        }
        IConditionProvider iConditionProviderProvider = provider(conditionRecord);
        if (iConditionProviderProvider != null) {
            try {
                Slog.d(this.TAG, "Subscribing to " + conditionRecord.id + " with " + conditionRecord.component);
                iConditionProviderProvider.onSubscribe(conditionRecord.id);
                conditionRecord.subscribed = true;
                e = null;
            } catch (RemoteException e) {
                e = e;
                Slog.w(this.TAG, "Error subscribing to " + conditionRecord, e);
            }
        } else {
            e = null;
        }
        ZenLog.traceSubscribe(conditionRecord != null ? conditionRecord.id : null, iConditionProviderProvider, e);
    }

    @SafeVarargs
    private static <T> ArraySet<T> safeSet(T... tArr) {
        ArraySet<T> arraySet = new ArraySet<>();
        if (tArr == null || tArr.length == 0) {
            return arraySet;
        }
        for (T t : tArr) {
            if (t != null) {
                arraySet.add(t);
            }
        }
        return arraySet;
    }

    private void unsubscribeLocked(ConditionRecord conditionRecord) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "unsubscribeLocked " + conditionRecord);
        }
        IConditionProvider iConditionProviderProvider = provider(conditionRecord);
        if (iConditionProviderProvider != null) {
            try {
                iConditionProviderProvider.onUnsubscribe(conditionRecord.id);
                e = null;
            } catch (RemoteException e) {
                e = e;
                Slog.w(this.TAG, "Error unsubscribing to " + conditionRecord, e);
            }
            conditionRecord.subscribed = false;
        } else {
            e = null;
        }
        ZenLog.traceUnsubscribe(conditionRecord != null ? conditionRecord.id : null, iConditionProviderProvider, e);
    }

    private static IConditionProvider provider(ConditionRecord conditionRecord) {
        if (conditionRecord == null) {
            return null;
        }
        return provider(conditionRecord.info);
    }

    private static IConditionProvider provider(ManagedServices.ManagedServiceInfo managedServiceInfo) {
        if (managedServiceInfo == null) {
            return null;
        }
        return managedServiceInfo.service;
    }

    private static class ConditionRecord {
        public final ComponentName component;
        public Condition condition;
        public final Uri id;
        public ManagedServices.ManagedServiceInfo info;
        public boolean subscribed;

        private ConditionRecord(Uri uri, ComponentName componentName) {
            this.id = uri;
            this.component = componentName;
        }

        public String toString() {
            return "ConditionRecord[id=" + this.id + ",component=" + this.component + ",subscribed=" + this.subscribed + ']';
        }
    }
}
