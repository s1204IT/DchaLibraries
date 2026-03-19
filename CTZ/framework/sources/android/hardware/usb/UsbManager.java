package android.hardware.usb;

import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class UsbManager {
    public static final String ACTION_USB_ACCESSORY_ATTACHED = "android.hardware.usb.action.USB_ACCESSORY_ATTACHED";
    public static final String ACTION_USB_ACCESSORY_DETACHED = "android.hardware.usb.action.USB_ACCESSORY_DETACHED";
    public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_PORT_CHANGED = "android.hardware.usb.action.USB_PORT_CHANGED";
    public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    public static final String EXTRA_ACCESSORY = "accessory";
    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_PERMISSION_GRANTED = "permission";
    public static final String EXTRA_PORT = "port";
    public static final String EXTRA_PORT_STATUS = "portStatus";
    public static final long FUNCTION_ACCESSORY = 2;
    public static final long FUNCTION_ADB = 1;
    public static final long FUNCTION_AUDIO_SOURCE = 64;
    public static final long FUNCTION_BYPASS = 32768;
    public static final long FUNCTION_MIDI = 8;
    public static final long FUNCTION_MTP = 4;
    private static final Map<String, Long> FUNCTION_NAME_TO_CODE = new HashMap();
    public static final long FUNCTION_NONE = 0;
    public static final long FUNCTION_PTP = 16;
    public static final long FUNCTION_RNDIS = 32;
    private static final long SETTABLE_FUNCTIONS = 32828;
    private static final String TAG = "UsbManager";
    public static final String USB_CONFIGURED = "configured";
    public static final String USB_CONNECTED = "connected";
    public static final String USB_DATA_UNLOCKED = "unlocked";
    public static final String USB_FUNCTION_ACCESSORY = "accessory";
    public static final String USB_FUNCTION_ADB = "adb";
    public static final String USB_FUNCTION_AUDIO_SOURCE = "audio_source";
    public static final String USB_FUNCTION_BYPASS = "via_bypass";
    public static final String USB_FUNCTION_MIDI = "midi";
    public static final String USB_FUNCTION_MTP = "mtp";
    public static final String USB_FUNCTION_NONE = "none";
    public static final String USB_FUNCTION_PTP = "ptp";
    public static final String USB_FUNCTION_RNDIS = "rndis";
    public static final String USB_HOST_CONNECTED = "host_connected";
    private final Context mContext;
    private final IUsbManager mService;

    static {
        FUNCTION_NAME_TO_CODE.put(USB_FUNCTION_MTP, 4L);
        FUNCTION_NAME_TO_CODE.put(USB_FUNCTION_PTP, 16L);
        FUNCTION_NAME_TO_CODE.put(USB_FUNCTION_RNDIS, 32L);
        FUNCTION_NAME_TO_CODE.put("midi", 8L);
        FUNCTION_NAME_TO_CODE.put("accessory", 2L);
        FUNCTION_NAME_TO_CODE.put(USB_FUNCTION_AUDIO_SOURCE, 64L);
        FUNCTION_NAME_TO_CODE.put(USB_FUNCTION_ADB, 1L);
        FUNCTION_NAME_TO_CODE.put(USB_FUNCTION_BYPASS, 32768L);
    }

    public UsbManager(Context context, IUsbManager iUsbManager) {
        this.mContext = context;
        this.mService = iUsbManager;
    }

    public HashMap<String, UsbDevice> getDeviceList() {
        HashMap<String, UsbDevice> map = new HashMap<>();
        if (this.mService == null) {
            return map;
        }
        Bundle bundle = new Bundle();
        try {
            this.mService.getDeviceList(bundle);
            for (String str : bundle.keySet()) {
                map.put(str, (UsbDevice) bundle.get(str));
            }
            return map;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UsbDeviceConnection openDevice(UsbDevice usbDevice) {
        try {
            String deviceName = usbDevice.getDeviceName();
            ParcelFileDescriptor parcelFileDescriptorOpenDevice = this.mService.openDevice(deviceName, this.mContext.getPackageName());
            if (parcelFileDescriptorOpenDevice != null) {
                UsbDeviceConnection usbDeviceConnection = new UsbDeviceConnection(usbDevice);
                boolean zOpen = usbDeviceConnection.open(deviceName, parcelFileDescriptorOpenDevice, this.mContext);
                parcelFileDescriptorOpenDevice.close();
                if (zOpen) {
                    return usbDeviceConnection;
                }
                return null;
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "exception in UsbManager.openDevice", e);
            return null;
        }
    }

    public UsbAccessory[] getAccessoryList() {
        if (this.mService == null) {
            return null;
        }
        try {
            UsbAccessory currentAccessory = this.mService.getCurrentAccessory();
            if (currentAccessory == null) {
                return null;
            }
            return new UsbAccessory[]{currentAccessory};
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory usbAccessory) {
        try {
            return this.mService.openAccessory(usbAccessory);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ParcelFileDescriptor getControlFd(long j) {
        try {
            return this.mService.getControlFd(j);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasPermission(UsbDevice usbDevice) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.hasDevicePermission(usbDevice, this.mContext.getPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean hasPermission(UsbAccessory usbAccessory) {
        if (this.mService == null) {
            return false;
        }
        try {
            return this.mService.hasAccessoryPermission(usbAccessory);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestPermission(UsbDevice usbDevice, PendingIntent pendingIntent) {
        try {
            this.mService.requestDevicePermission(usbDevice, this.mContext.getPackageName(), pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestPermission(UsbAccessory usbAccessory, PendingIntent pendingIntent) {
        try {
            this.mService.requestAccessoryPermission(usbAccessory, this.mContext.getPackageName(), pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void grantPermission(UsbDevice usbDevice) {
        grantPermission(usbDevice, Process.myUid());
    }

    public void grantPermission(UsbDevice usbDevice, int i) {
        try {
            this.mService.grantDevicePermission(usbDevice, i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public void grantPermission(UsbDevice usbDevice, String str) {
        try {
            grantPermission(usbDevice, this.mContext.getPackageManager().getPackageUidAsUser(str, this.mContext.getUserId()));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package " + str + " not found.", e);
        }
    }

    @Deprecated
    public boolean isFunctionEnabled(String str) {
        try {
            return this.mService.isFunctionEnabled(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setCurrentFunctions(long j) {
        try {
            this.mService.setCurrentFunctions(j);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setCurrentFunction(String str, boolean z) {
        try {
            this.mService.setCurrentFunction(str, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getCurrentFunctions() {
        try {
            return this.mService.getCurrentFunctions();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setScreenUnlockedFunctions(long j) {
        try {
            this.mService.setScreenUnlockedFunctions(j);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public long getScreenUnlockedFunctions() {
        try {
            return this.mService.getScreenUnlockedFunctions();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UsbPort[] getPorts() {
        if (this.mService == null) {
            return null;
        }
        try {
            return this.mService.getPorts();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public UsbPortStatus getPortStatus(UsbPort usbPort) {
        Preconditions.checkNotNull(usbPort, "port must not be null");
        try {
            return this.mService.getPortStatus(usbPort.getId());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setPortRoles(UsbPort usbPort, int i, int i2) {
        Preconditions.checkNotNull(usbPort, "port must not be null");
        UsbPort.checkRoles(i, i2);
        Log.d(TAG, "setPortRoles Package:" + this.mContext.getPackageName());
        try {
            this.mService.setPortRoles(usbPort.getId(), i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setUsbDeviceConnectionHandler(ComponentName componentName) {
        try {
            this.mService.setUsbDeviceConnectionHandler(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static boolean areSettableFunctions(long j) {
        if (j != 0) {
            return ((-32829) & j) == 0 && Long.bitCount(j) == 1;
        }
        return true;
    }

    public static String usbFunctionsToString(long j) {
        StringJoiner stringJoiner = new StringJoiner(",");
        if ((4 & j) != 0) {
            stringJoiner.add(USB_FUNCTION_MTP);
        }
        if ((16 & j) != 0) {
            stringJoiner.add(USB_FUNCTION_PTP);
        }
        if ((32 & j) != 0) {
            stringJoiner.add(USB_FUNCTION_RNDIS);
        }
        if ((8 & j) != 0) {
            stringJoiner.add("midi");
        }
        if ((2 & j) != 0) {
            stringJoiner.add("accessory");
        }
        if ((64 & j) != 0) {
            stringJoiner.add(USB_FUNCTION_AUDIO_SOURCE);
        }
        if ((1 & j) != 0) {
            stringJoiner.add(USB_FUNCTION_ADB);
        }
        if ((j & 32768) != 0) {
            stringJoiner.add(USB_FUNCTION_BYPASS);
        }
        return stringJoiner.toString();
    }

    public static long usbFunctionsFromString(String str) {
        long jLongValue = 0;
        if (str == null || str.equals("none")) {
            return 0L;
        }
        for (String str2 : str.split(",")) {
            if (FUNCTION_NAME_TO_CODE.containsKey(str2)) {
                jLongValue |= FUNCTION_NAME_TO_CODE.get(str2).longValue();
            } else if (str2.length() > 0) {
                throw new IllegalArgumentException("Invalid usb function " + str);
            }
        }
        return jLongValue;
    }
}
