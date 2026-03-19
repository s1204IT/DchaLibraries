package com.android.server.telecom;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.IAudioService;
import android.os.Binder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.CallAudioState;
import android.telecom.Log;
import android.telecom.Logging.Session;
import android.util.Printer;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.IState;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.telecom.CallAudioManager;
import com.android.server.telecom.TelecomSystem;
import com.android.server.telecom.bluetooth.BluetoothRouteManager;
import java.util.HashMap;
import java.util.Objects;

public class CallAudioRouteStateMachine extends StateMachine {

    @VisibleForTesting
    public static final SparseArray<String> AUDIO_ROUTE_TO_LOG_EVENT = new SparseArray<String>() {
        {
            put(2, "AUDIO_ROUTE_BT");
            put(1, "AUDIO_ROUTE_EARPIECE");
            put(8, "AUDIO_ROUTE_SPEAKER");
            put(4, "AUDIO_ROUTE_HEADSET");
        }
    };
    private static final SparseArray<String> MESSAGE_CODE_TO_NAME = new SparseArray<String>() {
        {
            put(1, "CONNECT_WIRED_HEADSET");
            put(2, "DISCONNECT_WIRED_HEADSET");
            put(5, "CONNECT_DOCK");
            put(6, "DISCONNECT_DOCK");
            put(7, "BLUETOOTH_DEVICE_LIST_CHANGED");
            put(8, "BT_ACTIVE_DEVICE_PRESENT");
            put(9, "BT_ACTIVE_DEVICE_GONE");
            put(1001, "SWITCH_EARPIECE");
            put(1002, "SWITCH_BLUETOOTH");
            put(1003, "SWITCH_HEADSET");
            put(1004, "SWITCH_SPEAKER");
            put(1005, "SWITCH_BASELINE_ROUTE");
            put(1101, "USER_SWITCH_EARPIECE");
            put(1102, "USER_SWITCH_BLUETOOTH");
            put(1103, "USER_SWITCH_HEADSET");
            put(1104, "USER_SWITCH_SPEAKER");
            put(1105, "USER_SWITCH_BASELINE_ROUTE");
            put(1201, "UPDATE_SYSTEM_AUDIO_ROUTE");
            put(1301, "BT_AUDIO_DISCONNECTED");
            put(1302, "BT_AUDIO_CONNECTED");
            put(1303, "BT_AUDIO_PENDING");
            put(3001, "MUTE_ON");
            put(3002, "MUTE_OFF");
            put(3003, "TOGGLE_MUTE");
            put(3004, "MUTE_EXTERNALLY_CHANGED");
            put(4001, "SWITCH_FOCUS");
            put(9001, "RUN_RUNNABLE");
        }
    };
    public static final String NAME = CallAudioRouteStateMachine.class.getName();
    private final ActiveBluetoothRoute mActiveBluetoothRoute;
    private final ActiveEarpieceRoute mActiveEarpieceRoute;
    private final ActiveHeadsetRoute mActiveHeadsetRoute;
    private final ActiveSpeakerRoute mActiveSpeakerRoute;
    private int mAudioFocusType;
    private final AudioManager mAudioManager;
    private final CallAudioManager.AudioServiceFactory mAudioServiceFactory;
    private int mAvailableRoutes;
    private final BluetoothRouteManager mBluetoothRouteManager;
    private CallAudioManager mCallAudioManager;
    private final CallsManager mCallsManager;
    private final Context mContext;
    private CallAudioState mCurrentCallAudioState;
    private int mDeviceSupportedRoutes;
    private final boolean mDoesDeviceSupportEarpieceRoute;
    private boolean mHasUserExplicitlyLeftBluetooth;
    private boolean mIsMuted;
    private CallAudioState mLastKnownCallAudioState;
    private final TelecomSystem.SyncRoot mLock;
    private final BroadcastReceiver mMuteChangeReceiver;
    private final QuiescentBluetoothRoute mQuiescentBluetoothRoute;
    private final QuiescentEarpieceRoute mQuiescentEarpieceRoute;
    private final QuiescentHeadsetRoute mQuiescentHeadsetRoute;
    private final QuiescentSpeakerRoute mQuiescentSpeakerRoute;
    private final RingingBluetoothRoute mRingingBluetoothRoute;
    private HashMap<Integer, AudioState> mRouteCodeToQuiescentState;
    private HashMap<String, Integer> mStateNameToRouteCode;
    private final StatusBarNotifier mStatusBarNotifier;
    private boolean mWasOnSpeaker;
    private final WiredHeadsetManager mWiredHeadsetManager;

    protected void onPreHandleMessage(Message message) {
        if (message.obj != null && (message.obj instanceof SomeArgs)) {
            Session session = (Session) ((SomeArgs) message.obj).arg1;
            String str = MESSAGE_CODE_TO_NAME.get(message.what, "unknown");
            Log.continueSession(session, "CARSM.pM_" + str);
            Log.i(this, "Message received: %s=%d, arg1=%d", new Object[]{str, Integer.valueOf(message.what), Integer.valueOf(message.arg1)});
        }
    }

    protected void onPostHandleMessage(Message message) {
        Log.endSession();
        if (message.obj != null && (message.obj instanceof SomeArgs)) {
            ((SomeArgs) message.obj).recycle();
        }
    }

    abstract class AudioState extends State {
        public abstract int getRouteCode();

        public abstract boolean isActive();

        public abstract void updateSystemAudioState();

        AudioState() {
        }

        public void enter() {
            super.enter();
            Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), "AUDIO_ROUTE", "Entering state " + getName());
            if (isActive()) {
                Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), CallAudioRouteStateMachine.AUDIO_ROUTE_TO_LOG_EVENT.get(getRouteCode(), "AUDIO_ROUTE"));
            }
        }

        public void exit() {
            Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), "AUDIO_ROUTE", "Leaving state " + getName());
            super.exit();
        }

        public boolean processMessage(Message message) {
            int i;
            boolean z;
            Log.i(this, "Processing message %s", new Object[]{CallAudioRouteStateMachine.MESSAGE_CODE_TO_NAME.get(message.what, Integer.toString(message.what))});
            int i2 = message.what;
            if (i2 == 1005) {
                CallAudioRouteStateMachine.this.sendInternalMessage(CallAudioRouteStateMachine.this.calculateBaselineRouteMessage(false, message.arg1 == 1));
                return true;
            }
            if (i2 == 1102) {
                CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = false;
                return false;
            }
            if (i2 == 1105) {
                CallAudioRouteStateMachine.this.sendInternalMessage(CallAudioRouteStateMachine.this.calculateBaselineRouteMessage(true, message.arg1 == 1));
                return true;
            }
            if (i2 == 4001) {
                CallAudioRouteStateMachine.this.mAudioFocusType = message.arg1;
                return false;
            }
            int i3 = 4;
            switch (i2) {
                case 1:
                    Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), "AUDIO_ROUTE", "Wired headset connected");
                    i = 1;
                    z = 0;
                    if (i3 == 0 || i != 0 || message.what == 7) {
                        CallAudioRouteStateMachine.this.mAvailableRoutes = CallAudioRouteStateMachine.this.modifyRoutes(CallAudioRouteStateMachine.this.mAvailableRoutes, i, i3, true);
                        CallAudioRouteStateMachine.this.mDeviceSupportedRoutes = CallAudioRouteStateMachine.this.modifyRoutes(CallAudioRouteStateMachine.this.mDeviceSupportedRoutes, i, i3, false);
                        updateSystemAudioState();
                    }
                    return z;
                case CallState.SELECT_PHONE_ACCOUNT:
                    Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), "AUDIO_ROUTE", "Wired headset disconnected");
                    if (!CallAudioRouteStateMachine.this.mDoesDeviceSupportEarpieceRoute) {
                        i = 4;
                        i3 = 0;
                        z = i3;
                        if (i3 == 0) {
                        }
                        return z;
                    }
                    i = 4;
                    z = 0;
                    i3 = 1;
                    if (i3 == 0) {
                        CallAudioRouteStateMachine.this.mAvailableRoutes = CallAudioRouteStateMachine.this.modifyRoutes(CallAudioRouteStateMachine.this.mAvailableRoutes, i, i3, true);
                        CallAudioRouteStateMachine.this.mDeviceSupportedRoutes = CallAudioRouteStateMachine.this.modifyRoutes(CallAudioRouteStateMachine.this.mDeviceSupportedRoutes, i, i3, false);
                        updateSystemAudioState();
                    }
                    return z;
                default:
                    switch (i2) {
                        case CallState.DISCONNECTED:
                            Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), "AUDIO_ROUTE", "Bluetooth device list changed");
                            i3 = 2;
                            if (CallAudioRouteStateMachine.this.mBluetoothRouteManager.getConnectedDevices().size() > 0) {
                                i = 0;
                            } else {
                                i = 2;
                                i3 = 0;
                            }
                            z = 1;
                            if (i3 == 0) {
                            }
                            return z;
                        case CallState.ABORTED:
                            Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), "AUDIO_ROUTE", "Bluetooth active device present");
                            i = 0;
                            i3 = 0;
                            z = i3;
                            if (i3 == 0) {
                            }
                            return z;
                        case 9:
                            Log.addEvent(CallAudioRouteStateMachine.this.mCallsManager.getForegroundCall(), "AUDIO_ROUTE", "Bluetooth active device gone");
                            i = 0;
                            i3 = 0;
                            z = i3;
                            if (i3 == 0) {
                            }
                            return z;
                        default:
                            return false;
                    }
            }
        }
    }

    class ActiveEarpieceRoute extends EarpieceRoute {
        ActiveEarpieceRoute() {
            super();
        }

        public String getName() {
            return "ActiveEarpieceRoute";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.setSpeakerphoneOn(false);
            CallAudioRouteStateMachine.this.setBluetoothOff();
            CallAudioRouteStateMachine.this.setSystemAudioState(new CallAudioState(CallAudioRouteStateMachine.this.mIsMuted, 1, CallAudioRouteStateMachine.this.mAvailableRoutes, null, CallAudioRouteStateMachine.this.mBluetoothRouteManager.getConnectedDevices()), true);
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
            CallAudioRouteStateMachine.this.setSystemAudioState(CallAudioRouteStateMachine.this.mCurrentCallAudioState);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i == 1302) {
                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                return true;
            }
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        return true;
                    case 1002:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 2) != 0) {
                            if (CallAudioRouteStateMachine.this.mAudioFocusType == 2 || CallAudioRouteStateMachine.this.mBluetoothRouteManager.isInbandRingingEnabled()) {
                                CallAudioRouteStateMachine.this.setBluetoothOn(message.obj instanceof SomeArgs ? (String) ((SomeArgs) message.obj).arg2 : null);
                            } else {
                                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mRingingBluetoothRoute);
                            }
                        } else {
                            Log.w(this, "Ignoring switch to bluetooth command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1003:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveHeadsetRoute);
                        } else {
                            Log.w(this, "Ignoring switch to headset command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1004:
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveSpeakerRoute);
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                break;
                            case 1102:
                                break;
                            case 1103:
                                break;
                            case 1104:
                                break;
                            default:
                                return false;
                        }
                        break;
                }
            } else {
                if (message.arg1 == 1) {
                    CallAudioRouteStateMachine.this.reinitialize();
                }
                return true;
            }
        }
    }

    class QuiescentEarpieceRoute extends EarpieceRoute {
        QuiescentEarpieceRoute() {
            super();
        }

        public String getName() {
            return "QuiescentEarpieceRoute";
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = false;
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i == 1302) {
                Log.w(this, "BT Audio came on in quiescent earpiece route.", new Object[0]);
                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                return true;
            }
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        return true;
                    case 1002:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 2) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentBluetoothRoute);
                        } else {
                            Log.w(this, "Ignoring switch to bluetooth command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1003:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentHeadsetRoute);
                        } else {
                            Log.w(this, "Ignoring switch to headset command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1004:
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentSpeakerRoute);
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                break;
                            case 1102:
                                break;
                            case 1103:
                                break;
                            case 1104:
                                break;
                            default:
                                return false;
                        }
                        break;
                }
            } else {
                if (message.arg1 == 2 || message.arg1 == 3) {
                    CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveEarpieceRoute);
                }
                return true;
            }
        }
    }

    abstract class EarpieceRoute extends AudioState {
        EarpieceRoute() {
            super();
        }

        @Override
        public int getRouteCode() {
            return 1;
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            switch (message.what) {
                case 1:
                    CallAudioRouteStateMachine.this.sendInternalMessage(1003);
                    return true;
                case CallState.SELECT_PHONE_ACCOUNT:
                    Log.e(this, new IllegalStateException(), "Wired headset should not go from connected to not when on earpiece", new Object[0]);
                    return true;
                case CallState.ACTIVE:
                    CallAudioRouteStateMachine.this.sendInternalMessage(1004);
                    return true;
                case CallState.ON_HOLD:
                    return true;
                case CallState.ABORTED:
                    if (!CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth) {
                        CallAudioRouteStateMachine.this.sendInternalMessage(1002);
                    } else {
                        Log.i(this, "Not switching to BT route from earpiece because user has explicitly disconnected.", new Object[0]);
                    }
                    return true;
                case 9:
                    return true;
                case 1301:
                    return true;
                default:
                    return false;
            }
        }
    }

    class ActiveHeadsetRoute extends HeadsetRoute {
        ActiveHeadsetRoute() {
            super();
        }

        public String getName() {
            return "ActiveHeadsetRoute";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.setSpeakerphoneOn(false);
            CallAudioRouteStateMachine.this.setBluetoothOff();
            CallAudioRouteStateMachine.this.setSystemAudioState(new CallAudioState(CallAudioRouteStateMachine.this.mIsMuted, 4, CallAudioRouteStateMachine.this.mAvailableRoutes, null, CallAudioRouteStateMachine.this.mBluetoothRouteManager.getConnectedDevices()), true);
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
            CallAudioRouteStateMachine.this.setSystemAudioState(CallAudioRouteStateMachine.this.mCurrentCallAudioState);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i == 1302) {
                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                return true;
            }
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveEarpieceRoute);
                        } else {
                            Log.w(this, "Ignoring switch to earpiece command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1002:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 2) != 0) {
                            if (CallAudioRouteStateMachine.this.mAudioFocusType == 2 || CallAudioRouteStateMachine.this.mBluetoothRouteManager.isInbandRingingEnabled()) {
                                CallAudioRouteStateMachine.this.setBluetoothOn(message.obj instanceof SomeArgs ? (String) ((SomeArgs) message.obj).arg2 : null);
                            } else {
                                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mRingingBluetoothRoute);
                            }
                        } else {
                            Log.w(this, "Ignoring switch to bluetooth command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1003:
                        return true;
                    case 1004:
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveSpeakerRoute);
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                break;
                            case 1102:
                                break;
                            case 1103:
                                break;
                            case 1104:
                                break;
                            default:
                                return false;
                        }
                        break;
                }
            } else {
                if (message.arg1 == 1) {
                    CallAudioRouteStateMachine.this.reinitialize();
                }
                return true;
            }
        }
    }

    class QuiescentHeadsetRoute extends HeadsetRoute {
        QuiescentHeadsetRoute() {
            super();
        }

        public String getName() {
            return "QuiescentHeadsetRoute";
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = false;
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i == 1302) {
                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                Log.w(this, "BT Audio came on in quiescent headset route.", new Object[0]);
                return true;
            }
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentEarpieceRoute);
                        } else {
                            Log.w(this, "Ignoring switch to earpiece command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1002:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 2) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentBluetoothRoute);
                        } else {
                            Log.w(this, "Ignoring switch to bluetooth command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1003:
                        return true;
                    case 1004:
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentSpeakerRoute);
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                break;
                            case 1102:
                                break;
                            case 1103:
                                break;
                            case 1104:
                                break;
                            default:
                                return false;
                        }
                        break;
                }
            } else {
                if (message.arg1 == 2 || message.arg1 == 3) {
                    CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveHeadsetRoute);
                }
                return true;
            }
        }
    }

    abstract class HeadsetRoute extends AudioState {
        HeadsetRoute() {
            super();
        }

        @Override
        public int getRouteCode() {
            return 4;
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            switch (message.what) {
                case 1:
                    Log.e(this, new IllegalStateException(), "Wired headset should already be connected.", new Object[0]);
                    return true;
                case CallState.SELECT_PHONE_ACCOUNT:
                    if (CallAudioRouteStateMachine.this.mWasOnSpeaker) {
                        CallAudioRouteStateMachine.this.sendInternalMessage(1004);
                    } else {
                        CallAudioRouteStateMachine.this.sendInternalMessage(1005, 1);
                    }
                    return true;
                case CallState.ACTIVE:
                    return true;
                case CallState.ON_HOLD:
                    return true;
                case CallState.ABORTED:
                    if (!CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth) {
                        CallAudioRouteStateMachine.this.sendInternalMessage(1002);
                    } else {
                        Log.i(this, "Not switching to BT route from headset because user has explicitly disconnected.", new Object[0]);
                    }
                    return true;
                case 9:
                    return true;
                case 1301:
                    return true;
                default:
                    return false;
            }
        }
    }

    class ActiveBluetoothRoute extends BluetoothRoute {
        ActiveBluetoothRoute() {
            super();
        }

        public String getName() {
            return "ActiveBluetoothRoute";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = false;
            CallAudioRouteStateMachine.this.mStatusBarNotifier.notifySpeakerphone(false);
            CallAudioRouteStateMachine.this.setSystemAudioState(new CallAudioState(CallAudioRouteStateMachine.this.mIsMuted, 2, CallAudioRouteStateMachine.this.mAvailableRoutes, CallAudioRouteStateMachine.this.mBluetoothRouteManager.getBluetoothAudioConnectedDevice(), CallAudioRouteStateMachine.this.mBluetoothRouteManager.getConnectedDevices()), true);
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
            if (CallAudioRouteStateMachine.this.mBluetoothRouteManager.getBluetoothAudioConnectedDevice() != null) {
                CallAudioRouteStateMachine.this.mCallAudioManager.onRingerModeChange();
            }
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
            CallAudioRouteStateMachine.this.setSystemAudioState(CallAudioRouteStateMachine.this.mCurrentCallAudioState);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) == 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveEarpieceRoute);
                        } else {
                            Log.w(this, "Ignoring switch to earpiece command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1002:
                        CallAudioRouteStateMachine.this.setBluetoothOn(message.obj instanceof SomeArgs ? (String) ((SomeArgs) message.obj).arg2 : null);
                        return true;
                    case 1003:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) == 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveHeadsetRoute);
                        } else {
                            Log.w(this, "Ignoring switch to headset command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1004:
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveSpeakerRoute);
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = true;
                                if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) == 0) {
                                }
                                return true;
                            case 1102:
                                break;
                            case 1103:
                                CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = true;
                                if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) == 0) {
                                }
                                return true;
                            case 1104:
                                CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = true;
                                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveSpeakerRoute);
                                return true;
                            default:
                                switch (i) {
                                    case 1301:
                                        CallAudioRouteStateMachine.this.sendInternalMessage(1005, 0);
                                        return true;
                                    case 1302:
                                        CallAudioRouteStateMachine.this.mCallAudioManager.onRingerModeChange();
                                        updateSystemAudioState();
                                        return true;
                                    default:
                                        return false;
                                }
                        }
                        break;
                }
            } else {
                if (message.arg1 == 1) {
                    CallAudioRouteStateMachine.this.setBluetoothOff();
                    CallAudioRouteStateMachine.this.reinitialize();
                } else if (message.arg1 == 3 && !CallAudioRouteStateMachine.this.mBluetoothRouteManager.isInbandRingingEnabled()) {
                    Log.i(this, "Do not setBluetoothOff() when ActiveBluetoothRoute transition", new Object[]{" to RingingBluetoothRoute"});
                    CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mRingingBluetoothRoute);
                }
                return true;
            }
        }
    }

    class RingingBluetoothRoute extends BluetoothRoute {
        RingingBluetoothRoute() {
            super();
        }

        public String getName() {
            return "RingingBluetoothRoute";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = false;
            CallAudioRouteStateMachine.this.setSpeakerphoneOn(false);
            CallAudioRouteStateMachine.this.setSystemAudioState(new CallAudioState(CallAudioRouteStateMachine.this.mIsMuted, 2, CallAudioRouteStateMachine.this.mAvailableRoutes, CallAudioRouteStateMachine.this.mBluetoothRouteManager.getBluetoothAudioConnectedDevice(), CallAudioRouteStateMachine.this.mBluetoothRouteManager.getConnectedDevices()));
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
            CallAudioRouteStateMachine.this.setSystemAudioState(CallAudioRouteStateMachine.this.mCurrentCallAudioState);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) == 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveEarpieceRoute);
                        } else {
                            Log.w(this, "Ignoring switch to earpiece command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1002:
                        return true;
                    case 1003:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) == 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveHeadsetRoute);
                        } else {
                            Log.w(this, "Ignoring switch to headset command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1004:
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveSpeakerRoute);
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = true;
                                if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) == 0) {
                                }
                                return true;
                            case 1102:
                                break;
                            case 1103:
                                CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = true;
                                if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) == 0) {
                                }
                                return true;
                            case 1104:
                                CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = true;
                                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveSpeakerRoute);
                                return true;
                            default:
                                switch (i) {
                                    case 1301:
                                        return true;
                                    case 1302:
                                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                                        return true;
                                    default:
                                        return false;
                                }
                        }
                        break;
                }
            } else {
                if (message.arg1 == 1) {
                    CallAudioRouteStateMachine.this.setBluetoothOff();
                    CallAudioRouteStateMachine.this.reinitialize();
                } else if (message.arg1 == 2) {
                    CallAudioRouteStateMachine.this.setBluetoothOn(null);
                }
                return true;
            }
        }
    }

    class QuiescentBluetoothRoute extends BluetoothRoute {
        QuiescentBluetoothRoute() {
            super();
        }

        public String getName() {
            return "QuiescentBluetoothRoute";
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = false;
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentEarpieceRoute);
                        } else {
                            Log.w(this, "Ignoring switch to earpiece command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1002:
                        return true;
                    case 1003:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentHeadsetRoute);
                        } else {
                            Log.w(this, "Ignoring switch to headset command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1004:
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentSpeakerRoute);
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                break;
                            case 1102:
                                break;
                            case 1103:
                                break;
                            case 1104:
                                break;
                            default:
                                switch (i) {
                                    case 1301:
                                        return true;
                                    case 1302:
                                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                                        return true;
                                    default:
                                        return false;
                                }
                        }
                        break;
                }
            } else {
                if (message.arg1 == 2) {
                    CallAudioRouteStateMachine.this.setBluetoothOn(null);
                } else if (message.arg1 == 3) {
                    if (CallAudioRouteStateMachine.this.mBluetoothRouteManager.isInbandRingingEnabled()) {
                        CallAudioRouteStateMachine.this.setBluetoothOn(null);
                    } else {
                        CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mRingingBluetoothRoute);
                    }
                }
                return true;
            }
        }
    }

    abstract class BluetoothRoute extends AudioState {
        BluetoothRoute() {
            super();
        }

        @Override
        public int getRouteCode() {
            return 2;
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            switch (message.what) {
                case 1:
                    CallAudioRouteStateMachine.this.sendInternalMessage(1003);
                    return true;
                case CallState.SELECT_PHONE_ACCOUNT:
                    return true;
                case CallState.DIALING:
                case CallState.RINGING:
                case CallState.DISCONNECTED:
                default:
                    return false;
                case CallState.ACTIVE:
                    return true;
                case CallState.ON_HOLD:
                    return true;
                case CallState.ABORTED:
                    Log.w(this, "Bluetooth active device should not have been null while we were in BT route.", new Object[0]);
                    return true;
                case 9:
                    CallAudioRouteStateMachine.this.sendInternalMessage(1005, 0);
                    CallAudioRouteStateMachine.this.mWasOnSpeaker = false;
                    return true;
            }
        }
    }

    class ActiveSpeakerRoute extends SpeakerRoute {
        ActiveSpeakerRoute() {
            super();
        }

        public String getName() {
            return "ActiveSpeakerRoute";
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.mWasOnSpeaker = true;
            CallAudioRouteStateMachine.this.setSpeakerphoneOn(true);
            CallAudioRouteStateMachine.this.setBluetoothOff();
            CallAudioRouteStateMachine.this.setSystemAudioState(new CallAudioState(CallAudioRouteStateMachine.this.mIsMuted, 8, CallAudioRouteStateMachine.this.mAvailableRoutes, null, CallAudioRouteStateMachine.this.mBluetoothRouteManager.getConnectedDevices()), true);
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
            CallAudioRouteStateMachine.this.setSystemAudioState(CallAudioRouteStateMachine.this.mCurrentCallAudioState);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i == 1302) {
                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                return true;
            }
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) == 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveEarpieceRoute);
                        } else {
                            Log.w(this, "Ignoring switch to earpiece command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1002:
                        String str = !(message.obj instanceof SomeArgs) ? (String) ((SomeArgs) message.obj).arg2 : null;
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 2) == 0) {
                            if (CallAudioRouteStateMachine.this.mAudioFocusType == 2 || CallAudioRouteStateMachine.this.mBluetoothRouteManager.isInbandRingingEnabled()) {
                                CallAudioRouteStateMachine.this.setBluetoothOn(str);
                            } else {
                                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mRingingBluetoothRoute);
                            }
                        } else {
                            Log.w(this, "Ignoring switch to bluetooth command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1003:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) == 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveHeadsetRoute);
                        } else {
                            Log.w(this, "Ignoring switch to headset command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1004:
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                CallAudioRouteStateMachine.this.mWasOnSpeaker = false;
                                if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) == 0) {
                                }
                                return true;
                            case 1102:
                                CallAudioRouteStateMachine.this.mWasOnSpeaker = false;
                                if (!(message.obj instanceof SomeArgs)) {
                                }
                                if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 2) == 0) {
                                }
                                return true;
                            case 1103:
                                CallAudioRouteStateMachine.this.mWasOnSpeaker = false;
                                if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) == 0) {
                                }
                                return true;
                            case 1104:
                                break;
                            default:
                                return false;
                        }
                        break;
                }
            } else {
                if (message.arg1 == 1) {
                    CallAudioRouteStateMachine.this.reinitialize();
                }
                return true;
            }
        }
    }

    class QuiescentSpeakerRoute extends SpeakerRoute {
        QuiescentSpeakerRoute() {
            super();
        }

        public String getName() {
            return "QuiescentSpeakerRoute";
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void enter() {
            super.enter();
            CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth = false;
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public void updateSystemAudioState() {
            CallAudioRouteStateMachine.this.updateInternalCallAudioState();
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            int i = message.what;
            if (i == 1302) {
                CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveBluetoothRoute);
                Log.w(this, "BT audio reported as connected while in quiescent speaker", new Object[0]);
                return true;
            }
            if (i != 4001) {
                switch (i) {
                    case 1001:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 1) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentEarpieceRoute);
                        } else {
                            Log.w(this, "Ignoring switch to earpiece command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1002:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 2) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentBluetoothRoute);
                        } else {
                            Log.w(this, "Ignoring switch to bluetooth command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1003:
                        if ((CallAudioRouteStateMachine.this.mAvailableRoutes & 4) != 0) {
                            CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mQuiescentHeadsetRoute);
                        } else {
                            Log.w(this, "Ignoring switch to headset command. Not available.", new Object[0]);
                        }
                        return true;
                    case 1004:
                        return true;
                    default:
                        switch (i) {
                            case 1101:
                                break;
                            case 1102:
                                break;
                            case 1103:
                                break;
                            case 1104:
                                break;
                            default:
                                return false;
                        }
                        break;
                }
            } else {
                if (message.arg1 == 2 || message.arg1 == 3) {
                    CallAudioRouteStateMachine.this.transitionTo(CallAudioRouteStateMachine.this.mActiveSpeakerRoute);
                }
                return true;
            }
        }
    }

    abstract class SpeakerRoute extends AudioState {
        SpeakerRoute() {
            super();
        }

        @Override
        public int getRouteCode() {
            return 8;
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            switch (message.what) {
                case 1:
                    CallAudioRouteStateMachine.this.sendInternalMessage(1003);
                    return true;
                case CallState.SELECT_PHONE_ACCOUNT:
                    return true;
                case CallState.ACTIVE:
                    return true;
                case CallState.ON_HOLD:
                    CallAudioRouteStateMachine.this.sendInternalMessage(1005, 1);
                    return true;
                case CallState.ABORTED:
                    if (!CallAudioRouteStateMachine.this.mHasUserExplicitlyLeftBluetooth) {
                        CallAudioRouteStateMachine.this.sendInternalMessage(1002);
                    } else {
                        Log.i(this, "Not switching to BT route from speaker because user has explicitly disconnected.", new Object[0]);
                    }
                    return true;
                case 9:
                    return true;
                case 1301:
                    return true;
                default:
                    return false;
            }
        }
    }

    public CallAudioRouteStateMachine(Context context, CallsManager callsManager, BluetoothRouteManager bluetoothRouteManager, WiredHeadsetManager wiredHeadsetManager, StatusBarNotifier statusBarNotifier, CallAudioManager.AudioServiceFactory audioServiceFactory, int i) {
        super(NAME);
        this.mMuteChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Log.startSession("CARSM.mCR");
                if ("android.media.action.MICROPHONE_MUTE_CHANGED".equals(intent.getAction())) {
                    if (!CallAudioRouteStateMachine.this.mCallsManager.hasEmergencyCall()) {
                        CallAudioRouteStateMachine.this.sendInternalMessage(3004);
                        return;
                    } else {
                        Log.i(this, "Mute was externally changed when there's an emergency call. Forcing mute back off.", new Object[0]);
                        CallAudioRouteStateMachine.this.sendInternalMessage(3002);
                        return;
                    }
                }
                Log.w(this, "Received non-mute-change intent", new Object[0]);
            }
        };
        this.mActiveEarpieceRoute = new ActiveEarpieceRoute();
        this.mActiveHeadsetRoute = new ActiveHeadsetRoute();
        this.mActiveBluetoothRoute = new ActiveBluetoothRoute();
        this.mActiveSpeakerRoute = new ActiveSpeakerRoute();
        this.mRingingBluetoothRoute = new RingingBluetoothRoute();
        this.mQuiescentEarpieceRoute = new QuiescentEarpieceRoute();
        this.mQuiescentHeadsetRoute = new QuiescentHeadsetRoute();
        this.mQuiescentBluetoothRoute = new QuiescentBluetoothRoute();
        this.mQuiescentSpeakerRoute = new QuiescentSpeakerRoute();
        this.mHasUserExplicitlyLeftBluetooth = false;
        addState(this.mActiveEarpieceRoute);
        addState(this.mActiveHeadsetRoute);
        addState(this.mActiveBluetoothRoute);
        addState(this.mActiveSpeakerRoute);
        addState(this.mRingingBluetoothRoute);
        addState(this.mQuiescentEarpieceRoute);
        addState(this.mQuiescentHeadsetRoute);
        addState(this.mQuiescentBluetoothRoute);
        addState(this.mQuiescentSpeakerRoute);
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mBluetoothRouteManager = bluetoothRouteManager;
        this.mWiredHeadsetManager = wiredHeadsetManager;
        this.mStatusBarNotifier = statusBarNotifier;
        this.mAudioServiceFactory = audioServiceFactory;
        switch (i) {
            case CallState.NEW:
                this.mDoesDeviceSupportEarpieceRoute = false;
                break;
            case 1:
                this.mDoesDeviceSupportEarpieceRoute = true;
                break;
            default:
                this.mDoesDeviceSupportEarpieceRoute = checkForEarpieceSupport();
                break;
        }
        this.mLock = callsManager.getLock();
        this.mStateNameToRouteCode = new HashMap<>(8);
        this.mStateNameToRouteCode.put(this.mQuiescentEarpieceRoute.getName(), 1);
        this.mStateNameToRouteCode.put(this.mQuiescentBluetoothRoute.getName(), 2);
        this.mStateNameToRouteCode.put(this.mQuiescentHeadsetRoute.getName(), 4);
        this.mStateNameToRouteCode.put(this.mQuiescentSpeakerRoute.getName(), 8);
        this.mStateNameToRouteCode.put(this.mRingingBluetoothRoute.getName(), 2);
        this.mStateNameToRouteCode.put(this.mActiveEarpieceRoute.getName(), 1);
        this.mStateNameToRouteCode.put(this.mActiveBluetoothRoute.getName(), 2);
        this.mStateNameToRouteCode.put(this.mActiveHeadsetRoute.getName(), 4);
        this.mStateNameToRouteCode.put(this.mActiveSpeakerRoute.getName(), 8);
        this.mRouteCodeToQuiescentState = new HashMap<>(4);
        this.mRouteCodeToQuiescentState.put(1, this.mQuiescentEarpieceRoute);
        this.mRouteCodeToQuiescentState.put(2, this.mQuiescentBluetoothRoute);
        this.mRouteCodeToQuiescentState.put(8, this.mQuiescentSpeakerRoute);
        this.mRouteCodeToQuiescentState.put(4, this.mQuiescentHeadsetRoute);
    }

    public void setCallAudioManager(CallAudioManager callAudioManager) {
        this.mCallAudioManager = callAudioManager;
    }

    public void initialize() {
        initialize(getInitialAudioState());
    }

    public void initialize(CallAudioState callAudioState) {
        if ((callAudioState.getRoute() & getCurrentCallSupportedRoutes()) == 0) {
            Log.e(this, new IllegalArgumentException(), "Route %d specified when supported call routes are: %d", new Object[]{Integer.valueOf(callAudioState.getRoute()), Integer.valueOf(getCurrentCallSupportedRoutes())});
        }
        this.mCurrentCallAudioState = callAudioState;
        this.mLastKnownCallAudioState = callAudioState;
        this.mDeviceSupportedRoutes = callAudioState.getSupportedRouteMask();
        this.mAvailableRoutes = this.mDeviceSupportedRoutes & getCurrentCallSupportedRoutes();
        this.mIsMuted = callAudioState.isMuted();
        this.mWasOnSpeaker = false;
        this.mContext.registerReceiver(this.mMuteChangeReceiver, new IntentFilter("android.media.action.MICROPHONE_MUTE_CHANGED"));
        this.mStatusBarNotifier.notifyMute(callAudioState.isMuted());
        this.mStatusBarNotifier.notifySpeakerphone(callAudioState.getRoute() == 8);
        setInitialState(this.mRouteCodeToQuiescentState.get(Integer.valueOf(callAudioState.getRoute())));
        start();
    }

    public CallAudioState getCurrentCallAudioState() {
        return this.mCurrentCallAudioState;
    }

    public void sendMessageWithSessionInfo(int i, int i2) {
        sendMessageWithSessionInfo(i, i2, null);
    }

    public void sendMessageWithSessionInfo(int i) {
        sendMessageWithSessionInfo(i, 0, null);
    }

    public void sendMessageWithSessionInfo(int i, int i2, String str) {
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = Log.createSubsession();
        someArgsObtain.arg2 = str;
        sendMessage(i, i2, 0, someArgsObtain);
    }

    protected void unhandledMessage(Message message) {
        int i = message.what;
        if (i == 1201) {
            updateRouteForForegroundCall();
            resendSystemAudioState();
            updateStatusBarAudioState();
            return;
        }
        if (i != 9001) {
            switch (i) {
                case 3001:
                    setMuteOn(true);
                    updateSystemMuteState();
                    break;
                case 3002:
                    setMuteOn(false);
                    updateSystemMuteState();
                    break;
                case 3003:
                    if (this.mIsMuted) {
                        sendInternalMessage(3002);
                    } else {
                        sendInternalMessage(3001);
                    }
                    break;
                case 3004:
                    this.mIsMuted = this.mAudioManager.isMicrophoneMute();
                    if (isInActiveState()) {
                        updateSystemMuteState();
                    }
                    break;
                default:
                    Log.e(this, new IllegalStateException(), "Unexpected message code %d", new Object[]{Integer.valueOf(message.what)});
                    break;
            }
            return;
        }
        ((Runnable) message.obj).run();
    }

    public void dumpPendingMessages(final IndentingPrintWriter indentingPrintWriter) {
        Looper looper = getHandler().getLooper();
        Objects.requireNonNull(indentingPrintWriter);
        looper.dump(new Printer() {
            @Override
            public final void println(String str) {
                indentingPrintWriter.println(str);
            }
        }, "");
    }

    public boolean isHfpDeviceAvailable() {
        return this.mBluetoothRouteManager.isBluetoothAvailable();
    }

    private void setSpeakerphoneOn(boolean z) {
        Log.i(this, "turning speaker phone %s", new Object[]{Boolean.valueOf(z)});
        this.mAudioManager.setSpeakerphoneOn(z);
        this.mStatusBarNotifier.notifySpeakerphone(z);
    }

    private void setBluetoothOn(String str) {
        if (this.mBluetoothRouteManager.isBluetoothAvailable()) {
            BluetoothDevice bluetoothAudioConnectedDevice = this.mBluetoothRouteManager.getBluetoothAudioConnectedDevice();
            if (str == null && bluetoothAudioConnectedDevice != null) {
                Log.i(this, "HFP audio already on. Skipping connecting.", new Object[0]);
                sendInternalMessage(1302);
            } else if (bluetoothAudioConnectedDevice == null || !Objects.equals(str, bluetoothAudioConnectedDevice.getAddress())) {
                Log.i(this, "connecting bluetooth audio: %s", new Object[]{str});
                this.mBluetoothRouteManager.connectBluetoothAudio(str);
            }
        }
    }

    private void setBluetoothOff() {
        if (this.mBluetoothRouteManager.isBluetoothAvailable() && this.mBluetoothRouteManager.isBluetoothAudioConnectedOrPending()) {
            Log.i(this, "disconnecting bluetooth audio", new Object[0]);
            this.mBluetoothRouteManager.disconnectBluetoothAudio();
        }
    }

    private void setMuteOn(boolean z) {
        this.mIsMuted = z;
        Log.addEvent(this.mCallsManager.getForegroundCall(), z ? "MUTE" : "UNMUTE");
        if (z != this.mAudioManager.isMicrophoneMute() && isInActiveState()) {
            IAudioService audioService = this.mAudioServiceFactory.getAudioService();
            Object[] objArr = new Object[2];
            objArr[0] = Boolean.valueOf(z);
            objArr[1] = Boolean.valueOf(audioService == null);
            Log.i(this, "changing microphone mute state to: %b [serviceIsNull=%b]", objArr);
            if (audioService != null) {
                try {
                    audioService.setMicrophoneMute(z, this.mContext.getOpPackageName(), getCurrentUserId());
                } catch (RemoteException e) {
                    Log.e(this, e, "Remote exception while toggling mute.", new Object[0]);
                }
            }
        }
    }

    private void updateSystemMuteState() {
        setSystemAudioState(new CallAudioState(this.mIsMuted, this.mCurrentCallAudioState.getRoute(), this.mAvailableRoutes, this.mCurrentCallAudioState.getActiveBluetoothDevice(), this.mBluetoothRouteManager.getConnectedDevices()));
        updateInternalCallAudioState();
    }

    private void updateInternalCallAudioState() {
        IState currentState = getCurrentState();
        if (currentState == null) {
            Log.e(this, new IllegalStateException(), "Current state should never be null when updateInternalCallAudioState is called.", new Object[0]);
            this.mCurrentCallAudioState = new CallAudioState(this.mIsMuted, this.mCurrentCallAudioState.getRoute(), this.mAvailableRoutes, this.mBluetoothRouteManager.getBluetoothAudioConnectedDevice(), this.mBluetoothRouteManager.getConnectedDevices());
        } else {
            this.mCurrentCallAudioState = new CallAudioState(this.mIsMuted, this.mStateNameToRouteCode.get(currentState.getName()).intValue(), this.mAvailableRoutes, this.mBluetoothRouteManager.getBluetoothAudioConnectedDevice(), this.mBluetoothRouteManager.getConnectedDevices());
        }
    }

    private void setSystemAudioState(CallAudioState callAudioState) {
        setSystemAudioState(callAudioState, false);
    }

    private void resendSystemAudioState() {
        setSystemAudioState(this.mLastKnownCallAudioState, true);
    }

    private void setSystemAudioState(CallAudioState callAudioState, boolean z) {
        synchronized (this.mLock) {
            Log.i(this, "setSystemAudioState: changing from %s to %s", new Object[]{this.mLastKnownCallAudioState, callAudioState});
            if (z || !callAudioState.equals(this.mLastKnownCallAudioState)) {
                this.mStatusBarNotifier.notifyMute(callAudioState.isMuted());
                this.mCallsManager.onCallAudioStateChanged(this.mLastKnownCallAudioState, callAudioState);
                updateAudioForForegroundCall(callAudioState);
                this.mLastKnownCallAudioState = callAudioState;
            }
        }
    }

    private void updateAudioForForegroundCall(CallAudioState callAudioState) {
        Call foregroundCall = this.mCallsManager.getForegroundCall();
        if (foregroundCall != null && foregroundCall.getConnectionService() != null) {
            foregroundCall.getConnectionService().onCallAudioStateChanged(foregroundCall, callAudioState);
        }
    }

    private int calculateSupportedRoutes() {
        int i;
        if (this.mWiredHeadsetManager.isPluggedIn()) {
            i = 12;
        } else if (this.mDoesDeviceSupportEarpieceRoute) {
            i = 9;
        } else {
            i = 8;
        }
        if (this.mBluetoothRouteManager.isBluetoothAvailable()) {
            return i | 2;
        }
        return i;
    }

    private void sendInternalMessage(int i) {
        sendInternalMessage(i, 0);
    }

    private void sendInternalMessage(int i, int i2) {
        Session sessionCreateSubsession = Log.createSubsession();
        if (sessionCreateSubsession != null) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = sessionCreateSubsession;
            sendMessageAtFrontOfQueue(i, i2, 0, someArgsObtain);
            return;
        }
        sendMessageAtFrontOfQueue(i, i2);
    }

    private CallAudioState getInitialAudioState() {
        int iCalculateSupportedRoutes = calculateSupportedRoutes() & getCurrentCallSupportedRoutes();
        return new CallAudioState(false, ((iCalculateSupportedRoutes & 2) == 0 || this.mBluetoothRouteManager.getActiveDeviceAddress() == null) ? (iCalculateSupportedRoutes & 4) != 0 ? 4 : (iCalculateSupportedRoutes & 1) != 0 ? 1 : 8 : 2, iCalculateSupportedRoutes, null, this.mBluetoothRouteManager.getConnectedDevices());
    }

    private int getCurrentUserId() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            int i = ActivityManager.getService().getCurrentUser().id;
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return i;
        } catch (RemoteException e) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            return 0;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            throw th;
        }
    }

    public boolean isInActiveState() {
        AudioState currentState = getCurrentState();
        if (currentState == null) {
            Log.w(this, "Current state is null, assuming inactive state", new Object[0]);
            return false;
        }
        return currentState.isActive();
    }

    private boolean checkForEarpieceSupport() {
        for (AudioDeviceInfo audioDeviceInfo : this.mAudioManager.getDevices(2)) {
            if (audioDeviceInfo.getType() == 1) {
                return true;
            }
        }
        return false;
    }

    private int calculateBaselineRouteMessage(boolean z, boolean z2) {
        boolean zHasVideoCall;
        if (!z) {
            synchronized (this.mLock) {
                zHasVideoCall = this.mCallsManager.hasVideoCall();
            }
        } else {
            zHasVideoCall = false;
        }
        return ((this.mAvailableRoutes & 2) == 0 || this.mHasUserExplicitlyLeftBluetooth || !z2 || this.mBluetoothRouteManager.getActiveDeviceAddress() == null) ? ((this.mAvailableRoutes & 1) == 0 || zHasVideoCall) ? (this.mAvailableRoutes & 4) != 0 ? z ? 1103 : 1003 : z ? 1104 : 1004 : z ? 1101 : 1001 : z ? 1102 : 1002;
    }

    private void reinitialize() {
        CallAudioState initialAudioState = getInitialAudioState();
        this.mDeviceSupportedRoutes = initialAudioState.getSupportedRouteMask();
        this.mAvailableRoutes = this.mDeviceSupportedRoutes & getCurrentCallSupportedRoutes();
        this.mIsMuted = initialAudioState.isMuted();
        setSpeakerphoneOn(initialAudioState.getRoute() == 8);
        setMuteOn(this.mIsMuted);
        this.mWasOnSpeaker = false;
        this.mHasUserExplicitlyLeftBluetooth = false;
        this.mLastKnownCallAudioState = initialAudioState;
        transitionTo((IState) this.mRouteCodeToQuiescentState.get(Integer.valueOf(initialAudioState.getRoute())));
    }

    private void updateRouteForForegroundCall() {
        this.mAvailableRoutes = this.mDeviceSupportedRoutes & getCurrentCallSupportedRoutes();
        CallAudioState currentCallAudioState = getCurrentCallAudioState();
        if ((currentCallAudioState.getRoute() & this.mAvailableRoutes) == 0) {
            sendInternalMessage(calculateBaselineRouteMessage(false, true));
        }
    }

    private int getCurrentCallSupportedRoutes() {
        Call foregroundCall = this.mCallsManager.getForegroundCall();
        if (foregroundCall != null) {
            return 15 & foregroundCall.getSupportedAudioRoutes();
        }
        return 15;
    }

    private int modifyRoutes(int i, int i2, int i3, boolean z) {
        int i4 = i & (~i2);
        if (z) {
            i3 &= getCurrentCallSupportedRoutes();
        }
        return i4 | i3;
    }

    public void restoreMuteOnWhenInCallMode() {
        if (this.mIsMuted && isInActiveState()) {
            IAudioService audioService = this.mAudioServiceFactory.getAudioService();
            Object[] objArr = new Object[2];
            objArr[0] = Boolean.valueOf(this.mIsMuted);
            objArr[1] = Boolean.valueOf(audioService == null);
            Log.i(this, "changing microphone mute state to: %b [serviceIsNull=%b]", objArr);
            if (audioService != null) {
                try {
                    audioService.setMicrophoneMute(this.mIsMuted, this.mContext.getOpPackageName(), getCurrentUserId());
                } catch (RemoteException e) {
                    Log.e(this, e, "Remote exception while toggling mute.", new Object[0]);
                }
            }
        }
    }

    private void updateStatusBarAudioState() {
        boolean zIsMuted = this.mCurrentCallAudioState.isMuted();
        boolean z = this.mCurrentCallAudioState.getRoute() == 8;
        if (this.mStatusBarNotifier.isStatusBarShowingMute() != zIsMuted) {
            Log.i(this, "updateStatusBarAudioState, isMute : %b -> %b", new Object[]{Boolean.valueOf(this.mStatusBarNotifier.isStatusBarShowingMute()), Boolean.valueOf(zIsMuted)});
            this.mStatusBarNotifier.notifyMute(zIsMuted);
        }
        if (this.mStatusBarNotifier.isStatusBarShowingSpeaker() != z) {
            Log.i(this, "updateStatusBarAudioState, isSpeakerOn : %b -> %b", new Object[]{Boolean.valueOf(this.mStatusBarNotifier.isStatusBarShowingSpeaker()), Boolean.valueOf(z)});
            this.mStatusBarNotifier.notifySpeakerphone(z);
        }
    }
}
