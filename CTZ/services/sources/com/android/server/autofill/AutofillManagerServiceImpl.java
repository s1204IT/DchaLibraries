package com.android.server.autofill;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
import android.app.AppGlobals;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.autofill.AutofillServiceInfo;
import android.service.autofill.FieldClassification;
import android.service.autofill.FillEventHistory;
import android.service.autofill.FillResponse;
import android.service.autofill.UserData;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.LocalLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.autofill.IAutoFillManagerClient;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.server.LocalServices;
import com.android.server.autofill.AutofillManagerService;
import com.android.server.autofill.ui.AutoFillUI;
import com.android.server.job.controllers.JobStatus;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class AutofillManagerServiceImpl {
    private static final int MAX_ABANDONED_SESSION_MILLIS = 30000;
    private static final int MAX_SESSION_ID_CREATE_TRIES = 2048;
    private static final String TAG = "AutofillManagerServiceImpl";
    private static final Random sRandom = new Random();
    private final AutofillManagerService.AutofillCompatState mAutofillCompatState;

    @GuardedBy("mLock")
    private RemoteCallbackList<IAutoFillManagerClient> mClients;
    private final Context mContext;

    @GuardedBy("mLock")
    private boolean mDisabled;

    @GuardedBy("mLock")
    private ArrayMap<ComponentName, Long> mDisabledActivities;

    @GuardedBy("mLock")
    private ArrayMap<String, Long> mDisabledApps;

    @GuardedBy("mLock")
    private FillEventHistory mEventHistory;
    private final FieldClassificationStrategy mFieldClassificationStrategy;

    @GuardedBy("mLock")
    private AutofillServiceInfo mInfo;
    private final Object mLock;
    private final LocalLog mRequestsHistory;

    @GuardedBy("mLock")
    private boolean mSetupComplete;
    private final AutoFillUI mUi;
    private final LocalLog mUiLatencyHistory;

    @GuardedBy("mLock")
    private UserData mUserData;
    private final int mUserId;
    private final LocalLog mWtfHistory;
    private final MetricsLogger mMetricsLogger = new MetricsLogger();
    private final Handler mHandler = new Handler(Looper.getMainLooper(), null, true);

    @GuardedBy("mLock")
    private final SparseArray<Session> mSessions = new SparseArray<>();
    private long mLastPrune = 0;

    AutofillManagerServiceImpl(Context context, Object obj, LocalLog localLog, LocalLog localLog2, LocalLog localLog3, int i, AutoFillUI autoFillUI, AutofillManagerService.AutofillCompatState autofillCompatState, boolean z) {
        this.mContext = context;
        this.mLock = obj;
        this.mRequestsHistory = localLog;
        this.mUiLatencyHistory = localLog2;
        this.mWtfHistory = localLog3;
        this.mUserId = i;
        this.mUi = autoFillUI;
        this.mFieldClassificationStrategy = new FieldClassificationStrategy(context, i);
        this.mAutofillCompatState = autofillCompatState;
        updateLocked(z);
    }

    CharSequence getServiceName() {
        String servicePackageName = getServicePackageName();
        if (servicePackageName == null) {
            return null;
        }
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            return packageManager.getApplicationLabel(packageManager.getApplicationInfo(servicePackageName, 0));
        } catch (Exception e) {
            Slog.e(TAG, "Could not get label for " + servicePackageName + ": " + e);
            return servicePackageName;
        }
    }

    @GuardedBy("mLock")
    private int getServiceUidLocked() {
        if (this.mInfo == null) {
            Slog.w(TAG, "getServiceUidLocked(): no mInfo");
            return -1;
        }
        return this.mInfo.getServiceInfo().applicationInfo.uid;
    }

    String[] getUrlBarResourceIdsForCompatMode(String str) {
        return this.mAutofillCompatState.getUrlBarResourceIds(str, this.mUserId);
    }

    String getServicePackageName() {
        ComponentName serviceComponentName = getServiceComponentName();
        if (serviceComponentName != null) {
            return serviceComponentName.getPackageName();
        }
        return null;
    }

    ComponentName getServiceComponentName() {
        synchronized (this.mLock) {
            if (this.mInfo == null) {
                return null;
            }
            return this.mInfo.getServiceInfo().getComponentName();
        }
    }

    private boolean isSetupCompletedLocked() {
        return "1".equals(Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "user_setup_complete", this.mUserId));
    }

    private String getComponentNameFromSettings() {
        return Settings.Secure.getStringForUser(this.mContext.getContentResolver(), "autofill_service", this.mUserId);
    }

    @GuardedBy("mLock")
    void updateLocked(boolean z) {
        ComponentName componentNameUnflattenFromString;
        ServiceInfo serviceInfo;
        boolean zIsEnabledLocked = isEnabledLocked();
        if (Helper.sVerbose) {
            Slog.v(TAG, "updateLocked(u=" + this.mUserId + "): wasEnabled=" + zIsEnabledLocked + ", mSetupComplete= " + this.mSetupComplete + ", disabled=" + z + ", mDisabled=" + this.mDisabled);
        }
        this.mSetupComplete = isSetupCompletedLocked();
        this.mDisabled = z;
        String componentNameFromSettings = getComponentNameFromSettings();
        if (TextUtils.isEmpty(componentNameFromSettings)) {
            componentNameUnflattenFromString = null;
            serviceInfo = null;
        } else {
            try {
                componentNameUnflattenFromString = ComponentName.unflattenFromString(componentNameFromSettings);
                try {
                    serviceInfo = AppGlobals.getPackageManager().getServiceInfo(componentNameUnflattenFromString, 0, this.mUserId);
                    if (serviceInfo == null) {
                        Slog.e(TAG, "Bad AutofillService name: " + componentNameFromSettings);
                    }
                } catch (RemoteException | RuntimeException e) {
                    e = e;
                    Slog.e(TAG, "Error getting service info for '" + componentNameFromSettings + "': " + e);
                    serviceInfo = null;
                }
            } catch (RemoteException | RuntimeException e2) {
                e = e2;
                componentNameUnflattenFromString = null;
            }
        }
        try {
            if (serviceInfo != null) {
                this.mInfo = new AutofillServiceInfo(this.mContext, componentNameUnflattenFromString, this.mUserId);
                if (Helper.sDebug) {
                    Slog.d(TAG, "Set component for user " + this.mUserId + " as " + this.mInfo);
                }
            } else {
                this.mInfo = null;
                if (Helper.sDebug) {
                    Slog.d(TAG, "Reset component for user " + this.mUserId + " (" + componentNameFromSettings + ")");
                }
            }
        } catch (Exception e3) {
            Slog.e(TAG, "Bad AutofillServiceInfo for '" + componentNameFromSettings + "': " + e3);
            this.mInfo = null;
        }
        boolean zIsEnabledLocked2 = isEnabledLocked();
        if (zIsEnabledLocked != zIsEnabledLocked2) {
            if (!zIsEnabledLocked2) {
                for (int size = this.mSessions.size() - 1; size >= 0; size--) {
                    this.mSessions.valueAt(size).removeSelfLocked();
                }
            }
            sendStateToClients(false);
        }
    }

    @GuardedBy("mLock")
    boolean addClientLocked(IAutoFillManagerClient iAutoFillManagerClient) {
        if (this.mClients == null) {
            this.mClients = new RemoteCallbackList<>();
        }
        this.mClients.register(iAutoFillManagerClient);
        return isEnabledLocked();
    }

    @GuardedBy("mLock")
    void removeClientLocked(IAutoFillManagerClient iAutoFillManagerClient) {
        if (this.mClients != null) {
            this.mClients.unregister(iAutoFillManagerClient);
        }
    }

    @GuardedBy("mLock")
    void setAuthenticationResultLocked(Bundle bundle, int i, int i2, int i3) {
        Session session;
        if (isEnabledLocked() && (session = this.mSessions.get(i)) != null && i3 == session.uid) {
            session.setAuthenticationResultLocked(bundle, i2);
        }
    }

    void setHasCallback(int i, int i2, boolean z) {
        Session session;
        if (isEnabledLocked() && (session = this.mSessions.get(i)) != null && i2 == session.uid) {
            synchronized (this.mLock) {
                session.setHasCallbackLocked(z);
            }
        }
    }

    @GuardedBy("mLock")
    int startSessionLocked(IBinder iBinder, int i, IBinder iBinder2, AutofillId autofillId, Rect rect, AutofillValue autofillValue, boolean z, ComponentName componentName, boolean z2, boolean z3, int i2) {
        IBinder iBinder3;
        if (!isEnabledLocked()) {
            return 0;
        }
        String shortString = componentName.toShortString();
        if (isAutofillDisabledLocked(componentName)) {
            if (Helper.sDebug) {
                Slog.d(TAG, "startSession(" + shortString + "): ignored because disabled by service");
            }
            try {
                IAutoFillManagerClient.Stub.asInterface(iBinder2).setSessionFinished(4);
            } catch (RemoteException e) {
                Slog.w(TAG, "Could not notify " + shortString + " that it's disabled: " + e);
            }
            return Integer.MIN_VALUE;
        }
        if (Helper.sVerbose) {
            StringBuilder sb = new StringBuilder();
            sb.append("startSession(): token=");
            iBinder3 = iBinder;
            sb.append(iBinder3);
            sb.append(", flags=");
            sb.append(i2);
            Slog.v(TAG, sb.toString());
        } else {
            iBinder3 = iBinder;
        }
        pruneAbandonedSessionsLocked();
        Session sessionCreateSessionByTokenLocked = createSessionByTokenLocked(iBinder3, i, iBinder2, z, componentName, z2, z3, i2);
        if (sessionCreateSessionByTokenLocked == null) {
            return Integer.MIN_VALUE;
        }
        this.mRequestsHistory.log("id=" + sessionCreateSessionByTokenLocked.id + " uid=" + i + " a=" + shortString + " s=" + this.mInfo.getServiceInfo().packageName + " u=" + this.mUserId + " i=" + autofillId + " b=" + rect + " hc=" + z + " f=" + i2);
        sessionCreateSessionByTokenLocked.updateLocked(autofillId, rect, autofillValue, 1, i2);
        return sessionCreateSessionByTokenLocked.id;
    }

    @GuardedBy("mLock")
    private void pruneAbandonedSessionsLocked() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        if (this.mLastPrune < jCurrentTimeMillis - 30000) {
            this.mLastPrune = jCurrentTimeMillis;
            if (this.mSessions.size() > 0) {
                new PruneTask().execute(new Void[0]);
            }
        }
    }

    @GuardedBy("mLock")
    void setAutofillFailureLocked(int i, int i2, List<AutofillId> list) {
        if (!isEnabledLocked()) {
            return;
        }
        Session session = this.mSessions.get(i);
        if (session == null || i2 != session.uid) {
            Slog.v(TAG, "setAutofillFailure(): no session for " + i + "(" + i2 + ")");
            return;
        }
        session.setAutofillFailureLocked(list);
    }

    @GuardedBy("mLock")
    void finishSessionLocked(int i, int i2) {
        if (!isEnabledLocked()) {
            return;
        }
        Session session = this.mSessions.get(i);
        if (session == null || i2 != session.uid) {
            if (Helper.sVerbose) {
                Slog.v(TAG, "finishSessionLocked(): no session for " + i + "(" + i2 + ")");
                return;
            }
            return;
        }
        session.logContextCommitted();
        boolean zShowSaveLocked = session.showSaveLocked();
        if (Helper.sVerbose) {
            Slog.v(TAG, "finishSessionLocked(): session finished on save? " + zShowSaveLocked);
        }
        if (zShowSaveLocked) {
            session.removeSelfLocked();
        }
    }

    @GuardedBy("mLock")
    void cancelSessionLocked(int i, int i2) {
        if (!isEnabledLocked()) {
            return;
        }
        Session session = this.mSessions.get(i);
        if (session == null || i2 != session.uid) {
            Slog.w(TAG, "cancelSessionLocked(): no session for " + i + "(" + i2 + ")");
            return;
        }
        session.removeSelfLocked();
    }

    @GuardedBy("mLock")
    void disableOwnedAutofillServicesLocked(int i) {
        Slog.i(TAG, "disableOwnedServices(" + i + "): " + this.mInfo);
        if (this.mInfo == null) {
            return;
        }
        ServiceInfo serviceInfo = this.mInfo.getServiceInfo();
        if (serviceInfo.applicationInfo.uid != i) {
            Slog.w(TAG, "disableOwnedServices(): ignored when called by UID " + i + " instead of " + serviceInfo.applicationInfo.uid + " for service " + this.mInfo);
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            String componentNameFromSettings = getComponentNameFromSettings();
            ComponentName componentName = serviceInfo.getComponentName();
            if (componentName.equals(ComponentName.unflattenFromString(componentNameFromSettings))) {
                this.mMetricsLogger.action(1135, componentName.getPackageName());
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "autofill_service", null, this.mUserId);
                destroySessionsLocked();
            } else {
                Slog.w(TAG, "disableOwnedServices(): ignored because current service (" + serviceInfo + ") does not match Settings (" + componentNameFromSettings + ")");
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    @GuardedBy("mLock")
    private Session createSessionByTokenLocked(IBinder iBinder, int i, IBinder iBinder2, boolean z, ComponentName componentName, boolean z2, boolean z3, int i2) {
        AutofillManagerServiceImpl autofillManagerServiceImpl = this;
        int i3 = 0;
        while (true) {
            i3++;
            if (i3 > 2048) {
                Slog.w(TAG, "Cannot create session in 2048 tries");
                return null;
            }
            int iNextInt = sRandom.nextInt();
            if (iNextInt == Integer.MIN_VALUE || autofillManagerServiceImpl.mSessions.indexOfKey(iNextInt) >= 0) {
                autofillManagerServiceImpl = autofillManagerServiceImpl;
            } else {
                autofillManagerServiceImpl.assertCallerLocked(componentName, z2);
                Session session = new Session(autofillManagerServiceImpl, autofillManagerServiceImpl.mUi, autofillManagerServiceImpl.mContext, autofillManagerServiceImpl.mHandler, autofillManagerServiceImpl.mUserId, autofillManagerServiceImpl.mLock, iNextInt, i, iBinder, iBinder2, z, autofillManagerServiceImpl.mUiLatencyHistory, autofillManagerServiceImpl.mWtfHistory, autofillManagerServiceImpl.mInfo.getServiceInfo().getComponentName(), componentName, z2, z3, i2);
                this.mSessions.put(session.id, session);
                return session;
            }
        }
    }

    private void assertCallerLocked(ComponentName componentName, boolean z) {
        String str;
        String packageName = componentName.getPackageName();
        PackageManager packageManager = this.mContext.getPackageManager();
        int callingUid = Binder.getCallingUid();
        try {
            int packageUidAsUser = packageManager.getPackageUidAsUser(packageName, UserHandle.getCallingUserId());
            if (callingUid != packageUidAsUser && !((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).hasRunningActivity(callingUid, packageName)) {
                String[] packagesForUid = packageManager.getPackagesForUid(callingUid);
                if (packagesForUid != null) {
                    str = packagesForUid[0];
                } else {
                    str = "uid-" + callingUid;
                }
                Slog.w(TAG, "App (package=" + str + ", UID=" + callingUid + ") passed component (" + componentName + ") owned by UID " + packageUidAsUser);
                LogMaker logMakerAddTaggedData = new LogMaker(948).setPackageName(str).addTaggedData(908, getServicePackageName()).addTaggedData(949, componentName == null ? "null" : componentName.flattenToShortString());
                if (z) {
                    logMakerAddTaggedData.addTaggedData(1414, 1);
                }
                this.mMetricsLogger.write(logMakerAddTaggedData);
                throw new SecurityException("Invalid component: " + componentName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Could not verify UID for " + componentName);
        }
    }

    boolean restoreSession(int i, int i2, IBinder iBinder, IBinder iBinder2) {
        Session session = this.mSessions.get(i);
        if (session == null || i2 != session.uid) {
            return false;
        }
        session.switchActivity(iBinder, iBinder2);
        return true;
    }

    @GuardedBy("mLock")
    boolean updateSessionLocked(int i, int i2, AutofillId autofillId, Rect rect, AutofillValue autofillValue, int i3, int i4) {
        Session session = this.mSessions.get(i);
        if (session != null && session.uid == i2) {
            session.updateLocked(autofillId, rect, autofillValue, i3, i4);
            return false;
        }
        if ((i4 & 1) != 0) {
            if (Helper.sDebug) {
                Slog.d(TAG, "restarting session " + i + " due to manual request on " + autofillId);
            }
            return true;
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "updateSessionLocked(): session gone for " + i + "(" + i2 + ")");
        }
        return false;
    }

    @GuardedBy("mLock")
    void removeSessionLocked(int i) {
        this.mSessions.remove(i);
    }

    void handleSessionSave(Session session) {
        synchronized (this.mLock) {
            if (this.mSessions.get(session.id) == null) {
                Slog.w(TAG, "handleSessionSave(): already gone: " + session.id);
                return;
            }
            session.callSaveLocked();
        }
    }

    void onPendingSaveUi(int i, IBinder iBinder) {
        if (Helper.sVerbose) {
            Slog.v(TAG, "onPendingSaveUi(" + i + "): " + iBinder);
        }
        synchronized (this.mLock) {
            for (int size = this.mSessions.size() - 1; size >= 0; size--) {
                Session sessionValueAt = this.mSessions.valueAt(size);
                if (sessionValueAt.isSaveUiPendingForTokenLocked(iBinder)) {
                    sessionValueAt.onPendingSaveUi(i, iBinder);
                    return;
                }
            }
            if (Helper.sDebug) {
                Slog.d(TAG, "No pending Save UI for token " + iBinder + " and operation " + DebugUtils.flagsToString(AutofillManager.class, "PENDING_UI_OPERATION_", i));
            }
        }
    }

    @GuardedBy("mLock")
    void handlePackageUpdateLocked(String str) {
        ServiceInfo serviceInfo = this.mFieldClassificationStrategy.getServiceInfo();
        if (serviceInfo != null && serviceInfo.packageName.equals(str)) {
            resetExtServiceLocked();
        }
    }

    @GuardedBy("mLock")
    void resetExtServiceLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "reset autofill service.");
        }
        this.mFieldClassificationStrategy.reset();
    }

    @GuardedBy("mLock")
    void destroyLocked() {
        if (Helper.sVerbose) {
            Slog.v(TAG, "destroyLocked()");
        }
        resetExtServiceLocked();
        int size = this.mSessions.size();
        ArraySet arraySet = new ArraySet(size);
        for (int i = 0; i < size; i++) {
            RemoteFillService remoteFillServiceDestroyLocked = this.mSessions.valueAt(i).destroyLocked();
            if (remoteFillServiceDestroyLocked != null) {
                arraySet.add(remoteFillServiceDestroyLocked);
            }
        }
        this.mSessions.clear();
        for (int i2 = 0; i2 < arraySet.size(); i2++) {
            ((RemoteFillService) arraySet.valueAt(i2)).destroy();
        }
        sendStateToClients(true);
        if (this.mClients != null) {
            this.mClients.kill();
            this.mClients = null;
        }
    }

    CharSequence getServiceLabel() {
        return this.mInfo.getServiceInfo().loadSafeLabel(this.mContext.getPackageManager(), 0.0f, 5);
    }

    Drawable getServiceIcon() {
        return this.mInfo.getServiceInfo().loadIcon(this.mContext.getPackageManager());
    }

    void setLastResponse(int i, FillResponse fillResponse) {
        synchronized (this.mLock) {
            this.mEventHistory = new FillEventHistory(i, fillResponse.getClientState());
        }
    }

    void resetLastResponse() {
        synchronized (this.mLock) {
            this.mEventHistory = null;
        }
    }

    @GuardedBy("mLock")
    private boolean isValidEventLocked(String str, int i) {
        if (this.mEventHistory == null) {
            Slog.w(TAG, str + ": not logging event because history is null");
            return false;
        }
        if (i != this.mEventHistory.getSessionId()) {
            if (Helper.sDebug) {
                Slog.d(TAG, str + ": not logging event for session " + i + " because tracked session is " + this.mEventHistory.getSessionId());
            }
            return false;
        }
        return true;
    }

    void setAuthenticationSelected(int i, Bundle bundle) {
        synchronized (this.mLock) {
            if (isValidEventLocked("setAuthenticationSelected()", i)) {
                this.mEventHistory.addEvent(new FillEventHistory.Event(2, null, bundle, null, null, null, null, null, null, null, null));
            }
        }
    }

    void logDatasetAuthenticationSelected(String str, int i, Bundle bundle) {
        synchronized (this.mLock) {
            if (isValidEventLocked("logDatasetAuthenticationSelected()", i)) {
                this.mEventHistory.addEvent(new FillEventHistory.Event(1, str, bundle, null, null, null, null, null, null, null, null));
            }
        }
    }

    void logSaveShown(int i, Bundle bundle) {
        synchronized (this.mLock) {
            if (isValidEventLocked("logSaveShown()", i)) {
                this.mEventHistory.addEvent(new FillEventHistory.Event(3, null, bundle, null, null, null, null, null, null, null, null));
            }
        }
    }

    void logDatasetSelected(String str, int i, Bundle bundle) {
        synchronized (this.mLock) {
            if (isValidEventLocked("logDatasetSelected()", i)) {
                this.mEventHistory.addEvent(new FillEventHistory.Event(0, str, bundle, null, null, null, null, null, null, null, null));
            }
        }
    }

    @GuardedBy("mLock")
    void logContextCommittedLocked(int i, Bundle bundle, ArrayList<String> arrayList, ArraySet<String> arraySet, ArrayList<AutofillId> arrayList2, ArrayList<String> arrayList3, ArrayList<AutofillId> arrayList4, ArrayList<ArrayList<String>> arrayList5, ComponentName componentName, boolean z) {
        logContextCommittedLocked(i, bundle, arrayList, arraySet, arrayList2, arrayList3, arrayList4, arrayList5, null, null, componentName, z);
    }

    @GuardedBy("mLock")
    void logContextCommittedLocked(int i, Bundle bundle, ArrayList<String> arrayList, ArraySet<String> arraySet, ArrayList<AutofillId> arrayList2, ArrayList<String> arrayList3, ArrayList<AutofillId> arrayList4, ArrayList<ArrayList<String>> arrayList5, ArrayList<AutofillId> arrayList6, ArrayList<FieldClassification> arrayList7, ComponentName componentName, boolean z) {
        ArrayList<String> arrayList8;
        ArraySet<String> arraySet2;
        ArrayList<AutofillId> arrayList9;
        ArrayList<String> arrayList10;
        AutofillId[] autofillIdArr;
        FieldClassification[] fieldClassificationArr;
        if (isValidEventLocked("logDatasetNotSelected()", i)) {
            if (Helper.sVerbose) {
                StringBuilder sb = new StringBuilder();
                sb.append("logContextCommitted() with FieldClassification: id=");
                sb.append(i);
                sb.append(", selectedDatasets=");
                arrayList8 = arrayList;
                sb.append(arrayList8);
                sb.append(", ignoredDatasetIds=");
                arraySet2 = arraySet;
                sb.append(arraySet2);
                sb.append(", changedAutofillIds=");
                arrayList9 = arrayList2;
                sb.append(arrayList9);
                sb.append(", changedDatasetIds=");
                arrayList10 = arrayList3;
                sb.append(arrayList10);
                sb.append(", manuallyFilledFieldIds=");
                sb.append(arrayList4);
                sb.append(", detectedFieldIds=");
                sb.append(arrayList6);
                sb.append(", detectedFieldClassifications=");
                sb.append(arrayList7);
                sb.append(", appComponentName=");
                sb.append(componentName.toShortString());
                sb.append(", compatMode=");
                sb.append(z);
                Slog.v(TAG, sb.toString());
            } else {
                arrayList8 = arrayList;
                arraySet2 = arraySet;
                arrayList9 = arrayList2;
                arrayList10 = arrayList3;
            }
            if (arrayList6 == null) {
                autofillIdArr = null;
                fieldClassificationArr = null;
            } else {
                AutofillId[] autofillIdArr2 = new AutofillId[arrayList6.size()];
                arrayList6.toArray(autofillIdArr2);
                FieldClassification[] fieldClassificationArr2 = new FieldClassification[arrayList7.size()];
                arrayList7.toArray(fieldClassificationArr2);
                int length = autofillIdArr2.length;
                float f = 0.0f;
                int i2 = 0;
                int i3 = 0;
                while (i2 < length) {
                    List<FieldClassification.Match> matches = fieldClassificationArr2[i2].getMatches();
                    int size = matches.size();
                    i3 += size;
                    float score = f;
                    int i4 = 0;
                    while (i4 < size) {
                        score += matches.get(i4).getScore();
                        i4++;
                        fieldClassificationArr2 = fieldClassificationArr2;
                    }
                    i2++;
                    f = score;
                }
                this.mMetricsLogger.write(Helper.newLogMaker(1273, componentName, getServicePackageName(), i, z).setCounterValue(length).addTaggedData(1274, Integer.valueOf((int) ((f * 100.0f) / i3))));
                autofillIdArr = autofillIdArr2;
                fieldClassificationArr = fieldClassificationArr2;
            }
            this.mEventHistory.addEvent(new FillEventHistory.Event(4, null, bundle, arrayList8, arraySet2, arrayList9, arrayList10, arrayList4, arrayList5, autofillIdArr, fieldClassificationArr));
        }
    }

    FillEventHistory getFillEventHistory(int i) {
        synchronized (this.mLock) {
            if (this.mEventHistory != null && isCalledByServiceLocked("getFillEventHistory", i)) {
                return this.mEventHistory;
            }
            return null;
        }
    }

    UserData getUserData() {
        UserData userData;
        synchronized (this.mLock) {
            userData = this.mUserData;
        }
        return userData;
    }

    UserData getUserData(int i) {
        synchronized (this.mLock) {
            if (isCalledByServiceLocked("getUserData", i)) {
                return this.mUserData;
            }
            return null;
        }
    }

    void setUserData(int i, UserData userData) {
        synchronized (this.mLock) {
            if (isCalledByServiceLocked("setUserData", i)) {
                this.mUserData = userData;
                this.mMetricsLogger.write(new LogMaker(1272).setPackageName(getServicePackageName()).addTaggedData(914, Integer.valueOf(this.mUserData == null ? 0 : this.mUserData.getCategoryIds().length)));
            }
        }
    }

    @GuardedBy("mLock")
    private boolean isCalledByServiceLocked(String str, int i) {
        if (getServiceUidLocked() != i) {
            Slog.w(TAG, str + "() called by UID " + i + ", but service UID is " + getServiceUidLocked());
            return false;
        }
        return true;
    }

    @GuardedBy("mLock")
    void dumpLocked(String str, PrintWriter printWriter) {
        String str2 = str + "  ";
        printWriter.print(str);
        printWriter.print("User: ");
        printWriter.println(this.mUserId);
        printWriter.print(str);
        printWriter.print("UID: ");
        printWriter.println(getServiceUidLocked());
        printWriter.print(str);
        printWriter.print("Autofill Service Info: ");
        if (this.mInfo == null) {
            printWriter.println("N/A");
        } else {
            printWriter.println();
            this.mInfo.dump(str2, printWriter);
            printWriter.print(str);
            printWriter.print("Service Label: ");
            printWriter.println(getServiceLabel());
        }
        printWriter.print(str);
        printWriter.print("Component from settings: ");
        printWriter.println(getComponentNameFromSettings());
        printWriter.print(str);
        printWriter.print("Default component: ");
        printWriter.println(this.mContext.getString(R.string.action_bar_home_description));
        printWriter.print(str);
        printWriter.print("Disabled: ");
        printWriter.println(this.mDisabled);
        printWriter.print(str);
        printWriter.print("Field classification enabled: ");
        printWriter.println(isFieldClassificationEnabledLocked());
        printWriter.print(str);
        printWriter.print("Compat pkgs: ");
        Object compatibilityPackagesLocked = getCompatibilityPackagesLocked();
        if (compatibilityPackagesLocked == null) {
            printWriter.println("N/A");
        } else {
            printWriter.println(compatibilityPackagesLocked);
        }
        printWriter.print(str);
        printWriter.print("Setup complete: ");
        printWriter.println(this.mSetupComplete);
        printWriter.print(str);
        printWriter.print("Last prune: ");
        printWriter.println(this.mLastPrune);
        printWriter.print(str);
        printWriter.print("Disabled apps: ");
        if (this.mDisabledApps == null) {
            printWriter.println("N/A");
        } else {
            int size = this.mDisabledApps.size();
            printWriter.println(size);
            StringBuilder sb = new StringBuilder();
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            for (int i = 0; i < size; i++) {
                String strKeyAt = this.mDisabledApps.keyAt(i);
                long jLongValue = this.mDisabledApps.valueAt(i).longValue();
                sb.append(str);
                sb.append(str);
                sb.append(i);
                sb.append(". ");
                sb.append(strKeyAt);
                sb.append(": ");
                TimeUtils.formatDuration(jLongValue - jElapsedRealtime, sb);
                sb.append('\n');
            }
            printWriter.println(sb);
        }
        printWriter.print(str);
        printWriter.print("Disabled activities: ");
        if (this.mDisabledActivities == null) {
            printWriter.println("N/A");
        } else {
            int size2 = this.mDisabledActivities.size();
            printWriter.println(size2);
            StringBuilder sb2 = new StringBuilder();
            long jElapsedRealtime2 = SystemClock.elapsedRealtime();
            for (int i2 = 0; i2 < size2; i2++) {
                ComponentName componentNameKeyAt = this.mDisabledActivities.keyAt(i2);
                long jLongValue2 = this.mDisabledActivities.valueAt(i2).longValue();
                sb2.append(str);
                sb2.append(str);
                sb2.append(i2);
                sb2.append(". ");
                sb2.append(componentNameKeyAt);
                sb2.append(": ");
                TimeUtils.formatDuration(jLongValue2 - jElapsedRealtime2, sb2);
                sb2.append('\n');
            }
            printWriter.println(sb2);
        }
        int size3 = this.mSessions.size();
        if (size3 == 0) {
            printWriter.print(str);
            printWriter.println("No sessions");
        } else {
            printWriter.print(str);
            printWriter.print(size3);
            printWriter.println(" sessions:");
            int i3 = 0;
            while (i3 < size3) {
                printWriter.print(str);
                printWriter.print("#");
                int i4 = i3 + 1;
                printWriter.println(i4);
                this.mSessions.valueAt(i3).dumpLocked(str2, printWriter);
                i3 = i4;
            }
        }
        printWriter.print(str);
        printWriter.print("Clients: ");
        if (this.mClients == null) {
            printWriter.println("N/A");
        } else {
            printWriter.println();
            this.mClients.dump(printWriter, str2);
        }
        if (this.mEventHistory == null || this.mEventHistory.getEvents() == null || this.mEventHistory.getEvents().size() == 0) {
            printWriter.print(str);
            printWriter.println("No event on last fill response");
        } else {
            printWriter.print(str);
            printWriter.println("Events of last fill response:");
            printWriter.print(str);
            int size4 = this.mEventHistory.getEvents().size();
            for (int i5 = 0; i5 < size4; i5++) {
                FillEventHistory.Event event = this.mEventHistory.getEvents().get(i5);
                printWriter.println("  " + i5 + ": eventType=" + event.getType() + " datasetId=" + event.getDatasetId());
            }
        }
        printWriter.print(str);
        printWriter.print("User data: ");
        if (this.mUserData == null) {
            printWriter.println("N/A");
        } else {
            printWriter.println();
            this.mUserData.dump(str2, printWriter);
        }
        printWriter.print(str);
        printWriter.println("Field Classification strategy: ");
        this.mFieldClassificationStrategy.dump(str2, printWriter);
    }

    @GuardedBy("mLock")
    void destroySessionsLocked() {
        if (this.mSessions.size() == 0) {
            this.mUi.destroyAll(null, null, false);
        } else {
            while (this.mSessions.size() > 0) {
                this.mSessions.valueAt(0).forceRemoveSelfLocked();
            }
        }
    }

    @GuardedBy("mLock")
    void destroyFinishedSessionsLocked() {
        for (int size = this.mSessions.size() - 1; size >= 0; size--) {
            Session sessionValueAt = this.mSessions.valueAt(size);
            if (sessionValueAt.isSavingLocked()) {
                if (Helper.sDebug) {
                    Slog.d(TAG, "destroyFinishedSessionsLocked(): " + sessionValueAt.id);
                }
                sessionValueAt.forceRemoveSelfLocked();
            }
        }
    }

    @GuardedBy("mLock")
    void listSessionsLocked(ArrayList<String> arrayList) {
        int size = this.mSessions.size();
        for (int i = 0; i < size; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.mInfo != null ? this.mInfo.getServiceInfo().getComponentName() : null);
            sb.append(":");
            sb.append(this.mSessions.keyAt(i));
            arrayList.add(sb.toString());
        }
    }

    @GuardedBy("mLock")
    ArrayMap<String, Long> getCompatibilityPackagesLocked() {
        if (this.mInfo != null) {
            return this.mInfo.getCompatibilityPackages();
        }
        return null;
    }

    private void sendStateToClients(boolean z) {
        int i;
        boolean z2;
        boolean zIsEnabledLocked;
        synchronized (this.mLock) {
            if (this.mClients == null) {
                return;
            }
            RemoteCallbackList<IAutoFillManagerClient> remoteCallbackList = this.mClients;
            int iBeginBroadcast = remoteCallbackList.beginBroadcast();
            for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                try {
                    IAutoFillManagerClient iAutoFillManagerClient = (IAutoFillManagerClient) remoteCallbackList.getBroadcastItem(i2);
                    try {
                    } catch (RemoteException e) {
                    }
                    synchronized (this.mLock) {
                        i = 1;
                        if (z) {
                            z2 = true;
                            zIsEnabledLocked = isEnabledLocked();
                        } else {
                            try {
                                if (isClientSessionDestroyedLocked(iAutoFillManagerClient)) {
                                    z2 = true;
                                    zIsEnabledLocked = isEnabledLocked();
                                } else {
                                    z2 = false;
                                    zIsEnabledLocked = isEnabledLocked();
                                }
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                    }
                    if (!zIsEnabledLocked) {
                        i = 0;
                    }
                    if (z2) {
                        i |= 2;
                    }
                    if (z) {
                        i |= 4;
                    }
                    if (Helper.sDebug) {
                        i |= 8;
                    }
                    if (Helper.sVerbose) {
                        i |= 16;
                    }
                    iAutoFillManagerClient.setState(i);
                } finally {
                    remoteCallbackList.finishBroadcast();
                }
            }
        }
    }

    @GuardedBy("mLock")
    private boolean isClientSessionDestroyedLocked(IAutoFillManagerClient iAutoFillManagerClient) {
        int size = this.mSessions.size();
        for (int i = 0; i < size; i++) {
            Session sessionValueAt = this.mSessions.valueAt(i);
            if (sessionValueAt.getClient().equals(iAutoFillManagerClient)) {
                return sessionValueAt.isDestroyed();
            }
        }
        return true;
    }

    @GuardedBy("mLock")
    boolean isEnabledLocked() {
        return (!this.mSetupComplete || this.mInfo == null || this.mDisabled) ? false : true;
    }

    void disableAutofillForApp(String str, long j, int i, boolean z) {
        synchronized (this.mLock) {
            if (this.mDisabledApps == null) {
                this.mDisabledApps = new ArrayMap<>(1);
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
            if (jElapsedRealtime < 0) {
                jElapsedRealtime = JobStatus.NO_LATEST_RUNTIME;
            }
            this.mDisabledApps.put(str, Long.valueOf(jElapsedRealtime));
            this.mMetricsLogger.write(Helper.newLogMaker(1231, str, getServicePackageName(), i, z).addTaggedData(1145, Integer.valueOf(j > 2147483647L ? Integer.MAX_VALUE : (int) j)));
        }
    }

    void disableAutofillForActivity(ComponentName componentName, long j, int i, boolean z) {
        int i2;
        synchronized (this.mLock) {
            if (this.mDisabledActivities == null) {
                this.mDisabledActivities = new ArrayMap<>(1);
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
            if (jElapsedRealtime < 0) {
                jElapsedRealtime = JobStatus.NO_LATEST_RUNTIME;
            }
            this.mDisabledActivities.put(componentName, Long.valueOf(jElapsedRealtime));
            if (j > 2147483647L) {
                i2 = Integer.MAX_VALUE;
            } else {
                i2 = (int) j;
            }
            LogMaker logMakerAddTaggedData = new LogMaker(1232).setComponentName(componentName).addTaggedData(908, getServicePackageName()).addTaggedData(1145, Integer.valueOf(i2)).addTaggedData(1456, Integer.valueOf(i));
            if (z) {
                logMakerAddTaggedData.addTaggedData(1414, 1);
            }
            this.mMetricsLogger.write(logMakerAddTaggedData);
        }
    }

    @GuardedBy("mLock")
    private boolean isAutofillDisabledLocked(ComponentName componentName) {
        long jElapsedRealtime;
        Long l;
        if (this.mDisabledActivities != null) {
            jElapsedRealtime = SystemClock.elapsedRealtime();
            Long l2 = this.mDisabledActivities.get(componentName);
            if (l2 != null) {
                if (l2.longValue() >= jElapsedRealtime) {
                    return true;
                }
                if (Helper.sVerbose) {
                    Slog.v(TAG, "Removing " + componentName.toShortString() + " from disabled list");
                }
                this.mDisabledActivities.remove(componentName);
            }
        } else {
            jElapsedRealtime = 0;
        }
        String packageName = componentName.getPackageName();
        if (this.mDisabledApps == null || (l = this.mDisabledApps.get(packageName)) == null) {
            return false;
        }
        if (jElapsedRealtime == 0) {
            jElapsedRealtime = SystemClock.elapsedRealtime();
        }
        if (l.longValue() >= jElapsedRealtime) {
            return true;
        }
        if (Helper.sVerbose) {
            Slog.v(TAG, "Removing " + packageName + " from disabled list");
        }
        this.mDisabledApps.remove(packageName);
        return false;
    }

    boolean isFieldClassificationEnabled(int i) {
        synchronized (this.mLock) {
            if (!isCalledByServiceLocked("isFieldClassificationEnabled", i)) {
                return false;
            }
            return isFieldClassificationEnabledLocked();
        }
    }

    boolean isFieldClassificationEnabledLocked() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "autofill_field_classification", 1, this.mUserId) == 1;
    }

    FieldClassificationStrategy getFieldClassificationStrategy() {
        return this.mFieldClassificationStrategy;
    }

    String[] getAvailableFieldClassificationAlgorithms(int i) {
        synchronized (this.mLock) {
            if (!isCalledByServiceLocked("getFCAlgorithms()", i)) {
                return null;
            }
            return this.mFieldClassificationStrategy.getAvailableAlgorithms();
        }
    }

    String getDefaultFieldClassificationAlgorithm(int i) {
        synchronized (this.mLock) {
            if (!isCalledByServiceLocked("getDefaultFCAlgorithm()", i)) {
                return null;
            }
            return this.mFieldClassificationStrategy.getDefaultAlgorithm();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AutofillManagerServiceImpl: [userId=");
        sb.append(this.mUserId);
        sb.append(", component=");
        sb.append(this.mInfo != null ? this.mInfo.getServiceInfo().getComponentName() : null);
        sb.append("]");
        return sb.toString();
    }

    private class PruneTask extends AsyncTask<Void, Void, Void> {
        private PruneTask() {
        }

        @Override
        protected Void doInBackground(Void... voidArr) {
            int size;
            SparseArray sparseArray;
            int i;
            synchronized (AutofillManagerServiceImpl.this.mLock) {
                size = AutofillManagerServiceImpl.this.mSessions.size();
                sparseArray = new SparseArray(size);
                for (int i2 = 0; i2 < size; i2++) {
                    Session session = (Session) AutofillManagerServiceImpl.this.mSessions.valueAt(i2);
                    sparseArray.put(session.id, session.getActivityTokenLocked());
                }
            }
            IActivityManager service = ActivityManager.getService();
            int i3 = size;
            int i4 = 0;
            while (i4 < i3) {
                try {
                    if (service.getActivityClassForToken((IBinder) sparseArray.valueAt(i4)) != null) {
                        sparseArray.removeAt(i4);
                        i4--;
                        i3--;
                    }
                } catch (RemoteException e) {
                    Slog.w(AutofillManagerServiceImpl.TAG, "Cannot figure out if activity is finished", e);
                }
                i4++;
            }
            synchronized (AutofillManagerServiceImpl.this.mLock) {
                for (i = 0; i < i3; i++) {
                    try {
                        Session session2 = (Session) AutofillManagerServiceImpl.this.mSessions.get(sparseArray.keyAt(i));
                        if (session2 != null && sparseArray.valueAt(i) == session2.getActivityTokenLocked()) {
                            if (session2.isSavingLocked()) {
                                if (Helper.sVerbose) {
                                    Slog.v(AutofillManagerServiceImpl.TAG, "Session " + session2.id + " is saving");
                                }
                            } else {
                                if (Helper.sDebug) {
                                    Slog.i(AutofillManagerServiceImpl.TAG, "Prune session " + session2.id + " (" + session2.getActivityTokenLocked() + ")");
                                }
                                session2.removeSelfLocked();
                            }
                        }
                    } finally {
                    }
                }
            }
            return null;
        }
    }
}
