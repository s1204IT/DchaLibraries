package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;

public abstract class RemovalClient extends ClientMonitor {
    private int mFingerId;

    public RemovalClient(Context context, long j, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i, int i2, int i3, boolean z, String str) {
        super(context, j, iBinder, iFingerprintServiceReceiver, i3, i2, z, str);
        this.mFingerId = i;
    }

    @Override
    public int start() {
        try {
            int iRemove = getFingerprintDaemon().remove(getGroupId(), this.mFingerId);
            if (iRemove != 0) {
                Slog.w("FingerprintService", "startRemove with id = " + this.mFingerId + " failed, result=" + iRemove);
                MetricsLogger.histogram(getContext(), "fingerprintd_remove_start_error", iRemove);
                onError(1, 0);
                return iRemove;
            }
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "startRemove failed", e);
        }
        return 0;
    }

    @Override
    public int stop(boolean z) {
        if (this.mAlreadyCancelled) {
            Slog.w("FingerprintService", "stopRemove: already cancelled!");
            return 0;
        }
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon == null) {
            Slog.w("FingerprintService", "stopRemoval: no fingerprint HAL!");
            return 3;
        }
        try {
            int iCancel = fingerprintDaemon.cancel();
            if (iCancel != 0) {
                Slog.w("FingerprintService", "stopRemoval failed, result=" + iCancel);
                return iCancel;
            }
            Slog.w("FingerprintService", "client " + getOwnerString() + " is no longer removing");
            this.mAlreadyCancelled = true;
            return 0;
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "stopRemoval failed", e);
            return 3;
        }
    }

    private boolean sendRemoved(int i, int i2, int i3) {
        IFingerprintServiceReceiver receiver = getReceiver();
        if (receiver != null) {
            try {
                receiver.onRemoved(getHalDeviceId(), i, i2, i3);
            } catch (RemoteException e) {
                Slog.w("FingerprintService", "Failed to notify Removed:", e);
            }
        }
        return i3 == 0;
    }

    @Override
    public boolean onRemoved(int i, int i2, int i3) {
        if (i != 0) {
            FingerprintUtils.getInstance().removeFingerprintIdForUser(getContext(), i, getTargetUserId());
        }
        return sendRemoved(i, getGroupId(), i3);
    }

    @Override
    public boolean onEnrollResult(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onEnrollResult() called for remove!");
        return true;
    }

    @Override
    public boolean onAuthenticated(int i, int i2) {
        Slog.w("FingerprintService", "onAuthenticated() called for remove!");
        return true;
    }

    @Override
    public boolean onEnumerationResult(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onEnumerationResult() called for remove!");
        return true;
    }
}
