package com.android.server.usb;

import android.R;
import android.content.Context;
import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.net.dhcp.DhcpPacket;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class UsbHostManager {
    private static final boolean DEBUG = false;
    private static final String TAG = UsbHostManager.class.getSimpleName();
    private final Context mContext;

    @GuardedBy("mLock")
    private UsbSettingsManager mCurrentSettings;
    private final String[] mHostBlacklist;
    private UsbConfiguration mNewConfiguration;
    private ArrayList<UsbConfiguration> mNewConfigurations;
    private UsbDevice mNewDevice;
    private ArrayList<UsbEndpoint> mNewEndpoints;
    private UsbInterface mNewInterface;
    private ArrayList<UsbInterface> mNewInterfaces;
    private final UsbAlsaManager mUsbAlsaManager;
    private final HashMap<String, UsbDevice> mDevices = new HashMap<>();
    private final Object mLock = new Object();

    private native void monitorUsbHostBus();

    private native ParcelFileDescriptor nativeOpenDevice(String str);

    public UsbHostManager(Context context, UsbAlsaManager alsaManager) {
        this.mContext = context;
        this.mHostBlacklist = context.getResources().getStringArray(R.array.config_autoTimeSourcesPriority);
        this.mUsbAlsaManager = alsaManager;
    }

    public void setCurrentSettings(UsbSettingsManager settings) {
        synchronized (this.mLock) {
            this.mCurrentSettings = settings;
        }
    }

    private UsbSettingsManager getCurrentSettings() {
        UsbSettingsManager usbSettingsManager;
        synchronized (this.mLock) {
            usbSettingsManager = this.mCurrentSettings;
        }
        return usbSettingsManager;
    }

    private boolean isBlackListed(String deviceName) {
        int count = this.mHostBlacklist.length;
        for (int i = 0; i < count; i++) {
            if (deviceName.startsWith(this.mHostBlacklist[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlackListed(int clazz, int subClass, int protocol) {
        if (clazz == 9) {
            return true;
        }
        return clazz == 3 && subClass == 1;
    }

    private boolean beginUsbDeviceAdded(String deviceName, int vendorID, int productID, int deviceClass, int deviceSubclass, int deviceProtocol, String manufacturerName, String productName, int version, String serialNumber) {
        if (isBlackListed(deviceName) || isBlackListed(deviceClass, deviceSubclass, deviceProtocol)) {
            return false;
        }
        synchronized (this.mLock) {
            if (this.mDevices.get(deviceName) != null) {
                Slog.w(TAG, "device already on mDevices list: " + deviceName);
                return false;
            }
            if (this.mNewDevice != null) {
                Slog.e(TAG, "mNewDevice is not null in endUsbDeviceAdded");
                return false;
            }
            String versionString = Integer.toString(version >> 8) + "." + (version & DhcpPacket.MAX_OPTION_LEN);
            this.mNewDevice = new UsbDevice(deviceName, vendorID, productID, deviceClass, deviceSubclass, deviceProtocol, manufacturerName, productName, versionString, serialNumber);
            this.mNewConfigurations = new ArrayList<>();
            this.mNewInterfaces = new ArrayList<>();
            this.mNewEndpoints = new ArrayList<>();
            return true;
        }
    }

    private void addUsbConfiguration(int id, String name, int attributes, int maxPower) {
        if (this.mNewConfiguration != null) {
            this.mNewConfiguration.setInterfaces((Parcelable[]) this.mNewInterfaces.toArray(new UsbInterface[this.mNewInterfaces.size()]));
            this.mNewInterfaces.clear();
        }
        this.mNewConfiguration = new UsbConfiguration(id, name, attributes, maxPower);
        this.mNewConfigurations.add(this.mNewConfiguration);
    }

    private void addUsbInterface(int id, String name, int altSetting, int Class, int subClass, int protocol) {
        if (this.mNewInterface != null) {
            this.mNewInterface.setEndpoints((Parcelable[]) this.mNewEndpoints.toArray(new UsbEndpoint[this.mNewEndpoints.size()]));
            this.mNewEndpoints.clear();
        }
        this.mNewInterface = new UsbInterface(id, altSetting, name, Class, subClass, protocol);
        this.mNewInterfaces.add(this.mNewInterface);
        if (this.mNewInterface.getInterfaceClass() != 14) {
            return;
        }
        SystemProperties.set("front_camera_version", "2");
    }

    private void addUsbEndpoint(int address, int attributes, int maxPacketSize, int interval) {
        this.mNewEndpoints.add(new UsbEndpoint(address, attributes, maxPacketSize, interval));
    }

    private void endUsbDeviceAdded() {
        if (this.mNewInterface != null) {
            this.mNewInterface.setEndpoints((Parcelable[]) this.mNewEndpoints.toArray(new UsbEndpoint[this.mNewEndpoints.size()]));
        }
        if (this.mNewConfiguration != null) {
            this.mNewConfiguration.setInterfaces((Parcelable[]) this.mNewInterfaces.toArray(new UsbInterface[this.mNewInterfaces.size()]));
        }
        synchronized (this.mLock) {
            if (this.mNewDevice != null) {
                this.mNewDevice.setConfigurations((Parcelable[]) this.mNewConfigurations.toArray(new UsbConfiguration[this.mNewConfigurations.size()]));
                this.mDevices.put(this.mNewDevice.getDeviceName(), this.mNewDevice);
                Slog.d(TAG, "Added device " + this.mNewDevice);
                getCurrentSettings().deviceAttached(this.mNewDevice);
                this.mUsbAlsaManager.usbDeviceAdded(this.mNewDevice);
            } else {
                Slog.e(TAG, "mNewDevice is null in endUsbDeviceAdded");
            }
            this.mNewDevice = null;
            this.mNewConfigurations = null;
            this.mNewInterfaces = null;
            this.mNewEndpoints = null;
            this.mNewConfiguration = null;
            this.mNewInterface = null;
        }
    }

    private void usbDeviceRemoved(String deviceName) {
        synchronized (this.mLock) {
            UsbDevice device = this.mDevices.remove(deviceName);
            if (device != null) {
                this.mUsbAlsaManager.usbDeviceRemoved(device);
                int numInterfaces = device.getInterfaceCount();
                int intf = 0;
                while (true) {
                    if (intf >= numInterfaces) {
                        break;
                    }
                    UsbInterface interfaces = device.getInterface(intf);
                    if (interfaces.getInterfaceClass() != 14) {
                        intf++;
                    } else {
                        SystemProperties.set("front_camera_version", "1");
                        break;
                    }
                }
                getCurrentSettings().deviceDetached(device);
            }
        }
    }

    public void systemReady() {
        synchronized (this.mLock) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    UsbHostManager.this.monitorUsbHostBus();
                }
            };
            new Thread(null, runnable, "UsbService host thread").start();
        }
    }

    public void getDeviceList(Bundle devices) {
        synchronized (this.mLock) {
            for (String name : this.mDevices.keySet()) {
                devices.putParcelable(name, this.mDevices.get(name));
            }
        }
    }

    public ParcelFileDescriptor openDevice(String deviceName) {
        ParcelFileDescriptor parcelFileDescriptorNativeOpenDevice;
        synchronized (this.mLock) {
            if (isBlackListed(deviceName)) {
                throw new SecurityException("USB device is on a restricted bus");
            }
            UsbDevice device = this.mDevices.get(deviceName);
            if (device == null) {
                throw new IllegalArgumentException("device " + deviceName + " does not exist or is restricted");
            }
            getCurrentSettings().checkPermission(device);
            parcelFileDescriptorNativeOpenDevice = nativeOpenDevice(deviceName);
        }
        return parcelFileDescriptorNativeOpenDevice;
    }

    public void dump(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("USB Host State:");
            for (String name : this.mDevices.keySet()) {
                pw.println("  " + name + ": " + this.mDevices.get(name));
            }
        }
    }
}
