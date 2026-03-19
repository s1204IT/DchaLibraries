package com.android.server.pm;

import android.content.pm.PackageInfo;
import android.content.pm.ShortcutInfo;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.pm.ShortcutService;
import com.android.server.pm.ShortcutUser;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutLauncher extends ShortcutPackageItem {
    private static final String ATTR_LAUNCHER_USER_ID = "launcher-user";
    private static final String ATTR_PACKAGE_NAME = "package-name";
    private static final String ATTR_PACKAGE_USER_ID = "package-user";
    private static final String ATTR_VALUE = "value";
    private static final String TAG = "ShortcutService";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PIN = "pin";
    static final String TAG_ROOT = "launcher-pins";
    private final int mOwnerUserId;
    private final ArrayMap<ShortcutUser.PackageWithUser, ArraySet<String>> mPinnedShortcuts;

    private ShortcutLauncher(ShortcutUser shortcutUser, int i, String str, int i2, ShortcutPackageInfo shortcutPackageInfo) {
        super(shortcutUser, i2, str, shortcutPackageInfo == null ? ShortcutPackageInfo.newEmpty() : shortcutPackageInfo);
        this.mPinnedShortcuts = new ArrayMap<>();
        this.mOwnerUserId = i;
    }

    public ShortcutLauncher(ShortcutUser shortcutUser, int i, String str, int i2) {
        this(shortcutUser, i, str, i2, null);
    }

    @Override
    public int getOwnerUserId() {
        return this.mOwnerUserId;
    }

    @Override
    protected boolean canRestoreAnyVersion() {
        return true;
    }

    private void onRestoreBlocked() {
        ArrayList arrayList = new ArrayList(this.mPinnedShortcuts.keySet());
        this.mPinnedShortcuts.clear();
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            ShortcutPackage packageShortcutsIfExists = this.mShortcutUser.getPackageShortcutsIfExists(((ShortcutUser.PackageWithUser) arrayList.get(size)).packageName);
            if (packageShortcutsIfExists != null) {
                packageShortcutsIfExists.refreshPinnedFlags();
            }
        }
    }

    @Override
    protected void onRestored(int i) {
        if (i != 0) {
            onRestoreBlocked();
        }
    }

    public void pinShortcuts(int i, String str, List<String> list, boolean z) {
        ShortcutPackage packageShortcutsIfExists = this.mShortcutUser.getPackageShortcutsIfExists(str);
        if (packageShortcutsIfExists == null) {
            return;
        }
        ShortcutUser.PackageWithUser packageWithUserOf = ShortcutUser.PackageWithUser.of(i, str);
        int size = list.size();
        if (size == 0) {
            this.mPinnedShortcuts.remove(packageWithUserOf);
        } else {
            ArraySet<String> arraySet = this.mPinnedShortcuts.get(packageWithUserOf);
            ArraySet<String> arraySet2 = new ArraySet<>();
            for (int i2 = 0; i2 < size; i2++) {
                String str2 = list.get(i2);
                ShortcutInfo shortcutInfoFindShortcutById = packageShortcutsIfExists.findShortcutById(str2);
                if (shortcutInfoFindShortcutById != null && (shortcutInfoFindShortcutById.isDynamic() || shortcutInfoFindShortcutById.isManifestShortcut() || ((arraySet != null && arraySet.contains(str2)) || z))) {
                    arraySet2.add(str2);
                }
            }
            this.mPinnedShortcuts.put(packageWithUserOf, arraySet2);
        }
        packageShortcutsIfExists.refreshPinnedFlags();
    }

    public ArraySet<String> getPinnedShortcutIds(String str, int i) {
        return this.mPinnedShortcuts.get(ShortcutUser.PackageWithUser.of(i, str));
    }

    public boolean hasPinned(ShortcutInfo shortcutInfo) {
        ArraySet<String> pinnedShortcutIds = getPinnedShortcutIds(shortcutInfo.getPackage(), shortcutInfo.getUserId());
        return pinnedShortcutIds != null && pinnedShortcutIds.contains(shortcutInfo.getId());
    }

    public void addPinnedShortcut(String str, int i, String str2, boolean z) {
        ArrayList arrayList;
        ArraySet<String> pinnedShortcutIds = getPinnedShortcutIds(str, i);
        if (pinnedShortcutIds != null) {
            arrayList = new ArrayList(pinnedShortcutIds.size() + 1);
            arrayList.addAll(pinnedShortcutIds);
        } else {
            arrayList = new ArrayList(1);
        }
        arrayList.add(str2);
        pinShortcuts(i, str, arrayList, z);
    }

    boolean cleanUpPackage(String str, int i) {
        return this.mPinnedShortcuts.remove(ShortcutUser.PackageWithUser.of(i, str)) != null;
    }

    public void ensurePackageInfo() {
        PackageInfo packageInfoWithSignatures = this.mShortcutUser.mService.getPackageInfoWithSignatures(getPackageName(), getPackageUserId());
        if (packageInfoWithSignatures == null) {
            Slog.w(TAG, "Package not found: " + getPackageName());
            return;
        }
        getPackageInfo().updateFromPackageInfo(packageInfoWithSignatures);
    }

    @Override
    public void saveToXml(XmlSerializer xmlSerializer, boolean z) throws IOException {
        int size;
        if ((z && !getPackageInfo().isBackupAllowed()) || (size = this.mPinnedShortcuts.size()) == 0) {
            return;
        }
        xmlSerializer.startTag(null, TAG_ROOT);
        ShortcutService.writeAttr(xmlSerializer, ATTR_PACKAGE_NAME, getPackageName());
        ShortcutService.writeAttr(xmlSerializer, ATTR_LAUNCHER_USER_ID, getPackageUserId());
        getPackageInfo().saveToXml(this.mShortcutUser.mService, xmlSerializer, z);
        for (int i = 0; i < size; i++) {
            ShortcutUser.PackageWithUser packageWithUserKeyAt = this.mPinnedShortcuts.keyAt(i);
            if (!z || packageWithUserKeyAt.userId == getOwnerUserId()) {
                xmlSerializer.startTag(null, "package");
                ShortcutService.writeAttr(xmlSerializer, ATTR_PACKAGE_NAME, packageWithUserKeyAt.packageName);
                ShortcutService.writeAttr(xmlSerializer, ATTR_PACKAGE_USER_ID, packageWithUserKeyAt.userId);
                ArraySet<String> arraySetValueAt = this.mPinnedShortcuts.valueAt(i);
                int size2 = arraySetValueAt.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    ShortcutService.writeTagValue(xmlSerializer, TAG_PIN, arraySetValueAt.valueAt(i2));
                }
                xmlSerializer.endTag(null, "package");
            }
        }
        xmlSerializer.endTag(null, TAG_ROOT);
    }

    public static ShortcutLauncher loadFromXml(XmlPullParser xmlPullParser, ShortcutUser shortcutUser, int i, boolean z) throws XmlPullParserException, IOException {
        int intAttribute;
        int intAttribute2;
        String stringAttribute = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_PACKAGE_NAME);
        if (!z) {
            intAttribute = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_LAUNCHER_USER_ID, i);
        } else {
            intAttribute = i;
        }
        ShortcutLauncher shortcutLauncher = new ShortcutLauncher(shortcutUser, i, stringAttribute, intAttribute);
        ArraySet<String> arraySet = null;
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            byte b = 1;
            if (next != 1 && (next != 3 || xmlPullParser.getDepth() > depth)) {
                if (next == 2) {
                    int depth2 = xmlPullParser.getDepth();
                    String name = xmlPullParser.getName();
                    if (depth2 == depth + 1) {
                        int iHashCode = name.hashCode();
                        if (iHashCode != -1923478059) {
                            if (iHashCode != -807062458 || !name.equals("package")) {
                                b = -1;
                            }
                            switch (b) {
                                case 0:
                                    shortcutLauncher.getPackageInfo().loadFromXml(xmlPullParser, z);
                                    continue;
                                    continue;
                                case 1:
                                    String stringAttribute2 = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_PACKAGE_NAME);
                                    if (!z) {
                                        intAttribute2 = ShortcutService.parseIntAttribute(xmlPullParser, ATTR_PACKAGE_USER_ID, i);
                                    } else {
                                        intAttribute2 = i;
                                    }
                                    ArraySet<String> arraySet2 = new ArraySet<>();
                                    shortcutLauncher.mPinnedShortcuts.put(ShortcutUser.PackageWithUser.of(intAttribute2, stringAttribute2), arraySet2);
                                    arraySet = arraySet2;
                                    continue;
                                    continue;
                            }
                        } else {
                            if (name.equals("package-info")) {
                                b = 0;
                            }
                            switch (b) {
                            }
                        }
                    }
                    if (depth2 == depth + 2) {
                        if (((name.hashCode() == 110997 && name.equals(TAG_PIN)) ? (byte) 0 : (byte) -1) == 0) {
                            if (arraySet == null) {
                                Slog.w(TAG, "pin in invalid place");
                            } else {
                                arraySet.add(ShortcutService.parseStringAttribute(xmlPullParser, ATTR_VALUE));
                            }
                        }
                    }
                    ShortcutService.warnForInvalidTag(depth2, name);
                }
            }
        }
        return shortcutLauncher;
    }

    public void dump(PrintWriter printWriter, String str, ShortcutService.DumpFilter dumpFilter) {
        printWriter.println();
        printWriter.print(str);
        printWriter.print("Launcher: ");
        printWriter.print(getPackageName());
        printWriter.print("  Package user: ");
        printWriter.print(getPackageUserId());
        printWriter.print("  Owner user: ");
        printWriter.print(getOwnerUserId());
        printWriter.println();
        getPackageInfo().dump(printWriter, str + "  ");
        printWriter.println();
        int size = this.mPinnedShortcuts.size();
        for (int i = 0; i < size; i++) {
            printWriter.println();
            ShortcutUser.PackageWithUser packageWithUserKeyAt = this.mPinnedShortcuts.keyAt(i);
            printWriter.print(str);
            printWriter.print("  ");
            printWriter.print("Package: ");
            printWriter.print(packageWithUserKeyAt.packageName);
            printWriter.print("  User: ");
            printWriter.println(packageWithUserKeyAt.userId);
            ArraySet<String> arraySetValueAt = this.mPinnedShortcuts.valueAt(i);
            int size2 = arraySetValueAt.size();
            for (int i2 = 0; i2 < size2; i2++) {
                printWriter.print(str);
                printWriter.print("    Pinned: ");
                printWriter.print(arraySetValueAt.valueAt(i2));
                printWriter.println();
            }
        }
    }

    @Override
    public JSONObject dumpCheckin(boolean z) throws JSONException {
        return super.dumpCheckin(z);
    }

    @VisibleForTesting
    ArraySet<String> getAllPinnedShortcutsForTest(String str, int i) {
        return new ArraySet<>((ArraySet) this.mPinnedShortcuts.get(ShortcutUser.PackageWithUser.of(i, str)));
    }
}
