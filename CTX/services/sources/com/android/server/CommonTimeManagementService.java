package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.INetworkManagementEventObserver;
import android.net.InterfaceConfiguration;
import android.os.Binder;
import android.os.CommonTimeConfig;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.util.DumpUtils;
import com.android.server.UiModeManagerService;
import com.android.server.net.BaseNetworkObserver;
import java.io.FileDescriptor;
import java.io.PrintWriter;

class CommonTimeManagementService extends Binder {
    private static final boolean ALLOW_WIFI;
    private static final String ALLOW_WIFI_PROP = "ro.common_time.allow_wifi";
    private static final boolean AUTO_DISABLE;
    private static final String AUTO_DISABLE_PROP = "ro.common_time.auto_disable";
    private static final byte BASE_SERVER_PRIO;
    private static final InterfaceScoreRule[] IFACE_SCORE_RULES;
    private static final int NATIVE_SERVICE_RECONNECT_TIMEOUT = 5000;
    private static final int NO_INTERFACE_TIMEOUT;
    private static final String NO_INTERFACE_TIMEOUT_PROP = "ro.common_time.no_iface_timeout";
    private static final String SERVER_PRIO_PROP = "ro.common_time.server_prio";
    private static final String TAG = CommonTimeManagementService.class.getSimpleName();
    private CommonTimeConfig mCTConfig;
    private final Context mContext;
    private String mCurIface;
    private INetworkManagementService mNetMgr;
    private final Object mLock = new Object();
    private Handler mReconnectHandler = new Handler();
    private Handler mNoInterfaceHandler = new Handler();
    private boolean mDetectedAtStartup = false;
    private byte mEffectivePrio = BASE_SERVER_PRIO;
    private INetworkManagementEventObserver mIfaceObserver = new BaseNetworkObserver() {
        public void interfaceStatusChanged(String str, boolean z) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }

        public void interfaceLinkStateChanged(String str, boolean z) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }

        public void interfaceAdded(String str) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }

        public void interfaceRemoved(String str) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }
    };
    private BroadcastReceiver mConnectivityMangerObserver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }
    };
    private CommonTimeConfig.OnServerDiedListener mCTServerDiedListener = new CommonTimeConfig.OnServerDiedListener() {
        public final void onServerDied() {
            this.f$0.scheduleTimeConfigReconnect();
        }
    };
    private Runnable mReconnectRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.connectToTimeConfig();
        }
    };
    private Runnable mNoInterfaceRunnable = new Runnable() {
        @Override
        public final void run() {
            this.f$0.handleNoInterfaceTimeout();
        }
    };

    static {
        AUTO_DISABLE = SystemProperties.getInt(AUTO_DISABLE_PROP, 1) != 0;
        ALLOW_WIFI = SystemProperties.getInt(ALLOW_WIFI_PROP, 0) != 0;
        int i = SystemProperties.getInt(SERVER_PRIO_PROP, 1);
        NO_INTERFACE_TIMEOUT = SystemProperties.getInt(NO_INTERFACE_TIMEOUT_PROP, 60000);
        if (i < 1) {
            BASE_SERVER_PRIO = (byte) 1;
        } else if (i > 30) {
            BASE_SERVER_PRIO = (byte) 30;
        } else {
            BASE_SERVER_PRIO = (byte) i;
        }
        if (ALLOW_WIFI) {
            IFACE_SCORE_RULES = new InterfaceScoreRule[]{new InterfaceScoreRule("wlan", (byte) 1), new InterfaceScoreRule("eth", (byte) 2)};
        } else {
            IFACE_SCORE_RULES = new InterfaceScoreRule[]{new InterfaceScoreRule("eth", (byte) 2)};
        }
    }

    public CommonTimeManagementService(Context context) {
        this.mContext = context;
    }

    void systemRunning() {
        if (ServiceManager.checkService("common_time.config") == null) {
            Log.i(TAG, "No common time service detected on this platform.  Common time services will be unavailable.");
            return;
        }
        this.mDetectedAtStartup = true;
        this.mNetMgr = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        try {
            this.mNetMgr.registerObserver(this.mIfaceObserver);
        } catch (RemoteException e) {
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mConnectivityMangerObserver, intentFilter);
        connectToTimeConfig();
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            if (!this.mDetectedAtStartup) {
                printWriter.println("Native Common Time service was not detected at startup.  Service is unavailable");
                return;
            }
            synchronized (this.mLock) {
                printWriter.println("Current Common Time Management Service Config:");
                Object[] objArr = new Object[1];
                objArr[0] = this.mCTConfig == null ? "reconnecting" : "alive";
                printWriter.println(String.format("  Native service     : %s", objArr));
                Object[] objArr2 = new Object[1];
                objArr2[0] = this.mCurIface == null ? "unbound" : this.mCurIface;
                printWriter.println(String.format("  Bound interface    : %s", objArr2));
                Object[] objArr3 = new Object[1];
                objArr3[0] = ALLOW_WIFI ? UiModeManagerService.Shell.NIGHT_MODE_STR_YES : UiModeManagerService.Shell.NIGHT_MODE_STR_NO;
                printWriter.println(String.format("  Allow WiFi         : %s", objArr3));
                Object[] objArr4 = new Object[1];
                objArr4[0] = AUTO_DISABLE ? UiModeManagerService.Shell.NIGHT_MODE_STR_YES : UiModeManagerService.Shell.NIGHT_MODE_STR_NO;
                printWriter.println(String.format("  Allow Auto Disable : %s", objArr4));
                printWriter.println(String.format("  Server Priority    : %d", Byte.valueOf(this.mEffectivePrio)));
                printWriter.println(String.format("  No iface timeout   : %d", Integer.valueOf(NO_INTERFACE_TIMEOUT)));
            }
        }
    }

    private static class InterfaceScoreRule {
        public final String mPrefix;
        public final byte mScore;

        public InterfaceScoreRule(String str, byte b) {
            this.mPrefix = str;
            this.mScore = b;
        }
    }

    private void cleanupTimeConfig() {
        this.mReconnectHandler.removeCallbacks(this.mReconnectRunnable);
        this.mNoInterfaceHandler.removeCallbacks(this.mNoInterfaceRunnable);
        if (this.mCTConfig != null) {
            this.mCTConfig.release();
            this.mCTConfig = null;
        }
    }

    private void connectToTimeConfig() {
        cleanupTimeConfig();
        try {
            synchronized (this.mLock) {
                this.mCTConfig = new CommonTimeConfig();
                this.mCTConfig.setServerDiedListener(this.mCTServerDiedListener);
                this.mCurIface = this.mCTConfig.getInterfaceBinding();
                this.mCTConfig.setAutoDisable(AUTO_DISABLE);
                this.mCTConfig.setMasterElectionPriority(this.mEffectivePrio);
            }
            if (NO_INTERFACE_TIMEOUT >= 0) {
                this.mNoInterfaceHandler.postDelayed(this.mNoInterfaceRunnable, NO_INTERFACE_TIMEOUT);
            }
            reevaluateServiceState();
        } catch (RemoteException e) {
            scheduleTimeConfigReconnect();
        }
    }

    private void scheduleTimeConfigReconnect() {
        cleanupTimeConfig();
        Log.w(TAG, String.format("Native service died, will reconnect in %d mSec", Integer.valueOf(NATIVE_SERVICE_RECONNECT_TIMEOUT)));
        this.mReconnectHandler.postDelayed(this.mReconnectRunnable, 5000L);
    }

    private void handleNoInterfaceTimeout() {
        if (this.mCTConfig != null) {
            Log.i(TAG, "Timeout waiting for interface to come up.  Forcing networkless master mode.");
            if (-7 == this.mCTConfig.forceNetworklessMasterMode()) {
                scheduleTimeConfigReconnect();
            }
        }
    }

    private void reevaluateServiceState() {
        String str;
        byte b;
        InterfaceConfiguration interfaceConfig;
        byte b2 = -1;
        boolean z = false;
        try {
            String[] strArrListInterfaces = this.mNetMgr.listInterfaces();
            if (strArrListInterfaces != null) {
                byte b3 = -1;
                str = null;
                for (String str2 : strArrListInterfaces) {
                    try {
                        InterfaceScoreRule[] interfaceScoreRuleArr = IFACE_SCORE_RULES;
                        int length = interfaceScoreRuleArr.length;
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                b = -1;
                                break;
                            }
                            InterfaceScoreRule interfaceScoreRule = interfaceScoreRuleArr[i];
                            if (str2.contains(interfaceScoreRule.mPrefix)) {
                                b = interfaceScoreRule.mScore;
                                break;
                            }
                            i++;
                        }
                        if (b > b3 && (interfaceConfig = this.mNetMgr.getInterfaceConfig(str2)) != null && interfaceConfig.isActive()) {
                            str = str2;
                            b3 = b;
                        }
                    } catch (RemoteException e) {
                        b2 = b3;
                        str = null;
                        synchronized (this.mLock) {
                        }
                    }
                }
                b2 = b3;
            } else {
                str = null;
            }
        } catch (RemoteException e2) {
        }
        synchronized (this.mLock) {
            if (str != null) {
                try {
                    if (this.mCurIface == null) {
                        Log.e(TAG, String.format("Binding common time service to %s.", str));
                        this.mCurIface = str;
                    } else if (str == null && this.mCurIface != null) {
                        Log.e(TAG, "Unbinding common time service.");
                        this.mCurIface = null;
                    } else if (str != null && this.mCurIface != null && !str.equals(this.mCurIface)) {
                        Log.e(TAG, String.format("Switching common time service binding from %s to %s.", this.mCurIface, str));
                        this.mCurIface = str;
                    }
                    z = true;
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
        if (!z || this.mCTConfig == null) {
            return;
        }
        byte b4 = b2 > 0 ? (byte) (b2 * BASE_SERVER_PRIO) : BASE_SERVER_PRIO;
        if (b4 != this.mEffectivePrio) {
            this.mEffectivePrio = b4;
            this.mCTConfig.setMasterElectionPriority(this.mEffectivePrio);
        }
        if (this.mCTConfig.setNetworkBinding(this.mCurIface) != 0) {
            scheduleTimeConfigReconnect();
        } else if (NO_INTERFACE_TIMEOUT >= 0) {
            this.mNoInterfaceHandler.removeCallbacks(this.mNoInterfaceRunnable);
            if (this.mCurIface == null) {
                this.mNoInterfaceHandler.postDelayed(this.mNoInterfaceRunnable, NO_INTERFACE_TIMEOUT);
            }
        }
    }
}
