package com.android.server.devicepolicy;

import android.app.AppOpsManagerInternal;
import android.app.admin.SystemUpdateInfo;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class Owners {
    private static final String ATTR_COMPONENT_NAME = "component";
    private static final String ATTR_FREEZE_RECORD_END = "end";
    private static final String ATTR_FREEZE_RECORD_START = "start";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_REMOTE_BUGREPORT_HASH = "remoteBugreportHash";
    private static final String ATTR_REMOTE_BUGREPORT_URI = "remoteBugreportUri";
    private static final String ATTR_USERID = "userId";
    private static final String ATTR_USER_RESTRICTIONS_MIGRATED = "userRestrictionsMigrated";
    private static final boolean DEBUG = false;
    private static final String DEVICE_OWNER_XML = "device_owner_2.xml";
    private static final String DEVICE_OWNER_XML_LEGACY = "device_owner.xml";
    private static final String PROFILE_OWNER_XML = "profile_owner.xml";
    private static final String TAG = "DevicePolicyManagerService";
    private static final String TAG_DEVICE_INITIALIZER = "device-initializer";
    private static final String TAG_DEVICE_OWNER = "device-owner";
    private static final String TAG_DEVICE_OWNER_CONTEXT = "device-owner-context";
    private static final String TAG_FREEZE_PERIOD_RECORD = "freeze-record";
    private static final String TAG_PENDING_OTA_INFO = "pending-ota-info";
    private static final String TAG_PROFILE_OWNER = "profile-owner";
    private static final String TAG_ROOT = "root";
    private static final String TAG_SYSTEM_UPDATE_POLICY = "system-update-policy";
    private OwnerInfo mDeviceOwner;
    private int mDeviceOwnerUserId;
    private final Injector mInjector;
    private final Object mLock;
    private final PackageManagerInternal mPackageManagerInternal;
    private final ArrayMap<Integer, OwnerInfo> mProfileOwners;
    private boolean mSystemReady;
    private LocalDate mSystemUpdateFreezeEnd;
    private LocalDate mSystemUpdateFreezeStart;
    private SystemUpdateInfo mSystemUpdateInfo;
    private SystemUpdatePolicy mSystemUpdatePolicy;
    private final UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;

    public Owners(UserManager userManager, UserManagerInternal userManagerInternal, PackageManagerInternal packageManagerInternal) {
        this(userManager, userManagerInternal, packageManagerInternal, new Injector());
    }

    @VisibleForTesting
    Owners(UserManager userManager, UserManagerInternal userManagerInternal, PackageManagerInternal packageManagerInternal, Injector injector) {
        this.mDeviceOwnerUserId = -10000;
        this.mProfileOwners = new ArrayMap<>();
        this.mLock = new Object();
        this.mUserManager = userManager;
        this.mUserManagerInternal = userManagerInternal;
        this.mPackageManagerInternal = packageManagerInternal;
        this.mInjector = injector;
    }

    void load() {
        synchronized (this.mLock) {
            File legacyConfigFile = getLegacyConfigFile();
            List<UserInfo> users = this.mUserManager.getUsers(true);
            if (readLegacyOwnerFileLocked(legacyConfigFile)) {
                writeDeviceOwner();
                Iterator<Integer> it = getProfileOwnerKeys().iterator();
                while (it.hasNext()) {
                    writeProfileOwner(it.next().intValue());
                }
                if (!legacyConfigFile.delete()) {
                    Slog.e(TAG, "Failed to remove the legacy setting file");
                }
            } else {
                new DeviceOwnerReadWriter().readFromFileLocked();
                Iterator it2 = users.iterator();
                while (it2.hasNext()) {
                    new ProfileOwnerReadWriter(((UserInfo) it2.next()).id).readFromFileLocked();
                }
            }
            this.mUserManagerInternal.setDeviceManaged(hasDeviceOwner());
            for (UserInfo userInfo : users) {
                this.mUserManagerInternal.setUserManaged(userInfo.id, hasProfileOwner(userInfo.id));
            }
            if (hasDeviceOwner() && hasProfileOwner(getDeviceOwnerUserId())) {
                Slog.w(TAG, String.format("User %d has both DO and PO, which is not supported", Integer.valueOf(getDeviceOwnerUserId())));
            }
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    private void pushToPackageManagerLocked() {
        SparseArray sparseArray = new SparseArray();
        for (int size = this.mProfileOwners.size() - 1; size >= 0; size--) {
            sparseArray.put(this.mProfileOwners.keyAt(size).intValue(), this.mProfileOwners.valueAt(size).packageName);
        }
        this.mPackageManagerInternal.setDeviceAndProfileOwnerPackages(this.mDeviceOwnerUserId, this.mDeviceOwner != null ? this.mDeviceOwner.packageName : null, sparseArray);
    }

    String getDeviceOwnerPackageName() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.packageName : null;
        }
        return str;
    }

    int getDeviceOwnerUserId() {
        int i;
        synchronized (this.mLock) {
            i = this.mDeviceOwnerUserId;
        }
        return i;
    }

    Pair<Integer, ComponentName> getDeviceOwnerUserIdAndComponent() {
        synchronized (this.mLock) {
            if (this.mDeviceOwner == null) {
                return null;
            }
            return Pair.create(Integer.valueOf(this.mDeviceOwnerUserId), this.mDeviceOwner.admin);
        }
    }

    String getDeviceOwnerName() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.name : null;
        }
        return str;
    }

    ComponentName getDeviceOwnerComponent() {
        ComponentName componentName;
        synchronized (this.mLock) {
            componentName = this.mDeviceOwner != null ? this.mDeviceOwner.admin : null;
        }
        return componentName;
    }

    String getDeviceOwnerRemoteBugreportUri() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.remoteBugreportUri : null;
        }
        return str;
    }

    String getDeviceOwnerRemoteBugreportHash() {
        String str;
        synchronized (this.mLock) {
            str = this.mDeviceOwner != null ? this.mDeviceOwner.remoteBugreportHash : null;
        }
        return str;
    }

    void setDeviceOwner(ComponentName componentName, String str, int i) {
        if (i < 0) {
            Slog.e(TAG, "Invalid user id for device owner user: " + i);
            return;
        }
        synchronized (this.mLock) {
            setDeviceOwnerWithRestrictionsMigrated(componentName, str, i, true);
        }
    }

    void setDeviceOwnerWithRestrictionsMigrated(ComponentName componentName, String str, int i, boolean z) {
        synchronized (this.mLock) {
            this.mDeviceOwner = new OwnerInfo(str, componentName, z, (String) null, (String) null);
            this.mDeviceOwnerUserId = i;
            this.mUserManagerInternal.setDeviceManaged(true);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void clearDeviceOwner() {
        synchronized (this.mLock) {
            this.mDeviceOwner = null;
            this.mDeviceOwnerUserId = -10000;
            this.mUserManagerInternal.setDeviceManaged(false);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void setProfileOwner(ComponentName componentName, String str, int i) {
        synchronized (this.mLock) {
            this.mProfileOwners.put(Integer.valueOf(i), new OwnerInfo(str, componentName, true, (String) null, (String) null));
            this.mUserManagerInternal.setUserManaged(i, true);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void removeProfileOwner(int i) {
        synchronized (this.mLock) {
            this.mProfileOwners.remove(Integer.valueOf(i));
            this.mUserManagerInternal.setUserManaged(i, false);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void transferProfileOwner(ComponentName componentName, int i) {
        synchronized (this.mLock) {
            OwnerInfo ownerInfo = this.mProfileOwners.get(Integer.valueOf(i));
            this.mProfileOwners.put(Integer.valueOf(i), new OwnerInfo(componentName.getPackageName(), componentName, ownerInfo.userRestrictionsMigrated, ownerInfo.remoteBugreportUri, ownerInfo.remoteBugreportHash));
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    void transferDeviceOwnership(ComponentName componentName) {
        synchronized (this.mLock) {
            this.mDeviceOwner = new OwnerInfo((String) null, componentName, this.mDeviceOwner.userRestrictionsMigrated, this.mDeviceOwner.remoteBugreportUri, this.mDeviceOwner.remoteBugreportHash);
            pushToPackageManagerLocked();
            pushToAppOpsLocked();
        }
    }

    ComponentName getProfileOwnerComponent(int i) {
        ComponentName componentName;
        synchronized (this.mLock) {
            OwnerInfo ownerInfo = this.mProfileOwners.get(Integer.valueOf(i));
            componentName = ownerInfo != null ? ownerInfo.admin : null;
        }
        return componentName;
    }

    String getProfileOwnerName(int i) {
        String str;
        synchronized (this.mLock) {
            OwnerInfo ownerInfo = this.mProfileOwners.get(Integer.valueOf(i));
            str = ownerInfo != null ? ownerInfo.name : null;
        }
        return str;
    }

    String getProfileOwnerPackage(int i) {
        String str;
        synchronized (this.mLock) {
            OwnerInfo ownerInfo = this.mProfileOwners.get(Integer.valueOf(i));
            str = ownerInfo != null ? ownerInfo.packageName : null;
        }
        return str;
    }

    Set<Integer> getProfileOwnerKeys() {
        Set<Integer> setKeySet;
        synchronized (this.mLock) {
            setKeySet = this.mProfileOwners.keySet();
        }
        return setKeySet;
    }

    SystemUpdatePolicy getSystemUpdatePolicy() {
        SystemUpdatePolicy systemUpdatePolicy;
        synchronized (this.mLock) {
            systemUpdatePolicy = this.mSystemUpdatePolicy;
        }
        return systemUpdatePolicy;
    }

    void setSystemUpdatePolicy(SystemUpdatePolicy systemUpdatePolicy) {
        synchronized (this.mLock) {
            this.mSystemUpdatePolicy = systemUpdatePolicy;
        }
    }

    void clearSystemUpdatePolicy() {
        synchronized (this.mLock) {
            this.mSystemUpdatePolicy = null;
        }
    }

    Pair<LocalDate, LocalDate> getSystemUpdateFreezePeriodRecord() {
        Pair<LocalDate, LocalDate> pair;
        synchronized (this.mLock) {
            pair = new Pair<>(this.mSystemUpdateFreezeStart, this.mSystemUpdateFreezeEnd);
        }
        return pair;
    }

    String getSystemUpdateFreezePeriodRecordAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append("start: ");
        if (this.mSystemUpdateFreezeStart != null) {
            sb.append(this.mSystemUpdateFreezeStart.toString());
        } else {
            sb.append("null");
        }
        sb.append("; end: ");
        if (this.mSystemUpdateFreezeEnd != null) {
            sb.append(this.mSystemUpdateFreezeEnd.toString());
        } else {
            sb.append("null");
        }
        return sb.toString();
    }

    boolean setSystemUpdateFreezePeriodRecord(LocalDate localDate, LocalDate localDate2) {
        boolean z;
        synchronized (this.mLock) {
            if (Objects.equals(this.mSystemUpdateFreezeStart, localDate)) {
                z = false;
            } else {
                this.mSystemUpdateFreezeStart = localDate;
                z = true;
            }
            if (!Objects.equals(this.mSystemUpdateFreezeEnd, localDate2)) {
                this.mSystemUpdateFreezeEnd = localDate2;
                z = true;
            }
        }
        return z;
    }

    boolean hasDeviceOwner() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceOwner != null;
        }
        return z;
    }

    boolean isDeviceOwnerUserId(int i) {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceOwner != null && this.mDeviceOwnerUserId == i;
        }
        return z;
    }

    boolean hasProfileOwner(int i) {
        boolean z;
        synchronized (this.mLock) {
            z = getProfileOwnerComponent(i) != null;
        }
        return z;
    }

    boolean getDeviceOwnerUserRestrictionsNeedsMigration() {
        boolean z;
        synchronized (this.mLock) {
            z = (this.mDeviceOwner == null || this.mDeviceOwner.userRestrictionsMigrated) ? false : true;
        }
        return z;
    }

    boolean getProfileOwnerUserRestrictionsNeedsMigration(int i) {
        boolean z;
        synchronized (this.mLock) {
            OwnerInfo ownerInfo = this.mProfileOwners.get(Integer.valueOf(i));
            z = (ownerInfo == null || ownerInfo.userRestrictionsMigrated) ? false : true;
        }
        return z;
    }

    void setDeviceOwnerUserRestrictionsMigrated() {
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.userRestrictionsMigrated = true;
            }
            writeDeviceOwner();
        }
    }

    void setDeviceOwnerRemoteBugreportUriAndHash(String str, String str2) {
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.remoteBugreportUri = str;
                this.mDeviceOwner.remoteBugreportHash = str2;
            }
            writeDeviceOwner();
        }
    }

    void setProfileOwnerUserRestrictionsMigrated(int i) {
        synchronized (this.mLock) {
            OwnerInfo ownerInfo = this.mProfileOwners.get(Integer.valueOf(i));
            if (ownerInfo != null) {
                ownerInfo.userRestrictionsMigrated = true;
            }
            writeProfileOwner(i);
        }
    }

    private boolean readLegacyOwnerFileLocked(File file) {
        OwnerInfo ownerInfo;
        int i;
        if (!file.exists()) {
            return false;
        }
        try {
            FileInputStream fileInputStreamOpenRead = new AtomicFile(file).openRead();
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next != 1) {
                    if (next == 2) {
                        String name = xmlPullParserNewPullParser.getName();
                        if (name.equals(TAG_DEVICE_OWNER)) {
                            this.mDeviceOwner = new OwnerInfo(xmlPullParserNewPullParser.getAttributeValue(null, "name"), xmlPullParserNewPullParser.getAttributeValue(null, "package"), false, (String) null, (String) null);
                            this.mDeviceOwnerUserId = 0;
                        } else if (!name.equals(TAG_DEVICE_INITIALIZER)) {
                            if (name.equals(TAG_PROFILE_OWNER)) {
                                String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                                String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, ATTR_COMPONENT_NAME);
                                int i2 = Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, ATTR_USERID));
                                if (attributeValue3 != null) {
                                    ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(attributeValue3);
                                    if (componentNameUnflattenFromString != null) {
                                        ownerInfo = new OwnerInfo(attributeValue2, componentNameUnflattenFromString, false, (String) null, (String) null);
                                        if (ownerInfo == null) {
                                        }
                                        this.mProfileOwners.put(Integer.valueOf(i), ownerInfo);
                                    } else {
                                        Slog.e(TAG, "Error parsing device-owner file. Bad component name " + attributeValue3);
                                        ownerInfo = null;
                                        if (ownerInfo == null) {
                                        }
                                        this.mProfileOwners.put(Integer.valueOf(i), ownerInfo);
                                    }
                                } else {
                                    ownerInfo = null;
                                    if (ownerInfo == null) {
                                        i = i2;
                                        ownerInfo = new OwnerInfo(attributeValue2, attributeValue, false, (String) null, (String) null);
                                    } else {
                                        i = i2;
                                    }
                                    this.mProfileOwners.put(Integer.valueOf(i), ownerInfo);
                                }
                            } else if (TAG_SYSTEM_UPDATE_POLICY.equals(name)) {
                                this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(xmlPullParserNewPullParser);
                            } else {
                                throw new XmlPullParserException("Unexpected tag in device owner file: " + name);
                            }
                        }
                    }
                } else {
                    fileInputStreamOpenRead.close();
                    break;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Error parsing device-owner file", e);
        }
        return true;
    }

    void writeDeviceOwner() {
        synchronized (this.mLock) {
            new DeviceOwnerReadWriter().writeToFileLocked();
        }
    }

    void writeProfileOwner(int i) {
        synchronized (this.mLock) {
            new ProfileOwnerReadWriter(i).writeToFileLocked();
        }
    }

    boolean saveSystemUpdateInfo(SystemUpdateInfo systemUpdateInfo) {
        synchronized (this.mLock) {
            if (Objects.equals(systemUpdateInfo, this.mSystemUpdateInfo)) {
                return false;
            }
            this.mSystemUpdateInfo = systemUpdateInfo;
            new DeviceOwnerReadWriter().writeToFileLocked();
            return true;
        }
    }

    public SystemUpdateInfo getSystemUpdateInfo() {
        SystemUpdateInfo systemUpdateInfo;
        synchronized (this.mLock) {
            systemUpdateInfo = this.mSystemUpdateInfo;
        }
        return systemUpdateInfo;
    }

    void pushToAppOpsLocked() {
        int packageUid;
        if (!this.mSystemReady) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            SparseIntArray sparseIntArray = new SparseIntArray();
            if (this.mDeviceOwner != null && (packageUid = this.mPackageManagerInternal.getPackageUid(this.mDeviceOwner.packageName, 4333568, this.mDeviceOwnerUserId)) >= 0) {
                sparseIntArray.put(this.mDeviceOwnerUserId, packageUid);
            }
            if (this.mProfileOwners != null) {
                for (int size = this.mProfileOwners.size() - 1; size >= 0; size--) {
                    int packageUid2 = this.mPackageManagerInternal.getPackageUid(this.mProfileOwners.valueAt(size).packageName, 4333568, this.mProfileOwners.keyAt(size).intValue());
                    if (packageUid2 >= 0) {
                        sparseIntArray.put(this.mProfileOwners.keyAt(size).intValue(), packageUid2);
                    }
                }
            }
            AppOpsManagerInternal appOpsManagerInternal = (AppOpsManagerInternal) LocalServices.getService(AppOpsManagerInternal.class);
            if (appOpsManagerInternal != null) {
                if (sparseIntArray.size() <= 0) {
                    sparseIntArray = null;
                }
                appOpsManagerInternal.setDeviceAndProfileOwners(sparseIntArray);
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    public void systemReady() {
        synchronized (this.mLock) {
            this.mSystemReady = true;
            pushToAppOpsLocked();
        }
    }

    private static abstract class FileReadWriter {
        private final File mFile;

        abstract boolean readInner(XmlPullParser xmlPullParser, int i, String str);

        abstract boolean shouldWrite();

        abstract void writeInner(XmlSerializer xmlSerializer) throws IOException;

        protected FileReadWriter(File file) {
            this.mFile = file;
        }

        void writeToFileLocked() {
            FileOutputStream fileOutputStreamStartWrite;
            IOException e;
            if (!shouldWrite()) {
                if (this.mFile.exists() && !this.mFile.delete()) {
                    Slog.e(Owners.TAG, "Failed to remove " + this.mFile.getPath());
                    return;
                }
                return;
            }
            AtomicFile atomicFile = new AtomicFile(this.mFile);
            try {
                fileOutputStreamStartWrite = atomicFile.startWrite();
            } catch (IOException e2) {
                fileOutputStreamStartWrite = null;
                e = e2;
            }
            try {
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, Owners.TAG_ROOT);
                writeInner(fastXmlSerializer);
                fastXmlSerializer.endTag(null, Owners.TAG_ROOT);
                fastXmlSerializer.endDocument();
                fastXmlSerializer.flush();
                atomicFile.finishWrite(fileOutputStreamStartWrite);
            } catch (IOException e3) {
                e = e3;
                Slog.e(Owners.TAG, "Exception when writing", e);
                if (fileOutputStreamStartWrite != null) {
                    atomicFile.failWrite(fileOutputStreamStartWrite);
                }
            }
        }

        void readFromFileLocked() throws Throwable {
            Throwable th;
            FileInputStream fileInputStreamOpenRead;
            Throwable e;
            XmlPullParser xmlPullParserNewPullParser;
            int i;
            if (!this.mFile.exists()) {
                return;
            }
            try {
                fileInputStreamOpenRead = new AtomicFile(this.mFile).openRead();
                try {
                    try {
                        xmlPullParserNewPullParser = Xml.newPullParser();
                        xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                        i = 0;
                    } catch (IOException | XmlPullParserException e2) {
                        e = e2;
                        Slog.e(Owners.TAG, "Error parsing owners information file", e);
                        IoUtils.closeQuietly(fileInputStreamOpenRead);
                        return;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                    throw th;
                }
            } catch (IOException | XmlPullParserException e3) {
                e = e3;
                fileInputStreamOpenRead = null;
            } catch (Throwable th3) {
                th = th3;
                fileInputStreamOpenRead = null;
                IoUtils.closeQuietly(fileInputStreamOpenRead);
                throw th;
            }
            while (true) {
                int next = xmlPullParserNewPullParser.next();
                if (next != 1) {
                    switch (next) {
                        case 2:
                            i++;
                            String name = xmlPullParserNewPullParser.getName();
                            if (i == 1) {
                                if (!Owners.TAG_ROOT.equals(name)) {
                                    Slog.e(Owners.TAG, "Invalid root tag: " + name);
                                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                                    return;
                                }
                            } else {
                                if (!readInner(xmlPullParserNewPullParser, i, name)) {
                                    IoUtils.closeQuietly(fileInputStreamOpenRead);
                                    return;
                                }
                                continue;
                            }
                        case 3:
                            i--;
                            continue;
                        default:
                            continue;
                    }
                    e = e2;
                    Slog.e(Owners.TAG, "Error parsing owners information file", e);
                }
            }
        }
    }

    private class DeviceOwnerReadWriter extends FileReadWriter {
        protected DeviceOwnerReadWriter() {
            super(Owners.this.getDeviceOwnerFile());
        }

        @Override
        boolean shouldWrite() {
            return (Owners.this.mDeviceOwner == null && Owners.this.mSystemUpdatePolicy == null && Owners.this.mSystemUpdateInfo == null) ? false : true;
        }

        @Override
        void writeInner(XmlSerializer xmlSerializer) throws IOException {
            if (Owners.this.mDeviceOwner != null) {
                Owners.this.mDeviceOwner.writeToXml(xmlSerializer, Owners.TAG_DEVICE_OWNER);
                xmlSerializer.startTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
                xmlSerializer.attribute(null, Owners.ATTR_USERID, String.valueOf(Owners.this.mDeviceOwnerUserId));
                xmlSerializer.endTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
            }
            if (Owners.this.mSystemUpdatePolicy != null) {
                xmlSerializer.startTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
                Owners.this.mSystemUpdatePolicy.saveToXml(xmlSerializer);
                xmlSerializer.endTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
            }
            if (Owners.this.mSystemUpdateInfo != null) {
                Owners.this.mSystemUpdateInfo.writeToXml(xmlSerializer, Owners.TAG_PENDING_OTA_INFO);
            }
            if (Owners.this.mSystemUpdateFreezeStart != null || Owners.this.mSystemUpdateFreezeEnd != null) {
                xmlSerializer.startTag(null, Owners.TAG_FREEZE_PERIOD_RECORD);
                if (Owners.this.mSystemUpdateFreezeStart != null) {
                    xmlSerializer.attribute(null, Owners.ATTR_FREEZE_RECORD_START, Owners.this.mSystemUpdateFreezeStart.toString());
                }
                if (Owners.this.mSystemUpdateFreezeEnd != null) {
                    xmlSerializer.attribute(null, Owners.ATTR_FREEZE_RECORD_END, Owners.this.mSystemUpdateFreezeEnd.toString());
                }
                xmlSerializer.endTag(null, Owners.TAG_FREEZE_PERIOD_RECORD);
            }
        }

        @Override
        boolean readInner(XmlPullParser xmlPullParser, int i, String str) {
            if (i > 2) {
                return true;
            }
            switch (str) {
                case "device-owner":
                    Owners.this.mDeviceOwner = OwnerInfo.readFromXml(xmlPullParser);
                    Owners.this.mDeviceOwnerUserId = 0;
                    return true;
                case "device-owner-context":
                    String attributeValue = xmlPullParser.getAttributeValue(null, Owners.ATTR_USERID);
                    try {
                        Owners.this.mDeviceOwnerUserId = Integer.parseInt(attributeValue);
                        break;
                    } catch (NumberFormatException e) {
                        Slog.e(Owners.TAG, "Error parsing user-id " + attributeValue);
                    }
                    return true;
                case "device-initializer":
                    return true;
                case "system-update-policy":
                    Owners.this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(xmlPullParser);
                    return true;
                case "pending-ota-info":
                    Owners.this.mSystemUpdateInfo = SystemUpdateInfo.readFromXml(xmlPullParser);
                    return true;
                case "freeze-record":
                    String attributeValue2 = xmlPullParser.getAttributeValue(null, Owners.ATTR_FREEZE_RECORD_START);
                    String attributeValue3 = xmlPullParser.getAttributeValue(null, Owners.ATTR_FREEZE_RECORD_END);
                    if (attributeValue2 != null && attributeValue3 != null) {
                        Owners.this.mSystemUpdateFreezeStart = LocalDate.parse(attributeValue2);
                        Owners.this.mSystemUpdateFreezeEnd = LocalDate.parse(attributeValue3);
                        if (Owners.this.mSystemUpdateFreezeStart.isAfter(Owners.this.mSystemUpdateFreezeEnd)) {
                            Slog.e(Owners.TAG, "Invalid system update freeze record loaded");
                            Owners.this.mSystemUpdateFreezeStart = null;
                            Owners.this.mSystemUpdateFreezeEnd = null;
                        }
                    }
                    return true;
                default:
                    Slog.e(Owners.TAG, "Unexpected tag: " + str);
                    return false;
            }
        }
    }

    private class ProfileOwnerReadWriter extends FileReadWriter {
        private final int mUserId;

        ProfileOwnerReadWriter(int i) {
            super(Owners.this.getProfileOwnerFile(i));
            this.mUserId = i;
        }

        @Override
        boolean shouldWrite() {
            return Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId)) != null;
        }

        @Override
        void writeInner(XmlSerializer xmlSerializer) throws IOException {
            OwnerInfo ownerInfo = (OwnerInfo) Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId));
            if (ownerInfo != null) {
                ownerInfo.writeToXml(xmlSerializer, Owners.TAG_PROFILE_OWNER);
            }
        }

        @Override
        boolean readInner(XmlPullParser xmlPullParser, int i, String str) {
            if (i > 2) {
                return true;
            }
            byte b = -1;
            if (str.hashCode() == 2145316239 && str.equals(Owners.TAG_PROFILE_OWNER)) {
                b = 0;
            }
            if (b == 0) {
                Owners.this.mProfileOwners.put(Integer.valueOf(this.mUserId), OwnerInfo.readFromXml(xmlPullParser));
                return true;
            }
            Slog.e(Owners.TAG, "Unexpected tag: " + str);
            return false;
        }
    }

    static class OwnerInfo {
        public final ComponentName admin;
        public final String name;
        public final String packageName;
        public String remoteBugreportHash;
        public String remoteBugreportUri;
        public boolean userRestrictionsMigrated;

        public OwnerInfo(String str, String str2, boolean z, String str3, String str4) {
            this.name = str;
            this.packageName = str2;
            this.admin = new ComponentName(str2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            this.userRestrictionsMigrated = z;
            this.remoteBugreportUri = str3;
            this.remoteBugreportHash = str4;
        }

        public OwnerInfo(String str, ComponentName componentName, boolean z, String str2, String str3) {
            this.name = str;
            this.admin = componentName;
            this.packageName = componentName.getPackageName();
            this.userRestrictionsMigrated = z;
            this.remoteBugreportUri = str2;
            this.remoteBugreportHash = str3;
        }

        public void writeToXml(XmlSerializer xmlSerializer, String str) throws IOException {
            xmlSerializer.startTag(null, str);
            xmlSerializer.attribute(null, "package", this.packageName);
            if (this.name != null) {
                xmlSerializer.attribute(null, "name", this.name);
            }
            if (this.admin != null) {
                xmlSerializer.attribute(null, Owners.ATTR_COMPONENT_NAME, this.admin.flattenToString());
            }
            xmlSerializer.attribute(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED, String.valueOf(this.userRestrictionsMigrated));
            if (this.remoteBugreportUri != null) {
                xmlSerializer.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_URI, this.remoteBugreportUri);
            }
            if (this.remoteBugreportHash != null) {
                xmlSerializer.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_HASH, this.remoteBugreportHash);
            }
            xmlSerializer.endTag(null, str);
        }

        public static OwnerInfo readFromXml(XmlPullParser xmlPullParser) {
            String attributeValue = xmlPullParser.getAttributeValue(null, "package");
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "name");
            String attributeValue3 = xmlPullParser.getAttributeValue(null, Owners.ATTR_COMPONENT_NAME);
            boolean zEquals = "true".equals(xmlPullParser.getAttributeValue(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED));
            String attributeValue4 = xmlPullParser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_URI);
            String attributeValue5 = xmlPullParser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_HASH);
            if (attributeValue3 != null) {
                ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(attributeValue3);
                if (componentNameUnflattenFromString != null) {
                    return new OwnerInfo(attributeValue2, componentNameUnflattenFromString, zEquals, attributeValue4, attributeValue5);
                }
                Slog.e(Owners.TAG, "Error parsing owner file. Bad component name " + attributeValue3);
            }
            return new OwnerInfo(attributeValue2, attributeValue, zEquals, attributeValue4, attributeValue5);
        }

        public void dump(String str, PrintWriter printWriter) {
            printWriter.println(str + "admin=" + this.admin);
            printWriter.println(str + "name=" + this.name);
            printWriter.println(str + "package=" + this.packageName);
        }
    }

    public void dump(String str, PrintWriter printWriter) {
        boolean z;
        if (this.mDeviceOwner != null) {
            printWriter.println(str + "Device Owner: ");
            this.mDeviceOwner.dump(str + "  ", printWriter);
            printWriter.println(str + "  User ID: " + this.mDeviceOwnerUserId);
            z = true;
        } else {
            z = false;
        }
        if (this.mSystemUpdatePolicy != null) {
            if (z) {
                printWriter.println();
            }
            printWriter.println(str + "System Update Policy: " + this.mSystemUpdatePolicy);
            z = true;
        }
        if (this.mProfileOwners != null) {
            for (Map.Entry<Integer, OwnerInfo> entry : this.mProfileOwners.entrySet()) {
                if (z) {
                    printWriter.println();
                }
                printWriter.println(str + "Profile Owner (User " + entry.getKey() + "): ");
                OwnerInfo value = entry.getValue();
                StringBuilder sb = new StringBuilder();
                sb.append(str);
                sb.append("  ");
                value.dump(sb.toString(), printWriter);
                z = true;
            }
        }
        if (this.mSystemUpdateInfo != null) {
            if (z) {
                printWriter.println();
            }
            printWriter.println(str + "Pending System Update: " + this.mSystemUpdateInfo);
            z = true;
        }
        if (this.mSystemUpdateFreezeStart != null || this.mSystemUpdateFreezeEnd != null) {
            if (z) {
                printWriter.println();
            }
            printWriter.println(str + "System update freeze record: " + getSystemUpdateFreezePeriodRecordAsString());
        }
    }

    @VisibleForTesting
    File getLegacyConfigFile() {
        return new File(this.mInjector.environmentGetDataSystemDirectory(), DEVICE_OWNER_XML_LEGACY);
    }

    @VisibleForTesting
    File getDeviceOwnerFile() {
        return new File(this.mInjector.environmentGetDataSystemDirectory(), DEVICE_OWNER_XML);
    }

    @VisibleForTesting
    File getProfileOwnerFile(int i) {
        return new File(this.mInjector.environmentGetUserSystemDirectory(i), PROFILE_OWNER_XML);
    }

    @VisibleForTesting
    public static class Injector {
        File environmentGetDataSystemDirectory() {
            return Environment.getDataSystemDirectory();
        }

        File environmentGetUserSystemDirectory(int i) {
            return Environment.getUserSystemDirectory(i);
        }
    }
}
