package com.android.server.oemlock;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.persistentdata.PersistentDataBlockManager;
import android.util.Slog;

class PersistentDataBlockLock extends OemLock {
    private static final String TAG = "OemLock";
    private Context mContext;

    PersistentDataBlockLock(Context context) {
        this.mContext = context;
    }

    @Override
    void setOemUnlockAllowedByCarrier(boolean z, byte[] bArr) {
        if (bArr != null) {
            Slog.w(TAG, "Signature provided but is not being used");
        }
        UserManager.get(this.mContext).setUserRestriction("no_oem_unlock", !z, UserHandle.SYSTEM);
        if (!z) {
            disallowUnlockIfNotUnlocked();
        }
    }

    @Override
    boolean isOemUnlockAllowedByCarrier() {
        return !UserManager.get(this.mContext).hasUserRestriction("no_oem_unlock", UserHandle.SYSTEM);
    }

    @Override
    void setOemUnlockAllowedByDevice(boolean z) {
        ((PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block")).setOemUnlockEnabled(z);
    }

    @Override
    boolean isOemUnlockAllowedByDevice() {
        return ((PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block")).getOemUnlockEnabled();
    }

    private void disallowUnlockIfNotUnlocked() {
        PersistentDataBlockManager persistentDataBlockManager = (PersistentDataBlockManager) this.mContext.getSystemService("persistent_data_block");
        if (persistentDataBlockManager.getFlashLockState() != 0) {
            persistentDataBlockManager.setOemUnlockEnabled(false);
        }
    }
}
