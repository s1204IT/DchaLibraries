package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ServiceStartArgs;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.SparseArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.webkit.WebViewZygote;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.server.AppStateTracker;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ServiceRecord;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usage.AppStandbyController;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ActiveServices {
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    private static final boolean LOG_SERVICE_START_STOP = false;
    static final int SERVICE_BACKGROUND_TIMEOUT = 200000;
    static final int SERVICE_START_FOREGROUND_TIMEOUT = 10000;
    static final int SERVICE_TIMEOUT = 20000;
    private static final boolean SHOW_DUNGEON_NOTIFICATION = false;
    private static final String TAG_MU = "ActivityManager_MU";
    final ActivityManagerService mAm;
    String mLastAnrDump;
    final int mMaxStartingBackground;
    private static final String TAG = "ActivityManager";
    private static final String TAG_SERVICE = TAG + ActivityManagerDebugConfig.POSTFIX_SERVICE;
    private static final String TAG_SERVICE_EXECUTING = TAG + ActivityManagerDebugConfig.POSTFIX_SERVICE_EXECUTING;
    private static final boolean DEBUG_DELAYED_SERVICE = ActivityManagerDebugConfig.DEBUG_SERVICE;
    private static final boolean DEBUG_DELAYED_STARTS = DEBUG_DELAYED_SERVICE;
    final SparseArray<ServiceMap> mServiceMap = new SparseArray<>();
    final ArrayMap<IBinder, ArrayList<ConnectionRecord>> mServiceConnections = new ArrayMap<>();
    final ArrayList<ServiceRecord> mPendingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mRestartingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mDestroyingServices = new ArrayList<>();
    private ArrayList<ServiceRecord> mTmpCollectionResults = null;
    boolean mScreenOn = true;
    final Runnable mLastAnrDumpClearer = new Runnable() {
        @Override
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActiveServices.this.mLastAnrDump = null;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    };

    class ForcedStandbyListener extends AppStateTracker.Listener {
        ForcedStandbyListener() {
        }

        @Override
        public void stopForegroundServicesForUidPackage(int i, String str) {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ServiceMap serviceMapLocked = ActiveServices.this.getServiceMapLocked(UserHandle.getUserId(i));
                    int size = serviceMapLocked.mServicesByName.size();
                    ArrayList arrayList = new ArrayList(size);
                    for (int i2 = 0; i2 < size; i2++) {
                        ServiceRecord serviceRecordValueAt = serviceMapLocked.mServicesByName.valueAt(i2);
                        if ((i == serviceRecordValueAt.serviceInfo.applicationInfo.uid || str.equals(serviceRecordValueAt.serviceInfo.packageName)) && serviceRecordValueAt.isForeground) {
                            arrayList.add(serviceRecordValueAt);
                        }
                    }
                    int size2 = arrayList.size();
                    if (size2 > 0 && ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.i(ActiveServices.TAG, "Package " + str + SliceClientPermissions.SliceAuthority.DELIMITER + i + " entering FAS with foreground services");
                    }
                    for (int i3 = 0; i3 < size2; i3++) {
                        ServiceRecord serviceRecord = (ServiceRecord) arrayList.get(i3);
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            Slog.i(ActiveServices.TAG, "  Stopping fg for service " + serviceRecord);
                        }
                        ActiveServices.this.setServiceForegroundInnerLocked(serviceRecord, 0, null, 0);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    static final class ActiveForegroundApp {
        boolean mAppOnTop;
        long mEndTime;
        long mHideTime;
        CharSequence mLabel;
        int mNumActive;
        String mPackageName;
        boolean mShownWhileScreenOn;
        boolean mShownWhileTop;
        long mStartTime;
        long mStartVisibleTime;
        int mUid;

        ActiveForegroundApp() {
        }
    }

    final class ServiceMap extends Handler {
        static final int MSG_BG_START_TIMEOUT = 1;
        static final int MSG_UPDATE_FOREGROUND_APPS = 2;
        final ArrayMap<String, ActiveForegroundApp> mActiveForegroundApps;
        boolean mActiveForegroundAppsChanged;
        final ArrayList<ServiceRecord> mDelayedStartList;
        final ArrayMap<Intent.FilterComparison, ServiceRecord> mServicesByIntent;
        final ArrayMap<ComponentName, ServiceRecord> mServicesByName;
        final ArrayList<ServiceRecord> mStartingBackground;
        final int mUserId;

        ServiceMap(Looper looper, int i) {
            super(looper);
            this.mServicesByName = new ArrayMap<>();
            this.mServicesByIntent = new ArrayMap<>();
            this.mDelayedStartList = new ArrayList<>();
            this.mStartingBackground = new ArrayList<>();
            this.mActiveForegroundApps = new ArrayMap<>();
            this.mUserId = i;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            rescheduleDelayedStartsLocked();
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case 2:
                    ActiveServices.this.updateForegroundApps(this);
                    return;
                default:
                    return;
            }
        }

        void ensureNotStartingBackgroundLocked(ServiceRecord serviceRecord) {
            if (this.mStartingBackground.remove(serviceRecord)) {
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    Slog.v(ActiveServices.TAG_SERVICE, "No longer background starting: " + serviceRecord);
                }
                rescheduleDelayedStartsLocked();
            }
            if (this.mDelayedStartList.remove(serviceRecord) && ActiveServices.DEBUG_DELAYED_STARTS) {
                Slog.v(ActiveServices.TAG_SERVICE, "No longer delaying start: " + serviceRecord);
            }
        }

        void rescheduleDelayedStartsLocked() {
            removeMessages(1);
            long jUptimeMillis = SystemClock.uptimeMillis();
            int size = this.mStartingBackground.size();
            int i = 0;
            while (i < size) {
                ServiceRecord serviceRecord = this.mStartingBackground.get(i);
                if (serviceRecord.startingBgTimeout <= jUptimeMillis) {
                    Slog.i(ActiveServices.TAG, "Waited long enough for: " + serviceRecord);
                    this.mStartingBackground.remove(i);
                    size += -1;
                    i += -1;
                }
                i++;
            }
            while (this.mDelayedStartList.size() > 0 && this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                ServiceRecord serviceRecordRemove = this.mDelayedStartList.remove(0);
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    Slog.v(ActiveServices.TAG_SERVICE, "REM FR DELAY LIST (exec next): " + serviceRecordRemove);
                }
                if (serviceRecordRemove.pendingStarts.size() <= 0) {
                    Slog.w(ActiveServices.TAG, "**** NO PENDING STARTS! " + serviceRecordRemove + " startReq=" + serviceRecordRemove.startRequested + " delayedStop=" + serviceRecordRemove.delayedStop);
                }
                if (ActiveServices.DEBUG_DELAYED_SERVICE && this.mDelayedStartList.size() > 0) {
                    Slog.v(ActiveServices.TAG_SERVICE, "Remaining delayed list:");
                    for (int i2 = 0; i2 < this.mDelayedStartList.size(); i2++) {
                        Slog.v(ActiveServices.TAG_SERVICE, "  #" + i2 + ": " + this.mDelayedStartList.get(i2));
                    }
                }
                serviceRecordRemove.delayed = false;
                try {
                    if (serviceRecordRemove.pendingStarts.size() > 0) {
                        ActiveServices.this.startServiceInnerLocked(this, serviceRecordRemove.pendingStarts.get(0).intent, serviceRecordRemove, false, true);
                    }
                } catch (TransactionTooLargeException e) {
                }
            }
            if (this.mStartingBackground.size() > 0) {
                ServiceRecord serviceRecord2 = this.mStartingBackground.get(0);
                if (serviceRecord2.startingBgTimeout > jUptimeMillis) {
                    jUptimeMillis = serviceRecord2.startingBgTimeout;
                }
                if (ActiveServices.DEBUG_DELAYED_SERVICE) {
                    Slog.v(ActiveServices.TAG_SERVICE, "Top bg start is " + serviceRecord2 + ", can delay others up to " + jUptimeMillis);
                }
                sendMessageAtTime(obtainMessage(1), jUptimeMillis);
            }
            if (this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                ActiveServices.this.mAm.backgroundServicesFinishedLocked(this.mUserId);
            }
        }
    }

    public ActiveServices(ActivityManagerService activityManagerService) {
        int i;
        int i2 = 1;
        this.mAm = activityManagerService;
        try {
            i = Integer.parseInt(SystemProperties.get("ro.config.max_starting_bg", "0"));
        } catch (RuntimeException e) {
            i = 0;
        }
        if (i > 0) {
            i2 = i;
        } else if (!ActivityManager.isLowRamDeviceStatic()) {
            i2 = 8;
        }
        this.mMaxStartingBackground = i2;
    }

    void systemServicesReady() {
        ((AppStateTracker) LocalServices.getService(AppStateTracker.class)).addListener(new ForcedStandbyListener());
    }

    ServiceRecord getServiceByNameLocked(ComponentName componentName, int i) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "getServiceByNameLocked(" + componentName + "), callingUser = " + i);
        }
        return getServiceMapLocked(i).mServicesByName.get(componentName);
    }

    boolean hasBackgroundServicesLocked(int i) {
        ServiceMap serviceMap = this.mServiceMap.get(i);
        return serviceMap != null && serviceMap.mStartingBackground.size() >= this.mMaxStartingBackground;
    }

    boolean hasForegroundServiceNotificationLocked(String str, int i, String str2) {
        ServiceMap serviceMap = this.mServiceMap.get(i);
        if (serviceMap != null) {
            for (int i2 = 0; i2 < serviceMap.mServicesByName.size(); i2++) {
                ServiceRecord serviceRecordValueAt = serviceMap.mServicesByName.valueAt(i2);
                if (serviceRecordValueAt.appInfo.packageName.equals(str) && serviceRecordValueAt.isForeground && Objects.equals(serviceRecordValueAt.foregroundNoti.getChannelId(), str2)) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG_SERVICE, "Channel u" + i + "/pkg=" + str + "/channelId=" + str2 + " has fg service notification");
                        return true;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    void stopForegroundServicesForChannelLocked(String str, int i, String str2) {
        ServiceMap serviceMap = this.mServiceMap.get(i);
        if (serviceMap != null) {
            for (int i2 = 0; i2 < serviceMap.mServicesByName.size(); i2++) {
                ServiceRecord serviceRecordValueAt = serviceMap.mServicesByName.valueAt(i2);
                if (serviceRecordValueAt.appInfo.packageName.equals(str) && serviceRecordValueAt.isForeground && Objects.equals(serviceRecordValueAt.foregroundNoti.getChannelId(), str2)) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG_SERVICE, "Stopping FGS u" + i + "/pkg=" + str + "/channelId=" + str2 + " for conversation channel clear");
                    }
                    stopServiceLocked(serviceRecordValueAt);
                }
            }
        }
    }

    private ServiceMap getServiceMapLocked(int i) {
        ServiceMap serviceMap = this.mServiceMap.get(i);
        if (serviceMap == null) {
            ServiceMap serviceMap2 = new ServiceMap(this.mAm.mHandler.getLooper(), i);
            this.mServiceMap.put(i, serviceMap2);
            return serviceMap2;
        }
        return serviceMap;
    }

    ArrayMap<ComponentName, ServiceRecord> getServicesLocked(int i) {
        return getServiceMapLocked(i).mServicesByName;
    }

    private boolean appRestrictedAnyInBackground(int i, String str) {
        return this.mAm.mAppOpsService.checkOperation(70, i, str) != 0;
    }

    ComponentName startServiceLocked(IApplicationThread iApplicationThread, Intent intent, String str, int i, int i2, boolean z, String str2, int i3) throws TransactionTooLargeException {
        String str3;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        boolean z9;
        boolean z10;
        int iCheckOperation;
        if (DEBUG_DELAYED_STARTS) {
            String str4 = TAG_SERVICE;
            StringBuilder sb = new StringBuilder();
            sb.append("startService: ");
            sb.append(intent);
            sb.append(" type=");
            str3 = str;
            sb.append(str3);
            sb.append(" args=");
            sb.append(intent.getExtras());
            Slog.v(str4, sb.toString());
        } else {
            str3 = str;
        }
        if (iApplicationThread != null) {
            ProcessRecord recordForAppLocked = this.mAm.getRecordForAppLocked(iApplicationThread);
            if (recordForAppLocked == null) {
                throw new SecurityException("Unable to find app for caller " + iApplicationThread + " (pid=" + i + ") when starting service " + intent);
            }
            z2 = recordForAppLocked.setSchedGroup != 0;
        } else {
            z2 = true;
        }
        boolean z11 = z2;
        ServiceLookupResult serviceLookupResultRetrieveServiceLocked = retrieveServiceLocked(intent, str3, str2, i, i2, i3, true, z2, false, false);
        if (serviceLookupResultRetrieveServiceLocked == null) {
            return null;
        }
        if (serviceLookupResultRetrieveServiceLocked.record == null) {
            return new ComponentName("!", serviceLookupResultRetrieveServiceLocked.permission != null ? serviceLookupResultRetrieveServiceLocked.permission : "private to package");
        }
        ServiceRecord serviceRecord = serviceLookupResultRetrieveServiceLocked.record;
        if (!this.mAm.mUserController.exists(serviceRecord.userId)) {
            Slog.w(TAG, "Trying to start service with non-existent user! " + serviceRecord.userId);
            return null;
        }
        boolean z12 = !this.mAm.isUidActiveLocked(serviceRecord.appInfo.uid);
        if (z12 && appRestrictedAnyInBackground(serviceRecord.appInfo.uid, serviceRecord.packageName)) {
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Forcing bg-only service start only for ");
                sb2.append(serviceRecord.shortName);
                sb2.append(" : bgLaunch=");
                sb2.append(z12);
                sb2.append(" callerFg=");
                z3 = z11;
                sb2.append(z3);
                Slog.d(TAG, sb2.toString());
            } else {
                z3 = z11;
            }
            z4 = true;
        } else {
            z3 = z11;
            z4 = false;
        }
        if (!z || (iCheckOperation = this.mAm.mAppOpsService.checkOperation(76, serviceRecord.appInfo.uid, serviceRecord.packageName)) == 3) {
            z5 = z;
            z6 = false;
        } else {
            switch (iCheckOperation) {
                case 0:
                    break;
                case 1:
                    Slog.w(TAG, "startForegroundService not allowed due to app op: service " + intent + " to " + serviceRecord.name.flattenToShortString() + " from pid=" + i + " uid=" + i2 + " pkg=" + str2);
                    z6 = true;
                    z5 = false;
                    break;
                default:
                    return new ComponentName("!!", "foreground not allowed as per app op");
            }
        }
        if (z4 || !(serviceRecord.startRequested || z5)) {
            z7 = z5;
            z8 = z3;
            int appStartModeLocked = this.mAm.getAppStartModeLocked(serviceRecord.appInfo.uid, serviceRecord.packageName, serviceRecord.appInfo.targetSdkVersion, i, false, false, z4);
            if (appStartModeLocked != 0) {
                Slog.w(TAG, "Background start not allowed: service " + intent + " to " + serviceRecord.name.flattenToShortString() + " from pid=" + i + " uid=" + i2 + " pkg=" + str2 + " startFg?=" + z7);
                if (appStartModeLocked == 1 || z6) {
                    return null;
                }
                if (z4 && z7) {
                    if (!ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                        return null;
                    }
                    Slog.v(TAG, "Silently dropping foreground service launch due to FAS");
                    return null;
                }
                return new ComponentName("?", "app is in background uid " + this.mAm.mActiveUids.get(serviceRecord.appInfo.uid));
            }
        } else {
            z7 = z5;
            z8 = z3;
        }
        if (serviceRecord.appInfo.targetSdkVersion < 26 && z7) {
            if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK || ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.i(TAG, "startForegroundService() but host targets " + serviceRecord.appInfo.targetSdkVersion + " - not requiring startForeground()");
            }
            z7 = false;
        }
        ActivityManagerService.NeededUriGrants neededUriGrantsCheckGrantUriPermissionFromIntentLocked = this.mAm.checkGrantUriPermissionFromIntentLocked(i2, serviceRecord.packageName, intent, intent.getFlags(), null, serviceRecord.userId);
        if (this.mAm.mPermissionReviewRequired && !requestStartTargetPermissionsReviewIfNeededLocked(serviceRecord, str2, i2, intent, z8, i3)) {
            return null;
        }
        if (unscheduleServiceRestartLocked(serviceRecord, i2, false) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "START SERVICE WHILE RESTART PENDING: " + serviceRecord);
        }
        serviceRecord.lastActivity = SystemClock.uptimeMillis();
        serviceRecord.startRequested = true;
        serviceRecord.delayedStop = false;
        serviceRecord.fgRequired = z7;
        serviceRecord.pendingStarts.add(new ServiceRecord.StartItem(serviceRecord, false, serviceRecord.makeNextStartId(), intent, neededUriGrantsCheckGrantUriPermissionFromIntentLocked, i2));
        if (z7) {
            this.mAm.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName, true);
        }
        ServiceMap serviceMapLocked = getServiceMapLocked(serviceRecord.userId);
        boolean z13 = z8;
        if (z13 || z7 || serviceRecord.app != null || !this.mAm.mUserController.hasStartedUserState(serviceRecord.userId)) {
            if (DEBUG_DELAYED_STARTS) {
                if (z13 || z7) {
                    Slog.v(TAG_SERVICE, "Not potential delay (callerFg=" + z13 + " uid=" + i2 + " pid=" + i + " fgRequired=" + z7 + "): " + serviceRecord);
                } else if (serviceRecord.app != null) {
                    Slog.v(TAG_SERVICE, "Not potential delay (cur app=" + serviceRecord.app + "): " + serviceRecord);
                } else {
                    Slog.v(TAG_SERVICE, "Not potential delay (user " + serviceRecord.userId + " not started): " + serviceRecord);
                }
            }
            z9 = false;
        } else {
            ProcessRecord processRecordLocked = this.mAm.getProcessRecordLocked(serviceRecord.processName, serviceRecord.appInfo.uid, false);
            if (processRecordLocked == null || processRecordLocked.curProcState > 10) {
                if (DEBUG_DELAYED_SERVICE) {
                    Slog.v(TAG_SERVICE, "Potential start delay of " + serviceRecord + " in " + processRecordLocked);
                }
                if (serviceRecord.delayed) {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "Continuing to delay: " + serviceRecord);
                    }
                    return serviceRecord.name;
                }
                if (serviceMapLocked.mStartingBackground.size() >= this.mMaxStartingBackground) {
                    Slog.i(TAG_SERVICE, "Delaying start of: " + serviceRecord);
                    serviceMapLocked.mDelayedStartList.add(serviceRecord);
                    serviceRecord.delayed = true;
                    return serviceRecord.name;
                }
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "Not delaying: " + serviceRecord);
                }
                z10 = true;
            } else if (processRecordLocked.curProcState >= 9) {
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "Not delaying, but counting as bg: " + serviceRecord);
                }
                z10 = true;
            } else {
                if (DEBUG_DELAYED_STARTS) {
                    StringBuilder sb3 = new StringBuilder(128);
                    sb3.append("Not potential delay (state=");
                    sb3.append(processRecordLocked.curProcState);
                    sb3.append(' ');
                    sb3.append(processRecordLocked.adjType);
                    String strMakeAdjReason = processRecordLocked.makeAdjReason();
                    if (strMakeAdjReason != null) {
                        sb3.append(' ');
                        sb3.append(strMakeAdjReason);
                    }
                    sb3.append("): ");
                    sb3.append(serviceRecord.toString());
                    Slog.v(TAG_SERVICE, sb3.toString());
                }
                z10 = false;
            }
            z9 = z10;
        }
        return startServiceInnerLocked(serviceMapLocked, intent, serviceRecord, z13, z9);
    }

    private boolean requestStartTargetPermissionsReviewIfNeededLocked(ServiceRecord serviceRecord, String str, int i, Intent intent, boolean z, final int i2) {
        if (!this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(serviceRecord.packageName, serviceRecord.userId)) {
            return true;
        }
        if (!z) {
            Slog.w(TAG, "u" + serviceRecord.userId + " Starting a service in package" + serviceRecord.packageName + " requires a permissions review");
            return false;
        }
        IIntentSender intentSenderLocked = this.mAm.getIntentSenderLocked(4, str, i, i2, null, null, 0, new Intent[]{intent}, new String[]{intent.resolveType(this.mAm.mContext.getContentResolver())}, 1409286144, null);
        final Intent intent2 = new Intent("android.intent.action.REVIEW_PERMISSIONS");
        intent2.addFlags(276824064);
        intent2.putExtra("android.intent.extra.PACKAGE_NAME", serviceRecord.packageName);
        intent2.putExtra("android.intent.extra.INTENT", new IntentSender(intentSenderLocked));
        if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
            Slog.i(TAG, "u" + serviceRecord.userId + " Launching permission review for package " + serviceRecord.packageName);
        }
        this.mAm.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ActiveServices.this.mAm.mContext.startActivityAsUser(intent2, new UserHandle(i2));
            }
        });
        return false;
    }

    ComponentName startServiceInnerLocked(ServiceMap serviceMap, Intent intent, ServiceRecord serviceRecord, boolean z, boolean z2) throws TransactionTooLargeException {
        ServiceState tracker = serviceRecord.getTracker();
        if (tracker != null) {
            tracker.setStarted(true, this.mAm.mProcessStats.getMemFactorLocked(), serviceRecord.lastActivity);
        }
        boolean z3 = false;
        serviceRecord.callStart = false;
        synchronized (serviceRecord.stats.getBatteryStats()) {
            serviceRecord.stats.startRunningLocked();
        }
        String strBringUpServiceLocked = bringUpServiceLocked(serviceRecord, intent.getFlags(), z, false, false);
        if (strBringUpServiceLocked != null) {
            return new ComponentName("!!", strBringUpServiceLocked);
        }
        if (serviceRecord.startRequested && z2) {
            if (serviceMap.mStartingBackground.size() == 0) {
                z3 = true;
            }
            serviceMap.mStartingBackground.add(serviceRecord);
            serviceRecord.startingBgTimeout = SystemClock.uptimeMillis() + this.mAm.mConstants.BG_START_TIMEOUT;
            if (DEBUG_DELAYED_SERVICE) {
                RuntimeException runtimeException = new RuntimeException("here");
                runtimeException.fillInStackTrace();
                Slog.v(TAG_SERVICE, "Starting background (first=" + z3 + "): " + serviceRecord, runtimeException);
            } else if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "Starting background (first=" + z3 + "): " + serviceRecord);
            }
            if (z3) {
                serviceMap.rescheduleDelayedStartsLocked();
            }
        } else if (z || serviceRecord.fgRequired) {
            serviceMap.ensureNotStartingBackgroundLocked(serviceRecord);
        }
        return serviceRecord.name;
    }

    private void stopServiceLocked(ServiceRecord serviceRecord) {
        if (serviceRecord.delayed) {
            if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "Delaying stop of pending: " + serviceRecord);
            }
            serviceRecord.delayedStop = true;
            return;
        }
        synchronized (serviceRecord.stats.getBatteryStats()) {
            serviceRecord.stats.stopRunningLocked();
        }
        serviceRecord.startRequested = false;
        if (serviceRecord.tracker != null) {
            serviceRecord.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        }
        serviceRecord.callStart = false;
        bringDownServiceIfNeededLocked(serviceRecord, false, false);
    }

    int stopServiceLocked(IApplicationThread iApplicationThread, Intent intent, String str, int i) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "stopService: " + intent + " type=" + str);
        }
        ProcessRecord recordForAppLocked = this.mAm.getRecordForAppLocked(iApplicationThread);
        if (iApplicationThread != null && recordForAppLocked == null) {
            throw new SecurityException("Unable to find app for caller " + iApplicationThread + " (pid=" + Binder.getCallingPid() + ") when stopping service " + intent);
        }
        ServiceLookupResult serviceLookupResultRetrieveServiceLocked = retrieveServiceLocked(intent, str, null, Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, false, false);
        if (serviceLookupResultRetrieveServiceLocked != null) {
            if (serviceLookupResultRetrieveServiceLocked.record != null) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    stopServiceLocked(serviceLookupResultRetrieveServiceLocked.record);
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return 1;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    throw th;
                }
            }
            return -1;
        }
        return 0;
    }

    void stopInBackgroundLocked(int i) {
        ServiceMap serviceMap = this.mServiceMap.get(UserHandle.getUserId(i));
        if (serviceMap != null) {
            ArrayList arrayList = null;
            for (int size = serviceMap.mServicesByName.size() - 1; size >= 0; size--) {
                ServiceRecord serviceRecordValueAt = serviceMap.mServicesByName.valueAt(size);
                if (serviceRecordValueAt.appInfo.uid == i && serviceRecordValueAt.startRequested && this.mAm.getAppStartModeLocked(serviceRecordValueAt.appInfo.uid, serviceRecordValueAt.packageName, serviceRecordValueAt.appInfo.targetSdkVersion, -1, false, false, false) != 0) {
                    if (arrayList == null) {
                        arrayList = new ArrayList();
                    }
                    String strFlattenToShortString = serviceRecordValueAt.name.flattenToShortString();
                    EventLogTags.writeAmStopIdleService(serviceRecordValueAt.appInfo.uid, strFlattenToShortString);
                    StringBuilder sb = new StringBuilder(64);
                    sb.append("Stopping service due to app idle: ");
                    UserHandle.formatUid(sb, serviceRecordValueAt.appInfo.uid);
                    sb.append(" ");
                    TimeUtils.formatDuration(serviceRecordValueAt.createRealTime - SystemClock.elapsedRealtime(), sb);
                    sb.append(" ");
                    sb.append(strFlattenToShortString);
                    Slog.w(TAG, sb.toString());
                    arrayList.add(serviceRecordValueAt);
                }
            }
            if (arrayList != null) {
                for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
                    ServiceRecord serviceRecord = (ServiceRecord) arrayList.get(size2);
                    serviceRecord.delayed = false;
                    serviceMap.ensureNotStartingBackgroundLocked(serviceRecord);
                    stopServiceLocked(serviceRecord);
                }
            }
        }
    }

    void killMisbehavingService(ServiceRecord serviceRecord, int i, int i2, String str) {
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                stopServiceLocked(serviceRecord);
                this.mAm.crashApplication(i, i2, str, -1, "Bad notification for startForeground", true);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    IBinder peekServiceLocked(Intent intent, String str, String str2) {
        ServiceLookupResult serviceLookupResultRetrieveServiceLocked = retrieveServiceLocked(intent, str, str2, Binder.getCallingPid(), Binder.getCallingUid(), UserHandle.getCallingUserId(), false, false, false, false);
        if (serviceLookupResultRetrieveServiceLocked != null) {
            if (serviceLookupResultRetrieveServiceLocked.record == null) {
                throw new SecurityException("Permission Denial: Accessing service from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + serviceLookupResultRetrieveServiceLocked.permission);
            }
            IntentBindRecord intentBindRecord = serviceLookupResultRetrieveServiceLocked.record.bindings.get(serviceLookupResultRetrieveServiceLocked.record.intent);
            if (intentBindRecord != null) {
                return intentBindRecord.binder;
            }
        }
        return null;
    }

    boolean stopServiceTokenLocked(ComponentName componentName, IBinder iBinder, int i) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "stopServiceToken: " + componentName + " " + iBinder + " startId=" + i);
        }
        ServiceRecord serviceRecordFindServiceLocked = findServiceLocked(componentName, iBinder, UserHandle.getCallingUserId());
        if (serviceRecordFindServiceLocked == null) {
            return false;
        }
        if (i >= 0) {
            ServiceRecord.StartItem startItemFindDeliveredStart = serviceRecordFindServiceLocked.findDeliveredStart(i, false, false);
            if (startItemFindDeliveredStart != null) {
                while (serviceRecordFindServiceLocked.deliveredStarts.size() > 0) {
                    ServiceRecord.StartItem startItemRemove = serviceRecordFindServiceLocked.deliveredStarts.remove(0);
                    startItemRemove.removeUriPermissionsLocked();
                    if (startItemRemove == startItemFindDeliveredStart) {
                        break;
                    }
                }
            }
            if (serviceRecordFindServiceLocked.getLastStartId() != i) {
                return false;
            }
            if (serviceRecordFindServiceLocked.deliveredStarts.size() > 0) {
                Slog.w(TAG, "stopServiceToken startId " + i + " is last, but have " + serviceRecordFindServiceLocked.deliveredStarts.size() + " remaining args");
            }
        }
        synchronized (serviceRecordFindServiceLocked.stats.getBatteryStats()) {
            serviceRecordFindServiceLocked.stats.stopRunningLocked();
        }
        serviceRecordFindServiceLocked.startRequested = false;
        if (serviceRecordFindServiceLocked.tracker != null) {
            serviceRecordFindServiceLocked.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        }
        serviceRecordFindServiceLocked.callStart = false;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        bringDownServiceIfNeededLocked(serviceRecordFindServiceLocked, false, false);
        Binder.restoreCallingIdentity(jClearCallingIdentity);
        return true;
    }

    public void setServiceForegroundLocked(ComponentName componentName, IBinder iBinder, int i, Notification notification, int i2) {
        int callingUserId = UserHandle.getCallingUserId();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            ServiceRecord serviceRecordFindServiceLocked = findServiceLocked(componentName, iBinder, callingUserId);
            if (serviceRecordFindServiceLocked != null) {
                setServiceForegroundInnerLocked(serviceRecordFindServiceLocked, i, notification, i2);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean foregroundAppShownEnoughLocked(ActiveForegroundApp activeForegroundApp, long j) {
        long j2;
        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
            Slog.d(TAG, "Shown enough: pkg=" + activeForegroundApp.mPackageName + ", uid=" + activeForegroundApp.mUid);
        }
        activeForegroundApp.mHideTime = JobStatus.NO_LATEST_RUNTIME;
        if (activeForegroundApp.mShownWhileTop) {
            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(TAG, "YES - shown while on top");
            }
        } else {
            if (this.mScreenOn || activeForegroundApp.mShownWhileScreenOn) {
                long j3 = activeForegroundApp.mStartVisibleTime;
                if (activeForegroundApp.mStartTime != activeForegroundApp.mStartVisibleTime) {
                    j2 = this.mAm.mConstants.FGSERVICE_SCREEN_ON_AFTER_TIME;
                } else {
                    j2 = this.mAm.mConstants.FGSERVICE_MIN_SHOWN_TIME;
                }
                long j4 = j3 + j2;
                if (j >= j4) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG, "YES - shown long enough with screen on");
                    }
                    return true;
                }
                long j5 = this.mAm.mConstants.FGSERVICE_MIN_REPORT_TIME + j;
                if (j5 <= j4) {
                    j5 = j4;
                }
                activeForegroundApp.mHideTime = j5;
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "NO -- wait " + (activeForegroundApp.mHideTime - j) + " with screen on");
                    return false;
                }
                return false;
            }
            long j6 = activeForegroundApp.mEndTime + this.mAm.mConstants.FGSERVICE_SCREEN_ON_BEFORE_TIME;
            if (j >= j6) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "YES - gone long enough with screen off");
                }
            } else {
                activeForegroundApp.mHideTime = j6;
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "NO -- wait " + (activeForegroundApp.mHideTime - j) + " with screen off");
                    return false;
                }
                return false;
            }
        }
        return true;
    }

    void updateForegroundApps(ServiceMap serviceMap) {
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                if (serviceMap != null) {
                    if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                        Slog.d(TAG, "Updating foreground apps for user " + serviceMap.mUserId);
                    }
                    ArrayList arrayList = null;
                    long j = Long.MAX_VALUE;
                    for (int size = serviceMap.mActiveForegroundApps.size() - 1; size >= 0; size--) {
                        ActiveForegroundApp activeForegroundAppValueAt = serviceMap.mActiveForegroundApps.valueAt(size);
                        if (activeForegroundAppValueAt.mEndTime != 0) {
                            if (foregroundAppShownEnoughLocked(activeForegroundAppValueAt, jElapsedRealtime)) {
                                serviceMap.mActiveForegroundApps.removeAt(size);
                                serviceMap.mActiveForegroundAppsChanged = true;
                            } else {
                                if (activeForegroundAppValueAt.mHideTime < j) {
                                    j = activeForegroundAppValueAt.mHideTime;
                                }
                                if (!activeForegroundAppValueAt.mAppOnTop) {
                                }
                            }
                        } else if (!activeForegroundAppValueAt.mAppOnTop) {
                            if (arrayList == null) {
                                arrayList = new ArrayList();
                            }
                            if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                                Slog.d(TAG, "Adding active: pkg=" + activeForegroundAppValueAt.mPackageName + ", uid=" + activeForegroundAppValueAt.mUid);
                            }
                            arrayList.add(activeForegroundAppValueAt);
                        }
                    }
                    serviceMap.removeMessages(2);
                    if (j < JobStatus.NO_LATEST_RUNTIME) {
                        if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                            Slog.d(TAG, "Next update time in: " + (j - jElapsedRealtime));
                        }
                        serviceMap.sendMessageAtTime(serviceMap.obtainMessage(2), (j + SystemClock.uptimeMillis()) - SystemClock.elapsedRealtime());
                    }
                }
                if (!serviceMap.mActiveForegroundAppsChanged) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                } else {
                    serviceMap.mActiveForegroundAppsChanged = false;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    private void requestUpdateActiveForegroundAppsLocked(ServiceMap serviceMap, long j) {
        Message messageObtainMessage = serviceMap.obtainMessage(2);
        if (j != 0) {
            serviceMap.sendMessageAtTime(messageObtainMessage, (j + SystemClock.uptimeMillis()) - SystemClock.elapsedRealtime());
        } else {
            serviceMap.mActiveForegroundAppsChanged = true;
            serviceMap.sendMessage(messageObtainMessage);
        }
    }

    private void decActiveForegroundAppLocked(ServiceMap serviceMap, ServiceRecord serviceRecord) {
        ActiveForegroundApp activeForegroundApp = serviceMap.mActiveForegroundApps.get(serviceRecord.packageName);
        if (activeForegroundApp != null) {
            activeForegroundApp.mNumActive--;
            if (activeForegroundApp.mNumActive <= 0) {
                activeForegroundApp.mEndTime = SystemClock.elapsedRealtime();
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "Ended running of service");
                }
                if (foregroundAppShownEnoughLocked(activeForegroundApp, activeForegroundApp.mEndTime)) {
                    serviceMap.mActiveForegroundApps.remove(serviceRecord.packageName);
                    serviceMap.mActiveForegroundAppsChanged = true;
                    requestUpdateActiveForegroundAppsLocked(serviceMap, 0L);
                } else if (activeForegroundApp.mHideTime < JobStatus.NO_LATEST_RUNTIME) {
                    requestUpdateActiveForegroundAppsLocked(serviceMap, activeForegroundApp.mHideTime);
                }
            }
        }
    }

    void updateScreenStateLocked(boolean z) {
        if (this.mScreenOn != z) {
            this.mScreenOn = z;
            if (z) {
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.d(TAG, "Screen turned on");
                }
                for (int size = this.mServiceMap.size() - 1; size >= 0; size--) {
                    ServiceMap serviceMapValueAt = this.mServiceMap.valueAt(size);
                    boolean z2 = false;
                    long j = JobStatus.NO_LATEST_RUNTIME;
                    for (int size2 = serviceMapValueAt.mActiveForegroundApps.size() - 1; size2 >= 0; size2--) {
                        ActiveForegroundApp activeForegroundAppValueAt = serviceMapValueAt.mActiveForegroundApps.valueAt(size2);
                        if (activeForegroundAppValueAt.mEndTime == 0) {
                            if (!activeForegroundAppValueAt.mShownWhileScreenOn) {
                                activeForegroundAppValueAt.mShownWhileScreenOn = true;
                                activeForegroundAppValueAt.mStartVisibleTime = jElapsedRealtime;
                            }
                        } else {
                            if (!activeForegroundAppValueAt.mShownWhileScreenOn && activeForegroundAppValueAt.mStartVisibleTime == activeForegroundAppValueAt.mStartTime) {
                                activeForegroundAppValueAt.mStartVisibleTime = jElapsedRealtime;
                                activeForegroundAppValueAt.mEndTime = jElapsedRealtime;
                            }
                            if (foregroundAppShownEnoughLocked(activeForegroundAppValueAt, jElapsedRealtime)) {
                                serviceMapValueAt.mActiveForegroundApps.remove(activeForegroundAppValueAt.mPackageName);
                                serviceMapValueAt.mActiveForegroundAppsChanged = true;
                                z2 = true;
                            } else if (activeForegroundAppValueAt.mHideTime < j) {
                                j = activeForegroundAppValueAt.mHideTime;
                            }
                        }
                    }
                    if (z2) {
                        requestUpdateActiveForegroundAppsLocked(serviceMapValueAt, 0L);
                    } else if (j < JobStatus.NO_LATEST_RUNTIME) {
                        requestUpdateActiveForegroundAppsLocked(serviceMapValueAt, j);
                    }
                }
            }
        }
    }

    void foregroundServiceProcStateChangedLocked(UidRecord uidRecord) {
        ServiceMap serviceMap = this.mServiceMap.get(UserHandle.getUserId(uidRecord.uid));
        if (serviceMap != null) {
            boolean z = false;
            for (int size = serviceMap.mActiveForegroundApps.size() - 1; size >= 0; size--) {
                ActiveForegroundApp activeForegroundAppValueAt = serviceMap.mActiveForegroundApps.valueAt(size);
                if (activeForegroundAppValueAt.mUid == uidRecord.uid) {
                    if (uidRecord.curProcState <= 2) {
                        if (!activeForegroundAppValueAt.mAppOnTop) {
                            activeForegroundAppValueAt.mAppOnTop = true;
                            z = true;
                        }
                        activeForegroundAppValueAt.mShownWhileTop = true;
                    } else if (activeForegroundAppValueAt.mAppOnTop) {
                        activeForegroundAppValueAt.mAppOnTop = false;
                        z = true;
                    }
                }
            }
            if (z) {
                requestUpdateActiveForegroundAppsLocked(serviceMap, 0L);
            }
        }
    }

    private void setServiceForegroundInnerLocked(ServiceRecord serviceRecord, int i, Notification notification, int i2) {
        boolean z;
        boolean z2;
        if (i == 0) {
            if (serviceRecord.isForeground) {
                ServiceMap serviceMapLocked = getServiceMapLocked(serviceRecord.userId);
                if (serviceMapLocked != null) {
                    decActiveForegroundAppLocked(serviceMapLocked, serviceRecord);
                }
                serviceRecord.isForeground = false;
                this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
                StatsLog.write(60, serviceRecord.appInfo.uid, serviceRecord.shortName, 2);
                if (serviceRecord.app != null) {
                    this.mAm.updateLruProcessLocked(serviceRecord.app, false, null);
                    updateServiceForegroundLocked(serviceRecord.app, true);
                }
            }
            if ((i2 & 1) != 0) {
                cancelForegroundNotificationLocked(serviceRecord);
                serviceRecord.foregroundId = 0;
                serviceRecord.foregroundNoti = null;
                return;
            } else {
                if (serviceRecord.appInfo.targetSdkVersion >= 21) {
                    serviceRecord.stripForegroundServiceFlagFromNotification();
                    if ((i2 & 2) != 0) {
                        serviceRecord.foregroundId = 0;
                        serviceRecord.foregroundNoti = null;
                        return;
                    }
                    return;
                }
                return;
            }
        }
        if (notification == null) {
            throw new IllegalArgumentException("null notification");
        }
        if (serviceRecord.appInfo.isInstantApp()) {
            switch (this.mAm.mAppOpsService.checkOperation(68, serviceRecord.appInfo.uid, serviceRecord.appInfo.packageName)) {
                case 0:
                    break;
                case 1:
                    Slog.w(TAG, "Instant app " + serviceRecord.appInfo.packageName + " does not have permission to create foreground services, ignoring.");
                    return;
                case 2:
                    throw new SecurityException("Instant app " + serviceRecord.appInfo.packageName + " does not have permission to create foreground services");
                default:
                    this.mAm.enforcePermission("android.permission.INSTANT_APP_FOREGROUND_SERVICE", serviceRecord.app.pid, serviceRecord.appInfo.uid, "startForeground");
                    break;
            }
        } else if (serviceRecord.appInfo.targetSdkVersion >= 28) {
            this.mAm.enforcePermission("android.permission.FOREGROUND_SERVICE", serviceRecord.app.pid, serviceRecord.appInfo.uid, "startForeground");
        }
        if (serviceRecord.fgRequired) {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE || ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                Slog.i(TAG, "Service called startForeground() as required: " + serviceRecord);
            }
            serviceRecord.fgRequired = false;
            serviceRecord.fgWaiting = false;
            this.mAm.mHandler.removeMessages(66, serviceRecord);
            z = true;
        } else {
            z = false;
        }
        try {
            int iCheckOperation = this.mAm.mAppOpsService.checkOperation(76, serviceRecord.appInfo.uid, serviceRecord.packageName);
            if (iCheckOperation != 3) {
                switch (iCheckOperation) {
                    case 0:
                        z2 = false;
                        break;
                    case 1:
                        Slog.w(TAG, "Service.startForeground() not allowed due to app op: service " + serviceRecord.shortName);
                        z2 = true;
                        break;
                    default:
                        throw new SecurityException("Foreground not allowed as per app op");
                }
            }
            if (!z2 && appRestrictedAnyInBackground(serviceRecord.appInfo.uid, serviceRecord.packageName)) {
                Slog.w(TAG, "Service.startForeground() not allowed due to bg restriction: service " + serviceRecord.shortName);
                updateServiceForegroundLocked(serviceRecord.app, false);
                z2 = true;
            }
            if (!z2) {
                if (serviceRecord.foregroundId != i) {
                    cancelForegroundNotificationLocked(serviceRecord);
                    serviceRecord.foregroundId = i;
                }
                notification.flags |= 64;
                serviceRecord.foregroundNoti = notification;
                if (!serviceRecord.isForeground) {
                    ServiceMap serviceMapLocked2 = getServiceMapLocked(serviceRecord.userId);
                    if (serviceMapLocked2 != null) {
                        ActiveForegroundApp activeForegroundApp = serviceMapLocked2.mActiveForegroundApps.get(serviceRecord.packageName);
                        if (activeForegroundApp == null) {
                            activeForegroundApp = new ActiveForegroundApp();
                            activeForegroundApp.mPackageName = serviceRecord.packageName;
                            activeForegroundApp.mUid = serviceRecord.appInfo.uid;
                            activeForegroundApp.mShownWhileScreenOn = this.mScreenOn;
                            if (serviceRecord.app != null) {
                                boolean z3 = serviceRecord.app.uidRecord.curProcState <= 2;
                                activeForegroundApp.mShownWhileTop = z3;
                                activeForegroundApp.mAppOnTop = z3;
                            }
                            long jElapsedRealtime = SystemClock.elapsedRealtime();
                            activeForegroundApp.mStartVisibleTime = jElapsedRealtime;
                            activeForegroundApp.mStartTime = jElapsedRealtime;
                            serviceMapLocked2.mActiveForegroundApps.put(serviceRecord.packageName, activeForegroundApp);
                            requestUpdateActiveForegroundAppsLocked(serviceMapLocked2, 0L);
                        }
                        activeForegroundApp.mNumActive++;
                    }
                    serviceRecord.isForeground = true;
                    this.mAm.mAppOpsService.startOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName, true);
                    StatsLog.write(60, serviceRecord.appInfo.uid, serviceRecord.shortName, 1);
                }
                serviceRecord.postNotification();
                if (serviceRecord.app != null) {
                    updateServiceForegroundLocked(serviceRecord.app, true);
                }
                getServiceMapLocked(serviceRecord.userId).ensureNotStartingBackgroundLocked(serviceRecord);
                this.mAm.notifyPackageUse(serviceRecord.serviceInfo.packageName, 2);
            } else if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                Slog.d(TAG, "Suppressing startForeground() for FAS " + serviceRecord);
            }
        } finally {
            if (z) {
                this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
            }
        }
    }

    private void cancelForegroundNotificationLocked(ServiceRecord serviceRecord) {
        if (serviceRecord.foregroundId != 0) {
            ServiceMap serviceMapLocked = getServiceMapLocked(serviceRecord.userId);
            if (serviceMapLocked != null) {
                for (int size = serviceMapLocked.mServicesByName.size() - 1; size >= 0; size--) {
                    ServiceRecord serviceRecordValueAt = serviceMapLocked.mServicesByName.valueAt(size);
                    if (serviceRecordValueAt != serviceRecord && serviceRecordValueAt.foregroundId == serviceRecord.foregroundId && serviceRecordValueAt.packageName.equals(serviceRecord.packageName)) {
                        return;
                    }
                }
            }
            serviceRecord.cancelNotification();
        }
    }

    private void updateServiceForegroundLocked(ProcessRecord processRecord, boolean z) {
        boolean z2 = true;
        int size = processRecord.services.size() - 1;
        while (true) {
            if (size >= 0) {
                ServiceRecord serviceRecordValueAt = processRecord.services.valueAt(size);
                if (serviceRecordValueAt.isForeground || serviceRecordValueAt.fgRequired) {
                    break;
                } else {
                    size--;
                }
            } else {
                z2 = false;
                break;
            }
        }
        this.mAm.updateProcessForegroundLocked(processRecord, z2, z);
    }

    private void updateWhitelistManagerLocked(ProcessRecord processRecord) {
        processRecord.whitelistManager = false;
        for (int size = processRecord.services.size() - 1; size >= 0; size--) {
            if (processRecord.services.valueAt(size).whitelistManager) {
                processRecord.whitelistManager = true;
                return;
            }
        }
    }

    public void updateServiceConnectionActivitiesLocked(ProcessRecord processRecord) {
        ArraySet arraySet = null;
        for (int i = 0; i < processRecord.connections.size(); i++) {
            ProcessRecord processRecord2 = processRecord.connections.valueAt(i).binding.service.app;
            if (processRecord2 != null && processRecord2 != processRecord) {
                if (arraySet == null) {
                    arraySet = new ArraySet();
                } else if (arraySet.contains(processRecord2)) {
                }
                arraySet.add(processRecord2);
                updateServiceClientActivitiesLocked(processRecord2, null, false);
            }
        }
    }

    private boolean updateServiceClientActivitiesLocked(ProcessRecord processRecord, ConnectionRecord connectionRecord, boolean z) {
        if (connectionRecord != null && connectionRecord.binding.client != null && connectionRecord.binding.client.activities.size() <= 0) {
            return false;
        }
        boolean z2 = false;
        for (int size = processRecord.services.size() - 1; size >= 0 && !z2; size--) {
            ServiceRecord serviceRecordValueAt = processRecord.services.valueAt(size);
            for (int size2 = serviceRecordValueAt.connections.size() - 1; size2 >= 0 && !z2; size2--) {
                ArrayList<ConnectionRecord> arrayListValueAt = serviceRecordValueAt.connections.valueAt(size2);
                int size3 = arrayListValueAt.size() - 1;
                while (true) {
                    if (size3 >= 0) {
                        ConnectionRecord connectionRecord2 = arrayListValueAt.get(size3);
                        if (connectionRecord2.binding.client != null && connectionRecord2.binding.client != processRecord && connectionRecord2.binding.client.activities.size() > 0) {
                            z2 = true;
                            break;
                        }
                        size3--;
                    }
                }
            }
        }
        if (z2 == processRecord.hasClientActivities) {
            return false;
        }
        processRecord.hasClientActivities = z2;
        if (z) {
            this.mAm.updateLruProcessLocked(processRecord, z2, null);
        }
        return true;
    }

    int bindServiceLocked(IApplicationThread iApplicationThread, IBinder iBinder, Intent intent, String str, final IServiceConnection iServiceConnection, int i, String str2, final int i2) throws Throwable {
        String str3;
        ActivityRecord activityRecord;
        Intent intent2;
        PendingIntent pendingIntent;
        int i3;
        final boolean z;
        boolean z2;
        long j;
        ServiceState tracker;
        Intent intentCloneFilter = intent;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            String str4 = TAG_SERVICE;
            StringBuilder sb = new StringBuilder();
            sb.append("bindService: ");
            sb.append(intentCloneFilter);
            sb.append(" type=");
            str3 = str;
            sb.append(str3);
            sb.append(" conn=");
            sb.append(iServiceConnection.asBinder());
            sb.append(" flags=0x");
            sb.append(Integer.toHexString(i));
            Slog.v(str4, sb.toString());
        } else {
            str3 = str;
        }
        ProcessRecord recordForAppLocked = this.mAm.getRecordForAppLocked(iApplicationThread);
        if (recordForAppLocked == null) {
            throw new SecurityException("Unable to find app for caller " + iApplicationThread + " (pid=" + Binder.getCallingPid() + ") when binding service " + intentCloneFilter);
        }
        PendingIntent pendingIntent2 = null;
        if (iBinder == null) {
            activityRecord = null;
        } else {
            ActivityRecord activityRecordIsInStackLocked = ActivityRecord.isInStackLocked(iBinder);
            if (activityRecordIsInStackLocked == null) {
                Slog.w(TAG, "Binding with unknown activity: " + iBinder);
                return 0;
            }
            activityRecord = activityRecordIsInStackLocked;
        }
        boolean z3 = recordForAppLocked.info.uid == 1000;
        if (z3) {
            intentCloneFilter.setDefusable(true);
            pendingIntent2 = (PendingIntent) intentCloneFilter.getParcelableExtra("android.intent.extra.client_intent");
            if (pendingIntent2 != null) {
                int intExtra = intentCloneFilter.getIntExtra("android.intent.extra.client_label", 0);
                if (intExtra != 0) {
                    intentCloneFilter = intent.cloneFilter();
                }
                intent2 = intentCloneFilter;
                pendingIntent = pendingIntent2;
                i3 = intExtra;
            } else {
                intent2 = intentCloneFilter;
                pendingIntent = pendingIntent2;
                i3 = 0;
            }
        }
        int i4 = i & 134217728;
        if (i4 != 0) {
            this.mAm.enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "BIND_TREAT_LIKE_ACTIVITY");
        }
        if ((i & DumpState.DUMP_SERVICE_PERMISSIONS) != 0 && !z3) {
            throw new SecurityException("Non-system caller " + iApplicationThread + " (pid=" + Binder.getCallingPid() + ") set BIND_ALLOW_WHITELIST_MANAGEMENT when binding service " + intent2);
        }
        int i5 = i & DumpState.DUMP_CHANGES;
        if (i5 != 0 && !z3) {
            throw new SecurityException("Non-system caller " + iApplicationThread + " (pid=" + Binder.getCallingPid() + ") set BIND_ALLOW_INSTANT when binding service " + intent2);
        }
        boolean z4 = recordForAppLocked.setSchedGroup != 0;
        final Intent intent3 = intent2;
        ActivityRecord activityRecord2 = activityRecord;
        ServiceLookupResult serviceLookupResultRetrieveServiceLocked = retrieveServiceLocked(intent2, str3, str2, Binder.getCallingPid(), Binder.getCallingUid(), i2, true, z4, (i & Integer.MIN_VALUE) != 0, i5 != 0);
        if (serviceLookupResultRetrieveServiceLocked == null) {
            return 0;
        }
        if (serviceLookupResultRetrieveServiceLocked.record == null) {
            return -1;
        }
        final ServiceRecord serviceRecord = serviceLookupResultRetrieveServiceLocked.record;
        if (this.mAm.mPermissionReviewRequired && this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(serviceRecord.packageName, serviceRecord.userId)) {
            z = z4;
            if (!z) {
                Slog.w(TAG, "u" + serviceRecord.userId + " Binding to a service in package" + serviceRecord.packageName + " requires a permissions review");
                return 0;
            }
            Parcelable remoteCallback = new RemoteCallback(new RemoteCallback.OnResultListener() {
                public void onResult(Bundle bundle) {
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            long jClearCallingIdentity = Binder.clearCallingIdentity();
                            try {
                                if (!ActiveServices.this.mPendingServices.contains(serviceRecord)) {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    return;
                                }
                                if (!ActiveServices.this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(serviceRecord.packageName, serviceRecord.userId)) {
                                    try {
                                        ActiveServices.this.bringUpServiceLocked(serviceRecord, intent3.getFlags(), z, false, false);
                                    } catch (RemoteException e) {
                                    }
                                } else {
                                    ActiveServices.this.unbindServiceLocked(iServiceConnection);
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            } finally {
                                Binder.restoreCallingIdentity(jClearCallingIdentity);
                            }
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }
            });
            final Intent intent4 = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent4.addFlags(276824064);
            intent4.putExtra("android.intent.extra.PACKAGE_NAME", serviceRecord.packageName);
            intent4.putExtra("android.intent.extra.REMOTE_CALLBACK", remoteCallback);
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                Slog.i(TAG, "u" + serviceRecord.userId + " Launching permission review for package " + serviceRecord.packageName);
            }
            this.mAm.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ActiveServices.this.mAm.mContext.startActivityAsUser(intent4, new UserHandle(i2));
                }
            });
            z2 = true;
        } else {
            z = z4;
            z2 = false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (unscheduleServiceRestartLocked(serviceRecord, recordForAppLocked.info.uid, false) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "BIND SERVICE WHILE RESTART PENDING: " + serviceRecord);
            }
            int i6 = i & 1;
            if (i6 != 0) {
                serviceRecord.lastActivity = SystemClock.uptimeMillis();
                if (!serviceRecord.hasAutoCreateConnections() && (tracker = serviceRecord.getTracker()) != null) {
                    tracker.setBound(true, this.mAm.mProcessStats.getMemFactorLocked(), serviceRecord.lastActivity);
                }
                this.mAm.startAssociationLocked(recordForAppLocked.uid, recordForAppLocked.processName, recordForAppLocked.curProcState, serviceRecord.appInfo.uid, serviceRecord.name, serviceRecord.processName);
                this.mAm.grantEphemeralAccessLocked(recordForAppLocked.userId, intent3, serviceRecord.appInfo.uid, UserHandle.getAppId(recordForAppLocked.uid));
                AppBindRecord appBindRecordRetrieveAppBindingLocked = serviceRecord.retrieveAppBindingLocked(intent3, recordForAppLocked);
                boolean z5 = z2;
                try {
                    ConnectionRecord connectionRecord = new ConnectionRecord(appBindRecordRetrieveAppBindingLocked, activityRecord2, iServiceConnection, i, i3, pendingIntent);
                    IBinder iBinderAsBinder = iServiceConnection.asBinder();
                    ArrayList<ConnectionRecord> arrayList = serviceRecord.connections.get(iBinderAsBinder);
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                        serviceRecord.connections.put(iBinderAsBinder, arrayList);
                    }
                    arrayList.add(connectionRecord);
                    appBindRecordRetrieveAppBindingLocked.connections.add(connectionRecord);
                    if (activityRecord2 != null) {
                        if (activityRecord2.connections == null) {
                            activityRecord2.connections = new HashSet<>();
                        }
                        activityRecord2.connections.add(connectionRecord);
                    }
                    appBindRecordRetrieveAppBindingLocked.client.connections.add(connectionRecord);
                    if ((connectionRecord.flags & 8) != 0) {
                        appBindRecordRetrieveAppBindingLocked.client.hasAboveClient = true;
                    }
                    if ((connectionRecord.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0) {
                        serviceRecord.whitelistManager = true;
                    }
                    if (serviceRecord.app != null) {
                        updateServiceClientActivitiesLocked(serviceRecord.app, connectionRecord, true);
                    }
                    ArrayList<ConnectionRecord> arrayList2 = this.mServiceConnections.get(iBinderAsBinder);
                    if (arrayList2 == null) {
                        arrayList2 = new ArrayList<>();
                        this.mServiceConnections.put(iBinderAsBinder, arrayList2);
                    }
                    arrayList2.add(connectionRecord);
                    if (i6 != 0) {
                        serviceRecord.lastActivity = SystemClock.uptimeMillis();
                        if (bringUpServiceLocked(serviceRecord, intent3.getFlags(), z, false, z5) != null) {
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                            return 0;
                        }
                    }
                    j = jClearCallingIdentity;
                    try {
                        if (serviceRecord.app != null) {
                            if (i4 != 0) {
                                serviceRecord.app.treatLikeActivity = true;
                            }
                            if (serviceRecord.whitelistManager) {
                                serviceRecord.app.whitelistManager = true;
                            }
                            this.mAm.updateLruProcessLocked(serviceRecord.app, serviceRecord.app.hasClientActivities || serviceRecord.app.treatLikeActivity, appBindRecordRetrieveAppBindingLocked.client);
                            this.mAm.updateOomAdjLocked(serviceRecord.app, true);
                        }
                        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                            Slog.v(TAG_SERVICE, "Bind " + serviceRecord + " with " + appBindRecordRetrieveAppBindingLocked + ": received=" + appBindRecordRetrieveAppBindingLocked.intent.received + " apps=" + appBindRecordRetrieveAppBindingLocked.intent.apps.size() + " doRebind=" + appBindRecordRetrieveAppBindingLocked.intent.doRebind);
                        }
                        if (serviceRecord.app != null && appBindRecordRetrieveAppBindingLocked.intent.received) {
                            try {
                                connectionRecord.conn.connected(serviceRecord.name, appBindRecordRetrieveAppBindingLocked.intent.binder, false);
                            } catch (Exception e) {
                                Slog.w(TAG, "Failure sending service " + serviceRecord.shortName + " to connection " + connectionRecord.conn.asBinder() + " (in " + connectionRecord.binding.client.processName + ")", e);
                            }
                            if (appBindRecordRetrieveAppBindingLocked.intent.apps.size() == 1 && appBindRecordRetrieveAppBindingLocked.intent.doRebind) {
                                requestServiceBindingLocked(serviceRecord, appBindRecordRetrieveAppBindingLocked.intent, z, true);
                            }
                        } else if (!appBindRecordRetrieveAppBindingLocked.intent.requested) {
                            requestServiceBindingLocked(serviceRecord, appBindRecordRetrieveAppBindingLocked.intent, z, false);
                        }
                        getServiceMapLocked(serviceRecord.userId).ensureNotStartingBackgroundLocked(serviceRecord);
                        Binder.restoreCallingIdentity(j);
                        return 1;
                    } catch (Throwable th) {
                        th = th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    j = jClearCallingIdentity;
                }
            }
        } catch (Throwable th3) {
            th = th3;
            j = jClearCallingIdentity;
        }
        Binder.restoreCallingIdentity(j);
        throw th;
    }

    void publishServiceLocked(ServiceRecord serviceRecord, Intent intent, IBinder iBinder) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "PUBLISHING " + serviceRecord + " " + intent + ": " + iBinder);
            }
            if (serviceRecord != null) {
                Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent);
                IntentBindRecord intentBindRecord = serviceRecord.bindings.get(filterComparison);
                if (intentBindRecord != null && !intentBindRecord.received) {
                    intentBindRecord.binder = iBinder;
                    intentBindRecord.requested = true;
                    intentBindRecord.received = true;
                    for (int size = serviceRecord.connections.size() - 1; size >= 0; size--) {
                        ArrayList<ConnectionRecord> arrayListValueAt = serviceRecord.connections.valueAt(size);
                        for (int i = 0; i < arrayListValueAt.size(); i++) {
                            ConnectionRecord connectionRecord = arrayListValueAt.get(i);
                            if (!filterComparison.equals(connectionRecord.binding.intent.intent)) {
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Not publishing to: " + connectionRecord);
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Bound intent: " + connectionRecord.binding.intent.intent);
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Published intent: " + intent);
                                }
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Publishing to: " + connectionRecord);
                                }
                                try {
                                    connectionRecord.conn.connected(serviceRecord.name, iBinder, false);
                                } catch (Exception e) {
                                    Slog.w(TAG, "Failure sending service " + serviceRecord.name + " to connection " + connectionRecord.conn.asBinder() + " (in " + connectionRecord.binding.client.processName + ")", e);
                                }
                            }
                        }
                    }
                }
                serviceDoneExecutingLocked(serviceRecord, this.mDestroyingServices.contains(serviceRecord), false);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    boolean unbindServiceLocked(IServiceConnection iServiceConnection) {
        IBinder iBinderAsBinder = iServiceConnection.asBinder();
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "unbindService: conn=" + iBinderAsBinder);
        }
        ArrayList<ConnectionRecord> arrayList = this.mServiceConnections.get(iBinderAsBinder);
        if (arrayList == null) {
            Slog.w(TAG, "Unbind failed: could not find connection for " + iServiceConnection.asBinder());
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        while (true) {
            try {
                boolean z = true;
                if (arrayList.size() > 0) {
                    ConnectionRecord connectionRecord = arrayList.get(0);
                    removeConnectionLocked(connectionRecord, null, null);
                    if (arrayList.size() > 0 && arrayList.get(0) == connectionRecord) {
                        Slog.wtf(TAG, "Connection " + connectionRecord + " not removed for binder " + iBinderAsBinder);
                        arrayList.remove(0);
                    }
                    if (connectionRecord.binding.service.app != null) {
                        if (connectionRecord.binding.service.app.whitelistManager) {
                            updateWhitelistManagerLocked(connectionRecord.binding.service.app);
                        }
                        if ((connectionRecord.flags & 134217728) != 0) {
                            connectionRecord.binding.service.app.treatLikeActivity = true;
                            ActivityManagerService activityManagerService = this.mAm;
                            ProcessRecord processRecord = connectionRecord.binding.service.app;
                            if (!connectionRecord.binding.service.app.hasClientActivities && !connectionRecord.binding.service.app.treatLikeActivity) {
                                z = false;
                            }
                            activityManagerService.updateLruProcessLocked(processRecord, z, null);
                        }
                        this.mAm.updateOomAdjLocked(connectionRecord.binding.service.app, false);
                    }
                } else {
                    this.mAm.updateOomAdjLocked();
                    return true;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    void unbindFinishedLocked(ServiceRecord serviceRecord, Intent intent, boolean z) {
        boolean z2;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        if (serviceRecord != null) {
            try {
                IntentBindRecord intentBindRecord = serviceRecord.bindings.get(new Intent.FilterComparison(intent));
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    String str = TAG_SERVICE;
                    StringBuilder sb = new StringBuilder();
                    sb.append("unbindFinished in ");
                    sb.append(serviceRecord);
                    sb.append(" at ");
                    sb.append(intentBindRecord);
                    sb.append(": apps=");
                    sb.append(intentBindRecord != null ? intentBindRecord.apps.size() : 0);
                    Slog.v(str, sb.toString());
                }
                boolean zContains = this.mDestroyingServices.contains(serviceRecord);
                if (intentBindRecord != null) {
                    if (intentBindRecord.apps.size() > 0 && !zContains) {
                        int size = intentBindRecord.apps.size() - 1;
                        while (true) {
                            if (size >= 0) {
                                ProcessRecord processRecord = intentBindRecord.apps.valueAt(size).client;
                                if (processRecord == null || processRecord.setSchedGroup == 0) {
                                    size--;
                                } else {
                                    z2 = true;
                                    break;
                                }
                            } else {
                                z2 = false;
                                break;
                            }
                        }
                        try {
                            requestServiceBindingLocked(serviceRecord, intentBindRecord, z2, true);
                        } catch (TransactionTooLargeException e) {
                        }
                    } else {
                        intentBindRecord.doRebind = true;
                    }
                }
                serviceDoneExecutingLocked(serviceRecord, zContains, false);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    private final ServiceRecord findServiceLocked(ComponentName componentName, IBinder iBinder, int i) {
        ServiceRecord serviceByNameLocked = getServiceByNameLocked(componentName, i);
        if (serviceByNameLocked == iBinder) {
            return serviceByNameLocked;
        }
        return null;
    }

    private final class ServiceLookupResult {
        final String permission;
        final ServiceRecord record;

        ServiceLookupResult(ServiceRecord serviceRecord, String str) {
            this.record = serviceRecord;
            this.permission = str;
        }
    }

    private class ServiceRestarter implements Runnable {
        private ServiceRecord mService;

        private ServiceRestarter() {
        }

        void setService(ServiceRecord serviceRecord) {
            this.mService = serviceRecord;
        }

        @Override
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActiveServices.this.performServiceRestartLocked(this.mService);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    private ServiceLookupResult retrieveServiceLocked(Intent intent, String str, String str2, int i, int i2, int i3, boolean z, boolean z2, boolean z3, boolean z4) {
        String str3;
        ServiceRecord serviceRecord;
        ServiceRecord serviceRecord2;
        int i4;
        int i5;
        int iPermissionToOpCode;
        ServiceInfo serviceInfo;
        BatteryStatsImpl.Uid.Pkg.Serv serviceStatsLocked;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            String str4 = TAG_SERVICE;
            StringBuilder sb = new StringBuilder();
            sb.append("retrieveServiceLocked: ");
            sb.append(intent);
            sb.append(" type=");
            str3 = str;
            sb.append(str3);
            sb.append(" callingUid=");
            sb.append(i2);
            Slog.v(str4, sb.toString());
        } else {
            str3 = str;
        }
        int iHandleIncomingUser = this.mAm.mUserController.handleIncomingUser(i, i2, i3, false, 1, "service", null);
        ServiceMap serviceMapLocked = getServiceMapLocked(iHandleIncomingUser);
        ComponentName component = intent.getComponent();
        if (component != null) {
            serviceRecord = serviceMapLocked.mServicesByName.get(component);
            if (ActivityManagerDebugConfig.DEBUG_SERVICE && serviceRecord != null) {
                Slog.v(TAG_SERVICE, "Retrieved by component: " + serviceRecord);
            }
        } else {
            serviceRecord = null;
        }
        if (serviceRecord == null && !z3) {
            serviceRecord = serviceMapLocked.mServicesByIntent.get(new Intent.FilterComparison(intent));
            if (ActivityManagerDebugConfig.DEBUG_SERVICE && serviceRecord != null) {
                Slog.v(TAG_SERVICE, "Retrieved by intent: " + serviceRecord);
            }
        }
        if (serviceRecord == null || (serviceRecord.serviceInfo.flags & 4) == 0 || str2.equals(serviceRecord.packageName)) {
            serviceRecord2 = serviceRecord;
        } else {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Whoops, can't use existing external service");
            }
            serviceRecord2 = null;
        }
        if (serviceRecord2 == null) {
            int i6 = 268436480;
            if (z4) {
                i6 = 276825088;
            }
            try {
                i4 = i2;
                i5 = i;
                try {
                    ResolveInfo resolveInfoResolveService = this.mAm.getPackageManagerInternalLocked().resolveService(intent, str3, i6, iHandleIncomingUser, i4);
                    ServiceInfo serviceInfo2 = resolveInfoResolveService != null ? resolveInfoResolveService.serviceInfo : null;
                    if (serviceInfo2 == null) {
                        Slog.w(TAG_SERVICE, "Unable to start service " + intent + " U=" + iHandleIncomingUser + ": not found");
                        return null;
                    }
                    ComponentName componentName = new ComponentName(serviceInfo2.applicationInfo.packageName, serviceInfo2.name);
                    if ((serviceInfo2.flags & 4) != 0) {
                        if (z3) {
                            if (!serviceInfo2.exported) {
                                throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + componentName + " is not exported");
                            }
                            if ((serviceInfo2.flags & 2) == 0) {
                                throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + componentName + " is not an isolatedProcess");
                            }
                            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(str2, 1024, iHandleIncomingUser);
                            if (applicationInfo == null) {
                                throw new SecurityException("BIND_EXTERNAL_SERVICE failed, could not resolve client package " + str2);
                            }
                            serviceInfo = new ServiceInfo(serviceInfo2);
                            serviceInfo.applicationInfo = new ApplicationInfo(serviceInfo.applicationInfo);
                            serviceInfo.applicationInfo.packageName = applicationInfo.packageName;
                            serviceInfo.applicationInfo.uid = applicationInfo.uid;
                            ComponentName componentName2 = new ComponentName(applicationInfo.packageName, componentName.getClassName());
                            intent.setComponent(componentName2);
                            componentName = componentName2;
                        } else {
                            throw new SecurityException("BIND_EXTERNAL_SERVICE required for " + componentName);
                        }
                    } else {
                        if (z3) {
                            throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + componentName + " is not an externalService");
                        }
                        serviceInfo = serviceInfo2;
                    }
                    if (iHandleIncomingUser > 0) {
                        if (this.mAm.isSingleton(serviceInfo.processName, serviceInfo.applicationInfo, serviceInfo.name, serviceInfo.flags) && this.mAm.isValidSingletonCall(i4, serviceInfo.applicationInfo.uid)) {
                            serviceMapLocked = getServiceMapLocked(0);
                            iHandleIncomingUser = 0;
                        }
                        ServiceInfo serviceInfo3 = new ServiceInfo(serviceInfo);
                        serviceInfo3.applicationInfo = this.mAm.getAppInfoForUser(serviceInfo3.applicationInfo, iHandleIncomingUser);
                        serviceInfo = serviceInfo3;
                    }
                    ServiceRecord serviceRecord3 = serviceMapLocked.mServicesByName.get(componentName);
                    try {
                        if (ActivityManagerDebugConfig.DEBUG_SERVICE && serviceRecord3 != null) {
                            Slog.v(TAG_SERVICE, "Retrieved via pm by intent: " + serviceRecord3);
                        }
                        if (serviceRecord3 == null && z) {
                            Intent.FilterComparison filterComparison = new Intent.FilterComparison(intent.cloneFilter());
                            ServiceRestarter serviceRestarter = new ServiceRestarter();
                            BatteryStatsImpl activeStatistics = this.mAm.mBatteryStatsService.getActiveStatistics();
                            synchronized (activeStatistics) {
                                serviceStatsLocked = activeStatistics.getServiceStatsLocked(serviceInfo.applicationInfo.uid, serviceInfo.packageName, serviceInfo.name);
                            }
                            ServiceRecord serviceRecord4 = new ServiceRecord(this.mAm, serviceStatsLocked, componentName, filterComparison, serviceInfo, z2, serviceRestarter);
                            try {
                                serviceRestarter.setService(serviceRecord4);
                                serviceMapLocked.mServicesByName.put(componentName, serviceRecord4);
                                serviceMapLocked.mServicesByIntent.put(filterComparison, serviceRecord4);
                                for (int size = this.mPendingServices.size() - 1; size >= 0; size--) {
                                    ServiceRecord serviceRecord5 = this.mPendingServices.get(size);
                                    if (serviceRecord5.serviceInfo.applicationInfo.uid == serviceInfo.applicationInfo.uid && serviceRecord5.name.equals(componentName)) {
                                        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                            Slog.v(TAG_SERVICE, "Remove pending: " + serviceRecord5);
                                        }
                                        this.mPendingServices.remove(size);
                                    }
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Retrieve created new service: " + serviceRecord4);
                                }
                                serviceRecord2 = serviceRecord4;
                            } catch (RemoteException e) {
                                serviceRecord2 = serviceRecord4;
                            }
                        } else {
                            serviceRecord2 = serviceRecord3;
                        }
                    } catch (RemoteException e2) {
                        serviceRecord2 = serviceRecord3;
                    }
                } catch (RemoteException e3) {
                }
            } catch (RemoteException e4) {
                i4 = i2;
                i5 = i;
            }
        } else {
            i4 = i2;
            i5 = i;
        }
        if (serviceRecord2 == null) {
            return null;
        }
        if (this.mAm.checkComponentPermission(serviceRecord2.permission, i5, i4, serviceRecord2.appInfo.uid, serviceRecord2.exported) != 0) {
            if (!serviceRecord2.exported) {
                Slog.w(TAG, "Permission Denial: Accessing service " + serviceRecord2.name + " from pid=" + i5 + ", uid=" + i4 + " that is not exported from uid " + serviceRecord2.appInfo.uid);
                return new ServiceLookupResult(null, "not exported from uid " + serviceRecord2.appInfo.uid);
            }
            Slog.w(TAG, "Permission Denial: Accessing service " + serviceRecord2.name + " from pid=" + i5 + ", uid=" + i4 + " requires " + serviceRecord2.permission);
            return new ServiceLookupResult(null, serviceRecord2.permission);
        }
        if (serviceRecord2.permission != null && str2 != null && (iPermissionToOpCode = AppOpsManager.permissionToOpCode(serviceRecord2.permission)) != -1 && this.mAm.mAppOpsService.noteOperation(iPermissionToOpCode, i4, str2) != 0) {
            Slog.w(TAG, "Appop Denial: Accessing service " + serviceRecord2.name + " from pid=" + i5 + ", uid=" + i4 + " requires appop " + AppOpsManager.opToName(iPermissionToOpCode));
            return null;
        }
        if (!this.mAm.mIntentFirewall.checkService(serviceRecord2.name, intent, i4, i5, str, serviceRecord2.appInfo)) {
            return null;
        }
        return new ServiceLookupResult(serviceRecord2, null);
    }

    private final void bumpServiceExecutingLocked(ServiceRecord serviceRecord, boolean z, String str) {
        boolean z2;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, ">>> EXECUTING " + str + " of " + serviceRecord + " in app " + serviceRecord.app);
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            Slog.v(TAG_SERVICE_EXECUTING, ">>> EXECUTING " + str + " of " + serviceRecord.shortName);
        }
        if (this.mAm.mBootPhase < 600 && serviceRecord.app != null && serviceRecord.app.pid == Process.myPid()) {
            Slog.w(TAG, "Too early to start/bind service in system_server: Phase=" + this.mAm.mBootPhase + " " + serviceRecord.getComponentName());
            z2 = false;
        } else {
            z2 = true;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (serviceRecord.executeNesting == 0) {
            serviceRecord.executeFg = z;
            ServiceState tracker = serviceRecord.getTracker();
            if (tracker != null) {
                tracker.setExecuting(true, this.mAm.mProcessStats.getMemFactorLocked(), jUptimeMillis);
            }
            if (serviceRecord.app != null) {
                serviceRecord.app.executingServices.add(serviceRecord);
                serviceRecord.app.execServicesFg |= z;
                if (z2 && serviceRecord.app.executingServices.size() == 1) {
                    scheduleServiceTimeoutLocked(serviceRecord.app);
                }
            }
        } else if (serviceRecord.app != null && z && !serviceRecord.app.execServicesFg) {
            serviceRecord.app.execServicesFg = true;
            if (z2) {
                scheduleServiceTimeoutLocked(serviceRecord.app);
            }
        }
        serviceRecord.executeFg = z | serviceRecord.executeFg;
        serviceRecord.executeNesting++;
        serviceRecord.executingStart = jUptimeMillis;
    }

    private final boolean requestServiceBindingLocked(ServiceRecord serviceRecord, IntentBindRecord intentBindRecord, boolean z, boolean z2) throws TransactionTooLargeException {
        if (serviceRecord.app == null || serviceRecord.app.thread == null) {
            return false;
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.d(TAG_SERVICE, "requestBind " + intentBindRecord + ": requested=" + intentBindRecord.requested + " rebind=" + z2);
        }
        if ((!intentBindRecord.requested || z2) && intentBindRecord.apps.size() > 0) {
            try {
                bumpServiceExecutingLocked(serviceRecord, z, "bind");
                serviceRecord.app.forceProcessStateUpTo(9);
                serviceRecord.app.thread.scheduleBindService(serviceRecord, intentBindRecord.intent.getIntent(), z2, serviceRecord.app.repProcState);
                if (!z2) {
                    intentBindRecord.requested = true;
                }
                intentBindRecord.hasBound = true;
                intentBindRecord.doRebind = false;
            } catch (TransactionTooLargeException e) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Crashed while binding " + serviceRecord, e);
                }
                boolean zContains = this.mDestroyingServices.contains(serviceRecord);
                serviceDoneExecutingLocked(serviceRecord, zContains, zContains);
                throw e;
            } catch (RemoteException e2) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Crashed while binding " + serviceRecord);
                }
                boolean zContains2 = this.mDestroyingServices.contains(serviceRecord);
                serviceDoneExecutingLocked(serviceRecord, zContains2, zContains2);
                return false;
            }
        }
        return true;
    }

    private final boolean scheduleServiceRestartLocked(ServiceRecord serviceRecord, boolean z) {
        boolean z2;
        long j;
        boolean z3;
        long j2;
        boolean z4;
        boolean z5;
        long j3;
        int i = 0;
        if (this.mAm.isShuttingDownLocked()) {
            Slog.w(TAG, "Not scheduling restart of crashed service " + serviceRecord.shortName + " - system is shutting down");
            return false;
        }
        ServiceMap serviceMapLocked = getServiceMapLocked(serviceRecord.userId);
        if (serviceMapLocked.mServicesByName.get(serviceRecord.name) != serviceRecord) {
            Slog.wtf(TAG, "Attempting to schedule restart of " + serviceRecord + " when found in map: " + serviceMapLocked.mServicesByName.get(serviceRecord.name));
            return false;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        int i2 = 3;
        if ((serviceRecord.serviceInfo.applicationInfo.flags & 8) == 0) {
            long j4 = this.mAm.mConstants.SERVICE_RESTART_DURATION;
            long j5 = this.mAm.mConstants.SERVICE_RESET_RUN_DURATION;
            int size = serviceRecord.deliveredStarts.size();
            if (size > 0) {
                int i3 = size - 1;
                boolean z6 = false;
                while (i3 >= 0) {
                    ServiceRecord.StartItem startItem = serviceRecord.deliveredStarts.get(i3);
                    startItem.removeUriPermissionsLocked();
                    if (startItem.intent == null) {
                        j3 = jUptimeMillis;
                    } else if (!z || (startItem.deliveryCount < i2 && startItem.doneExecutingCount < 6)) {
                        serviceRecord.pendingStarts.add(i, startItem);
                        j3 = jUptimeMillis;
                        long jUptimeMillis2 = (SystemClock.uptimeMillis() - startItem.deliveredTime) * 2;
                        if (j4 < jUptimeMillis2) {
                            j4 = jUptimeMillis2;
                        }
                        if (j5 < jUptimeMillis2) {
                            j5 = jUptimeMillis2;
                        }
                    } else {
                        Slog.w(TAG, "Canceling start item " + startItem.intent + " in service " + serviceRecord.name);
                        j3 = jUptimeMillis;
                        z6 = true;
                    }
                    i3--;
                    jUptimeMillis = j3;
                    i = 0;
                    i2 = 3;
                }
                j2 = jUptimeMillis;
                serviceRecord.deliveredStarts.clear();
                z4 = z6;
            } else {
                j2 = jUptimeMillis;
                z4 = false;
            }
            serviceRecord.totalRestartCount++;
            if (serviceRecord.restartDelay == 0) {
                serviceRecord.restartCount++;
                serviceRecord.restartDelay = j4;
            } else if (serviceRecord.crashCount > 1) {
                serviceRecord.restartDelay = this.mAm.mConstants.BOUND_SERVICE_CRASH_RESTART_DURATION * ((long) (serviceRecord.crashCount - 1));
            } else if (j2 > serviceRecord.restartTime + j5) {
                serviceRecord.restartCount = 1;
                serviceRecord.restartDelay = j4;
            } else {
                serviceRecord.restartDelay *= (long) this.mAm.mConstants.SERVICE_RESTART_DURATION_FACTOR;
                if (serviceRecord.restartDelay < j4) {
                    serviceRecord.restartDelay = j4;
                }
            }
            serviceRecord.nextRestartTime = j2 + serviceRecord.restartDelay;
            do {
                long j6 = this.mAm.mConstants.SERVICE_MIN_RESTART_TIME_BETWEEN;
                int size2 = this.mRestartingServices.size() - 1;
                while (true) {
                    if (size2 >= 0) {
                        ServiceRecord serviceRecord2 = this.mRestartingServices.get(size2);
                        if (serviceRecord2 != serviceRecord && serviceRecord.nextRestartTime >= serviceRecord2.nextRestartTime - j6 && serviceRecord.nextRestartTime < serviceRecord2.nextRestartTime + j6) {
                            serviceRecord.nextRestartTime = serviceRecord2.nextRestartTime + j6;
                            serviceRecord.restartDelay = serviceRecord.nextRestartTime - j2;
                            z5 = true;
                            break;
                        }
                        size2--;
                    } else {
                        z5 = false;
                        break;
                    }
                }
            } while (z5);
            z3 = z4;
            j = j2;
            z2 = false;
        } else {
            serviceRecord.totalRestartCount++;
            z2 = false;
            serviceRecord.restartCount = 0;
            serviceRecord.restartDelay = 0L;
            j = jUptimeMillis;
            serviceRecord.nextRestartTime = j;
            z3 = false;
        }
        if (!this.mRestartingServices.contains(serviceRecord)) {
            serviceRecord.createdFromFg = z2;
            this.mRestartingServices.add(serviceRecord);
            serviceRecord.makeRestarting(this.mAm.mProcessStats.getMemFactorLocked(), j);
        }
        cancelForegroundNotificationLocked(serviceRecord);
        this.mAm.mHandler.removeCallbacks(serviceRecord.restarter);
        this.mAm.mHandler.postAtTime(serviceRecord.restarter, serviceRecord.nextRestartTime);
        serviceRecord.nextRestartTime = SystemClock.uptimeMillis() + serviceRecord.restartDelay;
        Slog.w(TAG, "Scheduling restart of crashed service " + serviceRecord.shortName + " in " + serviceRecord.restartDelay + "ms");
        EventLog.writeEvent(EventLogTags.AM_SCHEDULE_SERVICE_RESTART, Integer.valueOf(serviceRecord.userId), serviceRecord.shortName, Long.valueOf(serviceRecord.restartDelay));
        return z3;
    }

    final void performServiceRestartLocked(ServiceRecord serviceRecord) {
        if (!this.mRestartingServices.contains(serviceRecord)) {
            return;
        }
        if (!isServiceNeededLocked(serviceRecord, false, false)) {
            Slog.wtf(TAG, "Restarting service that is not needed: " + serviceRecord);
            return;
        }
        try {
            bringUpServiceLocked(serviceRecord, serviceRecord.intent.getIntent().getFlags(), serviceRecord.createdFromFg, true, false);
        } catch (TransactionTooLargeException e) {
        }
    }

    private final boolean unscheduleServiceRestartLocked(ServiceRecord serviceRecord, int i, boolean z) {
        if (!z && serviceRecord.restartDelay == 0) {
            return false;
        }
        boolean zRemove = this.mRestartingServices.remove(serviceRecord);
        if (zRemove || i != serviceRecord.appInfo.uid) {
            serviceRecord.resetRestartCounter();
        }
        if (zRemove) {
            clearRestartingIfNeededLocked(serviceRecord);
        }
        this.mAm.mHandler.removeCallbacks(serviceRecord.restarter);
        return true;
    }

    private void clearRestartingIfNeededLocked(ServiceRecord serviceRecord) {
        if (serviceRecord.restartTracker != null) {
            boolean z = true;
            int size = this.mRestartingServices.size() - 1;
            while (true) {
                if (size >= 0) {
                    if (this.mRestartingServices.get(size).restartTracker == serviceRecord.restartTracker) {
                        break;
                    } else {
                        size--;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            if (!z) {
                serviceRecord.restartTracker.setRestarting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                serviceRecord.restartTracker = null;
            }
        }
    }

    private String bringUpServiceLocked(ServiceRecord serviceRecord, int i, boolean z, boolean z2, boolean z3) throws TransactionTooLargeException {
        ProcessRecord processRecordStartProcessLocked;
        String str;
        String strOnReadyToStartComponent;
        if (serviceRecord.app != null && serviceRecord.app.thread != null) {
            sendServiceArgsLocked(serviceRecord, z, false);
            return null;
        }
        if (!z2 && this.mRestartingServices.contains(serviceRecord)) {
            return null;
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Bringing up " + serviceRecord + " " + serviceRecord.intent + " fg=" + serviceRecord.fgRequired);
        }
        if (this.mRestartingServices.remove(serviceRecord)) {
            clearRestartingIfNeededLocked(serviceRecord);
        }
        if (serviceRecord.delayed) {
            if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "REM FR DELAY LIST (bring up): " + serviceRecord);
            }
            getServiceMapLocked(serviceRecord.userId).mDelayedStartList.remove(serviceRecord);
            serviceRecord.delayed = false;
        }
        if (!this.mAm.mUserController.hasStartedUserState(serviceRecord.userId)) {
            String str2 = "Unable to launch app " + serviceRecord.appInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceRecord.appInfo.uid + " for service " + serviceRecord.intent.getIntent() + ": user " + serviceRecord.userId + " is stopped";
            Slog.w(TAG, str2);
            bringDownServiceLocked(serviceRecord);
            return str2;
        }
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(serviceRecord.packageName, false, serviceRecord.userId);
        } catch (RemoteException e) {
        } catch (IllegalArgumentException e2) {
            Slog.w(TAG, "Failed trying to unstop package " + serviceRecord.packageName + ": " + e2);
        }
        boolean z4 = (serviceRecord.serviceInfo.flags & 2) != 0;
        String str3 = serviceRecord.processName;
        if (!z4) {
            processRecordStartProcessLocked = this.mAm.getProcessRecordLocked(str3, serviceRecord.appInfo.uid, false);
            if (ActivityManagerDebugConfig.DEBUG_MU) {
                Slog.v(TAG_MU, "bringUpServiceLocked: appInfo.uid=" + serviceRecord.appInfo.uid + " app=" + processRecordStartProcessLocked);
            }
            if (processRecordStartProcessLocked != null && processRecordStartProcessLocked.thread != null) {
                try {
                    processRecordStartProcessLocked.addPackage(serviceRecord.appInfo.packageName, serviceRecord.appInfo.longVersionCode, this.mAm.mProcessStats);
                    realStartServiceLocked(serviceRecord, processRecordStartProcessLocked, z);
                    return null;
                } catch (TransactionTooLargeException e3) {
                    throw e3;
                } catch (RemoteException e4) {
                    Slog.w(TAG, "Exception when starting service " + serviceRecord.shortName, e4);
                }
            }
        } else {
            processRecordStartProcessLocked = serviceRecord.isolatedProc;
            if (WebViewZygote.isMultiprocessEnabled() && serviceRecord.serviceInfo.packageName.equals(WebViewZygote.getPackageName())) {
                str = "webview_service";
            }
            if ((processRecordStartProcessLocked != null || (processRecordStartProcessLocked != null && processRecordStartProcessLocked.pid == 0)) && !z3) {
                strOnReadyToStartComponent = "allowed";
                if ("1".equals(SystemProperties.get("persist.vendor.duraspeed.support"))) {
                    strOnReadyToStartComponent = this.mAm.mAmsExt.onReadyToStartComponent(serviceRecord.appInfo.packageName, serviceRecord.appInfo.uid, "service");
                }
                if (strOnReadyToStartComponent != null && strOnReadyToStartComponent.equals("skipped")) {
                    Slog.d(TAG, "bringUpServiceLocked, suppress to start service!");
                    try {
                        AppGlobals.getPackageManager().setPackageStoppedState(serviceRecord.packageName, true, serviceRecord.userId);
                    } catch (Exception e5) {
                        Slog.w(TAG, "Exception: " + e5);
                    }
                } else {
                    processRecordStartProcessLocked = this.mAm.startProcessLocked(str3, serviceRecord.appInfo, true, i, str, serviceRecord.name, false, z4, false);
                    if (processRecordStartProcessLocked == null) {
                        String str4 = "Unable to launch app " + serviceRecord.appInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + serviceRecord.appInfo.uid + " for service " + serviceRecord.intent.getIntent() + ": process is bad";
                        Slog.w(TAG, str4);
                        bringDownServiceLocked(serviceRecord);
                        return str4;
                    }
                }
                if (z4) {
                    serviceRecord.isolatedProc = processRecordStartProcessLocked;
                }
            }
            if (serviceRecord.fgRequired) {
                if (ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE) {
                    Slog.v(TAG, "Whitelisting " + UserHandle.formatUid(serviceRecord.appInfo.uid) + " for fg-service launch");
                }
                this.mAm.tempWhitelistUidLocked(serviceRecord.appInfo.uid, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, "fg-service-launch");
            }
            if (!this.mPendingServices.contains(serviceRecord)) {
                this.mPendingServices.add(serviceRecord);
            }
            if (serviceRecord.delayedStop) {
                serviceRecord.delayedStop = false;
                if (serviceRecord.startRequested) {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "Applying delayed stop (in bring up): " + serviceRecord);
                    }
                    stopServiceLocked(serviceRecord);
                }
            }
            return null;
        }
        str = "service";
        if (processRecordStartProcessLocked != null) {
            strOnReadyToStartComponent = "allowed";
            if ("1".equals(SystemProperties.get("persist.vendor.duraspeed.support"))) {
            }
            if (strOnReadyToStartComponent != null) {
                processRecordStartProcessLocked = this.mAm.startProcessLocked(str3, serviceRecord.appInfo, true, i, str, serviceRecord.name, false, z4, false);
                if (processRecordStartProcessLocked == null) {
                }
                if (z4) {
                }
            }
        } else {
            strOnReadyToStartComponent = "allowed";
            if ("1".equals(SystemProperties.get("persist.vendor.duraspeed.support"))) {
            }
            if (strOnReadyToStartComponent != null) {
            }
        }
        if (serviceRecord.fgRequired) {
        }
        if (!this.mPendingServices.contains(serviceRecord)) {
        }
        if (serviceRecord.delayedStop) {
        }
        return null;
    }

    private final void requestServiceBindingsLocked(ServiceRecord serviceRecord, boolean z) throws TransactionTooLargeException {
        for (int size = serviceRecord.bindings.size() - 1; size >= 0 && requestServiceBindingLocked(serviceRecord, serviceRecord.bindings.valueAt(size), z, false); size--) {
        }
    }

    private final void realStartServiceLocked(ServiceRecord serviceRecord, ProcessRecord processRecord, boolean z) throws RemoteException {
        if (processRecord.thread == null) {
            throw new RemoteException();
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "realStartServiceLocked, ServiceRecord.uid = " + serviceRecord.appInfo.uid + ", ProcessRecord.uid = " + processRecord.uid);
        }
        serviceRecord.app = processRecord;
        long jUptimeMillis = SystemClock.uptimeMillis();
        serviceRecord.lastActivity = jUptimeMillis;
        serviceRecord.restartTime = jUptimeMillis;
        boolean zAdd = processRecord.services.add(serviceRecord);
        bumpServiceExecutingLocked(serviceRecord, z, "create");
        this.mAm.updateLruProcessLocked(processRecord, false, null);
        updateServiceForegroundLocked(serviceRecord.app, false);
        this.mAm.updateOomAdjLocked();
        try {
            try {
                synchronized (serviceRecord.stats.getBatteryStats()) {
                    serviceRecord.stats.startLaunchedLocked();
                }
                this.mAm.notifyPackageUse(serviceRecord.serviceInfo.packageName, 1);
                processRecord.forceProcessStateUpTo(9);
                processRecord.thread.scheduleCreateService(serviceRecord, serviceRecord.serviceInfo, this.mAm.compatibilityInfoForPackageLocked(serviceRecord.serviceInfo.applicationInfo), processRecord.repProcState);
                serviceRecord.postNotification();
                if (serviceRecord.whitelistManager) {
                    processRecord.whitelistManager = true;
                }
                requestServiceBindingsLocked(serviceRecord, z);
                updateServiceClientActivitiesLocked(processRecord, null, true);
                if (serviceRecord.startRequested && serviceRecord.callStart && serviceRecord.pendingStarts.size() == 0) {
                    serviceRecord.pendingStarts.add(new ServiceRecord.StartItem(serviceRecord, false, serviceRecord.makeNextStartId(), null, null, 0));
                }
                sendServiceArgsLocked(serviceRecord, z, true);
                if (serviceRecord.delayed) {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "REM FR DELAY LIST (new proc): " + serviceRecord);
                    }
                    getServiceMapLocked(serviceRecord.userId).mDelayedStartList.remove(serviceRecord);
                    serviceRecord.delayed = false;
                }
                if (serviceRecord.delayedStop) {
                    serviceRecord.delayedStop = false;
                    if (serviceRecord.startRequested) {
                        if (DEBUG_DELAYED_STARTS) {
                            Slog.v(TAG_SERVICE, "Applying delayed stop (from start): " + serviceRecord);
                        }
                        stopServiceLocked(serviceRecord);
                    }
                }
            } catch (DeadObjectException e) {
                Slog.w(TAG, "Application dead when creating service " + serviceRecord);
                this.mAm.appDiedLocked(processRecord);
                throw e;
            }
        } catch (Throwable th) {
            boolean zContains = this.mDestroyingServices.contains(serviceRecord);
            serviceDoneExecutingLocked(serviceRecord, zContains, zContains);
            if (zAdd) {
                processRecord.services.remove(serviceRecord);
                serviceRecord.app = null;
            }
            if (!zContains) {
                scheduleServiceRestartLocked(serviceRecord, false);
            }
            throw th;
        }
    }

    private final void sendServiceArgsLocked(ServiceRecord serviceRecord, boolean z, boolean z2) throws TransactionTooLargeException {
        int i;
        int size = serviceRecord.pendingStarts.size();
        if (size == 0) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        while (true) {
            if (serviceRecord.pendingStarts.size() <= 0) {
                break;
            }
            ServiceRecord.StartItem startItemRemove = serviceRecord.pendingStarts.remove(0);
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Sending arguments to: " + serviceRecord + " " + serviceRecord.intent + " args=" + startItemRemove.intent);
            }
            if (startItemRemove.intent != null || size <= 1) {
                startItemRemove.deliveredTime = SystemClock.uptimeMillis();
                serviceRecord.deliveredStarts.add(startItemRemove);
                startItemRemove.deliveryCount++;
                if (startItemRemove.neededGrants != null) {
                    this.mAm.grantUriPermissionUncheckedFromIntentLocked(startItemRemove.neededGrants, startItemRemove.getUriPermissionsLocked());
                }
                this.mAm.grantEphemeralAccessLocked(serviceRecord.userId, startItemRemove.intent, serviceRecord.appInfo.uid, UserHandle.getAppId(startItemRemove.callingId));
                bumpServiceExecutingLocked(serviceRecord, z, "start");
                if (!z2) {
                    this.mAm.updateOomAdjLocked(serviceRecord.app, true);
                    z2 = true;
                }
                if (serviceRecord.fgRequired && !serviceRecord.fgWaiting) {
                    if (!serviceRecord.isForeground) {
                        if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                            Slog.i(TAG, "Launched service must call startForeground() within timeout: " + serviceRecord);
                        }
                        scheduleServiceForegroundTransitionTimeoutLocked(serviceRecord);
                    } else {
                        if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                            Slog.i(TAG, "Service already foreground; no new timeout: " + serviceRecord);
                        }
                        serviceRecord.fgRequired = false;
                    }
                }
                i = startItemRemove.deliveryCount > 1 ? 2 : 0;
                if (startItemRemove.doneExecutingCount > 0) {
                    i |= 1;
                }
                arrayList.add(new ServiceStartArgs(startItemRemove.taskRemoved, startItemRemove.id, i, startItemRemove.intent));
            }
        }
        ParceledListSlice parceledListSlice = new ParceledListSlice(arrayList);
        parceledListSlice.setInlineCountLimit(4);
        Exception exc = null;
        try {
            serviceRecord.app.thread.scheduleServiceArgs(serviceRecord, parceledListSlice);
        } catch (TransactionTooLargeException e) {
            exc = e;
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Transaction too large for " + arrayList.size() + " args, first: " + ((ServiceStartArgs) arrayList.get(0)).args);
            }
            Slog.w(TAG, "Failed delivering service starts", exc);
        } catch (RemoteException e2) {
            exc = e2;
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Crashed while sending args: " + serviceRecord);
            }
            Slog.w(TAG, "Failed delivering service starts", exc);
        } catch (Exception e3) {
            exc = e3;
            Slog.w(TAG, "Unexpected exception", exc);
        }
        if (exc != null) {
            boolean zContains = this.mDestroyingServices.contains(serviceRecord);
            while (i < arrayList.size()) {
                serviceDoneExecutingLocked(serviceRecord, zContains, zContains);
                i++;
            }
            if (exc instanceof TransactionTooLargeException) {
                throw ((TransactionTooLargeException) exc);
            }
        }
    }

    private final boolean isServiceNeededLocked(ServiceRecord serviceRecord, boolean z, boolean z2) {
        if (serviceRecord.startRequested) {
            return true;
        }
        if (!z) {
            z2 = serviceRecord.hasAutoCreateConnections();
        }
        return z2;
    }

    private final void bringDownServiceIfNeededLocked(ServiceRecord serviceRecord, boolean z, boolean z2) {
        if (isServiceNeededLocked(serviceRecord, z, z2) || this.mPendingServices.contains(serviceRecord)) {
            return;
        }
        bringDownServiceLocked(serviceRecord);
    }

    private final void bringDownServiceLocked(ServiceRecord serviceRecord) {
        int size = serviceRecord.connections.size() - 1;
        while (true) {
            if (size < 0) {
                break;
            }
            ArrayList<ConnectionRecord> arrayListValueAt = serviceRecord.connections.valueAt(size);
            for (int i = 0; i < arrayListValueAt.size(); i++) {
                ConnectionRecord connectionRecord = arrayListValueAt.get(i);
                connectionRecord.serviceDead = true;
                try {
                    connectionRecord.conn.connected(serviceRecord.name, (IBinder) null, true);
                } catch (Exception e) {
                    Slog.w(TAG, "Failure disconnecting service " + serviceRecord.name + " to connection " + arrayListValueAt.get(i).conn.asBinder() + " (in " + arrayListValueAt.get(i).binding.client.processName + ")", e);
                }
            }
            size--;
        }
        if (serviceRecord.app != null && serviceRecord.app.thread != null) {
            for (int size2 = serviceRecord.bindings.size() - 1; size2 >= 0; size2--) {
                IntentBindRecord intentBindRecordValueAt = serviceRecord.bindings.valueAt(size2);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Bringing down binding " + intentBindRecordValueAt + ": hasBound=" + intentBindRecordValueAt.hasBound);
                }
                if (intentBindRecordValueAt.hasBound) {
                    try {
                        bumpServiceExecutingLocked(serviceRecord, false, "bring down unbind");
                        this.mAm.updateOomAdjLocked(serviceRecord.app, true);
                        intentBindRecordValueAt.hasBound = false;
                        intentBindRecordValueAt.requested = false;
                        serviceRecord.app.thread.scheduleUnbindService(serviceRecord, intentBindRecordValueAt.intent.getIntent());
                    } catch (Exception e2) {
                        Slog.w(TAG, "Exception when unbinding service " + serviceRecord.shortName, e2);
                        serviceProcessGoneLocked(serviceRecord);
                    }
                }
            }
        }
        if (serviceRecord.fgRequired) {
            Slog.w(TAG_SERVICE, "Bringing down service while still waiting for start foreground: " + serviceRecord);
            serviceRecord.fgRequired = false;
            serviceRecord.fgWaiting = false;
            this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
            this.mAm.mHandler.removeMessages(66, serviceRecord);
            if (serviceRecord.app != null) {
                Message messageObtainMessage = this.mAm.mHandler.obtainMessage(69);
                messageObtainMessage.obj = serviceRecord.app;
                messageObtainMessage.getData().putCharSequence("servicerecord", serviceRecord.toString());
                this.mAm.mHandler.sendMessage(messageObtainMessage);
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            RuntimeException runtimeException = new RuntimeException();
            runtimeException.fillInStackTrace();
            Slog.v(TAG_SERVICE, "Bringing down " + serviceRecord + " " + serviceRecord.intent, runtimeException);
        }
        serviceRecord.destroyTime = SystemClock.uptimeMillis();
        ServiceMap serviceMapLocked = getServiceMapLocked(serviceRecord.userId);
        ServiceRecord serviceRecordRemove = serviceMapLocked.mServicesByName.remove(serviceRecord.name);
        if (serviceRecordRemove != null && serviceRecordRemove != serviceRecord) {
            serviceMapLocked.mServicesByName.put(serviceRecord.name, serviceRecordRemove);
            throw new IllegalStateException("Bringing down " + serviceRecord + " but actually running " + serviceRecordRemove);
        }
        serviceMapLocked.mServicesByIntent.remove(serviceRecord.intent);
        serviceRecord.totalRestartCount = 0;
        unscheduleServiceRestartLocked(serviceRecord, 0, true);
        for (int size3 = this.mPendingServices.size() - 1; size3 >= 0; size3--) {
            if (this.mPendingServices.get(size3) == serviceRecord) {
                this.mPendingServices.remove(size3);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Removed pending: " + serviceRecord);
                }
            }
        }
        cancelForegroundNotificationLocked(serviceRecord);
        if (serviceRecord.isForeground) {
            decActiveForegroundAppLocked(serviceMapLocked, serviceRecord);
            this.mAm.mAppOpsService.finishOperation(AppOpsManager.getToken(this.mAm.mAppOpsService), 76, serviceRecord.appInfo.uid, serviceRecord.packageName);
            StatsLog.write(60, serviceRecord.appInfo.uid, serviceRecord.shortName, 2);
        }
        serviceRecord.isForeground = false;
        serviceRecord.foregroundId = 0;
        serviceRecord.foregroundNoti = null;
        serviceRecord.clearDeliveredStartsLocked();
        serviceRecord.pendingStarts.clear();
        if (serviceRecord.app != null) {
            synchronized (serviceRecord.stats.getBatteryStats()) {
                serviceRecord.stats.stopLaunchedLocked();
            }
            serviceRecord.app.services.remove(serviceRecord);
            if (serviceRecord.whitelistManager) {
                updateWhitelistManagerLocked(serviceRecord.app);
            }
            if (serviceRecord.app.thread != null) {
                updateServiceForegroundLocked(serviceRecord.app, false);
                try {
                    bumpServiceExecutingLocked(serviceRecord, false, "destroy");
                    this.mDestroyingServices.add(serviceRecord);
                    serviceRecord.destroying = true;
                    this.mAm.updateOomAdjLocked(serviceRecord.app, true);
                    serviceRecord.app.thread.scheduleStopService(serviceRecord);
                } catch (Exception e3) {
                    Slog.w(TAG, "Exception when destroying service " + serviceRecord.shortName, e3);
                    serviceProcessGoneLocked(serviceRecord);
                }
            } else if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Removed service that has no process: " + serviceRecord);
            }
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Removed service that is not running: " + serviceRecord);
        }
        if (serviceRecord.bindings.size() > 0) {
            serviceRecord.bindings.clear();
        }
        if (serviceRecord.restarter instanceof ServiceRestarter) {
            ((ServiceRestarter) serviceRecord.restarter).setService(null);
        }
        int memFactorLocked = this.mAm.mProcessStats.getMemFactorLocked();
        long jUptimeMillis = SystemClock.uptimeMillis();
        if (serviceRecord.tracker != null) {
            serviceRecord.tracker.setStarted(false, memFactorLocked, jUptimeMillis);
            serviceRecord.tracker.setBound(false, memFactorLocked, jUptimeMillis);
            if (serviceRecord.executeNesting == 0) {
                serviceRecord.tracker.clearCurrentOwner(serviceRecord, false);
                serviceRecord.tracker = null;
            }
        }
        serviceMapLocked.ensureNotStartingBackgroundLocked(serviceRecord);
    }

    void removeConnectionLocked(ConnectionRecord connectionRecord, ProcessRecord processRecord, ActivityRecord activityRecord) {
        IBinder iBinderAsBinder = connectionRecord.conn.asBinder();
        AppBindRecord appBindRecord = connectionRecord.binding;
        ServiceRecord serviceRecord = appBindRecord.service;
        ArrayList<ConnectionRecord> arrayList = serviceRecord.connections.get(iBinderAsBinder);
        if (arrayList != null) {
            arrayList.remove(connectionRecord);
            if (arrayList.size() == 0) {
                serviceRecord.connections.remove(iBinderAsBinder);
            }
        }
        appBindRecord.connections.remove(connectionRecord);
        if (connectionRecord.activity != null && connectionRecord.activity != activityRecord && connectionRecord.activity.connections != null) {
            connectionRecord.activity.connections.remove(connectionRecord);
        }
        if (appBindRecord.client != processRecord) {
            appBindRecord.client.connections.remove(connectionRecord);
            if ((connectionRecord.flags & 8) != 0) {
                appBindRecord.client.updateHasAboveClientLocked();
            }
            if ((connectionRecord.flags & DumpState.DUMP_SERVICE_PERMISSIONS) != 0) {
                serviceRecord.updateWhitelistManager();
                if (!serviceRecord.whitelistManager && serviceRecord.app != null) {
                    updateWhitelistManagerLocked(serviceRecord.app);
                }
            }
            if (serviceRecord.app != null) {
                updateServiceClientActivitiesLocked(serviceRecord.app, connectionRecord, true);
            }
        }
        ArrayList<ConnectionRecord> arrayList2 = this.mServiceConnections.get(iBinderAsBinder);
        if (arrayList2 != null) {
            arrayList2.remove(connectionRecord);
            if (arrayList2.size() == 0) {
                this.mServiceConnections.remove(iBinderAsBinder);
            }
        }
        this.mAm.stopAssociationLocked(appBindRecord.client.uid, appBindRecord.client.processName, serviceRecord.appInfo.uid, serviceRecord.name);
        if (appBindRecord.connections.size() == 0) {
            appBindRecord.intent.apps.remove(appBindRecord.client);
        }
        if (!connectionRecord.serviceDead) {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Disconnecting binding " + appBindRecord.intent + ": shouldUnbind=" + appBindRecord.intent.hasBound);
            }
            if (serviceRecord.app != null && serviceRecord.app.thread != null && appBindRecord.intent.apps.size() == 0 && appBindRecord.intent.hasBound) {
                try {
                    bumpServiceExecutingLocked(serviceRecord, false, "unbind");
                    if (appBindRecord.client != serviceRecord.app && (connectionRecord.flags & 32) == 0 && serviceRecord.app.setProcState <= 12) {
                        this.mAm.updateLruProcessLocked(serviceRecord.app, false, null);
                    }
                    this.mAm.updateOomAdjLocked(serviceRecord.app, true);
                    appBindRecord.intent.hasBound = false;
                    appBindRecord.intent.doRebind = false;
                    serviceRecord.app.thread.scheduleUnbindService(serviceRecord, appBindRecord.intent.intent.getIntent());
                } catch (Exception e) {
                    Slog.w(TAG, "Exception when unbinding service " + serviceRecord.shortName, e);
                    serviceProcessGoneLocked(serviceRecord);
                }
            }
            this.mPendingServices.remove(serviceRecord);
            if ((connectionRecord.flags & 1) != 0) {
                boolean zHasAutoCreateConnections = serviceRecord.hasAutoCreateConnections();
                if (!zHasAutoCreateConnections && serviceRecord.tracker != null) {
                    serviceRecord.tracker.setBound(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
                bringDownServiceIfNeededLocked(serviceRecord, true, zHasAutoCreateConnections);
            }
        }
    }

    void serviceDoneExecutingLocked(ServiceRecord serviceRecord, int i, int i2, int i3) {
        boolean zContains = this.mDestroyingServices.contains(serviceRecord);
        if (serviceRecord != null) {
            if (i == 1) {
                serviceRecord.callStart = true;
                if (i3 != 1000) {
                    switch (i3) {
                        case 0:
                        case 1:
                            serviceRecord.findDeliveredStart(i2, false, true);
                            serviceRecord.stopIfKilled = false;
                            break;
                        case 2:
                            serviceRecord.findDeliveredStart(i2, false, true);
                            if (serviceRecord.getLastStartId() == i2) {
                                serviceRecord.stopIfKilled = true;
                            }
                            break;
                        case 3:
                            ServiceRecord.StartItem startItemFindDeliveredStart = serviceRecord.findDeliveredStart(i2, false, false);
                            if (startItemFindDeliveredStart != null) {
                                startItemFindDeliveredStart.deliveryCount = 0;
                                startItemFindDeliveredStart.doneExecutingCount++;
                                serviceRecord.stopIfKilled = true;
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown service start result: " + i3);
                    }
                } else {
                    serviceRecord.findDeliveredStart(i2, true, true);
                }
                if (i3 == 0) {
                    serviceRecord.callStart = false;
                }
            } else if (i == 2) {
                if (zContains) {
                    if (serviceRecord.executeNesting != 1) {
                        Slog.w(TAG, "Service done with onDestroy, but executeNesting=" + serviceRecord.executeNesting + ": " + serviceRecord);
                        serviceRecord.executeNesting = 1;
                    }
                } else if (serviceRecord.app != null) {
                    Slog.w(TAG, "Service done with onDestroy, but not inDestroying: " + serviceRecord + ", app=" + serviceRecord.app);
                }
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            serviceDoneExecutingLocked(serviceRecord, zContains, zContains);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return;
        }
        Slog.w(TAG, "Done executing unknown service from pid " + Binder.getCallingPid());
    }

    private void serviceProcessGoneLocked(ServiceRecord serviceRecord) {
        if (serviceRecord.tracker != null) {
            int memFactorLocked = this.mAm.mProcessStats.getMemFactorLocked();
            long jUptimeMillis = SystemClock.uptimeMillis();
            serviceRecord.tracker.setExecuting(false, memFactorLocked, jUptimeMillis);
            serviceRecord.tracker.setBound(false, memFactorLocked, jUptimeMillis);
            serviceRecord.tracker.setStarted(false, memFactorLocked, jUptimeMillis);
        }
        serviceDoneExecutingLocked(serviceRecord, true, true);
    }

    private void serviceDoneExecutingLocked(ServiceRecord serviceRecord, boolean z, boolean z2) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "<<< DONE EXECUTING " + serviceRecord + ": nesting=" + serviceRecord.executeNesting + ", inDestroying=" + z + ", app=" + serviceRecord.app);
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            Slog.v(TAG_SERVICE_EXECUTING, "<<< DONE EXECUTING " + serviceRecord.shortName);
        }
        serviceRecord.executeNesting--;
        if (serviceRecord.executeNesting <= 0) {
            if (serviceRecord.app != null) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Nesting at 0 of " + serviceRecord.shortName);
                }
                serviceRecord.app.execServicesFg = false;
                serviceRecord.app.executingServices.remove(serviceRecord);
                if (serviceRecord.app.executingServices.size() == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE || ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
                        Slog.v(TAG_SERVICE_EXECUTING, "No more executingServices of " + serviceRecord.shortName);
                    }
                    this.mAm.mHandler.removeMessages(12, serviceRecord.app);
                    this.mAm.mAnrManager.removeServiceMonitorMessage();
                } else if (serviceRecord.executeFg) {
                    int size = serviceRecord.app.executingServices.size() - 1;
                    while (true) {
                        if (size < 0) {
                            break;
                        }
                        if (!serviceRecord.app.executingServices.valueAt(size).executeFg) {
                            size--;
                        } else {
                            serviceRecord.app.execServicesFg = true;
                            break;
                        }
                    }
                }
                if (z) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                        Slog.v(TAG_SERVICE, "doneExecuting remove destroying " + serviceRecord);
                    }
                    this.mDestroyingServices.remove(serviceRecord);
                    serviceRecord.bindings.clear();
                }
                this.mAm.updateOomAdjLocked(serviceRecord.app, true);
            }
            serviceRecord.executeFg = false;
            if (serviceRecord.tracker != null) {
                serviceRecord.tracker.setExecuting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                if (z2) {
                    serviceRecord.tracker.clearCurrentOwner(serviceRecord, false);
                    serviceRecord.tracker = null;
                }
            }
            if (z2) {
                if (serviceRecord.app != null && !serviceRecord.app.persistent) {
                    serviceRecord.app.services.remove(serviceRecord);
                    if (serviceRecord.whitelistManager) {
                        updateWhitelistManagerLocked(serviceRecord.app);
                    }
                }
                serviceRecord.app = null;
            }
        }
    }

    boolean attachApplicationLocked(ProcessRecord processRecord, String str) throws RemoteException {
        boolean z;
        ServiceRecord serviceRecord;
        if (this.mPendingServices.size() > 0) {
            ServiceRecord serviceRecord2 = null;
            int i = 0;
            z = false;
            while (i < this.mPendingServices.size()) {
                try {
                    serviceRecord = this.mPendingServices.get(i);
                } catch (RemoteException e) {
                    e = e;
                    serviceRecord = serviceRecord2;
                }
                try {
                    if (processRecord == serviceRecord.isolatedProc || (processRecord.uid == serviceRecord.appInfo.uid && str.equals(serviceRecord.processName))) {
                        this.mPendingServices.remove(i);
                        i--;
                        processRecord.addPackage(serviceRecord.appInfo.packageName, serviceRecord.appInfo.longVersionCode, this.mAm.mProcessStats);
                        realStartServiceLocked(serviceRecord, processRecord, serviceRecord.createdFromFg);
                        if (!isServiceNeededLocked(serviceRecord, false, false)) {
                            bringDownServiceLocked(serviceRecord);
                        }
                        z = true;
                    }
                    i++;
                    serviceRecord2 = serviceRecord;
                } catch (RemoteException e2) {
                    e = e2;
                    Slog.w(TAG, "Exception in new application when starting service " + serviceRecord.shortName, e);
                    throw e;
                }
            }
        } else {
            z = false;
        }
        if (this.mRestartingServices.size() > 0) {
            for (int i2 = 0; i2 < this.mRestartingServices.size(); i2++) {
                ServiceRecord serviceRecord3 = this.mRestartingServices.get(i2);
                if (processRecord == serviceRecord3.isolatedProc || (processRecord.uid == serviceRecord3.appInfo.uid && str.equals(serviceRecord3.processName))) {
                    this.mAm.mHandler.removeCallbacks(serviceRecord3.restarter);
                    this.mAm.mHandler.post(serviceRecord3.restarter);
                }
            }
        }
        return z;
    }

    void processStartTimedOutLocked(ProcessRecord processRecord) {
        int i = 0;
        while (i < this.mPendingServices.size()) {
            ServiceRecord serviceRecord = this.mPendingServices.get(i);
            if ((processRecord.uid == serviceRecord.appInfo.uid && processRecord.processName.equals(serviceRecord.processName)) || serviceRecord.isolatedProc == processRecord) {
                Slog.w(TAG, "Forcing bringing down service: " + serviceRecord);
                serviceRecord.isolatedProc = null;
                this.mPendingServices.remove(i);
                i += -1;
                bringDownServiceLocked(serviceRecord);
            }
            i++;
        }
    }

    private boolean collectPackageServicesLocked(String str, Set<String> set, boolean z, boolean z2, boolean z3, ArrayMap<ComponentName, ServiceRecord> arrayMap) {
        boolean z4 = false;
        for (int size = arrayMap.size() - 1; size >= 0; size--) {
            ServiceRecord serviceRecordValueAt = arrayMap.valueAt(size);
            if ((str == null || (serviceRecordValueAt.packageName.equals(str) && (set == null || set.contains(serviceRecordValueAt.name.getClassName())))) && (serviceRecordValueAt.app == null || z || !serviceRecordValueAt.app.persistent)) {
                if (!z2) {
                    return true;
                }
                Slog.i(TAG, "  Force stopping service " + serviceRecordValueAt);
                if (serviceRecordValueAt.app != null) {
                    serviceRecordValueAt.app.removed = z3;
                    if (!serviceRecordValueAt.app.persistent) {
                        serviceRecordValueAt.app.services.remove(serviceRecordValueAt);
                        if (serviceRecordValueAt.whitelistManager) {
                            updateWhitelistManagerLocked(serviceRecordValueAt.app);
                        }
                    }
                }
                serviceRecordValueAt.app = null;
                serviceRecordValueAt.isolatedProc = null;
                if (this.mTmpCollectionResults == null) {
                    this.mTmpCollectionResults = new ArrayList<>();
                }
                this.mTmpCollectionResults.add(serviceRecordValueAt);
                z4 = true;
            }
        }
        return z4;
    }

    boolean bringDownDisabledPackageServicesLocked(String str, Set<String> set, int i, boolean z, boolean z2, boolean z3) {
        boolean zCollectPackageServicesLocked;
        if (this.mTmpCollectionResults != null) {
            this.mTmpCollectionResults.clear();
        }
        if (i == -1) {
            zCollectPackageServicesLocked = false;
            for (int size = this.mServiceMap.size() - 1; size >= 0; size--) {
                zCollectPackageServicesLocked |= collectPackageServicesLocked(str, set, z, z3, z2, this.mServiceMap.valueAt(size).mServicesByName);
                if (!z3 && zCollectPackageServicesLocked) {
                    return true;
                }
                if (z3 && set == null) {
                    forceStopPackageLocked(str, this.mServiceMap.valueAt(size).mUserId);
                }
            }
        } else {
            ServiceMap serviceMap = this.mServiceMap.get(i);
            zCollectPackageServicesLocked = serviceMap != null ? collectPackageServicesLocked(str, set, z, z3, z2, serviceMap.mServicesByName) : false;
            if (z3 && set == null) {
                forceStopPackageLocked(str, i);
            }
        }
        if (this.mTmpCollectionResults != null) {
            for (int size2 = this.mTmpCollectionResults.size() - 1; size2 >= 0; size2--) {
                bringDownServiceLocked(this.mTmpCollectionResults.get(size2));
            }
            this.mTmpCollectionResults.clear();
        }
        return zCollectPackageServicesLocked;
    }

    void forceStopPackageLocked(String str, int i) {
        ServiceMap serviceMap = this.mServiceMap.get(i);
        if (serviceMap != null && serviceMap.mActiveForegroundApps.size() > 0) {
            for (int size = serviceMap.mActiveForegroundApps.size() - 1; size >= 0; size--) {
                if (serviceMap.mActiveForegroundApps.valueAt(size).mPackageName.equals(str)) {
                    serviceMap.mActiveForegroundApps.removeAt(size);
                    serviceMap.mActiveForegroundAppsChanged = true;
                }
            }
            if (serviceMap.mActiveForegroundAppsChanged) {
                requestUpdateActiveForegroundAppsLocked(serviceMap, 0L);
            }
        }
    }

    void cleanUpRemovedTaskLocked(TaskRecord taskRecord, ComponentName componentName, Intent intent) {
        ArrayList arrayList = new ArrayList();
        ArrayMap<ComponentName, ServiceRecord> servicesLocked = getServicesLocked(taskRecord.userId);
        for (int size = servicesLocked.size() - 1; size >= 0; size--) {
            ServiceRecord serviceRecordValueAt = servicesLocked.valueAt(size);
            if (serviceRecordValueAt.packageName.equals(componentName.getPackageName())) {
                arrayList.add(serviceRecordValueAt);
            }
        }
        for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
            ServiceRecord serviceRecord = (ServiceRecord) arrayList.get(size2);
            if (serviceRecord.startRequested) {
                if ((serviceRecord.serviceInfo.flags & 1) != 0) {
                    Slog.i(TAG, "Stopping service " + serviceRecord.shortName + ": remove task");
                    stopServiceLocked(serviceRecord);
                } else {
                    serviceRecord.pendingStarts.add(new ServiceRecord.StartItem(serviceRecord, true, serviceRecord.getLastStartId(), intent, null, 0));
                    if (serviceRecord.app != null && serviceRecord.app.thread != null) {
                        try {
                            sendServiceArgsLocked(serviceRecord, true, false);
                        } catch (TransactionTooLargeException e) {
                        }
                    }
                }
            }
        }
    }

    final void killServicesLocked(ProcessRecord processRecord, boolean z) {
        boolean z2;
        for (int size = processRecord.connections.size() - 1; size >= 0; size--) {
            removeConnectionLocked(processRecord.connections.valueAt(size), processRecord, null);
        }
        updateServiceConnectionActivitiesLocked(processRecord);
        processRecord.connections.clear();
        processRecord.whitelistManager = false;
        for (int size2 = processRecord.services.size() - 1; size2 >= 0; size2--) {
            ServiceRecord serviceRecordValueAt = processRecord.services.valueAt(size2);
            synchronized (serviceRecordValueAt.stats.getBatteryStats()) {
                serviceRecordValueAt.stats.stopLaunchedLocked();
            }
            if (serviceRecordValueAt.app != processRecord && serviceRecordValueAt.app != null && !serviceRecordValueAt.app.persistent) {
                serviceRecordValueAt.app.services.remove(serviceRecordValueAt);
            }
            serviceRecordValueAt.app = null;
            serviceRecordValueAt.isolatedProc = null;
            serviceRecordValueAt.executeNesting = 0;
            serviceRecordValueAt.forceClearTracker();
            if (this.mDestroyingServices.remove(serviceRecordValueAt) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "killServices remove destroying " + serviceRecordValueAt);
            }
            for (int size3 = serviceRecordValueAt.bindings.size() - 1; size3 >= 0; size3--) {
                IntentBindRecord intentBindRecordValueAt = serviceRecordValueAt.bindings.valueAt(size3);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Killing binding " + intentBindRecordValueAt + ": shouldUnbind=" + intentBindRecordValueAt.hasBound);
                }
                intentBindRecordValueAt.binder = null;
                intentBindRecordValueAt.hasBound = false;
                intentBindRecordValueAt.received = false;
                intentBindRecordValueAt.requested = false;
                for (int size4 = intentBindRecordValueAt.apps.size() - 1; size4 >= 0; size4--) {
                    ProcessRecord processRecordKeyAt = intentBindRecordValueAt.apps.keyAt(size4);
                    if (!processRecordKeyAt.killedByAm && processRecordKeyAt.thread != null) {
                        AppBindRecord appBindRecordValueAt = intentBindRecordValueAt.apps.valueAt(size4);
                        int size5 = appBindRecordValueAt.connections.size() - 1;
                        while (true) {
                            if (size5 >= 0) {
                                if ((appBindRecordValueAt.connections.valueAt(size5).flags & 49) == 1) {
                                    z2 = true;
                                    break;
                                }
                                size5--;
                            } else {
                                z2 = false;
                                break;
                            }
                        }
                        if (!z2) {
                        }
                    }
                }
            }
        }
        ServiceMap serviceMapLocked = getServiceMapLocked(processRecord.userId);
        for (int size6 = processRecord.services.size() - 1; size6 >= 0; size6--) {
            ServiceRecord serviceRecordValueAt2 = processRecord.services.valueAt(size6);
            if (!processRecord.persistent) {
                processRecord.services.removeAt(size6);
            }
            ServiceRecord serviceRecord = serviceMapLocked.mServicesByName.get(serviceRecordValueAt2.name);
            if (serviceRecord != serviceRecordValueAt2) {
                if (serviceRecord != null) {
                    Slog.wtf(TAG, "Service " + serviceRecordValueAt2 + " in process " + processRecord + " not same as in map: " + serviceRecord);
                }
            } else if (z && serviceRecordValueAt2.crashCount >= this.mAm.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY && (serviceRecordValueAt2.serviceInfo.applicationInfo.flags & 8) == 0) {
                Slog.w(TAG, "Service crashed " + serviceRecordValueAt2.crashCount + " times, stopping: " + serviceRecordValueAt2);
                EventLog.writeEvent(EventLogTags.AM_SERVICE_CRASHED_TOO_MUCH, Integer.valueOf(serviceRecordValueAt2.userId), Integer.valueOf(serviceRecordValueAt2.crashCount), serviceRecordValueAt2.shortName, Integer.valueOf(processRecord.pid));
                bringDownServiceLocked(serviceRecordValueAt2);
            } else if (!z || !this.mAm.mUserController.isUserRunning(serviceRecordValueAt2.userId, 0)) {
                bringDownServiceLocked(serviceRecordValueAt2);
            } else {
                boolean zScheduleServiceRestartLocked = scheduleServiceRestartLocked(serviceRecordValueAt2, true);
                if (serviceRecordValueAt2.startRequested && ((serviceRecordValueAt2.stopIfKilled || zScheduleServiceRestartLocked) && serviceRecordValueAt2.pendingStarts.size() == 0)) {
                    serviceRecordValueAt2.startRequested = false;
                    if (serviceRecordValueAt2.tracker != null) {
                        serviceRecordValueAt2.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                    }
                    if (!serviceRecordValueAt2.hasAutoCreateConnections()) {
                        bringDownServiceLocked(serviceRecordValueAt2);
                    }
                }
            }
        }
        if (!z) {
            processRecord.services.clear();
            for (int size7 = this.mRestartingServices.size() - 1; size7 >= 0; size7--) {
                ServiceRecord serviceRecord2 = this.mRestartingServices.get(size7);
                if (serviceRecord2.processName.equals(processRecord.processName) && serviceRecord2.serviceInfo.applicationInfo.uid == processRecord.info.uid) {
                    this.mRestartingServices.remove(size7);
                    clearRestartingIfNeededLocked(serviceRecord2);
                }
            }
            for (int size8 = this.mPendingServices.size() - 1; size8 >= 0; size8--) {
                ServiceRecord serviceRecord3 = this.mPendingServices.get(size8);
                if (serviceRecord3.processName.equals(processRecord.processName) && serviceRecord3.serviceInfo.applicationInfo.uid == processRecord.info.uid) {
                    this.mPendingServices.remove(size8);
                }
            }
        }
        int size9 = this.mDestroyingServices.size();
        while (size9 > 0) {
            size9--;
            ServiceRecord serviceRecord4 = this.mDestroyingServices.get(size9);
            if (serviceRecord4.app == processRecord) {
                serviceRecord4.forceClearTracker();
                this.mDestroyingServices.remove(size9);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "killServices remove destroying " + serviceRecord4);
                }
            }
        }
        processRecord.executingServices.clear();
    }

    ActivityManager.RunningServiceInfo makeRunningServiceInfoLocked(ServiceRecord serviceRecord) {
        ActivityManager.RunningServiceInfo runningServiceInfo = new ActivityManager.RunningServiceInfo();
        runningServiceInfo.service = serviceRecord.name;
        if (serviceRecord.app != null) {
            runningServiceInfo.pid = serviceRecord.app.pid;
        }
        runningServiceInfo.uid = serviceRecord.appInfo.uid;
        runningServiceInfo.process = serviceRecord.processName;
        runningServiceInfo.foreground = serviceRecord.isForeground;
        runningServiceInfo.activeSince = serviceRecord.createRealTime;
        runningServiceInfo.started = serviceRecord.startRequested;
        runningServiceInfo.clientCount = serviceRecord.connections.size();
        runningServiceInfo.crashCount = serviceRecord.crashCount;
        runningServiceInfo.lastActivityTime = serviceRecord.lastActivity;
        if (serviceRecord.isForeground) {
            runningServiceInfo.flags |= 2;
        }
        if (serviceRecord.startRequested) {
            runningServiceInfo.flags |= 1;
        }
        if (serviceRecord.app != null && serviceRecord.app.pid == ActivityManagerService.MY_PID) {
            runningServiceInfo.flags |= 4;
        }
        if (serviceRecord.app != null && serviceRecord.app.persistent) {
            runningServiceInfo.flags |= 8;
        }
        for (int size = serviceRecord.connections.size() - 1; size >= 0; size--) {
            ArrayList<ConnectionRecord> arrayListValueAt = serviceRecord.connections.valueAt(size);
            for (int i = 0; i < arrayListValueAt.size(); i++) {
                ConnectionRecord connectionRecord = arrayListValueAt.get(i);
                if (connectionRecord.clientLabel != 0) {
                    runningServiceInfo.clientPackage = connectionRecord.binding.client.info.packageName;
                    runningServiceInfo.clientLabel = connectionRecord.clientLabel;
                    return runningServiceInfo;
                }
            }
        }
        return runningServiceInfo;
    }

    List<ActivityManager.RunningServiceInfo> getRunningServiceInfoLocked(int i, int i2, int i3, boolean z, boolean z2) {
        ArrayList arrayList = new ArrayList();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        int i4 = 0;
        try {
            if (z2) {
                int[] users = this.mAm.mUserController.getUsers();
                for (int i5 = 0; i5 < users.length && arrayList.size() < i; i5++) {
                    ArrayMap<ComponentName, ServiceRecord> servicesLocked = getServicesLocked(users[i5]);
                    for (int i6 = 0; i6 < servicesLocked.size() && arrayList.size() < i; i6++) {
                        arrayList.add(makeRunningServiceInfoLocked(servicesLocked.valueAt(i6)));
                    }
                }
                while (i4 < this.mRestartingServices.size() && arrayList.size() < i) {
                    ServiceRecord serviceRecord = this.mRestartingServices.get(i4);
                    ActivityManager.RunningServiceInfo runningServiceInfoMakeRunningServiceInfoLocked = makeRunningServiceInfoLocked(serviceRecord);
                    runningServiceInfoMakeRunningServiceInfoLocked.restarting = serviceRecord.nextRestartTime;
                    arrayList.add(runningServiceInfoMakeRunningServiceInfoLocked);
                    i4++;
                }
            } else {
                int userId = UserHandle.getUserId(i3);
                ArrayMap<ComponentName, ServiceRecord> servicesLocked2 = getServicesLocked(userId);
                for (int i7 = 0; i7 < servicesLocked2.size() && arrayList.size() < i; i7++) {
                    ServiceRecord serviceRecordValueAt = servicesLocked2.valueAt(i7);
                    if (z || (serviceRecordValueAt.app != null && serviceRecordValueAt.app.uid == i3)) {
                        arrayList.add(makeRunningServiceInfoLocked(serviceRecordValueAt));
                    }
                }
                while (i4 < this.mRestartingServices.size() && arrayList.size() < i) {
                    ServiceRecord serviceRecord2 = this.mRestartingServices.get(i4);
                    if (serviceRecord2.userId == userId && (z || (serviceRecord2.app != null && serviceRecord2.app.uid == i3))) {
                        ActivityManager.RunningServiceInfo runningServiceInfoMakeRunningServiceInfoLocked2 = makeRunningServiceInfoLocked(serviceRecord2);
                        runningServiceInfoMakeRunningServiceInfoLocked2.restarting = serviceRecord2.nextRestartTime;
                        arrayList.add(runningServiceInfoMakeRunningServiceInfoLocked2);
                    }
                    i4++;
                }
            }
            return arrayList;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public PendingIntent getRunningServiceControlPanelLocked(ComponentName componentName) {
        ServiceRecord serviceByNameLocked = getServiceByNameLocked(componentName, UserHandle.getUserId(Binder.getCallingUid()));
        if (serviceByNameLocked != null) {
            for (int size = serviceByNameLocked.connections.size() - 1; size >= 0; size--) {
                ArrayList<ConnectionRecord> arrayListValueAt = serviceByNameLocked.connections.valueAt(size);
                for (int i = 0; i < arrayListValueAt.size(); i++) {
                    if (arrayListValueAt.get(i).clientIntent != null) {
                        return arrayListValueAt.get(i).clientIntent;
                    }
                }
            }
            return null;
        }
        return null;
    }

    void serviceTimeout(ProcessRecord processRecord) {
        String str;
        ServiceRecord serviceRecordValueAt;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (processRecord.executingServices.size() != 0 && processRecord.thread != null) {
                    long jUptimeMillis = SystemClock.uptimeMillis() - ((long) (processRecord.execServicesFg ? SERVICE_TIMEOUT : SERVICE_BACKGROUND_TIMEOUT));
                    long j = 0;
                    int size = processRecord.executingServices.size() - 1;
                    while (true) {
                        str = null;
                        if (size >= 0) {
                            serviceRecordValueAt = processRecord.executingServices.valueAt(size);
                            if (serviceRecordValueAt.executingStart < jUptimeMillis) {
                                break;
                            }
                            if (serviceRecordValueAt.executingStart > j) {
                                j = serviceRecordValueAt.executingStart;
                            }
                            size--;
                        } else {
                            serviceRecordValueAt = null;
                            break;
                        }
                    }
                    if (serviceRecordValueAt != null && this.mAm.mLruProcesses.contains(processRecord)) {
                        Slog.w(TAG, "Timeout executing service: " + serviceRecordValueAt);
                        StringWriter stringWriter = new StringWriter();
                        FastPrintWriter fastPrintWriter = new FastPrintWriter(stringWriter, false, 1024);
                        fastPrintWriter.println(serviceRecordValueAt);
                        serviceRecordValueAt.dump((PrintWriter) fastPrintWriter, "    ");
                        fastPrintWriter.close();
                        this.mLastAnrDump = stringWriter.toString();
                        this.mAm.mHandler.removeCallbacks(this.mLastAnrDumpClearer);
                        this.mAm.mHandler.postDelayed(this.mLastAnrDumpClearer, AppStandbyController.SettingsObserver.DEFAULT_SYSTEM_UPDATE_TIMEOUT);
                        str = "executing service " + serviceRecordValueAt.shortName;
                    } else {
                        Message messageObtainMessage = this.mAm.mHandler.obtainMessage(12);
                        messageObtainMessage.obj = processRecord;
                        this.mAm.mHandler.sendMessageAtTime(messageObtainMessage, j + (processRecord.execServicesFg ? 20000L : 200000L));
                    }
                    String str2 = str;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (str2 != null) {
                        this.mAm.mAppErrors.appNotResponding(processRecord, null, null, false, str2);
                        return;
                    }
                    return;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void serviceForegroundTimeout(ServiceRecord serviceRecord) {
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (serviceRecord.fgRequired && !serviceRecord.destroying) {
                    ProcessRecord processRecord = serviceRecord.app;
                    if (processRecord != null && processRecord.debugging) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK) {
                        Slog.i(TAG, "Service foreground-required timeout for " + serviceRecord);
                    }
                    serviceRecord.fgWaiting = false;
                    stopServiceLocked(serviceRecord);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (processRecord != null) {
                        this.mAm.mAppErrors.appNotResponding(processRecord, null, null, false, "Context.startForegroundService() did not then call Service.startForeground(): " + serviceRecord);
                        return;
                    }
                    return;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    public void updateServiceApplicationInfoLocked(ApplicationInfo applicationInfo) {
        ServiceMap serviceMap = this.mServiceMap.get(UserHandle.getUserId(applicationInfo.uid));
        if (serviceMap != null) {
            ArrayMap<ComponentName, ServiceRecord> arrayMap = serviceMap.mServicesByName;
            for (int size = arrayMap.size() - 1; size >= 0; size--) {
                ServiceRecord serviceRecordValueAt = arrayMap.valueAt(size);
                if (applicationInfo.packageName.equals(serviceRecordValueAt.appInfo.packageName)) {
                    serviceRecordValueAt.appInfo = applicationInfo;
                    serviceRecordValueAt.serviceInfo.applicationInfo = applicationInfo;
                }
            }
        }
    }

    void serviceForegroundCrash(ProcessRecord processRecord, CharSequence charSequence) {
        this.mAm.crashApplication(processRecord.uid, processRecord.pid, processRecord.info.packageName, processRecord.userId, "Context.startForegroundService() did not then call Service.startForeground(): " + ((Object) charSequence), false);
    }

    void scheduleServiceTimeoutLocked(ProcessRecord processRecord) {
        if (processRecord.executingServices.size() == 0 || processRecord.thread == null) {
            return;
        }
        Message messageObtainMessage = this.mAm.mHandler.obtainMessage(12);
        messageObtainMessage.obj = processRecord;
        this.mAm.mHandler.sendMessageDelayed(messageObtainMessage, processRecord.execServicesFg ? 20000L : 200000L);
        this.mAm.mAnrManager.sendServiceMonitorMessage();
    }

    void scheduleServiceForegroundTransitionTimeoutLocked(ServiceRecord serviceRecord) {
        if (serviceRecord.app.executingServices.size() == 0 || serviceRecord.app.thread == null) {
            return;
        }
        Message messageObtainMessage = this.mAm.mHandler.obtainMessage(66);
        messageObtainMessage.obj = serviceRecord;
        serviceRecord.fgWaiting = true;
        this.mAm.mHandler.sendMessageDelayed(messageObtainMessage, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
    }

    final class ServiceDumper {
        private final String[] args;
        private final boolean dumpAll;
        private final String dumpPackage;
        private final FileDescriptor fd;
        private final PrintWriter pw;
        private final ArrayList<ServiceRecord> services = new ArrayList<>();
        private final long nowReal = SystemClock.elapsedRealtime();
        private boolean needSep = false;
        private boolean printedAnything = false;
        private boolean printed = false;
        private final ActivityManagerService.ItemMatcher matcher = new ActivityManagerService.ItemMatcher();

        ServiceDumper(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, int i, boolean z, String str) {
            this.fd = fileDescriptor;
            this.pw = printWriter;
            this.args = strArr;
            this.dumpAll = z;
            this.dumpPackage = str;
            this.matcher.build(strArr, i);
            for (int i2 : ActiveServices.this.mAm.mUserController.getUsers()) {
                ServiceMap serviceMapLocked = ActiveServices.this.getServiceMapLocked(i2);
                if (serviceMapLocked.mServicesByName.size() > 0) {
                    for (int i3 = 0; i3 < serviceMapLocked.mServicesByName.size(); i3++) {
                        ServiceRecord serviceRecordValueAt = serviceMapLocked.mServicesByName.valueAt(i3);
                        if (this.matcher.match(serviceRecordValueAt, serviceRecordValueAt.name) && (str == null || str.equals(serviceRecordValueAt.appInfo.packageName))) {
                            this.services.add(serviceRecordValueAt);
                        }
                    }
                }
            }
        }

        private void dumpHeaderLocked() {
            this.pw.println("ACTIVITY MANAGER SERVICES (dumpsys activity services)");
            if (ActiveServices.this.mLastAnrDump != null) {
                this.pw.println("  Last ANR service:");
                this.pw.print(ActiveServices.this.mLastAnrDump);
                this.pw.println();
            }
        }

        void dumpLocked() {
            dumpHeaderLocked();
            try {
                for (int i : ActiveServices.this.mAm.mUserController.getUsers()) {
                    int i2 = 0;
                    while (i2 < this.services.size() && this.services.get(i2).userId != i) {
                        i2++;
                    }
                    this.printed = false;
                    if (i2 < this.services.size()) {
                        this.needSep = false;
                        while (i2 < this.services.size()) {
                            ServiceRecord serviceRecord = this.services.get(i2);
                            i2++;
                            if (serviceRecord.userId != i) {
                                break;
                            } else {
                                dumpServiceLocalLocked(serviceRecord);
                            }
                        }
                        this.needSep |= this.printed;
                    }
                    dumpUserRemainsLocked(i);
                }
            } catch (Exception e) {
                Slog.w(ActiveServices.TAG, "Exception in dumpServicesLocked", e);
            }
            dumpRemainsLocked();
        }

        void dumpWithClient() {
            int i;
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    dumpHeaderLocked();
                } finally {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            try {
            } catch (Exception e) {
                Slog.w(ActiveServices.TAG, "Exception in dumpServicesLocked", e);
            }
            for (int i2 : ActiveServices.this.mAm.mUserController.getUsers()) {
                int i3 = 0;
                while (i3 < this.services.size() && this.services.get(i3).userId != i2) {
                    i3++;
                }
                this.printed = false;
                if (i3 < this.services.size()) {
                    this.needSep = false;
                    while (i3 < this.services.size()) {
                        ServiceRecord serviceRecord = this.services.get(i3);
                        i3++;
                        if (serviceRecord.userId != i2) {
                            break;
                        }
                        synchronized (ActiveServices.this.mAm) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                dumpServiceLocalLocked(serviceRecord);
                            } finally {
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        dumpServiceClient(serviceRecord);
                        synchronized (ActiveServices.this.mAm) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                dumpRemainsLocked();
                            } finally {
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            }
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    this.needSep |= this.printed;
                }
                synchronized (ActiveServices.this.mAm) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        dumpUserRemainsLocked(i2);
                    } finally {
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
            synchronized (ActiveServices.this.mAm) {
            }
        }

        private void dumpUserHeaderLocked(int i) {
            if (!this.printed) {
                if (this.printedAnything) {
                    this.pw.println();
                }
                this.pw.println("  User " + i + " active services:");
                this.printed = true;
            }
            this.printedAnything = true;
            if (this.needSep) {
                this.pw.println();
            }
        }

        private void dumpServiceLocalLocked(ServiceRecord serviceRecord) {
            dumpUserHeaderLocked(serviceRecord.userId);
            this.pw.print("  * ");
            this.pw.println(serviceRecord);
            if (this.dumpAll) {
                serviceRecord.dump(this.pw, "    ");
                this.needSep = true;
                return;
            }
            this.pw.print("    app=");
            this.pw.println(serviceRecord.app);
            this.pw.print("    created=");
            TimeUtils.formatDuration(serviceRecord.createRealTime, this.nowReal, this.pw);
            this.pw.print(" started=");
            this.pw.print(serviceRecord.startRequested);
            this.pw.print(" connections=");
            this.pw.println(serviceRecord.connections.size());
            if (serviceRecord.connections.size() > 0) {
                this.pw.println("    Connections:");
                for (int i = 0; i < serviceRecord.connections.size(); i++) {
                    ArrayList<ConnectionRecord> arrayListValueAt = serviceRecord.connections.valueAt(i);
                    for (int i2 = 0; i2 < arrayListValueAt.size(); i2++) {
                        ConnectionRecord connectionRecord = arrayListValueAt.get(i2);
                        this.pw.print("      ");
                        this.pw.print(connectionRecord.binding.intent.intent.getIntent().toShortString(false, false, false, false));
                        this.pw.print(" -> ");
                        ProcessRecord processRecord = connectionRecord.binding.client;
                        this.pw.println(processRecord != null ? processRecord.toShortString() : "null");
                    }
                }
            }
        }

        private void dumpServiceClient(ServiceRecord serviceRecord) {
            IApplicationThread iApplicationThread;
            ProcessRecord processRecord = serviceRecord.app;
            if (processRecord == null || (iApplicationThread = processRecord.thread) == null) {
                return;
            }
            this.pw.println("    Client:");
            this.pw.flush();
            try {
                TransferPipe transferPipe = new TransferPipe();
                try {
                    iApplicationThread.dumpService(transferPipe.getWriteFd(), serviceRecord, this.args);
                    transferPipe.setBufferPrefix("      ");
                    transferPipe.go(this.fd, 2000L);
                    transferPipe.kill();
                } catch (Throwable th) {
                    transferPipe.kill();
                    throw th;
                }
            } catch (RemoteException e) {
                this.pw.println("      Got a RemoteException while dumping the service");
            } catch (IOException e2) {
                this.pw.println("      Failure while dumping the service: " + e2);
            }
            this.needSep = true;
        }

        private void dumpUserRemainsLocked(int i) {
            ServiceMap serviceMapLocked = ActiveServices.this.getServiceMapLocked(i);
            this.printed = false;
            int size = serviceMapLocked.mDelayedStartList.size();
            for (int i2 = 0; i2 < size; i2++) {
                ServiceRecord serviceRecord = serviceMapLocked.mDelayedStartList.get(i2);
                if (this.matcher.match(serviceRecord, serviceRecord.name) && (this.dumpPackage == null || this.dumpPackage.equals(serviceRecord.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        this.pw.println("  User " + i + " delayed start services:");
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Delayed start ");
                    this.pw.println(serviceRecord);
                }
            }
            this.printed = false;
            int size2 = serviceMapLocked.mStartingBackground.size();
            for (int i3 = 0; i3 < size2; i3++) {
                ServiceRecord serviceRecord2 = serviceMapLocked.mStartingBackground.get(i3);
                if (this.matcher.match(serviceRecord2, serviceRecord2.name) && (this.dumpPackage == null || this.dumpPackage.equals(serviceRecord2.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        this.pw.println("  User " + i + " starting in background:");
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Starting bg ");
                    this.pw.println(serviceRecord2);
                }
            }
        }

        private void dumpRemainsLocked() {
            if (ActiveServices.this.mPendingServices.size() > 0) {
                this.printed = false;
                for (int i = 0; i < ActiveServices.this.mPendingServices.size(); i++) {
                    ServiceRecord serviceRecord = ActiveServices.this.mPendingServices.get(i);
                    if (this.matcher.match(serviceRecord, serviceRecord.name) && (this.dumpPackage == null || this.dumpPackage.equals(serviceRecord.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Pending services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Pending ");
                        this.pw.println(serviceRecord);
                        serviceRecord.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (ActiveServices.this.mRestartingServices.size() > 0) {
                this.printed = false;
                for (int i2 = 0; i2 < ActiveServices.this.mRestartingServices.size(); i2++) {
                    ServiceRecord serviceRecord2 = ActiveServices.this.mRestartingServices.get(i2);
                    if (this.matcher.match(serviceRecord2, serviceRecord2.name) && (this.dumpPackage == null || this.dumpPackage.equals(serviceRecord2.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Restarting services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Restarting ");
                        this.pw.println(serviceRecord2);
                        serviceRecord2.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (ActiveServices.this.mDestroyingServices.size() > 0) {
                this.printed = false;
                for (int i3 = 0; i3 < ActiveServices.this.mDestroyingServices.size(); i3++) {
                    ServiceRecord serviceRecord3 = ActiveServices.this.mDestroyingServices.get(i3);
                    if (this.matcher.match(serviceRecord3, serviceRecord3.name) && (this.dumpPackage == null || this.dumpPackage.equals(serviceRecord3.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Destroying services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Destroy ");
                        this.pw.println(serviceRecord3);
                        serviceRecord3.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.dumpAll) {
                this.printed = false;
                for (int i4 = 0; i4 < ActiveServices.this.mServiceConnections.size(); i4++) {
                    ArrayList<ConnectionRecord> arrayListValueAt = ActiveServices.this.mServiceConnections.valueAt(i4);
                    for (int i5 = 0; i5 < arrayListValueAt.size(); i5++) {
                        ConnectionRecord connectionRecord = arrayListValueAt.get(i5);
                        if (this.matcher.match(connectionRecord.binding.service, connectionRecord.binding.service.name) && (this.dumpPackage == null || (connectionRecord.binding.client != null && this.dumpPackage.equals(connectionRecord.binding.client.info.packageName)))) {
                            this.printedAnything = true;
                            if (!this.printed) {
                                if (this.needSep) {
                                    this.pw.println();
                                }
                                this.needSep = true;
                                this.pw.println("  Connection bindings to services:");
                                this.printed = true;
                            }
                            this.pw.print("  * ");
                            this.pw.println(connectionRecord);
                            connectionRecord.dump(this.pw, "    ");
                        }
                    }
                }
            }
            if (this.matcher.all) {
                long jElapsedRealtime = SystemClock.elapsedRealtime();
                for (int i6 : ActiveServices.this.mAm.mUserController.getUsers()) {
                    ServiceMap serviceMap = ActiveServices.this.mServiceMap.get(i6);
                    if (serviceMap != null) {
                        boolean z = false;
                        for (int size = serviceMap.mActiveForegroundApps.size() - 1; size >= 0; size--) {
                            ActiveForegroundApp activeForegroundAppValueAt = serviceMap.mActiveForegroundApps.valueAt(size);
                            if (this.dumpPackage == null || this.dumpPackage.equals(activeForegroundAppValueAt.mPackageName)) {
                                if (!z) {
                                    this.printedAnything = true;
                                    if (this.needSep) {
                                        this.pw.println();
                                    }
                                    this.needSep = true;
                                    this.pw.print("Active foreground apps - user ");
                                    this.pw.print(i6);
                                    this.pw.println(":");
                                    z = true;
                                }
                                this.pw.print("  #");
                                this.pw.print(size);
                                this.pw.print(": ");
                                this.pw.println(activeForegroundAppValueAt.mPackageName);
                                if (activeForegroundAppValueAt.mLabel != null) {
                                    this.pw.print("    mLabel=");
                                    this.pw.println(activeForegroundAppValueAt.mLabel);
                                }
                                this.pw.print("    mNumActive=");
                                this.pw.print(activeForegroundAppValueAt.mNumActive);
                                this.pw.print(" mAppOnTop=");
                                this.pw.print(activeForegroundAppValueAt.mAppOnTop);
                                this.pw.print(" mShownWhileTop=");
                                this.pw.print(activeForegroundAppValueAt.mShownWhileTop);
                                this.pw.print(" mShownWhileScreenOn=");
                                this.pw.println(activeForegroundAppValueAt.mShownWhileScreenOn);
                                this.pw.print("    mStartTime=");
                                TimeUtils.formatDuration(activeForegroundAppValueAt.mStartTime - jElapsedRealtime, this.pw);
                                this.pw.print(" mStartVisibleTime=");
                                TimeUtils.formatDuration(activeForegroundAppValueAt.mStartVisibleTime - jElapsedRealtime, this.pw);
                                this.pw.println();
                                if (activeForegroundAppValueAt.mEndTime != 0) {
                                    this.pw.print("    mEndTime=");
                                    TimeUtils.formatDuration(activeForegroundAppValueAt.mEndTime - jElapsedRealtime, this.pw);
                                    this.pw.println();
                                }
                            }
                        }
                        if (serviceMap.hasMessagesOrCallbacks()) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.printedAnything = true;
                            this.needSep = true;
                            this.pw.print("  Handler - user ");
                            this.pw.print(i6);
                            this.pw.println(":");
                            serviceMap.dumpMine(new PrintWriterPrinter(this.pw), "    ");
                        }
                    }
                }
            }
            if (!this.printedAnything) {
                this.pw.println("  (nothing)");
            }
        }
    }

    ServiceDumper newServiceDumperLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, int i, boolean z, String str) {
        return new ServiceDumper(fileDescriptor, printWriter, strArr, i, z, str);
    }

    protected void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                long jStart = protoOutputStream.start(j);
                for (int i : this.mAm.mUserController.getUsers()) {
                    ServiceMap serviceMap = this.mServiceMap.get(i);
                    if (serviceMap != null) {
                        long jStart2 = protoOutputStream.start(2246267895809L);
                        protoOutputStream.write(1120986464257L, i);
                        ArrayMap<ComponentName, ServiceRecord> arrayMap = serviceMap.mServicesByName;
                        for (int i2 = 0; i2 < arrayMap.size(); i2++) {
                            arrayMap.valueAt(i2).writeToProto(protoOutputStream, 2246267895810L);
                        }
                        protoOutputStream.end(jStart2);
                    }
                }
                protoOutputStream.end(jStart);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    protected boolean dumpService(FileDescriptor fileDescriptor, PrintWriter printWriter, String str, String[] strArr, int i, boolean z) {
        boolean z2;
        ArrayList arrayList = new ArrayList();
        Predicate predicateFilterRecord = DumpUtils.filterRecord(str);
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                z2 = false;
                for (int i2 : this.mAm.mUserController.getUsers()) {
                    ServiceMap serviceMap = this.mServiceMap.get(i2);
                    if (serviceMap != null) {
                        ArrayMap<ComponentName, ServiceRecord> arrayMap = serviceMap.mServicesByName;
                        for (int i3 = 0; i3 < arrayMap.size(); i3++) {
                            ServiceRecord serviceRecordValueAt = arrayMap.valueAt(i3);
                            if (predicateFilterRecord.test(serviceRecordValueAt)) {
                                arrayList.add(serviceRecordValueAt);
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (arrayList.size() <= 0) {
            return false;
        }
        arrayList.sort(Comparator.comparing(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((ServiceRecord) obj).getComponentName();
            }
        }));
        int i4 = 0;
        while (i4 < arrayList.size()) {
            if (z2) {
                printWriter.println();
            }
            dumpService(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, fileDescriptor, printWriter, (ServiceRecord) arrayList.get(i4), strArr, z);
            i4++;
            z2 = true;
        }
        return true;
    }

    private void dumpService(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, ServiceRecord serviceRecord, String[] strArr, boolean z) {
        String str2 = str + "  ";
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                printWriter.print(str);
                printWriter.print("SERVICE ");
                printWriter.print(serviceRecord.shortName);
                printWriter.print(" ");
                printWriter.print(Integer.toHexString(System.identityHashCode(serviceRecord)));
                printWriter.print(" pid=");
                if (serviceRecord.app != null) {
                    printWriter.println(serviceRecord.app.pid);
                } else {
                    printWriter.println("(not running)");
                }
                if (z) {
                    serviceRecord.dump(printWriter, str2);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (serviceRecord.app != null && serviceRecord.app.thread != null) {
            printWriter.print(str);
            printWriter.println("  Client:");
            printWriter.flush();
            try {
                TransferPipe transferPipe = new TransferPipe();
                try {
                    serviceRecord.app.thread.dumpService(transferPipe.getWriteFd(), serviceRecord, strArr);
                    transferPipe.setBufferPrefix(str + "    ");
                    transferPipe.go(fileDescriptor);
                    transferPipe.kill();
                } catch (Throwable th2) {
                    transferPipe.kill();
                    throw th2;
                }
            } catch (RemoteException e) {
                printWriter.println(str + "    Got a RemoteException while dumping the service");
            } catch (IOException e2) {
                printWriter.println(str + "    Failure while dumping the service: " + e2);
            }
        }
    }
}
