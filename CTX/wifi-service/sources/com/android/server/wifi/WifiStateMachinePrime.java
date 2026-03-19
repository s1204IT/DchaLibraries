package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.app.IBatteryStats;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.ClientModeManager;
import com.android.server.wifi.ScanOnlyModeManager;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachinePrime;
import com.mediatek.server.wifi.MtkScanModeNotifier;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;

public class WifiStateMachinePrime {
    static final int BASE = 131072;
    static final int CMD_AP_STOPPED = 131096;
    static final int CMD_CLIENT_MODE_FAILED = 131376;
    static final int CMD_CLIENT_MODE_STOPPED = 131375;
    static final int CMD_SCAN_ONLY_MODE_FAILED = 131276;
    static final int CMD_SCAN_ONLY_MODE_STOPPED = 131275;
    static final int CMD_START_AP = 131093;
    static final int CMD_START_AP_FAILURE = 131094;
    static final int CMD_START_CLIENT_MODE = 131372;
    static final int CMD_START_CLIENT_MODE_FAILURE = 131373;
    static final int CMD_START_SCAN_ONLY_MODE = 131272;
    static final int CMD_START_SCAN_ONLY_MODE_FAILURE = 131273;
    static final int CMD_STOP_AP = 131095;
    static final int CMD_STOP_CLIENT_MODE = 131374;
    static final int CMD_STOP_SCAN_ONLY_MODE = 131274;
    private static final String TAG = "WifiStateMachinePrime";
    private final IBatteryStats mBatteryStats;
    private ClientModeManager.Listener mClientModeCallback;
    private final Context mContext;
    private DefaultModeManager mDefaultModeManager;
    private final Handler mHandler;
    private final Looper mLooper;
    private ScanOnlyModeManager.Listener mScanOnlyCallback;
    private final ScanRequestProxy mScanRequestProxy;
    private final SelfRecovery mSelfRecovery;
    private WifiManager.SoftApCallback mSoftApCallback;
    private BaseWifiDiagnostics mWifiDiagnostics;
    private final WifiInjector mWifiInjector;
    private final WifiNative mWifiNative;
    private final ArraySet<ActiveModeManager> mActiveModeManagers = new ArraySet<>();
    private ModeStateMachine mModeStateMachine = new ModeStateMachine();
    private WifiNative.StatusListener mWifiNativeStatusListener = new WifiNativeStatusListener();

    public void registerSoftApCallback(WifiManager.SoftApCallback softApCallback) {
        this.mSoftApCallback = softApCallback;
    }

    public void registerScanOnlyCallback(ScanOnlyModeManager.Listener listener) {
        this.mScanOnlyCallback = listener;
    }

    public void registerClientModeCallback(ClientModeManager.Listener listener) {
        this.mClientModeCallback = listener;
    }

    protected WifiStateMachinePrime(WifiInjector wifiInjector, Context context, Looper looper, WifiNative wifiNative, DefaultModeManager defaultModeManager, IBatteryStats iBatteryStats) {
        this.mWifiInjector = wifiInjector;
        this.mContext = context;
        this.mLooper = looper;
        this.mHandler = new Handler(looper);
        this.mWifiNative = wifiNative;
        this.mDefaultModeManager = defaultModeManager;
        this.mBatteryStats = iBatteryStats;
        this.mSelfRecovery = this.mWifiInjector.getSelfRecovery();
        this.mWifiDiagnostics = this.mWifiInjector.getWifiDiagnostics();
        this.mScanRequestProxy = this.mWifiInjector.getScanRequestProxy();
        this.mWifiNative.registerStatusListener(this.mWifiNativeStatusListener);
    }

    public void enterClientMode() {
        changeMode(0);
    }

    public void enterScanOnlyMode() {
        changeMode(1);
    }

    public void enterSoftAPMode(final SoftApModeConfiguration softApModeConfiguration) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.startSoftAp(softApModeConfiguration);
            }
        });
    }

    public void stopSoftAPMode() {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                WifiStateMachinePrime.lambda$stopSoftAPMode$1(this.f$0);
            }
        });
    }

    public static void lambda$stopSoftAPMode$1(WifiStateMachinePrime wifiStateMachinePrime) {
        for (ActiveModeManager activeModeManager : wifiStateMachinePrime.mActiveModeManagers) {
            if (activeModeManager instanceof SoftApManager) {
                Log.d(TAG, "Stopping SoftApModeManager");
                activeModeManager.stop();
            }
        }
        wifiStateMachinePrime.updateBatteryStatsWifiState(false);
    }

    public void disableWifi() {
        changeMode(3);
    }

    public void shutdownWifi() {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                WifiStateMachinePrime.lambda$shutdownWifi$2(this.f$0);
            }
        });
    }

    public static void lambda$shutdownWifi$2(WifiStateMachinePrime wifiStateMachinePrime) {
        Iterator<ActiveModeManager> it = wifiStateMachinePrime.mActiveModeManagers.iterator();
        while (it.hasNext()) {
            it.next().stop();
        }
        wifiStateMachinePrime.updateBatteryStatsWifiState(false);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Dump of WifiStateMachinePrime");
        printWriter.println("Current wifi mode: " + getCurrentMode());
        printWriter.println("NumActiveModeManagers: " + this.mActiveModeManagers.size());
        Iterator<ActiveModeManager> it = this.mActiveModeManagers.iterator();
        while (it.hasNext()) {
            it.next().dump(fileDescriptor, printWriter, strArr);
        }
    }

    protected String getCurrentMode() {
        return this.mModeStateMachine.getCurrentMode();
    }

    private void changeMode(int i) {
        this.mModeStateMachine.sendMessage(i);
    }

    private class ModeCallback {
        ActiveModeManager mActiveManager;

        private ModeCallback() {
        }

        void setActiveModeManager(ActiveModeManager activeModeManager) {
            this.mActiveManager = activeModeManager;
        }

        ActiveModeManager getActiveModeManager() {
            return this.mActiveManager;
        }
    }

    private class ModeStateMachine extends StateMachine {
        public static final int CMD_DISABLE_WIFI = 3;
        public static final int CMD_START_CLIENT_MODE = 0;
        public static final int CMD_START_SCAN_ONLY_MODE = 1;
        private final State mClientModeActiveState;
        private final State mScanOnlyModeActiveState;
        private final State mWifiDisabledState;

        ModeStateMachine() {
            super(WifiStateMachinePrime.TAG, WifiStateMachinePrime.this.mLooper);
            this.mWifiDisabledState = new WifiDisabledState();
            this.mClientModeActiveState = new ClientModeActiveState();
            this.mScanOnlyModeActiveState = new ScanOnlyModeActiveState();
            addState(this.mClientModeActiveState);
            addState(this.mScanOnlyModeActiveState);
            addState(this.mWifiDisabledState);
            Log.d(WifiStateMachinePrime.TAG, "Starting Wifi in WifiDisabledState");
            setInitialState(this.mWifiDisabledState);
            start();
        }

        private String getCurrentMode() {
            return getCurrentState().getName();
        }

        private boolean checkForAndHandleModeChange(Message message) {
            int i = message.what;
            if (i != 3) {
                switch (i) {
                    case 0:
                        Log.d(WifiStateMachinePrime.TAG, "Switching from " + getCurrentMode() + " to ClientMode");
                        WifiStateMachinePrime.this.mModeStateMachine.transitionTo(this.mClientModeActiveState);
                        return true;
                    case 1:
                        Log.d(WifiStateMachinePrime.TAG, "Switching from " + getCurrentMode() + " to ScanOnlyMode");
                        WifiStateMachinePrime.this.mModeStateMachine.transitionTo(this.mScanOnlyModeActiveState);
                        return true;
                    default:
                        return false;
                }
            }
            Log.d(WifiStateMachinePrime.TAG, "Switching from " + getCurrentMode() + " to WifiDisabled");
            WifiStateMachinePrime.this.mModeStateMachine.transitionTo(this.mWifiDisabledState);
            return true;
        }

        class ModeActiveState extends State {
            ActiveModeManager mManager;

            ModeActiveState() {
            }

            public boolean processMessage(Message message) {
                return false;
            }

            public void exit() {
                if (this.mManager != null) {
                    this.mManager.stop();
                    WifiStateMachinePrime.this.mActiveModeManagers.remove(this.mManager);
                }
                WifiStateMachinePrime.this.updateBatteryStatsWifiState(false);
            }
        }

        class WifiDisabledState extends ModeActiveState {
            WifiDisabledState() {
                super();
            }

            public void enter() {
                Log.d(WifiStateMachinePrime.TAG, "Entering WifiDisabledState");
                WifiStateMachinePrime.this.mDefaultModeManager.sendScanAvailableBroadcast(WifiStateMachinePrime.this.mContext, false);
                WifiStateMachinePrime.this.mScanRequestProxy.enableScanningForHiddenNetworks(false);
                WifiStateMachinePrime.this.mScanRequestProxy.clearScanResults();
            }

            @Override
            public boolean processMessage(Message message) {
                Log.d(WifiStateMachinePrime.TAG, "received a message in WifiDisabledState: " + message);
                if (ModeStateMachine.this.checkForAndHandleModeChange(message)) {
                    return true;
                }
                return false;
            }

            @Override
            public void exit() {
            }
        }

        class ClientModeActiveState extends ModeActiveState {
            ClientListener mListener;

            ClientModeActiveState() {
                super();
            }

            private class ClientListener implements ClientModeManager.Listener {
                private ClientListener() {
                }

                @Override
                public void onStateChanged(int i) {
                    if (this != ClientModeActiveState.this.mListener) {
                        Log.d(WifiStateMachinePrime.TAG, "Client mode state change from previous manager");
                        return;
                    }
                    Log.d(WifiStateMachinePrime.TAG, "State changed from client mode. state = " + i);
                    if (i == 4) {
                        WifiStateMachinePrime.this.mModeStateMachine.sendMessage(131376, this);
                    } else if (i == 1) {
                        WifiStateMachinePrime.this.mModeStateMachine.sendMessage(131375, this);
                    } else if (i == 3) {
                        Log.d(WifiStateMachinePrime.TAG, "client mode active");
                    }
                }
            }

            public void enter() {
                Log.d(WifiStateMachinePrime.TAG, "Entering ClientModeActiveState");
                this.mListener = new ClientListener();
                this.mManager = WifiStateMachinePrime.this.mWifiInjector.makeClientModeManager(this.mListener);
                this.mManager.start();
                WifiStateMachinePrime.this.mActiveModeManagers.add(this.mManager);
                WifiStateMachinePrime.this.updateBatteryStatsWifiState(true);
            }

            @Override
            public void exit() {
                super.exit();
                this.mListener = null;
            }

            @Override
            public boolean processMessage(Message message) {
                if (ModeStateMachine.this.checkForAndHandleModeChange(message)) {
                    return true;
                }
                int i = message.what;
                if (i == 0) {
                    Log.d(WifiStateMachinePrime.TAG, "Received CMD_START_CLIENT_MODE when active - drop");
                } else {
                    switch (i) {
                        case 131375:
                            if (this.mListener != message.obj) {
                                Log.d(WifiStateMachinePrime.TAG, "Client mode state change from previous manager");
                                return true;
                            }
                            Log.d(WifiStateMachinePrime.TAG, "ClientMode stopped, return to WifiDisabledState.");
                            WifiStateMachinePrime.this.mClientModeCallback.onStateChanged(1);
                            WifiStateMachinePrime.this.mModeStateMachine.transitionTo(ModeStateMachine.this.mWifiDisabledState);
                            break;
                            break;
                        case 131376:
                            if (this.mListener != message.obj) {
                                Log.d(WifiStateMachinePrime.TAG, "Client mode state change from previous manager");
                                return true;
                            }
                            Log.d(WifiStateMachinePrime.TAG, "ClientMode failed, return to WifiDisabledState.");
                            WifiStateMachinePrime.this.mClientModeCallback.onStateChanged(4);
                            WifiStateMachinePrime.this.mModeStateMachine.transitionTo(ModeStateMachine.this.mWifiDisabledState);
                            break;
                            break;
                        default:
                            return false;
                    }
                }
                return false;
            }
        }

        class ScanOnlyModeActiveState extends ModeActiveState {
            ScanOnlyListener mListener;

            ScanOnlyModeActiveState() {
                super();
            }

            private class ScanOnlyListener implements ScanOnlyModeManager.Listener {
                private ScanOnlyListener() {
                }

                @Override
                public void onStateChanged(int i) {
                    if (this != ScanOnlyModeActiveState.this.mListener) {
                        Log.d(WifiStateMachinePrime.TAG, "ScanOnly mode state change from previous manager");
                        return;
                    }
                    if (i == 4) {
                        Log.d(WifiStateMachinePrime.TAG, "ScanOnlyMode mode failed");
                        WifiStateMachinePrime.this.mModeStateMachine.sendMessage(WifiStateMachinePrime.CMD_SCAN_ONLY_MODE_FAILED, this);
                        return;
                    }
                    if (i == 1) {
                        Log.d(WifiStateMachinePrime.TAG, "ScanOnlyMode stopped");
                        WifiStateMachinePrime.this.mModeStateMachine.sendMessage(WifiStateMachinePrime.CMD_SCAN_ONLY_MODE_STOPPED, this);
                    } else if (i == 3) {
                        Log.d(WifiStateMachinePrime.TAG, "scan mode active");
                        MtkScanModeNotifier.setScanMode(true);
                    } else {
                        Log.d(WifiStateMachinePrime.TAG, "unexpected state update: " + i);
                    }
                }
            }

            public void enter() {
                Log.d(WifiStateMachinePrime.TAG, "Entering ScanOnlyModeActiveState");
                this.mListener = new ScanOnlyListener();
                this.mManager = WifiStateMachinePrime.this.mWifiInjector.makeScanOnlyModeManager(this.mListener);
                this.mManager.start();
                WifiStateMachinePrime.this.mActiveModeManagers.add(this.mManager);
                WifiStateMachinePrime.this.updateBatteryStatsWifiState(true);
                WifiStateMachinePrime.this.updateBatteryStatsScanModeActive();
            }

            @Override
            public void exit() {
                super.exit();
                this.mListener = null;
                MtkScanModeNotifier.setScanMode(false);
            }

            @Override
            public boolean processMessage(Message message) {
                if (ModeStateMachine.this.checkForAndHandleModeChange(message)) {
                    return true;
                }
                int i = message.what;
                if (i == 1) {
                    Log.d(WifiStateMachinePrime.TAG, "Received CMD_START_SCAN_ONLY_MODE when active - drop");
                } else {
                    switch (i) {
                        case WifiStateMachinePrime.CMD_SCAN_ONLY_MODE_STOPPED:
                            if (this.mListener != message.obj) {
                                Log.d(WifiStateMachinePrime.TAG, "ScanOnly mode state change from previous manager");
                                return true;
                            }
                            Log.d(WifiStateMachinePrime.TAG, "ScanOnlyMode stopped, return to WifiDisabledState.");
                            WifiStateMachinePrime.this.mScanOnlyCallback.onStateChanged(1);
                            WifiStateMachinePrime.this.mModeStateMachine.transitionTo(ModeStateMachine.this.mWifiDisabledState);
                            break;
                            break;
                        case WifiStateMachinePrime.CMD_SCAN_ONLY_MODE_FAILED:
                            if (this.mListener != message.obj) {
                                Log.d(WifiStateMachinePrime.TAG, "ScanOnly mode state change from previous manager");
                                return true;
                            }
                            Log.d(WifiStateMachinePrime.TAG, "ScanOnlyMode failed, return to WifiDisabledState.");
                            WifiStateMachinePrime.this.mScanOnlyCallback.onStateChanged(4);
                            WifiStateMachinePrime.this.mModeStateMachine.transitionTo(ModeStateMachine.this.mWifiDisabledState);
                            break;
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }
        }
    }

    private class SoftApCallbackImpl extends ModeCallback implements WifiManager.SoftApCallback {
        private SoftApCallbackImpl() {
            super();
        }

        public void onStateChanged(int i, int i2) {
            if (i == 11 || i == 14) {
                WifiStateMachinePrime.this.mActiveModeManagers.remove(getActiveModeManager());
                WifiStateMachinePrime.this.updateBatteryStatsWifiState(false);
            }
            if (WifiStateMachinePrime.this.mSoftApCallback != null) {
                WifiStateMachinePrime.this.mSoftApCallback.onStateChanged(i, i2);
            }
        }

        public void onNumClientsChanged(int i) {
            if (WifiStateMachinePrime.this.mSoftApCallback != null) {
                WifiStateMachinePrime.this.mSoftApCallback.onNumClientsChanged(i);
            } else {
                Log.d(WifiStateMachinePrime.TAG, "SoftApCallback is null. Dropping NumClientsChanged event.");
            }
        }
    }

    private void startSoftAp(SoftApModeConfiguration softApModeConfiguration) {
        Log.d(TAG, "Starting SoftApModeManager");
        WifiConfiguration wifiConfiguration = softApModeConfiguration.getWifiConfiguration();
        if (wifiConfiguration != null && wifiConfiguration.SSID != null) {
            Log.d(TAG, "Passing config to SoftApManager! " + wifiConfiguration);
        }
        SoftApCallbackImpl softApCallbackImpl = new SoftApCallbackImpl();
        SoftApManager softApManagerMakeSoftApManager = this.mWifiInjector.makeSoftApManager(softApCallbackImpl, softApModeConfiguration);
        softApCallbackImpl.setActiveModeManager(softApManagerMakeSoftApManager);
        softApManagerMakeSoftApManager.start();
        this.mActiveModeManagers.add(softApManagerMakeSoftApManager);
        updateBatteryStatsWifiState(true);
    }

    private void updateBatteryStatsWifiState(boolean z) {
        try {
            if (z) {
                if (this.mActiveModeManagers.size() == 1) {
                    this.mBatteryStats.noteWifiOn();
                }
            } else if (this.mActiveModeManagers.size() == 0) {
                this.mBatteryStats.noteWifiOff();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to note battery stats in wifi");
        }
    }

    private void updateBatteryStatsScanModeActive() {
        try {
            this.mBatteryStats.noteWifiState(1, (String) null);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to note battery stats in wifi");
        }
    }

    private final class WifiNativeStatusListener implements WifiNative.StatusListener {
        private WifiNativeStatusListener() {
        }

        @Override
        public void onStatusChanged(boolean z) {
            if (!z) {
                WifiStateMachinePrime.this.mHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        WifiStateMachinePrime.WifiNativeStatusListener.lambda$onStatusChanged$0(this.f$0);
                    }
                });
            }
        }

        public static void lambda$onStatusChanged$0(WifiNativeStatusListener wifiNativeStatusListener) {
            Log.e(WifiStateMachinePrime.TAG, "One of the native daemons died. Triggering recovery");
            WifiStateMachinePrime.this.mWifiDiagnostics.captureBugReportData(8);
            WifiStateMachinePrime.this.mWifiInjector.getSelfRecovery().trigger(1);
        }
    }
}
