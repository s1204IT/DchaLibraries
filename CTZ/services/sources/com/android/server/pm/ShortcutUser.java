package com.android.server.pm;

import android.content.ComponentName;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.Preconditions;
import com.android.server.pm.ShortcutService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class ShortcutUser {
    private static final String ATTR_KNOWN_LOCALES = "locales";
    private static final String ATTR_LAST_APP_SCAN_OS_FINGERPRINT = "last-app-scan-fp";
    private static final String ATTR_LAST_APP_SCAN_TIME = "last-app-scan-time2";
    private static final String ATTR_RESTORE_SOURCE_FINGERPRINT = "restore-from-fp";
    private static final String ATTR_VALUE = "value";
    private static final String KEY_LAUNCHERS = "launchers";
    private static final String KEY_PACKAGES = "packages";
    private static final String KEY_USER_ID = "userId";
    private static final String TAG = "ShortcutService";
    private static final String TAG_LAUNCHER = "launcher";
    static final String TAG_ROOT = "user";
    private ComponentName mCachedLauncher;
    private String mKnownLocales;
    private String mLastAppScanOsFingerprint;
    private long mLastAppScanTime;
    private ComponentName mLastKnownLauncher;
    private String mRestoreFromOsFingerprint;
    final ShortcutService mService;
    private final int mUserId;
    private final ArrayMap<String, ShortcutPackage> mPackages = new ArrayMap<>();
    private final ArrayMap<PackageWithUser, ShortcutLauncher> mLaunchers = new ArrayMap<>();

    static final class PackageWithUser {
        final String packageName;
        final int userId;

        private PackageWithUser(int i, String str) {
            this.userId = i;
            this.packageName = (String) Preconditions.checkNotNull(str);
        }

        public static PackageWithUser of(int i, String str) {
            return new PackageWithUser(i, str);
        }

        public static PackageWithUser of(ShortcutPackageItem shortcutPackageItem) {
            return new PackageWithUser(shortcutPackageItem.getPackageUserId(), shortcutPackageItem.getPackageName());
        }

        public int hashCode() {
            return this.packageName.hashCode() ^ this.userId;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof PackageWithUser)) {
                return false;
            }
            PackageWithUser packageWithUser = (PackageWithUser) obj;
            return this.userId == packageWithUser.userId && this.packageName.equals(packageWithUser.packageName);
        }

        public String toString() {
            return String.format("[Package: %d, %s]", Integer.valueOf(this.userId), this.packageName);
        }
    }

    public ShortcutUser(ShortcutService shortcutService, int i) {
        this.mService = shortcutService;
        this.mUserId = i;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public long getLastAppScanTime() {
        return this.mLastAppScanTime;
    }

    public void setLastAppScanTime(long j) {
        this.mLastAppScanTime = j;
    }

    public String getLastAppScanOsFingerprint() {
        return this.mLastAppScanOsFingerprint;
    }

    public void setLastAppScanOsFingerprint(String str) {
        this.mLastAppScanOsFingerprint = str;
    }

    @VisibleForTesting
    ArrayMap<String, ShortcutPackage> getAllPackagesForTest() {
        return this.mPackages;
    }

    public boolean hasPackage(String str) {
        return this.mPackages.containsKey(str);
    }

    private void addPackage(ShortcutPackage shortcutPackage) {
        shortcutPackage.replaceUser(this);
        this.mPackages.put(shortcutPackage.getPackageName(), shortcutPackage);
    }

    public ShortcutPackage removePackage(String str) {
        ShortcutPackage shortcutPackageRemove = this.mPackages.remove(str);
        this.mService.cleanupBitmapsForPackage(this.mUserId, str);
        return shortcutPackageRemove;
    }

    @VisibleForTesting
    ArrayMap<PackageWithUser, ShortcutLauncher> getAllLaunchersForTest() {
        return this.mLaunchers;
    }

    private void addLauncher(ShortcutLauncher shortcutLauncher) {
        shortcutLauncher.replaceUser(this);
        this.mLaunchers.put(PackageWithUser.of(shortcutLauncher.getPackageUserId(), shortcutLauncher.getPackageName()), shortcutLauncher);
    }

    public ShortcutLauncher removeLauncher(int i, String str) {
        return this.mLaunchers.remove(PackageWithUser.of(i, str));
    }

    public ShortcutPackage getPackageShortcutsIfExists(String str) {
        ShortcutPackage shortcutPackage = this.mPackages.get(str);
        if (shortcutPackage != null) {
            shortcutPackage.attemptToRestoreIfNeededAndSave();
        }
        return shortcutPackage;
    }

    public ShortcutPackage getPackageShortcuts(String str) {
        ShortcutPackage packageShortcutsIfExists = getPackageShortcutsIfExists(str);
        if (packageShortcutsIfExists == null) {
            ShortcutPackage shortcutPackage = new ShortcutPackage(this, this.mUserId, str);
            this.mPackages.put(str, shortcutPackage);
            return shortcutPackage;
        }
        return packageShortcutsIfExists;
    }

    public ShortcutLauncher getLauncherShortcuts(String str, int i) {
        PackageWithUser packageWithUserOf = PackageWithUser.of(i, str);
        ShortcutLauncher shortcutLauncher = this.mLaunchers.get(packageWithUserOf);
        if (shortcutLauncher == null) {
            ShortcutLauncher shortcutLauncher2 = new ShortcutLauncher(this, this.mUserId, str, i);
            this.mLaunchers.put(packageWithUserOf, shortcutLauncher2);
            return shortcutLauncher2;
        }
        shortcutLauncher.attemptToRestoreIfNeededAndSave();
        return shortcutLauncher;
    }

    public void forAllPackages(Consumer<? super ShortcutPackage> consumer) {
        int size = this.mPackages.size();
        for (int i = 0; i < size; i++) {
            consumer.accept(this.mPackages.valueAt(i));
        }
    }

    public void forAllLaunchers(Consumer<? super ShortcutLauncher> consumer) {
        int size = this.mLaunchers.size();
        for (int i = 0; i < size; i++) {
            consumer.accept(this.mLaunchers.valueAt(i));
        }
    }

    public void forAllPackageItems(Consumer<? super ShortcutPackageItem> consumer) {
        forAllLaunchers(consumer);
        forAllPackages(consumer);
    }

    public void forPackageItem(final String str, final int i, final Consumer<ShortcutPackageItem> consumer) {
        forAllPackageItems(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ShortcutUser.lambda$forPackageItem$0(i, str, consumer, (ShortcutPackageItem) obj);
            }
        });
    }

    static void lambda$forPackageItem$0(int i, String str, Consumer consumer, ShortcutPackageItem shortcutPackageItem) {
        if (shortcutPackageItem.getPackageUserId() == i && shortcutPackageItem.getPackageName().equals(str)) {
            consumer.accept(shortcutPackageItem);
        }
    }

    public void onCalledByPublisher(String str) {
        detectLocaleChange();
        rescanPackageIfNeeded(str, false);
    }

    private String getKnownLocales() {
        if (TextUtils.isEmpty(this.mKnownLocales)) {
            this.mKnownLocales = this.mService.injectGetLocaleTagsForUser(this.mUserId);
            this.mService.scheduleSaveUser(this.mUserId);
        }
        return this.mKnownLocales;
    }

    public void detectLocaleChange() {
        String strInjectGetLocaleTagsForUser = this.mService.injectGetLocaleTagsForUser(this.mUserId);
        if (getKnownLocales().equals(strInjectGetLocaleTagsForUser)) {
            return;
        }
        this.mKnownLocales = strInjectGetLocaleTagsForUser;
        forAllPackages(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ShortcutUser.lambda$detectLocaleChange$1((ShortcutPackage) obj);
            }
        });
        this.mService.scheduleSaveUser(this.mUserId);
    }

    static void lambda$detectLocaleChange$1(ShortcutPackage shortcutPackage) {
        shortcutPackage.resetRateLimiting();
        shortcutPackage.resolveResourceStrings();
    }

    public void rescanPackageIfNeeded(String str, boolean z) {
        boolean z2 = !this.mPackages.containsKey(str);
        if (!getPackageShortcuts(str).rescanPackageIfNeeded(z2, z) && z2) {
            this.mPackages.remove(str);
        }
    }

    public void attemptToRestoreIfNeededAndSave(ShortcutService shortcutService, String str, int i) {
        forPackageItem(str, i, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ShortcutPackageItem) obj).attemptToRestoreIfNeededAndSave();
            }
        });
    }

    public void saveToXml(XmlSerializer xmlSerializer, boolean z) throws XmlPullParserException, IOException {
        xmlSerializer.startTag(null, TAG_ROOT);
        if (!z) {
            ShortcutService.writeAttr(xmlSerializer, ATTR_KNOWN_LOCALES, this.mKnownLocales);
            ShortcutService.writeAttr(xmlSerializer, ATTR_LAST_APP_SCAN_TIME, this.mLastAppScanTime);
            ShortcutService.writeAttr(xmlSerializer, ATTR_LAST_APP_SCAN_OS_FINGERPRINT, this.mLastAppScanOsFingerprint);
            ShortcutService.writeAttr(xmlSerializer, ATTR_RESTORE_SOURCE_FINGERPRINT, this.mRestoreFromOsFingerprint);
            ShortcutService.writeTagValue(xmlSerializer, TAG_LAUNCHER, this.mLastKnownLauncher);
        } else {
            ShortcutService.writeAttr(xmlSerializer, ATTR_RESTORE_SOURCE_FINGERPRINT, this.mService.injectBuildFingerprint());
        }
        int size = this.mLaunchers.size();
        for (int i = 0; i < size; i++) {
            saveShortcutPackageItem(xmlSerializer, this.mLaunchers.valueAt(i), z);
        }
        int size2 = this.mPackages.size();
        for (int i2 = 0; i2 < size2; i2++) {
            saveShortcutPackageItem(xmlSerializer, this.mPackages.valueAt(i2), z);
        }
        xmlSerializer.endTag(null, TAG_ROOT);
    }

    private void saveShortcutPackageItem(XmlSerializer xmlSerializer, ShortcutPackageItem shortcutPackageItem, boolean z) throws XmlPullParserException, IOException {
        if (z && shortcutPackageItem.getPackageUserId() != shortcutPackageItem.getOwnerUserId()) {
            return;
        }
        shortcutPackageItem.saveToXml(xmlSerializer, z);
    }

    public static ShortcutUser loadFromXml(ShortcutService shortcutService, XmlPullParser xmlPullParser, int i, boolean z) throws XmlPullParserException, IOException, ShortcutService.InvalidFileFormatException {
        ShortcutUser shortcutUser = new ShortcutUser(shortcutService, i);
        try {
            shortcutUser.mKnownLocales = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_KNOWN_LOCALES);
            long longAttribute = ShortcutService.parseLongAttribute(xmlPullParser, ATTR_LAST_APP_SCAN_TIME);
            if (longAttribute >= shortcutService.injectCurrentTimeMillis()) {
                longAttribute = 0;
            }
            shortcutUser.mLastAppScanTime = longAttribute;
            shortcutUser.mLastAppScanOsFingerprint = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_LAST_APP_SCAN_OS_FINGERPRINT);
            shortcutUser.mRestoreFromOsFingerprint = ShortcutService.parseStringAttribute(xmlPullParser, ATTR_RESTORE_SOURCE_FINGERPRINT);
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
                            if (iHashCode == -1407250528) {
                                if (name.equals(TAG_LAUNCHER)) {
                                    b = 0;
                                }
                                switch (b) {
                                }
                            } else if (iHashCode != -1146595445) {
                                if (iHashCode != -807062458 || !name.equals(Settings.ATTR_PACKAGE)) {
                                    b = -1;
                                }
                                switch (b) {
                                    case 0:
                                        shortcutUser.mLastKnownLauncher = ShortcutService.parseComponentNameAttribute(xmlPullParser, ATTR_VALUE);
                                        continue;
                                        continue;
                                        continue;
                                    case 1:
                                        ShortcutPackage shortcutPackageLoadFromXml = ShortcutPackage.loadFromXml(shortcutService, shortcutUser, xmlPullParser, z);
                                        shortcutUser.mPackages.put(shortcutPackageLoadFromXml.getPackageName(), shortcutPackageLoadFromXml);
                                        continue;
                                        continue;
                                        continue;
                                    case 2:
                                        shortcutUser.addLauncher(ShortcutLauncher.loadFromXml(xmlPullParser, shortcutUser, i, z));
                                        continue;
                                        continue;
                                        continue;
                                }
                            } else {
                                if (name.equals("launcher-pins")) {
                                    b = 2;
                                }
                                switch (b) {
                                }
                            }
                        }
                        ShortcutService.warnForInvalidTag(depth2, name);
                    }
                }
            }
            return shortcutUser;
        } catch (RuntimeException e) {
            throw new ShortcutService.InvalidFileFormatException("Unable to parse file", e);
        }
    }

    public ComponentName getLastKnownLauncher() {
        return this.mLastKnownLauncher;
    }

    public void setLauncher(ComponentName componentName) {
        setLauncher(componentName, false);
    }

    public void clearLauncher() {
        setLauncher(null);
    }

    public void forceClearLauncher() {
        setLauncher(null, true);
    }

    private void setLauncher(ComponentName componentName, boolean z) {
        this.mCachedLauncher = componentName;
        if (Objects.equals(this.mLastKnownLauncher, componentName)) {
            return;
        }
        if (!z && componentName == null) {
            return;
        }
        this.mLastKnownLauncher = componentName;
        this.mService.scheduleSaveUser(this.mUserId);
    }

    public ComponentName getCachedLauncher() {
        return this.mCachedLauncher;
    }

    public void resetThrottling() {
        for (int size = this.mPackages.size() - 1; size >= 0; size--) {
            this.mPackages.valueAt(size).resetThrottling();
        }
    }

    public void mergeRestoredFile(ShortcutUser shortcutUser) {
        final ShortcutService shortcutService = this.mService;
        final int[] iArr = new int[1];
        final int[] iArr2 = new int[1];
        final int[] iArr3 = new int[1];
        this.mLaunchers.clear();
        shortcutUser.forAllLaunchers(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ShortcutUser.lambda$mergeRestoredFile$3(this.f$0, shortcutService, iArr, (ShortcutLauncher) obj);
            }
        });
        shortcutUser.forAllPackages(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ShortcutUser.lambda$mergeRestoredFile$4(this.f$0, shortcutService, iArr2, iArr3, (ShortcutPackage) obj);
            }
        });
        shortcutUser.mLaunchers.clear();
        shortcutUser.mPackages.clear();
        this.mRestoreFromOsFingerprint = shortcutUser.mRestoreFromOsFingerprint;
        Slog.i(TAG, "Restored: L=" + iArr[0] + " P=" + iArr2[0] + " S=" + iArr3[0]);
    }

    public static void lambda$mergeRestoredFile$3(ShortcutUser shortcutUser, ShortcutService shortcutService, int[] iArr, ShortcutLauncher shortcutLauncher) {
        if (shortcutService.isPackageInstalled(shortcutLauncher.getPackageName(), shortcutUser.getUserId()) && !shortcutService.shouldBackupApp(shortcutLauncher.getPackageName(), shortcutUser.getUserId())) {
            return;
        }
        shortcutUser.addLauncher(shortcutLauncher);
        iArr[0] = iArr[0] + 1;
    }

    public static void lambda$mergeRestoredFile$4(ShortcutUser shortcutUser, ShortcutService shortcutService, int[] iArr, int[] iArr2, ShortcutPackage shortcutPackage) {
        if (shortcutService.isPackageInstalled(shortcutPackage.getPackageName(), shortcutUser.getUserId()) && !shortcutService.shouldBackupApp(shortcutPackage.getPackageName(), shortcutUser.getUserId())) {
            return;
        }
        ShortcutPackage packageShortcutsIfExists = shortcutUser.getPackageShortcutsIfExists(shortcutPackage.getPackageName());
        if (packageShortcutsIfExists != null && packageShortcutsIfExists.hasNonManifestShortcuts()) {
            Log.w(TAG, "Shortcuts for package " + shortcutPackage.getPackageName() + " are being restored. Existing non-manifeset shortcuts will be overwritten.");
        }
        shortcutUser.addPackage(shortcutPackage);
        iArr[0] = iArr[0] + 1;
        iArr2[0] = iArr2[0] + shortcutPackage.getShortcutCount();
    }

    public void dump(PrintWriter printWriter, String str, ShortcutService.DumpFilter dumpFilter) {
        if (dumpFilter.shouldDumpDetails()) {
            printWriter.print(str);
            printWriter.print("User: ");
            printWriter.print(this.mUserId);
            printWriter.print("  Known locales: ");
            printWriter.print(this.mKnownLocales);
            printWriter.print("  Last app scan: [");
            printWriter.print(this.mLastAppScanTime);
            printWriter.print("] ");
            printWriter.println(ShortcutService.formatTime(this.mLastAppScanTime));
            str = str + str + "  ";
            printWriter.print(str);
            printWriter.print("Last app scan FP: ");
            printWriter.println(this.mLastAppScanOsFingerprint);
            printWriter.print(str);
            printWriter.print("Restore from FP: ");
            printWriter.print(this.mRestoreFromOsFingerprint);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("Cached launcher: ");
            printWriter.print(this.mCachedLauncher);
            printWriter.println();
            printWriter.print(str);
            printWriter.print("Last known launcher: ");
            printWriter.print(this.mLastKnownLauncher);
            printWriter.println();
        }
        for (int i = 0; i < this.mLaunchers.size(); i++) {
            ShortcutLauncher shortcutLauncherValueAt = this.mLaunchers.valueAt(i);
            if (dumpFilter.isPackageMatch(shortcutLauncherValueAt.getPackageName())) {
                shortcutLauncherValueAt.dump(printWriter, str, dumpFilter);
            }
        }
        for (int i2 = 0; i2 < this.mPackages.size(); i2++) {
            ShortcutPackage shortcutPackageValueAt = this.mPackages.valueAt(i2);
            if (dumpFilter.isPackageMatch(shortcutPackageValueAt.getPackageName())) {
                shortcutPackageValueAt.dump(printWriter, str, dumpFilter);
            }
        }
        if (dumpFilter.shouldDumpDetails()) {
            printWriter.println();
            printWriter.print(str);
            printWriter.println("Bitmap directories: ");
            dumpDirectorySize(printWriter, str + "  ", this.mService.getUserBitmapFilePath(this.mUserId));
        }
    }

    private void dumpDirectorySize(PrintWriter printWriter, String str, File file) {
        int i = 0;
        long j = 0;
        if (file.listFiles() != null) {
            File[] fileArrListFiles = file.listFiles();
            int length = fileArrListFiles.length;
            long length2 = 0;
            int i2 = 0;
            while (i < length) {
                File file2 = fileArrListFiles[i];
                if (file2.isFile()) {
                    i2++;
                    length2 += file2.length();
                } else if (file2.isDirectory()) {
                    dumpDirectorySize(printWriter, str + "  ", file2);
                }
                i++;
            }
            i = i2;
            j = length2;
        }
        printWriter.print(str);
        printWriter.print("Path: ");
        printWriter.print(file.getName());
        printWriter.print("/ has ");
        printWriter.print(i);
        printWriter.print(" files, size=");
        printWriter.print(j);
        printWriter.print(" (");
        printWriter.print(Formatter.formatFileSize(this.mService.mContext, j));
        printWriter.println(")");
    }

    public JSONObject dumpCheckin(boolean z) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put(KEY_USER_ID, this.mUserId);
        JSONArray jSONArray = new JSONArray();
        for (int i = 0; i < this.mLaunchers.size(); i++) {
            jSONArray.put(this.mLaunchers.valueAt(i).dumpCheckin(z));
        }
        jSONObject.put(KEY_LAUNCHERS, jSONArray);
        JSONArray jSONArray2 = new JSONArray();
        for (int i2 = 0; i2 < this.mPackages.size(); i2++) {
            jSONArray2.put(this.mPackages.valueAt(i2).dumpCheckin(z));
        }
        jSONObject.put(KEY_PACKAGES, jSONArray2);
        return jSONObject;
    }
}
