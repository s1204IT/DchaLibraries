package com.android.server.autofill;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.provider.Settings;
import android.service.autofill.FillEventHistory;
import android.service.autofill.UserData;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.LocalLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManagerInternal;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManager;
import android.view.autofill.IAutoFillManagerClient;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.IResultReceiver;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.Preconditions;
import com.android.server.FgThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.autofill.ui.AutoFillUI;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AutofillManagerService extends SystemService {
    private static final char COMPAT_PACKAGE_DELIMITER = ':';
    private static final char COMPAT_PACKAGE_URL_IDS_BLOCK_BEGIN = '[';
    private static final char COMPAT_PACKAGE_URL_IDS_BLOCK_END = ']';
    private static final char COMPAT_PACKAGE_URL_IDS_DELIMITER = ',';
    static final String RECEIVER_BUNDLE_EXTRA_SESSIONS = "sessions";
    private static final String TAG = "AutofillManagerService";

    @GuardedBy("mLock")
    private boolean mAllowInstantService;
    private final AutofillCompatState mAutofillCompatState;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;

    @GuardedBy("mLock")
    private final SparseBooleanArray mDisabledUsers;
    private final LocalService mLocalService;
    private final Object mLock;
    private final LocalLog mRequestsHistory;

    @GuardedBy("mLock")
    private SparseArray<AutofillManagerServiceImpl> mServicesCache;
    private final AutoFillUI mUi;
    private final LocalLog mUiLatencyHistory;
    private final LocalLog mWtfHistory;

    public AutofillManagerService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mServicesCache = new SparseArray<>();
        this.mDisabledUsers = new SparseBooleanArray();
        this.mRequestsHistory = new LocalLog(20);
        this.mUiLatencyHistory = new LocalLog(20);
        this.mWtfHistory = new LocalLog(50);
        this.mAutofillCompatState = new AutofillCompatState();
        this.mLocalService = new LocalService();
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(intent.getAction())) {
                    if (Helper.sDebug) {
                        Slog.d(AutofillManagerService.TAG, "Close system dialogs");
                    }
                    synchronized (AutofillManagerService.this.mLock) {
                        for (int i = 0; i < AutofillManagerService.this.mServicesCache.size(); i++) {
                            ((AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.valueAt(i)).destroyFinishedSessionsLocked();
                        }
                    }
                    AutofillManagerService.this.mUi.hideAll(null);
                }
            }
        };
        this.mContext = context;
        this.mUi = new AutoFillUI(ActivityThread.currentActivityThread().getSystemUiContext());
        boolean z = Build.IS_DEBUGGABLE;
        Slog.i(TAG, "Setting debug to " + z);
        setDebugLocked(z);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter, null, FgThread.getHandler());
        UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
        UserManagerInternal userManagerInternal = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        List users = userManager.getUsers();
        for (int i = 0; i < users.size(); i++) {
            int i2 = ((UserInfo) users.get(i)).id;
            boolean userRestriction = userManagerInternal.getUserRestriction(i2, "no_autofill");
            if (userRestriction) {
                if (userRestriction) {
                    Slog.i(TAG, "Disabling Autofill for user " + i2);
                }
                this.mDisabledUsers.put(i2, userRestriction);
            }
        }
        userManagerInternal.addUserRestrictionsListener(new UserManagerInternal.UserRestrictionsListener() {
            public final void onUserRestrictionsChanged(int i3, Bundle bundle, Bundle bundle2) {
                AutofillManagerService.lambda$new$0(this.f$0, i3, bundle, bundle2);
            }
        });
        startTrackingPackageChanges();
    }

    public static void lambda$new$0(AutofillManagerService autofillManagerService, int i, Bundle bundle, Bundle bundle2) {
        boolean z = bundle.getBoolean("no_autofill", false);
        synchronized (autofillManagerService.mLock) {
            if (autofillManagerService.mDisabledUsers.get(i) == z && Helper.sDebug) {
                Slog.d(TAG, "Autofill restriction did not change for user " + i);
                return;
            }
            Slog.i(TAG, "Updating Autofill for user " + i + ": disabled=" + z);
            autofillManagerService.mDisabledUsers.put(i, z);
            autofillManagerService.updateCachedServiceLocked(i, z);
        }
    }

    private void startTrackingPackageChanges() {
        new PackageMonitor() {
            public void onSomePackagesChanged() {
                synchronized (AutofillManagerService.this.mLock) {
                    AutofillManagerService.this.updateCachedServiceLocked(getChangingUserId());
                }
            }

            public void onPackageUpdateFinished(String str, int i) {
                synchronized (AutofillManagerService.this.mLock) {
                    if (str.equals(getActiveAutofillServicePackageName())) {
                        AutofillManagerService.this.removeCachedServiceLocked(getChangingUserId());
                    } else {
                        handlePackageUpdateLocked(str);
                    }
                }
            }

            public void onPackageRemoved(String str, int i) {
                ComponentName serviceComponentName;
                synchronized (AutofillManagerService.this.mLock) {
                    int changingUserId = getChangingUserId();
                    AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(changingUserId);
                    if (autofillManagerServiceImplPeekServiceForUserLocked != null && (serviceComponentName = autofillManagerServiceImplPeekServiceForUserLocked.getServiceComponentName()) != null && str.equals(serviceComponentName.getPackageName())) {
                        handleActiveAutofillServiceRemoved(changingUserId);
                    }
                }
            }

            public boolean onHandleForceStop(Intent intent, String[] strArr, int i, boolean z) {
                synchronized (AutofillManagerService.this.mLock) {
                    String activeAutofillServicePackageName = getActiveAutofillServicePackageName();
                    for (String str : strArr) {
                        if (str.equals(activeAutofillServicePackageName)) {
                            if (!z) {
                                return true;
                            }
                            AutofillManagerService.this.removeCachedServiceLocked(getChangingUserId());
                        } else {
                            handlePackageUpdateLocked(str);
                        }
                    }
                    return false;
                }
            }

            private void handleActiveAutofillServiceRemoved(int i) {
                AutofillManagerService.this.removeCachedServiceLocked(i);
                Settings.Secure.putStringForUser(AutofillManagerService.this.mContext.getContentResolver(), "autofill_service", null, i);
            }

            private String getActiveAutofillServicePackageName() {
                ComponentName serviceComponentName;
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(getChangingUserId());
                if (autofillManagerServiceImplPeekServiceForUserLocked == null || (serviceComponentName = autofillManagerServiceImplPeekServiceForUserLocked.getServiceComponentName()) == null) {
                    return null;
                }
                return serviceComponentName.getPackageName();
            }

            @GuardedBy("mLock")
            private void handlePackageUpdateLocked(String str) {
                int size = AutofillManagerService.this.mServicesCache.size();
                for (int i = 0; i < size; i++) {
                    ((AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.valueAt(i)).handlePackageUpdateLocked(str);
                }
            }
        }.register(this.mContext, (Looper) null, UserHandle.ALL, true);
    }

    @Override
    public void onStart() {
        publishBinderService("autofill", new AutoFillManagerServiceStub());
        publishLocalService(AutofillManagerInternal.class, this.mLocalService);
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 600) {
            new SettingsObserver(BackgroundThread.getHandler());
        }
    }

    @Override
    public void onUnlockUser(int i) {
        synchronized (this.mLock) {
            updateCachedServiceLocked(i);
        }
    }

    @Override
    public void onSwitchUser(int i) {
        if (Helper.sDebug) {
            Slog.d(TAG, "Hiding UI when user switched");
        }
        this.mUi.hideAll(null);
    }

    @Override
    public void onCleanupUser(int i) {
        synchronized (this.mLock) {
            removeCachedServiceLocked(i);
        }
    }

    @GuardedBy("mLock")
    AutofillManagerServiceImpl getServiceForUserLocked(int i) {
        int iHandleIncomingUser = ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, null, null);
        AutofillManagerServiceImpl autofillManagerServiceImpl = this.mServicesCache.get(iHandleIncomingUser);
        if (autofillManagerServiceImpl == null) {
            AutofillManagerServiceImpl autofillManagerServiceImpl2 = new AutofillManagerServiceImpl(this.mContext, this.mLock, this.mRequestsHistory, this.mUiLatencyHistory, this.mWtfHistory, iHandleIncomingUser, this.mUi, this.mAutofillCompatState, this.mDisabledUsers.get(iHandleIncomingUser));
            this.mServicesCache.put(i, autofillManagerServiceImpl2);
            addCompatibilityModeRequestsLocked(autofillManagerServiceImpl2, i);
            return autofillManagerServiceImpl2;
        }
        return autofillManagerServiceImpl;
    }

    @GuardedBy("mLock")
    AutofillManagerServiceImpl peekServiceForUserLocked(int i) {
        return this.mServicesCache.get(ActivityManager.handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, null, null));
    }

    void destroySessions(int i, IResultReceiver iResultReceiver) {
        Slog.i(TAG, "destroySessions() for userId " + i);
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            try {
                if (i != -1) {
                    AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = peekServiceForUserLocked(i);
                    if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                        autofillManagerServiceImplPeekServiceForUserLocked.destroySessionsLocked();
                    }
                } else {
                    int size = this.mServicesCache.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        this.mServicesCache.valueAt(i2).destroySessionsLocked();
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        try {
            iResultReceiver.send(0, new Bundle());
        } catch (RemoteException e) {
        }
    }

    void listSessions(int i, IResultReceiver iResultReceiver) {
        Slog.i(TAG, "listSessions() for userId " + i);
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        Bundle bundle = new Bundle();
        ArrayList<String> arrayList = new ArrayList<>();
        synchronized (this.mLock) {
            try {
                if (i != -1) {
                    AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = peekServiceForUserLocked(i);
                    if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                        autofillManagerServiceImplPeekServiceForUserLocked.listSessionsLocked(arrayList);
                    }
                } else {
                    int size = this.mServicesCache.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        this.mServicesCache.valueAt(i2).listSessionsLocked(arrayList);
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        bundle.putStringArrayList(RECEIVER_BUNDLE_EXTRA_SESSIONS, arrayList);
        try {
            iResultReceiver.send(0, bundle);
        } catch (RemoteException e) {
        }
    }

    void reset() {
        Slog.i(TAG, "reset()");
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            int size = this.mServicesCache.size();
            for (int i = 0; i < size; i++) {
                this.mServicesCache.valueAt(i).destroyLocked();
            }
            this.mServicesCache.clear();
        }
    }

    void setLogLevel(int i) {
        Slog.i(TAG, "setLogLevel(): " + i);
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        boolean z = true;
        boolean z2 = false;
        if (i != 4) {
            if (i != 2) {
                z = false;
            }
        } else {
            z2 = true;
        }
        synchronized (this.mLock) {
            setDebugLocked(z);
            setVerboseLocked(z2);
        }
    }

    int getLogLevel() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            if (Helper.sVerbose) {
                return 4;
            }
            return Helper.sDebug ? 2 : 0;
        }
    }

    int getMaxPartitions() {
        int i;
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            i = Helper.sPartitionMaxCount;
        }
        return i;
    }

    void setMaxPartitions(int i) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        Slog.i(TAG, "setMaxPartitions(): " + i);
        synchronized (this.mLock) {
            Helper.sPartitionMaxCount = i;
        }
    }

    int getMaxVisibleDatasets() {
        int i;
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            i = Helper.sVisibleDatasetsMaxCount;
        }
        return i;
    }

    void setMaxVisibleDatasets(int i) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        Slog.i(TAG, "setMaxVisibleDatasets(): " + i);
        synchronized (this.mLock) {
            Helper.sVisibleDatasetsMaxCount = i;
        }
    }

    void getScore(String str, String str2, String str3, RemoteCallback remoteCallback) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        new FieldClassificationStrategy(this.mContext, -2).getScores(remoteCallback, str, null, Arrays.asList(AutofillValue.forText(str2)), new String[]{str3});
    }

    Boolean getFullScreenMode() {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        return Helper.sFullScreenMode;
    }

    void setFullScreenMode(Boolean bool) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        Helper.sFullScreenMode = bool;
    }

    boolean getAllowInstantService() {
        boolean z;
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        synchronized (this.mLock) {
            z = this.mAllowInstantService;
        }
        return z;
    }

    void setAllowInstantService(boolean z) {
        this.mContext.enforceCallingPermission("android.permission.MANAGE_AUTO_FILL", TAG);
        Slog.i(TAG, "setAllowInstantService(): " + z);
        synchronized (this.mLock) {
            this.mAllowInstantService = z;
        }
    }

    private void setDebugLocked(boolean z) {
        Helper.sDebug = z;
        android.view.autofill.Helper.sDebug = z;
    }

    private void setVerboseLocked(boolean z) {
        Helper.sVerbose = z;
        android.view.autofill.Helper.sVerbose = z;
    }

    @GuardedBy("mLock")
    private void removeCachedServiceLocked(int i) {
        AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = peekServiceForUserLocked(i);
        if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
            this.mServicesCache.delete(i);
            autofillManagerServiceImplPeekServiceForUserLocked.destroyLocked();
            this.mAutofillCompatState.removeCompatibilityModeRequests(i);
        }
    }

    @GuardedBy("mLock")
    private void updateCachedServiceLocked(int i) {
        updateCachedServiceLocked(i, this.mDisabledUsers.get(i));
    }

    @GuardedBy("mLock")
    private void updateCachedServiceLocked(int i, boolean z) {
        AutofillManagerServiceImpl serviceForUserLocked = getServiceForUserLocked(i);
        if (serviceForUserLocked != null) {
            serviceForUserLocked.destroySessionsLocked();
            serviceForUserLocked.updateLocked(z);
            if (!serviceForUserLocked.isEnabledLocked()) {
                removeCachedServiceLocked(i);
            } else {
                addCompatibilityModeRequestsLocked(serviceForUserLocked, i);
            }
        }
    }

    private void addCompatibilityModeRequestsLocked(AutofillManagerServiceImpl autofillManagerServiceImpl, int i) {
        this.mAutofillCompatState.reset(i);
        ArrayMap<String, Long> compatibilityPackagesLocked = autofillManagerServiceImpl.getCompatibilityPackagesLocked();
        if (compatibilityPackagesLocked == null || compatibilityPackagesLocked.isEmpty()) {
            return;
        }
        Map<String, String[]> whitelistedCompatModePackages = getWhitelistedCompatModePackages();
        int size = compatibilityPackagesLocked.size();
        for (int i2 = 0; i2 < size; i2++) {
            String strKeyAt = compatibilityPackagesLocked.keyAt(i2);
            if (whitelistedCompatModePackages == null || !whitelistedCompatModePackages.containsKey(strKeyAt)) {
                Slog.w(TAG, "Ignoring not whitelisted compat package " + strKeyAt);
            } else {
                Long lValueAt = compatibilityPackagesLocked.valueAt(i2);
                if (lValueAt != null) {
                    this.mAutofillCompatState.addCompatibilityModeRequest(strKeyAt, lValueAt.longValue(), whitelistedCompatModePackages.get(strKeyAt), i);
                }
            }
        }
    }

    private String getWhitelistedCompatModePackagesFromSettings() {
        return Settings.Global.getString(this.mContext.getContentResolver(), "autofill_compat_mode_allowed_packages");
    }

    private Map<String, String[]> getWhitelistedCompatModePackages() {
        return getWhitelistedCompatModePackages(getWhitelistedCompatModePackagesFromSettings());
    }

    @VisibleForTesting
    static Map<String, String[]> getWhitelistedCompatModePackages(String str) {
        ArrayList arrayList;
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        ArrayMap arrayMap = new ArrayMap();
        TextUtils.SimpleStringSplitter simpleStringSplitter = new TextUtils.SimpleStringSplitter(COMPAT_PACKAGE_DELIMITER);
        simpleStringSplitter.setString(str);
        while (simpleStringSplitter.hasNext()) {
            String next = simpleStringSplitter.next();
            int iIndexOf = next.indexOf(91);
            if (iIndexOf != -1) {
                if (next.charAt(next.length() - 1) != ']') {
                    Slog.w(TAG, "Ignoring entry '" + next + "' on '" + str + "'because it does not end on '" + COMPAT_PACKAGE_URL_IDS_BLOCK_END + "'");
                } else {
                    String strSubstring = next.substring(0, iIndexOf);
                    arrayList = new ArrayList();
                    String strSubstring2 = next.substring(iIndexOf + 1, next.length() - 1);
                    if (Helper.sVerbose) {
                        Slog.v(TAG, "pkg:" + strSubstring + ": block:" + next + ": urls:" + arrayList + ": block:" + strSubstring2 + ":");
                    }
                    TextUtils.SimpleStringSplitter simpleStringSplitter2 = new TextUtils.SimpleStringSplitter(COMPAT_PACKAGE_URL_IDS_DELIMITER);
                    simpleStringSplitter2.setString(strSubstring2);
                    while (simpleStringSplitter2.hasNext()) {
                        arrayList.add(simpleStringSplitter2.next());
                    }
                    next = strSubstring;
                }
            } else {
                arrayList = null;
            }
            if (arrayList == null) {
                arrayMap.put(next, null);
            } else {
                String[] strArr = new String[arrayList.size()];
                arrayList.toArray(strArr);
                arrayMap.put(next, strArr);
            }
        }
        return arrayMap;
    }

    private final class LocalService extends AutofillManagerInternal {
        private LocalService() {
        }

        public void onBackKeyPressed() {
            if (Helper.sDebug) {
                Slog.d(AutofillManagerService.TAG, "onBackKeyPressed()");
            }
            AutofillManagerService.this.mUi.hideAll(null);
        }

        public boolean isCompatibilityModeRequested(String str, long j, int i) {
            return AutofillManagerService.this.mAutofillCompatState.isCompatibilityModeRequested(str, j, i);
        }
    }

    static final class PackageCompatState {
        private final long maxVersionCode;
        private final String[] urlBarResourceIds;

        PackageCompatState(long j, String[] strArr) {
            this.maxVersionCode = j;
            this.urlBarResourceIds = strArr;
        }

        public String toString() {
            return "maxVersionCode=" + this.maxVersionCode + ", urlBarResourceIds=" + Arrays.toString(this.urlBarResourceIds);
        }
    }

    static final class AutofillCompatState {
        private final Object mLock = new Object();

        @GuardedBy("mLock")
        private SparseArray<ArrayMap<String, PackageCompatState>> mUserSpecs;

        AutofillCompatState() {
        }

        boolean isCompatibilityModeRequested(String str, long j, int i) {
            synchronized (this.mLock) {
                if (this.mUserSpecs == null) {
                    return false;
                }
                ArrayMap<String, PackageCompatState> arrayMap = this.mUserSpecs.get(i);
                if (arrayMap == null) {
                    return false;
                }
                PackageCompatState packageCompatState = arrayMap.get(str);
                if (packageCompatState == null) {
                    return false;
                }
                return j <= packageCompatState.maxVersionCode;
            }
        }

        String[] getUrlBarResourceIds(String str, int i) {
            synchronized (this.mLock) {
                if (this.mUserSpecs == null) {
                    return null;
                }
                ArrayMap<String, PackageCompatState> arrayMap = this.mUserSpecs.get(i);
                if (arrayMap == null) {
                    return null;
                }
                PackageCompatState packageCompatState = arrayMap.get(str);
                if (packageCompatState == null) {
                    return null;
                }
                return packageCompatState.urlBarResourceIds;
            }
        }

        void addCompatibilityModeRequest(String str, long j, String[] strArr, int i) {
            synchronized (this.mLock) {
                if (this.mUserSpecs == null) {
                    this.mUserSpecs = new SparseArray<>();
                }
                ArrayMap<String, PackageCompatState> arrayMap = this.mUserSpecs.get(i);
                if (arrayMap == null) {
                    arrayMap = new ArrayMap<>();
                    this.mUserSpecs.put(i, arrayMap);
                }
                arrayMap.put(str, new PackageCompatState(j, strArr));
            }
        }

        void removeCompatibilityModeRequests(int i) {
            synchronized (this.mLock) {
                if (this.mUserSpecs != null) {
                    this.mUserSpecs.remove(i);
                    if (this.mUserSpecs.size() <= 0) {
                        this.mUserSpecs = null;
                    }
                }
            }
        }

        void reset(int i) {
            synchronized (this.mLock) {
                if (this.mUserSpecs != null) {
                    this.mUserSpecs.delete(i);
                    int size = this.mUserSpecs.size();
                    if (size == 0) {
                        if (Helper.sVerbose) {
                            Slog.v(AutofillManagerService.TAG, "reseting mUserSpecs");
                        }
                        this.mUserSpecs = null;
                    } else if (Helper.sVerbose) {
                        Slog.v(AutofillManagerService.TAG, "mUserSpecs down to " + size);
                    }
                }
            }
        }

        private void dump(String str, PrintWriter printWriter) {
            if (this.mUserSpecs == null) {
                printWriter.println("N/A");
                return;
            }
            printWriter.println();
            String str2 = str + "  ";
            for (int i = 0; i < this.mUserSpecs.size(); i++) {
                int iKeyAt = this.mUserSpecs.keyAt(i);
                printWriter.print(str);
                printWriter.print("User: ");
                printWriter.println(iKeyAt);
                ArrayMap<String, PackageCompatState> arrayMapValueAt = this.mUserSpecs.valueAt(i);
                for (int i2 = 0; i2 < arrayMapValueAt.size(); i2++) {
                    String strKeyAt = arrayMapValueAt.keyAt(i2);
                    PackageCompatState packageCompatStateValueAt = arrayMapValueAt.valueAt(i2);
                    printWriter.print(str2);
                    printWriter.print(strKeyAt);
                    printWriter.print(": ");
                    printWriter.println(packageCompatStateValueAt);
                }
            }
        }
    }

    final class AutoFillManagerServiceStub extends IAutoFillManager.Stub {
        AutoFillManagerServiceStub() {
        }

        public int addClient(IAutoFillManagerClient iAutoFillManagerClient, int i) {
            int i2;
            synchronized (AutofillManagerService.this.mLock) {
                i2 = 0;
                if (AutofillManagerService.this.getServiceForUserLocked(i).addClientLocked(iAutoFillManagerClient)) {
                    i2 = 1;
                }
                if (Helper.sDebug) {
                    i2 |= 2;
                }
                if (Helper.sVerbose) {
                    i2 |= 4;
                }
            }
            return i2;
        }

        public void removeClient(IAutoFillManagerClient iAutoFillManagerClient, int i) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.removeClientLocked(iAutoFillManagerClient);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "removeClient(): no service for " + i);
                }
            }
        }

        public void setAuthenticationResult(Bundle bundle, int i, int i2, int i3) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerService.this.getServiceForUserLocked(i3).setAuthenticationResultLocked(bundle, i, i2, getCallingUid());
            }
        }

        public void setHasCallback(int i, int i2, boolean z) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerService.this.getServiceForUserLocked(i2).setHasCallback(i, getCallingUid(), z);
            }
        }

        public int startSession(IBinder iBinder, IBinder iBinder2, AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i, boolean z, int i2, ComponentName componentName, boolean z2) {
            int iStartSessionLocked;
            IBinder iBinder3 = (IBinder) Preconditions.checkNotNull(iBinder, "activityToken");
            IBinder iBinder4 = (IBinder) Preconditions.checkNotNull(iBinder2, "appCallback");
            AutofillId autofillId2 = (AutofillId) Preconditions.checkNotNull(autofillId, "autoFillId");
            ComponentName componentName2 = (ComponentName) Preconditions.checkNotNull(componentName, "componentName");
            String str = (String) Preconditions.checkNotNull(componentName2.getPackageName());
            Preconditions.checkArgument(i == UserHandle.getUserId(getCallingUid()), "userId");
            try {
                AutofillManagerService.this.mContext.getPackageManager().getPackageInfoAsUser(str, 0, i);
                synchronized (AutofillManagerService.this.mLock) {
                    iStartSessionLocked = AutofillManagerService.this.getServiceForUserLocked(i).startSessionLocked(iBinder3, getCallingUid(), iBinder4, autofillId2, rect, autofillValue, z, componentName2, z2, AutofillManagerService.this.mAllowInstantService, i2);
                }
                return iStartSessionLocked;
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException(str + " is not a valid package", e);
            }
        }

        public FillEventHistory getFillEventHistory() throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    return autofillManagerServiceImplPeekServiceForUserLocked.getFillEventHistory(getCallingUid());
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getFillEventHistory(): no service for " + callingUserId);
                }
                return null;
            }
        }

        public UserData getUserData() throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    return autofillManagerServiceImplPeekServiceForUserLocked.getUserData(getCallingUid());
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getUserData(): no service for " + callingUserId);
                }
                return null;
            }
        }

        public String getUserDataId() throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                String id = null;
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    UserData userData = autofillManagerServiceImplPeekServiceForUserLocked.getUserData(getCallingUid());
                    if (userData != null) {
                        id = userData.getId();
                    }
                    return id;
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getUserDataId(): no service for " + callingUserId);
                }
                return null;
            }
        }

        public void setUserData(UserData userData) throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.setUserData(getCallingUid(), userData);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "setUserData(): no service for " + callingUserId);
                }
            }
        }

        public boolean isFieldClassificationEnabled() throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    return autofillManagerServiceImplPeekServiceForUserLocked.isFieldClassificationEnabled(getCallingUid());
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "isFieldClassificationEnabled(): no service for " + callingUserId);
                }
                return false;
            }
        }

        public String getDefaultFieldClassificationAlgorithm() throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    return autofillManagerServiceImplPeekServiceForUserLocked.getDefaultFieldClassificationAlgorithm(getCallingUid());
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getDefaultFcAlgorithm(): no service for " + callingUserId);
                }
                return null;
            }
        }

        public String[] getAvailableFieldClassificationAlgorithms() throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    return autofillManagerServiceImplPeekServiceForUserLocked.getAvailableFieldClassificationAlgorithms(getCallingUid());
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getAvailableFcAlgorithms(): no service for " + callingUserId);
                }
                return null;
            }
        }

        public ComponentName getAutofillServiceComponentName() throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(callingUserId);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    return autofillManagerServiceImplPeekServiceForUserLocked.getServiceComponentName();
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "getAutofillServiceComponentName(): no service for " + callingUserId);
                }
                return null;
            }
        }

        public boolean restoreSession(int i, IBinder iBinder, IBinder iBinder2) throws RemoteException {
            int callingUserId = UserHandle.getCallingUserId();
            IBinder iBinder3 = (IBinder) Preconditions.checkNotNull(iBinder, "activityToken");
            IBinder iBinder4 = (IBinder) Preconditions.checkNotNull(iBinder2, "appCallback");
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImpl = (AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.get(callingUserId);
                if (autofillManagerServiceImpl != null) {
                    return autofillManagerServiceImpl.restoreSession(i, getCallingUid(), iBinder3, iBinder4);
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "restoreSession(): no service for " + callingUserId);
                }
                return false;
            }
        }

        public void updateSession(int i, AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i2, int i3, int i4) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i4);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.updateSessionLocked(i, getCallingUid(), autofillId, rect, autofillValue, i2, i3);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "updateSession(): no service for " + i4);
                }
            }
        }

        public int updateOrRestartSession(IBinder iBinder, IBinder iBinder2, AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i, boolean z, int i2, ComponentName componentName, int i3, int i4, boolean z2) {
            boolean zUpdateSessionLocked;
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    zUpdateSessionLocked = autofillManagerServiceImplPeekServiceForUserLocked.updateSessionLocked(i3, getCallingUid(), autofillId, rect, autofillValue, i4, i2);
                } else {
                    if (Helper.sVerbose) {
                        Slog.v(AutofillManagerService.TAG, "updateOrRestartSession(): no service for " + i);
                    }
                    zUpdateSessionLocked = false;
                }
            }
            if (zUpdateSessionLocked) {
                return startSession(iBinder, iBinder2, autofillId, rect, autofillValue, i, z, i2, componentName, z2);
            }
            return i3;
        }

        public void setAutofillFailure(int i, List<AutofillId> list, int i2) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i2);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.setAutofillFailureLocked(i, getCallingUid(), list);
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "setAutofillFailure(): no service for " + i2);
                }
            }
        }

        public void finishSession(int i, int i2) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i2);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.finishSessionLocked(i, getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "finishSession(): no service for " + i2);
                }
            }
        }

        public void cancelSession(int i, int i2) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i2);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.cancelSessionLocked(i, getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "cancelSession(): no service for " + i2);
                }
            }
        }

        public void disableOwnedAutofillServices(int i) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.disableOwnedAutofillServicesLocked(Binder.getCallingUid());
                } else if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "cancelSession(): no service for " + i);
                }
            }
        }

        public boolean isServiceSupported(int i) {
            boolean z;
            synchronized (AutofillManagerService.this.mLock) {
                z = !AutofillManagerService.this.mDisabledUsers.get(i);
            }
            return z;
        }

        public boolean isServiceEnabled(int i, String str) {
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(i);
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    return Objects.equals(str, autofillManagerServiceImplPeekServiceForUserLocked.getServicePackageName());
                }
                if (Helper.sVerbose) {
                    Slog.v(AutofillManagerService.TAG, "isServiceEnabled(): no service for " + i);
                }
                return false;
            }
        }

        public void onPendingSaveUi(int i, IBinder iBinder) {
            Preconditions.checkNotNull(iBinder, "token");
            Preconditions.checkArgument(i == 1 || i == 2, "invalid operation: %d", new Object[]{Integer.valueOf(i)});
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerServiceImpl autofillManagerServiceImplPeekServiceForUserLocked = AutofillManagerService.this.peekServiceForUserLocked(UserHandle.getCallingUserId());
                if (autofillManagerServiceImplPeekServiceForUserLocked != null) {
                    autofillManagerServiceImplPeekServiceForUserLocked.onPendingSaveUi(i, iBinder);
                }
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) throws Throwable {
            boolean z;
            boolean z2;
            boolean z3;
            if (!DumpUtils.checkDumpPermission(AutofillManagerService.this.mContext, AutofillManagerService.TAG, printWriter)) {
                return;
            }
            if (strArr != null) {
                z = false;
                z2 = true;
                for (String str : strArr) {
                    byte b = -1;
                    int iHashCode = str.hashCode();
                    if (iHashCode != 900765093) {
                        if (iHashCode != 1098711592) {
                            if (iHashCode == 1333069025 && str.equals("--help")) {
                                b = 2;
                            }
                        } else if (str.equals("--no-history")) {
                            b = 0;
                        }
                    } else if (str.equals("--ui-only")) {
                        b = 1;
                    }
                    switch (b) {
                        case 0:
                            z2 = false;
                            break;
                        case 1:
                            z = true;
                            break;
                        case 2:
                            printWriter.println("Usage: dumpsys autofill [--ui-only|--no-history]");
                            return;
                        default:
                            Slog.w(AutofillManagerService.TAG, "Ignoring invalid dump arg: " + str);
                            break;
                    }
                }
            } else {
                z = false;
                z2 = true;
            }
            if (z) {
                AutofillManagerService.this.mUi.dump(printWriter);
                return;
            }
            boolean z4 = Helper.sDebug;
            try {
                synchronized (AutofillManagerService.this.mLock) {
                    try {
                        z3 = Helper.sDebug;
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        AutofillManagerService.this.setDebugLocked(true);
                        printWriter.print("Debug mode: ");
                        printWriter.println(z3);
                        printWriter.print("Verbose mode: ");
                        printWriter.println(Helper.sVerbose);
                        printWriter.print("Disabled users: ");
                        printWriter.println(AutofillManagerService.this.mDisabledUsers);
                        printWriter.print("Max partitions per session: ");
                        printWriter.println(Helper.sPartitionMaxCount);
                        printWriter.print("Max visible datasets: ");
                        printWriter.println(Helper.sVisibleDatasetsMaxCount);
                        if (Helper.sFullScreenMode != null) {
                            printWriter.print("Overridden full-screen mode: ");
                            printWriter.println(Helper.sFullScreenMode);
                        }
                        printWriter.println("User data constraints: ");
                        UserData.dumpConstraints("  ", printWriter);
                        int size = AutofillManagerService.this.mServicesCache.size();
                        printWriter.print("Cached services: ");
                        if (size == 0) {
                            printWriter.println("none");
                        } else {
                            printWriter.println(size);
                            for (int i = 0; i < size; i++) {
                                printWriter.print("\nService at index ");
                                printWriter.println(i);
                                ((AutofillManagerServiceImpl) AutofillManagerService.this.mServicesCache.valueAt(i)).dumpLocked("  ", printWriter);
                            }
                        }
                        AutofillManagerService.this.mUi.dump(printWriter);
                        printWriter.print("Autofill Compat State: ");
                        AutofillManagerService.this.mAutofillCompatState.dump("    ", printWriter);
                        printWriter.print("    ");
                        printWriter.print("from settings: ");
                        printWriter.println(AutofillManagerService.this.getWhitelistedCompatModePackagesFromSettings());
                        printWriter.print("Allow instant service: ");
                        printWriter.println(AutofillManagerService.this.mAllowInstantService);
                        if (z2) {
                            try {
                                printWriter.println();
                                printWriter.println("Requests history:");
                                printWriter.println();
                                AutofillManagerService.this.mRequestsHistory.reverseDump(fileDescriptor, printWriter, strArr);
                                printWriter.println();
                                printWriter.println("UI latency history:");
                                printWriter.println();
                                AutofillManagerService.this.mUiLatencyHistory.reverseDump(fileDescriptor, printWriter, strArr);
                                printWriter.println();
                                printWriter.println("WTF history:");
                                printWriter.println();
                                AutofillManagerService.this.mWtfHistory.reverseDump(fileDescriptor, printWriter, strArr);
                            } catch (Throwable th2) {
                                th = th2;
                                AutofillManagerService.this.setDebugLocked(z3);
                                throw th;
                            }
                        }
                        AutofillManagerService.this.setDebugLocked(z3);
                    } catch (Throwable th3) {
                        th = th3;
                        z4 = z3;
                        throw th;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                z3 = z4;
            }
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            new AutofillManagerServiceShellCommand(AutofillManagerService.this).exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }
    }

    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
            ContentResolver contentResolver = AutofillManagerService.this.mContext.getContentResolver();
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("autofill_service"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, -1);
            contentResolver.registerContentObserver(Settings.Global.getUriFor("autofill_compat_mode_allowed_packages"), false, this, -1);
        }

        @Override
        public void onChange(boolean z, Uri uri, int i) {
            if (Helper.sVerbose) {
                Slog.v(AutofillManagerService.TAG, "onChange(): uri=" + uri + ", userId=" + i);
            }
            synchronized (AutofillManagerService.this.mLock) {
                AutofillManagerService.this.updateCachedServiceLocked(i);
            }
        }
    }
}
