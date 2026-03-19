package com.android.server.wifi;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiNative;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class ScanOnlyModeManager implements ActiveModeManager {
    private static final String TAG = "WifiScanOnlyModeManager";
    private String mClientInterfaceName;
    private final Context mContext;
    private final Listener mListener;
    private final ScanRequestProxy mScanRequestProxy;
    private final ScanOnlyModeStateMachine mStateMachine;
    private final WakeupController mWakeupController;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;
    private boolean mIfaceIsUp = false;
    private boolean mExpectedStop = false;

    public interface Listener {
        void onStateChanged(int i);
    }

    ScanOnlyModeManager(Context context, Looper looper, WifiNative wifiNative, Listener listener, WifiMetrics wifiMetrics, ScanRequestProxy scanRequestProxy, WakeupController wakeupController) {
        this.mContext = context;
        this.mWifiNative = wifiNative;
        this.mListener = listener;
        this.mWifiMetrics = wifiMetrics;
        this.mScanRequestProxy = scanRequestProxy;
        this.mWakeupController = wakeupController;
        this.mStateMachine = new ScanOnlyModeStateMachine(looper);
    }

    @Override
    public void start() {
        this.mStateMachine.sendMessage(0);
    }

    @Override
    public void stop() {
        Log.d(TAG, " currentstate: " + getCurrentStateName());
        this.mExpectedStop = true;
        this.mStateMachine.quitNow();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("--Dump of ScanOnlyModeManager--");
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

    private void updateWifiState(int i) {
        if (this.mExpectedStop) {
            Log.d(TAG, "expected stop, not triggering callbacks: state = " + i);
            return;
        }
        if (i == 4 || i == 1) {
            this.mExpectedStop = true;
        }
        this.mListener.onStateChanged(i);
    }

    private class ScanOnlyModeStateMachine extends StateMachine {
        public static final int CMD_INTERFACE_DESTROYED = 4;
        public static final int CMD_INTERFACE_DOWN = 5;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_START = 0;
        private final State mIdleState;
        private final State mStartedState;
        private final WifiNative.InterfaceCallback mWifiNativeInterfaceCallback;

        ScanOnlyModeStateMachine(Looper looper) {
            super(ScanOnlyModeManager.TAG, looper);
            this.mIdleState = new IdleState();
            this.mStartedState = new StartedState();
            this.mWifiNativeInterfaceCallback = new WifiNative.InterfaceCallback() {
                @Override
                public void onDestroyed(String str) {
                    if (ScanOnlyModeManager.this.mClientInterfaceName != null && ScanOnlyModeManager.this.mClientInterfaceName.equals(str)) {
                        ScanOnlyModeStateMachine.this.sendMessage(4);
                    }
                }

                @Override
                public void onUp(String str) {
                    if (ScanOnlyModeManager.this.mClientInterfaceName != null && ScanOnlyModeManager.this.mClientInterfaceName.equals(str)) {
                        ScanOnlyModeStateMachine.this.sendMessage(3, 1);
                    }
                }

                @Override
                public void onDown(String str) {
                    if (ScanOnlyModeManager.this.mClientInterfaceName != null && ScanOnlyModeManager.this.mClientInterfaceName.equals(str)) {
                        ScanOnlyModeStateMachine.this.sendMessage(3, 0);
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
                Log.d(ScanOnlyModeManager.TAG, "entering IdleState");
                ScanOnlyModeManager.this.mClientInterfaceName = null;
            }

            public boolean processMessage(Message message) {
                if (message.what == 0) {
                    ScanOnlyModeManager.this.mClientInterfaceName = ScanOnlyModeManager.this.mWifiNative.setupInterfaceForClientMode(true, ScanOnlyModeStateMachine.this.mWifiNativeInterfaceCallback);
                    if (!TextUtils.isEmpty(ScanOnlyModeManager.this.mClientInterfaceName)) {
                        ScanOnlyModeManager.this.sendScanAvailableBroadcast(false);
                        ScanOnlyModeManager.this.mScanRequestProxy.enableScanningForHiddenNetworks(false);
                        ScanOnlyModeManager.this.mScanRequestProxy.clearScanResults();
                        ScanOnlyModeStateMachine.this.transitionTo(ScanOnlyModeStateMachine.this.mStartedState);
                    } else {
                        Log.e(ScanOnlyModeManager.TAG, "Failed to create ClientInterface. Sit in Idle");
                        ScanOnlyModeManager.this.updateWifiState(4);
                    }
                    return true;
                }
                Log.d(ScanOnlyModeManager.TAG, "received an invalid message: " + message);
                return false;
            }
        }

        private class StartedState extends State {
            private StartedState() {
            }

            private void onUpChanged(boolean z) {
                if (z != ScanOnlyModeManager.this.mIfaceIsUp) {
                    ScanOnlyModeManager.this.mIfaceIsUp = z;
                    if (z) {
                        Log.d(ScanOnlyModeManager.TAG, "Wifi is ready to use for scanning");
                        ScanOnlyModeManager.this.mWakeupController.start();
                        ScanOnlyModeManager.this.sendScanAvailableBroadcast(true);
                        ScanOnlyModeManager.this.updateWifiState(3);
                        return;
                    }
                    Log.d(ScanOnlyModeManager.TAG, "interface down - stop scan mode");
                    ScanOnlyModeManager.this.mStateMachine.sendMessage(5);
                }
            }

            public void enter() {
                Log.d(ScanOnlyModeManager.TAG, "entering StartedState");
                ScanOnlyModeManager.this.mScanRequestProxy.enableScanningForHiddenNetworks(false);
                ScanOnlyModeManager.this.mIfaceIsUp = false;
                onUpChanged(ScanOnlyModeManager.this.mWifiNative.isInterfaceUp(ScanOnlyModeManager.this.mClientInterfaceName));
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 0) {
                    switch (i) {
                        case 3:
                            onUpChanged(message.arg1 == 1);
                            break;
                        case 4:
                            Log.d(ScanOnlyModeManager.TAG, "Interface cleanly destroyed, report scan mode stop.");
                            ScanOnlyModeManager.this.mClientInterfaceName = null;
                            ScanOnlyModeStateMachine.this.transitionTo(ScanOnlyModeStateMachine.this.mIdleState);
                            break;
                        case 5:
                            Log.d(ScanOnlyModeManager.TAG, "interface down!  restart scan mode");
                            WifiInjector.getInstance().getSelfRecovery().trigger(2);
                            ScanOnlyModeStateMachine.this.transitionTo(ScanOnlyModeStateMachine.this.mIdleState);
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }

            public void exit() {
                ScanOnlyModeManager.this.mWakeupController.stop();
                if (ScanOnlyModeManager.this.mClientInterfaceName != null) {
                    ScanOnlyModeManager.this.mWifiNative.teardownInterface(ScanOnlyModeManager.this.mClientInterfaceName);
                    ScanOnlyModeManager.this.mClientInterfaceName = null;
                }
                ScanOnlyModeManager.this.updateWifiState(1);
                ScanOnlyModeManager.this.mStateMachine.quitNow();
            }
        }
    }

    private void sendScanAvailableBroadcast(boolean z) {
        sendScanAvailableBroadcast(this.mContext, z);
    }
}
