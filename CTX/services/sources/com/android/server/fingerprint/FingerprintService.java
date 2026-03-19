package com.android.server.fingerprint;

import android.R;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.PendingIntent;
import android.app.SynchronousUserSwitchObserver;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprintClientCallback;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintClientActiveCallback;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.fingerprint.IFingerprintServiceLockoutResetCallback;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.KeyStore;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.util.DumpUtils;
import com.android.server.SystemServerInitThreadPool;
import com.android.server.SystemService;
import com.android.server.am.AssistDataRequester;
import com.android.server.utils.PriorityDump;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FingerprintService extends SystemService implements IHwBinder.DeathRecipient {
    private static final String ACTION_LOCKOUT_RESET = "com.android.server.fingerprint.ACTION_LOCKOUT_RESET";
    private static final long CANCEL_TIMEOUT_LIMIT = 3000;
    private static final boolean CLEANUP_UNUSED_FP = true;
    static final boolean DEBUG = true;
    private static final long FAIL_LOCKOUT_TIMEOUT_MS = 30000;
    private static final String FP_DATA_DIR = "fpdata";
    private static final String KEY_LOCKOUT_RESET_USER = "lockout_reset_user";
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_PERMANENT = 20;
    private static final int MAX_FAILED_ATTEMPTS_LOCKOUT_TIMED = 5;
    private static final int MSG_USER_SWITCHING = 10;
    static final String TAG = "FingerprintService";
    private final IActivityManager mActivityManager;
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private final Map<Integer, Long> mAuthenticatorIds;
    private final CopyOnWriteArrayList<IFingerprintClientActiveCallback> mClientActiveCallbacks;
    private Context mContext;
    private HashMap<Integer, PerformanceStats> mCryptoPerformanceMap;
    private ClientMonitor mCurrentClient;
    private int mCurrentUserId;

    @GuardedBy("this")
    private IBiometricsFingerprint mDaemon;
    private IBiometricsFingerprintClientCallback mDaemonCallback;
    private SparseIntArray mFailedAttempts;
    private final FingerprintUtils mFingerprintUtils;
    private long mHalDeviceId;
    private Handler mHandler;
    private final String mKeyguardPackage;
    private final ArrayList<FingerprintServiceLockoutResetMonitor> mLockoutMonitors;
    private final BroadcastReceiver mLockoutReceiver;
    private ClientMonitor mPendingClient;
    private HashMap<Integer, PerformanceStats> mPerformanceMap;
    private PerformanceStats mPerformanceStats;
    private final PowerManager mPowerManager;
    private final Runnable mResetClientState;
    private final Runnable mResetFailedAttemptsForCurrentUserRunnable;
    private IStatusBarService mStatusBarService;
    private final TaskStackListener mTaskStackListener;
    private SparseBooleanArray mTimedLockoutCleared;
    private IBinder mToken;
    private ArrayList<UserFingerprint> mUnknownFingerprints;
    private final UserManager mUserManager;

    private class PerformanceStats {
        int accept;
        int acquire;
        int lockout;
        int permanentLockout;
        int reject;

        private PerformanceStats() {
        }
    }

    private class UserFingerprint {
        Fingerprint f;
        int userId;

        public UserFingerprint(Fingerprint fingerprint, int i) {
            this.f = fingerprint;
            this.userId = i;
        }
    }

    public FingerprintService(Context context) {
        super(context);
        this.mLockoutMonitors = new ArrayList<>();
        this.mClientActiveCallbacks = new CopyOnWriteArrayList<>();
        this.mAuthenticatorIds = Collections.synchronizedMap(new HashMap());
        this.mCurrentUserId = -10000;
        this.mFingerprintUtils = FingerprintUtils.getInstance();
        this.mToken = new Binder();
        this.mUnknownFingerprints = new ArrayList<>();
        this.mPerformanceMap = new HashMap<>();
        this.mCryptoPerformanceMap = new HashMap<>();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 10) {
                    FingerprintService.this.handleUserSwitching(message.arg1);
                    return;
                }
                Slog.w(FingerprintService.TAG, "Unknown message:" + message.what);
            }
        };
        this.mLockoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (FingerprintService.ACTION_LOCKOUT_RESET.equals(intent.getAction())) {
                    FingerprintService.this.resetFailedAttemptsForUser(false, intent.getIntExtra(FingerprintService.KEY_LOCKOUT_RESET_USER, 0));
                }
            }
        };
        this.mResetFailedAttemptsForCurrentUserRunnable = new Runnable() {
            @Override
            public void run() {
                FingerprintService.this.resetFailedAttemptsForUser(true, ActivityManager.getCurrentUser());
            }
        };
        this.mResetClientState = new Runnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("Client ");
                sb.append(FingerprintService.this.mCurrentClient != null ? FingerprintService.this.mCurrentClient.getOwnerString() : "null");
                sb.append(" failed to respond to cancel, starting client ");
                sb.append(FingerprintService.this.mPendingClient != null ? FingerprintService.this.mPendingClient.getOwnerString() : "null");
                Slog.w(FingerprintService.TAG, sb.toString());
                FingerprintService.this.mCurrentClient = null;
                FingerprintService.this.startClient(FingerprintService.this.mPendingClient, false);
            }
        };
        this.mTaskStackListener = new TaskStackListener() {
            public void onTaskStackChanged() {
                try {
                    if (FingerprintService.this.mCurrentClient instanceof AuthenticationClient) {
                        String ownerString = FingerprintService.this.mCurrentClient.getOwnerString();
                        if (!FingerprintService.this.isKeyguard(ownerString)) {
                            List tasks = FingerprintService.this.mActivityManager.getTasks(1);
                            if (!tasks.isEmpty()) {
                                String packageName = ((ActivityManager.RunningTaskInfo) tasks.get(0)).topActivity.getPackageName();
                                if (!packageName.contentEquals(ownerString)) {
                                    Slog.e(FingerprintService.TAG, "Stopping background authentication, top: " + packageName + " currentClient: " + ownerString);
                                    FingerprintService.this.mCurrentClient.stop(false);
                                }
                            }
                        }
                    }
                } catch (RemoteException e) {
                    Slog.e(FingerprintService.TAG, "Unable to get running tasks", e);
                }
            }
        };
        this.mDaemonCallback = new IBiometricsFingerprintClientCallback.Stub() {
            @Override
            public void onEnrollResult(final long j, final int i, final int i2, final int i3) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleEnrollResult(j, i, i2, i3);
                    }
                });
            }

            @Override
            public void onAcquired(final long j, final int i, final int i2) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleAcquired(j, i, i2);
                    }
                });
            }

            @Override
            public void onAuthenticated(final long j, final int i, final int i2, final ArrayList<Byte> arrayList) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleAuthenticated(j, i, i2, arrayList);
                    }
                });
            }

            @Override
            public void onError(final long j, final int i, final int i2) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleError(j, i, i2);
                    }
                });
            }

            @Override
            public void onRemoved(final long j, final int i, final int i2, final int i3) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleRemoved(j, i, i2, i3);
                    }
                });
            }

            @Override
            public void onEnumerate(final long j, final int i, final int i2, final int i3) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleEnumerate(j, i, i2, i3);
                    }
                });
            }
        };
        this.mContext = context;
        this.mKeyguardPackage = ComponentName.unflattenFromString(context.getResources().getString(R.string.alternative_fp_setup_notification_content)).getPackageName();
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        this.mContext.registerReceiver(this.mLockoutReceiver, new IntentFilter(ACTION_LOCKOUT_RESET), "android.permission.RESET_FINGERPRINT_LOCKOUT", null);
        this.mUserManager = UserManager.get(this.mContext);
        this.mTimedLockoutCleared = new SparseBooleanArray();
        this.mFailedAttempts = new SparseIntArray();
        this.mStatusBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        this.mActivityManager = ActivityManager.getService();
    }

    @Override
    public void serviceDied(long j) {
        Slog.v(TAG, "fingerprint HAL died");
        MetricsLogger.count(this.mContext, "fingerprintd_died", 1);
        handleError(this.mHalDeviceId, 1, 0);
    }

    public synchronized IBiometricsFingerprint getFingerprintDaemon() {
        if (this.mDaemon == null) {
            Slog.v(TAG, "mDaemon was null, reconnect to fingerprint");
            try {
                this.mDaemon = IBiometricsFingerprint.getService();
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to get biometric interface", e);
            } catch (NoSuchElementException e2) {
            }
            if (this.mDaemon == null) {
                Slog.w(TAG, "fingerprint HIDL not available");
                return null;
            }
            this.mDaemon.asBinder().linkToDeath(this, 0L);
            try {
                this.mHalDeviceId = this.mDaemon.setNotify(this.mDaemonCallback);
            } catch (RemoteException e3) {
                Slog.e(TAG, "Failed to open fingerprint HAL", e3);
                this.mDaemon = null;
            }
            Slog.v(TAG, "Fingerprint HAL id: " + this.mHalDeviceId);
            if (this.mHalDeviceId != 0) {
                loadAuthenticatorIds();
                updateActiveGroup(ActivityManager.getCurrentUser(), null);
                doFingerprintCleanupForUser(ActivityManager.getCurrentUser());
            } else {
                Slog.w(TAG, "Failed to open Fingerprint HAL!");
                MetricsLogger.count(this.mContext, "fingerprintd_openhal_error", 1);
                this.mDaemon = null;
            }
        }
        return this.mDaemon;
    }

    private void loadAuthenticatorIds() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        this.mAuthenticatorIds.clear();
        Iterator it = UserManager.get(this.mContext).getUsers(true).iterator();
        while (it.hasNext()) {
            int userOrWorkProfileId = getUserOrWorkProfileId(null, ((UserInfo) it.next()).id);
            if (!this.mAuthenticatorIds.containsKey(Integer.valueOf(userOrWorkProfileId))) {
                updateActiveGroup(userOrWorkProfileId, null);
            }
        }
        long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
        if (jCurrentTimeMillis2 > 1000) {
            Slog.w(TAG, "loadAuthenticatorIds() taking too long: " + jCurrentTimeMillis2 + "ms");
        }
    }

    private void doFingerprintCleanupForUser(int i) {
        enumerateUser(i);
    }

    private void clearEnumerateState() {
        Slog.v(TAG, "clearEnumerateState()");
        this.mUnknownFingerprints.clear();
    }

    private void enumerateUser(int i) {
        Slog.v(TAG, "Enumerating user(" + i + ")");
        startEnumerate(this.mToken, i, null, hasPermission("android.permission.MANAGE_FINGERPRINT") ^ true, true);
    }

    private void cleanupUnknownFingerprints() {
        if (!this.mUnknownFingerprints.isEmpty()) {
            UserFingerprint userFingerprint = this.mUnknownFingerprints.get(0);
            this.mUnknownFingerprints.remove(userFingerprint);
            startRemove(this.mToken, userFingerprint.f.getFingerId(), userFingerprint.f.getGroupId(), userFingerprint.userId, null, !hasPermission("android.permission.MANAGE_FINGERPRINT"), true);
            return;
        }
        clearEnumerateState();
    }

    protected void handleEnumerate(long j, int i, int i2, int i3) {
        ClientMonitor clientMonitor = this.mCurrentClient;
        if (!(clientMonitor instanceof InternalRemovalClient) && !(clientMonitor instanceof EnumerateClient)) {
            return;
        }
        clientMonitor.onEnumerationResult(i, i2, i3);
        if (i3 == 0) {
            if (clientMonitor instanceof InternalEnumerateClient) {
                List<Fingerprint> unknownFingerprints = ((InternalEnumerateClient) clientMonitor).getUnknownFingerprints();
                if (!unknownFingerprints.isEmpty()) {
                    Slog.w(TAG, "Adding " + unknownFingerprints.size() + " fingerprints for deletion");
                }
                Iterator<Fingerprint> it = unknownFingerprints.iterator();
                while (it.hasNext()) {
                    this.mUnknownFingerprints.add(new UserFingerprint(it.next(), clientMonitor.getTargetUserId()));
                }
                removeClient(clientMonitor);
                cleanupUnknownFingerprints();
                return;
            }
            removeClient(clientMonitor);
        }
    }

    protected void handleError(long j, int i, int i2) {
        ClientMonitor clientMonitor = this.mCurrentClient;
        if ((clientMonitor instanceof InternalRemovalClient) || (clientMonitor instanceof InternalEnumerateClient)) {
            clearEnumerateState();
        }
        if (clientMonitor != null && clientMonitor.onError(i, i2)) {
            removeClient(clientMonitor);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("handleError(client=");
        sb.append(clientMonitor != null ? clientMonitor.getOwnerString() : "null");
        sb.append(", error = ");
        sb.append(i);
        sb.append(")");
        Slog.v(TAG, sb.toString());
        if (i == 5) {
            this.mHandler.removeCallbacks(this.mResetClientState);
            if (this.mPendingClient != null) {
                Slog.v(TAG, "start pending client " + this.mPendingClient.getOwnerString());
                startClient(this.mPendingClient, false);
                this.mPendingClient = null;
                return;
            }
            return;
        }
        if (i == 1) {
            Slog.w(TAG, "Got ERROR_HW_UNAVAILABLE; try reconnecting next client.");
            synchronized (this) {
                this.mDaemon = null;
                this.mHalDeviceId = 0L;
                this.mCurrentUserId = -10000;
            }
        }
    }

    protected void handleRemoved(long j, int i, int i2, int i3) {
        Slog.w(TAG, "Removed: fid=" + i + ", gid=" + i2 + ", dev=" + j + ", rem=" + i3);
        ClientMonitor clientMonitor = this.mCurrentClient;
        if (clientMonitor != null && clientMonitor.onRemoved(i, i2, i3)) {
            removeClient(clientMonitor);
            if (!hasEnrolledFingerprints(i2)) {
                updateActiveGroup(i2, null);
            }
        }
        boolean z = clientMonitor instanceof InternalRemovalClient;
        if (z && !this.mUnknownFingerprints.isEmpty()) {
            cleanupUnknownFingerprints();
        } else if (z) {
            clearEnumerateState();
        }
    }

    protected void handleAuthenticated(long j, int i, int i2, ArrayList<Byte> arrayList) {
        ClientMonitor clientMonitor = this.mCurrentClient;
        if (i != 0) {
            byte[] bArr = new byte[arrayList.size()];
            for (int i3 = 0; i3 < arrayList.size(); i3++) {
                bArr[i3] = arrayList.get(i3).byteValue();
            }
            KeyStore.getInstance().addAuthToken(bArr);
        }
        if (clientMonitor != null && clientMonitor.onAuthenticated(i, i2)) {
            removeClient(clientMonitor);
        }
        if (i != 0) {
            this.mPerformanceStats.accept++;
        } else {
            this.mPerformanceStats.reject++;
        }
    }

    protected void handleAcquired(long j, int i, int i2) {
        ClientMonitor clientMonitor = this.mCurrentClient;
        if (clientMonitor != null && clientMonitor.onAcquired(i, i2)) {
            removeClient(clientMonitor);
        }
        if (this.mPerformanceStats != null && getLockoutMode() == 0 && (clientMonitor instanceof AuthenticationClient)) {
            this.mPerformanceStats.acquire++;
        }
    }

    protected void handleEnrollResult(long j, int i, int i2, int i3) {
        ClientMonitor clientMonitor = this.mCurrentClient;
        if (clientMonitor != null && clientMonitor.onEnrollResult(i, i2, i3)) {
            removeClient(clientMonitor);
            updateActiveGroup(i2, null);
        }
    }

    private void userActivity() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 2, 0);
    }

    void handleUserSwitching(int i) {
        if ((this.mCurrentClient instanceof InternalRemovalClient) || (this.mCurrentClient instanceof InternalEnumerateClient)) {
            Slog.w(TAG, "User switched while performing cleanup");
            removeClient(this.mCurrentClient);
            clearEnumerateState();
        }
        updateActiveGroup(i, null);
        doFingerprintCleanupForUser(i);
    }

    private void removeClient(ClientMonitor clientMonitor) {
        if (clientMonitor != null) {
            clientMonitor.destroy();
            if (clientMonitor != this.mCurrentClient && this.mCurrentClient != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unexpected client: ");
                sb.append(clientMonitor.getOwnerString());
                sb.append("expected: ");
                sb.append(this.mCurrentClient);
                Slog.w(TAG, sb.toString() != null ? this.mCurrentClient.getOwnerString() : "null");
            }
        }
        if (this.mCurrentClient != null) {
            Slog.v(TAG, "Done with client: " + clientMonitor.getOwnerString());
            this.mCurrentClient = null;
        }
        if (this.mPendingClient == null) {
            notifyClientActiveCallbacks(false);
        }
    }

    private int getLockoutMode() {
        int currentUser = ActivityManager.getCurrentUser();
        int i = this.mFailedAttempts.get(currentUser, 0);
        if (i >= 20) {
            return 2;
        }
        return (i <= 0 || this.mTimedLockoutCleared.get(currentUser, false) || i % 5 != 0) ? 0 : 1;
    }

    private void scheduleLockoutResetForUser(int i) {
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + 30000, getLockoutResetIntentForUser(i));
    }

    private void cancelLockoutResetForUser(int i) {
        this.mAlarmManager.cancel(getLockoutResetIntentForUser(i));
    }

    private PendingIntent getLockoutResetIntentForUser(int i) {
        return PendingIntent.getBroadcast(this.mContext, i, new Intent(ACTION_LOCKOUT_RESET).putExtra(KEY_LOCKOUT_RESET_USER, i), 134217728);
    }

    public long startPreEnroll(IBinder iBinder) {
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon == null) {
            Slog.w(TAG, "startPreEnroll: no fingerprint HAL!");
            return 0L;
        }
        try {
            return fingerprintDaemon.preEnroll();
        } catch (RemoteException e) {
            Slog.e(TAG, "startPreEnroll failed", e);
            return 0L;
        }
    }

    public int startPostEnroll(IBinder iBinder) {
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon == null) {
            Slog.w(TAG, "startPostEnroll: no fingerprint HAL!");
            return 0;
        }
        try {
            return fingerprintDaemon.postEnroll();
        } catch (RemoteException e) {
            Slog.e(TAG, "startPostEnroll failed", e);
            return 0;
        }
    }

    private void startClient(ClientMonitor clientMonitor, boolean z) {
        ClientMonitor clientMonitor2 = this.mCurrentClient;
        if (clientMonitor2 != null) {
            Slog.v(TAG, "request stop current client " + clientMonitor2.getOwnerString());
            if ((clientMonitor2 instanceof InternalEnumerateClient) || (clientMonitor2 instanceof InternalRemovalClient)) {
                if (clientMonitor != null) {
                    Slog.w(TAG, "Internal cleanup in progress but trying to start client " + clientMonitor.getClass().getSuperclass().getSimpleName() + "(" + clientMonitor.getOwnerString() + "), initiatedByClient = " + z);
                }
            } else {
                clientMonitor2.stop(z);
            }
            this.mPendingClient = clientMonitor;
            this.mHandler.removeCallbacks(this.mResetClientState);
            this.mHandler.postDelayed(this.mResetClientState, CANCEL_TIMEOUT_LIMIT);
            return;
        }
        if (clientMonitor != null) {
            this.mCurrentClient = clientMonitor;
            Slog.v(TAG, "starting client " + clientMonitor.getClass().getSuperclass().getSimpleName() + "(" + clientMonitor.getOwnerString() + "), initiatedByClient = " + z);
            notifyClientActiveCallbacks(true);
            clientMonitor.start();
        }
    }

    void startRemove(IBinder iBinder, int i, int i2, int i3, IFingerprintServiceReceiver iFingerprintServiceReceiver, boolean z, boolean z2) {
        if (iBinder == null) {
            Slog.w(TAG, "startRemove: token is null");
            return;
        }
        if (iFingerprintServiceReceiver == null) {
            Slog.w(TAG, "startRemove: receiver is null");
            return;
        }
        if (getFingerprintDaemon() == null) {
            Slog.w(TAG, "startRemove: no fingerprint HAL!");
        } else if (z2) {
            Context context = getContext();
            startClient(new InternalRemovalClient(context, this.mHalDeviceId, iBinder, iFingerprintServiceReceiver, i, i2, i3, z, context.getOpPackageName()) {
                @Override
                public void notifyUserActivity() {
                }

                @Override
                public IBiometricsFingerprint getFingerprintDaemon() {
                    return FingerprintService.this.getFingerprintDaemon();
                }
            }, true);
        } else {
            startClient(new RemovalClient(getContext(), this.mHalDeviceId, iBinder, iFingerprintServiceReceiver, i, i2, i3, z, iBinder.toString()) {
                @Override
                public void notifyUserActivity() {
                    FingerprintService.this.userActivity();
                }

                @Override
                public IBiometricsFingerprint getFingerprintDaemon() {
                    return FingerprintService.this.getFingerprintDaemon();
                }
            }, true);
        }
    }

    void startEnumerate(IBinder iBinder, int i, IFingerprintServiceReceiver iFingerprintServiceReceiver, boolean z, boolean z2) {
        if (getFingerprintDaemon() == null) {
            Slog.w(TAG, "startEnumerate: no fingerprint HAL!");
        } else {
            if (z2) {
                List<Fingerprint> enrolledFingerprints = getEnrolledFingerprints(i);
                Context context = getContext();
                startClient(new InternalEnumerateClient(context, this.mHalDeviceId, iBinder, iFingerprintServiceReceiver, i, i, z, context.getOpPackageName(), enrolledFingerprints) {
                    @Override
                    public void notifyUserActivity() {
                    }

                    @Override
                    public IBiometricsFingerprint getFingerprintDaemon() {
                        return FingerprintService.this.getFingerprintDaemon();
                    }
                }, true);
                return;
            }
            startClient(new EnumerateClient(getContext(), this.mHalDeviceId, iBinder, iFingerprintServiceReceiver, i, i, z, iBinder.toString()) {
                @Override
                public void notifyUserActivity() {
                    FingerprintService.this.userActivity();
                }

                @Override
                public IBiometricsFingerprint getFingerprintDaemon() {
                    return FingerprintService.this.getFingerprintDaemon();
                }
            }, true);
        }
    }

    public List<Fingerprint> getEnrolledFingerprints(int i) {
        return this.mFingerprintUtils.getFingerprintsForUser(this.mContext, i);
    }

    public boolean hasEnrolledFingerprints(int i) {
        if (i != UserHandle.getCallingUserId()) {
            checkPermission("android.permission.INTERACT_ACROSS_USERS");
        }
        return this.mFingerprintUtils.getFingerprintsForUser(this.mContext, i).size() > 0;
    }

    boolean hasPermission(String str) {
        return getContext().checkCallingOrSelfPermission(str) == 0;
    }

    void checkPermission(String str) {
        getContext().enforceCallingOrSelfPermission(str, "Must have " + str + " permission.");
    }

    int getEffectiveUserId(int i) {
        UserManager userManager = UserManager.get(this.mContext);
        if (userManager != null) {
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            int credentialOwnerProfile = userManager.getCredentialOwnerProfile(i);
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return credentialOwnerProfile;
        }
        Slog.e(TAG, "Unable to acquire UserManager");
        return i;
    }

    boolean isCurrentUserOrProfile(int i) {
        UserManager userManager = UserManager.get(this.mContext);
        if (userManager == null) {
            Slog.e(TAG, "Unable to acquire UserManager");
            return false;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            for (int i2 : userManager.getEnabledProfileIds(ActivityManager.getCurrentUser())) {
                if (i2 == i) {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                    return true;
                }
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private boolean isForegroundActivity(int i, int i2) {
        try {
            List runningAppProcesses = ActivityManager.getService().getRunningAppProcesses();
            int size = runningAppProcesses.size();
            for (int i3 = 0; i3 < size; i3++) {
                ActivityManager.RunningAppProcessInfo runningAppProcessInfo = (ActivityManager.RunningAppProcessInfo) runningAppProcesses.get(i3);
                if (runningAppProcessInfo.pid == i2 && runningAppProcessInfo.uid == i && runningAppProcessInfo.importance <= 125) {
                    return true;
                }
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed");
        }
        return false;
    }

    private boolean canUseFingerprint(String str, boolean z, int i, int i2, int i3) {
        if (getContext().checkCallingPermission("android.permission.USE_FINGERPRINT") != 0) {
            checkPermission("android.permission.USE_BIOMETRIC");
        }
        if (isKeyguard(str)) {
            return true;
        }
        if (!isCurrentUserOrProfile(i3)) {
            Slog.w(TAG, "Rejecting " + str + " ; not a current user or profile");
            return false;
        }
        if (this.mAppOps.noteOp(55, i, str) != 0) {
            Slog.w(TAG, "Rejecting " + str + " ; permission denied");
            return false;
        }
        if (!z || isForegroundActivity(i, i2) || currentClient(str)) {
            return true;
        }
        Slog.w(TAG, "Rejecting " + str + " ; not in foreground");
        return false;
    }

    private boolean currentClient(String str) {
        return this.mCurrentClient != null && this.mCurrentClient.getOwnerString().equals(str);
    }

    private boolean isKeyguard(String str) {
        return this.mKeyguardPackage.equals(str);
    }

    private void addLockoutResetMonitor(FingerprintServiceLockoutResetMonitor fingerprintServiceLockoutResetMonitor) {
        if (!this.mLockoutMonitors.contains(fingerprintServiceLockoutResetMonitor)) {
            this.mLockoutMonitors.add(fingerprintServiceLockoutResetMonitor);
        }
    }

    private void removeLockoutResetCallback(FingerprintServiceLockoutResetMonitor fingerprintServiceLockoutResetMonitor) {
        this.mLockoutMonitors.remove(fingerprintServiceLockoutResetMonitor);
    }

    private void notifyLockoutResetMonitors() {
        for (int i = 0; i < this.mLockoutMonitors.size(); i++) {
            this.mLockoutMonitors.get(i).sendLockoutReset();
        }
    }

    private void notifyClientActiveCallbacks(boolean z) {
        CopyOnWriteArrayList<IFingerprintClientActiveCallback> copyOnWriteArrayList = this.mClientActiveCallbacks;
        for (int i = 0; i < copyOnWriteArrayList.size(); i++) {
            try {
                copyOnWriteArrayList.get(i).onClientActiveChanged(z);
            } catch (RemoteException e) {
                this.mClientActiveCallbacks.remove(copyOnWriteArrayList.get(i));
            }
        }
    }

    private void startAuthentication(IBinder iBinder, long j, int i, int i2, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i3, boolean z, String str, Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) {
        int i4;
        updateActiveGroup(i2, str);
        Slog.v(TAG, "startAuthentication(" + str + ")");
        AuthenticationClient authenticationClient = new AuthenticationClient(getContext(), this.mHalDeviceId, iBinder, iFingerprintServiceReceiver, this.mCurrentUserId, i2, j, z, str, bundle, iBiometricPromptReceiver, this.mStatusBarService) {
            @Override
            public void onStart() {
                try {
                    FingerprintService.this.mActivityManager.registerTaskStackListener(FingerprintService.this.mTaskStackListener);
                } catch (RemoteException e) {
                    Slog.e(FingerprintService.TAG, "Could not register task stack listener", e);
                }
            }

            @Override
            public void onStop() {
                try {
                    FingerprintService.this.mActivityManager.unregisterTaskStackListener(FingerprintService.this.mTaskStackListener);
                } catch (RemoteException e) {
                    Slog.e(FingerprintService.TAG, "Could not unregister task stack listener", e);
                }
            }

            @Override
            public int handleFailedAttempt() {
                int currentUser = ActivityManager.getCurrentUser();
                FingerprintService.this.mFailedAttempts.put(currentUser, FingerprintService.this.mFailedAttempts.get(currentUser, 0) + 1);
                FingerprintService.this.mTimedLockoutCleared.put(ActivityManager.getCurrentUser(), false);
                int lockoutMode = FingerprintService.this.getLockoutMode();
                if (lockoutMode == 2) {
                    FingerprintService.this.mPerformanceStats.permanentLockout++;
                } else if (lockoutMode == 1) {
                    FingerprintService.this.mPerformanceStats.lockout++;
                }
                if (lockoutMode == 0) {
                    return 0;
                }
                FingerprintService.this.scheduleLockoutResetForUser(currentUser);
                return lockoutMode;
            }

            @Override
            public void resetFailedAttempts() {
                FingerprintService.this.resetFailedAttemptsForUser(true, ActivityManager.getCurrentUser());
            }

            @Override
            public void notifyUserActivity() {
                FingerprintService.this.userActivity();
            }

            @Override
            public IBiometricsFingerprint getFingerprintDaemon() {
                return FingerprintService.this.getFingerprintDaemon();
            }
        };
        int lockoutMode = getLockoutMode();
        if (lockoutMode != 0) {
            Slog.v(TAG, "In lockout mode(" + lockoutMode + ") ; disallowing authentication");
            if (lockoutMode == 1) {
                i4 = 7;
            } else {
                i4 = 9;
            }
            if (!authenticationClient.onError(i4, 0)) {
                Slog.w(TAG, "Cannot send permanent lockout message to client");
                return;
            }
            return;
        }
        startClient(authenticationClient, true);
    }

    private void startEnrollment(IBinder iBinder, byte[] bArr, int i, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i2, boolean z, String str) {
        updateActiveGroup(i, str);
        startClient(new EnrollClient(getContext(), this.mHalDeviceId, iBinder, iFingerprintServiceReceiver, i, i, bArr, z, str) {
            @Override
            public IBiometricsFingerprint getFingerprintDaemon() {
                return FingerprintService.this.getFingerprintDaemon();
            }

            @Override
            public void notifyUserActivity() {
                FingerprintService.this.userActivity();
            }
        }, true);
    }

    protected void resetFailedAttemptsForUser(boolean z, int i) {
        if (getLockoutMode() != 0) {
            Slog.v(TAG, "Reset fingerprint lockout, clearAttemptCounter=" + z);
        }
        if (z) {
            this.mFailedAttempts.put(i, 0);
        }
        this.mTimedLockoutCleared.put(i, true);
        cancelLockoutResetForUser(i);
        notifyLockoutResetMonitors();
    }

    private class FingerprintServiceLockoutResetMonitor implements IBinder.DeathRecipient {
        private static final long WAKELOCK_TIMEOUT_MS = 2000;
        private final IFingerprintServiceLockoutResetCallback mCallback;
        private final Runnable mRemoveCallbackRunnable = new Runnable() {
            @Override
            public void run() {
                FingerprintServiceLockoutResetMonitor.this.releaseWakelock();
                FingerprintService.this.removeLockoutResetCallback(FingerprintServiceLockoutResetMonitor.this);
            }
        };
        private final PowerManager.WakeLock mWakeLock;

        public FingerprintServiceLockoutResetMonitor(IFingerprintServiceLockoutResetCallback iFingerprintServiceLockoutResetCallback) {
            this.mCallback = iFingerprintServiceLockoutResetCallback;
            this.mWakeLock = FingerprintService.this.mPowerManager.newWakeLock(1, "lockout reset callback");
            try {
                this.mCallback.asBinder().linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.w(FingerprintService.TAG, "caught remote exception in linkToDeath", e);
            }
        }

        public void sendLockoutReset() {
            if (this.mCallback != null) {
                try {
                    this.mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
                    this.mCallback.onLockoutReset(FingerprintService.this.mHalDeviceId, new IRemoteCallback.Stub() {
                        public void sendResult(Bundle bundle) throws RemoteException {
                            FingerprintServiceLockoutResetMonitor.this.releaseWakelock();
                        }
                    });
                } catch (DeadObjectException e) {
                    Slog.w(FingerprintService.TAG, "Death object while invoking onLockoutReset: ", e);
                    FingerprintService.this.mHandler.post(this.mRemoveCallbackRunnable);
                } catch (RemoteException e2) {
                    Slog.w(FingerprintService.TAG, "Failed to invoke onLockoutReset: ", e2);
                    releaseWakelock();
                }
            }
        }

        @Override
        public void binderDied() {
            Slog.e(FingerprintService.TAG, "Lockout reset callback binder died");
            FingerprintService.this.mHandler.post(this.mRemoveCallbackRunnable);
        }

        private void releaseWakelock() {
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    private final class FingerprintServiceWrapper extends IFingerprintService.Stub {
        private FingerprintServiceWrapper() {
        }

        public long preEnroll(IBinder iBinder) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            return FingerprintService.this.startPreEnroll(iBinder);
        }

        public int postEnroll(IBinder iBinder) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            return FingerprintService.this.startPostEnroll(iBinder);
        }

        public void enroll(final IBinder iBinder, final byte[] bArr, final int i, final IFingerprintServiceReceiver iFingerprintServiceReceiver, final int i2, final String str) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            if (FingerprintService.this.getEnrolledFingerprints(i).size() >= FingerprintService.this.mContext.getResources().getInteger(R.integer.config_cdma_3waycall_flash_delay)) {
                Slog.w(FingerprintService.TAG, "Too many fingerprints registered");
            } else {
                if (!FingerprintService.this.isCurrentUserOrProfile(i)) {
                    return;
                }
                final boolean zIsRestricted = isRestricted();
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.startEnrollment(iBinder, bArr, i, iFingerprintServiceReceiver, i2, zIsRestricted, str);
                    }
                });
            }
        }

        private boolean isRestricted() {
            return !FingerprintService.this.hasPermission("android.permission.MANAGE_FINGERPRINT");
        }

        public void cancelEnrollment(final IBinder iBinder) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ClientMonitor clientMonitor = FingerprintService.this.mCurrentClient;
                    if ((clientMonitor instanceof EnrollClient) && clientMonitor.getToken() == iBinder) {
                        clientMonitor.stop(clientMonitor.getToken() == iBinder);
                    }
                }
            });
        }

        public void authenticate(final IBinder iBinder, final long j, final int i, final IFingerprintServiceReceiver iFingerprintServiceReceiver, final int i2, final String str, final Bundle bundle, final IBiometricPromptReceiver iBiometricPromptReceiver) {
            int callingUid = Binder.getCallingUid();
            int callingPid = Binder.getCallingPid();
            final int callingUserId = UserHandle.getCallingUserId();
            final boolean zIsRestricted = isRestricted();
            if (!FingerprintService.this.canUseFingerprint(str, true, callingUid, callingPid, callingUserId)) {
                Slog.v(FingerprintService.TAG, "authenticate(): reject " + str);
                return;
            }
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MetricsLogger.histogram(FingerprintService.this.mContext, "fingerprint_token", j != 0 ? 1 : 0);
                    HashMap map = j == 0 ? FingerprintService.this.mPerformanceMap : FingerprintService.this.mCryptoPerformanceMap;
                    PerformanceStats performanceStats = (PerformanceStats) map.get(Integer.valueOf(FingerprintService.this.mCurrentUserId));
                    if (performanceStats == null) {
                        performanceStats = new PerformanceStats();
                        map.put(Integer.valueOf(FingerprintService.this.mCurrentUserId), performanceStats);
                    }
                    FingerprintService.this.mPerformanceStats = performanceStats;
                    FingerprintService.this.startAuthentication(iBinder, j, callingUserId, i, iFingerprintServiceReceiver, i2, zIsRestricted, str, bundle, iBiometricPromptReceiver);
                }
            });
        }

        public void cancelAuthentication(final IBinder iBinder, String str) {
            if (FingerprintService.this.canUseFingerprint(str, true, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ClientMonitor clientMonitor = FingerprintService.this.mCurrentClient;
                        if (clientMonitor instanceof AuthenticationClient) {
                            if (clientMonitor.getToken() == iBinder) {
                                Slog.v(FingerprintService.TAG, "stop client " + clientMonitor.getOwnerString());
                                clientMonitor.stop(clientMonitor.getToken() == iBinder);
                                return;
                            }
                            Slog.v(FingerprintService.TAG, "can't stop client " + clientMonitor.getOwnerString() + " since tokens don't match");
                            return;
                        }
                        if (clientMonitor != null) {
                            Slog.v(FingerprintService.TAG, "can't cancel non-authenticating client " + clientMonitor.getOwnerString());
                        }
                    }
                });
                return;
            }
            Slog.v(FingerprintService.TAG, "cancelAuthentication(): reject " + str);
        }

        public void setActiveUser(final int i) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.updateActiveGroup(i, null);
                }
            });
        }

        public void remove(final IBinder iBinder, final int i, final int i2, final int i3, final IFingerprintServiceReceiver iFingerprintServiceReceiver) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            final boolean zIsRestricted = isRestricted();
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.startRemove(iBinder, i, i2, i3, iFingerprintServiceReceiver, zIsRestricted, false);
                }
            });
        }

        public void enumerate(final IBinder iBinder, final int i, final IFingerprintServiceReceiver iFingerprintServiceReceiver) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            final boolean zIsRestricted = isRestricted();
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.startEnumerate(iBinder, i, iFingerprintServiceReceiver, zIsRestricted, false);
                }
            });
        }

        public boolean isHardwareDetected(long j, String str) {
            boolean z = false;
            if (!FingerprintService.this.canUseFingerprint(str, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                return false;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                if (FingerprintService.this.getFingerprintDaemon() != null) {
                    if (FingerprintService.this.mHalDeviceId != 0) {
                        z = true;
                    }
                }
                return z;
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void rename(final int i, final int i2, final String str) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            if (FingerprintService.this.isCurrentUserOrProfile(i2)) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.mFingerprintUtils.renameFingerprintForUser(FingerprintService.this.mContext, i, i2, str);
                    }
                });
            }
        }

        public List<Fingerprint> getEnrolledFingerprints(int i, String str) {
            if (!FingerprintService.this.canUseFingerprint(str, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                return Collections.emptyList();
            }
            return FingerprintService.this.getEnrolledFingerprints(i);
        }

        public boolean hasEnrolledFingerprints(int i, String str) {
            if (!FingerprintService.this.canUseFingerprint(str, false, Binder.getCallingUid(), Binder.getCallingPid(), UserHandle.getCallingUserId())) {
                return false;
            }
            return FingerprintService.this.hasEnrolledFingerprints(i);
        }

        public long getAuthenticatorId(String str) {
            return FingerprintService.this.getAuthenticatorId(str);
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            if (DumpUtils.checkDumpPermission(FingerprintService.this.mContext, FingerprintService.TAG, printWriter)) {
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (strArr.length <= 0 || !PriorityDump.PROTO_ARG.equals(strArr[0])) {
                        FingerprintService.this.dumpInternal(printWriter);
                    } else {
                        FingerprintService.this.dumpProto(fileDescriptor);
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        }

        public void resetTimeout(byte[] bArr) {
            FingerprintService.this.checkPermission("android.permission.RESET_FINGERPRINT_LOCKOUT");
            FingerprintService.this.mHandler.post(FingerprintService.this.mResetFailedAttemptsForCurrentUserRunnable);
        }

        public void addLockoutResetCallback(final IFingerprintServiceLockoutResetCallback iFingerprintServiceLockoutResetCallback) throws RemoteException {
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.addLockoutResetMonitor(FingerprintService.this.new FingerprintServiceLockoutResetMonitor(iFingerprintServiceLockoutResetCallback));
                }
            });
        }

        public boolean isClientActive() {
            boolean z;
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            synchronized (FingerprintService.this) {
                z = (FingerprintService.this.mCurrentClient == null && FingerprintService.this.mPendingClient == null) ? false : true;
            }
            return z;
        }

        public void addClientActiveCallback(IFingerprintClientActiveCallback iFingerprintClientActiveCallback) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mClientActiveCallbacks.add(iFingerprintClientActiveCallback);
        }

        public void removeClientActiveCallback(IFingerprintClientActiveCallback iFingerprintClientActiveCallback) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mClientActiveCallbacks.remove(iFingerprintClientActiveCallback);
        }
    }

    private void dumpInternal(PrintWriter printWriter) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("service", "Fingerprint Manager");
            JSONArray jSONArray = new JSONArray();
            Iterator it = UserManager.get(getContext()).getUsers().iterator();
            while (it.hasNext()) {
                int identifier = ((UserInfo) it.next()).getUserHandle().getIdentifier();
                int size = this.mFingerprintUtils.getFingerprintsForUser(this.mContext, identifier).size();
                PerformanceStats performanceStats = this.mPerformanceMap.get(Integer.valueOf(identifier));
                PerformanceStats performanceStats2 = this.mCryptoPerformanceMap.get(Integer.valueOf(identifier));
                JSONObject jSONObject2 = new JSONObject();
                jSONObject2.put("id", identifier);
                jSONObject2.put(AssistDataRequester.KEY_RECEIVER_EXTRA_COUNT, size);
                int i = 0;
                jSONObject2.put("accept", performanceStats != null ? performanceStats.accept : 0);
                jSONObject2.put("reject", performanceStats != null ? performanceStats.reject : 0);
                jSONObject2.put("acquire", performanceStats != null ? performanceStats.acquire : 0);
                jSONObject2.put("lockout", performanceStats != null ? performanceStats.lockout : 0);
                jSONObject2.put("permanentLockout", performanceStats != null ? performanceStats.permanentLockout : 0);
                jSONObject2.put("acceptCrypto", performanceStats2 != null ? performanceStats2.accept : 0);
                jSONObject2.put("rejectCrypto", performanceStats2 != null ? performanceStats2.reject : 0);
                jSONObject2.put("acquireCrypto", performanceStats2 != null ? performanceStats2.acquire : 0);
                jSONObject2.put("lockoutCrypto", performanceStats2 != null ? performanceStats2.lockout : 0);
                if (performanceStats2 != null) {
                    i = performanceStats2.permanentLockout;
                }
                jSONObject2.put("permanentLockoutCrypto", i);
                jSONArray.put(jSONObject2);
            }
            jSONObject.put("prints", jSONArray);
        } catch (JSONException e) {
            Slog.e(TAG, "dump formatting failure", e);
        }
        printWriter.println(jSONObject);
    }

    private void dumpProto(FileDescriptor fileDescriptor) {
        ProtoOutputStream protoOutputStream = new ProtoOutputStream(fileDescriptor);
        Iterator it = UserManager.get(getContext()).getUsers().iterator();
        while (it.hasNext()) {
            int identifier = ((UserInfo) it.next()).getUserHandle().getIdentifier();
            long jStart = protoOutputStream.start(2246267895809L);
            protoOutputStream.write(1120986464257L, identifier);
            protoOutputStream.write(1120986464258L, this.mFingerprintUtils.getFingerprintsForUser(this.mContext, identifier).size());
            PerformanceStats performanceStats = this.mPerformanceMap.get(Integer.valueOf(identifier));
            if (performanceStats != null) {
                long jStart2 = protoOutputStream.start(1146756268035L);
                protoOutputStream.write(1120986464257L, performanceStats.accept);
                protoOutputStream.write(1120986464258L, performanceStats.reject);
                protoOutputStream.write(1120986464259L, performanceStats.acquire);
                protoOutputStream.write(1120986464260L, performanceStats.lockout);
                protoOutputStream.write(1120986464261L, performanceStats.permanentLockout);
                protoOutputStream.end(jStart2);
            }
            PerformanceStats performanceStats2 = this.mCryptoPerformanceMap.get(Integer.valueOf(identifier));
            if (performanceStats2 != null) {
                long jStart3 = protoOutputStream.start(1146756268036L);
                protoOutputStream.write(1120986464257L, performanceStats2.accept);
                protoOutputStream.write(1120986464258L, performanceStats2.reject);
                protoOutputStream.write(1120986464259L, performanceStats2.acquire);
                protoOutputStream.write(1120986464260L, performanceStats2.lockout);
                protoOutputStream.write(1120986464261L, performanceStats2.permanentLockout);
                protoOutputStream.end(jStart3);
            }
            protoOutputStream.end(jStart);
        }
        protoOutputStream.flush();
        this.mPerformanceMap.clear();
        this.mCryptoPerformanceMap.clear();
    }

    @Override
    public void onStart() {
        publishBinderService("fingerprint", new FingerprintServiceWrapper());
        SystemServerInitThreadPool.get().submit(new Runnable() {
            @Override
            public final void run() {
                this.f$0.getFingerprintDaemon();
            }
        }, "FingerprintService.onStart");
        listenForUserSwitches();
    }

    private void updateActiveGroup(int i, String str) {
        File dataVendorDeDirectory;
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon != null) {
            try {
                int userOrWorkProfileId = getUserOrWorkProfileId(str, i);
                if (userOrWorkProfileId != this.mCurrentUserId) {
                    int i2 = Build.VERSION.FIRST_SDK_INT;
                    if (i2 < 1) {
                        Slog.e(TAG, "First SDK version " + i2 + " is invalid; must be at least VERSION_CODES.BASE");
                    }
                    if (i2 <= 27) {
                        dataVendorDeDirectory = Environment.getUserSystemDirectory(userOrWorkProfileId);
                    } else {
                        dataVendorDeDirectory = Environment.getDataVendorDeDirectory(userOrWorkProfileId);
                    }
                    File file = new File(dataVendorDeDirectory, FP_DATA_DIR);
                    if (!file.exists()) {
                        if (!file.mkdir()) {
                            Slog.v(TAG, "Cannot make directory: " + file.getAbsolutePath());
                            return;
                        }
                        if (!SELinux.restorecon(file)) {
                            Slog.w(TAG, "Restorecons failed. Directory will have wrong label.");
                            return;
                        }
                    }
                    fingerprintDaemon.setActiveGroup(userOrWorkProfileId, file.getAbsolutePath());
                    this.mCurrentUserId = userOrWorkProfileId;
                }
                this.mAuthenticatorIds.put(Integer.valueOf(userOrWorkProfileId), Long.valueOf(hasEnrolledFingerprints(userOrWorkProfileId) ? fingerprintDaemon.getAuthenticatorId() : 0L));
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to setActiveGroup():", e);
            }
        }
    }

    private int getUserOrWorkProfileId(String str, int i) {
        if (!isKeyguard(str) && isWorkProfile(i)) {
            return i;
        }
        return getEffectiveUserId(i);
    }

    private boolean isWorkProfile(int i) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            UserInfo userInfo = this.mUserManager.getUserInfo(i);
            return userInfo != null && userInfo.isManagedProfile();
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void listenForUserSwitches() {
        try {
            ActivityManager.getService().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                public void onUserSwitching(int i) throws RemoteException {
                    FingerprintService.this.mHandler.obtainMessage(10, i, 0).sendToTarget();
                }
            }, TAG);
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to listen for user switching event", e);
        }
    }

    public long getAuthenticatorId(String str) {
        return this.mAuthenticatorIds.getOrDefault(Integer.valueOf(getUserOrWorkProfileId(str, UserHandle.getCallingUserId())), 0L).longValue();
    }
}
