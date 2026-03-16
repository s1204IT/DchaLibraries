package com.android.server.notification;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BenesseExtension;
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
import java.util.List;
import java.util.Set;

public abstract class ManagedServices {
    private static final String ENABLED_SERVICES_SEPARATOR = ":";
    protected final Context mContext;
    private int[] mLastSeenProfileIds;
    protected final Object mMutex;
    private final SettingsObserver mSettingsObserver;
    private final UserProfiles mUserProfiles;
    protected final String TAG = getClass().getSimpleName();
    protected final boolean DEBUG = Log.isLoggable(this.TAG, 3);
    protected final ArrayList<ManagedServiceInfo> mServices = new ArrayList<>();
    private final ArrayList<String> mServicesBinding = new ArrayList<>();
    private ArraySet<ComponentName> mEnabledServicesForCurrentProfiles = new ArraySet<>();
    private ArraySet<String> mEnabledServicesPackageNames = new ArraySet<>();
    private final Config mConfig = getConfig();

    protected abstract IInterface asInterface(IBinder iBinder);

    protected abstract Config getConfig();

    protected abstract void onServiceAdded(ManagedServiceInfo managedServiceInfo);

    public ManagedServices(Context context, Handler handler, Object mutex, UserProfiles userProfiles) {
        this.mContext = context;
        this.mMutex = mutex;
        this.mUserProfiles = userProfiles;
        this.mSettingsObserver = new SettingsObserver(handler);
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
                pw.println("      " + info.component + " (user " + info.userid + "): " + info.service + (info.isSystem ? " SYSTEM" : ""));
            }
        }
    }

    public void onPackagesChanged(boolean queryReplace, String[] pkgList) {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onPackagesChanged queryReplace=" + queryReplace + " pkgList=" + (pkgList == null ? null : Arrays.asList(pkgList)) + " mEnabledServicesPackageNames=" + this.mEnabledServicesPackageNames);
        }
        boolean anyServicesInvolved = false;
        if (pkgList != null && pkgList.length > 0) {
            for (String pkgName : pkgList) {
                if (this.mEnabledServicesPackageNames.contains(pkgName)) {
                    anyServicesInvolved = true;
                }
            }
        }
        if (anyServicesInvolved) {
            if (!queryReplace) {
                disableNonexistentServices();
            }
            rebindServices();
        }
    }

    public void onUserSwitched() {
        if (this.DEBUG) {
            Slog.d(this.TAG, "onUserSwitched");
        }
        if (Arrays.equals(this.mLastSeenProfileIds, this.mUserProfiles.getCurrentProfileIds())) {
            if (this.DEBUG) {
                Slog.d(this.TAG, "Current profile IDs didn't change, skipping rebindServices().");
                return;
            }
            return;
        }
        rebindServices();
    }

    public ManagedServiceInfo checkServiceTokenLocked(IInterface service) {
        checkNotNull(service);
        IBinder token = service.asBinder();
        int N = this.mServices.size();
        for (int i = 0; i < N; i++) {
            ManagedServiceInfo info = this.mServices.get(i);
            if (info.service.asBinder() == token) {
                return info;
            }
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
        if (info != null) {
            onServiceAdded(info);
        }
    }

    private void disableNonexistentServices() {
        int[] userIds = this.mUserProfiles.getCurrentProfileIds();
        for (int i : userIds) {
            disableNonexistentServices(i);
        }
    }

    private void disableNonexistentServices(int userId) {
        String flatIn = Settings.Secure.getStringForUser(this.mContext.getContentResolver(), this.mConfig.secureSettingName, userId);
        if (!TextUtils.isEmpty(flatIn)) {
            if (this.DEBUG) {
                Slog.v(this.TAG, "flat before: " + flatIn);
            }
            PackageManager pm = this.mContext.getPackageManager();
            List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(new Intent(this.mConfig.serviceInterface), 132, userId);
            if (this.DEBUG) {
                Slog.v(this.TAG, this.mConfig.serviceInterface + " services: " + installedServices);
            }
            Set<ComponentName> installed = new ArraySet<>();
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ResolveInfo resolveInfo = installedServices.get(i);
                ServiceInfo info = resolveInfo.serviceInfo;
                if (!this.mConfig.bindPermission.equals(info.permission)) {
                    Slog.w(this.TAG, "Skipping " + getCaption() + " service " + info.packageName + "/" + info.name + ": it does not require the permission " + this.mConfig.bindPermission);
                } else {
                    installed.add(new ComponentName(info.packageName, info.name));
                }
            }
            String flatOut = "";
            if (!installed.isEmpty()) {
                String[] enabled = flatIn.split(ENABLED_SERVICES_SEPARATOR);
                ArrayList<String> remaining = new ArrayList<>(enabled.length);
                for (int i2 = 0; i2 < enabled.length; i2++) {
                    ComponentName enabledComponent = ComponentName.unflattenFromString(enabled[i2]);
                    if (installed.contains(enabledComponent)) {
                        remaining.add(enabled[i2]);
                    }
                }
                flatOut = TextUtils.join(ENABLED_SERVICES_SEPARATOR, remaining);
            }
            if (this.DEBUG) {
                Slog.v(this.TAG, "flat after: " + flatOut);
            }
            if (!flatIn.equals(flatOut)) {
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), this.mConfig.secureSettingName, flatOut, userId);
            }
        }
    }

    private void rebindServices() {
        if (this.DEBUG) {
            Slog.d(this.TAG, "rebindServices");
        }
        int[] userIds = this.mUserProfiles.getCurrentProfileIds();
        int nUserIds = userIds.length;
        SparseArray<String> flat = new SparseArray<>();
        for (int i = 0; i < nUserIds; i++) {
            flat.put(userIds[i], Settings.Secure.getStringForUser(this.mContext.getContentResolver(), this.mConfig.secureSettingName, userIds[i]));
        }
        ArrayList<ManagedServiceInfo> toRemove = new ArrayList<>();
        SparseArray<ArrayList<ComponentName>> toAdd = new SparseArray<>();
        synchronized (this.mMutex) {
            for (ManagedServiceInfo service : this.mServices) {
                if (!service.isSystem) {
                    toRemove.add(service);
                }
            }
            ArraySet<ComponentName> newEnabled = new ArraySet<>();
            ArraySet<String> newPackages = new ArraySet<>();
            for (int i2 = 0; i2 < nUserIds; i2++) {
                ArrayList<ComponentName> add = new ArrayList<>();
                toAdd.put(userIds[i2], add);
                String toDecode = flat.get(userIds[i2]);
                if (toDecode != null) {
                    String[] components = toDecode.split(ENABLED_SERVICES_SEPARATOR);
                    for (String str : components) {
                        ComponentName component = ComponentName.unflattenFromString(str);
                        if (component != null) {
                            newEnabled.add(component);
                            add.add(component);
                            newPackages.add(component.getPackageName());
                        }
                    }
                }
            }
            this.mEnabledServicesForCurrentProfiles = newEnabled;
            this.mEnabledServicesPackageNames = newPackages;
        }
        for (ManagedServiceInfo info : toRemove) {
            ComponentName component2 = info.component;
            int oldUser = info.userid;
            Slog.v(this.TAG, "disabling " + getCaption() + " for user " + oldUser + ": " + component2);
            unregisterService(component2, info.userid);
        }
        for (int i3 = 0; i3 < nUserIds; i3++) {
            ArrayList<ComponentName> add2 = toAdd.get(userIds[i3]);
            int N = add2.size();
            for (int j = 0; j < N; j++) {
                ComponentName component3 = add2.get(j);
                Slog.v(this.TAG, "enabling " + getCaption() + " for user " + userIds[i3] + ": " + component3);
                registerService(component3, userIds[i3]);
            }
        }
        this.mLastSeenProfileIds = this.mUserProfiles.getCurrentProfileIds();
    }

    private void registerService(ComponentName name, final int userid) {
        if (this.DEBUG) {
            Slog.v(this.TAG, "registerService: " + name + " u=" + userid);
        }
        synchronized (this.mMutex) {
            final String servicesBindingTag = name.toString() + "/" + userid;
            if (!this.mServicesBinding.contains(servicesBindingTag)) {
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
                if (BenesseExtension.getDchaState() == 0) {
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
                    if (!this.mContext.bindServiceAsUser(intent, new ServiceConnection() {
                        IInterface mService;

                        @Override
                        public void onServiceConnected(ComponentName name2, IBinder binder) {
                            boolean added = false;
                            ManagedServiceInfo info2 = null;
                            synchronized (ManagedServices.this.mMutex) {
                                ManagedServices.this.mServicesBinding.remove(servicesBindingTag);
                                try {
                                    this.mService = ManagedServices.this.asInterface(binder);
                                    info2 = ManagedServices.this.newServiceInfo(this.mService, name2, userid, false, this, targetSdkVersion);
                                    binder.linkToDeath(info2, 0);
                                    added = ManagedServices.this.mServices.add(info2);
                                } catch (RemoteException e2) {
                                }
                            }
                            if (added) {
                                ManagedServices.this.onServiceAdded(info2);
                            }
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name2) {
                            Slog.v(ManagedServices.this.TAG, ManagedServices.this.getCaption() + " connection lost: " + name2);
                        }
                    }, 1, new UserHandle(userid))) {
                        this.mServicesBinding.remove(servicesBindingTag);
                        Slog.w(this.TAG, "Unable to bind " + getCaption() + " service: " + intent);
                    }
                } catch (SecurityException ex) {
                    Slog.e(this.TAG, "Unable to bind " + getCaption() + " service: " + intent, ex);
                }
            }
        }
    }

    private void unregisterService(ComponentName name, int userid) {
        synchronized (this.mMutex) {
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
        if (service == null) {
            throw new IllegalArgumentException(getCaption() + " must not be null");
        }
    }

    private ManagedServiceInfo registerServiceImpl(IInterface service, ComponentName component, int userid) {
        ManagedServiceInfo info;
        synchronized (this.mMutex) {
            try {
                info = newServiceInfo(service, component, userid, true, null, 21);
                service.asBinder().linkToDeath(info, 0);
                this.mServices.add(info);
            } catch (RemoteException e) {
                info = null;
            }
        }
        return info;
    }

    private void unregisterServiceImpl(IInterface service, int userid) {
        ManagedServiceInfo info = removeServiceImpl(service, userid);
        if (info != null && info.connection != null) {
            this.mContext.unbindService(info.connection);
        }
    }

    private class SettingsObserver extends ContentObserver {
        private final Uri mSecureSettingsUri;

        private SettingsObserver(Handler handler) {
            super(handler);
            this.mSecureSettingsUri = Settings.Secure.getUriFor(ManagedServices.this.mConfig.secureSettingName);
        }

        private void observe() {
            ContentResolver resolver = ManagedServices.this.mContext.getContentResolver();
            resolver.registerContentObserver(this.mSecureSettingsUri, false, this, -1);
            update(null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        private void update(Uri uri) {
            if (uri == null || this.mSecureSettingsUri.equals(uri)) {
                if (ManagedServices.this.DEBUG) {
                    Slog.d(ManagedServices.this.TAG, "Setting changed: mSecureSettingsUri=" + this.mSecureSettingsUri + " / uri=" + uri);
                }
                ManagedServices.this.rebindServices();
            }
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

        public String toString() {
            return "ManagedServiceInfo[component=" + this.component + ",userid=" + this.userid + ",isSystem=" + this.isSystem + ",targetSdkVersion=" + this.targetSdkVersion + ",connection=" + (this.connection == null ? null : "<connection>") + ",service=" + this.service + ']';
        }

        public boolean enabledAndUserMatches(int nid) {
            if (!isEnabledForCurrentProfiles()) {
                return false;
            }
            if (this.userid == -1 || nid == -1 || nid == this.userid) {
                return true;
            }
            return supportsProfiles() && ManagedServices.this.mUserProfiles.isCurrentProfile(nid);
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

    public static class UserProfiles {
        private final SparseArray<UserInfo> mCurrentProfiles = new SparseArray<>();

        public void updateCache(Context context) {
            UserManager userManager = (UserManager) context.getSystemService("user");
            if (userManager != null) {
                int currentUserId = ActivityManager.getCurrentUser();
                List<UserInfo> profiles = userManager.getProfiles(currentUserId);
                synchronized (this.mCurrentProfiles) {
                    this.mCurrentProfiles.clear();
                    for (UserInfo user : profiles) {
                        this.mCurrentProfiles.put(user.id, user);
                    }
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

    protected static class Config {
        String bindPermission;
        String caption;
        int clientLabel;
        String secureSettingName;
        String serviceInterface;
        String settingsAction;

        protected Config() {
        }
    }
}
