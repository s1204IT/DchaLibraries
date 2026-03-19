package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.IFingerprintServiceReceiver;
import android.os.IBinder;
import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import java.util.ArrayList;
import java.util.List;

public abstract class InternalEnumerateClient extends EnumerateClient {
    private List<Fingerprint> mEnrolledList;
    private List<Fingerprint> mUnknownFingerprints;

    public InternalEnumerateClient(Context context, long j, IBinder iBinder, IFingerprintServiceReceiver iFingerprintServiceReceiver, int i, int i2, boolean z, String str, List<Fingerprint> list) {
        super(context, j, iBinder, iFingerprintServiceReceiver, i2, i, z, str);
        this.mUnknownFingerprints = new ArrayList();
        this.mEnrolledList = list;
    }

    private void handleEnumeratedFingerprint(int i, int i2, int i3) {
        boolean z = false;
        int i4 = 0;
        while (true) {
            if (i4 >= this.mEnrolledList.size()) {
                break;
            }
            if (this.mEnrolledList.get(i4).getFingerId() != i) {
                i4++;
            } else {
                this.mEnrolledList.remove(i4);
                z = true;
                break;
            }
        }
        if (!z && i != 0) {
            this.mUnknownFingerprints.add(new Fingerprint(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i2, i, getHalDeviceId()));
        }
    }

    private void doFingerprintCleanup() {
        if (this.mEnrolledList == null) {
            return;
        }
        for (Fingerprint fingerprint : this.mEnrolledList) {
            Slog.e("FingerprintService", "Internal Enumerate: Removing dangling enrolled fingerprint: " + ((Object) fingerprint.getName()) + " " + fingerprint.getFingerId() + " " + fingerprint.getGroupId() + " " + fingerprint.getDeviceId());
            FingerprintUtils.getInstance().removeFingerprintIdForUser(getContext(), fingerprint.getFingerId(), getTargetUserId());
        }
        this.mEnrolledList.clear();
    }

    public List<Fingerprint> getUnknownFingerprints() {
        return this.mUnknownFingerprints;
    }

    @Override
    public boolean onEnumerationResult(int i, int i2, int i3) {
        handleEnumeratedFingerprint(i, i2, i3);
        if (i3 == 0) {
            doFingerprintCleanup();
        }
        return i3 == 0;
    }
}
