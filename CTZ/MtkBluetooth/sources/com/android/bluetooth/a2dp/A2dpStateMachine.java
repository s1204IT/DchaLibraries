package com.android.bluetooth.a2dp;

import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.vcard.VCardConfig;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

final class A2dpStateMachine extends StateMachine {
    static final int CONNECT = 1;
    private static final int CONNECT_TIMEOUT = 201;
    private static final boolean DBG = true;
    static final int DISCONNECT = 2;

    @VisibleForTesting
    static final int STACK_EVENT = 101;
    private static final String TAG = "A2dpStateMachine";

    @VisibleForTesting
    static int sConnectTimeoutMs = 30000;
    private A2dpNativeInterface mA2dpNativeInterface;
    private boolean mA2dpOffloadEnabled;
    private A2dpService mA2dpService;
    private BluetoothCodecStatus mCodecStatus;
    private Connected mConnected;
    private Connecting mConnecting;
    private int mConnectionState;
    private final BluetoothDevice mDevice;
    private Disconnected mDisconnected;
    private Disconnecting mDisconnecting;
    private boolean mIsPlaying;
    private int mLastBroadcastConnState;
    private int mLastConnectionState;

    A2dpStateMachine(BluetoothDevice bluetoothDevice, A2dpService a2dpService, A2dpNativeInterface a2dpNativeInterface, Looper looper) {
        super(TAG, looper);
        this.mConnectionState = 0;
        this.mLastConnectionState = -1;
        this.mLastBroadcastConnState = 0;
        this.mA2dpOffloadEnabled = false;
        this.mIsPlaying = false;
        setDbg(true);
        this.mDevice = bluetoothDevice;
        this.mA2dpService = a2dpService;
        this.mA2dpNativeInterface = a2dpNativeInterface;
        this.mDisconnected = new Disconnected();
        this.mConnecting = new Connecting();
        this.mDisconnecting = new Disconnecting();
        this.mConnected = new Connected();
        addState(this.mDisconnected);
        addState(this.mConnecting);
        addState(this.mDisconnecting);
        addState(this.mConnected);
        this.mA2dpOffloadEnabled = this.mA2dpService.mA2dpOffloadEnabled;
        setInitialState(this.mDisconnected);
    }

    static A2dpStateMachine make(BluetoothDevice bluetoothDevice, A2dpService a2dpService, A2dpNativeInterface a2dpNativeInterface, Looper looper) {
        Log.i(TAG, "make for device " + bluetoothDevice);
        A2dpStateMachine a2dpStateMachine = new A2dpStateMachine(bluetoothDevice, a2dpService, a2dpNativeInterface, looper);
        a2dpStateMachine.start();
        return a2dpStateMachine;
    }

    public void doQuit() {
        log("doQuit for device " + this.mDevice);
        if (this.mIsPlaying) {
            log("doQuit: stopped playing " + this.mDevice);
            this.mIsPlaying = false;
            broadcastAudioState(11, 10);
        }
        if (getConnectionState() != 0) {
            log("doQuit()- Broadcast A2DP State to DISCONNECTED");
            broadcastConnectionState(0, this.mLastBroadcastConnState);
        }
        quitNow();
    }

    public void cleanup() {
        log("cleanup for device " + this.mDevice);
    }

    @VisibleForTesting
    class Disconnected extends State {
        Disconnected() {
        }

        public void enter() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            StringBuilder sb = new StringBuilder();
            sb.append("Enter Disconnected(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            Log.i(A2dpStateMachine.TAG, sb.toString());
            A2dpStateMachine.this.mConnectionState = 0;
            A2dpStateMachine.this.removeDeferredMessages(2);
            if (A2dpStateMachine.this.mLastConnectionState != -1) {
                A2dpStateMachine.this.broadcastConnectionState(A2dpStateMachine.this.mConnectionState, A2dpStateMachine.this.mLastConnectionState);
                if (A2dpStateMachine.this.mIsPlaying) {
                    Log.i(A2dpStateMachine.TAG, "Disconnected: stopped playing: " + A2dpStateMachine.this.mDevice);
                    A2dpStateMachine.this.mIsPlaying = false;
                    A2dpStateMachine.this.broadcastAudioState(11, 10);
                }
            }
        }

        public void exit() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            A2dpStateMachine a2dpStateMachine = A2dpStateMachine.this;
            StringBuilder sb = new StringBuilder();
            sb.append("Exit Disconnected(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            a2dpStateMachine.log(sb.toString());
            A2dpStateMachine.this.mLastConnectionState = 0;
        }

        public boolean processMessage(Message message) {
            A2dpStateMachine.this.log("Disconnected process message(" + A2dpStateMachine.this.mDevice + "): " + A2dpStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i != A2dpStateMachine.STACK_EVENT) {
                switch (i) {
                    case 1:
                        Log.i(A2dpStateMachine.TAG, "Connecting to " + A2dpStateMachine.this.mDevice);
                        if (A2dpStateMachine.this.mA2dpNativeInterface.connectA2dp(A2dpStateMachine.this.mDevice)) {
                            if (A2dpStateMachine.this.mA2dpService.okToConnect(A2dpStateMachine.this.mDevice, true)) {
                                A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mConnecting);
                            } else {
                                Log.w(A2dpStateMachine.TAG, "Outgoing A2DP Connecting request rejected: " + A2dpStateMachine.this.mDevice);
                            }
                        } else {
                            Log.e(A2dpStateMachine.TAG, "Disconnected: error connecting to " + A2dpStateMachine.this.mDevice);
                        }
                        break;
                    case 2:
                        Log.w(A2dpStateMachine.TAG, "Disconnected: DISCONNECT ignored: " + A2dpStateMachine.this.mDevice);
                        break;
                    default:
                        return false;
                }
            } else {
                A2dpStackEvent a2dpStackEvent = (A2dpStackEvent) message.obj;
                A2dpStateMachine.this.log("Disconnected: stack event: " + a2dpStackEvent);
                if (!A2dpStateMachine.this.mDevice.equals(a2dpStackEvent.device)) {
                    Log.wtfStack(A2dpStateMachine.TAG, "Device(" + A2dpStateMachine.this.mDevice + "): event mismatch: " + a2dpStackEvent);
                }
                int i2 = a2dpStackEvent.type;
                if (i2 == 1) {
                    processConnectionEvent(a2dpStackEvent.valueInt);
                } else if (i2 == 3) {
                    A2dpStateMachine.this.processCodecConfigEvent(a2dpStackEvent.codecStatus);
                } else {
                    Log.e(A2dpStateMachine.TAG, "Disconnected: ignoring stack event: " + a2dpStackEvent);
                }
            }
            return true;
        }

        private void processConnectionEvent(int i) {
            switch (i) {
                case 0:
                    Log.w(A2dpStateMachine.TAG, "Ignore A2DP DISCONNECTED event: " + A2dpStateMachine.this.mDevice);
                    break;
                case 1:
                    if (A2dpStateMachine.this.mA2dpService.okToConnect(A2dpStateMachine.this.mDevice, false)) {
                        Log.i(A2dpStateMachine.TAG, "Incoming A2DP Connecting request accepted: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mConnecting);
                    } else {
                        Log.w(A2dpStateMachine.TAG, "Incoming A2DP Connecting request rejected: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice);
                    }
                    break;
                case 2:
                    Log.w(A2dpStateMachine.TAG, "A2DP Connected from Disconnected state: " + A2dpStateMachine.this.mDevice);
                    if (A2dpStateMachine.this.mA2dpService.okToConnect(A2dpStateMachine.this.mDevice, false)) {
                        Log.i(A2dpStateMachine.TAG, "Incoming A2DP Connected request accepted: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mConnected);
                    } else {
                        Log.w(A2dpStateMachine.TAG, "Incoming A2DP Connected request rejected: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice);
                    }
                    break;
                case 3:
                    Log.w(A2dpStateMachine.TAG, "Ignore A2DP DISCONNECTING event: " + A2dpStateMachine.this.mDevice);
                    break;
                default:
                    Log.e(A2dpStateMachine.TAG, "Incorrect event: " + i + " device: " + A2dpStateMachine.this.mDevice);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connecting extends State {
        Connecting() {
        }

        public void enter() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            StringBuilder sb = new StringBuilder();
            sb.append("Enter Connecting(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            Log.i(A2dpStateMachine.TAG, sb.toString());
            A2dpStateMachine.this.sendMessageDelayed(A2dpStateMachine.CONNECT_TIMEOUT, A2dpStateMachine.sConnectTimeoutMs);
            A2dpStateMachine.this.mConnectionState = 1;
            A2dpStateMachine.this.broadcastConnectionState(A2dpStateMachine.this.mConnectionState, A2dpStateMachine.this.mLastConnectionState);
        }

        public void exit() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            A2dpStateMachine a2dpStateMachine = A2dpStateMachine.this;
            StringBuilder sb = new StringBuilder();
            sb.append("Exit Connecting(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            a2dpStateMachine.log(sb.toString());
            A2dpStateMachine.this.mLastConnectionState = 1;
            A2dpStateMachine.this.removeMessages(A2dpStateMachine.CONNECT_TIMEOUT);
        }

        public boolean processMessage(Message message) {
            A2dpStateMachine.this.log("Connecting process message(" + A2dpStateMachine.this.mDevice + "): " + A2dpStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i == A2dpStateMachine.STACK_EVENT) {
                A2dpStackEvent a2dpStackEvent = (A2dpStackEvent) message.obj;
                A2dpStateMachine.this.log("Connecting: stack event: " + a2dpStackEvent);
                if (!A2dpStateMachine.this.mDevice.equals(a2dpStackEvent.device)) {
                    Log.wtfStack(A2dpStateMachine.TAG, "Device(" + A2dpStateMachine.this.mDevice + "): event mismatch: " + a2dpStackEvent);
                }
                switch (a2dpStackEvent.type) {
                    case 1:
                        processConnectionEvent(a2dpStackEvent.valueInt);
                        break;
                    case 2:
                        break;
                    case 3:
                        A2dpStateMachine.this.processCodecConfigEvent(a2dpStackEvent.codecStatus);
                        break;
                    default:
                        Log.e(A2dpStateMachine.TAG, "Connecting: ignoring stack event: " + a2dpStackEvent);
                        break;
                }
            } else if (i == A2dpStateMachine.CONNECT_TIMEOUT) {
                Log.w(A2dpStateMachine.TAG, "Connecting connection timeout: " + A2dpStateMachine.this.mDevice);
                A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice);
                A2dpStackEvent a2dpStackEvent2 = new A2dpStackEvent(1);
                a2dpStackEvent2.device = A2dpStateMachine.this.mDevice;
                a2dpStackEvent2.valueInt = 0;
                A2dpStateMachine.this.sendMessage(A2dpStateMachine.STACK_EVENT, a2dpStackEvent2);
            } else {
                switch (i) {
                    case 1:
                        A2dpStateMachine.this.deferMessage(message);
                        break;
                    case 2:
                        Log.i(A2dpStateMachine.TAG, "Connecting: connection canceled to " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnected);
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

        private void processConnectionEvent(int i) {
            switch (i) {
                case 0:
                    Log.w(A2dpStateMachine.TAG, "Connecting device disconnected: " + A2dpStateMachine.this.mDevice);
                    A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnected);
                    break;
                case 1:
                    break;
                case 2:
                    A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mConnected);
                    break;
                case 3:
                    Log.w(A2dpStateMachine.TAG, "Connecting interrupted: device is disconnecting: " + A2dpStateMachine.this.mDevice);
                    A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnecting);
                    break;
                default:
                    Log.e(A2dpStateMachine.TAG, "Incorrect event: " + i);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Disconnecting extends State {
        Disconnecting() {
        }

        public void enter() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            StringBuilder sb = new StringBuilder();
            sb.append("Enter Disconnecting(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            Log.i(A2dpStateMachine.TAG, sb.toString());
            A2dpStateMachine.this.sendMessageDelayed(A2dpStateMachine.CONNECT_TIMEOUT, A2dpStateMachine.sConnectTimeoutMs);
            A2dpStateMachine.this.mConnectionState = 3;
            A2dpStateMachine.this.broadcastConnectionState(A2dpStateMachine.this.mConnectionState, A2dpStateMachine.this.mLastConnectionState);
        }

        public void exit() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            A2dpStateMachine a2dpStateMachine = A2dpStateMachine.this;
            StringBuilder sb = new StringBuilder();
            sb.append("Exit Disconnecting(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            a2dpStateMachine.log(sb.toString());
            A2dpStateMachine.this.mLastConnectionState = 3;
            A2dpStateMachine.this.removeMessages(A2dpStateMachine.CONNECT_TIMEOUT);
        }

        public boolean processMessage(Message message) {
            A2dpStateMachine.this.log("Disconnecting process message(" + A2dpStateMachine.this.mDevice + "): " + A2dpStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i == A2dpStateMachine.STACK_EVENT) {
                A2dpStackEvent a2dpStackEvent = (A2dpStackEvent) message.obj;
                A2dpStateMachine.this.log("Disconnecting: stack event: " + a2dpStackEvent);
                if (!A2dpStateMachine.this.mDevice.equals(a2dpStackEvent.device)) {
                    Log.wtfStack(A2dpStateMachine.TAG, "Device(" + A2dpStateMachine.this.mDevice + "): event mismatch: " + a2dpStackEvent);
                }
                int i2 = a2dpStackEvent.type;
                if (i2 == 1) {
                    processConnectionEvent(a2dpStackEvent.valueInt);
                } else if (i2 == 3) {
                    A2dpStateMachine.this.processCodecConfigEvent(a2dpStackEvent.codecStatus);
                } else {
                    Log.e(A2dpStateMachine.TAG, "Disconnecting: ignoring stack event: " + a2dpStackEvent);
                }
            } else if (i == A2dpStateMachine.CONNECT_TIMEOUT) {
                Log.w(A2dpStateMachine.TAG, "Disconnecting connection timeout: " + A2dpStateMachine.this.mDevice);
                A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice);
                A2dpStackEvent a2dpStackEvent2 = new A2dpStackEvent(1);
                a2dpStackEvent2.device = A2dpStateMachine.this.mDevice;
                a2dpStackEvent2.valueInt = 0;
                A2dpStateMachine.this.sendMessage(A2dpStateMachine.STACK_EVENT, a2dpStackEvent2);
            } else {
                switch (i) {
                    case 1:
                        A2dpStateMachine.this.deferMessage(message);
                        break;
                    case 2:
                        A2dpStateMachine.this.deferMessage(message);
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

        private void processConnectionEvent(int i) {
            switch (i) {
                case 0:
                    Log.i(A2dpStateMachine.TAG, "Disconnected: " + A2dpStateMachine.this.mDevice);
                    A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnected);
                    break;
                case 1:
                    if (A2dpStateMachine.this.mA2dpService.okToConnect(A2dpStateMachine.this.mDevice, false)) {
                        Log.i(A2dpStateMachine.TAG, "Disconnecting interrupted: try to reconnect: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mConnecting);
                    } else {
                        Log.w(A2dpStateMachine.TAG, "Incoming A2DP Connecting request rejected: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice);
                    }
                    break;
                case 2:
                    if (A2dpStateMachine.this.mA2dpService.okToConnect(A2dpStateMachine.this.mDevice, false)) {
                        Log.w(A2dpStateMachine.TAG, "Disconnecting interrupted: device is connected: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mConnected);
                    } else {
                        Log.w(A2dpStateMachine.TAG, "Incoming A2DP Connected request rejected: " + A2dpStateMachine.this.mDevice);
                        A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice);
                    }
                    break;
                case 3:
                    break;
                default:
                    Log.e(A2dpStateMachine.TAG, "Incorrect event: " + i);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connected extends State {
        Connected() {
        }

        public void enter() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            StringBuilder sb = new StringBuilder();
            sb.append("Enter Connected(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            Log.i(A2dpStateMachine.TAG, sb.toString());
            A2dpStateMachine.this.mConnectionState = 2;
            A2dpStateMachine.this.removeDeferredMessages(1);
            A2dpStateMachine.this.broadcastConnectionState(A2dpStateMachine.this.mConnectionState, A2dpStateMachine.this.mLastConnectionState);
            A2dpStateMachine.this.broadcastAudioState(11, 10);
        }

        public void exit() {
            Message currentMessage = A2dpStateMachine.this.getCurrentMessage();
            A2dpStateMachine a2dpStateMachine = A2dpStateMachine.this;
            StringBuilder sb = new StringBuilder();
            sb.append("Exit Connected(");
            sb.append(A2dpStateMachine.this.mDevice);
            sb.append("): ");
            sb.append(currentMessage == null ? "null" : A2dpStateMachine.messageWhatToString(currentMessage.what));
            a2dpStateMachine.log(sb.toString());
            A2dpStateMachine.this.mLastConnectionState = 2;
        }

        public boolean processMessage(Message message) {
            A2dpStateMachine.this.log("Connected process message(" + A2dpStateMachine.this.mDevice + "): " + A2dpStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i != A2dpStateMachine.STACK_EVENT) {
                switch (i) {
                    case 1:
                        Log.w(A2dpStateMachine.TAG, "Connected: CONNECT ignored: " + A2dpStateMachine.this.mDevice);
                        return true;
                    case 2:
                        Log.i(A2dpStateMachine.TAG, "Disconnecting from " + A2dpStateMachine.this.mDevice);
                        if (!A2dpStateMachine.this.mA2dpNativeInterface.disconnectA2dp(A2dpStateMachine.this.mDevice)) {
                            Log.e(A2dpStateMachine.TAG, "Connected: error disconnecting from " + A2dpStateMachine.this.mDevice);
                            A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnected);
                            return true;
                        }
                        A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnecting);
                        return true;
                    default:
                        return false;
                }
            }
            A2dpStackEvent a2dpStackEvent = (A2dpStackEvent) message.obj;
            A2dpStateMachine.this.log("Connected: stack event: " + a2dpStackEvent);
            if (!A2dpStateMachine.this.mDevice.equals(a2dpStackEvent.device)) {
                Log.wtfStack(A2dpStateMachine.TAG, "Device(" + A2dpStateMachine.this.mDevice + "): event mismatch: " + a2dpStackEvent);
            }
            switch (a2dpStackEvent.type) {
                case 1:
                    processConnectionEvent(a2dpStackEvent.valueInt);
                    return true;
                case 2:
                    processAudioStateEvent(a2dpStackEvent.valueInt);
                    return true;
                case 3:
                    A2dpStateMachine.this.processCodecConfigEvent(a2dpStackEvent.codecStatus);
                    return true;
                default:
                    Log.e(A2dpStateMachine.TAG, "Connected: ignoring stack event: " + a2dpStackEvent);
                    return true;
            }
        }

        private void processConnectionEvent(int i) {
            switch (i) {
                case 0:
                    Log.i(A2dpStateMachine.TAG, "Disconnected from " + A2dpStateMachine.this.mDevice);
                    A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnected);
                    break;
                case 1:
                    Log.w(A2dpStateMachine.TAG, "Ignore A2DP CONNECTING event: " + A2dpStateMachine.this.mDevice);
                    break;
                case 2:
                    Log.w(A2dpStateMachine.TAG, "Ignore A2DP CONNECTED event: " + A2dpStateMachine.this.mDevice);
                    break;
                case 3:
                    Log.i(A2dpStateMachine.TAG, "Disconnecting from " + A2dpStateMachine.this.mDevice);
                    A2dpStateMachine.this.transitionTo(A2dpStateMachine.this.mDisconnecting);
                    break;
                default:
                    Log.e(A2dpStateMachine.TAG, "Connection State Device: " + A2dpStateMachine.this.mDevice + " bad event: " + i);
                    break;
            }
        }

        private void processAudioStateEvent(int i) {
            switch (i) {
                case 0:
                case 1:
                    synchronized (this) {
                        if (A2dpStateMachine.this.mIsPlaying) {
                            Log.i(A2dpStateMachine.TAG, "Connected: stopped playing: " + A2dpStateMachine.this.mDevice);
                            A2dpStateMachine.this.mIsPlaying = false;
                            A2dpStateMachine.this.broadcastAudioState(11, 10);
                        }
                        break;
                    }
                    return;
                case 2:
                    synchronized (this) {
                        if (!A2dpStateMachine.this.mIsPlaying) {
                            Log.i(A2dpStateMachine.TAG, "Connected: started playing: " + A2dpStateMachine.this.mDevice);
                            A2dpStateMachine.this.mIsPlaying = true;
                            A2dpStateMachine.this.broadcastAudioState(10, 11);
                        }
                        break;
                    }
                    return;
                default:
                    Log.e(A2dpStateMachine.TAG, "Audio State Device: " + A2dpStateMachine.this.mDevice + " bad state: " + i);
                    return;
            }
        }
    }

    int getConnectionState() {
        return this.mConnectionState;
    }

    BluetoothDevice getDevice() {
        return this.mDevice;
    }

    boolean isConnected() {
        boolean z;
        synchronized (this) {
            z = getCurrentState() == this.mConnected;
        }
        return z;
    }

    boolean isPlaying() {
        boolean z;
        synchronized (this) {
            z = this.mIsPlaying;
        }
        return z;
    }

    BluetoothCodecStatus getCodecStatus() {
        BluetoothCodecStatus bluetoothCodecStatus;
        synchronized (this) {
            bluetoothCodecStatus = this.mCodecStatus;
        }
        return bluetoothCodecStatus;
    }

    private void processCodecConfigEvent(BluetoothCodecStatus bluetoothCodecStatus) {
        BluetoothCodecConfig codecConfig;
        synchronized (this) {
            if (this.mCodecStatus != null) {
                codecConfig = this.mCodecStatus.getCodecConfig();
            } else {
                codecConfig = null;
            }
            this.mCodecStatus = bluetoothCodecStatus;
        }
        Log.d(TAG, "A2DP Codec Config: " + codecConfig + "->" + bluetoothCodecStatus.getCodecConfig());
        BluetoothCodecConfig[] codecsLocalCapabilities = bluetoothCodecStatus.getCodecsLocalCapabilities();
        int length = codecsLocalCapabilities.length;
        for (int i = 0; i < length; i++) {
            Log.d(TAG, "A2DP Codec Local Capability: " + codecsLocalCapabilities[i]);
        }
        for (BluetoothCodecConfig bluetoothCodecConfig : bluetoothCodecStatus.getCodecsSelectableCapabilities()) {
            Log.d(TAG, "A2DP Codec Selectable Capability: " + bluetoothCodecConfig);
        }
        if (this.mA2dpOffloadEnabled) {
            BluetoothCodecConfig codecConfig2 = this.mCodecStatus.getCodecConfig();
            boolean z = true;
            if ((codecConfig == null || codecConfig.getCodecType() == codecConfig2.getCodecType()) && codecConfig2.sameAudioFeedingParameters(codecConfig) && (codecConfig2.getCodecType() != 4 || codecConfig == null || codecConfig.getCodecSpecific1() == codecConfig2.getCodecSpecific1())) {
                z = false;
            }
            if (z) {
                this.mA2dpService.codecConfigUpdated(this.mDevice, this.mCodecStatus, false);
                return;
            }
            return;
        }
        this.mA2dpService.codecConfigUpdated(this.mDevice, this.mCodecStatus, bluetoothCodecStatus.getCodecConfig().sameAudioFeedingParameters(codecConfig));
    }

    private void broadcastConnectionState(int i, int i2) {
        log("Connection state " + this.mDevice + ": " + profileStateToString(i2) + "->" + profileStateToString(i));
        Intent intent = new Intent("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i2);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent.addFlags(83886080);
        this.mA2dpService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
        this.mLastBroadcastConnState = i;
    }

    private void broadcastAudioState(int i, int i2) {
        log("A2DP Playing state : device: " + this.mDevice + " State:" + audioStateToString(i2) + "->" + audioStateToString(i));
        Intent intent = new Intent("android.bluetooth.a2dp.profile.action.PLAYING_STATE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i2);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        this.mA2dpService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    protected String getLogRecString(Message message) {
        return messageWhatToString(message.what) + ": arg1=" + message.arg1 + ", arg2=" + message.arg2 + ", obj=" + message.obj;
    }

    private static String messageWhatToString(int i) {
        if (i == STACK_EVENT) {
            return "STACK_EVENT";
        }
        if (i != CONNECT_TIMEOUT) {
            switch (i) {
                case 1:
                    return "CONNECT";
                case 2:
                    return "DISCONNECT";
                default:
                    return Integer.toString(i);
            }
        }
        return "CONNECT_TIMEOUT";
    }

    private static String profileStateToString(int i) {
        switch (i) {
            case 0:
                return "DISCONNECTED";
            case 1:
                return "CONNECTING";
            case 2:
                return "CONNECTED";
            case 3:
                return "DISCONNECTING";
            default:
                return Integer.toString(i);
        }
    }

    private static String audioStateToString(int i) {
        switch (i) {
            case 10:
                return "PLAYING";
            case 11:
                return "NOT_PLAYING";
            default:
                return Integer.toString(i);
        }
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mDevice: " + this.mDevice);
        ProfileService.println(sb, "  StateMachine: " + toString());
        ProfileService.println(sb, "  mIsPlaying: " + this.mIsPlaying);
        synchronized (this) {
            if (this.mCodecStatus != null) {
                ProfileService.println(sb, "  mCodecConfig: " + this.mCodecStatus.getCodecConfig());
            }
        }
        ProfileService.println(sb, "  StateMachine: " + this);
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        super.dump(new FileDescriptor(), printWriter, new String[0]);
        printWriter.flush();
        stringWriter.flush();
        ProfileService.println(sb, "  StateMachineLog:");
        Scanner scanner = new Scanner(stringWriter.toString());
        while (scanner.hasNextLine()) {
            ProfileService.println(sb, "    " + scanner.nextLine());
        }
        scanner.close();
    }

    protected void log(String str) {
        super.log(str);
    }
}
