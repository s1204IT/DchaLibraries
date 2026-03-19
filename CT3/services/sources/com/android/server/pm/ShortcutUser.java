package com.android.server.pm;

import android.content.ComponentName;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.SparseArray;
import com.android.internal.util.Preconditions;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;
import libcore.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutUser {
    private static final String ATTR_KNOWN_LOCALE_CHANGE_SEQUENCE_NUMBER = "locale-seq-no";
    private static final String ATTR_VALUE = "value";
    private static final String TAG = "ShortcutService";
    private static final String TAG_LAUNCHER = "launcher";
    static final String TAG_ROOT = "user";
    private long mKnownLocaleChangeSequenceNumber;
    private ComponentName mLauncherComponent;
    private final int mUserId;
    private final ArrayMap<String, ShortcutPackage> mPackages = new ArrayMap<>();
    private final SparseArray<ShortcutPackage> mPackagesFromUid = new SparseArray<>();
    private final ArrayMap<PackageWithUser, ShortcutLauncher> mLaunchers = new ArrayMap<>();

    static final class PackageWithUser {
        final String packageName;
        final int userId;

        private PackageWithUser(int userId, String packageName) {
            this.userId = userId;
            this.packageName = (String) Preconditions.checkNotNull(packageName);
        }

        public static PackageWithUser of(int userId, String packageName) {
            return new PackageWithUser(userId, packageName);
        }

        public static PackageWithUser of(ShortcutPackageItem spi) {
            return new PackageWithUser(spi.getPackageUserId(), spi.getPackageName());
        }

        public int hashCode() {
            return this.packageName.hashCode() ^ this.userId;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PackageWithUser)) {
                return false;
            }
            PackageWithUser that = (PackageWithUser) obj;
            if (this.userId == that.userId) {
                return this.packageName.equals(that.packageName);
            }
            return false;
        }

        public String toString() {
            return String.format("{Package: %d, %s}", Integer.valueOf(this.userId), this.packageName);
        }
    }

    public ShortcutUser(int userId) {
        this.mUserId = userId;
    }

    public int getUserId() {
        return this.mUserId;
    }

    ArrayMap<String, ShortcutPackage> getAllPackagesForTest() {
        return this.mPackages;
    }

    public ShortcutPackage removePackage(ShortcutService s, String packageName) {
        ShortcutPackage removed = this.mPackages.remove(packageName);
        s.cleanupBitmapsForPackage(this.mUserId, packageName);
        return removed;
    }

    ArrayMap<PackageWithUser, ShortcutLauncher> getAllLaunchersForTest() {
        return this.mLaunchers;
    }

    public void addLauncher(ShortcutLauncher launcher) {
        this.mLaunchers.put(PackageWithUser.of(launcher.getPackageUserId(), launcher.getPackageName()), launcher);
    }

    public ShortcutLauncher removeLauncher(int packageUserId, String packageName) {
        return this.mLaunchers.remove(PackageWithUser.of(packageUserId, packageName));
    }

    public ShortcutPackage getPackageShortcuts(ShortcutService s, String packageName) {
        ShortcutPackage ret = this.mPackages.get(packageName);
        if (ret == null) {
            ShortcutPackage ret2 = new ShortcutPackage(s, this, this.mUserId, packageName);
            this.mPackages.put(packageName, ret2);
            return ret2;
        }
        ret.attemptToRestoreIfNeededAndSave(s);
        return ret;
    }

    public ShortcutLauncher getLauncherShortcuts(ShortcutService s, String packageName, int launcherUserId) {
        PackageWithUser key = PackageWithUser.of(launcherUserId, packageName);
        ShortcutLauncher ret = this.mLaunchers.get(key);
        if (ret == null) {
            ShortcutLauncher ret2 = new ShortcutLauncher(this, this.mUserId, packageName, launcherUserId);
            this.mLaunchers.put(key, ret2);
            return ret2;
        }
        ret.attemptToRestoreIfNeededAndSave(s);
        return ret;
    }

    public void forAllPackages(Consumer<? super ShortcutPackage> callback) {
        int size = this.mPackages.size();
        for (int i = 0; i < size; i++) {
            callback.accept(this.mPackages.valueAt(i));
        }
    }

    public void forAllLaunchers(Consumer<? super ShortcutLauncher> callback) {
        int size = this.mLaunchers.size();
        for (int i = 0; i < size; i++) {
            callback.accept(this.mLaunchers.valueAt(i));
        }
    }

    public void forAllPackageItems(Consumer<? super ShortcutPackageItem> callback) {
        forAllLaunchers(callback);
        forAllPackages(callback);
    }

    public void forPackageItem(final String packageName, final int packageUserId, final Consumer<ShortcutPackageItem> callback) {
        forAllPackageItems(new Consumer() {
            @Override
            public void accept(Object arg0) {
                ShortcutUser.m2593com_android_server_pm_ShortcutUser_lambda$1(packageUserId, packageName, callback, (ShortcutPackageItem) arg0);
            }
        });
    }

    static void m2593com_android_server_pm_ShortcutUser_lambda$1(int packageUserId, String packageName, Consumer callback, ShortcutPackageItem spi) {
        if (spi.getPackageUserId() == packageUserId && spi.getPackageName().equals(packageName)) {
            callback.accept(spi);
        }
    }

    public void resetThrottlingIfNeeded(final ShortcutService s) {
        long currentNo = s.getLocaleChangeSequenceNumber();
        if (this.mKnownLocaleChangeSequenceNumber >= currentNo) {
            return;
        }
        this.mKnownLocaleChangeSequenceNumber = currentNo;
        forAllPackages(new Consumer() {
            @Override
            public void accept(Object arg0) {
                ((ShortcutPackage) arg0).resetRateLimiting(s);
            }
        });
        s.scheduleSaveUser(this.mUserId);
    }

    public void handlePackageUpdated(ShortcutService s, String packageName, int newVersionCode) {
        if (!this.mPackages.containsKey(packageName)) {
            return;
        }
        getPackageShortcuts(s, packageName).handlePackageUpdated(s, newVersionCode);
    }

    public void attemptToRestoreIfNeededAndSave(final ShortcutService s, String packageName, int packageUserId) {
        forPackageItem(packageName, packageUserId, new Consumer() {
            @Override
            public void accept(Object arg0) {
                ((ShortcutPackageItem) arg0).attemptToRestoreIfNeededAndSave(s);
            }
        });
    }

    public void saveToXml(ShortcutService s, XmlSerializer out, boolean forBackup) throws XmlPullParserException, IOException {
        out.startTag(null, TAG_ROOT);
        ShortcutService.writeAttr(out, ATTR_KNOWN_LOCALE_CHANGE_SEQUENCE_NUMBER, this.mKnownLocaleChangeSequenceNumber);
        ShortcutService.writeTagValue(out, TAG_LAUNCHER, this.mLauncherComponent);
        int size = this.mLaunchers.size();
        for (int i = 0; i < size; i++) {
            saveShortcutPackageItem(s, out, this.mLaunchers.valueAt(i), forBackup);
        }
        int size2 = this.mPackages.size();
        for (int i2 = 0; i2 < size2; i2++) {
            saveShortcutPackageItem(s, out, this.mPackages.valueAt(i2), forBackup);
        }
        out.endTag(null, TAG_ROOT);
    }

    private void saveShortcutPackageItem(ShortcutService s, XmlSerializer out, ShortcutPackageItem spi, boolean forBackup) throws XmlPullParserException, IOException {
        if (forBackup && (!s.shouldBackupApp(spi.getPackageName(), spi.getPackageUserId()) || spi.getPackageUserId() != spi.getOwnerUserId())) {
            return;
        }
        spi.saveToXml(out, forBackup);
    }

    public static ShortcutUser loadFromXml(ShortcutService s, XmlPullParser parser, int userId, boolean fromBackup) throws XmlPullParserException, IOException {
        ShortcutUser ret = new ShortcutUser(userId);
        ret.mKnownLocaleChangeSequenceNumber = ShortcutService.parseLongAttribute(parser, ATTR_KNOWN_LOCALE_CHANGE_SEQUENCE_NUMBER);
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type == 2) {
                int depth = parser.getDepth();
                String tag = parser.getName();
                if (depth == outerDepth + 1) {
                    if (!tag.equals(TAG_LAUNCHER)) {
                        if (!tag.equals("package")) {
                            if (tag.equals("launcher-pins")) {
                                ret.addLauncher(ShortcutLauncher.loadFromXml(parser, ret, userId, fromBackup));
                            }
                        } else {
                            ShortcutPackage shortcuts = ShortcutPackage.loadFromXml(s, ret, parser, fromBackup);
                            ret.mPackages.put(shortcuts.getPackageName(), shortcuts);
                        }
                    } else {
                        ret.mLauncherComponent = ShortcutService.parseComponentNameAttribute(parser, ATTR_VALUE);
                    }
                }
                ShortcutService.warnForInvalidTag(depth, tag);
            }
        }
        return ret;
    }

    public ComponentName getLauncherComponent() {
        return this.mLauncherComponent;
    }

    public void setLauncherComponent(ShortcutService s, ComponentName launcherComponent) {
        if (Objects.equal(this.mLauncherComponent, launcherComponent)) {
            return;
        }
        this.mLauncherComponent = launcherComponent;
        s.scheduleSaveUser(this.mUserId);
    }

    public void resetThrottling() {
        for (int i = this.mPackages.size() - 1; i >= 0; i--) {
            this.mPackages.valueAt(i).resetThrottling();
        }
    }

    public void dump(ShortcutService s, PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("User: ");
        pw.print(this.mUserId);
        pw.print("  Known locale seq#: ");
        pw.print(this.mKnownLocaleChangeSequenceNumber);
        pw.println();
        String prefix2 = prefix + prefix + "  ";
        pw.print(prefix2);
        pw.print("Default launcher: ");
        pw.print(this.mLauncherComponent);
        pw.println();
        for (int i = 0; i < this.mLaunchers.size(); i++) {
            this.mLaunchers.valueAt(i).dump(s, pw, prefix2);
        }
        for (int i2 = 0; i2 < this.mPackages.size(); i2++) {
            this.mPackages.valueAt(i2).dump(s, pw, prefix2);
        }
        pw.println();
        pw.print(prefix2);
        pw.println("Bitmap directories: ");
        dumpDirectorySize(s, pw, prefix2 + "  ", s.getUserBitmapFilePath(this.mUserId));
    }

    private void dumpDirectorySize(ShortcutService s, PrintWriter pw, String prefix, File path) {
        int numFiles = 0;
        long size = 0;
        File[] children = path.listFiles();
        if (children != null) {
            for (File child : path.listFiles()) {
                if (child.isFile()) {
                    numFiles++;
                    size += child.length();
                } else if (child.isDirectory()) {
                    dumpDirectorySize(s, pw, prefix + "  ", child);
                }
            }
        }
        pw.print(prefix);
        pw.print("Path: ");
        pw.print(path.getName());
        pw.print("/ has ");
        pw.print(numFiles);
        pw.print(" files, size=");
        pw.print(size);
        pw.print(" (");
        pw.print(Formatter.formatFileSize(s.mContext, size));
        pw.println(")");
    }
}
