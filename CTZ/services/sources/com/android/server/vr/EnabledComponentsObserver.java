package com.android.server.vr;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.content.PackageMonitor;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.vr.SettingsObserver;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EnabledComponentsObserver implements SettingsObserver.SettingChangeListener {
    public static final int DISABLED = -1;
    private static final String ENABLED_SERVICES_SEPARATOR = ":";
    public static final int NOT_INSTALLED = -2;
    public static final int NO_ERROR = 0;
    private static final String TAG = EnabledComponentsObserver.class.getSimpleName();
    private final Context mContext;
    private final Object mLock;
    private final String mServiceName;
    private final String mServicePermission;
    private final String mSettingName;
    private final SparseArray<ArraySet<ComponentName>> mInstalledSet = new SparseArray<>();
    private final SparseArray<ArraySet<ComponentName>> mEnabledSet = new SparseArray<>();
    private final Set<EnabledComponentChangeListener> mEnabledComponentListeners = new ArraySet();

    public interface EnabledComponentChangeListener {
        void onEnabledComponentChanged();
    }

    private EnabledComponentsObserver(Context context, String str, String str2, String str3, Object obj, Collection<EnabledComponentChangeListener> collection) {
        this.mLock = obj;
        this.mContext = context;
        this.mSettingName = str;
        this.mServiceName = str3;
        this.mServicePermission = str2;
        this.mEnabledComponentListeners.addAll(collection);
    }

    public static EnabledComponentsObserver build(Context context, Handler handler, String str, Looper looper, String str2, String str3, Object obj, Collection<EnabledComponentChangeListener> collection) {
        SettingsObserver settingsObserverBuild = SettingsObserver.build(context, handler, str);
        EnabledComponentsObserver enabledComponentsObserver = new EnabledComponentsObserver(context, str, str2, str3, obj, collection);
        new PackageMonitor() {
            public void onSomePackagesChanged() {
                EnabledComponentsObserver.this.onPackagesChanged();
            }

            public void onPackageDisappeared(String str4, int i) {
                EnabledComponentsObserver.this.onPackagesChanged();
            }

            public void onPackageModified(String str4) {
                EnabledComponentsObserver.this.onPackagesChanged();
            }

            public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
                EnabledComponentsObserver.this.onPackagesChanged();
                return super.onHandleForceStop(intent, strArr, i, z);
            }
        }.register(context, looper, UserHandle.ALL, true);
        settingsObserverBuild.addListener(enabledComponentsObserver);
        return enabledComponentsObserver;
    }

    public void onPackagesChanged() {
        rebuildAll();
    }

    @Override
    public void onSettingChanged() {
        rebuildAll();
    }

    @Override
    public void onSettingRestored(String str, String str2, int i) {
        rebuildAll();
    }

    public void onUsersChanged() {
        rebuildAll();
    }

    public void rebuildAll() {
        synchronized (this.mLock) {
            this.mInstalledSet.clear();
            this.mEnabledSet.clear();
            for (int i : getCurrentProfileIds()) {
                ArraySet<ComponentName> arraySetLoadComponentNamesForUser = loadComponentNamesForUser(i);
                ArraySet<ComponentName> arraySetLoadComponentNamesFromSetting = loadComponentNamesFromSetting(this.mSettingName, i);
                arraySetLoadComponentNamesFromSetting.retainAll(arraySetLoadComponentNamesForUser);
                this.mInstalledSet.put(i, arraySetLoadComponentNamesForUser);
                this.mEnabledSet.put(i, arraySetLoadComponentNamesFromSetting);
            }
        }
        sendSettingChanged();
    }

    public int isValid(ComponentName componentName, int i) {
        synchronized (this.mLock) {
            ArraySet<ComponentName> arraySet = this.mInstalledSet.get(i);
            if (arraySet != null && arraySet.contains(componentName)) {
                ArraySet<ComponentName> arraySet2 = this.mEnabledSet.get(i);
                if (arraySet2 != null && arraySet2.contains(componentName)) {
                    return 0;
                }
                return -1;
            }
            return -2;
        }
    }

    public ArraySet<ComponentName> getInstalled(int i) {
        synchronized (this.mLock) {
            ArraySet<ComponentName> arraySet = this.mInstalledSet.get(i);
            if (arraySet != null) {
                return arraySet;
            }
            return new ArraySet<>();
        }
    }

    public ArraySet<ComponentName> getEnabled(int i) {
        synchronized (this.mLock) {
            ArraySet<ComponentName> arraySet = this.mEnabledSet.get(i);
            if (arraySet != null) {
                return arraySet;
            }
            return new ArraySet<>();
        }
    }

    private int[] getCurrentProfileIds() {
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        if (userManager == null) {
            return null;
        }
        return userManager.getEnabledProfileIds(ActivityManager.getCurrentUser());
    }

    public static ArraySet<ComponentName> loadComponentNames(PackageManager packageManager, int i, String str, String str2) {
        ArraySet<ComponentName> arraySet = new ArraySet<>();
        List listQueryIntentServicesAsUser = packageManager.queryIntentServicesAsUser(new Intent(str), 786564, i);
        if (listQueryIntentServicesAsUser != null) {
            int size = listQueryIntentServicesAsUser.size();
            for (int i2 = 0; i2 < size; i2++) {
                ServiceInfo serviceInfo = ((ResolveInfo) listQueryIntentServicesAsUser.get(i2)).serviceInfo;
                ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                if (!str2.equals(serviceInfo.permission)) {
                    Slog.w(TAG, "Skipping service " + serviceInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceInfo.name + ": it does not require the permission " + str2);
                } else {
                    arraySet.add(componentName);
                }
            }
        }
        return arraySet;
    }

    private ArraySet<ComponentName> loadComponentNamesForUser(int i) {
        return loadComponentNames(this.mContext.getPackageManager(), i, this.mServiceName, this.mServicePermission);
    }

    private ArraySet<ComponentName> loadComponentNamesFromSetting(String str, int i) {
        String stringForUser = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), str, i);
        if (TextUtils.isEmpty(stringForUser)) {
            return new ArraySet<>();
        }
        String[] strArrSplit = stringForUser.split(ENABLED_SERVICES_SEPARATOR);
        ArraySet<ComponentName> arraySet = new ArraySet<>(strArrSplit.length);
        for (String str2 : strArrSplit) {
            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(str2);
            if (componentNameUnflattenFromString != null) {
                arraySet.add(componentNameUnflattenFromString);
            }
        }
        return arraySet;
    }

    private void sendSettingChanged() {
        Iterator<EnabledComponentChangeListener> it = this.mEnabledComponentListeners.iterator();
        while (it.hasNext()) {
            it.next().onEnabledComponentChanged();
        }
    }
}
