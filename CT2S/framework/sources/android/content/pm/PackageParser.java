package android.content.pm;

import android.Manifest;
import android.accounts.GrantCredentialsPermissionActivity;
import android.app.ActivityManager;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.hardware.Camera;
import android.hardware.usb.UsbManager;
import android.net.ProxyInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.os.UserHandle;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.StrictJarFile;
import java.util.zip.ZipEntry;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import libcore.io.IoUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PackageParser {
    private static final String ANDROID_MANIFEST_FILENAME = "AndroidManifest.xml";
    private static final String ANDROID_RESOURCES = "http://schemas.android.com/apk/res/android";
    private static final boolean DEBUG_BACKUP = false;
    private static final boolean DEBUG_JAR = false;
    private static final boolean DEBUG_PARSER = false;
    public static final int PARSE_CHATTY = 2;
    public static final int PARSE_COLLECT_CERTIFICATES = 256;
    private static final int PARSE_DEFAULT_INSTALL_LOCATION = -1;
    public static final int PARSE_FORWARD_LOCK = 16;
    public static final int PARSE_IGNORE_PROCESSES = 8;
    public static final int PARSE_IS_PRIVILEGED = 128;
    public static final int PARSE_IS_SYSTEM = 1;
    public static final int PARSE_IS_SYSTEM_DIR = 64;
    public static final int PARSE_MUST_BE_APK = 4;
    public static final int PARSE_ON_SDCARD = 32;
    public static final int PARSE_TRUSTED_OVERLAY = 512;
    private static final boolean RIGID_PARSER = false;
    private static final String TAG = "PackageParser";
    private static ArrayList<String> mLargeHeapList;

    @Deprecated
    private String mArchiveSourcePath;
    private boolean mOnlyCoreApps;
    private ParseComponentArgs mParseActivityAliasArgs;
    private ParseComponentArgs mParseActivityArgs;
    private ParsePackageItemArgs mParseInstrumentationArgs;
    private ParseComponentArgs mParseProviderArgs;
    private ParseComponentArgs mParseServiceArgs;
    private String[] mSeparateProcesses;
    public static final NewPermissionInfo[] NEW_PERMISSIONS = {new NewPermissionInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, 4, 0), new NewPermissionInfo("android.permission.READ_PHONE_STATE", 4, 0)};
    public static final SplitPermissionInfo[] SPLIT_PERMISSIONS = {new SplitPermissionInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, HapticFeedbackConstants.SAFE_MODE_ENABLED), new SplitPermissionInfo(Manifest.permission.READ_CONTACTS, new String[]{Manifest.permission.READ_CALL_LOG}, 16), new SplitPermissionInfo(Manifest.permission.WRITE_CONTACTS, new String[]{Manifest.permission.WRITE_CALL_LOG}, 16)};
    private static final int SDK_VERSION = Build.VERSION.SDK_INT;
    private static final String[] SDK_CODENAMES = Build.VERSION.ACTIVE_CODENAMES;
    private static boolean sCompatibilityModeEnabled = true;
    private static final Comparator<String> sSplitNameComparator = new SplitNameComparator();
    private static AtomicReference<byte[]> sBuffer = new AtomicReference<>();
    private int mParseError = 1;
    private DisplayMetrics mMetrics = new DisplayMetrics();

    public static class IntentInfo extends IntentFilter {
        public int banner;
        public boolean hasDefault;
        public int icon;
        public int labelRes;
        public int logo;
        public CharSequence nonLocalizedLabel;
        public int preferred;
    }

    public static class NewPermissionInfo {
        public final int fileVersion;
        public final String name;
        public final int sdkVersion;

        public NewPermissionInfo(String name, int sdkVersion, int fileVersion) {
            this.name = name;
            this.sdkVersion = sdkVersion;
            this.fileVersion = fileVersion;
        }
    }

    public static class SplitPermissionInfo {
        public final String[] newPerms;
        public final String rootPerm;
        public final int targetSdk;

        public SplitPermissionInfo(String rootPerm, String[] newPerms, int targetSdk) {
            this.rootPerm = rootPerm;
            this.newPerms = newPerms;
            this.targetSdk = targetSdk;
        }
    }

    static {
        Reader reader;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            mLargeHeapList = new ArrayList<>();
            File xmlFile = new File("/system/etc/largeheaplist.xml");
            if (xmlFile.exists()) {
                Reader reader2 = null;
                try {
                    reader = new InputStreamReader(new FileInputStream(xmlFile), "UTF-8");
                } catch (Exception e) {
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    Document document = builder.parse(new InputSource(reader));
                    NodeList entries = document.getElementsByTagName("PackageName");
                    int length = entries.getLength();
                    for (int i = 0; i < length; i++) {
                        Node node = entries.item(i);
                        String value = node.getTextContent();
                        mLargeHeapList.add(value);
                        Slog.i(TAG, "One app[" + value + "] need large heap founded" + value);
                    }
                    IoUtils.closeQuietly(reader);
                } catch (Exception e2) {
                    reader2 = reader;
                    IoUtils.closeQuietly(reader2);
                } catch (Throwable th2) {
                    th = th2;
                    reader2 = reader;
                    IoUtils.closeQuietly(reader2);
                    throw th;
                }
            }
        } catch (ParserConfigurationException e3) {
            throw new Error(e3);
        }
    }

    static class ParsePackageItemArgs {
        final int bannerRes;
        final int iconRes;
        final int labelRes;
        final int logoRes;
        final int nameRes;
        final String[] outError;
        final Package owner;
        TypedArray sa;
        String tag;

        ParsePackageItemArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _logoRes, int _bannerRes) {
            this.owner = _owner;
            this.outError = _outError;
            this.nameRes = _nameRes;
            this.labelRes = _labelRes;
            this.iconRes = _iconRes;
            this.logoRes = _logoRes;
            this.bannerRes = _bannerRes;
        }
    }

    static class ParseComponentArgs extends ParsePackageItemArgs {
        final int descriptionRes;
        final int enabledRes;
        int flags;
        final int processRes;
        final String[] sepProcesses;

        ParseComponentArgs(Package _owner, String[] _outError, int _nameRes, int _labelRes, int _iconRes, int _logoRes, int _bannerRes, String[] _sepProcesses, int _processRes, int _descriptionRes, int _enabledRes) {
            super(_owner, _outError, _nameRes, _labelRes, _iconRes, _logoRes, _bannerRes);
            this.sepProcesses = _sepProcesses;
            this.processRes = _processRes;
            this.descriptionRes = _descriptionRes;
            this.enabledRes = _enabledRes;
        }
    }

    public static class PackageLite {
        public final String baseCodePath;
        public final int baseRevisionCode;
        public final String codePath;
        public final boolean coreApp;
        public final int installLocation;
        public final boolean multiArch;
        public final String packageName;
        public final String[] splitCodePaths;
        public final String[] splitNames;
        public final int[] splitRevisionCodes;
        public final VerifierInfo[] verifiers;
        public final int versionCode;

        public PackageLite(String codePath, ApkLite baseApk, String[] splitNames, String[] splitCodePaths, int[] splitRevisionCodes) {
            this.packageName = baseApk.packageName;
            this.versionCode = baseApk.versionCode;
            this.installLocation = baseApk.installLocation;
            this.verifiers = baseApk.verifiers;
            this.splitNames = splitNames;
            this.codePath = codePath;
            this.baseCodePath = baseApk.codePath;
            this.splitCodePaths = splitCodePaths;
            this.baseRevisionCode = baseApk.revisionCode;
            this.splitRevisionCodes = splitRevisionCodes;
            this.coreApp = baseApk.coreApp;
            this.multiArch = baseApk.multiArch;
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList<>();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }
    }

    public static class ApkLite {
        public final String codePath;
        public final boolean coreApp;
        public final int installLocation;
        public final boolean multiArch;
        public final String packageName;
        public final int revisionCode;
        public final Signature[] signatures;
        public final String splitName;
        public final VerifierInfo[] verifiers;
        public final int versionCode;

        public ApkLite(String codePath, String packageName, String splitName, int versionCode, int revisionCode, int installLocation, List<VerifierInfo> verifiers, Signature[] signatures, boolean coreApp, boolean multiArch) {
            this.codePath = codePath;
            this.packageName = packageName;
            this.splitName = splitName;
            this.versionCode = versionCode;
            this.revisionCode = revisionCode;
            this.installLocation = installLocation;
            this.verifiers = (VerifierInfo[]) verifiers.toArray(new VerifierInfo[verifiers.size()]);
            this.signatures = signatures;
            this.coreApp = coreApp;
            this.multiArch = multiArch;
        }
    }

    public PackageParser() {
        this.mMetrics.setToDefaults();
    }

    public void setSeparateProcesses(String[] procs) {
        this.mSeparateProcesses = procs;
    }

    public void setOnlyCoreApps(boolean onlyCoreApps) {
        this.mOnlyCoreApps = onlyCoreApps;
    }

    public void setDisplayMetrics(DisplayMetrics metrics) {
        this.mMetrics = metrics;
    }

    public static final boolean isApkFile(File file) {
        return isApkPath(file.getName());
    }

    private static boolean isApkPath(String path) {
        return path.endsWith(".apk");
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, ArraySet<String> grantedPermissions, PackageUserState state) {
        return generatePackageInfo(p, gids, flags, firstInstallTime, lastUpdateTime, grantedPermissions, state, UserHandle.getCallingUserId());
    }

    private static boolean checkUseInstalledOrHidden(int flags, PackageUserState state) {
        return (state.installed && !state.hidden) || (flags & 8192) != 0;
    }

    public static boolean isAvailable(PackageUserState state) {
        return checkUseInstalledOrHidden(0, state);
    }

    public static PackageInfo generatePackageInfo(Package p, int[] gids, int flags, long firstInstallTime, long lastUpdateTime, ArraySet<String> grantedPermissions, PackageUserState state, int userId) {
        int N;
        int N2;
        int j;
        int N3;
        int j2;
        int N4;
        int j3;
        int N5;
        int j4;
        if (!checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        PackageInfo pi = new PackageInfo();
        pi.packageName = p.packageName;
        pi.splitNames = p.splitNames;
        pi.versionCode = p.mVersionCode;
        pi.baseRevisionCode = p.baseRevisionCode;
        pi.splitRevisionCodes = p.splitRevisionCodes;
        pi.versionName = p.mVersionName;
        pi.sharedUserId = p.mSharedUserId;
        pi.sharedUserLabel = p.mSharedUserLabel;
        pi.applicationInfo = generateApplicationInfo(p, flags, state, userId);
        pi.installLocation = p.installLocation;
        pi.coreApp = p.coreApp;
        if ((pi.applicationInfo.flags & 1) != 0 || (pi.applicationInfo.flags & 128) != 0) {
            pi.requiredForAllUsers = p.mRequiredForAllUsers;
        }
        pi.restrictedAccountType = p.mRestrictedAccountType;
        pi.requiredAccountType = p.mRequiredAccountType;
        pi.overlayTarget = p.mOverlayTarget;
        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if ((flags & 256) != 0) {
            pi.gids = gids;
        }
        if ((flags & 16384) != 0) {
            int N6 = p.configPreferences != null ? p.configPreferences.size() : 0;
            if (N6 > 0) {
                pi.configPreferences = new ConfigurationInfo[N6];
                p.configPreferences.toArray(pi.configPreferences);
            }
            int N7 = p.reqFeatures != null ? p.reqFeatures.size() : 0;
            if (N7 > 0) {
                pi.reqFeatures = new FeatureInfo[N7];
                p.reqFeatures.toArray(pi.reqFeatures);
            }
            int N8 = p.featureGroups != null ? p.featureGroups.size() : 0;
            if (N8 > 0) {
                pi.featureGroups = new FeatureGroupInfo[N8];
                p.featureGroups.toArray(pi.featureGroups);
            }
        }
        if ((flags & 1) != 0 && (N5 = p.activities.size()) > 0) {
            if ((flags & 512) != 0) {
                pi.activities = new ActivityInfo[N5];
            } else {
                int num = 0;
                for (int i = 0; i < N5; i++) {
                    if (p.activities.get(i).info.enabled) {
                        num++;
                    }
                }
                pi.activities = new ActivityInfo[num];
            }
            int i2 = 0;
            int j5 = 0;
            while (i2 < N5) {
                Activity activity = p.activities.get(i2);
                if (activity.info.enabled || (flags & 512) != 0) {
                    j4 = j5 + 1;
                    pi.activities[j5] = generateActivityInfo(p.activities.get(i2), flags, state, userId);
                } else {
                    j4 = j5;
                }
                i2++;
                j5 = j4;
            }
        }
        if ((flags & 2) != 0 && (N4 = p.receivers.size()) > 0) {
            if ((flags & 512) != 0) {
                pi.receivers = new ActivityInfo[N4];
            } else {
                int num2 = 0;
                for (int i3 = 0; i3 < N4; i3++) {
                    if (p.receivers.get(i3).info.enabled) {
                        num2++;
                    }
                }
                pi.receivers = new ActivityInfo[num2];
            }
            int i4 = 0;
            int j6 = 0;
            while (i4 < N4) {
                Activity activity2 = p.receivers.get(i4);
                if (activity2.info.enabled || (flags & 512) != 0) {
                    j3 = j6 + 1;
                    pi.receivers[j6] = generateActivityInfo(p.receivers.get(i4), flags, state, userId);
                } else {
                    j3 = j6;
                }
                i4++;
                j6 = j3;
            }
        }
        if ((flags & 4) != 0 && (N3 = p.services.size()) > 0) {
            if ((flags & 512) != 0) {
                pi.services = new ServiceInfo[N3];
            } else {
                int num3 = 0;
                for (int i5 = 0; i5 < N3; i5++) {
                    if (p.services.get(i5).info.enabled) {
                        num3++;
                    }
                }
                pi.services = new ServiceInfo[num3];
            }
            int i6 = 0;
            int j7 = 0;
            while (i6 < N3) {
                Service service = p.services.get(i6);
                if (service.info.enabled || (flags & 512) != 0) {
                    j2 = j7 + 1;
                    pi.services[j7] = generateServiceInfo(p.services.get(i6), flags, state, userId);
                } else {
                    j2 = j7;
                }
                i6++;
                j7 = j2;
            }
        }
        if ((flags & 8) != 0 && (N2 = p.providers.size()) > 0) {
            if ((flags & 512) != 0) {
                pi.providers = new ProviderInfo[N2];
            } else {
                int num4 = 0;
                for (int i7 = 0; i7 < N2; i7++) {
                    if (p.providers.get(i7).info.enabled) {
                        num4++;
                    }
                }
                pi.providers = new ProviderInfo[num4];
            }
            int i8 = 0;
            int j8 = 0;
            while (i8 < N2) {
                Provider provider = p.providers.get(i8);
                if (provider.info.enabled || (flags & 512) != 0) {
                    j = j8 + 1;
                    pi.providers[j8] = generateProviderInfo(p.providers.get(i8), flags, state, userId);
                } else {
                    j = j8;
                }
                i8++;
                j8 = j;
            }
        }
        if ((flags & 16) != 0 && (N = p.instrumentation.size()) > 0) {
            pi.instrumentation = new InstrumentationInfo[N];
            for (int i9 = 0; i9 < N; i9++) {
                pi.instrumentation[i9] = generateInstrumentationInfo(p.instrumentation.get(i9), flags);
            }
        }
        if ((flags & 4096) != 0) {
            int N9 = p.permissions.size();
            if (N9 > 0) {
                pi.permissions = new PermissionInfo[N9];
                for (int i10 = 0; i10 < N9; i10++) {
                    pi.permissions[i10] = generatePermissionInfo(p.permissions.get(i10), flags);
                }
            }
            int N10 = p.requestedPermissions.size();
            if (N10 > 0) {
                pi.requestedPermissions = new String[N10];
                pi.requestedPermissionsFlags = new int[N10];
                for (int i11 = 0; i11 < N10; i11++) {
                    String perm = p.requestedPermissions.get(i11);
                    pi.requestedPermissions[i11] = perm;
                    if (p.requestedPermissionsRequired.get(i11).booleanValue()) {
                        int[] iArr = pi.requestedPermissionsFlags;
                        iArr[i11] = iArr[i11] | 1;
                    }
                    if (grantedPermissions != null && grantedPermissions.contains(perm)) {
                        int[] iArr2 = pi.requestedPermissionsFlags;
                        iArr2[i11] = iArr2[i11] | 2;
                    }
                }
            }
        }
        if ((flags & 64) != 0) {
            int N11 = p.mSignatures != null ? p.mSignatures.length : 0;
            if (N11 > 0) {
                pi.signatures = new Signature[N11];
                System.arraycopy(p.mSignatures, 0, pi.signatures, 0, N11);
                return pi;
            }
            return pi;
        }
        return pi;
    }

    private static Certificate[][] loadCertificates(StrictJarFile jarFile, ZipEntry entry) throws PackageParserException {
        Exception e;
        InputStream is = null;
        try {
            try {
                is = jarFile.getInputStream(entry);
                readFullyIgnoringContents(is);
                return jarFile.getCertificateChains(entry);
            } finally {
                IoUtils.closeQuietly(is);
            }
        } catch (IOException e2) {
            e = e2;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed reading " + entry.getName() + " in " + jarFile, e);
        } catch (RuntimeException e3) {
            e = e3;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed reading " + entry.getName() + " in " + jarFile, e);
        }
    }

    private static class SplitNameComparator implements Comparator<String> {
        private SplitNameComparator() {
        }

        @Override
        public int compare(String lhs, String rhs) {
            if (lhs == null) {
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.compareTo(rhs);
        }
    }

    public static PackageLite parsePackageLite(File packageFile, int flags) throws PackageParserException {
        return packageFile.isDirectory() ? parseClusterPackageLite(packageFile, flags) : parseMonolithicPackageLite(packageFile, flags);
    }

    private static PackageLite parseMonolithicPackageLite(File packageFile, int flags) throws Throwable {
        ApkLite baseApk = parseApkLite(packageFile, flags);
        String packagePath = packageFile.getAbsolutePath();
        return new PackageLite(packagePath, baseApk, null, null, null);
    }

    private static PackageLite parseClusterPackageLite(File packageDir, int flags) throws Throwable {
        File[] files = packageDir.listFiles();
        if (ArrayUtils.isEmpty(files)) {
            throw new PackageParserException(-100, "No packages found in split");
        }
        String packageName = null;
        int versionCode = 0;
        ArrayMap<String, ApkLite> apks = new ArrayMap<>();
        for (File file : files) {
            if (isApkFile(file)) {
                ApkLite lite = parseApkLite(file, flags);
                if (packageName == null) {
                    packageName = lite.packageName;
                    versionCode = lite.versionCode;
                } else {
                    if (!packageName.equals(lite.packageName)) {
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Inconsistent package " + lite.packageName + " in " + file + "; expected " + packageName);
                    }
                    if (versionCode != lite.versionCode) {
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Inconsistent version " + lite.versionCode + " in " + file + "; expected " + versionCode);
                    }
                }
                if (apks.put(lite.splitName, lite) != null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Split name " + lite.splitName + " defined more than once; most recent was " + file);
                }
            }
        }
        ApkLite baseApk = apks.remove(null);
        if (baseApk == null) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Missing base APK in " + packageDir);
        }
        int size = apks.size();
        String[] splitNames = null;
        String[] splitCodePaths = null;
        int[] splitRevisionCodes = null;
        if (size > 0) {
            String[] splitNames2 = new String[size];
            splitCodePaths = new String[size];
            splitRevisionCodes = new int[size];
            splitNames = (String[]) apks.keySet().toArray(splitNames2);
            Arrays.sort(splitNames, sSplitNameComparator);
            for (int i = 0; i < size; i++) {
                splitCodePaths[i] = apks.get(splitNames[i]).codePath;
                splitRevisionCodes[i] = apks.get(splitNames[i]).revisionCode;
            }
        }
        String codePath = packageDir.getAbsolutePath();
        return new PackageLite(codePath, baseApk, splitNames, splitCodePaths, splitRevisionCodes);
    }

    public Package parsePackage(File packageFile, int flags) throws PackageParserException {
        return packageFile.isDirectory() ? parseClusterPackage(packageFile, flags) : parseMonolithicPackage(packageFile, flags);
    }

    private Package parseClusterPackage(File packageDir, int flags) throws Throwable {
        PackageLite lite = parseClusterPackageLite(packageDir, 0);
        if (this.mOnlyCoreApps && !lite.coreApp) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + packageDir);
        }
        AssetManager assets = new AssetManager();
        try {
            loadApkIntoAssetManager(assets, lite.baseCodePath, flags);
            if (!ArrayUtils.isEmpty(lite.splitCodePaths)) {
                String[] arr$ = lite.splitCodePaths;
                for (String path : arr$) {
                    loadApkIntoAssetManager(assets, path, flags);
                }
            }
            File baseApk = new File(lite.baseCodePath);
            Package pkg = parseBaseApk(baseApk, assets, flags);
            if (pkg == null) {
                throw new PackageParserException(-100, "Failed to parse base APK: " + baseApk);
            }
            if (!ArrayUtils.isEmpty(lite.splitNames)) {
                int num = lite.splitNames.length;
                pkg.splitNames = lite.splitNames;
                pkg.splitCodePaths = lite.splitCodePaths;
                pkg.splitRevisionCodes = lite.splitRevisionCodes;
                pkg.splitFlags = new int[num];
                for (int i = 0; i < num; i++) {
                    parseSplitApk(pkg, i, assets, flags);
                }
            }
            pkg.codePath = packageDir.getAbsolutePath();
            return pkg;
        } finally {
            IoUtils.closeQuietly(assets);
        }
    }

    @Deprecated
    public Package parseMonolithicPackage(File apkFile, int flags) throws PackageParserException {
        if (this.mOnlyCoreApps) {
            PackageLite lite = parseMonolithicPackageLite(apkFile, flags);
            if (!lite.coreApp) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Not a coreApp: " + apkFile);
            }
        }
        AssetManager assets = new AssetManager();
        try {
            Package pkg = parseBaseApk(apkFile, assets, flags);
            pkg.codePath = apkFile.getAbsolutePath();
            return pkg;
        } finally {
            IoUtils.closeQuietly(assets);
        }
    }

    private static int loadApkIntoAssetManager(AssetManager assets, String apkPath, int flags) throws PackageParserException {
        if ((flags & 4) != 0 && !isApkPath(apkPath)) {
            throw new PackageParserException(-100, "Invalid package file: " + apkPath);
        }
        int cookie = assets.addAssetPath(apkPath);
        if (cookie == 0) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Failed adding asset path: " + apkPath);
        }
        return cookie;
    }

    private Package parseBaseApk(File apkFile, AssetManager assets, int flags) throws Throwable {
        Resources res;
        String apkPath = apkFile.getAbsolutePath();
        this.mParseError = 1;
        this.mArchiveSourcePath = apkFile.getAbsolutePath();
        int cookie = loadApkIntoAssetManager(assets, apkPath, flags);
        try {
            try {
                res = new Resources(assets, this.mMetrics, null);
            } catch (Throwable th) {
                th = th;
            }
        } catch (PackageParserException e) {
            throw e;
        } catch (Exception e2) {
            e = e2;
        }
        try {
            assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
            XmlResourceParser parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
            String[] outError = new String[1];
            Package pkg = parseBaseApk(res, parser, flags, outError);
            if (pkg == null) {
                throw new PackageParserException(this.mParseError, apkPath + " (at " + parser.getPositionDescription() + "): " + outError[0]);
            }
            pkg.baseCodePath = apkPath;
            pkg.mSignatures = null;
            IoUtils.closeQuietly(parser);
            return pkg;
        } catch (PackageParserException e3) {
            throw e3;
        } catch (Exception e4) {
            e = e4;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e);
        } catch (Throwable th2) {
            th = th2;
            IoUtils.closeQuietly((AutoCloseable) null);
            throw th;
        }
    }

    private void parseSplitApk(Package pkg, int splitIndex, AssetManager assets, int flags) throws Throwable {
        XmlResourceParser parser;
        Resources res;
        String apkPath = pkg.splitCodePaths[splitIndex];
        new File(apkPath);
        this.mParseError = 1;
        this.mArchiveSourcePath = apkPath;
        int cookie = loadApkIntoAssetManager(assets, apkPath, flags);
        try {
            try {
                res = new Resources(assets, this.mMetrics, null);
                try {
                    assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
                    parser = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
                } catch (PackageParserException e) {
                    e = e;
                } catch (Exception e2) {
                    e = e2;
                } catch (Throwable th) {
                    th = th;
                    parser = null;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (PackageParserException e3) {
            e = e3;
        } catch (Exception e4) {
            e = e4;
        } catch (Throwable th3) {
            th = th3;
            parser = null;
        }
        try {
            String[] outError = new String[1];
            if (parseSplitApk(pkg, res, parser, flags, splitIndex, outError) == null) {
                throw new PackageParserException(this.mParseError, apkPath + " (at " + parser.getPositionDescription() + "): " + outError[0]);
            }
            IoUtils.closeQuietly(parser);
        } catch (PackageParserException e5) {
            e = e5;
            throw e;
        } catch (Exception e6) {
            e = e6;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to read manifest from " + apkPath, e);
        } catch (Throwable th4) {
            th = th4;
            IoUtils.closeQuietly(parser);
            throw th;
        }
    }

    private Package parseSplitApk(Package pkg, Resources res, XmlResourceParser parser, int flags, int splitIndex, String[] outError) throws XmlPullParserException, PackageParserException, IOException {
        parsePackageSplitNames(parser, parser, flags);
        this.mParseInstrumentationArgs = null;
        this.mParseActivityArgs = null;
        this.mParseServiceArgs = null;
        this.mParseProviderArgs = null;
        boolean foundApp = false;
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(GrantCredentialsPermissionActivity.EXTRAS_PACKAGES)) {
                    if (foundApp) {
                        Slog.w(TAG, "<manifest> has more than one <application>");
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        foundApp = true;
                        if (!parseSplitApplication(pkg, res, parser, parser, flags, splitIndex, outError)) {
                            return null;
                        }
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <manifest>: " + parser.getName() + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    public void collectManifestDigest(Package pkg) throws PackageParserException {
        pkg.manifestDigest = null;
        try {
            StrictJarFile jarFile = new StrictJarFile(pkg.baseCodePath);
            try {
                ZipEntry je = jarFile.findEntry(ANDROID_MANIFEST_FILENAME);
                if (je != null) {
                    pkg.manifestDigest = ManifestDigest.fromInputStream(jarFile.getInputStream(je));
                }
            } finally {
                jarFile.close();
            }
        } catch (IOException | RuntimeException e) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "Failed to collect manifest digest");
        }
    }

    public void collectCertificates(Package pkg, int flags) throws PackageParserException {
        pkg.mCertificates = (Certificate[][]) null;
        pkg.mSignatures = null;
        pkg.mSigningKeys = null;
        collectCertificates(pkg, new File(pkg.baseCodePath), flags);
        if (!ArrayUtils.isEmpty(pkg.splitCodePaths)) {
            String[] arr$ = pkg.splitCodePaths;
            for (String splitCodePath : arr$) {
                collectCertificates(pkg, new File(splitCodePath), flags);
            }
        }
    }

    private static void collectCertificates(Package pkg, File apkFile, int flags) throws Throwable {
        Exception e;
        StrictJarFile jarFile;
        String apkPath = apkFile.getAbsolutePath();
        StrictJarFile jarFile2 = null;
        try {
            try {
                jarFile = new StrictJarFile(apkPath);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e2) {
            e = e2;
        } catch (RuntimeException e3) {
            e = e3;
        } catch (GeneralSecurityException e4) {
            e = e4;
        }
        try {
            ZipEntry manifestEntry = jarFile.findEntry(ANDROID_MANIFEST_FILENAME);
            if (manifestEntry == null) {
                throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST, "Package " + apkPath + " has no manifest");
            }
            List<ZipEntry> toVerify = new ArrayList<>();
            toVerify.add(manifestEntry);
            if ((flags & 1) == 0) {
                for (ZipEntry entry : jarFile) {
                    if (!entry.isDirectory() && !entry.getName().startsWith("META-INF/") && !entry.getName().equals(ANDROID_MANIFEST_FILENAME)) {
                        toVerify.add(entry);
                    }
                }
            }
            for (ZipEntry entry2 : toVerify) {
                Certificate[][] entryCerts = loadCertificates(jarFile, entry2);
                if (ArrayUtils.isEmpty(entryCerts)) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Package " + apkPath + " has no certificates at entry " + entry2.getName());
                }
                Signature[] entrySignatures = convertToSignatures(entryCerts);
                if (pkg.mCertificates == null) {
                    pkg.mCertificates = entryCerts;
                    pkg.mSignatures = entrySignatures;
                    pkg.mSigningKeys = new ArraySet<>();
                    for (Certificate[] certificateArr : entryCerts) {
                        pkg.mSigningKeys.add(certificateArr[0].getPublicKey());
                    }
                } else if (!Signature.areExactMatch(pkg.mSignatures, entrySignatures)) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES, "Package " + apkPath + " has mismatched certificates at entry " + entry2.getName());
                }
            }
            closeQuietly(jarFile);
        } catch (IOException e5) {
            e = e5;
            e = e;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + apkPath, e);
        } catch (RuntimeException e6) {
            e = e6;
            e = e;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES, "Failed to collect certificates from " + apkPath, e);
        } catch (GeneralSecurityException e7) {
            e = e7;
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING, "Failed to collect certificates from " + apkPath, e);
        } catch (Throwable th2) {
            th = th2;
            jarFile2 = jarFile;
            closeQuietly(jarFile2);
            throw th;
        }
    }

    private static Signature[] convertToSignatures(Certificate[][] certs) throws CertificateEncodingException {
        Signature[] res = new Signature[certs.length];
        for (int i = 0; i < certs.length; i++) {
            res[i] = new Signature(certs[i]);
        }
        return res;
    }

    public static ApkLite parseApkLite(File apkFile, int flags) throws Throwable {
        XmlResourceParser parser;
        AssetManager assets;
        Exception e;
        Signature[] signatures;
        String apkPath = apkFile.getAbsolutePath();
        try {
            try {
                assets = new AssetManager();
            } catch (Throwable th) {
                th = th;
            }
            try {
                assets.setConfiguration(0, 0, null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Build.VERSION.RESOURCES_SDK_INT);
                int cookie = assets.addAssetPath(apkPath);
                if (cookie == 0) {
                    throw new PackageParserException(-100, "Failed to parse " + apkPath);
                }
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.setToDefaults();
                Resources res = new Resources(assets, metrics, null);
                XmlResourceParser parser2 = assets.openXmlResourceParser(cookie, ANDROID_MANIFEST_FILENAME);
                if ((flags & 256) != 0) {
                    try {
                        Package tempPkg = new Package(null);
                        collectCertificates(tempPkg, apkFile, 0);
                        signatures = tempPkg.mSignatures;
                    } catch (IOException e2) {
                        e = e2;
                        e = e;
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to parse " + apkPath, e);
                    } catch (RuntimeException e3) {
                        e = e3;
                        e = e;
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to parse " + apkPath, e);
                    } catch (XmlPullParserException e4) {
                        e = e4;
                        e = e;
                        throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION, "Failed to parse " + apkPath, e);
                    }
                } else {
                    signatures = null;
                }
                ApkLite apkLite = parseApkLite(apkPath, res, parser2, parser2, flags, signatures);
                IoUtils.closeQuietly(parser2);
                IoUtils.closeQuietly(assets);
                return apkLite;
            } catch (IOException e5) {
                e = e5;
            } catch (RuntimeException e6) {
                e = e6;
            } catch (XmlPullParserException e7) {
                e = e7;
            } catch (Throwable th2) {
                th = th2;
                parser = null;
                IoUtils.closeQuietly(parser);
                IoUtils.closeQuietly(assets);
                throw th;
            }
        } catch (IOException e8) {
            e = e8;
        } catch (RuntimeException e9) {
            e = e9;
        } catch (XmlPullParserException e10) {
            e = e10;
        } catch (Throwable th3) {
            th = th3;
            parser = null;
            assets = null;
        }
    }

    private static String validateName(String name, boolean requiresSeparator) {
        int N = name.length();
        boolean hasSep = false;
        boolean front = true;
        for (int i = 0; i < N; i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
            } else if (front || ((c < '0' || c > '9') && c != '_')) {
                if (c == '.') {
                    hasSep = true;
                    front = true;
                } else {
                    return "bad character '" + c + "'";
                }
            }
        }
        if (hasSep || !requiresSeparator) {
            return null;
        }
        return "must have at least one '.' separator";
    }

    private static Pair<String, String> parsePackageSplitNames(XmlPullParser parser, AttributeSet attrs, int flags) throws XmlPullParserException, PackageParserException, IOException {
        int type;
        String error;
        do {
            type = parser.next();
            if (type == 2) {
                break;
            }
        } while (type != 1);
        if (type != 2) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No start tag found");
        }
        if (!parser.getName().equals("manifest")) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED, "No <manifest> tag");
        }
        String packageName = attrs.getAttributeValue(null, "package");
        if (!ZenModeConfig.SYSTEM_AUTHORITY.equals(packageName) && (error = validateName(packageName, true)) != null) {
            throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest package: " + error);
        }
        String splitName = attrs.getAttributeValue(null, "split");
        if (splitName != null) {
            if (splitName.length() == 0) {
                splitName = null;
            } else {
                String error2 = validateName(splitName, false);
                if (error2 != null) {
                    throw new PackageParserException(PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME, "Invalid manifest split: " + error2);
                }
            }
        }
        String strIntern = packageName.intern();
        if (splitName != null) {
            splitName = splitName.intern();
        }
        return Pair.create(strIntern, splitName);
    }

    private static ApkLite parseApkLite(String codePath, Resources res, XmlPullParser parser, AttributeSet attrs, int flags, Signature[] signatures) throws XmlPullParserException, PackageParserException, IOException {
        VerifierInfo verifier;
        Pair<String, String> packageSplit = parsePackageSplitNames(parser, attrs, flags);
        int installLocation = -1;
        int versionCode = 0;
        int revisionCode = 0;
        boolean coreApp = false;
        boolean multiArch = false;
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            String attr = attrs.getAttributeName(i);
            if (attr.equals("installLocation")) {
                installLocation = attrs.getAttributeIntValue(i, -1);
            } else if (attr.equals("versionCode")) {
                versionCode = attrs.getAttributeIntValue(i, 0);
            } else if (attr.equals("revisionCode")) {
                revisionCode = attrs.getAttributeIntValue(i, 0);
            } else if (attr.equals("coreApp")) {
                coreApp = attrs.getAttributeBooleanValue(i, false);
            }
        }
        int searchDepth = parser.getDepth() + 1;
        List<VerifierInfo> verifiers = new ArrayList<>();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() < searchDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                if (parser.getDepth() == searchDepth && "package-verifier".equals(parser.getName()) && (verifier = parseVerifier(res, parser, attrs, flags)) != null) {
                    verifiers.add(verifier);
                }
                if (parser.getDepth() == searchDepth && GrantCredentialsPermissionActivity.EXTRAS_PACKAGES.equals(parser.getName())) {
                    int i2 = 0;
                    while (true) {
                        if (i2 >= attrs.getAttributeCount()) {
                            break;
                        }
                        if (!"multiArch".equals(attrs.getAttributeName(i2))) {
                            i2++;
                        } else {
                            multiArch = attrs.getAttributeBooleanValue(i2, false);
                            break;
                        }
                    }
                }
            }
        }
        return new ApkLite(codePath, packageSplit.first, packageSplit.second, versionCode, revisionCode, installLocation, verifiers, signatures, coreApp, multiArch);
    }

    public static Signature stringToSignature(String str) {
        int N = str.length();
        byte[] sig = new byte[N];
        for (int i = 0; i < N; i++) {
            sig[i] = (byte) str.charAt(i);
        }
        return new Signature(sig);
    }

    private Package parseBaseApk(Resources res, XmlResourceParser parser, int flags, String[] outError) throws XmlPullParserException, IOException {
        boolean trustedOverlay = (flags & 512) != 0;
        this.mParseInstrumentationArgs = null;
        this.mParseActivityArgs = null;
        this.mParseServiceArgs = null;
        this.mParseProviderArgs = null;
        try {
            Pair<String, String> packageSplit = parsePackageSplitNames(parser, parser, flags);
            String pkgName = packageSplit.first;
            String splitName = packageSplit.second;
            if (!TextUtils.isEmpty(splitName)) {
                outError[0] = "Expected base APK, but found split " + splitName;
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
                return null;
            }
            Package pkg = new Package(pkgName);
            boolean foundApp = false;
            TypedArray sa = res.obtainAttributes(parser, R.styleable.AndroidManifest);
            ApplicationInfo applicationInfo = pkg.applicationInfo;
            int integer = sa.getInteger(1, 0);
            applicationInfo.versionCode = integer;
            pkg.mVersionCode = integer;
            pkg.baseRevisionCode = sa.getInteger(5, 0);
            pkg.mVersionName = sa.getNonConfigurationString(2, 0);
            if (pkg.mVersionName != null) {
                pkg.mVersionName = pkg.mVersionName.intern();
            }
            String str = sa.getNonConfigurationString(0, 0);
            if (str != null && str.length() > 0) {
                String nameError = validateName(str, true);
                if (nameError != null && !ZenModeConfig.SYSTEM_AUTHORITY.equals(pkgName)) {
                    outError[0] = "<manifest> specifies bad sharedUserId name \"" + str + "\": " + nameError;
                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
                    return null;
                }
                pkg.mSharedUserId = str.intern();
                pkg.mSharedUserLabel = sa.getResourceId(3, 0);
            }
            pkg.installLocation = sa.getInteger(4, -1);
            pkg.applicationInfo.installLocation = pkg.installLocation;
            pkg.coreApp = parser.getAttributeBooleanValue(null, "coreApp", false);
            sa.recycle();
            if ((flags & 16) != 0) {
                pkg.applicationInfo.flags |= 536870912;
            }
            if ((flags & 32) != 0) {
                pkg.applicationInfo.flags |= 262144;
            }
            int supportsSmallScreens = 1;
            int supportsNormalScreens = 1;
            int supportsLargeScreens = 1;
            int supportsXLargeScreens = 1;
            int resizeable = 1;
            int anyDensity = 1;
            int outerDepth = parser.getDepth();
            while (true) {
                int type = parser.next();
                if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                    break;
                }
                if (type != 3 && type != 4) {
                    String tagName = parser.getName();
                    if (tagName.equals(GrantCredentialsPermissionActivity.EXTRAS_PACKAGES)) {
                        if (foundApp) {
                            Slog.w(TAG, "<manifest> has more than one <application>");
                            XmlUtils.skipCurrentTag(parser);
                        } else {
                            foundApp = true;
                            if (!parseBaseApplication(pkg, res, parser, parser, flags, outError)) {
                                return null;
                            }
                        }
                    } else if (tagName.equals("overlay")) {
                        pkg.mTrustedOverlay = trustedOverlay;
                        TypedArray sa2 = res.obtainAttributes(parser, R.styleable.AndroidManifestResourceOverlay);
                        pkg.mOverlayTarget = sa2.getString(1);
                        pkg.mOverlayPriority = sa2.getInt(0, -1);
                        sa2.recycle();
                        if (pkg.mOverlayTarget == null) {
                            outError[0] = "<overlay> does not specify a target package";
                            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                            return null;
                        }
                        if (pkg.mOverlayPriority < 0 || pkg.mOverlayPriority > 9999) {
                            break;
                        }
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("key-sets")) {
                        if (!parseKeySets(pkg, res, parser, parser, outError)) {
                            return null;
                        }
                    } else if (tagName.equals("permission-group")) {
                        if (parsePermissionGroup(pkg, flags, res, parser, parser, outError) == null) {
                            return null;
                        }
                    } else if (tagName.equals(UsbManager.EXTRA_PERMISSION_GRANTED)) {
                        if (parsePermission(pkg, res, parser, parser, outError) == null) {
                            return null;
                        }
                    } else if (tagName.equals("permission-tree")) {
                        if (parsePermissionTree(pkg, res, parser, parser, outError) == null) {
                            return null;
                        }
                    } else if (tagName.equals("uses-permission")) {
                        if (!parseUsesPermission(pkg, res, parser, parser, outError)) {
                            return null;
                        }
                    } else if (tagName.equals("uses-configuration")) {
                        ConfigurationInfo cPref = new ConfigurationInfo();
                        TypedArray sa3 = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesConfiguration);
                        cPref.reqTouchScreen = sa3.getInt(0, 0);
                        cPref.reqKeyboardType = sa3.getInt(1, 0);
                        if (sa3.getBoolean(2, false)) {
                            cPref.reqInputFeatures |= 1;
                        }
                        cPref.reqNavigation = sa3.getInt(3, 0);
                        if (sa3.getBoolean(4, false)) {
                            cPref.reqInputFeatures |= 2;
                        }
                        sa3.recycle();
                        pkg.configPreferences = ArrayUtils.add(pkg.configPreferences, cPref);
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("uses-feature")) {
                        FeatureInfo fi = parseUsesFeature(res, parser);
                        pkg.reqFeatures = ArrayUtils.add(pkg.reqFeatures, fi);
                        if (fi.name == null) {
                            ConfigurationInfo cPref2 = new ConfigurationInfo();
                            cPref2.reqGlEsVersion = fi.reqGlEsVersion;
                            pkg.configPreferences = ArrayUtils.add(pkg.configPreferences, cPref2);
                        }
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("feature-group")) {
                        FeatureGroupInfo group = new FeatureGroupInfo();
                        ArrayList<FeatureInfo> features = null;
                        int innerDepth = parser.getDepth();
                        while (true) {
                            int type2 = parser.next();
                            if (type2 == 1 || (type2 == 3 && parser.getDepth() <= innerDepth)) {
                                break;
                            }
                            if (type2 != 3 && type2 != 4) {
                                String innerTagName = parser.getName();
                                if (innerTagName.equals("uses-feature")) {
                                    FeatureInfo featureInfo = parseUsesFeature(res, parser);
                                    featureInfo.flags |= 1;
                                    features = ArrayUtils.add(features, featureInfo);
                                } else {
                                    Slog.w(TAG, "Unknown element under <feature-group>: " + innerTagName + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                                }
                                XmlUtils.skipCurrentTag(parser);
                            }
                        }
                        if (features != null) {
                            group.features = new FeatureInfo[features.size()];
                            group.features = (FeatureInfo[]) features.toArray(group.features);
                        }
                        pkg.featureGroups = ArrayUtils.add(pkg.featureGroups, group);
                    } else if (tagName.equals("uses-sdk")) {
                        if (SDK_VERSION > 0) {
                            TypedArray sa4 = res.obtainAttributes(parser, R.styleable.AndroidManifestUsesSdk);
                            int minVers = 0;
                            String minCode = null;
                            int targetVers = 0;
                            String targetCode = null;
                            TypedValue val = sa4.peekValue(0);
                            if (val != null) {
                                if (val.type == 3 && val.string != null) {
                                    minCode = val.string.toString();
                                    targetCode = minCode;
                                } else {
                                    minVers = val.data;
                                    targetVers = minVers;
                                }
                            }
                            TypedValue val2 = sa4.peekValue(1);
                            if (val2 != null) {
                                if (val2.type == 3 && val2.string != null) {
                                    minCode = val2.string.toString();
                                    targetCode = minCode;
                                } else {
                                    targetVers = val2.data;
                                }
                            }
                            sa4.recycle();
                            if (minCode != null) {
                                boolean allowedCodename = false;
                                String[] arr$ = SDK_CODENAMES;
                                int len$ = arr$.length;
                                int i$ = 0;
                                while (true) {
                                    if (i$ >= len$) {
                                        break;
                                    }
                                    String codename = arr$[i$];
                                    if (!minCode.equals(codename)) {
                                        i$++;
                                    } else {
                                        allowedCodename = true;
                                        break;
                                    }
                                }
                                if (!allowedCodename) {
                                    if (SDK_CODENAMES.length > 0) {
                                        outError[0] = "Requires development platform " + minCode + " (current platform is any of " + Arrays.toString(SDK_CODENAMES) + ")";
                                    } else {
                                        outError[0] = "Requires development platform " + minCode + " but this is a release platform.";
                                    }
                                    this.mParseError = -12;
                                    return null;
                                }
                            } else if (minVers > SDK_VERSION) {
                                outError[0] = "Requires newer sdk version #" + minVers + " (current version is #" + SDK_VERSION + ")";
                                this.mParseError = -12;
                                return null;
                            }
                            if (targetCode != null) {
                                boolean allowedCodename2 = false;
                                String[] arr$2 = SDK_CODENAMES;
                                int len$2 = arr$2.length;
                                int i$2 = 0;
                                while (true) {
                                    if (i$2 >= len$2) {
                                        break;
                                    }
                                    String codename2 = arr$2[i$2];
                                    if (!targetCode.equals(codename2)) {
                                        i$2++;
                                    } else {
                                        allowedCodename2 = true;
                                        break;
                                    }
                                }
                                if (!allowedCodename2) {
                                    if (SDK_CODENAMES.length > 0) {
                                        outError[0] = "Requires development platform " + targetCode + " (current platform is any of " + Arrays.toString(SDK_CODENAMES) + ")";
                                    } else {
                                        outError[0] = "Requires development platform " + targetCode + " but this is a release platform.";
                                    }
                                    this.mParseError = -12;
                                    return null;
                                }
                                pkg.applicationInfo.targetSdkVersion = 10000;
                            } else {
                                pkg.applicationInfo.targetSdkVersion = targetVers;
                            }
                        }
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("supports-screens")) {
                        TypedArray sa5 = res.obtainAttributes(parser, R.styleable.AndroidManifestSupportsScreens);
                        pkg.applicationInfo.requiresSmallestWidthDp = sa5.getInteger(6, 0);
                        pkg.applicationInfo.compatibleWidthLimitDp = sa5.getInteger(7, 0);
                        pkg.applicationInfo.largestWidthLimitDp = sa5.getInteger(8, 0);
                        supportsSmallScreens = sa5.getInteger(1, supportsSmallScreens);
                        supportsNormalScreens = sa5.getInteger(2, supportsNormalScreens);
                        supportsLargeScreens = sa5.getInteger(3, supportsLargeScreens);
                        supportsXLargeScreens = sa5.getInteger(5, supportsXLargeScreens);
                        resizeable = sa5.getInteger(4, resizeable);
                        anyDensity = sa5.getInteger(0, anyDensity);
                        sa5.recycle();
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("protected-broadcast")) {
                        TypedArray sa6 = res.obtainAttributes(parser, R.styleable.AndroidManifestProtectedBroadcast);
                        String name = sa6.getNonResourceString(0);
                        sa6.recycle();
                        if (name != null && (flags & 1) != 0) {
                            if (pkg.protectedBroadcasts == null) {
                                pkg.protectedBroadcasts = new ArrayList<>();
                            }
                            if (!pkg.protectedBroadcasts.contains(name)) {
                                pkg.protectedBroadcasts.add(name.intern());
                            }
                        }
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("instrumentation")) {
                        if (parseInstrumentation(pkg, res, parser, parser, outError) == null) {
                            return null;
                        }
                    } else if (tagName.equals("original-package")) {
                        TypedArray sa7 = res.obtainAttributes(parser, R.styleable.AndroidManifestOriginalPackage);
                        String orig = sa7.getNonConfigurationString(0, 0);
                        if (!pkg.packageName.equals(orig)) {
                            if (pkg.mOriginalPackages == null) {
                                pkg.mOriginalPackages = new ArrayList<>();
                                pkg.mRealPackage = pkg.packageName;
                            }
                            pkg.mOriginalPackages.add(orig);
                        }
                        sa7.recycle();
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("adopt-permissions")) {
                        TypedArray sa8 = res.obtainAttributes(parser, R.styleable.AndroidManifestOriginalPackage);
                        String name2 = sa8.getNonConfigurationString(0, 0);
                        sa8.recycle();
                        if (name2 != null) {
                            if (pkg.mAdoptPermissions == null) {
                                pkg.mAdoptPermissions = new ArrayList<>();
                            }
                            pkg.mAdoptPermissions.add(name2);
                        }
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("uses-gl-texture")) {
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("compatible-screens")) {
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("supports-input")) {
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("eat-comment")) {
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        Slog.w(TAG, "Unknown element under <manifest>: " + parser.getName() + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        } catch (PackageParserException e) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
            return null;
        }
    }

    private FeatureInfo parseUsesFeature(Resources res, AttributeSet attrs) throws XmlPullParserException, IOException {
        FeatureInfo fi = new FeatureInfo();
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesFeature);
        fi.name = sa.getNonResourceString(0);
        if (fi.name == null) {
            fi.reqGlEsVersion = sa.getInt(1, 0);
        }
        if (sa.getBoolean(2, true)) {
            fi.flags |= 1;
        }
        sa.recycle();
        return fi;
    }

    private boolean parseUsesPermission(Package pkg, Resources res, XmlResourceParser parser, AttributeSet attrs, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesPermission);
        String name = sa.getNonResourceString(0);
        int maxSdkVersion = 0;
        TypedValue val = sa.peekValue(1);
        if (val != null && val.type >= 16 && val.type <= 31) {
            maxSdkVersion = val.data;
        }
        sa.recycle();
        if ((maxSdkVersion == 0 || maxSdkVersion >= Build.VERSION.RESOURCES_SDK_INT) && name != null) {
            int index = pkg.requestedPermissions.indexOf(name);
            if (index != -1) {
                if (!pkg.requestedPermissionsRequired.get(index).booleanValue()) {
                    outError[0] = "conflicting <uses-permission> entries";
                    this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                    return false;
                }
            } else {
                pkg.requestedPermissions.add(name.intern());
                pkg.requestedPermissionsRequired.add(1 != 0 ? Boolean.TRUE : Boolean.FALSE);
            }
        }
        XmlUtils.skipCurrentTag(parser);
        return true;
    }

    private static String buildClassName(String pkg, CharSequence clsSeq, String[] outError) {
        if (clsSeq == null || clsSeq.length() <= 0) {
            outError[0] = "Empty class name in package " + pkg;
            return null;
        }
        String cls = clsSeq.toString();
        char c = cls.charAt(0);
        if (c == '.') {
            return (pkg + cls).intern();
        }
        if (cls.indexOf(46) < 0) {
            return (pkg + '.' + cls).intern();
        }
        if (c >= 'a' && c <= 'z') {
            return cls.intern();
        }
        outError[0] = "Bad class name " + cls + " in package " + pkg;
        return null;
    }

    private static String buildCompoundName(String pkg, CharSequence procSeq, String type, String[] outError) {
        String proc = procSeq.toString();
        char c = proc.charAt(0);
        if (pkg != null && c == ':') {
            if (proc.length() < 2) {
                outError[0] = "Bad " + type + " name " + proc + " in package " + pkg + ": must be at least two characters";
                return null;
            }
            String subName = proc.substring(1);
            String nameError = validateName(subName, false);
            if (nameError != null) {
                outError[0] = "Invalid " + type + " name " + proc + " in package " + pkg + ": " + nameError;
                return null;
            }
            return (pkg + proc).intern();
        }
        String nameError2 = validateName(proc, true);
        if (nameError2 != null && !"system".equals(proc)) {
            outError[0] = "Invalid " + type + " name " + proc + " in package " + pkg + ": " + nameError2;
            return null;
        }
        return proc.intern();
    }

    private static String buildProcessName(String pkg, String defProc, CharSequence procSeq, int flags, String[] separateProcesses, String[] outError) {
        if ((flags & 8) != 0 && !"system".equals(procSeq)) {
            return defProc != null ? defProc : pkg;
        }
        if (separateProcesses != null) {
            for (int i = separateProcesses.length - 1; i >= 0; i--) {
                String sp = separateProcesses[i];
                if (sp.equals(pkg) || sp.equals(defProc) || sp.equals(procSeq)) {
                    return pkg;
                }
            }
        }
        return (procSeq == null || procSeq.length() <= 0) ? defProc : buildCompoundName(pkg, procSeq, "process", outError);
    }

    private static String buildTaskAffinityName(String pkg, String defProc, CharSequence procSeq, String[] outError) {
        if (procSeq != null) {
            if (procSeq.length() <= 0) {
                return null;
            }
            String defProc2 = buildCompoundName(pkg, procSeq, "taskAffinity", outError);
            return defProc2;
        }
        return defProc;
    }

    private boolean parseKeySets(Package owner, Resources res, XmlPullParser parser, AttributeSet attrs, String[] outError) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        int currentKeySetDepth = -1;
        String currentKeySet = null;
        ArrayMap<String, PublicKey> publicKeys = new ArrayMap<>();
        ArraySet<String> upgradeKeySets = new ArraySet<>();
        ArrayMap<String, ArraySet<String>> definedKeySets = new ArrayMap<>();
        ArraySet<String> improperKeySets = new ArraySet<>();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type == 3) {
                if (parser.getDepth() == currentKeySetDepth) {
                    currentKeySet = null;
                    currentKeySetDepth = -1;
                }
            } else {
                String tagName = parser.getName();
                if (tagName.equals("key-set")) {
                    if (currentKeySet != null) {
                        Slog.w(TAG, "Improperly nested 'key-set' tag at " + parser.getPositionDescription());
                        return false;
                    }
                    TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestKeySet);
                    String keysetName = sa.getNonResourceString(0);
                    definedKeySets.put(keysetName, new ArraySet<>());
                    currentKeySet = keysetName;
                    currentKeySetDepth = parser.getDepth();
                    sa.recycle();
                } else if (tagName.equals("public-key")) {
                    if (currentKeySet == null) {
                        Slog.w(TAG, "Improperly nested 'public-key' tag at " + parser.getPositionDescription());
                        return false;
                    }
                    TypedArray sa2 = res.obtainAttributes(attrs, R.styleable.AndroidManifestPublicKey);
                    String publicKeyName = sa2.getNonResourceString(0);
                    String encodedKey = sa2.getNonResourceString(1);
                    if (encodedKey == null && publicKeys.get(publicKeyName) == null) {
                        Slog.w(TAG, "'public-key' " + publicKeyName + " must define a public-key value on first use at " + parser.getPositionDescription());
                        sa2.recycle();
                        return false;
                    }
                    if (encodedKey != null) {
                        PublicKey currentKey = parsePublicKey(encodedKey);
                        if (currentKey == null) {
                            Slog.w(TAG, "No recognized valid key in 'public-key' tag at " + parser.getPositionDescription() + " key-set " + currentKeySet + " will not be added to the package's defined key-sets.");
                            sa2.recycle();
                            improperKeySets.add(currentKeySet);
                            XmlUtils.skipCurrentTag(parser);
                        } else if (publicKeys.get(publicKeyName) == null || publicKeys.get(publicKeyName).equals(currentKey)) {
                            publicKeys.put(publicKeyName, currentKey);
                        } else {
                            Slog.w(TAG, "Value of 'public-key' " + publicKeyName + " conflicts with previously defined value at " + parser.getPositionDescription());
                            sa2.recycle();
                            return false;
                        }
                    }
                    definedKeySets.get(currentKeySet).add(publicKeyName);
                    sa2.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("upgrade-key-set")) {
                    TypedArray sa3 = res.obtainAttributes(attrs, R.styleable.AndroidManifestUpgradeKeySet);
                    String name = sa3.getNonResourceString(0);
                    upgradeKeySets.add(name);
                    sa3.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <key-sets>: " + parser.getName() + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private PermissionGroup parsePermissionGroup(Package owner, int flags, Resources res, XmlPullParser parser, AttributeSet attrs, String[] outError) throws XmlPullParserException, IOException {
        PermissionGroup perm = new PermissionGroup(owner);
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestPermissionGroup);
        if (!parsePackageItemInfo(owner, perm.info, outError, "<permission-group>", sa, 2, 0, 1, 5, 7)) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.descriptionRes = sa.getResourceId(4, 0);
        perm.info.flags = sa.getInt(6, 0);
        perm.info.priority = sa.getInt(3, 0);
        if (perm.info.priority > 0 && (flags & 1) == 0) {
            perm.info.priority = 0;
        }
        sa.recycle();
        if (!parseAllMetaData(res, parser, attrs, "<permission-group>", perm, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.permissionGroups.add(perm);
        return perm;
    }

    private Permission parsePermission(Package owner, Resources res, XmlPullParser parser, AttributeSet attrs, String[] outError) throws XmlPullParserException, IOException {
        Permission perm = new Permission(owner);
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestPermission);
        if (!parsePackageItemInfo(owner, perm.info, outError, "<permission>", sa, 2, 0, 1, 6, 8)) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.group = sa.getNonResourceString(4);
        if (perm.info.group != null) {
            perm.info.group = perm.info.group.intern();
        }
        perm.info.descriptionRes = sa.getResourceId(5, 0);
        perm.info.protectionLevel = sa.getInt(3, 0);
        perm.info.flags = sa.getInt(7, 0);
        sa.recycle();
        if (perm.info.protectionLevel == -1) {
            outError[0] = "<permission> does not specify protectionLevel";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.protectionLevel = PermissionInfo.fixProtectionLevel(perm.info.protectionLevel);
        if ((perm.info.protectionLevel & 240) != 0 && (perm.info.protectionLevel & 15) != 2) {
            outError[0] = "<permission>  protectionLevel specifies a flag but is not based on signature type";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        if (!parseAllMetaData(res, parser, attrs, "<permission>", perm, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.permissions.add(perm);
        return perm;
    }

    private Permission parsePermissionTree(Package owner, Resources res, XmlPullParser parser, AttributeSet attrs, String[] outError) throws XmlPullParserException, IOException {
        Permission perm = new Permission(owner);
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestPermissionTree);
        if (!parsePackageItemInfo(owner, perm.info, outError, "<permission-tree>", sa, 2, 0, 1, 3, 4)) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        sa.recycle();
        int index = perm.info.name.indexOf(46);
        if (index > 0) {
            index = perm.info.name.indexOf(46, index + 1);
        }
        if (index < 0) {
            outError[0] = "<permission-tree> name has less than three segments: " + perm.info.name;
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        perm.info.descriptionRes = 0;
        perm.info.protectionLevel = 0;
        perm.tree = true;
        if (!parseAllMetaData(res, parser, attrs, "<permission-tree>", perm, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.permissions.add(perm);
        return perm;
    }

    private Instrumentation parseInstrumentation(Package owner, Resources res, XmlPullParser parser, AttributeSet attrs, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestInstrumentation);
        if (this.mParseInstrumentationArgs == null) {
            this.mParseInstrumentationArgs = new ParsePackageItemArgs(owner, outError, 2, 0, 1, 6, 7);
            this.mParseInstrumentationArgs.tag = "<instrumentation>";
        }
        this.mParseInstrumentationArgs.sa = sa;
        Instrumentation a = new Instrumentation(this.mParseInstrumentationArgs, new InstrumentationInfo());
        if (outError[0] != null) {
            sa.recycle();
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        String str = sa.getNonResourceString(3);
        a.info.targetPackage = str != null ? str.intern() : null;
        a.info.handleProfiling = sa.getBoolean(4, false);
        a.info.functionalTest = sa.getBoolean(5, false);
        sa.recycle();
        if (a.info.targetPackage == null) {
            outError[0] = "<instrumentation> does not specify targetPackage";
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        if (!parseAllMetaData(res, parser, attrs, "<instrumentation>", a, outError)) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return null;
        }
        owner.instrumentation.add(a);
        return a;
    }

    private boolean parseBaseApplication(Package owner, Resources res, XmlPullParser parser, AttributeSet attrs, int flags, String[] outError) throws XmlPullParserException, IOException {
        String str;
        CharSequence pname;
        ApplicationInfo ai = owner.applicationInfo;
        String pkgName = owner.applicationInfo.packageName;
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestApplication);
        String name = sa.getNonConfigurationString(3, 0);
        if (name != null) {
            ai.className = buildClassName(pkgName, name, outError);
            if (ai.className == null) {
                sa.recycle();
                this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                return false;
            }
        }
        String manageSpaceActivity = sa.getNonConfigurationString(4, 1024);
        if (manageSpaceActivity != null) {
            ai.manageSpaceActivityName = buildClassName(pkgName, manageSpaceActivity, outError);
        }
        boolean allowBackup = sa.getBoolean(17, true);
        if (allowBackup) {
            ai.flags |= 32768;
            String backupAgent = sa.getNonConfigurationString(16, 1024);
            if (backupAgent != null) {
                ai.backupAgentName = buildClassName(pkgName, backupAgent, outError);
                if (sa.getBoolean(18, true)) {
                    ai.flags |= 65536;
                }
                if (sa.getBoolean(21, false)) {
                    ai.flags |= 131072;
                }
                if (sa.getBoolean(32, false)) {
                    ai.flags |= 67108864;
                }
            }
        }
        TypedValue v = sa.peekValue(1);
        if (v != null) {
            int i = v.resourceId;
            ai.labelRes = i;
            if (i == 0) {
                ai.nonLocalizedLabel = v.coerceToString();
            }
        }
        ai.icon = sa.getResourceId(2, 0);
        ai.logo = sa.getResourceId(22, 0);
        ai.banner = sa.getResourceId(30, 0);
        ai.theme = sa.getResourceId(0, 0);
        ai.descriptionRes = sa.getResourceId(13, 0);
        if ((flags & 1) != 0 && sa.getBoolean(8, false)) {
            ai.flags |= 8;
        }
        if (sa.getBoolean(27, false)) {
            owner.mRequiredForAllUsers = true;
        }
        String restrictedAccountType = sa.getString(28);
        if (restrictedAccountType != null && restrictedAccountType.length() > 0) {
            owner.mRestrictedAccountType = restrictedAccountType;
        }
        String requiredAccountType = sa.getString(29);
        if (requiredAccountType != null && requiredAccountType.length() > 0) {
            owner.mRequiredAccountType = requiredAccountType;
        }
        if (sa.getBoolean(10, false)) {
            ai.flags |= 2;
        }
        if (sa.getBoolean(20, false)) {
            ai.flags |= 16384;
        }
        owner.baseHardwareAccelerated = sa.getBoolean(23, owner.applicationInfo.targetSdkVersion >= 14);
        if (sa.getBoolean(7, true)) {
            ai.flags |= 4;
        }
        if (sa.getBoolean(14, false)) {
            ai.flags |= 32;
        }
        if (sa.getBoolean(5, true)) {
            ai.flags |= 64;
        }
        if (sa.getBoolean(15, false)) {
            ai.flags |= 256;
        }
        if (sa.getBoolean(24, false)) {
            ai.flags |= 1048576;
        }
        if (mLargeHeapList.contains(pkgName)) {
            ai.flags |= 1048576;
            Slog.i(TAG, "Set FLAG_LARGE_HEAP for package : " + pkgName);
        }
        if (sa.getBoolean(26, false)) {
            ai.flags |= 4194304;
        }
        if (sa.getBoolean(33, false)) {
            ai.flags |= Integer.MIN_VALUE;
        }
        String str2 = sa.getNonConfigurationString(6, 0);
        ai.permission = (str2 == null || str2.length() <= 0) ? null : str2.intern();
        if (owner.applicationInfo.targetSdkVersion >= 8) {
            str = sa.getNonConfigurationString(12, 1024);
        } else {
            str = sa.getNonResourceString(12);
        }
        ai.taskAffinity = buildTaskAffinityName(ai.packageName, ai.packageName, str, outError);
        if (outError[0] == null) {
            if (owner.applicationInfo.targetSdkVersion >= 8) {
                pname = sa.getNonConfigurationString(11, 1024);
            } else {
                pname = sa.getNonResourceString(11);
            }
            ai.processName = buildProcessName(ai.packageName, null, pname, flags, this.mSeparateProcesses, outError);
            ai.enabled = sa.getBoolean(9, true);
            if (sa.getBoolean(31, false)) {
                ai.flags |= 33554432;
            }
        }
        ai.uiOptions = sa.getInt(25, 0);
        sa.recycle();
        if (outError[0] != null) {
            this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
            return false;
        }
        int innerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= innerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(Context.ACTIVITY_SERVICE)) {
                    Activity a = parseActivity(owner, res, parser, attrs, flags, outError, false, owner.baseHardwareAccelerated);
                    if (a == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a);
                } else if (tagName.equals("receiver")) {
                    Activity a2 = parseActivity(owner, res, parser, attrs, flags, outError, true, false);
                    if (a2 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.receivers.add(a2);
                } else if (tagName.equals(Notification.CATEGORY_SERVICE)) {
                    Service s = parseService(owner, res, parser, attrs, flags, outError);
                    if (s == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.services.add(s);
                } else if (tagName.equals("provider")) {
                    Provider p = parseProvider(owner, res, parser, attrs, flags, outError);
                    if (p == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.providers.add(p);
                } else if (tagName.equals("activity-alias")) {
                    Activity a3 = parseActivityAlias(owner, res, parser, attrs, flags, outError);
                    if (a3 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a3);
                } else if (parser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(res, parser, attrs, owner.mAppMetaData, outError);
                    owner.mAppMetaData = metaData;
                    if (metaData == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                } else if (tagName.equals("library")) {
                    TypedArray sa2 = res.obtainAttributes(attrs, R.styleable.AndroidManifestLibrary);
                    String lname = sa2.getNonResourceString(0);
                    sa2.recycle();
                    if (lname != null) {
                        String lname2 = lname.intern();
                        if (!ArrayUtils.contains(owner.libraryNames, lname2)) {
                            owner.libraryNames = ArrayUtils.add(owner.libraryNames, lname2);
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("uses-library")) {
                    TypedArray sa3 = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesLibrary);
                    String lname3 = sa3.getNonResourceString(0);
                    boolean req = sa3.getBoolean(1, true);
                    sa3.recycle();
                    if (lname3 != null) {
                        String lname4 = lname3.intern();
                        if (req) {
                            owner.usesLibraries = ArrayUtils.add(owner.usesLibraries, lname4);
                        } else {
                            owner.usesOptionalLibraries = ArrayUtils.add(owner.usesOptionalLibraries, lname4);
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("uses-package")) {
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <application>: " + tagName + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private boolean parseSplitApplication(Package owner, Resources res, XmlPullParser parser, AttributeSet attrs, int flags, int splitIndex, String[] outError) throws XmlPullParserException, IOException {
        if (res.obtainAttributes(attrs, R.styleable.AndroidManifestApplication).getBoolean(7, true)) {
            int[] iArr = owner.splitFlags;
            iArr[splitIndex] = iArr[splitIndex] | 4;
        }
        int innerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= innerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String tagName = parser.getName();
                if (tagName.equals(Context.ACTIVITY_SERVICE)) {
                    Activity a = parseActivity(owner, res, parser, attrs, flags, outError, false, owner.baseHardwareAccelerated);
                    if (a == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a);
                } else if (tagName.equals("receiver")) {
                    Activity a2 = parseActivity(owner, res, parser, attrs, flags, outError, true, false);
                    if (a2 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.receivers.add(a2);
                } else if (tagName.equals(Notification.CATEGORY_SERVICE)) {
                    Service s = parseService(owner, res, parser, attrs, flags, outError);
                    if (s == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.services.add(s);
                } else if (tagName.equals("provider")) {
                    Provider p = parseProvider(owner, res, parser, attrs, flags, outError);
                    if (p == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.providers.add(p);
                } else if (tagName.equals("activity-alias")) {
                    Activity a3 = parseActivityAlias(owner, res, parser, attrs, flags, outError);
                    if (a3 == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                    owner.activities.add(a3);
                } else if (parser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(res, parser, attrs, owner.mAppMetaData, outError);
                    owner.mAppMetaData = metaData;
                    if (metaData == null) {
                        this.mParseError = PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
                        return false;
                    }
                } else if (tagName.equals("uses-library")) {
                    TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestUsesLibrary);
                    String lname = sa.getNonResourceString(0);
                    boolean req = sa.getBoolean(1, true);
                    sa.recycle();
                    if (lname != null) {
                        String lname2 = lname.intern();
                        if (req) {
                            owner.usesLibraries = ArrayUtils.add(owner.usesLibraries, lname2);
                            owner.usesOptionalLibraries = ArrayUtils.remove(owner.usesOptionalLibraries, lname2);
                        } else if (!ArrayUtils.contains(owner.usesLibraries, lname2)) {
                            owner.usesOptionalLibraries = ArrayUtils.add(owner.usesOptionalLibraries, lname2);
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("uses-package")) {
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <application>: " + tagName + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private boolean parsePackageItemInfo(Package owner, PackageItemInfo outInfo, String[] outError, String tag, TypedArray sa, int nameRes, int labelRes, int iconRes, int logoRes, int bannerRes) {
        String name = sa.getNonConfigurationString(nameRes, 0);
        if (name == null) {
            outError[0] = tag + " does not specify android:name";
            return false;
        }
        outInfo.name = buildClassName(owner.applicationInfo.packageName, name, outError);
        if (outInfo.name == null) {
            return false;
        }
        int iconVal = sa.getResourceId(iconRes, 0);
        if (iconVal != 0) {
            outInfo.icon = iconVal;
            outInfo.nonLocalizedLabel = null;
        }
        int logoVal = sa.getResourceId(logoRes, 0);
        if (logoVal != 0) {
            outInfo.logo = logoVal;
        }
        int bannerVal = sa.getResourceId(bannerRes, 0);
        if (bannerVal != 0) {
            outInfo.banner = bannerVal;
        }
        TypedValue v = sa.peekValue(labelRes);
        if (v != null) {
            int i = v.resourceId;
            outInfo.labelRes = i;
            if (i == 0) {
                outInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        outInfo.packageName = owner.packageName;
        return true;
    }

    private Activity parseActivity(Package r23, Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, int i, String[] strArr, boolean z, boolean z2) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestActivity);
        if (this.mParseActivityArgs == null) {
            this.mParseActivityArgs = new ParseComponentArgs(r23, strArr, 3, 1, 2, 23, 30, this.mSeparateProcesses, 7, 17, 5);
        }
        this.mParseActivityArgs.tag = z ? "<receiver>" : "<activity>";
        this.mParseActivityArgs.sa = typedArrayObtainAttributes;
        this.mParseActivityArgs.flags = i;
        Activity activity = new Activity(this.mParseActivityArgs, new ActivityInfo());
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        boolean zHasValue = typedArrayObtainAttributes.hasValue(6);
        if (zHasValue) {
            activity.info.exported = typedArrayObtainAttributes.getBoolean(6, false);
        }
        activity.info.theme = typedArrayObtainAttributes.getResourceId(0, 0);
        activity.info.uiOptions = typedArrayObtainAttributes.getInt(26, activity.info.applicationInfo.uiOptions);
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(27, 1024);
        if (nonConfigurationString != null) {
            String strBuildClassName = buildClassName(activity.info.packageName, nonConfigurationString, strArr);
            if (strArr[0] == null) {
                activity.info.parentActivityName = strBuildClassName;
            } else {
                Log.e(TAG, "Activity " + activity.info.name + " specified invalid parentActivityName " + nonConfigurationString);
                strArr[0] = null;
            }
        }
        String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(4, 0);
        if (nonConfigurationString2 == null) {
            activity.info.permission = r23.applicationInfo.permission;
        } else {
            activity.info.permission = nonConfigurationString2.length() > 0 ? nonConfigurationString2.toString().intern() : null;
        }
        activity.info.taskAffinity = buildTaskAffinityName(r23.applicationInfo.packageName, r23.applicationInfo.taskAffinity, typedArrayObtainAttributes.getNonConfigurationString(8, 1024), strArr);
        activity.info.flags = 0;
        if (typedArrayObtainAttributes.getBoolean(9, false)) {
            activity.info.flags |= 1;
        }
        if (typedArrayObtainAttributes.getBoolean(10, false)) {
            activity.info.flags |= 2;
        }
        if (typedArrayObtainAttributes.getBoolean(11, false)) {
            activity.info.flags |= 4;
        }
        if (typedArrayObtainAttributes.getBoolean(21, false)) {
            activity.info.flags |= 128;
        }
        if (typedArrayObtainAttributes.getBoolean(18, false)) {
            activity.info.flags |= 8;
        }
        if (typedArrayObtainAttributes.getBoolean(12, false)) {
            activity.info.flags |= 16;
        }
        if (typedArrayObtainAttributes.getBoolean(13, false)) {
            activity.info.flags |= 32;
        }
        if (typedArrayObtainAttributes.getBoolean(19, (r23.applicationInfo.flags & 32) != 0)) {
            activity.info.flags |= 64;
        }
        if (typedArrayObtainAttributes.getBoolean(22, false)) {
            activity.info.flags |= 256;
        }
        if (typedArrayObtainAttributes.getBoolean(29, false)) {
            activity.info.flags |= 1024;
        }
        if (typedArrayObtainAttributes.getBoolean(24, false)) {
            activity.info.flags |= 2048;
        }
        if (!z) {
            if (typedArrayObtainAttributes.getBoolean(25, z2)) {
                activity.info.flags |= 512;
            }
            activity.info.launchMode = typedArrayObtainAttributes.getInt(14, 0);
            activity.info.documentLaunchMode = typedArrayObtainAttributes.getInt(33, 0);
            activity.info.maxRecents = typedArrayObtainAttributes.getInt(34, ActivityManager.getDefaultAppRecentsLimitStatic());
            activity.info.screenOrientation = typedArrayObtainAttributes.getInt(15, -1);
            activity.info.configChanges = typedArrayObtainAttributes.getInt(16, 0);
            activity.info.softInputMode = typedArrayObtainAttributes.getInt(20, 0);
            activity.info.persistableMode = typedArrayObtainAttributes.getInteger(32, 0);
            if (typedArrayObtainAttributes.getBoolean(31, false)) {
                activity.info.flags |= Integer.MIN_VALUE;
            }
            if (typedArrayObtainAttributes.getBoolean(35, false)) {
                activity.info.flags |= 8192;
            }
            if (typedArrayObtainAttributes.getBoolean(36, false)) {
                activity.info.flags |= 4096;
            }
            if (typedArrayObtainAttributes.getBoolean(37, false)) {
                activity.info.flags |= 16384;
            }
        } else {
            activity.info.launchMode = 0;
            activity.info.configChanges = 0;
        }
        if (z) {
            if (typedArrayObtainAttributes.getBoolean(28, false)) {
                activity.info.flags |= 1073741824;
                if (activity.info.exported && (i & 128) == 0) {
                    Slog.w(TAG, "Activity exported request ignored due to singleUser: " + activity.className + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    activity.info.exported = false;
                    zHasValue = true;
                }
            }
            if (typedArrayObtainAttributes.getBoolean(38, false)) {
                activity.info.flags |= 536870912;
            }
        }
        typedArrayObtainAttributes.recycle();
        if (z && (r23.applicationInfo.flags & 268435456) != 0 && activity.info.processName == r23.packageName) {
            strArr[0] = "Heavy-weight applications can not have receivers in main process";
        }
        if (strArr[0] != null) {
            return null;
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlPullParser.getName().equals("intent-filter")) {
                    ActivityIntentInfo activityIntentInfo = new ActivityIntentInfo(activity);
                    if (!parseIntent(resources, xmlPullParser, attributeSet, true, activityIntentInfo, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo.countActions() == 0) {
                        Slog.w(TAG, "No actions in intent filter at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    } else {
                        activity.intents.add(activityIntentInfo);
                    }
                } else if (!z && xmlPullParser.getName().equals("preferred")) {
                    ActivityIntentInfo activityIntentInfo2 = new ActivityIntentInfo(activity);
                    if (!parseIntent(resources, xmlPullParser, attributeSet, false, activityIntentInfo2, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo2.countActions() == 0) {
                        Slog.w(TAG, "No actions in preferred at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    } else {
                        if (r23.preferredActivityFilters == null) {
                            r23.preferredActivityFilters = new ArrayList<>();
                        }
                        r23.preferredActivityFilters.add(activityIntentInfo2);
                    }
                } else if (xmlPullParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlPullParser, attributeSet, activity.metaData, strArr);
                    activity.metaData = metaData;
                    if (metaData == null) {
                        return null;
                    }
                } else {
                    Slog.w(TAG, "Problem in package " + this.mArchiveSourcePath + ":");
                    if (z) {
                        Slog.w(TAG, "Unknown element under <receiver>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    } else {
                        Slog.w(TAG, "Unknown element under <activity>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    }
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
    }

    private Activity parseActivityAlias(Package r29, Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, int i, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestActivityAlias);
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(7, 1024);
        if (nonConfigurationString == null) {
            strArr[0] = "<activity-alias> does not specify android:targetActivity";
            typedArrayObtainAttributes.recycle();
            return null;
        }
        String strBuildClassName = buildClassName(r29.applicationInfo.packageName, nonConfigurationString, strArr);
        if (strBuildClassName == null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        if (this.mParseActivityAliasArgs == null) {
            this.mParseActivityAliasArgs = new ParseComponentArgs(r29, strArr, 2, 0, 1, 8, 10, this.mSeparateProcesses, 0, 6, 4);
            this.mParseActivityAliasArgs.tag = "<activity-alias>";
        }
        this.mParseActivityAliasArgs.sa = typedArrayObtainAttributes;
        this.mParseActivityAliasArgs.flags = i;
        Activity activity = null;
        int size = r29.activities.size();
        int i2 = 0;
        while (true) {
            if (i2 >= size) {
                break;
            }
            Activity activity2 = r29.activities.get(i2);
            if (!strBuildClassName.equals(activity2.info.name)) {
                i2++;
            } else {
                activity = activity2;
                break;
            }
        }
        if (activity == null) {
            strArr[0] = "<activity-alias> target activity " + strBuildClassName + " not found in manifest";
            typedArrayObtainAttributes.recycle();
            return null;
        }
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.targetActivity = strBuildClassName;
        activityInfo.configChanges = activity.info.configChanges;
        activityInfo.flags = activity.info.flags;
        activityInfo.icon = activity.info.icon;
        activityInfo.logo = activity.info.logo;
        activityInfo.banner = activity.info.banner;
        activityInfo.labelRes = activity.info.labelRes;
        activityInfo.nonLocalizedLabel = activity.info.nonLocalizedLabel;
        activityInfo.launchMode = activity.info.launchMode;
        activityInfo.processName = activity.info.processName;
        if (activityInfo.descriptionRes == 0) {
            activityInfo.descriptionRes = activity.info.descriptionRes;
        }
        activityInfo.screenOrientation = activity.info.screenOrientation;
        activityInfo.taskAffinity = activity.info.taskAffinity;
        activityInfo.theme = activity.info.theme;
        activityInfo.softInputMode = activity.info.softInputMode;
        activityInfo.uiOptions = activity.info.uiOptions;
        activityInfo.parentActivityName = activity.info.parentActivityName;
        activityInfo.maxRecents = activity.info.maxRecents;
        Activity activity3 = new Activity(this.mParseActivityAliasArgs, activityInfo);
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        boolean zHasValue = typedArrayObtainAttributes.hasValue(5);
        if (zHasValue) {
            activity3.info.exported = typedArrayObtainAttributes.getBoolean(5, false);
        }
        String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(3, 0);
        if (nonConfigurationString2 != null) {
            activity3.info.permission = nonConfigurationString2.length() > 0 ? nonConfigurationString2.toString().intern() : null;
        }
        String nonConfigurationString3 = typedArrayObtainAttributes.getNonConfigurationString(9, 1024);
        if (nonConfigurationString3 != null) {
            String strBuildClassName2 = buildClassName(activity3.info.packageName, nonConfigurationString3, strArr);
            if (strArr[0] == null) {
                activity3.info.parentActivityName = strBuildClassName2;
            } else {
                Log.e(TAG, "Activity alias " + activity3.info.name + " specified invalid parentActivityName " + nonConfigurationString3);
                strArr[0] = null;
            }
        }
        typedArrayObtainAttributes.recycle();
        if (strArr[0] != null) {
            return null;
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlPullParser.getName().equals("intent-filter")) {
                    ActivityIntentInfo activityIntentInfo = new ActivityIntentInfo(activity3);
                    if (!parseIntent(resources, xmlPullParser, attributeSet, true, activityIntentInfo, strArr)) {
                        return null;
                    }
                    if (activityIntentInfo.countActions() == 0) {
                        Slog.w(TAG, "No actions in intent filter at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    } else {
                        activity3.intents.add(activityIntentInfo);
                    }
                } else if (xmlPullParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlPullParser, attributeSet, activity3.metaData, strArr);
                    activity3.metaData = metaData;
                    if (metaData == null) {
                        return null;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <activity-alias>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
    }

    private Provider parseProvider(Package owner, Resources res, XmlPullParser parser, AttributeSet attrs, int flags, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestProvider);
        if (this.mParseProviderArgs == null) {
            this.mParseProviderArgs = new ParseComponentArgs(owner, outError, 2, 0, 1, 15, 17, this.mSeparateProcesses, 8, 14, 6);
            this.mParseProviderArgs.tag = "<provider>";
        }
        this.mParseProviderArgs.sa = sa;
        this.mParseProviderArgs.flags = flags;
        Provider p = new Provider(this.mParseProviderArgs, new ProviderInfo());
        if (outError[0] != null) {
            sa.recycle();
            return null;
        }
        boolean providerExportedDefault = false;
        if (owner.applicationInfo.targetSdkVersion < 17) {
            providerExportedDefault = true;
        }
        p.info.exported = sa.getBoolean(7, providerExportedDefault);
        String cpname = sa.getNonConfigurationString(10, 0);
        p.info.isSyncable = sa.getBoolean(11, false);
        String permission = sa.getNonConfigurationString(3, 0);
        String str = sa.getNonConfigurationString(4, 0);
        if (str == null) {
            str = permission;
        }
        if (str == null) {
            p.info.readPermission = owner.applicationInfo.permission;
        } else {
            p.info.readPermission = str.length() > 0 ? str.toString().intern() : null;
        }
        String str2 = sa.getNonConfigurationString(5, 0);
        if (str2 == null) {
            str2 = permission;
        }
        if (str2 == null) {
            p.info.writePermission = owner.applicationInfo.permission;
        } else {
            p.info.writePermission = str2.length() > 0 ? str2.toString().intern() : null;
        }
        p.info.grantUriPermissions = sa.getBoolean(13, false);
        p.info.multiprocess = sa.getBoolean(9, false);
        p.info.initOrder = sa.getInt(12, 0);
        p.info.flags = 0;
        if (sa.getBoolean(16, false)) {
            p.info.flags |= 1073741824;
            if (p.info.exported && (flags & 128) == 0) {
                Slog.w(TAG, "Provider exported request ignored due to singleUser: " + p.className + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                p.info.exported = false;
            }
        }
        sa.recycle();
        if ((owner.applicationInfo.flags & 268435456) != 0 && p.info.processName == owner.packageName) {
            outError[0] = "Heavy-weight applications can not have providers in main process";
            return null;
        }
        if (cpname == null) {
            outError[0] = "<provider> does not include authorities attribute";
            return null;
        }
        if (cpname.length() <= 0) {
            outError[0] = "<provider> has empty authorities attribute";
            return null;
        }
        p.info.authority = cpname.intern();
        if (!parseProviderTags(res, parser, attrs, p, outError)) {
            return null;
        }
        return p;
    }

    private boolean parseProviderTags(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Provider provider, String[] strArr) throws XmlPullParserException, IOException {
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlPullParser.getName().equals("intent-filter")) {
                    ProviderIntentInfo providerIntentInfo = new ProviderIntentInfo(provider);
                    if (!parseIntent(resources, xmlPullParser, attributeSet, true, providerIntentInfo, strArr)) {
                        return false;
                    }
                    provider.intents.add(providerIntentInfo);
                } else if (xmlPullParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlPullParser, attributeSet, provider.metaData, strArr);
                    provider.metaData = metaData;
                    if (metaData == null) {
                        return false;
                    }
                } else if (xmlPullParser.getName().equals("grant-uri-permission")) {
                    TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestGrantUriPermission);
                    PatternMatcher patternMatcher = null;
                    String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(0, 0);
                    if (nonConfigurationString != null) {
                        patternMatcher = new PatternMatcher(nonConfigurationString, 0);
                    }
                    String nonConfigurationString2 = typedArrayObtainAttributes.getNonConfigurationString(1, 0);
                    if (nonConfigurationString2 != null) {
                        patternMatcher = new PatternMatcher(nonConfigurationString2, 1);
                    }
                    String nonConfigurationString3 = typedArrayObtainAttributes.getNonConfigurationString(2, 0);
                    if (nonConfigurationString3 != null) {
                        patternMatcher = new PatternMatcher(nonConfigurationString3, 2);
                    }
                    typedArrayObtainAttributes.recycle();
                    if (patternMatcher != null) {
                        if (provider.info.uriPermissionPatterns == null) {
                            provider.info.uriPermissionPatterns = new PatternMatcher[1];
                            provider.info.uriPermissionPatterns[0] = patternMatcher;
                        } else {
                            int length = provider.info.uriPermissionPatterns.length;
                            PatternMatcher[] patternMatcherArr = new PatternMatcher[length + 1];
                            System.arraycopy(provider.info.uriPermissionPatterns, 0, patternMatcherArr, 0, length);
                            patternMatcherArr[length] = patternMatcher;
                            provider.info.uriPermissionPatterns = patternMatcherArr;
                        }
                        provider.info.grantUriPermissions = true;
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    } else {
                        Slog.w(TAG, "Unknown element under <path-permission>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    }
                } else if (xmlPullParser.getName().equals("path-permission")) {
                    TypedArray typedArrayObtainAttributes2 = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestPathPermission);
                    PathPermission pathPermission = null;
                    String nonConfigurationString4 = typedArrayObtainAttributes2.getNonConfigurationString(0, 0);
                    String nonConfigurationString5 = typedArrayObtainAttributes2.getNonConfigurationString(1, 0);
                    if (nonConfigurationString5 == null) {
                        nonConfigurationString5 = nonConfigurationString4;
                    }
                    String nonConfigurationString6 = typedArrayObtainAttributes2.getNonConfigurationString(2, 0);
                    if (nonConfigurationString6 == null) {
                        nonConfigurationString6 = nonConfigurationString4;
                    }
                    boolean z = false;
                    if (nonConfigurationString5 != null) {
                        nonConfigurationString5 = nonConfigurationString5.intern();
                        z = true;
                    }
                    if (nonConfigurationString6 != null) {
                        nonConfigurationString6 = nonConfigurationString6.intern();
                        z = true;
                    }
                    if (!z) {
                        Slog.w(TAG, "No readPermission or writePermssion for <path-permission>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                        XmlUtils.skipCurrentTag(xmlPullParser);
                    } else {
                        String nonConfigurationString7 = typedArrayObtainAttributes2.getNonConfigurationString(3, 0);
                        if (nonConfigurationString7 != null) {
                            pathPermission = new PathPermission(nonConfigurationString7, 0, nonConfigurationString5, nonConfigurationString6);
                        }
                        String nonConfigurationString8 = typedArrayObtainAttributes2.getNonConfigurationString(4, 0);
                        if (nonConfigurationString8 != null) {
                            pathPermission = new PathPermission(nonConfigurationString8, 1, nonConfigurationString5, nonConfigurationString6);
                        }
                        String nonConfigurationString9 = typedArrayObtainAttributes2.getNonConfigurationString(5, 0);
                        if (nonConfigurationString9 != null) {
                            pathPermission = new PathPermission(nonConfigurationString9, 2, nonConfigurationString5, nonConfigurationString6);
                        }
                        typedArrayObtainAttributes2.recycle();
                        if (pathPermission != null) {
                            if (provider.info.pathPermissions == null) {
                                provider.info.pathPermissions = new PathPermission[1];
                                provider.info.pathPermissions[0] = pathPermission;
                            } else {
                                int length2 = provider.info.pathPermissions.length;
                                PathPermission[] pathPermissionArr = new PathPermission[length2 + 1];
                                System.arraycopy(provider.info.pathPermissions, 0, pathPermissionArr, 0, length2);
                                pathPermissionArr[length2] = pathPermission;
                                provider.info.pathPermissions = pathPermissionArr;
                            }
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        } else {
                            Slog.w(TAG, "No path, pathPrefix, or pathPattern for <path-permission>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                            XmlUtils.skipCurrentTag(xmlPullParser);
                        }
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <provider>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
    }

    private Service parseService(Package r21, Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, int i, String[] strArr) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = resources.obtainAttributes(attributeSet, R.styleable.AndroidManifestService);
        if (this.mParseServiceArgs == null) {
            this.mParseServiceArgs = new ParseComponentArgs(r21, strArr, 2, 0, 1, 8, 12, this.mSeparateProcesses, 6, 7, 4);
            this.mParseServiceArgs.tag = "<service>";
        }
        this.mParseServiceArgs.sa = typedArrayObtainAttributes;
        this.mParseServiceArgs.flags = i;
        Service service = new Service(this.mParseServiceArgs, new ServiceInfo());
        if (strArr[0] != null) {
            typedArrayObtainAttributes.recycle();
            return null;
        }
        boolean zHasValue = typedArrayObtainAttributes.hasValue(5);
        if (zHasValue) {
            service.info.exported = typedArrayObtainAttributes.getBoolean(5, false);
        }
        String nonConfigurationString = typedArrayObtainAttributes.getNonConfigurationString(3, 0);
        if (nonConfigurationString == null) {
            service.info.permission = r21.applicationInfo.permission;
        } else {
            service.info.permission = nonConfigurationString.length() > 0 ? nonConfigurationString.toString().intern() : null;
        }
        service.info.flags = 0;
        if (typedArrayObtainAttributes.getBoolean(9, false)) {
            service.info.flags |= 1;
        }
        if (typedArrayObtainAttributes.getBoolean(10, false)) {
            service.info.flags |= 2;
        }
        if (typedArrayObtainAttributes.getBoolean(11, false)) {
            service.info.flags |= 1073741824;
            if (service.info.exported && (i & 128) == 0) {
                Slog.w(TAG, "Service exported request ignored due to singleUser: " + service.className + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                service.info.exported = false;
                zHasValue = true;
            }
        }
        typedArrayObtainAttributes.recycle();
        if ((r21.applicationInfo.flags & 268435456) != 0 && service.info.processName == r21.packageName) {
            strArr[0] = "Heavy-weight applications can not have services in main process";
            return null;
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next = xmlPullParser.next();
            if (next == 1 || (next == 3 && xmlPullParser.getDepth() <= depth)) {
                break;
            }
            if (next != 3 && next != 4) {
                if (xmlPullParser.getName().equals("intent-filter")) {
                    ServiceIntentInfo serviceIntentInfo = new ServiceIntentInfo(service);
                    if (!parseIntent(resources, xmlPullParser, attributeSet, true, serviceIntentInfo, strArr)) {
                        return null;
                    }
                    service.intents.add(serviceIntentInfo);
                } else if (xmlPullParser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(resources, xmlPullParser, attributeSet, service.metaData, strArr);
                    service.metaData = metaData;
                    if (metaData == null) {
                        return null;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <service>: " + xmlPullParser.getName() + " at " + this.mArchiveSourcePath + " " + xmlPullParser.getPositionDescription());
                    XmlUtils.skipCurrentTag(xmlPullParser);
                }
            }
        }
    }

    private boolean parseAllMetaData(Resources res, XmlPullParser parser, AttributeSet attrs, String tag, Component outInfo, String[] outError) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                if (parser.getName().equals("meta-data")) {
                    Bundle metaData = parseMetaData(res, parser, attrs, outInfo.metaData, outError);
                    outInfo.metaData = metaData;
                    if (metaData == null) {
                        return false;
                    }
                } else {
                    Slog.w(TAG, "Unknown element under " + tag + ": " + parser.getName() + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private Bundle parseMetaData(Resources res, XmlPullParser parser, AttributeSet attrs, Bundle data, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestMetaData);
        if (data == null) {
            data = new Bundle();
        }
        String name = sa.getNonConfigurationString(0, 0);
        if (name == null) {
            outError[0] = "<meta-data> requires an android:name attribute";
            sa.recycle();
            return null;
        }
        String name2 = name.intern();
        TypedValue v = sa.peekValue(2);
        if (v != null && v.resourceId != 0) {
            data.putInt(name2, v.resourceId);
        } else {
            TypedValue v2 = sa.peekValue(1);
            if (v2 != null) {
                if (v2.type == 3) {
                    CharSequence cs = v2.coerceToString();
                    data.putString(name2, cs != null ? cs.toString().intern() : null);
                } else if (v2.type == 18) {
                    data.putBoolean(name2, v2.data != 0);
                } else if (v2.type >= 16 && v2.type <= 31) {
                    data.putInt(name2, v2.data);
                } else if (v2.type == 4) {
                    data.putFloat(name2, v2.getFloat());
                } else {
                    Slog.w(TAG, "<meta-data> only supports string, integer, float, color, boolean, and resource reference types: " + parser.getName() + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                }
            } else {
                outError[0] = "<meta-data> requires an android:value or android:resource attribute";
                data = null;
            }
        }
        sa.recycle();
        XmlUtils.skipCurrentTag(parser);
        return data;
    }

    private static VerifierInfo parseVerifier(Resources res, XmlPullParser parser, AttributeSet attrs, int flags) {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestPackageVerifier);
        String packageName = sa.getNonResourceString(0);
        String encodedPublicKey = sa.getNonResourceString(1);
        sa.recycle();
        if (packageName == null || packageName.length() == 0) {
            Slog.i(TAG, "verifier package name was null; skipping");
            return null;
        }
        PublicKey publicKey = parsePublicKey(encodedPublicKey);
        if (publicKey == null) {
            Slog.i(TAG, "Unable to parse verifier public key for " + packageName);
            return null;
        }
        return new VerifierInfo(packageName, publicKey);
    }

    public static final PublicKey parsePublicKey(String encodedPublicKey) {
        if (encodedPublicKey == null) {
            Slog.i(TAG, "Could not parse null public key");
            return null;
        }
        try {
            byte[] encoded = Base64.decode(encodedPublicKey, 0);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(keySpec);
            } catch (NoSuchAlgorithmException e) {
                Log.wtf(TAG, "Could not parse public key because RSA isn't included in build");
                return null;
            } catch (InvalidKeySpecException e2) {
                try {
                    KeyFactory keyFactory2 = KeyFactory.getInstance("DSA");
                    return keyFactory2.generatePublic(keySpec);
                } catch (NoSuchAlgorithmException e3) {
                    Log.wtf(TAG, "Could not parse public key because DSA isn't included in build");
                    return null;
                } catch (InvalidKeySpecException e4) {
                    return null;
                }
            }
        } catch (IllegalArgumentException e5) {
            Slog.i(TAG, "Could not parse verifier public key; invalid Base64");
            return null;
        }
    }

    private boolean parseIntent(Resources res, XmlPullParser parser, AttributeSet attrs, boolean allowGlobs, IntentInfo outInfo, String[] outError) throws XmlPullParserException, IOException {
        TypedArray sa = res.obtainAttributes(attrs, R.styleable.AndroidManifestIntentFilter);
        int priority = sa.getInt(2, 0);
        outInfo.setPriority(priority);
        TypedValue v = sa.peekValue(0);
        if (v != null) {
            int i = v.resourceId;
            outInfo.labelRes = i;
            if (i == 0) {
                outInfo.nonLocalizedLabel = v.coerceToString();
            }
        }
        outInfo.icon = sa.getResourceId(1, 0);
        outInfo.logo = sa.getResourceId(3, 0);
        outInfo.banner = sa.getResourceId(4, 0);
        sa.recycle();
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                break;
            }
            if (type != 3 && type != 4) {
                String nodeName = parser.getName();
                if (nodeName.equals(Camera.Parameters.SCENE_MODE_ACTION)) {
                    String value = attrs.getAttributeValue(ANDROID_RESOURCES, "name");
                    if (value == null || value == ProxyInfo.LOCAL_EXCL_LIST) {
                        break;
                    }
                    XmlUtils.skipCurrentTag(parser);
                    outInfo.addAction(value);
                } else if (nodeName.equals("category")) {
                    String value2 = attrs.getAttributeValue(ANDROID_RESOURCES, "name");
                    if (value2 == null || value2 == ProxyInfo.LOCAL_EXCL_LIST) {
                        break;
                    }
                    XmlUtils.skipCurrentTag(parser);
                    outInfo.addCategory(value2);
                } else if (nodeName.equals("data")) {
                    TypedArray sa2 = res.obtainAttributes(attrs, R.styleable.AndroidManifestData);
                    String str = sa2.getNonConfigurationString(0, 0);
                    if (str != null) {
                        try {
                            outInfo.addDataType(str);
                        } catch (IntentFilter.MalformedMimeTypeException e) {
                            outError[0] = e.toString();
                            sa2.recycle();
                            return false;
                        }
                    }
                    String str2 = sa2.getNonConfigurationString(1, 0);
                    if (str2 != null) {
                        outInfo.addDataScheme(str2);
                    }
                    String str3 = sa2.getNonConfigurationString(7, 0);
                    if (str3 != null) {
                        outInfo.addDataSchemeSpecificPart(str3, 0);
                    }
                    String str4 = sa2.getNonConfigurationString(8, 0);
                    if (str4 != null) {
                        outInfo.addDataSchemeSpecificPart(str4, 1);
                    }
                    String str5 = sa2.getNonConfigurationString(9, 0);
                    if (str5 != null) {
                        if (!allowGlobs) {
                            outError[0] = "sspPattern not allowed here; ssp must be literal";
                            return false;
                        }
                        outInfo.addDataSchemeSpecificPart(str5, 2);
                    }
                    String host = sa2.getNonConfigurationString(2, 0);
                    String port = sa2.getNonConfigurationString(3, 0);
                    if (host != null) {
                        outInfo.addDataAuthority(host, port);
                    }
                    String str6 = sa2.getNonConfigurationString(4, 0);
                    if (str6 != null) {
                        outInfo.addDataPath(str6, 0);
                    }
                    String str7 = sa2.getNonConfigurationString(5, 0);
                    if (str7 != null) {
                        outInfo.addDataPath(str7, 1);
                    }
                    String str8 = sa2.getNonConfigurationString(6, 0);
                    if (str8 != null) {
                        if (!allowGlobs) {
                            outError[0] = "pathPattern not allowed here; path must be literal";
                            return false;
                        }
                        outInfo.addDataPath(str8, 2);
                    }
                    sa2.recycle();
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <intent-filter>: " + parser.getName() + " at " + this.mArchiveSourcePath + " " + parser.getPositionDescription());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    public static final class Package {
        public String baseCodePath;
        public boolean baseHardwareAccelerated;
        public int baseRevisionCode;
        public String codePath;
        public boolean coreApp;
        public String cpuAbiOverride;
        public int installLocation;
        public Certificate[][] mCertificates;
        public Object mExtras;
        public ArrayMap<String, ArraySet<PublicKey>> mKeySetMapping;
        public long mLastPackageUsageTimeInMills;
        public boolean mOperationPending;
        public int mOverlayPriority;
        public String mOverlayTarget;
        public String mRequiredAccountType;
        public boolean mRequiredForAllUsers;
        public String mRestrictedAccountType;
        public String mSharedUserId;
        public int mSharedUserLabel;
        public Signature[] mSignatures;
        public ArraySet<PublicKey> mSigningKeys;
        public boolean mTrustedOverlay;
        public ArraySet<String> mUpgradeKeySets;
        public int mVersionCode;
        public String mVersionName;
        public ManifestDigest manifestDigest;
        public String packageName;
        public ArrayList<String> protectedBroadcasts;
        public String[] splitCodePaths;
        public int[] splitFlags;
        public String[] splitNames;
        public int[] splitRevisionCodes;
        public final ApplicationInfo applicationInfo = new ApplicationInfo();
        public final ArrayList<Permission> permissions = new ArrayList<>(0);
        public final ArrayList<PermissionGroup> permissionGroups = new ArrayList<>(0);
        public final ArrayList<Activity> activities = new ArrayList<>(0);
        public final ArrayList<Activity> receivers = new ArrayList<>(0);
        public final ArrayList<Provider> providers = new ArrayList<>(0);
        public final ArrayList<Service> services = new ArrayList<>(0);
        public final ArrayList<Instrumentation> instrumentation = new ArrayList<>(0);
        public final ArrayList<String> requestedPermissions = new ArrayList<>();
        public final ArrayList<Boolean> requestedPermissionsRequired = new ArrayList<>();
        public ArrayList<String> libraryNames = null;
        public ArrayList<String> usesLibraries = null;
        public ArrayList<String> usesOptionalLibraries = null;
        public String[] usesLibraryFiles = null;
        public ArrayList<ActivityIntentInfo> preferredActivityFilters = null;
        public ArrayList<String> mOriginalPackages = null;
        public String mRealPackage = null;
        public ArrayList<String> mAdoptPermissions = null;
        public Bundle mAppMetaData = null;
        public int mPreferredOrder = 0;
        public final ArraySet<String> mDexOptPerformed = new ArraySet<>(4);
        public ArrayList<ConfigurationInfo> configPreferences = null;
        public ArrayList<FeatureInfo> reqFeatures = null;
        public ArrayList<FeatureGroupInfo> featureGroups = null;

        public Package(String packageName) {
            this.packageName = packageName;
            this.applicationInfo.packageName = packageName;
            this.applicationInfo.uid = -1;
        }

        public List<String> getAllCodePaths() {
            ArrayList<String> paths = new ArrayList<>();
            paths.add(this.baseCodePath);
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                Collections.addAll(paths, this.splitCodePaths);
            }
            return paths;
        }

        public List<String> getAllCodePathsExcludingResourceOnly() {
            ArrayList<String> paths = new ArrayList<>();
            if ((this.applicationInfo.flags & 4) != 0) {
                paths.add(this.baseCodePath);
            }
            if (!ArrayUtils.isEmpty(this.splitCodePaths)) {
                for (int i = 0; i < this.splitCodePaths.length; i++) {
                    if ((this.splitFlags[i] & 4) != 0) {
                        paths.add(this.splitCodePaths[i]);
                    }
                }
            }
            return paths;
        }

        public void setPackageName(String newName) {
            this.packageName = newName;
            this.applicationInfo.packageName = newName;
            for (int i = this.permissions.size() - 1; i >= 0; i--) {
                this.permissions.get(i).setPackageName(newName);
            }
            for (int i2 = this.permissionGroups.size() - 1; i2 >= 0; i2--) {
                this.permissionGroups.get(i2).setPackageName(newName);
            }
            for (int i3 = this.activities.size() - 1; i3 >= 0; i3--) {
                this.activities.get(i3).setPackageName(newName);
            }
            for (int i4 = this.receivers.size() - 1; i4 >= 0; i4--) {
                this.receivers.get(i4).setPackageName(newName);
            }
            for (int i5 = this.providers.size() - 1; i5 >= 0; i5--) {
                this.providers.get(i5).setPackageName(newName);
            }
            for (int i6 = this.services.size() - 1; i6 >= 0; i6--) {
                this.services.get(i6).setPackageName(newName);
            }
            for (int i7 = this.instrumentation.size() - 1; i7 >= 0; i7--) {
                this.instrumentation.get(i7).setPackageName(newName);
            }
        }

        public boolean hasComponentClassName(String name) {
            for (int i = this.activities.size() - 1; i >= 0; i--) {
                if (name.equals(this.activities.get(i).className)) {
                    return true;
                }
            }
            for (int i2 = this.receivers.size() - 1; i2 >= 0; i2--) {
                if (name.equals(this.receivers.get(i2).className)) {
                    return true;
                }
            }
            for (int i3 = this.providers.size() - 1; i3 >= 0; i3--) {
                if (name.equals(this.providers.get(i3).className)) {
                    return true;
                }
            }
            for (int i4 = this.services.size() - 1; i4 >= 0; i4--) {
                if (name.equals(this.services.get(i4).className)) {
                    return true;
                }
            }
            for (int i5 = this.instrumentation.size() - 1; i5 >= 0; i5--) {
                if (name.equals(this.instrumentation.get(i5).className)) {
                    return true;
                }
            }
            return false;
        }

        public String toString() {
            return "Package{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.packageName + "}";
        }
    }

    public static class Component<II extends IntentInfo> {
        public final String className;
        ComponentName componentName;
        String componentShortName;
        public final ArrayList<II> intents;
        public Bundle metaData;
        public final Package owner;

        public Component(Package _owner) {
            this.owner = _owner;
            this.intents = null;
            this.className = null;
        }

        public Component(ParsePackageItemArgs args, PackageItemInfo outInfo) {
            this.owner = args.owner;
            this.intents = new ArrayList<>(0);
            String name = args.sa.getNonConfigurationString(args.nameRes, 0);
            if (name != null) {
                outInfo.name = PackageParser.buildClassName(this.owner.applicationInfo.packageName, name, args.outError);
                if (outInfo.name == null) {
                    this.className = null;
                    args.outError[0] = args.tag + " does not have valid android:name";
                    return;
                }
                this.className = outInfo.name;
                int iconVal = args.sa.getResourceId(args.iconRes, 0);
                if (iconVal != 0) {
                    outInfo.icon = iconVal;
                    outInfo.nonLocalizedLabel = null;
                }
                int logoVal = args.sa.getResourceId(args.logoRes, 0);
                if (logoVal != 0) {
                    outInfo.logo = logoVal;
                }
                int bannerVal = args.sa.getResourceId(args.bannerRes, 0);
                if (bannerVal != 0) {
                    outInfo.banner = bannerVal;
                }
                TypedValue v = args.sa.peekValue(args.labelRes);
                if (v != null) {
                    int i = v.resourceId;
                    outInfo.labelRes = i;
                    if (i == 0) {
                        outInfo.nonLocalizedLabel = v.coerceToString();
                    }
                }
                outInfo.packageName = this.owner.packageName;
                return;
            }
            this.className = null;
            args.outError[0] = args.tag + " does not specify android:name";
        }

        public Component(ParseComponentArgs args, ComponentInfo outInfo) {
            CharSequence pname;
            this((ParsePackageItemArgs) args, (PackageItemInfo) outInfo);
            if (args.outError[0] == null) {
                if (args.processRes != 0) {
                    if (this.owner.applicationInfo.targetSdkVersion >= 8) {
                        pname = args.sa.getNonConfigurationString(args.processRes, 1024);
                    } else {
                        pname = args.sa.getNonResourceString(args.processRes);
                    }
                    outInfo.processName = PackageParser.buildProcessName(this.owner.applicationInfo.packageName, this.owner.applicationInfo.processName, pname, args.flags, args.sepProcesses, args.outError);
                }
                if (args.descriptionRes != 0) {
                    outInfo.descriptionRes = args.sa.getResourceId(args.descriptionRes, 0);
                }
                outInfo.enabled = args.sa.getBoolean(args.enabledRes, true);
            }
        }

        public Component(Component<II> clone) {
            this.owner = clone.owner;
            this.intents = clone.intents;
            this.className = clone.className;
            this.componentName = clone.componentName;
            this.componentShortName = clone.componentShortName;
        }

        public ComponentName getComponentName() {
            if (this.componentName != null) {
                return this.componentName;
            }
            if (this.className != null) {
                this.componentName = new ComponentName(this.owner.applicationInfo.packageName, this.className);
            }
            return this.componentName;
        }

        public void appendComponentShortName(StringBuilder sb) {
            ComponentName.appendShortString(sb, this.owner.applicationInfo.packageName, this.className);
        }

        public void printComponentShortName(PrintWriter pw) {
            ComponentName.printShortString(pw, this.owner.applicationInfo.packageName, this.className);
        }

        public void setPackageName(String packageName) {
            this.componentName = null;
            this.componentShortName = null;
        }
    }

    public static final class Permission extends Component<IntentInfo> {
        public PermissionGroup group;
        public final PermissionInfo info;
        public boolean tree;

        public Permission(Package _owner) {
            super(_owner);
            this.info = new PermissionInfo();
        }

        public Permission(Package _owner, PermissionInfo _info) {
            super(_owner);
            this.info = _info;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            return "Permission{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.info.name + "}";
        }
    }

    public static final class PermissionGroup extends Component<IntentInfo> {
        public final PermissionGroupInfo info;

        public PermissionGroup(Package _owner) {
            super(_owner);
            this.info = new PermissionGroupInfo();
        }

        public PermissionGroup(Package _owner, PermissionGroupInfo _info) {
            super(_owner);
            this.info = _info;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            return "PermissionGroup{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.info.name + "}";
        }
    }

    private static boolean copyNeeded(int flags, Package p, PackageUserState state, Bundle metaData, int userId) {
        if (userId != 0) {
            return true;
        }
        if (state.enabled != 0) {
            boolean enabled = state.enabled == 1;
            if (p.applicationInfo.enabled != enabled) {
                return true;
            }
        }
        if (!state.installed || state.hidden || state.stopped) {
            return true;
        }
        if ((flags & 128) == 0 || (metaData == null && p.mAppMetaData == null)) {
            return ((flags & 1024) == 0 || p.usesLibraryFiles == null) ? false : true;
        }
        return true;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state) {
        return generateApplicationInfo(p, flags, state, UserHandle.getCallingUserId());
    }

    private static void updateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state) {
        if (!sCompatibilityModeEnabled) {
            ai.disableCompatibilityMode();
        }
        if (state.installed) {
            ai.flags |= 8388608;
        } else {
            ai.flags &= -8388609;
        }
        if (state.hidden) {
            ai.flags |= 134217728;
        } else {
            ai.flags &= -134217729;
        }
        if (state.enabled == 1) {
            ai.enabled = true;
        } else if (state.enabled == 4) {
            ai.enabled = (32768 & flags) != 0;
        } else if (state.enabled == 2 || state.enabled == 3) {
            ai.enabled = false;
        }
        ai.enabledSetting = state.enabled;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags, PackageUserState state, int userId) {
        if (p == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        if (!copyNeeded(flags, p, state, null, userId) && ((32768 & flags) == 0 || state.enabled != 4)) {
            updateApplicationInfo(p.applicationInfo, flags, state);
            return p.applicationInfo;
        }
        ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
        if (userId != 0) {
            ai.uid = UserHandle.getUid(userId, ai.uid);
            ai.dataDir = PackageManager.getDataDirForUser(userId, ai.packageName);
        }
        if ((flags & 128) != 0) {
            ai.metaData = p.mAppMetaData;
        }
        if ((flags & 1024) != 0) {
            ai.sharedLibraryFiles = p.usesLibraryFiles;
        }
        if (state.stopped) {
            ai.flags |= 2097152;
        } else {
            ai.flags &= -2097153;
        }
        updateApplicationInfo(ai, flags, state);
        return ai;
    }

    public static ApplicationInfo generateApplicationInfo(ApplicationInfo ai, int flags, PackageUserState state, int userId) {
        ApplicationInfo ai2 = null;
        if (ai != null && checkUseInstalledOrHidden(flags, state)) {
            ai2 = new ApplicationInfo(ai);
            if (userId != 0) {
                ai2.uid = UserHandle.getUid(userId, ai2.uid);
                ai2.dataDir = PackageManager.getDataDirForUser(userId, ai2.packageName);
            }
            if (state.stopped) {
                ai2.flags |= 2097152;
            } else {
                ai2.flags &= -2097153;
            }
            updateApplicationInfo(ai2, flags, state);
        }
        return ai2;
    }

    public static final PermissionInfo generatePermissionInfo(Permission p, int flags) {
        if (p == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static final PermissionGroupInfo generatePermissionGroupInfo(PermissionGroup pg, int flags) {
        if (pg == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return pg.info;
        }
        PermissionGroupInfo pgi = new PermissionGroupInfo(pg.info);
        pgi.metaData = pg.metaData;
        return pgi;
    }

    public static final class Activity extends Component<ActivityIntentInfo> {
        public final ActivityInfo info;

        public Activity(ParseComponentArgs args, ActivityInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Activity{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }
    }

    public static final ActivityInfo generateActivityInfo(Activity a, int flags, PackageUserState state, int userId) {
        if (a == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        if (!copyNeeded(flags, a.owner, state, a.metaData, userId)) {
            return a.info;
        }
        ActivityInfo ai = new ActivityInfo(a.info);
        ai.metaData = a.metaData;
        ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
        return ai;
    }

    public static final ActivityInfo generateActivityInfo(ActivityInfo ai, int flags, PackageUserState state, int userId) {
        if (ai == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        ActivityInfo ai2 = new ActivityInfo(ai);
        ai2.applicationInfo = generateApplicationInfo(ai2.applicationInfo, flags, state, userId);
        return ai2;
    }

    public static final class Service extends Component<ServiceIntentInfo> {
        public final ServiceInfo info;

        public Service(ParseComponentArgs args, ServiceInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Service{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }
    }

    public static final ServiceInfo generateServiceInfo(Service s, int flags, PackageUserState state, int userId) {
        if (s == null || !checkUseInstalledOrHidden(flags, state)) {
            return null;
        }
        if (!copyNeeded(flags, s.owner, state, s.metaData, userId)) {
            return s.info;
        }
        ServiceInfo si = new ServiceInfo(s.info);
        si.metaData = s.metaData;
        si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
        return si;
    }

    public static final class Provider extends Component<ProviderIntentInfo> {
        public final ProviderInfo info;
        public boolean syncable;

        public Provider(ParseComponentArgs args, ProviderInfo _info) {
            super(args, (ComponentInfo) _info);
            this.info = _info;
            this.info.applicationInfo = args.owner.applicationInfo;
            this.syncable = false;
        }

        public Provider(Provider existingProvider) {
            super(existingProvider);
            this.info = existingProvider.info;
            this.syncable = existingProvider.syncable;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Provider{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }
    }

    public static final ProviderInfo generateProviderInfo(Provider p, int flags, PackageUserState state, int userId) {
        if (p != null && checkUseInstalledOrHidden(flags, state)) {
            if (!copyNeeded(flags, p.owner, state, p.metaData, userId) && ((flags & 2048) != 0 || p.info.uriPermissionPatterns == null)) {
                return p.info;
            }
            ProviderInfo pi = new ProviderInfo(p.info);
            pi.metaData = p.metaData;
            if ((flags & 2048) == 0) {
                pi.uriPermissionPatterns = null;
            }
            pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
            return pi;
        }
        return null;
    }

    public static final class Instrumentation extends Component {
        public final InstrumentationInfo info;

        public Instrumentation(ParsePackageItemArgs args, InstrumentationInfo _info) {
            super(args, _info);
            this.info = _info;
        }

        @Override
        public void setPackageName(String packageName) {
            super.setPackageName(packageName);
            this.info.packageName = packageName;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Instrumentation{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }
    }

    public static final InstrumentationInfo generateInstrumentationInfo(Instrumentation i, int flags) {
        if (i == null) {
            return null;
        }
        if ((flags & 128) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    public static final class ActivityIntentInfo extends IntentInfo {
        public final Activity activity;

        public ActivityIntentInfo(Activity _activity) {
            this.activity = _activity;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActivityIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.activity.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }
    }

    public static final class ServiceIntentInfo extends IntentInfo {
        public final Service service;

        public ServiceIntentInfo(Service _service) {
            this.service = _service;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ServiceIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.service.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }
    }

    public static final class ProviderIntentInfo extends IntentInfo {
        public final Provider provider;

        public ProviderIntentInfo(Provider provider) {
            this.provider = provider;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ProviderIntentInfo{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            this.provider.appendComponentShortName(sb);
            sb.append('}');
            return sb.toString();
        }
    }

    public static void setCompatibilityModeEnabled(boolean compatibilityModeEnabled) {
        sCompatibilityModeEnabled = compatibilityModeEnabled;
    }

    public static long readFullyIgnoringContents(InputStream in) throws IOException {
        byte[] buffer = sBuffer.getAndSet(null);
        if (buffer == null) {
            buffer = new byte[4096];
        }
        int count = 0;
        while (true) {
            int n = in.read(buffer, 0, buffer.length);
            if (n != -1) {
                count += n;
            } else {
                sBuffer.set(buffer);
                return count;
            }
        }
    }

    public static void closeQuietly(StrictJarFile jarFile) {
        if (jarFile != null) {
            try {
                jarFile.close();
            } catch (Exception e) {
            }
        }
    }

    public static class PackageParserException extends Exception {
        public final int error;

        public PackageParserException(int error, String detailMessage) {
            super(detailMessage);
            this.error = error;
        }

        public PackageParserException(int error, String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
            this.error = error;
        }
    }
}
