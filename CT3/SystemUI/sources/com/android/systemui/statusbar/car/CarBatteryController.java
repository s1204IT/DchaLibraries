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
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile != 16) {
                return;
            }
            CarBatteryController.this.mBluetoothHeadsetClient = (BluetoothHeadsetClient) proxy;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile != 16) {
                return;
            }
            CarBatteryController.this.mBluetoothHeadsetClient = null;
        }
    };

    public interface BatteryViewHandler {
        void hideBatteryView();

        void showBatteryView();
    }

    public CarBatteryController(Context context) {
        this.mContext = context;
        this.mAdapter.getProfileProxy(context.getApplicationContext(), this.mHfpServiceListener, 16);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CarBatteryController state:");
        pw.print("    mLevel=");
        pw.println(this.mLevel);
    }

    @Override
    public void setPowerSaveMode(boolean powerSave) {
    }

    @Override
    public void addStateChangedCallback(BatteryController.BatteryStateChangeCallback cb) {
        this.mChangeCallbacks.add(cb);
        cb.onBatteryLevelChanged(this.mLevel, false, false);
        cb.onPowerSaveChanged(false);
    }

    @Override
    public void removeStateChangedCallback(BatteryController.BatteryStateChangeCallback cb) {
        this.mChangeCallbacks.remove(cb);
    }

    public void addBatteryViewHandler(BatteryViewHandler batteryViewHandler) {
        this.mBatteryViewHandler = batteryViewHandler;
    }

    public void startListening() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.bluetooth.headsetclient.profile.action.AG_EVENT");
        this.mContext.registerReceiver(this, filter);
    }

    public void stopListening() {
        this.mContext.unregisterReceiver(this);
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
            int batteryLevel = intent.getIntExtra("android.bluetooth.headsetclient.extra.BATTERY_LEVEL", -1);
            updateBatteryLevel(batteryLevel);
            if (batteryLevel == -1 || this.mBatteryViewHandler == null) {
                return;
            }
            this.mBatteryViewHandler.showBatteryView();
            return;
        }
        if (!"android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED".equals(action)) {
            return;
        }
        int newState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
        if (Log.isLoggable("CarBatteryController", 3)) {
            int oldState = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1);
            Log.d("CarBatteryController", "ACTION_CONNECTION_STATE_CHANGED event: " + oldState + " -> " + newState);
        }
        BluetoothDevice device = (BluetoothDevice) intent.getExtra("android.bluetooth.device.extra.DEVICE");
        updateBatteryIcon(device, newState);
    }

    private void updateBatteryLevel(int batteryLevel) {
        if (batteryLevel == -1) {
            if (Log.isLoggable("CarBatteryController", 3)) {
                Log.d("CarBatteryController", "Battery level invalid. Ignoring.");
                return;
            }
            return;
        }
        switch (batteryLevel) {
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
            Log.d("CarBatteryController", "Battery level: " + batteryLevel + "; setting mLevel as: " + this.mLevel);
        }
        notifyBatteryLevelChanged();
    }

    private void updateBatteryIcon(BluetoothDevice device, int newState) {
        Bundle featuresBundle;
        if (newState == 2) {
            if (Log.isLoggable("CarBatteryController", 3)) {
                Log.d("CarBatteryController", "Device connected");
            }
            if (this.mBatteryViewHandler != null) {
                this.mBatteryViewHandler.showBatteryView();
            }
            if (this.mBluetoothHeadsetClient == null || device == null || (featuresBundle = this.mBluetoothHeadsetClient.getCurrentAgEvents(device)) == null) {
                return;
            }
            int batteryLevel = featuresBundle.getInt("android.bluetooth.headsetclient.extra.BATTERY_LEVEL", -1);
            updateBatteryLevel(batteryLevel);
            return;
        }
        if (newState != 0) {
            return;
        }
        if (Log.isLoggable("CarBatteryController", 3)) {
            Log.d("CarBatteryController", "Device disconnected");
        }
        if (this.mBatteryViewHandler == null) {
            return;
        }
        this.mBatteryViewHandler.hideBatteryView();
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
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
