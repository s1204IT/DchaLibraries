package com.android.server.usb;

import android.R;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.os.Binder;
import android.os.Environment;
import android.os.UserHandle;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.Xml;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class UsbSettingsManager {
    private static final boolean DEBUG = false;
    private static final String TAG = "UsbSettingsManager";
    private static final File sSingleUserSettingsFile = new File("/data/system/usb_device_manager.xml");
    private final Context mContext;
    private final boolean mDisablePermissionDialogs;
    private final PackageManager mPackageManager;
    private final AtomicFile mSettingsFile;
    private final UserHandle mUser;
    private final Context mUserContext;
    private final HashMap<String, SparseBooleanArray> mDevicePermissionMap = new HashMap<>();
    private final HashMap<UsbAccessory, SparseBooleanArray> mAccessoryPermissionMap = new HashMap<>();
    private final HashMap<DeviceFilter, String> mDevicePreferenceMap = new HashMap<>();
    private final HashMap<AccessoryFilter, String> mAccessoryPreferenceMap = new HashMap<>();
    private final Object mLock = new Object();
    MyPackageMonitor mPackageMonitor = new MyPackageMonitor();

    private static class DeviceFilter {
        public final int mClass;
        public final String mManufacturerName;
        public final int mProductId;
        public final String mProductName;
        public final int mProtocol;
        public final String mSerialNumber;
        public final int mSubclass;
        public final int mVendorId;

        public DeviceFilter(int vid, int pid, int clasz, int subclass, int protocol, String manufacturer, String product, String serialnum) {
            this.mVendorId = vid;
            this.mProductId = pid;
            this.mClass = clasz;
            this.mSubclass = subclass;
            this.mProtocol = protocol;
            this.mManufacturerName = manufacturer;
            this.mProductName = product;
            this.mSerialNumber = serialnum;
        }

        public DeviceFilter(UsbDevice device) {
            this.mVendorId = device.getVendorId();
            this.mProductId = device.getProductId();
            this.mClass = device.getDeviceClass();
            this.mSubclass = device.getDeviceSubclass();
            this.mProtocol = device.getDeviceProtocol();
            this.mManufacturerName = device.getManufacturerName();
            this.mProductName = device.getProductName();
            this.mSerialNumber = device.getSerialNumber();
        }

        public static DeviceFilter read(XmlPullParser parser) throws XmlPullParserException, IOException {
            int vendorId = -1;
            int productId = -1;
            int deviceClass = -1;
            int deviceSubclass = -1;
            int deviceProtocol = -1;
            String manufacturerName = null;
            String productName = null;
            String serialNumber = null;
            int count = parser.getAttributeCount();
            for (int i = 0; i < count; i++) {
                String name = parser.getAttributeName(i);
                String value = parser.getAttributeValue(i);
                if ("manufacturer-name".equals(name)) {
                    manufacturerName = value;
                } else if ("product-name".equals(name)) {
                    productName = value;
                } else if ("serial-number".equals(name)) {
                    serialNumber = value;
                } else {
                    int radix = 10;
                    if (value != null && value.length() > 2 && value.charAt(0) == '0' && (value.charAt(1) == 'x' || value.charAt(1) == 'X')) {
                        radix = 16;
                        value = value.substring(2);
                    }
                    try {
                        int intValue = Integer.parseInt(value, radix);
                        if ("vendor-id".equals(name)) {
                            vendorId = intValue;
                        } else if ("product-id".equals(name)) {
                            productId = intValue;
                        } else if ("class".equals(name)) {
                            deviceClass = intValue;
                        } else if ("subclass".equals(name)) {
                            deviceSubclass = intValue;
                        } else if ("protocol".equals(name)) {
                            deviceProtocol = intValue;
                        }
                    } catch (NumberFormatException e) {
                        Slog.e(UsbSettingsManager.TAG, "invalid number for field " + name, e);
                    }
                }
            }
            return new DeviceFilter(vendorId, productId, deviceClass, deviceSubclass, deviceProtocol, manufacturerName, productName, serialNumber);
        }

        public void write(XmlSerializer serializer) throws IOException {
            serializer.startTag(null, "usb-device");
            if (this.mVendorId != -1) {
                serializer.attribute(null, "vendor-id", Integer.toString(this.mVendorId));
            }
            if (this.mProductId != -1) {
                serializer.attribute(null, "product-id", Integer.toString(this.mProductId));
            }
            if (this.mClass != -1) {
                serializer.attribute(null, "class", Integer.toString(this.mClass));
            }
            if (this.mSubclass != -1) {
                serializer.attribute(null, "subclass", Integer.toString(this.mSubclass));
            }
            if (this.mProtocol != -1) {
                serializer.attribute(null, "protocol", Integer.toString(this.mProtocol));
            }
            if (this.mManufacturerName != null) {
                serializer.attribute(null, "manufacturer-name", this.mManufacturerName);
            }
            if (this.mProductName != null) {
                serializer.attribute(null, "product-name", this.mProductName);
            }
            if (this.mSerialNumber != null) {
                serializer.attribute(null, "serial-number", this.mSerialNumber);
            }
            serializer.endTag(null, "usb-device");
        }

        private boolean matches(int clasz, int subclass, int protocol) {
            if ((this.mClass == -1 || clasz == this.mClass) && ((this.mSubclass == -1 || subclass == this.mSubclass) && (this.mProtocol == -1 || protocol == this.mProtocol))) {
                return true;
            }
            return UsbSettingsManager.DEBUG;
        }

        public boolean matches(UsbDevice device) {
            if (this.mVendorId != -1 && device.getVendorId() != this.mVendorId) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mProductId != -1 && device.getProductId() != this.mProductId) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mManufacturerName != null && device.getManufacturerName() == null) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mProductName != null && device.getProductName() == null) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mSerialNumber != null && device.getSerialNumber() == null) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mManufacturerName != null && device.getManufacturerName() != null && !this.mManufacturerName.equals(device.getManufacturerName())) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mProductName != null && device.getProductName() != null && !this.mProductName.equals(device.getProductName())) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mSerialNumber != null && device.getSerialNumber() != null && !this.mSerialNumber.equals(device.getSerialNumber())) {
                return UsbSettingsManager.DEBUG;
            }
            if (matches(device.getDeviceClass(), device.getDeviceSubclass(), device.getDeviceProtocol())) {
                return true;
            }
            int count = device.getInterfaceCount();
            for (int i = 0; i < count; i++) {
                UsbInterface intf = device.getInterface(i);
                if (matches(intf.getInterfaceClass(), intf.getInterfaceSubclass(), intf.getInterfaceProtocol())) {
                    return true;
                }
            }
            return UsbSettingsManager.DEBUG;
        }

        public boolean matches(DeviceFilter f) {
            return (this.mVendorId == -1 || f.mVendorId == this.mVendorId) ? (this.mProductId == -1 || f.mProductId == this.mProductId) ? (f.mManufacturerName == null || this.mManufacturerName != null) ? (f.mProductName == null || this.mProductName != null) ? (f.mSerialNumber == null || this.mSerialNumber != null) ? (this.mManufacturerName == null || f.mManufacturerName == null || this.mManufacturerName.equals(f.mManufacturerName)) ? (this.mProductName == null || f.mProductName == null || this.mProductName.equals(f.mProductName)) ? (this.mSerialNumber == null || f.mSerialNumber == null || this.mSerialNumber.equals(f.mSerialNumber)) ? matches(f.mClass, f.mSubclass, f.mProtocol) : UsbSettingsManager.DEBUG : UsbSettingsManager.DEBUG : UsbSettingsManager.DEBUG : UsbSettingsManager.DEBUG : UsbSettingsManager.DEBUG : UsbSettingsManager.DEBUG : UsbSettingsManager.DEBUG : UsbSettingsManager.DEBUG;
        }

        public boolean equals(Object obj) {
            if (this.mVendorId == -1 || this.mProductId == -1 || this.mClass == -1 || this.mSubclass == -1 || this.mProtocol == -1) {
                return UsbSettingsManager.DEBUG;
            }
            if (obj instanceof DeviceFilter) {
                DeviceFilter filter = (DeviceFilter) obj;
                if (filter.mVendorId != this.mVendorId || filter.mProductId != this.mProductId || filter.mClass != this.mClass || filter.mSubclass != this.mSubclass || filter.mProtocol != this.mProtocol) {
                    return UsbSettingsManager.DEBUG;
                }
                if ((filter.mManufacturerName != null && this.mManufacturerName == null) || ((filter.mManufacturerName == null && this.mManufacturerName != null) || ((filter.mProductName != null && this.mProductName == null) || ((filter.mProductName == null && this.mProductName != null) || ((filter.mSerialNumber != null && this.mSerialNumber == null) || (filter.mSerialNumber == null && this.mSerialNumber != null)))))) {
                    return UsbSettingsManager.DEBUG;
                }
                if ((filter.mManufacturerName == null || this.mManufacturerName == null || this.mManufacturerName.equals(filter.mManufacturerName)) && ((filter.mProductName == null || this.mProductName == null || this.mProductName.equals(filter.mProductName)) && (filter.mSerialNumber == null || this.mSerialNumber == null || this.mSerialNumber.equals(filter.mSerialNumber)))) {
                    return true;
                }
                return UsbSettingsManager.DEBUG;
            }
            if (!(obj instanceof UsbDevice)) {
                return UsbSettingsManager.DEBUG;
            }
            UsbDevice device = (UsbDevice) obj;
            if (device.getVendorId() != this.mVendorId || device.getProductId() != this.mProductId || device.getDeviceClass() != this.mClass || device.getDeviceSubclass() != this.mSubclass || device.getDeviceProtocol() != this.mProtocol) {
                return UsbSettingsManager.DEBUG;
            }
            if ((this.mManufacturerName != null && device.getManufacturerName() == null) || ((this.mManufacturerName == null && device.getManufacturerName() != null) || ((this.mProductName != null && device.getProductName() == null) || ((this.mProductName == null && device.getProductName() != null) || ((this.mSerialNumber != null && device.getSerialNumber() == null) || (this.mSerialNumber == null && device.getSerialNumber() != null)))))) {
                return UsbSettingsManager.DEBUG;
            }
            if ((device.getManufacturerName() == null || this.mManufacturerName.equals(device.getManufacturerName())) && ((device.getProductName() == null || this.mProductName.equals(device.getProductName())) && (device.getSerialNumber() == null || this.mSerialNumber.equals(device.getSerialNumber())))) {
                return true;
            }
            return UsbSettingsManager.DEBUG;
        }

        public int hashCode() {
            return ((this.mVendorId << 16) | this.mProductId) ^ (((this.mClass << 16) | (this.mSubclass << 8)) | this.mProtocol);
        }

        public String toString() {
            return "DeviceFilter[mVendorId=" + this.mVendorId + ",mProductId=" + this.mProductId + ",mClass=" + this.mClass + ",mSubclass=" + this.mSubclass + ",mProtocol=" + this.mProtocol + ",mManufacturerName=" + this.mManufacturerName + ",mProductName=" + this.mProductName + ",mSerialNumber=" + this.mSerialNumber + "]";
        }
    }

    private static class AccessoryFilter {
        public final String mManufacturer;
        public final String mModel;
        public final String mVersion;

        public AccessoryFilter(String manufacturer, String model, String version) {
            this.mManufacturer = manufacturer;
            this.mModel = model;
            this.mVersion = version;
        }

        public AccessoryFilter(UsbAccessory accessory) {
            this.mManufacturer = accessory.getManufacturer();
            this.mModel = accessory.getModel();
            this.mVersion = accessory.getVersion();
        }

        public static AccessoryFilter read(XmlPullParser parser) throws XmlPullParserException, IOException {
            String manufacturer = null;
            String model = null;
            String version = null;
            int count = parser.getAttributeCount();
            for (int i = 0; i < count; i++) {
                String name = parser.getAttributeName(i);
                String value = parser.getAttributeValue(i);
                if ("manufacturer".equals(name)) {
                    manufacturer = value;
                } else if ("model".equals(name)) {
                    model = value;
                } else if ("version".equals(name)) {
                    version = value;
                }
            }
            return new AccessoryFilter(manufacturer, model, version);
        }

        public void write(XmlSerializer serializer) throws IOException {
            serializer.startTag(null, "usb-accessory");
            if (this.mManufacturer != null) {
                serializer.attribute(null, "manufacturer", this.mManufacturer);
            }
            if (this.mModel != null) {
                serializer.attribute(null, "model", this.mModel);
            }
            if (this.mVersion != null) {
                serializer.attribute(null, "version", this.mVersion);
            }
            serializer.endTag(null, "usb-accessory");
        }

        public boolean matches(UsbAccessory acc) {
            if (this.mManufacturer != null && !acc.getManufacturer().equals(this.mManufacturer)) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mModel != null && !acc.getModel().equals(this.mModel)) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mVersion == null || acc.getVersion().equals(this.mVersion)) {
                return true;
            }
            return UsbSettingsManager.DEBUG;
        }

        public boolean matches(AccessoryFilter f) {
            if (this.mManufacturer != null && !f.mManufacturer.equals(this.mManufacturer)) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mModel != null && !f.mModel.equals(this.mModel)) {
                return UsbSettingsManager.DEBUG;
            }
            if (this.mVersion == null || f.mVersion.equals(this.mVersion)) {
                return true;
            }
            return UsbSettingsManager.DEBUG;
        }

        public boolean equals(Object obj) {
            if (this.mManufacturer == null || this.mModel == null || this.mVersion == null) {
                return UsbSettingsManager.DEBUG;
            }
            if (obj instanceof AccessoryFilter) {
                AccessoryFilter filter = (AccessoryFilter) obj;
                if (this.mManufacturer.equals(filter.mManufacturer) && this.mModel.equals(filter.mModel) && this.mVersion.equals(filter.mVersion)) {
                    return true;
                }
                return UsbSettingsManager.DEBUG;
            }
            if (!(obj instanceof UsbAccessory)) {
                return UsbSettingsManager.DEBUG;
            }
            UsbAccessory accessory = (UsbAccessory) obj;
            if (this.mManufacturer.equals(accessory.getManufacturer()) && this.mModel.equals(accessory.getModel()) && this.mVersion.equals(accessory.getVersion())) {
                return true;
            }
            return UsbSettingsManager.DEBUG;
        }

        public int hashCode() {
            return ((this.mManufacturer == null ? 0 : this.mManufacturer.hashCode()) ^ (this.mModel == null ? 0 : this.mModel.hashCode())) ^ (this.mVersion != null ? this.mVersion.hashCode() : 0);
        }

        public String toString() {
            return "AccessoryFilter[mManufacturer=\"" + this.mManufacturer + "\", mModel=\"" + this.mModel + "\", mVersion=\"" + this.mVersion + "\"]";
        }
    }

    private class MyPackageMonitor extends PackageMonitor {
        private MyPackageMonitor() {
        }

        public void onPackageAdded(String packageName, int uid) {
            UsbSettingsManager.this.handlePackageUpdate(packageName);
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            UsbSettingsManager.this.handlePackageUpdate(packageName);
            return UsbSettingsManager.DEBUG;
        }

        public void onPackageRemoved(String packageName, int uid) {
            UsbSettingsManager.this.clearDefaults(packageName);
        }
    }

    public UsbSettingsManager(Context context, UserHandle user) {
        try {
            this.mUserContext = context.createPackageContextAsUser("android", 0, user);
            this.mContext = context;
            this.mPackageManager = this.mUserContext.getPackageManager();
            this.mUser = user;
            this.mSettingsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(user.getIdentifier()), "usb_device_manager.xml"));
            this.mDisablePermissionDialogs = context.getResources().getBoolean(R.^attr-private.iconfactoryIconSize);
            synchronized (this.mLock) {
                if (UserHandle.OWNER.equals(user)) {
                    upgradeSingleUserLocked();
                }
                readSettingsLocked();
            }
            this.mPackageMonitor.register(this.mUserContext, null, true);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Missing android package");
        }
    }

    private void readPreference(XmlPullParser parser) throws XmlPullParserException, IOException {
        String packageName = null;
        int count = parser.getAttributeCount();
        int i = 0;
        while (true) {
            if (i >= count) {
                break;
            }
            if (!"package".equals(parser.getAttributeName(i))) {
                i++;
            } else {
                packageName = parser.getAttributeValue(i);
                break;
            }
        }
        XmlUtils.nextElement(parser);
        if ("usb-device".equals(parser.getName())) {
            DeviceFilter filter = DeviceFilter.read(parser);
            this.mDevicePreferenceMap.put(filter, packageName);
        } else if ("usb-accessory".equals(parser.getName())) {
            AccessoryFilter filter2 = AccessoryFilter.read(parser);
            this.mAccessoryPreferenceMap.put(filter2, packageName);
        }
        XmlUtils.nextElement(parser);
    }

    private void upgradeSingleUserLocked() throws Throwable {
        FileInputStream fis;
        if (sSingleUserSettingsFile.exists()) {
            this.mDevicePreferenceMap.clear();
            this.mAccessoryPreferenceMap.clear();
            FileInputStream fis2 = null;
            try {
                try {
                    fis = new FileInputStream(sSingleUserSettingsFile);
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e) {
                e = e;
            } catch (XmlPullParserException e2) {
                e = e2;
            }
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(fis, null);
                XmlUtils.nextElement(parser);
                while (parser.getEventType() != 1) {
                    String tagName = parser.getName();
                    if ("preference".equals(tagName)) {
                        readPreference(parser);
                    } else {
                        XmlUtils.nextElement(parser);
                    }
                }
                IoUtils.closeQuietly(fis);
                fis2 = fis;
            } catch (IOException e3) {
                e = e3;
                fis2 = fis;
                Log.wtf(TAG, "Failed to read single-user settings", e);
                IoUtils.closeQuietly(fis2);
            } catch (XmlPullParserException e4) {
                e = e4;
                fis2 = fis;
                Log.wtf(TAG, "Failed to read single-user settings", e);
                IoUtils.closeQuietly(fis2);
            } catch (Throwable th2) {
                th = th2;
                fis2 = fis;
                IoUtils.closeQuietly(fis2);
                throw th;
            }
            writeSettingsLocked();
            sSingleUserSettingsFile.delete();
        }
    }

    private void readSettingsLocked() {
        this.mDevicePreferenceMap.clear();
        this.mAccessoryPreferenceMap.clear();
        FileInputStream stream = null;
        try {
            try {
                stream = this.mSettingsFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, null);
                XmlUtils.nextElement(parser);
                while (parser.getEventType() != 1) {
                    String tagName = parser.getName();
                    if ("preference".equals(tagName)) {
                        readPreference(parser);
                    } else {
                        XmlUtils.nextElement(parser);
                    }
                }
                IoUtils.closeQuietly(stream);
            } catch (FileNotFoundException e) {
                IoUtils.closeQuietly(stream);
            } catch (Exception e2) {
                Slog.e(TAG, "error reading settings file, deleting to start fresh", e2);
                this.mSettingsFile.delete();
                IoUtils.closeQuietly(stream);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(stream);
            throw th;
        }
    }

    private void writeSettingsLocked() {
        FileOutputStream fos = null;
        try {
            fos = this.mSettingsFile.startWrite();
            XmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument((String) null, true);
            fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            fastXmlSerializer.startTag((String) null, "settings");
            for (DeviceFilter filter : this.mDevicePreferenceMap.keySet()) {
                fastXmlSerializer.startTag((String) null, "preference");
                fastXmlSerializer.attribute((String) null, "package", this.mDevicePreferenceMap.get(filter));
                filter.write(fastXmlSerializer);
                fastXmlSerializer.endTag((String) null, "preference");
            }
            for (AccessoryFilter filter2 : this.mAccessoryPreferenceMap.keySet()) {
                fastXmlSerializer.startTag((String) null, "preference");
                fastXmlSerializer.attribute((String) null, "package", this.mAccessoryPreferenceMap.get(filter2));
                filter2.write(fastXmlSerializer);
                fastXmlSerializer.endTag((String) null, "preference");
            }
            fastXmlSerializer.endTag((String) null, "settings");
            fastXmlSerializer.endDocument();
            this.mSettingsFile.finishWrite(fos);
        } catch (IOException e) {
            Slog.e(TAG, "Failed to write settings", e);
            if (fos != null) {
                this.mSettingsFile.failWrite(fos);
            }
        }
    }

    private boolean packageMatchesLocked(ResolveInfo info, String metaDataName, UsbDevice device, UsbAccessory accessory) {
        ActivityInfo ai = info.activityInfo;
        XmlResourceParser parser = null;
        try {
            try {
                XmlResourceParser parser2 = ai.loadXmlMetaData(this.mPackageManager, metaDataName);
                if (parser2 == null) {
                    Slog.w(TAG, "no meta-data for " + info);
                    if (parser2 == null) {
                        return DEBUG;
                    }
                    parser2.close();
                    return DEBUG;
                }
                XmlUtils.nextElement(parser2);
                while (parser2.getEventType() != 1) {
                    String tagName = parser2.getName();
                    if (device != null && "usb-device".equals(tagName)) {
                        DeviceFilter filter = DeviceFilter.read(parser2);
                        if (filter.matches(device)) {
                            if (parser2 != null) {
                                parser2.close();
                            }
                            return true;
                        }
                    } else if (accessory != null && "usb-accessory".equals(tagName)) {
                        AccessoryFilter filter2 = AccessoryFilter.read(parser2);
                        if (filter2.matches(accessory)) {
                            if (parser2 != null) {
                                parser2.close();
                            }
                            return true;
                        }
                    }
                    XmlUtils.nextElement(parser2);
                }
                if (parser2 == null) {
                    return DEBUG;
                }
                parser2.close();
                return DEBUG;
            } catch (Exception e) {
                Slog.w(TAG, "Unable to load component info " + info.toString(), e);
                if (0 == 0) {
                    return DEBUG;
                }
                parser.close();
                return DEBUG;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                parser.close();
            }
            throw th;
        }
    }

    private final ArrayList<ResolveInfo> getDeviceMatchesLocked(UsbDevice device, Intent intent) {
        ArrayList<ResolveInfo> matches = new ArrayList<>();
        List<ResolveInfo> resolveInfos = this.mPackageManager.queryIntentActivities(intent, 128);
        int count = resolveInfos.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            if (packageMatchesLocked(resolveInfo, intent.getAction(), device, null)) {
                matches.add(resolveInfo);
            }
        }
        return matches;
    }

    private final ArrayList<ResolveInfo> getAccessoryMatchesLocked(UsbAccessory accessory, Intent intent) {
        ArrayList<ResolveInfo> matches = new ArrayList<>();
        List<ResolveInfo> resolveInfos = this.mPackageManager.queryIntentActivities(intent, 128);
        int count = resolveInfos.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);
            if (packageMatchesLocked(resolveInfo, intent.getAction(), null, accessory)) {
                matches.add(resolveInfo);
            }
        }
        return matches;
    }

    public void deviceAttached(UsbDevice device) {
        ArrayList<ResolveInfo> matches;
        String defaultPackage;
        Intent intent = new Intent("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intent.putExtra("device", device);
        intent.addFlags(268435456);
        synchronized (this.mLock) {
            matches = getDeviceMatchesLocked(device, intent);
            defaultPackage = this.mDevicePreferenceMap.get(new DeviceFilter(device));
        }
        this.mUserContext.sendBroadcast(intent);
        resolveActivity(intent, matches, defaultPackage, device, null);
    }

    public void deviceDetached(UsbDevice device) {
        this.mDevicePermissionMap.remove(device.getDeviceName());
        Intent intent = new Intent("android.hardware.usb.action.USB_DEVICE_DETACHED");
        intent.putExtra("device", device);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void accessoryAttached(UsbAccessory accessory) {
        ArrayList<ResolveInfo> matches;
        String defaultPackage;
        Intent intent = new Intent("android.hardware.usb.action.USB_ACCESSORY_ATTACHED");
        intent.putExtra("accessory", accessory);
        intent.addFlags(268435456);
        synchronized (this.mLock) {
            matches = getAccessoryMatchesLocked(accessory, intent);
            defaultPackage = this.mAccessoryPreferenceMap.get(new AccessoryFilter(accessory));
        }
        resolveActivity(intent, matches, defaultPackage, null, accessory);
    }

    public void accessoryDetached(UsbAccessory accessory) {
        this.mAccessoryPermissionMap.remove(accessory);
        Intent intent = new Intent("android.hardware.usb.action.USB_ACCESSORY_DETACHED");
        intent.putExtra("accessory", accessory);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void resolveActivity(Intent intent, ArrayList<ResolveInfo> matches, String defaultPackage, UsbDevice device, UsbAccessory accessory) {
        String uri;
        int count = matches.size();
        if (count == 0) {
            if (accessory != null && (uri = accessory.getUri()) != null && uri.length() > 0) {
                Intent dialogIntent = new Intent();
                dialogIntent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbAccessoryUriActivity");
                dialogIntent.addFlags(268435456);
                dialogIntent.putExtra("accessory", accessory);
                dialogIntent.putExtra("uri", uri);
                try {
                    this.mUserContext.startActivityAsUser(dialogIntent, this.mUser);
                    return;
                } catch (ActivityNotFoundException e) {
                    Slog.e(TAG, "unable to start UsbAccessoryUriActivity");
                    return;
                }
            }
            return;
        }
        ResolveInfo defaultRI = null;
        if (count == 1 && defaultPackage == null) {
            ResolveInfo rInfo = matches.get(0);
            if (rInfo.activityInfo != null && rInfo.activityInfo.applicationInfo != null && (rInfo.activityInfo.applicationInfo.flags & 1) != 0) {
                defaultRI = rInfo;
            }
            if (this.mDisablePermissionDialogs) {
                ResolveInfo rInfo2 = matches.get(0);
                if (rInfo2.activityInfo != null) {
                    defaultPackage = rInfo2.activityInfo.packageName;
                }
            }
        }
        if (defaultRI == null && defaultPackage != null) {
            int i = 0;
            while (true) {
                if (i >= count) {
                    break;
                }
                ResolveInfo rInfo3 = matches.get(i);
                if (rInfo3.activityInfo == null || !defaultPackage.equals(rInfo3.activityInfo.packageName)) {
                    i++;
                } else {
                    defaultRI = rInfo3;
                    break;
                }
            }
        }
        if (defaultRI != null) {
            if (device != null) {
                grantDevicePermission(device, defaultRI.activityInfo.applicationInfo.uid);
            } else if (accessory != null) {
                grantAccessoryPermission(accessory, defaultRI.activityInfo.applicationInfo.uid);
            }
            try {
                intent.setComponent(new ComponentName(defaultRI.activityInfo.packageName, defaultRI.activityInfo.name));
                this.mUserContext.startActivityAsUser(intent, this.mUser);
                return;
            } catch (ActivityNotFoundException e2) {
                Slog.e(TAG, "startActivity failed", e2);
                return;
            }
        }
        Intent resolverIntent = new Intent();
        resolverIntent.addFlags(268435456);
        if (count == 1) {
            resolverIntent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbConfirmActivity");
            resolverIntent.putExtra("rinfo", matches.get(0));
            if (device != null) {
                resolverIntent.putExtra("device", device);
            } else {
                resolverIntent.putExtra("accessory", accessory);
            }
        } else {
            resolverIntent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbResolverActivity");
            resolverIntent.putParcelableArrayListExtra("rlist", matches);
            resolverIntent.putExtra("android.intent.extra.INTENT", intent);
        }
        try {
            this.mUserContext.startActivityAsUser(resolverIntent, this.mUser);
        } catch (ActivityNotFoundException e3) {
            Slog.e(TAG, "unable to start activity " + resolverIntent);
        }
    }

    private boolean clearCompatibleMatchesLocked(String packageName, DeviceFilter filter) {
        boolean changed = DEBUG;
        for (DeviceFilter test : this.mDevicePreferenceMap.keySet()) {
            if (filter.matches(test)) {
                this.mDevicePreferenceMap.remove(test);
                changed = true;
            }
        }
        return changed;
    }

    private boolean clearCompatibleMatchesLocked(String packageName, AccessoryFilter filter) {
        boolean changed = DEBUG;
        for (AccessoryFilter test : this.mAccessoryPreferenceMap.keySet()) {
            if (filter.matches(test)) {
                this.mAccessoryPreferenceMap.remove(test);
                changed = true;
            }
        }
        return changed;
    }

    private boolean handlePackageUpdateLocked(String packageName, ActivityInfo aInfo, String metaDataName) {
        XmlResourceParser parser;
        XmlResourceParser parser2 = null;
        boolean changed = DEBUG;
        try {
            try {
                parser = aInfo.loadXmlMetaData(this.mPackageManager, metaDataName);
            } catch (Exception e) {
                Slog.w(TAG, "Unable to load component info " + aInfo.toString(), e);
                if (0 != 0) {
                    parser2.close();
                }
            }
            if (parser != null) {
                XmlUtils.nextElement(parser);
                while (parser.getEventType() != 1) {
                    String tagName = parser.getName();
                    if ("usb-device".equals(tagName)) {
                        DeviceFilter filter = DeviceFilter.read(parser);
                        if (clearCompatibleMatchesLocked(packageName, filter)) {
                            changed = true;
                        }
                    } else if ("usb-accessory".equals(tagName)) {
                        AccessoryFilter filter2 = AccessoryFilter.read(parser);
                        if (clearCompatibleMatchesLocked(packageName, filter2)) {
                            changed = true;
                        }
                    }
                    XmlUtils.nextElement(parser);
                }
                if (parser != null) {
                    parser.close();
                }
                return changed;
            }
            if (parser == null) {
                return DEBUG;
            }
            parser.close();
            return DEBUG;
        } catch (Throwable th) {
            if (0 != 0) {
                parser2.close();
            }
            throw th;
        }
    }

    private void handlePackageUpdate(String packageName) {
        synchronized (this.mLock) {
            boolean changed = DEBUG;
            try {
                PackageInfo info = this.mPackageManager.getPackageInfo(packageName, 129);
                ActivityInfo[] activities = info.activities;
                if (activities != null) {
                    for (int i = 0; i < activities.length; i++) {
                        if (handlePackageUpdateLocked(packageName, activities[i], "android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                            changed = true;
                        }
                        if (handlePackageUpdateLocked(packageName, activities[i], "android.hardware.usb.action.USB_ACCESSORY_ATTACHED")) {
                            changed = true;
                        }
                    }
                    if (changed) {
                        writeSettingsLocked();
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Slog.e(TAG, "handlePackageUpdate could not find package " + packageName, e);
            }
        }
    }

    public boolean hasPermission(UsbDevice device) {
        boolean z;
        synchronized (this.mLock) {
            int uid = Binder.getCallingUid();
            if (uid == 1000 || this.mDisablePermissionDialogs) {
                z = true;
            } else {
                SparseBooleanArray uidList = this.mDevicePermissionMap.get(device.getDeviceName());
                if (uidList == null) {
                    z = DEBUG;
                } else {
                    z = uidList.get(uid);
                }
            }
        }
        return z;
    }

    public boolean hasPermission(UsbAccessory accessory) {
        boolean z;
        synchronized (this.mLock) {
            int uid = Binder.getCallingUid();
            if (uid == 1000 || this.mDisablePermissionDialogs) {
                z = true;
            } else {
                SparseBooleanArray uidList = this.mAccessoryPermissionMap.get(accessory);
                if (uidList == null) {
                    z = DEBUG;
                } else {
                    z = uidList.get(uid);
                }
            }
        }
        return z;
    }

    public void checkPermission(UsbDevice device) {
        if (!hasPermission(device)) {
            throw new SecurityException("User has not given permission to device " + device);
        }
    }

    public void checkPermission(UsbAccessory accessory) {
        if (!hasPermission(accessory)) {
            throw new SecurityException("User has not given permission to accessory " + accessory);
        }
    }

    private void requestPermissionDialog(Intent intent, String packageName, PendingIntent pi) {
        int uid = Binder.getCallingUid();
        try {
            ApplicationInfo aInfo = this.mPackageManager.getApplicationInfo(packageName, 0);
            if (aInfo.uid != uid) {
                throw new IllegalArgumentException("package " + packageName + " does not match caller's uid " + uid);
            }
            long identity = Binder.clearCallingIdentity();
            intent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbPermissionActivity");
            intent.addFlags(268435456);
            intent.putExtra("android.intent.extra.INTENT", pi);
            intent.putExtra("package", packageName);
            intent.putExtra("android.intent.extra.UID", uid);
            try {
                this.mUserContext.startActivityAsUser(intent, this.mUser);
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start UsbPermissionActivity");
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        } catch (PackageManager.NameNotFoundException e2) {
            throw new IllegalArgumentException("package " + packageName + " not found");
        }
    }

    public void requestPermission(UsbDevice device, String packageName, PendingIntent pi) {
        Intent intent = new Intent();
        if (hasPermission(device)) {
            intent.putExtra("device", device);
            intent.putExtra("permission", true);
            try {
                pi.send(this.mUserContext, 0, intent);
                return;
            } catch (PendingIntent.CanceledException e) {
                return;
            }
        }
        intent.putExtra("device", device);
        requestPermissionDialog(intent, packageName, pi);
    }

    public void requestPermission(UsbAccessory accessory, String packageName, PendingIntent pi) {
        Intent intent = new Intent();
        if (hasPermission(accessory)) {
            intent.putExtra("accessory", accessory);
            intent.putExtra("permission", true);
            try {
                pi.send(this.mUserContext, 0, intent);
                return;
            } catch (PendingIntent.CanceledException e) {
                return;
            }
        }
        intent.putExtra("accessory", accessory);
        requestPermissionDialog(intent, packageName, pi);
    }

    public void setDevicePackage(UsbDevice device, String packageName) {
        boolean changed;
        DeviceFilter filter = new DeviceFilter(device);
        synchronized (this.mLock) {
            if (packageName == null) {
                changed = this.mDevicePreferenceMap.remove(filter) != null;
            } else {
                changed = !packageName.equals(this.mDevicePreferenceMap.get(filter));
                if (changed) {
                    this.mDevicePreferenceMap.put(filter, packageName);
                }
            }
            if (changed) {
                writeSettingsLocked();
            }
        }
    }

    public void setAccessoryPackage(UsbAccessory accessory, String packageName) {
        boolean changed;
        AccessoryFilter filter = new AccessoryFilter(accessory);
        synchronized (this.mLock) {
            if (packageName == null) {
                changed = this.mAccessoryPreferenceMap.remove(filter) != null;
            } else {
                changed = !packageName.equals(this.mAccessoryPreferenceMap.get(filter));
                if (changed) {
                    this.mAccessoryPreferenceMap.put(filter, packageName);
                }
            }
            if (changed) {
                writeSettingsLocked();
            }
        }
    }

    public void grantDevicePermission(UsbDevice device, int uid) {
        synchronized (this.mLock) {
            String deviceName = device.getDeviceName();
            SparseBooleanArray uidList = this.mDevicePermissionMap.get(deviceName);
            if (uidList == null) {
                uidList = new SparseBooleanArray(1);
                this.mDevicePermissionMap.put(deviceName, uidList);
            }
            uidList.put(uid, true);
        }
    }

    public void grantAccessoryPermission(UsbAccessory accessory, int uid) {
        synchronized (this.mLock) {
            SparseBooleanArray uidList = this.mAccessoryPermissionMap.get(accessory);
            if (uidList == null) {
                uidList = new SparseBooleanArray(1);
                this.mAccessoryPermissionMap.put(accessory, uidList);
            }
            uidList.put(uid, true);
        }
    }

    public boolean hasDefaults(String packageName) {
        boolean z = true;
        synchronized (this.mLock) {
            if (!this.mDevicePreferenceMap.values().contains(packageName)) {
                if (!this.mAccessoryPreferenceMap.values().contains(packageName)) {
                    z = DEBUG;
                }
            }
        }
        return z;
    }

    public void clearDefaults(String packageName) {
        synchronized (this.mLock) {
            if (clearPackageDefaultsLocked(packageName)) {
                writeSettingsLocked();
            }
        }
    }

    private boolean clearPackageDefaultsLocked(String packageName) {
        boolean cleared = DEBUG;
        synchronized (this.mLock) {
            if (this.mDevicePreferenceMap.containsValue(packageName)) {
                Object[] keys = this.mDevicePreferenceMap.keySet().toArray();
                for (Object key : keys) {
                    if (packageName.equals(this.mDevicePreferenceMap.get(key))) {
                        this.mDevicePreferenceMap.remove(key);
                        cleared = true;
                    }
                }
            }
            if (this.mAccessoryPreferenceMap.containsValue(packageName)) {
                Object[] keys2 = this.mAccessoryPreferenceMap.keySet().toArray();
                for (Object key2 : keys2) {
                    if (packageName.equals(this.mAccessoryPreferenceMap.get(key2))) {
                        this.mAccessoryPreferenceMap.remove(key2);
                        cleared = true;
                    }
                }
            }
        }
        return cleared;
    }

    public void dump(FileDescriptor fd, PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("  Device permissions:");
            for (String deviceName : this.mDevicePermissionMap.keySet()) {
                pw.print("    " + deviceName + ": ");
                SparseBooleanArray uidList = this.mDevicePermissionMap.get(deviceName);
                int count = uidList.size();
                for (int i = 0; i < count; i++) {
                    pw.print(Integer.toString(uidList.keyAt(i)) + " ");
                }
                pw.println("");
            }
            pw.println("  Accessory permissions:");
            for (UsbAccessory accessory : this.mAccessoryPermissionMap.keySet()) {
                pw.print("    " + accessory + ": ");
                SparseBooleanArray uidList2 = this.mAccessoryPermissionMap.get(accessory);
                int count2 = uidList2.size();
                for (int i2 = 0; i2 < count2; i2++) {
                    pw.print(Integer.toString(uidList2.keyAt(i2)) + " ");
                }
                pw.println("");
            }
            pw.println("  Device preferences:");
            for (DeviceFilter filter : this.mDevicePreferenceMap.keySet()) {
                pw.println("    " + filter + ": " + this.mDevicePreferenceMap.get(filter));
            }
            pw.println("  Accessory preferences:");
            for (AccessoryFilter filter2 : this.mAccessoryPreferenceMap.keySet()) {
                pw.println("    " + filter2 + ": " + this.mAccessoryPreferenceMap.get(filter2));
            }
        }
    }
}
