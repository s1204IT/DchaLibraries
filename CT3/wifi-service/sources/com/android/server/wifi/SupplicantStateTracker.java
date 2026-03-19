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

public class SupplicantStateTracker extends StateMachine {

    private static final int[] f0androidnetwifiSupplicantStateSwitchesValues = null;
    private static boolean DBG = false;
    private static final int MAX_RETRIES_ON_ASSOCIATION_REJECT = 16;
    private static final int MAX_RETRIES_ON_AUTHENTICATION_FAILURE = 2;
    private static final String TAG = "SupplicantStateTracker";
    private boolean mAuthFailureInSupplicantBroadcast;
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
    private final WifiConfigManager mWifiConfigManager;

    private static int[] m30getandroidnetwifiSupplicantStateSwitchesValues() {
        if (f0androidnetwifiSupplicantStateSwitchesValues != null) {
            return f0androidnetwifiSupplicantStateSwitchesValues;
        }
        int[] iArr = new int[SupplicantState.values().length];
        try {
            iArr[SupplicantState.ASSOCIATED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SupplicantState.ASSOCIATING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SupplicantState.AUTHENTICATING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SupplicantState.COMPLETED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SupplicantState.DISCONNECTED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[SupplicantState.DORMANT.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[SupplicantState.FOUR_WAY_HANDSHAKE.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[SupplicantState.GROUP_HANDSHAKE.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[SupplicantState.INACTIVE.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[SupplicantState.INTERFACE_DISABLED.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[SupplicantState.INVALID.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[SupplicantState.SCANNING.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[SupplicantState.UNINITIALIZED.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        f0androidnetwifiSupplicantStateSwitchesValues = iArr;
        return iArr;
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

    public SupplicantStateTracker(Context c, WifiConfigManager wcs, Handler t) {
        super(TAG, t.getLooper());
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
        this.mWifiConfigManager = wcs;
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
            this.mWifiConfigManager.enableAllNetworks();
            this.mNetworksDisabledDuringConnect = false;
        }
        this.mWifiConfigManager.updateNetworkSelectionStatus(netId, disableReason);
    }

    private void transitionOnSupplicantStateChange(StateChangeResult stateChangeResult) {
        SupplicantState supState = stateChangeResult.state;
        if (DBG) {
            Log.d(TAG, "Supplicant state: " + supState.toString() + "\n");
        }
        switch (m30getandroidnetwifiSupplicantStateSwitchesValues()[supState.ordinal()]) {
            case 1:
            case 2:
            case 3:
            case 7:
            case 8:
                transitionTo(this.mHandshakeState);
                break;
            case 4:
                transitionTo(this.mCompletedState);
                break;
            case 5:
                transitionTo(this.mDisconnectState);
                break;
            case 6:
                transitionTo(this.mDormantState);
                break;
            case 9:
                transitionTo(this.mInactiveState);
                break;
            case 10:
                break;
            case 11:
            case 13:
                transitionTo(this.mUninitializedState);
                break;
            case 12:
                transitionTo(this.mScanState);
                break;
            default:
                Log.e(TAG, "Unknown supplicant state " + supState);
                break;
        }
    }

    private void sendSupplicantStateChangedBroadcast(SupplicantState state, boolean failedAuth) {
        int supplState;
        switch (m30getandroidnetwifiSupplicantStateSwitchesValues()[state.ordinal()]) {
            case 1:
                supplState = 7;
                break;
            case 2:
                supplState = 6;
                break;
            case 3:
                supplState = 5;
                break;
            case 4:
                supplState = 10;
                break;
            case 5:
                supplState = 1;
                break;
            case 6:
                supplState = 11;
                break;
            case 7:
                supplState = 8;
                break;
            case 8:
                supplState = 9;
                break;
            case 9:
                supplState = 3;
                break;
            case 10:
                supplState = 2;
                break;
            case 11:
                supplState = 0;
                break;
            case 12:
                supplState = 4;
                break;
            case 13:
                supplState = 12;
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
                    SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast = true;
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
                    if (!SupplicantState.isHandshakeState(state)) {
                        return false;
                    }
                    if (this.mLoopDetectIndex > state.ordinal()) {
                        this.mLoopDetectCount++;
                    }
                    if (this.mLoopDetectCount > 4) {
                        Log.d(SupplicantStateTracker.TAG, "Supplicant loop detected, disabling network " + stateChangeResult.networkId);
                        SupplicantStateTracker.this.handleNetworkConnectionFailure(stateChangeResult.networkId, 3);
                    }
                    this.mLoopDetectIndex = state.ordinal();
                    SupplicantStateTracker.this.sendSupplicantStateChangedBroadcast(state, SupplicantStateTracker.this.mAuthFailureInSupplicantBroadcast);
                    return true;
                default:
                    return false;
            }
        }
    }

    class CompletedState extends State {
        CompletedState() {
        }

        public void enter() {
            if (SupplicantStateTracker.DBG) {
                Log.d(SupplicantStateTracker.TAG, getName() + "\n");
            }
            if (!SupplicantStateTracker.this.mNetworksDisabledDuringConnect) {
                return;
            }
            SupplicantStateTracker.this.mWifiConfigManager.enableAllNetworks();
            SupplicantStateTracker.this.mNetworksDisabledDuringConnect = false;
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
                        return true;
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
        pw.println("mAuthFailureInSupplicantBroadcast " + this.mAuthFailureInSupplicantBroadcast);
        pw.println("mNetworksDisabledDuringConnect " + this.mNetworksDisabledDuringConnect);
        pw.println();
    }

    boolean isNetworksDisabledDuringConnect() {
        return this.mNetworksDisabledDuringConnect;
    }
}
