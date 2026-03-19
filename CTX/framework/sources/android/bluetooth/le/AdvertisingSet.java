package android.bluetooth.le;

import android.bluetooth.IBluetoothGatt;
import android.bluetooth.IBluetoothManager;
import android.os.RemoteException;
import android.util.Log;

public final class AdvertisingSet {
    private static final String TAG = "AdvertisingSet";
    private int mAdvertiserId;
    private final IBluetoothGatt mGatt;

    AdvertisingSet(int i, IBluetoothManager iBluetoothManager) {
        this.mAdvertiserId = i;
        try {
            this.mGatt = iBluetoothManager.getBluetoothGatt();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get Bluetooth gatt - ", e);
            throw new IllegalStateException("Failed to get Bluetooth");
        }
    }

    void setAdvertiserId(int i) {
        this.mAdvertiserId = i;
    }

    public void enableAdvertising(boolean z, int i, int i2) {
        try {
            this.mGatt.enableAdvertisingSet(this.mAdvertiserId, z, i, i2);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public void setAdvertisingData(AdvertiseData advertiseData) {
        try {
            this.mGatt.setAdvertisingData(this.mAdvertiserId, advertiseData);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public void setScanResponseData(AdvertiseData advertiseData) {
        try {
            this.mGatt.setScanResponseData(this.mAdvertiserId, advertiseData);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public void setAdvertisingParameters(AdvertisingSetParameters advertisingSetParameters) {
        try {
            this.mGatt.setAdvertisingParameters(this.mAdvertiserId, advertisingSetParameters);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public void setPeriodicAdvertisingParameters(PeriodicAdvertisingParameters periodicAdvertisingParameters) {
        try {
            this.mGatt.setPeriodicAdvertisingParameters(this.mAdvertiserId, periodicAdvertisingParameters);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public void setPeriodicAdvertisingData(AdvertiseData advertiseData) {
        try {
            this.mGatt.setPeriodicAdvertisingData(this.mAdvertiserId, advertiseData);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public void setPeriodicAdvertisingEnabled(boolean z) {
        try {
            this.mGatt.setPeriodicAdvertisingEnable(this.mAdvertiserId, z);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public void getOwnAddress() {
        try {
            this.mGatt.getOwnAddress(this.mAdvertiserId);
        } catch (RemoteException e) {
            Log.e(TAG, "remote exception - ", e);
        }
    }

    public int getAdvertiserId() {
        return this.mAdvertiserId;
    }
}
