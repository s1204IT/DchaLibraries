package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import org.ksoap2.kdom.Node;

class SupplicantStateTracker extends StateMachine {
    private static boolean DBG = false;
    private static final int MAX_RETRIES_ON_ASSOCIATION_REJECT = 16;
    private static final int MAX_RETRIES_ON_AUTHENTICATION_FAILURE = 2;
    private static final String TAG = "SupplicantStateTracker";
    private int mAssociationRejectCount;
    private boolean mAuthFailureInSupplicantBroadcast;
    private int mAuthenticationFailuresCount;
    private final IBatteryStats mBatteryStats;
    private final State mCompletedState;
    private final Context mContext;
    private final State mDefaultState;
    private final State mDisconnectState;
    private final State mDormantState;
    private final State mHandshakeState;
    private final State mInactiveState;
    private boolean mNetworksDisabledDuringConnect;
    private final State mScanState;
    private final State mUninitializedState;
    private final WifiConfigStore mWifiConfigStore;
    private final WifiStateMachine mWifiStateMachine;

    static int access$108(SupplicantStateTracker x0) {
        int i = x0.mAuthenticationFailuresCount;
        x0.mAuthenticationFailuresCount = i + 1;
        return i;
    }

    static int access$808(SupplicantStateTracker x0) {
        int i = x0.mAssociationRejectCount;
        x0.mAssociationRejectCount = i + 1;
        return i;
    }

    void enableVerboseLogging(int verbose) {
        if (verbose > 0) {
            DBG = true;
        } else {
            DBG = false;
        }
    }

    public String getSupplicantStateName() {
        return getCurrentState().getName();
    }

    public SupplicantStateTracker(Context c, WifiStateMachine wsm, WifiConfigStore wcs, Handler t) {
        super(TAG, t.getLooper());
        this.mAuthenticationFailuresCount = 0;
        this.mAssociationRejectCount = 0;
        this.mAuthFailureInSupplicantBroadcast = false;
        this.mNetworksDisabledDuringConnect = false;
        this.mUninitializedState = new UninitializedState();
        this.mDefaultState = new DefaultState();
        this.mInactiveState = new InactiveState();
        this.mDisconnectState = new DisconnectedState();
        this.mScanState = new ScanState();
        this.mHandshakeState = new HandshakeState();
        this.mCompletedState = new CompletedState();
        this.mDormantState = new DormantState();
        this.mContext = c;
        this.mWifiStateMachine = wsm;
        this.mWifiConfigStore = wcs;
        this.mBatteryStats = ServiceManager.getService("batterystats");
        addState(this.mDefaultState);
        addState(this.mUninitializedState, this.mDefaultState);
        addState(this.mInactiveState, this.mDefaultState);
        addState(this.mDisconnectState, this.mDefaultState);
        addState(this.mScanState, this.mDefaultState);
        addState(this.mHandshakeState, this.mDefaultState);
        addState(this.mCompletedState, this.mDefaultState);
        addState(this.mDormantState, this.mDefaultState);
        setInitialState(this.mUninitializedState);
        setLogRecSize(50);
        setLogOnlyTransitions(true);
        start();
    }

    private void handleNetworkConnectionFailure(int netId, int disableReason) {
        if (DBG) {
            Log.d(TAG, "handleNetworkConnectionFailure netId=" + Integer.toString(netId) + " reason " + Integer.toString(disableReason) + " mNetworksDisabledDuringConnect=" + this.mNetworksDisabledDuringConnect);
        }
        if (this.mNetworksDisabledDuringConnect) {
            this.mWifiConfigStore.enableAllNetworks();
            this.mNetworksDisabledDuringConnect = false;
        }
        this.mWifiConfigStore.disableNetwork(netId, disableReason);
    }

    private void transitionOnSupplicantStateChange(StateChangeResult stateChangeResult) {
        SupplicantState supState = stateChangeResult.state;
        if (DBG) {
            Log.d(TAG, "Supplicant state: " + supState.toString() + "\n");
        }
        switch (AnonymousClass1.$SwitchMap$android$net$wifi$SupplicantState[supState.ordinal()]) {
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
            case Node.ENTITY_REF:
            case Node.IGNORABLE_WHITESPACE:
            case Node.PROCESSING_INSTRUCTION:
                transitionTo(this.mHandshakeState);
                break;
            case Node.COMMENT:
                transitionTo(this.mCompletedState);
                break;
            case Node.DOCDECL:
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
                Log.e(TAG, "Unknown supplicant state " + supState);
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

    private void sendSupplicantStateChangedBroadcast(SupplicantState state, boolean failedAuth) {
        int supplState;
        switch (AnonymousClass1.$SwitchMap$android$net$wifi$SupplicantState[state.ordinal()]) {
            case 1:
                supplState = 1;
                break;
            case 2:
                supplState = 2;
                break;
            case 3:
                supplState = 4;
                break;
            case 4:
                supplState = 5;
                break;
            case 5:
                supplState = 6;
                break;
            case Node.ENTITY_REF:
                supplState = 7;
                break;
            case Node.IGNORABLE_WHITESPACE:
                supplState = 8;
                break;
            case Node.PROCESSING_INSTRUCTION:
                supplState = 9;
                break;
            case Node.COMMENT:
                supplState = 10;
                break;
            case Node.DOCDECL:
                supplState = 11;
                break;
            case 11:
                supplState = 3;
                break;
            case 12:
                supplState = 12;
                break;
            case 13:
                supplState = 0;
                break;
            default:
                Slog.w(TAG, "Unknown supplicant state " + state);
                supplState = 0;
                break;
        }
        try {
            this.mBatteryStats.noteWifiSupplicantStateChanged(supplState, failedAuth);
        } catch (RemoteException e) {
        }
        Intent intent = new Intent("android.net.wifi.supplicant.STATE_CHANGE");
        intent.addFlags(603979776);
        intent.putExtra("newState", (Parcelable) state);
        if (failedAuth) {
            intent.putExtra("supplicantError", 1);
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
                    SupplicantState state = stateChangeResult.state;
                    SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(state, SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast);
                    SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast = false;
                    SupplicantStateTracker.this.transitionOnSupplicantStateChange(stateChangeResult);
                    return true;
                case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    SupplicantStateTracker.access$108(SupplicantStateTracker.this);
                    SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast = true;
                    return true;
                case WifiMonitor.ASSOCIATION_REJECTION_EVENT:
                    SupplicantStateTracker.access$808(SupplicantStateTracker.this);
                    return true;
                case 151553:
                    SupplicantStateTracker.this.mNetworksDisabledDuringConnect = true;
                    SupplicantStateTracker.this.mAssociationRejectCount = 0;
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
            Message message = SupplicantStateTracker.this.getCurrentMessage();
            StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
            if (SupplicantStateTracker.this.mAuthenticationFailuresCount < 2) {
                if (SupplicantStateTracker.this.mAssociationRejectCount >= SupplicantStateTracker.MAX_RETRIES_ON_ASSOCIATION_REJECT) {
                    Log.d(SupplicantStateTracker.TAG, "Association getting rejected, disabling network " + stateChangeResult.networkId);
                    SupplicantStateTracker.this.handleNetworkConnectionFailure(stateChangeResult.networkId, 4);
                    SupplicantStateTracker.this.mAssociationRejectCount = 0;
                    return;
                }
                return;
            }
            Log.d(SupplicantStateTracker.TAG, "Failed to authenticate, disabling network " + stateChangeResult.networkId);
            SupplicantStateTracker.this.handleNetworkConnectionFailure(stateChangeResult.networkId, 3);
            SupplicantStateTracker.this.mAuthenticationFailuresCount = 0;
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
            switch (message.what) {
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantState state = stateChangeResult.state;
                    if (SupplicantState.isHandshakeState(state)) {
                        if (this.mLoopDetectIndex > state.ordinal()) {
                            this.mLoopDetectCount++;
                        }
                        if (this.mLoopDetectCount > 4) {
                            Log.d(SupplicantStateTracker.TAG, "Supplicant loop detected, disabling network " + stateChangeResult.networkId);
                            SupplicantStateTracker.this.handleNetworkConnectionFailure(stateChangeResult.networkId, 3);
                        }
                        this.mLoopDetectIndex = state.ordinal();
                        SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(state, SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast);
                    }
                    break;
            }
            return false;
        }
    }

    class CompletedState extends State {
        CompletedState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
            SupplicantStateTracker.this.mAuthenticationFailuresCount = 0;
            SupplicantStateTracker.this.mAssociationRejectCount = 0;
            if (SupplicantStateTracker.this.mNetworksDisabledDuringConnect) {
                SupplicantStateTracker.this.mWifiConfigStore.enableAllNetworks();
                SupplicantStateTracker.this.mNetworksDisabledDuringConnect = false;
            }
        }

        public boolean processMessage(Message message) {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + message.toString() + "\n");
            }
            switch (message.what) {
                case 131183:
                    SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(SupplicantState.DISCONNECTED, false);
                    SupplicantStateTracker.this.transitionTo(SupplicantStateTracker.this.mUninitializedState);
                    return true;
                case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    StateChangeResult stateChangeResult = (StateChangeResult) message.obj;
                    SupplicantState state = stateChangeResult.state;
                    SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(state, SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast);
                    if (!SupplicantState.isConnecting(state)) {
                        SupplicantStateTracker.this.transitionOnSupplicantStateChange(stateChangeResult);
                    }
                    return true;
                default:
                    return false;
            }
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

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println("mAuthenticationFailuresCount " + this.mAuthenticationFailuresCount);
        pw.println("mAuthFailureInSupplicantBroadcast " + this.mAuthFailureInSupplicantBroadcast);
        pw.println("mNetworksDisabledDuringConnect " + this.mNetworksDisabledDuringConnect);
        pw.println();
    }
}
