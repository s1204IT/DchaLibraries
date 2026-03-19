package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.bluetooth.hfp.HeadsetStateMachine;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

@VisibleForTesting
public class HeadsetStateMachine extends StateMachine {
    static final int CALL_STATE_CHANGED = 9;
    private static final int CLCC_RSP_TIMEOUT = 104;
    private static final int CLCC_RSP_TIMEOUT_MS = 5000;
    static final int CONNECT = 1;
    static final int CONNECT_AUDIO = 3;
    private static final int CONNECT_TIMEOUT = 201;
    static final int DEVICE_STATE_CHANGED = 10;
    static final int DIALING_OUT_RESULT = 14;
    static final int DISCONNECT = 2;
    static final int DISCONNECT_AUDIO = 4;
    private static final String HEADSET_AUDIO_FEATURE_OFF = "off";
    private static final String HEADSET_AUDIO_FEATURE_ON = "on";
    private static final String HEADSET_NAME = "bt_headset_name";
    private static final String HEADSET_NREC = "bt_headset_nrec";
    private static final String HEADSET_VGS = "bt_headset_vgs";
    private static final String HEADSET_WBS = "bt_wbs";
    static final int INTENT_CONNECTION_ACCESS_REPLY = 8;
    static final int INTENT_SCO_VOLUME_CHANGED = 7;
    static final int QUERY_PHONE_STATE_TIMEOUT = 105;
    static final int QUERY_PHONE_STATE_TIMEOUT_VALUE = 2000;
    static final int SEND_BSIR = 13;
    static final int SEND_CCLC_RESPONSE = 11;
    static final int SEND_VENDOR_SPECIFIC_RESULT_CODE = 12;
    static final int STACK_EVENT = 101;
    private static final String TAG = "HeadsetStateMachine";
    static final int VOICE_RECOGNITION_RESULT = 15;
    static final int VOICE_RECOGNITION_START = 5;
    static final int VOICE_RECOGNITION_STOP = 6;
    private final AdapterService mAdapterService;
    private HeadsetAgIndicatorEnableState mAgIndicatorEnableState;
    private final AudioConnecting mAudioConnecting;
    private final AudioDisconnecting mAudioDisconnecting;
    private final AudioOn mAudioOn;
    private final HashMap<String, String> mAudioParams;
    private boolean mCheckCleanupConnectionBroadcast;
    private final Connected mConnected;
    private final Connecting mConnecting;
    private long mConnectingTimestampMs;
    private HeadsetStateBase mCurrentState;
    private final BluetoothDevice mDevice;
    private final Disconnected mDisconnected;
    private final Disconnecting mDisconnecting;
    private final HeadsetService mHeadsetService;
    private int mMicVolume;
    private final HeadsetNativeInterface mNativeInterface;
    private boolean mNeedDialingOutReply;
    private final AtPhonebook mPhonebook;
    private HeadsetStateBase mPrevState;
    private int mSpeakerVolume;
    private final HeadsetSystemInterface mSystemInterface;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");

    @VisibleForTesting
    static int sConnectTimeoutMs = 30000;
    private static final HeadsetAgIndicatorEnableState DEFAULT_AG_INDICATOR_ENABLE_STATE = new HeadsetAgIndicatorEnableState(true, true, true, true);
    private static final Map<String, Integer> VENDOR_SPECIFIC_AT_COMMAND_COMPANY_ID = new HashMap();

    static {
        VENDOR_SPECIFIC_AT_COMMAND_COMPANY_ID.put("+XEVENT", 85);
        VENDOR_SPECIFIC_AT_COMMAND_COMPANY_ID.put("+ANDROID", 224);
        VENDOR_SPECIFIC_AT_COMMAND_COMPANY_ID.put("+XAPL", 76);
        VENDOR_SPECIFIC_AT_COMMAND_COMPANY_ID.put("+IPHONEACCEV", 76);
    }

    private HeadsetStateMachine(BluetoothDevice bluetoothDevice, Looper looper, HeadsetService headsetService, AdapterService adapterService, HeadsetNativeInterface headsetNativeInterface, HeadsetSystemInterface headsetSystemInterface) {
        super(TAG, (Looper) Objects.requireNonNull(looper, "looper cannot be null"));
        this.mDisconnected = new Disconnected();
        this.mConnecting = new Connecting();
        this.mDisconnecting = new Disconnecting();
        this.mConnected = new Connected();
        this.mAudioOn = new AudioOn();
        this.mAudioConnecting = new AudioConnecting();
        this.mAudioDisconnecting = new AudioDisconnecting();
        this.mCurrentState = null;
        this.mConnectingTimestampMs = Long.MIN_VALUE;
        this.mAudioParams = new HashMap<>();
        setDbg(DBG);
        this.mDevice = (BluetoothDevice) Objects.requireNonNull(bluetoothDevice, "device cannot be null");
        this.mHeadsetService = (HeadsetService) Objects.requireNonNull(headsetService, "headsetService cannot be null");
        this.mNativeInterface = (HeadsetNativeInterface) Objects.requireNonNull(headsetNativeInterface, "nativeInterface cannot be null");
        this.mSystemInterface = (HeadsetSystemInterface) Objects.requireNonNull(headsetSystemInterface, "systemInterface cannot be null");
        this.mAdapterService = (AdapterService) Objects.requireNonNull(adapterService, "AdapterService cannot be null");
        this.mPhonebook = new AtPhonebook(this.mHeadsetService, this.mNativeInterface);
        addState(this.mDisconnected);
        addState(this.mConnecting);
        addState(this.mDisconnecting);
        addState(this.mConnected);
        addState(this.mAudioOn);
        addState(this.mAudioConnecting);
        addState(this.mAudioDisconnecting);
        setInitialState(this.mDisconnected);
    }

    static HeadsetStateMachine make(BluetoothDevice bluetoothDevice, Looper looper, HeadsetService headsetService, AdapterService adapterService, HeadsetNativeInterface headsetNativeInterface, HeadsetSystemInterface headsetSystemInterface) {
        HeadsetStateMachine headsetStateMachine = new HeadsetStateMachine(bluetoothDevice, looper, headsetService, adapterService, headsetNativeInterface, headsetSystemInterface);
        headsetStateMachine.start();
        Log.i(TAG, "Created state machine " + headsetStateMachine + " for " + bluetoothDevice);
        return headsetStateMachine;
    }

    static void destroy(HeadsetStateMachine headsetStateMachine) {
        Log.i(TAG, "destroy");
        if (headsetStateMachine == null) {
            Log.w(TAG, "destroy(), stateMachine is null");
            return;
        }
        headsetStateMachine.cleanupBeforeQuit();
        headsetStateMachine.quitNow();
        headsetStateMachine.cleanup();
    }

    public void cleanup() {
        if (this.mPhonebook != null) {
            this.mPhonebook.cleanup();
        }
        this.mAudioParams.clear();
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "  mCurrentDevice: " + this.mDevice);
        ProfileService.println(sb, "  mCurrentState: " + getCurrentHeadsetState());
        ProfileService.println(sb, "  mPrevState: " + this.mPrevState);
        ProfileService.println(sb, "  mConnectionState: " + getConnectionState());
        ProfileService.println(sb, "  mAudioState: " + getAudioState());
        ProfileService.println(sb, "  mNeedDialingOutReply: " + this.mNeedDialingOutReply);
        ProfileService.println(sb, "  mSpeakerVolume: " + this.mSpeakerVolume);
        ProfileService.println(sb, "  mMicVolume: " + this.mMicVolume);
        ProfileService.println(sb, "  mConnectingTimestampMs(uptimeMillis): " + this.mConnectingTimestampMs);
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

    private abstract class HeadsetStateBase extends State {
        abstract int getAudioStateInt();

        abstract int getConnectionStateInt();

        public abstract void processConnectionEvent(Message message, int i);

        private HeadsetStateBase() {
        }

        public void enter() {
            stateLogD("enter...");
            HeadsetStateMachine.this.mCurrentState = this;
            Log.d(HeadsetStateMachine.TAG, "mCurrentState = " + HeadsetStateMachine.this.mCurrentState);
            if (!(this instanceof Disconnected) && HeadsetStateMachine.this.mPrevState == null) {
                throw new IllegalStateException("mPrevState is null on enter()");
            }
            enforceValidConnectionStateTransition();
        }

        public void exit() {
            stateLogD("exit...");
            HeadsetStateMachine.this.mPrevState = this;
        }

        public String toString() {
            return getName();
        }

        void broadcastStateTransitions() {
            if (HeadsetStateMachine.this.mPrevState != null) {
                if (getAudioStateInt() != HeadsetStateMachine.this.mPrevState.getAudioStateInt() || ((HeadsetStateMachine.this.mPrevState instanceof AudioDisconnecting) && (this instanceof AudioOn))) {
                    stateLogD("audio state changed: " + HeadsetStateMachine.this.mDevice + ": " + HeadsetStateMachine.this.mPrevState + " -> " + this);
                    broadcastAudioState(HeadsetStateMachine.this.mDevice, HeadsetStateMachine.this.mPrevState.getAudioStateInt(), getAudioStateInt());
                }
                if (getConnectionStateInt() != HeadsetStateMachine.this.mPrevState.getConnectionStateInt()) {
                    stateLogD("connection state changed: " + HeadsetStateMachine.this.mDevice + ": " + HeadsetStateMachine.this.mPrevState + " -> " + this);
                    broadcastConnectionState(HeadsetStateMachine.this.mDevice, HeadsetStateMachine.this.mPrevState.getConnectionStateInt(), getConnectionStateInt());
                }
            }
        }

        void broadcastConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) {
            stateLogD("broadcastConnectionState " + bluetoothDevice + ": " + i + "->" + i2);
            HeadsetStateMachine.this.mHeadsetService.onConnectionStateChangedFromStateMachine(bluetoothDevice, i, i2);
            Intent intent = new Intent("android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED");
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i);
            intent.putExtra("android.bluetooth.profile.extra.STATE", i2);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
            intent.addFlags(16777216);
            HeadsetStateMachine.this.mHeadsetService.sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
        }

        void broadcastAudioState(BluetoothDevice bluetoothDevice, int i, int i2) {
            stateLogD("broadcastAudioState: " + bluetoothDevice + ": " + i + "->" + i2);
            HeadsetStateMachine.this.mHeadsetService.onAudioStateChangedFromStateMachine(bluetoothDevice, i, i2);
            Intent intent = new Intent("android.bluetooth.headset.profile.action.AUDIO_STATE_CHANGED");
            intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i);
            intent.putExtra("android.bluetooth.profile.extra.STATE", i2);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
            HeadsetStateMachine.this.mHeadsetService.sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
        }

        void enforceValidConnectionStateTransition() {
            boolean z = false;
            if (this != HeadsetStateMachine.this.mDisconnected ? !(this != HeadsetStateMachine.this.mConnecting ? this != HeadsetStateMachine.this.mDisconnecting ? this != HeadsetStateMachine.this.mConnected ? this != HeadsetStateMachine.this.mAudioConnecting ? this != HeadsetStateMachine.this.mAudioDisconnecting ? this != HeadsetStateMachine.this.mAudioOn || (HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioConnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioDisconnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mConnected) : HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioOn : HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mConnected : HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mConnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioDisconnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mDisconnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioConnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioOn && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mDisconnected : HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mConnected && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioConnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioOn && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioDisconnecting : HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mDisconnected) : !(HeadsetStateMachine.this.mPrevState != null && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mConnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mDisconnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mConnected && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioOn && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioConnecting && HeadsetStateMachine.this.mPrevState != HeadsetStateMachine.this.mAudioDisconnecting)) {
                z = true;
            }
            if (!z) {
                throw new IllegalStateException("Invalid state transition from " + HeadsetStateMachine.this.mPrevState + " to " + this + " for device " + HeadsetStateMachine.this.mDevice);
            }
        }

        void stateLogD(String str) {
            HeadsetStateMachine.this.log(getName() + ": currentDevice=" + HeadsetStateMachine.this.mDevice + ", msg=" + str);
        }

        void stateLogW(String str) {
            HeadsetStateMachine.this.logw(getName() + ": currentDevice=" + HeadsetStateMachine.this.mDevice + ", msg=" + str);
        }

        void stateLogE(String str) {
            HeadsetStateMachine.this.loge(getName() + ": currentDevice=" + HeadsetStateMachine.this.mDevice + ", msg=" + str);
        }

        void stateLogV(String str) {
            HeadsetStateMachine.this.logv(getName() + ": currentDevice=" + HeadsetStateMachine.this.mDevice + ", msg=" + str);
        }

        void stateLogI(String str) {
            HeadsetStateMachine.this.logi(getName() + ": currentDevice=" + HeadsetStateMachine.this.mDevice + ", msg=" + str);
        }

        void stateLogWtfStack(String str) {
            Log.wtfStack(HeadsetStateMachine.TAG, getName() + ": " + str);
        }
    }

    class Disconnected extends HeadsetStateBase {
        Disconnected() {
            super();
        }

        @Override
        int getConnectionStateInt() {
            return 0;
        }

        @Override
        int getAudioStateInt() {
            return 10;
        }

        @Override
        public void enter() {
            super.enter();
            HeadsetStateMachine.this.mConnectingTimestampMs = Long.MIN_VALUE;
            HeadsetStateMachine.this.mPhonebook.resetAtState();
            HeadsetStateMachine.this.updateAgIndicatorEnableState(null);
            HeadsetStateMachine.this.mNeedDialingOutReply = false;
            HeadsetStateMachine.this.mAudioParams.clear();
            broadcastStateTransitions();
            if (HeadsetStateMachine.this.mPrevState != null && HeadsetStateMachine.this.mAdapterService.getBondState(HeadsetStateMachine.this.mDevice) == 10) {
                HeadsetStateMachine.this.getHandler().post(new Runnable() {
                    @Override
                    public final void run() {
                        HeadsetStateMachine.Disconnected disconnected = this.f$0;
                        HeadsetStateMachine.this.mHeadsetService.removeStateMachine(HeadsetStateMachine.this.mDevice);
                    }
                });
            }
        }

        public boolean processMessage(Message message) {
            stateLogD("processMessage:" + message.what);
            switch (message.what) {
                case 1:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                    stateLogD("Connecting to " + bluetoothDevice);
                    if (HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                        if (HeadsetStateMachine.this.mNativeInterface.connectHfp(bluetoothDevice)) {
                            HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnecting);
                        } else {
                            stateLogE("CONNECT failed for connectHfp(" + bluetoothDevice + ")");
                            broadcastConnectionState(bluetoothDevice, 0, 0);
                        }
                    } else {
                        stateLogE("CONNECT failed, device=" + bluetoothDevice + ", currentDevice=" + HeadsetStateMachine.this.mDevice);
                    }
                    return true;
                case 2:
                    return true;
                case 9:
                    stateLogD("Ignoring CALL_STATE_CHANGED event");
                    return true;
                case 10:
                    stateLogD("Ignoring DEVICE_STATE_CHANGED event");
                    return true;
                case HeadsetStateMachine.STACK_EVENT:
                    HeadsetStackEvent headsetStackEvent = (HeadsetStackEvent) message.obj;
                    stateLogD("STACK_EVENT: " + headsetStackEvent);
                    if (!HeadsetStateMachine.this.mDevice.equals(headsetStackEvent.device)) {
                        stateLogE("Event device does not match currentDevice[" + HeadsetStateMachine.this.mDevice + "], event: " + headsetStackEvent);
                    } else if (headsetStackEvent.type == 1) {
                        processConnectionEvent(message, headsetStackEvent.valueInt);
                    } else {
                        stateLogE("Unexpected stack event: " + headsetStackEvent);
                    }
                    return true;
                default:
                    stateLogE("Unexpected msg " + HeadsetStateMachine.getMessageName(message.what) + ": " + message);
                    return false;
            }
        }

        @Override
        public void processConnectionEvent(Message message, int i) {
            stateLogD("processConnectionEvent, state=" + i);
            if (i != 4) {
                switch (i) {
                    case 0:
                        stateLogW("ignore DISCONNECTED event");
                        break;
                    case 1:
                    case 2:
                        if (HeadsetStateMachine.this.mHeadsetService.okToAcceptConnection(HeadsetStateMachine.this.mDevice)) {
                            stateLogI("accept incoming connection");
                            HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnecting);
                        } else {
                            stateLogI("rejected incoming HF, HeadsetService null or priority=" + HeadsetStateMachine.this.mHeadsetService.getPriority(HeadsetStateMachine.this.mDevice) + " bondState=" + HeadsetStateMachine.this.mAdapterService.getBondState(HeadsetStateMachine.this.mDevice));
                            if (!HeadsetStateMachine.this.mNativeInterface.disconnectHfp(HeadsetStateMachine.this.mDevice)) {
                                stateLogE("failed to disconnect");
                            }
                            broadcastConnectionState(HeadsetStateMachine.this.mDevice, 0, 0);
                        }
                        break;
                    default:
                        stateLogE("Incorrect state: " + i);
                        break;
                }
            }
            stateLogW("Ignore DISCONNECTING event");
        }
    }

    class Connecting extends HeadsetStateBase {
        Connecting() {
            super();
        }

        @Override
        int getConnectionStateInt() {
            return 1;
        }

        @Override
        int getAudioStateInt() {
            return 10;
        }

        @Override
        public void enter() {
            super.enter();
            HeadsetStateMachine.this.mConnectingTimestampMs = SystemClock.uptimeMillis();
            HeadsetStateMachine.this.sendMessageDelayed(HeadsetStateMachine.CONNECT_TIMEOUT, HeadsetStateMachine.this.mDevice, HeadsetStateMachine.sConnectTimeoutMs);
            broadcastStateTransitions();
        }

        public boolean processMessage(Message message) {
            stateLogD("processMessage:" + message.what);
            int i = message.what;
            if (i != HeadsetStateMachine.STACK_EVENT) {
                if (i == HeadsetStateMachine.CLCC_RSP_TIMEOUT) {
                    stateLogW("receive CLCC_RSP_TIMEOUT at connecting state!");
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                    if (HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                        HeadsetStateMachine.this.mNativeInterface.clccResponse(bluetoothDevice, 0, 0, 0, 0, false, "", 0);
                        return true;
                    }
                    stateLogW("CLCC_RSP_TIMEOUT failed " + bluetoothDevice + " is not currentDevice");
                    return true;
                }
                if (i != HeadsetStateMachine.CONNECT_TIMEOUT) {
                    switch (i) {
                        case 1:
                        case 2:
                        case 3:
                            HeadsetStateMachine.this.deferMessage(message);
                            return true;
                        default:
                            switch (i) {
                                case 9:
                                    stateLogD("ignoring CALL_STATE_CHANGED event");
                                    return true;
                                case 10:
                                    stateLogD("ignoring DEVICE_STATE_CHANGED event");
                                    return true;
                                case 11:
                                    stateLogW("receive SEND_CCLC_RESPONSE at connecting state!");
                                    HeadsetStateMachine.this.processSendClccResponse((HeadsetClccResponse) message.obj);
                                    return true;
                                default:
                                    stateLogE("Unexpected msg " + HeadsetStateMachine.getMessageName(message.what) + ": " + message);
                                    return false;
                            }
                    }
                }
                BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice2)) {
                    stateLogE("Unknown device timeout " + bluetoothDevice2);
                    return true;
                }
                stateLogW("CONNECT_TIMEOUT");
                HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mDisconnected);
                return true;
            }
            HeadsetStackEvent headsetStackEvent = (HeadsetStackEvent) message.obj;
            stateLogD("STACK_EVENT: " + headsetStackEvent);
            if (!HeadsetStateMachine.this.mDevice.equals(headsetStackEvent.device)) {
                stateLogE("Event device does not match currentDevice[" + HeadsetStateMachine.this.mDevice + "], event: " + headsetStackEvent);
                return true;
            }
            switch (headsetStackEvent.type) {
                case 1:
                    processConnectionEvent(message, headsetStackEvent.valueInt);
                    return true;
                case 2:
                case 8:
                case 9:
                default:
                    stateLogE("Unexpected event: " + headsetStackEvent);
                    return true;
                case 3:
                    stateLogW("Unexpected VR event, device=" + headsetStackEvent.device + ", state=" + headsetStackEvent.valueInt);
                    HeadsetStateMachine.this.processVrEvent(headsetStackEvent.valueInt);
                    return true;
                case 4:
                    stateLogW("Unexpected answer event for " + headsetStackEvent.device);
                    HeadsetStateMachine.this.mSystemInterface.answerCall(headsetStackEvent.device);
                    return true;
                case 5:
                    stateLogW("Unexpected hangup event for " + headsetStackEvent.device);
                    HeadsetStateMachine.this.mSystemInterface.hangupCall(headsetStackEvent.device);
                    return true;
                case 6:
                    stateLogW("Unexpected volume event for " + headsetStackEvent.device);
                    HeadsetStateMachine.this.processVolumeEvent(headsetStackEvent.valueInt, headsetStackEvent.valueInt2);
                    return true;
                case 7:
                    stateLogW("Unexpected dial event, device=" + headsetStackEvent.device);
                    HeadsetStateMachine.this.processDialCall(headsetStackEvent.valueString);
                    return true;
                case 10:
                    HeadsetStateMachine.this.processAtChld(headsetStackEvent.valueInt, headsetStackEvent.device);
                    return true;
                case 11:
                    stateLogW("Unexpected subscriber number event for" + headsetStackEvent.device + ", state=" + headsetStackEvent.valueInt);
                    HeadsetStateMachine.this.processSubscriberNumberRequest(headsetStackEvent.device);
                    return true;
                case 12:
                    HeadsetStateMachine.this.processAtCind(headsetStackEvent.device);
                    return true;
                case 13:
                    stateLogW("Unexpected COPS event for " + headsetStackEvent.device);
                    HeadsetStateMachine.this.processAtCops(headsetStackEvent.device);
                    return true;
                case 14:
                    Log.w(HeadsetStateMachine.TAG, "Connecting: Unexpected CLCC event for" + headsetStackEvent.device);
                    HeadsetStateMachine.this.processAtClcc(headsetStackEvent.device);
                    return true;
                case 15:
                    stateLogW("Unexpected unknown AT event for" + headsetStackEvent.device + ", cmd=" + headsetStackEvent.valueString);
                    HeadsetStateMachine.this.processUnknownAt(headsetStackEvent.valueString, headsetStackEvent.device);
                    return true;
                case 16:
                    stateLogW("Unexpected key-press event for " + headsetStackEvent.device);
                    HeadsetStateMachine.this.processKeyPressed(headsetStackEvent.device);
                    return true;
                case 17:
                    HeadsetStateMachine.this.processWBSEvent(headsetStackEvent.valueInt);
                    return true;
                case 18:
                    HeadsetStateMachine.this.processAtBind(headsetStackEvent.valueString, headsetStackEvent.device);
                    return true;
                case 19:
                    stateLogW("Unexpected BIEV event for " + headsetStackEvent.device + ", indId=" + headsetStackEvent.valueInt + ", indVal=" + headsetStackEvent.valueInt2);
                    HeadsetStateMachine.this.processAtBiev(headsetStackEvent.valueInt, headsetStackEvent.valueInt2, headsetStackEvent.device);
                    return true;
            }
        }

        @Override
        public void processConnectionEvent(Message message, int i) {
            stateLogD("processConnectionEvent, state=" + i);
            switch (i) {
                case 0:
                    stateLogW("Disconnected");
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mDisconnected);
                    break;
                case 1:
                    break;
                case 2:
                    stateLogD("RFCOMM connected");
                    break;
                case 3:
                    stateLogD("SLC connected");
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnected);
                    break;
                case 4:
                    stateLogW("Disconnecting");
                    break;
                default:
                    stateLogE("Incorrect state " + i);
                    break;
            }
        }

        @Override
        public void exit() {
            HeadsetStateMachine.this.removeMessages(HeadsetStateMachine.CONNECT_TIMEOUT);
            super.exit();
        }
    }

    class Disconnecting extends HeadsetStateBase {
        Disconnecting() {
            super();
        }

        @Override
        int getConnectionStateInt() {
            return 3;
        }

        @Override
        int getAudioStateInt() {
            return 10;
        }

        @Override
        public void enter() {
            super.enter();
            HeadsetStateMachine.this.sendMessageDelayed(HeadsetStateMachine.CONNECT_TIMEOUT, HeadsetStateMachine.this.mDevice, HeadsetStateMachine.sConnectTimeoutMs);
            broadcastStateTransitions();
        }

        public boolean processMessage(Message message) {
            stateLogD("processMessage:" + message.what);
            int i = message.what;
            if (i == HeadsetStateMachine.STACK_EVENT) {
                HeadsetStackEvent headsetStackEvent = (HeadsetStackEvent) message.obj;
                stateLogD("STACK_EVENT: " + headsetStackEvent);
                if (!HeadsetStateMachine.this.mDevice.equals(headsetStackEvent.device)) {
                    stateLogE("Event device does not match currentDevice[" + HeadsetStateMachine.this.mDevice + "], event: " + headsetStackEvent);
                } else if (headsetStackEvent.type == 1) {
                    processConnectionEvent(message, headsetStackEvent.valueInt);
                } else {
                    stateLogE("Unexpected event: " + headsetStackEvent);
                }
            } else if (i != HeadsetStateMachine.CONNECT_TIMEOUT) {
                switch (i) {
                    case 1:
                    case 2:
                    case 3:
                        HeadsetStateMachine.this.deferMessage(message);
                        break;
                    default:
                        stateLogE("Unexpected msg " + HeadsetStateMachine.getMessageName(message.what) + ": " + message);
                        return false;
                }
            } else {
                BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                    stateLogE("Unknown device timeout " + bluetoothDevice);
                } else {
                    stateLogE("timeout");
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mDisconnected);
                }
            }
            return true;
        }

        @Override
        public void processConnectionEvent(Message message, int i) {
            if (i == 0) {
                stateLogD("processConnectionEvent: Disconnected");
                HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mDisconnected);
            } else if (i == 3) {
                stateLogD("processConnectionEvent: Connected");
                HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnected);
            } else {
                stateLogE("processConnectionEvent: Bad state: " + i);
            }
        }

        @Override
        public void exit() {
            HeadsetStateMachine.this.removeMessages(HeadsetStateMachine.CONNECT_TIMEOUT);
            super.exit();
        }
    }

    private abstract class ConnectedBase extends HeadsetStateBase {
        public abstract void processAudioEvent(int i);

        private ConnectedBase() {
            super();
        }

        @Override
        int getConnectionStateInt() {
            return 2;
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != HeadsetStateMachine.STACK_EVENT) {
                if (i != HeadsetStateMachine.CLCC_RSP_TIMEOUT) {
                    if (i != HeadsetStateMachine.CONNECT_TIMEOUT) {
                        switch (i) {
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                                break;
                            case 5:
                                BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                                if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                                    stateLogW("VOICE_RECOGNITION_START failed " + bluetoothDevice + " is not currentDevice");
                                } else if (!HeadsetStateMachine.this.mNativeInterface.startVoiceRecognition(HeadsetStateMachine.this.mDevice)) {
                                    stateLogW("Failed to start voice recognition");
                                }
                                break;
                            case 6:
                                BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                                if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice2)) {
                                    stateLogW("VOICE_RECOGNITION_STOP failed " + bluetoothDevice2 + " is not currentDevice");
                                } else if (!HeadsetStateMachine.this.mNativeInterface.stopVoiceRecognition(HeadsetStateMachine.this.mDevice)) {
                                    stateLogW("Failed to stop voice recognition");
                                }
                                break;
                            default:
                                switch (i) {
                                    case 8:
                                        HeadsetStateMachine.this.handleAccessPermissionResult((Intent) message.obj);
                                        break;
                                    case 9:
                                        HeadsetStateMachine.this.removeMessages(HeadsetStateMachine.QUERY_PHONE_STATE_TIMEOUT);
                                        HeadsetCallState headsetCallState = (HeadsetCallState) message.obj;
                                        if (!HeadsetStateMachine.this.mNativeInterface.phoneStateChange(HeadsetStateMachine.this.mDevice, headsetCallState)) {
                                            stateLogW("processCallState: failed to update call state " + headsetCallState);
                                        }
                                        break;
                                    case 10:
                                        HeadsetStateMachine.this.mNativeInterface.notifyDeviceStatus(HeadsetStateMachine.this.mDevice, (HeadsetDeviceState) message.obj);
                                        break;
                                    case 11:
                                        HeadsetStateMachine.this.processSendClccResponse((HeadsetClccResponse) message.obj);
                                        break;
                                    case 12:
                                        HeadsetStateMachine.this.processSendVendorSpecificResultCode((HeadsetVendorSpecificResultCode) message.obj);
                                        break;
                                    case 13:
                                        HeadsetStateMachine.this.mNativeInterface.sendBsir(HeadsetStateMachine.this.mDevice, message.arg1 == 1);
                                        break;
                                    case 14:
                                        BluetoothDevice bluetoothDevice3 = (BluetoothDevice) message.obj;
                                        if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice3)) {
                                            stateLogW("DIALING_OUT_RESULT failed " + bluetoothDevice3 + " is not currentDevice");
                                        } else if (HeadsetStateMachine.this.mNeedDialingOutReply) {
                                            HeadsetStateMachine.this.mNeedDialingOutReply = false;
                                            HeadsetStateMachine.this.mNativeInterface.atResponseCode(HeadsetStateMachine.this.mDevice, message.arg1 == 1 ? 1 : 0, 0);
                                        }
                                        break;
                                    case 15:
                                        BluetoothDevice bluetoothDevice4 = (BluetoothDevice) message.obj;
                                        if (HeadsetStateMachine.this.mDevice.equals(bluetoothDevice4)) {
                                            HeadsetStateMachine.this.mNativeInterface.atResponseCode(HeadsetStateMachine.this.mDevice, message.arg1 == 1 ? 1 : 0, 0);
                                        } else {
                                            stateLogW("VOICE_RECOGNITION_RESULT failed " + bluetoothDevice4 + " is not currentDevice");
                                        }
                                        break;
                                    default:
                                        stateLogE("Unexpected msg " + HeadsetStateMachine.getMessageName(message.what) + ": " + message);
                                        return false;
                                }
                                break;
                        }
                    }
                    throw new IllegalStateException("Illegal message in generic handler: " + message);
                }
                BluetoothDevice bluetoothDevice5 = (BluetoothDevice) message.obj;
                if (HeadsetStateMachine.this.mDevice.equals(bluetoothDevice5)) {
                    HeadsetStateMachine.this.mNativeInterface.clccResponse(bluetoothDevice5, 0, 0, 0, 0, false, "", 0);
                } else {
                    stateLogW("CLCC_RSP_TIMEOUT failed " + bluetoothDevice5 + " is not currentDevice");
                }
            } else {
                HeadsetStackEvent headsetStackEvent = (HeadsetStackEvent) message.obj;
                stateLogD("STACK_EVENT: " + headsetStackEvent);
                if (HeadsetStateMachine.this.mDevice.equals(headsetStackEvent.device)) {
                    switch (headsetStackEvent.type) {
                        case 1:
                            processConnectionEvent(message, headsetStackEvent.valueInt);
                            break;
                        case 2:
                            processAudioEvent(headsetStackEvent.valueInt);
                            break;
                        case 3:
                            HeadsetStateMachine.this.processVrEvent(headsetStackEvent.valueInt);
                            break;
                        case 4:
                            HeadsetStateMachine.this.mSystemInterface.answerCall(headsetStackEvent.device);
                            break;
                        case 5:
                            HeadsetStateMachine.this.mSystemInterface.hangupCall(headsetStackEvent.device);
                            break;
                        case 6:
                            HeadsetStateMachine.this.processVolumeEvent(headsetStackEvent.valueInt, headsetStackEvent.valueInt2);
                            break;
                        case 7:
                            HeadsetStateMachine.this.processDialCall(headsetStackEvent.valueString);
                            break;
                        case 8:
                            HeadsetStateMachine.this.mSystemInterface.sendDtmf(headsetStackEvent.valueInt, headsetStackEvent.device);
                            break;
                        case 9:
                            HeadsetStateMachine.this.processNoiseReductionEvent(headsetStackEvent.valueInt == 1);
                            break;
                        case 10:
                            HeadsetStateMachine.this.processAtChld(headsetStackEvent.valueInt, headsetStackEvent.device);
                            break;
                        case 11:
                            HeadsetStateMachine.this.processSubscriberNumberRequest(headsetStackEvent.device);
                            break;
                        case 12:
                            HeadsetStateMachine.this.processAtCind(headsetStackEvent.device);
                            break;
                        case 13:
                            HeadsetStateMachine.this.processAtCops(headsetStackEvent.device);
                            break;
                        case 14:
                            HeadsetStateMachine.this.processAtClcc(headsetStackEvent.device);
                            break;
                        case 15:
                            HeadsetStateMachine.this.processUnknownAt(headsetStackEvent.valueString, headsetStackEvent.device);
                            break;
                        case 16:
                            HeadsetStateMachine.this.processKeyPressed(headsetStackEvent.device);
                            break;
                        case 17:
                            HeadsetStateMachine.this.processWBSEvent(headsetStackEvent.valueInt);
                            break;
                        case 18:
                            HeadsetStateMachine.this.processAtBind(headsetStackEvent.valueString, headsetStackEvent.device);
                            break;
                        case 19:
                            HeadsetStateMachine.this.processAtBiev(headsetStackEvent.valueInt, headsetStackEvent.valueInt2, headsetStackEvent.device);
                            break;
                        case 20:
                            HeadsetStateMachine.this.updateAgIndicatorEnableState((HeadsetAgIndicatorEnableState) headsetStackEvent.valueObject);
                            break;
                        default:
                            stateLogE("Unknown stack event: " + headsetStackEvent);
                            break;
                    }
                } else {
                    stateLogE("Event device does not match currentDevice[" + HeadsetStateMachine.this.mDevice + "], event: " + headsetStackEvent);
                }
            }
            return true;
        }

        @Override
        public void processConnectionEvent(Message message, int i) {
            stateLogD("processConnectionEvent, state=" + i);
            if (i != 0) {
                switch (i) {
                    case 2:
                        stateLogE("processConnectionEvent: RFCOMM connected again, shouldn't happen");
                        break;
                    case 3:
                        stateLogE("processConnectionEvent: SLC connected again, shouldn't happen");
                        break;
                    case 4:
                        stateLogI("processConnectionEvent: Disconnecting");
                        HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mDisconnecting);
                        break;
                    default:
                        stateLogE("processConnectionEvent: bad state: " + i);
                        break;
                }
            }
            stateLogI("processConnectionEvent: Disconnected");
            HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mDisconnected);
        }
    }

    class Connected extends ConnectedBase {
        Connected() {
            super();
        }

        @Override
        int getAudioStateInt() {
            return 10;
        }

        @Override
        public void enter() {
            super.enter();
            if (HeadsetStateMachine.this.mConnectingTimestampMs == Long.MIN_VALUE) {
                HeadsetStateMachine.this.mConnectingTimestampMs = SystemClock.uptimeMillis();
            }
            if (HeadsetStateMachine.this.mPrevState == HeadsetStateMachine.this.mConnecting) {
                HeadsetStateMachine.this.updateAgIndicatorEnableState(HeadsetStateMachine.DEFAULT_AG_INDICATOR_ENABLE_STATE);
                HeadsetStateMachine.this.processNoiseReductionEvent(true);
                HeadsetStateMachine.this.mSystemInterface.queryPhoneState();
                Message messageObtainMessage = HeadsetStateMachine.this.obtainMessage(HeadsetStateMachine.QUERY_PHONE_STATE_TIMEOUT);
                Log.d(HeadsetStateMachine.TAG, "set QUERY_PHONE_STATE_TIMEOUT!!");
                HeadsetStateMachine.this.sendMessageDelayed(messageObtainMessage, 2000L);
                HeadsetStateMachine.this.removeDeferredMessages(1);
            }
            broadcastStateTransitions();
        }

        @Override
        public boolean processMessage(Message message) {
            stateLogD("processMessage:" + message.what);
            int i = message.what;
            if (i == HeadsetStateMachine.QUERY_PHONE_STATE_TIMEOUT) {
                HeadsetStateMachine.this.mSystemInterface.queryPhoneState();
                return true;
            }
            switch (i) {
                case 1:
                    stateLogW("CONNECT, ignored, device=" + ((BluetoothDevice) message.obj) + ", currentDevice" + HeadsetStateMachine.this.mDevice);
                    return true;
                case 2:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                    stateLogD("DISCONNECT from device=" + bluetoothDevice);
                    if (HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                        if (HeadsetStateMachine.this.mNativeInterface.disconnectHfp(bluetoothDevice)) {
                            HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mDisconnecting);
                            return true;
                        }
                        stateLogE("DISCONNECT from " + bluetoothDevice + " failed");
                        broadcastConnectionState(bluetoothDevice, 2, 2);
                        return true;
                    }
                    stateLogW("DISCONNECT, device " + bluetoothDevice + " not connected");
                    return true;
                case 3:
                    stateLogD("CONNECT_AUDIO, device=" + HeadsetStateMachine.this.mDevice);
                    HeadsetStateMachine.this.mSystemInterface.getAudioManager().setParameters("A2dpSuspended=true");
                    if (!HeadsetStateMachine.this.mNativeInterface.connectAudio(HeadsetStateMachine.this.mDevice)) {
                        HeadsetStateMachine.this.mSystemInterface.getAudioManager().setParameters("A2dpSuspended=false");
                        stateLogE("Failed to connect SCO audio for " + HeadsetStateMachine.this.mDevice);
                        broadcastAudioState(HeadsetStateMachine.this.mDevice, 10, 10);
                        return true;
                    }
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioConnecting);
                    return true;
                case 4:
                    stateLogD("ignore DISCONNECT_AUDIO, device=" + HeadsetStateMachine.this.mDevice);
                    return true;
                default:
                    return super.processMessage(message);
            }
        }

        @Override
        public void processAudioEvent(int i) {
            stateLogD("processAudioEvent, state=" + i);
            switch (i) {
                case 0:
                case 3:
                    break;
                case 1:
                    if (!HeadsetStateMachine.this.mHeadsetService.isScoAcceptable(HeadsetStateMachine.this.mDevice)) {
                        stateLogW("processAudioEvent: reject incoming pending audio connection");
                        if (!HeadsetStateMachine.this.mNativeInterface.disconnectAudio(HeadsetStateMachine.this.mDevice)) {
                            stateLogE("processAudioEvent: failed to disconnect pending audio");
                        }
                        broadcastAudioState(HeadsetStateMachine.this.mDevice, 10, 10);
                    } else {
                        stateLogI("processAudioEvent: audio connecting");
                        HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioConnecting);
                    }
                    break;
                case 2:
                    if (!HeadsetStateMachine.this.mHeadsetService.isScoAcceptable(HeadsetStateMachine.this.mDevice)) {
                        stateLogW("processAudioEvent: reject incoming audio connection");
                        if (!HeadsetStateMachine.this.mNativeInterface.disconnectAudio(HeadsetStateMachine.this.mDevice)) {
                            stateLogE("processAudioEvent: failed to disconnect audio");
                        }
                        broadcastAudioState(HeadsetStateMachine.this.mDevice, 10, 10);
                    } else {
                        stateLogI("processAudioEvent: audio connected");
                        HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioOn);
                    }
                    break;
                default:
                    stateLogE("processAudioEvent: bad state: " + i);
                    break;
            }
        }

        @Override
        public void exit() {
            HeadsetStateMachine.this.removeMessages(HeadsetStateMachine.QUERY_PHONE_STATE_TIMEOUT);
            super.exit();
        }
    }

    class AudioConnecting extends ConnectedBase {
        AudioConnecting() {
            super();
        }

        @Override
        int getAudioStateInt() {
            return 11;
        }

        @Override
        public void enter() {
            super.enter();
            HeadsetStateMachine.this.sendMessageDelayed(HeadsetStateMachine.CONNECT_TIMEOUT, HeadsetStateMachine.this.mDevice, HeadsetStateMachine.sConnectTimeoutMs);
            broadcastStateTransitions();
        }

        @Override
        public boolean processMessage(Message message) {
            stateLogD("processMessage:" + message.what);
            int i = message.what;
            if (i != HeadsetStateMachine.CONNECT_TIMEOUT) {
                switch (i) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        HeadsetStateMachine.this.deferMessage(message);
                        return true;
                    default:
                        return super.processMessage(message);
                }
            }
            BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
            if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                stateLogW("CONNECT_TIMEOUT for unknown device " + bluetoothDevice);
                return true;
            }
            stateLogW("CONNECT_TIMEOUT");
            HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnected);
            return true;
        }

        @Override
        public void processAudioEvent(int i) {
            switch (i) {
                case 0:
                    stateLogW("processAudioEvent: audio connection failed");
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnected);
                    break;
                case 1:
                case 3:
                    break;
                case 2:
                    stateLogI("processAudioEvent: audio connected");
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioOn);
                    break;
                default:
                    stateLogE("processAudioEvent: bad state: " + i);
                    break;
            }
        }

        @Override
        public void exit() {
            HeadsetStateMachine.this.removeMessages(HeadsetStateMachine.CONNECT_TIMEOUT);
            super.exit();
        }
    }

    class AudioOn extends ConnectedBase {
        AudioOn() {
            super();
        }

        @Override
        int getAudioStateInt() {
            return 12;
        }

        @Override
        public void enter() {
            super.enter();
            HeadsetStateMachine.this.removeDeferredMessages(3);
            if (!HeadsetStateMachine.this.mDevice.equals(HeadsetStateMachine.this.mHeadsetService.getActiveDevice())) {
                HeadsetStateMachine.this.mHeadsetService.setActiveDevice(HeadsetStateMachine.this.mDevice);
            }
            HeadsetStateMachine.this.setAudioParameters();
            broadcastStateTransitions();
        }

        @Override
        public boolean processMessage(Message message) {
            stateLogD("processMessage:" + message.what);
            int i = message.what;
            if (i == 7) {
                processIntentScoVolume((Intent) message.obj, HeadsetStateMachine.this.mDevice);
                return true;
            }
            if (i != HeadsetStateMachine.STACK_EVENT) {
                switch (i) {
                    case 1:
                        stateLogW("CONNECT, ignored, device=" + ((BluetoothDevice) message.obj) + ", currentDevice" + HeadsetStateMachine.this.mDevice);
                        return true;
                    case 2:
                        BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
                        stateLogD("DISCONNECT, device=" + bluetoothDevice);
                        if (HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                            if (!HeadsetStateMachine.this.mNativeInterface.disconnectAudio(HeadsetStateMachine.this.mDevice)) {
                                stateLogW("DISCONNECT failed, device=" + HeadsetStateMachine.this.mDevice);
                            }
                            HeadsetStateMachine.this.deferMessage(HeadsetStateMachine.this.obtainMessage(2, HeadsetStateMachine.this.mDevice));
                            HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioDisconnecting);
                            return true;
                        }
                        stateLogW("DISCONNECT, device " + bluetoothDevice + " not connected");
                        return true;
                    case 3:
                        BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                        if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice2)) {
                            stateLogW("CONNECT_AUDIO device is not connected " + bluetoothDevice2);
                            return true;
                        }
                        stateLogW("CONNECT_AUDIO device auido is already connected " + bluetoothDevice2);
                        return true;
                    case 4:
                        BluetoothDevice bluetoothDevice3 = (BluetoothDevice) message.obj;
                        if (HeadsetStateMachine.this.mDevice.equals(bluetoothDevice3)) {
                            if (HeadsetStateMachine.this.mNativeInterface.disconnectAudio(HeadsetStateMachine.this.mDevice)) {
                                stateLogD("DISCONNECT_AUDIO, device=" + HeadsetStateMachine.this.mDevice);
                                HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioDisconnecting);
                                return true;
                            }
                            stateLogW("DISCONNECT_AUDIO failed, device=" + HeadsetStateMachine.this.mDevice);
                            broadcastAudioState(HeadsetStateMachine.this.mDevice, 12, 12);
                            return true;
                        }
                        stateLogW("DISCONNECT_AUDIO, failed, device=" + bluetoothDevice3 + ", currentDevice=" + HeadsetStateMachine.this.mDevice);
                        return true;
                    default:
                        return super.processMessage(message);
                }
            }
            HeadsetStackEvent headsetStackEvent = (HeadsetStackEvent) message.obj;
            stateLogD("STACK_EVENT: " + headsetStackEvent);
            if (!HeadsetStateMachine.this.mDevice.equals(headsetStackEvent.device)) {
                stateLogE("Event device does not match currentDevice[" + HeadsetStateMachine.this.mDevice + "], event: " + headsetStackEvent);
                return true;
            }
            if (headsetStackEvent.type == 17) {
                stateLogE("Cannot change WBS state when audio is connected: " + headsetStackEvent);
                return true;
            }
            super.processMessage(message);
            return true;
        }

        @Override
        public void processAudioEvent(int i) {
            if (i == 0) {
                stateLogI("processAudioEvent: audio disconnected by remote");
                HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnected);
            } else if (i == 3) {
                stateLogI("processAudioEvent: audio being disconnected by remote");
                HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioDisconnecting);
            } else {
                stateLogE("processAudioEvent: bad state: " + i);
            }
        }

        private void processIntentScoVolume(Intent intent, BluetoothDevice bluetoothDevice) {
            int intExtra = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 0);
            if (HeadsetStateMachine.this.mSpeakerVolume != intExtra) {
                HeadsetStateMachine.this.mSpeakerVolume = intExtra;
                HeadsetStateMachine.this.mNativeInterface.setVolume(bluetoothDevice, 0, HeadsetStateMachine.this.mSpeakerVolume);
            }
        }
    }

    class AudioDisconnecting extends ConnectedBase {
        AudioDisconnecting() {
            super();
        }

        @Override
        int getAudioStateInt() {
            return 12;
        }

        @Override
        public void enter() {
            super.enter();
            HeadsetStateMachine.this.sendMessageDelayed(HeadsetStateMachine.CONNECT_TIMEOUT, HeadsetStateMachine.this.mDevice, HeadsetStateMachine.sConnectTimeoutMs);
            broadcastStateTransitions();
        }

        @Override
        public boolean processMessage(Message message) {
            stateLogD("processMessage:" + message.what);
            int i = message.what;
            if (i != HeadsetStateMachine.CONNECT_TIMEOUT) {
                switch (i) {
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        HeadsetStateMachine.this.deferMessage(message);
                        return true;
                    default:
                        return super.processMessage(message);
                }
            }
            BluetoothDevice bluetoothDevice = (BluetoothDevice) message.obj;
            if (!HeadsetStateMachine.this.mDevice.equals(bluetoothDevice)) {
                stateLogW("CONNECT_TIMEOUT for unknown device " + bluetoothDevice);
                return true;
            }
            stateLogW("CONNECT_TIMEOUT");
            HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnected);
            return true;
        }

        @Override
        public void processAudioEvent(int i) {
            switch (i) {
                case 0:
                    stateLogI("processAudioEvent: audio disconnected");
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mConnected);
                    break;
                case 1:
                case 3:
                    break;
                case 2:
                    stateLogW("processAudioEvent: audio disconnection failed");
                    HeadsetStateMachine.this.transitionTo(HeadsetStateMachine.this.mAudioOn);
                    break;
                default:
                    stateLogE("processAudioEvent: bad state: " + i);
                    break;
            }
        }

        @Override
        public void exit() {
            HeadsetStateMachine.this.removeMessages(HeadsetStateMachine.CONNECT_TIMEOUT);
            super.exit();
        }
    }

    @VisibleForTesting
    public synchronized BluetoothDevice getDevice() {
        return this.mDevice;
    }

    @VisibleForTesting
    public synchronized int getConnectionState() {
        HeadsetStateBase currentHeadsetState = getCurrentHeadsetState();
        if (currentHeadsetState == null) {
            return 0;
        }
        return currentHeadsetState.getConnectionStateInt();
    }

    public synchronized int getAudioState() {
        HeadsetStateBase currentHeadsetState = getCurrentHeadsetState();
        if (currentHeadsetState == null) {
            return 10;
        }
        return currentHeadsetState.getAudioStateInt();
    }

    public long getConnectingTimestampMs() {
        return this.mConnectingTimestampMs;
    }

    private void broadcastVendorSpecificEventIntent(String str, int i, int i2, Object[] objArr, BluetoothDevice bluetoothDevice) {
        log("broadcastVendorSpecificEventIntent(" + str + ")");
        Intent intent = new Intent("android.bluetooth.headset.action.VENDOR_SPECIFIC_HEADSET_EVENT");
        intent.putExtra("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD", str);
        intent.putExtra("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE", i2);
        intent.putExtra("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_ARGS", (Serializable) objArr);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addCategory("android.bluetooth.headset.intent.category.companyid." + Integer.toString(i));
        this.mHeadsetService.sendBroadcastAsUser(intent, UserHandle.ALL, ProfileService.BLUETOOTH_PERM);
    }

    private void setAudioParameters() {
        String str = SystemProperties.get("persist.vendor.bluetooth.hfp.vol");
        String str2 = HEADSET_AUDIO_FEATURE_OFF;
        if (str.isEmpty() || Integer.parseInt(str) == 1) {
            Log.d(TAG, "Set VGS: on for device: " + this.mDevice);
            str2 = HEADSET_AUDIO_FEATURE_ON;
        } else {
            Log.d(TAG, "Set VGS: off for device: " + this.mDevice);
        }
        String strJoin = String.join(";", "bt_headset_name=" + getCurrentDeviceName(), "bt_headset_nrec=" + this.mAudioParams.getOrDefault(HEADSET_NREC, HEADSET_AUDIO_FEATURE_OFF), "bt_headset_vgs=" + str2, "bt_wbs=" + this.mAudioParams.getOrDefault(HEADSET_WBS, HEADSET_AUDIO_FEATURE_OFF));
        StringBuilder sb = new StringBuilder();
        sb.append("setAudioParameters for ");
        sb.append(this.mDevice);
        sb.append(": ");
        sb.append(strJoin);
        Log.i(TAG, sb.toString());
        this.mSystemInterface.getAudioManager().setParameters(strJoin);
    }

    private String parseUnknownAt(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        int i = 0;
        while (true) {
            if (i >= str.length()) {
                break;
            }
            char cCharAt = str.charAt(i);
            if (cCharAt == '\"') {
                int iIndexOf = str.indexOf(34, i + 1);
                if (iIndexOf == -1) {
                    sb.append(str.substring(i, str.length()));
                    sb.append('\"');
                    break;
                }
                sb.append(str.substring(i, iIndexOf + 1));
                i = iIndexOf;
            } else if (cCharAt != ' ') {
                sb.append(Character.toUpperCase(cCharAt));
            }
            i++;
        }
        return sb.toString();
    }

    private int getAtCommandType(String str) {
        String strTrim = str.trim();
        if (strTrim.length() <= 5) {
            return -1;
        }
        String strSubstring = strTrim.substring(5);
        if (strSubstring.startsWith("?")) {
            return 0;
        }
        if (strSubstring.startsWith("=?")) {
            return 2;
        }
        return strSubstring.startsWith("=") ? 1 : -1;
    }

    private void processDialCall(String str) {
        String lastDialledNumber;
        if (this.mHeadsetService.hasDeviceInitiatedDialingOut()) {
            Log.w(TAG, "processDialCall, already dialling");
            this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
            return;
        }
        if (str == null || str.length() == 0) {
            lastDialledNumber = this.mPhonebook.getLastDialledNumber();
            if (lastDialledNumber == null) {
                Log.w(TAG, "processDialCall, last dial number null");
                this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
                return;
            }
        } else if (str.charAt(0) != '>') {
            if (str.charAt(str.length() - 1) == ';') {
                str = str.substring(0, str.length() - 1);
            }
            lastDialledNumber = PhoneNumberUtils.convertPreDial(str);
        } else {
            if (str.startsWith(">9999")) {
                Log.w(TAG, "Number is too big");
                this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
                return;
            }
            log("processDialCall, memory dial do last dial for now");
            lastDialledNumber = this.mPhonebook.getLastDialledNumber();
            if (lastDialledNumber == null) {
                Log.w(TAG, "processDialCall, last dial number null");
                this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
                return;
            }
        }
        if (!this.mHeadsetService.dialOutgoingCall(this.mDevice, lastDialledNumber)) {
            Log.w(TAG, "processDialCall, failed to dial in service");
            this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
        } else {
            this.mNeedDialingOutReply = true;
        }
    }

    private void processVrEvent(int i) {
        if (i == 1) {
            if (!this.mHeadsetService.startVoiceRecognitionByHeadset(this.mDevice)) {
                this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
            }
        } else {
            if (i == 0) {
                if (this.mHeadsetService.stopVoiceRecognitionByHeadset(this.mDevice)) {
                    this.mNativeInterface.atResponseCode(this.mDevice, 1, 0);
                    return;
                } else {
                    this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
                    return;
                }
            }
            this.mNativeInterface.atResponseCode(this.mDevice, 0, 0);
        }
    }

    private void processVolumeEvent(int i, int i2) {
        if (!this.mDevice.equals(this.mHeadsetService.getActiveDevice())) {
            Log.w(TAG, "processVolumeEvent, ignored because " + this.mDevice + " is not active");
            return;
        }
        if (i != 0) {
            if (i == 1) {
                this.mMicVolume = i2;
                return;
            }
            Log.e(TAG, "Bad volume type: " + i);
            return;
        }
        this.mSpeakerVolume = i2;
        this.mSystemInterface.getAudioManager().setStreamVolume(6, i2, getCurrentState() != this.mAudioOn ? 0 : 1);
    }

    private void processNoiseReductionEvent(boolean z) {
        String orDefault = this.mAudioParams.getOrDefault(HEADSET_NREC, HEADSET_AUDIO_FEATURE_OFF);
        String str = z ? HEADSET_AUDIO_FEATURE_ON : HEADSET_AUDIO_FEATURE_OFF;
        this.mAudioParams.put(HEADSET_NREC, str);
        log("processNoiseReductionEvent: bt_headset_nrec change " + orDefault + " -> " + str);
        if (getAudioState() == 12) {
            setAudioParameters();
        }
    }

    private void processWBSEvent(int i) {
        String orDefault = this.mAudioParams.getOrDefault(HEADSET_WBS, HEADSET_AUDIO_FEATURE_OFF);
        switch (i) {
            case 0:
            case 1:
                this.mAudioParams.put(HEADSET_WBS, HEADSET_AUDIO_FEATURE_OFF);
                break;
            case 2:
                this.mAudioParams.put(HEADSET_WBS, HEADSET_AUDIO_FEATURE_ON);
                break;
            default:
                Log.e(TAG, "processWBSEvent: unknown wbsConfig " + i);
                return;
        }
        log("processWBSEvent: bt_headset_nrec change " + orDefault + " -> " + this.mAudioParams.get(HEADSET_WBS));
    }

    private void processAtChld(int i, BluetoothDevice bluetoothDevice) {
        if (this.mSystemInterface.processChld(i)) {
            this.mNativeInterface.atResponseCode(bluetoothDevice, 1, 0);
        } else {
            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
        }
    }

    private void processSubscriberNumberRequest(BluetoothDevice bluetoothDevice) {
        String subscriberNumber = this.mSystemInterface.getSubscriberNumber();
        if (subscriberNumber != null) {
            this.mNativeInterface.atResponseString(bluetoothDevice, "+CNUM: ,\"" + subscriberNumber + "\"," + PhoneNumberUtils.toaFromString(subscriberNumber) + ",,4");
            this.mNativeInterface.atResponseCode(bluetoothDevice, 1, 0);
            return;
        }
        Log.e(TAG, "getSubscriberNumber returns null");
        this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
    }

    private void processAtCind(BluetoothDevice bluetoothDevice) {
        int numActiveCall;
        int numHeldCall;
        HeadsetPhoneState headsetPhoneState = this.mSystemInterface.getHeadsetPhoneState();
        if (this.mHeadsetService.isVirtualCallStarted()) {
            numActiveCall = 1;
            numHeldCall = 0;
        } else {
            numActiveCall = headsetPhoneState.getNumActiveCall();
            numHeldCall = headsetPhoneState.getNumHeldCall();
        }
        this.mNativeInterface.cindResponse(bluetoothDevice, headsetPhoneState.getCindService(), numActiveCall, numHeldCall, headsetPhoneState.getCallState(), headsetPhoneState.getCindSignal(), headsetPhoneState.getCindRoam(), headsetPhoneState.getCindBatteryCharge());
    }

    private void processAtCops(BluetoothDevice bluetoothDevice) {
        String networkOperator = this.mSystemInterface.getNetworkOperator();
        if (networkOperator == null) {
            networkOperator = "";
        }
        this.mNativeInterface.copsResponse(bluetoothDevice, networkOperator);
    }

    private void processAtClcc(BluetoothDevice bluetoothDevice) {
        String str;
        int i;
        if (this.mHeadsetService.isVirtualCallStarted()) {
            String subscriberNumber = this.mSystemInterface.getSubscriberNumber();
            if (subscriberNumber == null) {
                subscriberNumber = "";
            }
            int i2 = PhoneNumberUtils.toaFromString(subscriberNumber);
            if (isVirtualCallClccAcceptable(bluetoothDevice)) {
                str = subscriberNumber;
                i = i2;
            } else {
                Log.w(TAG, "Clear the phoneNumber & reset the type");
                i = 129;
                str = "";
            }
            this.mNativeInterface.clccResponse(bluetoothDevice, 1, 0, 0, 0, false, str, i);
            this.mNativeInterface.clccResponse(bluetoothDevice, 0, 0, 0, 0, false, "", 0);
            return;
        }
        if (!this.mSystemInterface.listCurrentCalls()) {
            Log.e(TAG, "processAtClcc: failed to list current calls for " + bluetoothDevice);
            this.mNativeInterface.clccResponse(bluetoothDevice, 0, 0, 0, 0, false, "", 0);
            return;
        }
        sendMessageDelayed(CLCC_RSP_TIMEOUT, bluetoothDevice, 5000L);
    }

    private void processAtCscs(String str, int i, BluetoothDevice bluetoothDevice) {
        log("processAtCscs - atString = " + str);
        if (this.mPhonebook != null) {
            synchronized (this.mPhonebook) {
                this.mPhonebook.handleCscsCommand(str, i, bluetoothDevice);
            }
        } else {
            Log.e(TAG, "Phonebook handle null for At+CSCS");
            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
        }
    }

    private void processAtCpbs(String str, int i, BluetoothDevice bluetoothDevice) {
        log("processAtCpbs - atString = " + str);
        if (this.mPhonebook != null) {
            synchronized (this.mPhonebook) {
                this.mPhonebook.handleCpbsCommand(str, i, bluetoothDevice);
            }
        } else {
            Log.e(TAG, "Phonebook handle null for At+CPBS");
            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
        }
    }

    private void processAtCpbr(String str, int i, BluetoothDevice bluetoothDevice) {
        log("processAtCpbr - atString = " + str);
        if (this.mPhonebook != null) {
            synchronized (this.mPhonebook) {
                this.mPhonebook.handleCpbrCommand(str, i, bluetoothDevice);
            }
        } else {
            Log.e(TAG, "Phonebook handle null for At+CPBR");
            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
        }
    }

    private static int findChar(char c, String str, int i) {
        while (i < str.length()) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\"') {
                i = str.indexOf(34, i + 1);
                if (i == -1) {
                    return str.length();
                }
            } else if (cCharAt == c) {
                return i;
            }
            i++;
        }
        return str.length();
    }

    private static Object[] generateArgs(String str) {
        ArrayList arrayList = new ArrayList();
        int i = 0;
        while (i <= str.length()) {
            int iFindChar = findChar(',', str, i);
            String strSubstring = str.substring(i, iFindChar);
            try {
                arrayList.add(new Integer(strSubstring));
            } catch (NumberFormatException e) {
                arrayList.add(strSubstring);
            }
            i = iFindChar + 1;
        }
        return arrayList.toArray();
    }

    private void processVendorSpecificAt(String str, BluetoothDevice bluetoothDevice) {
        log("processVendorSpecificAt - atString = " + str);
        int iIndexOf = str.indexOf("=");
        if (iIndexOf == -1) {
            Log.w(TAG, "processVendorSpecificAt: command type error in " + str);
            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
            return;
        }
        String strSubstring = str.substring(0, iIndexOf);
        Integer num = VENDOR_SPECIFIC_AT_COMMAND_COMPANY_ID.get(strSubstring);
        if (num == null) {
            Log.i(TAG, "processVendorSpecificAt: unsupported command: " + str);
            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
            return;
        }
        String strSubstring2 = str.substring(iIndexOf + 1);
        if (strSubstring2.startsWith("?")) {
            Log.w(TAG, "processVendorSpecificAt: command type error in " + str);
            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
            return;
        }
        Object[] objArrGenerateArgs = generateArgs(strSubstring2);
        if (strSubstring.equals("+XAPL")) {
            processAtXapl(objArrGenerateArgs, bluetoothDevice);
        }
        broadcastVendorSpecificEventIntent(strSubstring, num.intValue(), 2, objArrGenerateArgs, bluetoothDevice);
        this.mNativeInterface.atResponseCode(bluetoothDevice, 1, 0);
    }

    private void processAtXapl(Object[] objArr, BluetoothDevice bluetoothDevice) {
        if (objArr.length != 2) {
            Log.w(TAG, "processAtXapl() args length must be 2: " + String.valueOf(objArr.length));
            return;
        }
        if (!(objArr[0] instanceof String) || !(objArr[1] instanceof Integer)) {
            Log.w(TAG, "processAtXapl() argument types not match");
            return;
        }
        this.mNativeInterface.atResponseString(bluetoothDevice, "+XAPL=iPhone," + String.valueOf(2));
    }

    private void processUnknownAt(String str, BluetoothDevice bluetoothDevice) {
        if (bluetoothDevice == null) {
            Log.w(TAG, "processUnknownAt device is null");
            return;
        }
        log("processUnknownAt - atString = " + str);
        String unknownAt = parseUnknownAt(str);
        int atCommandType = getAtCommandType(unknownAt);
        if (unknownAt.startsWith("+CSCS")) {
            processAtCscs(unknownAt.substring(5), atCommandType, bluetoothDevice);
            return;
        }
        if (unknownAt.startsWith("+CPBS")) {
            processAtCpbs(unknownAt.substring(5), atCommandType, bluetoothDevice);
        } else if (unknownAt.startsWith("+CPBR")) {
            processAtCpbr(unknownAt.substring(5), atCommandType, bluetoothDevice);
        } else {
            processVendorSpecificAt(unknownAt, bluetoothDevice);
        }
    }

    private void processKeyPressed(BluetoothDevice bluetoothDevice) {
        if (this.mSystemInterface.isRinging()) {
            this.mSystemInterface.answerCall(bluetoothDevice);
            return;
        }
        if (this.mSystemInterface.isInCall()) {
            if (getAudioState() == 10) {
                if (!this.mHeadsetService.setActiveDevice(this.mDevice)) {
                    Log.w(TAG, "processKeyPressed, failed to set active device to " + this.mDevice);
                    return;
                }
                return;
            }
            this.mSystemInterface.hangupCall(bluetoothDevice);
            return;
        }
        if (getAudioState() != 10) {
            if (!this.mNativeInterface.disconnectAudio(this.mDevice)) {
                Log.w(TAG, "processKeyPressed, failed to disconnect audio from " + this.mDevice);
                return;
            }
            return;
        }
        if (this.mHeadsetService.hasDeviceInitiatedDialingOut()) {
            Log.w(TAG, "processKeyPressed, already dialling");
            return;
        }
        String lastDialledNumber = this.mPhonebook.getLastDialledNumber();
        if (lastDialledNumber == null) {
            Log.w(TAG, "processKeyPressed, last dial number null");
        } else if (!this.mHeadsetService.dialOutgoingCall(this.mDevice, lastDialledNumber)) {
            Log.w(TAG, "processKeyPressed, failed to call in service");
        }
    }

    private void sendIndicatorIntent(BluetoothDevice bluetoothDevice, int i, int i2) {
        Intent intent = new Intent("android.bluetooth.headset.action.HF_INDICATORS_VALUE_CHANGED");
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.putExtra("android.bluetooth.headset.extra.HF_INDICATORS_IND_ID", i);
        intent.putExtra("android.bluetooth.headset.extra.HF_INDICATORS_IND_VALUE", i2);
        this.mHeadsetService.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private void processAtBind(String str, BluetoothDevice bluetoothDevice) {
        log("processAtBind: " + str);
        int i = 0;
        int iIntValue = 0;
        while (i < str.length()) {
            int iFindChar = findChar(',', str, i);
            try {
                iIntValue = Integer.valueOf(str.substring(i, iFindChar)).intValue();
            } catch (NumberFormatException e) {
                Log.e(TAG, Log.getStackTraceString(new Throwable()));
            }
            switch (iIntValue) {
                case 1:
                    log("Send Broadcast intent for the Enhanced Driver Safety indicator.");
                    sendIndicatorIntent(bluetoothDevice, iIntValue, -1);
                    break;
                case 2:
                    log("Send Broadcast intent for the Battery Level indicator.");
                    sendIndicatorIntent(bluetoothDevice, iIntValue, -1);
                    break;
                default:
                    log("Invalid HF Indicator Received");
                    break;
            }
            i = iFindChar + 1;
        }
    }

    private void processAtBiev(int i, int i2, BluetoothDevice bluetoothDevice) {
        log("processAtBiev: ind_id=" + i + ", ind_value=" + i2);
        sendIndicatorIntent(bluetoothDevice, i, i2);
    }

    private void processSendClccResponse(HeadsetClccResponse headsetClccResponse) {
        if (!hasMessages(CLCC_RSP_TIMEOUT)) {
            return;
        }
        if (headsetClccResponse.mIndex == 0) {
            removeMessages(CLCC_RSP_TIMEOUT);
        }
        this.mNativeInterface.clccResponse(this.mDevice, headsetClccResponse.mIndex, headsetClccResponse.mDirection, headsetClccResponse.mStatus, headsetClccResponse.mMode, headsetClccResponse.mMpty, headsetClccResponse.mNumber, headsetClccResponse.mType);
    }

    private void processSendVendorSpecificResultCode(HeadsetVendorSpecificResultCode headsetVendorSpecificResultCode) {
        String str = headsetVendorSpecificResultCode.mCommand + ": ";
        if (headsetVendorSpecificResultCode.mArg != null) {
            str = str + headsetVendorSpecificResultCode.mArg;
        }
        this.mNativeInterface.atResponseString(headsetVendorSpecificResultCode.mDevice, str);
    }

    private String getCurrentDeviceName() {
        String remoteName = this.mAdapterService.getRemoteName(this.mDevice);
        if (remoteName == null) {
            return "<unknown>";
        }
        return remoteName;
    }

    private void updateAgIndicatorEnableState(HeadsetAgIndicatorEnableState headsetAgIndicatorEnableState) {
        if (Objects.equals(this.mAgIndicatorEnableState, headsetAgIndicatorEnableState)) {
            Log.i(TAG, "updateAgIndicatorEnableState, no change in indicator state " + this.mAgIndicatorEnableState);
            return;
        }
        this.mAgIndicatorEnableState = headsetAgIndicatorEnableState;
        int i = 1;
        if (this.mAgIndicatorEnableState == null || (!this.mAgIndicatorEnableState.service && !Utils.isPtsTestMode())) {
            i = 0;
        }
        if (this.mAgIndicatorEnableState != null && this.mAgIndicatorEnableState.signal) {
            i |= 256;
        }
        this.mSystemInterface.getHeadsetPhoneState().listenForPhoneState(this.mDevice, i);
    }

    protected void log(String str) {
        if (DBG) {
            super.log(str);
        }
    }

    protected String getLogRecString(Message message) {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageName(message.what));
        sb.append(": ");
        sb.append("arg1=");
        sb.append(message.arg1);
        sb.append(", arg2=");
        sb.append(message.arg2);
        sb.append(", obj=");
        if (message.obj instanceof HeadsetMessageObject) {
            ((HeadsetMessageObject) message.obj).buildString(sb);
        } else {
            sb.append(message.obj);
        }
        return sb.toString();
    }

    private void handleAccessPermissionResult(Intent intent) {
        int iProcessCpbrCommand;
        log("handleAccessPermissionResult");
        BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
        if (!this.mPhonebook.getCheckingAccessPermission()) {
            return;
        }
        synchronized (this.mPhonebook) {
            if (intent.getAction().equals("android.bluetooth.device.action.CONNECTION_ACCESS_REPLY")) {
                if (intent.getIntExtra("android.bluetooth.device.extra.CONNECTION_ACCESS_RESULT", 2) == 1) {
                    if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                        this.mDevice.setPhonebookAccessPermission(1);
                    }
                    iProcessCpbrCommand = this.mPhonebook.processCpbrCommand(bluetoothDevice);
                    this.mPhonebook.setCpbrIndex(-1);
                    this.mPhonebook.setCheckingAccessPermission(false);
                } else {
                    if (intent.getBooleanExtra("android.bluetooth.device.extra.ALWAYS_ALLOWED", false)) {
                        this.mDevice.setPhonebookAccessPermission(2);
                    }
                    iProcessCpbrCommand = 0;
                    this.mPhonebook.setCpbrIndex(-1);
                    this.mPhonebook.setCheckingAccessPermission(false);
                }
            } else {
                iProcessCpbrCommand = 0;
                this.mPhonebook.setCpbrIndex(-1);
                this.mPhonebook.setCheckingAccessPermission(false);
            }
        }
        if (iProcessCpbrCommand >= 0) {
            this.mNativeInterface.atResponseCode(bluetoothDevice, iProcessCpbrCommand, 0);
        } else {
            log("handleAccessPermissionResult - RESULT_NONE");
        }
    }

    private static String getMessageName(int i) {
        if (i == STACK_EVENT) {
            return "STACK_EVENT";
        }
        if (i == CLCC_RSP_TIMEOUT) {
            return "CLCC_RSP_TIMEOUT";
        }
        if (i != CONNECT_TIMEOUT) {
            switch (i) {
                case 1:
                    return "CONNECT";
                case 2:
                    return "DISCONNECT";
                case 3:
                    return "CONNECT_AUDIO";
                case 4:
                    return "DISCONNECT_AUDIO";
                case 5:
                    return "VOICE_RECOGNITION_START";
                case 6:
                    return "VOICE_RECOGNITION_STOP";
                case 7:
                    return "INTENT_SCO_VOLUME_CHANGED";
                case 8:
                    return "INTENT_CONNECTION_ACCESS_REPLY";
                case 9:
                    return "CALL_STATE_CHANGED";
                case 10:
                    return "DEVICE_STATE_CHANGED";
                case 11:
                    return "SEND_CCLC_RESPONSE";
                case 12:
                    return "SEND_VENDOR_SPECIFIC_RESULT_CODE";
                default:
                    switch (i) {
                        case 14:
                            return "DIALING_OUT_RESULT";
                        case 15:
                            return "VOICE_RECOGNITION_RESULT";
                        default:
                            return "UNKNOWN(" + i + ")";
                    }
            }
        }
        return "CONNECT_TIMEOUT";
    }

    void cleanupBeforeQuit() {
        HeadsetStateBase currentHeadsetState = getCurrentHeadsetState();
        int audioStateInt = currentHeadsetState.getAudioStateInt();
        currentHeadsetState.getConnectionStateInt();
        log("cleanupBeforeQuit: currentState = " + currentHeadsetState);
        if (audioStateInt == 12) {
            this.mSystemInterface.getAudioManager().setBluetoothScoOn(false);
            currentHeadsetState.broadcastAudioState(this.mDevice, audioStateInt, 10);
        }
    }

    private boolean isVirtualCallClccAcceptable(BluetoothDevice bluetoothDevice) {
        String strSubstring = bluetoothDevice.toString().substring(0, 8);
        String name = bluetoothDevice.getName();
        if (strSubstring.compareTo("C0:2A:46") == 0 && name.compareTo("XYT") == 0) {
            log("[" + name + "]" + strSubstring + " is blocked phone number!!!");
            return false;
        }
        return true;
    }

    private HeadsetStateBase getCurrentHeadsetState() {
        if (this.mCurrentState == null) {
            return this.mDisconnected;
        }
        return this.mCurrentState;
    }
}
