package com.android.server;

import android.accounts.GrantCredentialsPermissionActivity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SystemConfig {
    private static final int ALLOW_ALL = -1;
    private static final int ALLOW_APP_CONFIGS = 8;
    private static final int ALLOW_FEATURES = 1;
    private static final int ALLOW_HIDDENAPI_WHITELISTING = 64;
    private static final int ALLOW_LIBS = 2;
    private static final int ALLOW_OEM_PERMISSIONS = 32;
    private static final int ALLOW_PERMISSIONS = 4;
    private static final int ALLOW_PRIVAPP_PERMISSIONS = 16;
    static final String TAG = "SystemConfig";
    static SystemConfig sInstance;
    int[] mGlobalGids;
    final SparseArray<ArraySet<String>> mSystemPermissions = new SparseArray<>();
    final ArrayMap<String, String> mSharedLibraries = new ArrayMap<>();
    final ArrayMap<String, FeatureInfo> mAvailableFeatures = new ArrayMap<>();
    final ArraySet<String> mUnavailableFeatures = new ArraySet<>();
    final ArrayMap<String, PermissionEntry> mPermissions = new ArrayMap<>();
    final ArraySet<String> mAllowInPowerSaveExceptIdle = new ArraySet<>();
    final ArraySet<String> mAllowInPowerSave = new ArraySet<>();
    final ArraySet<String> mAllowInDataUsageSave = new ArraySet<>();
    final ArraySet<String> mAllowUnthrottledLocation = new ArraySet<>();
    final ArraySet<String> mAllowImplicitBroadcasts = new ArraySet<>();
    final ArraySet<String> mLinkedApps = new ArraySet<>();
    final ArraySet<String> mSystemUserWhitelistedApps = new ArraySet<>();
    final ArraySet<String> mSystemUserBlacklistedApps = new ArraySet<>();
    final ArraySet<ComponentName> mDefaultVrComponents = new ArraySet<>();
    final ArraySet<ComponentName> mBackupTransportWhitelist = new ArraySet<>();
    final ArraySet<String> mHiddenApiPackageWhitelist = new ArraySet<>();
    final ArraySet<String> mDisabledUntilUsedPreinstalledCarrierApps = new ArraySet<>();
    final ArrayMap<String, List<String>> mDisabledUntilUsedPreinstalledCarrierAssociatedApps = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mVendorPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mVendorPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mProductPrivAppPermissions = new ArrayMap<>();
    final ArrayMap<String, ArraySet<String>> mProductPrivAppDenyPermissions = new ArrayMap<>();
    final ArrayMap<String, ArrayMap<String, Boolean>> mOemPermissions = new ArrayMap<>();

    public static final class PermissionEntry {
        public int[] gids;
        public final String name;
        public boolean perUser;

        PermissionEntry(String str, boolean z) {
            this.name = str;
            this.perUser = z;
        }
    }

    public static SystemConfig getInstance() {
        SystemConfig systemConfig;
        synchronized (SystemConfig.class) {
            if (sInstance == null) {
                sInstance = new SystemConfig();
            }
            systemConfig = sInstance;
        }
        return systemConfig;
    }

    public int[] getGlobalGids() {
        return this.mGlobalGids;
    }

    public SparseArray<ArraySet<String>> getSystemPermissions() {
        return this.mSystemPermissions;
    }

    public ArrayMap<String, String> getSharedLibraries() {
        return this.mSharedLibraries;
    }

    public ArrayMap<String, FeatureInfo> getAvailableFeatures() {
        return this.mAvailableFeatures;
    }

    public ArrayMap<String, PermissionEntry> getPermissions() {
        return this.mPermissions;
    }

    public ArraySet<String> getAllowImplicitBroadcasts() {
        return this.mAllowImplicitBroadcasts;
    }

    public ArraySet<String> getAllowInPowerSaveExceptIdle() {
        return this.mAllowInPowerSaveExceptIdle;
    }

    public ArraySet<String> getAllowInPowerSave() {
        return this.mAllowInPowerSave;
    }

    public ArraySet<String> getAllowInDataUsageSave() {
        return this.mAllowInDataUsageSave;
    }

    public ArraySet<String> getAllowUnthrottledLocation() {
        return this.mAllowUnthrottledLocation;
    }

    public ArraySet<String> getLinkedApps() {
        return this.mLinkedApps;
    }

    public ArraySet<String> getSystemUserWhitelistedApps() {
        return this.mSystemUserWhitelistedApps;
    }

    public ArraySet<String> getSystemUserBlacklistedApps() {
        return this.mSystemUserBlacklistedApps;
    }

    public ArraySet<String> getHiddenApiWhitelistedApps() {
        return this.mHiddenApiPackageWhitelist;
    }

    public ArraySet<ComponentName> getDefaultVrComponents() {
        return this.mDefaultVrComponents;
    }

    public ArraySet<ComponentName> getBackupTransportWhitelist() {
        return this.mBackupTransportWhitelist;
    }

    public ArraySet<String> getDisabledUntilUsedPreinstalledCarrierApps() {
        return this.mDisabledUntilUsedPreinstalledCarrierApps;
    }

    public ArrayMap<String, List<String>> getDisabledUntilUsedPreinstalledCarrierAssociatedApps() {
        return this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps;
    }

    public ArraySet<String> getPrivAppPermissions(String str) {
        return this.mPrivAppPermissions.get(str);
    }

    public ArraySet<String> getPrivAppDenyPermissions(String str) {
        return this.mPrivAppDenyPermissions.get(str);
    }

    public ArraySet<String> getVendorPrivAppPermissions(String str) {
        return this.mVendorPrivAppPermissions.get(str);
    }

    public ArraySet<String> getVendorPrivAppDenyPermissions(String str) {
        return this.mVendorPrivAppDenyPermissions.get(str);
    }

    public ArraySet<String> getProductPrivAppPermissions(String str) {
        return this.mProductPrivAppPermissions.get(str);
    }

    public ArraySet<String> getProductPrivAppDenyPermissions(String str) {
        return this.mProductPrivAppDenyPermissions.get(str);
    }

    public Map<String, Boolean> getOemPermissions(String str) {
        ArrayMap<String, Boolean> arrayMap = this.mOemPermissions.get(str);
        if (arrayMap != null) {
            return arrayMap;
        }
        return Collections.emptyMap();
    }

    SystemConfig() {
        readPermissions(Environment.buildPath(Environment.getRootDirectory(), "etc", "sysconfig"), -1);
        readPermissions(Environment.buildPath(Environment.getRootDirectory(), "etc", StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS), -1);
        int i = Build.VERSION.FIRST_SDK_INT <= 27 ? 31 : 19;
        readPermissions(Environment.buildPath(Environment.getVendorDirectory(), "etc", "sysconfig"), i);
        readPermissions(Environment.buildPath(Environment.getVendorDirectory(), "etc", StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS), i);
        readPermissions(Environment.buildPath(Environment.getOdmDirectory(), "etc", "sysconfig"), i);
        readPermissions(Environment.buildPath(Environment.getOdmDirectory(), "etc", StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS), i);
        readPermissions(Environment.buildPath(Environment.getOemDirectory(), "etc", "sysconfig"), 33);
        readPermissions(Environment.buildPath(Environment.getOemDirectory(), "etc", StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS), 33);
        readPermissions(Environment.buildPath(Environment.getProductDirectory(), "etc", "sysconfig"), 31);
        readPermissions(Environment.buildPath(Environment.getProductDirectory(), "etc", StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS), 31);
    }

    void readPermissions(File file, int i) {
        if (!file.exists() || !file.isDirectory()) {
            if (i == -1) {
                Slog.w(TAG, "No directory " + file + ", skipping");
                return;
            }
            return;
        }
        if (!file.canRead()) {
            Slog.w(TAG, "Directory " + file + " cannot be read");
            return;
        }
        File file2 = null;
        for (File file3 : file.listFiles()) {
            if (file3.getPath().endsWith("etc/permissions/platform.xml")) {
                file2 = file3;
            } else if (!file3.getPath().endsWith(".xml")) {
                Slog.i(TAG, "Non-xml file " + file3 + " in " + file + " directory, ignoring");
            } else if (!file3.canRead()) {
                Slog.w(TAG, "Permissions library file " + file3 + " cannot be read");
            } else {
                readPermissionsFromXml(file3, i);
            }
        }
        if (file2 != null) {
            readPermissionsFromXml(file2, i);
        }
    }

    private void readPermissionsFromXml(File file, int i) {
        int i2;
        XmlPullParser xmlPullParserNewPullParser;
        int next;
        int i3;
        boolean z;
        int i4;
        boolean z2;
        try {
            FileReader fileReader = new FileReader(file);
            boolean zIsLowRamDeviceStatic = ActivityManager.isLowRamDeviceStatic();
            try {
                try {
                    xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileReader);
                    do {
                        next = xmlPullParserNewPullParser.next();
                        i3 = 1;
                        if (next == 2) {
                            break;
                        }
                    } while (next != 1);
                } catch (IOException e) {
                    Slog.w(TAG, "Got exception parsing permissions.", e);
                } catch (XmlPullParserException e2) {
                    Slog.w(TAG, "Got exception parsing permissions.", e2);
                }
                if (next != 2) {
                    throw new XmlPullParserException("No start tag found");
                }
                if (!xmlPullParserNewPullParser.getName().equals(StorageVolume.ScopedAccessProviderContract.TABLE_PERMISSIONS) && !xmlPullParserNewPullParser.getName().equals("config")) {
                    throw new XmlPullParserException("Unexpected start tag in " + file + ": found " + xmlPullParserNewPullParser.getName() + ", expected 'permissions' or 'config'");
                }
                boolean z3 = i == -1;
                boolean z4 = (i & 2) != 0;
                boolean z5 = (i & 1) != 0;
                boolean z6 = (i & 4) != 0;
                boolean z7 = (i & 8) != 0;
                boolean z8 = (i & 16) != 0;
                boolean z9 = (i & 32) != 0;
                boolean z10 = (i & 64) != 0;
                while (true) {
                    XmlUtils.nextElement(xmlPullParserNewPullParser);
                    if (xmlPullParserNewPullParser.getEventType() == i3) {
                        break;
                    }
                    String name = xmlPullParserNewPullParser.getName();
                    if (WifiConfiguration.GroupCipher.varName.equals(name) && z3) {
                        String attributeValue = xmlPullParserNewPullParser.getAttributeValue(null, "gid");
                        if (attributeValue != null) {
                            this.mGlobalGids = ArrayUtils.appendInt(this.mGlobalGids, Process.getGidForName(attributeValue));
                        } else {
                            Slog.w(TAG, "<group> without gid in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                        }
                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                    } else if (UsbManager.EXTRA_PERMISSION_GRANTED.equals(name) && z6) {
                        String attributeValue2 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                        if (attributeValue2 == null) {
                            Slog.w(TAG, "<permission> without name in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                            XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                        } else {
                            readPermission(xmlPullParserNewPullParser, attributeValue2.intern());
                            z = z6;
                            i4 = 1;
                            i3 = i4;
                            z6 = z;
                        }
                    } else {
                        if ("assign-permission".equals(name) && z6) {
                            String attributeValue3 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                            if (attributeValue3 == null) {
                                Slog.w(TAG, "<assign-permission> without name in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                            } else {
                                String attributeValue4 = xmlPullParserNewPullParser.getAttributeValue(null, GrantCredentialsPermissionActivity.EXTRAS_REQUESTING_UID);
                                if (attributeValue4 == null) {
                                    Slog.w(TAG, "<assign-permission> without uid in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                } else {
                                    int uidForName = Process.getUidForName(attributeValue4);
                                    if (uidForName < 0) {
                                        StringBuilder sb = new StringBuilder();
                                        z = z6;
                                        sb.append("<assign-permission> with unknown uid \"");
                                        sb.append(attributeValue4);
                                        sb.append("  in ");
                                        sb.append(file);
                                        sb.append(" at ");
                                        sb.append(xmlPullParserNewPullParser.getPositionDescription());
                                        Slog.w(TAG, sb.toString());
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else {
                                        z = z6;
                                        String strIntern = attributeValue3.intern();
                                        ArraySet<String> arraySet = this.mSystemPermissions.get(uidForName);
                                        if (arraySet == null) {
                                            arraySet = new ArraySet<>();
                                            this.mSystemPermissions.put(uidForName, arraySet);
                                        }
                                        arraySet.add(strIntern);
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                        i4 = 1;
                                        i3 = i4;
                                        z6 = z;
                                    }
                                }
                            }
                        } else {
                            z = z6;
                            if ("library".equals(name) && z4) {
                                String attributeValue5 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                                String attributeValue6 = xmlPullParserNewPullParser.getAttributeValue(null, ContentResolver.SCHEME_FILE);
                                if (attributeValue5 == null) {
                                    Slog.w(TAG, "<library> without name in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                } else if (attributeValue6 == null) {
                                    Slog.w(TAG, "<library> without file in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                } else {
                                    this.mSharedLibraries.put(attributeValue5, attributeValue6);
                                }
                                XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                            } else {
                                if ("feature".equals(name) && z5) {
                                    String attributeValue7 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                                    int intAttribute = XmlUtils.readIntAttribute(xmlPullParserNewPullParser, "version", 0);
                                    if (zIsLowRamDeviceStatic) {
                                        i4 = 1;
                                        z2 = !"true".equals(xmlPullParserNewPullParser.getAttributeValue(null, "notLowRam"));
                                    } else {
                                        z2 = true;
                                        i4 = 1;
                                    }
                                    if (attributeValue7 == null) {
                                        Slog.w(TAG, "<feature> without name in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                    } else if (z2) {
                                        addFeature(attributeValue7, intAttribute);
                                    }
                                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                } else {
                                    i4 = 1;
                                    if ("unavailable-feature".equals(name) && z5) {
                                        String attributeValue8 = xmlPullParserNewPullParser.getAttributeValue(null, "name");
                                        if (attributeValue8 == null) {
                                            Slog.w(TAG, "<unavailable-feature> without name in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mUnavailableFeatures.add(attributeValue8);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("allow-in-power-save-except-idle".equals(name) && z3) {
                                        String attributeValue9 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue9 == null) {
                                            Slog.w(TAG, "<allow-in-power-save-except-idle> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mAllowInPowerSaveExceptIdle.add(attributeValue9);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("allow-in-power-save".equals(name) && z3) {
                                        String attributeValue10 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue10 == null) {
                                            Slog.w(TAG, "<allow-in-power-save> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mAllowInPowerSave.add(attributeValue10);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("allow-in-data-usage-save".equals(name) && z3) {
                                        String attributeValue11 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue11 == null) {
                                            Slog.w(TAG, "<allow-in-data-usage-save> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mAllowInDataUsageSave.add(attributeValue11);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("allow-unthrottled-location".equals(name) && z3) {
                                        String attributeValue12 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue12 == null) {
                                            Slog.w(TAG, "<allow-unthrottled-location> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mAllowUnthrottledLocation.add(attributeValue12);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("allow-implicit-broadcast".equals(name) && z3) {
                                        String attributeValue13 = xmlPullParserNewPullParser.getAttributeValue(null, "action");
                                        if (attributeValue13 == null) {
                                            Slog.w(TAG, "<allow-implicit-broadcast> without action in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mAllowImplicitBroadcasts.add(attributeValue13);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("app-link".equals(name) && z7) {
                                        String attributeValue14 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue14 == null) {
                                            Slog.w(TAG, "<app-link> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mLinkedApps.add(attributeValue14);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("system-user-whitelisted-app".equals(name) && z7) {
                                        String attributeValue15 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue15 == null) {
                                            Slog.w(TAG, "<system-user-whitelisted-app> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mSystemUserWhitelistedApps.add(attributeValue15);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("system-user-blacklisted-app".equals(name) && z7) {
                                        String attributeValue16 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue16 == null) {
                                            Slog.w(TAG, "<system-user-blacklisted-app without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mSystemUserBlacklistedApps.add(attributeValue16);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("default-enabled-vr-app".equals(name) && z7) {
                                        String attributeValue17 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        String attributeValue18 = xmlPullParserNewPullParser.getAttributeValue(null, "class");
                                        if (attributeValue17 == null) {
                                            Slog.w(TAG, "<default-enabled-vr-app without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else if (attributeValue18 == null) {
                                            Slog.w(TAG, "<default-enabled-vr-app without class in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mDefaultVrComponents.add(new ComponentName(attributeValue17, attributeValue18));
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("backup-transport-whitelisted-service".equals(name) && z5) {
                                        String attributeValue19 = xmlPullParserNewPullParser.getAttributeValue(null, "service");
                                        if (attributeValue19 == null) {
                                            Slog.w(TAG, "<backup-transport-whitelisted-service> without service in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString(attributeValue19);
                                            if (componentNameUnflattenFromString == null) {
                                                Slog.w(TAG, "<backup-transport-whitelisted-service> with invalid service name " + attributeValue19 + " in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                            } else {
                                                this.mBackupTransportWhitelist.add(componentNameUnflattenFromString);
                                            }
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("disabled-until-used-preinstalled-carrier-associated-app".equals(name) && z7) {
                                        String attributeValue20 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        String attributeValue21 = xmlPullParserNewPullParser.getAttributeValue(null, "carrierAppPackage");
                                        if (attributeValue20 == null || attributeValue21 == null) {
                                            Slog.w(TAG, "<disabled-until-used-preinstalled-carrier-associated-app without package or carrierAppPackage in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            List<String> arrayList = this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps.get(attributeValue21);
                                            if (arrayList == null) {
                                                arrayList = new ArrayList<>();
                                                this.mDisabledUntilUsedPreinstalledCarrierAssociatedApps.put(attributeValue21, arrayList);
                                            }
                                            arrayList.add(attributeValue20);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("disabled-until-used-preinstalled-carrier-app".equals(name) && z7) {
                                        String attributeValue22 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue22 == null) {
                                            Slog.w(TAG, "<disabled-until-used-preinstalled-carrier-app> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mDisabledUntilUsedPreinstalledCarrierApps.add(attributeValue22);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else if ("privapp-permissions".equals(name) && z8) {
                                        boolean z11 = file.toPath().startsWith(Environment.getVendorDirectory().toPath()) || file.toPath().startsWith(Environment.getOdmDirectory().toPath());
                                        boolean zStartsWith = file.toPath().startsWith(Environment.getProductDirectory().toPath());
                                        if (z11) {
                                            readPrivAppPermissions(xmlPullParserNewPullParser, this.mVendorPrivAppPermissions, this.mVendorPrivAppDenyPermissions);
                                        } else if (zStartsWith) {
                                            readPrivAppPermissions(xmlPullParserNewPullParser, this.mProductPrivAppPermissions, this.mProductPrivAppDenyPermissions);
                                        } else {
                                            readPrivAppPermissions(xmlPullParserNewPullParser, this.mPrivAppPermissions, this.mPrivAppDenyPermissions);
                                        }
                                    } else if ("oem-permissions".equals(name) && z9) {
                                        readOemPermissions(xmlPullParserNewPullParser);
                                    } else if ("hidden-api-whitelisted-app".equals(name) && z10) {
                                        String attributeValue23 = xmlPullParserNewPullParser.getAttributeValue(null, "package");
                                        if (attributeValue23 == null) {
                                            Slog.w(TAG, "<hidden-api-whitelisted-app> without package in " + file + " at " + xmlPullParserNewPullParser.getPositionDescription());
                                        } else {
                                            this.mHiddenApiPackageWhitelist.add(attributeValue23);
                                        }
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    } else {
                                        Slog.w(TAG, "Tag " + name + " is unknown or not allowed in " + file.getParent());
                                        XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    }
                                }
                                i3 = i4;
                                z6 = z;
                            }
                        }
                        z6 = z;
                    }
                    i3 = 1;
                }
                IoUtils.closeQuietly(fileReader);
                if (StorageManager.isFileEncryptedNativeOnly()) {
                    i2 = 0;
                    addFeature(PackageManager.FEATURE_FILE_BASED_ENCRYPTION, 0);
                    addFeature(PackageManager.FEATURE_SECURELY_REMOVES_USERS, 0);
                } else {
                    i2 = 0;
                }
                if (StorageManager.hasAdoptable()) {
                    addFeature(PackageManager.FEATURE_ADOPTABLE_STORAGE, i2);
                }
                if (ActivityManager.isLowRamDeviceStatic()) {
                    addFeature(PackageManager.FEATURE_RAM_LOW, i2);
                } else {
                    addFeature(PackageManager.FEATURE_RAM_NORMAL, i2);
                }
                Iterator<String> it = this.mUnavailableFeatures.iterator();
                while (it.hasNext()) {
                    removeFeature(it.next());
                }
            } catch (Throwable th) {
                IoUtils.closeQuietly(fileReader);
                throw th;
            }
        } catch (FileNotFoundException e3) {
            Slog.w(TAG, "Couldn't find or open permissions file " + file);
        }
    }

    private void addFeature(String str, int i) {
        FeatureInfo featureInfo = this.mAvailableFeatures.get(str);
        if (featureInfo == null) {
            FeatureInfo featureInfo2 = new FeatureInfo();
            featureInfo2.name = str;
            featureInfo2.version = i;
            this.mAvailableFeatures.put(str, featureInfo2);
            return;
        }
        featureInfo.version = Math.max(featureInfo.version, i);
    }

    private void removeFeature(String str) {
        if (this.mAvailableFeatures.remove(str) != null) {
            Slog.d(TAG, "Removed unavailable feature " + str);
        }
    }

    void readPermission(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        if (this.mPermissions.containsKey(str)) {
            throw new IllegalStateException("Duplicate permission definition for " + str);
        }
        PermissionEntry permissionEntry = new PermissionEntry(str, XmlUtils.readBooleanAttribute(xmlPullParser, "perUser", false));
        this.mPermissions.put(str, permissionEntry);
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next != 1) {
                if (next != 3 || xmlPullParser.getDepth() > depth) {
                    if (next != 3 && next != 4) {
                        if (WifiConfiguration.GroupCipher.varName.equals(xmlPullParser.getName())) {
                            String attributeValue = xmlPullParser.getAttributeValue(null, "gid");
                            if (attributeValue != null) {
                                permissionEntry.gids = ArrayUtils.appendInt(permissionEntry.gids, Process.getGidForName(attributeValue));
                            } else {
                                Slog.w(TAG, "<group> without gid at " + xmlPullParser.getPositionDescription());
                            }
                        }
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    private void readPrivAppPermissions(XmlPullParser xmlPullParser, ArrayMap<String, ArraySet<String>> arrayMap, ArrayMap<String, ArraySet<String>> arrayMap2) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(attributeValue)) {
            Slog.w(TAG, "package is required for <privapp-permissions> in " + xmlPullParser.getPositionDescription());
            return;
        }
        ArraySet<String> arraySet = arrayMap.get(attributeValue);
        if (arraySet == null) {
            arraySet = new ArraySet<>();
        }
        ArraySet<String> arraySet2 = arrayMap2.get(attributeValue);
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            String name = xmlPullParser.getName();
            if (UsbManager.EXTRA_PERMISSION_GRANTED.equals(name)) {
                String attributeValue2 = xmlPullParser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(attributeValue2)) {
                    Slog.w(TAG, "name is required for <permission> in " + xmlPullParser.getPositionDescription());
                } else {
                    arraySet.add(attributeValue2);
                }
            } else if ("deny-permission".equals(name)) {
                String attributeValue3 = xmlPullParser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(attributeValue3)) {
                    Slog.w(TAG, "name is required for <deny-permission> in " + xmlPullParser.getPositionDescription());
                } else {
                    if (arraySet2 == null) {
                        arraySet2 = new ArraySet<>();
                    }
                    arraySet2.add(attributeValue3);
                }
            }
        }
        arrayMap.put(attributeValue, arraySet);
        if (arraySet2 != null) {
            arrayMap2.put(attributeValue, arraySet2);
        }
    }

    void readOemPermissions(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, "package");
        if (TextUtils.isEmpty(attributeValue)) {
            Slog.w(TAG, "package is required for <oem-permissions> in " + xmlPullParser.getPositionDescription());
            return;
        }
        ArrayMap<String, Boolean> arrayMap = this.mOemPermissions.get(attributeValue);
        if (arrayMap == null) {
            arrayMap = new ArrayMap<>();
        }
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            String name = xmlPullParser.getName();
            if (UsbManager.EXTRA_PERMISSION_GRANTED.equals(name)) {
                String attributeValue2 = xmlPullParser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(attributeValue2)) {
                    Slog.w(TAG, "name is required for <permission> in " + xmlPullParser.getPositionDescription());
                } else {
                    arrayMap.put(attributeValue2, Boolean.TRUE);
                }
            } else if ("deny-permission".equals(name)) {
                String attributeValue3 = xmlPullParser.getAttributeValue(null, "name");
                if (TextUtils.isEmpty(attributeValue3)) {
                    Slog.w(TAG, "name is required for <deny-permission> in " + xmlPullParser.getPositionDescription());
                } else {
                    arrayMap.put(attributeValue3, Boolean.FALSE);
                }
            }
        }
        this.mOemPermissions.put(attributeValue, arrayMap);
    }
}
