package com.android.bluetooth.btservice;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.a2dp.A2dpService;
import com.android.bluetooth.hearingaid.HearingAidService;
import com.android.bluetooth.hfp.HeadsetService;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class ActiveDeviceManager {
    private static final boolean DBG = true;
    private static final int MESSAGE_A2DP_ACTION_ACTIVE_DEVICE_CHANGED = 3;
    private static final int MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED = 2;
    private static final int MESSAGE_ADAPTER_ACTION_STATE_CHANGED = 1;
    private static final int MESSAGE_HEARING_AID_ACTION_ACTIVE_DEVICE_CHANGED = 6;
    private static final int MESSAGE_HFP_ACTION_ACTIVE_DEVICE_CHANGED = 5;
    private static final int MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED = 4;
    private static final String TAG = "BluetoothActiveDeviceManager";
    private final AdapterService mAdapterService;
    private final AudioManager mAudioManager;
    private final ServiceFactory mFactory;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    private final List<BluetoothDevice> mA2dpConnectedDevices = new LinkedList();
    private final List<BluetoothDevice> mHfpConnectedDevices = new LinkedList();
    private BluetoothDevice mA2dpActiveDevice = null;
    private BluetoothDevice mHfpActiveDevice = null;
    private BluetoothDevice mHearingAidActiveDevice = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                Log.e(ActiveDeviceManager.TAG, "Received intent with null action");
            }
            switch (action) {
                case "android.bluetooth.adapter.action.STATE_CHANGED":
                    ActiveDeviceManager.this.mHandler.obtainMessage(1, intent).sendToTarget();
                    break;
                case "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED":
                    ActiveDeviceManager.this.mHandler.obtainMessage(2, intent).sendToTarget();
                    break;
                case "android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED":
                    ActiveDeviceManager.this.mHandler.obtainMessage(3, intent).sendToTarget();
                    break;
                case "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED":
                    ActiveDeviceManager.this.mHandler.obtainMessage(4, intent).sendToTarget();
                    break;
                case "android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED":
                    ActiveDeviceManager.this.mHandler.obtainMessage(5, intent).sendToTarget();
                    break;
                case "android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED":
                    ActiveDeviceManager.this.mHandler.obtainMessage(6, intent).sendToTarget();
                    break;
                default:
                    Log.e(ActiveDeviceManager.TAG, "Received unexpected intent, action=" + action);
                    break;
            }
        }
    };
    private final AudioManagerAudioDeviceCallback mAudioManagerAudioDeviceCallback = new AudioManagerAudioDeviceCallback();

    class ActiveDeviceManagerHandler extends Handler {
        ActiveDeviceManagerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    int intExtra = ((Intent) message.obj).getIntExtra("android.bluetooth.adapter.extra.STATE", -1);
                    Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_ADAPTER_ACTION_STATE_CHANGED): newState=" + intExtra);
                    if (intExtra == 12) {
                        ActiveDeviceManager.this.resetState();
                    }
                    break;
                case 2:
                    Intent intent = (Intent) message.obj;
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    int intExtra2 = intent.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1);
                    int intExtra3 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                    if (intExtra2 != intExtra3) {
                        if (intExtra3 == 2) {
                            Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED): device " + bluetoothDevice + " connected");
                            if (!ActiveDeviceManager.this.mA2dpConnectedDevices.contains(bluetoothDevice)) {
                                ActiveDeviceManager.this.mA2dpConnectedDevices.add(bluetoothDevice);
                                if (ActiveDeviceManager.this.mHearingAidActiveDevice == null) {
                                    ActiveDeviceManager.this.setA2dpActiveDevice(bluetoothDevice);
                                }
                                break;
                            }
                        } else {
                            if (intExtra2 == 2) {
                                Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_A2DP_ACTION_CONNECTION_STATE_CHANGED): device " + bluetoothDevice + " disconnected");
                                ActiveDeviceManager.this.mA2dpConnectedDevices.remove(bluetoothDevice);
                                if (Objects.equals(ActiveDeviceManager.this.mA2dpActiveDevice, bluetoothDevice)) {
                                    ActiveDeviceManager.this.setA2dpActiveDevice(null);
                                }
                            }
                            break;
                        }
                    }
                    break;
                case 3:
                    BluetoothDevice bluetoothDevice2 = (BluetoothDevice) ((Intent) message.obj).getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_A2DP_ACTION_ACTIVE_DEVICE_CHANGED): device= " + bluetoothDevice2);
                    if (bluetoothDevice2 != null && !Objects.equals(ActiveDeviceManager.this.mA2dpActiveDevice, bluetoothDevice2)) {
                        ActiveDeviceManager.this.setHearingAidActiveDevice(null);
                    }
                    ActiveDeviceManager.this.mA2dpActiveDevice = bluetoothDevice2;
                    break;
                case 4:
                    Intent intent2 = (Intent) message.obj;
                    BluetoothDevice bluetoothDevice3 = (BluetoothDevice) intent2.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    int intExtra4 = intent2.getIntExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", -1);
                    int intExtra5 = intent2.getIntExtra("android.bluetooth.profile.extra.STATE", -1);
                    if (intExtra4 != intExtra5) {
                        if (intExtra5 == 2) {
                            Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED): device " + bluetoothDevice3 + " connected");
                            if (!ActiveDeviceManager.this.mHfpConnectedDevices.contains(bluetoothDevice3)) {
                                ActiveDeviceManager.this.mHfpConnectedDevices.add(bluetoothDevice3);
                                if (ActiveDeviceManager.this.mHearingAidActiveDevice == null) {
                                    ActiveDeviceManager.this.setHfpActiveDevice(bluetoothDevice3);
                                }
                                break;
                            }
                        } else {
                            if (intExtra4 == 2) {
                                Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_HFP_ACTION_CONNECTION_STATE_CHANGED): device " + bluetoothDevice3 + " disconnected");
                                ActiveDeviceManager.this.mHfpConnectedDevices.remove(bluetoothDevice3);
                                if (Objects.equals(ActiveDeviceManager.this.mHfpActiveDevice, bluetoothDevice3)) {
                                    ActiveDeviceManager.this.setHfpActiveDevice(null);
                                }
                            }
                            break;
                        }
                    }
                    break;
                case 5:
                    BluetoothDevice bluetoothDevice4 = (BluetoothDevice) ((Intent) message.obj).getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_HFP_ACTION_ACTIVE_DEVICE_CHANGED): device= " + bluetoothDevice4);
                    if (bluetoothDevice4 != null && !Objects.equals(ActiveDeviceManager.this.mHfpActiveDevice, bluetoothDevice4)) {
                        ActiveDeviceManager.this.setHearingAidActiveDevice(null);
                    }
                    ActiveDeviceManager.this.mHfpActiveDevice = bluetoothDevice4;
                    break;
                case 6:
                    BluetoothDevice bluetoothDevice5 = (BluetoothDevice) ((Intent) message.obj).getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.d(ActiveDeviceManager.TAG, "handleMessage(MESSAGE_HA_ACTION_ACTIVE_DEVICE_CHANGED): device= " + bluetoothDevice5);
                    ActiveDeviceManager.this.mHearingAidActiveDevice = bluetoothDevice5;
                    if (bluetoothDevice5 != null) {
                        ActiveDeviceManager.this.setA2dpActiveDevice(null);
                        ActiveDeviceManager.this.setHfpActiveDevice(null);
                    }
                    break;
            }
        }
    }

    private class AudioManagerAudioDeviceCallback extends AudioDeviceCallback {
        private AudioManagerAudioDeviceCallback() {
        }

        private boolean isWiredAudioHeadset(AudioDeviceInfo audioDeviceInfo) {
            int type = audioDeviceInfo.getType();
            if (type != 22) {
                switch (type) {
                    case 3:
                    case 4:
                        return true;
                    default:
                        return false;
                }
            }
            return true;
        }

        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] audioDeviceInfoArr) {
            Log.d(ActiveDeviceManager.TAG, "onAudioDevicesAdded");
            int length = audioDeviceInfoArr.length;
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                AudioDeviceInfo audioDeviceInfo = audioDeviceInfoArr[i];
                Log.d(ActiveDeviceManager.TAG, "Audio device added: " + ((Object) audioDeviceInfo.getProductName()) + " type: " + audioDeviceInfo.getType());
                if (!isWiredAudioHeadset(audioDeviceInfo)) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                ActiveDeviceManager.this.wiredAudioDeviceConnected();
            }
        }

        @Override
        public void onAudioDevicesRemoved(AudioDeviceInfo[] audioDeviceInfoArr) {
        }
    }

    ActiveDeviceManager(AdapterService adapterService, ServiceFactory serviceFactory) {
        this.mAdapterService = adapterService;
        this.mFactory = serviceFactory;
        this.mAudioManager = (AudioManager) adapterService.getSystemService("audio");
    }

    void start() {
        Log.d(TAG, "start()");
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mHandler = new ActiveDeviceManagerHandler(this.mHandlerThread.getLooper());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.headset.profile.action.ACTIVE_DEVICE_CHANGED");
        intentFilter.addAction("android.bluetooth.hearingaid.profile.action.ACTIVE_DEVICE_CHANGED");
        this.mAdapterService.registerReceiver(this.mReceiver, intentFilter);
        this.mAudioManager.registerAudioDeviceCallback(this.mAudioManagerAudioDeviceCallback, this.mHandler);
    }

    void cleanup() {
        Log.d(TAG, "cleanup()");
        this.mAudioManager.unregisterAudioDeviceCallback(this.mAudioManagerAudioDeviceCallback);
        this.mAdapterService.unregisterReceiver(this.mReceiver);
        if (this.mHandlerThread != null) {
            this.mHandlerThread.quit();
            this.mHandlerThread = null;
        }
        resetState();
    }

    @VisibleForTesting
    public Looper getHandlerLooper() {
        if (this.mHandlerThread == null) {
            return null;
        }
        return this.mHandlerThread.getLooper();
    }

    private void setA2dpActiveDevice(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "setA2dpActiveDevice(" + bluetoothDevice + ")");
        A2dpService a2dpService = this.mFactory.getA2dpService();
        if (a2dpService == null || !a2dpService.setActiveDevice(bluetoothDevice)) {
            return;
        }
        this.mA2dpActiveDevice = bluetoothDevice;
    }

    private void setHfpActiveDevice(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "setHfpActiveDevice(" + bluetoothDevice + ")");
        HeadsetService headsetService = this.mFactory.getHeadsetService();
        if (headsetService == null || !headsetService.setActiveDevice(bluetoothDevice)) {
            return;
        }
        this.mHfpActiveDevice = bluetoothDevice;
    }

    private void setHearingAidActiveDevice(BluetoothDevice bluetoothDevice) {
        Log.d(TAG, "setHearingAidActiveDevice(" + bluetoothDevice + ")");
        HearingAidService hearingAidService = this.mFactory.getHearingAidService();
        if (hearingAidService == null || !hearingAidService.setActiveDevice(bluetoothDevice)) {
            return;
        }
        this.mHearingAidActiveDevice = bluetoothDevice;
    }

    private void resetState() {
        this.mA2dpConnectedDevices.clear();
        this.mA2dpActiveDevice = null;
        this.mHfpConnectedDevices.clear();
        this.mHfpActiveDevice = null;
        this.mHearingAidActiveDevice = null;
    }

    @VisibleForTesting
    BroadcastReceiver getBroadcastReceiver() {
        return this.mReceiver;
    }

    @VisibleForTesting
    BluetoothDevice getA2dpActiveDevice() {
        return this.mA2dpActiveDevice;
    }

    @VisibleForTesting
    BluetoothDevice getHfpActiveDevice() {
        return this.mHfpActiveDevice;
    }

    @VisibleForTesting
    BluetoothDevice getHearingAidActiveDevice() {
        return this.mHearingAidActiveDevice;
    }

    @VisibleForTesting
    void wiredAudioDeviceConnected() {
        Log.d(TAG, "wiredAudioDeviceConnected");
        setA2dpActiveDevice(null);
        setHfpActiveDevice(null);
        setHearingAidActiveDevice(null);
    }
}
