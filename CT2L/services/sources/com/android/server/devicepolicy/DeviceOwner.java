package com.android.server.devicepolicy;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.RemoteException;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class DeviceOwner {
    private static final String ATTR_COMPONENT_NAME = "component";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_PACKAGE = "package";
    private static final String ATTR_USERID = "userId";
    private static final String DEVICE_OWNER_XML = "device_owner.xml";
    private static final String TAG = "DevicePolicyManagerService";
    private static final String TAG_DEVICE_OWNER = "device-owner";
    private static final String TAG_PROFILE_OWNER = "profile-owner";
    private AtomicFile fileForWriting;
    private OwnerInfo mDeviceOwner;
    private InputStream mInputStreamForTest;
    private OutputStream mOutputStreamForTest;
    private final HashMap<Integer, OwnerInfo> mProfileOwners = new HashMap<>();

    private DeviceOwner() {
    }

    DeviceOwner(InputStream in, OutputStream out) {
        this.mInputStreamForTest = in;
        this.mOutputStreamForTest = out;
    }

    static DeviceOwner load() {
        DeviceOwner owner = new DeviceOwner();
        if (!new File(Environment.getSystemSecureDirectory(), DEVICE_OWNER_XML).exists()) {
            return null;
        }
        owner.readOwnerFile();
        return owner;
    }

    static DeviceOwner createWithDeviceOwner(String packageName, String ownerName) {
        DeviceOwner owner = new DeviceOwner();
        owner.mDeviceOwner = new OwnerInfo(ownerName, packageName);
        return owner;
    }

    static DeviceOwner createWithProfileOwner(String packageName, String ownerName, int userId) {
        DeviceOwner owner = new DeviceOwner();
        owner.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(ownerName, packageName));
        return owner;
    }

    static DeviceOwner createWithProfileOwner(ComponentName admin, String ownerName, int userId) {
        DeviceOwner owner = new DeviceOwner();
        owner.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(ownerName, admin));
        return owner;
    }

    String getDeviceOwnerPackageName() {
        if (this.mDeviceOwner != null) {
            return this.mDeviceOwner.packageName;
        }
        return null;
    }

    String getDeviceOwnerName() {
        if (this.mDeviceOwner != null) {
            return this.mDeviceOwner.name;
        }
        return null;
    }

    void setDeviceOwner(String packageName, String ownerName) {
        this.mDeviceOwner = new OwnerInfo(ownerName, packageName);
    }

    void clearDeviceOwner() {
        this.mDeviceOwner = null;
    }

    void setProfileOwner(String packageName, String ownerName, int userId) {
        this.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(ownerName, packageName));
    }

    void setProfileOwner(ComponentName admin, String ownerName, int userId) {
        this.mProfileOwners.put(Integer.valueOf(userId), new OwnerInfo(ownerName, admin));
    }

    void removeProfileOwner(int userId) {
        this.mProfileOwners.remove(Integer.valueOf(userId));
    }

    String getProfileOwnerPackageName(int userId) {
        OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
        if (profileOwner != null) {
            return profileOwner.packageName;
        }
        return null;
    }

    ComponentName getProfileOwnerComponent(int userId) {
        OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
        if (profileOwner != null) {
            return profileOwner.admin;
        }
        return null;
    }

    String getProfileOwnerName(int userId) {
        OwnerInfo profileOwner = this.mProfileOwners.get(Integer.valueOf(userId));
        if (profileOwner != null) {
            return profileOwner.name;
        }
        return null;
    }

    Set<Integer> getProfileOwnerKeys() {
        return this.mProfileOwners.keySet();
    }

    boolean hasDeviceOwner() {
        return this.mDeviceOwner != null;
    }

    static boolean isInstalled(String packageName, PackageManager pm) {
        try {
            PackageInfo pi = pm.getPackageInfo(packageName, 0);
            if (pi != null) {
                return pi.applicationInfo.flags != 0;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.w(TAG, "Device Owner package " + packageName + " not installed.");
            return false;
        }
    }

    static boolean isInstalledForUser(String packageName, int userHandle) {
        try {
            PackageInfo pi = AppGlobals.getPackageManager().getPackageInfo(packageName, 0, userHandle);
            if (pi != null) {
                return pi.applicationInfo.flags != 0;
            }
            return false;
        } catch (RemoteException re) {
            throw new RuntimeException("Package manager has died", re);
        }
    }

    void readOwnerFile() {
        try {
            InputStream input = openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, null);
            while (true) {
                int type = parser.next();
                if (type != 1) {
                    if (type == 2) {
                        String tag = parser.getName();
                        if (tag.equals(TAG_DEVICE_OWNER)) {
                            String name = parser.getAttributeValue(null, ATTR_NAME);
                            String packageName = parser.getAttributeValue(null, ATTR_PACKAGE);
                            this.mDeviceOwner = new OwnerInfo(name, packageName);
                        } else if (tag.equals(TAG_PROFILE_OWNER)) {
                            String profileOwnerPackageName = parser.getAttributeValue(null, ATTR_PACKAGE);
                            String profileOwnerName = parser.getAttributeValue(null, ATTR_NAME);
                            String profileOwnerComponentStr = parser.getAttributeValue(null, ATTR_COMPONENT_NAME);
                            int userId = Integer.parseInt(parser.getAttributeValue(null, ATTR_USERID));
                            OwnerInfo profileOwnerInfo = null;
                            if (profileOwnerComponentStr != null) {
                                ComponentName admin = ComponentName.unflattenFromString(profileOwnerComponentStr);
                                if (admin != null) {
                                    profileOwnerInfo = new OwnerInfo(profileOwnerName, admin);
                                } else {
                                    Slog.e(TAG, "Error parsing device-owner file. Bad component name " + profileOwnerComponentStr);
                                }
                            }
                            if (profileOwnerInfo == null) {
                                profileOwnerInfo = new OwnerInfo(profileOwnerName, profileOwnerPackageName);
                            }
                            this.mProfileOwners.put(Integer.valueOf(userId), profileOwnerInfo);
                        } else {
                            throw new XmlPullParserException("Unexpected tag in device owner file: " + tag);
                        }
                    }
                } else {
                    input.close();
                    return;
                }
            }
        } catch (IOException ioe) {
            Slog.e(TAG, "IO Exception when reading device-owner file\n" + ioe);
        } catch (XmlPullParserException xppe) {
            Slog.e(TAG, "Error parsing device-owner file\n" + xppe);
        }
    }

    void writeOwnerFile() {
        synchronized (this) {
            writeOwnerFileLocked();
        }
    }

    private void writeOwnerFileLocked() {
        try {
            OutputStream outputStream = startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(outputStream, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            if (this.mDeviceOwner != null) {
                fastXmlSerializer.startTag(null, TAG_DEVICE_OWNER);
                fastXmlSerializer.attribute(null, ATTR_PACKAGE, this.mDeviceOwner.packageName);
                if (this.mDeviceOwner.name != null) {
                    fastXmlSerializer.attribute(null, ATTR_NAME, this.mDeviceOwner.name);
                }
                fastXmlSerializer.endTag(null, TAG_DEVICE_OWNER);
            }
            if (this.mProfileOwners.size() > 0) {
                for (Map.Entry<Integer, OwnerInfo> owner : this.mProfileOwners.entrySet()) {
                    fastXmlSerializer.startTag(null, TAG_PROFILE_OWNER);
                    OwnerInfo ownerInfo = owner.getValue();
                    fastXmlSerializer.attribute(null, ATTR_PACKAGE, ownerInfo.packageName);
                    fastXmlSerializer.attribute(null, ATTR_NAME, ownerInfo.name);
                    fastXmlSerializer.attribute(null, ATTR_USERID, Integer.toString(owner.getKey().intValue()));
                    if (ownerInfo.admin != null) {
                        fastXmlSerializer.attribute(null, ATTR_COMPONENT_NAME, ownerInfo.admin.flattenToString());
                    }
                    fastXmlSerializer.endTag(null, TAG_PROFILE_OWNER);
                }
            }
            fastXmlSerializer.endDocument();
            fastXmlSerializer.flush();
            finishWrite(outputStream);
        } catch (IOException ioe) {
            Slog.e(TAG, "IO Exception when writing device-owner file\n" + ioe);
        }
    }

    private InputStream openRead() throws IOException {
        return this.mInputStreamForTest != null ? this.mInputStreamForTest : new AtomicFile(new File(Environment.getSystemSecureDirectory(), DEVICE_OWNER_XML)).openRead();
    }

    private OutputStream startWrite() throws IOException {
        if (this.mOutputStreamForTest != null) {
            return this.mOutputStreamForTest;
        }
        this.fileForWriting = new AtomicFile(new File(Environment.getSystemSecureDirectory(), DEVICE_OWNER_XML));
        return this.fileForWriting.startWrite();
    }

    private void finishWrite(OutputStream stream) {
        if (this.fileForWriting != null) {
            this.fileForWriting.finishWrite((FileOutputStream) stream);
        }
    }

    static class OwnerInfo {
        public ComponentName admin;
        public String name;
        public String packageName;

        public OwnerInfo(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
            this.admin = new ComponentName(packageName, "");
        }

        public OwnerInfo(String name, ComponentName admin) {
            this.name = name;
            this.admin = admin;
            this.packageName = admin.getPackageName();
        }
    }
}
