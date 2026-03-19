package com.android.server.wifi;

import android.R;
import android.content.Context;
import android.hardware.wifi.hostapd.V1_0.HostapdStatus;
import android.hardware.wifi.hostapd.V1_0.IHostapd;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.net.wifi.WifiConfiguration;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.util.NativeUtil;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class HostapdHal {
    private static final String TAG = "HostapdHal";
    private WifiNative.HostapdDeathEventHandler mDeathEventHandler;
    private final boolean mEnableAcs;
    private final boolean mEnableIeee80211AC;
    private IHostapd mIHostapd;
    private final Object mLock = new Object();
    private boolean mVerboseLoggingEnabled = false;
    private IServiceManager mIServiceManager = null;
    private final IServiceNotification mServiceNotificationCallback = new IServiceNotification.Stub() {
        public void onRegistration(String str, String str2, boolean z) {
            synchronized (HostapdHal.this.mLock) {
                if (HostapdHal.this.mVerboseLoggingEnabled) {
                    Log.i(HostapdHal.TAG, "IServiceNotification.onRegistration for: " + str + ", " + str2 + " preexisting=" + z);
                }
                if (!HostapdHal.this.initHostapdService()) {
                    Log.e(HostapdHal.TAG, "initalizing IHostapd failed.");
                    HostapdHal.this.hostapdServiceDiedHandler();
                } else {
                    Log.i(HostapdHal.TAG, "Completed initialization of IHostapd.");
                }
            }
        }
    };
    private final IHwBinder.DeathRecipient mServiceManagerDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            HostapdHal.lambda$new$0(this.f$0, j);
        }
    };
    private final IHwBinder.DeathRecipient mHostapdDeathRecipient = new IHwBinder.DeathRecipient() {
        @Override
        public final void serviceDied(long j) {
            HostapdHal.lambda$new$1(this.f$0, j);
        }
    };

    public static void lambda$new$0(HostapdHal hostapdHal, long j) {
        synchronized (hostapdHal.mLock) {
            Log.w(TAG, "IServiceManager died: cookie=" + j);
            hostapdHal.hostapdServiceDiedHandler();
            hostapdHal.mIServiceManager = null;
        }
    }

    public static void lambda$new$1(HostapdHal hostapdHal, long j) {
        synchronized (hostapdHal.mLock) {
            Log.w(TAG, "IHostapd/IHostapd died: cookie=" + j);
            hostapdHal.hostapdServiceDiedHandler();
        }
    }

    public HostapdHal(Context context) {
        this.mEnableAcs = context.getResources().getBoolean(R.^attr-private.regularColor);
        this.mEnableIeee80211AC = context.getResources().getBoolean(R.^attr-private.relativeTimeDisambiguationText);
    }

    void enableVerboseLogging(boolean z) {
        synchronized (this.mLock) {
            this.mVerboseLoggingEnabled = z;
        }
    }

    private boolean linkToServiceManagerDeath() {
        synchronized (this.mLock) {
            if (this.mIServiceManager == null) {
                return false;
            }
            try {
                if (!this.mIServiceManager.linkToDeath(this.mServiceManagerDeathRecipient, 0L)) {
                    Log.wtf(TAG, "Error on linkToDeath on IServiceManager");
                    hostapdServiceDiedHandler();
                    this.mIServiceManager = null;
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "IServiceManager.linkToDeath exception", e);
                this.mIServiceManager = null;
                return false;
            }
        }
    }

    public boolean initialize() {
        synchronized (this.mLock) {
            if (this.mVerboseLoggingEnabled) {
                Log.i(TAG, "Registering IHostapd service ready callback.");
            }
            this.mIHostapd = null;
            if (this.mIServiceManager != null) {
                return true;
            }
            try {
                this.mIServiceManager = getServiceManagerMockable();
                if (this.mIServiceManager == null) {
                    Log.e(TAG, "Failed to get HIDL Service Manager");
                    return false;
                }
                if (!linkToServiceManagerDeath()) {
                    return false;
                }
                if (this.mIServiceManager.registerForNotifications(IHostapd.kInterfaceName, "", this.mServiceNotificationCallback)) {
                    return true;
                }
                Log.e(TAG, "Failed to register for notifications to android.hardware.wifi.hostapd@1.0::IHostapd");
                this.mIServiceManager = null;
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "Exception while trying to register a listener for IHostapd service: " + e);
                hostapdServiceDiedHandler();
                this.mIServiceManager = null;
                return false;
            }
        }
    }

    private boolean linkToHostapdDeath() {
        synchronized (this.mLock) {
            if (this.mIHostapd == null) {
                return false;
            }
            try {
                if (!this.mIHostapd.linkToDeath(this.mHostapdDeathRecipient, 0L)) {
                    Log.wtf(TAG, "Error on linkToDeath on IHostapd");
                    hostapdServiceDiedHandler();
                    return false;
                }
                return true;
            } catch (RemoteException e) {
                Log.e(TAG, "IHostapd.linkToDeath exception", e);
                return false;
            }
        }
    }

    private boolean initHostapdService() {
        synchronized (this.mLock) {
            try {
                try {
                    this.mIHostapd = getHostapdMockable();
                    if (this.mIHostapd == null) {
                        Log.e(TAG, "Got null IHostapd service. Stopping hostapd HIDL startup");
                        return false;
                    }
                    if (!linkToHostapdDeath()) {
                        return false;
                    }
                    return true;
                } catch (RemoteException e) {
                    Log.e(TAG, "IHostapd.getService exception: " + e);
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public boolean addAccessPoint(String str, WifiConfiguration wifiConfiguration) {
        synchronized (this.mLock) {
            IHostapd.IfaceParams ifaceParams = new IHostapd.IfaceParams();
            ifaceParams.ifaceName = str;
            ifaceParams.hwModeParams.enable80211N = true;
            ifaceParams.hwModeParams.enable80211AC = this.mEnableIeee80211AC;
            try {
                ifaceParams.channelParams.band = getBand(wifiConfiguration);
                if (this.mEnableAcs) {
                    ifaceParams.channelParams.enableAcs = true;
                    ifaceParams.channelParams.acsShouldExcludeDfs = true;
                } else {
                    if (ifaceParams.channelParams.band == 2) {
                        Log.d(TAG, "ACS is not supported on this device, using 2.4 GHz band.");
                        ifaceParams.channelParams.band = 0;
                    }
                    ifaceParams.channelParams.enableAcs = false;
                    ifaceParams.channelParams.channel = wifiConfiguration.apChannel;
                }
                IHostapd.NetworkParams networkParams = new IHostapd.NetworkParams();
                networkParams.ssid.addAll(NativeUtil.stringToByteArrayList(wifiConfiguration.SSID));
                networkParams.isHidden = wifiConfiguration.hiddenSSID;
                networkParams.encryptionType = getEncryptionType(wifiConfiguration);
                networkParams.pskPassphrase = wifiConfiguration.preSharedKey != null ? wifiConfiguration.preSharedKey : "";
                if (!checkHostapdAndLogFailure("addAccessPoint")) {
                    return false;
                }
                try {
                    return checkStatusAndLogFailure(this.mIHostapd.addAccessPoint(ifaceParams, networkParams), "addAccessPoint");
                } catch (RemoteException e) {
                    handleRemoteException(e, "addAccessPoint");
                    return false;
                }
            } catch (IllegalArgumentException e2) {
                Log.e(TAG, "Unrecognized apBand " + wifiConfiguration.apBand);
                return false;
            }
        }
    }

    public boolean removeAccessPoint(String str) {
        synchronized (this.mLock) {
            if (!checkHostapdAndLogFailure("removeAccessPoint")) {
                return false;
            }
            try {
                return checkStatusAndLogFailure(this.mIHostapd.removeAccessPoint(str), "removeAccessPoint");
            } catch (RemoteException e) {
                handleRemoteException(e, "removeAccessPoint");
                return false;
            }
        }
    }

    public boolean registerDeathHandler(WifiNative.HostapdDeathEventHandler hostapdDeathEventHandler) {
        if (this.mDeathEventHandler != null) {
            Log.e(TAG, "Death handler already present");
        }
        this.mDeathEventHandler = hostapdDeathEventHandler;
        return true;
    }

    public boolean deregisterDeathHandler() {
        if (this.mDeathEventHandler == null) {
            Log.e(TAG, "No Death handler present");
        }
        this.mDeathEventHandler = null;
        return true;
    }

    private void clearState() {
        synchronized (this.mLock) {
            this.mIHostapd = null;
        }
    }

    private void hostapdServiceDiedHandler() {
        synchronized (this.mLock) {
            clearState();
            if (this.mDeathEventHandler != null) {
                this.mDeathEventHandler.onDeath();
            }
        }
    }

    public boolean isInitializationStarted() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIServiceManager != null;
        }
        return z;
    }

    public boolean isInitializationComplete() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIHostapd != null;
        }
        return z;
    }

    public void terminate() {
        synchronized (this.mLock) {
            if (checkHostapdAndLogFailure("terminate")) {
                try {
                    this.mIHostapd.terminate();
                } catch (RemoteException e) {
                    handleRemoteException(e, "terminate");
                }
            }
        }
    }

    @VisibleForTesting
    protected IServiceManager getServiceManagerMockable() throws RemoteException {
        IServiceManager service;
        synchronized (this.mLock) {
            service = IServiceManager.getService();
        }
        return service;
    }

    @VisibleForTesting
    protected IHostapd getHostapdMockable() throws RemoteException {
        IHostapd service;
        synchronized (this.mLock) {
            service = IHostapd.getService();
        }
        return service;
    }

    private static int getEncryptionType(WifiConfiguration wifiConfiguration) {
        int authType = wifiConfiguration.getAuthType();
        if (authType != 4) {
            switch (authType) {
                case 0:
                default:
                    return 0;
                case 1:
                    return 1;
            }
        }
        return 2;
    }

    private static int getBand(WifiConfiguration wifiConfiguration) {
        switch (wifiConfiguration.apBand) {
            case -1:
                return 2;
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                throw new IllegalArgumentException();
        }
    }

    private boolean checkHostapdAndLogFailure(String str) {
        synchronized (this.mLock) {
            if (this.mIHostapd == null) {
                Log.e(TAG, "Can't call " + str + ", IHostapd is null");
                return false;
            }
            return true;
        }
    }

    private boolean checkStatusAndLogFailure(HostapdStatus hostapdStatus, String str) {
        synchronized (this.mLock) {
            if (hostapdStatus.code != 0) {
                Log.e(TAG, "IHostapd." + str + " failed: " + hostapdStatus.code + ", " + hostapdStatus.debugMessage);
                return false;
            }
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "IHostapd." + str + " succeeded");
            }
            return true;
        }
    }

    private void handleRemoteException(RemoteException remoteException, String str) {
        synchronized (this.mLock) {
            hostapdServiceDiedHandler();
            Log.e(TAG, "IHostapd." + str + " failed with exception", remoteException);
        }
    }

    private static void logd(String str) {
        Log.d(TAG, str);
    }

    private static void logi(String str) {
        Log.i(TAG, str);
    }

    private static void loge(String str) {
        Log.e(TAG, str);
    }
}
