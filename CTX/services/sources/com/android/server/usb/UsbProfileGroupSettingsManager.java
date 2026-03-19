package com.android.server.usb;

import android.R;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.XmlResourceParser;
import android.hardware.usb.AccessoryFilter;
import android.hardware.usb.DeviceFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.net.util.NetworkConstants;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Environment;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.Immutable;
import com.android.internal.app.IntentForwarderActivity;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.slice.SliceClientPermissions;
import com.android.server.usb.MtpNotificationManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

class UsbProfileGroupSettingsManager {
    private static final boolean DEBUG = false;
    private static final String TAG = UsbProfileGroupSettingsManager.class.getSimpleName();
    private static final File sSingleUserSettingsFile = new File("/data/system/usb_device_manager.xml");
    private final Context mContext;
    private final boolean mDisablePermissionDialogs;

    @GuardedBy("mLock")
    private boolean mIsWriteSettingsScheduled;
    private final MtpNotificationManager mMtpNotificationManager;
    private final PackageManager mPackageManager;
    private final UserHandle mParentUser;
    private final AtomicFile mSettingsFile;
    private final UsbSettingsManager mSettingsManager;
    private final UserManager mUserManager;

    @GuardedBy("mLock")
    private final HashMap<DeviceFilter, UserPackage> mDevicePreferenceMap = new HashMap<>();

    @GuardedBy("mLock")
    private final HashMap<AccessoryFilter, UserPackage> mAccessoryPreferenceMap = new HashMap<>();
    private final Object mLock = new Object();
    MyPackageMonitor mPackageMonitor = new MyPackageMonitor();

    @Immutable
    private static class UserPackage {
        final String packageName;
        final UserHandle user;

        private UserPackage(String str, UserHandle userHandle) {
            this.packageName = str;
            this.user = userHandle;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof UserPackage)) {
                return false;
            }
            UserPackage userPackage = (UserPackage) obj;
            return this.user.equals(userPackage.user) && this.packageName.equals(userPackage.packageName);
        }

        public int hashCode() {
            return (31 * this.user.hashCode()) + this.packageName.hashCode();
        }

        public String toString() {
            return this.user.getIdentifier() + SliceClientPermissions.SliceAuthority.DELIMITER + this.packageName;
        }

        public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
            long jStart = dualDumpOutputStream.start(str, j);
            dualDumpOutputStream.write("user_id", 1120986464257L, this.user.getIdentifier());
            dualDumpOutputStream.write("package_name", 1138166333442L, this.packageName);
            dualDumpOutputStream.end(jStart);
        }
    }

    private class MyPackageMonitor extends PackageMonitor {
        private MyPackageMonitor() {
        }

        public void onPackageAdded(String str, int i) {
            if (UsbProfileGroupSettingsManager.this.mUserManager.isSameProfileGroup(UsbProfileGroupSettingsManager.this.mParentUser.getIdentifier(), UserHandle.getUserId(i))) {
                UsbProfileGroupSettingsManager.this.handlePackageAdded(new UserPackage(str, UserHandle.getUserHandleForUid(i)));
            }
        }

        public void onPackageRemoved(String str, int i) {
            if (!UsbProfileGroupSettingsManager.this.mUserManager.isSameProfileGroup(UsbProfileGroupSettingsManager.this.mParentUser.getIdentifier(), UserHandle.getUserId(i))) {
                return;
            }
            UsbProfileGroupSettingsManager.this.clearDefaults(str, UserHandle.getUserHandleForUid(i));
        }
    }

    UsbProfileGroupSettingsManager(Context context, UserHandle userHandle, UsbSettingsManager usbSettingsManager) {
        try {
            Context contextCreatePackageContextAsUser = context.createPackageContextAsUser(PackageManagerService.PLATFORM_PACKAGE_NAME, 0, userHandle);
            this.mContext = context;
            this.mPackageManager = context.getPackageManager();
            this.mSettingsManager = usbSettingsManager;
            this.mUserManager = (UserManager) context.getSystemService("user");
            this.mParentUser = userHandle;
            this.mSettingsFile = new AtomicFile(new File(Environment.getUserSystemDirectory(userHandle.getIdentifier()), "usb_device_manager.xml"), "usb-state");
            this.mDisablePermissionDialogs = context.getResources().getBoolean(R.^attr-private.dreamActivityOpenExitAnimation);
            synchronized (this.mLock) {
                if (UserHandle.SYSTEM.equals(userHandle)) {
                    upgradeSingleUserLocked();
                }
                readSettingsLocked();
            }
            this.mPackageMonitor.register(context, null, UserHandle.ALL, true);
            this.mMtpNotificationManager = new MtpNotificationManager(contextCreatePackageContextAsUser, new MtpNotificationManager.OnOpenInAppListener() {
                @Override
                public final void onOpenInApp(UsbDevice usbDevice) {
                    this.f$0.resolveActivity(UsbProfileGroupSettingsManager.createDeviceAttachedIntent(usbDevice), usbDevice, false);
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Missing android package");
        }
    }

    void removeAllDefaultsForUser(UserHandle userHandle) {
        synchronized (this.mLock) {
            boolean z = false;
            Iterator<Map.Entry<DeviceFilter, UserPackage>> it = this.mDevicePreferenceMap.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().user.equals(userHandle)) {
                    it.remove();
                    z = true;
                }
            }
            Iterator<Map.Entry<AccessoryFilter, UserPackage>> it2 = this.mAccessoryPreferenceMap.entrySet().iterator();
            while (it2.hasNext()) {
                if (it2.next().getValue().user.equals(userHandle)) {
                    it2.remove();
                    z = true;
                }
            }
            if (z) {
                scheduleWriteSettingsLocked();
            }
        }
    }

    private void readPreference(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        UserHandle userForSerialNumber = this.mParentUser;
        int attributeCount = xmlPullParser.getAttributeCount();
        String attributeValue = null;
        for (int i = 0; i < attributeCount; i++) {
            if (Settings.ATTR_PACKAGE.equals(xmlPullParser.getAttributeName(i))) {
                attributeValue = xmlPullParser.getAttributeValue(i);
            }
            if ("user".equals(xmlPullParser.getAttributeName(i))) {
                userForSerialNumber = this.mUserManager.getUserForSerialNumber(Integer.parseInt(xmlPullParser.getAttributeValue(i)));
            }
        }
        XmlUtils.nextElement(xmlPullParser);
        if ("usb-device".equals(xmlPullParser.getName())) {
            DeviceFilter deviceFilter = DeviceFilter.read(xmlPullParser);
            if (userForSerialNumber != null) {
                this.mDevicePreferenceMap.put(deviceFilter, new UserPackage(attributeValue, userForSerialNumber));
            }
        } else if ("usb-accessory".equals(xmlPullParser.getName())) {
            AccessoryFilter accessoryFilter = AccessoryFilter.read(xmlPullParser);
            if (userForSerialNumber != null) {
                this.mAccessoryPreferenceMap.put(accessoryFilter, new UserPackage(attributeValue, userForSerialNumber));
            }
        }
        XmlUtils.nextElement(xmlPullParser);
    }

    @GuardedBy("mLock")
    private void upgradeSingleUserLocked() throws Throwable {
        Throwable th;
        FileInputStream fileInputStream;
        Throwable e;
        if (sSingleUserSettingsFile.exists()) {
            this.mDevicePreferenceMap.clear();
            this.mAccessoryPreferenceMap.clear();
            try {
                try {
                    fileInputStream = new FileInputStream(sSingleUserSettingsFile);
                    try {
                        XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
                        XmlUtils.nextElement(xmlPullParserNewPullParser);
                        while (xmlPullParserNewPullParser.getEventType() != 1) {
                            if ("preference".equals(xmlPullParserNewPullParser.getName())) {
                                readPreference(xmlPullParserNewPullParser);
                            } else {
                                XmlUtils.nextElement(xmlPullParserNewPullParser);
                            }
                        }
                    } catch (IOException | XmlPullParserException e2) {
                        e = e2;
                        Log.wtf(TAG, "Failed to read single-user settings", e);
                    }
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly((AutoCloseable) null);
                    throw th;
                }
            } catch (IOException | XmlPullParserException e3) {
                fileInputStream = null;
                e = e3;
            } catch (Throwable th3) {
                th = th3;
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
            IoUtils.closeQuietly(fileInputStream);
            scheduleWriteSettingsLocked();
            sSingleUserSettingsFile.delete();
        }
    }

    @GuardedBy("mLock")
    private void readSettingsLocked() throws Throwable {
        Throwable th;
        FileInputStream fileInputStreamOpenRead;
        Exception e;
        this.mDevicePreferenceMap.clear();
        this.mAccessoryPreferenceMap.clear();
        try {
            try {
                fileInputStreamOpenRead = this.mSettingsFile.openRead();
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    while (xmlPullParserNewPullParser.getEventType() != 1) {
                        if ("preference".equals(xmlPullParserNewPullParser.getName())) {
                            readPreference(xmlPullParserNewPullParser);
                        } else {
                            XmlUtils.nextElement(xmlPullParserNewPullParser);
                        }
                    }
                } catch (FileNotFoundException e2) {
                } catch (Exception e3) {
                    e = e3;
                    Slog.e(TAG, "error reading settings file, deleting to start fresh", e);
                    this.mSettingsFile.delete();
                }
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        } catch (FileNotFoundException e4) {
            fileInputStreamOpenRead = null;
        } catch (Exception e5) {
            fileInputStreamOpenRead = null;
            e = e5;
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
        IoUtils.closeQuietly(fileInputStreamOpenRead);
    }

    @GuardedBy("mLock")
    private void scheduleWriteSettingsLocked() {
        if (this.mIsWriteSettingsScheduled) {
            return;
        }
        this.mIsWriteSettingsScheduled = true;
        AsyncTask.execute(new Runnable() {
            @Override
            public final void run() {
                UsbProfileGroupSettingsManager.lambda$scheduleWriteSettingsLocked$1(this.f$0);
            }
        });
    }

    public static void lambda$scheduleWriteSettingsLocked$1(UsbProfileGroupSettingsManager usbProfileGroupSettingsManager) {
        FileOutputStream fileOutputStreamStartWrite;
        IOException e;
        synchronized (usbProfileGroupSettingsManager.mLock) {
            try {
                fileOutputStreamStartWrite = usbProfileGroupSettingsManager.mSettingsFile.startWrite();
                try {
                    FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument((String) null, true);
                    fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                    fastXmlSerializer.startTag((String) null, "settings");
                    for (DeviceFilter deviceFilter : usbProfileGroupSettingsManager.mDevicePreferenceMap.keySet()) {
                        fastXmlSerializer.startTag((String) null, "preference");
                        fastXmlSerializer.attribute((String) null, Settings.ATTR_PACKAGE, usbProfileGroupSettingsManager.mDevicePreferenceMap.get(deviceFilter).packageName);
                        fastXmlSerializer.attribute((String) null, "user", String.valueOf(usbProfileGroupSettingsManager.getSerial(usbProfileGroupSettingsManager.mDevicePreferenceMap.get(deviceFilter).user)));
                        deviceFilter.write(fastXmlSerializer);
                        fastXmlSerializer.endTag((String) null, "preference");
                    }
                    for (AccessoryFilter accessoryFilter : usbProfileGroupSettingsManager.mAccessoryPreferenceMap.keySet()) {
                        fastXmlSerializer.startTag((String) null, "preference");
                        fastXmlSerializer.attribute((String) null, Settings.ATTR_PACKAGE, usbProfileGroupSettingsManager.mAccessoryPreferenceMap.get(accessoryFilter).packageName);
                        fastXmlSerializer.attribute((String) null, "user", String.valueOf(usbProfileGroupSettingsManager.getSerial(usbProfileGroupSettingsManager.mAccessoryPreferenceMap.get(accessoryFilter).user)));
                        accessoryFilter.write(fastXmlSerializer);
                        fastXmlSerializer.endTag((String) null, "preference");
                    }
                    fastXmlSerializer.endTag((String) null, "settings");
                    fastXmlSerializer.endDocument();
                    usbProfileGroupSettingsManager.mSettingsFile.finishWrite(fileOutputStreamStartWrite);
                } catch (IOException e2) {
                    e = e2;
                    Slog.e(TAG, "Failed to write settings", e);
                    if (fileOutputStreamStartWrite != null) {
                        usbProfileGroupSettingsManager.mSettingsFile.failWrite(fileOutputStreamStartWrite);
                    }
                }
            } catch (IOException e3) {
                fileOutputStreamStartWrite = null;
                e = e3;
            }
            usbProfileGroupSettingsManager.mIsWriteSettingsScheduled = false;
        }
    }

    private boolean packageMatchesLocked(ResolveInfo resolveInfo, String str, UsbDevice usbDevice, UsbAccessory usbAccessory) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        if (isForwardMatch(resolveInfo)) {
            return true;
        }
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                xmlResourceParserLoadXmlMetaData = resolveInfo.activityInfo.loadXmlMetaData(this.mPackageManager, str);
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
            xmlResourceParserLoadXmlMetaData = xmlResourceParser;
        }
        try {
        } catch (Exception e2) {
            e = e2;
            xmlResourceParser = xmlResourceParserLoadXmlMetaData;
            Slog.w(TAG, "Unable to load component info " + resolveInfo.toString(), e);
            if (xmlResourceParser != null) {
                xmlResourceParser.close();
            }
        } catch (Throwable th2) {
            th = th2;
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            throw th;
        }
        if (xmlResourceParserLoadXmlMetaData == null) {
            Slog.w(TAG, "no meta-data for " + resolveInfo);
            if (xmlResourceParserLoadXmlMetaData != null) {
                xmlResourceParserLoadXmlMetaData.close();
            }
            return false;
        }
        XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
        while (xmlResourceParserLoadXmlMetaData.getEventType() != 1) {
            String name = xmlResourceParserLoadXmlMetaData.getName();
            if (usbDevice == null || !"usb-device".equals(name)) {
                if (usbAccessory != null && "usb-accessory".equals(name) && AccessoryFilter.read(xmlResourceParserLoadXmlMetaData).matches(usbAccessory)) {
                    if (xmlResourceParserLoadXmlMetaData != null) {
                        xmlResourceParserLoadXmlMetaData.close();
                    }
                    return true;
                }
            } else if (DeviceFilter.read(xmlResourceParserLoadXmlMetaData).matches(usbDevice)) {
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                return true;
            }
            XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
        }
        if (xmlResourceParserLoadXmlMetaData != null) {
            xmlResourceParserLoadXmlMetaData.close();
        }
        return false;
    }

    private ArrayList<ResolveInfo> queryIntentActivitiesForAllProfiles(Intent intent) {
        List enabledProfiles = this.mUserManager.getEnabledProfiles(this.mParentUser.getIdentifier());
        ArrayList<ResolveInfo> arrayList = new ArrayList<>();
        int size = enabledProfiles.size();
        for (int i = 0; i < size; i++) {
            arrayList.addAll(this.mPackageManager.queryIntentActivitiesAsUser(intent, 128, ((UserInfo) enabledProfiles.get(i)).id));
        }
        return arrayList;
    }

    private boolean isForwardMatch(ResolveInfo resolveInfo) {
        return resolveInfo.getComponentInfo().name.equals(IntentForwarderActivity.FORWARD_INTENT_TO_MANAGED_PROFILE);
    }

    private ArrayList<ResolveInfo> preferHighPriority(ArrayList<ResolveInfo> arrayList) {
        SparseArray sparseArray = new SparseArray();
        SparseIntArray sparseIntArray = new SparseIntArray();
        ArrayList arrayList2 = new ArrayList();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = arrayList.get(i);
            if (isForwardMatch(resolveInfo)) {
                arrayList2.add(resolveInfo);
            } else {
                if (sparseIntArray.indexOfKey(resolveInfo.targetUserId) < 0) {
                    sparseIntArray.put(resolveInfo.targetUserId, Integer.MIN_VALUE);
                    sparseArray.put(resolveInfo.targetUserId, new ArrayList());
                }
                int i2 = sparseIntArray.get(resolveInfo.targetUserId);
                ArrayList arrayList3 = (ArrayList) sparseArray.get(resolveInfo.targetUserId);
                if (resolveInfo.priority == i2) {
                    arrayList3.add(resolveInfo);
                } else if (resolveInfo.priority > i2) {
                    sparseIntArray.put(resolveInfo.targetUserId, resolveInfo.priority);
                    arrayList3.clear();
                    arrayList3.add(resolveInfo);
                }
            }
        }
        ArrayList<ResolveInfo> arrayList4 = new ArrayList<>(arrayList2);
        int size2 = sparseArray.size();
        for (int i3 = 0; i3 < size2; i3++) {
            arrayList4.addAll((Collection) sparseArray.valueAt(i3));
        }
        return arrayList4;
    }

    private ArrayList<ResolveInfo> removeForwardIntentIfNotNeeded(ArrayList<ResolveInfo> arrayList) {
        int size = arrayList.size();
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < size; i3++) {
            ResolveInfo resolveInfo = arrayList.get(i3);
            if (!isForwardMatch(resolveInfo)) {
                if (UserHandle.getUserHandleForUid(resolveInfo.activityInfo.applicationInfo.uid).equals(this.mParentUser)) {
                    i++;
                } else {
                    i2++;
                }
            }
        }
        if (i == 0 || i2 == 0) {
            ArrayList<ResolveInfo> arrayList2 = new ArrayList<>(i + i2);
            for (int i4 = 0; i4 < size; i4++) {
                ResolveInfo resolveInfo2 = arrayList.get(i4);
                if (!isForwardMatch(resolveInfo2)) {
                    arrayList2.add(resolveInfo2);
                }
            }
            return arrayList2;
        }
        return arrayList;
    }

    private ArrayList<ResolveInfo> getDeviceMatchesLocked(UsbDevice usbDevice, Intent intent) {
        ArrayList<ResolveInfo> arrayList = new ArrayList<>();
        ArrayList<ResolveInfo> arrayListQueryIntentActivitiesForAllProfiles = queryIntentActivitiesForAllProfiles(intent);
        int size = arrayListQueryIntentActivitiesForAllProfiles.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = arrayListQueryIntentActivitiesForAllProfiles.get(i);
            if (packageMatchesLocked(resolveInfo, intent.getAction(), usbDevice, null)) {
                arrayList.add(resolveInfo);
            }
        }
        return removeForwardIntentIfNotNeeded(preferHighPriority(arrayList));
    }

    private ArrayList<ResolveInfo> getAccessoryMatchesLocked(UsbAccessory usbAccessory, Intent intent) {
        ArrayList<ResolveInfo> arrayList = new ArrayList<>();
        ArrayList<ResolveInfo> arrayListQueryIntentActivitiesForAllProfiles = queryIntentActivitiesForAllProfiles(intent);
        int size = arrayListQueryIntentActivitiesForAllProfiles.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = arrayListQueryIntentActivitiesForAllProfiles.get(i);
            if (packageMatchesLocked(resolveInfo, intent.getAction(), null, usbAccessory)) {
                arrayList.add(resolveInfo);
            }
        }
        return removeForwardIntentIfNotNeeded(preferHighPriority(arrayList));
    }

    public void deviceAttached(UsbDevice usbDevice) {
        Intent intentCreateDeviceAttachedIntent = createDeviceAttachedIntent(usbDevice);
        this.mContext.sendBroadcastAsUser(intentCreateDeviceAttachedIntent, UserHandle.ALL);
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        resolveActivity(intentCreateDeviceAttachedIntent, usbDevice, true);
    }

    private void resolveActivity(Intent intent, UsbDevice usbDevice, boolean z) {
        ArrayList<ResolveInfo> deviceMatchesLocked;
        ActivityInfo defaultActivityLocked;
        synchronized (this.mLock) {
            deviceMatchesLocked = getDeviceMatchesLocked(usbDevice, intent);
            defaultActivityLocked = getDefaultActivityLocked(deviceMatchesLocked, this.mDevicePreferenceMap.get(new DeviceFilter(usbDevice)));
        }
        if (z && MtpNotificationManager.shouldShowNotification(this.mPackageManager, usbDevice) && defaultActivityLocked == null) {
            this.mMtpNotificationManager.showNotification(usbDevice);
        } else {
            resolveActivity(intent, deviceMatchesLocked, defaultActivityLocked, usbDevice, null);
        }
    }

    public void deviceAttachedForFixedHandler(UsbDevice usbDevice, ComponentName componentName) {
        Intent intentCreateDeviceAttachedIntent = createDeviceAttachedIntent(usbDevice);
        this.mContext.sendBroadcast(intentCreateDeviceAttachedIntent);
        try {
            ApplicationInfo applicationInfoAsUser = this.mPackageManager.getApplicationInfoAsUser(componentName.getPackageName(), 0, this.mParentUser.getIdentifier());
            this.mSettingsManager.getSettingsForUser(UserHandle.getUserId(applicationInfoAsUser.uid)).grantDevicePermission(usbDevice, applicationInfoAsUser.uid);
            Intent intent = new Intent(intentCreateDeviceAttachedIntent);
            intent.setComponent(componentName);
            try {
                this.mContext.startActivityAsUser(intent, this.mParentUser);
            } catch (ActivityNotFoundException e) {
                Slog.e(TAG, "unable to start activity " + intent);
            }
        } catch (PackageManager.NameNotFoundException e2) {
            Slog.e(TAG, "Default USB handling package (" + componentName.getPackageName() + ") not found  for user " + this.mParentUser);
        }
    }

    void usbDeviceRemoved(UsbDevice usbDevice) {
        this.mMtpNotificationManager.hideNotification(usbDevice.getDeviceId());
    }

    public void accessoryAttached(UsbAccessory usbAccessory) {
        ArrayList<ResolveInfo> accessoryMatchesLocked;
        ActivityInfo defaultActivityLocked;
        Intent intent = new Intent("android.hardware.usb.action.USB_ACCESSORY_ATTACHED");
        intent.putExtra("accessory", usbAccessory);
        intent.addFlags(285212672);
        synchronized (this.mLock) {
            accessoryMatchesLocked = getAccessoryMatchesLocked(usbAccessory, intent);
            defaultActivityLocked = getDefaultActivityLocked(accessoryMatchesLocked, this.mAccessoryPreferenceMap.get(new AccessoryFilter(usbAccessory)));
        }
        resolveActivity(intent, accessoryMatchesLocked, defaultActivityLocked, null, usbAccessory);
    }

    private void resolveActivity(Intent intent, ArrayList<ResolveInfo> arrayList, ActivityInfo activityInfo, UsbDevice usbDevice, UsbAccessory usbAccessory) {
        UserHandle userHandleForUid;
        String uri;
        if (arrayList.size() == 0) {
            if (usbAccessory != null && (uri = usbAccessory.getUri()) != null && uri.length() > 0) {
                Intent intent2 = new Intent();
                intent2.setClassName("com.android.systemui", "com.android.systemui.usb.UsbAccessoryUriActivity");
                intent2.addFlags(268435456);
                intent2.putExtra("accessory", usbAccessory);
                intent2.putExtra("uri", uri);
                try {
                    this.mContext.startActivityAsUser(intent2, this.mParentUser);
                    return;
                } catch (ActivityNotFoundException e) {
                    Slog.e(TAG, "unable to start UsbAccessoryUriActivity");
                    return;
                }
            }
            return;
        }
        if (activityInfo != null) {
            UsbUserSettingsManager settingsForUser = this.mSettingsManager.getSettingsForUser(UserHandle.getUserId(activityInfo.applicationInfo.uid));
            if (usbDevice != null) {
                settingsForUser.grantDevicePermission(usbDevice, activityInfo.applicationInfo.uid);
            } else if (usbAccessory != null) {
                settingsForUser.grantAccessoryPermission(usbAccessory, activityInfo.applicationInfo.uid);
            }
            try {
                intent.setComponent(new ComponentName(activityInfo.packageName, activityInfo.name));
                this.mContext.startActivityAsUser(intent, UserHandle.getUserHandleForUid(activityInfo.applicationInfo.uid));
                return;
            } catch (ActivityNotFoundException e2) {
                Slog.e(TAG, "startActivity failed", e2);
                return;
            }
        }
        Intent intent3 = new Intent();
        intent3.addFlags(268435456);
        if (arrayList.size() == 1) {
            ResolveInfo resolveInfo = arrayList.get(0);
            intent3.setClassName("com.android.systemui", "com.android.systemui.usb.UsbConfirmActivity");
            intent3.putExtra("rinfo", resolveInfo);
            userHandleForUid = UserHandle.getUserHandleForUid(resolveInfo.activityInfo.applicationInfo.uid);
            if (usbDevice != null) {
                intent3.putExtra("device", usbDevice);
            } else {
                intent3.putExtra("accessory", usbAccessory);
            }
        } else {
            UserHandle userHandle = this.mParentUser;
            intent3.setClassName("com.android.systemui", "com.android.systemui.usb.UsbResolverActivity");
            intent3.putParcelableArrayListExtra("rlist", arrayList);
            intent3.putExtra("android.intent.extra.INTENT", intent);
            userHandleForUid = userHandle;
        }
        try {
            this.mContext.startActivityAsUser(intent3, userHandleForUid);
        } catch (ActivityNotFoundException e3) {
            Slog.e(TAG, "unable to start activity " + intent3, e3);
        }
    }

    private ActivityInfo getDefaultActivityLocked(ArrayList<ResolveInfo> arrayList, UserPackage userPackage) {
        ActivityInfo activityInfo;
        if (userPackage != null) {
            for (ResolveInfo resolveInfo : arrayList) {
                if (resolveInfo.activityInfo != null && userPackage.equals(new UserPackage(resolveInfo.activityInfo.packageName, UserHandle.getUserHandleForUid(resolveInfo.activityInfo.applicationInfo.uid)))) {
                    return resolveInfo.activityInfo;
                }
            }
        }
        if (arrayList.size() == 1 && (activityInfo = arrayList.get(0).activityInfo) != null) {
            if (this.mDisablePermissionDialogs) {
                return activityInfo;
            }
            if (activityInfo.applicationInfo != null && (activityInfo.applicationInfo.flags & 1) != 0) {
                return activityInfo;
            }
        }
        return null;
    }

    @GuardedBy("mLock")
    private boolean clearCompatibleMatchesLocked(UserPackage userPackage, DeviceFilter deviceFilter) {
        ArrayList arrayList = new ArrayList();
        for (DeviceFilter deviceFilter2 : this.mDevicePreferenceMap.keySet()) {
            if (deviceFilter.contains(deviceFilter2) && !this.mDevicePreferenceMap.get(deviceFilter2).equals(userPackage)) {
                arrayList.add(deviceFilter2);
            }
        }
        if (!arrayList.isEmpty()) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                this.mDevicePreferenceMap.remove((DeviceFilter) it.next());
            }
        }
        return !arrayList.isEmpty();
    }

    @GuardedBy("mLock")
    private boolean clearCompatibleMatchesLocked(UserPackage userPackage, AccessoryFilter accessoryFilter) {
        ArrayList arrayList = new ArrayList();
        for (AccessoryFilter accessoryFilter2 : this.mAccessoryPreferenceMap.keySet()) {
            if (accessoryFilter.contains(accessoryFilter2) && !this.mAccessoryPreferenceMap.get(accessoryFilter2).equals(userPackage)) {
                arrayList.add(accessoryFilter2);
            }
        }
        if (!arrayList.isEmpty()) {
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                this.mAccessoryPreferenceMap.remove((AccessoryFilter) it.next());
            }
        }
        return !arrayList.isEmpty();
    }

    @GuardedBy("mLock")
    private boolean handlePackageAddedLocked(UserPackage userPackage, ActivityInfo activityInfo, String str) throws Throwable {
        XmlResourceParser xmlResourceParserLoadXmlMetaData;
        boolean z = false;
        XmlResourceParser xmlResourceParser = null;
        try {
            try {
                xmlResourceParserLoadXmlMetaData = activityInfo.loadXmlMetaData(this.mPackageManager, str);
            } catch (Exception e) {
                e = e;
            }
            if (xmlResourceParserLoadXmlMetaData == null) {
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                return false;
            }
            try {
                XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
                while (xmlResourceParserLoadXmlMetaData.getEventType() != 1) {
                    String name = xmlResourceParserLoadXmlMetaData.getName();
                    if ("usb-device".equals(name)) {
                        if (clearCompatibleMatchesLocked(userPackage, DeviceFilter.read(xmlResourceParserLoadXmlMetaData))) {
                            z = true;
                        }
                    } else if ("usb-accessory".equals(name) && clearCompatibleMatchesLocked(userPackage, AccessoryFilter.read(xmlResourceParserLoadXmlMetaData))) {
                        z = true;
                    }
                    XmlUtils.nextElement(xmlResourceParserLoadXmlMetaData);
                }
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
            } catch (Exception e2) {
                e = e2;
                xmlResourceParser = xmlResourceParserLoadXmlMetaData;
                Slog.w(TAG, "Unable to load component info " + activityInfo.toString(), e);
                if (xmlResourceParser != null) {
                    xmlResourceParser.close();
                }
            } catch (Throwable th) {
                th = th;
                if (xmlResourceParserLoadXmlMetaData != null) {
                    xmlResourceParserLoadXmlMetaData.close();
                }
                throw th;
            }
            return z;
        } catch (Throwable th2) {
            th = th2;
            xmlResourceParserLoadXmlMetaData = xmlResourceParser;
        }
    }

    private void handlePackageAdded(UserPackage userPackage) {
        synchronized (this.mLock) {
            try {
                try {
                    ActivityInfo[] activityInfoArr = this.mPackageManager.getPackageInfoAsUser(userPackage.packageName, NetworkConstants.ICMPV6_ECHO_REPLY_TYPE, userPackage.user.getIdentifier()).activities;
                    if (activityInfoArr == null) {
                        return;
                    }
                    boolean z = false;
                    for (int i = 0; i < activityInfoArr.length; i++) {
                        if (handlePackageAddedLocked(userPackage, activityInfoArr[i], "android.hardware.usb.action.USB_DEVICE_ATTACHED")) {
                            z = true;
                        }
                        if (handlePackageAddedLocked(userPackage, activityInfoArr[i], "android.hardware.usb.action.USB_ACCESSORY_ATTACHED")) {
                            z = true;
                        }
                    }
                    if (z) {
                        scheduleWriteSettingsLocked();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "handlePackageUpdate could not find package " + userPackage, e);
                }
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private int getSerial(UserHandle userHandle) {
        return this.mUserManager.getUserSerialNumber(userHandle.getIdentifier());
    }

    void setDevicePackage(UsbDevice usbDevice, String str, UserHandle userHandle) {
        DeviceFilter deviceFilter = new DeviceFilter(usbDevice);
        synchronized (this.mLock) {
            boolean zEquals = true;
            try {
                if (str == null) {
                    if (this.mDevicePreferenceMap.remove(deviceFilter) == null) {
                        zEquals = false;
                    }
                } else {
                    UserPackage userPackage = new UserPackage(str, userHandle);
                    zEquals = true ^ userPackage.equals(this.mDevicePreferenceMap.get(deviceFilter));
                    if (zEquals) {
                        this.mDevicePreferenceMap.put(deviceFilter, userPackage);
                    }
                }
                if (zEquals) {
                    scheduleWriteSettingsLocked();
                }
            } finally {
            }
        }
    }

    void setAccessoryPackage(UsbAccessory usbAccessory, String str, UserHandle userHandle) {
        AccessoryFilter accessoryFilter = new AccessoryFilter(usbAccessory);
        synchronized (this.mLock) {
            boolean zEquals = true;
            try {
                if (str == null) {
                    if (this.mAccessoryPreferenceMap.remove(accessoryFilter) == null) {
                        zEquals = false;
                    }
                } else {
                    UserPackage userPackage = new UserPackage(str, userHandle);
                    zEquals = true ^ userPackage.equals(this.mAccessoryPreferenceMap.get(accessoryFilter));
                    if (zEquals) {
                        this.mAccessoryPreferenceMap.put(accessoryFilter, userPackage);
                    }
                }
                if (zEquals) {
                    scheduleWriteSettingsLocked();
                }
            } finally {
            }
        }
    }

    boolean hasDefaults(String str, UserHandle userHandle) {
        UserPackage userPackage = new UserPackage(str, userHandle);
        synchronized (this.mLock) {
            if (this.mDevicePreferenceMap.values().contains(userPackage)) {
                return true;
            }
            return this.mAccessoryPreferenceMap.values().contains(userPackage);
        }
    }

    void clearDefaults(String str, UserHandle userHandle) {
        UserPackage userPackage = new UserPackage(str, userHandle);
        synchronized (this.mLock) {
            if (clearPackageDefaultsLocked(userPackage)) {
                scheduleWriteSettingsLocked();
            }
        }
    }

    private boolean clearPackageDefaultsLocked(UserPackage userPackage) {
        boolean z;
        synchronized (this.mLock) {
            if (this.mDevicePreferenceMap.containsValue(userPackage)) {
                z = false;
                for (DeviceFilter deviceFilter : (DeviceFilter[]) this.mDevicePreferenceMap.keySet().toArray(new DeviceFilter[0])) {
                    if (userPackage.equals(this.mDevicePreferenceMap.get(deviceFilter))) {
                        this.mDevicePreferenceMap.remove(deviceFilter);
                        z = true;
                    }
                }
            } else {
                z = false;
            }
            if (this.mAccessoryPreferenceMap.containsValue(userPackage)) {
                for (AccessoryFilter accessoryFilter : (AccessoryFilter[]) this.mAccessoryPreferenceMap.keySet().toArray(new AccessoryFilter[0])) {
                    if (userPackage.equals(this.mAccessoryPreferenceMap.get(accessoryFilter))) {
                        this.mAccessoryPreferenceMap.remove(accessoryFilter);
                        z = true;
                    }
                }
            }
        }
        return z;
    }

    public void dump(DualDumpOutputStream dualDumpOutputStream, String str, long j) {
        long jStart = dualDumpOutputStream.start(str, j);
        synchronized (this.mLock) {
            dualDumpOutputStream.write("parent_user_id", 1120986464257L, this.mParentUser.getIdentifier());
            for (DeviceFilter deviceFilter : this.mDevicePreferenceMap.keySet()) {
                long jStart2 = dualDumpOutputStream.start("device_preferences", 2246267895810L);
                deviceFilter.dump(dualDumpOutputStream, "filter", 1146756268033L);
                this.mDevicePreferenceMap.get(deviceFilter).dump(dualDumpOutputStream, "user_package", 1146756268034L);
                dualDumpOutputStream.end(jStart2);
            }
            for (AccessoryFilter accessoryFilter : this.mAccessoryPreferenceMap.keySet()) {
                long jStart3 = dualDumpOutputStream.start("accessory_preferences", 2246267895811L);
                accessoryFilter.dump(dualDumpOutputStream, "filter", 1146756268033L);
                this.mAccessoryPreferenceMap.get(accessoryFilter).dump(dualDumpOutputStream, "user_package", 1146756268034L);
                dualDumpOutputStream.end(jStart3);
            }
        }
        dualDumpOutputStream.end(jStart);
    }

    private static Intent createDeviceAttachedIntent(UsbDevice usbDevice) {
        Intent intent = new Intent("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        intent.putExtra("device", usbDevice);
        intent.addFlags(285212672);
        return intent;
    }
}
