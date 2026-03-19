package com.android.server.oemlock;

import android.content.Context;
import android.hardware.oemlock.V1_0.IOemLock;
import android.os.RemoteException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.NoSuchElementException;

class VendorLock extends OemLock {
    private static final String TAG = "OemLock";
    private Context mContext;
    private IOemLock mOemLock;

    static IOemLock getOemLockHalService() {
        try {
            return IOemLock.getService();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (NoSuchElementException e2) {
            Slog.i(TAG, "OemLock HAL not present on device");
            return null;
        }
    }

    VendorLock(Context context, IOemLock iOemLock) {
        this.mContext = context;
        this.mOemLock = iOemLock;
    }

    @Override
    void setOemUnlockAllowedByCarrier(boolean z, byte[] bArr) {
        try {
            switch (this.mOemLock.setOemUnlockAllowedByCarrier(z, toByteArrayList(bArr))) {
                case 0:
                    Slog.i(TAG, "Updated carrier allows OEM lock state to: " + z);
                    return;
                case 1:
                    break;
                case 2:
                    throw new SecurityException("Invalid signature used in attempt to carrier unlock");
                default:
                    Slog.e(TAG, "Unknown return value indicates code is out of sync with HAL");
                    break;
            }
            throw new RuntimeException("Failed to set carrier OEM unlock state");
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set carrier state with HAL", e);
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    boolean isOemUnlockAllowedByCarrier() {
        final Integer[] numArr = new Integer[1];
        final Boolean[] boolArr = new Boolean[1];
        try {
            this.mOemLock.isOemUnlockAllowedByCarrier(new IOemLock.isOemUnlockAllowedByCarrierCallback() {
                @Override
                public final void onValues(int i, boolean z) {
                    VendorLock.lambda$isOemUnlockAllowedByCarrier$0(numArr, boolArr, i, z);
                }
            });
            switch (numArr[0].intValue()) {
                case 0:
                    return boolArr[0].booleanValue();
                case 1:
                    break;
                default:
                    Slog.e(TAG, "Unknown return value indicates code is out of sync with HAL");
                    break;
            }
            throw new RuntimeException("Failed to get carrier OEM unlock state");
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get carrier state from HAL");
            throw e.rethrowFromSystemServer();
        }
    }

    static void lambda$isOemUnlockAllowedByCarrier$0(Integer[] numArr, Boolean[] boolArr, int i, boolean z) {
        numArr[0] = Integer.valueOf(i);
        boolArr[0] = Boolean.valueOf(z);
    }

    @Override
    void setOemUnlockAllowedByDevice(boolean z) {
        try {
            switch (this.mOemLock.setOemUnlockAllowedByDevice(z)) {
                case 0:
                    Slog.i(TAG, "Updated device allows OEM lock state to: " + z);
                    return;
                case 1:
                    break;
                default:
                    Slog.e(TAG, "Unknown return value indicates code is out of sync with HAL");
                    break;
            }
            throw new RuntimeException("Failed to set device OEM unlock state");
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to set device state with HAL", e);
            throw e.rethrowFromSystemServer();
        }
    }

    @Override
    boolean isOemUnlockAllowedByDevice() {
        final Integer[] numArr = new Integer[1];
        final Boolean[] boolArr = new Boolean[1];
        try {
            this.mOemLock.isOemUnlockAllowedByDevice(new IOemLock.isOemUnlockAllowedByDeviceCallback() {
                @Override
                public final void onValues(int i, boolean z) {
                    VendorLock.lambda$isOemUnlockAllowedByDevice$1(numArr, boolArr, i, z);
                }
            });
            switch (numArr[0].intValue()) {
                case 0:
                    return boolArr[0].booleanValue();
                case 1:
                    break;
                default:
                    Slog.e(TAG, "Unknown return value indicates code is out of sync with HAL");
                    break;
            }
            throw new RuntimeException("Failed to get device OEM unlock state");
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to get devie state from HAL");
            throw e.rethrowFromSystemServer();
        }
    }

    static void lambda$isOemUnlockAllowedByDevice$1(Integer[] numArr, Boolean[] boolArr, int i, boolean z) {
        numArr[0] = Integer.valueOf(i);
        boolArr[0] = Boolean.valueOf(z);
    }

    private ArrayList toByteArrayList(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList(bArr.length);
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
        return arrayList;
    }
}
