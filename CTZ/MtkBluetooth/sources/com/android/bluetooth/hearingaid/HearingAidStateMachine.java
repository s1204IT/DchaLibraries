package com.android.bluetooth.hearingaid;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

final class HearingAidStateMachine extends StateMachine {
    static final int CONNECT = 1;
    private static final int CONNECT_TIMEOUT = 201;
    private static final boolean DBG = false;
    static final int DISCONNECT = 2;

    @VisibleForTesting
    static final int STACK_EVENT = 101;
    private static final String TAG = "HearingAidStateMachine";

    @VisibleForTesting
    static int sConnectTimeoutMs = 30000;
    private Connected mConnected;
    private Connecting mConnecting;
    private int mConnectionState;
    private final BluetoothDevice mDevice;
    private Disconnected mDisconnected;
    private Disconnecting mDisconnecting;
    private int mLastConnectionState;
    private HearingAidNativeInterface mNativeInterface;
    private HearingAidService mService;

    HearingAidStateMachine(BluetoothDevice bluetoothDevice, HearingAidService hearingAidService, HearingAidNativeInterface hearingAidNativeInterface, Looper looper) {
        super(TAG, looper);
        this.mConnectionState = 0;
        this.mLastConnectionState = -1;
        this.mDevice = bluetoothDevice;
        this.mService = hearingAidService;
        this.mNativeInterface = hearingAidNativeInterface;
        this.mDisconnected = new Disconnected();
        this.mConnecting = new Connecting();
        this.mDisconnecting = new Disconnecting();
        this.mConnected = new Connected();
        addState(this.mDisconnected);
        addState(this.mConnecting);
        addState(this.mDisconnecting);
        addState(this.mConnected);
        setInitialState(this.mDisconnected);
    }

    static HearingAidStateMachine make(BluetoothDevice bluetoothDevice, HearingAidService hearingAidService, HearingAidNativeInterface hearingAidNativeInterface, Looper looper) {
        Log.i(TAG, "make for device " + bluetoothDevice);
        HearingAidStateMachine hearingAidStateMachine = new HearingAidStateMachine(bluetoothDevice, hearingAidService, hearingAidNativeInterface, looper);
        hearingAidStateMachine.start();
        return hearingAidStateMachine;
    }

    public void doQuit() {
        log("doQuit for device " + this.mDevice);
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
            Log.i(HearingAidStateMachine.TAG, "Enter Disconnected(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.mConnectionState = 0;
            HearingAidStateMachine.this.removeDeferredMessages(2);
            if (HearingAidStateMachine.this.mLastConnectionState != -1) {
                HearingAidStateMachine.this.broadcastConnectionState(HearingAidStateMachine.this.mConnectionState, HearingAidStateMachine.this.mLastConnectionState);
            }
        }

        public void exit() {
            HearingAidStateMachine.this.log("Exit Disconnected(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.mLastConnectionState = 0;
        }

        public boolean processMessage(Message message) {
            HearingAidStateMachine.this.log("Disconnected process message(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i != HearingAidStateMachine.STACK_EVENT) {
                switch (i) {
                    case 1:
                        HearingAidStateMachine.this.log("Connecting to " + HearingAidStateMachine.this.mDevice);
                        if (HearingAidStateMachine.this.mNativeInterface.connectHearingAid(HearingAidStateMachine.this.mDevice)) {
                            if (HearingAidStateMachine.this.mService.okToConnect(HearingAidStateMachine.this.mDevice)) {
                                HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mConnecting);
                            } else {
                                Log.w(HearingAidStateMachine.TAG, "Outgoing HearingAid Connecting request rejected: " + HearingAidStateMachine.this.mDevice);
                            }
                        } else {
                            Log.e(HearingAidStateMachine.TAG, "Disconnected: error connecting to " + HearingAidStateMachine.this.mDevice);
                        }
                        break;
                    case 2:
                        Log.w(HearingAidStateMachine.TAG, "Disconnected: DISCONNECT ignored: " + HearingAidStateMachine.this.mDevice);
                        break;
                    default:
                        return false;
                }
            } else {
                HearingAidStackEvent hearingAidStackEvent = (HearingAidStackEvent) message.obj;
                if (!HearingAidStateMachine.this.mDevice.equals(hearingAidStackEvent.device)) {
                    Log.wtfStack(HearingAidStateMachine.TAG, "Device(" + HearingAidStateMachine.this.mDevice + "): event mismatch: " + hearingAidStackEvent);
                }
                if (hearingAidStackEvent.type == 1) {
                    processConnectionEvent(hearingAidStackEvent.valueInt1);
                } else {
                    Log.e(HearingAidStateMachine.TAG, "Disconnected: ignoring stack event: " + hearingAidStackEvent);
                }
            }
            return true;
        }

        private void processConnectionEvent(int i) {
            switch (i) {
                case 0:
                    Log.w(HearingAidStateMachine.TAG, "Ignore HearingAid DISCONNECTED event: " + HearingAidStateMachine.this.mDevice);
                    break;
                case 1:
                    if (HearingAidStateMachine.this.mService.okToConnect(HearingAidStateMachine.this.mDevice)) {
                        Log.i(HearingAidStateMachine.TAG, "Incoming HearingAid Connecting request accepted: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mConnecting);
                    } else {
                        Log.w(HearingAidStateMachine.TAG, "Incoming HearingAid Connecting request rejected: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice);
                    }
                    break;
                case 2:
                    Log.w(HearingAidStateMachine.TAG, "HearingAid Connected from Disconnected state: " + HearingAidStateMachine.this.mDevice);
                    if (HearingAidStateMachine.this.mService.okToConnect(HearingAidStateMachine.this.mDevice)) {
                        Log.i(HearingAidStateMachine.TAG, "Incoming HearingAid Connected request accepted: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mConnected);
                    } else {
                        Log.w(HearingAidStateMachine.TAG, "Incoming HearingAid Connected request rejected: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice);
                    }
                    break;
                case 3:
                    Log.w(HearingAidStateMachine.TAG, "Ignore HearingAid DISCONNECTING event: " + HearingAidStateMachine.this.mDevice);
                    break;
                default:
                    Log.e(HearingAidStateMachine.TAG, "Incorrect state: " + i + " device: " + HearingAidStateMachine.this.mDevice);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connecting extends State {
        Connecting() {
        }

        public void enter() {
            Log.i(HearingAidStateMachine.TAG, "Enter Connecting(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.sendMessageDelayed(HearingAidStateMachine.CONNECT_TIMEOUT, (long) HearingAidStateMachine.sConnectTimeoutMs);
            HearingAidStateMachine.this.mConnectionState = 1;
            HearingAidStateMachine.this.broadcastConnectionState(HearingAidStateMachine.this.mConnectionState, HearingAidStateMachine.this.mLastConnectionState);
        }

        public void exit() {
            HearingAidStateMachine.this.log("Exit Connecting(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.mLastConnectionState = 1;
            HearingAidStateMachine.this.removeMessages(HearingAidStateMachine.CONNECT_TIMEOUT);
        }

        public boolean processMessage(Message message) {
            HearingAidStateMachine.this.log("Connecting process message(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i == HearingAidStateMachine.STACK_EVENT) {
                HearingAidStackEvent hearingAidStackEvent = (HearingAidStackEvent) message.obj;
                HearingAidStateMachine.this.log("Connecting: stack event: " + hearingAidStackEvent);
                if (!HearingAidStateMachine.this.mDevice.equals(hearingAidStackEvent.device)) {
                    Log.wtfStack(HearingAidStateMachine.TAG, "Device(" + HearingAidStateMachine.this.mDevice + "): event mismatch: " + hearingAidStackEvent);
                }
                if (hearingAidStackEvent.type == 1) {
                    processConnectionEvent(hearingAidStackEvent.valueInt1);
                } else {
                    Log.e(HearingAidStateMachine.TAG, "Connecting: ignoring stack event: " + hearingAidStackEvent);
                }
            } else if (i == HearingAidStateMachine.CONNECT_TIMEOUT) {
                Log.w(HearingAidStateMachine.TAG, "Connecting connection timeout: " + HearingAidStateMachine.this.mDevice);
                HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice);
                HearingAidStackEvent hearingAidStackEvent2 = new HearingAidStackEvent(1);
                hearingAidStackEvent2.device = HearingAidStateMachine.this.mDevice;
                hearingAidStackEvent2.valueInt1 = 0;
                HearingAidStateMachine.this.sendMessage(HearingAidStateMachine.STACK_EVENT, hearingAidStackEvent2);
            } else {
                switch (i) {
                    case 1:
                        HearingAidStateMachine.this.deferMessage(message);
                        break;
                    case 2:
                        HearingAidStateMachine.this.log("Connecting: connection canceled to " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnected);
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
                    Log.w(HearingAidStateMachine.TAG, "Connecting device disconnected: " + HearingAidStateMachine.this.mDevice);
                    HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnected);
                    break;
                case 1:
                    break;
                case 2:
                    HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mConnected);
                    break;
                case 3:
                    Log.w(HearingAidStateMachine.TAG, "Connecting interrupted: device is disconnecting: " + HearingAidStateMachine.this.mDevice);
                    HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnecting);
                    break;
                default:
                    Log.e(HearingAidStateMachine.TAG, "Incorrect state: " + i);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Disconnecting extends State {
        Disconnecting() {
        }

        public void enter() {
            Log.i(HearingAidStateMachine.TAG, "Enter Disconnecting(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.sendMessageDelayed(HearingAidStateMachine.CONNECT_TIMEOUT, (long) HearingAidStateMachine.sConnectTimeoutMs);
            HearingAidStateMachine.this.mConnectionState = 3;
            HearingAidStateMachine.this.broadcastConnectionState(HearingAidStateMachine.this.mConnectionState, HearingAidStateMachine.this.mLastConnectionState);
        }

        public void exit() {
            HearingAidStateMachine.this.log("Exit Disconnecting(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.mLastConnectionState = 3;
            HearingAidStateMachine.this.removeMessages(HearingAidStateMachine.CONNECT_TIMEOUT);
        }

        public boolean processMessage(Message message) {
            HearingAidStateMachine.this.log("Disconnecting process message(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i == HearingAidStateMachine.STACK_EVENT) {
                HearingAidStackEvent hearingAidStackEvent = (HearingAidStackEvent) message.obj;
                HearingAidStateMachine.this.log("Disconnecting: stack event: " + hearingAidStackEvent);
                if (!HearingAidStateMachine.this.mDevice.equals(hearingAidStackEvent.device)) {
                    Log.wtfStack(HearingAidStateMachine.TAG, "Device(" + HearingAidStateMachine.this.mDevice + "): event mismatch: " + hearingAidStackEvent);
                }
                if (hearingAidStackEvent.type == 1) {
                    processConnectionEvent(hearingAidStackEvent.valueInt1);
                } else {
                    Log.e(HearingAidStateMachine.TAG, "Disconnecting: ignoring stack event: " + hearingAidStackEvent);
                }
            } else if (i == HearingAidStateMachine.CONNECT_TIMEOUT) {
                Log.w(HearingAidStateMachine.TAG, "Disconnecting connection timeout: " + HearingAidStateMachine.this.mDevice);
                HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice);
                HearingAidStackEvent hearingAidStackEvent2 = new HearingAidStackEvent(1);
                hearingAidStackEvent2.device = HearingAidStateMachine.this.mDevice;
                hearingAidStackEvent2.valueInt1 = 0;
                HearingAidStateMachine.this.sendMessage(HearingAidStateMachine.STACK_EVENT, hearingAidStackEvent2);
            } else {
                switch (i) {
                    case 1:
                        HearingAidStateMachine.this.deferMessage(message);
                        break;
                    case 2:
                        HearingAidStateMachine.this.deferMessage(message);
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
                    Log.i(HearingAidStateMachine.TAG, "Disconnected: " + HearingAidStateMachine.this.mDevice);
                    HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnected);
                    break;
                case 1:
                    if (HearingAidStateMachine.this.mService.okToConnect(HearingAidStateMachine.this.mDevice)) {
                        Log.i(HearingAidStateMachine.TAG, "Disconnecting interrupted: try to reconnect: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mConnecting);
                    } else {
                        Log.w(HearingAidStateMachine.TAG, "Incoming HearingAid Connecting request rejected: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice);
                    }
                    break;
                case 2:
                    if (HearingAidStateMachine.this.mService.okToConnect(HearingAidStateMachine.this.mDevice)) {
                        Log.w(HearingAidStateMachine.TAG, "Disconnecting interrupted: device is connected: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mConnected);
                    } else {
                        Log.w(HearingAidStateMachine.TAG, "Incoming HearingAid Connected request rejected: " + HearingAidStateMachine.this.mDevice);
                        HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice);
                    }
                    break;
                case 3:
                    break;
                default:
                    Log.e(HearingAidStateMachine.TAG, "Incorrect state: " + i);
                    break;
            }
        }
    }

    @VisibleForTesting
    class Connected extends State {
        Connected() {
        }

        public void enter() {
            Log.i(HearingAidStateMachine.TAG, "Enter Connected(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.mConnectionState = 2;
            HearingAidStateMachine.this.removeDeferredMessages(1);
            HearingAidStateMachine.this.broadcastConnectionState(HearingAidStateMachine.this.mConnectionState, HearingAidStateMachine.this.mLastConnectionState);
        }

        public void exit() {
            HearingAidStateMachine.this.log("Exit Connected(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(HearingAidStateMachine.this.getCurrentMessage().what));
            HearingAidStateMachine.this.mLastConnectionState = 2;
        }

        public boolean processMessage(Message message) {
            HearingAidStateMachine.this.log("Connected process message(" + HearingAidStateMachine.this.mDevice + "): " + HearingAidStateMachine.messageWhatToString(message.what));
            int i = message.what;
            if (i != HearingAidStateMachine.STACK_EVENT) {
                switch (i) {
                    case 1:
                        Log.w(HearingAidStateMachine.TAG, "Connected: CONNECT ignored: " + HearingAidStateMachine.this.mDevice);
                        break;
                    case 2:
                        HearingAidStateMachine.this.log("Disconnecting from " + HearingAidStateMachine.this.mDevice);
                        if (HearingAidStateMachine.this.mNativeInterface.disconnectHearingAid(HearingAidStateMachine.this.mDevice)) {
                            HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnecting);
                        } else {
                            Log.e(HearingAidStateMachine.TAG, "Connected: error disconnecting from " + HearingAidStateMachine.this.mDevice);
                            HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnected);
                        }
                        break;
                    default:
                        return false;
                }
            } else {
                HearingAidStackEvent hearingAidStackEvent = (HearingAidStackEvent) message.obj;
                HearingAidStateMachine.this.log("Connected: stack event: " + hearingAidStackEvent);
                if (!HearingAidStateMachine.this.mDevice.equals(hearingAidStackEvent.device)) {
                    Log.wtfStack(HearingAidStateMachine.TAG, "Device(" + HearingAidStateMachine.this.mDevice + "): event mismatch: " + hearingAidStackEvent);
                }
                if (hearingAidStackEvent.type == 1) {
                    processConnectionEvent(hearingAidStackEvent.valueInt1);
                } else {
                    Log.e(HearingAidStateMachine.TAG, "Connected: ignoring stack event: " + hearingAidStackEvent);
                }
            }
            return true;
        }

        private void processConnectionEvent(int i) {
            if (i == 0) {
                Log.i(HearingAidStateMachine.TAG, "Disconnected from " + HearingAidStateMachine.this.mDevice);
                HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnected);
                return;
            }
            if (i != 3) {
                Log.e(HearingAidStateMachine.TAG, "Connection State Device: " + HearingAidStateMachine.this.mDevice + " bad state: " + i);
                return;
            }
            Log.i(HearingAidStateMachine.TAG, "Disconnecting from " + HearingAidStateMachine.this.mDevice);
            HearingAidStateMachine.this.transitionTo(HearingAidStateMachine.this.mDisconnecting);
        }
    }

    int getConnectionState() {
        return this.mConnectionState;
    }

    BluetoothDevice getDevice() {
        return this.mDevice;
    }

    synchronized boolean isConnected() {
        return getCurrentState() == this.mConnected;
    }

    private void broadcastConnectionState(int i, int i2) {
        log("Connection state " + this.mDevice + ": " + profileStateToString(i2) + "->" + profileStateToString(i));
        Intent intent = new Intent("android.bluetooth.hearingaid.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i2);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", this.mDevice);
        intent.addFlags(83886080);
        this.mService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
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

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mDevice: " + this.mDevice);
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
    }
}
