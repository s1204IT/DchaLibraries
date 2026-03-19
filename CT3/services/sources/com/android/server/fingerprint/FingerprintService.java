package com.android.server.fingerprint;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.SynchronousUserSwitchObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintDaemon;
import android.hardware.fingerprint.IFingerprintDaemonCallback;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.fingerprint.IFingerprintServiceLockoutResetCallback;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.server.SystemService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FingerprintService extends SystemService implements IBinder.DeathRecipient {
    private static final String ACTION_LOCKOUT_RESET = "com.android.server.fingerprint.ACTION_LOCKOUT_RESET";
    private static final long CANCEL_TIMEOUT_LIMIT = 3000;
    static final boolean DEBUG = true;
    private static final long FAIL_LOCKOUT_TIMEOUT_MS = 30000;
    private static final String FINGERPRINTD = "android.hardware.fingerprint.IFingerprintDaemon";
    private static final String FP_DATA_DIR = "fpdata";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int MSG_USER_SWITCHING = 10;
    static final String TAG = "FingerprintService";
    private final AlarmManager mAlarmManager;
    private final AppOpsManager mAppOps;
    private Context mContext;
    private long mCurrentAuthenticatorId;
    private ClientMonitor mCurrentClient;
    private int mCurrentUserId;
    private IFingerprintDaemon mDaemon;
    private IFingerprintDaemonCallback mDaemonCallback;
    private int mFailedAttempts;
    private final FingerprintUtils mFingerprintUtils;
    private long mHalDeviceId;
    private Handler mHandler;
    private final String mKeyguardPackage;
    private final ArrayList<FingerprintServiceLockoutResetMonitor> mLockoutMonitors;
    private final BroadcastReceiver mLockoutReceiver;
    private ClientMonitor mPendingClient;
    private final PowerManager mPowerManager;
    private final Runnable mResetClientState;
    private final Runnable mResetFailedAttemptsRunnable;
    private final UserManager mUserManager;

    public FingerprintService(Context context) {
        super(context);
        this.mLockoutMonitors = new ArrayList<>();
        this.mCurrentUserId = -2;
        this.mFingerprintUtils = FingerprintUtils.getInstance();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 10:
                        FingerprintService.this.handleUserSwitching(msg.arg1);
                        break;
                    default:
                        Slog.w(FingerprintService.TAG, "Unknown message:" + msg.what);
                        break;
                }
            }
        };
        this.mLockoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!FingerprintService.ACTION_LOCKOUT_RESET.equals(intent.getAction())) {
                    return;
                }
                FingerprintService.this.resetFailedAttempts();
            }
        };
        this.mResetFailedAttemptsRunnable = new Runnable() {
            @Override
            public void run() {
                FingerprintService.this.resetFailedAttempts();
            }
        };
        this.mResetClientState = new Runnable() {
            @Override
            public void run() {
                Slog.w(FingerprintService.TAG, "Client " + (FingerprintService.this.mCurrentClient != null ? FingerprintService.this.mCurrentClient.getOwnerString() : "null") + " failed to respond to cancel, starting client " + (FingerprintService.this.mPendingClient != null ? FingerprintService.this.mPendingClient.getOwnerString() : "null"));
                FingerprintService.this.mCurrentClient = null;
                FingerprintService.this.startClient(FingerprintService.this.mPendingClient, false);
            }
        };
        this.mDaemonCallback = new IFingerprintDaemonCallback.Stub() {
            public void onEnrollResult(final long deviceId, final int fingerId, final int groupId, final int remaining) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleEnrollResult(deviceId, fingerId, groupId, remaining);
                    }
                });
            }

            public void onAcquired(final long deviceId, final int acquiredInfo) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleAcquired(deviceId, acquiredInfo);
                    }
                });
            }

            public void onAuthenticated(final long deviceId, final int fingerId, final int groupId) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleAuthenticated(deviceId, fingerId, groupId);
                    }
                });
            }

            public void onError(final long deviceId, final int error) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleError(deviceId, error);
                    }
                });
            }

            public void onRemoved(final long deviceId, final int fingerId, final int groupId) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleRemoved(deviceId, fingerId, groupId);
                    }
                });
            }

            public void onEnumerate(final long deviceId, final int[] fingerIds, final int[] groupIds) {
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.handleEnumerate(deviceId, fingerIds, groupIds);
                    }
                });
            }
        };
        this.mContext = context;
        this.mKeyguardPackage = ComponentName.unflattenFromString(context.getResources().getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_PUK_IN_PROGRESS)).getPackageName();
        this.mAppOps = (AppOpsManager) context.getSystemService(AppOpsManager.class);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        this.mContext.registerReceiver(this.mLockoutReceiver, new IntentFilter(ACTION_LOCKOUT_RESET), "android.permission.RESET_FINGERPRINT_LOCKOUT", null);
        this.mUserManager = UserManager.get(this.mContext);
    }

    @Override
    public void binderDied() {
        Slog.v(TAG, "fingerprintd died");
        this.mDaemon = null;
        handleError(this.mHalDeviceId, 1);
    }

    public IFingerprintDaemon getFingerprintDaemon() {
        if (this.mDaemon == null) {
            this.mDaemon = IFingerprintDaemon.Stub.asInterface(ServiceManager.getService(FINGERPRINTD));
            if (this.mDaemon != null) {
                try {
                    this.mDaemon.asBinder().linkToDeath(this, 0);
                    this.mDaemon.init(this.mDaemonCallback);
                    this.mHalDeviceId = this.mDaemon.openHal();
                    if (this.mHalDeviceId != 0) {
                        updateActiveGroup(ActivityManager.getCurrentUser(), null);
                    } else {
                        Slog.w(TAG, "Failed to open Fingerprint HAL!");
                        this.mDaemon = null;
                    }
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to open fingeprintd HAL", e);
                    this.mDaemon = null;
                }
            } else {
                Slog.w(TAG, "fingerprint service not available");
            }
        }
        return this.mDaemon;
    }

    protected void handleEnumerate(long deviceId, int[] fingerIds, int[] groupIds) {
        if (fingerIds.length != groupIds.length) {
            Slog.w(TAG, "fingerIds and groupIds differ in length: f[]=" + Arrays.toString(fingerIds) + ", g[]=" + Arrays.toString(groupIds));
        } else {
            Slog.w(TAG, "Enumerate: f[]=" + fingerIds + ", g[]=" + groupIds);
        }
    }

    protected void handleError(long deviceId, int error) {
        ClientMonitor client = this.mCurrentClient;
        if (client != null && client.onError(error)) {
            removeClient(client);
        }
        Slog.v(TAG, "handleError(client=" + (client != null ? client.getOwnerString() : "null") + ", error = " + error + ")");
        if (error == 5) {
            this.mHandler.removeCallbacks(this.mResetClientState);
            if (this.mPendingClient != null) {
                Slog.v(TAG, "start pending client " + this.mPendingClient.getOwnerString());
                startClient(this.mPendingClient, false);
                this.mPendingClient = null;
            }
        }
    }

    protected void handleRemoved(long deviceId, int fingerId, int groupId) {
        ClientMonitor client = this.mCurrentClient;
        if (client == null || !client.onRemoved(fingerId, groupId)) {
            return;
        }
        removeClient(client);
    }

    protected void handleAuthenticated(long deviceId, int fingerId, int groupId) {
        ClientMonitor client = this.mCurrentClient;
        if (client == null || !client.onAuthenticated(fingerId, groupId)) {
            return;
        }
        removeClient(client);
    }

    protected void handleAcquired(long deviceId, int acquiredInfo) {
        ClientMonitor client = this.mCurrentClient;
        if (client == null || !client.onAcquired(acquiredInfo)) {
            return;
        }
        removeClient(client);
    }

    protected void handleEnrollResult(long deviceId, int fingerId, int groupId, int remaining) {
        ClientMonitor client = this.mCurrentClient;
        if (client == null || !client.onEnrollResult(fingerId, groupId, remaining)) {
            return;
        }
        removeClient(client);
    }

    private void userActivity() {
        long now = SystemClock.uptimeMillis();
        this.mPowerManager.userActivity(now, 2, 0);
    }

    void handleUserSwitching(int userId) {
        updateActiveGroup(userId, null);
    }

    private void removeClient(ClientMonitor client) {
        if (client != null) {
            client.destroy();
            if (client != this.mCurrentClient && this.mCurrentClient != null) {
                Slog.w(TAG, new StringBuilder().append("Unexpected client: ").append(client.getOwnerString()).append("expected: ").append(this.mCurrentClient).toString() != null ? this.mCurrentClient.getOwnerString() : "null");
            }
        }
        if (this.mCurrentClient == null) {
            return;
        }
        Slog.v(TAG, "Done with client: " + (client != null ? client.getOwnerString() : "null"));
        this.mCurrentClient = null;
    }

    private boolean inLockoutMode() {
        return this.mFailedAttempts >= 5;
    }

    private void scheduleLockoutReset() {
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + FAIL_LOCKOUT_TIMEOUT_MS, getLockoutResetIntent());
    }

    private void cancelLockoutReset() {
        this.mAlarmManager.cancel(getLockoutResetIntent());
    }

    private PendingIntent getLockoutResetIntent() {
        return PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_LOCKOUT_RESET), 134217728);
    }

    public long startPreEnroll(IBinder token) {
        IFingerprintDaemon daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w(TAG, "startPreEnroll: no fingeprintd!");
            return 0L;
        }
        try {
            return daemon.preEnroll();
        } catch (RemoteException e) {
            Slog.e(TAG, "startPreEnroll failed", e);
            return 0L;
        }
    }

    public int startPostEnroll(IBinder token) {
        IFingerprintDaemon daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w(TAG, "startPostEnroll: no fingeprintd!");
            return 0;
        }
        try {
            return daemon.postEnroll();
        } catch (RemoteException e) {
            Slog.e(TAG, "startPostEnroll failed", e);
            return 0;
        }
    }

    private void startClient(ClientMonitor newClient, boolean initiatedByClient) {
        ClientMonitor currentClient = this.mCurrentClient;
        if (currentClient != null) {
            Slog.v(TAG, "request stop current client " + currentClient.getOwnerString());
            currentClient.stop(initiatedByClient);
            this.mPendingClient = newClient;
            this.mHandler.removeCallbacks(this.mResetClientState);
            this.mHandler.postDelayed(this.mResetClientState, CANCEL_TIMEOUT_LIMIT);
            return;
        }
        if (newClient != null) {
            this.mCurrentClient = newClient;
            Slog.v(TAG, "starting client " + newClient.getClass().getSuperclass().getSimpleName() + "(" + newClient.getOwnerString() + "), initiatedByClient = " + initiatedByClient + ")");
            newClient.start();
        }
    }

    void startRemove(IBinder token, int fingerId, int groupId, int userId, IFingerprintServiceReceiver receiver, boolean restricted) {
        IFingerprintDaemon daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w(TAG, "startRemove: no fingeprintd!");
        } else {
            RemovalClient client = new RemovalClient(getContext(), this.mHalDeviceId, token, receiver, fingerId, groupId, userId, restricted, token.toString()) {
                @Override
                public void notifyUserActivity() {
                    FingerprintService.this.userActivity();
                }

                @Override
                public IFingerprintDaemon getFingerprintDaemon() {
                    return FingerprintService.this.getFingerprintDaemon();
                }
            };
            startClient(client, true);
        }
    }

    public List<Fingerprint> getEnrolledFingerprints(int userId) {
        return this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId);
    }

    public boolean hasEnrolledFingerprints(int userId) {
        if (userId != UserHandle.getCallingUserId()) {
            checkPermission("android.permission.INTERACT_ACROSS_USERS");
        }
        return this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId).size() > 0;
    }

    boolean hasPermission(String permission) {
        return getContext().checkCallingOrSelfPermission(permission) == 0;
    }

    void checkPermission(String permission) {
        getContext().enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    int getEffectiveUserId(int userId) {
        UserManager um = UserManager.get(this.mContext);
        if (um != null) {
            long callingIdentity = Binder.clearCallingIdentity();
            int userId2 = um.getCredentialOwnerProfile(userId);
            Binder.restoreCallingIdentity(callingIdentity);
            return userId2;
        }
        Slog.e(TAG, "Unable to acquire UserManager");
        return userId;
    }

    boolean isCurrentUserOrProfile(int userId) {
        UserManager um = UserManager.get(this.mContext);
        for (int profileId : um.getEnabledProfileIds(userId)) {
            if (profileId == userId) {
                return true;
            }
        }
        return false;
    }

    private boolean isForegroundActivity(int uid, int pid) {
        try {
            List<ActivityManager.RunningAppProcessInfo> procs = ActivityManagerNative.getDefault().getRunningAppProcesses();
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                ActivityManager.RunningAppProcessInfo proc = procs.get(i);
                if (proc.pid == pid && proc.uid == uid && proc.importance == 100) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            Slog.w(TAG, "am.getRunningAppProcesses() failed");
            return false;
        }
    }

    private boolean canUseFingerprint(String opPackageName, boolean foregroundOnly, int uid, int pid) {
        checkPermission("android.permission.USE_FINGERPRINT");
        if (isKeyguard(opPackageName)) {
            return true;
        }
        if (!isCurrentUserOrProfile(UserHandle.getCallingUserId())) {
            Slog.w(TAG, "Rejecting " + opPackageName + " ; not a current user or profile");
            return false;
        }
        if (this.mAppOps.noteOp(55, uid, opPackageName) != 0) {
            Slog.w(TAG, "Rejecting " + opPackageName + " ; permission denied");
            return false;
        }
        if (!foregroundOnly || isForegroundActivity(uid, pid)) {
            return true;
        }
        Slog.w(TAG, "Rejecting " + opPackageName + " ; not in foreground");
        return false;
    }

    private boolean isKeyguard(String clientPackage) {
        return this.mKeyguardPackage.equals(clientPackage);
    }

    private void addLockoutResetMonitor(FingerprintServiceLockoutResetMonitor monitor) {
        if (this.mLockoutMonitors.contains(monitor)) {
            return;
        }
        this.mLockoutMonitors.add(monitor);
    }

    private void removeLockoutResetCallback(FingerprintServiceLockoutResetMonitor monitor) {
        this.mLockoutMonitors.remove(monitor);
    }

    private void notifyLockoutResetMonitors() {
        for (int i = 0; i < this.mLockoutMonitors.size(); i++) {
            this.mLockoutMonitors.get(i).sendLockoutReset();
        }
    }

    private void startAuthentication(IBinder token, long opId, int callingUserId, int groupId, IFingerprintServiceReceiver receiver, int flags, boolean restricted, String opPackageName) {
        updateActiveGroup(groupId, opPackageName);
        Slog.v(TAG, "startAuthentication(" + opPackageName + ")");
        AuthenticationClient client = new AuthenticationClient(getContext(), this.mHalDeviceId, token, receiver, this.mCurrentUserId, groupId, opId, restricted, opPackageName) {
            @Override
            public boolean handleFailedAttempt() {
                FingerprintService.this.mFailedAttempts++;
                if (FingerprintService.this.inLockoutMode()) {
                    FingerprintService.this.scheduleLockoutReset();
                    return true;
                }
                return false;
            }

            @Override
            public void resetFailedAttempts() {
                FingerprintService.this.resetFailedAttempts();
            }

            @Override
            public void notifyUserActivity() {
                FingerprintService.this.userActivity();
            }

            @Override
            public IFingerprintDaemon getFingerprintDaemon() {
                return FingerprintService.this.getFingerprintDaemon();
            }
        };
        if (inLockoutMode()) {
            Slog.v(TAG, "In lockout mode; disallowing authentication");
            if (!client.onError(7)) {
                Slog.w(TAG, "Cannot send timeout message to client");
                return;
            }
            return;
        }
        startClient(client, true);
    }

    private void startEnrollment(IBinder token, byte[] cryptoToken, int userId, IFingerprintServiceReceiver receiver, int flags, boolean restricted, String opPackageName) {
        updateActiveGroup(userId, opPackageName);
        EnrollClient client = new EnrollClient(getContext(), this.mHalDeviceId, token, receiver, userId, userId, cryptoToken, restricted, opPackageName) {
            @Override
            public IFingerprintDaemon getFingerprintDaemon() {
                return FingerprintService.this.getFingerprintDaemon();
            }

            @Override
            public void notifyUserActivity() {
                FingerprintService.this.userActivity();
            }
        };
        startClient(client, true);
    }

    protected void resetFailedAttempts() {
        if (inLockoutMode()) {
            Slog.v(TAG, "Reset fingerprint lockout");
        }
        this.mFailedAttempts = 0;
        cancelLockoutReset();
        notifyLockoutResetMonitors();
    }

    private class FingerprintServiceLockoutResetMonitor {
        private final IFingerprintServiceLockoutResetCallback mCallback;
        private final Runnable mRemoveCallbackRunnable = new Runnable() {
            @Override
            public void run() {
                FingerprintService.this.removeLockoutResetCallback(FingerprintServiceLockoutResetMonitor.this);
            }
        };

        public FingerprintServiceLockoutResetMonitor(IFingerprintServiceLockoutResetCallback callback) {
            this.mCallback = callback;
        }

        public void sendLockoutReset() {
            if (this.mCallback == null) {
                return;
            }
            try {
                this.mCallback.onLockoutReset(FingerprintService.this.mHalDeviceId);
            } catch (DeadObjectException e) {
                Slog.w(FingerprintService.TAG, "Death object while invoking onLockoutReset: ", e);
                FingerprintService.this.mHandler.post(this.mRemoveCallbackRunnable);
            } catch (RemoteException e2) {
                Slog.w(FingerprintService.TAG, "Failed to invoke onLockoutReset: ", e2);
            }
        }
    }

    private final class FingerprintServiceWrapper extends IFingerprintService.Stub {
        FingerprintServiceWrapper(FingerprintService this$0, FingerprintServiceWrapper fingerprintServiceWrapper) {
            this();
        }

        private FingerprintServiceWrapper() {
        }

        public long preEnroll(IBinder token) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            return FingerprintService.this.startPreEnroll(token);
        }

        public int postEnroll(IBinder token) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            return FingerprintService.this.startPostEnroll(token);
        }

        public void enroll(final IBinder token, final byte[] cryptoToken, final int userId, final IFingerprintServiceReceiver receiver, final int flags, final String opPackageName) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            int limit = FingerprintService.this.mContext.getResources().getInteger(R.integer.config_externalDisplayPeakHeight);
            int enrolled = FingerprintService.this.getEnrolledFingerprints(userId).size();
            if (enrolled >= limit) {
                Slog.w(FingerprintService.TAG, "Too many fingerprints registered");
            } else {
                if (!FingerprintService.this.isCurrentUserOrProfile(userId)) {
                    return;
                }
                final boolean restricted = isRestricted();
                FingerprintService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        FingerprintService.this.startEnrollment(token, cryptoToken, userId, receiver, flags, restricted, opPackageName);
                    }
                });
            }
        }

        private boolean isRestricted() {
            return !FingerprintService.this.hasPermission("android.permission.MANAGE_FINGERPRINT");
        }

        public void cancelEnrollment(final IBinder token) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ClientMonitor client = FingerprintService.this.mCurrentClient;
                    if (!(client instanceof EnrollClient) || client.getToken() != token) {
                        return;
                    }
                    client.stop(client.getToken() == token);
                }
            });
        }

        public void authenticate(final IBinder token, final long opId, final int groupId, final IFingerprintServiceReceiver receiver, final int flags, final String opPackageName) {
            final int callingUid = Binder.getCallingUid();
            final int callingUserId = UserHandle.getCallingUserId();
            final int pid = Binder.getCallingPid();
            final boolean restricted = isRestricted();
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MetricsLogger.histogram(FingerprintService.this.mContext, "fingerprint_token", opId != 0 ? 1 : 0);
                    if (!FingerprintService.this.canUseFingerprint(opPackageName, true, callingUid, pid)) {
                        Slog.v(FingerprintService.TAG, "authenticate(): reject " + opPackageName);
                    } else {
                        FingerprintService.this.startAuthentication(token, opId, callingUserId, groupId, receiver, flags, restricted, opPackageName);
                    }
                }
            });
        }

        public void cancelAuthentication(final IBinder token, final String opPackageName) {
            final int uid = Binder.getCallingUid();
            final int pid = Binder.getCallingPid();
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!FingerprintService.this.canUseFingerprint(opPackageName, true, uid, pid)) {
                        Slog.v(FingerprintService.TAG, "cancelAuthentication(): reject " + opPackageName);
                        return;
                    }
                    ClientMonitor client = FingerprintService.this.mCurrentClient;
                    if (client instanceof AuthenticationClient) {
                        if (client.getToken() == token) {
                            Slog.v(FingerprintService.TAG, "stop client " + client.getOwnerString());
                            client.stop(client.getToken() == token);
                            return;
                        } else {
                            Slog.v(FingerprintService.TAG, "can't stop client " + client.getOwnerString() + " since tokens don't match");
                            return;
                        }
                    }
                    if (client == null) {
                        return;
                    }
                    Slog.v(FingerprintService.TAG, "can't cancel non-authenticating client " + client.getOwnerString());
                }
            });
        }

        public void setActiveUser(final int userId) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.updateActiveGroup(userId, null);
                }
            });
        }

        public void remove(final IBinder token, final int fingerId, final int groupId, final int userId, final IFingerprintServiceReceiver receiver) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            final boolean restricted = isRestricted();
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.startRemove(token, fingerId, groupId, userId, receiver, restricted);
                }
            });
        }

        public boolean isHardwareDetected(long deviceId, String opPackageName) {
            return FingerprintService.this.canUseFingerprint(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid()) && FingerprintService.this.mHalDeviceId != 0;
        }

        public void rename(final int fingerId, final int groupId, final String name) {
            FingerprintService.this.checkPermission("android.permission.MANAGE_FINGERPRINT");
            if (!FingerprintService.this.isCurrentUserOrProfile(groupId)) {
                return;
            }
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.mFingerprintUtils.renameFingerprintForUser(FingerprintService.this.mContext, fingerId, groupId, name);
                }
            });
        }

        public List<Fingerprint> getEnrolledFingerprints(int userId, String opPackageName) {
            if (!FingerprintService.this.canUseFingerprint(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid())) {
                return Collections.emptyList();
            }
            if (!FingerprintService.this.isCurrentUserOrProfile(userId)) {
                return Collections.emptyList();
            }
            return FingerprintService.this.getEnrolledFingerprints(userId);
        }

        public boolean hasEnrolledFingerprints(int userId, String opPackageName) {
            if (FingerprintService.this.canUseFingerprint(opPackageName, false, Binder.getCallingUid(), Binder.getCallingPid()) && FingerprintService.this.isCurrentUserOrProfile(userId)) {
                return FingerprintService.this.hasEnrolledFingerprints(userId);
            }
            return false;
        }

        public long getAuthenticatorId(String opPackageName) {
            return FingerprintService.this.getAuthenticatorId(opPackageName);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (FingerprintService.this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
                pw.println("Permission Denial: can't dump Fingerprint from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
                return;
            }
            long ident = Binder.clearCallingIdentity();
            try {
                FingerprintService.this.dumpInternal(pw);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void resetTimeout(byte[] token) {
            FingerprintService.this.checkPermission("android.permission.RESET_FINGERPRINT_LOCKOUT");
            FingerprintService.this.mHandler.post(FingerprintService.this.mResetFailedAttemptsRunnable);
        }

        public void addLockoutResetCallback(final IFingerprintServiceLockoutResetCallback callback) throws RemoteException {
            FingerprintService.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    FingerprintService.this.addLockoutResetMonitor(FingerprintService.this.new FingerprintServiceLockoutResetMonitor(callback));
                }
            });
        }
    }

    private void dumpInternal(PrintWriter printWriter) {
        JSONObject jSONObject = new JSONObject();
        try {
            jSONObject.put("service", "Fingerprint Manager");
            JSONArray sets = new JSONArray();
            for (UserInfo user : UserManager.get(getContext()).getUsers()) {
                int userId = user.getUserHandle().getIdentifier();
                int N = this.mFingerprintUtils.getFingerprintsForUser(this.mContext, userId).size();
                JSONObject set = new JSONObject();
                set.put("id", userId);
                set.put("count", N);
                sets.put(set);
            }
            jSONObject.put("prints", sets);
        } catch (JSONException e) {
            Slog.e(TAG, "dump formatting failure", e);
        }
        printWriter.println(jSONObject);
    }

    @Override
    public void onStart() {
        publishBinderService("fingerprint", new FingerprintServiceWrapper(this, null));
        getFingerprintDaemon();
        Slog.v(TAG, "Fingerprint HAL id: " + this.mHalDeviceId);
        listenForUserSwitches();
    }

    private void updateActiveGroup(int userId, String clientPackage) {
        IFingerprintDaemon daemon = getFingerprintDaemon();
        if (daemon == null) {
            return;
        }
        try {
            int userId2 = getUserOrWorkProfileId(clientPackage, userId);
            if (userId2 != this.mCurrentUserId) {
                File systemDir = Environment.getUserSystemDirectory(userId2);
                File fpDir = new File(systemDir, FP_DATA_DIR);
                if (!fpDir.exists()) {
                    if (!fpDir.mkdir()) {
                        Slog.v(TAG, "Cannot make directory: " + fpDir.getAbsolutePath());
                        return;
                    } else if (!SELinux.restorecon(fpDir)) {
                        Slog.w(TAG, "Restorecons failed. Directory will have wrong label.");
                        return;
                    }
                }
                daemon.setActiveGroup(userId2, fpDir.getAbsolutePath().getBytes());
                this.mCurrentUserId = userId2;
            }
            this.mCurrentAuthenticatorId = daemon.getAuthenticatorId();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to setActiveGroup():", e);
        }
    }

    private int getUserOrWorkProfileId(String clientPackage, int userId) {
        if (!isKeyguard(clientPackage) && isWorkProfile(userId)) {
            return userId;
        }
        return getEffectiveUserId(userId);
    }

    private boolean isWorkProfile(int userId) {
        UserInfo info = this.mUserManager.getUserInfo(userId);
        if (info != null) {
            return info.isManagedProfile();
        }
        return false;
    }

    private void listenForUserSwitches() {
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new SynchronousUserSwitchObserver() {
                public void onUserSwitching(int newUserId) throws RemoteException {
                    FingerprintService.this.mHandler.obtainMessage(10, newUserId, 0).sendToTarget();
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                }

                public void onForegroundProfileSwitch(int newProfileId) {
                }
            });
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to listen for user switching event", e);
        }
    }

    public long getAuthenticatorId(String opPackageName) {
        return this.mCurrentAuthenticatorId;
    }
}
