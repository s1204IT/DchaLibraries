package com.android.mtp;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.mtp.MtpDevice;
import android.mtp.MtpDeviceInfo;
import android.mtp.MtpEvent;
import android.mtp.MtpObjectInfo;
import android.mtp.MtpStorageInfo;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

class MtpManager {
    private final SparseArray<MtpDevice> mDevices = new SparseArray<>();
    private final UsbManager mManager;

    MtpManager(Context context) {
        this.mManager = (UsbManager) context.getSystemService("usb");
    }

    synchronized MtpDeviceRecord openDevice(int i) throws IOException {
        UsbDevice usbDevice;
        usbDevice = null;
        Iterator<UsbDevice> it = this.mManager.getDeviceList().values().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            UsbDevice next = it.next();
            if (next.getDeviceId() == i) {
                usbDevice = next;
                break;
            }
        }
        ensureNotNull(usbDevice, "Not found USB device: " + i);
        if (!this.mManager.hasPermission(usbDevice)) {
            this.mManager.grantPermission(usbDevice);
            if (!this.mManager.hasPermission(usbDevice)) {
                throw new IOException("Failed to grant a device permission.");
            }
        }
        MtpDevice mtpDevice = new MtpDevice(usbDevice);
        if (!mtpDevice.open((UsbDeviceConnection) ensureNotNull(this.mManager.openDevice(usbDevice), "Failed to open a USB connection."))) {
            throw new BusyDeviceException();
        }
        this.mDevices.put(i, mtpDevice);
        return createDeviceRecord(usbDevice);
    }

    synchronized void closeDevice(int i) throws IOException {
        getDevice(i).close();
        this.mDevices.remove(i);
    }

    synchronized MtpDeviceRecord[] getDevices() {
        ArrayList arrayList;
        arrayList = new ArrayList();
        for (UsbDevice usbDevice : this.mManager.getDeviceList().values()) {
            if (isMtpDevice(usbDevice)) {
                arrayList.add(createDeviceRecord(usbDevice));
            }
        }
        return (MtpDeviceRecord[]) arrayList.toArray(new MtpDeviceRecord[arrayList.size()]);
    }

    MtpObjectInfo getObjectInfo(int i, int i2) throws IOException {
        MtpObjectInfo mtpObjectInfo;
        MtpDevice device = getDevice(i);
        synchronized (device) {
            mtpObjectInfo = (MtpObjectInfo) ensureNotNull(device.getObjectInfo(i2), "Failed to get object info: " + i2);
        }
        return mtpObjectInfo;
    }

    int[] getObjectHandles(int i, int i2, int i3) throws IOException {
        int[] iArr;
        MtpDevice device = getDevice(i);
        synchronized (device) {
            iArr = (int[]) ensureNotNull(device.getObjectHandles(i2, 0, i3), "Failed to fetch object handles.");
        }
        return iArr;
    }

    long getPartialObject(int i, int i2, long j, long j2, byte[] bArr) throws IOException {
        long partialObject;
        MtpDevice device = getDevice(i);
        synchronized (device) {
            partialObject = device.getPartialObject(i2, j, j2, bArr);
        }
        return partialObject;
    }

    long getPartialObject64(int i, int i2, long j, long j2, byte[] bArr) throws IOException {
        long partialObject64;
        MtpDevice device = getDevice(i);
        synchronized (device) {
            partialObject64 = device.getPartialObject64(i2, j, j2, bArr);
        }
        return partialObject64;
    }

    byte[] getThumbnail(int i, int i2) throws IOException {
        byte[] bArr;
        MtpDevice device = getDevice(i);
        synchronized (device) {
            bArr = (byte[]) ensureNotNull(device.getThumbnail(i2), "Failed to obtain thumbnail bytes");
        }
        return bArr;
    }

    void deleteDocument(int i, int i2) throws IOException {
        MtpDevice device = getDevice(i);
        synchronized (device) {
            if (!device.deleteObject(i2)) {
                throw new IOException("Failed to delete document");
            }
        }
    }

    int createDocument(int i, MtpObjectInfo mtpObjectInfo, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        int objectHandle;
        MtpDevice device = getDevice(i);
        synchronized (device) {
            MtpObjectInfo mtpObjectInfoSendObjectInfo = device.sendObjectInfo(mtpObjectInfo);
            if (mtpObjectInfoSendObjectInfo == null) {
                throw new SendObjectInfoFailure();
            }
            if (mtpObjectInfo.getFormat() != 12289 && !device.sendObject(mtpObjectInfoSendObjectInfo.getObjectHandle(), mtpObjectInfoSendObjectInfo.getCompressedSize(), parcelFileDescriptor)) {
                throw new IOException("Failed to send contents of a document");
            }
            objectHandle = mtpObjectInfoSendObjectInfo.getObjectHandle();
        }
        return objectHandle;
    }

    void importFile(int i, int i2, ParcelFileDescriptor parcelFileDescriptor) throws IOException {
        MtpDevice device = getDevice(i);
        synchronized (device) {
            if (!device.importFile(i2, parcelFileDescriptor)) {
                throw new IOException("Failed to import file to FD");
            }
        }
    }

    @VisibleForTesting
    MtpEvent readEvent(int i, CancellationSignal cancellationSignal) throws IOException {
        return getDevice(i).readEvent(cancellationSignal);
    }

    long getObjectSizeLong(int i, int i2, int i3) throws IOException {
        return getDevice(i).getObjectSizeLong(i2, i3);
    }

    private synchronized MtpDevice getDevice(int i) throws IOException {
        return (MtpDevice) ensureNotNull(this.mDevices.get(i), "USB device " + i + " is not opened.");
    }

    private MtpRoot[] getRoots(int i) throws IOException {
        MtpRoot[] mtpRootArr;
        MtpDevice device = getDevice(i);
        synchronized (device) {
            int[] iArr = (int[]) ensureNotNull(device.getStorageIds(), "Failed to obtain storage IDs.");
            ArrayList arrayList = new ArrayList();
            for (int i2 : iArr) {
                MtpStorageInfo storageInfo = device.getStorageInfo(i2);
                if (storageInfo != null) {
                    arrayList.add(new MtpRoot(device.getDeviceId(), storageInfo));
                }
            }
            mtpRootArr = (MtpRoot[]) arrayList.toArray(new MtpRoot[arrayList.size()]);
        }
        return mtpRootArr;
    }

    private MtpDeviceRecord createDeviceRecord(UsbDevice usbDevice) {
        MtpRoot[] roots;
        int[] eventsSupported;
        int[] iArr;
        int[] iArr2;
        MtpRoot[] mtpRootArr;
        MtpDevice mtpDevice = this.mDevices.get(usbDevice.getDeviceId());
        boolean z = mtpDevice != null;
        String productName = usbDevice.getProductName();
        int[] operationsSupported = null;
        if (z) {
            try {
                roots = getRoots(usbDevice.getDeviceId());
            } catch (IOException e) {
                Log.e("MtpDocumentsProvider", "Failed to open device", e);
                roots = new MtpRoot[0];
            }
            MtpDeviceInfo deviceInfo = mtpDevice.getDeviceInfo();
            if (deviceInfo != null) {
                operationsSupported = deviceInfo.getOperationsSupported();
                eventsSupported = deviceInfo.getEventsSupported();
            } else {
                eventsSupported = null;
            }
            iArr = eventsSupported;
            iArr2 = operationsSupported;
            mtpRootArr = roots;
        } else {
            mtpRootArr = new MtpRoot[0];
            iArr2 = null;
            iArr = null;
        }
        return new MtpDeviceRecord(usbDevice.getDeviceId(), productName, usbDevice.getSerialNumber(), z, mtpRootArr, iArr2, iArr);
    }

    static boolean isMtpDevice(UsbDevice usbDevice) {
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == 6 && usbInterface.getInterfaceSubclass() == 1 && usbInterface.getInterfaceProtocol() == 1) {
                return true;
            }
            if (usbInterface.getInterfaceClass() == 255 && usbInterface.getInterfaceSubclass() == 255 && usbInterface.getInterfaceProtocol() == 0 && "MTP".equals(usbInterface.getName())) {
                return true;
            }
        }
        return false;
    }

    private static <T> T ensureNotNull(T t, String str) throws IOException {
        if (t != null) {
            return t;
        }
        throw new IOException(str);
    }
}
