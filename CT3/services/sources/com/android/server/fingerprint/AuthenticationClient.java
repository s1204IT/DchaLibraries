package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintDaemon;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;

public abstract class AuthenticationClient extends ClientMonitor {
    private long mOpId;

    public abstract boolean handleFailedAttempt();

    public abstract void resetFailedAttempts();

    public AuthenticationClient(Context context, long halDeviceId, IBinder token, IFingerprintServiceReceiver receiver, int targetUserId, int groupId, long opId, boolean restricted, String owner) {
        super(context, halDeviceId, token, receiver, targetUserId, groupId, restricted, owner);
        this.mOpId = opId;
    }

    @Override
    public boolean onAuthenticated(int fingerId, int groupId) {
        boolean result = false;
        boolean authenticated = fingerId != 0;
        IFingerprintServiceReceiver receiver = getReceiver();
        if (receiver != null) {
            try {
                MetricsLogger.action(getContext(), 252, authenticated);
                if (authenticated) {
                    Slog.v("FingerprintService", "onAuthenticated(owner=" + getOwnerString() + ", id=" + fingerId + ", gp=" + groupId + ")");
                    receiver.onAuthenticationSucceeded(getHalDeviceId(), !getIsRestricted() ? new Fingerprint("", groupId, fingerId, getHalDeviceId()) : null, getTargetUserId());
                } else {
                    receiver.onAuthenticationFailed(getHalDeviceId());
                }
            } catch (RemoteException e) {
                Slog.w("FingerprintService", "Failed to notify Authenticated:", e);
                result = true;
            }
        } else {
            result = true;
        }
        if (authenticated) {
            if (receiver != null) {
                FingerprintUtils.vibrateFingerprintSuccess(getContext());
            }
            boolean result2 = result | true;
            resetFailedAttempts();
            return result2;
        }
        if (receiver != null) {
            FingerprintUtils.vibrateFingerprintError(getContext());
        }
        boolean inLockoutMode = handleFailedAttempt();
        if (inLockoutMode) {
            try {
                Slog.w("FingerprintService", "Forcing lockout (fp driver code should do this!)");
                receiver.onError(getHalDeviceId(), 7);
            } catch (RemoteException e2) {
                Slog.w("FingerprintService", "Failed to notify lockout:", e2);
            }
        }
        return result | inLockoutMode;
    }

    @Override
    public int start() {
        IFingerprintDaemon daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w("FingerprintService", "start authentication: no fingeprintd!");
            return 3;
        }
        try {
            int result = daemon.authenticate(this.mOpId, getGroupId());
            if (result != 0) {
                Slog.w("FingerprintService", "startAuthentication failed, result=" + result);
                onError(1);
                return result;
            }
            Slog.w("FingerprintService", "client " + getOwnerString() + " is authenticating...");
            return 0;
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "startAuthentication failed", e);
            return 3;
        }
    }

    @Override
    public int stop(boolean initiatedByClient) {
        IFingerprintDaemon daemon = getFingerprintDaemon();
        if (daemon == null) {
            Slog.w("FingerprintService", "stopAuthentication: no fingeprintd!");
            return 3;
        }
        try {
            int result = daemon.cancelAuthentication();
            if (result != 0) {
                Slog.w("FingerprintService", "stopAuthentication failed, result=" + result);
                return result;
            }
            Slog.w("FingerprintService", "client " + getOwnerString() + " is no longer authenticating");
            return 0;
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "stopAuthentication failed", e);
            return 3;
        }
    }

    @Override
    public boolean onEnrollResult(int fingerId, int groupId, int rem) {
        Slog.w("FingerprintService", "onEnrollResult() called for authenticate!");
        return true;
    }

    @Override
    public boolean onRemoved(int fingerId, int groupId) {
        Slog.w("FingerprintService", "onRemoved() called for authenticate!");
        return true;
    }

    @Override
    public boolean onEnumerationResult(int fingerId, int groupId) {
        Slog.w("FingerprintService", "onEnumerationResult() called for authenticate!");
        return true;
    }
}
