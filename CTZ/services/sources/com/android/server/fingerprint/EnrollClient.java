package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.biometrics.fingerprint.V2_1.IBiometricsFingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import java.util.Arrays;

public abstract class EnrollClient extends ClientMonitor {
    private static final int ENROLLMENT_TIMEOUT_MS = 60000;
    private static final long MS_PER_SEC = 1000;
    private byte[] mCryptoToken;

    public EnrollClient(Context context, long j, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i, int i2, byte[] bArr, boolean z, String str) {
        super(context, j, iBinder, iFingerprintServiceReceiver, i, i2, z, str);
        this.mCryptoToken = Arrays.copyOf(bArr, bArr.length);
    }

    @Override
    public boolean onEnrollResult(int i, int i2, int i3) {
        if (i2 != getGroupId()) {
            Slog.w("FingerprintService", "groupId != getGroupId(), groupId: " + i2 + " getGroupId():" + getGroupId());
        }
        if (i3 == 0) {
            FingerprintUtils.getInstance().addFingerprintForUser(getContext(), i, getTargetUserId());
        }
        return sendEnrollResult(i, i2, i3);
    }

    private boolean sendEnrollResult(int i, int i2, int i3) {
        IFingerprintServiceReceiver receiver = getReceiver();
        if (receiver == null) {
            return true;
        }
        vibrateSuccess();
        MetricsLogger.action(getContext(), 251);
        try {
            receiver.onEnrollResult(getHalDeviceId(), i, i2, i3);
            return i3 == 0;
        } catch (RemoteException e) {
            Slog.w("FingerprintService", "Failed to notify EnrollResult:", e);
            return true;
        }
    }

    @Override
    public int start() {
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon == null) {
            Slog.w("FingerprintService", "enroll: no fingerprint HAL!");
            return 3;
        }
        try {
            int iEnroll = fingerprintDaemon.enroll(this.mCryptoToken, getGroupId(), 60);
            if (iEnroll != 0) {
                Slog.w("FingerprintService", "startEnroll failed, result=" + iEnroll);
                MetricsLogger.histogram(getContext(), "fingerprintd_enroll_start_error", iEnroll);
                onError(1, 0);
                return iEnroll;
            }
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "startEnroll failed", e);
        }
        return 0;
    }

    @Override
    public int stop(boolean z) {
        if (this.mAlreadyCancelled) {
            Slog.w("FingerprintService", "stopEnroll: already cancelled!");
            return 0;
        }
        IBiometricsFingerprint fingerprintDaemon = getFingerprintDaemon();
        if (fingerprintDaemon == null) {
            Slog.w("FingerprintService", "stopEnrollment: no fingerprint HAL!");
            return 3;
        }
        try {
            int iCancel = fingerprintDaemon.cancel();
            if (iCancel != 0) {
                Slog.w("FingerprintService", "startEnrollCancel failed, result = " + iCancel);
                return iCancel;
            }
        } catch (RemoteException e) {
            Slog.e("FingerprintService", "stopEnrollment failed", e);
        }
        if (z) {
            onError(5, 0);
        }
        this.mAlreadyCancelled = true;
        return 0;
    }

    @Override
    public boolean onRemoved(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onRemoved() called for enroll!");
        return true;
    }

    @Override
    public boolean onEnumerationResult(int i, int i2, int i3) {
        Slog.w("FingerprintService", "onEnumerationResult() called for enroll!");
        return true;
    }

    @Override
    public boolean onAuthenticated(int i, int i2) {
        Slog.w("FingerprintService", "onAuthenticated() called for enroll!");
        return true;
    }
}
