package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiNative;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class ClientModeManager implements ActiveModeManager {
    private static final String TAG = "WifiClientModeManager";
    private String mClientInterfaceName;
    private final Context mContext;
    private final Listener mListener;
    private final ScanRequestProxy mScanRequestProxy;
    private final ClientModeStateMachine mStateMachine;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;
    private final WifiStateMachine mWifiStateMachine;
    private boolean mIfaceIsUp = false;
    private boolean mExpectedStop = false;

    public interface Listener {
        void onStateChanged(int i);
    }

    ClientModeManager(Context context, Looper looper, WifiNative wifiNative, Listener listener, WifiMetrics wifiMetrics, ScanRequestProxy scanRequestProxy, WifiStateMachine wifiStateMachine) {
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mListener = listener;
        this.mWifiMetrics = wifiMetrics;
        this.mScanRequestProxy = scanRequestProxy;
        this.mWifiStateMachine = wifiStateMachine;
        this.mStateMachine = new ClientModeStateMachine(looper);
    }

    @Override
    public void start() {
        this.mStateMachine.sendMessage(0);
    }

    @Override
    public void stop() {
        Log.d(TAG, " currentstate: " + getCurrentStateName());
        this.mExpectedStop = true;
        if (this.mClientInterfaceName != null) {
            if (this.mIfaceIsUp) {
                updateWifiState(0, 3);
            } else {
                updateWifiState(0, 2);
            }
        }
        this.mStateMachine.quitNow();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("--Dump of ClientModeManager--");
        printWriter.println("current StateMachine mode: " + getCurrentStateName());
        printWriter.println("mClientInterfaceName: " + this.mClientInterfaceName);
        printWriter.println("mIfaceIsUp: " + this.mIfaceIsUp);
    }

    private String getCurrentStateName() {
        IState currentState = this.mStateMachine.getCurrentState();
        if (currentState != null) {
            return currentState.getName();
        }
        return "StateMachine not active";
    }

    private void updateWifiState(int i, int i2) {
        if (!this.mExpectedStop) {
            this.mListener.onStateChanged(i);
        } else {
            Log.d(TAG, "expected stop, not triggering callbacks: newState = " + i);
        }
        if (i == 4 || i == 1) {
            this.mExpectedStop = true;
        }
        if (i == 4) {
            return;
        }
        this.mWifiStateMachine.setWifiStateForApiCalls(i);
        Intent intent = new Intent("android.net.wifi.WIFI_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("wifi_state", i);
        intent.putExtra("previous_wifi_state", i2);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private class ClientModeStateMachine extends StateMachine {
        public static final int CMD_INTERFACE_DESTROYED = 4;
        public static final int CMD_INTERFACE_DOWN = 5;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_START = 0;
        private final State mIdleState;
        private final State mStartedState;
        private final WifiNative.InterfaceCallback mWifiNativeInterfaceCallback;

        ClientModeStateMachine(Looper looper) {
            super(ClientModeManager.TAG, looper);
            this.mIdleState = new IdleState();
            this.mStartedState = new StartedState();
            this.mWifiNativeInterfaceCallback = new WifiNative.InterfaceCallback() {
                @Override
                public void onDestroyed(String str) {
                    if (ClientModeManager.this.mClientInterfaceName != null && ClientModeManager.this.mClientInterfaceName.equals(str)) {
                        Log.d(ClientModeManager.TAG, "STA iface " + str + " was destroyed, stopping client mode");
                        ClientModeManager.this.mWifiStateMachine.handleIfaceDestroyed();
                        ClientModeStateMachine.this.sendMessage(4);
                    }
                }

                @Override
                public void onUp(String str) {
                    if (ClientModeManager.this.mClientInterfaceName != null && ClientModeManager.this.mClientInterfaceName.equals(str)) {
                        ClientModeStateMachine.this.sendMessage(3, 1);
                    }
                }

                @Override
                public void onDown(String str) {
                    if (ClientModeManager.this.mClientInterfaceName != null && ClientModeManager.this.mClientInterfaceName.equals(str)) {
                        ClientModeStateMachine.this.sendMessage(3, 0);
                    }
                }
            };
            addState(this.mIdleState);
            addState(this.mStartedState);
            setInitialState(this.mIdleState);
            start();
        }

        private class IdleState extends State {
            private IdleState() {
            }

            public void enter() {
                Log.d(ClientModeManager.TAG, "entering IdleState");
                ClientModeManager.this.mClientInterfaceName = null;
                ClientModeManager.this.mIfaceIsUp = false;
            }

            public boolean processMessage(Message message) {
                if (message.what == 0) {
                    ClientModeManager.this.updateWifiState(2, 1);
                    ClientModeManager.this.mClientInterfaceName = ClientModeManager.this.mWifiNative.setupInterfaceForClientMode(false, ClientModeStateMachine.this.mWifiNativeInterfaceCallback);
                    if (!TextUtils.isEmpty(ClientModeManager.this.mClientInterfaceName)) {
                        ClientModeStateMachine.this.sendScanAvailableBroadcast(false);
                        ClientModeManager.this.mScanRequestProxy.enableScanningForHiddenNetworks(false);
                        ClientModeManager.this.mScanRequestProxy.clearScanResults();
                        ClientModeStateMachine.this.transitionTo(ClientModeStateMachine.this.mStartedState);
                    } else {
                        Log.e(ClientModeManager.TAG, "Failed to create ClientInterface. Sit in Idle");
                        ClientModeManager.this.updateWifiState(4, 2);
                        ClientModeManager.this.updateWifiState(1, 4);
                    }
                    return true;
                }
                Log.d(ClientModeManager.TAG, "received an invalid message: " + message);
                return false;
            }
        }

        private class StartedState extends State {
            private StartedState() {
            }

            private void onUpChanged(boolean z) {
                if (z != ClientModeManager.this.mIfaceIsUp) {
                    ClientModeManager.this.mIfaceIsUp = z;
                    if (!z) {
                        if (ClientModeManager.this.mWifiStateMachine.isConnectedMacRandomizationEnabled()) {
                            return;
                        }
                        Log.d(ClientModeManager.TAG, "interface down!");
                        ClientModeManager.this.mStateMachine.sendMessage(5);
                        return;
                    }
                    Log.d(ClientModeManager.TAG, "Wifi is ready to use for client mode");
                    ClientModeStateMachine.this.sendScanAvailableBroadcast(true);
                    ClientModeManager.this.mWifiStateMachine.setOperationalMode(1, ClientModeManager.this.mClientInterfaceName);
                    ClientModeManager.this.updateWifiState(3, 2);
                }
            }

            public void enter() {
                Log.d(ClientModeManager.TAG, "entering StartedState");
                ClientModeManager.this.mIfaceIsUp = false;
                onUpChanged(ClientModeManager.this.mWifiNative.isInterfaceUp(ClientModeManager.this.mClientInterfaceName));
                ClientModeManager.this.mScanRequestProxy.enableScanningForHiddenNetworks(true);
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 0) {
                    switch (i) {
                        case 3:
                            onUpChanged(message.arg1 == 1);
                            break;
                        case 4:
                            Log.d(ClientModeManager.TAG, "interface destroyed - client mode stopping");
                            ClientModeManager.this.updateWifiState(0, 3);
                            ClientModeManager.this.mClientInterfaceName = null;
                            ClientModeStateMachine.this.transitionTo(ClientModeStateMachine.this.mIdleState);
                            break;
                        case 5:
                            Log.e(ClientModeManager.TAG, "Detected an interface down, reporting failure to SelfRecovery");
                            ClientModeManager.this.mWifiStateMachine.failureDetected(2);
                            ClientModeManager.this.updateWifiState(0, 4);
                            ClientModeStateMachine.this.transitionTo(ClientModeStateMachine.this.mIdleState);
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }

            public void exit() {
                ClientModeManager.this.mWifiStateMachine.setOperationalMode(4, null);
                if (ClientModeManager.this.mClientInterfaceName != null) {
                    ClientModeManager.this.mWifiNative.teardownInterface(ClientModeManager.this.mClientInterfaceName);
                    ClientModeManager.this.mClientInterfaceName = null;
                    ClientModeManager.this.mIfaceIsUp = false;
                }
                ClientModeManager.this.updateWifiState(1, 0);
                ClientModeManager.this.mStateMachine.quitNow();
            }
        }

        private void sendScanAvailableBroadcast(boolean z) {
            Log.d(ClientModeManager.TAG, "sending scan available broadcast: " + z);
            Intent intent = new Intent("wifi_scan_available");
            intent.addFlags(67108864);
            if (z) {
                intent.putExtra("scan_enabled", 3);
            } else {
                intent.putExtra("scan_enabled", 1);
            }
            ClientModeManager.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }
}
