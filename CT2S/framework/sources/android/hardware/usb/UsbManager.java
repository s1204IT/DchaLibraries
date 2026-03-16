package android.hardware.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.net.ProxyInfo;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import java.util.HashMap;

public class UsbManager {
    public static final String ACTION_USB_ACCESSORY_ATTACHED = "android.hardware.usb.action.USB_ACCESSORY_ATTACHED";
    public static final String ACTION_USB_ACCESSORY_DETACHED = "android.hardware.usb.action.USB_ACCESSORY_DETACHED";
    public static final String ACTION_USB_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    public static final String EXTRA_ACCESSORY = "accessory";
    public static final String EXTRA_DEVICE = "device";
    public static final String EXTRA_PERMISSION_GRANTED = "permission";
    private static final String TAG = "UsbManager";
    public static final String USB_CONFIGURED = "configured";
    public static final String USB_CONNECTED = "connected";
    public static final String USB_FUNCTION_ACCESSORY = "accessory";
    public static final String USB_FUNCTION_ADB = "adb";
    public static final String USB_FUNCTION_AUDIO_SOURCE = "audio_source";
    public static final String USB_FUNCTION_CHARGE_ONLY = "charge_only";
    public static final String USB_FUNCTION_DIAG = "marvell_diag";
    public static final String USB_FUNCTION_MASS_STORAGE = "mass_storage";
    public static final String USB_FUNCTION_MODEM = "acm";
    public static final String USB_FUNCTION_MTP = "mtp";
    public static final String USB_FUNCTION_PCSUITE = "pcsuite";
    public static final String USB_FUNCTION_PTP = "ptp";
    public static final String USB_FUNCTION_RNDIS = "rndis";
    private final Context mContext;
    private final IUsbManager mService;

    public UsbManager(Context context, IUsbManager service) {
        this.mContext = context;
        this.mService = service;
    }

    public HashMap<String, UsbDevice> getDeviceList() {
        Bundle bundle = new Bundle();
        try {
            this.mService.getDeviceList(bundle);
            HashMap<String, UsbDevice> result = new HashMap<>();
            for (String name : bundle.keySet()) {
                result.put(name, (UsbDevice) bundle.get(name));
            }
            return result;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getDeviceList", e);
            return null;
        }
    }

    public UsbDeviceConnection openDevice(UsbDevice device) {
        try {
            String deviceName = device.getDeviceName();
            ParcelFileDescriptor pfd = this.mService.openDevice(deviceName);
            if (pfd != null) {
                UsbDeviceConnection connection = new UsbDeviceConnection(device);
                boolean result = connection.open(deviceName, pfd);
                pfd.close();
                if (result) {
                    return connection;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception in UsbManager.openDevice", e);
        }
        return null;
    }

    public UsbAccessory[] getAccessoryList() {
        try {
            UsbAccessory accessory = this.mService.getCurrentAccessory();
            if (accessory == null) {
                return null;
            }
            return new UsbAccessory[]{accessory};
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in getAccessoryList", e);
            return null;
        }
    }

    public ParcelFileDescriptor openAccessory(UsbAccessory accessory) {
        try {
            return this.mService.openAccessory(accessory);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in openAccessory", e);
            return null;
        }
    }

    public boolean hasPermission(UsbDevice device) {
        try {
            return this.mService.hasDevicePermission(device);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in hasPermission", e);
            return false;
        }
    }

    public boolean hasPermission(UsbAccessory accessory) {
        try {
            return this.mService.hasAccessoryPermission(accessory);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in hasPermission", e);
            return false;
        }
    }

    public void requestPermission(UsbDevice device, PendingIntent pi) {
        try {
            this.mService.requestDevicePermission(device, this.mContext.getPackageName(), pi);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in requestPermission", e);
        }
    }

    public void requestPermission(UsbAccessory accessory, PendingIntent pi) {
        try {
            this.mService.requestAccessoryPermission(accessory, this.mContext.getPackageName(), pi);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in requestPermission", e);
        }
    }

    private static boolean propertyContainsFunction(String property, String function) {
        String functions = SystemProperties.get(property, ProxyInfo.LOCAL_EXCL_LIST);
        int index = functions.indexOf(function);
        if (index < 0) {
            return false;
        }
        if (index > 0 && functions.charAt(index - 1) != ',') {
            return false;
        }
        int charAfter = index + function.length();
        return charAfter >= functions.length() || functions.charAt(charAfter) == ',';
    }

    public boolean isFunctionEnabled(String function) {
        return propertyContainsFunction("sys.usb.config", function);
    }

    private static String removeFunction(String functions, String function) {
        String[] split = functions.split(",");
        for (int i = 0; i < split.length; i++) {
            if (function.equals(split[i])) {
                split[i] = null;
            }
        }
        StringBuilder builder = new StringBuilder();
        for (String s : split) {
            if (s != null) {
                if (builder.length() > 0) {
                    builder.append(",");
                }
                builder.append(s);
            }
        }
        return builder.toString();
    }

    public String getDefaultFunction() {
        String functions = SystemProperties.get("persist.sys.usb.config", ProxyInfo.LOCAL_EXCL_LIST);
        return removeFunction(functions, USB_FUNCTION_ADB);
    }

    public void setCurrentFunction(String function, boolean makeDefault) {
        try {
            this.mService.setCurrentFunction(function, makeDefault);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setCurrentFunction", e);
        }
    }

    public void setMassStorageBackingFile(String path) {
        try {
            this.mService.setMassStorageBackingFile(path);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in setDefaultFunction", e);
        }
    }
}
