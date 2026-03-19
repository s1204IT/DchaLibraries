package com.android.bluetooth.hid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.internal.annotations.GuardedBy;

public class HidDeviceNativeInterface {
    private static final Object INSTANCE_LOCK = new Object();
    private static final String TAG = "HidDeviceNativeInterface";

    @GuardedBy("INSTANCE_LOCK")
    private static HidDeviceNativeInterface sInstance;
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();

    private static native void classInitNative();

    private native void cleanupNative();

    private native boolean connectNative(byte[] bArr);

    private native boolean disconnectNative();

    private native void initNative();

    private native boolean registerAppNative(String str, String str2, String str3, byte b, byte[] bArr, int[] iArr, int[] iArr2);

    private native boolean replyReportNative(byte b, byte b2, byte[] bArr);

    private native boolean reportErrorNative(byte b);

    private native boolean sendReportNative(int i, byte[] bArr);

    private native boolean unplugNative();

    private native boolean unregisterAppNative();

    static {
        classInitNative();
    }

    @VisibleForTesting
    private HidDeviceNativeInterface() {
        if (this.mAdapter == null) {
            Log.wtfStack(TAG, "No Bluetooth Adapter Available");
        }
    }

    public static HidDeviceNativeInterface getInstance() {
        HidDeviceNativeInterface hidDeviceNativeInterface;
        synchronized (INSTANCE_LOCK) {
            if (sInstance == null) {
                setInstance(new HidDeviceNativeInterface());
            }
            hidDeviceNativeInterface = sInstance;
        }
        return hidDeviceNativeInterface;
    }

    private static void setInstance(HidDeviceNativeInterface hidDeviceNativeInterface) {
        sInstance = hidDeviceNativeInterface;
    }

    public void init() {
        initNative();
    }

    public void cleanup() {
        cleanupNative();
    }

    public boolean registerApp(String str, String str2, String str3, byte b, byte[] bArr, int[] iArr, int[] iArr2) {
        return registerAppNative(str, str2, str3, b, bArr, iArr, iArr2);
    }

    public boolean unregisterApp() {
        return unregisterAppNative();
    }

    public boolean sendReport(int i, byte[] bArr) {
        return sendReportNative(i, bArr);
    }

    public boolean replyReport(byte b, byte b2, byte[] bArr) {
        return replyReportNative(b, b2, bArr);
    }

    public boolean unplug() {
        return unplugNative();
    }

    public boolean connect(BluetoothDevice bluetoothDevice) {
        return connectNative(getByteAddress(bluetoothDevice));
    }

    public boolean disconnect() {
        return disconnectNative();
    }

    public boolean reportError(byte b) {
        return reportErrorNative(b);
    }

    private synchronized void onApplicationStateChanged(byte[] bArr, boolean z) {
        HidDeviceService hidDeviceService = HidDeviceService.getHidDeviceService();
        if (hidDeviceService != null) {
            hidDeviceService.onApplicationStateChangedFromNative(getDevice(bArr), z);
        } else {
            Log.wtfStack(TAG, "FATAL: onApplicationStateChanged() is called from the stack while service is not available.");
        }
    }

    private synchronized void onConnectStateChanged(byte[] bArr, int i) {
        HidDeviceService hidDeviceService = HidDeviceService.getHidDeviceService();
        if (hidDeviceService != null) {
            hidDeviceService.onConnectStateChangedFromNative(getDevice(bArr), i);
        } else {
            Log.wtfStack(TAG, "FATAL: onConnectStateChanged() is called from the stack while service is not available.");
        }
    }

    private synchronized void onGetReport(byte b, byte b2, short s) {
        HidDeviceService hidDeviceService = HidDeviceService.getHidDeviceService();
        if (hidDeviceService != null) {
            hidDeviceService.onGetReportFromNative(b, b2, s);
        } else {
            Log.wtfStack(TAG, "FATAL: onGetReport() is called from the stack while service is not available.");
        }
    }

    private synchronized void onSetReport(byte b, byte b2, byte[] bArr) {
        HidDeviceService hidDeviceService = HidDeviceService.getHidDeviceService();
        if (hidDeviceService != null) {
            hidDeviceService.onSetReportFromNative(b, b2, bArr);
        } else {
            Log.wtfStack(TAG, "FATAL: onSetReport() is called from the stack while service is not available.");
        }
    }

    private synchronized void onSetProtocol(byte b) {
        HidDeviceService hidDeviceService = HidDeviceService.getHidDeviceService();
        if (hidDeviceService != null) {
            hidDeviceService.onSetProtocolFromNative(b);
        } else {
            Log.wtfStack(TAG, "FATAL: onSetProtocol() is called from the stack while service is not available.");
        }
    }

    private synchronized void onInterruptData(byte b, byte[] bArr) {
        HidDeviceService hidDeviceService = HidDeviceService.getHidDeviceService();
        if (hidDeviceService != null) {
            hidDeviceService.onInterruptDataFromNative(b, bArr);
        } else {
            Log.wtfStack(TAG, "FATAL: onInterruptData() is called from the stack while service is not available.");
        }
    }

    private synchronized void onVirtualCableUnplug() {
        HidDeviceService hidDeviceService = HidDeviceService.getHidDeviceService();
        if (hidDeviceService != null) {
            hidDeviceService.onVirtualCableUnplugFromNative();
        } else {
            Log.wtfStack(TAG, "FATAL: onVirtualCableUnplug() is called from the stack while service is not available.");
        }
    }

    private BluetoothDevice getDevice(byte[] bArr) {
        if (bArr == null) {
            return null;
        }
        return this.mAdapter.getRemoteDevice(bArr);
    }

    private byte[] getByteAddress(BluetoothDevice bluetoothDevice) {
        return Utils.getBytesFromAddress(bluetoothDevice.getAddress());
    }
}
