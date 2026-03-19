package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;

public abstract class EnumerateClient extends ClientMonitor {
    public EnumerateClient(Context context, long j, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i, int i2, boolean z, String str) {
        super(context, j, iBinder, iFingerprintServiceReceiver, i2, i, z, str);
    }

    @Override
    public int start() {
        try {
            int iEnumerate = getFingerprintDaemon().enumerate();
            if (iEnumerate != 0) {
                Slog.w("FingerprintService", "start enumerate for user " + getTargetUserId() + " failed, result=" + iEnumerate);
                MetricsLogger.histogram(getContext(), "fingerprintd_enum_start_error", iEnumerate);
                onError(1, 0);
                return iEnumerate;
            }
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "startEnumeration failed", e);
        }
        return 0;
    }

    @Override
    public int stop(boolean z) {
        if (this.mAlreadyCancelled) {
            Slog.w("FingerprintService", "stopEnumerate: already cancelled!");
            return 0;
        }
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon == null) {
            Slog.w("FingerprintService", "stopEnumeration: no fingerprint HAL!");
            return 3;
        }
        try {
            int iCancel = fingerprintDaemon.cancel();
            if (iCancel != 0) {
                Slog.w("FingerprintService", "stop enumeration failed, result=" + iCancel);
                return iCancel;
            }
            if (z) {
                onError(5, 0);
            }
            this.mAlreadyCancelled = true;
            return 0;
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "stopEnumeration failed", e);
            return 3;
        }
    }

    @Override
    public boolean onEnumerationResult(int i, int i2, int i3) {
        IFingerprintServiceReceiver receiver = getReceiver();
        if (receiver == null) {
            return true;
        }
        try {
            receiver.onEnumerated(getHalDeviceId(), i, i2, i3);
        } catch (RemoteException e) {
            Slog.w("FingerprintService", "Failed to notify enumerated:", e);
        }
        return i3 == 0;
    }

    @Override
    public boolean onAuthenticated(int i, int i2) {
        Slog.w("FingerprintService", "onAuthenticated() called for enumerate!");
        return true;
    }

    @Override
    public boolean onEnrollResult(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onEnrollResult() called for enumerate!");
        return true;
    }

    @Override
    public boolean onRemoved(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onRemoved() called for enumerate!");
        return true;
    }
}
