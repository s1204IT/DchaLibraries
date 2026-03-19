package com.android.server.fingerprint;

import android.content.Context;
import android.hardware.fingerprint.Fingerprint;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import java.util.List;

public class FingerprintUtils {
    private static FingerprintUtils sInstance;
    private static final Object sInstanceLock = new Object();

    @GuardedBy("this")
    private final SparseArray<FingerprintsUserState> mUsers = new SparseArray<>();

    public static FingerprintUtils getInstance() {
        synchronized (sInstanceLock) {
            if (sInstance == null) {
                sInstance = new FingerprintUtils();
            }
        }
        return sInstance;
    }

    private FingerprintUtils() {
    }

    public List<Fingerprint> getFingerprintsForUser(Context context, int i) {
        return getStateForUser(context, i).getFingerprints();
    }

    public void addFingerprintForUser(Context context, int i, int i2) {
        getStateForUser(context, i2).addFingerprint(i, i2);
    }

    public void removeFingerprintIdForUser(Context context, int i, int i2) {
        getStateForUser(context, i2).removeFingerprint(i);
    }

    public void renameFingerprintForUser(Context context, int i, int i2, CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            return;
        }
        getStateForUser(context, i2).renameFingerprint(i, charSequence);
    }

    private FingerprintsUserState getStateForUser(Context context, int i) {
        FingerprintsUserState fingerprintsUserState;
        synchronized (this) {
            fingerprintsUserState = this.mUsers.get(i);
            if (fingerprintsUserState == null) {
                fingerprintsUserState = new FingerprintsUserState(context, i);
                this.mUsers.put(i, fingerprintsUserState);
            }
        }
        return fingerprintsUserState;
    }
}
