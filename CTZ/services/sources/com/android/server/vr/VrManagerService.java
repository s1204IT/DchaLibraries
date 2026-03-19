package com.android.server.vr;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.NotificationManager;
import android.app.Vr2dDisplayProperties;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.vr.IPersistentVrStateCallbacks;
import android.service.vr.IVrListener;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.util.DumpUtils;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.utils.ManagedApplicationService;
import com.android.server.vr.EnabledComponentsObserver;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;

public class VrManagerService extends SystemService implements EnabledComponentsObserver.EnabledComponentChangeListener, ActivityManagerInternal.ScreenObserver {
    static final boolean DBG = false;
    private static final int EVENT_LOG_SIZE = 64;
    private static final int FLAG_ALL = 7;
    private static final int FLAG_AWAKE = 1;
    private static final int FLAG_KEYGUARD_UNLOCKED = 4;
    private static final int FLAG_NONE = 0;
    private static final int FLAG_SCREEN_ON = 2;
    private static final int INVALID_APPOPS_MODE = -1;
    private static final int MSG_PENDING_VR_STATE_CHANGE = 1;
    private static final int MSG_PERSISTENT_VR_MODE_STATE_CHANGE = 2;
    private static final int MSG_VR_STATE_CHANGE = 0;
    private static final int PENDING_STATE_DELAY_MS = 300;
    public static final String TAG = "VrManagerService";
    private static final ManagedApplicationService.BinderChecker sBinderChecker = new ManagedApplicationService.BinderChecker() {
        @Override
        public IInterface asInterface(IBinder iBinder) {
            return IVrListener.Stub.asInterface(iBinder);
        }

        @Override
        public boolean checkType(IInterface iInterface) {
            return iInterface instanceof IVrListener;
        }
    };
    private boolean mBootsToVr;
    private EnabledComponentsObserver mComponentObserver;
    private Context mContext;
    private ManagedApplicationService mCurrentVrCompositorService;
    private ComponentName mCurrentVrModeComponent;
    private int mCurrentVrModeUser;
    private ManagedApplicationService mCurrentVrService;
    private ComponentName mDefaultVrService;
    private final ManagedApplicationService.EventCallback mEventCallback;
    private boolean mGuard;
    private final Handler mHandler;
    private final Object mLock;
    private boolean mLogLimitHit;
    private final ArrayDeque<ManagedApplicationService.LogFormattable> mLoggingDeque;
    private final NotificationAccessManager mNotifAccessManager;
    private INotificationManager mNotificationManager;
    private final IBinder mOverlayToken;
    private VrState mPendingState;
    private boolean mPersistentVrModeEnabled;
    private final RemoteCallbackList<IPersistentVrStateCallbacks> mPersistentVrStateRemoteCallbacks;
    private int mPreviousCoarseLocationMode;
    private int mPreviousManageOverlayMode;
    private boolean mRunning2dInVr;
    private boolean mStandby;
    private int mSystemSleepFlags;
    private boolean mUseStandbyToExitVrMode;
    private boolean mUserUnlocked;
    private Vr2dDisplay mVr2dDisplay;
    private int mVrAppProcessId;
    private final IVrManager mVrManager;
    private boolean mVrModeAllowed;
    private boolean mVrModeEnabled;
    private final RemoteCallbackList<IVrStateCallbacks> mVrStateRemoteCallbacks;
    private boolean mWasDefaultGranted;

    private static native void initializeNative();

    private static native void setVrModeNative(boolean z);

    private void updateVrModeAllowedLocked() {
        VrState vrState;
        boolean z = (this.mSystemSleepFlags == 7 || (this.mBootsToVr && this.mUseStandbyToExitVrMode)) && this.mUserUnlocked && !(this.mStandby && this.mUseStandbyToExitVrMode);
        if (this.mVrModeAllowed != z) {
            this.mVrModeAllowed = z;
            if (this.mVrModeAllowed) {
                if (this.mBootsToVr) {
                    setPersistentVrModeEnabled(true);
                }
                if (this.mBootsToVr && !this.mVrModeEnabled) {
                    setVrMode(true, this.mDefaultVrService, 0, -1, null);
                    return;
                }
                return;
            }
            setPersistentModeAndNotifyListenersLocked(false);
            if (this.mVrModeEnabled && this.mCurrentVrService != null) {
                vrState = new VrState(this.mVrModeEnabled, this.mRunning2dInVr, this.mCurrentVrService.getComponent(), this.mCurrentVrService.getUserId(), this.mVrAppProcessId, this.mCurrentVrModeComponent);
            } else {
                vrState = null;
            }
            this.mPendingState = vrState;
            updateCurrentVrServiceLocked(false, false, null, 0, -1, null);
        }
    }

    private void setScreenOn(boolean z) {
        setSystemState(2, z);
    }

    public void onAwakeStateChanged(boolean z) {
        setSystemState(1, z);
    }

    public void onKeyguardStateChanged(boolean z) {
        setSystemState(4, !z);
    }

    private void setSystemState(int i, boolean z) {
        synchronized (this.mLock) {
            int i2 = this.mSystemSleepFlags;
            if (z) {
                this.mSystemSleepFlags = i | this.mSystemSleepFlags;
            } else {
                this.mSystemSleepFlags = (~i) & this.mSystemSleepFlags;
            }
            if (i2 != this.mSystemSleepFlags) {
                updateVrModeAllowedLocked();
            }
        }
    }

    private String getStateAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append((this.mSystemSleepFlags & 1) != 0 ? "awake, " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        sb.append((this.mSystemSleepFlags & 2) != 0 ? "screen_on, " : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        sb.append((this.mSystemSleepFlags & 4) != 0 ? "keyguard_off" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        return sb.toString();
    }

    private void setUserUnlocked() {
        synchronized (this.mLock) {
            this.mUserUnlocked = true;
            updateVrModeAllowedLocked();
        }
    }

    private void setStandbyEnabled(boolean z) {
        synchronized (this.mLock) {
            if (!this.mBootsToVr) {
                Slog.e(TAG, "Attempting to set standby mode on a non-standalone device");
            } else {
                this.mStandby = z;
                updateVrModeAllowedLocked();
            }
        }
    }

    private static class SettingEvent implements ManagedApplicationService.LogFormattable {
        public final long timestamp = System.currentTimeMillis();
        public final String what;

        SettingEvent(String str) {
            this.what = str;
        }

        @Override
        public String toLogString(SimpleDateFormat simpleDateFormat) {
            return simpleDateFormat.format(new Date(this.timestamp)) + "   " + this.what;
        }
    }

    private static class VrState implements ManagedApplicationService.LogFormattable {
        final ComponentName callingPackage;
        final boolean defaultPermissionsGranted;
        final boolean enabled;
        final int processId;
        final boolean running2dInVr;
        final ComponentName targetPackageName;
        final long timestamp;
        final int userId;

        VrState(boolean z, boolean z2, ComponentName componentName, int i, int i2, ComponentName componentName2) {
            this.enabled = z;
            this.running2dInVr = z2;
            this.userId = i;
            this.processId = i2;
            this.targetPackageName = componentName;
            this.callingPackage = componentName2;
            this.defaultPermissionsGranted = false;
            this.timestamp = System.currentTimeMillis();
        }

        VrState(boolean z, boolean z2, ComponentName componentName, int i, int i2, ComponentName componentName2, boolean z3) {
            this.enabled = z;
            this.running2dInVr = z2;
            this.userId = i;
            this.processId = i2;
            this.targetPackageName = componentName;
            this.callingPackage = componentName2;
            this.defaultPermissionsGranted = z3;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toLogString(SimpleDateFormat simpleDateFormat) {
            StringBuilder sb = new StringBuilder(simpleDateFormat.format(new Date(this.timestamp)));
            sb.append("  ");
            sb.append("State changed to:");
            sb.append("  ");
            sb.append(this.enabled ? "ENABLED" : "DISABLED");
            sb.append("\n");
            if (this.enabled) {
                sb.append("  ");
                sb.append("User=");
                sb.append(this.userId);
                sb.append("\n");
                sb.append("  ");
                sb.append("Current VR Activity=");
                sb.append(this.callingPackage == null ? "None" : this.callingPackage.flattenToString());
                sb.append("\n");
                sb.append("  ");
                sb.append("Bound VrListenerService=");
                sb.append(this.targetPackageName == null ? "None" : this.targetPackageName.flattenToString());
                sb.append("\n");
                if (this.defaultPermissionsGranted) {
                    sb.append("  ");
                    sb.append("Default permissions granted to the bound VrListenerService.");
                    sb.append("\n");
                }
            }
            return sb.toString();
        }
    }

    private final class NotificationAccessManager {
        private final SparseArray<ArraySet<String>> mAllowedPackages;
        private final ArrayMap<String, Integer> mNotificationAccessPackageToUserId;

        private NotificationAccessManager() {
            this.mAllowedPackages = new SparseArray<>();
            this.mNotificationAccessPackageToUserId = new ArrayMap<>();
        }

        public void update(Collection<String> collection) {
            int currentUser = ActivityManager.getCurrentUser();
            ArraySet<String> arraySet = this.mAllowedPackages.get(currentUser);
            if (arraySet == null) {
                arraySet = new ArraySet<>();
            }
            for (int size = this.mNotificationAccessPackageToUserId.size() - 1; size >= 0; size--) {
                int iIntValue = this.mNotificationAccessPackageToUserId.valueAt(size).intValue();
                if (iIntValue != currentUser) {
                    String strKeyAt = this.mNotificationAccessPackageToUserId.keyAt(size);
                    VrManagerService.this.revokeNotificationListenerAccess(strKeyAt, iIntValue);
                    VrManagerService.this.revokeNotificationPolicyAccess(strKeyAt);
                    VrManagerService.this.revokeCoarseLocationPermissionIfNeeded(strKeyAt, iIntValue);
                    this.mNotificationAccessPackageToUserId.removeAt(size);
                }
            }
            for (String str : arraySet) {
                if (!collection.contains(str)) {
                    VrManagerService.this.revokeNotificationListenerAccess(str, currentUser);
                    VrManagerService.this.revokeNotificationPolicyAccess(str);
                    VrManagerService.this.revokeCoarseLocationPermissionIfNeeded(str, currentUser);
                    this.mNotificationAccessPackageToUserId.remove(str);
                }
            }
            for (String str2 : collection) {
                if (!arraySet.contains(str2)) {
                    VrManagerService.this.grantNotificationPolicyAccess(str2);
                    VrManagerService.this.grantNotificationListenerAccess(str2, currentUser);
                    VrManagerService.this.grantCoarseLocationPermissionIfNeeded(str2, currentUser);
                    this.mNotificationAccessPackageToUserId.put(str2, Integer.valueOf(currentUser));
                }
            }
            arraySet.clear();
            arraySet.addAll(collection);
            this.mAllowedPackages.put(currentUser, arraySet);
        }
    }

    @Override
    public void onEnabledComponentChanged() {
        synchronized (this.mLock) {
            ArraySet<ComponentName> enabled = this.mComponentObserver.getEnabled(ActivityManager.getCurrentUser());
            ArraySet arraySet = new ArraySet();
            for (ComponentName componentName : enabled) {
                if (isDefaultAllowed(componentName.getPackageName())) {
                    arraySet.add(componentName.getPackageName());
                }
            }
            this.mNotifAccessManager.update(arraySet);
            if (this.mVrModeAllowed) {
                consumeAndApplyPendingStateLocked(false);
                if (this.mCurrentVrService == null) {
                    return;
                }
                updateCurrentVrServiceLocked(this.mVrModeEnabled, this.mRunning2dInVr, this.mCurrentVrService.getComponent(), this.mCurrentVrService.getUserId(), this.mVrAppProcessId, this.mCurrentVrModeComponent);
            }
        }
    }

    private void enforceCallerPermissionAnyOf(String... strArr) {
        for (String str : strArr) {
            if (this.mContext.checkCallingOrSelfPermission(str) == 0) {
                return;
            }
        }
        throw new SecurityException("Caller does not hold at least one of the permissions: " + Arrays.toString(strArr));
    }

    private final class LocalService extends VrManagerInternal {
        private LocalService() {
        }

        @Override
        public void setVrMode(boolean z, ComponentName componentName, int i, int i2, ComponentName componentName2) {
            VrManagerService.this.setVrMode(z, componentName, i, i2, componentName2);
        }

        @Override
        public void onScreenStateChanged(boolean z) {
            VrManagerService.this.setScreenOn(z);
        }

        @Override
        public boolean isCurrentVrListener(String str, int i) {
            return VrManagerService.this.isCurrentVrListener(str, i);
        }

        @Override
        public int hasVrPackage(ComponentName componentName, int i) {
            return VrManagerService.this.hasVrPackage(componentName, i);
        }

        @Override
        public void setPersistentVrModeEnabled(boolean z) {
            VrManagerService.this.setPersistentVrModeEnabled(z);
        }

        @Override
        public void setVr2dDisplayProperties(Vr2dDisplayProperties vr2dDisplayProperties) {
            VrManagerService.this.setVr2dDisplayProperties(vr2dDisplayProperties);
        }

        @Override
        public int getVr2dDisplayId() {
            return VrManagerService.this.getVr2dDisplayId();
        }

        @Override
        public void addPersistentVrModeStateListener(IPersistentVrStateCallbacks iPersistentVrStateCallbacks) {
            VrManagerService.this.addPersistentStateCallback(iPersistentVrStateCallbacks);
        }
    }

    public VrManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mOverlayToken = new Binder();
        this.mVrStateRemoteCallbacks = new RemoteCallbackList<>();
        this.mPersistentVrStateRemoteCallbacks = new RemoteCallbackList<>();
        this.mPreviousCoarseLocationMode = -1;
        this.mPreviousManageOverlayMode = -1;
        this.mLoggingDeque = new ArrayDeque<>(64);
        this.mNotifAccessManager = new NotificationAccessManager();
        this.mSystemSleepFlags = 5;
        this.mEventCallback = new ManagedApplicationService.EventCallback() {
            @Override
            public void onServiceEvent(ManagedApplicationService.LogEvent logEvent) {
                ComponentName component;
                VrManagerService.this.logEvent(logEvent);
                synchronized (VrManagerService.this.mLock) {
                    component = VrManagerService.this.mCurrentVrService == null ? null : VrManagerService.this.mCurrentVrService.getComponent();
                    if (component != null && component.equals(logEvent.component) && (logEvent.event == 2 || logEvent.event == 3)) {
                        VrManagerService.this.callFocusedActivityChangedLocked();
                    }
                }
                if (!VrManagerService.this.mBootsToVr && logEvent.event == 4) {
                    if (component == null || component.equals(logEvent.component)) {
                        Slog.e(VrManagerService.TAG, "VrListenerSevice has died permanently, leaving system VR mode.");
                        VrManagerService.this.setPersistentVrModeEnabled(false);
                    }
                }
            }
        };
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                boolean z;
                switch (message.what) {
                    case 0:
                        z = message.arg1 == 1;
                        int iBeginBroadcast = VrManagerService.this.mVrStateRemoteCallbacks.beginBroadcast();
                        while (iBeginBroadcast > 0) {
                            iBeginBroadcast--;
                            try {
                                VrManagerService.this.mVrStateRemoteCallbacks.getBroadcastItem(iBeginBroadcast).onVrStateChanged(z);
                            } catch (RemoteException e) {
                            }
                        }
                        VrManagerService.this.mVrStateRemoteCallbacks.finishBroadcast();
                        return;
                    case 1:
                        synchronized (VrManagerService.this.mLock) {
                            if (VrManagerService.this.mVrModeAllowed) {
                                VrManagerService.this.consumeAndApplyPendingStateLocked();
                            }
                            break;
                        }
                        return;
                    case 2:
                        z = message.arg1 == 1;
                        int iBeginBroadcast2 = VrManagerService.this.mPersistentVrStateRemoteCallbacks.beginBroadcast();
                        while (iBeginBroadcast2 > 0) {
                            iBeginBroadcast2--;
                            try {
                                VrManagerService.this.mPersistentVrStateRemoteCallbacks.getBroadcastItem(iBeginBroadcast2).onPersistentVrStateChanged(z);
                            } catch (RemoteException e2) {
                            }
                        }
                        VrManagerService.this.mPersistentVrStateRemoteCallbacks.finishBroadcast();
                        return;
                    default:
                        throw new IllegalStateException("Unknown message type: " + message.what);
                }
            }
        };
        this.mVrManager = new IVrManager.Stub() {
            public void registerListener(IVrStateCallbacks iVrStateCallbacks) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (iVrStateCallbacks != null) {
                    VrManagerService.this.addStateCallback(iVrStateCallbacks);
                    return;
                }
                throw new IllegalArgumentException("Callback binder object is null.");
            }

            public void unregisterListener(IVrStateCallbacks iVrStateCallbacks) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (iVrStateCallbacks != null) {
                    VrManagerService.this.removeStateCallback(iVrStateCallbacks);
                    return;
                }
                throw new IllegalArgumentException("Callback binder object is null.");
            }

            public void registerPersistentVrStateListener(IPersistentVrStateCallbacks iPersistentVrStateCallbacks) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (iPersistentVrStateCallbacks != null) {
                    VrManagerService.this.addPersistentStateCallback(iPersistentVrStateCallbacks);
                    return;
                }
                throw new IllegalArgumentException("Callback binder object is null.");
            }

            public void unregisterPersistentVrStateListener(IPersistentVrStateCallbacks iPersistentVrStateCallbacks) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                if (iPersistentVrStateCallbacks != null) {
                    VrManagerService.this.removePersistentStateCallback(iPersistentVrStateCallbacks);
                    return;
                }
                throw new IllegalArgumentException("Callback binder object is null.");
            }

            public boolean getVrModeState() {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                return VrManagerService.this.getVrMode();
            }

            public boolean getPersistentVrModeEnabled() {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER", "android.permission.ACCESS_VR_STATE");
                return VrManagerService.this.getPersistentVrMode();
            }

            public void setPersistentVrModeEnabled(boolean z) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
                VrManagerService.this.setPersistentVrModeEnabled(z);
            }

            public void setVr2dDisplayProperties(Vr2dDisplayProperties vr2dDisplayProperties) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
                VrManagerService.this.setVr2dDisplayProperties(vr2dDisplayProperties);
            }

            public int getVr2dDisplayId() {
                return VrManagerService.this.getVr2dDisplayId();
            }

            public void setAndBindCompositor(String str) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
                VrManagerService.this.setAndBindCompositor(str == null ? null : ComponentName.unflattenFromString(str));
            }

            public void setStandbyEnabled(boolean z) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.ACCESS_VR_MANAGER");
                VrManagerService.this.setStandbyEnabled(z);
            }

            public void setVrInputMethod(ComponentName componentName) {
                VrManagerService.this.enforceCallerPermissionAnyOf("android.permission.RESTRICTED_VR_ACCESS");
                ((InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class)).startVrInputMethodNoCheck(componentName);
            }

            protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
                String strFlattenToString;
                String strFlattenToString2;
                if (DumpUtils.checkDumpPermission(VrManagerService.this.mContext, VrManagerService.TAG, printWriter)) {
                    printWriter.println("********* Dump of VrManagerService *********");
                    StringBuilder sb = new StringBuilder();
                    sb.append("VR mode is currently: ");
                    sb.append(VrManagerService.this.mVrModeAllowed ? "allowed" : "disallowed");
                    printWriter.println(sb.toString());
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("Persistent VR mode is currently: ");
                    sb2.append(VrManagerService.this.mPersistentVrModeEnabled ? "enabled" : "disabled");
                    printWriter.println(sb2.toString());
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("Currently bound VR listener service: ");
                    if (VrManagerService.this.mCurrentVrService != null) {
                        strFlattenToString = VrManagerService.this.mCurrentVrService.getComponent().flattenToString();
                    } else {
                        strFlattenToString = "None";
                    }
                    sb3.append(strFlattenToString);
                    printWriter.println(sb3.toString());
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("Currently bound VR compositor service: ");
                    if (VrManagerService.this.mCurrentVrCompositorService != null) {
                        strFlattenToString2 = VrManagerService.this.mCurrentVrCompositorService.getComponent().flattenToString();
                    } else {
                        strFlattenToString2 = "None";
                    }
                    sb4.append(strFlattenToString2);
                    printWriter.println(sb4.toString());
                    printWriter.println("Previous state transitions:\n");
                    VrManagerService.this.dumpStateTransitions(printWriter);
                    printWriter.println("\n\nRemote Callbacks:");
                    int iBeginBroadcast = VrManagerService.this.mVrStateRemoteCallbacks.beginBroadcast();
                    while (true) {
                        int i = iBeginBroadcast - 1;
                        if (iBeginBroadcast <= 0) {
                            break;
                        }
                        printWriter.print("  ");
                        printWriter.print(VrManagerService.this.mVrStateRemoteCallbacks.getBroadcastItem(i));
                        if (i > 0) {
                            printWriter.println(",");
                        }
                        iBeginBroadcast = i;
                    }
                    VrManagerService.this.mVrStateRemoteCallbacks.finishBroadcast();
                    printWriter.println("\n\nPersistent Vr State Remote Callbacks:");
                    int iBeginBroadcast2 = VrManagerService.this.mPersistentVrStateRemoteCallbacks.beginBroadcast();
                    while (true) {
                        int i2 = iBeginBroadcast2 - 1;
                        if (iBeginBroadcast2 <= 0) {
                            break;
                        }
                        printWriter.print("  ");
                        printWriter.print(VrManagerService.this.mPersistentVrStateRemoteCallbacks.getBroadcastItem(i2));
                        if (i2 > 0) {
                            printWriter.println(",");
                        }
                        iBeginBroadcast2 = i2;
                    }
                    VrManagerService.this.mPersistentVrStateRemoteCallbacks.finishBroadcast();
                    printWriter.println("\n");
                    printWriter.println("Installed VrListenerService components:");
                    int i3 = VrManagerService.this.mCurrentVrModeUser;
                    ArraySet<ComponentName> installed = VrManagerService.this.mComponentObserver.getInstalled(i3);
                    if (installed == null || installed.size() == 0) {
                        printWriter.println("None");
                    } else {
                        for (ComponentName componentName : installed) {
                            printWriter.print("  ");
                            printWriter.println(componentName.flattenToString());
                        }
                    }
                    printWriter.println("Enabled VrListenerService components:");
                    ArraySet<ComponentName> enabled = VrManagerService.this.mComponentObserver.getEnabled(i3);
                    if (enabled == null || enabled.size() == 0) {
                        printWriter.println("None");
                    } else {
                        for (ComponentName componentName2 : enabled) {
                            printWriter.print("  ");
                            printWriter.println(componentName2.flattenToString());
                        }
                    }
                    printWriter.println("\n");
                    printWriter.println("********* End of VrManagerService Dump *********");
                }
            }
        };
    }

    @Override
    public void onStart() {
        synchronized (this.mLock) {
            initializeNative();
            this.mContext = getContext();
        }
        boolean z = false;
        this.mBootsToVr = SystemProperties.getBoolean("ro.boot.vr", false);
        if (this.mBootsToVr && SystemProperties.getBoolean("persist.vr.use_standby_to_exit_vr_mode", true)) {
            z = true;
        }
        this.mUseStandbyToExitVrMode = z;
        publishLocalService(VrManagerInternal.class, new LocalService());
        publishBinderService("vrmanager", this.mVrManager.asBinder());
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).registerScreenObserver(this);
            this.mNotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
            synchronized (this.mLock) {
                Looper mainLooper = Looper.getMainLooper();
                Handler handler = new Handler(mainLooper);
                ArrayList arrayList = new ArrayList();
                arrayList.add(this);
                this.mComponentObserver = EnabledComponentsObserver.build(this.mContext, handler, "enabled_vr_listeners", mainLooper, "android.permission.BIND_VR_LISTENER_SERVICE", "android.service.vr.VrListenerService", this.mLock, arrayList);
                this.mComponentObserver.rebuildAll();
            }
            ArraySet defaultVrComponents = SystemConfig.getInstance().getDefaultVrComponents();
            if (defaultVrComponents.size() > 0) {
                this.mDefaultVrService = (ComponentName) defaultVrComponents.valueAt(0);
            } else {
                Slog.i(TAG, "No default vr listener service found.");
            }
            this.mVr2dDisplay = new Vr2dDisplay((DisplayManager) getContext().getSystemService("display"), (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class), (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class), this.mVrManager);
            this.mVr2dDisplay.init(getContext(), this.mBootsToVr);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_UNLOCKED");
            getContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                        VrManagerService.this.setUserUnlocked();
                    }
                }
            }, intentFilter);
        }
    }

    @Override
    public void onStartUser(int i) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override
    public void onSwitchUser(int i) {
        FgThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                VrManagerService.lambda$onSwitchUser$0(this.f$0);
            }
        });
    }

    public static void lambda$onSwitchUser$0(VrManagerService vrManagerService) {
        synchronized (vrManagerService.mLock) {
            vrManagerService.mComponentObserver.onUsersChanged();
        }
    }

    @Override
    public void onStopUser(int i) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    @Override
    public void onCleanupUser(int i) {
        synchronized (this.mLock) {
            this.mComponentObserver.onUsersChanged();
        }
    }

    private void updateOverlayStateLocked(String str, int i, int i2) {
        String[] strArr;
        AppOpsManager appOpsManager = (AppOpsManager) getContext().getSystemService(AppOpsManager.class);
        if (i2 != i) {
            appOpsManager.setUserRestrictionForUser(24, false, this.mOverlayToken, null, i2);
        }
        if (str == null) {
            strArr = new String[0];
        } else {
            strArr = new String[]{str};
        }
        appOpsManager.setUserRestrictionForUser(24, this.mVrModeEnabled, this.mOverlayToken, strArr, i);
    }

    private void updateDependentAppOpsLocked(String str, int i, String str2, int i2) {
        if (Objects.equals(str, str2)) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            updateOverlayStateLocked(str, i, i2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean updateCurrentVrServiceLocked(boolean z, boolean z2, ComponentName componentName, int i, int i2, ComponentName componentName2) {
        String packageName;
        boolean z3;
        boolean z4;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            boolean z5 = this.mComponentObserver.isValid(componentName, i) == 0;
            boolean z6 = z5 && z;
            if (!this.mVrModeEnabled && !z6) {
                return z5;
            }
            if (this.mCurrentVrService != null) {
                packageName = this.mCurrentVrService.getComponent().getPackageName();
            } else {
                packageName = null;
            }
            int i3 = this.mCurrentVrModeUser;
            changeVrModeLocked(z6);
            if (!z6) {
                if (this.mCurrentVrService != null) {
                    Slog.i(TAG, "Leaving VR mode, disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                    this.mCurrentVrService.disconnect();
                    updateCompositorServiceLocked(-10000, null);
                    this.mCurrentVrService = null;
                    z3 = false;
                    z4 = false;
                }
                z4 = true;
                z3 = false;
            } else {
                if (this.mCurrentVrService != null) {
                    if (this.mCurrentVrService.disconnectIfNotMatching(componentName, i)) {
                        Slog.i(TAG, "VR mode component changed to " + componentName + ", disconnecting " + this.mCurrentVrService.getComponent() + " for user " + this.mCurrentVrService.getUserId());
                        updateCompositorServiceLocked(-10000, null);
                        createAndConnectService(componentName, i);
                    } else {
                        z4 = true;
                        z3 = false;
                    }
                } else {
                    createAndConnectService(componentName, i);
                }
                z3 = true;
                z4 = false;
            }
            if (((componentName2 != null || this.mPersistentVrModeEnabled) && !Objects.equals(componentName2, this.mCurrentVrModeComponent)) || this.mRunning2dInVr != z2) {
                z3 = true;
            }
            this.mCurrentVrModeComponent = componentName2;
            this.mRunning2dInVr = z2;
            this.mVrAppProcessId = i2;
            if (this.mCurrentVrModeUser != i) {
                this.mCurrentVrModeUser = i;
                z3 = true;
            }
            updateDependentAppOpsLocked(this.mCurrentVrService != null ? this.mCurrentVrService.getComponent().getPackageName() : null, this.mCurrentVrModeUser, packageName, i3);
            if (this.mCurrentVrService != null && z3) {
                callFocusedActivityChangedLocked();
            }
            if (!z4) {
                logStateLocked();
            }
            return z5;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void callFocusedActivityChangedLocked() {
        final ComponentName componentName = this.mCurrentVrModeComponent;
        final boolean z = this.mRunning2dInVr;
        final int i = this.mVrAppProcessId;
        this.mCurrentVrService.sendEvent(new ManagedApplicationService.PendingEvent() {
            @Override
            public void runEvent(IInterface iInterface) throws RemoteException {
                ((IVrListener) iInterface).focusedActivityChanged(componentName, z, i);
            }
        });
    }

    private boolean isDefaultAllowed(String str) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = this.mContext.getPackageManager().getApplicationInfo(str, 128);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            return false;
        }
        if (!applicationInfo.isSystemApp() && !applicationInfo.isUpdatedSystemApp()) {
            return false;
        }
        return true;
    }

    private void grantNotificationPolicyAccess(String str) {
        ((NotificationManager) this.mContext.getSystemService(NotificationManager.class)).setNotificationPolicyAccessGranted(str, true);
    }

    private void revokeNotificationPolicyAccess(String str) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        notificationManager.removeAutomaticZenRules(str);
        notificationManager.setNotificationPolicyAccessGranted(str, false);
    }

    private void grantNotificationListenerAccess(String str, int i) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        for (ComponentName componentName : EnabledComponentsObserver.loadComponentNames(this.mContext.getPackageManager(), i, "android.service.notification.NotificationListenerService", "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")) {
            if (Objects.equals(componentName.getPackageName(), str)) {
                notificationManager.setNotificationListenerAccessGrantedForUser(componentName, i, true);
            }
        }
    }

    private void revokeNotificationListenerAccess(String str, int i) {
        NotificationManager notificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        for (ComponentName componentName : notificationManager.getEnabledNotificationListeners(i)) {
            if (componentName != null && componentName.getPackageName().equals(str)) {
                notificationManager.setNotificationListenerAccessGrantedForUser(componentName, i, false);
            }
        }
    }

    private void grantCoarseLocationPermissionIfNeeded(String str, int i) {
        if (!isPermissionUserUpdated("android.permission.ACCESS_COARSE_LOCATION", str, i)) {
            try {
                this.mContext.getPackageManager().grantRuntimePermission(str, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(i));
            } catch (IllegalArgumentException e) {
                Slog.w(TAG, "Could not grant coarse location permission, package " + str + " was removed.");
            }
        }
    }

    private void revokeCoarseLocationPermissionIfNeeded(String str, int i) {
        if (!isPermissionUserUpdated("android.permission.ACCESS_COARSE_LOCATION", str, i)) {
            try {
                this.mContext.getPackageManager().revokeRuntimePermission(str, "android.permission.ACCESS_COARSE_LOCATION", new UserHandle(i));
            } catch (IllegalArgumentException e) {
                Slog.w(TAG, "Could not revoke coarse location permission, package " + str + " was removed.");
            }
        }
    }

    private boolean isPermissionUserUpdated(String str, String str2, int i) {
        return (this.mContext.getPackageManager().getPermissionFlags(str, str2, new UserHandle(i)) & 3) != 0;
    }

    private ArraySet<String> getNotificationListeners(ContentResolver contentResolver, int i) {
        String stringForUser = Settings.Secure.getStringForUser(contentResolver, "enabled_notification_listeners", i);
        ArraySet<String> arraySet = new ArraySet<>();
        if (stringForUser != null) {
            for (String str : stringForUser.split(":")) {
                if (!TextUtils.isEmpty(str)) {
                    arraySet.add(str);
                }
            }
        }
        return arraySet;
    }

    private static String formatSettings(Collection<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        }
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        for (String str : collection) {
            if (!BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS.equals(str)) {
                if (!z) {
                    sb.append(':');
                }
                sb.append(str);
                z = false;
            }
        }
        return sb.toString();
    }

    private void createAndConnectService(ComponentName componentName, int i) {
        this.mCurrentVrService = createVrListenerService(componentName, i);
        this.mCurrentVrService.connect();
        Slog.i(TAG, "Connecting " + componentName + " for user " + i);
    }

    private void changeVrModeLocked(boolean z) {
        if (this.mVrModeEnabled != z) {
            this.mVrModeEnabled = z;
            StringBuilder sb = new StringBuilder();
            sb.append("VR mode ");
            sb.append(this.mVrModeEnabled ? "enabled" : "disabled");
            Slog.i(TAG, sb.toString());
            setVrModeNative(this.mVrModeEnabled);
            onVrModeChangedLocked();
        }
    }

    private void onVrModeChangedLocked() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, this.mVrModeEnabled ? 1 : 0, 0));
    }

    private ManagedApplicationService createVrListenerService(ComponentName componentName, int i) {
        return ManagedApplicationService.build(this.mContext, componentName, i, R.string.network_partial_connectivity_detailed, BenesseExtension.getDchaState() == 0 ? "android.settings.VR_LISTENER_SETTINGS" : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, sBinderChecker, true, this.mBootsToVr ? 1 : 2, this.mHandler, this.mEventCallback);
    }

    private ManagedApplicationService createVrCompositorService(ComponentName componentName, int i) {
        return ManagedApplicationService.build(this.mContext, componentName, i, 0, null, null, true, this.mBootsToVr ? 1 : 3, this.mHandler, this.mEventCallback);
    }

    private void consumeAndApplyPendingStateLocked() {
        consumeAndApplyPendingStateLocked(true);
    }

    private void consumeAndApplyPendingStateLocked(boolean z) {
        if (this.mPendingState != null) {
            updateCurrentVrServiceLocked(this.mPendingState.enabled, this.mPendingState.running2dInVr, this.mPendingState.targetPackageName, this.mPendingState.userId, this.mPendingState.processId, this.mPendingState.callingPackage);
            this.mPendingState = null;
        } else if (z) {
            updateCurrentVrServiceLocked(false, false, null, 0, -1, null);
        }
    }

    private void logStateLocked() {
        logEvent(new VrState(this.mVrModeEnabled, this.mRunning2dInVr, this.mCurrentVrService == null ? null : this.mCurrentVrService.getComponent(), this.mCurrentVrModeUser, this.mVrAppProcessId, this.mCurrentVrModeComponent, this.mWasDefaultGranted));
    }

    private void logEvent(ManagedApplicationService.LogFormattable logFormattable) {
        synchronized (this.mLoggingDeque) {
            if (this.mLoggingDeque.size() == 64) {
                this.mLoggingDeque.removeFirst();
                this.mLogLimitHit = true;
            }
            this.mLoggingDeque.add(logFormattable);
        }
    }

    private void dumpStateTransitions(PrintWriter printWriter) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
        synchronized (this.mLoggingDeque) {
            if (this.mLoggingDeque.size() == 0) {
                printWriter.print("  ");
                printWriter.println("None");
            }
            if (this.mLogLimitHit) {
                printWriter.println("...");
            }
            Iterator<ManagedApplicationService.LogFormattable> it = this.mLoggingDeque.iterator();
            while (it.hasNext()) {
                printWriter.println(it.next().toLogString(simpleDateFormat));
            }
        }
    }

    private void setVrMode(boolean z, ComponentName componentName, int i, int i2, ComponentName componentName2) {
        boolean z2;
        ComponentName componentName3;
        synchronized (this.mLock) {
            if (!z) {
                try {
                    z2 = this.mPersistentVrModeEnabled;
                } catch (Throwable th) {
                    throw th;
                }
            }
            boolean z3 = !z && this.mPersistentVrModeEnabled;
            if (z3) {
                componentName3 = this.mDefaultVrService;
            } else {
                componentName3 = componentName;
            }
            VrState vrState = new VrState(z2, z3, componentName3, i, i2, componentName2);
            if (!this.mVrModeAllowed) {
                this.mPendingState = vrState;
                return;
            }
            if (!z2 && this.mCurrentVrService != null) {
                if (this.mPendingState == null) {
                    this.mHandler.sendEmptyMessageDelayed(1, 300L);
                }
                this.mPendingState = vrState;
            } else {
                this.mHandler.removeMessages(1);
                this.mPendingState = null;
                updateCurrentVrServiceLocked(z2, z3, componentName3, i, i2, componentName2);
            }
        }
    }

    private void setPersistentVrModeEnabled(boolean z) {
        synchronized (this.mLock) {
            setPersistentModeAndNotifyListenersLocked(z);
            if (!z) {
                setVrMode(false, null, 0, -1, null);
            }
        }
    }

    public void setVr2dDisplayProperties(Vr2dDisplayProperties vr2dDisplayProperties) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (this.mVr2dDisplay != null) {
                this.mVr2dDisplay.setVirtualDisplayProperties(vr2dDisplayProperties);
            } else {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
                Slog.w(TAG, "Vr2dDisplay is null!");
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private int getVr2dDisplayId() {
        if (this.mVr2dDisplay != null) {
            return this.mVr2dDisplay.getVirtualDisplayId();
        }
        Slog.w(TAG, "Vr2dDisplay is null!");
        return -1;
    }

    private void setAndBindCompositor(ComponentName componentName) {
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                updateCompositorServiceLocked(callingUserId, componentName);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void updateCompositorServiceLocked(int i, ComponentName componentName) {
        if (this.mCurrentVrCompositorService != null && this.mCurrentVrCompositorService.disconnectIfNotMatching(componentName, i)) {
            Slog.i(TAG, "Disconnecting compositor service: " + this.mCurrentVrCompositorService.getComponent());
            this.mCurrentVrCompositorService = null;
        }
        if (componentName != null && this.mCurrentVrCompositorService == null) {
            Slog.i(TAG, "Connecting compositor service: " + componentName);
            this.mCurrentVrCompositorService = createVrCompositorService(componentName, i);
            this.mCurrentVrCompositorService.connect();
        }
    }

    private void setPersistentModeAndNotifyListenersLocked(boolean z) {
        if (this.mPersistentVrModeEnabled == z) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Persistent VR mode ");
        sb.append(z ? "enabled" : "disabled");
        String string = sb.toString();
        Slog.i(TAG, string);
        logEvent(new SettingEvent(string));
        this.mPersistentVrModeEnabled = z;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, this.mPersistentVrModeEnabled ? 1 : 0, 0));
    }

    private int hasVrPackage(ComponentName componentName, int i) {
        int iIsValid;
        synchronized (this.mLock) {
            iIsValid = this.mComponentObserver.isValid(componentName, i);
        }
        return iIsValid;
    }

    private boolean isCurrentVrListener(String str, int i) {
        synchronized (this.mLock) {
            boolean z = false;
            if (this.mCurrentVrService == null) {
                return false;
            }
            if (this.mCurrentVrService.getComponent().getPackageName().equals(str) && i == this.mCurrentVrService.getUserId()) {
                z = true;
            }
            return z;
        }
    }

    private void addStateCallback(IVrStateCallbacks iVrStateCallbacks) {
        this.mVrStateRemoteCallbacks.register(iVrStateCallbacks);
    }

    private void removeStateCallback(IVrStateCallbacks iVrStateCallbacks) {
        this.mVrStateRemoteCallbacks.unregister(iVrStateCallbacks);
    }

    private void addPersistentStateCallback(IPersistentVrStateCallbacks iPersistentVrStateCallbacks) {
        this.mPersistentVrStateRemoteCallbacks.register(iPersistentVrStateCallbacks);
    }

    private void removePersistentStateCallback(IPersistentVrStateCallbacks iPersistentVrStateCallbacks) {
        this.mPersistentVrStateRemoteCallbacks.unregister(iPersistentVrStateCallbacks);
    }

    private boolean getVrMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mVrModeEnabled;
        }
        return z;
    }

    private boolean getPersistentVrMode() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mPersistentVrModeEnabled;
        }
        return z;
    }
}
