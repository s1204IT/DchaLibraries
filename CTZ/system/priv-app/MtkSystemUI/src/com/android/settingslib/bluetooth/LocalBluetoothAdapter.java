package com.android.settingslib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.os.ParcelUuid;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class LocalBluetoothAdapter {
    private static LocalBluetoothAdapter sInstance;
    private final BluetoothAdapter mAdapter;
    private LocalBluetoothProfileManager mProfileManager;
    private int mState = Integer.MIN_VALUE;

    private LocalBluetoothAdapter(BluetoothAdapter bluetoothAdapter) {
        this.mAdapter = bluetoothAdapter;
    }

    void setProfileManager(LocalBluetoothProfileManager localBluetoothProfileManager) {
        this.mProfileManager = localBluetoothProfileManager;
    }

    static synchronized LocalBluetoothAdapter getInstance() {
        BluetoothAdapter defaultAdapter;
        if (sInstance == null && (defaultAdapter = BluetoothAdapter.getDefaultAdapter()) != null) {
            sInstance = new LocalBluetoothAdapter(defaultAdapter);
        }
        return sInstance;
    }

    public void cancelDiscovery() {
        this.mAdapter.cancelDiscovery();
    }

    public boolean enable() {
        return this.mAdapter.enable();
    }

    void getProfileProxy(Context context, BluetoothProfile.ServiceListener serviceListener, int i) {
        this.mAdapter.getProfileProxy(context, serviceListener, i);
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return this.mAdapter.getBondedDevices();
    }

    public BluetoothLeScanner getBluetoothLeScanner() {
        return this.mAdapter.getBluetoothLeScanner();
    }

    public int getState() {
        return this.mAdapter.getState();
    }

    public ParcelUuid[] getUuids() {
        return this.mAdapter.getUuids();
    }

    public boolean isDiscovering() {
        return this.mAdapter.isDiscovering();
    }

    public int getConnectionState() {
        return this.mAdapter.getConnectionState();
    }

    public synchronized int getBluetoothState() {
        syncBluetoothState();
        return this.mState;
    }

    void setBluetoothStateInt(int i) {
        synchronized (this) {
            if (this.mState == i) {
                return;
            }
            this.mState = i;
            if (i == 12 && this.mProfileManager != null) {
                this.mProfileManager.setBluetoothStateOn();
            }
        }
    }

    boolean syncBluetoothState() {
        if (this.mAdapter.getState() != this.mState) {
            setBluetoothStateInt(this.mAdapter.getState());
            return true;
        }
        return false;
    }

    public boolean setBluetoothEnabled(boolean z) {
        boolean zDisable;
        int i;
        if (z) {
            zDisable = this.mAdapter.enable();
        } else {
            zDisable = this.mAdapter.disable();
        }
        if (zDisable) {
            if (z) {
                i = 11;
            } else {
                i = 13;
            }
            setBluetoothStateInt(i);
        } else {
            syncBluetoothState();
        }
        return zDisable;
    }

    public int getMaxConnectedAudioDevices() {
        return this.mAdapter.getMaxConnectedAudioDevices();
    }

    public List<Integer> getSupportedProfiles() {
        return this.mAdapter.getSupportedProfiles();
    }
}
