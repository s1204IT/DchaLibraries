package com.android.bluetooth.hid;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.IBluetoothHidDevice;
import android.bluetooth.IBluetoothHidDeviceCallback;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import com.android.bluetooth.BluetoothMetricsProto;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.MetricsLogger;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.annotations.VisibleForTesting;
import com.android.vcard.VCardConfig;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class HidDeviceService extends ProfileService {
    private static final boolean DBG = false;
    private static final int FOREGROUND_IMPORTANCE_CUTOFF = 200;
    static final int HAL_CONN_STATE_CONNECTED = 0;
    static final int HAL_CONN_STATE_CONNECTING = 1;
    static final int HAL_CONN_STATE_DISCONNECTED = 2;
    static final int HAL_CONN_STATE_DISCONNECTING = 3;
    private static final int MESSAGE_APPLICATION_STATE_CHANGED = 1;
    private static final int MESSAGE_CONNECT_STATE_CHANGED = 2;
    private static final int MESSAGE_GET_REPORT = 3;
    private static final int MESSAGE_IMPORTANCE_CHANGE = 8;
    private static final int MESSAGE_INTR_DATA = 6;
    private static final int MESSAGE_SET_PROTOCOL = 5;
    private static final int MESSAGE_SET_REPORT = 4;
    private static final int MESSAGE_VC_UNPLUG = 7;
    private static final String TAG = HidDeviceService.class.getSimpleName();
    private static HidDeviceService sHidDeviceService;
    private ActivityManager mActivityManager;
    private IBluetoothHidDeviceCallback mCallback;
    private BluetoothHidDeviceDeathRecipient mDeathRcpt;
    private HidDeviceServiceHandler mHandler;
    private BluetoothDevice mHidDevice;
    private HidDeviceNativeInterface mHidDeviceNativeInterface;
    private boolean mNativeAvailable = false;
    private int mHidDeviceState = 0;
    private int mUserUid = 0;
    private ActivityManager.OnUidImportanceListener mUidImportanceListener = new ActivityManager.OnUidImportanceListener() {
        public void onUidImportance(int i, int i2) {
            Message messageObtainMessage = HidDeviceService.this.mHandler.obtainMessage(8);
            messageObtainMessage.arg1 = i2;
            messageObtainMessage.arg2 = i;
            HidDeviceService.this.mHandler.sendMessage(messageObtainMessage);
        }
    };

    private class HidDeviceServiceHandler extends Handler {
        private HidDeviceServiceHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    BluetoothDevice bluetoothDevice = message.obj != null ? (BluetoothDevice) message.obj : null;
                    boolean z = message.arg1 != 0;
                    if (z) {
                        Log.d(HidDeviceService.TAG, "App registered, set device to: " + bluetoothDevice);
                        HidDeviceService.this.mHidDevice = bluetoothDevice;
                    } else {
                        HidDeviceService.this.mHidDevice = null;
                    }
                    try {
                        if (HidDeviceService.this.mCallback != null) {
                            HidDeviceService.this.mCallback.onAppStatusChanged(bluetoothDevice, z);
                        }
                    } catch (RemoteException e) {
                        Log.e(HidDeviceService.TAG, "e=" + e.toString());
                        e.printStackTrace();
                    }
                    if (z) {
                        HidDeviceService.this.mDeathRcpt = new BluetoothHidDeviceDeathRecipient(HidDeviceService.this);
                        if (HidDeviceService.this.mCallback != null) {
                            try {
                                HidDeviceService.this.mCallback.asBinder().linkToDeath(HidDeviceService.this.mDeathRcpt, 0);
                                Log.i(HidDeviceService.TAG, "IBinder.linkToDeath() ok");
                            } catch (RemoteException e2) {
                                e2.printStackTrace();
                            }
                        }
                    } else if (HidDeviceService.this.mDeathRcpt != null && HidDeviceService.this.mCallback != null) {
                        try {
                            HidDeviceService.this.mCallback.asBinder().unlinkToDeath(HidDeviceService.this.mDeathRcpt, 0);
                            Log.i(HidDeviceService.TAG, "IBinder.unlinkToDeath() ok");
                        } catch (NoSuchElementException e3) {
                            e3.printStackTrace();
                        }
                        HidDeviceService.this.mDeathRcpt.cleanup();
                        HidDeviceService.this.mDeathRcpt = null;
                    }
                    if (!z) {
                        HidDeviceService.this.mCallback = null;
                    }
                    break;
                case 2:
                    BluetoothDevice bluetoothDevice2 = (BluetoothDevice) message.obj;
                    int iConvertHalState = HidDeviceService.convertHalState(message.arg1);
                    if (iConvertHalState != 0) {
                        HidDeviceService.this.mHidDevice = bluetoothDevice2;
                    }
                    HidDeviceService.this.setAndBroadcastConnectionState(bluetoothDevice2, iConvertHalState);
                    try {
                        if (HidDeviceService.this.mCallback != null) {
                            HidDeviceService.this.mCallback.onConnectionStateChanged(bluetoothDevice2, iConvertHalState);
                        }
                    } catch (RemoteException e4) {
                        e4.printStackTrace();
                        return;
                    }
                    break;
                case 3:
                    byte b = (byte) message.arg1;
                    byte b2 = (byte) message.arg2;
                    int iIntValue = message.obj != null ? ((Integer) message.obj).intValue() : 0;
                    try {
                        if (HidDeviceService.this.mCallback != null) {
                            HidDeviceService.this.mCallback.onGetReport(HidDeviceService.this.mHidDevice, b, b2, iIntValue);
                        }
                    } catch (RemoteException e5) {
                        e5.printStackTrace();
                        return;
                    }
                    break;
                case 4:
                    byte b3 = (byte) message.arg1;
                    byte b4 = (byte) message.arg2;
                    byte[] bArrArray = ((ByteBuffer) message.obj).array();
                    try {
                        if (HidDeviceService.this.mCallback != null) {
                            HidDeviceService.this.mCallback.onSetReport(HidDeviceService.this.mHidDevice, b3, b4, bArrArray);
                        }
                    } catch (RemoteException e6) {
                        e6.printStackTrace();
                        return;
                    }
                    break;
                case 5:
                    byte b5 = (byte) message.arg1;
                    try {
                        if (HidDeviceService.this.mCallback != null) {
                            HidDeviceService.this.mCallback.onSetProtocol(HidDeviceService.this.mHidDevice, b5);
                        }
                    } catch (RemoteException e7) {
                        e7.printStackTrace();
                        return;
                    }
                    break;
                case 6:
                    byte b6 = (byte) message.arg1;
                    byte[] bArrArray2 = ((ByteBuffer) message.obj).array();
                    try {
                        if (HidDeviceService.this.mCallback != null) {
                            HidDeviceService.this.mCallback.onInterruptData(HidDeviceService.this.mHidDevice, b6, bArrArray2);
                        }
                    } catch (RemoteException e8) {
                        e8.printStackTrace();
                        return;
                    }
                    break;
                case 7:
                    try {
                        if (HidDeviceService.this.mCallback != null) {
                            HidDeviceService.this.mCallback.onVirtualCableUnplug(HidDeviceService.this.mHidDevice);
                        }
                    } catch (RemoteException e9) {
                        e9.printStackTrace();
                    }
                    HidDeviceService.this.mHidDevice = null;
                    break;
                case 8:
                    int i = message.arg1;
                    int i2 = message.arg2;
                    if (i > 200 && i2 >= 10000) {
                        HidDeviceService.this.unregisterAppUid(i2);
                        break;
                    }
                    break;
            }
        }
    }

    private static class BluetoothHidDeviceDeathRecipient implements IBinder.DeathRecipient {
        private HidDeviceService mService;

        BluetoothHidDeviceDeathRecipient(HidDeviceService hidDeviceService) {
            this.mService = hidDeviceService;
        }

        @Override
        public void binderDied() {
            Log.w(HidDeviceService.TAG, "Binder died, need to unregister app :(");
            this.mService.unregisterApp();
        }

        public void cleanup() {
            this.mService = null;
        }
    }

    @VisibleForTesting
    static class BluetoothHidDeviceBinder extends IBluetoothHidDevice.Stub implements ProfileService.IProfileServiceBinder {
        private static final String TAG = BluetoothHidDeviceBinder.class.getSimpleName();
        private HidDeviceService mService;

        BluetoothHidDeviceBinder(HidDeviceService hidDeviceService) {
            this.mService = hidDeviceService;
        }

        @VisibleForTesting
        HidDeviceService getServiceForTesting() {
            if (this.mService != null && this.mService.isAvailable()) {
                return this.mService;
            }
            return null;
        }

        @Override
        public void cleanup() {
            this.mService = null;
        }

        private HidDeviceService getService() {
            if (!Utils.checkCaller()) {
                Log.w(TAG, "HidDevice call not allowed for non-active user");
                return null;
            }
            if (this.mService == null || !this.mService.isAvailable()) {
                return null;
            }
            return this.mService;
        }

        public boolean registerApp(BluetoothHidDeviceAppSdpSettings bluetoothHidDeviceAppSdpSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings2, IBluetoothHidDeviceCallback iBluetoothHidDeviceCallback) {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.registerApp(bluetoothHidDeviceAppSdpSettings, bluetoothHidDeviceAppQosSettings, bluetoothHidDeviceAppQosSettings2, iBluetoothHidDeviceCallback);
        }

        public boolean unregisterApp() {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.unregisterApp();
        }

        public boolean sendReport(BluetoothDevice bluetoothDevice, int i, byte[] bArr) {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.sendReport(bluetoothDevice, i, bArr);
        }

        public boolean replyReport(BluetoothDevice bluetoothDevice, byte b, byte b2, byte[] bArr) {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.replyReport(bluetoothDevice, b, b2, bArr);
        }

        public boolean unplug(BluetoothDevice bluetoothDevice) {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.unplug(bluetoothDevice);
        }

        public boolean connect(BluetoothDevice bluetoothDevice) {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.connect(bluetoothDevice);
        }

        public boolean disconnect(BluetoothDevice bluetoothDevice) {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.disconnect(bluetoothDevice);
        }

        public boolean reportError(BluetoothDevice bluetoothDevice, byte b) {
            HidDeviceService service = getService();
            if (service == null) {
                return false;
            }
            return service.reportError(bluetoothDevice, b);
        }

        public int getConnectionState(BluetoothDevice bluetoothDevice) {
            HidDeviceService service = getService();
            if (service == null) {
                return 0;
            }
            return service.getConnectionState(bluetoothDevice);
        }

        public List<BluetoothDevice> getConnectedDevices() {
            return getDevicesMatchingConnectionStates(new int[]{2});
        }

        public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
            HidDeviceService service = getService();
            if (service == null) {
                return new ArrayList(0);
            }
            return service.getDevicesMatchingConnectionStates(iArr);
        }

        public String getUserAppName() {
            HidDeviceService service = getService();
            if (service == null) {
                return "";
            }
            return service.getUserAppName();
        }
    }

    @Override
    protected ProfileService.IProfileServiceBinder initBinder() {
        return new BluetoothHidDeviceBinder(this);
    }

    private boolean checkDevice(BluetoothDevice bluetoothDevice) {
        if (this.mHidDevice == null || !this.mHidDevice.equals(bluetoothDevice)) {
            Log.w(TAG, "Unknown device: " + bluetoothDevice);
            return false;
        }
        return true;
    }

    private boolean checkCallingUid() {
        if (Binder.getCallingUid() != this.mUserUid) {
            Log.w(TAG, "checkCallingUid(): caller UID doesn't match registered user UID");
            return false;
        }
        return true;
    }

    synchronized boolean registerApp(BluetoothHidDeviceAppSdpSettings bluetoothHidDeviceAppSdpSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings, BluetoothHidDeviceAppQosSettings bluetoothHidDeviceAppQosSettings2, IBluetoothHidDeviceCallback iBluetoothHidDeviceCallback) {
        int[] iArr;
        int[] iArr2;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (this.mUserUid != 0) {
            Log.w(TAG, "registerApp(): failed because another app is registered");
            return false;
        }
        int callingUid = Binder.getCallingUid();
        if (callingUid >= 10000 && this.mActivityManager.getUidImportance(callingUid) > 200) {
            Log.w(TAG, "registerApp(): failed because the app is not foreground");
            return false;
        }
        this.mUserUid = callingUid;
        this.mCallback = iBluetoothHidDeviceCallback;
        HidDeviceNativeInterface hidDeviceNativeInterface = this.mHidDeviceNativeInterface;
        String name = bluetoothHidDeviceAppSdpSettings.getName();
        String description = bluetoothHidDeviceAppSdpSettings.getDescription();
        String provider = bluetoothHidDeviceAppSdpSettings.getProvider();
        byte subclass = bluetoothHidDeviceAppSdpSettings.getSubclass();
        byte[] descriptors = bluetoothHidDeviceAppSdpSettings.getDescriptors();
        if (bluetoothHidDeviceAppQosSettings != null) {
            iArr = new int[]{bluetoothHidDeviceAppQosSettings.getServiceType(), bluetoothHidDeviceAppQosSettings.getTokenRate(), bluetoothHidDeviceAppQosSettings.getTokenBucketSize(), bluetoothHidDeviceAppQosSettings.getPeakBandwidth(), bluetoothHidDeviceAppQosSettings.getLatency(), bluetoothHidDeviceAppQosSettings.getDelayVariation()};
        } else {
            iArr = null;
        }
        if (bluetoothHidDeviceAppQosSettings2 == null) {
            iArr2 = null;
        } else {
            iArr2 = new int[]{bluetoothHidDeviceAppQosSettings2.getServiceType(), bluetoothHidDeviceAppQosSettings2.getTokenRate(), bluetoothHidDeviceAppQosSettings2.getTokenBucketSize(), bluetoothHidDeviceAppQosSettings2.getPeakBandwidth(), bluetoothHidDeviceAppQosSettings2.getLatency(), bluetoothHidDeviceAppQosSettings2.getDelayVariation()};
        }
        return hidDeviceNativeInterface.registerApp(name, description, provider, subclass, descriptors, iArr, iArr2);
    }

    synchronized boolean unregisterApp() {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        return unregisterAppUid(Binder.getCallingUid());
    }

    private synchronized boolean unregisterAppUid(int i) {
        if (this.mUserUid == 0 || (i != this.mUserUid && i >= 10000)) {
            return false;
        }
        this.mUserUid = 0;
        return this.mHidDeviceNativeInterface.unregisterApp();
    }

    synchronized boolean sendReport(BluetoothDevice bluetoothDevice, int i, byte[] bArr) {
        boolean z;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (!checkDevice(bluetoothDevice) || !checkCallingUid()) {
            z = false;
        } else if (this.mHidDeviceNativeInterface.sendReport(i, bArr)) {
            z = true;
        }
        return z;
    }

    synchronized boolean replyReport(BluetoothDevice bluetoothDevice, byte b, byte b2, byte[] bArr) {
        boolean z;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (!checkDevice(bluetoothDevice) || !checkCallingUid()) {
            z = false;
        } else if (this.mHidDeviceNativeInterface.replyReport(b, b2, bArr)) {
            z = true;
        }
        return z;
    }

    synchronized boolean unplug(BluetoothDevice bluetoothDevice) {
        boolean z;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (!checkDevice(bluetoothDevice) || !checkCallingUid()) {
            z = false;
        } else if (this.mHidDeviceNativeInterface.unplug()) {
            z = true;
        }
        return z;
    }

    synchronized boolean connect(BluetoothDevice bluetoothDevice) {
        boolean z;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (checkCallingUid()) {
            z = this.mHidDeviceNativeInterface.connect(bluetoothDevice);
        }
        return z;
    }

    synchronized boolean disconnect(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        int callingUid = Binder.getCallingUid();
        boolean z = false;
        if (callingUid != this.mUserUid && callingUid >= 10000) {
            Log.w(TAG, "disconnect(): caller UID doesn't match user UID");
            return false;
        }
        if (checkDevice(bluetoothDevice) && this.mHidDeviceNativeInterface.disconnect()) {
            z = true;
        }
        return z;
    }

    synchronized boolean reportError(BluetoothDevice bluetoothDevice, byte b) {
        boolean z;
        enforceCallingOrSelfPermission("android.permission.BLUETOOTH_ADMIN", "Need BLUETOOTH ADMIN permission");
        if (!checkDevice(bluetoothDevice) || !checkCallingUid()) {
            z = false;
        } else if (this.mHidDeviceNativeInterface.reportError(b)) {
            z = true;
        }
        return z;
    }

    synchronized String getUserAppName() {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (this.mUserUid < 10000) {
            return "";
        }
        String nameForUid = getPackageManager().getNameForUid(this.mUserUid);
        if (nameForUid == null) {
            nameForUid = "";
        }
        return nameForUid;
    }

    @Override
    protected boolean start() {
        if (sHidDeviceService != null) {
            Log.d(TAG, "start() twice, just return!!!");
            return true;
        }
        this.mHandler = new HidDeviceServiceHandler();
        this.mHidDeviceNativeInterface = HidDeviceNativeInterface.getInstance();
        this.mHidDeviceNativeInterface.init();
        this.mNativeAvailable = true;
        this.mActivityManager = (ActivityManager) getSystemService("activity");
        this.mActivityManager.addOnUidImportanceListener(this.mUidImportanceListener, 200);
        setHidDeviceService(this);
        return true;
    }

    @Override
    protected boolean stop() {
        if (sHidDeviceService == null) {
            Log.w(TAG, "stop() called before start()");
            return true;
        }
        setHidDeviceService(null);
        if (this.mNativeAvailable) {
            this.mHidDeviceNativeInterface.cleanup();
            this.mNativeAvailable = false;
        }
        if (this.mActivityManager != null) {
            this.mActivityManager.removeOnUidImportanceListener(this.mUidImportanceListener);
        }
        return true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Need to unregister app");
        unregisterApp();
        return super.onUnbind(intent);
    }

    public static synchronized HidDeviceService getHidDeviceService() {
        if (sHidDeviceService == null) {
            Log.d(TAG, "getHidDeviceService(): service is NULL");
            return null;
        }
        if (!sHidDeviceService.isAvailable()) {
            Log.d(TAG, "getHidDeviceService(): service is not available");
            return null;
        }
        return sHidDeviceService;
    }

    private static synchronized void setHidDeviceService(HidDeviceService hidDeviceService) {
        sHidDeviceService = hidDeviceService;
    }

    int getConnectionState(BluetoothDevice bluetoothDevice) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        if (this.mHidDevice != null && this.mHidDevice.equals(bluetoothDevice)) {
            return this.mHidDeviceState;
        }
        return 0;
    }

    List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] iArr) {
        enforceCallingOrSelfPermission(ProfileService.BLUETOOTH_PERM, "Need BLUETOOTH permission");
        ArrayList arrayList = new ArrayList();
        if (this.mHidDevice != null) {
            int length = iArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (iArr[i] != this.mHidDeviceState) {
                    i++;
                } else {
                    arrayList.add(this.mHidDevice);
                    break;
                }
            }
        }
        return arrayList;
    }

    synchronized void onApplicationStateChangedFromNative(BluetoothDevice bluetoothDevice, boolean z) {
        Message messageObtainMessage = this.mHandler.obtainMessage(1);
        messageObtainMessage.obj = bluetoothDevice;
        messageObtainMessage.arg1 = z ? 1 : 0;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    synchronized void onConnectStateChangedFromNative(BluetoothDevice bluetoothDevice, int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(2);
        messageObtainMessage.obj = bluetoothDevice;
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    synchronized void onGetReportFromNative(byte b, byte b2, short s) {
        Message messageObtainMessage = this.mHandler.obtainMessage(3);
        messageObtainMessage.obj = s > 0 ? new Integer(s) : null;
        messageObtainMessage.arg1 = b;
        messageObtainMessage.arg2 = b2;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    synchronized void onSetReportFromNative(byte b, byte b2, byte[] bArr) {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        Message messageObtainMessage = this.mHandler.obtainMessage(4);
        messageObtainMessage.arg1 = b;
        messageObtainMessage.arg2 = b2;
        messageObtainMessage.obj = byteBufferWrap;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    synchronized void onSetProtocolFromNative(byte b) {
        Message messageObtainMessage = this.mHandler.obtainMessage(5);
        messageObtainMessage.arg1 = b;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    synchronized void onInterruptDataFromNative(byte b, byte[] bArr) {
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
        Message messageObtainMessage = this.mHandler.obtainMessage(6);
        messageObtainMessage.arg1 = b;
        messageObtainMessage.obj = byteBufferWrap;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    synchronized void onVirtualCableUnplugFromNative() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7));
    }

    private void setAndBroadcastConnectionState(BluetoothDevice bluetoothDevice, int i) {
        if (this.mHidDevice != null && !this.mHidDevice.equals(bluetoothDevice)) {
            Log.w(TAG, "Connection state changed for unknown device, ignoring");
            return;
        }
        int i2 = this.mHidDeviceState;
        this.mHidDeviceState = i;
        if (i2 == i) {
            Log.w(TAG, "Connection state is unchanged, ignoring");
            return;
        }
        if (i == 2) {
            MetricsLogger.logProfileConnectionEvent(BluetoothMetricsProto.ProfileId.HID_DEVICE);
        }
        Intent intent = new Intent("android.bluetooth.hiddevice.profile.action.CONNECTION_STATE_CHANGED");
        intent.putExtra("android.bluetooth.profile.extra.PREVIOUS_STATE", i2);
        intent.putExtra("android.bluetooth.profile.extra.STATE", i);
        intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
        intent.addFlags(VCardConfig.FLAG_APPEND_TYPE_PARAM);
        sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
    }

    private static int convertHalState(int i) {
        switch (i) {
        }
        return 0;
    }
}
