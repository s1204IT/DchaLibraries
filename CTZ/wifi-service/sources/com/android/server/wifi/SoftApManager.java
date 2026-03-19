package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.util.ApConfigUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

public class SoftApManager implements ActiveModeManager {
    private static final int MIN_SOFT_AP_TIMEOUT_DELAY_MS = 600000;

    @VisibleForTesting
    public static final String SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG = "SoftApManager Soft AP Send Message Timeout";
    private static final String TAG = "SoftApManager";
    private WifiConfiguration mApConfig;
    private String mApInterfaceName;
    private final WifiManager.SoftApCallback mCallback;
    private final Context mContext;
    private final String mCountryCode;
    private final FrameworkFacade mFrameworkFacade;
    private boolean mIfaceIsUp;
    private final int mMode;
    private final SoftApStateMachine mStateMachine;
    private final WifiApConfigStore mWifiApConfigStore;
    private final WifiMetrics mWifiMetrics;
    private final WifiNative mWifiNative;
    private int mReportedFrequency = -1;
    private int mReportedBandwidth = -1;
    private int mNumAssociatedStations = 0;
    private boolean mTimeoutEnabled = false;
    private final WifiNative.SoftApListener mSoftApListener = new WifiNative.SoftApListener() {
        @Override
        public void onNumAssociatedStationsChanged(int i) {
            SoftApManager.this.mStateMachine.sendMessage(4, i);
        }

        @Override
        public void onSoftApChannelSwitched(int i, int i2) {
            SoftApManager.this.mStateMachine.sendMessage(9, i, i2);
        }
    };

    public SoftApManager(Context context, Looper looper, FrameworkFacade frameworkFacade, WifiNative wifiNative, String str, WifiManager.SoftApCallback softApCallback, WifiApConfigStore wifiApConfigStore, SoftApModeConfiguration softApModeConfiguration, WifiMetrics wifiMetrics) {
        this.mContext = context;
        this.mFrameworkFacade = frameworkFacade;
        this.mWifiNative = wifiNative;
        this.mCountryCode = str;
        this.mCallback = softApCallback;
        this.mWifiApConfigStore = wifiApConfigStore;
        this.mMode = softApModeConfiguration.getTargetMode();
        WifiConfiguration wifiConfiguration = softApModeConfiguration.getWifiConfiguration();
        if (wifiConfiguration == null) {
            this.mApConfig = this.mWifiApConfigStore.getApConfiguration();
        } else {
            this.mApConfig = wifiConfiguration;
        }
        this.mWifiMetrics = wifiMetrics;
        this.mStateMachine = new SoftApStateMachine(looper);
    }

    @Override
    public void start() {
        this.mStateMachine.sendMessage(0, this.mApConfig);
    }

    @Override
    public void stop() {
        Log.d(TAG, " currentstate: " + getCurrentStateName());
        if (this.mApInterfaceName != null) {
            if (this.mIfaceIsUp) {
                updateApState(10, 13, 0);
            } else {
                updateApState(10, 12, 0);
            }
        }
        this.mStateMachine.quitNow();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("--Dump of SoftApManager--");
        printWriter.println("current StateMachine mode: " + getCurrentStateName());
        printWriter.println("mApInterfaceName: " + this.mApInterfaceName);
        printWriter.println("mIfaceIsUp: " + this.mIfaceIsUp);
        printWriter.println("mMode: " + this.mMode);
        printWriter.println("mCountryCode: " + this.mCountryCode);
        if (this.mApConfig != null) {
            printWriter.println("mApConfig.SSID: " + this.mApConfig.SSID);
            printWriter.println("mApConfig.apBand: " + this.mApConfig.apBand);
            printWriter.println("mApConfig.hiddenSSID: " + this.mApConfig.hiddenSSID);
        } else {
            printWriter.println("mApConfig: null");
        }
        printWriter.println("mNumAssociatedStations: " + this.mNumAssociatedStations);
        printWriter.println("mTimeoutEnabled: " + this.mTimeoutEnabled);
        printWriter.println("mReportedFrequency: " + this.mReportedFrequency);
        printWriter.println("mReportedBandwidth: " + this.mReportedBandwidth);
    }

    private String getCurrentStateName() {
        IState currentState = this.mStateMachine.getCurrentState();
        if (currentState != null) {
            return currentState.getName();
        }
        return "StateMachine not active";
    }

    private void updateApState(int i, int i2, int i3) {
        this.mCallback.onStateChanged(i, i3);
        Intent intent = new Intent("android.net.wifi.WIFI_AP_STATE_CHANGED");
        intent.addFlags(67108864);
        intent.putExtra("wifi_state", i);
        intent.putExtra("previous_wifi_state", i2);
        if (i == 14) {
            intent.putExtra("wifi_ap_error_code", i3);
        }
        intent.putExtra("wifi_ap_interface_name", this.mApInterfaceName);
        intent.putExtra("wifi_ap_mode", this.mMode);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private int startSoftAp(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration == null || wifiConfiguration.SSID == null) {
            Log.e(TAG, "Unable to start soft AP without valid configuration");
            return 2;
        }
        WifiConfiguration wifiConfiguration2 = new WifiConfiguration(wifiConfiguration);
        int iUpdateApChannelConfig = ApConfigUtil.updateApChannelConfig(this.mWifiNative, this.mCountryCode, this.mWifiApConfigStore.getAllowed2GChannel(), wifiConfiguration2);
        if (iUpdateApChannelConfig != 0) {
            Log.e(TAG, "Failed to update AP band and channel");
            return iUpdateApChannelConfig;
        }
        if (this.mCountryCode != null && !this.mWifiNative.setCountryCodeHal(this.mApInterfaceName, this.mCountryCode.toUpperCase(Locale.ROOT)) && wifiConfiguration.apBand == 1) {
            Log.e(TAG, "Failed to set country code, required for setting up soft ap in 5GHz");
            return 2;
        }
        if (wifiConfiguration2.hiddenSSID) {
            Log.d(TAG, "SoftAP is a hidden network");
        }
        if (!this.mWifiNative.startSoftAp(this.mApInterfaceName, wifiConfiguration2, this.mSoftApListener)) {
            Log.e(TAG, "Soft AP start failed");
            return 2;
        }
        Log.d(TAG, "Soft AP is started");
        return 0;
    }

    private void stopSoftAp() {
        this.mWifiNative.teardownInterface(this.mApInterfaceName);
        Log.d(TAG, "Soft AP is stopped");
    }

    private class SoftApStateMachine extends StateMachine {
        public static final int CMD_INTERFACE_DESTROYED = 7;
        public static final int CMD_INTERFACE_DOWN = 8;
        public static final int CMD_INTERFACE_STATUS_CHANGED = 3;
        public static final int CMD_NO_ASSOCIATED_STATIONS_TIMEOUT = 5;
        public static final int CMD_NUM_ASSOCIATED_STATIONS_CHANGED = 4;
        public static final int CMD_SOFT_AP_CHANNEL_SWITCHED = 9;
        public static final int CMD_START = 0;
        public static final int CMD_TIMEOUT_TOGGLE_CHANGED = 6;
        private final State mIdleState;
        private final State mStartedState;
        private final WifiNative.InterfaceCallback mWifiNativeInterfaceCallback;

        SoftApStateMachine(Looper looper) {
            super(SoftApManager.TAG, looper);
            this.mIdleState = new IdleState();
            this.mStartedState = new StartedState();
            this.mWifiNativeInterfaceCallback = new WifiNative.InterfaceCallback() {
                @Override
                public void onDestroyed(String str) {
                    if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(str)) {
                        SoftApStateMachine.this.sendMessage(7);
                    }
                }

                @Override
                public void onUp(String str) {
                    if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(str)) {
                        SoftApStateMachine.this.sendMessage(3, 1);
                    }
                }

                @Override
                public void onDown(String str) {
                    if (SoftApManager.this.mApInterfaceName != null && SoftApManager.this.mApInterfaceName.equals(str)) {
                        SoftApStateMachine.this.sendMessage(3, 0);
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
                SoftApManager.this.mApInterfaceName = null;
                SoftApManager.this.mIfaceIsUp = false;
            }

            public boolean processMessage(Message message) {
                if (message.what == 0) {
                    SoftApManager.this.mApInterfaceName = SoftApManager.this.mWifiNative.setupInterfaceForSoftApMode(SoftApStateMachine.this.mWifiNativeInterfaceCallback);
                    if (!TextUtils.isEmpty(SoftApManager.this.mApInterfaceName)) {
                        SoftApManager.this.updateApState(12, 11, 0);
                        int iStartSoftAp = SoftApManager.this.startSoftAp((WifiConfiguration) message.obj);
                        if (iStartSoftAp == 0) {
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mStartedState);
                        } else {
                            int i = iStartSoftAp == 1 ? 1 : 0;
                            SoftApManager.this.updateApState(14, 12, i);
                            SoftApManager.this.stopSoftAp();
                            SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, i);
                        }
                    } else {
                        Log.e(SoftApManager.TAG, "setup failure when creating ap interface.");
                        SoftApManager.this.updateApState(14, 11, 0);
                        SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(false, 0);
                    }
                }
                return true;
            }
        }

        private class StartedState extends State {
            private SoftApTimeoutEnabledSettingObserver mSettingObserver;
            private WakeupMessage mSoftApTimeoutMessage;
            private int mTimeoutDelay;

            private StartedState() {
            }

            private class SoftApTimeoutEnabledSettingObserver extends ContentObserver {
                SoftApTimeoutEnabledSettingObserver(Handler handler) {
                    super(handler);
                }

                public void register() {
                    SoftApManager.this.mFrameworkFacade.registerContentObserver(SoftApManager.this.mContext, Settings.Global.getUriFor("soft_ap_timeout_enabled"), true, this);
                    SoftApManager.this.mTimeoutEnabled = getValue();
                }

                public void unregister() {
                    SoftApManager.this.mFrameworkFacade.unregisterContentObserver(SoftApManager.this.mContext, this);
                }

                @Override
                public void onChange(boolean z) {
                    super.onChange(z);
                    SoftApManager.this.mStateMachine.sendMessage(6, getValue() ? 1 : 0);
                }

                private boolean getValue() {
                    return SoftApManager.this.mFrameworkFacade.getIntegerSetting(SoftApManager.this.mContext, "soft_ap_timeout_enabled", 1) == 1;
                }
            }

            private int getConfigSoftApTimeoutDelay() {
                int integer = SoftApManager.this.mContext.getResources().getInteger(R.integer.config_longPressOnPowerDurationMs);
                if (integer < SoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS) {
                    Log.w(SoftApManager.TAG, "Overriding timeout delay with minimum limit value");
                    integer = SoftApManager.MIN_SOFT_AP_TIMEOUT_DELAY_MS;
                }
                Log.d(SoftApManager.TAG, "Timeout delay: " + integer);
                return integer;
            }

            private void scheduleTimeoutMessage() {
                if (!SoftApManager.this.mTimeoutEnabled) {
                    return;
                }
                this.mSoftApTimeoutMessage.schedule(SystemClock.elapsedRealtime() + ((long) this.mTimeoutDelay));
                Log.d(SoftApManager.TAG, "Timeout message scheduled");
            }

            private void cancelTimeoutMessage() {
                this.mSoftApTimeoutMessage.cancel();
                Log.d(SoftApManager.TAG, "Timeout message canceled");
            }

            private void setNumAssociatedStations(int i) {
                if (SoftApManager.this.mNumAssociatedStations != i) {
                    SoftApManager.this.mNumAssociatedStations = i;
                    Log.d(SoftApManager.TAG, "Number of associated stations changed: " + SoftApManager.this.mNumAssociatedStations);
                    if (SoftApManager.this.mCallback != null) {
                        SoftApManager.this.mCallback.onNumClientsChanged(SoftApManager.this.mNumAssociatedStations);
                    } else {
                        Log.e(SoftApManager.TAG, "SoftApCallback is null. Dropping NumClientsChanged event.");
                    }
                    SoftApManager.this.mWifiMetrics.addSoftApNumAssociatedStationsChangedEvent(SoftApManager.this.mNumAssociatedStations, SoftApManager.this.mMode);
                    if (SoftApManager.this.mNumAssociatedStations == 0) {
                        scheduleTimeoutMessage();
                    } else {
                        cancelTimeoutMessage();
                    }
                }
            }

            private void onUpChanged(boolean z) {
                if (z != SoftApManager.this.mIfaceIsUp) {
                    SoftApManager.this.mIfaceIsUp = z;
                    if (z) {
                        Log.d(SoftApManager.TAG, "SoftAp is ready for use");
                        SoftApManager.this.updateApState(13, 12, 0);
                        SoftApManager.this.mWifiMetrics.incrementSoftApStartResult(true, 0);
                        if (SoftApManager.this.mCallback != null) {
                            SoftApManager.this.mCallback.onNumClientsChanged(SoftApManager.this.mNumAssociatedStations);
                        }
                    } else {
                        SoftApStateMachine.this.sendMessage(8);
                    }
                    SoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(z, SoftApManager.this.mMode);
                }
            }

            public void enter() {
                SoftApManager.this.mIfaceIsUp = false;
                onUpChanged(SoftApManager.this.mWifiNative.isInterfaceUp(SoftApManager.this.mApInterfaceName));
                this.mTimeoutDelay = getConfigSoftApTimeoutDelay();
                Handler handler = SoftApManager.this.mStateMachine.getHandler();
                this.mSoftApTimeoutMessage = new WakeupMessage(SoftApManager.this.mContext, handler, SoftApManager.SOFT_AP_SEND_MESSAGE_TIMEOUT_TAG, 5);
                this.mSettingObserver = new SoftApTimeoutEnabledSettingObserver(handler);
                if (this.mSettingObserver != null) {
                    this.mSettingObserver.register();
                }
                Log.d(SoftApManager.TAG, "Resetting num stations on start");
                SoftApManager.this.mNumAssociatedStations = 0;
                scheduleTimeoutMessage();
            }

            public void exit() {
                if (SoftApManager.this.mApInterfaceName != null) {
                    SoftApManager.this.stopSoftAp();
                }
                if (this.mSettingObserver != null) {
                    this.mSettingObserver.unregister();
                }
                Log.d(SoftApManager.TAG, "Resetting num stations on stop");
                SoftApManager.this.mNumAssociatedStations = 0;
                cancelTimeoutMessage();
                SoftApManager.this.mWifiMetrics.addSoftApUpChangedEvent(false, SoftApManager.this.mMode);
                SoftApManager.this.updateApState(11, 10, 0);
                SoftApManager.this.mApInterfaceName = null;
                SoftApManager.this.mIfaceIsUp = false;
                SoftApManager.this.mStateMachine.quitNow();
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 0) {
                    switch (i) {
                        case 3:
                            onUpChanged(message.arg1 == 1);
                            break;
                        case 4:
                            if (message.arg1 < 0) {
                                Log.e(SoftApManager.TAG, "Invalid number of associated stations: " + message.arg1);
                            } else {
                                Log.d(SoftApManager.TAG, "Setting num stations on CMD_NUM_ASSOCIATED_STATIONS_CHANGED");
                                setNumAssociatedStations(message.arg1);
                            }
                            break;
                        case 5:
                            if (SoftApManager.this.mTimeoutEnabled) {
                                if (SoftApManager.this.mNumAssociatedStations != 0) {
                                    Log.wtf(SoftApManager.TAG, "Timeout message received but has clients. Dropping.");
                                } else {
                                    Log.i(SoftApManager.TAG, "Timeout message received. Stopping soft AP.");
                                    SoftApManager.this.updateApState(10, 13, 0);
                                    SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                                }
                            } else {
                                Log.wtf(SoftApManager.TAG, "Timeout message received while timeout is disabled. Dropping.");
                            }
                            break;
                        case 6:
                            boolean z = message.arg1 == 1;
                            if (SoftApManager.this.mTimeoutEnabled != z) {
                                SoftApManager.this.mTimeoutEnabled = z;
                                if (!SoftApManager.this.mTimeoutEnabled) {
                                    cancelTimeoutMessage();
                                }
                                if (SoftApManager.this.mTimeoutEnabled && SoftApManager.this.mNumAssociatedStations == 0) {
                                    scheduleTimeoutMessage();
                                }
                            }
                            break;
                        case 7:
                            Log.d(SoftApManager.TAG, "Interface was cleanly destroyed.");
                            SoftApManager.this.updateApState(10, 13, 0);
                            SoftApManager.this.mApInterfaceName = null;
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                            break;
                        case 8:
                            Log.w(SoftApManager.TAG, "interface error, stop and report failure");
                            SoftApManager.this.updateApState(14, 13, 0);
                            SoftApManager.this.updateApState(10, 14, 0);
                            SoftApStateMachine.this.transitionTo(SoftApStateMachine.this.mIdleState);
                            break;
                        case 9:
                            SoftApManager.this.mReportedFrequency = message.arg1;
                            SoftApManager.this.mReportedBandwidth = message.arg2;
                            Log.d(SoftApManager.TAG, "Channel switched. Frequency: " + SoftApManager.this.mReportedFrequency + " Bandwidth: " + SoftApManager.this.mReportedBandwidth);
                            SoftApManager.this.mWifiMetrics.addSoftApChannelSwitchedEvent(SoftApManager.this.mReportedFrequency, SoftApManager.this.mReportedBandwidth, SoftApManager.this.mMode);
                            int[] array = new int[0];
                            if (SoftApManager.this.mApConfig.apBand == 0) {
                                array = SoftApManager.this.mWifiNative.getChannelsForBand(1);
                            } else if (SoftApManager.this.mApConfig.apBand == 1) {
                                array = SoftApManager.this.mWifiNative.getChannelsForBand(2);
                            } else if (SoftApManager.this.mApConfig.apBand == -1) {
                                array = Stream.concat(Arrays.stream(SoftApManager.this.mWifiNative.getChannelsForBand(1)).boxed(), Arrays.stream(SoftApManager.this.mWifiNative.getChannelsForBand(2)).boxed()).mapToInt(new ToIntFunction() {
                                    @Override
                                    public final int applyAsInt(Object obj) {
                                        return Integer.valueOf(((Integer) obj).intValue()).intValue();
                                    }
                                }).toArray();
                            }
                            if (!ArrayUtils.contains(array, SoftApManager.this.mReportedFrequency)) {
                                Log.e(SoftApManager.TAG, "Channel does not satisfy user band preference: " + SoftApManager.this.mReportedFrequency);
                                SoftApManager.this.mWifiMetrics.incrementNumSoftApUserBandPreferenceUnsatisfied();
                            }
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }
        }
    }
}
