package com.android.server.devicepolicy;

import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.pm.PackageManagerInternal;
import android.content.pm.UserInfo;
import android.os.Environment;
import android.os.UserManager;
import android.os.UserManagerInternal;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class Owners {
    private static final String ATTR_COMPONENT_NAME = "component";
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
    private static final String TAG_PROFILE_OWNER = "profile-owner";
    private static final String TAG_ROOT = "root";
    private static final String TAG_SYSTEM_UPDATE_POLICY = "system-update-policy";
    private OwnerInfo mDeviceOwner;
    private final PackageManagerInternal mPackageManagerInternal;
    private SystemUpdatePolicy mSystemUpdatePolicy;
    private final UserManager mUserManager;
    private final UserManagerInternal mUserManagerInternal;
    private int mDeviceOwnerUserId = -10000;
    private final ArrayMap<Integer, OwnerInfo> mProfileOwners = new ArrayMap<>();
    private final Object mLock = new Object();

    public Owners(UserManager userManager, UserManagerInternal userManagerInternal, PackageManagerInternal packageManagerInternal) {
        this.mUserManager = userManager;
        this.mUserManagerInternal = userManagerInternal;
        this.mPackageManagerInternal = packageManagerInternal;
    }

    void load() {
        synchronized (this.mLock) {
            File legacy = getLegacyConfigFileWithTestOverride();
            List<UserInfo> users = this.mUserManager.getUsers(true);
            if (readLegacyOwnerFileLocked(legacy)) {
                writeDeviceOwner();
                Iterator userId$iterator = getProfileOwnerKeys().iterator();
                while (userId$iterator.hasNext()) {
                    int userId = ((Integer) userId$iterator.next()).intValue();
                    writeProfileOwner(userId);
                }
                if (!legacy.delete()) {
                    Slog.e(TAG, "Failed to remove the legacy setting file");
                }
            } else {
                new DeviceOwnerReadWriter().readFromFileLocked();
                Iterator ui$iterator = users.iterator();
                while (ui$iterator.hasNext()) {
                    new ProfileOwnerReadWriter(((UserInfo) ui$iterator.next()).id).readFromFileLocked();
                }
            }
            this.mUserManagerInternal.setDeviceManaged(hasDeviceOwner());
            for (UserInfo ui : users) {
                this.mUserManagerInternal.setUserManaged(ui.id, hasProfileOwner(ui.id));
            }
            if (hasDeviceOwner() && hasProfileOwner(getDeviceOwnerUserId())) {
                Slog.w(TAG, String.format("User %d has both DO and PO, which is not supported", Integer.valueOf(getDeviceOwnerUserId())));
            }
            pushToPackageManagerLocked();
        }
    }

    private void pushToPackageManagerLocked() {
        SparseArray<String> po = new SparseArray<>();
        for (int i = this.mProfileOwners.size() - 1; i >= 0; i--) {
            po.put(this.mProfileOwners.keyAt(i).intValue(), this.mProfileOwners.valueAt(i).packageName);
        }
        this.mPackageManagerInternal.setDeviceAndProfileOwnerPackages(this.mDeviceOwnerUserId, this.mDeviceOwner != null ? this.mDeviceOwner.packageName : null, po);
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

    void setDeviceOwner(ComponentName admin, String ownerName, int userId) {
        if (userId < 0) {
            Slog.e(TAG, "Invalid user id for device owner user: " + userId);
            return;
        }
        synchronized (this.mLock) {
            setDeviceOwnerWithRestrictionsMigrated(admin, ownerName, userId, true);
        }
    }

    void setDeviceOwnerWithRestrictionsMigrated(ComponentName admin, String ownerName, int userId, boolean userRestrictionsMigrated) {
        synchronized (this.mLock) {
            this.mDeviceOwner = new OwnerInfo(ownerName, admin, userRestrictionsMigrated, (String) null, (String) null);
            this.mDeviceOwnerUserId = userId;
            this.mUserManagerInternal.setDeviceManaged(true);
            pushToPackageManagerLocked();
        }
    }

    void clearDeviceOwner() {
        synchronized (this.mLock) {
            this.mDeviceOwner = null;
            this.mDeviceOwnerUserId = -10000;
            this.mUserManagerInternal.setDeviceManaged(false);
            pushToPackageManagerLocked();
        }
    }

    void setProfileOwner(ComponentName admin, String ownerName, int userId) {
        synchronized (this.mLock) {
            this.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(ownerName, admin, true, (String) null, (String) null));
            this.mUserManagerInternal.setUserManaged(userId, true);
            pushToPackageManagerLocked();
        }
    }

    void removeProfileOwner(int userId) {
        synchronized (this.mLock) {
            this.mProfileOwners.remove(Integer.valueOf(userId));
            this.mUserManagerInternal.setUserManaged(userId, false);
            pushToPackageManagerLocked();
        }
    }

    ComponentName getProfileOwnerComponent(int userId) {
        ComponentName componentName;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
            componentName = profileOwner != null ? profileOwner.admin : null;
        }
        return componentName;
    }

    String getProfileOwnerName(int userId) {
        String str;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
            str = profileOwner != null ? profileOwner.name : null;
        }
        return str;
    }

    String getProfileOwnerPackage(int userId) {
        String str;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
            str = profileOwner != null ? profileOwner.packageName : null;
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

    boolean hasDeviceOwner() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mDeviceOwner != null;
        }
        return z;
    }

    boolean isDeviceOwnerUserId(int userId) {
        boolean z = false;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                if (this.mDeviceOwnerUserId == userId) {
                    z = true;
                }
            }
        }
        return z;
    }

    boolean hasProfileOwner(int userId) {
        boolean z;
        synchronized (this.mLock) {
            z = getProfileOwnerComponent(userId) != null;
        }
        return z;
    }

    boolean getDeviceOwnerUserRestrictionsNeedsMigration() {
        boolean z = false;
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                if (!this.mDeviceOwner.userRestrictionsMigrated) {
                    z = true;
                }
            }
        }
        return z;
    }

    boolean getProfileOwnerUserRestrictionsNeedsMigration(int userId) {
        boolean z = false;
        synchronized (this.mLock) {
            OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                if (!profileOwner.userRestrictionsMigrated) {
                    z = true;
                }
            }
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

    void setDeviceOwnerRemoteBugreportUriAndHash(String remoteBugreportUri, String remoteBugreportHash) {
        synchronized (this.mLock) {
            if (this.mDeviceOwner != null) {
                this.mDeviceOwner.remoteBugreportUri = remoteBugreportUri;
                this.mDeviceOwner.remoteBugreportHash = remoteBugreportHash;
            }
            writeDeviceOwner();
        }
    }

    void setProfileOwnerUserRestrictionsMigrated(int userId) {
        synchronized (this.mLock) {
            OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
            if (profileOwner != null) {
                profileOwner.userRestrictionsMigrated = true;
            }
            writeProfileOwner(userId);
        }
    }

    private boolean readLegacyOwnerFileLocked(File file) {
        if (!file.exists()) {
            return false;
        }
        try {
            InputStream input = new AtomicFile(file).openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, StandardCharsets.UTF_8.name());
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type == 2) {
                        String tag = parser.getName();
                        if (tag.equals(TAG_DEVICE_OWNER)) {
                            String name = parser.getAttributeValue(null, ATTR_NAME);
                            String packageName = parser.getAttributeValue(null, ATTR_PACKAGE);
                            this.mDeviceOwner = new OwnerInfo(name, packageName, false, (String) null, (String) null);
                            this.mDeviceOwnerUserId = 0;
                        } else if (tag.equals(TAG_DEVICE_INITIALIZER)) {
                            continue;
                        } else if (tag.equals(TAG_PROFILE_OWNER)) {
                            String profileOwnerPackageName = parser.getAttributeValue(null, ATTR_PACKAGE);
                            String profileOwnerName = parser.getAttributeValue(null, ATTR_NAME);
                            String profileOwnerComponentStr = parser.getAttributeValue(null, ATTR_COMPONENT_NAME);
                            int userId = Integer.parseInt(parser.getAttributeValue(null, ATTR_USERID));
                            OwnerInfo ownerInfo = null;
                            if (profileOwnerComponentStr != null) {
                                ComponentName admin = ComponentName.unflattenFromString(profileOwnerComponentStr);
                                if (admin != null) {
                                    ownerInfo = new OwnerInfo(profileOwnerName, admin, false, (String) null, (String) null);
                                } else {
                                    Slog.e(TAG, "Error parsing device-owner file. Bad component name " + profileOwnerComponentStr);
                                }
                            }
                            if (ownerInfo == null) {
                                ownerInfo = new OwnerInfo(profileOwnerName, profileOwnerPackageName, false, (String) null, (String) null);
                            }
                            this.mProfileOwners.put(Integer.valueOf(userId), ownerInfo);
                        } else if (TAG_SYSTEM_UPDATE_POLICY.equals(tag)) {
                            this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(parser);
                        } else {
                            throw new XmlPullParserException("Unexpected tag in device owner file: " + tag);
                        }
                    }
                } else {
                    input.close();
                    return true;
                }
            }
        } catch (IOException | XmlPullParserException e) {
            Slog.e(TAG, "Error parsing device-owner file", e);
            return true;
        }
    }

    void writeDeviceOwner() {
        synchronized (this.mLock) {
            new DeviceOwnerReadWriter().writeToFileLocked();
        }
    }

    void writeProfileOwner(int userId) {
        synchronized (this.mLock) {
            new ProfileOwnerReadWriter(userId).writeToFileLocked();
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
            if (!shouldWrite()) {
                if (this.mFile.exists() && !this.mFile.delete()) {
                    Slog.e(Owners.TAG, "Failed to remove " + this.mFile.getPath());
                    return;
                }
                return;
            }
            AtomicFile f = new AtomicFile(this.mFile);
            FileOutputStream outputStream = null;
            try {
                outputStream = f.startWrite();
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(outputStream, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.startTag(null, Owners.TAG_ROOT);
                writeInner(fastXmlSerializer);
                fastXmlSerializer.endTag(null, Owners.TAG_ROOT);
                fastXmlSerializer.endDocument();
                fastXmlSerializer.flush();
                f.finishWrite(outputStream);
            } catch (IOException e) {
                Slog.e(Owners.TAG, "Exception when writing", e);
                if (outputStream == null) {
                    return;
                }
                f.failWrite(outputStream);
            }
        }

        void readFromFileLocked() {
            if (this.mFile.exists()) {
                AtomicFile f = new AtomicFile(this.mFile);
                InputStream input = null;
                try {
                    input = f.openRead();
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(input, StandardCharsets.UTF_8.name());
                    int depth = 0;
                    while (true) {
                        int type = parser.next();
                        if (type != 1) {
                            switch (type) {
                                case 2:
                                    depth++;
                                    String tag = parser.getName();
                                    if (depth == 1) {
                                        if (!Owners.TAG_ROOT.equals(tag)) {
                                            Slog.e(Owners.TAG, "Invalid root tag: " + tag);
                                            return;
                                        }
                                    } else if (!readInner(parser, depth, tag)) {
                                        return;
                                    }
                                case 3:
                                    depth--;
                                    break;
                            }
                        } else {
                            return;
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    Slog.e(Owners.TAG, "Error parsing device-owner file", e);
                } finally {
                    IoUtils.closeQuietly(input);
                }
            }
        }
    }

    private class DeviceOwnerReadWriter extends FileReadWriter {
        protected DeviceOwnerReadWriter() {
            super(Owners.this.getDeviceOwnerFileWithTestOverride());
        }

        @Override
        boolean shouldWrite() {
            return (Owners.this.mDeviceOwner == null && Owners.this.mSystemUpdatePolicy == null) ? false : true;
        }

        @Override
        void writeInner(XmlSerializer out) throws IOException {
            if (Owners.this.mDeviceOwner != null) {
                Owners.this.mDeviceOwner.writeToXml(out, Owners.TAG_DEVICE_OWNER);
                out.startTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
                out.attribute(null, Owners.ATTR_USERID, String.valueOf(Owners.this.mDeviceOwnerUserId));
                out.endTag(null, Owners.TAG_DEVICE_OWNER_CONTEXT);
            }
            if (Owners.this.mSystemUpdatePolicy == null) {
                return;
            }
            out.startTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
            Owners.this.mSystemUpdatePolicy.saveToXml(out);
            out.endTag(null, Owners.TAG_SYSTEM_UPDATE_POLICY);
        }

        @Override
        boolean readInner(XmlPullParser parser, int depth, String tag) {
            if (depth > 2) {
                return true;
            }
            if (!tag.equals(Owners.TAG_DEVICE_OWNER)) {
                if (!tag.equals(Owners.TAG_DEVICE_OWNER_CONTEXT)) {
                    if (!tag.equals(Owners.TAG_DEVICE_INITIALIZER)) {
                        if (tag.equals(Owners.TAG_SYSTEM_UPDATE_POLICY)) {
                            Owners.this.mSystemUpdatePolicy = SystemUpdatePolicy.restoreFromXml(parser);
                        } else {
                            Slog.e(Owners.TAG, "Unexpected tag: " + tag);
                            return false;
                        }
                    }
                } else {
                    String userIdString = parser.getAttributeValue(null, Owners.ATTR_USERID);
                    try {
                        Owners.this.mDeviceOwnerUserId = Integer.parseInt(userIdString);
                    } catch (NumberFormatException e) {
                        Slog.e(Owners.TAG, "Error parsing user-id " + userIdString);
                    }
                }
            } else {
                Owners.this.mDeviceOwner = OwnerInfo.readFromXml(parser);
                Owners.this.mDeviceOwnerUserId = 0;
            }
            return true;
        }
    }

    private class ProfileOwnerReadWriter extends FileReadWriter {
        private final int mUserId;

        ProfileOwnerReadWriter(int userId) {
            super(Owners.this.getProfileOwnerFileWithTestOverride(userId));
            this.mUserId = userId;
        }

        @Override
        boolean shouldWrite() {
            return Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId)) != null;
        }

        @Override
        void writeInner(XmlSerializer out) throws IOException {
            OwnerInfo profileOwner = (OwnerInfo) Owners.this.mProfileOwners.get(Integer.valueOf(this.mUserId));
            if (profileOwner == null) {
                return;
            }
            profileOwner.writeToXml(out, Owners.TAG_PROFILE_OWNER);
        }

        @Override
        boolean readInner(XmlPullParser parser, int depth, String tag) {
            if (depth > 2) {
                return true;
            }
            if (tag.equals(Owners.TAG_PROFILE_OWNER)) {
                Owners.this.mProfileOwners.put(Integer.valueOf(this.mUserId), OwnerInfo.readFromXml(parser));
                return true;
            }
            Slog.e(Owners.TAG, "Unexpected tag: " + tag);
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

        public OwnerInfo(String name, String packageName, boolean userRestrictionsMigrated, String remoteBugreportUri, String remoteBugreportHash) {
            this.name = name;
            this.packageName = packageName;
            this.admin = new ComponentName(packageName, "");
            this.userRestrictionsMigrated = userRestrictionsMigrated;
            this.remoteBugreportUri = remoteBugreportUri;
            this.remoteBugreportHash = remoteBugreportHash;
        }

        public OwnerInfo(String name, ComponentName admin, boolean userRestrictionsMigrated, String remoteBugreportUri, String remoteBugreportHash) {
            this.name = name;
            this.admin = admin;
            this.packageName = admin.getPackageName();
            this.userRestrictionsMigrated = userRestrictionsMigrated;
            this.remoteBugreportUri = remoteBugreportUri;
            this.remoteBugreportHash = remoteBugreportHash;
        }

        public void writeToXml(XmlSerializer out, String tag) throws IOException {
            out.startTag(null, tag);
            out.attribute(null, Owners.ATTR_PACKAGE, this.packageName);
            if (this.name != null) {
                out.attribute(null, Owners.ATTR_NAME, this.name);
            }
            if (this.admin != null) {
                out.attribute(null, Owners.ATTR_COMPONENT_NAME, this.admin.flattenToString());
            }
            out.attribute(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED, String.valueOf(this.userRestrictionsMigrated));
            if (this.remoteBugreportUri != null) {
                out.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_URI, this.remoteBugreportUri);
            }
            if (this.remoteBugreportHash != null) {
                out.attribute(null, Owners.ATTR_REMOTE_BUGREPORT_HASH, this.remoteBugreportHash);
            }
            out.endTag(null, tag);
        }

        public static OwnerInfo readFromXml(XmlPullParser parser) {
            String packageName = parser.getAttributeValue(null, Owners.ATTR_PACKAGE);
            String name = parser.getAttributeValue(null, Owners.ATTR_NAME);
            String componentName = parser.getAttributeValue(null, Owners.ATTR_COMPONENT_NAME);
            String userRestrictionsMigratedStr = parser.getAttributeValue(null, Owners.ATTR_USER_RESTRICTIONS_MIGRATED);
            boolean userRestrictionsMigrated = "true".equals(userRestrictionsMigratedStr);
            String remoteBugreportUri = parser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_URI);
            String remoteBugreportHash = parser.getAttributeValue(null, Owners.ATTR_REMOTE_BUGREPORT_HASH);
            if (componentName != null) {
                ComponentName admin = ComponentName.unflattenFromString(componentName);
                if (admin != null) {
                    return new OwnerInfo(name, admin, userRestrictionsMigrated, remoteBugreportUri, remoteBugreportHash);
                }
                Slog.e(Owners.TAG, "Error parsing owner file. Bad component name " + componentName);
            }
            return new OwnerInfo(name, packageName, userRestrictionsMigrated, remoteBugreportUri, remoteBugreportHash);
        }

        public void dump(String prefix, PrintWriter pw) {
            pw.println(prefix + "admin=" + this.admin);
            pw.println(prefix + "name=" + this.name);
            pw.println(prefix + "package=" + this.packageName);
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        boolean needBlank = false;
        if (this.mDeviceOwner != null) {
            pw.println(prefix + "Device Owner: ");
            this.mDeviceOwner.dump(prefix + "  ", pw);
            pw.println(prefix + "  User ID: " + this.mDeviceOwnerUserId);
            needBlank = true;
        }
        if (this.mSystemUpdatePolicy != null) {
            if (needBlank) {
                pw.println();
            }
            pw.println(prefix + "System Update Policy: " + this.mSystemUpdatePolicy);
            needBlank = true;
        }
        if (this.mProfileOwners == null) {
            return;
        }
        for (Map.Entry<Integer, OwnerInfo> entry : this.mProfileOwners.entrySet()) {
            if (needBlank) {
                pw.println();
            }
            pw.println(prefix + "Profile Owner (User " + entry.getKey() + "): ");
            entry.getValue().dump(prefix + "  ", pw);
            needBlank = true;
        }
    }

    File getLegacyConfigFileWithTestOverride() {
        return new File(Environment.getDataSystemDirectory(), DEVICE_OWNER_XML_LEGACY);
    }

    File getDeviceOwnerFileWithTestOverride() {
        return new File(Environment.getDataSystemDirectory(), DEVICE_OWNER_XML);
    }

    File getProfileOwnerFileWithTestOverride(int userId) {
        return new File(Environment.getUserSystemDirectory(userId), PROFILE_OWNER_XML);
    }
}
