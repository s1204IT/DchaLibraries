package com.android.server.vr;

import android.R;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.vr.IVrListener;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.android.server.utils.ManagedApplicationService;
import com.android.server.vr.EnabledComponentsObserver;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class VrManagerService extends SystemService implements EnabledComponentsObserver.EnabledComponentChangeListener {
    private static final int EVENT_LOG_SIZE = 32;
    private static final int INVALID_APPOPS_MODE = -1;
    private static final int MSG_PENDING_VR_STATE_CHANGE = 1;
    private static final int MSG_VR_STATE_CHANGE = 0;
    private static final int PENDING_STATE_DELAY_MS = 300;
    public static final String TAG = "VrManagerService";
    public static final String VR_MANAGER_BINDER_SERVICE = "vrmanager";
    private static final ManagedApplicationService.BinderChecker sBinderChecker = new ManagedApplicationService.BinderChecker() {
        @Override
        public IInterface asInterface(IBinder binder) {
            return IVrListener.Stub.asInterface(binder);
        }

        @Override
        public boolean checkType(IInterface service) {
            return service instanceof IVrListener;
        }
    };
    private EnabledComponentsObserver mComponentObserver;
    private Context mContext;
    private ComponentName mCurrentVrModeComponent;
    private int mCurrentVrModeUser;
    private ManagedApplicationService mCurrentVrService;
    private boolean mGuard;
    private final Handler mHandler;
    private final Object mLock;
    private final ArrayDeque<VrState> mLoggingDeque;
    private final NotificationAccessManager mNotifAccessManager;
    private final IBinder mOverlayToken;
    private VrState mPendingState;
    private int mPreviousCoarseLocationMode;
    private int mPreviousManageOverlayMode;
    private final RemoteCallbackList<IVrStateCallbacks> mRemoteCallbacks;
    private final IVrManager mVrManager;
    private boolean mVrModeEnabled;
    private boolean mWasDefaultGranted;

    private static native void initializeNative();

    private static native void setVrModeNative(boolean z);

    private static class VrState {
        final ComponentName callingPackage;
        final boolean defaultPermissionsGranted;
        final boolean enabled;
        final ComponentName targetPackageName;
        final long timestamp;
        final int userId;

        VrState(boolean enabled, ComponentName targetPackageName, int userId, ComponentName callingPackage) {
            this.enabled = enabled;
            this.userId = userId;
            this.targetPackageName = targetPackageName;
            this.callingPackage = callingPackage;
            this.defaultPermissionsGranted = false;
            this.timestamp = System.currentTimeMillis();
        }

        VrState(boolean enabled, ComponentName targetPackageName, int userId, ComponentName callingPackage, boolean defaultPermissionsGranted) {
            this.enabled = enabled;
            this.userId = userId;
            this.targetPackageName = targetPackageName;
            this.callingPackage = callingPackage;
            this.defaultPermissionsGranted = defaultPermissionsGranted;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final class NotificationAccessManager {
        private final SparseArray<ArraySet<String>> mAllowedPackages;
        private final ArrayMap<String, Integer> mNotificationAccessPackageToUserId;

        NotificationAccessManager(VrManagerService this$0, NotificationAccessManager notificationAccessManager) {
            this();
        }

        private NotificationAccessManager() {
            this.mAllowedPackages = new SparseArray<>();
            this.mNotificationAccessPackageToUserId = new ArrayMap<>();
        }

        public void update(Collection<String> packageNames) {
            int currentUserId = ActivityManager.getCurrentUser();
            ArraySet<String> allowed = this.mAllowedPackages.get(currentUserId);
            if (allowed == null) {
                allowed = new ArraySet<>();
            }
            int listenerCount = this.mNotificationAccessPackageToUserId.size();
            for (int i = listenerCount - 1; i >= 0; i--) {
                int grantUserId = this.mNotificationAccessPackageToUserId.valueAt(i).intValue();
                if (grantUserId != currentUserId) {
                    String packageName = this.mNotificationAccessPackageToUserId.keyAt(i);
                    VrManagerService.this.revokeNotificationListenerAccess(packageName, grantUserId);
                    VrManagerService.this.revokeNotificationPolicyAccess(packageName);
                    this.mNotificationAccessPackageToUserId.removeAt(i);
                }
            }
            for (String pkg : allowed) {
                if (!packageNames.contains(pkg)) {
                    VrManagerService.this.revokeNotificationListenerAccess(pkg, currentUserId);
                    VrManagerService.this.revokeNotificationPolicyAccess(pkg);
                    this.mNotificationAccessPackageToUserId.remove(pkg);
                }
            }
            for (String pkg2 : packageNames) {
                if (!allowed.contains(pkg2)) {
                    VrManagerService.this.grantNotificationPolicyAccess(pkg2);
                    VrManagerService.this.grantNotificationListenerAccess(pkg2, currentUserId);
                    this.mNotificationAccessPackageToUserId.put(pkg2, Integer.valueOf(currentUserId));
                }
            }
            allowed.clear();
            allowed.addAll(packageNames);
            this.mAllowedPackages.put(currentUserId, allowed);
        }
    }

    @Override
    public void onEnabledComponentChanged() {
        synchronized (this.mLock) {
            int currentUser = ActivityManager.getCurrentUser();
            ArraySet<ComponentName> enabledListeners = this.mComponentObserver.getEnabled(currentUser);
            ArraySet<String> enabledPackages = new ArraySet<>();
            for (ComponentName n : enabledListeners) {
                String pkg = n.getPackageName();
                if (isDefaultAllowed(pkg)) {
                    enabledPackages.add(n.getPackageName());
                }
            }
            this.mNotifAccessManager.update(enabledPackages);
            if (this.mCurrentVrService == null) {
                return;
            }
            consumeAndApplyPendingStateLocked();
            if (this.mCurrentVrService == null) {
                return;
            }
            updateCurrentVrServiceLocked(this.mVrModeEnabled, this.mCurrentVrService.getComponent(), this.mCurrentVrService.getUserId(), null);
        }
    }

    private void enforceCallerPermission(String permission) {
        if (this.mContext.checkCallingOrSelfPermission(permission) == 0) {
        } else {
            throw new SecurityException("Caller does not hold the permission " + permission);
        }
    }

    private final class LocalService extends VrManagerInternal {
        LocalService(VrManagerService this$0, LocalService localService) {
            this();
        }

        private LocalService() {
        }

        @Override
        public void setVrMode(boolean enabled, ComponentName packageName, int userId, ComponentName callingPackage) {
            VrManagerService.this.setVrMode(enabled, packageName, userId, callingPackage, false);
        }

        @Override
        public void setVrModeImmediate(boolean enabled, ComponentName packageName, int userId, ComponentName callingPackage) {
            VrManagerService.this.setVrMode(enabled, packageName, userId, callingPackage, true);
        }

        @Override
        public boolean isCurrentVrListener(String packageName, int userId) {
            return VrManagerService.this.isCurrentVrListener(packageName, userId);
        }

        @Override
        public int hasVrPackage(ComponentName packageName, int userId) {
            return VrManagerService.this.hasVrPackage(packageName, userId);
        }
    }

    public VrManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mOverlayToken = new Binder();
        this.mRemoteCallbacks = new RemoteCallbackList<>();
        this.mPreviousCoarseLocationMode = -1;
        this.mPreviousManageOverlayMode = -1;
        this.mLoggingDeque = new ArrayDeque<>(32);
        this.mNotifAccessManager = new NotificationAccessManager(this, null);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        boolean state = msg.arg1 == 1;
                        int i = VrManagerService.this.mRemoteCallbacks.beginBroadcast();
                        while (i > 0) {
                            i--;
                            try {
                                VrManagerService.this.mRemoteCallbacks.getBroadcastItem(i).onVrStateChanged(state);
                            } catch (RemoteException e) {
                            }
                        }
                        VrManagerService.this.mRemoteCallbacks.finishBroadcast();
                        return;
                    case 1:
                        synchronized (VrManagerService.this.mLock) {
                            VrManagerService.this.consumeAndApplyPendingStateLocked();
                        }
                        return;
                    default:
                        throw new IllegalStateException("Unknown message type: " + msg.what);
                }
            }
        };
        this.mVrManager = new IVrManager.Stub() {
            public void registerListener(IVrStateCallbacks cb) {
                VrManagerService.this.enforceCallerPermission("android.permission.ACCESS_VR_MANAGER");
                if (cb == null) {
                    throw new IllegalArgumentException("Callback binder object is null.");
                }
                VrManagerService.this.addStateCallback(cb);
            }

            public void unregisterListener(IVrStateCallbacks cb) {
                VrManagerService.this.enforceCallerPermission("android.permission.ACCESS_VR_MANAGER");
                if (cb == null) {
                    throw new IllegalArgumentException("Callback binder object is null.");
                }
                VrManagerService.this.removeStateCallback(cb);
            }

            public boolean getVrModeState() {
                return VrManagerService.this.getVrMode();
            }

            protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
                if (VrManagerService.this.getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                    pw.println("permission denied: can't dump VrManagerService from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                    return;
                }
                pw.println("********* Dump of VrManagerService *********");
                pw.println("Previous state transitions:\n");
                VrManagerService.this.dumpStateTransitions(pw);
                pw.println("\n\nRemote Callbacks:");
                int i = VrManagerService.this.mRemoteCallbacks.beginBroadcast();
                while (true) {
                    int i2 = i;
                    i = i2 - 1;
                    if (i2 <= 0) {
                        break;
                    }
                    pw.print("  ");
                    pw.print(VrManagerService.this.mRemoteCallbacks.getBroadcastItem(i));
                    if (i > 0) {
                        pw.println(",");
                    }
                }
                VrManagerService.this.mRemoteCallbacks.finishBroadcast();
                pw.println("\n");
                pw.println("Installed VrListenerService components:");
                int userId = VrManagerService.this.mCurrentVrModeUser;
                ArraySet<ComponentName> installed = VrManagerService.this.mComponentObserver.getInstalled(userId);
                if (installed == null || installed.size() == 0) {
                    pw.println("None");
                } else {
                    for (ComponentName n : installed) {
                        pw.print("  ");
                        pw.println(n.flattenToString());
                    }
                }
                pw.println("Enabled VrListenerService components:");
                ArraySet<ComponentName> enabled = VrManagerService.this.mComponentObserver.getEnabled(userId);
                if (enabled == null || enabled.size() == 0) {
                    pw.println("None");
                } else {
                    for (ComponentName n2 : enabled) {
                        pw.print("  ");
                        pw.println(n2.flattenToString());
                    }
                }
                pw.println("\n");
                pw.println("********* End of VrManagerService Dump *********");
            }
        };
    }

    @Override
    public void onStart() {
        synchronized (this.mLock) {
            initializeNative();
            this.mContext = getContext();
        }
        publishLocalService(VrManagerInternal.class, new LocalService(this, null));
        publishBinderService(VR_MANAGER_BINDER_SERVICE, this.mVrManager.asBinder());
        setEnabledStatusOfVrComponents();
    }

    private void setEnabledStatusOfVrComponents() {
        PackageInfo packageInfo;
        final ArraySet<ComponentName> vrComponents = SystemConfig.getInstance().getDefaultVrComponents();
        if (vrComponents == null) {
            return;
        }
        final ArraySet<String> vrComponentPackageNames = new ArraySet<>();
        for (ComponentName componentName : vrComponents) {
            vrComponentPackageNames.add(componentName.getPackageName());
        }
        PackageManager pm = this.mContext.getPackageManager();
        List<PackageInfo> packageInfos = pm.getInstalledPackages(PackageManagerService.DumpState.DUMP_KEYSETS);
        boolean vrModeIsUsed = false;
        Iterator packageInfo$iterator = packageInfos.iterator();
        while (packageInfo$iterator.hasNext() && ((packageInfo = (PackageInfo) packageInfo$iterator.next()) == null || packageInfo.packageName == null || pm.getApplicationEnabledSetting(packageInfo.packageName) != 0 || !(vrModeIsUsed = enableVrComponentsIfVrModeUsed(pm, packageInfo, vrComponentPackageNames, vrComponents)))) {
        }
        if (vrModeIsUsed) {
            return;
        }
        Slog.i(TAG, "No VR packages found, disabling VR components");
        setVrComponentsEnabledOrDisabled(vrComponents, false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addDataScheme("package");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PackageManager pm2 = context.getPackageManager();
                String packageName = intent.getData().getSchemeSpecificPart();
                if (packageName == null) {
                    return;
                }
                try {
                    PackageInfo packageInfo2 = pm2.getPackageInfo(packageName, PackageManagerService.DumpState.DUMP_KEYSETS);
                    VrManagerService.this.enableVrComponentsIfVrModeUsed(pm2, packageInfo2, vrComponentPackageNames, vrComponents);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
        }, intentFilter);
    }

    private void setVrComponentsEnabledOrDisabled(ArraySet<ComponentName> vrComponents, boolean enabled) {
        int state;
        if (enabled) {
            state = 1;
        } else {
            state = 2;
        }
        PackageManager pm = this.mContext.getPackageManager();
        for (ComponentName componentName : vrComponents) {
            try {
                pm.getPackageInfo(componentName.getPackageName(), PackageManagerService.DumpState.DUMP_KEYSETS);
                pm.setApplicationEnabledSetting(componentName.getPackageName(), state, 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
    }

    private boolean enableVrComponentsIfVrModeUsed(PackageManager pm, PackageInfo packageInfo, ArraySet<String> vrComponentPackageNames, ArraySet<ComponentName> vrComponents) {
        boolean zContains;
        if (vrComponents == null) {
            zContains = false;
        } else {
            zContains = vrComponentPackageNames.contains(packageInfo.packageName);
        }
        if (packageInfo != null && packageInfo.reqFeatures != null && !zContains) {
            for (FeatureInfo featureInfo : packageInfo.reqFeatures) {
                if (featureInfo.name != null && (featureInfo.name.equals("android.software.vr.mode") || featureInfo.name.equals("android.hardware.vr.high_performance"))) {
                    Slog.i(TAG, "VR package found, enabling VR components");
                    setVrComponentsEnabledOrDisabled(vrComponents, true);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 500) {
            return;
        }
        synchronized (this.mLock) {
            Looper looper = Looper.getMainLooper();
            Handler handler = new Handler(looper);
            ArrayList<EnabledComponentsObserver.EnabledComponentChangeListener> listeners = new ArrayList<>();
            listeners.add(this);
            this.mComponentObserver = EnabledComponentsObserver.build(this.mContext, handler, "enabled_vr_listeners", looper, "android.permission.BIND_VR_LISTENER_SERVICE", "android.service.vr.VrListenerService", this.mLock, listeners);
            this.mComponentObserver.rebuildAll();
        }
    }

    @Override
    public void onStartUser(int userHandle) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override
    public void onSwitchUser(int userHandle) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override
    public void onStopUser(int userHandle) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override
    public void onCleanupUser(int userHandle) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    private void updateOverlayStateLocked(String exemptedPackage, int newUserId, int oldUserId) {
        AppOpsManager appOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
        if (oldUserId != newUserId) {
            appOpsManager.setUserRestrictionForUser(24, false, this.mOverlayToken, null, oldUserId);
        }
        String[] exemptions = exemptedPackage == null ? new String[0] : new String[]{exemptedPackage};
        appOpsManager.setUserRestrictionForUser(24, this.mVrModeEnabled, this.mOverlayToken, exemptions, newUserId);
    }

    private void updateDependentAppOpsLocked(String newVrServicePackage, int newUserId, String oldVrServicePackage, int oldUserId) {
        if (Objects.equals(newVrServicePackage, oldVrServicePackage)) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            updateOverlayStateLocked(newVrServicePackage, newUserId, oldUserId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean updateCurrentVrServiceLocked(boolean enabled, ComponentName component, int userId, ComponentName calling) {
        boolean sendUpdatedCaller = false;
        long identity = Binder.clearCallingIdentity();
        try {
            boolean validUserComponent = this.mComponentObserver.isValid(component, userId) == 0;
            if (!this.mVrModeEnabled && !enabled) {
                return validUserComponent;
            }
            String packageName = this.mCurrentVrService != null ? this.mCurrentVrService.getComponent().getPackageName() : null;
            int oldUserId = this.mCurrentVrModeUser;
            changeVrModeLocked(enabled);
            if (!enabled || !validUserComponent) {
                if (this.mCurrentVrService != null) {
                    Slog.i(TAG, "Disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                    this.mCurrentVrService.disconnect();
                    this.mCurrentVrService = null;
                }
            } else if (this.mCurrentVrService != null) {
                if (this.mCurrentVrService.disconnectIfNotMatching(component, userId)) {
                    Slog.i(TAG, "Disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                    createAndConnectService(component, userId);
                    sendUpdatedCaller = true;
                }
            } else {
                createAndConnectService(component, userId);
                sendUpdatedCaller = true;
            }
            if (calling != null && !Objects.equals(calling, this.mCurrentVrModeComponent)) {
                this.mCurrentVrModeComponent = calling;
                sendUpdatedCaller = true;
            }
            if (this.mCurrentVrModeUser != userId) {
                this.mCurrentVrModeUser = userId;
                sendUpdatedCaller = true;
            }
            String packageName2 = this.mCurrentVrService != null ? this.mCurrentVrService.getComponent().getPackageName() : null;
            int newUserId = this.mCurrentVrModeUser;
            updateDependentAppOpsLocked(packageName2, newUserId, packageName, oldUserId);
            if (this.mCurrentVrService != null && sendUpdatedCaller) {
                final ComponentName c = this.mCurrentVrModeComponent;
                this.mCurrentVrService.sendEvent(new ManagedApplicationService.PendingEvent() {
                    @Override
                    public void runEvent(IInterface service) throws RemoteException {
                        IVrListener l = (IVrListener) service;
                        l.focusedActivityChanged(c);
                    }
                });
            }
            logStateLocked();
            return validUserComponent;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private boolean isDefaultAllowed(String packageName) {
        PackageManager pm = this.mContext.getPackageManager();
        ApplicationInfo info = null;
        try {
            info = pm.getApplicationInfo(packageName, 128);
        } catch (PackageManager.NameNotFoundException e) {
        }
        if (info == null) {
            return false;
        }
        if (!info.isSystemApp() && !info.isUpdatedSystemApp()) {
            return false;
        }
        return true;
    }

    private void grantNotificationPolicyAccess(String pkg) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        nm.setNotificationPolicyAccessGranted(pkg, true);
    }

    private void revokeNotificationPolicyAccess(String pkg) {
        NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        nm.removeAutomaticZenRules(pkg);
        nm.setNotificationPolicyAccessGranted(pkg, false);
    }

    private void grantNotificationListenerAccess(String pkg, int userId) {
        PackageManager pm = this.mContext.getPackageManager();
        ArraySet<ComponentName> possibleServices = EnabledComponentsObserver.loadComponentNames(pm, userId, "android.service.notification.NotificationListenerService", "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE");
        ContentResolver resolver = this.mContext.getContentResolver();
        ArraySet<String> current = getNotificationListeners(resolver, userId);
        for (ComponentName c : possibleServices) {
            String flatName = c.flattenToString();
            if (Objects.equals(c.getPackageName(), pkg) && !current.contains(flatName)) {
                current.add(flatName);
            }
        }
        if (current.size() <= 0) {
            return;
        }
        String flatSettings = formatSettings(current);
        Settings.Secure.putStringForUser(resolver, "enabled_notification_listeners", flatSettings, userId);
    }

    private void revokeNotificationListenerAccess(String pkg, int userId) {
        ContentResolver resolver = this.mContext.getContentResolver();
        ArraySet<String> current = getNotificationListeners(resolver, userId);
        ArrayList<String> toRemove = new ArrayList<>();
        for (String c : current) {
            ComponentName component = ComponentName.unflattenFromString(c);
            if (component.getPackageName().equals(pkg)) {
                toRemove.add(c);
            }
        }
        current.removeAll(toRemove);
        String flatSettings = formatSettings(current);
        Settings.Secure.putStringForUser(resolver, "enabled_notification_listeners", flatSettings, userId);
    }

    private boolean isPermissionUserUpdated(String permission, String pkg, int userId) {
        int flags = this.mContext.getPackageManager().getPermissionFlags(permission, pkg, new UserHandle(userId));
        return (flags & 3) != 0;
    }

    private ArraySet<String> getNotificationListeners(ContentResolver resolver, int userId) {
        String flat = Settings.Secure.getStringForUser(resolver, "enabled_notification_listeners", userId);
        ArraySet<String> current = new ArraySet<>();
        if (flat != null) {
            String[] allowed = flat.split(":");
            for (String s : allowed) {
                current.add(s);
            }
        }
        return current;
    }

    private static String formatSettings(Collection<String> c) {
        if (c == null || c.isEmpty()) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        boolean start = true;
        for (String s : c) {
            if (!"".equals(s)) {
                if (!start) {
                    b.append(':');
                }
                b.append(s);
                start = false;
            }
        }
        return b.toString();
    }

    private void createAndConnectService(ComponentName component, int userId) {
        this.mCurrentVrService = create(this.mContext, component, userId);
        this.mCurrentVrService.connect();
        Slog.i(TAG, "Connecting " + component + " for user " + userId);
    }

    private void changeVrModeLocked(boolean enabled) {
        if (this.mVrModeEnabled == enabled) {
            return;
        }
        this.mVrModeEnabled = enabled;
        Slog.i(TAG, "VR mode " + (this.mVrModeEnabled ? "enabled" : "disabled"));
        setVrModeNative(this.mVrModeEnabled);
        onVrModeChangedLocked();
    }

    private void onVrModeChangedLocked() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, this.mVrModeEnabled ? 1 : 0, 0));
    }

    private static ManagedApplicationService create(Context context, ComponentName component, int userId) {
        return ManagedApplicationService.build(context, component, userId, R.string.forward_intent_to_work, BenesseExtension.getDchaState() == 0 ? "android.settings.VR_LISTENER_SETTINGS" : "", sBinderChecker);
    }

    private void consumeAndApplyPendingStateLocked() {
        if (this.mPendingState == null) {
            return;
        }
        updateCurrentVrServiceLocked(this.mPendingState.enabled, this.mPendingState.targetPackageName, this.mPendingState.userId, this.mPendingState.callingPackage);
        this.mPendingState = null;
    }

    private void logStateLocked() {
        VrState current = new VrState(this.mVrModeEnabled, this.mCurrentVrService == null ? null : this.mCurrentVrService.getComponent(), this.mCurrentVrModeUser, this.mCurrentVrModeComponent, this.mWasDefaultGranted);
        if (this.mLoggingDeque.size() == 32) {
            this.mLoggingDeque.removeFirst();
        }
        this.mLoggingDeque.add(current);
    }

    private void dumpStateTransitions(PrintWriter pw) {
        SimpleDateFormat d = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        if (this.mLoggingDeque.size() == 0) {
            pw.print("  ");
            pw.println("None");
        }
        for (VrState state : this.mLoggingDeque) {
            pw.print(d.format(new Date(state.timestamp)));
            pw.print("  ");
            pw.print("State changed to:");
            pw.print("  ");
            pw.println(state.enabled ? "ENABLED" : "DISABLED");
            if (state.enabled) {
                pw.print("  ");
                pw.print("User=");
                pw.println(state.userId);
                pw.print("  ");
                pw.print("Current VR Activity=");
                pw.println(state.callingPackage == null ? "None" : state.callingPackage.flattenToString());
                pw.print("  ");
                pw.print("Bound VrListenerService=");
                pw.println(state.targetPackageName == null ? "None" : state.targetPackageName.flattenToString());
                if (state.defaultPermissionsGranted) {
                    pw.print("  ");
                    pw.println("Default permissions granted to the bound VrListenerService.");
                }
            }
        }
    }

    private void setVrMode(boolean enabled, ComponentName targetPackageName, int userId, ComponentName callingPackage, boolean immediate) {
        synchronized (this.mLock) {
            if (!enabled) {
                if (this.mCurrentVrService != null && !immediate) {
                    if (this.mPendingState == null) {
                        this.mHandler.sendEmptyMessageDelayed(1, 300L);
                    }
                    this.mPendingState = new VrState(enabled, targetPackageName, userId, callingPackage);
                    return;
                }
            }
            this.mHandler.removeMessages(1);
            this.mPendingState = null;
            updateCurrentVrServiceLocked(enabled, targetPackageName, userId, callingPackage);
        }
    }

    private int hasVrPackage(ComponentName targetPackageName, int userId) {
        int iIsValid;
        synchronized (this.mLock) {
            iIsValid = this.mComponentObserver.isValid(targetPackageName, userId);
        }
        return iIsValid;
    }

    private boolean isCurrentVrListener(String packageName, int userId) {
        boolean z = false;
        synchronized (this.mLock) {
            if (this.mCurrentVrService == null) {
                return false;
            }
            if (this.mCurrentVrService.getComponent().getPackageName().equals(packageName)) {
                if (userId == this.mCurrentVrService.getUserId()) {
                    z = true;
                }
            }
            return z;
        }
    }

    private void addStateCallback(IVrStateCallbacks cb) {
        this.mRemoteCallbacks.register(cb);
    }

    private void removeStateCallback(IVrStateCallbacks cb) {
        this.mRemoteCallbacks.unregister(cb);
    }

    private boolean getVrMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mVrModeEnabled;
        }
        return z;
    }
}
