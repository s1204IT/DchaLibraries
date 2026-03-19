package com.android.server.telecom;

import com.android.server.telecom.DockManager;
import com.android.server.telecom.WiredHeadsetManager;
import com.android.server.telecom.bluetooth.BluetoothRouteManager;

public class CallAudioRoutePeripheralAdapter implements DockManager.Listener, WiredHeadsetManager.Listener, BluetoothRouteManager.BluetoothStateListener {
    private final BluetoothRouteManager mBluetoothRouteManager;
    private final CallAudioRouteStateMachine mCallAudioRouteStateMachine;

    public CallAudioRoutePeripheralAdapter(CallAudioRouteStateMachine callAudioRouteStateMachine, BluetoothRouteManager bluetoothRouteManager, WiredHeadsetManager wiredHeadsetManager, DockManager dockManager) {
        this.mCallAudioRouteStateMachine = callAudioRouteStateMachine;
        this.mBluetoothRouteManager = bluetoothRouteManager;
        this.mBluetoothRouteManager.setListener(this);
        wiredHeadsetManager.addListener(this);
        dockManager.addListener(this);
    }

    public boolean isBluetoothAudioOn() {
        return this.mBluetoothRouteManager.isBluetoothAudioConnectedOrPending();
    }

    @Override
    public void onBluetoothDeviceListChanged() {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(7);
    }

    @Override
    public void onBluetoothActiveDevicePresent() {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(8);
    }

    @Override
    public void onBluetoothActiveDeviceGone() {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(9);
    }

    @Override
    public void onBluetoothAudioConnected() {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1302);
    }

    @Override
    public void onBluetoothAudioDisconnected() {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1301);
    }

    @Override
    public void onWiredHeadsetPluggedInChanged(boolean z, boolean z2) {
        if (!z && z2) {
            this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(1);
        } else if (z && !z2) {
            this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(2);
        }
    }

    @Override
    public void onDockChanged(boolean z) {
        this.mCallAudioRouteStateMachine.sendMessageWithSessionInfo(z ? 5 : 6);
    }
}
