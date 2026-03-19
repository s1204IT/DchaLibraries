package com.android.server.wifi.aware;

import android.app.AppOpsManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.wifi.aware.Characteristics;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.DiscoverySession;
import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.IWifiAwareManager;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.server.wifi.FrameworkFacade;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class WifiAwareServiceImpl extends IWifiAwareManager.Stub {
    private static final String TAG = "WifiAwareService";
    private static final boolean VDBG = false;
    private AppOpsManager mAppOps;
    private Context mContext;
    private WifiAwareShellCommand mShellCommand;
    private WifiAwareStateManager mStateManager;
    private WifiPermissionsUtil mWifiPermissionsUtil;
    boolean mDbg = false;
    private final Object mLock = new Object();
    private final SparseArray<IBinder.DeathRecipient> mDeathRecipientsByClientId = new SparseArray<>();
    private int mNextClientId = 1;
    private final SparseIntArray mUidByClientId = new SparseIntArray();

    public WifiAwareServiceImpl(Context context) {
        this.mContext = context.getApplicationContext();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
    }

    public int getMockableCallingUid() {
        return getCallingUid();
    }

    public void start(HandlerThread handlerThread, final WifiAwareStateManager wifiAwareStateManager, WifiAwareShellCommand wifiAwareShellCommand, WifiAwareMetrics wifiAwareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper wifiPermissionsWrapper, final FrameworkFacade frameworkFacade, final WifiAwareNativeManager wifiAwareNativeManager, final WifiAwareNativeApi wifiAwareNativeApi, final WifiAwareNativeCallback wifiAwareNativeCallback) {
        Log.i(TAG, "Starting Wi-Fi Aware service");
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mStateManager = wifiAwareStateManager;
        this.mShellCommand = wifiAwareShellCommand;
        this.mStateManager.start(this.mContext, handlerThread.getLooper(), wifiAwareMetrics, wifiPermissionsUtil, wifiPermissionsWrapper);
        frameworkFacade.registerContentObserver(this.mContext, Settings.Global.getUriFor("wifi_verbose_logging_enabled"), true, new ContentObserver(new Handler(handlerThread.getLooper())) {
            @Override
            public void onChange(boolean z) {
                WifiAwareServiceImpl.this.enableVerboseLogging(frameworkFacade.getIntegerSetting(WifiAwareServiceImpl.this.mContext, "wifi_verbose_logging_enabled", 0), wifiAwareStateManager, wifiAwareNativeManager, wifiAwareNativeApi, wifiAwareNativeCallback);
            }
        });
        enableVerboseLogging(frameworkFacade.getIntegerSetting(this.mContext, "wifi_verbose_logging_enabled", 0), wifiAwareStateManager, wifiAwareNativeManager, wifiAwareNativeApi, wifiAwareNativeCallback);
    }

    private void enableVerboseLogging(int i, WifiAwareStateManager wifiAwareStateManager, WifiAwareNativeManager wifiAwareNativeManager, WifiAwareNativeApi wifiAwareNativeApi, WifiAwareNativeCallback wifiAwareNativeCallback) {
        boolean z;
        if (i > 0) {
            z = true;
        } else {
            z = false;
        }
        this.mDbg = z;
        wifiAwareStateManager.mDbg = z;
        if (wifiAwareStateManager.mDataPathMgr != null) {
            wifiAwareStateManager.mDataPathMgr.mDbg = z;
            WifiInjector.getInstance().getWifiMetrics().getWifiAwareMetrics().mDbg = z;
        }
        wifiAwareNativeCallback.mDbg = z;
        wifiAwareNativeManager.mDbg = z;
        wifiAwareNativeApi.mDbg = z;
    }

    public void startLate() {
        Log.i(TAG, "Late initialization of Wi-Fi Aware service");
        this.mStateManager.startLate();
    }

    public boolean isUsageEnabled() {
        enforceAccessPermission();
        return this.mStateManager.isUsageEnabled();
    }

    public Characteristics getCharacteristics() {
        enforceAccessPermission();
        if (this.mStateManager.getCapabilities() == null) {
            return null;
        }
        return this.mStateManager.getCapabilities().toPublicCharacteristics();
    }

    public void connect(final IBinder iBinder, String str, IWifiAwareEventCallback iWifiAwareEventCallback, ConfigRequest configRequest, boolean z) {
        final int i;
        enforceAccessPermission();
        enforceChangePermission();
        int mockableCallingUid = getMockableCallingUid();
        this.mAppOps.checkPackage(mockableCallingUid, str);
        if (iWifiAwareEventCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        if (iBinder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        }
        if (z) {
            enforceLocationPermission(str, getMockableCallingUid());
        }
        if (configRequest != null) {
            enforceNetworkStackPermission();
        } else {
            configRequest = new ConfigRequest.Builder().build();
        }
        ConfigRequest configRequest2 = configRequest;
        configRequest2.validate();
        int callingPid = getCallingPid();
        synchronized (this.mLock) {
            i = this.mNextClientId;
            this.mNextClientId = i + 1;
        }
        if (this.mDbg) {
            Log.v(TAG, "connect: uid=" + mockableCallingUid + ", clientId=" + i + ", configRequest" + configRequest2 + ", notifyOnIdentityChanged=" + z);
        }
        IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                if (WifiAwareServiceImpl.this.mDbg) {
                    Log.v(WifiAwareServiceImpl.TAG, "binderDied: clientId=" + i);
                }
                iBinder.unlinkToDeath(this, 0);
                synchronized (WifiAwareServiceImpl.this.mLock) {
                    WifiAwareServiceImpl.this.mDeathRecipientsByClientId.delete(i);
                    WifiAwareServiceImpl.this.mUidByClientId.delete(i);
                }
                WifiAwareServiceImpl.this.mStateManager.disconnect(i);
            }
        };
        try {
            iBinder.linkToDeath(deathRecipient, 0);
            synchronized (this.mLock) {
                this.mDeathRecipientsByClientId.put(i, deathRecipient);
                this.mUidByClientId.put(i, mockableCallingUid);
            }
            this.mStateManager.connect(i, mockableCallingUid, callingPid, str, iWifiAwareEventCallback, configRequest2, z);
        } catch (RemoteException e) {
            Log.e(TAG, "Error on linkToDeath - " + e);
            try {
                iWifiAwareEventCallback.onConnectFail(1);
            } catch (RemoteException e2) {
                Log.e(TAG, "Error on onConnectFail()");
            }
        }
    }

    public void disconnect(int i, IBinder iBinder) {
        enforceAccessPermission();
        enforceChangePermission();
        int mockableCallingUid = getMockableCallingUid();
        enforceClientValidity(mockableCallingUid, i);
        if (this.mDbg) {
            Log.v(TAG, "disconnect: uid=" + mockableCallingUid + ", clientId=" + i);
        }
        if (iBinder == null) {
            throw new IllegalArgumentException("Binder must not be null");
        }
        synchronized (this.mLock) {
            IBinder.DeathRecipient deathRecipient = this.mDeathRecipientsByClientId.get(i);
            if (deathRecipient != null) {
                iBinder.unlinkToDeath(deathRecipient, 0);
                this.mDeathRecipientsByClientId.delete(i);
            }
            this.mUidByClientId.delete(i);
        }
        this.mStateManager.disconnect(i);
    }

    public void terminateSession(int i, int i2) {
        enforceAccessPermission();
        enforceChangePermission();
        enforceClientValidity(getMockableCallingUid(), i);
        this.mStateManager.terminateSession(i, i2);
    }

    public void publish(String str, int i, PublishConfig publishConfig, IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback) {
        enforceAccessPermission();
        enforceChangePermission();
        int mockableCallingUid = getMockableCallingUid();
        this.mAppOps.checkPackage(mockableCallingUid, str);
        enforceLocationPermission(str, getMockableCallingUid());
        if (iWifiAwareDiscoverySessionCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        if (publishConfig == null) {
            throw new IllegalArgumentException("PublishConfig must not be null");
        }
        publishConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
        enforceClientValidity(mockableCallingUid, i);
        this.mStateManager.publish(i, publishConfig, iWifiAwareDiscoverySessionCallback);
    }

    public void updatePublish(int i, int i2, PublishConfig publishConfig) {
        enforceAccessPermission();
        enforceChangePermission();
        if (publishConfig == null) {
            throw new IllegalArgumentException("PublishConfig must not be null");
        }
        publishConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
        enforceClientValidity(getMockableCallingUid(), i);
        this.mStateManager.updatePublish(i, i2, publishConfig);
    }

    public void subscribe(String str, int i, SubscribeConfig subscribeConfig, IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback) {
        enforceAccessPermission();
        enforceChangePermission();
        int mockableCallingUid = getMockableCallingUid();
        this.mAppOps.checkPackage(mockableCallingUid, str);
        enforceLocationPermission(str, getMockableCallingUid());
        if (iWifiAwareDiscoverySessionCallback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        if (subscribeConfig == null) {
            throw new IllegalArgumentException("SubscribeConfig must not be null");
        }
        subscribeConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
        enforceClientValidity(mockableCallingUid, i);
        this.mStateManager.subscribe(i, subscribeConfig, iWifiAwareDiscoverySessionCallback);
    }

    public void updateSubscribe(int i, int i2, SubscribeConfig subscribeConfig) {
        enforceAccessPermission();
        enforceChangePermission();
        if (subscribeConfig == null) {
            throw new IllegalArgumentException("SubscribeConfig must not be null");
        }
        subscribeConfig.assertValid(this.mStateManager.getCharacteristics(), this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.rtt"));
        enforceClientValidity(getMockableCallingUid(), i);
        this.mStateManager.updateSubscribe(i, i2, subscribeConfig);
    }

    public void sendMessage(int i, int i2, int i3, byte[] bArr, int i4, int i5) {
        enforceAccessPermission();
        enforceChangePermission();
        if (i5 != 0) {
            enforceNetworkStackPermission();
        }
        if (bArr != null && bArr.length > this.mStateManager.getCharacteristics().getMaxServiceSpecificInfoLength()) {
            throw new IllegalArgumentException("Message length longer than supported by device characteristics");
        }
        if (i5 < 0 || i5 > DiscoverySession.getMaxSendRetryCount()) {
            throw new IllegalArgumentException("Invalid 'retryCount' must be non-negative and <= DiscoverySession.MAX_SEND_RETRY_COUNT");
        }
        enforceClientValidity(getMockableCallingUid(), i);
        this.mStateManager.sendMessage(i, i2, i3, bArr, i4, i5);
    }

    public void requestMacAddresses(int i, List list, IWifiAwareMacAddressProvider iWifiAwareMacAddressProvider) {
        enforceNetworkStackPermission();
        this.mStateManager.requestMacAddresses(i, list, iWifiAwareMacAddressProvider);
    }

    public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
        this.mShellCommand.exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump WifiAwareService from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        printWriter.println("Wi-Fi Aware Service");
        synchronized (this.mLock) {
            printWriter.println("  mNextClientId: " + this.mNextClientId);
            printWriter.println("  mDeathRecipientsByClientId: " + this.mDeathRecipientsByClientId);
            printWriter.println("  mUidByClientId: " + this.mUidByClientId);
        }
        this.mStateManager.dump(fileDescriptor, printWriter, strArr);
    }

    private void enforceClientValidity(int i, int i2) {
        synchronized (this.mLock) {
            int iIndexOfKey = this.mUidByClientId.indexOfKey(i2);
            if (iIndexOfKey < 0 || this.mUidByClientId.valueAt(iIndexOfKey) != i) {
                throw new SecurityException("Attempting to use invalid uid+clientId mapping: uid=" + i + ", clientId=" + i2);
            }
        }
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceLocationPermission(String str, int i) {
        this.mWifiPermissionsUtil.enforceLocationPermission(str, i);
    }

    private void enforceNetworkStackPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.NETWORK_STACK", TAG);
    }
}
