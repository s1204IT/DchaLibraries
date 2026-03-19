package com.android.server.notification;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.notification.NotificationManagerService;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class ManagedServices {
    protected static final String ENABLED_SERVICES_SEPARATOR = ":";
    protected final Context mContext;
    private int[] mLastSeenProfileIds;
    protected final Object mMutex;
    private ArraySet<String> mRestored;
    private final SettingsObserver mSettingsObserver;
    private final UserProfiles mUserProfiles;
    protected final String TAG = getClass().getSimpleName();
    protected final boolean DEBUG = Log.isLoggable(this.TAG, 3);
    protected final ArrayList<ManagedServiceInfo> mServices = new ArrayList<>();
    private final ArrayList<String> mServicesBinding = new ArrayList<>();
    private ArraySet<ComponentName> mEnabledServicesForCurrentProfiles = new ArraySet<>();
    private ArraySet<String> mEnabledServicesPackageNames = new ArraySet<>();
    private ArraySet<String> mRestoredPackages = new ArraySet<>();
    private ArraySet<ComponentName> mSnoozingForCurrentProfiles = new ArraySet<>();
    private final Config mConfig = getConfig();
    private final BroadcastReceiver mRestoreReceiver = new SettingRestoredReceiver();

    public static class Config {
        public String bindPermission;
        public String caption;
        public int clientLabel;
        public String secondarySettingName;
        public String secureSettingName;
        public String serviceInterface;
        public String settingsAction;
    }

    protected abstract IInterface asInterface(IBinder iBinder);

    protected abstract boolean checkType(IInterface iInterface);

    protected abstract Config getConfig();

    protected abstract void onServiceAdded(ManagedServiceInfo managedServiceInfo);

    public ManagedServices(Context context, Handler handler, Object mutex, UserProfiles userProfiles) {
        this.mContext = context;
        this.mMutex = mutex;
        this.mUserProfiles = userProfiles;
        this.mSettingsObserver = new SettingsObserver(this, handler, null);
        IntentFilter filter = new IntentFilter("android.os.action.SETTING_RESTORED");
        context.registerReceiver(this.mRestoreReceiver, filter);
        rebuildRestoredPackages();
    }

    class SettingRestoredReceiver extends BroadcastReceiver {
        SettingRestoredReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"android.os.action.SETTING_RESTORED".equals(intent.getAction())) {
                return;
            }
            String element = intent.getStringExtra("setting_name");
            if (!Objects.equals(element, ManagedServices.this.mConfig.secureSettingName) && !Objects.equals(element, ManagedServices.this.mConfig.secondarySettingName)) {
                return;
            }
            String prevValue = intent.getStringExtra("previous_value");
            String newValue = intent.getStringExtra("new_value");
            ManagedServices.this.settingRestored(element, prevValue, newValue, getSendingUserId());
        }
    }

    private String getCaption() {
        return this.mConfig.caption;
    }

    protected void onServiceRemovedLocked(ManagedServiceInfo removed) {
    }

    private ManagedServiceInfo newServiceInfo(IInterface service, ComponentName component, int userid, boolean isSystem, ServiceConnection connection, int targetSdkVersion) {
        return new ManagedServiceInfo(service, component, userid, isSystem, connection, targetSdkVersion);
    }

    public void onBootPhaseAppsCanStart() {
        this.mSettingsObserver.observe();
    }

    public void dump(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        pw.println("    All " + getCaption() + "s (" + this.mEnabledServicesForCurrentProfiles.size() + ") enabled for current profiles:");
        for (ComponentName cmpt : this.mEnabledServicesForCurrentProfiles) {
            if (filter == null || filter.matches(cmpt)) {
                pw.println("      " + cmpt);
            }
        }
        pw.println("    Live " + getCaption() + "s (" + this.mServices.size() + "):");
        for (ManagedServiceInfo info : this.mServices) {
            if (filter == null || filter.matches(info.component)) {
                pw.println("      " + info.component + " (user " + info.userid + "): " + info.service + (info.isSystem ? " SYSTEM" : "") + (info.isGuest(this) ? " GUEST" : ""));
            }
        }
        pw.println("    Snoozed " + getCaption() + "s (" + this.mSnoozingForCurrentProfiles.size() + "):");
        for (ComponentName name : this.mSnoozingForCurrentProfiles) {
            pw.println("      " + name.flattenToShortString());
        }
    }

    public static String restoredSettingName(String setting) {
        return setting + ":restored";
    }

    public void settingRestored(String element, String oldValue, String newValue, int userid) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "Restored managed service setting: " + element + " ovalue=" + oldValue + " nvalue=" + newValue);
        }
        if ((!this.mConfig.secureSettingName.equals(element) && !this.mConfig.secondarySettingName.equals(element)) || element == null) {
            return;
        }
        Settings.Secure.putStringForUser(this.mContext.getContentResolver(), restoredSettingName(element), newValue, userid);
        updateSettingsAccordingToInstalledServices(element, userid);
        rebuildRestoredPackages();
    }

    public boolean isComponentEnabledForPackage(String pkg) {
        return this.mEnabledServicesPackageNames.contains(pkg);
    }

    public void onPackagesChanged(boolean queryReplace, String[] pkgList) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onPackagesChanged queryReplace=" + queryReplace + " pkgList=" + (pkgList != null ? Arrays.asList(pkgList) : null) + " mEnabledServicesPackageNames=" + this.mEnabledServicesPackageNames);
        }
        boolean anyServicesInvolved = false;
        if (pkgList != null && pkgList.length > 0) {
            for (String pkgName : pkgList) {
                if (this.mEnabledServicesPackageNames.contains(pkgName) || this.mRestoredPackages.contains(pkgName)) {
                    anyServicesInvolved = true;
                }
            }
        }
        if (anyServicesInvolved) {
            if (!queryReplace) {
                updateSettingsAccordingToInstalledServices();
                rebuildRestoredPackages();
            }
            rebindServices(false);
        }
    }

    public void onUserSwitched(int user) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserSwitched u=" + user);
        }
        rebuildRestoredPackages();
        if (Arrays.equals(this.mLastSeenProfileIds, this.mUserProfiles.getCurrentProfileIds())) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "Current profile IDs didn't change, skipping rebindServices().");
                return;
            }
            return;
        }
        rebindServices(true);
    }

    public void onUserUnlocked(int user) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserUnlocked u=" + user);
        }
        rebuildRestoredPackages();
        rebindServices(false);
    }

    public ManagedServiceInfo getServiceFromTokenLocked(IInterface service) {
        if (service == null) {
            return null;
        }
        IBinder token = service.asBinder();
        int N = this.mServices.size();
        for (int i = 0; i < N; i++) {
            ManagedServiceInfo info = this.mServices.get(i);
            if (info.service.asBinder() == token) {
                return info;
            }
        }
        return null;
    }

    public ManagedServiceInfo checkServiceTokenLocked(IInterface service) {
        checkNotNull(service);
        ManagedServiceInfo info = getServiceFromTokenLocked(service);
        if (info != null) {
            return info;
        }
        throw new SecurityException("Disallowed call from unknown " + getCaption() + ": " + service);
    }

    public void unregisterService(IInterface service, int userid) {
        checkNotNull(service);
        unregisterServiceImpl(service, userid);
    }

    public void registerService(IInterface service, ComponentName component, int userid) {
        checkNotNull(service);
        ManagedServiceInfo info = registerServiceImpl(service, component, userid);
        if (info == null) {
            return;
        }
        onServiceAdded(info);
    }

    public void registerGuestService(ManagedServiceInfo guest) {
        checkNotNull(guest.service);
        if (!checkType(guest.service)) {
            throw new IllegalArgumentException();
        }
        if (registerServiceImpl(guest) == null) {
            return;
        }
        onServiceAdded(guest);
    }

    public void setComponentState(ComponentName component, boolean enabled) {
        boolean previous = !this.mSnoozingForCurrentProfiles.contains(component);
        if (previous == enabled) {
            return;
        }
        if (enabled) {
            this.mSnoozingForCurrentProfiles.remove(component);
        } else {
            this.mSnoozingForCurrentProfiles.add(component);
        }
        if (this.DEBUG) {
            Slog.d(this.TAG, (enabled ? "Enabling " : "Disabling ") + "component " + component.flattenToShortString());
        }
        synchronized (this.mMutex) {
            int[] userIds = this.mUserProfiles.getCurrentProfileIds();
            for (int userId : userIds) {
                if (enabled) {
                    registerServiceLocked(component, userId);
                } else {
                    unregisterServiceLocked(component, userId);
                }
            }
        }
    }

    private void rebuildRestoredPackages() {
        this.mRestoredPackages.clear();
        this.mSnoozingForCurrentProfiles.clear();
        String secureSettingName = restoredSettingName(this.mConfig.secureSettingName);
        String strRestoredSettingName = this.mConfig.secondarySettingName == null ? null : restoredSettingName(this.mConfig.secondarySettingName);
        int[] userIds = this.mUserProfiles.getCurrentProfileIds();
        int N = userIds.length;
        for (int i = 0; i < N; i++) {
            ArraySet<ComponentName> names = loadComponentNamesFromSetting(secureSettingName, userIds[i]);
            if (strRestoredSettingName != null) {
                names.addAll((ArraySet<? extends ComponentName>) loadComponentNamesFromSetting(strRestoredSettingName, userIds[i]));
            }
            for (ComponentName name : names) {
                this.mRestoredPackages.add(name.getPackageName());
            }
        }
    }

    protected ArraySet<ComponentName> loadComponentNamesFromSetting(String settingName, int userId) {
        ContentResolver cr = this.mContext.getContentResolver();
        String settingValue = Settings.Secure.getStringForUser(cr, settingName, userId);
        if (TextUtils.isEmpty(settingValue)) {
            return new ArraySet<>();
        }
        String[] restored = settingValue.split(ENABLED_SERVICES_SEPARATOR);
        ArraySet<ComponentName> result = new ArraySet<>(restored.length);
        for (String str : restored) {
            ComponentName value = ComponentName.unflattenFromString(str);
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

    private void storeComponentsToSetting(Set<ComponentName> components, String settingName, int userId) {
        String[] componentNames = null;
        if (components != null) {
            componentNames = new String[components.size()];
            int index = 0;
            for (ComponentName c : components) {
                componentNames[index] = c.flattenToString();
                index++;
            }
        }
        String value = componentNames == null ? "" : TextUtils.join(ENABLED_SERVICES_SEPARATOR, componentNames);
        ContentResolver cr = this.mContext.getContentResolver();
        Settings.Secure.putStringForUser(cr, settingName, value, userId);
    }

    private void updateSettingsAccordingToInstalledServices() {
        int[] userIds = this.mUserProfiles.getCurrentProfileIds();
        int N = userIds.length;
        for (int i = 0; i < N; i++) {
            updateSettingsAccordingToInstalledServices(this.mConfig.secureSettingName, userIds[i]);
            if (this.mConfig.secondarySettingName != null) {
                updateSettingsAccordingToInstalledServices(this.mConfig.secondarySettingName, userIds[i]);
            }
        }
        rebuildRestoredPackages();
    }

    protected Set<ComponentName> queryPackageForServices(String packageName, int userId) {
        Set<ComponentName> installed = new ArraySet<>();
        PackageManager pm = this.mContext.getPackageManager();
        Intent queryIntent = new Intent(this.mConfig.serviceInterface);
        if (!TextUtils.isEmpty(packageName)) {
            queryIntent.setPackage(packageName);
        }
        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(queryIntent, 132, userId);
        if (this.DEBUG) {
            Slog.v(this.TAG, this.mConfig.serviceInterface + " services: " + installedServices);
        }
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ResolveInfo resolveInfo = installedServices.get(i);
                ServiceInfo info = resolveInfo.serviceInfo;
                ComponentName component = new ComponentName(info.packageName, info.name);
                if (this.mConfig.bindPermission.equals(info.permission)) {
                    installed.add(component);
                } else {
                    Slog.w(this.TAG, "Skipping " + getCaption() + " service " + info.packageName + "/" + info.name + ": it does not require the permission " + this.mConfig.bindPermission);
                }
            }
        }
        return installed;
    }

    private void updateSettingsAccordingToInstalledServices(String setting, int userId) {
        boolean restoredChanged = false;
        boolean currentChanged = false;
        ArraySet<ComponentName> restored = loadComponentNamesFromSetting(restoredSettingName(setting), userId);
        ArraySet<ComponentName> current = loadComponentNamesFromSetting(setting, userId);
        Set<ComponentName> installed = queryPackageForServices(null, userId);
        ArraySet<ComponentName> retained = new ArraySet<>();
        for (ComponentName component : installed) {
            if (restored != null) {
                boolean wasRestored = restored.remove(component);
                if (wasRestored) {
                    if (this.DEBUG) {
                        Slog.v(this.TAG, "Restoring " + component + " for user " + userId);
                    }
                    restoredChanged = true;
                    currentChanged = true;
                    retained.add(component);
                }
            }
            if (current != null && current.contains(component)) {
                retained.add(component);
            }
        }
        if (currentChanged | ((current == null ? 0 : current.size()) != retained.size())) {
            if (this.DEBUG) {
                Slog.v(this.TAG, "List of  " + getCaption() + " services was updated " + current);
            }
            storeComponentsToSetting(retained, setting, userId);
        }
        if (!restoredChanged) {
            return;
        }
        if (this.DEBUG) {
            Slog.v(this.TAG, "List of  " + getCaption() + " restored services was updated " + restored);
        }
        storeComponentsToSetting(restored, restoredSettingName(setting), userId);
    }

    private void rebindServices(boolean forceRebind) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "rebindServices");
        }
        int[] userIds = this.mUserProfiles.getCurrentProfileIds();
        int nUserIds = userIds.length;
        SparseArray<ArraySet<ComponentName>> componentsByUser = new SparseArray<>();
        for (int i = 0; i < nUserIds; i++) {
            componentsByUser.put(userIds[i], loadComponentNamesFromSetting(this.mConfig.secureSettingName, userIds[i]));
            if (this.mConfig.secondarySettingName != null) {
                componentsByUser.get(userIds[i]).addAll(loadComponentNamesFromSetting(this.mConfig.secondarySettingName, userIds[i]));
            }
        }
        ArrayList<ManagedServiceInfo> removableBoundServices = new ArrayList<>();
        SparseArray<Set<ComponentName>> toAdd = new SparseArray<>();
        synchronized (this.mMutex) {
            for (ManagedServiceInfo service : this.mServices) {
                if (!service.isSystem && !service.isGuest(this)) {
                    removableBoundServices.add(service);
                }
            }
            this.mEnabledServicesForCurrentProfiles.clear();
            this.mEnabledServicesPackageNames.clear();
            for (int i2 = 0; i2 < nUserIds; i2++) {
                ArraySet<ComponentName> userComponents = componentsByUser.get(userIds[i2]);
                if (userComponents == null) {
                    toAdd.put(userIds[i2], new ArraySet<>());
                } else {
                    Set<ComponentName> add = new HashSet<>(userComponents);
                    add.removeAll(this.mSnoozingForCurrentProfiles);
                    toAdd.put(userIds[i2], add);
                    this.mEnabledServicesForCurrentProfiles.addAll((ArraySet<? extends ComponentName>) userComponents);
                    for (int j = 0; j < userComponents.size(); j++) {
                        this.mEnabledServicesPackageNames.add(userComponents.valueAt(j).getPackageName());
                    }
                }
            }
        }
        for (ManagedServiceInfo info : removableBoundServices) {
            ComponentName component = info.component;
            int oldUser = info.userid;
            Set<ComponentName> allowedComponents = toAdd.get(info.userid);
            if (allowedComponents != null) {
                if (allowedComponents.contains(component) && !forceRebind) {
                    allowedComponents.remove(component);
                } else {
                    Slog.v(this.TAG, "disabling " + getCaption() + " for user " + oldUser + ": " + component);
                    unregisterService(component, oldUser);
                }
            }
        }
        for (int i3 = 0; i3 < nUserIds; i3++) {
            for (ComponentName component2 : toAdd.get(userIds[i3])) {
                Slog.v(this.TAG, "enabling " + getCaption() + " for " + userIds[i3] + ": " + component2);
                registerService(component2, userIds[i3]);
            }
        }
        this.mLastSeenProfileIds = userIds;
    }

    private void registerService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            registerServiceLocked(name, userid);
        }
    }

    public void registerSystemService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            registerServiceLocked(name, userid, true);
        }
    }

    private void registerServiceLocked(ComponentName name, int userid) {
        registerServiceLocked(name, userid, false);
    }

    private void registerServiceLocked(ComponentName name, final int userid, final boolean isSystem) {
        if (this.DEBUG) {
            Slog.v(this.TAG, "registerService: " + name + " u=" + userid);
        }
        final String servicesBindingTag = name.toString() + "/" + userid;
        if (this.mServicesBinding.contains(servicesBindingTag)) {
            return;
        }
        this.mServicesBinding.add(servicesBindingTag);
        int N = this.mServices.size();
        for (int i = N - 1; i >= 0; i--) {
            ManagedServiceInfo info = this.mServices.get(i);
            if (name.equals(info.component) && info.userid == userid) {
                if (this.DEBUG) {
                    Slog.v(this.TAG, "    disconnecting old " + getCaption() + ": " + info.service);
                }
                removeServiceLocked(i);
                if (info.connection != null) {
                    this.mContext.unbindService(info.connection);
                }
            }
        }
        Intent intent = new Intent(this.mConfig.serviceInterface);
        intent.setComponent(name);
        intent.putExtra("android.intent.extra.client_label", this.mConfig.clientLabel);
        if (this.mConfig.settingsAction != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(this.mContext, 0, new Intent(this.mConfig.settingsAction), 0);
            intent.putExtra("android.intent.extra.client_intent", pendingIntent);
        }
        ApplicationInfo appInfo = null;
        try {
            appInfo = this.mContext.getPackageManager().getApplicationInfo(name.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
        }
        final int targetSdkVersion = appInfo != null ? appInfo.targetSdkVersion : 1;
        try {
            if (this.DEBUG) {
                Slog.v(this.TAG, "binding: " + intent);
            }
            ServiceConnection serviceConnection = new ServiceConnection() {
                IInterface mService;

                @Override
                public void onServiceConnected(ComponentName name2, IBinder binder) {
                    boolean added = false;
                    ManagedServiceInfo info2 = null;
                    synchronized (ManagedServices.this.mMutex) {
                        ManagedServices.this.mServicesBinding.remove(servicesBindingTag);
                        try {
                            this.mService = ManagedServices.this.asInterface(binder);
                            info2 = ManagedServices.this.newServiceInfo(this.mService, name2, userid, isSystem, this, targetSdkVersion);
                            binder.linkToDeath(info2, 0);
                            added = ManagedServices.this.mServices.add(info2);
                        } catch (RemoteException e2) {
                        }
                    }
                    if (!added) {
                        return;
                    }
                    ManagedServices.this.onServiceAdded(info2);
                }

                @Override
                public void onServiceDisconnected(ComponentName name2) {
                    Slog.v(ManagedServices.this.TAG, ManagedServices.this.getCaption() + " connection lost: " + name2);
                }
            };
            if (this.mContext.bindServiceAsUser(intent, serviceConnection, 83886081, new UserHandle(userid))) {
                return;
            }
            this.mServicesBinding.remove(servicesBindingTag);
            Slog.w(this.TAG, "Unable to bind " + getCaption() + " service: " + intent);
        } catch (SecurityException ex) {
            Slog.e(this.TAG, "Unable to bind " + getCaption() + " service: " + intent, ex);
        }
    }

    private void unregisterService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
            unregisterServiceLocked(name, userid);
        }
    }

    private void unregisterServiceLocked(ComponentName name, int userid) {
        int N = this.mServices.size();
        for (int i = N - 1; i >= 0; i--) {
            ManagedServiceInfo info = this.mServices.get(i);
            if (name.equals(info.component) && info.userid == userid) {
                removeServiceLocked(i);
                if (info.connection != null) {
                    try {
                        this.mContext.unbindService(info.connection);
                    } catch (IllegalArgumentException ex) {
                        Slog.e(this.TAG, getCaption() + " " + name + " could not be unbound: " + ex);
                    }
                }
            }
        }
    }

    private ManagedServiceInfo removeServiceImpl(IInterface service, int userid) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "removeServiceImpl service=" + service + " u=" + userid);
        }
        ManagedServiceInfo serviceInfo = null;
        synchronized (this.mMutex) {
            int N = this.mServices.size();
            for (int i = N - 1; i >= 0; i--) {
                ManagedServiceInfo info = this.mServices.get(i);
                if (info.service.asBinder() == service.asBinder() && info.userid == userid) {
                    if (this.DEBUG) {
                        Slog.d(this.TAG, "Removing active service " + info.component);
                    }
                    serviceInfo = removeServiceLocked(i);
                }
            }
        }
        return serviceInfo;
    }

    private ManagedServiceInfo removeServiceLocked(int i) {
        ManagedServiceInfo info = this.mServices.remove(i);
        onServiceRemovedLocked(info);
        return info;
    }

    private void checkNotNull(IInterface service) {
        if (service != null) {
        } else {
            throw new IllegalArgumentException(getCaption() + " must not be null");
        }
    }

    private ManagedServiceInfo registerServiceImpl(IInterface service, ComponentName component, int userid) {
        ManagedServiceInfo info = newServiceInfo(service, component, userid, true, null, 21);
        return registerServiceImpl(info);
    }

    private ManagedServiceInfo registerServiceImpl(ManagedServiceInfo info) {
        synchronized (this.mMutex) {
            try {
                info.service.asBinder().linkToDeath(info, 0);
                this.mServices.add(info);
            } catch (RemoteException e) {
                return null;
            }
        }
        return info;
    }

    private void unregisterServiceImpl(IInterface service, int userid) {
        ManagedServiceInfo info = removeServiceImpl(service, userid);
        if (info == null || info.connection == null || info.isGuest(this)) {
            return;
        }
        this.mContext.unbindService(info.connection);
    }

    private class SettingsObserver extends ContentObserver {
        private final Uri mSecondarySettingsUri;
        private final Uri mSecureSettingsUri;

        SettingsObserver(ManagedServices this$0, Handler handler, SettingsObserver settingsObserver) {
            this(handler);
        }

        private SettingsObserver(Handler handler) {
            super(handler);
            this.mSecureSettingsUri = Settings.Secure.getUriFor(ManagedServices.this.mConfig.secureSettingName);
            if (ManagedServices.this.mConfig.secondarySettingName != null) {
                this.mSecondarySettingsUri = Settings.Secure.getUriFor(ManagedServices.this.mConfig.secondarySettingName);
            } else {
                this.mSecondarySettingsUri = null;
            }
        }

        private void observe() {
            ContentResolver resolver = ManagedServices.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mSecureSettingsUri, false, this, -1);
            if (this.mSecondarySettingsUri != null) {
                resolver.registerContentObserver(this.mSecondarySettingsUri, false, this, -1);
            }
            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        private void update(Uri uri) {
            if (uri != null && !this.mSecureSettingsUri.equals(uri) && !uri.equals(this.mSecondarySettingsUri)) {
                return;
            }
            if (ManagedServices.this.DEBUG) {
                Slog.d(ManagedServices.this.TAG, "Setting changed: uri=" + uri);
            }
            ManagedServices.this.rebindServices(false);
            ManagedServices.this.rebuildRestoredPackages();
        }
    }

    public class ManagedServiceInfo implements IBinder.DeathRecipient {
        public ComponentName component;
        public ServiceConnection connection;
        public boolean isSystem;
        public IInterface service;
        public int targetSdkVersion;
        public int userid;

        public ManagedServiceInfo(IInterface service, ComponentName component, int userid, boolean isSystem, ServiceConnection connection, int targetSdkVersion) {
            this.service = service;
            this.component = component;
            this.userid = userid;
            this.isSystem = isSystem;
            this.connection = connection;
            this.targetSdkVersion = targetSdkVersion;
        }

        public boolean isGuest(ManagedServices host) {
            return ManagedServices.this != host;
        }

        public ManagedServices getOwner() {
            return ManagedServices.this;
        }

        public String toString() {
            return "ManagedServiceInfo[component=" + this.component + ",userid=" + this.userid + ",isSystem=" + this.isSystem + ",targetSdkVersion=" + this.targetSdkVersion + ",connection=" + (this.connection != null ? "<connection>" : null) + ",service=" + this.service + ']';
        }

        public boolean enabledAndUserMatches(int nid) {
            if (!isEnabledForCurrentProfiles()) {
                return false;
            }
            if (this.userid == -1 || this.isSystem || nid == -1 || nid == this.userid) {
                return true;
            }
            if (supportsProfiles()) {
                return ManagedServices.this.mUserProfiles.isCurrentProfile(nid);
            }
            return false;
        }

        public boolean supportsProfiles() {
            return this.targetSdkVersion >= 21;
        }

        @Override
        public void binderDied() {
            if (ManagedServices.this.DEBUG) {
                Slog.d(ManagedServices.this.TAG, "binderDied");
            }
            ManagedServices.this.removeServiceImpl(this.service, this.userid);
        }

        public boolean isEnabledForCurrentProfiles() {
            if (this.isSystem) {
                return true;
            }
            if (this.connection == null) {
                return false;
            }
            return ManagedServices.this.mEnabledServicesForCurrentProfiles.contains(this.component);
        }
    }

    public boolean isComponentEnabledForCurrentProfiles(ComponentName component) {
        return this.mEnabledServicesForCurrentProfiles.contains(component);
    }

    public static class UserProfiles {
        private final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();

        public void updateCache(Context context) {
            UserManager userManager = (UserManager) context.getSystemService("user");
            if (userManager == null) {
                return;
            }
            int currentUserId = ActivityManager.getCurrentUser();
            List<UserInfo> profiles = userManager.getProfiles(currentUserId);
            synchronized (this.mCurrentProfiles) {
                this.mCurrentProfiles.clear();
                for (UserInfo user : profiles) {
                    this.mCurrentProfiles.put(user.id, user);
                }
            }
        }

        public int[] getCurrentProfileIds() {
            int[] users;
            synchronized (this.mCurrentProfiles) {
                users = new int[this.mCurrentProfiles.size()];
                int N = this.mCurrentProfiles.size();
                for (int i = 0; i < N; i++) {
                    users[i] = this.mCurrentProfiles.keyAt(i);
                }
            }
            return users;
        }

        public boolean isCurrentProfile(int userId) {
            boolean z;
            synchronized (this.mCurrentProfiles) {
                z = this.mCurrentProfiles.get(userId) != null;
            }
            return z;
        }
    }
}
