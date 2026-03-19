package com.android.server;

import android.content.Context;

class BluetoothService extends SystemService {
    private BluetoothManagerService mBluetoothManagerService;

    public BluetoothService(Context context) {
        super(context);
        this.mBluetoothManagerService = new BluetoothManagerService(context);
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            publishBinderService("bluetooth_manager", this.mBluetoothManagerService);
        } else if (i == 1000) {
            this.mBluetoothManagerService.handleOnBootPhase();
        }
    }

    @Override
    public void onSwitchUser(int i) {
        this.mBluetoothManagerService.handleOnSwitchUser(i);
    }

    @Override
    public void onUnlockUser(int i) {
        this.mBluetoothManagerService.handleOnUnlockUser(i);
    }
}
