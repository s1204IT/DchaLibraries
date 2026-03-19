package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SupplicantStateTracker extends StateMachine {
    private static boolean DBG = false;
    private static final int MAX_RETRIES_ON_ASSOCIATION_REJECT = 16;
    private static final int MAX_RETRIES_ON_AUTHENTICATION_FAILURE = 2;
    private static final String TAG = "SupplicantStateTracker";
    private boolean mAuthFailureInSupplicantBroadcast;
    private int mAuthFailureReason;
    private final IBatteryStats mBatteryStats;
    private final State mCompletedState;
    private final State mConnectionActiveState;
    private final Context mContext;
    private final State mDefaultState;
    private final State mDisconnectState;
    private final State mDormantState;
    private FrameworkFacade mFacade;
    private final State mHandshakeState;
    private final State mInactiveState;
    private boolean mNetworksDisabledDuringConnect;
    private final State mScanState;
    private final State mUninitializedState;
    private final WifiConfigManager mWifiConfigManager;

    void enableVerboseLogging(int i) {
        if (i > 0) {
            DBG = true;
        } else {
            DBG = false;
        }
    }

    public String getSupplicantStateName() {
        return getCurrentState().getName();
    }

    public SupplicantStateTracker(Context context, WifiConfigManager wifiConfigManager, FrameworkFacade frameworkFacade, Handler handler) {
        super(TAG, handler.getLooper());
        this.mAuthFailureInSupplicantBroadcast = false;
        this.mNetworksDisabledDuringConnect = false;
        this.mUninitializedState = new UninitializedState();
        this.mDefaultState = new DefaultState();
        this.mInactiveState = new InactiveState();
        this.mDisconnectState = new DisconnectedState();
        this.mScanState = new ScanState();
        this.mConnectionActiveState = new ConnectionActiveState();
        this.mHandshakeState = new HandshakeState();
        this.mCompletedState = new CompletedState();
        this.mDormantState = new DormantState();
        this.mContext = context;
        this.mWifiConfigManager = wifiConfigManager;
        this.mFacade = frameworkFacade;
        this.mBatteryStats = this.mFacade.getBatteryService();
        addState(this.mDefaultState);
        addState(this.mUninitializedState, this.mDefaultState);
        addState(this.mInactiveState, this.mDefaultState);
        addState(this.mDisconnectState, this.mDefaultState);
        addState(this.mConnectionActiveState, this.mDefaultState);
        addState(this.mScanState, this.mConnectionActiveState);
        addState(this.mHandshakeState, this.mConnectionActiveState);
        addState(this.mCompletedState, this.mConnectionActiveState);
        addState(this.mDormantState, this.mConnectionActiveState);
        setInitialState(this.mUninitializedState);
        setLogRecSize(50);
        setLogOnlyTransitions(true);
        start();
    }

    private void handleNetworkConnectionFailure(int i, int i2) {
        if (DBG) {
            Log.d(TAG, "handleNetworkConnectionFailure netId=" + Integer.toString(i) + " reason " + Integer.toString(i2) + " mNetworksDisabledDuringConnect=" + this.mNetworksDisabledDuringConnect);
        }
        if (this.mNetworksDisabledDuringConnect) {
            this.mNetworksDisabledDuringConnect = false;
        }
        this.mWifiConfigManager.updateNetworkSelectionStatus(i, i2);
    }

    private void transitionOnSupplicantStateChange(StateChangeResult stateChangeResult) {
        SupplicantState supplicantState = stateChangeResult.state;
        if (DBG) {
            Log.d(TAG, "Supplicant state: " + supplicantState.toString() + "\n");
        }
        switch (AnonymousClass1.$SwitchMap$android$net$wifi$SupplicantState[supplicantState.ordinal()]) {
            case 1:
                transitionTo(this.mDisconnectState);
                break;
            case 2:
                break;
            case 3:
                transitionTo(this.mScanState);
                break;
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
                transitionTo(this.mHandshakeState);
                break;
            case 9:
                transitionTo(this.mCompletedState);
                break;
            case 10:
                transitionTo(this.mDormantState);
                break;
            case 11:
                transitionTo(this.mInactiveState);
                break;
            case 12:
            case 13:
                transitionTo(this.mUninitializedState);
                break;
            default:
                Log.e(TAG, "Unknown supplicant state " + supplicantState);
                break;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$net$wifi$SupplicantState = new int[SupplicantState.values().length];

        static {
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.DISCONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INTERFACE_DISABLED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.SCANNING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.AUTHENTICATING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.ASSOCIATING.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.ASSOCIATED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.FOUR_WAY_HANDSHAKE.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.GROUP_HANDSHAKE.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.COMPLETED.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.DORMANT.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INACTIVE.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.UNINITIALIZED.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$android$net$wifi$SupplicantState[SupplicantState.INVALID.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    private void sendSupplicantStateChangedBroadcast(SupplicantState supplicantState, boolean z) {
        sendSupplicantStateChangedBroadcast(supplicantState, z, 0);
    }

    private void sendSupplicantStateChangedBroadcast(SupplicantState supplicantState, boolean z, int i) {
        int i2;
        switch (AnonymousClass1.$SwitchMap$android$net$wifi$SupplicantState[supplicantState.ordinal()]) {
            case 1:
                i2 = 1;
                break;
            case 2:
                i2 = 2;
                break;
            case 3:
                i2 = 4;
                break;
            case 4:
                i2 = 5;
                break;
            case 5:
                i2 = 6;
                break;
            case 6:
                i2 = 7;
                break;
            case 7:
                i2 = 8;
                break;
            case 8:
                i2 = 9;
                break;
            case 9:
                i2 = 10;
                break;
            case 10:
                i2 = 11;
                break;
            case 11:
                i2 = 3;
                break;
            case 12:
                i2 = 12;
                break;
            default:
                Slog.w(TAG, "Unknown supplicant state " + supplicantState);
            case 13:
                i2 = 0;
                break;
        }
        try {
            this.mBatteryStats.noteWifiSupplicantStateChanged(i2, z);
        } catch (RemoteException e) {
        }
        Intent intent = new Intent("android.net.wifi.supplicant.STATE_CHANGE");
        intent.addFlags(603979776);
        intent.putExtra("newState", (Parcelable) supplicantState);
        if (z) {
            intent.putExtra("supplicantError", 1);
            intent.putExtra("supplicantErrorReason", i);
        }
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    class DefaultState extends State {
        DefaultState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
        }

        public boolean processMessage(Message message) {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + message.toString() + "\n");
            }
            switch (message.what) {
                case 131183:
                    SupplicantStateTracker.this.transitionTo(SupplicantStateTracker.this.mUninitializedState);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(stateChangeResult.state, SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast, SupplicantStateTracker.this.mAuthFailureReason);
                    SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast = false;
                    SupplicantStateTracker.this.mAuthFailureReason = 0;
                    SupplicantStateTracker.this.transitionOnSupplicantStateChange(stateChangeResult);
                    return true;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast = true;
                    SupplicantStateTracker.this.mAuthFailureReason = message.arg1;
                    return true;
                case 151553:
                    SupplicantStateTracker.this.mNetworksDisabledDuringConnect = true;
                    return true;
                default:
                    Log.e(SupplicantStateTracker.TAG, "Ignoring " + message);
                    return true;
            }
        }
    }

    class UninitializedState extends State {
        UninitializedState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
        }
    }

    class InactiveState extends State {
        InactiveState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
        }
    }

    class DisconnectedState extends State {
        DisconnectedState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
        }
    }

    class ScanState extends State {
        ScanState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
        }
    }

    class ConnectionActiveState extends State {
        ConnectionActiveState() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 131183) {
                SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(SupplicantState.DISCONNECTED, false);
            }
            return false;
        }
    }

    class HandshakeState extends State {
        private static final int MAX_SUPPLICANT_LOOP_ITERATIONS = 4;
        private int mLoopDetectCount;
        private int mLoopDetectIndex;

        HandshakeState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
            this.mLoopDetectIndex = 0;
            this.mLoopDetectCount = 0;
        }

        public boolean processMessage(Message message) {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + message.toString() + "\n");
            }
            if (message.what != 147462) {
                return false;
            }
            StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
            SupplicantState supplicantState = stateChangeResult.state;
            if (!SupplicantState.isHandshakeState(supplicantState)) {
                return false;
            }
            if (this.mLoopDetectIndex > supplicantState.ordinal()) {
                this.mLoopDetectCount++;
            }
            if (this.mLoopDetectCount > 4) {
                Log.d(SupplicantStateTracker.TAG, "Supplicant loop detected, disabling network " + stateChangeResult.networkId);
                SupplicantStateTracker.this.handleNetworkConnectionFailure(stateChangeResult.networkId, 3);
            }
            this.mLoopDetectIndex = supplicantState.ordinal();
            SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(supplicantState, SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast, SupplicantStateTracker.this.mAuthFailureReason);
            return true;
        }
    }

    class CompletedState extends State {
        CompletedState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
            if (SupplicantStateTracker.this.mNetworksDisabledDuringConnect) {
                SupplicantStateTracker.this.mNetworksDisabledDuringConnect = false;
            }
        }

        public boolean processMessage(Message message) {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + message.toString() + "\n");
            }
            if (message.what == 147462) {
                StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                SupplicantState supplicantState = stateChangeResult.state;
                SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(supplicantState, SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast, SupplicantStateTracker.this.mAuthFailureReason);
                if (!SupplicantState.isConnecting(supplicantState)) {
                    SupplicantStateTracker.this.transitionOnSupplicantStateChange(stateChangeResult);
                    return true;
                }
                return true;
            }
            return false;
        }
    }

    class DormantState extends State {
        DormantState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mAuthFailureInSupplicantBroadcast " + this.mAuthFailureInSupplicantBroadcast);
        printWriter.println("mAuthFailureReason " + this.mAuthFailureReason);
        printWriter.println("mNetworksDisabledDuringConnect " + this.mNetworksDisabledDuringConnect);
        printWriter.println();
    }
}
