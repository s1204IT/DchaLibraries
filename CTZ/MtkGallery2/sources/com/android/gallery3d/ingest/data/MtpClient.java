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
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
                MtpDevice mtpDeviceOpenDeviceLocked = (MtpDevice) MtpClient.this.mDevices.get(deviceName);
                if ("android.hardware.usb.action.USB_DEVICE_ATTACHED".equals(action)) {
                    if (mtpDeviceOpenDeviceLocked == null) {
                        mtpDeviceOpenDeviceLocked = MtpClient.this.openDeviceLocked(usbDevice);
                    }
                    if (mtpDeviceOpenDeviceLocked != null) {
                        Iterator it = MtpClient.this.mListeners.iterator();
                        while (it.hasNext()) {
                            ((Listener) it.next()).deviceAdded(mtpDeviceOpenDeviceLocked);
                        }
                    }
                } else if ("android.hardware.usb.action.USB_DEVICE_DETACHED".equals(action)) {
                    if (mtpDeviceOpenDeviceLocked != null) {
                        MtpClient.this.mDevices.remove(deviceName);
                        MtpClient.this.mRequestPermissionDevices.remove(deviceName);
                        MtpClient.this.mIgnoredDevices.remove(deviceName);
                        Iterator it2 = MtpClient.this.mListeners.iterator();
                        while (it2.hasNext()) {
                            ((Listener) it2.next()).deviceRemoved(mtpDeviceOpenDeviceLocked);
                        }
                    }
                } else if ("com.android.gallery3d.ingest.action.USB_PERMISSION".equals(action)) {
                    MtpClient.this.mRequestPermissionDevices.remove(deviceName);
                    boolean booleanExtra = intent.getBooleanExtra("permission", false);
                    Log.d("Gallery2/MtpClient", "ACTION_USB_PERMISSION: " + booleanExtra);
                    if (!booleanExtra) {
                        MtpClient.this.mIgnoredDevices.add(deviceName);
                    } else {
                        if (mtpDeviceOpenDeviceLocked == null) {
                            mtpDeviceOpenDeviceLocked = MtpClient.this.openDeviceLocked(usbDevice);
                        }
                        if (mtpDeviceOpenDeviceLocked != null) {
                            Iterator it3 = MtpClient.this.mListeners.iterator();
                            while (it3.hasNext()) {
                                ((Listener) it3.next()).deviceAdded(mtpDeviceOpenDeviceLocked);
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

    public static boolean isCamera(UsbDevice usbDevice) {
        int interfaceCount = usbDevice.getInterfaceCount();
        for (int i = 0; i < interfaceCount; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == 6 && usbInterface.getInterfaceSubclass() == 1 && usbInterface.getInterfaceProtocol() == 1) {
                return true;
            }
        }
        return false;
    }

    public MtpClient(Context context) {
        this.mContext = context;
        this.mUsbManager = (UsbManager) context.getSystemService("usb");
        this.mPermissionIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent("com.android.gallery3d.ingest.action.USB_PERMISSION"), 0);
        IntentFilter intentFilter = new IntentFilter("com.android.gallery3d.ingest.action.USB_PERMISSION");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intentFilter.addAction("com.android.gallery3d.ingest.action.USB_PERMISSION");
        context.registerReceiver(this.mUsbReceiver, intentFilter);
    }

    private MtpDevice openDeviceLocked(UsbDevice usbDevice) {
        String deviceName = usbDevice.getDeviceName();
        if (isCamera(usbDevice) && !this.mIgnoredDevices.contains(deviceName) && !this.mRequestPermissionDevices.contains(deviceName)) {
            if (!this.mUsbManager.hasPermission(usbDevice)) {
                this.mUsbManager.requestPermission(usbDevice, this.mPermissionIntent);
                this.mRequestPermissionDevices.add(deviceName);
                return null;
            }
            UsbDeviceConnection usbDeviceConnectionOpenDevice = this.mUsbManager.openDevice(usbDevice);
            if (usbDeviceConnectionOpenDevice != null) {
                MtpDevice mtpDevice = new MtpDevice(usbDevice);
                if (mtpDevice.open(usbDeviceConnectionOpenDevice)) {
                    this.mDevices.put(usbDevice.getDeviceName(), mtpDevice);
                    return mtpDevice;
                }
                this.mIgnoredDevices.add(deviceName);
                return null;
            }
            this.mIgnoredDevices.add(deviceName);
            return null;
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
