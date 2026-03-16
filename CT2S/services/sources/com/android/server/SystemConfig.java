package com.android.server;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.pm.FeatureInfo;
import android.os.Environment;
import android.os.Process;
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
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SystemConfig {
    static final String TAG = "SystemConfig";
    static SystemConfig sInstance;
    int[] mGlobalGids;
    final SparseArray<ArraySet<String>> mSystemPermissions = new SparseArray<>();
    final ArrayMap<String, String> mSharedLibraries = new ArrayMap<>();
    final ArrayMap<String, FeatureInfo> mAvailableFeatures = new ArrayMap<>();
    final ArraySet<String> mUnavailableFeatures = new ArraySet<>();
    final ArrayMap<String, PermissionEntry> mPermissions = new ArrayMap<>();
    final ArraySet<String> mAllowInPowerSave = new ArraySet<>();
    final ArraySet<String> mFixedImeApps = new ArraySet<>();
    final ArraySet<ComponentName> mBackupTransportWhitelist = new ArraySet<>();

    public static final class PermissionEntry {
        public int[] gids;
        public final String name;

        PermissionEntry(String _name) {
            this.name = _name;
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

    public ArraySet<String> getAllowInPowerSave() {
        return this.mAllowInPowerSave;
    }

    public ArraySet<String> getFixedImeApps() {
        return this.mFixedImeApps;
    }

    public ArraySet<ComponentName> getBackupTransportWhitelist() {
        return this.mBackupTransportWhitelist;
    }

    SystemConfig() {
        readPermissions(Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc", "sysconfig"}), false);
        readPermissions(Environment.buildPath(Environment.getRootDirectory(), new String[]{"etc", "permissions"}), false);
        readPermissions(Environment.buildPath(Environment.getOemDirectory(), new String[]{"etc", "sysconfig"}), true);
        readPermissions(Environment.buildPath(Environment.getOemDirectory(), new String[]{"etc", "permissions"}), true);
    }

    void readPermissions(File libraryDir, boolean onlyFeatures) {
        if (!libraryDir.exists() || !libraryDir.isDirectory()) {
            if (!onlyFeatures) {
                Slog.w(TAG, "No directory " + libraryDir + ", skipping");
                return;
            }
            return;
        }
        if (!libraryDir.canRead()) {
            Slog.w(TAG, "Directory " + libraryDir + " cannot be read");
            return;
        }
        File platformFile = null;
        File[] arr$ = libraryDir.listFiles();
        for (File f : arr$) {
            if (f.getPath().endsWith("etc/permissions/platform.xml")) {
                platformFile = f;
            } else if (!f.getPath().endsWith(".xml")) {
                Slog.i(TAG, "Non-xml file " + f + " in " + libraryDir + " directory, ignoring");
            } else if (!f.canRead()) {
                Slog.w(TAG, "Permissions library file " + f + " cannot be read");
            } else {
                readPermissionsFromXml(f, onlyFeatures);
            }
        }
        if (platformFile != null) {
            readPermissionsFromXml(platformFile, onlyFeatures);
        }
    }

    private void readPermissionsFromXml(File permFile, boolean onlyFeatures) {
        XmlPullParser parser;
        int type;
        boolean allowed;
        try {
            FileReader permReader = new FileReader(permFile);
            boolean lowRam = ActivityManager.isLowRamDeviceStatic();
            try {
                parser = Xml.newPullParser();
                parser.setInput(permReader);
                do {
                    type = parser.next();
                    if (type == 2) {
                        break;
                    }
                } while (type != 1);
            } catch (IOException e) {
                Slog.w(TAG, "Got exception parsing permissions.", e);
            } catch (XmlPullParserException e2) {
                Slog.w(TAG, "Got exception parsing permissions.", e2);
            } finally {
                IoUtils.closeQuietly(permReader);
            }
            if (type != 2) {
                throw new XmlPullParserException("No start tag found");
            }
            if (!parser.getName().equals("permissions") && !parser.getName().equals("config")) {
                throw new XmlPullParserException("Unexpected start tag in " + permFile + ": found " + parser.getName() + ", expected 'permissions' or 'config'");
            }
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == 1) {
                    break;
                }
                String name = parser.getName();
                if ("group".equals(name) && !onlyFeatures) {
                    String gidStr = parser.getAttributeValue(null, "gid");
                    if (gidStr != null) {
                        int gid = Process.getGidForName(gidStr);
                        this.mGlobalGids = ArrayUtils.appendInt(this.mGlobalGids, gid);
                    } else {
                        Slog.w(TAG, "<group> without gid in " + permFile + " at " + parser.getPositionDescription());
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if ("permission".equals(name) && !onlyFeatures) {
                    String perm = parser.getAttributeValue(null, "name");
                    if (perm == null) {
                        Slog.w(TAG, "<permission> without name in " + permFile + " at " + parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        readPermission(parser, perm.intern());
                    }
                } else if ("assign-permission".equals(name) && !onlyFeatures) {
                    String perm2 = parser.getAttributeValue(null, "name");
                    if (perm2 == null) {
                        Slog.w(TAG, "<assign-permission> without name in " + permFile + " at " + parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        String uidStr = parser.getAttributeValue(null, "uid");
                        if (uidStr == null) {
                            Slog.w(TAG, "<assign-permission> without uid in " + permFile + " at " + parser.getPositionDescription());
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            int uid = Process.getUidForName(uidStr);
                            if (uid < 0) {
                                Slog.w(TAG, "<assign-permission> with unknown uid \"" + uidStr + "  in " + permFile + " at " + parser.getPositionDescription());
                                XmlUtils.skipCurrentTag(parser);
                            } else {
                                String perm3 = perm2.intern();
                                ArraySet<String> perms = this.mSystemPermissions.get(uid);
                                if (perms == null) {
                                    perms = new ArraySet<>();
                                    this.mSystemPermissions.put(uid, perms);
                                }
                                perms.add(perm3);
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                    }
                } else if ("library".equals(name) && !onlyFeatures) {
                    String lname = parser.getAttributeValue(null, "name");
                    String lfile = parser.getAttributeValue(null, "file");
                    if (lname == null) {
                        Slog.w(TAG, "<library> without name in " + permFile + " at " + parser.getPositionDescription());
                    } else if (lfile == null) {
                        Slog.w(TAG, "<library> without file in " + permFile + " at " + parser.getPositionDescription());
                    } else {
                        this.mSharedLibraries.put(lname, lfile);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if ("feature".equals(name)) {
                    String fname = parser.getAttributeValue(null, "name");
                    if (lowRam) {
                        String notLowRam = parser.getAttributeValue(null, "notLowRam");
                        allowed = !"true".equals(notLowRam);
                    } else {
                        allowed = true;
                    }
                    if (fname == null) {
                        Slog.w(TAG, "<feature> without name in " + permFile + " at " + parser.getPositionDescription());
                    } else if (allowed) {
                        FeatureInfo fi = new FeatureInfo();
                        fi.name = fname;
                        this.mAvailableFeatures.put(fname, fi);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if ("unavailable-feature".equals(name)) {
                    String fname2 = parser.getAttributeValue(null, "name");
                    if (fname2 == null) {
                        Slog.w(TAG, "<unavailable-feature> without name in " + permFile + " at " + parser.getPositionDescription());
                    } else {
                        this.mUnavailableFeatures.add(fname2);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if ("allow-in-power-save".equals(name) && !onlyFeatures) {
                    String pkgname = parser.getAttributeValue(null, "package");
                    if (pkgname == null) {
                        Slog.w(TAG, "<allow-in-power-save> without package in " + permFile + " at " + parser.getPositionDescription());
                    } else {
                        this.mAllowInPowerSave.add(pkgname);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if ("fixed-ime-app".equals(name) && !onlyFeatures) {
                    String pkgname2 = parser.getAttributeValue(null, "package");
                    if (pkgname2 == null) {
                        Slog.w(TAG, "<fixed-ime-app> without package in " + permFile + " at " + parser.getPositionDescription());
                    } else {
                        this.mFixedImeApps.add(pkgname2);
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if ("backup-transport-whitelisted-service".equals(name)) {
                    String serviceName = parser.getAttributeValue(null, "service");
                    if (serviceName == null) {
                        Slog.w(TAG, "<backup-transport-whitelisted-service> without service in " + permFile + " at " + parser.getPositionDescription());
                    } else {
                        ComponentName cn = ComponentName.unflattenFromString(serviceName);
                        if (cn == null) {
                            Slog.w(TAG, "<backup-transport-whitelisted-service> with invalid service name " + serviceName + " in " + permFile + " at " + parser.getPositionDescription());
                        } else {
                            this.mBackupTransportWhitelist.add(cn);
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    XmlUtils.skipCurrentTag(parser);
                }
            }
            for (String fname3 : this.mUnavailableFeatures) {
                if (this.mAvailableFeatures.remove(fname3) != null) {
                    Slog.d(TAG, "Removed unavailable feature " + fname3);
                }
            }
        } catch (FileNotFoundException e3) {
            Slog.w(TAG, "Couldn't find or open permissions file " + permFile);
        }
    }

    void readPermission(XmlPullParser parser, String name) throws XmlPullParserException, IOException {
        String name2 = name.intern();
        PermissionEntry perm = this.mPermissions.get(name2);
        if (perm == null) {
            perm = new PermissionEntry(name2);
            this.mPermissions.put(name2, perm);
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1) {
                return;
            }
            if (type != 3 || parser.getDepth() > outerDepth) {
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if ("group".equals(tagName)) {
                        String gidStr = parser.getAttributeValue(null, "gid");
                        if (gidStr != null) {
                            int gid = Process.getGidForName(gidStr);
                            perm.gids = ArrayUtils.appendInt(perm.gids, gid);
                        } else {
                            Slog.w(TAG, "<group> without gid at " + parser.getPositionDescription());
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            } else {
                return;
            }
        }
    }
}
