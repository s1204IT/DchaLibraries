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
    public void onBootPhase(int phase) {
        if (phase == 500) {
            publishBinderService("bluetooth_manager", this.mBluetoothManagerService);
        } else {
            if (phase != 1000) {
                return;
            }
            this.mBluetoothManagerService.handleOnBootPhase();
        }
    }

    @Override
    public void onSwitchUser(int userHandle) {
        this.mBluetoothManagerService.handleOnSwitchUser(userHandle);
    }

    @Override
    public void onUnlockUser(int userHandle) {
        this.mBluetoothManagerService.handleOnUnlockUser(userHandle);
    }
}
