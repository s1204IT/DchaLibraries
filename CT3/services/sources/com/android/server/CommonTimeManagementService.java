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
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.net.BaseNetworkObserver;
import com.mediatek.datashaping.DataShapingUtils;
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
    private Handler mReconnectHandler = new Handler();
    private Handler mNoInterfaceHandler = new Handler();
    private Object mLock = new Object();
    private boolean mDetectedAtStartup = false;
    private byte mEffectivePrio = BASE_SERVER_PRIO;
    private INetworkManagementEventObserver mIfaceObserver = new BaseNetworkObserver() {
        public void interfaceStatusChanged(String iface, boolean up) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }

        public void interfaceLinkStateChanged(String iface, boolean up) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }

        public void interfaceAdded(String iface) {
            CommonTimeManagementService.this.reevaluateServiceState();
        }

        public void interfaceRemoved(String iface) {
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
        public void onServerDied() {
            CommonTimeManagementService.this.scheduleTimeConfigReconnect();
        }
    };
    private Runnable mReconnectRunnable = new Runnable() {
        @Override
        public void run() {
            CommonTimeManagementService.this.connectToTimeConfig();
        }
    };
    private Runnable mNoInterfaceRunnable = new Runnable() {
        @Override
        public void run() {
            CommonTimeManagementService.this.handleNoInterfaceTimeout();
        }
    };

    static {
        AUTO_DISABLE = SystemProperties.getInt(AUTO_DISABLE_PROP, 1) != 0;
        ALLOW_WIFI = SystemProperties.getInt(ALLOW_WIFI_PROP, 0) != 0;
        int tmp = SystemProperties.getInt(SERVER_PRIO_PROP, 1);
        NO_INTERFACE_TIMEOUT = SystemProperties.getInt(NO_INTERFACE_TIMEOUT_PROP, 60000);
        if (tmp < 1) {
            BASE_SERVER_PRIO = (byte) 1;
        } else if (tmp > 30) {
            BASE_SERVER_PRIO = (byte) 30;
        } else {
            BASE_SERVER_PRIO = (byte) tmp;
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
        IBinder b = ServiceManager.getService("network_management");
        this.mNetMgr = INetworkManagementService.Stub.asInterface(b);
        try {
            this.mNetMgr.registerObserver(this.mIfaceObserver);
        } catch (RemoteException e) {
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mConnectivityMangerObserver, filter);
        connectToTimeConfig();
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println(String.format("Permission Denial: can't dump CommonTimeManagement service from from pid=%d, uid=%d", Integer.valueOf(Binder.getCallingPid()), Integer.valueOf(Binder.getCallingUid())));
            return;
        }
        if (!this.mDetectedAtStartup) {
            pw.println("Native Common Time service was not detected at startup.  Service is unavailable");
            return;
        }
        synchronized (this.mLock) {
            pw.println("Current Common Time Management Service Config:");
            Object[] objArr = new Object[1];
            objArr[0] = this.mCTConfig == null ? "reconnecting" : "alive";
            pw.println(String.format("  Native service     : %s", objArr));
            Object[] objArr2 = new Object[1];
            objArr2[0] = this.mCurIface == null ? "unbound" : this.mCurIface;
            pw.println(String.format("  Bound interface    : %s", objArr2));
            Object[] objArr3 = new Object[1];
            objArr3[0] = ALLOW_WIFI ? "yes" : "no";
            pw.println(String.format("  Allow WiFi         : %s", objArr3));
            Object[] objArr4 = new Object[1];
            objArr4[0] = AUTO_DISABLE ? "yes" : "no";
            pw.println(String.format("  Allow Auto Disable : %s", objArr4));
            pw.println(String.format("  Server Priority    : %d", Byte.valueOf(this.mEffectivePrio)));
            pw.println(String.format("  No iface timeout   : %d", Integer.valueOf(NO_INTERFACE_TIMEOUT)));
        }
    }

    private static class InterfaceScoreRule {
        public final String mPrefix;
        public final byte mScore;

        public InterfaceScoreRule(String prefix, byte score) {
            this.mPrefix = prefix;
            this.mScore = score;
        }
    }

    private void cleanupTimeConfig() {
        this.mReconnectHandler.removeCallbacks(this.mReconnectRunnable);
        this.mNoInterfaceHandler.removeCallbacks(this.mNoInterfaceRunnable);
        if (this.mCTConfig == null) {
            return;
        }
        this.mCTConfig.release();
        this.mCTConfig = null;
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
        this.mReconnectHandler.postDelayed(this.mReconnectRunnable, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
    }

    private void handleNoInterfaceTimeout() {
        if (this.mCTConfig == null) {
            return;
        }
        Log.i(TAG, "Timeout waiting for interface to come up.  Forcing networkless master mode.");
        if (-7 != this.mCTConfig.forceNetworklessMasterMode()) {
            return;
        }
        scheduleTimeConfigReconnect();
    }

    private void reevaluateServiceState() {
        byte newPrio;
        InterfaceConfiguration config;
        String bindIface = null;
        byte bestScore = -1;
        try {
            String[] ifaceList = this.mNetMgr.listInterfaces();
            if (ifaceList != null) {
                for (String iface : ifaceList) {
                    byte thisScore = -1;
                    InterfaceScoreRule[] interfaceScoreRuleArr = IFACE_SCORE_RULES;
                    int i = 0;
                    int length = interfaceScoreRuleArr.length;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        InterfaceScoreRule r = interfaceScoreRuleArr[i];
                        if (!iface.contains(r.mPrefix)) {
                            i++;
                        } else {
                            thisScore = r.mScore;
                            break;
                        }
                    }
                    if (thisScore > bestScore && (config = this.mNetMgr.getInterfaceConfig(iface)) != null && config.isActive()) {
                        bindIface = iface;
                        bestScore = thisScore;
                    }
                }
            }
        } catch (RemoteException e) {
            bindIface = null;
        }
        boolean doRebind = true;
        synchronized (this.mLock) {
            if (bindIface != null) {
                if (this.mCurIface == null) {
                    Log.e(TAG, String.format("Binding common time service to %s.", bindIface));
                    this.mCurIface = bindIface;
                } else if (bindIface == null && this.mCurIface != null) {
                    Log.e(TAG, "Unbinding common time service.");
                    this.mCurIface = null;
                } else if (bindIface != null && this.mCurIface != null && !bindIface.equals(this.mCurIface)) {
                    Log.e(TAG, String.format("Switching common time service binding from %s to %s.", this.mCurIface, bindIface));
                    this.mCurIface = bindIface;
                } else {
                    doRebind = false;
                }
            }
        }
        if (!doRebind || this.mCTConfig == null) {
            return;
        }
        if (bestScore > 0) {
            newPrio = (byte) (BASE_SERVER_PRIO * bestScore);
        } else {
            newPrio = BASE_SERVER_PRIO;
        }
        if (newPrio != this.mEffectivePrio) {
            this.mEffectivePrio = newPrio;
            this.mCTConfig.setMasterElectionPriority(this.mEffectivePrio);
        }
        int res = this.mCTConfig.setNetworkBinding(this.mCurIface);
        if (res != 0) {
            scheduleTimeConfigReconnect();
        } else {
            if (NO_INTERFACE_TIMEOUT < 0) {
                return;
            }
            this.mNoInterfaceHandler.removeCallbacks(this.mNoInterfaceRunnable);
            if (this.mCurIface != null) {
                return;
            }
            this.mNoInterfaceHandler.postDelayed(this.mNoInterfaceRunnable, NO_INTERFACE_TIMEOUT);
        }
    }
}
