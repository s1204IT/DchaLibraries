package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.wifi.WifiConfiguration;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.util.ApConfigUtil;
import java.util.ArrayList;
import java.util.Locale;

public class SoftApManager {
    private static final String TAG = "SoftApManager";
    private final ArrayList<Integer> mAllowed2GChannels;
    private final ConnectivityManager mConnectivityManager;
    private final Context mContext;
    private final String mCountryCode;
    private final String mInterfaceName;
    private final Listener mListener;
    private final INetworkManagementService mNmService;
    private final SoftApStateMachine mStateMachine;
    private String mTetherInterfaceName;
    private BroadcastReceiver mTetherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(SoftApManager.TAG, "onReceive : ConnectivityManager.ACTION_TETHER_STATE_CHANGED");
            ArrayList<String> available = intent.getStringArrayListExtra("availableArray");
            ArrayList<String> active = intent.getStringArrayListExtra("activeArray");
            SoftApManager.this.mStateMachine.sendMessage(2, new TetherStateChange(available, active));
        }
    };
    private final WifiNative mWifiNative;

    public interface Listener {
        void onStateChanged(int i, int i2);
    }

    private static class TetherStateChange {
        public ArrayList<String> active;
        public ArrayList<String> available;

        TetherStateChange(ArrayList<String> av, ArrayList<String> ac) {
            this.available = av;
            this.active = ac;
        }
    }

    public SoftApManager(Context context, Looper looper, WifiNative wifiNative, INetworkManagementService nmService, ConnectivityManager connectivityManager, String countryCode, ArrayList<Integer> allowed2GChannels, Listener listener) {
        this.mStateMachine = new SoftApStateMachine(looper);
        this.mContext = context;
        this.mNmService = nmService;
        this.mWifiNative = wifiNative;
        this.mConnectivityManager = connectivityManager;
        this.mCountryCode = countryCode;
        this.mAllowed2GChannels = allowed2GChannels;
        this.mListener = listener;
        this.mInterfaceName = this.mWifiNative.getInterfaceName();
        this.mContext.registerReceiver(this.mTetherReceiver, new IntentFilter("android.net.conn.TETHER_STATE_CHANGED"));
    }

    public void start(WifiConfiguration config) {
        this.mStateMachine.sendMessage(0, config);
    }

    public void stop() {
        this.mStateMachine.sendMessage(1);
    }

    public void destroy() {
        this.mContext.unregisterReceiver(this.mTetherReceiver);
    }

    private void updateApState(int state, int reason) {
        if (this.mListener == null) {
            return;
        }
        this.mListener.onStateChanged(state, reason);
    }

    private int startSoftAp(WifiConfiguration config) {
        if (config == null) {
            Log.e(TAG, "Unable to start soft AP without configuration");
            return 2;
        }
        WifiConfiguration localConfig = new WifiConfiguration(config);
        if (this.mCountryCode != null && !this.mWifiNative.setCountryCodeHal(this.mCountryCode.toUpperCase(Locale.ROOT)) && config.apBand == 1) {
            Log.e(TAG, "Failed to set country code, required for setting up soft ap in 5GHz");
            return 2;
        }
        int result = ApConfigUtil.updateApChannelConfig(this.mWifiNative, this.mCountryCode, this.mAllowed2GChannels, localConfig);
        if (result != 0) {
            Log.e(TAG, "Failed to update AP band and channel");
            return result;
        }
        try {
            this.mNmService.startAccessPoint(localConfig, this.mInterfaceName);
            Log.d(TAG, "Soft AP is started");
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Exception in starting soft AP: " + e);
            return 2;
        }
    }

    private void stopSoftAp() {
        try {
            this.mNmService.stopAccessPoint(this.mInterfaceName);
            Log.d(TAG, "Soft AP is stopped");
        } catch (Exception e) {
            Log.e(TAG, "Exception in stopping soft AP: " + e);
        }
    }

    private boolean startTethering(ArrayList<String> available) {
        String[] wifiRegexs = this.mConnectivityManager.getTetherableWifiRegexs();
        for (String intf : available) {
            for (String regex : wifiRegexs) {
                if (intf.matches(regex)) {
                    try {
                        InterfaceConfiguration ifcg = this.mNmService.getInterfaceConfig(intf);
                        if (ifcg != null) {
                            ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress("192.168.43.1"), 24));
                            ifcg.setInterfaceUp();
                            this.mNmService.setInterfaceConfig(intf, ifcg);
                        }
                        if (this.mConnectivityManager.tether(intf) != 0) {
                            Log.e(TAG, "Error tethering on " + intf);
                            return false;
                        }
                        this.mTetherInterfaceName = intf;
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error configuring interface " + intf + ", :" + e);
                        return false;
                    }
                }
            }
        }
        return false;
    }

    private void stopTethering() {
        try {
            InterfaceConfiguration ifcg = this.mNmService.getInterfaceConfig(this.mTetherInterfaceName);
            if (ifcg != null) {
                ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress("0.0.0.0"), 0));
                this.mNmService.setInterfaceConfig(this.mTetherInterfaceName, ifcg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting interface " + this.mTetherInterfaceName + ", :" + e);
        }
        if (this.mConnectivityManager.untether(this.mTetherInterfaceName) == 0) {
            return;
        }
        Log.e(TAG, "Untether initiate failed!");
    }

    private boolean isWifiTethered(ArrayList<String> active) {
        String[] wifiRegexs = this.mConnectivityManager.getTetherableWifiRegexs();
        for (String intf : active) {
            for (String regex : wifiRegexs) {
                if (intf.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private class SoftApStateMachine extends StateMachine {
        public static final int CMD_START = 0;
        public static final int CMD_STOP = 1;
        public static final int CMD_TETHER_NOTIFICATION_TIMEOUT = 3;
        public static final int CMD_TETHER_STATE_CHANGE = 2;
        private static final int TETHER_NOTIFICATION_TIME_OUT_MSECS = 5000;
        private final State mIdleState;
        private final State mStartedState;
        private int mTetherToken;
        private final State mTetheredState;
        private final State mTetheringState;
        private final State mUntetheringState;

        SoftApStateMachine(Looper looper) {
            super(SoftApManager.TAG, looper);
            this.mTetherToken = 0;
            this.mIdleState = new IdleState(this, null);
            this.mStartedState = new StartedState(this, 0 == true ? 1 : 0);
            this.mTetheringState = new TetheringState(this, 0 == true ? 1 : 0);
            this.mTetheredState = new TetheredState(this, 0 == true ? 1 : 0);
            this.mUntetheringState = new UntetheringState(this, 0 == true ? 1 : 0);
            addState(this.mIdleState);
            addState(this.mStartedState, this.mIdleState);
            addState(this.mTetheringState, this.mStartedState);
            addState(this.mTetheredState, this.mStartedState);
            addState(this.mUntetheringState, this.mStartedState);
            setInitialState(this.mIdleState);
            start();
        }

        private class IdleState extends State {
            IdleState(SoftApStateMachine this$1, IdleState idleState) {
                this();
            }

            private IdleState() {
            }

            public void enter() {
                Log.d(SoftApManager.TAG, getName());
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 0:
                        SoftApManager.this.updateApState(12, 0);
                        int result = SoftApManager.this.startSoftAp((WifiConfiguration) message.obj);
                        if (result == 0) {
                            SoftApManager.this.updateApState(13, 0);
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                        } else {
                            int reason = 0;
                            if (result == 1) {
                                reason = 1;
                            }
                            SoftApManager.this.updateApState(14, reason);
                        }
                        return true;
                    case 1:
                    default:
                        return true;
                    case 2:
                        Log.e(SoftApManager.TAG, "Defer CMD_TETHER_STATE_CHANGE msg");
                        SoftApStateMachine.this.deferMessage(message);
                        return true;
                }
            }
        }

        private class StartedState extends State {
            StartedState(SoftApStateMachine this$1, StartedState startedState) {
                this();
            }

            private StartedState() {
            }

            public void enter() {
                Log.d(SoftApManager.TAG, getName());
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 0:
                        return true;
                    case 1:
                        SoftApManager.this.updateApState(10, 0);
                        SoftApManager.this.stopSoftAp();
                        SoftApManager.this.updateApState(11, 0);
                        SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                        return true;
                    case 2:
                        TetherStateChange stateChange = (TetherStateChange) message.obj;
                        if (SoftApManager.this.startTethering(stateChange.available)) {
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mTetheringState);
                            return true;
                        }
                        return true;
                    default:
                        return false;
                }
            }
        }

        private class TetheringState extends State {
            TetheringState(SoftApStateMachine this$1, TetheringState tetheringState) {
                this();
            }

            private TetheringState() {
            }

            public void enter() {
                Log.d(SoftApManager.TAG, getName());
                SoftApStateMachine.this.sendMessageDelayed(SoftApStateMachine.this.obtainMessage(3, SoftApStateMachine.this.mTetherToken++), 5000L);
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 2:
                        TetherStateChange stateChange = (TetherStateChange) message.obj;
                        if (SoftApManager.this.isWifiTethered(stateChange.active)) {
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mTetheredState);
                        }
                        return true;
                    case 3:
                        if (message.arg1 == SoftApStateMachine.this.mTetherToken) {
                            Log.e(SoftApManager.TAG, "Failed to get tether update, shutdown soft access point");
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                            SoftApStateMachine.this.sendMessageAtFrontOfQueue(1);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        }

        private class TetheredState extends State {
            TetheredState(SoftApStateMachine this$1, TetheredState tetheredState) {
                this();
            }

            private TetheredState() {
            }

            public void enter() {
                Log.d(SoftApManager.TAG, getName());
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 1:
                        Log.d(SoftApManager.TAG, "Untethering before stopping AP");
                        SoftApManager.this.stopTethering();
                        SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mUntetheringState);
                        return true;
                    case 2:
                        TetherStateChange stateChange = (TetherStateChange) message.obj;
                        if (!SoftApManager.this.isWifiTethered(stateChange.active)) {
                            Log.e(SoftApManager.TAG, "Tethering reports wifi as untethered!, shut down soft Ap");
                            SoftApStateMachine.this.sendMessage(1);
                        }
                        return true;
                    default:
                        return false;
                }
            }
        }

        private class UntetheringState extends State {
            UntetheringState(SoftApStateMachine this$1, UntetheringState untetheringState) {
                this();
            }

            private UntetheringState() {
            }

            public void enter() {
                Log.d(SoftApManager.TAG, getName());
                SoftApStateMachine.this.sendMessageDelayed(SoftApStateMachine.this.obtainMessage(3, SoftApStateMachine.this.mTetherToken++), 5000L);
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 2:
                        TetherStateChange stateChange = (TetherStateChange) message.obj;
                        if (!SoftApManager.this.isWifiTethered(stateChange.active)) {
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                            SoftApStateMachine.this.sendMessageAtFrontOfQueue(1);
                        }
                        return true;
                    case 3:
                        if (message.arg1 == SoftApStateMachine.this.mTetherToken) {
                            Log.e(SoftApManager.TAG, "Failed to get tether update, force stop access point");
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                            SoftApStateMachine.this.sendMessageAtFrontOfQueue(1);
                        }
                        return true;
                    default:
                        SoftApStateMachine.this.deferMessage(message);
                        return true;
                }
            }
        }
    }
}
