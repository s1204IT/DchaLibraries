package com.android.systemui.statusbar.car;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import com.android.systemui.statusbar.policy.BatteryController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class CarBatteryController extends BroadcastReceiver implements BatteryController {
    private BatteryViewHandler mBatteryViewHandler;
    private BluetoothHeadsetClient mBluetoothHeadsetClient;
    private final Context mContext;
    private int mLevel;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final ArrayList<BatteryController.BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private final BluetoothProfile.ServiceListener mHfpServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
            if (i == 16) {
                CarBatteryController.this.mBluetoothHeadsetClient = (BluetoothHeadsetClient) bluetoothProfile;
            }
        }

        @Override
        public void onServiceDisconnected(int i) {
            if (i == 16) {
                CarBatteryController.this.mBluetoothHeadsetClient = null;
            }
        }
    };

    public interface BatteryViewHandler {
        void hideBatteryView();

        void showBatteryView();
    }

    public CarBatteryController(Context context) {
        this.mContext = context;
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.getProfileProxy(context.getApplicationContext(), this.mHfpServiceListener, 16);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("CarBatteryController state:");
        printWriter.print("    mLevel=");
        printWriter.println(this.mLevel);
    }

    @Override
    public void setPowerSaveMode(boolean z) {
    }

    @Override
    public void addCallback(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        this.mChangeCallbacks.add(batteryStateChangeCallback);
        batteryStateChangeCallback.onBatteryLevelChanged(this.mLevel, false, false);
        batteryStateChangeCallback.onPowerSaveChanged(false);
    }

    @Override
    public void removeCallback(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        this.mChangeCallbacks.remove(batteryStateChangeCallback);
    }

    public void addBatteryViewHandler(BatteryViewHandler batteryViewHandler) {
        this.mBatteryViewHandler = batteryViewHandler;
    }

    public void startListening() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headsetclient.profile.action.AG_EVENT");
        this.mContext.registerReceiver(this, intentFilter);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Log.isLoggable("CarBatteryController", 3)) {
            Log.d("CarBatteryController", "onReceive(). action: " + action);
        }
        if ("android.bluetooth.headsetclient.profile.action.AG_EVENT".equals(action)) {
            if (Log.isLoggable("CarBatteryController", 3)) {
                Log.d("CarBatteryController", "Received ACTION_AG_EVENT");
            }
            int intExtra = intent.getIntExtra("android.bluetooth.headsetclient.extra.BATTERY_LEVEL", -1);
            updateBatteryLevel(intExtra);
            if (intExtra != -1 && this.mBatteryViewHandler != null) {
                this.mBatteryViewHandler.showBatteryView();
                return;
            }
            return;
        }
        if ("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            int intExtra2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
            if (Log.isLoggable("CarBatteryController", 3)) {
                Log.d("CarBatteryController", "ACTION_CONNECTION_STATE_CHANGED event: " + intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1) + " -> " + intExtra2);
            }
            updateBatteryIcon((BluetoothDevice) intent.getExtra("android.bluetooth.device.extra.DEVICE"), intExtra2);
        }
    }

    private void updateBatteryLevel(int i) {
        if (i == -1) {
            if (Log.isLoggable("CarBatteryController", 3)) {
                Log.d("CarBatteryController", "Battery level invalid. Ignoring.");
                return;
            }
            return;
        }
        switch (i) {
            case 1:
                this.mLevel = 12;
                break;
            case 2:
                this.mLevel = 28;
                break;
            case 3:
                this.mLevel = 63;
                break;
            case 4:
                this.mLevel = 87;
                break;
            case 5:
                this.mLevel = 100;
                break;
            default:
                this.mLevel = 0;
                break;
        }
        if (Log.isLoggable("CarBatteryController", 3)) {
            Log.d("CarBatteryController", "Battery level: " + i + "; setting mLevel as: " + this.mLevel);
        }
        notifyBatteryLevelChanged();
    }

    private void updateBatteryIcon(BluetoothDevice bluetoothDevice, int i) {
        Bundle currentAgEvents;
        if (i == 2) {
            if (Log.isLoggable("CarBatteryController", 3)) {
                Log.d("CarBatteryController", "Device connected");
            }
            if (this.mBatteryViewHandler != null) {
                this.mBatteryViewHandler.showBatteryView();
            }
            if (this.mBluetoothHeadsetClient == null || bluetoothDevice == null || (currentAgEvents = this.mBluetoothHeadsetClient.getCurrentAgEvents(bluetoothDevice)) == null) {
                return;
            }
            updateBatteryLevel(currentAgEvents.getInt("android.bluetooth.headsetclient.extra.BATTERY_LEVEL", -1));
            return;
        }
        if (i == 0) {
            if (Log.isLoggable("CarBatteryController", 3)) {
                Log.d("CarBatteryController", "Device disconnected");
            }
            if (this.mBatteryViewHandler != null) {
                this.mBatteryViewHandler.hideBatteryView();
            }
        }
    }

    @Override
    public void dispatchDemoCommand(String str, Bundle bundle) {
    }

    @Override
    public boolean isPowerSave() {
        return false;
    }

    private void notifyBatteryLevelChanged() {
        int size = this.mChangeCallbacks.size();
        for (int i = 0; i < size; i++) {
            this.mChangeCallbacks.get(i).onBatteryLevelChanged(this.mLevel, false, false);
        }
    }
}
