package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.IWifiNanIface;
import android.hardware.wifi.V1_0.WifiStatus;
import android.os.Handler;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.HalDeviceManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class WifiAwareNativeManager {
    private static final String TAG = "WifiAwareNativeManager";
    private static final boolean VDBG = false;
    private HalDeviceManager mHalDeviceManager;
    private Handler mHandler;
    private InterfaceDestroyedListener mInterfaceDestroyedListener;
    private WifiAwareNativeCallback mWifiAwareNativeCallback;
    private WifiAwareStateManager mWifiAwareStateManager;
    boolean mDbg = false;
    private final Object mLock = new Object();
    private IWifiNanIface mWifiNanIface = null;
    private InterfaceAvailableForRequestListener mInterfaceAvailableForRequestListener = new InterfaceAvailableForRequestListener();
    private int mReferenceCount = 0;

    WifiAwareNativeManager(WifiAwareStateManager wifiAwareStateManager, HalDeviceManager halDeviceManager, WifiAwareNativeCallback wifiAwareNativeCallback) {
        this.mWifiAwareStateManager = wifiAwareStateManager;
        this.mHalDeviceManager = halDeviceManager;
        this.mWifiAwareNativeCallback = wifiAwareNativeCallback;
    }

    public android.hardware.wifi.V1_2.IWifiNanIface mockableCastTo_1_2(IWifiNanIface iWifiNanIface) {
        return android.hardware.wifi.V1_2.IWifiNanIface.castFrom((IHwInterface) iWifiNanIface);
    }

    public void start(Handler handler) {
        this.mHandler = handler;
        this.mHalDeviceManager.initialize();
        this.mHalDeviceManager.registerStatusListener(new HalDeviceManager.ManagerStatusListener() {
            @Override
            public void onStatusChanged() {
                if (!WifiAwareNativeManager.this.mHalDeviceManager.isStarted()) {
                    WifiAwareNativeManager.this.awareIsDown();
                } else {
                    WifiAwareNativeManager.this.mHalDeviceManager.registerInterfaceAvailableForRequestListener(3, WifiAwareNativeManager.this.mInterfaceAvailableForRequestListener, WifiAwareNativeManager.this.mHandler);
                }
            }
        }, this.mHandler);
        if (this.mHalDeviceManager.isStarted()) {
            this.mHalDeviceManager.registerInterfaceAvailableForRequestListener(3, this.mInterfaceAvailableForRequestListener, this.mHandler);
        }
    }

    @VisibleForTesting
    public IWifiNanIface getWifiNanIface() {
        IWifiNanIface iWifiNanIface;
        synchronized (this.mLock) {
            iWifiNanIface = this.mWifiNanIface;
        }
        return iWifiNanIface;
    }

    public void tryToGetAware() {
        WifiStatus wifiStatusRegisterEventCallback_1_2;
        synchronized (this.mLock) {
            if (this.mDbg) {
                Log.d(TAG, "tryToGetAware: mWifiNanIface=" + this.mWifiNanIface + ", mReferenceCount=" + this.mReferenceCount);
            }
            if (this.mWifiNanIface != null) {
                this.mReferenceCount++;
                return;
            }
            if (this.mHalDeviceManager == null) {
                Log.e(TAG, "tryToGetAware: mHalDeviceManager is null!?");
                awareIsDown();
                return;
            }
            this.mInterfaceDestroyedListener = new InterfaceDestroyedListener();
            IWifiNanIface iWifiNanIfaceCreateNanIface = this.mHalDeviceManager.createNanIface(this.mInterfaceDestroyedListener, this.mHandler);
            if (iWifiNanIfaceCreateNanIface == null) {
                Log.e(TAG, "Was not able to obtain an IWifiNanIface (even though enabled!?)");
                awareIsDown();
            } else {
                if (this.mDbg) {
                    Log.v(TAG, "Obtained an IWifiNanIface");
                }
                try {
                    android.hardware.wifi.V1_2.IWifiNanIface iWifiNanIfaceMockableCastTo_1_2 = mockableCastTo_1_2(iWifiNanIfaceCreateNanIface);
                    if (iWifiNanIfaceMockableCastTo_1_2 == null) {
                        this.mWifiAwareNativeCallback.mIsHal12OrLater = false;
                        wifiStatusRegisterEventCallback_1_2 = iWifiNanIfaceCreateNanIface.registerEventCallback(this.mWifiAwareNativeCallback);
                    } else {
                        this.mWifiAwareNativeCallback.mIsHal12OrLater = true;
                        wifiStatusRegisterEventCallback_1_2 = iWifiNanIfaceMockableCastTo_1_2.registerEventCallback_1_2(this.mWifiAwareNativeCallback);
                    }
                    if (wifiStatusRegisterEventCallback_1_2.code != 0) {
                        Log.e(TAG, "IWifiNanIface.registerEventCallback error: " + statusString(wifiStatusRegisterEventCallback_1_2));
                        this.mHalDeviceManager.removeIface(iWifiNanIfaceCreateNanIface);
                        awareIsDown();
                        return;
                    }
                    this.mWifiNanIface = iWifiNanIfaceCreateNanIface;
                    this.mReferenceCount = 1;
                } catch (RemoteException e) {
                    Log.e(TAG, "IWifiNanIface.registerEventCallback exception: " + e);
                    awareIsDown();
                }
            }
        }
    }

    public void releaseAware() {
        if (this.mDbg) {
            Log.d(TAG, "releaseAware: mWifiNanIface=" + this.mWifiNanIface + ", mReferenceCount=" + this.mReferenceCount);
        }
        if (this.mWifiNanIface == null) {
            return;
        }
        if (this.mHalDeviceManager == null) {
            Log.e(TAG, "releaseAware: mHalDeviceManager is null!?");
            return;
        }
        synchronized (this.mLock) {
            this.mReferenceCount--;
            if (this.mReferenceCount != 0) {
                return;
            }
            this.mInterfaceDestroyedListener.active = false;
            this.mInterfaceDestroyedListener = null;
            this.mHalDeviceManager.removeIface(this.mWifiNanIface);
            this.mWifiNanIface = null;
        }
    }

    private void awareIsDown() {
        synchronized (this.mLock) {
            if (this.mDbg) {
                Log.d(TAG, "awareIsDown: mWifiNanIface=" + this.mWifiNanIface + ", mReferenceCount =" + this.mReferenceCount);
            }
            this.mWifiNanIface = null;
            this.mReferenceCount = 0;
            this.mWifiAwareStateManager.disableUsage();
        }
    }

    private class InterfaceDestroyedListener implements HalDeviceManager.InterfaceDestroyedListener {
        public boolean active;

        private InterfaceDestroyedListener() {
            this.active = true;
        }

        @Override
        public void onDestroyed(String str) {
            if (WifiAwareNativeManager.this.mDbg) {
                Log.d(WifiAwareNativeManager.TAG, "Interface was destroyed: mWifiNanIface=" + WifiAwareNativeManager.this.mWifiNanIface + ", active=" + this.active);
            }
            if (this.active && WifiAwareNativeManager.this.mWifiNanIface != null) {
                WifiAwareNativeManager.this.awareIsDown();
            }
        }
    }

    private class InterfaceAvailableForRequestListener implements HalDeviceManager.InterfaceAvailableForRequestListener {
        private InterfaceAvailableForRequestListener() {
        }

        @Override
        public void onAvailabilityChanged(boolean z) {
            if (WifiAwareNativeManager.this.mDbg) {
                Log.d(WifiAwareNativeManager.TAG, "Interface availability = " + z + ", mWifiNanIface=" + WifiAwareNativeManager.this.mWifiNanIface);
            }
            synchronized (WifiAwareNativeManager.this.mLock) {
                try {
                    if (z) {
                        WifiAwareNativeManager.this.mWifiAwareStateManager.enableUsage();
                    } else if (WifiAwareNativeManager.this.mWifiNanIface == null) {
                        WifiAwareNativeManager.this.mWifiAwareStateManager.disableUsage();
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    private static String statusString(WifiStatus wifiStatus) {
        if (wifiStatus == null) {
            return "status=null";
        }
        return wifiStatus.code + " (" + wifiStatus.description + ")";
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("WifiAwareNativeManager:");
        printWriter.println("  mWifiNanIface: " + this.mWifiNanIface);
        printWriter.println("  mReferenceCount: " + this.mReferenceCount);
        this.mWifiAwareNativeCallback.dump(fileDescriptor, printWriter, strArr);
        this.mHalDeviceManager.dump(fileDescriptor, printWriter, strArr);
    }
}
