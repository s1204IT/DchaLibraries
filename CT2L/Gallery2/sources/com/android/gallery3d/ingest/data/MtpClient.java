package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDevice;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@TargetApi(12)
public class MtpClient {
    private final Context mContext;
    private final PendingIntent mPermissionIntent;
    private final UsbManager mUsbManager;
    private final ArrayList<Listener> mListeners = new ArrayList<>();
    private final HashMap<String, MtpDevice> mDevices = new HashMap<>();
    private final ArrayList<String> mRequestPermissionDevices = new ArrayList<>();
    private final ArrayList<String> mIgnoredDevices = new ArrayList<>();
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra("device");
            String deviceName = usbDevice.getDeviceName();
            synchronized (MtpClient.this.mDevices) {
                MtpDevice mtpDevice = (MtpDevice) MtpClient.this.mDevices.get(deviceName);
                if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
                    if (mtpDevice == null) {
                        mtpDevice = MtpClient.this.openDeviceLocked(usbDevice);
                    }
                    if (mtpDevice != null) {
                        for (Listener listener : MtpClient.this.mListeners) {
                            listener.deviceAdded(mtpDevice);
                        }
                    }
                } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
                    if (mtpDevice != null) {
                        MtpClient.this.mDevices.remove(deviceName);
                        MtpClient.this.mRequestPermissionDevices.remove(deviceName);
                        MtpClient.this.mIgnoredDevices.remove(deviceName);
                        for (Listener listener2 : MtpClient.this.mListeners) {
                            listener2.deviceRemoved(mtpDevice);
                        }
                    }
                } else if ("com.android.gallery3d.ingest.action.USB_PERMISSION".equals(action)) {
                    MtpClient.this.mRequestPermissionDevices.remove(deviceName);
                    boolean permission = intent.getBooleanExtra("permission", false);
                    Log.d("MtpClient", "ACTION_USB_PERMISSION: " + permission);
                    if (!permission) {
                        MtpClient.this.mIgnoredDevices.add(deviceName);
                    } else {
                        if (mtpDevice == null) {
                            mtpDevice = MtpClient.this.openDeviceLocked(usbDevice);
                        }
                        if (mtpDevice != null) {
                            for (Listener listener3 : MtpClient.this.mListeners) {
                                listener3.deviceAdded(mtpDevice);
                            }
                        }
                    }
                }
            }
        }
    };

    public interface Listener {
        void deviceAdded(MtpDevice mtpDevice);

        void deviceRemoved(MtpDevice mtpDevice);
    }

    public static boolean isCamera(UsbDevice device) {
        int count = device.getInterfaceCount();
        for (int i = 0; i < count; i++) {
            UsbInterface intf = device.getInterface(i);
            if (intf.getInterfaceClass() == 6 && intf.getInterfaceSubclass() == 1 && intf.getInterfaceProtocol() == 1) {
                return true;
            }
        }
        return false;
    }

    public MtpClient(Context context) {
        this.mContext = context;
        this.mUsbManager = (UsbManager) context.getSystemService("usb");
        this.mPermissionIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.gallery3d.ingest.action.USB_PERMISSION"), 0);
        IntentFilter filter = new IntentFilter("com.android.gallery3d.ingest.action.USB_PERMISSION");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        filter.addAction("com.android.gallery3d.ingest.action.USB_PERMISSION");
        context.registerReceiver(this.mUsbReceiver, filter);
    }

    private MtpDevice openDeviceLocked(UsbDevice usbDevice) {
        String deviceName = usbDevice.getDeviceName();
        if (isCamera(usbDevice) && !this.mIgnoredDevices.contains(deviceName) && !this.mRequestPermissionDevices.contains(deviceName)) {
            if (!this.mUsbManager.hasPermission(usbDevice)) {
                this.mUsbManager.requestPermission(usbDevice, this.mPermissionIntent);
                this.mRequestPermissionDevices.add(deviceName);
            } else {
                UsbDeviceConnection connection = this.mUsbManager.openDevice(usbDevice);
                if (connection != null) {
                    MtpDevice mtpDevice = new MtpDevice(usbDevice);
                    if (mtpDevice.open(connection)) {
                        this.mDevices.put(usbDevice.getDeviceName(), mtpDevice);
                        return mtpDevice;
                    }
                    this.mIgnoredDevices.add(deviceName);
                } else {
                    this.mIgnoredDevices.add(deviceName);
                }
            }
        }
        return null;
    }

    public void close() {
        this.mContext.unregisterReceiver(this.mUsbReceiver);
    }

    public void addListener(Listener listener) {
        synchronized (this.mDevices) {
            if (!this.mListeners.contains(listener)) {
                this.mListeners.add(listener);
            }
        }
    }

    public List<MtpDevice> getDeviceList() {
        ArrayList arrayList;
        synchronized (this.mDevices) {
            for (UsbDevice usbDevice : this.mUsbManager.getDeviceList().values()) {
                if (this.mDevices.get(usbDevice.getDeviceName()) == null) {
                    openDeviceLocked(usbDevice);
                }
            }
            arrayList = new ArrayList(this.mDevices.values());
        }
        return arrayList;
    }
}
