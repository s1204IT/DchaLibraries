package com.android.server.pm;

import android.content.Intent;
import android.content.pm.InstantAppInfo;
import android.content.pm.PackageParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.ByteStringUtils;
import android.util.PackageUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.InstantAppRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

class InstantAppRegistry {
    private static final String ATTR_GRANTED = "granted";
    private static final String ATTR_LABEL = "label";
    private static final String ATTR_NAME = "name";
    private static final boolean DEBUG = false;
    private static final long DEFAULT_INSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_INSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final long DEFAULT_UNINSTALLED_INSTANT_APP_MAX_CACHE_PERIOD = 15552000000L;
    static final long DEFAULT_UNINSTALLED_INSTANT_APP_MIN_CACHE_PERIOD = 604800000;
    private static final String INSTANT_APPS_FOLDER = "instant";
    private static final String INSTANT_APP_ANDROID_ID_FILE = "android_id";
    private static final String INSTANT_APP_COOKIE_FILE_PREFIX = "cookie_";
    private static final String INSTANT_APP_COOKIE_FILE_SIFFIX = ".dat";
    private static final String INSTANT_APP_ICON_FILE = "icon.png";
    private static final String INSTANT_APP_METADATA_FILE = "metadata.xml";
    private static final String LOG_TAG = "InstantAppRegistry";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_PERMISSION = "permission";
    private static final String TAG_PERMISSIONS = "permissions";
    private final CookiePersistence mCookiePersistence = new CookiePersistence(BackgroundThread.getHandler().getLooper());

    @GuardedBy("mService.mPackages")
    private SparseArray<SparseBooleanArray> mInstalledInstantAppUids;

    @GuardedBy("mService.mPackages")
    private SparseArray<SparseArray<SparseBooleanArray>> mInstantGrants;
    private final PackageManagerService mService;

    @GuardedBy("mService.mPackages")
    private SparseArray<List<UninstalledInstantAppState>> mUninstalledInstantApps;

    public InstantAppRegistry(PackageManagerService packageManagerService) {
        this.mService = packageManagerService;
    }

    public byte[] getInstantAppCookieLPw(String str, int i) {
        PackageParser.Package r0 = this.mService.mPackages.get(str);
        if (r0 == null) {
            return null;
        }
        byte[] pendingPersistCookieLPr = this.mCookiePersistence.getPendingPersistCookieLPr(r0, i);
        if (pendingPersistCookieLPr != null) {
            return pendingPersistCookieLPr;
        }
        File filePeekInstantCookieFile = peekInstantCookieFile(str, i);
        if (filePeekInstantCookieFile != null && filePeekInstantCookieFile.exists()) {
            try {
                return IoUtils.readFileAsByteArray(filePeekInstantCookieFile.toString());
            } catch (IOException e) {
                Slog.w(LOG_TAG, "Error reading cookie file: " + filePeekInstantCookieFile);
            }
        }
        return null;
    }

    public boolean setInstantAppCookieLPw(String str, byte[] bArr, int i) {
        int instantAppCookieMaxBytes;
        if (bArr != null && bArr.length > 0 && bArr.length > (instantAppCookieMaxBytes = this.mService.mContext.getPackageManager().getInstantAppCookieMaxBytes())) {
            Slog.e(LOG_TAG, "Instant app cookie for package " + str + " size " + bArr.length + " bytes while max size is " + instantAppCookieMaxBytes);
            return false;
        }
        PackageParser.Package r5 = this.mService.mPackages.get(str);
        if (r5 == null) {
            return false;
        }
        this.mCookiePersistence.schedulePersistLPw(i, r5, bArr);
        return true;
    }

    private void persistInstantApplicationCookie(byte[] bArr, String str, File file, int i) throws Exception {
        synchronized (this.mService.mPackages) {
            File instantApplicationDir = getInstantApplicationDir(str, i);
            if (!instantApplicationDir.exists() && !instantApplicationDir.mkdirs()) {
                Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
                return;
            }
            if (file.exists() && !file.delete()) {
                Slog.e(LOG_TAG, "Cannot delete instant app cookie file");
            }
            if (bArr != null && bArr.length > 0) {
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    try {
                        fileOutputStream.write(bArr, 0, bArr.length);
                    } finally {
                        $closeResource(null, fileOutputStream);
                    }
                } catch (IOException e) {
                    Slog.e(LOG_TAG, "Error writing instant app cookie file: " + file, e);
                }
            }
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public Bitmap getInstantAppIconLPw(String str, int i) {
        File file = new File(getInstantApplicationDir(str, i), INSTANT_APP_ICON_FILE);
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.toString());
        }
        return null;
    }

    public String getInstantAppAndroidIdLPw(String str, int i) {
        File file = new File(getInstantApplicationDir(str, i), INSTANT_APP_ANDROID_ID_FILE);
        if (file.exists()) {
            try {
                return IoUtils.readFileAsString(file.getAbsolutePath());
            } catch (IOException e) {
                Slog.e(LOG_TAG, "Failed to read instant app android id file: " + file, e);
            }
        }
        return generateInstantAppAndroidIdLPw(str, i);
    }

    private String generateInstantAppAndroidIdLPw(String str, int i) throws Exception {
        byte[] bArr = new byte[8];
        new SecureRandom().nextBytes(bArr);
        String lowerCase = ByteStringUtils.toHexString(bArr).toLowerCase(Locale.US);
        File instantApplicationDir = getInstantApplicationDir(str, i);
        if (!instantApplicationDir.exists() && !instantApplicationDir.mkdirs()) {
            Slog.e(LOG_TAG, "Cannot create instant app cookie directory");
            return lowerCase;
        }
        File file = new File(getInstantApplicationDir(str, i), INSTANT_APP_ANDROID_ID_FILE);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            Throwable th = null;
            try {
                fileOutputStream.write(lowerCase.getBytes());
            } finally {
                $closeResource(th, fileOutputStream);
            }
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error writing instant app android id file: " + file, e);
        }
        return lowerCase;
    }

    public List<InstantAppInfo> getInstantAppsLPr(int i) {
        List<InstantAppInfo> installedInstantApplicationsLPr = getInstalledInstantApplicationsLPr(i);
        List<InstantAppInfo> uninstalledInstantApplicationsLPr = getUninstalledInstantApplicationsLPr(i);
        if (installedInstantApplicationsLPr != null) {
            if (uninstalledInstantApplicationsLPr != null) {
                installedInstantApplicationsLPr.addAll(uninstalledInstantApplicationsLPr);
            }
            return installedInstantApplicationsLPr;
        }
        return uninstalledInstantApplicationsLPr;
    }

    public void onPackageInstalledLPw(final PackageParser.Package r12, int[] iArr) {
        PackageSetting packageSetting = (PackageSetting) r12.mExtras;
        if (packageSetting == null) {
            return;
        }
        for (int i : iArr) {
            if (this.mService.mPackages.get(r12.packageName) != null && packageSetting.getInstalled(i)) {
                propagateInstantAppPermissionsIfNeeded(r12, i);
                if (packageSetting.getInstantApp(i)) {
                    addInstantAppLPw(i, packageSetting.appId);
                }
                removeUninstalledInstantAppStateLPw(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return ((InstantAppRegistry.UninstalledInstantAppState) obj).mInstantAppInfo.getPackageName().equals(r12.packageName);
                    }
                }, i);
                File instantApplicationDir = getInstantApplicationDir(r12.packageName, i);
                new File(instantApplicationDir, INSTANT_APP_METADATA_FILE).delete();
                new File(instantApplicationDir, INSTANT_APP_ICON_FILE).delete();
                File filePeekInstantCookieFile = peekInstantCookieFile(r12.packageName, i);
                if (filePeekInstantCookieFile == null) {
                    continue;
                } else {
                    String name = filePeekInstantCookieFile.getName();
                    String strSubstring = name.substring(INSTANT_APP_COOKIE_FILE_PREFIX.length(), name.length() - INSTANT_APP_COOKIE_FILE_SIFFIX.length());
                    if (r12.mSigningDetails.checkCapability(strSubstring, 1)) {
                        return;
                    }
                    for (String str : PackageUtils.computeSignaturesSha256Digests(r12.mSigningDetails.signatures)) {
                        if (str.equals(strSubstring)) {
                            return;
                        }
                    }
                    Slog.i(LOG_TAG, "Signature for package " + r12.packageName + " changed - dropping cookie");
                    this.mCookiePersistence.cancelPendingPersistLPw(r12, i);
                    filePeekInstantCookieFile.delete();
                }
            }
        }
    }

    public void onPackageUninstalledLPw(PackageParser.Package r7, int[] iArr) throws Throwable {
        PackageSetting packageSetting = (PackageSetting) r7.mExtras;
        if (packageSetting == null) {
            return;
        }
        for (int i : iArr) {
            if (this.mService.mPackages.get(r7.packageName) == null || !packageSetting.getInstalled(i)) {
                if (packageSetting.getInstantApp(i)) {
                    addUninstalledInstantAppLPw(r7, i);
                    removeInstantAppLPw(i, packageSetting.appId);
                } else {
                    deleteDir(getInstantApplicationDir(r7.packageName, i));
                    this.mCookiePersistence.cancelPendingPersistLPw(r7, i);
                    removeAppLPw(i, packageSetting.appId);
                }
            }
        }
    }

    public void onUserRemovedLPw(int i) {
        if (this.mUninstalledInstantApps != null) {
            this.mUninstalledInstantApps.remove(i);
            if (this.mUninstalledInstantApps.size() <= 0) {
                this.mUninstalledInstantApps = null;
            }
        }
        if (this.mInstalledInstantAppUids != null) {
            this.mInstalledInstantAppUids.remove(i);
            if (this.mInstalledInstantAppUids.size() <= 0) {
                this.mInstalledInstantAppUids = null;
            }
        }
        if (this.mInstantGrants != null) {
            this.mInstantGrants.remove(i);
            if (this.mInstantGrants.size() <= 0) {
                this.mInstantGrants = null;
            }
        }
        deleteDir(getInstantApplicationsDir(i));
    }

    public boolean isInstantAccessGranted(int i, int i2, int i3) {
        SparseArray<SparseBooleanArray> sparseArray;
        SparseBooleanArray sparseBooleanArray;
        if (this.mInstantGrants == null || (sparseArray = this.mInstantGrants.get(i)) == null || (sparseBooleanArray = sparseArray.get(i2)) == null) {
            return false;
        }
        return sparseBooleanArray.get(i3);
    }

    public void grantInstantAccessLPw(int i, Intent intent, int i2, int i3) {
        SparseBooleanArray sparseBooleanArray;
        Set<String> categories;
        if (this.mInstalledInstantAppUids == null || (sparseBooleanArray = this.mInstalledInstantAppUids.get(i)) == null || !sparseBooleanArray.get(i3) || sparseBooleanArray.get(i2)) {
            return;
        }
        if (intent != null && "android.intent.action.VIEW".equals(intent.getAction()) && (categories = intent.getCategories()) != null && categories.contains("android.intent.category.BROWSABLE")) {
            return;
        }
        if (this.mInstantGrants == null) {
            this.mInstantGrants = new SparseArray<>();
        }
        SparseArray<SparseBooleanArray> sparseArray = this.mInstantGrants.get(i);
        if (sparseArray == null) {
            sparseArray = new SparseArray<>();
            this.mInstantGrants.put(i, sparseArray);
        }
        SparseBooleanArray sparseBooleanArray2 = sparseArray.get(i2);
        if (sparseBooleanArray2 == null) {
            sparseBooleanArray2 = new SparseBooleanArray();
            sparseArray.put(i2, sparseBooleanArray2);
        }
        sparseBooleanArray2.put(i3, true);
    }

    public void addInstantAppLPw(int i, int i2) {
        if (this.mInstalledInstantAppUids == null) {
            this.mInstalledInstantAppUids = new SparseArray<>();
        }
        SparseBooleanArray sparseBooleanArray = this.mInstalledInstantAppUids.get(i);
        if (sparseBooleanArray == null) {
            sparseBooleanArray = new SparseBooleanArray();
            this.mInstalledInstantAppUids.put(i, sparseBooleanArray);
        }
        sparseBooleanArray.put(i2, true);
    }

    private void removeInstantAppLPw(int i, int i2) {
        SparseBooleanArray sparseBooleanArray;
        SparseArray<SparseBooleanArray> sparseArray;
        if (this.mInstalledInstantAppUids == null || (sparseBooleanArray = this.mInstalledInstantAppUids.get(i)) == null) {
            return;
        }
        sparseBooleanArray.delete(i2);
        if (this.mInstantGrants == null || (sparseArray = this.mInstantGrants.get(i)) == null) {
            return;
        }
        for (int size = sparseArray.size() - 1; size >= 0; size--) {
            sparseArray.valueAt(size).delete(i2);
        }
    }

    private void removeAppLPw(int i, int i2) {
        SparseArray<SparseBooleanArray> sparseArray;
        if (this.mInstantGrants == null || (sparseArray = this.mInstantGrants.get(i)) == null) {
            return;
        }
        sparseArray.delete(i2);
    }

    private void addUninstalledInstantAppLPw(PackageParser.Package r6, int i) throws Throwable {
        InstantAppInfo instantAppInfoCreateInstantAppInfoForPackage = createInstantAppInfoForPackage(r6, i, false);
        if (instantAppInfoCreateInstantAppInfoForPackage == null) {
            return;
        }
        if (this.mUninstalledInstantApps == null) {
            this.mUninstalledInstantApps = new SparseArray<>();
        }
        List<UninstalledInstantAppState> arrayList = this.mUninstalledInstantApps.get(i);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mUninstalledInstantApps.put(i, arrayList);
        }
        arrayList.add(new UninstalledInstantAppState(instantAppInfoCreateInstantAppInfoForPackage, System.currentTimeMillis()));
        writeUninstalledInstantAppMetadata(instantAppInfoCreateInstantAppInfoForPackage, i);
        writeInstantApplicationIconLPw(r6, i);
    }

    private void writeInstantApplicationIconLPw(PackageParser.Package r7, int i) {
        Bitmap bitmap;
        if (!getInstantApplicationDir(r7.packageName, i).exists()) {
            return;
        }
        Drawable drawableLoadIcon = r7.applicationInfo.loadIcon(this.mService.mContext.getPackageManager());
        if (drawableLoadIcon instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawableLoadIcon).getBitmap();
        } else {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(drawableLoadIcon.getIntrinsicWidth(), drawableLoadIcon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            drawableLoadIcon.setBounds(0, 0, drawableLoadIcon.getIntrinsicWidth(), drawableLoadIcon.getIntrinsicHeight());
            drawableLoadIcon.draw(canvas);
            bitmap = bitmapCreateBitmap;
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(getInstantApplicationDir(r7.packageName, i), INSTANT_APP_ICON_FILE));
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            } finally {
                $closeResource(null, fileOutputStream);
            }
        } catch (Exception e) {
            Slog.e(LOG_TAG, "Error writing instant app icon", e);
        }
    }

    boolean hasInstantApplicationMetadataLPr(String str, int i) {
        return hasUninstalledInstantAppStateLPr(str, i) || hasInstantAppMetadataLPr(str, i);
    }

    public void deleteInstantApplicationMetadataLPw(final String str, int i) {
        removeUninstalledInstantAppStateLPw(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((InstantAppRegistry.UninstalledInstantAppState) obj).mInstantAppInfo.getPackageName().equals(str);
            }
        }, i);
        File instantApplicationDir = getInstantApplicationDir(str, i);
        new File(instantApplicationDir, INSTANT_APP_METADATA_FILE).delete();
        new File(instantApplicationDir, INSTANT_APP_ICON_FILE).delete();
        new File(instantApplicationDir, INSTANT_APP_ANDROID_ID_FILE).delete();
        File filePeekInstantCookieFile = peekInstantCookieFile(str, i);
        if (filePeekInstantCookieFile != null) {
            filePeekInstantCookieFile.delete();
        }
    }

    private void removeUninstalledInstantAppStateLPw(Predicate<UninstalledInstantAppState> predicate, int i) {
        List<UninstalledInstantAppState> list;
        if (this.mUninstalledInstantApps == null || (list = this.mUninstalledInstantApps.get(i)) == null) {
            return;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            if (predicate.test(list.get(size))) {
                list.remove(size);
                if (list.isEmpty()) {
                    this.mUninstalledInstantApps.remove(i);
                    if (this.mUninstalledInstantApps.size() <= 0) {
                        this.mUninstalledInstantApps = null;
                        return;
                    }
                    return;
                }
            }
        }
    }

    private boolean hasUninstalledInstantAppStateLPr(String str, int i) {
        List<UninstalledInstantAppState> list;
        if (this.mUninstalledInstantApps == null || (list = this.mUninstalledInstantApps.get(i)) == null) {
            return false;
        }
        int size = list.size();
        for (int i2 = 0; i2 < size; i2++) {
            if (str.equals(list.get(i2).mInstantAppInfo.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasInstantAppMetadataLPr(String str, int i) {
        File instantApplicationDir = getInstantApplicationDir(str, i);
        return new File(instantApplicationDir, INSTANT_APP_METADATA_FILE).exists() || new File(instantApplicationDir, INSTANT_APP_ICON_FILE).exists() || new File(instantApplicationDir, INSTANT_APP_ANDROID_ID_FILE).exists() || peekInstantCookieFile(str, i) != null;
    }

    void pruneInstantApps() {
        try {
            pruneInstantApps(JobStatus.NO_LATEST_RUNTIME, Settings.Global.getLong(this.mService.mContext.getContentResolver(), "installed_instant_app_max_cache_period", 15552000000L), Settings.Global.getLong(this.mService.mContext.getContentResolver(), "uninstalled_instant_app_max_cache_period", 15552000000L));
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed and uninstalled instant apps", e);
        }
    }

    boolean pruneInstalledInstantApps(long j, long j2) {
        try {
            return pruneInstantApps(j, j2, JobStatus.NO_LATEST_RUNTIME);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning installed instant apps", e);
            return false;
        }
    }

    boolean pruneUninstalledInstantApps(long j, long j2) {
        try {
            return pruneInstantApps(j, JobStatus.NO_LATEST_RUNTIME, j2);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Error pruning uninstalled instant apps", e);
            return false;
        }
    }

    private boolean pruneInstantApps(long j, long j2, final long j3) throws IOException {
        ArrayList arrayList;
        File[] fileArrListFiles;
        long j4;
        File fileFindPathForUuid = ((StorageManager) this.mService.mContext.getSystemService(StorageManager.class)).findPathForUuid(StorageManager.UUID_PRIVATE_INTERNAL);
        if (fileFindPathForUuid.getUsableSpace() >= j) {
            return true;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        synchronized (this.mService.mPackages) {
            int[] userIds = PackageManagerService.sUserManager.getUserIds();
            int size = this.mService.mPackages.size();
            arrayList = null;
            int i = 0;
            while (i < size) {
                PackageParser.Package packageValueAt = this.mService.mPackages.valueAt(i);
                if (jCurrentTimeMillis - packageValueAt.getLatestPackageUseTimeInMills() < j2 || !(packageValueAt.mExtras instanceof PackageSetting)) {
                    j4 = jCurrentTimeMillis;
                } else {
                    PackageSetting packageSetting = (PackageSetting) packageValueAt.mExtras;
                    int length = userIds.length;
                    j4 = jCurrentTimeMillis;
                    int i2 = 0;
                    boolean z = false;
                    while (true) {
                        if (i2 >= length) {
                            break;
                        }
                        int i3 = length;
                        int i4 = userIds[i2];
                        if (packageSetting.getInstalled(i4)) {
                            if (packageSetting.getInstantApp(i4)) {
                                z = true;
                            } else {
                                z = false;
                                break;
                            }
                        }
                        i2++;
                        length = i3;
                    }
                    if (z) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(packageValueAt.packageName);
                    }
                }
                i++;
                jCurrentTimeMillis = j4;
            }
            if (arrayList != null) {
                arrayList.sort(new Comparator() {
                    @Override
                    public final int compare(Object obj, Object obj2) {
                        return InstantAppRegistry.lambda$pruneInstantApps$2(this.f$0, (String) obj, (String) obj2);
                    }
                });
            }
        }
        if (arrayList != null) {
            int size2 = arrayList.size();
            for (int i5 = 0; i5 < size2; i5++) {
                if (this.mService.deletePackageX((String) arrayList.get(i5), -1L, 0, 2) == 1 && fileFindPathForUuid.getUsableSpace() >= j) {
                    return true;
                }
            }
        }
        synchronized (this.mService.mPackages) {
            for (int i6 : UserManagerService.getInstance().getUserIds()) {
                removeUninstalledInstantAppStateLPw(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return InstantAppRegistry.lambda$pruneInstantApps$3(j3, (InstantAppRegistry.UninstalledInstantAppState) obj);
                    }
                }, i6);
                File instantApplicationsDir = getInstantApplicationsDir(i6);
                if (instantApplicationsDir.exists() && (fileArrListFiles = instantApplicationsDir.listFiles()) != null) {
                    for (File file : fileArrListFiles) {
                        if (file.isDirectory()) {
                            File file2 = new File(file, INSTANT_APP_METADATA_FILE);
                            if (file2.exists() && System.currentTimeMillis() - file2.lastModified() > j3) {
                                deleteDir(file);
                                if (fileFindPathForUuid.getUsableSpace() >= j) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

    public static int lambda$pruneInstantApps$2(InstantAppRegistry instantAppRegistry, String str, String str2) {
        PackageParser.Package r8 = instantAppRegistry.mService.mPackages.get(str);
        PackageParser.Package r9 = instantAppRegistry.mService.mPackages.get(str2);
        if (r8 == null && r9 == null) {
            return 0;
        }
        if (r8 == null) {
            return -1;
        }
        if (r9 == null || r8.getLatestPackageUseTimeInMills() > r9.getLatestPackageUseTimeInMills()) {
            return 1;
        }
        if (r8.getLatestPackageUseTimeInMills() < r9.getLatestPackageUseTimeInMills()) {
            return -1;
        }
        if (!(r8.mExtras instanceof PackageSetting) || !(r9.mExtras instanceof PackageSetting)) {
            return 0;
        }
        if (((PackageSetting) r8.mExtras).firstInstallTime <= ((PackageSetting) r9.mExtras).firstInstallTime) {
            return -1;
        }
        return 1;
    }

    static boolean lambda$pruneInstantApps$3(long j, UninstalledInstantAppState uninstalledInstantAppState) {
        return System.currentTimeMillis() - uninstalledInstantAppState.mTimestamp > j;
    }

    private List<InstantAppInfo> getInstalledInstantApplicationsLPr(int i) {
        InstantAppInfo instantAppInfoCreateInstantAppInfoForPackage;
        int size = this.mService.mPackages.size();
        ArrayList arrayList = null;
        for (int i2 = 0; i2 < size; i2++) {
            PackageParser.Package packageValueAt = this.mService.mPackages.valueAt(i2);
            PackageSetting packageSetting = (PackageSetting) packageValueAt.mExtras;
            if (packageSetting != null && packageSetting.getInstantApp(i) && (instantAppInfoCreateInstantAppInfoForPackage = createInstantAppInfoForPackage(packageValueAt, i, true)) != null) {
                if (arrayList == null) {
                    arrayList = new ArrayList();
                }
                arrayList.add(instantAppInfoCreateInstantAppInfoForPackage);
            }
        }
        return arrayList;
    }

    private InstantAppInfo createInstantAppInfoForPackage(PackageParser.Package r4, int i, boolean z) {
        PackageSetting packageSetting = (PackageSetting) r4.mExtras;
        if (packageSetting == null || !packageSetting.getInstalled(i)) {
            return null;
        }
        String[] strArr = new String[r4.requestedPermissions.size()];
        r4.requestedPermissions.toArray(strArr);
        Set<String> permissions = packageSetting.getPermissionsState().getPermissions(i);
        String[] strArr2 = new String[permissions.size()];
        permissions.toArray(strArr2);
        if (z) {
            return new InstantAppInfo(r4.applicationInfo, strArr, strArr2);
        }
        return new InstantAppInfo(r4.applicationInfo.packageName, r4.applicationInfo.loadLabel(this.mService.mContext.getPackageManager()), strArr, strArr2);
    }

    private List<InstantAppInfo> getUninstalledInstantApplicationsLPr(int i) {
        List<UninstalledInstantAppState> uninstalledInstantAppStatesLPr = getUninstalledInstantAppStatesLPr(i);
        ArrayList arrayList = null;
        if (uninstalledInstantAppStatesLPr == null || uninstalledInstantAppStatesLPr.isEmpty()) {
            return null;
        }
        int size = uninstalledInstantAppStatesLPr.size();
        for (int i2 = 0; i2 < size; i2++) {
            UninstalledInstantAppState uninstalledInstantAppState = uninstalledInstantAppStatesLPr.get(i2);
            if (arrayList == null) {
                arrayList = new ArrayList();
            }
            arrayList.add(uninstalledInstantAppState.mInstantAppInfo);
        }
        return arrayList;
    }

    private void propagateInstantAppPermissionsIfNeeded(PackageParser.Package r9, int i) {
        InstantAppInfo instantAppInfoPeekOrParseUninstalledInstantAppInfo = peekOrParseUninstalledInstantAppInfo(r9.packageName, i);
        if (instantAppInfoPeekOrParseUninstalledInstantAppInfo == null || ArrayUtils.isEmpty(instantAppInfoPeekOrParseUninstalledInstantAppInfo.getGrantedPermissions())) {
            return;
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            for (String str : instantAppInfoPeekOrParseUninstalledInstantAppInfo.getGrantedPermissions()) {
                if (this.mService.mSettings.canPropagatePermissionToInstantApp(str) && r9.requestedPermissions.contains(str)) {
                    this.mService.grantRuntimePermission(r9.packageName, str, i);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private InstantAppInfo peekOrParseUninstalledInstantAppInfo(String str, int i) {
        List<UninstalledInstantAppState> list;
        if (this.mUninstalledInstantApps != null && (list = this.mUninstalledInstantApps.get(i)) != null) {
            int size = list.size();
            for (int i2 = 0; i2 < size; i2++) {
                UninstalledInstantAppState uninstalledInstantAppState = list.get(i2);
                if (uninstalledInstantAppState.mInstantAppInfo.getPackageName().equals(str)) {
                    return uninstalledInstantAppState.mInstantAppInfo;
                }
            }
        }
        UninstalledInstantAppState metadataFile = parseMetadataFile(new File(getInstantApplicationDir(str, i), INSTANT_APP_METADATA_FILE));
        if (metadataFile == null) {
            return null;
        }
        return metadataFile.mInstantAppInfo;
    }

    private List<UninstalledInstantAppState> getUninstalledInstantAppStatesLPr(int i) {
        List<UninstalledInstantAppState> arrayList;
        File[] fileArrListFiles;
        UninstalledInstantAppState metadataFile;
        if (this.mUninstalledInstantApps != null) {
            arrayList = this.mUninstalledInstantApps.get(i);
            if (arrayList != null) {
                return arrayList;
            }
        } else {
            arrayList = null;
        }
        File instantApplicationsDir = getInstantApplicationsDir(i);
        if (instantApplicationsDir.exists() && (fileArrListFiles = instantApplicationsDir.listFiles()) != null) {
            for (File file : fileArrListFiles) {
                if (file.isDirectory() && (metadataFile = parseMetadataFile(new File(file, INSTANT_APP_METADATA_FILE))) != null) {
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                    }
                    arrayList.add(metadataFile);
                }
            }
        }
        if (arrayList != null) {
            if (this.mUninstalledInstantApps == null) {
                this.mUninstalledInstantApps = new SparseArray<>();
            }
            this.mUninstalledInstantApps.put(i, arrayList);
        }
        return arrayList;
    }

    private static UninstalledInstantAppState parseMetadataFile(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            FileInputStream fileInputStreamOpenRead = new AtomicFile(file).openRead();
            File parentFile = file.getParentFile();
            long jLastModified = file.lastModified();
            String name = parentFile.getName();
            try {
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    return new UninstalledInstantAppState(parseMetadata(xmlPullParserNewPullParser, name), jLastModified);
                } catch (IOException | XmlPullParserException e) {
                    throw new IllegalStateException("Failed parsing instant metadata file: " + file, e);
                }
            } finally {
                IoUtils.closeQuietly(fileInputStreamOpenRead);
            }
        } catch (FileNotFoundException e2) {
            Slog.i(LOG_TAG, "No instant metadata file");
            return null;
        }
    }

    private static File computeInstantCookieFile(String str, String str2, int i) {
        return new File(getInstantApplicationDir(str, i), INSTANT_APP_COOKIE_FILE_PREFIX + str2 + INSTANT_APP_COOKIE_FILE_SIFFIX);
    }

    private static File peekInstantCookieFile(String str, int i) {
        File[] fileArrListFiles;
        File instantApplicationDir = getInstantApplicationDir(str, i);
        if (!instantApplicationDir.exists() || (fileArrListFiles = instantApplicationDir.listFiles()) == null) {
            return null;
        }
        for (File file : fileArrListFiles) {
            if (!file.isDirectory() && file.getName().startsWith(INSTANT_APP_COOKIE_FILE_PREFIX) && file.getName().endsWith(INSTANT_APP_COOKIE_FILE_SIFFIX)) {
                return file;
            }
        }
        return null;
    }

    private static InstantAppInfo parseMetadata(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if ("package".equals(xmlPullParser.getName())) {
                return parsePackage(xmlPullParser, str);
            }
        }
        return null;
    }

    private static InstantAppInfo parsePackage(XmlPullParser xmlPullParser, String str) throws XmlPullParserException, IOException {
        String attributeValue = xmlPullParser.getAttributeValue(null, ATTR_LABEL);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (TAG_PERMISSIONS.equals(xmlPullParser.getName())) {
                parsePermissions(xmlPullParser, arrayList, arrayList2);
            }
        }
        String[] strArr = new String[arrayList.size()];
        arrayList.toArray(strArr);
        String[] strArr2 = new String[arrayList2.size()];
        arrayList2.toArray(strArr2);
        return new InstantAppInfo(str, attributeValue, strArr, strArr2);
    }

    private static void parsePermissions(XmlPullParser xmlPullParser, List<String> list, List<String> list2) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (XmlUtils.nextElementWithin(xmlPullParser, depth)) {
            if (TAG_PERMISSION.equals(xmlPullParser.getName())) {
                String stringAttribute = XmlUtils.readStringAttribute(xmlPullParser, "name");
                list.add(stringAttribute);
                if (XmlUtils.readBooleanAttribute(xmlPullParser, ATTR_GRANTED)) {
                    list2.add(stringAttribute);
                }
            }
        }
    }

    private void writeUninstalledInstantAppMetadata(InstantAppInfo instantAppInfo, int i) throws Throwable {
        FileOutputStream fileOutputStreamStartWrite;
        File instantApplicationDir = getInstantApplicationDir(instantAppInfo.getPackageName(), i);
        if (instantApplicationDir.exists() || instantApplicationDir.mkdirs()) {
            AtomicFile atomicFile = new AtomicFile(new File(instantApplicationDir, INSTANT_APP_METADATA_FILE));
            FileOutputStream fileOutputStream = null;
            try {
                try {
                    fileOutputStreamStartWrite = atomicFile.startWrite();
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Throwable th2) {
                th = th2;
                fileOutputStreamStartWrite = fileOutputStream;
            }
            try {
                XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
                xmlSerializerNewSerializer.setOutput(fileOutputStreamStartWrite, StandardCharsets.UTF_8.name());
                xmlSerializerNewSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                xmlSerializerNewSerializer.startDocument(null, true);
                xmlSerializerNewSerializer.startTag(null, "package");
                xmlSerializerNewSerializer.attribute(null, ATTR_LABEL, instantAppInfo.loadLabel(this.mService.mContext.getPackageManager()).toString());
                xmlSerializerNewSerializer.startTag(null, TAG_PERMISSIONS);
                for (String str : instantAppInfo.getRequestedPermissions()) {
                    xmlSerializerNewSerializer.startTag(null, TAG_PERMISSION);
                    xmlSerializerNewSerializer.attribute(null, "name", str);
                    if (ArrayUtils.contains(instantAppInfo.getGrantedPermissions(), str)) {
                        xmlSerializerNewSerializer.attribute(null, ATTR_GRANTED, String.valueOf(true));
                    }
                    xmlSerializerNewSerializer.endTag(null, TAG_PERMISSION);
                }
                xmlSerializerNewSerializer.endTag(null, TAG_PERMISSIONS);
                xmlSerializerNewSerializer.endTag(null, "package");
                xmlSerializerNewSerializer.endDocument();
                atomicFile.finishWrite(fileOutputStreamStartWrite);
                IoUtils.closeQuietly(fileOutputStreamStartWrite);
            } catch (Throwable th3) {
                th = th3;
                IoUtils.closeQuietly(fileOutputStreamStartWrite);
                throw th;
            }
        }
    }

    private static File getInstantApplicationsDir(int i) {
        return new File(Environment.getUserSystemDirectory(i), INSTANT_APPS_FOLDER);
    }

    private static File getInstantApplicationDir(String str, int i) {
        return new File(getInstantApplicationsDir(i), str);
    }

    private static void deleteDir(File file) {
        File[] fileArrListFiles = file.listFiles();
        if (fileArrListFiles != null) {
            for (File file2 : fileArrListFiles) {
                deleteDir(file2);
            }
        }
        file.delete();
    }

    private static final class UninstalledInstantAppState {
        final InstantAppInfo mInstantAppInfo;
        final long mTimestamp;

        public UninstalledInstantAppState(InstantAppInfo instantAppInfo, long j) {
            this.mInstantAppInfo = instantAppInfo;
            this.mTimestamp = j;
        }
    }

    private final class CookiePersistence extends Handler {
        private static final long PERSIST_COOKIE_DELAY_MILLIS = 1000;
        private final SparseArray<ArrayMap<PackageParser.Package, SomeArgs>> mPendingPersistCookies;

        public CookiePersistence(Looper looper) {
            super(looper);
            this.mPendingPersistCookies = new SparseArray<>();
        }

        public void schedulePersistLPw(int i, PackageParser.Package r5, byte[] bArr) {
            File fileComputeInstantCookieFile = InstantAppRegistry.computeInstantCookieFile(r5.packageName, PackageUtils.computeSignaturesSha256Digest(r5.mSigningDetails.signatures), i);
            if (!r5.mSigningDetails.hasSignatures()) {
                Slog.wtf(InstantAppRegistry.LOG_TAG, "Parsed Instant App contains no valid signatures!");
            }
            File filePeekInstantCookieFile = InstantAppRegistry.peekInstantCookieFile(r5.packageName, i);
            if (filePeekInstantCookieFile != null && !fileComputeInstantCookieFile.equals(filePeekInstantCookieFile)) {
                filePeekInstantCookieFile.delete();
            }
            cancelPendingPersistLPw(r5, i);
            addPendingPersistCookieLPw(i, r5, bArr, fileComputeInstantCookieFile);
            sendMessageDelayed(obtainMessage(i, r5), 1000L);
        }

        public byte[] getPendingPersistCookieLPr(PackageParser.Package r2, int i) {
            SomeArgs someArgs;
            ArrayMap<PackageParser.Package, SomeArgs> arrayMap = this.mPendingPersistCookies.get(i);
            if (arrayMap != null && (someArgs = arrayMap.get(r2)) != null) {
                return (byte[]) someArgs.arg1;
            }
            return null;
        }

        public void cancelPendingPersistLPw(PackageParser.Package r1, int i) {
            removeMessages(i, r1);
            SomeArgs someArgsRemovePendingPersistCookieLPr = removePendingPersistCookieLPr(r1, i);
            if (someArgsRemovePendingPersistCookieLPr != null) {
                someArgsRemovePendingPersistCookieLPr.recycle();
            }
        }

        private void addPendingPersistCookieLPw(int i, PackageParser.Package r4, byte[] bArr, File file) {
            ArrayMap<PackageParser.Package, SomeArgs> arrayMap = this.mPendingPersistCookies.get(i);
            if (arrayMap == null) {
                arrayMap = new ArrayMap<>();
                this.mPendingPersistCookies.put(i, arrayMap);
            }
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = bArr;
            someArgsObtain.arg2 = file;
            arrayMap.put(r4, someArgsObtain);
        }

        private SomeArgs removePendingPersistCookieLPr(PackageParser.Package r2, int i) {
            ArrayMap<PackageParser.Package, SomeArgs> arrayMap = this.mPendingPersistCookies.get(i);
            if (arrayMap != null) {
                SomeArgs someArgsRemove = arrayMap.remove(r2);
                if (!arrayMap.isEmpty()) {
                    return someArgsRemove;
                }
                this.mPendingPersistCookies.remove(i);
                return someArgsRemove;
            }
            return null;
        }

        @Override
        public void handleMessage(Message message) throws Exception {
            int i = message.what;
            PackageParser.Package r5 = (PackageParser.Package) message.obj;
            SomeArgs someArgsRemovePendingPersistCookieLPr = removePendingPersistCookieLPr(r5, i);
            if (someArgsRemovePendingPersistCookieLPr == null) {
                return;
            }
            byte[] bArr = (byte[]) someArgsRemovePendingPersistCookieLPr.arg1;
            File file = (File) someArgsRemovePendingPersistCookieLPr.arg2;
            someArgsRemovePendingPersistCookieLPr.recycle();
            InstantAppRegistry.this.persistInstantApplicationCookie(bArr, r5.packageName, file, i);
        }
    }
}
