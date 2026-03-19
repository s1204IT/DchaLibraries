package com.android.server.usb;

import android.R;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.internal.util.dump.DumpUtils;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.report.TextReportCanvas;
import com.android.server.usb.descriptors.tree.UsbDescriptorsTree;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class UsbHostManager {
    private static final boolean DEBUG = false;
    private static final int LINUX_FOUNDATION_VID = 7531;
    private static final int MAX_CONNECT_RECORDS = 32;
    private static final String TAG = UsbHostManager.class.getSimpleName();
    static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
    private final Context mContext;

    @GuardedBy("mSettingsLock")
    private UsbProfileGroupSettingsManager mCurrentSettings;
    private final String[] mHostBlacklist;
    private ConnectionRecord mLastConnect;
    private int mNumConnects;
    private final UsbSettingsManager mSettingsManager;
    private final UsbAlsaManager mUsbAlsaManager;

    @GuardedBy("mHandlerLock")
    private ComponentName mUsbDeviceConnectionHandler;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final HashMap<String, UsbDevice> mDevices = new HashMap<>();
    private Object mSettingsLock = new Object();
    private Object mHandlerLock = new Object();
    private final LinkedList<ConnectionRecord> mConnections = new LinkedList<>();

    private native void monitorUsbHostBus();

    private native ParcelFileDescriptor nativeOpenDevice(String str);

    class ConnectionRecord {
        static final int CONNECT = 0;
        static final int CONNECT_BADDEVICE = 2;
        static final int CONNECT_BADPARSE = 1;
        static final int DISCONNECT = -1;
        private static final int kDumpBytesPerLine = 16;
        final byte[] mDescriptors;
        String mDeviceAddress;
        final int mMode;
        long mTimestamp = System.currentTimeMillis();

        ConnectionRecord(String str, int i, byte[] bArr) {
            this.mDeviceAddress = str;
            this.mMode = i;
            this.mDescriptors = bArr;
        }

        private String formatTime() {
            return new StringBuilder(UsbHostManager.sFormat.format(new Date(this.mTimestamp))).toString();
        }

        void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
            long jStart = dualDumpOutputStream.start(str, j);
            dualDumpOutputStream.write("device_address", 1138166333441L, this.mDeviceAddress);
            dualDumpOutputStream.write("mode", 1159641169922L, this.mMode);
            dualDumpOutputStream.write(WatchlistLoggingHandler.WatchlistEventKeys.TIMESTAMP, 1112396529667L, this.mTimestamp);
            if (this.mMode != -1) {
                UsbDescriptorParser usbDescriptorParser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                UsbDeviceDescriptor deviceDescriptor = usbDescriptorParser.getDeviceDescriptor();
                dualDumpOutputStream.write("manufacturer", 1120986464260L, deviceDescriptor.getVendorID());
                dualDumpOutputStream.write("product", 1120986464261L, deviceDescriptor.getProductID());
                long jStart2 = dualDumpOutputStream.start("is_headset", 1146756268038L);
                dualDumpOutputStream.write("in", 1133871366145L, usbDescriptorParser.isInputHeadset());
                dualDumpOutputStream.write("out", 1133871366146L, usbDescriptorParser.isOutputHeadset());
                dualDumpOutputStream.end(jStart2);
            }
            dualDumpOutputStream.end(jStart);
        }

        void dumpShort(IndentingPrintWriter indentingPrintWriter) {
            if (this.mMode != -1) {
                indentingPrintWriter.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                UsbDescriptorParser usbDescriptorParser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                UsbDeviceDescriptor deviceDescriptor = usbDescriptorParser.getDeviceDescriptor();
                indentingPrintWriter.println("manfacturer:0x" + Integer.toHexString(deviceDescriptor.getVendorID()) + " product:" + Integer.toHexString(deviceDescriptor.getProductID()));
                indentingPrintWriter.println("isHeadset[in: " + usbDescriptorParser.isInputHeadset() + " , out: " + usbDescriptorParser.isOutputHeadset() + "]");
                return;
            }
            indentingPrintWriter.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }

        void dumpTree(IndentingPrintWriter indentingPrintWriter) {
            if (this.mMode != -1) {
                indentingPrintWriter.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                UsbDescriptorParser usbDescriptorParser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                StringBuilder sb = new StringBuilder();
                UsbDescriptorsTree usbDescriptorsTree = new UsbDescriptorsTree();
                usbDescriptorsTree.parse(usbDescriptorParser);
                usbDescriptorsTree.report(new TextReportCanvas(usbDescriptorParser, sb));
                sb.append("isHeadset[in: " + usbDescriptorParser.isInputHeadset() + " , out: " + usbDescriptorParser.isOutputHeadset() + "]");
                indentingPrintWriter.println(sb.toString());
                return;
            }
            indentingPrintWriter.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }

        void dumpList(IndentingPrintWriter indentingPrintWriter) {
            if (this.mMode != -1) {
                indentingPrintWriter.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                UsbDescriptorParser usbDescriptorParser = new UsbDescriptorParser(this.mDeviceAddress, this.mDescriptors);
                StringBuilder sb = new StringBuilder();
                TextReportCanvas textReportCanvas = new TextReportCanvas(usbDescriptorParser, sb);
                Iterator<UsbDescriptor> it = usbDescriptorParser.getDescriptors().iterator();
                while (it.hasNext()) {
                    it.next().report(textReportCanvas);
                }
                indentingPrintWriter.println(sb.toString());
                indentingPrintWriter.println("isHeadset[in: " + usbDescriptorParser.isInputHeadset() + " , out: " + usbDescriptorParser.isOutputHeadset() + "]");
                return;
            }
            indentingPrintWriter.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }

        void dumpRaw(IndentingPrintWriter indentingPrintWriter) {
            if (this.mMode != -1) {
                indentingPrintWriter.println(formatTime() + " Connect " + this.mDeviceAddress + " mode:" + this.mMode);
                int length = this.mDescriptors.length;
                StringBuilder sb = new StringBuilder();
                sb.append("Raw Descriptors ");
                sb.append(length);
                sb.append(" bytes");
                indentingPrintWriter.println(sb.toString());
                int i = 0;
                int i2 = 0;
                while (i < length / 16) {
                    StringBuilder sb2 = new StringBuilder();
                    int i3 = i2;
                    int i4 = 0;
                    while (i4 < 16) {
                        sb2.append("0x");
                        sb2.append(String.format("0x%02X", Byte.valueOf(this.mDescriptors[i3])));
                        sb2.append(" ");
                        i4++;
                        i3++;
                    }
                    indentingPrintWriter.println(sb2.toString());
                    i++;
                    i2 = i3;
                }
                StringBuilder sb3 = new StringBuilder();
                while (i2 < length) {
                    sb3.append("0x");
                    sb3.append(String.format("0x%02X", Byte.valueOf(this.mDescriptors[i2])));
                    sb3.append(" ");
                    i2++;
                }
                indentingPrintWriter.println(sb3.toString());
                return;
            }
            indentingPrintWriter.println(formatTime() + " Disconnect " + this.mDeviceAddress);
        }
    }

    public UsbHostManager(Context context, UsbAlsaManager usbAlsaManager, UsbSettingsManager usbSettingsManager) {
        this.mContext = context;
        this.mHostBlacklist = context.getResources().getStringArray(R.array.config_displayCutoutSideOverrideArray);
        this.mUsbAlsaManager = usbAlsaManager;
        this.mSettingsManager = usbSettingsManager;
        String string = context.getResources().getString(R.string.accessibility_system_action_dismiss_notification_shade);
        if (!TextUtils.isEmpty(string)) {
            setUsbDeviceConnectionHandler(ComponentName.unflattenFromString(string));
        }
    }

    public void setCurrentUserSettings(UsbProfileGroupSettingsManager usbProfileGroupSettingsManager) {
        synchronized (this.mSettingsLock) {
            this.mCurrentSettings = usbProfileGroupSettingsManager;
        }
    }

    private UsbProfileGroupSettingsManager getCurrentUserSettings() {
        UsbProfileGroupSettingsManager usbProfileGroupSettingsManager;
        synchronized (this.mSettingsLock) {
            usbProfileGroupSettingsManager = this.mCurrentSettings;
        }
        return usbProfileGroupSettingsManager;
    }

    public void setUsbDeviceConnectionHandler(ComponentName componentName) {
        synchronized (this.mHandlerLock) {
            this.mUsbDeviceConnectionHandler = componentName;
        }
    }

    private ComponentName getUsbDeviceConnectionHandler() {
        ComponentName componentName;
        synchronized (this.mHandlerLock) {
            componentName = this.mUsbDeviceConnectionHandler;
        }
        return componentName;
    }

    private boolean isBlackListed(String str) {
        int length = this.mHostBlacklist.length;
        for (int i = 0; i < length; i++) {
            if (str.startsWith(this.mHostBlacklist[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlackListed(int i, int i2) {
        if (i == 9) {
            return true;
        }
        return i == 3 && i2 == 1;
    }

    private void addConnectionRecord(String str, int i, byte[] bArr) {
        this.mNumConnects++;
        while (this.mConnections.size() >= 32) {
            this.mConnections.removeFirst();
        }
        ConnectionRecord connectionRecord = new ConnectionRecord(str, i, bArr);
        this.mConnections.add(connectionRecord);
        if (i != -1) {
            this.mLastConnect = connectionRecord;
        }
    }

    private void logUsbDevice(UsbDescriptorParser usbDescriptorParser) {
        String deviceReleaseString;
        String serialString;
        String mfgString;
        String productString;
        int vendorID;
        int productID;
        UsbDeviceDescriptor deviceDescriptor = usbDescriptorParser.getDeviceDescriptor();
        if (deviceDescriptor == null) {
            deviceReleaseString = "<unknown>";
            serialString = "<unknown>";
            mfgString = "<unknown>";
            productString = "<unknown>";
            vendorID = 0;
            productID = 0;
        } else {
            vendorID = deviceDescriptor.getVendorID();
            productID = deviceDescriptor.getProductID();
            mfgString = deviceDescriptor.getMfgString(usbDescriptorParser);
            productString = deviceDescriptor.getProductString(usbDescriptorParser);
            deviceReleaseString = deviceDescriptor.getDeviceReleaseString();
            serialString = deviceDescriptor.getSerialString(usbDescriptorParser);
        }
        if (vendorID == LINUX_FOUNDATION_VID) {
            return;
        }
        boolean zHasAudioInterface = usbDescriptorParser.hasAudioInterface();
        boolean zHasHIDInterface = usbDescriptorParser.hasHIDInterface();
        boolean zHasStorageInterface = usbDescriptorParser.hasStorageInterface();
        Slog.d(TAG, (("USB device attached: " + String.format("vidpid %04x:%04x", Integer.valueOf(vendorID), Integer.valueOf(productID))) + String.format(" mfg/product/ver/serial %s/%s/%s/%s", mfgString, productString, deviceReleaseString, serialString)) + String.format(" hasAudio/HID/Storage: %b/%b/%b", Boolean.valueOf(zHasAudioInterface), Boolean.valueOf(zHasHIDInterface), Boolean.valueOf(zHasStorageInterface)));
    }

    private boolean usbDeviceAdded(String str, int i, int i2, byte[] bArr) {
        if (isBlackListed(str)) {
            return false;
        }
        UsbDescriptorParser usbDescriptorParser = new UsbDescriptorParser(str, bArr);
        logUsbDevice(usbDescriptorParser);
        if (isBlackListed(i, i2)) {
            return false;
        }
        synchronized (this.mLock) {
            if (this.mDevices.get(str) != null) {
                Slog.w(TAG, "device already on mDevices list: " + str);
                return false;
            }
            UsbDevice androidUsbDevice = usbDescriptorParser.toAndroidUsbDevice();
            if (androidUsbDevice == null) {
                Slog.e(TAG, "Couldn't create UsbDevice object.");
                addConnectionRecord(str, 2, usbDescriptorParser.getRawDescriptors());
            } else {
                this.mDevices.put(str, androidUsbDevice);
                Slog.d(TAG, "Added device " + androidUsbDevice);
                ComponentName usbDeviceConnectionHandler = getUsbDeviceConnectionHandler();
                if (usbDeviceConnectionHandler == null) {
                    getCurrentUserSettings().deviceAttached(androidUsbDevice);
                } else {
                    getCurrentUserSettings().deviceAttachedForFixedHandler(androidUsbDevice, usbDeviceConnectionHandler);
                }
                this.mUsbAlsaManager.usbDeviceAdded(str, androidUsbDevice, usbDescriptorParser);
                if (isUvcDevice(androidUsbDevice)) {
                    SystemProperties.set("front_camera_version", "2");
                }
                addConnectionRecord(str, 0, usbDescriptorParser.getRawDescriptors());
            }
            return true;
        }
    }

    private void usbDeviceRemoved(String str) {
        synchronized (this.mLock) {
            UsbDevice usbDeviceRemove = this.mDevices.remove(str);
            if (usbDeviceRemove != null) {
                Slog.d(TAG, "Removed device at " + str + ": " + usbDeviceRemove.getProductName());
                this.mUsbAlsaManager.usbDeviceRemoved(str);
                this.mSettingsManager.usbDeviceRemoved(usbDeviceRemove);
                getCurrentUserSettings().usbDeviceRemoved(usbDeviceRemove);
                if (isUvcDevice(usbDeviceRemove)) {
                    SystemProperties.set("front_camera_version", "1");
                }
                addConnectionRecord(str, -1, null);
            } else {
                Slog.d(TAG, "Removed device at " + str + " was already gone");
            }
        }
    }

    public void systemReady() {
        synchronized (this.mLock) {
            new Thread(null, new Runnable() {
                @Override
                public final void run() {
                    this.f$0.monitorUsbHostBus();
                }
            }, "UsbService host thread").start();
        }
    }

    public void getDeviceList(Bundle bundle) {
        synchronized (this.mLock) {
            for (String str : this.mDevices.keySet()) {
                bundle.putParcelable(str, this.mDevices.get(str));
            }
        }
    }

    public ParcelFileDescriptor openDevice(String str, UsbUserSettingsManager usbUserSettingsManager, String str2, int i) {
        ParcelFileDescriptor parcelFileDescriptorNativeOpenDevice;
        synchronized (this.mLock) {
            if (isBlackListed(str)) {
                throw new SecurityException("USB device is on a restricted bus");
            }
            UsbDevice usbDevice = this.mDevices.get(str);
            if (usbDevice == null) {
                throw new IllegalArgumentException("device " + str + " does not exist or is restricted");
            }
            usbUserSettingsManager.checkPermission(usbDevice, str2, i);
            parcelFileDescriptorNativeOpenDevice = nativeOpenDevice(str);
        }
        return parcelFileDescriptorNativeOpenDevice;
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        synchronized (this.mHandlerLock) {
            if (this.mUsbDeviceConnectionHandler != null) {
                DumpUtils.writeComponentName(dualDumpOutputStream, "default_usb_host_connection_handler", 1146756268033L, this.mUsbDeviceConnectionHandler);
            }
        }
        synchronized (this.mLock) {
            Iterator<String> it = this.mDevices.keySet().iterator();
            while (it.hasNext()) {
                com.android.internal.usb.DumpUtils.writeDevice(dualDumpOutputStream, "devices", 2246267895810L, this.mDevices.get(it.next()));
            }
            dualDumpOutputStream.write("num_connects", 1120986464259L, this.mNumConnects);
            Iterator<ConnectionRecord> it2 = this.mConnections.iterator();
            while (it2.hasNext()) {
                it2.next().dump(dualDumpOutputStream, "connections", 2246267895812L);
            }
        }
        dualDumpOutputStream.end(jStart);
    }

    public void dumpDescriptors(IndentingPrintWriter indentingPrintWriter, String[] strArr) {
        if (this.mLastConnect != null) {
            indentingPrintWriter.println("Last Connected USB Device:");
            if (strArr.length <= 1 || strArr[1].equals("-dump-short")) {
                this.mLastConnect.dumpShort(indentingPrintWriter);
                return;
            }
            if (strArr[1].equals("-dump-tree")) {
                this.mLastConnect.dumpTree(indentingPrintWriter);
                return;
            } else if (strArr[1].equals("-dump-list")) {
                this.mLastConnect.dumpList(indentingPrintWriter);
                return;
            } else {
                if (strArr[1].equals("-dump-raw")) {
                    this.mLastConnect.dumpRaw(indentingPrintWriter);
                    return;
                }
                return;
            }
        }
        indentingPrintWriter.println("No USB Devices have been connected.");
    }

    private static boolean isUvcDevice(UsbDevice usbDevice) {
        int interfaceCount = usbDevice.getInterfaceCount();
        for (int i = 0; i < interfaceCount; i++) {
            if (usbDevice.getInterface(i).getInterfaceClass() == 14) {
                return true;
            }
        }
        return false;
    }
}
