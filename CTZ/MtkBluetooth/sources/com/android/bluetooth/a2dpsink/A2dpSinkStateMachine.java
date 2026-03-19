package com.android.bluetooth.a2dpsink;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAudioConfig;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.avrcpcontroller.AvrcpControllerService;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class A2dpSinkStateMachine extends StateMachine {
    static final int AUDIO_STATE_REMOTE_SUSPEND = 0;
    static final int AUDIO_STATE_STARTED = 2;
    static final int AUDIO_STATE_STOPPED = 1;
    public static final int AVRC_ID_PAUSE = 70;
    public static final int AVRC_ID_PLAY = 68;
    static final int CONNECT = 1;
    static final int CONNECTION_STATE_CONNECTED = 2;
    static final int CONNECTION_STATE_CONNECTING = 1;
    static final int CONNECTION_STATE_DISCONNECTED = 0;
    static final int CONNECTION_STATE_DISCONNECTING = 3;
    private static final int CONNECT_TIMEOUT = 201;
    private static final boolean DBG = false;
    static final int DISCONNECT = 2;
    public static final int EVENT_AVRCP_CT_PAUSE = 302;
    public static final int EVENT_AVRCP_CT_PLAY = 301;
    public static final int EVENT_AVRCP_TG_PAUSE = 304;
    public static final int EVENT_AVRCP_TG_PLAY = 303;
    public static final int EVENT_REQUEST_FOCUS = 305;
    private static final int EVENT_TYPE_AUDIO_CONFIG_CHANGED = 3;
    private static final int EVENT_TYPE_AUDIO_STATE_CHANGED = 2;
    private static final int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    private static final int EVENT_TYPE_NONE = 0;
    private static final int IS_INVALID_DEVICE = 0;
    private static final int IS_VALID_DEVICE = 1;
    public static final int KEY_STATE_PRESSED = 0;
    public static final int KEY_STATE_RELEASED = 1;
    private static final int MSG_CONNECTION_STATE_CHANGED = 0;
    private static final int STACK_EVENT = 101;
    private BluetoothAdapter mAdapter;
    private final HashMap<BluetoothDevice, BluetoothAudioConfig> mAudioConfigs;
    private Connected mConnected;
    private Context mContext;
    private BluetoothDevice mCurrentDevice;
    private Disconnected mDisconnected;
    private BluetoothDevice mIncomingDevice;
    private IntentBroadcastHandler mIntentBroadcastHandler;
    private final Object mLockForPatch;
    private Pending mPending;
    private BluetoothDevice mPlayingDevice;
    private A2dpSinkService mService;
    private A2dpSinkStreamHandler mStreaming;
    private BluetoothDevice mTargetDevice;

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean connectA2dpNative(byte[] bArr);

    private native boolean disconnectA2dpNative(byte[] bArr);

    private native void initNative();

    public native void informAudioFocusStateNative(int i);

    public native void informAudioTrackGainNative(float f);

    static {
        classInitNative();
    }

    private A2dpSinkStateMachine(A2dpSinkService a2dpSinkService, Context context) {
        super("A2dpSinkStateMachine");
        this.mLockForPatch = new Object();
        this.mCurrentDevice = null;
        this.mTargetDevice = null;
        this.mIncomingDevice = null;
        this.mPlayingDevice = null;
        this.mStreaming = null;
        this.mAudioConfigs = new HashMap<>();
        this.mService = a2dpSinkService;
        this.mContext = context;
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        initNative();
        this.mDisconnected = new Disconnected();
        this.mPending = new Pending();
        this.mConnected = new Connected();
        addState(this.mDisconnected);
        addState(this.mPending);
        addState(this.mConnected);
        setInitialState(this.mDisconnected);
        this.mIntentBroadcastHandler = new IntentBroadcastHandler();
    }

    static A2dpSinkStateMachine make(A2dpSinkService a2dpSinkService, Context context) {
        Log.d("A2dpSinkStateMachine", "make");
        A2dpSinkStateMachine a2dpSinkStateMachine = new A2dpSinkStateMachine(a2dpSinkService, context);
        a2dpSinkStateMachine.start();
        return a2dpSinkStateMachine;
    }

    public void doQuit() {
        synchronized (this) {
            this.mStreaming = null;
        }
        quitNow();
    }

    public void cleanup() {
        cleanupNative();
        this.mAudioConfigs.clear();
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mCurrentDevice: " + this.mCurrentDevice);
        ProfileService.println(sb, "mTargetDevice: " + this.mTargetDevice);
        ProfileService.println(sb, "mIncomingDevice: " + this.mIncomingDevice);
        ProfileService.println(sb, "StateMachine: " + toString());
    }

    private class Disconnected extends State {
        private Disconnected() {
        }

        public void enter() {
            A2dpSinkStateMachine.this.log("Enter Disconnected: " + A2dpSinkStateMachine.this.getCurrentMessage().what);
        }

        public boolean processMessage(Message message) {
            A2dpSinkStateMachine.this.log("Disconnected process message: " + message.what);
            if (A2dpSinkStateMachine.this.mCurrentDevice != null || A2dpSinkStateMachine.this.mTargetDevice != null || A2dpSinkStateMachine.this.mIncomingDevice != null) {
                A2dpSinkStateMachine.this.loge("ERROR: current, target, or mIncomingDevice not null in Disconnected");
                return false;
            }
            int i = message.what;
            if (i != A2dpSinkStateMachine.STACK_EVENT) {
                switch (i) {
                    case 1:
                        BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                        A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 1, 0);
                        if (!A2dpSinkStateMachine.this.connectA2dpNative(A2dpSinkStateMachine.this.getByteAddress(bluetoothDevice))) {
                            A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 0, 1);
                            break;
                        } else {
                            synchronized (A2dpSinkStateMachine.this) {
                                A2dpSinkStateMachine.this.mTargetDevice = bluetoothDevice;
                                A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mPending);
                                break;
                            }
                            A2dpSinkStateMachine.this.sendMessageDelayed(A2dpSinkStateMachine.CONNECT_TIMEOUT, 30000L);
                            break;
                        }
                    case 2:
                        return true;
                    default:
                        return false;
                }
            } else {
                StackEvent stackEvent = (StackEvent) message.obj;
                int i2 = stackEvent.type;
                if (i2 == 1) {
                    processConnectionEvent(stackEvent.device, stackEvent.valueInt);
                } else if (i2 == 3) {
                    A2dpSinkStateMachine.this.processAudioConfigEvent(stackEvent.device, stackEvent.audioConfig);
                } else {
                    A2dpSinkStateMachine.this.loge("Unexpected stack event: " + stackEvent.type);
                }
            }
            return true;
        }

        public void exit() {
            A2dpSinkStateMachine.this.log("Exit Disconnected: " + A2dpSinkStateMachine.this.getCurrentMessage().what);
        }

        private void processConnectionEvent(BluetoothDevice bluetoothDevice, int i) {
            switch (i) {
                case 0:
                    A2dpSinkStateMachine.this.logw("Ignore A2DP DISCONNECTED event, device: " + bluetoothDevice);
                    return;
                case 1:
                    if (A2dpSinkStateMachine.this.okToConnect(bluetoothDevice)) {
                        A2dpSinkStateMachine.this.logi("Incoming A2DP accepted");
                        A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 1, 0);
                        synchronized (A2dpSinkStateMachine.this) {
                            A2dpSinkStateMachine.this.mIncomingDevice = bluetoothDevice;
                            A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mPending);
                            break;
                        }
                        return;
                    }
                    A2dpSinkStateMachine.this.logi("Incoming A2DP rejected");
                    A2dpSinkStateMachine.this.disconnectA2dpNative(A2dpSinkStateMachine.this.getByteAddress(bluetoothDevice));
                    return;
                case 2:
                    A2dpSinkStateMachine.this.logw("A2DP Connected from Disconnected state");
                    if (A2dpSinkStateMachine.this.okToConnect(bluetoothDevice)) {
                        A2dpSinkStateMachine.this.logi("Incoming A2DP accepted");
                        A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 2, 0);
                        synchronized (A2dpSinkStateMachine.this) {
                            A2dpSinkStateMachine.this.mCurrentDevice = bluetoothDevice;
                            A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mConnected);
                            break;
                        }
                        return;
                    }
                    A2dpSinkStateMachine.this.logi("Incoming A2DP rejected");
                    A2dpSinkStateMachine.this.disconnectA2dpNative(A2dpSinkStateMachine.this.getByteAddress(bluetoothDevice));
                    return;
                case 3:
                    A2dpSinkStateMachine.this.logw("Ignore HF DISCONNECTING event, device: " + bluetoothDevice);
                    return;
                default:
                    A2dpSinkStateMachine.this.loge("Incorrect state: " + i);
                    return;
            }
        }
    }

    private class Pending extends State {
        private Pending() {
        }

        public void enter() {
            A2dpSinkStateMachine.this.log("Enter Pending: " + A2dpSinkStateMachine.this.getCurrentMessage().what);
        }

        public boolean processMessage(Message message) {
            A2dpSinkStateMachine.this.log("Pending process message: " + message.what);
            int i = message.what;
            if (i == A2dpSinkStateMachine.STACK_EVENT) {
                StackEvent stackEvent = (StackEvent) message.obj;
                A2dpSinkStateMachine.this.log("STACK_EVENT " + stackEvent.type);
                int i2 = stackEvent.type;
                if (i2 == 1) {
                    A2dpSinkStateMachine.this.removeMessages(A2dpSinkStateMachine.CONNECT_TIMEOUT);
                    processConnectionEvent(stackEvent.device, stackEvent.valueInt);
                } else if (i2 == 3) {
                    A2dpSinkStateMachine.this.processAudioConfigEvent(stackEvent.device, stackEvent.audioConfig);
                } else {
                    A2dpSinkStateMachine.this.loge("Unexpected stack event: " + stackEvent.type);
                }
            } else if (i != A2dpSinkStateMachine.CONNECT_TIMEOUT) {
                switch (i) {
                    case 1:
                        A2dpSinkStateMachine.this.logd("Disconnect before connecting to another target");
                        break;
                    case 2:
                        BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                        if (A2dpSinkStateMachine.this.mCurrentDevice != null && A2dpSinkStateMachine.this.mTargetDevice != null && A2dpSinkStateMachine.this.mTargetDevice.equals(bluetoothDevice)) {
                            A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 0, 1);
                            synchronized (A2dpSinkStateMachine.this) {
                                A2dpSinkStateMachine.this.mTargetDevice = null;
                                break;
                            }
                        }
                        break;
                    default:
                        return false;
                }
            } else {
                A2dpSinkStateMachine.this.onConnectionStateChanged(A2dpSinkStateMachine.this.getByteAddress(A2dpSinkStateMachine.this.mTargetDevice), 0);
            }
            return true;
        }

        private void processConnectionEvent(BluetoothDevice bluetoothDevice, int i) {
            A2dpSinkStateMachine.this.log("processConnectionEvent state " + i);
            A2dpSinkStateMachine.this.log("Devices curr: " + A2dpSinkStateMachine.this.mCurrentDevice + " target: " + A2dpSinkStateMachine.this.mTargetDevice + " incoming: " + A2dpSinkStateMachine.this.mIncomingDevice + " device: " + bluetoothDevice);
            switch (i) {
                case 0:
                    A2dpSinkStateMachine.this.mAudioConfigs.remove(bluetoothDevice);
                    if (A2dpSinkStateMachine.this.mCurrentDevice == null || !A2dpSinkStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                        if (A2dpSinkStateMachine.this.mTargetDevice == null || !A2dpSinkStateMachine.this.mTargetDevice.equals(bluetoothDevice)) {
                            if (A2dpSinkStateMachine.this.mIncomingDevice != null && A2dpSinkStateMachine.this.mIncomingDevice.equals(bluetoothDevice)) {
                                A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mIncomingDevice, 0, 1);
                                synchronized (A2dpSinkStateMachine.this) {
                                    A2dpSinkStateMachine.this.mIncomingDevice = null;
                                    A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mDisconnected);
                                    break;
                                }
                                return;
                            }
                            A2dpSinkStateMachine.this.loge("Unknown device Disconnected: " + bluetoothDevice);
                            return;
                        }
                        A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mTargetDevice, 0, 1);
                        synchronized (A2dpSinkStateMachine.this) {
                            A2dpSinkStateMachine.this.mTargetDevice = null;
                            A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mDisconnected);
                            break;
                        }
                        return;
                    }
                    A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mCurrentDevice, 0, 3);
                    synchronized (A2dpSinkStateMachine.this) {
                        A2dpSinkStateMachine.this.mCurrentDevice = null;
                        break;
                    }
                    if (A2dpSinkStateMachine.this.mTargetDevice != null) {
                        if (!A2dpSinkStateMachine.this.connectA2dpNative(A2dpSinkStateMachine.this.getByteAddress(A2dpSinkStateMachine.this.mTargetDevice))) {
                            A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mTargetDevice, 0, 1);
                            synchronized (A2dpSinkStateMachine.this) {
                                A2dpSinkStateMachine.this.mTargetDevice = null;
                                A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mDisconnected);
                                break;
                            }
                            return;
                        }
                        return;
                    }
                    synchronized (A2dpSinkStateMachine.this) {
                        A2dpSinkStateMachine.this.mIncomingDevice = null;
                        A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mDisconnected);
                        break;
                    }
                    return;
                case 1:
                    if (A2dpSinkStateMachine.this.mCurrentDevice == null || !A2dpSinkStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                        if (A2dpSinkStateMachine.this.mTargetDevice == null || !A2dpSinkStateMachine.this.mTargetDevice.equals(bluetoothDevice)) {
                            if (A2dpSinkStateMachine.this.mIncomingDevice == null || !A2dpSinkStateMachine.this.mIncomingDevice.equals(bluetoothDevice)) {
                                A2dpSinkStateMachine.this.log("Incoming connection while pending, ignore");
                                return;
                            } else {
                                A2dpSinkStateMachine.this.loge("Another connecting event on the incoming device");
                                return;
                            }
                        }
                        A2dpSinkStateMachine.this.log("Stack and target device are connecting");
                        return;
                    }
                    A2dpSinkStateMachine.this.log("current device tries to connect back");
                    return;
                case 2:
                    if (A2dpSinkStateMachine.this.mCurrentDevice == null || !A2dpSinkStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                        if (A2dpSinkStateMachine.this.mTargetDevice == null || !A2dpSinkStateMachine.this.mTargetDevice.equals(bluetoothDevice)) {
                            if (A2dpSinkStateMachine.this.mIncomingDevice != null && A2dpSinkStateMachine.this.mIncomingDevice.equals(bluetoothDevice)) {
                                A2dpSinkStateMachine.this.loge("incoming device is not null");
                                A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mIncomingDevice, 2, 1);
                                synchronized (A2dpSinkStateMachine.this) {
                                    A2dpSinkStateMachine.this.mCurrentDevice = A2dpSinkStateMachine.this.mIncomingDevice;
                                    A2dpSinkStateMachine.this.mIncomingDevice = null;
                                    A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mConnected);
                                    break;
                                }
                                return;
                            }
                            A2dpSinkStateMachine.this.loge("Unknown device Connected: " + bluetoothDevice);
                            A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 2, 0);
                            A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mCurrentDevice, 0, 1);
                            synchronized (A2dpSinkStateMachine.this) {
                                A2dpSinkStateMachine.this.mCurrentDevice = bluetoothDevice;
                                A2dpSinkStateMachine.this.mTargetDevice = null;
                                A2dpSinkStateMachine.this.mIncomingDevice = null;
                                A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mConnected);
                                break;
                            }
                            return;
                        }
                        A2dpSinkStateMachine.this.loge("target device is not null");
                        A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mTargetDevice, 2, 1);
                        synchronized (A2dpSinkStateMachine.this) {
                            A2dpSinkStateMachine.this.mCurrentDevice = A2dpSinkStateMachine.this.mTargetDevice;
                            A2dpSinkStateMachine.this.mTargetDevice = null;
                            A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mConnected);
                            break;
                        }
                        return;
                    }
                    A2dpSinkStateMachine.this.loge("current device is not null");
                    A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mCurrentDevice, 2, 3);
                    if (A2dpSinkStateMachine.this.mTargetDevice != null) {
                        A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mTargetDevice, 0, 1);
                    }
                    synchronized (A2dpSinkStateMachine.this) {
                        A2dpSinkStateMachine.this.mTargetDevice = null;
                        A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mConnected);
                        break;
                    }
                    return;
                case 3:
                    if (A2dpSinkStateMachine.this.mCurrentDevice == null || !A2dpSinkStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                        if (A2dpSinkStateMachine.this.mTargetDevice == null || !A2dpSinkStateMachine.this.mTargetDevice.equals(bluetoothDevice)) {
                            if (A2dpSinkStateMachine.this.mIncomingDevice != null && A2dpSinkStateMachine.this.mIncomingDevice.equals(bluetoothDevice)) {
                                A2dpSinkStateMachine.this.loge("IncomingDevice is getting disconnected");
                                return;
                            }
                            A2dpSinkStateMachine.this.loge("Disconnecting unknown device: " + bluetoothDevice);
                            return;
                        }
                        A2dpSinkStateMachine.this.loge("TargetDevice is getting disconnected");
                        return;
                    }
                    return;
                default:
                    A2dpSinkStateMachine.this.loge("Incorrect state: " + i);
                    return;
            }
        }
    }

    private class Connected extends State {
        private Connected() {
        }

        public void enter() {
            A2dpSinkStateMachine.this.log("Enter Connected: " + A2dpSinkStateMachine.this.getCurrentMessage().what);
            A2dpSinkStateMachine.this.broadcastAudioState(A2dpSinkStateMachine.this.mCurrentDevice, 11, 10);
            synchronized (A2dpSinkStateMachine.this) {
                if (A2dpSinkStateMachine.this.mStreaming == null) {
                    A2dpSinkStateMachine.this.mStreaming = new A2dpSinkStreamHandler(A2dpSinkStateMachine.this, A2dpSinkStateMachine.this.mContext);
                }
            }
            if (A2dpSinkStateMachine.this.mStreaming.getAudioFocus() == 0) {
                A2dpSinkStateMachine.this.informAudioFocusStateNative(0);
            }
        }

        public boolean processMessage(Message message) {
            A2dpSinkStateMachine.this.log("Connected process message: " + message.what);
            if (A2dpSinkStateMachine.this.mCurrentDevice == null) {
                A2dpSinkStateMachine.this.loge("ERROR: mCurrentDevice is null in Connected");
                return false;
            }
            int i = message.what;
            if (i != A2dpSinkStateMachine.STACK_EVENT) {
                switch (i) {
                    case 1:
                        A2dpSinkStateMachine.this.logd("Disconnect before connecting to another target");
                        break;
                    case 2:
                        BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                        if (A2dpSinkStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                            A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 3, 2);
                            if (!A2dpSinkStateMachine.this.disconnectA2dpNative(A2dpSinkStateMachine.this.getByteAddress(bluetoothDevice))) {
                                A2dpSinkStateMachine.this.broadcastConnectionState(bluetoothDevice, 2, 0);
                            } else {
                                A2dpSinkStateMachine.this.mPlayingDevice = null;
                                A2dpSinkStateMachine.this.mStreaming.obtainMessage(6).sendToTarget();
                                A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mPending);
                            }
                            break;
                        }
                        break;
                    default:
                        switch (i) {
                            case A2dpSinkStateMachine.EVENT_AVRCP_CT_PLAY:
                                A2dpSinkStateMachine.this.mStreaming.obtainMessage(2).sendToTarget();
                                break;
                            case A2dpSinkStateMachine.EVENT_AVRCP_CT_PAUSE:
                                A2dpSinkStateMachine.this.mStreaming.obtainMessage(3).sendToTarget();
                                break;
                            case A2dpSinkStateMachine.EVENT_AVRCP_TG_PLAY:
                                A2dpSinkStateMachine.this.mStreaming.obtainMessage(4).sendToTarget();
                                break;
                            case A2dpSinkStateMachine.EVENT_AVRCP_TG_PAUSE:
                                A2dpSinkStateMachine.this.mStreaming.obtainMessage(5).sendToTarget();
                                break;
                            case A2dpSinkStateMachine.EVENT_REQUEST_FOCUS:
                                A2dpSinkStateMachine.this.mStreaming.obtainMessage(8).sendToTarget();
                                break;
                        }
                        break;
                }
                return false;
            }
            StackEvent stackEvent = (StackEvent) message.obj;
            switch (stackEvent.type) {
                case 1:
                    processConnectionEvent(stackEvent.device, stackEvent.valueInt);
                    break;
                case 2:
                    processAudioStateEvent(stackEvent.device, stackEvent.valueInt);
                    break;
                case 3:
                    A2dpSinkStateMachine.this.processAudioConfigEvent(stackEvent.device, stackEvent.audioConfig);
                    break;
                default:
                    A2dpSinkStateMachine.this.loge("Unexpected stack event: " + stackEvent.type);
                    break;
            }
            return false;
        }

        private void processConnectionEvent(BluetoothDevice bluetoothDevice, int i) {
            if (i == 0) {
                A2dpSinkStateMachine.this.mAudioConfigs.remove(bluetoothDevice);
                if (A2dpSinkStateMachine.this.mPlayingDevice != null && bluetoothDevice.equals(A2dpSinkStateMachine.this.mPlayingDevice)) {
                    A2dpSinkStateMachine.this.mPlayingDevice = null;
                }
                if (A2dpSinkStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                    A2dpSinkStateMachine.this.broadcastConnectionState(A2dpSinkStateMachine.this.mCurrentDevice, 0, 2);
                    synchronized (A2dpSinkStateMachine.this) {
                        A2dpSinkStateMachine.this.mStreaming.obtainMessage(6).sendToTarget();
                        A2dpSinkStateMachine.this.mCurrentDevice = null;
                        A2dpSinkStateMachine.this.transitionTo(A2dpSinkStateMachine.this.mDisconnected);
                    }
                    return;
                }
                A2dpSinkStateMachine.this.loge("Disconnected from unknown device: " + bluetoothDevice);
                return;
            }
            A2dpSinkStateMachine.this.loge("Connection State Device: " + bluetoothDevice + " bad state: " + i);
        }

        private void processAudioStateEvent(BluetoothDevice bluetoothDevice, int i) {
            if (!A2dpSinkStateMachine.this.mCurrentDevice.equals(bluetoothDevice)) {
                A2dpSinkStateMachine.this.loge("Audio State Device:" + bluetoothDevice + "is different from ConnectedDevice:" + A2dpSinkStateMachine.this.mCurrentDevice);
            }
            A2dpSinkStateMachine.this.log(" processAudioStateEvent in state " + i);
            switch (i) {
                case 0:
                case 1:
                    A2dpSinkStateMachine.this.mStreaming.obtainMessage(1).sendToTarget();
                    break;
                case 2:
                    A2dpSinkStateMachine.this.mStreaming.obtainMessage(0).sendToTarget();
                    break;
                default:
                    A2dpSinkStateMachine.this.loge("Audio State Device: " + bluetoothDevice + " bad state: " + i);
                    break;
            }
        }
    }

    private void processAudioConfigEvent(BluetoothDevice bluetoothDevice, BluetoothAudioConfig bluetoothAudioConfig) {
        log("processAudioConfigEvent: " + bluetoothDevice);
        this.mAudioConfigs.put(bluetoothDevice, bluetoothAudioConfig);
        broadcastAudioConfig(bluetoothDevice, bluetoothAudioConfig);
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        if (getCurrentState() == this.mDisconnected) {
            return 0;
        }
        synchronized (this) {
            Pending currentState = getCurrentState();
            if (currentState == this.mPending) {
                if (this.mTargetDevice != null && this.mTargetDevice.equals(bluetoothDevice)) {
                    return 1;
                }
                if (this.mCurrentDevice == null || !this.mCurrentDevice.equals(bluetoothDevice)) {
                    return (this.mIncomingDevice == null || !this.mIncomingDevice.equals(bluetoothDevice)) ? 0 : 1;
                }
                return 3;
            }
            if (currentState == this.mConnected) {
                return this.mCurrentDevice.equals(bluetoothDevice) ? 2 : 0;
            }
            loge("Bad currentState: " + currentState);
            return 0;
        }
    }

    BluetoothAudioConfig getAudioConfig(BluetoothDevice bluetoothDevice) {
        return this.mAudioConfigs.get(bluetoothDevice);
    }

    List<BluetoothDevice> getConnectedDevices() {
        ArrayList arrayList = new ArrayList();
        synchronized (this) {
            if (getCurrentState() == this.mConnected) {
                arrayList.add(this.mCurrentDevice);
            }
        }
        return arrayList;
    }

    boolean isPlaying(BluetoothDevice bluetoothDevice) {
        synchronized (this) {
            if (this.mPlayingDevice != null && bluetoothDevice.equals(this.mPlayingDevice)) {
                return true;
            }
            return false;
        }
    }

    boolean okToConnect(BluetoothDevice bluetoothDevice) {
        AdapterService.getAdapterService();
        int priority = this.mService.getPriority(bluetoothDevice);
        if (priority <= 0) {
            if (-1 == priority && bluetoothDevice.getBondState() != 10) {
                return true;
            }
            logw("okToConnect not OK to connect " + bluetoothDevice);
            return false;
        }
        return true;
    }

    synchronized List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        ArrayList arrayList;
        arrayList = new ArrayList();
        for (BluetoothDevice bluetoothDevice : this.mAdapter.getBondedDevices()) {
            if (BluetoothUuid.isUuidPresent(bluetoothDevice.getUuids(), BluetoothUuid.AudioSource)) {
                int connectionState = getConnectionState(bluetoothDevice);
                for (int i : iArr) {
                    if (connectionState == i) {
                        arrayList.add(bluetoothDevice);
                    }
                }
            }
        }
        return arrayList;
    }

    private void broadcastConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) {
        this.mIntentBroadcastHandler.sendMessageDelayed(this.mIntentBroadcastHandler.obtainMessage(0, i2, i, bluetoothDevice), 0);
    }

    private void broadcastAudioState(BluetoothDevice bluetoothDevice, int i, int i2) {
        Intent intent = new Intent("android.bluetooth.a2dp-sink.profile.action.PLAYING_STATE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i2);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
        log("A2DP Playing state : device: " + bluetoothDevice + " State:" + i2 + "->" + i);
    }

    private void broadcastAudioConfig(BluetoothDevice bluetoothDevice, BluetoothAudioConfig bluetoothAudioConfig) {
        Intent intent = new Intent("android.bluetooth.a2dp-sink.profile.action.AUDIO_CONFIG_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.a2dp-sink.profile.extra.AUDIO_CONFIG", (Parcelable) bluetoothAudioConfig);
        this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
        log("A2DP Audio Config : device: " + bluetoothDevice + " config: " + bluetoothAudioConfig);
    }

    private byte[] getByteAddress(BluetoothDevice bluetoothDevice) {
        return Utils.getBytesFromAddress(bluetoothDevice.getAddress());
    }

    private void onConnectionStateChanged(byte[] bArr, int i) {
        StackEvent stackEvent = new StackEvent(1);
        stackEvent.device = getDevice(bArr);
        stackEvent.valueInt = i;
        sendMessage(STACK_EVENT, stackEvent);
    }

    private void onAudioStateChanged(byte[] bArr, int i) {
        StackEvent stackEvent = new StackEvent(2);
        stackEvent.device = getDevice(bArr);
        stackEvent.valueInt = i;
        sendMessage(STACK_EVENT, stackEvent);
    }

    private void onAudioConfigChanged(byte[] bArr, int i, int i2) {
        StackEvent stackEvent = new StackEvent(3);
        stackEvent.device = getDevice(bArr);
        stackEvent.audioConfig = new BluetoothAudioConfig(i, i2 == 1 ? 16 : 12, 2);
        sendMessage(STACK_EVENT, stackEvent);
    }

    private BluetoothDevice getDevice(byte[] bArr) {
        return this.mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(bArr));
    }

    private class StackEvent {
        public BluetoothAudioConfig audioConfig;
        public BluetoothDevice device;
        public int type;
        public int valueInt;

        private StackEvent(int i) {
            this.type = 0;
            this.device = null;
            this.valueInt = 0;
            this.audioConfig = null;
            this.type = i;
        }
    }

    private class IntentBroadcastHandler extends Handler {
        private IntentBroadcastHandler() {
        }

        private void onConnectionStateChanged(BluetoothDevice bluetoothDevice, int i, int i2) {
            if (i != i2 && i2 == 2) {
                MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.A2DP_SINK);
            }
            Intent intent = new Intent("android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED");
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i);
            intent.putExtra("android.bluetooth.profile.extra.STATE", i2);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
            A2dpSinkStateMachine.this.mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
            A2dpSinkStateMachine.this.log("Connection state " + bluetoothDevice + ": " + i + "->" + i2);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                onConnectionStateChanged((BluetoothDevice) message.obj, message.arg1, message.arg2);
            }
        }
    }

    public boolean sendPassThruPlay(BluetoothDevice bluetoothDevice) {
        log("sendPassThruPlay + ");
        AvrcpControllerService avrcpControllerService = AvrcpControllerService.getAvrcpControllerService();
        if (avrcpControllerService != null && bluetoothDevice != null && avrcpControllerService.getConnectedDevices().contains(bluetoothDevice)) {
            avrcpControllerService.sendPassThroughCmd(bluetoothDevice, 68, 0);
            avrcpControllerService.sendPassThroughCmd(bluetoothDevice, 68, 1);
            log(" sendPassThruPlay command sent - ");
            return true;
        }
        log("passthru command not sent, connection unavailable");
        return false;
    }
}
