package com.android.server.fingerprint;

import android.R;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.EventLog;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.backup.BackupManagerConstants;
import java.util.List;

public abstract class AuthenticationClient extends ClientMonitor {
    public static final int LOCKOUT_NONE = 0;
    public static final int LOCKOUT_PERMANENT = 2;
    public static final int LOCKOUT_TIMED = 1;
    private Bundle mBundle;
    protected boolean mDialogDismissed;
    protected IBiometricPromptReceiver mDialogReceiver;
    private IBiometricPromptReceiver mDialogReceiverFromClient;
    private final FingerprintManager mFingerprintManager;
    private boolean mInLockout;
    private long mOpId;
    private IStatusBarService mStatusBarService;

    public abstract int handleFailedAttempt();

    public abstract void onStart();

    public abstract void onStop();

    public abstract void resetFailedAttempts();

    public AuthenticationClient(Context context, long j, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i, int i2, long j2, boolean z, String str, Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver, IStatusBarService iStatusBarService) {
        super(context, j, iBinder, iFingerprintServiceReceiver, i, i2, z, str);
        this.mDialogReceiver = new IBiometricPromptReceiver.Stub() {
            public void onDialogDismissed(int i3) {
                if (AuthenticationClient.this.mBundle != null && AuthenticationClient.this.mDialogReceiverFromClient != null) {
                    try {
                        AuthenticationClient.this.mDialogReceiverFromClient.onDialogDismissed(i3);
                        if (i3 == 3) {
                            AuthenticationClient.this.onError(10, 0);
                        }
                        AuthenticationClient.this.mDialogDismissed = true;
                    } catch (RemoteException e) {
                        Slog.e("FingerprintService", "Unable to notify dialog dismissed", e);
                    }
                    AuthenticationClient.this.stop(true);
                }
            }
        };
        this.mOpId = j2;
        this.mBundle = bundle;
        this.mDialogReceiverFromClient = iBiometricPromptReceiver;
        this.mStatusBarService = iStatusBarService;
        this.mFingerprintManager = (FingerprintManager) getContext().getSystemService("fingerprint");
    }

    @Override
    public void binderDied() {
        super.binderDied();
        stop(false);
    }

    @Override
    public boolean onAcquired(int i, int i2) {
        if (this.mBundle == null) {
            return super.onAcquired(i, i2);
        }
        try {
            if (i != 0) {
                try {
                    this.mStatusBarService.onFingerprintHelp(this.mFingerprintManager.getAcquiredString(i, i2));
                } catch (RemoteException e) {
                    Slog.e("FingerprintService", "Remote exception when sending acquired message", e);
                    if (i == 0) {
                        notifyUserActivity();
                    }
                    return true;
                }
            }
            if (i == 0) {
                notifyUserActivity();
            }
            return false;
        } catch (Throwable th) {
            if (i == 0) {
                notifyUserActivity();
            }
            throw th;
        }
    }

    @Override
    public boolean onError(int i, int i2) {
        if (this.mDialogDismissed) {
            return true;
        }
        if (this.mBundle != null) {
            try {
                this.mStatusBarService.onFingerprintError(this.mFingerprintManager.getErrorString(i, i2));
            } catch (RemoteException e) {
                Slog.e("FingerprintService", "Remote exception when sending error", e);
            }
        }
        return super.onError(i, i2);
    }

    @Override
    public boolean onAuthenticated(int i, int i2) {
        boolean z;
        boolean z2;
        IFingerprintServiceReceiver iFingerprintServiceReceiver;
        Fingerprint fingerprint;
        boolean z3;
        int i3;
        IActivityManager service;
        boolean z4;
        boolean z5 = i != 0;
        if (z5 && !isKeyguard(getContext(), getOwnerString())) {
            if (((ActivityManager) getContext().getSystemService("activity")) != null) {
                service = ActivityManager.getService();
            } else {
                service = null;
            }
            if (service == null) {
                Slog.e("FingerprintService", "Unable to get activity manager service");
            } else {
                try {
                    List tasks = service.getTasks(1);
                    if (tasks == null || tasks.isEmpty()) {
                        Slog.e("FingerprintService", "No running tasks reported");
                    } else {
                        ComponentName componentName = ((ActivityManager.RunningTaskInfo) tasks.get(0)).topActivity;
                        if (componentName == null) {
                            Slog.e("FingerprintService", "Unable to get top activity");
                        } else {
                            String packageName = componentName.getPackageName();
                            if (!packageName.contentEquals(getOwnerString())) {
                                Slog.e("FingerprintService", "Background authentication detected, top: " + packageName + ", client: " + this);
                            } else {
                                z4 = false;
                                z = z4;
                            }
                        }
                    }
                    z4 = true;
                    z = z4;
                } catch (RemoteException e) {
                    Slog.e("FingerprintService", "Unable to get running tasks", e);
                    z = true;
                }
            }
            z = true;
        } else {
            z = false;
        }
        if (z) {
            Slog.e("FingerprintService", "Failing possible background authentication");
            ApplicationInfo applicationInfo = getContext().getApplicationInfo();
            Object[] objArr = new Object[3];
            objArr[0] = "159249069";
            objArr[1] = Integer.valueOf(applicationInfo != null ? applicationInfo.uid : -1);
            objArr[2] = "Attempted background authentication";
            EventLog.writeEvent(1397638484, objArr);
            z2 = false;
        } else {
            z2 = z5;
        }
        if (this.mBundle != null) {
            try {
                if (z2) {
                    if (z) {
                        ApplicationInfo applicationInfo2 = getContext().getApplicationInfo();
                        Object[] objArr2 = new Object[3];
                        objArr2[0] = "159249069";
                        objArr2[1] = Integer.valueOf(applicationInfo2 != null ? applicationInfo2.uid : -1);
                        objArr2[2] = "Successful background authentication! Dialog notified";
                        EventLog.writeEvent(1397638484, objArr2);
                    }
                    this.mStatusBarService.onFingerprintAuthenticated();
                } else {
                    this.mStatusBarService.onFingerprintHelp(getContext().getResources().getString(R.string.config_defaultAppPredictionService));
                }
            } catch (RemoteException e2) {
                Slog.e("FingerprintService", "Failed to notify Authenticated:", e2);
            }
        }
        IFingerprintServiceReceiver receiver = getReceiver();
        if (receiver != null) {
            try {
                MetricsLogger.action(getContext(), 252, z2);
                if (!z2) {
                    receiver.onAuthenticationFailed(getHalDeviceId());
                    iFingerprintServiceReceiver = receiver;
                } else {
                    if (z) {
                        ApplicationInfo applicationInfo3 = getContext().getApplicationInfo();
                        Object[] objArr3 = new Object[3];
                        objArr3[0] = "159249069";
                        objArr3[1] = Integer.valueOf(applicationInfo3 != null ? applicationInfo3.uid : -1);
                        objArr3[2] = "Successful background authentication! Receiver notified";
                        EventLog.writeEvent(1397638484, objArr3);
                    }
                    Slog.v("FingerprintService", "onAuthenticated(owner=" + getOwnerString() + ", id=" + i + ", gp=" + i2 + ")");
                    if (!getIsRestricted()) {
                        iFingerprintServiceReceiver = receiver;
                        try {
                            fingerprint = new Fingerprint(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i2, i, getHalDeviceId());
                        } catch (RemoteException e3) {
                            e = e3;
                            Slog.w("FingerprintService", "Failed to notify Authenticated:", e);
                            z3 = true;
                        }
                    } else {
                        iFingerprintServiceReceiver = receiver;
                        fingerprint = null;
                    }
                    iFingerprintServiceReceiver.onAuthenticationSucceeded(getHalDeviceId(), fingerprint, getTargetUserId());
                }
                z3 = false;
            } catch (RemoteException e4) {
                e = e4;
                iFingerprintServiceReceiver = receiver;
            }
            if (z2) {
                if (iFingerprintServiceReceiver != null) {
                    vibrateError();
                }
                int iHandleFailedAttempt = handleFailedAttempt();
                if (iHandleFailedAttempt != 0) {
                    try {
                        this.mInLockout = true;
                        Slog.w("FingerprintService", "Forcing lockout (fp driver code should do this!), mode(" + iHandleFailedAttempt + ")");
                        stop(false);
                        if (iHandleFailedAttempt == 1) {
                            i3 = 7;
                        } else {
                            i3 = 9;
                        }
                        iFingerprintServiceReceiver.onError(getHalDeviceId(), i3, 0);
                        if (this.mBundle != null) {
                            this.mStatusBarService.onFingerprintError(this.mFingerprintManager.getErrorString(i3, 0));
                        }
                    } catch (RemoteException e5) {
                        Slog.w("FingerprintService", "Failed to notify lockout:", e5);
                    }
                }
                return z3 | (iHandleFailedAttempt != 0);
            }
            if (z) {
                ApplicationInfo applicationInfo4 = getContext().getApplicationInfo();
                Object[] objArr4 = new Object[3];
                objArr4[0] = "159249069";
                objArr4[1] = Integer.valueOf(applicationInfo4 != null ? applicationInfo4.uid : -1);
                objArr4[2] = "Successful background authentication! Lockout reset";
                EventLog.writeEvent(1397638484, objArr4);
            }
            if (iFingerprintServiceReceiver != null) {
                vibrateSuccess();
            }
            boolean z6 = z3 | true;
            resetFailedAttempts();
            onStop();
            return z6;
        }
        iFingerprintServiceReceiver = receiver;
        z3 = true;
        if (z2) {
        }
    }

    private static boolean isKeyguard(Context context, String str) {
        boolean z = (context.checkCallingOrSelfPermission("android.permission.USE_FINGERPRINT") == 0) || (context.checkCallingOrSelfPermission("android.permission.USE_BIOMETRIC") == 0);
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(context.getResources().getString(R.string.alternative_fp_setup_notification_content));
        String packageName = componentNameUnflattenFromString != null ? componentNameUnflattenFromString.getPackageName() : null;
        return z && packageName != null && packageName.equals(str);
    }

    @Override
    public int start() {
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon == null) {
            Slog.w("FingerprintService", "start authentication: no fingerprint HAL!");
            return 3;
        }
        onStart();
        try {
            int iAuthenticate = fingerprintDaemon.authenticate(this.mOpId, getGroupId());
            if (iAuthenticate != 0) {
                Slog.w("FingerprintService", "startAuthentication failed, result=" + iAuthenticate);
                MetricsLogger.histogram(getContext(), "fingeprintd_auth_start_error", iAuthenticate);
                onError(1, 0);
                return iAuthenticate;
            }
            Slog.w("FingerprintService", "client " + getOwnerString() + " is authenticating...");
            if (this.mBundle != null) {
                try {
                    this.mStatusBarService.showFingerprintDialog(this.mBundle, this.mDialogReceiver);
                } catch (RemoteException e) {
                    Slog.e("FingerprintService", "Unable to show fingerprint dialog", e);
                }
            }
            return 0;
        } catch (RemoteException e2) {
            Slog.e("FingerprintService", "startAuthentication failed", e2);
            return 3;
        }
    }

    @Override
    public int stop(boolean z) {
        if (this.mAlreadyCancelled) {
            Slog.w("FingerprintService", "stopAuthentication: already cancelled!");
            return 0;
        }
        onStop();
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        try {
            if (fingerprintDaemon == null) {
                Slog.w("FingerprintService", "stopAuthentication: no fingerprint HAL!");
                return 3;
            }
            try {
                int iCancel = fingerprintDaemon.cancel();
                if (iCancel != 0) {
                    Slog.w("FingerprintService", "stopAuthentication failed, result=" + iCancel);
                    if (this.mBundle != null && !this.mDialogDismissed && !this.mInLockout) {
                        try {
                            this.mStatusBarService.hideFingerprintDialog();
                        } catch (RemoteException e) {
                            Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e);
                        }
                    }
                    return iCancel;
                }
                Slog.w("FingerprintService", "client " + getOwnerString() + " is no longer authenticating");
                if (this.mBundle != null && !this.mDialogDismissed && !this.mInLockout) {
                    try {
                        this.mStatusBarService.hideFingerprintDialog();
                    } catch (RemoteException e2) {
                        Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e2);
                    }
                }
                this.mAlreadyCancelled = true;
                return 0;
            } catch (RemoteException e3) {
                Slog.e("FingerprintService", "stopAuthentication failed", e3);
                if (this.mBundle != null && !this.mDialogDismissed && !this.mInLockout) {
                    try {
                        this.mStatusBarService.hideFingerprintDialog();
                    } catch (RemoteException e4) {
                        Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e4);
                    }
                }
                return 3;
            }
        } catch (Throwable th) {
            if (this.mBundle != null && !this.mDialogDismissed && !this.mInLockout) {
                try {
                    this.mStatusBarService.hideFingerprintDialog();
                } catch (RemoteException e5) {
                    Slog.e("FingerprintService", "Unable to hide fingerprint dialog", e5);
                }
            }
            throw th;
        }
    }

    @Override
    public boolean onEnrollResult(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onEnrollResult() called for authenticate!");
        return true;
    }

    @Override
    public boolean onRemoved(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onRemoved() called for authenticate!");
        return true;
    }

    @Override
    public boolean onEnumerationResult(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onEnumerationResult() called for authenticate!");
        return true;
    }
}
